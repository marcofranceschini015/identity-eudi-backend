package com.identityeudi.backend.presentation.persistence

import com.identityeudi.backend.presentation.domain.Presentation
import com.identityeudi.backend.presentation.domain.PresentationState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Presentation JPA entity.
 */
@Entity
@Table(name = "presentations")
class PresentationEntity(

    @Id
    @Column(name = "id", nullable = false, unique = true, updatable = false)
    val id: UUID,

    @Column(name = "state", nullable = false)
    var state: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime,

    @Column(name = "tenant", nullable = false)
    var tenant: String,
) {
    fun toDomain(): Presentation = Presentation(
        id = id,
        state = PresentationState.valueOf(state),
        createdAt = createdAt,
        expiresAt = expiresAt,
        tenant = tenant,
    )

    companion object {
        fun fromDomain(presentation: Presentation): PresentationEntity = PresentationEntity(
            id = presentation.id,
            state = presentation.state.name,
            createdAt = presentation.createdAt,
            expiresAt = presentation.expiresAt,
            tenant = presentation.tenant,
        )
    }
}
