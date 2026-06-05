package com.identityeudi.backend.session.client

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

/**
 * Configures the shared [WebClient] used by [LissiClient].
 *
 * The Lissi connector URL is tenant-dependent (`{tenant}.demo.connector.lissi.io`),
 * so we deliberately do **not** set a baseUrl on the WebClient itself - each call
 * passes the full URL via `.uri(...)`.
 *
 * The 5-second timeout is enforced at three levels for defense in depth:
 *   - TCP connect:    via Netty `CONNECT_TIMEOUT_MILLIS`
 *   - Read / write:   via Netty channel handlers
 *   - Reactive total: via `.timeout(...)` on the call site (in [LissiClient])
 */
@Configuration
class LissiClientConfig {

    @Bean
    fun lissiWebClient(): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT.toMillis().toInt())
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(TIMEOUT.seconds.toInt()))
                conn.addHandlerLast(WriteTimeoutHandler(TIMEOUT.seconds.toInt()))
            }

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    companion object {
        val TIMEOUT: Duration = Duration.ofSeconds(5)
    }
}
