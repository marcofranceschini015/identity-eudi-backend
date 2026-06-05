package com.identityeudi.backend.session.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Session domain model.
 *
 * @property id The session ID.
 * @property state The session state.
 * @property credentialId The credential ID.
 * @property createdAt The session creation timestamp.
 * @property expiresAt The session expiration timestamp.
 * @property tenant The tenant ID.
 */
data class Session(
    val id: UUID,
    val state: SessionState,
    val credentialId: UUID,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    val tenant: String,
)
