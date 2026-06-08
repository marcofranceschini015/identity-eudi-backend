package com.identityeudi.backend.presentation.service

import com.identityeudi.backend.presentation.client.LissiPresentationClient
import com.identityeudi.backend.presentation.client.dto.PresentationSessionResponse
import com.identityeudi.backend.presentation.domain.Presentation
import com.identityeudi.backend.presentation.domain.PresentationRepository
import com.identityeudi.backend.presentation.domain.PresentationState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PresentationServiceTest {

    private val lissiPresentationClient: LissiPresentationClient = mockk()
    private val presentationRepository: PresentationRepository = mockk()
    private val service = PresentationService(lissiPresentationClient, presentationRepository)

    private val tenant = "check24-bank"
    private val presentationSessionId = UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18")
    private val createdAt = OffsetDateTime.of(2026, 6, 8, 9, 53, 57, 419_000_000, ZoneOffset.UTC)
    private val expiresAt = OffsetDateTime.of(2026, 6, 8, 10, 53, 57, 419_000_000, ZoneOffset.UTC)

    @Test
    fun `createPresentation calls the client, persists the domain presentation, and returns a CreatedPresentation`() {
        val connectorResponse = PresentationSessionResponse(
            presentationSessionId = presentationSessionId,
            state = "CREATED",
            presentationRequestUri = "openid4vp://?request_uri=https://example.com/req",
            createdAt = createdAt,
            expiresAt = expiresAt,
        )
        every { lissiPresentationClient.createPresentationSession(tenant) } returns connectorResponse
        val savedSlot = slot<Presentation>()
        every { presentationRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.createPresentation(tenant)

        assertThat(savedSlot.captured).isEqualTo(
            Presentation(
                id = presentationSessionId,
                state = PresentationState.CREATED,
                createdAt = createdAt,
                expiresAt = expiresAt,
                tenant = tenant,
            ),
        )
        assertThat(result.presentation).isEqualTo(savedSlot.captured)
        assertThat(result.presentationRequestUri)
            .isEqualTo("openid4vp://?request_uri=https://example.com/req")

        verify(exactly = 1) { lissiPresentationClient.createPresentationSession(tenant) }
        verify(exactly = 1) { presentationRepository.save(any()) }
    }

    @Test
    fun `createPresentation propagates client failures without touching the repository`() {
        every { lissiPresentationClient.createPresentationSession(any()) }
            .throws(IllegalArgumentException("Unknown tenant: nope"))

        assertThatThrownBy { service.createPresentation("nope") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("nope")

        verify(exactly = 0) { presentationRepository.save(any()) }
    }

    @Test
    fun `pollPresentation loads the presentation to recover the tenant and skips the write when state is still CREATED`() {
        val persisted = Presentation(
            id = presentationSessionId,
            state = PresentationState.CREATED,
            createdAt = createdAt,
            expiresAt = expiresAt,
            tenant = tenant,
        )
        every { presentationRepository.findById(presentationSessionId) } returns persisted
        every {
            lissiPresentationClient.getPresentationSession(tenant, presentationSessionId)
        } returns connectorResponse(state = "CREATED")

        val state = service.pollPresentation(presentationSessionId)

        assertThat(state).isEqualTo(PresentationState.CREATED)
        verify(exactly = 1) { presentationRepository.findById(presentationSessionId) }
        verify(exactly = 1) { lissiPresentationClient.getPresentationSession(tenant, presentationSessionId) }
        verify(exactly = 0) { presentationRepository.save(any()) }
    }

    @Test
    fun `pollPresentation updates the persisted presentation and returns the new state when it has moved on`() {
        val persisted = Presentation(
            id = presentationSessionId,
            state = PresentationState.CREATED,
            createdAt = createdAt,
            expiresAt = expiresAt,
            tenant = tenant,
        )
        every { presentationRepository.findById(presentationSessionId) } returns persisted
        every {
            lissiPresentationClient.getPresentationSession(tenant, presentationSessionId)
        } returns connectorResponse(state = "COMPLETE")
        val savedSlot = slot<Presentation>()
        every { presentationRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val state = service.pollPresentation(presentationSessionId)

        assertThat(state).isEqualTo(PresentationState.COMPLETE)
        assertThat(savedSlot.captured).isEqualTo(persisted.copy(state = PresentationState.COMPLETE))
        verify(exactly = 1) { presentationRepository.findById(presentationSessionId) }
        verify(exactly = 1) { presentationRepository.save(any()) }
    }

    @Test
    fun `pollPresentation throws and never calls the connector when the presentation is not in the database`() {
        every { presentationRepository.findById(presentationSessionId) } returns null

        assertThatThrownBy { service.pollPresentation(presentationSessionId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(presentationSessionId.toString())

        verify(exactly = 0) { lissiPresentationClient.getPresentationSession(any(), any()) }
        verify(exactly = 0) { presentationRepository.save(any()) }
    }

    private fun connectorResponse(state: String) = PresentationSessionResponse(
        presentationSessionId = presentationSessionId,
        state = state,
        presentationRequestUri = "openid4vp://?request_uri=https://example.com/req",
        createdAt = createdAt,
        expiresAt = expiresAt,
    )
}
