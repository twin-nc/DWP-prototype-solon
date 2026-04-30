---
id: STD-INT-003
title: File Transfer Standard — FTPS and PGP
status: Approved
owner: Integration Platform
applies_to: All services performing bulk file exchange with DWP or third parties
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - integration/integration-reliability-and-reconciliation.md
  - security/data-sensitivity-and-redaction.md
  - platform/evidence-immutability-and-replay.md
last_changed: 2026-04-16
---

## Purpose

Define mandatory requirements for file-based data exchange with DWP and third parties, covering FTPS protocol configuration, PGP encryption, checksum generation, file naming, and reconciliation. Derived from tender requirements INT07, INT12–INT26, INT23.

---

## Scope

This standard applies to:
- Batch extracts from the COTS system to the DWP Data Integration Platform (INT06)
- Inbound files from DWP to the COTS or integration adapters (INT24)
- Any file-based integration with third parties (DCA placements, bureau data feeds)

It does not apply to real-time REST API integrations (see STD-INT-001) or the COTS internal data store.

---

## FTPS Protocol Requirements (MUST)

All FTPS connections MUST:

1. Use **explicit passive mode** (INT13).
2. Use **TLS 1.2** minimum for the control and data channel (INT17).
3. Use a cipher suite from the approved list (INT20):
   - `TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384`
   - `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256`
   - `TLS_DHE_RSA_WITH_AES_256_GCM_SHA384`
   - `TLS_DHE_RSA_WITH_AES_128_GCM_SHA256`
   - `TLS_RSA_WITH_AES_256_GCM_SHA384`
   - `TLS_RSA_WITH_AES_128_GCM_SHA256`
4. Use a **SHA-2 signed certificate** from an approved Certificate Authority (INT18, INT19):
   - Approved CAs: DigiCert, Comodo/AddTrust, VeriSign/Symantec, GoDaddy, Thawte, QuoVadis, DWP Root CA, Entrust.
5. Use a **single external IP address** with no Network Address Translation (NAT) on the response path (INT14).
6. Place files in the **default folder** available upon connection (INT21).
7. Not use SSL Pinning (MOB11 — applies to any mobile-facing endpoint; consistent with FTPS clients too).

SFTP (SSH File Transfer Protocol) MAY be used as a fallback where FTPS is technically impossible (INT22) — document the reason in a deviation record.

---

## PGP Encryption Requirements (MUST)

### Outbound files (COTS/adapter → DWP)

1. All batch extract files MUST be **PGP-encrypted** before transfer (INT07d).
2. The PGP key used MUST be the DWP-provided public key. Key ID and fingerprint MUST be recorded in `docs/project-foundation/CI-SECRET-REGISTER.md`.
3. Files MUST NOT be stored in an unencrypted state at any point — neither in transit nor at rest on any Netcompany system (INT23).

### Inbound files (DWP → COTS/adapter)

1. Files received from DWP MUST be encrypted using PGP (INT23).
2. The integration adapter MUST verify the PGP signature before processing. Unsigned or unverifiable files MUST be rejected and logged with error code `DC-FTPS-0001`.
3. Decrypted content MUST be processed in-memory or in an encrypted temporary store. Plaintext MUST NOT be written to disk unencrypted.

### Key management

1. PGP private keys MUST be stored in the project secrets manager (not committed to the repository).
2. Key rotation must follow the CI secret register rotation policy (`docs/project-foundation/CI-SECRET-REGISTER.md`).
3. When DWP rotates their public key, both the old and new key MUST be accepted for a transition window agreed with DWP. Log key ID used for each file processed.

---

## Checksum and Integrity (MUST)

1. Every outbound batch extract file MUST be accompanied by a **SHA-256 checksum file** (INT07d).
2. Checksum file naming: `<filename>.sha256`.
3. The checksum file MUST be transferred alongside the data file in the same FTPS session.
4. For inbound files, the adapter MUST verify the SHA-256 checksum before handing off to the COTS. Checksum mismatch MUST trigger error `DC-FTPS-0002` and reject the file without processing.

---

## File Naming and Splitting (MUST)

1. Files exceeding **2 GB** MUST be split into sequentially named parts (INT07c):
   - Naming pattern: `<base-name>_part<NNN>.<ext>` (e.g., `dc-extract-2026-04-16_part001.json`)
   - A manifest file MUST accompany split files listing all parts and their checksums.
2. All file names MUST include an ISO 8601 date component and a run identifier for traceability.
3. File names MUST NOT contain PII (customer names, account numbers).

---

## Transfer Direction (MUST)

1. **DWP initiates push to the COTS FTPS server** — DWP pushes files to the Netcompany/COTS-provided FTPS endpoint (INT24, INT26).
2. **DWP initiates pull from the COTS FTPS server** — DWP pulls extract files from the Netcompany/COTS-provided FTPS endpoint (INT25).
3. The Netcompany/COTS team is responsible for provisioning and maintaining the FTPS server endpoint (INT26).
4. Outbound transfers SHOULD be pushed by the adapter (INT15). Pull-based outbound is a fallback (INT16).

---

## Metadata Requirements (MUST)

Every batch extract MUST be accompanied by metadata (INT07b, USE04) describing:
- Dataset name and description
- Attribute names, descriptions, formats, sizes
- PII indicator per attribute
- Validation rules per attribute
- Conforms to DWP-specific naming conventions (to be confirmed with DWP client)

Metadata MUST be provided in an **uploadable format** (USE02) — confirm format with DWP Data Integration Platform team.

---

## Reconciliation (MUST)

1. Every file transfer (inbound and outbound) MUST be logged with: filename, transfer direction, timestamp, file size, checksum result, PGP verification result, and processing outcome.
2. A reconciliation report MUST be available covering all transfer events up to any given point in time — required for disaster recovery (REC05).
3. Failed transfers MUST trigger an alert within the observability layer (UAAF.24).
4. Exception reports for files that fail processing MUST be produced daily (DW.71 pattern).

---

## Prohibited Practices

- **MUST NOT** store PGP private keys in the repository or application configuration files.
- **MUST NOT** log decrypted file content or PII extracted from files.
- **MUST NOT** process a file whose checksum or PGP signature has not been verified.
- **MUST NOT** use FTP (unencrypted) under any circumstances.
- **MUST NOT** use SSL 3.0 or TLS 1.0/1.1.
- **MUST NOT** assume DWP private network connectivity (MPLS, PSN) — design for Internet-grade transport (INT34).

---

## Error Codes

| Code | Trigger |
|---|---|
| `DC-FTPS-0001` | Inbound file PGP signature missing or unverifiable |
| `DC-FTPS-0002` | Inbound file SHA-256 checksum mismatch |
| `DC-FTPS-0003` | Outbound transfer failed after retry exhaustion |
| `DC-FTPS-0004` | Split file manifest missing or incomplete |
| `DC-FTPS-0005` | File exceeds 2 GB without accompanying manifest |

---

## Open Items (confirm with DWP client)

| Item | Owner | Status |
|---|---|---|
| DWP public PGP key fingerprint and rotation schedule | DWP client | Awaiting contract award |
| Metadata format and DWP naming convention specification | DWP Data Integration Platform team | Awaiting contract award |
| FTPS server IP address and hostname provisioning approach | DevOps / Release Engineer | Awaiting infrastructure design |
| Agreed transition window for PGP key rotation | DWP client | Awaiting contract award |
| Whether SFTP fallback (INT22) applies to any specific interface | DWP client | Awaiting contract award |
