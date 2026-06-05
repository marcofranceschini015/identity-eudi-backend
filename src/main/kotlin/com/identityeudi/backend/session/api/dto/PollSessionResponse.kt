package com.identityeudi.backend.session.api.dto

import com.identityeudi.backend.session.domain.SessionState

/** Response of `GET /api/session/{sessionId}`. */
data class PollSessionResponse(
    val state: SessionState,
)
