package com.identityeudi.backend.session.client.dto

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Body of `POST /api/v1/issuance-sessions` on a Lissi demo connector.
 *
 * Kept as plain DTOs (separate from the domain model) so the wire format
 * can evolve independently from [com.identityeudi.backend.session.domain.Session].
 */
data class IssuanceSessionRequest(
    val flow: Flow,
    val externalUserId: UUID,
    val credentialTemplateName: String,
    val revocable: Boolean,
    val validFrom: OffsetDateTime,
    val validUntil: OffsetDateTime,
    val subjectClaims: Map<String, String>,
) {
    data class Flow(val preAuthorizedCode: PreAuthorizedCode)
    data class PreAuthorizedCode(val oneTimePassword: Boolean)
}
