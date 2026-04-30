# Configuration Tool Architecture

Source: [configuration-tool-architecture.drawio](./configuration-tool-architecture.drawio)

This document formalizes the whiteboard sketch into a clean architecture description.

## Purpose

The architecture introduces a `Configuration Tool` as a dedicated configuration layer around `Solon Tax`.

The `Configuration Tool` replaces the temporary whiteboard label `WIZARD`. Going forward, the correct name is:

- `Configuration Tool`

The architecture also makes room for `Agentic AI`, which is not a business-facing runtime system in itself, but a developer and configurator toolbox used to build, reconfigure, and evolve the `Configuration Tool`.

## Architecture Overview

At a high level, the solution consists of four main parts:

1. `Solon Tax`
   - the core debt collection platform
   - executes the operational transactions and flows
2. `Configuration Tool`
   - the configuration layer on top of and around `Solon Tax`
   - used to define, adjust, and govern how `Solon Tax` is configured
3. `Agentic AI`
   - the toolbox used by developers and configurators to help build and reconfigure the `Configuration Tool`
4. `Case Worker Workspace`
   - the frontline runtime workspace for case workers
   - delivered as part of the `Business Control & Experience Layer`, not as a direct Solon Tax UI
   - consumes services from both `Solon Tax` APIs and the layer's own Core Services (queue, scripting, vulnerability enforcement, I&E)

## Core Concept

`Solon Tax` remains the operational system of record and execution engine.

On the left and right side of `Solon Tax`, the original sketch indicates business transactions and flows moving through the platform from left to right. These can be understood as:

- inbound transactions, events, cases, or requests entering `Solon Tax`
- operational processing and orchestration inside `Solon Tax`
- outbound flows, outcomes, status changes, tasks, or downstream actions leaving `Solon Tax`

The `Configuration Tool` does not replace `Solon Tax`. Instead, it provides a structured way for business and technical users to configure and evolve the behavior surrounding the platform.

## Component Model

### 1. Solon Tax

`Solon Tax` is the central platform component.

Responsibilities:

- manage debt collection processes and operational logic
- process transactions and flows
- expose the business capabilities that need to be configured or supported
- serve as the runtime platform used by case workers

### 2. Configuration Tool

The `Configuration Tool` is the dedicated configuration layer around `Solon Tax`.

Responsibilities:

- provide controlled configuration capabilities for business-facing setup
- expose structured interfaces for analysts and business developers
- act as the target system that developers and configurators can rebuild or reconfigure
- translate business configuration intent into concrete platform configuration for `Solon Tax`

Expected scope:

- process and rule configuration
- task and workflow setup
- policy, reference, and operational configuration
- environment-safe change management and governance

### 3. Agentic AI

`Agentic AI` is the top-layer toolbox used by the delivery team.

Responsibilities:

- assist developers in building and evolving the `Configuration Tool`
- assist configurators in reconfiguring or extending the `Configuration Tool`
- accelerate analysis, documentation, generation, validation, and change design

Important boundary:

- `Agentic AI` is not the primary day-to-day interface for business users
- `Agentic AI` supports the people who build and maintain the `Configuration Tool`

### 4. Case Worker Workspace

The `Case Worker Workspace` is the frontline runtime workspace for operational users, delivered within the `Business Control & Experience Layer`.

Responsibilities:

- provide a guided, policy-enforced workspace for case workers
- expose account and case views, ID&V, vulnerability and restriction flags, guided scripting, arrangement capture, permitted actions, and audit-visible notes
- consume `Solon Tax` APIs for live case, task, suppression, and financial data
- consume layer Core Services for queue context, guided scripting, vulnerability enforcement, and I&E capture

Important boundaries:

- case workers do not use `Agentic AI`
- case workers do not access the `Configuration Tool` workspaces directly
- the `Case Worker Workspace` sits within the shared product shell alongside the `Operations Workspace` and configuration tooling, separated by role-based access control

## User Access Model

The sketch distinguishes different user groups and the systems they can access.

### Developers

Developers have access to:

- `Agentic AI`
- `Configuration Tool`

Developers use `Agentic AI` to help design, rebuild, and extend the `Configuration Tool`.

### Configurators

Configurators have access to:

- `Agentic AI`
- `Configuration Tool`

Configurators use `Agentic AI` as an accelerator for reconfiguration and use the `Configuration Tool` as the controlled configuration surface.

### Business Developers

Business developers have access to:

- `Configuration Tool`

They use it to manage business-relevant configuration without needing direct access to the AI toolbox.

### Analysts

Analysts have access to:

- `Configuration Tool`

They use it to inspect, define, and refine configuration artifacts and business behavior.

### Case Workers

Case workers have access to:

- `Case Worker Workspace`

They work within the guided runtime workspace, which composes data from `Solon Tax` and layer Core Services under a policy-enforced, scripted interface.

## Interaction Model

The intended primary interactions are:

1. business transactions and flows pass through `Solon Tax`
2. the `Configuration Tool` configures and governs how the platform is set up
3. `Agentic AI` helps developers and configurators evolve the `Configuration Tool`
4. case workers operate through the `Case Worker Workspace`, which composes `Solon Tax` data and layer services under guided, policy-enforced journeys

## Design Principles

- `Solon Tax` remains the operational core
- `Configuration Tool` is the formal configuration layer
- `Agentic AI` is a build-and-change accelerator, not the main business application
- `Case Worker Workspace` is role-separated from the configuration experience within the shared product shell
- user access is segmented by role and responsibility

## Agent-Friendly Representation

```yaml
system: Solon Tax Configuration Tool Architecture

components:
  - name: Agentic AI
    type: enablement-toolbox
    purpose: support developers and configurators in building and reconfiguring the Configuration Tool

  - name: Configuration Tool
    type: configuration-layer
    formerly_named: WIZARD
    purpose: configure and govern business and technical setup around Solon Tax

  - name: Solon Tax
    type: core-platform
    purpose: execute debt collection transactions, flows, and operational processes

  - name: Case Worker Workspace
    type: runtime-workspace
    purpose: frontline guided workspace within the Business Control & Experience Layer; composes Solon Tax data and layer Core Services

relations:
  - from: Agentic AI
    to: Configuration Tool
    relation: used_to_build_and_reconfigure

  - from: Configuration Tool
    to: Solon Tax
    relation: configures_and_governs

  - from: Case Worker Workspace
    to: Solon Tax
    relation: consumes_via_rest_api

  - from: Case Worker Workspace
    to: Business Control & Experience Layer Core Services
    relation: consumes_for_scripting_queue_vulnerability_ie

  - from: External Transactions and Flows
    to: Solon Tax
    relation: inbound_processing

  - from: Solon Tax
    to: External Transactions and Flows
    relation: outbound_processing

roles:
  - role: Developers
    access:
      - Agentic AI
      - Configuration Tool

  - role: Configurators
    access:
      - Agentic AI
      - Configuration Tool

  - role: Business Developers
    access:
      - Configuration Tool

  - role: Analysts
    access:
      - Configuration Tool

  - role: Case Workers
    access:
      - Case Worker Workspace
```

## Short Summary

The target architecture centers on `Solon Tax` as the runtime platform, wraps it with a `Business Control & Experience Layer` that owns configuration tooling, operational workspaces, Core Services, and persistent storage, uses `Agentic AI` as a toolbox for building and reconfiguring the layer, and provides a `Case Worker Workspace` as the frontline runtime surface within that layer — role-separated from configuration by RBAC, not by physical product boundary.
