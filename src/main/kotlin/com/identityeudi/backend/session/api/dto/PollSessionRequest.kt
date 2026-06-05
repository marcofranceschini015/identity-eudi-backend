package com.identityeudi.backend.session.api.dto

import jakarta.validation.constraints.NotBlank

/** Body of `GET /api/session/{sessionId}`. */
data class PollSessionRequest(
    @field:NotBlank
    val tenant: String,
)
