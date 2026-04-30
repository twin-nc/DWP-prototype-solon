# Drools Runtime API — Reference
**Version:** 2.1.0  
**Base path:** `/drools-runtime` (routed via Solon API Gateway)  
**Auth:** Bearer JWT from Keycloak (enforced at API gateway level)

---

## Endpoints

| Method | Path | Summary |
|---|---|---|
| POST | `/execute-stateless/{containerId}` | Execute a Drools stateless session against a named KIE container |

---

### POST `/execute-stateless/{containerId}`

Executes a Drools stateless session against the requested `containerId`.

**Path parameter:**

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `containerId` | string | Yes | Identifies the KIE container (rule set / knowledge base) to execute against |

**Request body:** `application/json` — string  
JSON representation of `BatchExecutionCommandImpl` from `org.drools:drools-commands`, serialized by `XStreamJSon:newJSonMarshaller` from `org.drools:drools-xml-support`. Classes of serialized facts must be present in the KIE container's classloader.

**Response:** `application/json` — string  
JSON representation of `ExecutionResultImpl` from `org.drools:drools-commands`, serialized by the same marshaller.

---

## Usage in the additional layer

The Task Configurator's Drools Rule Builder calls this endpoint to execute business rule evaluations — for example, affordability calculations, risk scoring, or eligibility decisions — against a pre-deployed KIE container in Solon.

The `containerId` maps to a specific rule set deployed in the Drools runtime (e.g. a container for debt affordability rules, or a container for vulnerability risk scoring). Each rule set is deployed and versioned independently in Solon's Drools runtime.

**Example call (affordability check):**
```
POST /drools-runtime/execute-stateless/dwp-affordability-rules-1.0
Content-Type: application/json
Authorization: Bearer {jwt}

"{...BatchExecutionCommandImpl JSON...}"
```

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Keycloak JWT — enforced at API gateway level |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Indicates request origin |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include description labels |
