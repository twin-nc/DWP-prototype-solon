:::mermaid
flowchart TB
    id1["Assignment API"]:::highlight
    id2["Assignment"]:::highlight
    id3["Resources"]
    id4["Users & Groups"]
    id1 -. " Manages " .-> id2
    id2 <-->|" Links to "| id3
    id2 <-->|" Links to "| id4
    Implementation[" Downstream "] -. " Uses " .-> id1
    classDef highlight fill: #f96, stroke: #f50, color: #000
:::
