package com.identityeudi.backend.presentation.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Presentation domain model.
 *
 * @property id The presentation session ID.
 * @property state The presentation state.
 * @property createdAt The presentation creation timestamp.
 * @property expiresAt The presentation expiration timestamp.
 * @property tenant The tenant ID.
 */
data class Presentation(
    val id: UUID,
    val state: PresentationState,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    val tenant: String,
)
