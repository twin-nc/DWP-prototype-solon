---
name: java-build-resolver
description: >
  Systematic resolution of Java/Maven build failures for Spring Boot projects —
  compilation errors, dependency conflicts, Flyway migrations, and autoconfiguration
  problems diagnosed and fixed before retrying the build.
invocation: /java-build-resolver
inputs:
  - name: build_output
    required: true
    description: Full Maven build output including the error section (mvn output from stdout/stderr)
  - name: changed_files
    required: false
    description: List of files changed since the last clean build, if known
outputs:
  - name: fix_plan
    description: Root cause category, affected files, fix applied, and verification command
roles:
  - backend-builder
---

# Java Build Resolver

## Purpose
Maven build failures in a Spring Boot monolith fail in well-known patterns. This skill classifies the failure, applies the correct fix, and confirms the build passes before marking the work done. It prevents the common loop of editing blindly and re-running `mvn` until something accidentally works.

**Scope: local dev loop only.** This skill is for failures you are diagnosing on your own machine by running `mvn verify` directly. If the failure is appearing as a red check on a GitHub Actions PR, use `ci-failure-root-cause-fixer` instead — CI failures often span multiple checks and need system-level triage before a fix is applied.

## When to Use
- `mvn verify` or `mvn package` exits non-zero on your local machine
- Compilation errors after adding or changing a class
- Dependency resolution failures (`Could not resolve`, `ArtifactDescriptorException`)
- Flyway migration errors on startup (`FlywayException`, checksum mismatch, version conflict)
- Spring Boot autoconfiguration failures (`APPLICATION FAILED TO START`, `BeanCreationException`)
- Test-compilation failures (distinct from test logic failures — the code won't compile)
- Lombok or MapStruct annotation processing errors

## Do Not Use This Skill When
- **CI is red on a PR** — use `ci-failure-root-cause-fixer` for system-level diagnosis across multiple failing checks
- **The app compiles and starts but produces wrong runtime behaviour** — use `agent-introspection-debugging`
- **Tests compile and run but assertions fail** — the build is working; diagnose the test logic directly against the acceptance criteria
- **CI is failing for non-Java reasons** (lint, Docker, Helm) — use `ci-failure-root-cause-fixer`

## Error Categories and Fix Approach

### Category 1 — Compilation Error
**Signal:** `ERROR … cannot find symbol`, `package X does not exist`, `incompatible types`

Fix sequence:
1. Identify the file and line number from the Maven error output.
2. Check whether the symbol is missing due to: a missing import, a deleted class, a renamed method, or a Lombok annotation not being processed.
3. If Lombok: confirm `lombok` is in `pom.xml` as `provided` scope and the annotation processor is enabled.
4. Fix the source, then run `mvn compile -pl <module> -am` to verify before running the full suite.

### Category 2 — Dependency Resolution Failure
**Signal:** `Could not resolve dependencies`, `Artifact … not found`, `version conflict`

Fix sequence:
1. Run `mvn dependency:tree -Dincludes=<groupId>` to see the conflict tree.
2. For version conflicts: add an explicit `<dependencyManagement>` entry to pin the correct version.
3. For missing artifacts: check the artifact exists in Maven Central or the configured registry (`ufstpit-dev-docker.repo.netcompany.com`). If registry auth is the issue, verify `~/.m2/settings.xml` credentials.
4. Never delete `pom.xml` lock entries blindly — understand which transitive is pulling the wrong version first.

### Category 3 — Flyway Migration Error
**Signal:** `FlywayException`, `Migration checksum mismatch`, `Found more than one migration with version`

Fix sequence:
1. **Checksum mismatch:** a committed migration file was edited after it ran. Never edit a committed migration. Create a new migration that corrects the data or schema.
2. **Duplicate version:** two migration files share the same version prefix. Renumber the newer one.
3. **Out-of-order:** a migration was added with a version lower than the current schema version. Either renumber it higher or enable `outOfOrder=true` in `application.yml` (dev only).
4. Run `mvn flyway:info -pl backend` to inspect current migration state before applying a fix.

### Category 4 — Spring Boot Autoconfiguration Failure
**Signal:** `APPLICATION FAILED TO START`, `BeanCreationException`, `NoSuchBeanDefinitionException`, `UnsatisfiedDependencyException`

Fix sequence:
1. Read the full cause chain in the error output — Spring wraps exceptions multiple levels deep.
2. Identify the innermost cause: missing property, missing bean, circular dependency, or wrong profile.
3. For missing properties: check `application.yml` and `application-local.yml` for the required key. If the failure occurs only in tests, also check `application-test.yml` — tests activate the `test` profile and a missing property there will not appear in the main app context.
4. For circular dependencies: introduce `@Lazy` on one injection point or restructure to break the cycle.
5. For missing beans: confirm the class is in a package scanned by `@SpringBootApplication` or explicitly `@Import`ed.

### Category 5 — Test Compilation Failure
**Signal:** Errors in `src/test/java`, often after refactoring production code

Fix sequence:
1. Treat test compilation errors the same as production compilation errors.
2. Check whether a refactored method signature is no longer compatible with test call sites.
3. Update test call sites or test fixtures to match the new signature.
4. Do not delete tests to make the build pass — fix the call site.

### Category 6 — JPA/Hibernate Schema Validation Failure
**Signal:** `SchemaManagementException`, `Schema-validation: missing table`, `Schema-validation: wrong column type`

This occurs when `spring.jpa.hibernate.ddl-auto=validate` is set and the Flyway-managed schema does not match the entity model — either a migration was added without a matching entity update, or vice versa.

Fix sequence:
1. Run `mvn flyway:info -pl backend` to confirm which migrations have been applied.
2. Compare the failing entity's `@Table`/`@Column` annotations against the schema produced by the relevant migration file.
3. If the migration is missing a column or table: add a new Flyway migration to bring the schema in line. Do not edit an already-applied migration.
4. If the entity mapping is wrong: correct the annotation (column name, type, nullable) to match the database schema.
5. Never set `ddl-auto=update` to silence the error — it will silently diverge the schema from Flyway's version history.

### Category 7 — Spring Security / Keycloak JWT Configuration Failure
**Signal:** `APPLICATION FAILED TO START` with `NoResourceServerConfigured`, `InvalidBearerTokenException` at startup, `IllegalStateException: Cannot configure JwtDecoder`, or `issuer-uri` connection refused

This occurs when the application starts with an active Keycloak resource server configuration but the Keycloak instance is unreachable or the OIDC discovery URL is wrong.

Fix sequence:
1. Check `application.yml` for `spring.security.oauth2.resourceserver.jwt.issuer-uri` — confirm the URL matches the Keycloak realm URL for the active profile.
2. For local: Keycloak must be running (`docker compose up keycloak`). The issuer URI should be `http://localhost:8080/realms/<realm>`.
3. For tests: Spring Security will attempt to contact the issuer URI at startup. Either configure a mock JWT decoder in `application-test.yml` (`spring.security.oauth2.resourceserver.jwt.jwk-set-uri` pointing to a WireMock stub) or use `@AutoConfigureMockMvc` with `@WithMockUser` and disable JWT validation for the test profile.
4. Confirm the realm name in the URI matches the imported Keycloak realm (`infrastructure/keycloak/`).

## Steps

1. Run `mvn verify -pl backend -am` and capture the full output.
2. If this is a first-pass failure after a code change, run `mvn clean verify -pl backend -am` **once** to rule out stale `.class` files before categorising. Do not repeat the clean step on subsequent retries — it adds significant time without diagnostic value after the first pass.
3. Identify the **first** `[ERROR]` line — Maven cascades errors, so the root cause is almost always the first one.
4. Match to a category above.
5. Apply the fix.
6. Run `mvn verify -pl backend -am` (or the narrowest scope that covers the failure) to confirm.
7. If the build passes, produce the output contract below.

## Output Contract

```
## Java Build Resolver Report

Error category: <Category 1–7 or "composite">
Root cause: <one sentence>
Affected file(s): <list>
Fix applied: <description of the change>
Verification command: <mvn command run>
Verification output: <first relevant line(s) confirming BUILD SUCCESS or the remaining error>
Result: PASS / STILL FAILING
Residual risk: <any follow-on risk or "none">
```

## Guardrails
- Do not edit a Flyway migration file that has already been applied to any environment — always add a new migration.
- Do not delete a failing test to clear a compilation error.
- Do not pin a dependency version without first understanding which transitive brought the conflict — a blind pin can hide a deeper incompatibility.
- Run the narrowest possible Maven scope to verify (e.g. `-pl backend -am`) before running the full `mvn verify`.
- Do not run `mvn clean` more than once per failure investigation — clean rebuilds are expensive on a monolith and add no diagnostic value after the first pass.
