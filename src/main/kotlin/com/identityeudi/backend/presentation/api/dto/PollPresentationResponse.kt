package com.identityeudi.backend.presentation.api.dto

import com.identityeudi.backend.presentation.domain.PresentationState

/** Response of `GET /api/presentation/{presentationSessionId}`. */
data class PollPresentationResponse(
    val state: PresentationState,
)
