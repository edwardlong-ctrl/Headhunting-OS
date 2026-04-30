# Task 18B: Consultant Company + Job CRUD Endpoints

## Worktree / Branch Rules
- repo path: /Users/edwardlong/Documents/New project
- current main HEAD: bdf3b82
- mode: worktree (do NOT modify main directly)
- commit allowed: yes (single squash-ready commit in worktree)
- merge: no (merge will be done from the main session)

## Operating Rules
Read `docs/roadmap/codex-task-operating-rules.md` before any implementation.
Read `docs/roadmap/current-engineering-snapshot.md` for current baseline.

## Confirmed by Repo (repo-truth, verified on main bdf3b82)

1. **CompanyService** (`services/core-api/src/main/java/.../company/service/CompanyService.java`):
   - Write methods: `createCompany(Company)`, `createContact(CompanyContact)`, `upsertPreference(CompanyPreference)`
   - Read methods: `findCompanyByIdAndOrganizationId`, `findCompaniesByOrganizationIdAndStatus`, `findAllCompaniesByOrganizationId`, `findContactsByCompanyIdAndOrganizationId`, `findPreferencesByCompanyIdAndOrganizationId`
   - **No update method exists for Company or CompanyContact.**

2. **CompanyPersistencePort** (`...company/port/CompanyPersistencePort.java`):
   - `create(Company)`, `findByIdAndOrganizationId(...)`, `findByOrganizationIdAndStatus(...)`, `findAllByOrganizationId(...)`
   - **No update method exists.**

3. **JobService** (`...job/service/JobService.java`):
   - Write methods: `createJob(Job)`, `createRequirement(JobRequirement)`, `createScorecard(JobScorecard)`
   - Read methods: `findJobByIdAndOrganizationId`, `findJobsByOrganizationIdAndStatus`, `findJobsByCompanyIdAndOrganizationId`, `findAllJobsByOrganizationId`, `findRequirementsByJobIdAndOrganizationId`, `findActiveScorecardByJobIdAndOrganizationId`
   - **No update method exists for Job.**

4. **JobPersistencePort** (`...job/port/JobPersistencePort.java`):
   - `create(Job)`, `findByIdAndOrganizationId(...)`, `findByOrganizationIdAndStatus(...)`, `findByCompanyIdAndOrganizationId(...)`, `findAllByOrganizationId(...)`
   - **No update method exists.**

5. **Existing Consultant controllers** (`...apiboundary/consultant/`):
   - `ConsultantCompanyController`: GET / (list), GET /{companyId} (detail)
   - `ConsultantJobController`: GET / (list), GET /{jobId} (detail)
   - Pattern: header-based `X-RTO-Actor-Role=consultant` + `X-RTO-Organization-Id`, delegates to `ConsultantApiQueryService`, returns `ApiResponseEnvelope<ApiSafeResponseBody>`.

6. **CRITICAL regression barrier** in `ApiBoundaryRegressionClosureTest.java` (~line 452):
   ```java
   for (Path controllerFile : controllerFiles) {
       String source = Files.readString(controllerFile);
       assertThat(source)
           .doesNotContain("@PostMapping")
           .doesNotContain("@PutMapping")
           .doesNotContain("@PatchMapping")
           .doesNotContain("@DeleteMapping")
   ```
   This iterates ALL controllers and asserts NONE have POST/PUT/PATCH/DELETE.
   **Task 18B must update this test** to:
   - Allow `@PostMapping` and `@PutMapping` only on `ConsultantCompanyController.java` and `ConsultantJobController.java`
   - Keep the assertion that `ClientSafeCandidateCardController.java` and `HealthController.java` have NO write mappings

7. **Company domain model**: `Company.java` is a record with `companyId`, `organizationId`, `name`, `displayName`, `industry`, `website`, `headquartersLocation`, `sizeBand`, `status`, `paymentReliability`, `ownerConsultantId`, `metadata`, `createdAt`, `updatedAt`, `version`. Has builder pattern.

8. **Sealed interface**: `ApiSafeResponseBody` currently permits: `ApiErrorResponse`, `ApiAccessDeniedResponse`, `ApiValidationErrorResponse`, `ClientSafeCandidateCardResponse`, 6 consultant response DTOs, `PagedResult`.

9. **FieldAccessPolicy**: Consultant READ on COMPANY, JOB, SHORTLIST already allowed. **No WRITE action policy exists yet** -- must be added.

10. **Flyway**: Latest migration is V12. **Task 18B must NOT add any migration** (writes use existing V10/V12 tables).

11. **Test count**: 595 tests, 0 failures on current main.

## Mandated Outcomes (from roadmap Task 18 + current baseline)

1. Add `update(Company)` to CompanyPersistencePort, JdbcCompanyPersistencePort, CompanyService.
2. Add `update(Job)` to JobPersistencePort, JdbcJobPersistencePort, JobService.
3. Add a `ConsultantApiCommandService` facade (parallel to ConsultantApiQueryService) for write operations, enforcing `PermissionEnforcer.requireAllowed()` before writes.
4. Add `AccessAction.WRITE` support (if not already present) in FieldAccessPolicy for Consultant on COMPANY and JOB.
5. Add POST endpoint to `ConsultantCompanyController` (create company).
6. Add PUT endpoint to `ConsultantCompanyController` (update company).
7. Add POST endpoint to `ConsultantJobController` (create job).
8. Add PUT endpoint to `ConsultantJobController` (update job).
9. Add request DTOs: `CompanyCreateRequest`, `CompanyUpdateRequest`, `JobCreateRequest`, `JobUpdateRequest` -- all as records implementing a marker or standalone.
10. Add response DTOs if needed or reuse existing detail responses for create/update returns.
11. Update `ApiBoundaryRegressionClosureTest` to allow POST/PUT on consultant controllers only (see barrier above).
12. Add write operation tests: success create, success update, missing role -> 403, missing org -> 400, invalid payload -> 400, org-scoped isolation (create with org A, verify cannot read with org B).
13. **No CanonicalWriteGate required** -- Company/Job are direct domain entities, not canonical profiles.
14. **No WorkflowEvent required** -- audit events for CRUD operations deferred to Task 26 (Workflow Engine).
15. **No new migration** -- writes use existing V10/V12 tables.

## Open Design Decisions (propose before implementing)

1. Should create endpoints return the full detail DTO or just an ID acknowledgment?
   - Recommendation: return full detail DTO for consistency with GET detail.
2. Should update enforce optimistic locking via `version` field in the request?
   - Recommendation: yes, require `version` in update request, throw 409 Conflict on mismatch.
3. Should CompanyContact CRUD be separate endpoints (POST /companies/{id}/contacts) or nested?
   - Recommendation: nested under company (POST /api/consultant/companies/{companyId}/contacts).
4. Should JobRequirement/JobScorecard create be part of this task or deferred?
   - Recommendation: defer to Task 18C to keep this task within size limits.

## Forbidden Scope

- No new Flyway migration.
- No CanonicalWriteGate for Company/Job writes.
- No frontend changes.
- No Client/Candidate/Owner/Admin endpoints.
- No Spring Security or real auth/session.
- No Shortlist write endpoints (defer to Task 18C).
- No JobRequirement/JobScorecard CRUD (defer to Task 18C).
- Do NOT touch `CanonicalWriteGate.java` or `CanonicalWriteService.java`.
- Do NOT touch files in `governedintake/`, `intake/`, `documentstorage/`, `consentdisclosure/`, or `truthlayer/`.

## Validation

```sh
git diff --check
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

Full Maven test suite must pass. Report exact test count.

## Docs Closeout

Update the following files after implementation:
- `docs/roadmap/current-engineering-snapshot.md` -- update HEAD, test count, completed task entry, known gaps
- `docs/roadmap/implementation-status.md` -- add Task 18B entry
- `docs/roadmap/known-gaps.md` -- add Task 18B scope limitations entry

## Final Report Requirements

Must distinguish:
- Code completed (files new/modified count)
- Tests passed (exact count, 0 failures required)
- Docs updated (list which)
- Full suite run: yes/no
- Known hardening gaps (e.g., no optimistic lock, no audit event, no nested child CRUD)
- Completion label: "Consultant Company + Job CRUD baseline" -- NOT "full product API"
