package com.identityeudi.backend.presentation.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.identityeudi.backend.presentation.domain.Presentation
import com.identityeudi.backend.presentation.domain.PresentationState
import com.identityeudi.backend.presentation.service.CreatedPresentation
import com.identityeudi.backend.presentation.service.PresentationService
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

@WebMvcTest(PresentationController::class)
@Import(PresentationControllerTest.MockedServiceConfig::class)
class PresentationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var presentationService: PresentationService

    @TestConfiguration
    class MockedServiceConfig {
        @Bean
        fun presentationService(): PresentationService = mockk()
    }

    @Test
    fun `POST api presentation returns 201 with the mapped response body`() {
        val tenant = "check24-bank"
        val presentationSessionId = UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18")
        val createdAt = OffsetDateTime.of(2026, 6, 8, 9, 53, 57, 0, ZoneOffset.UTC)
        val expiresAt = OffsetDateTime.of(2026, 6, 8, 10, 53, 57, 0, ZoneOffset.UTC)
        val created = CreatedPresentation(
            presentation = Presentation(
                id = presentationSessionId,
                state = PresentationState.CREATED,
                createdAt = createdAt,
                expiresAt = expiresAt,
                tenant = tenant,
            ),
            presentationRequestUri = "openid4vp://?request_uri=https://example.com/req",
        )
        every { presentationService.createPresentation(tenant) } returns created

        val requestBody = objectMapper.writeValueAsString(mapOf("tenant" to tenant))

        mockMvc.perform(
            post("/api/presentation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.presentationSessionId").value(presentationSessionId.toString()))
            .andExpect(jsonPath("$.presentationRequestUri").value(created.presentationRequestUri))
            .andExpect(jsonPath("$.state").value("CREATED"))
            .andExpect(jsonPath("$.createdAt").doesNotExist())
            .andExpect(jsonPath("$.expiresAt").doesNotExist())

        verify(exactly = 1) { presentationService.createPresentation(tenant) }
    }

    @Test
    fun `POST api presentation returns 400 when tenant is blank`() {
        val requestBody = """{"tenant":""}"""

        mockMvc.perform(
            post("/api/presentation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        ).andExpect(status().isBadRequest)

        verify(exactly = 0) { presentationService.createPresentation(any()) }
    }

    @Test
    fun `GET api presentation by id returns 200 with the current state`() {
        val presentationSessionId = UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18")
        every { presentationService.pollPresentation(presentationSessionId) } returns PresentationState.COMPLETE

        mockMvc.perform(get("/api/presentation/{presentationSessionId}", presentationSessionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.state").value("COMPLETE"))

        verify(exactly = 1) { presentationService.pollPresentation(presentationSessionId) }
    }

    @Test
    fun `POST api presentation returns 400 when body is missing required fields`() {
        val requestBody = """{}"""

        mockMvc.perform(
            post("/api/presentation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        ).andExpect(status().isBadRequest)
    }
}
