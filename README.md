# lab-02-cpo
## Running on docker reference

Starts the backend with the H2 web console enabled so you can inspect persisted log entries at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:demo`).

```bash
docker run -p 8080:8080 \
   -e SPRING_H2_CONSOLE_SETTINGS_WEB_ALLOW_OTHERS=true \
    ghcr.io/polito-cpo-2026/lab-02-backend:0.0.1                                                                                                                    
```

# API Reference

## Overview

A Spring Boot service that exposes a single REST endpoint (`GET /log`) acting as a runtime
snapshot. Every call is persisted to a database and emits structured log lines enriched with
request-scoped identifiers. The Pod name and application version are injected via environment
variables, making the service deployable on Kubernetes without code changes.

---

## Architecture

```
HTTP client
    │
    ▼
TraceFilter          ← resolves/generates X-Request-Id & X-Correlation-Id,
    │                            logs every incoming request, sets MDC
    ▼
LogController (GET /log)      ← thin adapter: reads IDs from request attributes,
    │                            delegates to service
    ▼
LogService / LogServiceImpl   ← business logic: builds snapshot, persists LogEntry,
    │                            returns LogResponse
    ▼
LogRepository (JPA)           ← Spring Data repo backed by H2 (dev) or PostgreSQL (prod)

PodIdentityScheduler          ← emits "Pod identity" log line on a fixed interval (default 30 s)
```

---

## Endpoint

### `GET /log`

Returns a runtime snapshot of the current instance.

#### Request headers (all optional)

| Header | Description |
|---|---|
| `X-Request-Id` | Unique identifier for this HTTP request. Generated if absent. |
| `X-Correlation-Id` | Identifier shared across a group of related requests (e.g. a test sequence or short client session). Generated if absent. |

If either header is present but blank it is treated as absent and a new value is generated.

#### Response `200 OK`

```json
{
  "appVersion":    "0.1.0",
  "podName":       "demo-6b9d8f-xk2p",
  "timestamp":     "2026-04-07T10:00:00Z",
  "requestId":     "b3c4d5e6-fa12-4321-8abc-000000000001",
  "correlationId": "a1b2c3d4-0000-4000-8000-000000000099"
}
```

| Field | Type | Description |
|---|---|---|
| `appVersion` | string | Value of `APP_VERSION` env var |
| `podName` | string | Value of `POD_NAME` env var |
| `timestamp` | ISO-8601 instant | Server time at snapshot creation |
| `requestId` | UUID string | Unique per HTTP request |
| `correlationId` | UUID string | Shared across related requests |

#### Side effects

Every successful call inserts one row into the `log_entries` table.

---

## Request / Correlation ID convention

```
Client session / test sequence
│
├── Request 1 ──► X-Request-Id: <uuid-A>   X-Correlation-Id: <uuid-session>
├── Request 2 ──► X-Request-Id: <uuid-B>   X-Correlation-Id: <uuid-session>
└── Request 3 ──► X-Request-Id: <uuid-C>   X-Correlation-Id: <uuid-session>
```

- **Request ID** - changes on every call; identifies one HTTP round-trip in logs.
- **Correlation ID** - stays constant for a group (e.g. one automated test run, one user
  session). Lets you filter logs for all requests belonging to the same workflow.

When clients do not send the headers the backend auto-generates both, so each request gets
independent random values. Clients that want to correlate requests across calls must send the
same `X-Correlation-Id` themselves.

---

## Environment variables

| Variable | Default (dev) | Description |
|---|---|---|
| `APP_VERSION` | `0.1.0` | Application version included in every snapshot |
| `POD_NAME` | `local` | Kubernetes Pod name (or any host identifier) |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1` | JDBC URL for the log store |
| `POD_LOG_INTERVAL_MS` | `30000` | Interval (ms) between periodic Pod identity log lines |

Kubernetes deployment excerpt:

```yaml
env:
  - name: APP_VERSION
    value: "0.1.0"
  - name: POD_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
  - name: SPRING_DATASOURCE_URL
    value: "jdbc:h2:mem:demo"
```

---

## Database

### Table: `log_entries`

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT (PK, auto) | Surrogate key |
| `app_version` | VARCHAR | Value of `APP_VERSION` at snapshot time |
| `pod_name` | VARCHAR | Value of `POD_NAME` at snapshot time |
| `timestamp` | TIMESTAMP | Server UTC instant |
| `request_id` | VARCHAR | Request-scoped identifier |
| `correlation_id` | VARCHAR | Correlation identifier |

Schema is managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update` in dev).
Switch to `validate` or use Flyway/Liquibase for production.

---

## Logging

All log lines include `requestId` and `correlationId` in the MDC, making them available to
any appender pattern that references `%X{requestId}` / `%X{correlationId}`.

| Event | Logger | Level | Key fields |
|---|---|---|---|
| Every incoming HTTP request | `TraceFilter` | INFO | method, URI, requestId, correlationId |
| Snapshot created | `LogServiceImpl` | INFO | version, pod, requestId, correlationId |
| Periodic Pod identity | `PodIdentityScheduler` | INFO | pod name, version |

---

## Running locally

```bash
./gradlew bootRun
```

## Running with Postgresql DB (docker compose)

```bash
docker-compose up -d
```

```bash
# Basic call - server generates both IDs
curl http://localhost:8080/log

# Provide a correlation ID to link multiple calls
CORR="$(uuidgen)"
curl -H "X-Correlation-Id: $CORR" http://localhost:8080/log
curl -H "X-Correlation-Id: $CORR" http://localhost:8080/log

# H2 console (dev only)
open http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:demo
```

---

## Module structure

```
src/main/kotlin/it/tonicminds/lab02cpo/
├── Lab02CpoApplication.kt      Spring Boot entry point
├── AppConfig.kt                @ConfigurationProperties(prefix = "app")
├── LogEntry.kt                 JPA entity → log_entries table
├── LogRepository.kt            Spring Data JPA repository
├── TraceFilter.kt     Servlet filter - ID resolution + MDC + request logging
├── LogService.kt               Service interface
├── LogServiceImpl.kt           Service implementation
├── LogController.kt            REST controller + LogResponse DTO
└── PodIdentityScheduler.kt     @Scheduled periodic logger

src/test/kotlin/it/tonicminds/lab02cpo/
├── Lab02CpoApplicationTests.kt Context smoke test
├── LogServiceTest.kt           Unit tests - LogServiceImpl (Mockito, no Spring context)
├── LogControllerTest.kt        Web-layer slice tests - @WebMvcTest + @MockitoBean
└── TraceFilterTest.kt Unit tests - filter ID resolution logic
```
