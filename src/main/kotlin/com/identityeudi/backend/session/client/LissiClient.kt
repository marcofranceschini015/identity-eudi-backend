package com.identityeudi.backend.session.client

import com.identityeudi.backend.config.TenantsProperties
import com.identityeudi.backend.session.client.dto.IssuanceSessionRequest
import com.identityeudi.backend.session.client.dto.IssuanceSessionResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Outbound HTTP client for the Lissi demo connector.
 *
 * The connector is multi-tenant: each tenant has its own host
 * (`{tenant}.demo.connector.lissi.io`) and its own API key. Both are read
 * from [TenantsProperties], which is bound from `application.yml`.
 *
 * The client returns the raw [IssuanceSessionResponse] DTO; mapping to the
 * domain `Session` is the responsibility of the application service so the
 * connector wire format doesn't leak past the boundary.
 */
@Component
class LissiClient(
    private val tenants: TenantsProperties,
    private val lissiProperties: LissiProperties,
    private val lissiWebClient: WebClient,
) {

    /**
     * Creates a new issuance session on the Lissi connector for the given
     * [tenantName] and returns the parsed response.
     *
     * @throws IllegalArgumentException if [tenantName] is not configured
     * @throws IllegalStateException    if the connector returns an empty body
     */
    fun createIssuanceSession(
        tenantName: String,
        subjectClaims: Map<String, String>,
    ): IssuanceSessionResponse {
        val tenant = tenants.tenants.firstOrNull { it.name == tenantName }
            ?: throw IllegalArgumentException("Unknown tenant: $tenantName")

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val request = IssuanceSessionRequest(
            flow = IssuanceSessionRequest.Flow(
                preAuthorizedCode = IssuanceSessionRequest.PreAuthorizedCode(oneTimePassword = true),
            ),
            externalUserId = UUID.randomUUID(),
            credentialTemplateName = tenant.template,
            revocable = true,
            validFrom = now,
            validUntil = now.plusDays(VALIDITY_DAYS),
            subjectClaims = subjectClaims,
        )

        val baseUrl = lissiProperties.baseUrlTemplate.replace("{tenant}", tenantName)
        return lissiWebClient
            .post()
            .uri("$baseUrl/api/v1/issuance-sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .header(API_KEY_HEADER, tenant.apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono<IssuanceSessionResponse>()
            .timeout(LissiClientConfig.TIMEOUT)
            .block()
            ?: throw IllegalStateException("Empty response from Lissi connector for tenant '$tenantName'")
    }

    companion object {
        private const val API_KEY_HEADER = "LC-Api-Key"
        private const val VALIDITY_DAYS = 10L
    }
}
