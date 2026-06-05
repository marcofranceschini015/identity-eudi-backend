# identity-eudi-backend

Backend for testing identity verification with an EUDI wallet via the Lissi demo connector.

Built with **Spring Boot 3 + Kotlin + Java 21**, PostgreSQL for persistence and
Spring `WebClient` (Reactor Netty) for outbound HTTP calls.

> **Status:** demo / proof of concept. Secrets and API keys are intentionally
> committed in plain text in `application.yml` to keep the local setup simple.
> **Do not deploy this configuration to a real environment.**

## Project layout

```
.
├── build.gradle.kts
├── settings.gradle.kts
├── docker/                                # local dev stack (PostgreSQL)
│   ├── docker-compose.yml
│   └── README.md
└── src
    ├── main
    │   ├── kotlin/com/identityeudi/backend
    │   │   ├── IdentityEudiBackendApplication.kt
    │   │   ├── config/
    │   │   │   └── TenantsProperties.kt   # binds `app.tenants` from application.yml
    │   │   └── session/
    │   │       ├── api/                   # REST controller + web DTOs
    │   │       │   ├── SessionController.kt
    │   │       │   └── dto/
    │   │       │       ├── CreateSessionRequest.kt
    │   │       │       ├── CreateSessionResponse.kt
    │   │       │       ├── PollSessionRequest.kt
    │   │       │       └── PollSessionResponse.kt
    │   │       ├── client/                # outbound HTTP client (Lissi connector)
    │   │       │   ├── LissiClient.kt
    │   │       │   ├── LissiClientConfig.kt
    │   │       │   ├── LissiProperties.kt
    │   │       │   └── dto/
    │   │       │       ├── IssuanceSessionRequest.kt
    │   │       │       └── IssuanceSessionResponse.kt
    │   │       ├── domain/                # pure domain model + outbound port
    │   │       │   ├── Session.kt
    │   │       │   ├── SessionRepository.kt
    │   │       │   └── SessionState.kt
    │   │       ├── persistence/           # JPA entity + adapter implementing the port
    │   │       │   ├── SessionEntity.kt
    │   │       │   ├── SessionJpaRepository.kt
    │   │       │   └── SessionRepositoryAdapter.kt
    │   │       └── service/               # application services
    │   │           ├── IssuedSession.kt
    │   │           └── SessionService.kt
    │   └── resources/application.yml
    └── test
        ├── kotlin/com/identityeudi/backend
        │   ├── session/
        │   │   ├── api/SessionControllerTest.kt
        │   │   ├── client/LissiClientTest.kt
        │   │   ├── persistence/
        │   │   │   ├── SessionEntityTest.kt
        │   │   │   └── SessionRepositoryAdapterTest.kt
        │   │   └── service/SessionServiceTest.kt
        │   └── support/                   # shared test infrastructure
        │       ├── DatabaseTest.kt
        │       └── PostgresTestContainerBase.kt
        └── resources/
            ├── application-test.yml
            └── db/
                ├── init-session-persistence.sql
                └── clean-after-test.sql
```

The package layout follows DDD / hexagonal conventions:

| Package        | Owns                                              | Depends on              |
| -------------- | ------------------------------------------------- | ----------------------- |
| `api/`         | HTTP request/response shape, validation, status   | `service/`              |
| `service/`     | Use-case orchestration, mapping connector → domain| `client/`, `domain/`    |
| `client/`      | Wire protocol with Lissi, DTOs, timeouts          | `config/`               |
| `domain/`      | Pure model + outbound port                        | nothing                 |
| `persistence/` | JPA entity + adapter implementing the port        | `domain/`               |

## Prerequisites

- JDK 21
- Docker (for the local PostgreSQL and for the Testcontainers-based integration tests)

## Run locally

1. Start the database:

   ```bash
   docker compose -f docker/docker-compose.yml up -d
   ```

2. Start the Spring Boot application:

   ```bash
   ./gradlew bootRun
   ```

   The app will be available at <http://localhost:8080>, and the health
   endpoint at <http://localhost:8080/actuator/health>.

3. Check that the app is up:

   ```bash
   curl -i http://localhost:8080/actuator/health
   ```

   Expected response:

   ```
   HTTP/1.1 200
   Content-Type: application/vnd.spring-boot.actuator.v3+json

   {"status":"UP"}
   ```

4. Run the tests (Docker must be running — the persistence tests use a Postgres
   Testcontainer):

   ```bash
   ./gradlew test
   ```

## REST API

All endpoints are exposed under `/api/session`.

### `POST /api/session` — create an issuance session

Starts a new issuance session on the Lissi connector for the given tenant,
persists it locally in `CREATED` state, and returns the wallet redirect
information.

Request body:

```json
{
  "tenant": "check24-bank",
  "userData": {
    "first_name": "John",
    "last_name": "Doe",
    "iban": "testIban"
  }
}
```

Response (`201 Created`):

```json
{
  "sessionId": "47935416-de71-4711-91ea-c97dd1ab3d18",
  "state": "CREATED",
  "redirectUrl": "openid-credential-offer://?credential_offer_uri=...",
  "oneTimePassword": "1944"
}
```

Example:

```bash
curl -i -X POST http://localhost:8080/api/session \
  -H 'Content-Type: application/json' \
  -d '{
    "tenant": "check24-bank",
    "userData": { "first_name": "John", "last_name": "Doe", "iban": "testIban" }
  }'
```

### `GET /api/session/{sessionId}` — poll the session state

Asks the Lissi connector for the current state of an existing session.
If the connector reports a state other than the initial `CREATED`, the
persisted row is updated to match; the initial state never triggers a
database write.

Request body:

```json
{
  "tenant": "check24-bank"
}
```

Response (`200 OK`):

```json
{
  "state": "ISSUED"
}
```

Possible values for `state`: `CREATED`, `ISSUED`, `FAILED`, `REVOKED`.

Example:

```bash
curl -i -X GET http://localhost:8080/api/session/47935416-de71-4711-91ea-c97dd1ab3d18 \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"check24-bank"}'
```

> **Note on `GET` with a body.** Some HTTP clients and intermediaries strip the
> body from `GET` requests. For a demo it's fine; if this ever needs to be
> exposed publicly, move `tenant` to a query parameter or a header.

## Configuration

All runtime configuration lives in [`src/main/resources/application.yml`](src/main/resources/application.yml).

### Tenants (`app.tenants`)

Each tenant is bound to a Kotlin `data class` via `@ConfigurationProperties`
(see [`TenantsProperties`](src/main/kotlin/com/identityeudi/backend/config/TenantsProperties.kt)).
Scanning is enabled at application level via `@ConfigurationPropertiesScan`.

```yaml
app:
  tenants:
    - name: check24-bank
      apiKey: REPLACE_ME_WITH_REAL_API_KEY
      template: LoanCredential
```

Inject anywhere with constructor injection:

```kotlin
@Service
class SomeService(private val tenants: TenantsProperties) { /* ... */ }
```

### Lissi connector (`lissi.base-url-template`)

The Lissi demo connector is multi-tenant: each tenant has its own host. The
template controls how the URL is built; `{tenant}` is replaced at call time
with the tenant name.

```yaml
lissi:
  base-url-template: https://{tenant}.demo.connector.lissi.io
```

Override it (for example, to point at a local stub during development):

```yaml
lissi:
  base-url-template: http://localhost:9000
```

## Tests

The suite mixes pure unit tests with two kinds of Spring slices:

| Test class                       | Type                       | What it covers                                                 |
| -------------------------------- | -------------------------- | -------------------------------------------------------------- |
| `SessionEntityTest`              | Plain JUnit                | Domain ↔ JPA mapping, round-trip property test                 |
| `SessionRepositoryAdapterTest`   | `@DataJpaTest` + Postgres  | Real Postgres via Testcontainers, seed/teardown via `@Sql`     |
| `LissiClientTest`                | MockWebServer              | Request shape, headers, response parsing, error propagation    |
| `SessionServiceTest`             | Plain JUnit + mockk        | Use-case orchestration with mocked client + repository         |
| `SessionControllerTest`          | `@WebMvcTest` + mockk      | HTTP mapping, validation, status codes                         |

Docker (or OrbStack) must be running for the persistence tests. On OrbStack the
Gradle build auto-detects the socket at `~/.orbstack/run/docker.sock` and
exports the env vars Testcontainers needs.

## License

This project is licensed under the Apache License 2.0 — see [LICENSE](LICENSE).
