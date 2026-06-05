package com.identityeudi.backend.session.persistence

import com.identityeudi.backend.session.domain.Session
import com.identityeudi.backend.session.domain.SessionState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/** Pure unit tests for [SessionEntity] domain <-> entity mapping. */
class SessionEntityTest {

    private val anyId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val anyCredentialId: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val anyCreatedAt: OffsetDateTime =
        OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    private val anyExpiresAt: OffsetDateTime =
        OffsetDateTime.of(2026, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun `fromDomain copies every field and serialises the state enum to its name`() {
        val session = Session(
            id = anyId,
            state = SessionState.CREATED,
            credentialId = anyCredentialId,
            createdAt = anyCreatedAt,
            expiresAt = anyExpiresAt,
            tenant = "check24-bank",
        )

        val entity = SessionEntity.fromDomain(session)

        assertThat(entity.id).isEqualTo(session.id)
        assertThat(entity.state).isEqualTo("CREATED")
        assertThat(entity.credentialId).isEqualTo(session.credentialId)
        assertThat(entity.createdAt).isEqualTo(session.createdAt)
        assertThat(entity.expiresAt).isEqualTo(session.expiresAt)
        assertThat(entity.tenant).isEqualTo(session.tenant)
    }

    @Test
    fun `toDomain copies every field and deserialises the state enum`() {
        val entity = SessionEntity(
            id = anyId,
            state = "ISSUED",
            credentialId = anyCredentialId,
            createdAt = anyCreatedAt,
            expiresAt = anyExpiresAt,
            tenant = "check24-bank",
        )

        val session = entity.toDomain()

        assertThat(session.id).isEqualTo(entity.id)
        assertThat(session.state).isEqualTo(SessionState.ISSUED)
        assertThat(session.credentialId).isEqualTo(entity.credentialId)
        assertThat(session.createdAt).isEqualTo(entity.createdAt)
        assertThat(session.expiresAt).isEqualTo(entity.expiresAt)
        assertThat(session.tenant).isEqualTo(entity.tenant)
    }

    @ParameterizedTest
    @EnumSource(SessionState::class)
    fun `fromDomain then toDomain is the identity for every SessionState`(state: SessionState) {
        val original = Session(
            id = anyId,
            state = state,
            credentialId = anyCredentialId,
            createdAt = anyCreatedAt,
            expiresAt = anyExpiresAt,
            tenant = "check24-bank",
        )

        val roundTripped = SessionEntity.fromDomain(original).toDomain()

        assertThat(roundTripped).usingRecursiveComparison().isEqualTo(original)
    }

    @Test
    fun `toDomain throws IllegalArgumentException for an unknown state string`() {
        val entity = SessionEntity(
            id = anyId,
            state = "NOT_A_REAL_STATE",
            credentialId = anyCredentialId,
            createdAt = anyCreatedAt,
            expiresAt = anyExpiresAt,
            tenant = "check24-bank",
        )

        assertThat(runCatching { entity.toDomain() }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
