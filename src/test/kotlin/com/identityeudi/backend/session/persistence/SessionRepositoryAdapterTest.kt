package com.identityeudi.backend.session.persistence

import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionState
import com.identityeudi.backend.support.DatabaseTest
import com.identityeudi.backend.support.PostgresTestContainerBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/** Database integration test for [SessionRepositoryAdapter]. */
@DatabaseTest
@Import(SessionRepositoryAdapter::class)
@Sql(value = ["/db/init-session-persistence.sql"], executionPhase = BEFORE_TEST_METHOD)
@Sql(value = ["/db/clean-after-test.sql"], executionPhase = AFTER_TEST_METHOD)
class SessionRepositoryAdapterTest : PostgresTestContainerBase() {

    @Autowired
    private lateinit var adapter: SessionRepositoryAdapter

    @Test
    fun `save persists a new session and returns the equivalent domain object`() {
        val expected = Session(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            state = SessionState.ISSUED,
            credentialId = UUID.fromString("44444444-4444-4444-4444-444444444444"),
            createdAt = OffsetDateTime.of(2026, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC),
            expiresAt = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC),
            tenant = "check24-bank",
        )

        val actual = adapter.save(expected)

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        assertThat(adapter.findById(expected.id))
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    fun `save updates an existing session`() {
        val seededId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val seeded = adapter.findById(seededId)
        assertThat(seeded).isNotNull
        assertThat(seeded!!.state).isEqualTo(SessionState.CREATED)

        val updated = seeded.copy(
            state = SessionState.FAILED,
            expiresAt = seeded.expiresAt.plusHours(1),
        )

        val result = adapter.save(updated)

        assertThat(result).usingRecursiveComparison().isEqualTo(updated)
        assertThat(adapter.findById(seededId))
            .usingRecursiveComparison()
            .isEqualTo(updated)
    }

    @Test
    fun `findById returns the seeded session`() {
        val id = UUID.fromString("11111111-1111-1111-1111-111111111111")

        val found = adapter.findById(id)

        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(id)
        assertThat(found.state).isEqualTo(SessionState.CREATED)
        assertThat(found.credentialId)
            .isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"))
        assertThat(found.tenant).isEqualTo("check24-bank")
    }

    @Test
    fun `findById returns null when the session does not exist`() {
        val unknown = UUID.fromString("99999999-9999-9999-9999-999999999999")

        val found = adapter.findById(unknown)

        assertThat(found).isNull()
    }
}
