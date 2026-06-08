package com.identityeudi.backend.presentation.api.dto

import jakarta.validation.constraints.NotBlank

/** Body of `POST /api/presentation`. */
data class CreatePresentationRequest(
    @field:NotBlank
    val tenant: String,
)
