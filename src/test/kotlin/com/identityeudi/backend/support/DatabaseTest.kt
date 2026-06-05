package com.identityeudi.backend.support

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

/**
 * Meta-annotation for database integration tests.
 *
 * Loads only the slice of the Spring context required for JPA
 * (`@DataJpaTest`) and disables the in-memory DB replacement so the
 * Testcontainers Postgres is actually used.
 *
 * Activates the `test` profile so [src/test/resources/application-test.yml]
 * is picked up.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
annotation class DatabaseTest
