package com.identityeudi.backend.presentation.api.dto

import com.identityeudi.backend.presentation.domain.PresentationState
import com.identityeudi.backend.presentation.service.CreatedPresentation
import java.util.UUID

/** Response of `POST /api/presentation`. */
data class CreatePresentationResponse(
    val presentationSessionId: UUID,
    val presentationRequestUri: String,
    val state: PresentationState,
) {
    companion object {
        fun from(created: CreatedPresentation): CreatePresentationResponse = CreatePresentationResponse(
            presentationSessionId = created.presentation.id,
            presentationRequestUri = created.presentationRequestUri,
            state = created.presentation.state,
        )
    }
}
