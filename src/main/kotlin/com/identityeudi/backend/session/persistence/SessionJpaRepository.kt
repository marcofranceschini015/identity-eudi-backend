package com.identityeudi.backend.session.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository.
 */
interface SessionJpaRepository : JpaRepository<SessionEntity, UUID>
