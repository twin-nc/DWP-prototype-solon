# Docker Compose Template

Use this template as the starting point for `docker-compose.yml`. It satisfies STD-PLAT-010 (Local Development Environment). Replace all `{{PLACEHOLDER}}` values before committing.

The goal: `docker compose up` starts the complete working stack with no manual steps beyond copying `.env.example` to `.env`.

---

## Rules checklist before committing your docker-compose.yml

- [ ] All services the application depends on are defined (database, cache, broker, etc.)
- [ ] Application service uses `depends_on: condition: service_healthy` for all stateful dependencies
- [ ] Every dependency referenced by `condition: service_healthy` defines a `healthcheck` block
- [ ] Application service defines its own `healthcheck` using `/health/ready`
- [ ] All sensitive values come from `.env` — nothing hardcoded
- [ ] Works entirely offline — no external connectivity required
- [ ] `.env.example` is kept in sync with every variable referenced in this file

---

## Full skeleton with PostgreSQL

This is the most common pattern. Adapt the service names and add or remove services for your stack.

```yaml
services:

  # ── Database ───────────────────────────────────────────────────────────────
  # Pin the Postgres version. Update intentionally, not silently.
  # Use ${POSTGRES_USER} so the healthcheck matches the configured user.
  db:
    image: postgres:16
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres}"]
      interval: 5s
      timeout: 5s
      retries: 5
    volumes:
      - db_data:/var/lib/postgresql/data

  # ── Application API ────────────────────────────────────────────────────────
  api:
    build: .
    env_file: .env
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      # Use /health/ready so dependent services (e.g. portal) wait until the
      # API is fully initialised and the database is reachable.
      test: ["CMD-SHELL", "wget -qO- http://localhost:8080/health/ready || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 6
      start_period: 40s

  # ── Portal (if applicable) ─────────────────────────────────────────────────
  # Wait for the API to be healthy before starting the portal.
  portal:
    build: ./portal
    env_file: .env
    ports:
      - "3000:3000"
    depends_on:
      api:
        condition: service_healthy

volumes:
  db_data:
```

---

## Adding Redis

```yaml
  redis:
    image: redis:7.2-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  api:
    # ...
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
```

---

## Adding a message broker (RabbitMQ)

```yaml
  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    ports:
      - "15672:15672"   # Management UI — local only
```

---

## Corresponding .env.example

Every variable referenced in `docker-compose.yml` MUST appear in `.env.example` with a comment and a safe default:

```bash
# ── Application ─────────────────────────────────────────────────────────────

# Port the API listens on (default: 8080)
PORT=8080

# Log level: debug | info | warn | error (default: info)
LOG_LEVEL=info

# PostgreSQL connection string used by the application
# Inside Docker Compose, 'db' resolves to the postgres service.
# For local dev outside Docker: postgresql://postgres:changeme@localhost:5432/myapp
DATABASE_URL=postgresql://postgres:changeme@db:5432/myapp

# ── PostgreSQL container ─────────────────────────────────────────────────────
# These are read by the postgres:16 image to initialise the container.
# They must match the credentials used in DATABASE_URL above.
POSTGRES_USER=postgres
POSTGRES_PASSWORD=changeme
POSTGRES_DB=myapp
```

---

## Notes

### Why `condition: service_healthy` and not `condition: service_started`?

`service_started` only waits for the container process to start — not for the service inside it to be ready. PostgreSQL starts its container process immediately but takes several seconds to finish initialising. Without `service_healthy`, the application tries to connect while the database is still not accepting connections and fails with a misleading error.

### Why does the API healthcheck use `start_period`?

`start_period` tells Docker not to count failures during the first N seconds as real failures. This gives the application time to complete startup (including running database migrations) before the healthcheck starts counting against the `retries` threshold.

### Why use wget instead of curl in the healthcheck?

Alpine-based images include `wget` but not `curl`. Using `wget -qO-` is more portable. If your image includes `curl`, either works:

```yaml
test: ["CMD-SHELL", "curl -f http://localhost:8080/health/ready || exit 1"]
```

### Volumes vs bind mounts for the database

The `db_data` named volume persists the database between `docker compose up` and `docker compose down`. To wipe the database, use `docker compose down -v` (removes volumes). Never use a bind mount for the database in development — it causes permission issues on some host OSes.
