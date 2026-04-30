# Task 20: Document Storage and SourceItem v1

## Worktree / Branch Rules
- repo path: /Users/edwardlong/Documents/New project
- current main HEAD: bdf3b82
- mode: worktree (do NOT modify main directly)
- commit allowed: yes (single squash-ready commit in worktree)
- merge: no (merge will be done from the main session)

## Operating Rules
Read `docs/roadmap/codex-task-operating-rules.md` before any implementation.
Read `docs/roadmap/current-engineering-snapshot.md` for current baseline.
Read `docs/roadmap/task-20-document-storage-design.md` -- this is the design document produced by Task 20-preflight. Follow its architectural decisions unless you find a specific technical reason to deviate, in which case document the deviation.

## Task Type: Backend code + migration

## Confirmed by Repo (repo-truth, verified on main bdf3b82)

1. **Design doc exists**: `docs/roadmap/task-20-document-storage-design.md` (594 lines, merged at 3425c6c).
   Key decisions: S3-compatible abstraction, InMemoryDocumentStore for tests, storage key convention `{org_id}/{source_item_id}/{hash[:16]}/{filename}`, SHA-256 content hash, MIME whitelist approach, proxy download for v1, virus scan placeholder.

2. **SourceItem record** (`...governedintake/SourceItem.java`):
   - Already has: `sourceItemId`, `organizationId`, `sourceType`, `origin`, `title`, `contentHash`, `externalRef`, `storageRef`, `rawRef`, `language`, `uploadedByActorType`, `uploadedByActorId`, `receivedAt`, `createdAt`, `metadataJson`, `status`.
   - **Missing from design doc requirements**: `mimeType`, `fileSizeBytes`, `originalFilename`, `scanStatus`.

3. **Intake tables**: `intake.source_item` (V4), `intake.information_packet` (V4), `intake.extraction_run` (V5). Existing V2 `recruiting.source_item` is a separate legacy table.

4. **Latest migration**: V12 (`V12__harden_product_data_model_org_scope.sql`). **Next migration must be V13**.

5. **No object storage abstraction exists** anywhere in the codebase.

6. **No upload/download controllers exist**.

7. **Docker Compose**: Verify whether `docker-compose.yml` exists and what services are defined before adding MinIO.

8. **FieldAccessPolicy**: No DOCUMENT resource type or UPLOAD action exists yet.

9. **Test count**: 595 tests, 0 failures on current main.

10. **CanonicalWriteGate**: Must NOT be bypassed. Uploaded documents create SourceItem/InformationPacket only -- never canonical facts.

## Design Checkpoint (from design doc -- verify before coding)

Before writing code, confirm you agree with these design doc decisions:
1. InMemoryDocumentStore for tests, MinioDocumentStore or FileSystemDocumentStore for dev
2. V13 migration adds `mime_type`, `file_size_bytes`, `original_filename`, `scan_status` columns to `intake.source_item`
3. Unique constraint on `(organization_id, content_hash)` in `intake.source_item` for dedup
4. No merge of V2 `recruiting.source_item` and V4 `intake.source_item` tables
5. Virus scan is NoOp placeholder for v1
6. Proxy download (not presigned URLs) for v1

If you disagree with any decision, document the reason and propose an alternative BEFORE implementing.

## Mandated Outcomes

1. **V13 migration** (`V13__add_document_storage_columns.sql`):
   - Add `mime_type VARCHAR(127)`, `file_size_bytes BIGINT`, `original_filename VARCHAR(512)`, `scan_status VARCHAR(50) DEFAULT 'not_scanned'` to `intake.source_item`.
   - Add unique constraint `uq_source_item_org_content_hash` on `(organization_id, content_hash)` WHERE `content_hash IS NOT NULL`.
   - Follow existing migration naming/comment conventions.

2. **DocumentStore interface** (new package, e.g. `documentstorage`):
   - `store(DocumentStoreKey key, InputStream content, long contentLength)` -> void or result
   - `retrieve(DocumentStoreKey key)` -> InputStream
   - `exists(DocumentStoreKey key)` -> boolean
   - `delete(DocumentStoreKey key)` -> void
   - `DocumentStoreKey` value object encapsulating the storage path convention

3. **InMemoryDocumentStore** for tests (stores byte[] in ConcurrentHashMap).

4. **VirusScanPort** interface + **NoOpVirusScanPort** implementation (always returns CLEAN).

5. **DocumentUploadCommand** record: organizationId, sourceType, origin, title, uploadedByActorType, uploadedByActorId, originalFilename, mimeType, contentLength.

6. **DocumentUploadService** (or extend GovernedIntakeService):
   - Validate MIME type against whitelist
   - Validate file size against limits (per design doc: PDF/DOCX 25MB, images 10MB, text 5MB)
   - Compute content hash (SHA-256)
   - Check duplicate via content hash + organization_id
   - Call DocumentStore.store()
   - Create SourceItem record (status=REGISTERED, storageRef=storage key, contentHash, mimeType, fileSizeBytes, originalFilename, scanStatus=not_scanned)
   - Call VirusScanPort (NoOp for v1)
   - Return result with sourceItemId

7. **Upload controller** (e.g. `POST /api/consultant/documents/upload`):
   - Accepts multipart/form-data
   - Requires `X-RTO-Actor-Role=consultant` + `X-RTO-Organization-Id` (existing auth pattern)
   - Returns `ApiResponseEnvelope` with upload result

8. **Download controller** (e.g. `GET /api/consultant/documents/{sourceItemId}/download`):
   - Requires same auth headers
   - Streams file from DocumentStore
   - Sets Content-Type and Content-Disposition headers
   - Organization scope check (sourceItem.organizationId must match request org)

9. **SourceItem record enhancement**: Add `mimeType`, `fileSizeBytes`, `originalFilename`, `scanStatus` fields to the Java record. Update JdbcSourceItemPersistencePort accordingly.

10. **Negative tests**:
    - Upload with unsupported MIME type -> 415
    - Upload exceeding size limit -> 413 or 400
    - Download with wrong organization -> 403 or 404
    - Download non-existent sourceItem -> 404
    - Upload without auth headers -> 403

11. **Leakage test**: Download response must not contain raw Candidate/Profile data, internal package names, or stack traces in error responses.

## Open Design Decisions

1. Should the upload endpoint be under `/api/consultant/documents/` or a portal-neutral `/api/documents/`?
   - Recommendation: `/api/consultant/documents/` for now, add other portal upload routes in later tasks.
2. Should duplicate upload (same org + same content hash) return 200 with existing sourceItemId or 409 Conflict?
   - Recommendation: return 200 with existing sourceItemId (idempotent).
3. Should InformationPacket be auto-created on upload, or require a separate attach step?
   - Recommendation: auto-create a single InformationPacket per upload for v1 simplicity.

## Forbidden Scope

- No CanonicalWriteGate bypass -- uploads create SourceItem only.
- No AI extraction or text parsing (that is Task 22).
- No frontend changes.
- No Client/Candidate upload endpoints (defer to portal tasks 31/32).
- No real virus scanning (NoOp placeholder).
- No presigned URL download (proxy only for v1).
- No merging of V2 `recruiting.source_item` and V4 `intake.source_item` tables.
- Do NOT touch `apiboundary/consultant/ConsultantCompanyController.java`, `ConsultantJobController.java`, or `ConsultantShortlistController.java`.
- Do NOT touch `company/`, `job/`, `shortlist/` packages.
- Do NOT touch `CanonicalWriteGate.java` or `CanonicalWriteService.java`.

## Validation

```sh
git diff --check
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

Full Maven test suite must pass. Report exact test count.

## Docs Closeout

Update:
- `docs/roadmap/current-engineering-snapshot.md` -- update HEAD, test count, add Task 20 entry, add known gaps
- `docs/roadmap/implementation-status.md` -- add Task 20 entry
- `docs/roadmap/known-gaps.md` -- add Task 20 scope limitations entry (no real virus scan, no AI extraction, no client/candidate upload, no presigned URLs, V2/V4 table duality remains)

## Final Report Requirements

Must distinguish:
- Code completed (files new/modified count)
- Tests passed (exact count, 0 failures required)
- Docs updated (list which)
- Full suite run: yes/no
- Known hardening gaps
- Completion label: "Document storage v1 baseline with placeholder virus scan" -- NOT "full document management system"
