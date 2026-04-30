# DWP Debt Collection Management System (DCMS) — Monorepo Scaffold Design Plan

---

## Executive Summary

This document provides a detailed, step-by-step scaffold design for the DWP Debt Collection Management System (DCMS) — a greenfield Java 21 + React + TypeScript monorepo. The plan covers six interconnected scaffolding pieces:

1. **Monorepo skeleton** — folder structure, root-level configuration files
2. **Backend** — Maven project structure, Spring Boot configuration, health endpoints, logging
3. **Frontend** — React + TypeScript project, GOV.UK Design System integration, Nginx
4. **Docker Compose** — full local development stack with all dependencies
5. **Helm chart** — exactly 5 templates for Kubernetes deployment
6. **GitHub Actions workflows** — CI/CD automation (build, test, scan, deploy)

All scaffolding follows the delivery methodology standards:
- **STD-PLAT-009** (Containerization): Multi-stage Docker builds, pinned images, non-root users, SIGTERM handling
- **STD-PLAT-010** (Local Dev Environment): `docker compose up` one-command startup, `depends_on: service_healthy`, health checks
- **STD-OPS-003** (Health Endpoints): `/health/live` and `/health/ready` without authentication
- **STD-OPS-004** (Structured Logging): JSON to stdout with correlationId via MDC
- **STD-PLAT-008** (Local/Remote Parity): identical config keys, same auth flow, JSON logs in both environments
- **STD-PLAT-006** (Database Migrations): Flyway immutability, idempotency, zero-downtime patterns

---

## 1. Monorepo Skeleton

> **Note (2026-04-22):** The domain package list in section 1.1 predates ADR-001 through ADR-006. The authoritative domain model and package structure is now defined in `CLAUDE.md` (Architecture section) and ADR-003. In particular: `domain/debt/` and `domain/workflow/` do not exist; the process engine lives at `infrastructure/process/`; interfaces live at `shared/process/port/`. Treat section 1.1 as a scaffold skeleton only — not an authoritative package list.

### 1.1 Folder Structure

```
DWP-system-prototype/
├── backend/                          ← Spring Boot monolith (Maven)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/netcompany/dcms/
│   │   │   │   ├── DcmsApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── CorrelationIdFilter.java
│   │   │   │   │   └── ApplicationProperties.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── user/
│   │   │   │   │   ├── debt/
│   │   │   │   │   ├── workallocation/
│   │   │   │   │   ├── payment/
│   │   │   │   │   ├── communications/
│   │   │   │   │   ├── workflow/
│   │   │   │   │   ├── audit/
│   │   │   │   │   └── integration/
│   │   │   │   └── shared/
│   │   │   │       ├── health/
│   │   │   │       │   ├── HealthController.java
│   │   │   │       │   └── HealthCheckService.java
│   │   │   │       └── error/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── application-local.properties
│   │   │       ├── application-dev.properties
│   │   │       ├── logback-spring.xml
│   │   │       └── db/
│   │   │           └── migration/
│   │   │               └── V1.0__initial_schema.sql
│   │   └── test/
│   │       └── java/com/netcompany/dcms/
│   ├── pom.xml
│   ├── Dockerfile
│   └── .dockerignore
│
├── frontend/                         ← React + TypeScript (npm)
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── index.tsx
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   ├── Layout/
│   │   │   │   ├── Header.tsx
│   │   │   │   ├── Footer.tsx
│   │   │   │   └── Navigation.tsx
│   │   │   └── Common/
│   │   ├── pages/
│   │   │   ├── Dashboard.tsx
│   │   │   └── NotFound.tsx
│   │   ├── services/
│   │   │   ├── api.ts
│   │   │   └── health.ts
│   │   ├── types/
│   │   ├── styles/
│   │   │   ├── globals.scss
│   │   │   └── govuk-overrides.scss
│   │   └── utils/
│   ├── package.json
│   ├── tsconfig.json
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── .dockerignore
│   └── .env.example
│
├── infrastructure/
│   ├── docker-compose.yml
│   ├── .env.example
│   ├── keycloak/
│   │   └── realm-dcms.json
│   └── helm/
│       └── dcms/
│           ├── Chart.yaml
│           ├── values.yaml
│           ├── values-local.yaml
│           ├── values-dev.yaml
│           ├── README.md
│           └── templates/
│               ├── _helpers.tpl
│               ├── Deployment.yaml
│               ├── Service.yaml
│               ├── ConfigMap.yaml
│               ├── Ingress.yaml
│               └── ServiceAccount.yaml
│
├── docs/
│   ├── project-foundation/
│   │   ├── scaffold-design-plan.md   ← this file
│   │   ├── architecture.md
│   │   ├── remote-environment-spec.md
│   │   └── decision-log.md
│   └── runbooks/
│       ├── local-setup.md
│       └── troubleshooting.md
│
├── .github/
│   ├── workflows/
│   │   ├── ci.yml
│   │   ├── compose-smoke-test.yml
│   │   ├── helm-lint.yml
│   │   └── deploy-dev.yml
│   └── pull_request_template.md
│
├── .gitignore
├── .env.example
├── README.md
└── CLAUDE.md
```

### 1.2 Key Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Monorepo structure | Single `backend/`, `frontend/`, `infrastructure/` | Aligns with stated monorepo decision; simplifies single-command deployment |
| Java namespace | `com.netcompany.dcms` | Netcompany org + project prefix |
| Parent POM | Deferred | Reduces complexity for greenfield; add only when Maven shared config becomes necessary |
| Database | Single PostgreSQL 16 | Single monolith + single frontend; shared DB is correct at this stage |
| Keycloak config | Realm JSON in Git | Ensures local/dev parity; realm exported or pre-seeded |

### 1.3 Open Decisions (Human Input Required)

| Decision | Options | Impact |
|---|---|---|
| Release versioning | Semantic (0.1.0) vs Calendar (2026.04.21) | Helm Chart.yaml version field |
| Frontend build tool | **Vite** (recommended) vs Create React App | Build output path (`dist/` vs `build/`) affects Dockerfile COPY path |
| Keycloak user seeding | Pre-seeded realm JSON vs manual admin | Affects `realm-dcms.json` content |

---

## 2. Backend (Spring Boot + Maven)

### 2.1 pom.xml Structure

**File:** `backend/pom.xml`

| Section | Value |
|---|---|
| `groupId` | `com.netcompany` |
| `artifactId` | `dcms-backend` |
| `version` | TBD (pending versioning decision) |
| `packaging` | `jar` |
| Parent | `spring-boot-starter-parent:3.4.x` |

**Key dependencies:**

| Dependency | Version | Purpose |
|---|---|---|
| `spring-boot-starter-web` | from parent | REST API |
| `spring-boot-starter-data-jpa` | from parent | JPA/Hibernate |
| `spring-boot-starter-security` | from parent | Security framework |
| `spring-boot-starter-oauth2-resource-server` | from parent | JWT validation |
| `org.postgresql:postgresql` | `42.7.x` | PostgreSQL JDBC driver |
| `org.flywaydb:flyway-core` | `10.x` | DB migrations (Community Edition) |
| `net.logstash.logback:logstash-logback-encoder` | `7.4` | JSON structured logging |
| `spring-boot-starter-test` | from parent | JUnit 5, Mockito |

**Build plugins:**

| Plugin | Purpose |
|---|---|
| `maven-compiler-plugin` | Java 21, `-parameters` flag |
| `maven-surefire-plugin` | Unit test runner |
| `spring-boot-maven-plugin` | Executable JAR with embedded Tomcat |

### 2.2 Application Configuration

**`backend/src/main/resources/application.properties` (base):**

```properties
spring.application.name=dcms-backend

server.port=${PORT:8080}
server.servlet.context-path=/api
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/dcms}
spring.datasource.username=${POSTGRES_USER:postgres}
spring.datasource.password=${POSTGRES_PASSWORD:changeme}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.format_sql=false

spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false
spring.flyway.validate-on-migrate=true
spring.flyway.locations=classpath:db/migration

logging.level.root=${LOG_LEVEL:INFO}
logging.config=classpath:logback-spring.xml

spring.security.oauth2.resourceserver.jwt.issuer-uri=${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/dcms}
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${KEYCLOAK_JWK_SET_URI:http://localhost:8180/realms/dcms/protocol/openid-connect/certs}
```

**`application-local.properties` (local overrides):**

```properties
logging.level.root=DEBUG
logging.level.com.netcompany.dcms=DEBUG
spring.datasource.url=jdbc:postgresql://db:5432/dcms
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/dcms
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://keycloak:8080/realms/dcms/protocol/openid-connect/certs
```

**`application-dev.properties`:** No local defaults — all config from environment variables injected by Kubernetes.

### 2.3 Health Endpoints (STD-OPS-003)

**Files:**
- `shared/health/HealthController.java` — `GET /health/live` (200 always) and `GET /health/ready` (200 or 503)
- `shared/health/HealthCheckService.java` — executes `SELECT 1` against DataSource
- `config/SecurityConfig.java` — permits `/health/**` without authentication

```java
// HealthController.java
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness(HealthCheckService svc) {
        if (svc.isDatabaseReachable()) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }
        return ResponseEntity.status(503)
            .body(Map.of("status", "degraded", "database", "unreachable"));
    }
}
```

### 2.4 Structured Logging (STD-OPS-004)

**`logback-spring.xml`:**

```xml
<configuration>
  <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <providers>
        <timestamp><fieldName>timestamp</fieldName></timestamp>
        <logLevel><fieldName>level</fieldName></logLevel>
        <message/>
        <mdc><includeMdcKeyName>correlationId</includeMdcKeyName></mdc>
        <stackTrace/>
      </providers>
    </encoder>
  </appender>
  <root level="${LOG_LEVEL:-INFO}">
    <appender-ref ref="JSON_CONSOLE"/>
  </root>
  <logger name="org.springframework" level="INFO"/>
  <logger name="org.hibernate" level="WARN"/>
</configuration>
```

**`config/CorrelationIdFilter.java`:** `OncePerRequestFilter` that reads `X-Correlation-ID` from incoming request header (or generates UUID), puts it in MDC, removes it after response.

### 2.5 Flyway Migrations

**Location:** `backend/src/main/resources/db/migration/`

**Naming:** `V{major}.{minor}__{description}.sql`

**Initial files:**
- `V1.0__initial_schema.sql` — core tables
- `V1.1__audit_table.sql` — audit trail schema

**Rules (STD-PLAT-006):**
- Immutable once applied to any environment
- Use `IF NOT EXISTS` / `ON CONFLICT DO NOTHING` for idempotency
- Include GitHub issue reference as header comment

### 2.6 Security Configuration

**`config/SecurityConfig.java`:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### 2.7 Backend Dockerfile

**`backend/Dockerfile`:**

```dockerfile
# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz \
    | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
WORKDIR /app

COPY --from=build /app/target/*-exec.jar app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Parity with VATRI:**
- `eclipse-temurin:21-jdk-jammy` build, `eclipse-temurin:21-jre-jammy` runtime (matches starter pack mandate; use `-alpine` variants if image size is a concern)
- Inline Maven 3.9.6 download (no wrapper jar in Docker)
- SIGTERM handled via `server.shutdown=graceful` in application.properties

---

## 3. Frontend (React + TypeScript + GOV.UK Design System + Nginx)

### 3.1 Key Dependencies (package.json)

| Package | Version | Purpose |
|---|---|---|
| `react` | `^18` | UI framework |
| `react-dom` | `^18` | DOM rendering |
| `react-router-dom` | `^6` | Client-side routing |
| `typescript` | `^5` | Type safety |
| `govuk-frontend` | `^5` | GOV.UK Design System components |
| `sass` | `^2` | SCSS support for GOV.UK styles |
| `axios` | `^1` | HTTP client |
| `uuid` | `^9` | Correlation ID generation |

**Dev dependencies:** `vite`, `@vitejs/plugin-react`, `@types/react`, `@types/react-dom`, `jest`, `@testing-library/react`, `eslint`, `prettier`

**Scripts:**

```json
{
  "dev": "vite",
  "build": "tsc && vite build",
  "test": "jest",
  "lint": "eslint src --ext .ts,.tsx"
}
```

### 3.2 GOV.UK Design System Integration

**`src/styles/govuk-overrides.scss`:**

```scss
@import 'node_modules/govuk-frontend/dist/govuk/all';
// Project-specific overrides below
```

**`src/index.tsx`:** Import `./styles/govuk-overrides.scss` as the entry-point stylesheet.

**Component pattern:**

```tsx
// Use GOV.UK CSS classes directly
<button className="govuk-button">Submit</button>
<table className="govuk-table">...</table>
<input className="govuk-input" />
```

### 3.3 HTTP Client (api.ts)

- Axios instance with `baseURL: process.env.REACT_APP_API_URL`
- Request interceptor: generates UUID and sets `X-Correlation-ID` header on every request
- Response interceptor: redirects to `/auth/login` on 401

### 3.4 Environment Variables

**`frontend/.env.example`:**

```bash
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_KEYCLOAK_URL=http://localhost:8180
REACT_APP_KEYCLOAK_REALM=dcms
REACT_APP_KEYCLOAK_CLIENT_ID=dcms-frontend
REACT_APP_LOG_LEVEL=info
```

### 3.5 Frontend Dockerfile

```dockerfile
# ── Build stage ──────────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build --chown=appuser:appgroup /app/dist /usr/share/nginx/html

USER appuser
EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
```

> **Note:** Build output path is `dist/` when using Vite. Change to `build/` if using Create React App.

### 3.6 Nginx Configuration (nginx.conf)

Key rules:
- Listens on port 8080 (not 80 — non-root user cannot bind to ports < 1024)
- SPA fallback: `try_files $uri $uri/ /index.html`
- Security headers: `X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`
- `/api/` proxied to backend container `api:8080`

---

## 4. Docker Compose (Local Development Stack)

### 4.1 Services

**File:** `infrastructure/docker-compose.yml`

| Service | Image | Port | Depends On |
|---|---|---|---|
| `db` | `postgres:16` | `5432` | — |
| `keycloak` | `quay.io/keycloak/keycloak:24` | `8180→8080` | `db` (healthy) |
| `api` | Built from `backend/Dockerfile` | `8080` | `db` (healthy), `keycloak` (healthy) |
| `frontend` | Built from `frontend/Dockerfile` | `3000→8080` | `api` (healthy) |

All services use `depends_on: condition: service_healthy`. All services define a `healthcheck` block.

### 4.2 Healthcheck Definitions

```yaml
# db
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres}"]
  interval: 5s
  timeout: 5s
  retries: 5

# keycloak
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health/ready"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s

# api
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:8080/health/ready || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 6
  start_period: 40s

# frontend
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:8080/ || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 3
  start_period: 10s
```

### 4.3 Environment Variables (.env.example)

**File:** `infrastructure/.env.example`

```bash
# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=dcms

# Keycloak
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_HOSTNAME=keycloak

# Application
PORT=8080
LOG_LEVEL=info
```

### 4.4 Keycloak Realm Import

Keycloak starts with `--import-realm` flag, loading `infrastructure/keycloak/realm-dcms.json` at container startup. The realm JSON defines:
- Realm name: `dcms`
- Clients: `dcms-backend`, `dcms-frontend`
- Roles: `AGENT`, `TEAM_LEADER`, `OPS_MANAGER`, `COMPLIANCE`, `ADMIN`
- Seed users (for local dev only)

---

## 5. Helm Chart (Kubernetes Deployment)

### 5.1 Chart Structure

**Location:** `infrastructure/helm/dcms/`

Exactly 5 templates (per STD-PLAT-011):

| Template | Purpose |
|---|---|
| `Deployment.yaml` | Pod deployment, replicas, health probes, resource limits |
| `Service.yaml` | ClusterIP service on port 8080 |
| `ConfigMap.yaml` | Non-sensitive env vars from `values.yaml env:` |
| `Ingress.yaml` | HTTP ingress (no TLS — DevOps configures TLS) |
| `ServiceAccount.yaml` | Basic service account (no annotations — DevOps configures) |

Plus `_helpers.tpl` for shared template functions.

### 5.2 Values Files

| File | Purpose |
|---|---|
| `values.yaml` | Base defaults (image, replicas, service port, env vars) |
| `values-local.yaml` | k3d overrides (pullPolicy: Never, debug log, local DB/KC URLs) |
| `values-dev.yaml` | AKS overrides (2 replicas, prod-like resources, dev ingress host) |

**Image configuration in values.yaml:**

```yaml
image:
  repository: ufstpit-dev-docker.repo.netcompany.com/dcms_v0/dcms_v0-backend
  tag: ""     # Overridden at deploy time: --set image.tag=${GITHUB_SHA}
  pullPolicy: IfNotPresent

env:
  PORT: "8080"
  LOG_LEVEL: "info"
  DATABASE_URL: ""           # Injected from Kubernetes Secret in dev
  KEYCLOAK_ISSUER_URI: ""   # Injected from Kubernetes Secret in dev

secrets: {}   # Sensitive values — populated by DevOps via External Secrets
```

### 5.3 Health Probes in Deployment.yaml

```yaml
livenessProbe:
  httpGet:
    path: /health/live
    port: http
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /health/ready
    port: http
  initialDelaySeconds: 15
  periodSeconds: 10
  failureThreshold: 3
```

### 5.4 Verification Commands

```bash
helm lint infrastructure/helm/dcms
helm template dcms infrastructure/helm/dcms -f infrastructure/helm/dcms/values-local.yaml
helm template dcms infrastructure/helm/dcms -f infrastructure/helm/dcms/values-dev.yaml
```

Both must pass with no errors before handoff (per NFR-C4-011).

---

## 6. GitHub Actions Workflows

### 6.1 ci.yml — Build, Test, Scan

**Trigger:** Every push and PR to `main`

**Jobs:**

| Job | Steps |
|---|---|
| `backend-build` | Java 21 setup, `mvn verify` |
| `backend-test` | `mvn test` |
| `backend-image` | Docker multi-stage build (needs: build + test) |
| `backend-scan` | Trivy CRITICAL/HIGH scan on backend image |
| `frontend-build` | Node 20 setup, `npm ci`, `npm run build`, `npm run lint` |
| `frontend-test` | `npm test` |
| `frontend-image` | Docker multi-stage build (needs: build + test) |

**On main branch only:** push images to registry with dual tags (`{sha}` + `:dev`).

**Secrets required:**

```
REGISTRY=ufstpit-dev-docker.repo.netcompany.com
REGISTRY_USER=<username>
REGISTRY_PASSWORD=<PAT>
```

### 6.2 compose-smoke-test.yml — Full Stack Health Validation

**Trigger:** Push to `main`

**Steps:**
1. `cp infrastructure/.env.example infrastructure/.env`
2. `docker compose -f infrastructure/docker-compose.yml up -d --build`
3. Wait for services (60s timeout)
4. `curl -f http://localhost:8080/health/ready`
5. `curl -f http://localhost:3000/`
6. On failure: print all service logs
7. Always: `docker compose down -v`

### 6.3 helm-lint.yml — Chart Validation

**Trigger:** Push to any branch when files in `infrastructure/helm/**` change

**Steps:**
1. `helm lint infrastructure/helm/dcms`
2. `helm template dcms infrastructure/helm/dcms -f values-local.yaml`
3. `helm template dcms infrastructure/helm/dcms -f values-dev.yaml`

### 6.4 deploy-dev.yml — Deploy to Dev AKS

**Trigger:** `workflow_run` on CI success on `main`

**Steps:**
1. Login to registry
2. Push backend image with `{sha}` and `:dev` tags
3. Push frontend image with `{sha}` and `:dev` tags
4. `helm upgrade --install dcms ... --atomic --set image.tag=${sha}`

**Secrets required:**

```
KUBE_CONFIG=<base64-encoded kubeconfig for dev AKS>
```

---

## 7. Implementation Sequencing

**Dependency-ordered creation:**

1. Root files — `.gitignore`, `README.md`, `.env.example`
2. Backend — `pom.xml`, domain skeleton, health endpoints, logging, Flyway, Dockerfile
3. Frontend — `package.json`, GOV.UK setup, API client, Dockerfile, nginx.conf
4. Keycloak realm — `infrastructure/keycloak/realm-dcms.json`
5. Docker Compose — `infrastructure/docker-compose.yml`, `infrastructure/.env.example`
6. Helm chart — `Chart.yaml`, 5 templates, `values.yaml`, `values-local.yaml`, `values-dev.yaml`
7. GitHub Actions — `ci.yml`, `compose-smoke-test.yml`, `helm-lint.yml`, `deploy-dev.yml`
8. Documentation — `docs/project-foundation/`, runbooks

**Critical path:**
- Docker Compose requires all Dockerfiles → Backend and Frontend must be scaffolded first
- Helm deployment requires health endpoints → Backend `/health/live` and `/health/ready` must exist
- CD pipeline requires images in registry → CI must be fully working first

---

## 8. Open Decisions (Require Human Input Before Implementation)

| Decision | Options | Impact |
|---|---|---|
| **Release versioning** | Semantic (`0.1.0`) vs Calendar (`2026.04.21`) | Helm `Chart.yaml` version field |
| **Frontend build tool** | **Vite** (recommended) vs Create React App | Dockerfile COPY path (`dist/` vs `build/`) |
| **Keycloak user seeding** | Pre-seeded realm JSON vs manual admin console | Content of `realm-dcms.json` |
| **Dev secrets management** | Sealed Secrets vs Azure Key Vault vs Manual | Helm chart `secrets:` section; DevOps concern |
| **Dev ingress host** | To be set by DevOps | `values-dev.yaml` `ingress.host` field |

---

## 9. Standards Compliance Matrix

| Standard | Where Applied | Compliance |
|---|---|---|
| **STD-PLAT-009** (Containerization) | Backend/Frontend Dockerfiles | Multi-stage, pinned images, non-root user, SIGTERM via `server.shutdown=graceful` |
| **STD-PLAT-010** (Local Dev Env) | `docker-compose.yml`, `.env.example`, `README.md` | Single-command startup, all healthchecks, offline-capable |
| **STD-OPS-003** (Health Endpoints) | `HealthController.java`, Helm probes | `/health/live` 200 always, `/health/ready` 200/503, no auth |
| **STD-OPS-004** (Structured Logging) | `logback-spring.xml`, `CorrelationIdFilter.java` | JSON to stdout, `correlationId` in every entry via MDC |
| **STD-PLAT-008** (Local/Remote Parity) | `values-local.yaml` vs `values-dev.yaml` | Identical config keys, Keycloak runs locally (not skipped) |
| **STD-PLAT-006** (DB Migrations) | Flyway setup, `V*.sql` naming | Immutable, idempotent, auto-run on startup |
| **NFR-C4-004 to 011** | All scaffold pieces | Dockerfile, compose, health, logging, DB, README, Helm |

---

## 10. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Helm chart drifts from compose config | Treat `docker-compose.yml` and Helm values as a pair; update together in same PR |
| Keycloak realm out of sync between envs | Export realm from running instance after seeding; commit to Git; import on startup |
| Correlation ID lost in async code | Configure `TaskDecorator` on Spring async executor to copy MDC context |
| Sensitive data in logs | Audit `logback-spring.xml`; add redaction patterns for known sensitive fields |
| Flyway migration conflicts | Enforce renumbering before merge via PR checklist; migrations are immutable after merge |
| Frontend Nginx port < 1024 | Already mitigated — Nginx listens on 8080, not 80 |
