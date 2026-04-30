:::mermaid
flowchart TB
    id1["Process Assignment API"]:::highlight
    id2["Process Assignment"]:::highlight
    id3["Processes"]
    id4["Users & Groups"]
    id1 <-->|" Manage assignments "| id2
    id2 <-->|" Links processes to "| id3
    id2 <-->|" Links users/groups to "| id4
    Users["Business Users"] -->|" Use assigned processes "| id1
    Admin["Administrators"] -->|" Configure assignments "| id1
    Events["System Events"] -->|" Trigger automatic assignments "| id2
    classDef highlight fill: #f96, stroke: #f50, color: #000
:::