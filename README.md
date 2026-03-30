# RateFort: Production-Grade API Gateway & Rate Limiter

RateFort is a high-performance API Gateway built with Java Spring Boot and Redis, featuring distributed rate limiting with multiple algorithms.

## Features

- **Request Proxying**: Seamlessly forwards requests to downstream services.
- **Advanced Rate Limiting**:
  - **Token Bucket**: Handles bursts while maintaining a steady rate.
  - **Sliding Window Log**: Eliminates window boundary issues for smooth throttling.
- **Flexible Throttling**: Identity resolution by IP address or `X-API-KEY` header.
- **YAML Config**: Define rules per route dynamically.
- **Distributed State**: Redis-backed counters for stateless service scaling.
- **Observability**: Built-in Prometheus metrics for monitoring.

## Getting Started

### Prerequisites

- Docker and Docker Compose

### Running the Gateway

1. Clone the repository and navigate to the project root.
2. Build and start the services:

```bash
docker-compose up --build
```

The gateway will be available at `http://localhost:8080`.

## Configuration

Edit `src/main/resources/application.yml` to configure rules:

```yaml
ratefort:
  rules:
    - path: "/api/posts/**"
      limit: 10
      duration-seconds: 60
      algorithm: TOKEN_BUCKET
    - path: "/api/comments/**"
      limit: 5
      duration-seconds: 60
      algorithm: SLIDING_WINDOW
```

## Testing Rate Limiting

### 1. Proxying Test
Forward a request to the downstream service (configured as JSONPlaceholder):
```bash
curl http://localhost:8080/api/posts/1
```

### 2. Token Bucket Throttling (/api/posts/**)
The limit is 10 requests per 60 seconds. Send 11 requests:
```bash
for i in {1..11}; do curl -i http://localhost:8080/api/posts/1; done
```
The 11th request should return `HTTP 429 Too Many Requests` with a `Retry-After` header.

### 3. Sliding Window Throttling (/api/comments/**)
The limit is 5 requests per 60 seconds. Send 6 requests:
```bash
for i in {1..6}; do curl -i http://localhost:8080/api/comments/1; done
```

### 4. Throttling by API Key
Test per-client limits using a header:
```bash
curl -i -H "X-API-KEY: my-secret-key" http://localhost:8080/api/posts/1
```

## Monitoring

View Prometheus metrics:
```bash
curl http://localhost:8080/actuator/prometheus
```
Look for:
- `rate_limit_hits_total`
- `rate_limit_throttles_total`
