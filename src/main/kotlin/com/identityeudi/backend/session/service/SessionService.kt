package com.identityeudi.backend.session.service

import com.identityeudi.backend.session.client.LissiClient
import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionRepository
import com.identityeudi.backend.session.domain.SessionState
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Application service for the session use cases.
 *
 * Orchestrates the outbound call to the Lissi connector, builds the
 * domain [Session] from the connector's response, persists it through
 * the [SessionRepository] port, and returns an [IssuedSession] with
 * everything the caller needs.
 */
@Service
class SessionService(
    private val lissiClient: LissiClient,
    private val sessionRepository: SessionRepository,
) {

    fun createSession(tenant: String, userData: Map<String, String>): IssuedSession {
        val response = lissiClient.createIssuanceSession(
            tenantName = tenant,
            subjectClaims = userData,
        )

        val session = Session(
            id = response.id,
            state = SessionState.valueOf(response.state),
            credentialId = response.issuance.credentialId,
            createdAt = response.createdAt,
            expiresAt = response.expiresAt,
            tenant = tenant,
        )

        val persisted = sessionRepository.save(session)

        return IssuedSession(
            session = persisted,
            redirectUrl = response.credentialOfferDetails.credentialOfferUri,
            oneTimePassword = response.credentialOfferDetails.oneTimePassword,
        )
    }

    /**
     * Polls the connector for the current state of the session.
     *
     * The session row is loaded first so we can recover the tenant it
     * originally belonged to; this lets the public API expose the session by
     * id alone, without forcing the client to remember which tenant created
     * it. If the state has moved on from the initial [SessionState.CREATED]
     * value, the persisted session is updated to match; the initial state
     * never triggers a database write.
     *
     * @return the latest state reported by the connector
     * @throws IllegalStateException if the local session cannot be found
     */
    fun pollSession(sessionId: UUID): SessionState {
        val existing = sessionRepository.findById(sessionId)
            ?: throw IllegalStateException("Session not found: $sessionId")

        val response = lissiClient.getIssuanceSession(existing.tenant, sessionId)
        val newState = SessionState.valueOf(response.state)

        if (newState != SessionState.CREATED) {
            sessionRepository.save(existing.copy(state = newState))
        }

        return newState
    }
}
