# otel-demo (Spring Boot 4, Java 25)

Small Spring Boot service to demonstrate modern observability with OpenTelemetry and Micrometer:
- Structured JSON logging (Logback + Logstash encoder)
- Distributed tracing (OpenTelemetry Java agent)
- Metrics (Micrometer) exported over OTLP to Grafana Cloud

Service name: `otel-demo`
Package: `com.shahidyousuf.otel_demo`

## Endpoints
- `GET /hello` – simple response, structured log, increments counter and timer
- `GET /work?ms=250` – simulates latency, increments counter and timer

## Requirements
- Java 25
- Gradle (wrapper included)

## Run locally
1) Create a `.env` file in the project root (not committed). Basic auth is used for both the Java agent (traces) and Micrometer (metrics).

Example `.env` (replace the instance ID and token with your own):

```
# OpenTelemetry Java agent (traces)
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp-gateway-<region>.grafana.net/otlp
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic <base64(instance_id:glc_token)>

# Micrometer (metrics) – use full metrics path
GRAFANA_OTLP_METRICS_URL=https://otlp-gateway-<region>.grafana.net/otlp/v1/metrics
GRAFANA_OTLP_HEADERS_AUTHORIZATION=Basic <base64(instance_id:glc_token)>

# Optional fallbacks (only if you prefer to provide token directly)
# GRAFANA_OTLP_TOKEN=glc_...
```

Tips
- The “instance_id” is the numeric ID for your stack (Grafana Cloud → Stack → Details or Prometheus remote_write instructions). It is not the UUID nor the stack handle.
- The token can be an Access Policy token (glc_…). For Basic, build `base64("<instance_id>:<token>")`.

2) Start the app

```
./gradlew bootRun
```

The build wires the OpenTelemetry Java agent automatically using `otel.properties`. Auth headers for the agent are taken from `OTEL_EXPORTER_OTLP_HEADERS` in `.env` (do not commit secrets).

3) Exercise the API

```
curl http://localhost:8080/hello
curl "http://localhost:8080/work?ms=250"
```

## Observability

Traces
- The Java agent exports traces to Grafana Cloud via OTLP HTTP.
- Configure the OTLP endpoint and auth through `.env` as shown above.

Metrics
- Micrometer OTLP exporter is enabled in `src/main/resources/application.properties` and reads endpoint + auth from `.env`.
- Default export step: 10s. First samples can take ~30–120s to appear.
- Useful meters (Prometheus style):
  - HTTP server: `http_server_requests_seconds_bucket|count|sum`
  - Custom counters: `hello_requests_total`, `work_requests_total`
  - Custom timers: `hello_latency_seconds_*`, `work_latency_seconds_*`

Grafana queries (examples)
- RPS per endpoint: `sum by (endpoint)(rate(http_server_requests_seconds_count{service="otel-demo"}[5m]))`
- p95 latency (/work): `histogram_quantile(0.95, sum by (le)(rate(work_latency_seconds_bucket{service="otel-demo",endpoint="/work"}[5m])))`
- Requests per endpoint: `sum by (endpoint)(rate(work_requests_total{service="otel-demo"}[5m]))`

Span metrics vs app metrics
- Grafana may show `traces_spanmetrics_*` (derived from traces). These are separate from app metrics. Use label filters like `service="otel-demo"` and `endpoint="/work"` to focus on Micrometer meters.

## Logs via Alloy (recommended)

Use Grafana Alloy as a single OTLP receiver to accept logs from the OpenTelemetry Java agent and forward them to Grafana Cloud Loki.

1) Configure credentials in `.env`

```
# Loki credentials (Grafana Cloud)
LOKI_URL=https://logs-prod-<region>.grafana.net/loki/api/v1/push
LOKI_USERNAME=<numeric instance id>
LOKI_PASSWORD=<glc_ token with logs:write>

# Enable OTLP logs for the Java agent and point to Alloy locally
OTEL_LOGS_EXPORTER=otlp
OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=http://localhost:4318/v1/logs
```

2) Start Alloy locally

```
docker run -d --name alloy -p 4318:4318 \
  -v $(pwd)/ops/alloy.river:/etc/alloy/config.alloy:ro \
  -e LOKI_URL -e LOKI_USERNAME -e LOKI_PASSWORD \
  grafana/alloy:latest run --server.http.listen-addr=0.0.0.0:12345 /etc/alloy/config.alloy
```

3) Start the app

```
./gradlew bootRun
```

The build injects `.env` into the BootRun environment so the OTel agent sees `OTEL_*` vars and exports logs to Alloy (which forwards them to Loki).

4) Verify in Grafana
- Explore → Logs → filter `service="otel-demo"`.
- Logs include `trace_id` and `span_id` for click-through trace correlation.

Notes
- `otel.properties` keeps agent metrics disabled and traces enabled; logs are enabled via `OTEL_LOGS_EXPORTER=otlp` in `.env`.
- Ensure your token has logs:write and your region/host matches your stack.

## Configuration notes
- `application.properties` imports `.env` via Spring ConfigData: `spring.config.import=optional:file:.env[.properties]`.
- Keep secrets (tokens/headers) only in `.env`.
- The app uses Basic auth for metrics by default via `GRAFANA_OTLP_HEADERS_AUTHORIZATION`.
- `otel.properties` avoids hardcoding headers; env var `OTEL_EXPORTER_OTLP_HEADERS` provides the Basic header for the Java agent.

## Troubleshooting
- 401 from OTLP metrics: confirm Basic header uses numeric instance ID and correct token; verify region host matches your stack.
- 404 from OTLP metrics: ensure URL includes `/otlp/v1/metrics` for Micrometer.
- No app metrics in Grafana: make requests to `/hello` and `/work?ms=250`; wait at least one export step.
- Still stuck? Set `logging.level.io.micrometer.registry.otlp=DEBUG` temporarily to inspect publish attempts.
