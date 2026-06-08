package com.identityeudi.backend.presentation.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository.
 */
interface PresentationJpaRepository : JpaRepository<PresentationEntity, UUID>
