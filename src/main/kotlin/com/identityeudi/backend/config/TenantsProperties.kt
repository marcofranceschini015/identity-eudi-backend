package com.identityeudi.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Strongly-typed binding for the `app.tenants` configuration section in `application.yml`.
 *
 * Each [Tenant] represents a third-party issuer/relying-party that the backend integrates with,
 * together with the credential template that should be used when interacting with it.
 */
@ConfigurationProperties(prefix = "app")
data class TenantsProperties(
    val tenants: List<Tenant> = emptyList(),
) {
    data class Tenant(
        val name: String,
        val apiKey: String,
        val template: String,
        val presentation: String
    )
}
