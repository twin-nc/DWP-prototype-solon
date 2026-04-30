:::mermaid
flowchart LR
    common("amplio.essentials:common")
    rest("amplio.libraries:assignment-rest"):::highlight
    service("amplio.ibraries:assignment-service"):::highlight
    rest .-> service
    service .-> common
    classDef highlight fill: #f96, stroke: #f50, color: #000
:::
