# Skills Index

This folder is the canonical Claude Code skills pack for this repository.

| Skill | Purpose | Primary Agent | Invocation Boundary |
| --- | --- | --- | --- |
| `agent-boundary-escalator` | Decide whether work should stay with the current agent, hand off, or be escalated across authority or skill boundaries. | `delivery-designer` | Deciding ownership and handoff. |
| `api-contract-draft` | Turn an issue, slice, or integration need into a concrete API contract draft with operations, payloads, errors, and versioning notes. | `solution-architect` | Contract definition. |
| `breaking-change-impact-mapper` | Map downstream consumer, docs, tests, release, and operations impact for a breaking or consumer-sensitive change. | `solution-architect` | Mapping downstream impact of a breaking or sensitive change. |
| `change-classification-assistant` | Classify a proposed change and identify the implied governance, review, testing, and evidence burden. | `delivery-designer` | Classifying change impact. |
| `ci-gate-recommender` | Recommend CI checks, validation gates, and enforcement based on change risk and standards obligations. | `devops-release-engineer` | Recommending validation gates and CI checks. |
| `contract-versioning-advisor` | Determine whether a contract change is additive, breaking, deprecated, or behavior-altering and recommend a migration path. | `solution-architect` | Choosing a versioning and migration path for a contract change. |
| `create-domain-pack` | Turn a slice or workflow into a domain pack covering entities, states, invariants, validations, boundaries, and lifecycle rules. | `solution-architect` | Domain modeling. |
| `date-effective-policy-integrator` | Shape or review effective-dated policy selection so behavior is driven by governed policy data instead of hardcoded logic. | `domain-expert` | Effective-dated policy or rule integration. |
| `defect-triage-root-cause-classifier` | Classify a defect or incident into likely root-cause families to guide triage and remediation. | `test-designer` | Structured defect triage. |
| `design-review` | Critique a design or implementation approach for assumptions, failure modes, hidden coupling, and ownership risk. | `design-critic` | Critiquing a design or approach. |
| `deterministic-implementation-builder` | Guide implementation so identical inputs and governed context produce identical outputs and stable errors. | `backend-builder` | Implementation guidance for deterministic behavior. |
| `documentation-authority-resolver` | Resolve conflicts among standards, requirements, contracts, tests, code, and examples using the authority hierarchy. | `solution-architect` | Resolving documentation or artifact authority conflicts. |
| `error-envelope-normalizer` | Normalize caller-visible error semantics so codes, categories, statuses, and retry behavior stay stable. | `backend-builder` | Normalizing caller-visible errors. |
| `generate-domain-tests` | Turn domain rules, invariants, validations, and state transitions into concrete domain or unit test cases. | `test-builder` | Concrete domain or unit test generation. |
| `generate-e2e-scenarios` | Turn workflows, acceptance criteria, or test strategy inputs into concrete end-to-end scenario outlines across actors and boundaries. | `test-builder` | Workflow-level scenario generation across boundaries. |
| `immutable-records-guard` | Check or shape changes so append-only records remain immutable and corrections happen via amendments or supersession. | `db-designer` | Preserving append-only record and amendment semantics. |
| `issue-to-build-plan` | Translate a GitHub issue or work item into an actionable build plan with subtasks, reviews, tests, and evidence work. | `delivery-designer` | Execution planning. |
| `migration-safety-reviewer` | Review schema, data, and rollout migrations for rollback risk, cast safety, compatibility, sequencing, and legal-history safety. | `db-designer` | Reviewing migration and rollout safety. |
| `observability-evidence-separator` | Separate logs, metrics, and traces used for diagnosis from governed audit or release evidence. | `devops-release-engineer` | Separating observability from governed evidence. |
| `policy-bundle-change-reviewer` | Review effective-dated policy or rule-bundle changes for applicability, legal references, versioning, and edge-date coverage. | `domain-expert` | Reviewing a policy-bundle change. |
| `reconciliation-flow-reviewer` | Review retry, divergence, idempotency, and reconciliation behavior across system boundaries. | `integration-protocol-specialist` | Reviewing reconciliation, retry, and divergence handling. |
| `release-evidence-pack-builder` | Assemble the release evidence pack from trace links, tests, parity checks, deviations, and signoff inputs. | `devops-release-engineer` | Assembling the evidence bundle. |
| `release-readiness-gate` | Synthesize compliance, testing, migration, operational readiness, and open gaps into a release recommendation. | `devops-release-engineer` | Release decision synthesis. |
| `requirements-trace-map-updater` | Update or draft requirement-to-contract, rule, test, and evidence mappings for a feature or release slice. | `business-analyst` | Updating the trace-map artifact itself. |
| `review-pr` | Review a pull request or code diff for bugs, regressions, missing tests, standards risks, and downstream impact. | `code-reviewer` | Reviewing an implemented code change. |
| `rule-coverage-analyzer` | Check whether date-effective, policy-driven, and rule-based behavior is adequately covered by tests. | `test-designer` | Analyzing rule and policy coverage. |
| `seed-data-builder` | Produce deterministic seed-data plans or examples aligned to acceptance criteria, domain rules, and test scenarios. | `test-builder` | Stable fixture or seed-data design. |
| `sensitive-data-redaction-checker` | Scan logs, traces, prompts, fixtures, docs, or proposals for unsafe exposure of sensitive or restricted data. | `devops-release-engineer` | Checking for unsafe data exposure. |
| `standards-governance-reviewer` | Review a plan, design, change, or exception against the standards pack and identify remediation or signoff needs. | `solution-architect` | Standards compliance and exception governance. |
| `state-precedence-reviewer` | Review whether latest, active, current, superseded, derived, and historical state are resolved consistently and safely. | `solution-architect` | Reviewing state resolution rules. |
| `test-plan` | Turn a slice or change into a structured test plan across the right test layers, data needs, and evidence obligations. | `test-designer` | Verification planning. |
| `traceability-and-evidence-enforcer` | Check that a change maintains a trace chain from requirements through behavior, tests, and evidence artifacts. | `delivery-designer` | Traceability and evidence linkage. |
| `update-docs-for-change` | Identify and draft the documentation, trace, release-artifact, and operational doc updates caused by a change. | `business-analyst` | Identifying and drafting documentation updates caused by a change. |
| `write-acceptance-criteria` | Turn a work item into clear, testable acceptance criteria with edge cases, negative cases, assumptions, and open questions. | `business-analyst` | Defining target behavior. |
| `remote-deployment-readiness` | Verify all prerequisites are in place before deploying to a remote environment: log access, environment spec, local/remote parity, GitOps sync. | `devops-release-engineer` | Remote deployment pre-flight and remote debugging preparation. |

> **Note on `domain-expert`:** Some skills list `domain-expert` as primary agent. This is a placeholder for any project-specific domain specialist role (tax expert, legal expert, compliance expert, etc.). Name it per your project's domain requirements. If the project has no domain-expert role, reassign these skills to `business-analyst` or `solution-architect` as appropriate.

> **Note on `date-effective-policy-integrator` and `policy-bundle-change-reviewer`:** These are most relevant for projects with effective-dated rule sets or jurisdiction-specific policy bundles. For projects without date-effective rules, mark them as N/A in your skills audit rather than removing them — a future feature phase may need them.
