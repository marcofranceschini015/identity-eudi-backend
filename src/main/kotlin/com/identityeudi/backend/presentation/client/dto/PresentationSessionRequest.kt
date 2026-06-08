package com.identityeudi.backend.presentation.client.dto

import java.util.UUID

/**
 * Body of `POST /api/v1/presentation-sessions` on a Lissi demo connector.
 */
data class PresentationSessionRequest(
    val presentationTemplateName: String,
    val externalUserId: UUID,
)
