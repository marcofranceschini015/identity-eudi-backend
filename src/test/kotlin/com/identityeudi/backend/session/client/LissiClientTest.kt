package com.identityeudi.backend.session.client

import com.identityeudi.backend.config.TenantsProperties
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

class LissiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: LissiClient

    private val tenantName = "check24-bank"
    private val apiKey = "test-api-key"

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }

        val tenants = TenantsProperties(
            tenants = listOf(
                TenantsProperties.Tenant(
                    name = tenantName,
                    apiKey = apiKey,
                    template = "LoanCredential",
                ),
            ),
        )
        // The base URL template normally contains `{tenant}` so the host varies
        // per tenant; in the test we point at a single MockWebServer regardless,
        // so the placeholder is simply omitted.
        val lissiProperties = LissiProperties(
            baseUrlTemplate = "http://${server.hostName}:${server.port}",
        )

        client = LissiClient(
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
    fun `createIssuanceSession posts the expected payload and returns the parsed response`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SAMPLE_RESPONSE),
        )

        val response = client.createIssuanceSession(
            tenantName = tenantName,
            subjectClaims = mapOf("first_name" to "John", "last_name" to "Doe", "iban" to "testIban"),
        )

        assertThat(response.id).isEqualTo(UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18"))
        assertThat(response.state).isEqualTo("CREATED")
        assertThat(response.issuance.credentialId)
            .isEqualTo(UUID.fromString("248068f8-0189-4343-bbcb-8e5b0af5dbc5"))
        assertThat(response.credentialOfferDetails.credentialOfferUri)
            .startsWith("openid-credential-offer://")
        assertThat(response.credentialOfferDetails.oneTimePassword).isEqualTo("1944")
        assertThat(response.createdAt.toString()).isEqualTo("2026-06-05T07:29:38.082Z")
        assertThat(response.expiresAt.toString()).isEqualTo("2026-06-05T07:39:38.082Z")

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/api/v1/issuance-sessions")
        assertThat(recorded.getHeader("Content-Type")).startsWith("application/json")
        assertThat(recorded.getHeader("LC-Api-Key")).isEqualTo(apiKey)

        val body = recorded.body.readUtf8()
        assertThat(body).contains("\"credentialTemplateName\":\"LoanCredential\"")
        assertThat(body).contains("\"revocable\":true")
        assertThat(body).contains("\"oneTimePassword\":true")
        assertThat(body).contains("\"first_name\":\"John\"")
    }

    @Test
    fun `createIssuanceSession throws when tenant is not configured`() {
        assertThatThrownBy {
            client.createIssuanceSession("unknown-tenant", emptyMap())
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("unknown-tenant")
    }

    @Test
    fun `createIssuanceSession propagates HTTP errors from the connector`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        assertThatThrownBy {
            client.createIssuanceSession(tenantName, emptyMap())
        }.isInstanceOf(WebClientResponseException::class.java)
    }

    companion object {
        private val SAMPLE_RESPONSE = """
            {
                "id": "47935416-de71-4711-91ea-c97dd1ab3d18",
                "state": "CREATED",
                "externalUserId": "93ced838-9a8b-4e59-aed8-e3db65074e97",
                "credentialOfferDetails": {
                    "credentialOfferUri": "openid-credential-offer://?credential_offer_uri=...",
                    "oneTimePassword": "1944"
                },
                "issuance": {
                    "credentialTemplateId": "c0a38342-2157-47f8-93e7-aa3317de75a6",
                    "credentialId": "248068f8-0189-4343-bbcb-8e5b0af5dbc5",
                    "revocable": true,
                    "requireOneTimePassword": true,
                    "validFrom": "2026-06-03T15:38:01.569Z",
                    "validUntil": "2026-06-06T15:38:01.569Z",
                    "subjectClaims": {
                        "first_name": "John",
                        "last_name": "Doe",
                        "iban": "testIban"
                    }
                },
                "createdAt": "2026-06-05T07:29:38.082Z",
                "expiresAt": "2026-06-05T07:39:38.082Z"
            }
        """.trimIndent()
    }
}
