# DCMS Architecture Diagrams

Each architecture diagram is maintained in two formats:
- Mermaid source (`.mmd`) for text-based review and versioning.
- draw.io source (`.drawio`) for visual editing in diagrams.net.

| Mermaid | draw.io | What it shows |
|---|---|---|
| [01-system-context.mmd](01-system-context.mmd) | [01-system-context.drawio](01-system-context.drawio) | C4 context - DCMS and all external actors and systems it interacts with |
| [02-domain-module-map.mmd](02-domain-module-map.mmd) | [02-domain-module-map.drawio](02-domain-module-map.drawio) | Backend domain packages, infrastructure packages, ownership boundaries, and scaffold status |
| [03-process-engine-layers.mmd](03-process-engine-layers.mmd) | [03-process-engine-layers.drawio](03-process-engine-layers.drawio) | Three-layer delegate command pattern (ADR-003): domain handlers -> shared port -> Flowable infrastructure |
| [04-debt-lifecycle.mmd](04-debt-lifecycle.mmd) | [04-debt-lifecycle.drawio](04-debt-lifecycle.drawio) | Debt journey from ingest through initiation, segmentation, treatment events, and terminal outcomes (ADR-001, ADR-002) |
| [05-segmentation-configurability.mmd](05-segmentation-configurability.mmd) | [05-segmentation-configurability.drawio](05-segmentation-configurability.drawio) | Segmentation via DMN, initial segment taxonomy, and two-tier configurability model (ADR-004) |
| [06-infrastructure-and-auth.mmd](06-infrastructure-and-auth.mmd) | [06-infrastructure-and-auth.drawio](06-infrastructure-and-auth.drawio) | Local vs dev topology, CI/CD pipeline, and shared Keycloak auth flow |
| n/a | [07-hybrid-architecture-overview.drawio](07-hybrid-architecture-overview.drawio) | Consolidated architecture view showing how event-driven workflow orchestration and object-oriented domain modules interact inside the monolith |
| n/a | [08-runtime-sequence-flows.drawio](08-runtime-sequence-flows.drawio) | Three clean runtime flow lanes (case initiation, benefit ceased, payment missed) with explicit left-to-right arrow routing |
| n/a | [customer-journey-with-roles.drawio](customer-journey-with-roles.drawio) | Customer journey end-to-end with agent roles mapped at each stage; source: FEAT5 candidate pack (development-plan.md); last verified: 2026-04-24 |
| n/a | [vulnerable-track-customer-journey.drawio](vulnerable-track-customer-journey.drawio) | Customer journey variant for vulnerable customers including divergence points and mandatory review steps; source: FEAT5 candidate pack (development-plan.md); last verified: 2026-04-24 |

## Scaffold status colour key (diagram 02)

| Colour | Meaning |
|---|---|
| Green | Scaffolded |
| Amber | Partially implemented |
| Red | Not yet scaffolded |
| Blue | Infrastructure / external |
