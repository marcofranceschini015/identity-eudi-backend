package com.identityeudi.backend.session.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Subset of the Lissi response we actually consume.
 *
 * Unknown fields are ignored so the connector can keep adding properties
 * without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IssuanceSessionResponse(
    val id: UUID,
    val state: String,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    val issuance: Issuance,
    val credentialOfferDetails: CredentialOfferDetails,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Issuance(val credentialId: UUID)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CredentialOfferDetails(
        val credentialOfferUri: String,
        val oneTimePassword: String,
    )
}
