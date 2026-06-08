package com.identityeudi.backend.presentation.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Subset of the Lissi presentation response we actually consume.
 *
 * Unknown fields are ignored so the connector can keep adding properties
 * without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PresentationSessionResponse(
    val presentationSessionId: UUID,
    val state: String,
    val presentationRequestUri: String,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
)
