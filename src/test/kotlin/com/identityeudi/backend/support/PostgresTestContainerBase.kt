package com.identityeudi.backend.support

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for tests that need a real PostgreSQL instance.
 *
 * A single, *static* container is started once per JVM and shared across
 * every test class extending this base — that keeps the suite fast (the
 * 1-3s container startup cost is paid only once) while still giving each
 * test the isolation of a real database.
 *
 * Spring Boot wires the container into the application's `DataSource`
 * automatically thanks to `@ServiceConnection` (Spring Boot 3.1+),
 * so no `@DynamicPropertySource` boilerplate is needed.
 */
@Testcontainers
abstract class PostgresTestContainerBase {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("eudi-test")
            .withUsername("eudi")
            .withPassword("eudi")
            .withReuse(true)
    }
}
