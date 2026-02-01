# otel-demo (Spring Boot 4, Java 25)

Small Spring Boot service showcasing end-to-end observability with OpenTelemetry and Micrometer:
- Structured JSON logs (Logback + Logstash encoder)
- Distributed tracing (OpenTelemetry Java agent)
- Application metrics (Micrometer OTLP)
- Central log shipping via Grafana Alloy → Grafana Cloud Loki

Service name: `otel-demo`
Package: `com.shahidyousuf.otel_demo`

## Architecture
- App logs in JSON and emits traces/metrics via OpenTelemetry and Micrometer.
- OpenTelemetry Java agent exports:
  - Traces → Grafana Cloud OTLP gateway
  - Logs → Alloy OTLP receiver (HTTP :4318)
- Grafana Alloy forwards logs to Grafana Cloud Loki using Basic auth.

### Data Flow
```
   +------------------------------+                     +------------------------------+
   | Spring Boot App              |                     | Grafana Cloud                 |
   | service.name = otel-demo     |                     | - OTLP Gateway (Traces/Metrics)|
   |                              |                     | - Loki (Logs)                 |
   | - Logback JSON               |                     +------------------------------+
   | - OpenTelemetry Java Agent   |                                   ^
   +---------------+--------------+                                   |
                   | OTLP logs (http/protobuf)                        | Loki push (HTTP)
                   | endpoint: http://localhost:4318/v1/logs          | url: $LOKI_URL
                   v                                                   |
   +---------------+--------------+   Basic auth (username/token)      |
   | Grafana Alloy                 +-----------------------------------+
   | - otelcol.receiver.otlp :4318 |   https://logs-.../loki/api/v1/push
   | - loki.write (basic_auth)     |
   +---------------+--------------+
                   ^
                   |
                   | OTLP traces + metrics (HTTP)
                   | endpoint: https://otlp-gateway-<region>.grafana.net/otlp
                   |   - Traces: /otlp
                   |   - Metrics: /otlp/v1/metrics
```

Key files
- `otel.properties` – agent exporters and settings
- `ops/alloy.river` – Alloy pipeline (OTLP logs receiver → Loki write)
- `docker-compose.yml` – local Alloy container exposing `:4318`
- `src/main/resources/logback-spring.xml` – JSON console + optional rolling file

## Requirements
- Java 25
- Gradle (wrapper included)
- Docker (for Alloy)

## Setup
1) Configure `.env` (not committed)

Example values (replace with your stack’s instance ID and token):

```
# OpenTelemetry Java agent (traces)
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp-gateway-<region>.grafana.net/otlp
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic <base64(instance_id:glc_token)>

# Micrometer (metrics)
GRAFANA_OTLP_METRICS_URL=https://otlp-gateway-<region>.grafana.net/otlp/v1/metrics
GRAFANA_OTLP_HEADERS_AUTHORIZATION=Basic <base64(instance_id:glc_token)>

# Logs via Alloy → Loki
OTEL_LOGS_EXPORTER=otlp
OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=http://localhost:4318/v1/logs
LOKI_URL=https://logs-prod-<region>.grafana.net/loki/api/v1/push
LOKI_USERNAME=<numeric instance id>
LOKI_PASSWORD=<glc_ token with logs:write>
```

Notes
- The instance ID is the numeric Stack ID from Grafana Cloud.
- The token can be a Grafana Cloud Access Policy token (glc_…). For Basic auth headers, use `base64("<instance_id>:<token>")`.

2) Start Alloy

Using Compose (recommended):
```
docker compose up -d alloy
```

Or via docker run:
```
docker run -d --name alloy -p 4318:4318 \
  -v $(pwd)/ops/alloy.river:/etc/alloy/config.alloy:ro \
  -e LOKI_URL -e LOKI_USERNAME -e LOKI_PASSWORD \
  grafana/alloy:latest run --server.http.listen-addr=0.0.0.0:12345 /etc/alloy/config.alloy
```

3) Run the app
```
./gradlew bootRun
```

4) Exercise the API
```
curl http://localhost:8080/hello
curl "http://localhost:8080/work?ms=250"
```

## Verify in Grafana Cloud
- Logs (Loki): Explore → query `{service="otel-demo"} | logfmt` and inspect `trace_id`/`span_id` fields.
- Traces: Explore → Tempo or Traces view; filter `service.name = otel-demo`.
- Metrics: Explore → Prometheus; examples below.

## Useful queries
- Logs (by endpoint): `{service="otel-demo", endpoint="/work"} | logfmt`
- RPS per endpoint: `sum by (endpoint)(rate(http_server_requests_seconds_count{service="otel-demo"}[5m]))`
- p95 latency (/work): `histogram_quantile(0.95, sum by (le)(rate(work_latency_seconds_bucket{service="otel-demo",endpoint="/work"}[5m])))`
- Requests per endpoint: `sum by (endpoint)(rate(work_requests_total{service="otel-demo"}[5m]))`

## Configuration notes
- Spring imports `.env` via ConfigData: `spring.config.import=optional:file:.env[.properties]`.
- Secrets live only in `.env`. `.gitignore` excludes `.env`, `logs/`, `.DS_Store`, and the agent `.jar`.
- `otel.properties` configures the Java agent: traces and logs exported over OTLP; app metrics are sent by Micrometer.
