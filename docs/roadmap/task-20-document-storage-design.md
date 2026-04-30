# Task 20: Document Storage and SourceItem v1 — Design Document

**Version:** 1.0
**Date:** 2026-04-30
**Priority:** P0 (Operational Core milestone)
**Status:** Pre-implementation design

---

## 1. Current State Analysis

### 1.1 What `intake.source_item` (V4) Already Stores

The V4 migration (`V4__create_governed_intake_tables.sql`) creates the governed
intake schema in the `intake` PostgreSQL schema. The `intake.source_item` table
is the provenance record for raw input entering the system.

| Column | Type | Purpose |
| --- | --- | --- |
| `source_item_id` | `uuid` | Primary key |
| `organization_id` | `uuid` | Tenant/organization scope (FK to `identity.organization`) |
| `source_type` | `text` CHECK | Enum: `CV`, `LINKEDIN_TEXT`, `WECHAT_NOTE`, `CALL_NOTE`, `EMAIL`, `INTERVIEW_FEEDBACK`, `JD`, `COMPANY_MATERIAL`, `OLD_SYSTEM_EXPORT`, `OTHER` |
| `origin` | `text` CHECK | Who provided it: `CONSULTANT_UPLOAD`, `CANDIDATE_UPLOAD`, `CLIENT_UPLOAD`, `EMAIL_IMPORT`, `SYSTEM_IMPORT`, `ADMIN_IMPORT`, `OTHER` |
| `title` | `text` | Human-readable title (nullable, non-blank) |
| `content_hash` | `text` | SHA-256 or similar hash for deduplication (nullable, non-blank) |
| `external_ref` | `text` | External reference (e.g., ATS ID, LinkedIn URL) (nullable) |
| `storage_ref` | `text` | **Placeholder** — intended to point to object storage but currently unused (nullable) |
| `raw_ref` | `text` | Reference to raw/unprocessed version (nullable) |
| `language` | `text` | Language code (nullable) |
| `uploaded_by_actor_type` | `governance.actor_role` | Role of uploader (not nullable) |
| `uploaded_by_actor_id` | `uuid` | FK to `identity.user_account` (nullable) |
| `received_at` | `timestamptz` | When the source was received |
| `metadata_json` | `jsonb` | Arbitrary metadata, max 8192 chars, defaults to `'{}'` |
| `status` | `text` CHECK | Lifecycle: `RECEIVED`, `REGISTERED`, `ATTACHED_TO_PACKET`, `SUPERSEDED`, `REJECTED` |
| `created_at`, `updated_at` | `timestamptz` | Audit timestamps |

**Indices:**
- `UNIQUE (source_item_id, organization_id)`
- `UNIQUE INDEX intake_source_item_org_content_hash_uidx ON (organization_id, content_hash) WHERE content_hash IS NOT NULL` — prevents duplicate upload within an org
- `INDEX ON (organization_id, status)`
- `INDEX ON (organization_id, source_type, received_at)`
- `INDEX ON (organization_id, uploaded_by_actor_type, uploaded_by_actor_id)`

**Linkage:** Connected to `intake.information_packet` via the junction table
`intake.information_packet_source_item` with composite FKs ensuring
organization-scoped integrity.

**Key observation:** `storage_ref` and `raw_ref` are defined as nullable text
columns. The table is designed to carry a reference to external object storage,
but no object storage implementation exists. The `content_hash` column already
provides the deduplication foundation.

### 1.2 What `recruiting.source_item` (V2) Already Stores

The V2 migration also created a `recruiting.source_item` table (kernel phase).
This older table overlaps with `intake.source_item` but uses a different status
model and has a direct `information_packet_id` FK (rather than a junction
table).

| Column | Differences from `intake.source_item` |
| --- | --- |
| `information_packet_id` | Direct FK to `recruiting.information_packet` (one-to-one/one-to-many) |
| `source_type` | Plain `text`, no CHECK constraint |
| `origin_actor_type` | `governance.actor_role`, no `origin` column |
| `status` | Different vocabulary: `uploaded`, `classifying`, `classified`, `parsing`, `parsed`, `failed`, `quarantined`, `archived` |
| `sensitivity_level` | Classifies document sensitivity |
| `parsed_text_ref` | Reference to parsed/extracted text |
| `source_timestamp` | When the source document was created (vs. received) |
| No `raw_ref`, `metadata_json` | Uses `metadata jsonb` instead |

**References to `recruiting.source_item`:**
- `recruiting.candidate_document` (V10) has `source_item_id uuid REFERENCES recruiting.source_item (source_item_id)`
- `governance.claim_ledger_item` (V2) has `source_item_id uuid REFERENCES recruiting.source_item (source_item_id)`
- `recruiting.information_packet` (V2) has `source_item_ids uuid[]` (array of source item IDs)

### 1.3 What `recruiting.candidate_document` (V10) Already Stores

The V10 migration creates this as the canonical document metadata record linked
to a candidate.

| Column | Purpose |
| --- | --- |
| `candidate_document_id` | Primary key |
| `organization_id` | Org scope (FK to `identity.organization`) |
| `candidate_id` | FK to `recruiting.candidate` |
| `document_type` | Type of document (e.g., "resume", "certificate") |
| `title` | Human-readable title |
| `storage_ref` | **Placeholder** — points to object storage location (nullable) |
| `content_hash` | Deduplication hash |
| `source_item_id` | Links to `recruiting.source_item` |
| `language` | Language code |
| `status` | `active`, `superseded`, `archived` |
| `superseded_by_document_id` | Self-referencing FK for version chaining |
| `metadata` | JSONB, defaults to `'{}'` |
| `version` | Optimistic lock, defaults to 1 |

**Comment on table:** "Document metadata linking candidate to
SourceItem/InformationPacket provenance. Does not store file content directly."

**CandidateDocument domain record** (Java `record`):
- Package: `com.recruitingtransactionos.coreapi.candidatedocument`
- Fields: `candidateDocumentId`, `organizationId`, `candidateId`, `documentType`,
  `title`, `storageRef`, `contentHash`, `sourceItemId` (UUID, not typed),
  `language`, `status`, `supersededByDocumentId`, `metadata`, `createdAt`,
  `updatedAt`, `version`
- Has full service + persistence port + JDBC adapter
- `sourceItemId` is a bare `UUID`, typed `CandidateDocumentId` wrapper

### 1.4 Gaps for Real File Storage

| Gap | Detail |
| --- | --- |
| **No blob/object storage** | Both `intake.source_item.storage_ref` and `recruiting.candidate_document.storage_ref` are text columns with no backing storage implementation. No S3-compatible, MinIO, or local filesystem integration exists. |
| **No upload API** | No HTTP endpoint for multipart file upload. No streaming ingestion. No chunked upload. |
| **No file type/size validation** | No MIME type whitelist. No maximum file size enforcement. No content inspection beyond hash computation. |
| **No malware scan** | No virus scanning integration or even a placeholder boundary for one. |
| **No download/read API** | No access-controlled file retrieval endpoint. No presigned URL generation. No proxy download. |
| **No access control for files** | The `storage_ref` column is readable by any code that queries the table. No per-file ACL. No role-based download authorization. |
| **No document text extraction** | No text extraction pipeline (PDF parsing, OCR, etc.). The `parsed_text_ref` in V2's `recruiting.source_item` is unused. |
| **Dual `source_item` tables** | `recruiting.source_item` (V2) and `intake.source_item` (V4) overlap in purpose but are disconnected. V10's `candidate_document` references V2's table. V4's table has richer provenance. Task 20 must decide the canonical SourceItem table strategy. |
| **`storage_ref` format undefined** | No convention for how `storage_ref` is structured (S3 URI? relative path? opaque key?). |

---

## 2. Architecture Proposal

### 2.1 Object Storage Strategy

**Principle:** Use an S3-compatible abstraction from day one. This ensures dev/prod
parity and avoids filesystem lock-in.

| Environment | Implementation | Rationale |
| --- | --- | --- |
| **Local development** | MinIO (Docker container) | S3-compatible, zero-cost, runs alongside existing PostgreSQL Docker container |
| **CI/test** | In-memory or temp-dir adapter | Speed; test isolation; no Docker dependency |
| **Production/staging** | AWS S3, Alibaba Cloud OSS, or any S3-compatible provider | Configurable via Spring profiles; same API |

**Abstraction layer:**
```
interface DocumentStore {
    StoreResult store(StoreRequest request);          // returns storage_ref
    InputStream retrieve(String storageRef);           // returns file stream
    void delete(String storageRef);                    // deletion (soft by default)
    URL presignDownload(String storageRef, Duration ttl); // presigned URL (optional v1)
    boolean exists(String storageRef);
}
```

Implementations:
- `MinioDocumentStore` — for local dev (port matches S3 API)
- `InMemoryDocumentStore` — for integration tests
- `S3DocumentStore` — for production (AWS SDK v2)

The `DocumentStore` interface lives in the `governedintake` package as a
`port` interface, following the existing hexagonal architecture pattern.

**Storage key convention:**
```
{organization_id}/{source_item_id}/{content_hash[:16]}/{original_filename}
```
This ensures:
- Organization-scoped prefix (easy lifecycle/retention management)
- Source item ID makes lookup deterministic
- Content hash prefix prevents accidental collisions
- Original filename preserved for download Content-Disposition header

### 2.2 Upload Flow

```
┌──────────┐     ┌───────────────┐     ┌──────────────┐     ┌───────────────┐
│ Uploader │────▶│  Upload API   │────▶│ DocumentStore │────▶│ Object Store  │
│ (portal) │     │  (Controller) │     │   (Port)      │     │ (MinIO/S3)   │
└──────────┘     └───────┬───────┘     └──────────────┘     └───────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ Governed-    │
                  │ IntakeService│
                  └──────┬───────┘
                         │
                  ┌───────▼───────┐     ┌──────────────┐
                  │ SourceItem    │────▶│ PostgreSQL    │
                  │ Registration  │     │ (intake schema)│
                  └───────────────┘     └──────────────┘
                         │
                         ▼
                  ┌───────────────┐
                  │ Information-  │
                  │ Packet attach │
                  └───────────────┘
```

**Step-by-step:**

1. **Uploader** (Consultant, Candidate, or Client) submits a file via
   `POST /api/consultant/intake/upload` (or equivalent portal endpoint) with:
   - Multipart file
   - `source_type` (CV, JD, WECHAT_NOTE, CALL_NOTE, etc.)
   - `origin` (who is uploading)
   - Optional: `title`, `language`, `external_ref`
   - Optional but recommended: `information_packet_id` for immediate attachment

2. **Controller** receives the multipart request:
   - Validates MIME type against configured whitelist
   - Validates file size against configured maximum
   - Computes SHA-256 content hash
   - Rejects files exceeding limits with HTTP 413/415

3. **GovernedIntakeService.registerSourceItemWithFile()** (new method):
   - Creates a `SourceItemRegistrationCommand` with `content_hash`, `storage_ref` (initially null), metadata
   - Persists the `SourceItem` row via `SourceItemPersistencePort`
   - Calls `DocumentStore.store()` with the file stream → obtains `storage_ref`
   - Updates the `SourceItem` row with the actual `storage_ref`
   - If `information_packet_id` was provided, attaches the source item to the packet
   - Returns the `SourceItem` record

   **Transaction note:** The SourceItem row insert and document store write are
   NOT in the same transaction (object storage is not transactional). The design
   must account for orphaned objects:
   - If DB insert succeeds but storage fails → delete SourceItem row (cleanup)
   - If storage succeeds but DB update of `storage_ref` fails → the stored object
     is orphaned but harmless; a periodic garbage collector can clean up
     unreferenced objects (future task)

4. **Virus scan placeholder:** Before the file is stored, the `DocumentStore`
   implementation invokes a `VirusScanPort`. In v1, this port has a no-op
   implementation (pass-through). The boundary is defined so it can be swapped
   for ClamAV or a cloud scanning service later.

5. **Duplicate detection:** The `content_hash` unique index on
   `(organization_id, content_hash)` already prevents duplicate uploads within
   the same org. The upload API returns HTTP 409 Conflict with the existing
   `source_item_id` when a duplicate hash is detected.

### 2.3 Metadata Linkage

```
File (Object Store)
  │
  │ storage_ref
  ▼
SourceItem (intake.source_item)          ← created on upload
  │ source_item_id
  ├──▶ InformationPacketSourceItem (junction)
  │      │ information_packet_id
  │      ▼
  │    InformationPacket (intake.information_packet)
  │      │
  │      ├──▶ Extraction (Task 21/22)
  │      │      │
  │      │      ▼
  │      │    ClaimLedgerItem (governance.claim_ledger_item)
  │      │      │
  │      │      ▼
  │      │    ReviewEvent (governance.review_event)
  │      │      │
  │      │      ▼
  │      │    CanonicalWriteAttempt (governance.canonical_write_attempt)
  │      │      │
  │      │      ▼
  │      │    CandidateProfile / Job / Company canonical fields
  │      │
  │      └──▶ CandidateDocument (recruiting.candidate_document) ← optional bridge
  │             │ source_item_id (currently points to recruiting.source_item)
  │             ▼
  │           Candidate (recruiting.candidate)
  │
  └──▶ AITaskRun (source refs) ← for extraction task auditing
```

**Key linkage design decisions:**
- The primary source of truth for file provenance is `intake.source_item`.
- `intake.information_packet_source_item` is the junction table connecting
  sources to packets.
- `recruiting.candidate_document` links back to `recruiting.source_item` via
  FK today. Task 20 should add an explicit bridge or migration so
  `candidate_document.source_item_id` can also reference
  `intake.source_item`.
- The Claim Ledger already has `source_item_id REFERENCES recruiting.source_item`.
  Task 20 should allow the claim ledger to reference `intake.source_item` as well,
  or preferably unify on `intake.source_item` going forward.

**Recommendation for dual-table situation:**
Task 20 should NOT attempt to merge the two source_item tables (that is a
migration risk). Instead:
1. New file uploads always create `intake.source_item` records (V4).
2. `recruiting.source_item` (V2) is kept for backward compatibility with
   existing kernel tables.
3. A nullable `intake_source_item_id` column is added to `recruiting.source_item`
   (and vice versa) so the two can be linked when needed.
4. The `CandidateDocument` domain model and V10 migration should be updated
   to accept `source_item_id` as a link to `intake.source_item` (add nullable
   column or change FK target via a new migration).
5. Long-term (post-pilot, Task 46+), a migration path should normalize to a
   single `intake.source_item` table.

### 2.4 Access Control

**Principle:** Document access follows the same role-scoped rules as the rest
of the system, with additional per-document access checks.

| Actor | Can Read | Constraints |
| --- | --- | --- |
| **Consultant** | Documents in their organization | Read access via org-scoped queries. Cannot read other orgs' documents. |
| **Candidate** | Only their own uploaded documents | Self-scoped: `origin = 'CANDIDATE_UPLOAD'` AND `uploaded_by_actor_id = candidate's user_id` |
| **Client** | JD and company materials they uploaded; anonymous candidate documents | Cannot access raw CV/screenshots/consultant notes. Client-visible documents require consent gate after shortlist. |
| **Owner** | All documents in their organization | Audit trail logged on access. |
| **Admin** | All documents (for audit/governance) | Access logged with reason. Cannot automatically download candidate-consent-restricted files without audit justification. |

**Implementation approach:**
- The download endpoint is role-guarded at the API layer (Task 19 auth headers
  or Spring Security context).
- The service layer verifies: `document.organization_id == caller.organization_id`
  AND caller role is permitted to read documents of the given `source_type`
  and `origin`.
- For Candidate access: verify `source_item.uploaded_by_actor_id == caller.user_id`
  AND `source_item.origin == 'CANDIDATE_UPLOAD'`.
- For Client access: only allow `source_type IN ('JD', 'COMPANY_MATERIAL')`
  and `origin = 'CLIENT_UPLOAD'`. Block all candidate-origin documents.

**New table: `intake.document_access_log`** (optional for v1, recommended):
```sql
CREATE TABLE intake.document_access_log (
  access_log_id uuid PRIMARY KEY,
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  source_item_id uuid NOT NULL REFERENCES intake.source_item (source_item_id),
  accessed_by_actor_type governance.actor_role NOT NULL,
  accessed_by_actor_id uuid REFERENCES identity.user_account (user_account_id),
  access_reason text,           -- e.g., 'intake_review', 'audit', 'candidate_self'
  ip_address inet,
  accessed_at timestamptz NOT NULL DEFAULT now()
);
```

### 2.5 File Type Support Baseline

| Category | MIME Types | Max Size (v1) | Source Types |
| --- | --- | --- | --- |
| **Documents** | `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | 25 MB | CV, JD, COMPANY_MATERIAL |
| **Images** | `image/png`, `image/jpeg`, `image/webp` | 10 MB | WECHAT_NOTE, CALL_NOTE (screenshots) |
| **Plain text** | `text/plain`, `text/markdown` | 5 MB | CALL_NOTE, EMAIL, OTHER |

**Whitelist approach:** Only these MIME types are accepted. Unknown types
return HTTP 415 Unsupported Media Type.

**Filename sanitization:** Original filenames are sanitized (remove path
traversal, limit to alphanumeric + `-_.`, enforce max 255 chars).

---

## 3. Schema Delta Preview

### 3.1 New Tables

#### `intake.document_access_log` (P2 — nice-to-have for v1)

```sql
CREATE TABLE intake.document_access_log (
  access_log_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id uuid NOT NULL REFERENCES identity.organization (organization_id),
  source_item_id uuid NOT NULL REFERENCES intake.source_item (source_item_id),
  accessed_by_actor_type governance.actor_role NOT NULL,
  accessed_by_actor_id uuid REFERENCES identity.user_account (user_account_id),
  access_reason text CHECK (access_reason IS NULL OR btrim(access_reason) <> ''),
  ip_address inet,
  accessed_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX document_access_log_org_source_idx
  ON intake.document_access_log (organization_id, source_item_id);
CREATE INDEX document_access_log_org_actor_idx
  ON intake.document_access_log (organization_id, accessed_by_actor_type, accessed_by_actor_id);
```

### 3.2 New Columns on Existing Tables

#### `intake.source_item`

| Column | Type | Purpose |
| --- | --- | --- |
| `mime_type` | `text` | The detected/declared MIME type of the uploaded file |
| `file_size_bytes` | `bigint` | Size of the stored file in bytes |
| `original_filename` | `text` | Sanitized original filename for download Content-Disposition |
| `scan_status` | `text` | Virus scan status: `UNSCANNED`, `CLEAN`, `INFECTED`, `ERROR` (v1 default: `UNSCANNED`) |

Rationale: `storage_ref` already exists. Adding MIME type, file size, and
filename makes the SourceItem a complete file metadata record. The `scan_status`
column is the virus scan placeholder boundary.

#### `recruiting.source_item` (bridge column)

| Column | Type | Purpose |
| --- | --- | --- |
| `intake_source_item_id` | `uuid` | Nullable FK to `intake.source_item(source_item_id)`. Allows linking the V2 record to the richer V4 record when both exist. |

#### `recruiting.candidate_document` (alternative FK target)

The `source_item_id` column currently has:
```sql
source_item_id uuid REFERENCES recruiting.source_item (source_item_id),
```

Task 20 may add:
```sql
-- Alternative: add a second column
intake_source_item_id uuid REFERENCES intake.source_item (source_item_id),
```

Or change the FK target to `intake.source_item` via a migration. The design
favors adding a second nullable column rather than changing the FK target,
to avoid breaking existing V10 references.

### 3.3 No Changes To

- `intake.information_packet` — already designed to hold references to source
  items via the junction table.
- `intake.information_packet_source_item` — junction table is correct as-is.
- `governance.claim_ledger_item` — `source_item_id` FK to `recruiting.source_item`
  is adequate; the bridge column on `recruiting.source_item` provides the
  indirect link to `intake.source_item`.
- `governance.canonical_write_attempt` — no schema change needed.

### 3.4 Migration Numbering

The new migration should be **V12** (V11 was `canonical_write_attempt`).
Naming: `V12__add_document_storage_columns.sql`

---

## 4. Open Questions for Task 20 Implementation

### Q1: Presigned URL vs Proxy Download

| Approach | Pros | Cons |
| --- | --- | --- |
| **Presigned URL** | Offloads bandwidth; simpler backend; faster for large files | URL leakage risk; time-limited; harder to enforce access revocation |
| **Proxy download** (backend streams from object store to client) | Full access control per request; audit log natural; no URL leakage | Backend bandwidth cost; slower; higher memory pressure |

**Recommendation:** Start with **proxy download** for v1. Presigned URLs can be
added later as an optimization for large files. Proxy download gives us full
access control and audit logging from day one, which aligns with the governance
requirements.

### Q2: Max File Size and Type Constraints

| Parameter | Proposed v1 Default | Configurable? |
| --- | --- | --- |
| Max CV (PDF/DOCX) | 25 MB | Yes, via `app.document-storage.max-cv-size` |
| Max image (PNG/JPEG) | 10 MB | Yes |
| Max plain text | 5 MB | Yes |
| Max request body | 50 MB (Spring default) | Must increase via `spring.servlet.multipart.max-file-size` |

**Recommendation:** Make all limits configurable via Spring properties.
Reject oversized uploads at the controller level before any processing.

### Q3: Virus Scan — Placeholder vs Real Integration

**v1 approach:** Placeholder boundary only.
- Define `VirusScanPort` interface with `ScanResult scan(InputStream)`.
- Implement `NoOpVirusScanPort` that returns `CLEAN` for all files.
- Set `scan_status = 'UNSCANNED'` on all v1 uploads.

**Production consideration:** A real ClamAV integration requires:
- Running ClamAV daemon (separate container)
- Streaming file content to ClamAV socket
- Handling scan timeouts for large files
- Defining action on `INFECTED` (reject upload, quarantine, notify admin)

This is deferred to Task 41 (Security Hardening) or a follow-up task.

### Q4: Encryption at Rest — Requirement Level for v1

| Level | Approach | Effort |
| --- | --- | --- |
| **None** | Object store defaults (S3/MinIO server-side encryption) | Zero effort |
| **SSE-S3 / SSE-KMS** | Use provider-managed keys | Configuration only |
| **Client-side encryption** | Encrypt before storing, manage keys in app | Significant effort |

**Recommendation:** For v1, rely on object store defaults. MinIO supports
server-side encryption; production S3 providers have SSE-S3 as default. This is
adequate for a controlled pilot. Client-side encryption (with application-managed
keys) should be evaluated in Task 41 (Security Hardening).

### Q5: SourceItem Table Unification Strategy

**Current state:** Two tables — `recruiting.source_item` (V2) and
`intake.source_item` (V4) — serve overlapping purposes.

**Options:**
1. **Add bridge column** (recommended for v1): Add `intake_source_item_id` to
   `recruiting.source_item`. New uploads always create `intake.source_item`.
   Link tables (candidate_document, claim_ledger_item) can follow the bridge.
2. **Migrate and drop V2 table**: Risky. Too many dependent FKs. Better suited
   for post-pilot (Task 46).
3. **Keep both, document the split**: Simple but creates ongoing confusion.

**Recommendation:** Option 1 (bridge column) for Task 20. The bridge is a
single nullable column, low risk, and provides a path forward.

### Q6: Stream Upload vs Full Buffering

For virus scanning and hash computation, the file must be fully buffered at
some point. Options:
- Buffer to temp file on disk (handles large files, but filesystem dependency)
- Buffer to memory up to a threshold, then spill to disk
- Stream directly to object store, then compute hash from stored object

**Recommendation:** Buffer to memory up to 64 MB, spill to temp file if larger.
This is the standard Spring Boot multipart behavior. For v1, the upload sizes
are capped at 25 MB for documents, so in-memory handling is acceptable.

### Q7: Content Hash Algorithm

Current `content_hash` column has no enforced format. For consistency:

**Recommendation:** Use SHA-256, prefixed with algorithm for future flexibility:
```
sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

This allows adding `sha512:` or `blake3:` in the future without ambiguity.

---

## 5. Dependency Map

### 5.1 Tasks That Depend on Task 20

| Task | Dependency | Reason |
| --- | --- | --- |
| **Task 22: Document Intelligence** | Direct | Needs stored files to parse/chunk/extract text. Cannot OCR or parse documents that aren't stored. |
| **Task 23: Governed AI Intake E2E** | Direct | Upload/source -> AI extraction -> claim ledger -> review requires files to be stored and retrievable. |
| **Task 24: Consultant Portal v1** | Indirect | Upload UI needs the upload API from Task 20. Can mock initially but should have real upload before portal is "complete." |
| **Task 31: Candidate Portal v1** | Direct | Candidate CV/profile upload requires document storage. |
| **Task 32: Client Portal v1** | Moderate | Client JD upload requires document storage. |
| **Task 38: Pilot Seed Data** | Moderate | Seed data includes source documents that need to be stored. |
| **Task 41: Security Hardening** | Dependent | Virus scanning, PII masking in files, and upload security all depend on the storage layer existing. |

### 5.2 Tasks Task 20 Depends On

Task 20 is designed to be **independent** of Tasks 18 and 19:

| Task | Dependency Level | Reason |
| --- | --- | --- |
| **Task 5A (V4 governed intake)** | Hard dependency (COMPLETED) | `intake.source_item` and `intake.information_packet` tables are the foundation. Already exists. |
| **Task 16 (V10 product data model)** | Soft dependency (COMPLETED) | `recruiting.candidate_document` already exists and has a `storage_ref` + `source_item_id` FK. Task 20 builds on this. |
| **Task 18 (API Layer)** | None | Task 20 can define its own upload/download API endpoints. May later be refactored to align with Task 18 API conventions. |
| **Task 19 (Auth/RBAC)** | None (for storage layer) | The `DocumentStore` abstraction and `GovernedIntakeService` additions are pure backend. Auth checks on download endpoints can use the existing temporary header-based context until Task 19 replaces it. |

**Task 20 can be implemented in parallel with Tasks 18 and 19.** The backend
storage abstraction, SourceItem schema additions, and core service logic do not
require the full API layer or production auth.

---

## 6. Implementation Notes for Task 20 Prompt

### 6.1 What Task 20 Should Produce

| Deliverable | Type | Description |
| --- | --- | --- |
| `DocumentStore` interface | Java `interface` | In `governedintake/port/DocumentStore.java` |
| `MinioDocumentStore` | Java `class` | S3-compatible implementation using MinIO SDK |
| `InMemoryDocumentStore` | Java `class` | For integration tests |
| `VirusScanPort` interface | Java `interface` | Boundary for virus scanning |
| `NoOpVirusScanPort` | Java `class` | Pass-through implementation |
| `DocumentUploadCommand` | Java `record` | Command object for multipart upload |
| `GovernedIntakeService` extensions | Java methods | `registerSourceItemWithFile()`, `retrieveDocument()`, `deleteDocument()` |
| Upload/download controllers | Spring `@RestController` | At least: `POST /api/intake/upload`, `GET /api/intake/documents/{sourceItemId}/download` |
| V12 Flyway migration | SQL | New columns on `intake.source_item`, bridge column on `recruiting.source_item` |
| Integration tests | Java test class | Test upload → store → retrieve → delete cycle with `InMemoryDocumentStore` |
| Docker Compose entry | YAML | MinIO service definition |

### 6.2 What Task 20 Should NOT Produce

- No UI/portal upload components (those belong in Tasks 24/31/32).
- No real AI parsing pipeline (Task 22).
- No real malware scanner (Task 41).
- No migration that drops `recruiting.source_item`.
- No file type auto-detection beyond MIME sniffing.
- No OCR, PDF parsing, or text extraction (Task 22).

---

## 7. Summary

| Section | Status |
| --- | --- |
| Current state analysis | Complete — `intake.source_item` (V4) and `recruiting.candidate_document` (V10) analyzed; gaps identified |
| Architecture proposal | Object storage via S3-compatible abstraction (MinIO dev, S3 prod); proxy download; org-scoped access control |
| Schema delta preview | 1 new table (`document_access_log`), 4 new columns on `intake.source_item`, 1 bridge column on `recruiting.source_item` |
| Open questions | 7 identified (presigned vs proxy, file size limits, virus scan placeholder, encryption, table unification, buffering, hash format) |
| Dependency map | 7 downstream tasks depend on Task 20; Task 20 depends on Tasks 5A and 16 (both completed); independent of 18/19 |
| Ready for implementation | Yes — all architectural decisions documented and justified |
