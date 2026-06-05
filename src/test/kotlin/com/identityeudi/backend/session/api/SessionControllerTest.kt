package com.identityeudi.backend.session.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionState
import com.identityeudi.backend.session.service.IssuedSession
import com.identityeudi.backend.session.service.SessionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@WebMvcTest(SessionController::class)
@Import(SessionControllerTest.MockedServiceConfig::class)
class SessionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var sessionService: SessionService

    @TestConfiguration
    class MockedServiceConfig {
        @Bean
        fun sessionService(): SessionService = mockk()
    }

    @Test
    fun `POST api session returns 201 with the mapped response body`() {
        val tenant = "check24-bank"
        val sessionId = UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18")
        val issued = IssuedSession(
            session = Session(
                id = sessionId,
                state = SessionState.CREATED,
                credentialId = UUID.fromString("248068f8-0189-4343-bbcb-8e5b0af5dbc5"),
                createdAt = OffsetDateTime.of(2026, 6, 5, 7, 29, 38, 0, ZoneOffset.UTC),
                expiresAt = OffsetDateTime.of(2026, 6, 5, 7, 39, 38, 0, ZoneOffset.UTC),
                tenant = tenant,
            ),
            redirectUrl = "openid-credential-offer://?credential_offer_uri=foo",
            oneTimePassword = "1944",
        )
        val userData = mapOf("first_name" to "John", "iban" to "testIban")
        every { sessionService.createSession(tenant, userData) } returns issued

        val requestBody = objectMapper.writeValueAsString(
            mapOf("tenant" to tenant, "userData" to userData),
        )

        mockMvc.perform(
            post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
            .andExpect(jsonPath("$.state").value("CREATED"))
            .andExpect(jsonPath("$.redirectUrl").value(issued.redirectUrl))
            .andExpect(jsonPath("$.oneTimePassword").value("1944"))

        verify(exactly = 1) { sessionService.createSession(tenant, userData) }
    }

    @Test
    fun `POST api session returns 400 when tenant is blank`() {
        val requestBody = """{"tenant":"","userData":{}}"""

        mockMvc.perform(
            post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { sessionService.createSession(any(), any()) }
    }

    @Test
    fun `GET api session by id returns 200 with the current state`() {
        val tenant = "check24-bank"
        val sessionId = UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18")
        every { sessionService.pollSession(tenant, sessionId) } returns SessionState.ISSUED

        mockMvc.perform(
            get("/api/session/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"tenant":"$tenant"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.state").value("ISSUED"))

        verify(exactly = 1) { sessionService.pollSession(tenant, sessionId) }
    }

    @Test
    fun `GET api session by id returns 400 when tenant is blank`() {
        val sessionId = UUID.randomUUID()

        mockMvc.perform(
            get("/api/session/{sessionId}", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"tenant":""}"""),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { sessionService.pollSession(any(), any()) }
    }

    @Test
    fun `POST api session returns 400 when body is missing required fields`() {
        val requestBody = """{"tenant":"check24-bank"}"""

        mockMvc.perform(
            post("/api/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        ).andExpect(status().isBadRequest)
    }
}
