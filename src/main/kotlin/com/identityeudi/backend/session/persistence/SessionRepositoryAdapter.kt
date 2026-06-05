package com.identityeudi.backend.session.persistence

import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Adapter that implements the domain port [SessionRepository] using JPA.
 * This is the only place in the codebase where we cross the
 * `domain` ↔ `persistence` boundary.
 */
@Repository
class SessionRepositoryAdapter(
    private val repository: SessionJpaRepository,
) : SessionRepository {

    override fun save(session: Session): Session =
        repository.save(SessionEntity.fromDomain(session)).toDomain()

    override fun findById(id: UUID): Session? =
        repository.findById(id).map(SessionEntity::toDomain).orElse(null)
}
