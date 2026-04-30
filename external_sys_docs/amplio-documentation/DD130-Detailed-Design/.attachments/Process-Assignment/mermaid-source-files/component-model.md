:::mermaid
flowchart LR
    process-assignment-rest("amplio.libraries:process-assignment-rest"):::highlight
    process-assignment-service("amplio.libraries:process-assignment-service"):::highlight
    process-assignment-rest .-> assignment-rest["amplio.libraries:assignment rest"]
    process-assignment-rest .-> process-assignment-service
    process-assignment-rest .-> foundation.libraries:table-rest["foundation.libraries:table rest"]
    process-assignment-service .-> process-engine["amplio.libraries:process engine"]
    process-assignment-service .-> process-engine-api["amplio.libraries:process-engine api"]
    process-assignment-service .-> assignment-service["amplio.libraries:process-assignment service"]
    process-assignment-service .-> amplio-essentials-common["amplio.essentials:common"]
    classDef highlight fill: #f96, stroke: #f50, color: #000
:::