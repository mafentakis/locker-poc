# Progress Log

## Milestone Tracker

| # | Milestone | Status | Commit | Date |
|---|-----------|--------|--------|------|
| 0 | SPEC + README finalized | ✅ Done | d23f156 | 2026-04-24 |
| 1 | `scripts/gen-certs.sh` + cert-init Dockerfile | ✅ Done | 93c2b12 | 2026-04-24 |
| 2 | Mosquitto container + config + healthcheck | ✅ Done | 93c2b12 | 2026-04-24 |
| 3 | `common` module (DTOs, TlsContextFactory, HealthFile, logging) | ✅ Done | f6b219f | 2026-04-24 |
| 4 | `mqtt-subscriber` | ✅ Done | 4ac2351 | 2026-04-24 |
| 5 | `mqtt-publisher` | ✅ Done | 4ac2351 | 2026-04-24 |
| 6 | `rest-server` (HTTPS + /health + mTLS) | ✅ Done | 2c05b68 | 2026-04-24 |
| 7 | `rest-server` MQTT publish wired into handler | ✅ Done | 2c05b68 | 2026-04-24 |
| 8 | `rest-client` | ✅ Done | 0ef384c | 2026-04-24 |
| 9 | `docker-compose.yml` final wiring | ✅ Done | f3aa5c8 | 2026-04-24 |
| 10 | `integration-test` module | ✅ Done | 96160ab | 2026-04-24 |
| 11 | README quick-start verification + progress log update | ✅ Done | 74fa01c | 2026-04-24 |
| R1 | Refactor: standalone POMs + independent Dockerfiles + Dockerized IT | ✅ Done | — | 2026-04-24 |

---

## Log

### 2026-04-24 — Milestone 0: SPEC + README
- Created `SPEC.md` (13 sections, full technical specification)
- Created `README.md` (layout, Mermaid diagrams, stack table)
- Created `.gitignore`
- Created `progress-log.md`

### 2026-04-24 — Milestones 1-2: Infrastructure
- `scripts/gen-certs.sh`: generates CA + 5 leaf certs + PKCS#12 keystores + truststore
- `cert-init/Dockerfile`: alpine + openssl + keytool
- `mosquitto/mosquitto.conf`: TLS 8883, mTLS, `use_identity_as_username`

### 2026-04-24 — Milestone 3: Common module
- Parent aggregator `pom.xml` (Java 21, dependency management)
- `common/`: DTOs (EventHeaders, CompartmentOpenedEventMsg, EventEnvelope, ErrorResponseTo)
- `TlsContextFactory`: builds SSLContext from PKCS#12, TLSv1.3, also trust-only variant
- `HealthFile`: writes sentinel files for Docker healthchecks
- `JsonMapper`: pre-configured ObjectMapper singleton
- `logging.properties`: timestamped format to STDOUT

### 2026-04-24 — Milestones 4-5: MQTT modules
- `mqtt-subscriber`: connects mTLS, subscribes to `compartment/opened`, logs + deserializes, writes `.ready` sentinel
- `mqtt-publisher`: standalone one-shot, publishes test event, writes `.done` sentinel
- Multi-stage Dockerfiles for both

### 2026-04-24 — Milestones 6-7: REST server
- `RestServerMain`: JDK HttpsServer on 8443, mTLS (`needClientAuth=true`)
- `GET /health` → `{"status":"UP"}` (mTLS-protected)
- `OpenCompartmentHandler`: validates path params + headers, logs, returns 200, emits MQTT event
- `MqttEventEmitter`: Paho MQTTv5 publisher for `CompartmentOpenedEventMsg`
- Dockerfile with `curl` installed for healthcheck

### 2026-04-24 — Milestone 8: REST client
- `RestClientMain`: JDK HttpClient, sends openCompartment POST with all required headers, writes `.done`

### 2026-04-24 — Milestone 9: Docker Compose
- Full `docker-compose.yml` with 6 services + `locker-net` bridge network
- Every container has a healthcheck per SPEC §6.5
- `depends_on` with `service_healthy` / `service_completed_successfully` conditions

### 2026-04-24 — Milestone 10: Integration test
- `integration-test` module with JUnit 5 + Maven Failsafe
- `DockerComposeStack`: JUnit extension for stack lifecycle (up/down/wait)
- `CertLoader`: loads client certs from `./certs`
- `MqttTestClient`: simple subscriber for test assertions
- 7 test cases: health, happy path + event, invalid compartment, missing header, no-client-cert HTTPS, no-client-cert MQTT, standalone publisher verification

### 2026-04-24 — Refactor R1: Independent Docker builds + Dockerized integration test
- **All module POMs made standalone** — removed `<parent>`, inlined versions/properties/plugins. Root `pom.xml` is now an optional aggregator for IDE use only.
- **Each Dockerfile copies only `common/` + its own module** — `mvn -f common/pom.xml install` then `mvn -f <module>/pom.xml package`. No cross-module knowledge.
- **Integration test now runs in Docker** — new `integration-test/Dockerfile`, added as compose service with `depends_on` on healthy `rest-server`, `mosquitto`, `mqtt-subscriber`.
- **IT classes moved from `src/test/java` to `src/main/java`** — shaded into runnable JAR with `TestRunnerMain` that launches JUnit programmatically.
- **Removed `DockerComposeStack.java`** — no longer needed; compose is the orchestrator.
- **`MqttTestClient` takes `brokerUri` parameter** — connects via Docker DNS, not localhost.
- **`CertLoader` uses `CERTS_DIR` env var** — defaults to `/certs` (Docker mount).
- **`EndToEndIT` uses `REST_SERVER_URL` and `MQTT_BROKER_URI` env vars** — defaults to Docker service names.
- Run everything: `docker compose up --build`
- View test results: `docker compose logs integration-test`
