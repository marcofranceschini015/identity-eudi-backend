package com.identityeudi.backend.session.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/** Body of `POST /api/session`. */
data class CreateSessionRequest(
    @field:NotBlank
    val tenant: String,

    @field:NotNull
    val userData: Map<String, String>,
)
