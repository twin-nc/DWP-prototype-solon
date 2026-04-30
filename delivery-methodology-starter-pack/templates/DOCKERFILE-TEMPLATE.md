# Dockerfile Template

Use this template as the starting point when containerising a new service. It satisfies STD-PLAT-009 (Containerization). Replace `{{RUNTIME}}` sections with the block appropriate for your language.

Replace all `{{PLACEHOLDER}}` values before committing.

---

## Rules checklist before committing your Dockerfile

- [ ] Multi-stage build — build stage and runtime stage are separate
- [ ] Both stages use pinned base image tags — no `latest` or floating tags
- [ ] Final image contains only runtime dependencies
- [ ] Non-root user explicitly created and switched to before `ENTRYPOINT`
- [ ] `EXPOSE` matches the default port the application listens on (8080)
- [ ] `PORT` env var accepted by the application runtime
- [ ] SIGTERM handling configured (see framework-specific notes below)

---

## Java / Spring Boot (Maven)

```dockerfile
# ── Build stage ─────────────────────────────────────────────────────────────
# Pin to a specific Maven + JDK version. Update intentionally, not silently.
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy POM first so dependency layer is cached independently of source changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Runtime stage ────────────────────────────────────────────────────────────
# Use JRE only — no JDK, no Maven in the final image
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user before adding any files
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
WORKDIR /app

# Copy only the compiled artefact from the build stage
COPY --from=build /app/target/*.jar app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**SIGTERM for Spring Boot** — add to `application.properties`:

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

**PORT env var for Spring Boot** — add to `application.properties`:

```properties
server.port=${PORT:8080}
```

---

## Java / Spring Boot (Gradle)

```dockerfile
# ── Build stage ──────────────────────────────────────────────────────────────
FROM gradle:8.7-jdk21 AS build
WORKDIR /app

# Cache Gradle wrapper and dependencies
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon -q

COPY src ./src
RUN gradle bootJar --no-daemon -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Node.js (npm)

```dockerfile
# ── Build stage ──────────────────────────────────────────────────────────────
# Pin to a specific Node version + Alpine for a smaller build image
FROM node:22.3-alpine AS build
WORKDIR /app

# Install ALL dependencies (including devDependencies needed for the build)
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM node:22.3-alpine

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Install only production dependencies
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# Copy the build output from the build stage
COPY --from=build /app/dist ./dist

USER appuser
EXPOSE 8080

CMD ["node", "dist/index.js"]
```

**SIGTERM for Node.js** — handle in your application entry point:

```js
process.on('SIGTERM', async () => {
  // Stop accepting new requests (close server)
  server.close(async () => {
    // Drain in-flight requests, close DB connections, etc.
    await db.end();
    process.exit(0);
  });
});
```

**PORT env var for Node.js:**

```js
const port = process.env.PORT ?? 8080;
server.listen(port);
```

---

## Next.js

```dockerfile
# ── Dependencies stage ────────────────────────────────────────────────────────
FROM node:22.3-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ── Build stage ───────────────────────────────────────────────────────────────
FROM node:22.3-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM node:22.3-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

ENV NODE_ENV=production

COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

USER appuser
EXPOSE 3000

CMD ["node", "server.js"]
```

For the standalone output to work, set in `next.config.js`:

```js
module.exports = {
  output: 'standalone',
};
```

---

## Notes

### Why COPY pom.xml before COPY src?

Docker layer caching works from top to bottom. If you copy source files and the manifest (pom.xml / package.json) together, any source change invalidates the dependency download layer. Copying the manifest first means dependency layers are only invalidated when dependencies actually change.

### Why not use ENTRYPOINT + CMD together?

For application containers, a single `ENTRYPOINT ["java", "-jar", "app.jar"]` is clearer. Reserve `CMD` for providing default arguments that callers might override (e.g., database migration vs. server). If in doubt, use `ENTRYPOINT`.

### Running locally vs. in compose

In `docker-compose.yml`, use `build: .` to build from the Dockerfile. The built image is used in compose — you do not need to manually build before running `docker compose up`.
