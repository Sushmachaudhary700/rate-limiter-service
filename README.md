# Rate Limiter as a Service

A standalone Spring Boot microservice that lets other services check "is this
client allowed to make this request right now?" It supports two algorithms —
**Token Bucket** and **Sliding Window** — and enforces limits atomically in
Redis so the check stays correct even when this service is running as
multiple replicas.

## Tech stack

| Layer | Tech |
|---|---|
| Framework | Spring Boot 3, Java 17 |
| Rate-limit state | Redis (Lettuce client) + Lua scripts for atomicity |
| Rule storage | PostgreSQL, Spring Data JPA |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Mockito, MockMvc |
| Containerization | Docker, Docker Compose |

## Running it in IntelliJ

### 1. Prerequisites
- IntelliJ IDEA (Community or Ultimate)
- JDK 17 installed (`File > Project Structure > SDK` → set to 17)
- Docker Desktop running (for Redis + Postgres) — or install both locally

### 2. Open the project
`File > Open` → select the unzipped `rate-limiter-service` folder. IntelliJ
will detect the `pom.xml` and prompt to import as a Maven project — click
**Load Maven Project**. Wait for dependencies to download.

### 3. Start Redis and PostgreSQL
Easiest option — run both via Docker, leave the Spring Boot app running
in IntelliJ itself:
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_DB=ratelimiter -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  postgres:16-alpine
```
(No Docker? Install both locally: `brew install redis postgresql` on Mac, or
`sudo apt install redis-server postgresql` on Ubuntu/WSL — then create a
`ratelimiter` database and update the credentials in `application.yml` if
they differ from `postgres`/`postgres`.)

### 4. Run the app
Open `RateLimiterApplication.java` → click the green ▶ run icon next to
`public static void main`. IntelliJ will start it on `http://localhost:8080`.

If Lombok annotations show red underlines / getters seem "missing": go to
`Settings > Plugins` → install **Lombok** plugin → restart IntelliJ. Also
enable `Settings > Build, Execution, Deployment > Compiler > Annotation
Processors` → check **Enable annotation processing**.

### 5. Try it out
Two demo rules are seeded automatically on startup (`demo-client` on
`/api/orders` and `/api/search`). Check the API docs at:
```
http://localhost:8080/docs
```

Or curl it directly:
```bash
curl -X POST http://localhost:8080/api/v1/rate-limit/check \
  -H "Content-Type: application/json" \
  -d '{"clientId":"demo-client","resource":"/api/orders","tokensRequested":1}'
```
Call it 11 times quickly — the 11th should return `429 Too Many Requests`
(the seeded rule allows 10 requests per 10 seconds).

Manage rules via the admin API:
```bash
curl http://localhost:8080/api/v1/rules
```

### 6. Run tests
Right-click `src/test/java` → **Run 'All Tests'**, or:
```bash
mvn test
```

### 7. Run everything via Docker Compose instead (optional)
If you'd rather not run Redis, Postgres, and the app separately:
```bash
docker compose up --build
```
This starts the app, Redis, and Postgres together, all wired to talk to each other.

## Project structure
```
src/main/java/com/ratelimiter/
├── RateLimiterApplication.java     # entry point
├── config/RedisConfig.java         # Redis + Lua script beans
├── model/                          # RateLimitRule entity, algorithm enum
├── repository/                     # Spring Data JPA repository
├── service/
│   ├── RateLimiter.java            # strategy interface
│   ├── RateLimiterFactory.java     # picks strategy by algorithm
│   ├── RateLimitService.java       # orchestrates rule lookup + dispatch
│   └── impl/                       # TokenBucket + SlidingWindow impls
├── controller/                     # public check API + admin rule CRUD
└── exception/                      # centralized error handling
src/main/resources/scripts/         # the actual Lua scripts run inside Redis
```

## Why this project is interview-worthy
- Demonstrates two real rate-limiting algorithms, not just a hardcoded counter
- Shows understanding of distributed systems correctness (why plain
  Java-side counters break across multiple instances, and how Redis + Lua
  fixes that)
- Uses Strategy + Factory pattern so a third algorithm can be added without
  touching existing code (Open-Closed Principle)
- Ships with Docker, tests, and API docs — a complete, demoable deliverable
