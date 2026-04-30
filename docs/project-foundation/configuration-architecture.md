# Configuration Architecture

Source: [configuration-architecture.drawio](./configuration-architecture.drawio)

This document rewrites the diagram into agent-readable text.

## System

`Collection System - Configuration Architecture`

## Core Structure

The configuration architecture is organized into three nested layers:

1. `Process Management`
2. `Tasks & Rules`
3. `Foundations`

Nesting relationship:

- `Process Management` contains `Tasks & Rules`
- `Tasks & Rules` contains `Foundations`

Notes from the diagram:

- `Process Management` is `nestable` and supports `process of processes`
- `Tasks & Rules` is `nestable` and supports `task of tasks`
- `Foundations` is `nested` inside `Tasks & Rules`

## Configurators

Three configurator components are shown on the left side of the architecture:

1. `Process Configurator`
   - configures `Process Management`
2. `Task Configurator`
   - configures `Tasks & Rules`
3. `Foundations Configurator`
   - configures `Foundations`

## Policy Bundles

The diagram shows three cross-cutting policy bundles that intersect the layered architecture:

1. `Fraud Policy Bundle`
2. `Repay Plan Policy Bundle`
3. `Vulnerability Policy Bundle`

Interpretation:

- These bundles are represented as cross-sections rather than isolated layers.
- They appear to span or influence multiple architectural layers.

## Agent-Friendly Representation

```yaml
system: Collection System - Configuration Architecture

layers:
  - name: Process Management
    type: architecture-layer
    properties:
      - nestable
      - process-of-processes
    contains:
      - Tasks & Rules

  - name: Tasks & Rules
    type: architecture-layer
    properties:
      - nestable
      - task-of-tasks
    contained_by:
      - Process Management
    contains:
      - Foundations

  - name: Foundations
    type: architecture-layer
    properties:
      - nested
    contained_by:
      - Tasks & Rules

configurators:
  - name: Process Configurator
    configures:
      - Process Management

  - name: Task Configurator
    configures:
      - Tasks & Rules

  - name: Foundations Configurator
    configures:
      - Foundations

policy_bundles:
  - name: Fraud Policy Bundle
    type: cross-cutting

  - name: Repay Plan Policy Bundle
    type: cross-cutting

  - name: Vulnerability Policy Bundle
    type: cross-cutting
```

## Legend Translation

- `Configurator`: component used to define or maintain a layer
- `Architecture Layer`: structural layer in the configuration model
- `Policy Bundle (cross-section)`: policy grouping that cuts across one or more layers

## Short Summary

The architecture separates configuration into nested layers from high-level process management down to foundations. Each layer has a dedicated configurator, while policy bundles act as cross-cutting concerns that overlay the layered model.
