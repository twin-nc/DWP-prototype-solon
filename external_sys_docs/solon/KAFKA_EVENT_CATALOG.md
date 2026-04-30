# Solon-Tax Kafka Event Catalog

> Generated: 2026-04-28  
> Source: codebase exploration of `C:\Work\icarus`

---

## Infrastructure

| Property | Value |
|---|---|
| **Broker (deployed)** | `solon-kafka-controller-headless.<env>-solon-kafka:9092` |
| **Broker (local)** | `localhost:9092` |
| **Serialization** | Plain JSON via Jackson (no Avro / Protobuf) |
| **Error handling** | All consumers auto-route failures to `dead-letter-topic` via `@IcarusKafkaListener` |
| **Delivery guarantee** | Transactional outbox pattern (`MessageOutboxProcessorServiceImpl`) |
| **Topic namespaces** | `irm.*` (IRM environment), `iss.*` (ISS environment) — structurally identical |

---

## Base Event Envelope

All events extend `IcarusEvent`. Every Kafka message payload includes these base fields:

```json
{
  "id": "<uuid>",
  "timestamp": "<ISO-8601 instant>",
  "producer": "<service name>",
  "user": "<username>",
  "contextToken": "<JWT or session token>"
}
```

Domain-specific fields are added on top of this base.

---

## 1. Case Status Changes

| Topic | Event Class | Key Fields |
|---|---|---|
| `irm.process-management.process-flow-status-changed` | `ProcessFlowStatusChangedEvent` | `processFlowId`, `entityStatusCL`, `entityCL`, `sourceEntityId`, `processInstanceId` |
| `irm.suppression-management.suppression-status-changed` | `SuppressionStatusChangedEvent` | `SuppressionDomain suppressionDomain` |
| `irm.case-management.entity-version` | `EntityVersionEvent` | `changeType`, `objectType`, `objectId`, `object`, `entityStatusCl` |

**Notes:**
- `irm.suppression-management.suppression-status-changed` is consumed by **three separate services**: `taxpayer-accounting-custom`, `case-management-custom`, and `process-management-custom`.
- The full case state machine (PENDING → IN_PROGRESS → COMPLETED / CANCELED / SUSPENDED) is implemented in `revenue-management-be-custom/case-management-custom/.../CaseStatusTransitionPlugInService.java`.

---

## 2. Task Completions

Task completions do not have a single dedicated topic. They are signalled back into the BPMN engine via signal commands:

| Topic | Event Class | Key Fields | Notes |
|---|---|---|---|
| `irm.*.signal-process-instance` | `SignalProcessInstanceCommand` | `processInstanceId`, `signalName`, `variables` | Primary mechanism across all modules |
| `irm.taxpayer-accounting.signal-process-flow` | `SignalProcessFlowCommand` | — | Signals a process flow node |
| `irm.taxpayer-accounting.signal-bill-segment` | `SignalBillSegmentCommand` | — | Signals a bill segment node |

Human task completion goes through REST first, then internally produces a `SignalProcessInstanceCommand`.

---

## 3. Payment Events

| Topic | Direction | Event Class | Key Fields |
|---|---|---|---|
| `irm.taxpayer-accounting.balance-updated` | Outbound | `BalanceUpdatedEvent` | `amount (BigDecimal)`, `obligationActionId`, `atLeastOneDebitNotCanceled`, `allocationEnabled` |
| `irm.taxpayer-accounting.payment-hint-updated` | Outbound | `PaymentHintUpdatedEvent` | `paymentHintId`, `obligationActionId` |
| `irm.taxpayer-accounting.trigger-allocation` | Outbound | `TriggerAllocationCommand` | `obligationActionId` |
| `irm.taxpayer-accounting.ftgl-create` | Outbound | `FTGLCreateCommand` | `ftId`, `accountingRuleType`, `allocationLinkAmount` |
| `irm.adapter-allocation.adjust-allocation-links` | Outbound | `AdjustAllocationLinksCommand` | `linksToCreate`, `linkIdsToDelete`, `obligationActionId` |
| `irm.etl-payment-file-validation.close-and-create-exception` | Outbound (batch) | `CloseAndCreateExceptionCommand` | `sourceEntityId`, `entityCL`, `exceptions`, `exceptionTypeCodesToClose` |
| `irm.etl-payment-upload.close-and-create-exception` | Outbound (batch) | `CloseAndCreateExceptionCommand` | same |
| `irm.taxpayer-accounting-etl.message-created` | ETL → TA | `IncomingMessageCommand` | `messageId`, `messageTypeCL` |

---

## 4. Process Node Transitions (BPMN Engine Commands)

Every BPMN service task sends a command to an `irm.bpmn-engine.*` inbound topic. The process engine bridge also publishes full engine state to outbound topics.

### Inbound BPMN Engine Command Topics

| Topic | Event Class | Key Fields |
|---|---|---|
| `irm.bpmn-engine.case-creation` | — | Starts a new case |
| `irm.bpmn-engine.suspend-case-activity` | `SuspendCaseActivityCommand` | `processInstanceId`, `caseId`, `signalName` |
| `irm.bpmn-engine.resume-case-activity` | `ResumeCaseActivityCommand` | `processInstanceId`, `caseId`, `signalName` |
| `irm.bpmn-engine.configure-case-scope` | `ConfigureCaseScopeEvent` | `caseId`, `signalName`, `billSegmentType`, `liabilityId` |
| `irm.bpmn-engine.create-start-child-case` | `CreateStartChildCaseCommand` | `parentCaseId`, `childCaseTypeCode` |
| `irm.bpmn-engine.complete-child-case` | `CompleteChildCaseCommand` | `childCaseId`, `parentSignalName` |
| `irm.bpmn-engine.verify-timeliness` | `VerifyTimelinessCommand` | — |
| `irm.bpmn-engine.set-milestone-due-date` | `SetMilestoneDueDateCommand` | — |
| `irm.bpmn-engine.trigger.debt.recovery.process` | `TriggerDebtRecoveryProcessEvent` | `caseId`, `parentCaseId`, `parentCaseSignalName` |
| `irm.bpmn-engine.send-data-to-collection-agency` | `SendSummaryToCollectionAgencyEvent` | `caseId`, `handoverAmount`, `processInstanceId`, `signalName` |
| `irm.bpmn-engine.receive-data-for-collection-agency` | — | — |
| `irm.bpmn-engine.mark-case-debt-for-handover-as-handed-over` | — | — |
| `irm.bpmn-engine.mark-case-debt-for-handover-as-canceled` | — | — |
| `irm.bpmn-engine.bill-segment-service-freeze-command` | `FreezeBillSegmentCommand` | `billSegmentId`, `signalName` |
| `irm.bpmn-engine.create-bill-segment-for-case` | `BillSegmentCreationEvent` | `processInstanceId`, `caseId`, `signalName`, `billSegmentType`, `fee` |
| `irm.bpmn-engine.overpayment-process-store-data-area-command` | — | — |
| `irm.bpmn-engine.transition-overpayment-process-status` | — | — |
| `irm.bpmn-engine.update-bill-segment-direct-deposit-status` | — | — |
| `irm.bpmn-engine.push-notification-redis` | `PushNotificationCommand` | `entityCL`, `entityId`, `entityStatusCL`, `titleKey`, `messageKey`, `notificationTypeCode` |
| `irm.bpmn-engine.check-tax-return-late-filer` | `CheckTaxReturnLateFilerEvent` | `obligationActionId`, `taxReturnId` |
| `irm.bpmn-engine.screen-risk` | — | — |
| `irm.bpmn-engine.transition-form-status` | — | — |

### Outbound Process Engine Bridge Topics (Camunda 7)

| Topic | Content |
|---|---|
| `irm.icarus-camunda-process-definition` | Process definition metadata |
| `irm.icarus-camunda-process-instance` | Full process instance state |
| `irm.icarus-camunda-element-instance` | Per-element (task/gateway) lifecycle events |
| `irm.icarus-camunda-variable` | Variable changes |
| `irm.icarus-camunda-incident` | Incident events |
| `irm.icarus-camunda-history-event` | Historical audit events |

### Outbound Process Engine Bridge Topics (Camunda 8 / Amplio / Zeebe)

| Topic | Content |
|---|---|
| `irm.icarus-amplio-process` | Process definition metadata |
| `irm.icarus-amplio-process-instance` | Full process instance state |
| `irm.icarus-amplio-variable` | Variable changes |
| `irm.icarus-amplio-incident` | Incident events |
| `irm.icarus-amplio-error` | Error events |

---

## 5. Suppression Activations

| Topic | Direction | Event Class | Key Fields |
|---|---|---|---|
| `irm.suppression-management-etl.activate-suppression` | ETL → Suppression Mgmt | `ActivateSuppressionCommand` | `suppressionId` |
| `irm.suppression-management-etl.release-suppression` | ETL → Suppression Mgmt | `ReleaseSuppressionCommand` | `suppressionId`, `entityType`, `entityId` |
| `irm.bpmn-engine.create-suppression` | BPMN → Suppression Mgmt | `CreateSuppressionCommand` | `Suppression suppression` |
| `irm.bpmn-engine.release-suppression` | BPMN → Suppression Mgmt | — | — |
| `irm.suppression-management.suppression-status-changed` | Suppression Mgmt → consumers | `SuppressionStatusChangedEvent` | `SuppressionDomain suppressionDomain` |
| `irm.suppression-management.entity-version` | Suppression Mgmt → consumers | `EntityVersionEvent` | `changeType`, `objectType`, `objectId`, `object`, `entityStatusCl` |
| `irm.suppression-management.audit` | Suppression Mgmt → audit | — | Audit trail |

---

## 6. Breach Detections

No dedicated "breach" topic exists. Compliance violations and breach detections flow through:

| Topic | Direction | Event Class | Key Fields |
|---|---|---|---|
| `irm.compliance-monitor.start-process` | ETL → Compliance Monitor | `StartProcessCommand` | `processTypeCode`, `entityId`, `Map<String,Object> processVariables` |
| `irm.case-management-etl.start-case-candidate-for-intervention` | ETL → Case Mgmt | `StartCaseForCandidateCommand` | `caseTypeCode`, `relatedEntities`, `candidateId` |
| `irm.case-management.candidate-updated` | Case Mgmt → consumers | `CandidateUpdatedEvent` | `caseId`, `candidateId`, `status` |

---

## 7. Other Domain Events

### Registration

| Topic | Event Class | Key Fields |
|---|---|---|
| `irm.registration.entity-version` | `EntityVersionEvent` | `changeType`, `objectType`, `objectId`, `object`, `entityStatusCl` |
| `irm.registration.obligation-cancelled` | `ObligationCancelledEvent` | `ObligationDomain obligation` |
| `irm.registration.obligation-action-cancelled` | `ObligationActionCancelledEvent` | `obligationActionId`, `ObligationDomain obligation` |
| `irm.registration.signal-process-instance` | `SignalProcessInstanceCommand` | `processInstanceId`, `signalName`, `variables` |
| `irm.registration.user-right` | — | User right modifications |
| `irm.registration.right` | — | Party role relationship changes |
| `irm.registration.audit` | — | Audit trail |

### UI Configuration / Forms

| Topic | Event Class | Key Fields |
|---|---|---|
| `irm.ui-configuration.form-posted` | `FormPostedEvent` | `formId`, `obligationActionId` |
| `irm.ui-configuration.form-reversed` | `FormReversedEvent` | `formId`, `obligationActionId` |
| `irm.ui-configuration.entity-version` | `EntityVersionEvent` | — |
| `irm.ui-configuration.signal-process-instance` | `SignalProcessInstanceCommand` | — |
| `irm.ui-configuration.close-and-create-exception` | `CloseAndCreateExceptionCommand` | — |
| `irm.ui-configuration.audit` | — | Audit trail |

### Reference Data & Translation

| Topic | Event Class | Key Fields |
|---|---|---|
| `irm.reference-data.codelist-changed` | `CodeListChangedEvent` | `codeListCode`, `categoryCodes` |
| `irm.reference-data.clear-refdata-caches` | — | Cache invalidation |
| `irm.translation-managment.translatable-item-changed` | `TranslatableItemChangedEvent` | `namespaceCL`, `itemKey` |

### Audit & Authorization

| Topic | Event Class | Key Fields |
|---|---|---|
| `irm.audit.entity-changed` | `EntityChangedEvent` | `changeType`, `objectType`, `objectId`, `oldVersion`, `newVersion` |
| `oags-update-topic` | `OagsUpdateEvent` | `objectType`, `objectId`, `author`, `changeTimestamp` |

### Debt Recovery

| Topic | Event Class | Key Fields |
|---|---|---|
| `irm.taxpayer-accounting-etl.start-case` | `StartCaseCommand` | `caseTypeCode`, `relatedEntities`, `partyRoleId` |
| `irm.taxpayer-accounting-etl.start-case-handover-debt` | `StartCaseForHandoverDebtCommand` | `caseTypeCode`, `handoverAmountThreshold`, `partyRoleId` |
| `irm.taxpayer-accounting-etl.start-overpayment-process` | `StartOverpaymentProcessCommand` | `overpaymentProcessTypeCode`, `liabilityId` |
| `irm.taxpayer-accounting.liability-scope-updated` | `LiabilityScopeUpdatedEvent` | `liabilityId`, `obligationActionId` |
| `irm.taxpayer-accounting.bill-created` | `BillCreatedEvent` | `BillDomain bill` |

### Dead Letter Queue

| Topic | Consumer | Purpose |
|---|---|---|
| `dead-letter-topic` | `KafkaErrorHandlingEventConsumer` (exception-management) | All failed consumer messages are routed here automatically |

---

## Key Source Locations

| Concern | Location |
|---|---|
| Topic constants (config) | `<service>/src/main/resources/application-base.yml` and `application-custom.yml` |
| Base event class | `revenue-management-be/shared/event/.../IcarusEvent.java` |
| Event DTOs | `revenue-management-be/shared/event/` and each domain's `-dto` module |
| Generic Kafka producer | `revenue-management-be/shared/kafka/.../KafkaMessageProducerImpl.java` |
| Abstract producer base | `revenue-management-be/shared/kafka/.../AbstractKafkaIcarusProducer.java` |
| Abstract consumer base | `revenue-management-be/shared/kafka/.../AbstractKafkaIcarusConsumer.java` |
| `@IcarusKafkaListener` annotation | `revenue-management-be/shared/kafka/.../listener/IcarusKafkaListener.java` |
| Outbox processor | `revenue-management-be/shared/message-outbox/.../MessageOutboxProcessorServiceImpl.java` |
| BPMN bridge topic config | `revenue-management-adapters/process-engine/process-engine-bridge/.../application-irm.yml` |
| Zeebe/Amplio consumers | `core-bpm/core-bpm-spring-boot-4/core-bpm-zeebe-event-starter/.../consumer/` |
| ETL producers | `revenue-management-etl/<domain>/src/main/java/.../QuickStartCommandProducer.java` |
