package com.identityeudi.backend.presentation.persistence

import com.identityeudi.backend.presentation.domain.Presentation
import com.identityeudi.backend.presentation.domain.PresentationRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Adapter that implements the domain port [PresentationRepository] using JPA.
 * This is the only place in the codebase where we cross the
 * `domain` ↔ `persistence` boundary.
 */
@Repository
class PresentationRepositoryAdapter(
    private val repository: PresentationJpaRepository,
) : PresentationRepository {

    override fun save(presentation: Presentation): Presentation =
        repository.save(PresentationEntity.fromDomain(presentation)).toDomain()

    override fun findById(id: UUID): Presentation? =
        repository.findById(id).map(PresentationEntity::toDomain).orElse(null)
}
