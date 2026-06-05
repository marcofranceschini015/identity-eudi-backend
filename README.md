# identity-eudi-backend

Backend for testing application for identity verification using EUDI wallet with Lissi demo connector.

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
├── docker/                     # local dev stack (PostgreSQL)
│   ├── docker-compose.yml
│   └── README.md
└── src
    ├── main
    │   ├── kotlin/com/identityeudi/backend
    │   │   ├── IdentityEudiBackendApplication.kt
    │   │   └── config/TenantsProperties.kt
    │   └── resources/application.yml
    └── test
        ├── kotlin/com/identityeudi/backend
        │   └── IdentityEudiBackendApplicationTests.kt
        └── resources/application-test.yml
```

## Prerequisites

- JDK 21
- Docker (for the local PostgreSQL)

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

4. Run the tests:

   ```bash
   ./gradlew test
   ```

## Configuration

All runtime configuration lives in [`src/main/resources/application.yml`](src/main/resources/application.yml).

The tenants list is bound to a Kotlin `data class` via
`@ConfigurationProperties` (see
[`TenantsProperties`](src/main/kotlin/com/identityeudi/backend/config/TenantsProperties.kt)).
Scanning is enabled at application level via `@ConfigurationPropertiesScan`.

Inject it anywhere with constructor injection, e.g.:

```kotlin
@Service
class SomeService(private val tenants: TenantsProperties) { /* ... */ }
```

## License

This project is licensed under the Apache License 2.0 — see [LICENSE](LICENSE).
