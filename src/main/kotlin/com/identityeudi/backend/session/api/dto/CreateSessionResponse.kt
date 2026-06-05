package com.identityeudi.backend.session.api.dto

import com.identityeudi.backend.session.domain.SessionState
import com.identityeudi.backend.session.service.IssuedSession
import java.util.UUID

/** Response of `POST /api/session`. */
data class CreateSessionResponse(
    val sessionId: UUID,
    val state: SessionState,
    val redirectUrl: String,
    val oneTimePassword: String,
) {
    companion object {
        fun from(issued: IssuedSession): CreateSessionResponse = CreateSessionResponse(
            sessionId = issued.session.id,
            state = issued.session.state,
            redirectUrl = issued.redirectUrl,
            oneTimePassword = issued.oneTimePassword,
        )
    }
}
