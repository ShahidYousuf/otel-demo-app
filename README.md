# OpenTelemetry Demo Application

A comprehensive Spring Boot 4 (Java 25) demo showcasing **end-to-end observability** with OpenTelemetry, Micrometer, and Grafana Cloud. This repository demonstrates best practices for implementing the three pillars of observability: **logs, traces, and metrics**.

## 🎯 Features

This demo includes:

- ✅ **Structured JSON Logging** - Logback with Logstash encoder for structured logs
- ✅ **Distributed Tracing** - OpenTelemetry Java agent with automatic instrumentation
- ✅ **Custom Spans** - Manual span creation for business operations
- ✅ **Application Metrics** - Micrometer with OTLP export (counters, timers, histograms)
- ✅ **Request/Response Logging** - Automatic HTTP request/response logging with trace context
- ✅ **Request ID Propagation** - Cross-service correlation via OpenTelemetry Baggage
- ✅ **Error Tracking** - Automatic error metrics via AOP
- ✅ **AOP-based Metrics** - Clean metrics collection without try-finally patterns
- ✅ **Log Correlation** - Trace IDs and span IDs automatically included in logs

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                          │
│                         (otel-demo service)                             │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Controllers (HelloController, WorkController)                  │   │
│  │  - @TrackMetrics annotation for AOP metrics                     │   │
│  │  - Custom business logic                                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                           │                                              │
│                           ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  ObservabilityFilter                                           │   │
│  │  - Request/Response logging                                    │   │
│  │  - Request ID generation/propagation                           │   │
│  │  - OpenTelemetry Baggage injection                             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                           │                                              │
│                           ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  BusinessService                                               │   │
│  │  - Custom spans (business.work)                                │   │
│  │  - Baggage extraction (request.id)                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │
│  │  Logback JSON    │  │  OpenTelemetry   │  │  Micrometer      │      │
│  │  (Logstash)      │  │  Java Agent      │  │  OTLP Registry   │      │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘      │
└─────────────────────────────────────────────────────────────────────────┘
         │                      │                      │
         │                      │                      │
         │                      │                      │
         ▼                      ▼                      ▼
┌─────────────────┐  ┌──────────────────────────────────────────────┐
│  Grafana Alloy  │  │         Grafana Cloud                        │
│  (Local)        │  │                                              │
│                 │  │  ┌──────────────────────────────────────┐   │
│  OTLP Receiver  │  │  │  OTLP Gateway                        │   │
│  Port :4318     │  │  │  - Traces (/otlp)                    │   │
│                 │  │  │  - Metrics (/otlp/v1/metrics)       │   │
│  ┌───────────┐  │  │  └──────────────────────────────────────┘   │
│  │ Loki Write│  │  │                                              │
│  │ (Basic    │  │  │  ┌──────────────────────────────────────┐   │
│  │  Auth)    │  │  │  │  Loki                                  │   │
│  └───────────┘  │  │  │  - Logs (via Alloy)                  │   │
│       │          │  │  └──────────────────────────────────────┘   │
│       │          │  │                                              │
│       └──────────┼──┼──────────────────────────────────────────────┘
│                  │  │
│                  │  │  ┌──────────────────────────────────────┐   │
│                  │  │  │  Tempo                              │   │
│                  │  │  │  - Distributed traces                │   │
│                  │  │  └──────────────────────────────────────┘   │
│                  │  │                                              │
│                  │  │  ┌──────────────────────────────────────┐   │
│                  │  │  │  Prometheus                          │   │
│                  │  │  │  - Application metrics               │   │
│                  │  │  └──────────────────────────────────────┘   │
│                  │  └──────────────────────────────────────────────┘
│                  │
└──────────────────┘
```

### Data Flow

1. **Logs**: Application → Logback (JSON) → OpenTelemetry Agent → Alloy (OTLP receiver) → Loki
2. **Traces**: Application → OpenTelemetry Agent → Grafana Cloud OTLP Gateway → Tempo
3. **Metrics**: Application → Micrometer OTLP Registry → Grafana Cloud OTLP Gateway → Prometheus

## 🏗️ Project Structure

```
otel-demo/
├── src/main/java/com/shahidyousuf/otel_demo/
│   ├── annotation/
│   │   └── TrackMetrics.java          # AOP annotation for metrics
│   ├── aspect/
│   │   └── MetricsAspect.java          # AOP aspect for metrics/errors
│   ├── filter/
│   │   └── ObservabilityFilter.java    # Request/response logging & baggage
│   ├── service/
│   │   └── BusinessService.java        # Custom spans example
│   ├── dto/                            # Data transfer objects
│   ├── HelloController.java            # Simple endpoint
│   ├── WorkController.java             # Work endpoint with custom spans
│   └── OtlpRegistryConfig.java         # Micrometer OTLP configuration
├── src/main/resources/
│   ├── application.properties          # Spring Boot config
│   └── logback-spring.xml              # Logback JSON configuration
├── ops/
│   └── alloy.river                     # Grafana Alloy configuration
├── docker-compose.yml                   # Alloy container setup
├── otel.properties                      # OpenTelemetry agent config
└── build.gradle                         # Gradle dependencies
```

## 📋 Requirements

- **Java 25** (or compatible JDK)
- **Gradle** (wrapper included)
- **Docker** (for Grafana Alloy)
- **Grafana Cloud account** (for traces, metrics, and logs)

## 🚀 Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd otel-demo
```

### 2. Configure Environment Variables

Create a `.env` file in the project root (not committed to git):

```bash
# OpenTelemetry Java Agent - Traces
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp-gateway-<region>.grafana.net/otlp
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic <base64(instance_id:glc_token)>

# Micrometer - Metrics
GRAFANA_OTLP_METRICS_URL=https://otlp-gateway-<region>.grafana.net/otlp/v1/metrics
GRAFANA_OTLP_HEADERS_AUTHORIZATION=Basic <base64(instance_id:glc_token)>

# Logs via Alloy → Loki
OTEL_LOGS_EXPORTER=otlp
OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=http://localhost:4318/v1/logs
LOKI_URL=https://logs-prod-<region>.grafana.net/loki/api/v1/push
LOKI_USERNAME=<numeric_instance_id>
LOKI_PASSWORD=<glc_token_with_logs_write>
```

**Notes:**
- Replace `<region>` with your Grafana Cloud region (e.g., `us-east-1`, `eu-west-1`)
- The instance ID is your numeric Stack ID from Grafana Cloud
- Generate a Grafana Cloud Access Policy token with appropriate scopes
- For Basic auth, use: `echo -n "instance_id:token" | base64`

### 3. Start Grafana Alloy

Using Docker Compose (recommended):

```bash
docker compose up -d alloy
```

Verify Alloy is running:

```bash
curl http://localhost:12345/-/healthy
```

### 4. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 5. Test the Endpoints

```bash
# Simple hello endpoint
curl http://localhost:8080/hello

# Work endpoint with custom delay
curl "http://localhost:8080/work?ms=250"

# Work endpoint with custom request ID
curl "http://localhost:8080/work?ms=500" -H "X-Request-Id: my-custom-id"

# Test error handling (invalid parameter)
curl "http://localhost:8080/work?ms=70000"
```

## 🔍 Observability Features Explained

### 1. Request/Response Logging

The `ObservabilityFilter` automatically logs all HTTP requests and responses with:
- HTTP method and URI
- Query parameters
- Response status code
- Request duration
- Request ID (from header or auto-generated)
- Trace ID and Span ID (via MDC)

**Example log entry:**
```json
{
  "@timestamp": "2026-02-01T11:50:21.876Z",
  "message": "Outgoing response",
  "method": "GET",
  "uri": "/work",
  "status": 200,
  "duration_ms": 508,
  "request_id": "test-123",
  "trace_id": "9fb37b2b7a684536c2da0dc65181c9ea",
  "span_id": "71aa357ebc3abc8f",
  "service": "otel-demo"
}
```

### 2. Request ID Propagation

Request IDs are:
- Extracted from `X-Request-Id` header (if present)
- Auto-generated if not provided
- Stored in MDC for logging
- Propagated via OpenTelemetry Baggage
- Available throughout the request lifecycle

### 3. Custom Spans

The `BusinessService` demonstrates manual span creation:

```java
Span workSpan = tracer.spanBuilder("business.work")
    .setAttribute("work.duration_ms", ms)
    .startSpan();
```

Custom spans allow you to:
- Track business operations separately from HTTP spans
- Add custom attributes
- Extract baggage values
- Record exceptions

### 4. AOP-based Metrics

Instead of try-finally blocks, use the `@TrackMetrics` annotation:

```java
@GetMapping("/hello")
@TrackMetrics
public HelloResponse hello() {
    // Your code - metrics collected automatically
}
```

The `MetricsAspect` automatically tracks:
- Request count (`endpoint.requests`)
- Error count (`endpoint.errors`)
- Latency (`endpoint.latency`)

### 5. Error Tracking

Errors are automatically tracked via the AOP aspect:
- `ResponseStatusException` → tracked as errors
- Other exceptions → tracked as errors
- Error metrics tagged by endpoint

## 📊 Viewing Data in Grafana Cloud

### Logs (Loki)

**Query all logs:**
```logql
{service="otel-demo"} | logfmt
```

**Query by endpoint:**
```logql
{service="otel-demo", endpoint="/work"} | logfmt
```

**Query with trace correlation:**
```logql
{service="otel-demo"} | logfmt | trace_id="<trace_id>"
```

**View request/response logs:**
```logql
{service="otel-demo"} | logfmt | message=~"Incoming request|Outgoing response"
```

### Traces (Tempo)

**Filter by service:**
```
service.name = otel-demo
```

**Find custom spans:**
```
service.name = otel-demo AND name = business.work
```

**Trace by request ID:**
Use the trace ID from logs to find the full trace in Tempo.

### Metrics (Prometheus)

**Requests per second by endpoint:**
```promql
sum by (endpoint)(rate(endpoint_requests_total{service="otel-demo"}[5m]))
```

**Error rate by endpoint:**
```promql
sum by (endpoint)(rate(endpoint_errors_total{service="otel-demo"}[5m]))
```

**Latency percentiles:**
```promql
histogram_quantile(0.95, 
  sum by (le, endpoint)(rate(endpoint_latency_seconds_bucket{service="otel-demo"}[5m]))
)
```

**HTTP server requests (Spring Boot auto-metrics):**
```promql
sum by (endpoint)(rate(http_server_requests_seconds_count{service="otel-demo"}[5m]))
```

**p95 latency for /work endpoint:**
```promql
histogram_quantile(0.95, 
  sum by (le)(rate(http_server_requests_seconds_bucket{
    service="otel-demo",
    uri="/work"
  }[5m]))
)
```

## 🔧 Configuration Details

### OpenTelemetry Agent (`otel.properties`)

- **Service name**: `otel-demo`
- **Traces**: Exported to Grafana Cloud OTLP Gateway
- **Logs**: Exported to local Alloy (port 4318)
- **Metrics**: Disabled (using Micrometer instead)
- **Sampling**: `always_on` (for demo purposes)

### Micrometer (`application.properties`)

- **Registry**: OTLP Meter Registry
- **Export interval**: 10 seconds
- **Temporality**: CUMULATIVE
- **Resource attributes**: `service.name`, `service.version`

### Logback (`logback-spring.xml`)

- **Format**: JSON (Logstash encoder)
- **Output**: Console + rolling file (`logs/otel-demo/otel-demo.log`)
- **MDC keys**: `trace_id`, `span_id`, `request_id`
- **Custom fields**: `service` name

### Grafana Alloy (`ops/alloy.river`)

- **OTLP receiver**: HTTP on port 4318
- **Batch processor**: Groups logs before export
- **Loki exporter**: Pushes to Grafana Cloud Loki with Basic auth

## 🧪 Testing Observability

### Test Request ID Propagation

```bash
# Send request with custom ID
curl "http://localhost:8080/work?ms=200" -H "X-Request-Id: test-123"

# Check logs - request_id should be "test-123" throughout
# Check traces - baggage should contain request.id=test-123
```

### Test Custom Spans

```bash
curl "http://localhost:8080/work?ms=500"

# In Grafana Tempo, look for:
# - HTTP span: GET /work
# - Custom span: business.work (child of HTTP span)
```

### Test Error Tracking

```bash
# Invalid parameter (400 error)
curl "http://localhost:8080/work?ms=70000"

# Check metrics: endpoint_errors_total{endpoint="/work"} should increment
# Check logs: status=400 in response log
```

### Test Log Correlation

1. Make a request: `curl http://localhost:8080/hello`
2. Extract `trace_id` from logs
3. Search for that `trace_id` in Grafana:
   - **Loki**: All logs with that trace_id
   - **Tempo**: Full trace with all spans

## 📝 Key Files Reference

| File | Purpose |
|------|---------|
| `otel.properties` | OpenTelemetry Java agent configuration |
| `ops/alloy.river` | Grafana Alloy pipeline configuration |
| `docker-compose.yml` | Local Alloy container setup |
| `src/main/resources/logback-spring.xml` | Logback JSON logging configuration |
| `src/main/resources/application.properties` | Spring Boot and Micrometer configuration |
| `src/main/java/.../filter/ObservabilityFilter.java` | Request/response logging and baggage propagation |
| `src/main/java/.../aspect/MetricsAspect.java` | AOP-based metrics collection |
| `src/main/java/.../service/BusinessService.java` | Custom spans example |

## 🎓 Learning Resources

This demo demonstrates:

1. **OpenTelemetry Best Practices**
   - Automatic instrumentation via Java agent
   - Manual span creation for business logic
   - Baggage propagation for cross-cutting concerns

2. **Spring Boot Observability**
   - Structured logging with Logback
   - Micrometer metrics integration
   - AOP for cross-cutting metrics

3. **Grafana Cloud Integration**
   - OTLP protocol for traces and metrics
   - Loki for log aggregation
   - Alloy as a local collector

4. **Production Patterns**
   - Request ID correlation
   - Error tracking and alerting
   - Performance monitoring

## 🤝 Contributing

This is a demo repository. Feel free to:
- Fork and adapt for your use case
- Submit issues or improvements
- Use as a reference for your own projects

## 📄 License

[Add your license here]

## 🙏 Acknowledgments

- OpenTelemetry project
- Spring Boot team
- Grafana Labs
- Micrometer project
