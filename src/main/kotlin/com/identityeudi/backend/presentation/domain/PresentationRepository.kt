package com.identityeudi.backend.presentation.domain

import java.util.UUID

/**
 * Presentation repository port.
 */
interface PresentationRepository {
    /**
     * Saves a presentation.
     *
     * @param presentation The presentation to save.
     * @return The saved presentation.
     */
    fun save(presentation: Presentation): Presentation

    /**
     * Finds a presentation by ID.
     *
     * @param id The presentation ID.
     * @return The found presentation, or null if not found.
     */
    fun findById(id: UUID): Presentation?
}
