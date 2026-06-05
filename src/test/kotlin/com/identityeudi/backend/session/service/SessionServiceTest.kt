package com.identityeudi.backend.session.service

import com.identityeudi.backend.session.client.LissiClient
import com.identityeudi.backend.session.client.dto.IssuanceSessionResponse
import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionRepository
import com.identityeudi.backend.session.domain.SessionState
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

class SessionServiceTest {

    private val lissiClient: LissiClient = mockk()
    private val sessionRepository: SessionRepository = mockk()
    private val service = SessionService(lissiClient, sessionRepository)

    private val tenant = "check24-bank"
    private val sessionId = UUID.fromString("47935416-de71-4711-91ea-c97dd1ab3d18")
    private val credentialId = UUID.fromString("248068f8-0189-4343-bbcb-8e5b0af5dbc5")
    private val createdAt = OffsetDateTime.of(2026, 6, 5, 7, 29, 38, 82_000_000, ZoneOffset.UTC)
    private val expiresAt = OffsetDateTime.of(2026, 6, 5, 7, 39, 38, 82_000_000, ZoneOffset.UTC)

    @Test
    fun `createSession calls the client, persists the domain session, and returns an IssuedSession`() {
        val userData = mapOf("first_name" to "John", "iban" to "testIban")
        val connectorResponse = IssuanceSessionResponse(
            id = sessionId,
            state = "CREATED",
            createdAt = createdAt,
            expiresAt = expiresAt,
            issuance = IssuanceSessionResponse.Issuance(credentialId = credentialId),
            credentialOfferDetails = IssuanceSessionResponse.CredentialOfferDetails(
                credentialOfferUri = "openid-credential-offer://?credential_offer_uri=foo",
                oneTimePassword = "1944",
            ),
        )
        every { lissiClient.createIssuanceSession(tenant, userData) } returns connectorResponse
        val savedSlot = slot<Session>()
        every { sessionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.createSession(tenant, userData)

        assertThat(savedSlot.captured).isEqualTo(
            Session(
                id = sessionId,
                state = SessionState.CREATED,
                credentialId = credentialId,
                createdAt = createdAt,
                expiresAt = expiresAt,
                tenant = tenant,
            ),
        )
        assertThat(result.session).isEqualTo(savedSlot.captured)
        assertThat(result.redirectUrl).isEqualTo("openid-credential-offer://?credential_offer_uri=foo")
        assertThat(result.oneTimePassword).isEqualTo("1944")

        verify(exactly = 1) { lissiClient.createIssuanceSession(tenant, userData) }
        verify(exactly = 1) { sessionRepository.save(any()) }
    }

    @Test
    fun `createSession propagates client failures without touching the repository`() {
        every { lissiClient.createIssuanceSession(any(), any()) }
            .throws(IllegalArgumentException("Unknown tenant: nope"))

        assertThatThrownBy { service.createSession("nope", emptyMap()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("nope")

        verify(exactly = 0) { sessionRepository.save(any()) }
    }

    @Test
    fun `pollSession loads the session to recover the tenant and skips the write when state is still CREATED`() {
        val persisted = Session(
            id = sessionId,
            state = SessionState.CREATED,
            credentialId = credentialId,
            createdAt = createdAt,
            expiresAt = expiresAt,
            tenant = tenant,
        )
        every { sessionRepository.findById(sessionId) } returns persisted
        every { lissiClient.getIssuanceSession(tenant, sessionId) } returns connectorResponse(state = "CREATED")

        val state = service.pollSession(sessionId)

        assertThat(state).isEqualTo(SessionState.CREATED)
        verify(exactly = 1) { sessionRepository.findById(sessionId) }
        verify(exactly = 1) { lissiClient.getIssuanceSession(tenant, sessionId) }
        verify(exactly = 0) { sessionRepository.save(any()) }
    }

    @Test
    fun `pollSession updates the persisted session and returns the new state when it has moved on`() {
        val persisted = Session(
            id = sessionId,
            state = SessionState.CREATED,
            credentialId = credentialId,
            createdAt = createdAt,
            expiresAt = expiresAt,
            tenant = tenant,
        )
        every { sessionRepository.findById(sessionId) } returns persisted
        every { lissiClient.getIssuanceSession(tenant, sessionId) } returns connectorResponse(state = "ISSUED")
        val savedSlot = slot<Session>()
        every { sessionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val state = service.pollSession(sessionId)

        assertThat(state).isEqualTo(SessionState.ISSUED)
        assertThat(savedSlot.captured).isEqualTo(persisted.copy(state = SessionState.ISSUED))
        verify(exactly = 1) { sessionRepository.findById(sessionId) }
        verify(exactly = 1) { sessionRepository.save(any()) }
    }

    @Test
    fun `pollSession throws and never calls the connector when the session is not in the database`() {
        every { sessionRepository.findById(sessionId) } returns null

        assertThatThrownBy { service.pollSession(sessionId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(sessionId.toString())

        verify(exactly = 0) { lissiClient.getIssuanceSession(any(), any()) }
        verify(exactly = 0) { sessionRepository.save(any()) }
    }

    private fun connectorResponse(state: String) = IssuanceSessionResponse(
        id = sessionId,
        state = state,
        createdAt = createdAt,
        expiresAt = expiresAt,
        issuance = IssuanceSessionResponse.Issuance(credentialId = credentialId),
        credentialOfferDetails = IssuanceSessionResponse.CredentialOfferDetails(
            credentialOfferUri = "openid-credential-offer://?credential_offer_uri=foo",
            oneTimePassword = "1944",
        ),
    )
}
