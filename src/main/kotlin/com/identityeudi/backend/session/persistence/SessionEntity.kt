package com.identityeudi.backend.session.persistence

import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Session JPA entity.
 */
@Entity
@Table(name = "sessions")
class SessionEntity(

    @Id
    @Column(name = "id", nullable = false, unique = true, updatable = false)
    val id: UUID,

    @Column(name = "state", nullable = false)
    var state: String,

    @Column(name = "credential_id", nullable = false)
    var credentialId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime,

    @Column(name = "tenant", nullable = false)
    var tenant: String,
) {
    fun toDomain(): Session = Session(
        id = id,
        state = SessionState.valueOf(state),
        credentialId = credentialId,
        createdAt = createdAt,
        expiresAt = expiresAt,
        tenant = tenant,
    )

    companion object {
        fun fromDomain(session: Session): SessionEntity = SessionEntity(
            id = session.id,
            state = session.state.name,
            credentialId = session.credentialId,
            createdAt = session.createdAt,
            expiresAt = session.expiresAt,
            tenant = session.tenant,
        )
    }
}
