package com.identityeudi.backend.presentation.client

import com.identityeudi.backend.config.TenantsProperties
import com.identityeudi.backend.session.client.LissiProperties
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.UUID

class LissiPresentationClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: LissiPresentationClient

    private val tenantName = "check24-bank"
    private val apiKey = "test-api-key"
    private val presentationTemplate = "LoanPresentation"

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }

        val tenants = TenantsProperties(
            tenants = listOf(
                TenantsProperties.Tenant(
                    name = tenantName,
                    apiKey = apiKey,
                    template = "LoanCredential",
                    presentation = presentationTemplate,
                ),
            ),
        )
        // The base URL template normally contains `{tenant}` so the host varies
        // per tenant; in the test we point at a single MockWebServer regardless,
        // so the placeholder is simply omitted.
        val lissiProperties = LissiProperties(
            baseUrlTemplate = "http://${server.hostName}:${server.port}",
        )

        client = LissiPresentationClient(
            tenants = tenants,
            lissiProperties = lissiProperties,
            lissiWebClient = WebClient.builder().build(),
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `createPresentationSession posts the expected payload and returns the parsed response`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SAMPLE_RESPONSE),
        )

        val response = client.createPresentationSession(tenantName = tenantName)

        assertThat(response.presentationSessionId)
            .isEqualTo(UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18"))
        assertThat(response.state).isEqualTo("CREATED")
        assertThat(response.presentationRequestUri)
            .isEqualTo("openid4vp://?request_uri=https://example.com/req")
        assertThat(response.createdAt.toString()).isEqualTo("2026-06-08T09:53:57.419Z")
        assertThat(response.expiresAt.toString()).isEqualTo("2026-06-08T10:53:57.419Z")

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/api/v1/presentation-sessions")
        assertThat(recorded.getHeader("Content-Type")).startsWith("application/json")
        assertThat(recorded.getHeader("LC-Api-Key")).isEqualTo(apiKey)

        val body = recorded.body.readUtf8()
        assertThat(body).contains("\"presentationTemplateName\":\"$presentationTemplate\"")
        assertThat(body).contains("\"externalUserId\":\"")
    }

    @Test
    fun `createPresentationSession sends a fresh externalUserId on every call`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SAMPLE_RESPONSE),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SAMPLE_RESPONSE),
        )

        client.createPresentationSession(tenantName)
        client.createPresentationSession(tenantName)

        val first = server.takeRequest().body.readUtf8()
        val second = server.takeRequest().body.readUtf8()

        val externalUserIdRegex = "\"externalUserId\":\"([0-9a-f-]+)\"".toRegex()
        val firstId = externalUserIdRegex.find(first)?.groupValues?.get(1)
        val secondId = externalUserIdRegex.find(second)?.groupValues?.get(1)
        assertThat(firstId).isNotNull()
        assertThat(secondId).isNotNull()
        assertThat(firstId).isNotEqualTo(secondId)
    }

    @Test
    fun `getPresentationSession performs a GET with the API key and returns the parsed response`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SAMPLE_RESPONSE),
        )
        val presentationSessionId = UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18")

        val response = client.getPresentationSession(tenantName, presentationSessionId)

        assertThat(response.presentationSessionId).isEqualTo(presentationSessionId)
        assertThat(response.state).isEqualTo("CREATED")

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("GET")
        assertThat(recorded.path).isEqualTo("/api/v1/presentation-sessions/$presentationSessionId")
        assertThat(recorded.getHeader("LC-Api-Key")).isEqualTo(apiKey)
        assertThat(recorded.bodySize).isZero()
    }

    @Test
    fun `getPresentationSession throws when tenant is not configured`() {
        assertThatThrownBy {
            client.getPresentationSession("unknown-tenant", UUID.randomUUID())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("unknown-tenant")
    }

    @Test
    fun `createPresentationSession throws when tenant is not configured`() {
        assertThatThrownBy {
            client.createPresentationSession("unknown-tenant")
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("unknown-tenant")
    }

    @Test
    fun `createPresentationSession propagates HTTP errors from the connector`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        assertThatThrownBy {
            client.createPresentationSession(tenantName)
        }.isInstanceOf(WebClientResponseException::class.java)
    }

    companion object {
        private val SAMPLE_RESPONSE = """
            {
                "presentationSessionId": "47935416-de71-4711-91ea-c97dd1ab3d18",
                "presentationTemplateName": "LoanPresentation",
                "callBackUrl": "https://example.com/",
                "externalUserId": "93ced838-9a8b-4e59-aed8-e3db65074e97",
                "presentationRequestUri": "openid4vp://?request_uri=https://example.com/req",
                "state": "CREATED",
                "expiresAt": "2026-06-08T10:53:57.419Z",
                "createdAt": "2026-06-08T09:53:57.419Z"
            }
        """.trimIndent()
    }
}
