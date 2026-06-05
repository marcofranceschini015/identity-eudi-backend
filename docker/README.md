# Local development stack

This folder contains the Docker setup used for local development.

For now it only spins up a **PostgreSQL** instance, so you can run the
Spring Boot application directly from your IDE (or `./gradlew bootRun`)
against `localhost:5432`.

## Credentials (demo only — do NOT use in production)

| Field    | Value |
| -------- | ----- |
| Host     | `localhost` |
| Port     | `5432` |
| Database | `eudi` |
| User     | `eudi` |
| Password | `eudi` |

## Commands

Start the database in the background:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Stop it:

```bash
docker compose -f docker/docker-compose.yml down
```

Wipe data (delete the named volume):

```bash
docker compose -f docker/docker-compose.yml down -v
```
