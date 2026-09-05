# Observability Runbook — Point-of-Sale (Vert.x)

Ringkas untuk diagnosa cepat. Source of truth kode: `common/observability`,
`common/config/TelemetryConfig`, `apigateway/observability/GatewayMetricsMiddleware`.

## Stack (compose)

| Komponen | Port | Peran |
|---|---|---|
| OTel Collector | 4317 (gRPC OTLP), 8889 (Prometheus) | Terima traces/metrics dari service; expose ke Prometheus |
| Prometheus | 9090 | Scrape OTel (8889), kafka-exporter (9308), postgres-exporter (9187), node-exporter (9100) |
| Grafana | 3000 | Dashboard (jvm.json, logs.json) + alerting |
| Jaeger | 16686 | Traces UI |
| Alertmanager | 9093 | Routing alert → email |
| Loki | 3100 | Logs |

## Alur telemetry

```
Client → NGINX :80 → Gateway :5000 (GatewayMetricsMiddleware: span+metrics)
  → gRPC → Service (TracingMetrics: span + requests_total / request_duration_seconds / requests_in_flight)
  → RedisService (redis cache hits/misses/sets)
  → OTel SDK (OTLP :4317) → OTel Collector → {Prometheus :8889, Jaeger}
Prometheus → Alertmanager → email
```

Metrics utama (via Prometheus/OTel):
- `http.requests_total{http.status_class}` — gateway request count (2xx/3xx/4xx/5xx).
- `http.request_duration_seconds` / `http.in_flight_requests` — gateway latency/in-flight.
- `requests_total{method,status}` / `request_duration_seconds` / `requests_in_flight` — per-service gRPC (dari `TracingMetrics`).
- `redis.cache.hits` / `redis.cache.misses` / `redis.cache.sets` — RedisService.
- `kafka_consumergroup_lag` — kafka-exporter `:9308` (consumer lag).
- JVM runtime metrics (memory/gc/cpu) — `TelemetryConfig` + JFR.

## Alert utama (observability/rules/)

| Alert | Indikasi | Tindakan |
|---|---|---|
| `*ServiceDown` / down | Service crash / image lama / port salah | `docker compose ps`; cek log service; cek `GRPC_*_ADDR` env |
| Redis hit rate rendah / error | `Failed to connect to all nodes` | Pastikan cluster `ok`, 6 node, dan slot endpoint stabil (`172.18.0.40-.45` pada Compose); cek `redis-cli cluster info` dan `cluster slots` |
| Kafka lag naik | Consumer lambat / mati | cek `kafka-exporter:9308`; log email-service; topic tak diproses |
| 5xx tinggi | Service error / chaos aktif | cek `/api/chaos/policies`; log gateway (trace_id); Jaeger trace |
| Latency histogram naik | DB slow / chaos latency | cek `pg_stat_activity`; chaos policy latency |

## Diagnosa cepat

```bash
# Health & status
curl localhost:5000/health
docker compose -f deployments/local/docker-compose.yml ps

# Redis cluster lifecycle (Compose; jangan gunakan down -v sebagai default)
docker exec redis_node_1 sh -c 'redis-cli -a "${REDIS_PASSWORD:-dragon_knight}" --no-auth-warning cluster info'
docker exec redis_node_1 sh -c 'redis-cli -a "${REDIS_PASSWORD:-dragon_knight}" --no-auth-warning cluster slots'
# PgBouncer admin is internal port 5432; host port is localhost:6432
PGPASSWORD=DRAGON psql -h localhost -p 6432 -U DRAGON -d pgbouncer -c 'SHOW POOLS;'

# Chaos aktif?
curl localhost:5000/api/chaos/policies
curl -X POST localhost:5000/api/chaos/halt          # matikan semua
curl -X POST localhost:5000/api/chaos/policies/reload  # reload dari chaos.yaml

# Metrics mentah
curl localhost:8889/metrics | grep -E 'requests_total|in_flight'   # via otel-collector
curl localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job, health}'
curl localhost:9090/api/v1/query?query=http.requests_total

# Traces
open http://localhost:16686    # Jaeger — cari service api-gateway / auth-service

# Trace id di error envelope gateway → cocokkan di Jaeger
curl -s localhost:5000/api/products | jq '.trace_id'
```

## Aturan

- Jangan log token/password (`.setPassword`/JWT tidak boleh masuk log).
- Error envelope gateway `{status,message,code,trace_id}` — `trace_id` = OTel trace id asli
  (sejak Fase 5 `GatewayMetricsMiddleware` membuat span aktif).
- Perubahan chaos: hanya `enabled: true` pada policy yang diuji; `POST /api/chaos/halt`
  harus selalu bisa mematikan.
