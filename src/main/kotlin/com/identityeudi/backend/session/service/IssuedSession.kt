package com.identityeudi.backend.session.service

import com.identityeudi.backend.session.domain.Session

/**
 * Result of a successful session issuance.
 *
 * Bundles the persisted [session] with the *ephemeral* pieces of information
 * returned by the connector that are needed by the caller (typically the
 * user-facing API) but that we deliberately do not persist:
 * the wallet redirect URL and the one-time password.
 */
data class IssuedSession(
    val session: Session,
    val redirectUrl: String,
    val oneTimePassword: String,
)
