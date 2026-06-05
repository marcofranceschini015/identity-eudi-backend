package com.identityeudi.backend.session.service

import com.identityeudi.backend.session.client.LissiClient
import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionRepository
import com.identityeudi.backend.session.domain.SessionState
import org.springframework.stereotype.Service

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
}
