package com.identityeudi.backend.session.domain

import java.util.UUID

/**
 * Session repository interface.
 *
 * @property save Saves a session.
 * @property findById Finds a session by ID.
 */
interface SessionRepository {
    /**
     * Saves a session.
     *
     * @param session The session to save.
     * @return The saved session.
     */
    fun save(session: Session): Session

    /**
     * Finds a session by ID.
     *
     * @param id The session ID.
     * @return The found session, or null if not found.
     */
    fun findById(id: UUID): Session?
}
