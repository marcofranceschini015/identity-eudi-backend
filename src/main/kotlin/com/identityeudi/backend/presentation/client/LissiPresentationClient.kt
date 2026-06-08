package com.identityeudi.backend.presentation.client

import com.identityeudi.backend.config.TenantsProperties
import com.identityeudi.backend.presentation.client.dto.PresentationSessionRequest
import com.identityeudi.backend.presentation.client.dto.PresentationSessionResponse
import com.identityeudi.backend.session.client.LissiClientConfig
import com.identityeudi.backend.session.client.LissiProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

/**
 * Outbound HTTP client for the Lissi demo connector presentation API.
 *
 * Reuses the same per-tenant base URL, API key and shared [WebClient] as the
 * issuance flow; only the path and payload differ.
 */
@Component
class LissiPresentationClient(
    private val tenants: TenantsProperties,
    private val lissiProperties: LissiProperties,
    private val lissiWebClient: WebClient,
) {

    /**
     * Creates a new presentation session on the Lissi connector for the given
     * [tenantName] and returns the parsed response.
     *
     * @throws IllegalArgumentException if [tenantName] is not configured
     * @throws IllegalStateException    if the connector returns an empty body
     */
    fun createPresentationSession(tenantName: String): PresentationSessionResponse {
        val tenant = resolveTenant(tenantName)

        val request = PresentationSessionRequest(
            presentationTemplateName = tenant.presentation,
            externalUserId = UUID.randomUUID(),
        )

        val baseUrl = lissiProperties.baseUrlTemplate.replace("{tenant}", tenantName)
        return lissiWebClient
            .post()
            .uri("$baseUrl/api/v1/presentation-sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .header(API_KEY_HEADER, tenant.apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono<PresentationSessionResponse>()
            .timeout(LissiClientConfig.TIMEOUT)
            .block()
            ?: throw IllegalStateException("Empty response from Lissi connector for tenant '$tenantName'")
    }

    /**
     * Retrieves the current state of a presentation session previously created
     * via [createPresentationSession]. Used to poll for progress.
     *
     * @throws IllegalArgumentException if [tenantName] is not configured
     * @throws IllegalStateException    if the connector returns an empty body
     */
    fun getPresentationSession(
        tenantName: String,
        presentationSessionId: UUID,
    ): PresentationSessionResponse {
        val tenant = resolveTenant(tenantName)
        val baseUrl = lissiProperties.baseUrlTemplate.replace("{tenant}", tenantName)
        return lissiWebClient
            .get()
            .uri("$baseUrl/api/v1/presentation-sessions/$presentationSessionId")
            .header(API_KEY_HEADER, tenant.apiKey)
            .retrieve()
            .bodyToMono<PresentationSessionResponse>()
            .timeout(LissiClientConfig.TIMEOUT)
            .block()
            ?: throw IllegalStateException("Empty response from Lissi connector for tenant '$tenantName'")
    }

    private fun resolveTenant(tenantName: String): TenantsProperties.Tenant =
        tenants.tenants.firstOrNull { it.name == tenantName }
            ?: throw IllegalArgumentException("Unknown tenant: $tenantName")

    companion object {
        private const val API_KEY_HEADER = "LC-Api-Key"
    }
}
