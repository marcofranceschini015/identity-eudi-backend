package com.identityeudi.backend.presentation.service

import com.identityeudi.backend.presentation.client.LissiPresentationClient
import com.identityeudi.backend.presentation.domain.Presentation
import com.identityeudi.backend.presentation.domain.PresentationRepository
import com.identityeudi.backend.presentation.domain.PresentationState
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Application service for the presentation use cases.
 *
 * Orchestrates the outbound call to the Lissi connector, builds the
 * domain [Presentation] from the connector's response, persists it through
 * the [PresentationRepository] port, and returns a [CreatedPresentation]
 * with everything the caller needs.
 */
@Service
class PresentationService(
    private val lissiPresentationClient: LissiPresentationClient,
    private val presentationRepository: PresentationRepository,
) {

    fun createPresentation(tenant: String): CreatedPresentation {
        val response = lissiPresentationClient.createPresentationSession(tenantName = tenant)

        val presentation = Presentation(
            id = response.presentationSessionId,
            state = PresentationState.valueOf(response.state),
            createdAt = response.createdAt,
            expiresAt = response.expiresAt,
            tenant = tenant,
        )

        val persisted = presentationRepository.save(presentation)

        return CreatedPresentation(
            presentation = persisted,
            presentationRequestUri = response.presentationRequestUri,
        )
    }

    /**
     * Polls the connector for the current state of the presentation.
     *
     * The presentation row is loaded first so we can recover the tenant it
     * originally belonged to; this lets the public API expose the presentation
     * by id alone, without forcing the client to remember which tenant created
     * it. If the state has moved on from the initial [PresentationState.CREATED]
     * value, the persisted presentation is updated to match; the initial state
     * never triggers a database write.
     *
     * @return the latest state reported by the connector
     * @throws IllegalStateException if the local presentation cannot be found
     */
    fun pollPresentation(presentationSessionId: UUID): PresentationState {
        val existing = presentationRepository.findById(presentationSessionId)
            ?: throw IllegalStateException("Presentation not found: $presentationSessionId")

        val response = lissiPresentationClient.getPresentationSession(existing.tenant, presentationSessionId)
        val newState = PresentationState.valueOf(response.state)

        if (newState != PresentationState.CREATED) {
            presentationRepository.save(existing.copy(state = newState))
        }

        return newState
    }
}
