package com.identityeudi.backend.presentation.service

import com.identityeudi.backend.presentation.domain.Presentation

/**
 * Result of a successful presentation creation.
 *
 * Bundles the persisted [presentation] with the *ephemeral* pieces of
 * information returned by the connector that are needed by the caller
 * (typically the user-facing API) but that we deliberately do not persist:
 * the wallet-facing presentation request URI.
 */
data class CreatedPresentation(
    val presentation: Presentation,
    val presentationRequestUri: String,
)
