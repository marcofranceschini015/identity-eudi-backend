package com.identityeudi.backend.session.client

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for the Lissi demo connector client.
 *
 * [baseUrlTemplate] must contain the `{tenant}` placeholder, which
 * [LissiClient] substitutes with the tenant name at call time.
 */
@ConfigurationProperties(prefix = "lissi")
data class LissiProperties(
    val baseUrlTemplate: String = "https://{tenant}.demo.connector.lissi.io",
)
