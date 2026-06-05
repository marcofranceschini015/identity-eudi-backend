package com.identityeudi.backend.session.domain

enum class SessionState {
    CREATED,
    ISSUED,
    FAILED,
    REVOKED,
}
