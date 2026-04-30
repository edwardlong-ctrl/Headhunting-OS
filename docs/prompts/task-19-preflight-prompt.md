# Task 19-preflight: Auth/Identity Architecture Design Document

## Worktree / Branch Rules
- repo path: /Users/edwardlong/Documents/New project
- current main HEAD: bdf3b82
- mode: worktree (do NOT modify main directly)
- commit allowed: yes (single squash-ready commit in worktree)
- merge: no (merge will be done from the main session)

## Operating Rules
Read `docs/roadmap/codex-task-operating-rules.md` before any implementation.
Read `docs/roadmap/current-engineering-snapshot.md` for current baseline.

## Task Type: Docs-only design

This is a docs-only preflight task. **No business code, no migration, no test changes.**

## Confirmed by Repo (verified on main bdf3b82)

1. **Current identityaccess package** (`...identityaccess/`): 11 files:
   - `AccessAction.java`, `AccessDecision.java`, `AccessDeniedException.java`, `AccessRequest.java`
   - `FieldAccessPolicy.java`, `FieldClassification.java`
   - `PermissionEnforcer.java`, `PermissionEvaluator.java`
   - `PortalRole.java` (Owner, Consultant, Client, Candidate, Admin, System, AI_ASSISTANT)
   - `RelationshipScope.java`, `ResourceType.java`

2. **Current auth mechanism**: Header-based temporary access context:
   - `X-RTO-Actor-Role`: string matching PortalRole wire values
   - `X-RTO-Organization-Id`: UUID header
   - No User entity, no session, no login, no token, no Spring Security.

3. **PermissionEvaluator**: Deterministic, no-database, deny-by-default. Evaluates (role, resource, action, field, scope, identity-disclosure) tuples.

4. **FieldAccessPolicy**: Explicit allow rules for specific (role, action, resource) triples. Currently allows:
   - Client READ on CLIENT_SAFE_CANDIDATE_CARD at CLIENT_SAFE/GENERALIZED
   - Candidate READ on CANDIDATE_PROFILE at CLIENT_SAFE/GENERALIZED with SELF scope
   - Consultant READ on COMPANY, JOB, SHORTLIST

5. **Controllers**: 5 total (Health, ClientSafeCard, ConsultantCompany, ConsultantJob, ConsultantShortlist). All use header-based auth.

6. **Roadmap Task 19 requirements** (from `docs/roadmap/productization-roadmap.md`):
   - User, Organization, Membership, RoleAssignment, and session model
   - Login/session baseline with Spring Security or equivalent
   - Consultant agency org membership
   - Client company org membership
   - Candidate self-owned identity
   - Admin/System role separation
   - Field-level ABAC wired to API/service
   - Org boundary tests, cross-org negative tests, ID enumeration tests
   - Replace temporary header-based access context

7. **No Spring Security dependency** exists in `pom.xml` currently.

8. **Database schemas**: `governance`, `recruiting`, `intake`, `workflow`, `privacy`, `metadata`. No `identity` or `auth` schema exists.

## Mandated Deliverable

Produce a single design document: `docs/roadmap/task-19-auth-identity-design.md`

The document must cover:

### 1. Current State Analysis
- Map every current use of the header-based auth mechanism (which controllers, which services)
- Identify all FieldAccessPolicy rules
- List all PortalRole values and their current capabilities

### 2. Data Model Design
- `User` entity (id, email, passwordHash, status, createdAt, updatedAt)
- `Organization` entity (id, name, type [agency/company/platform], status)
- `Membership` entity (userId, organizationId, role, status, joinedAt)
- `RoleAssignment` entity (userId, organizationId, portalRole, grantedBy, grantedAt)
- `Session` model (token/JWT strategy, expiry, refresh)
- Propose a new `identity` schema or namespace
- Propose migration version (probably V14, after Task 20's V13)

### 3. Spring Security Integration Strategy
- How to wire Spring Security filter chain
- How to replace header-based context with SecurityContext
- How to preserve backward compatibility during migration (can headers coexist with real auth for testing?)
- Session strategy: stateless JWT vs stateful session

### 4. Organization Membership Model
- Consultant -> agency organization membership
- Client -> company organization membership (via CompanyContact?)
- Candidate -> self-owned (no org membership, or special "self" org?)
- Admin -> platform-level role
- Multi-org support: can a user belong to multiple orgs?

### 5. Controller Migration Plan
- How each existing controller transitions from header-based to SecurityContext
- What changes are needed per controller
- How to maintain test compatibility

### 6. Cross-Org Boundary Design
- How org isolation is enforced at service layer (current: manual orgId parameter)
- Should Spring Security filter inject org scope automatically?
- Cross-org negative test strategy

### 7. Open Questions
- List at least 5 open questions with recommendations
- E.g.: JWT vs session? Password hashing algorithm? OAuth support needed for v1? Email verification? Password reset?

### 8. Task 19 Subtask Breakdown
- If Task 19 is too large for one prompt (50+ files), propose a split into 19A/19B/19C with clear boundaries

## Forbidden Scope

- No business code changes.
- No migration.
- No test changes.
- No pom.xml changes.
- This is a design document only.

## Validation

```sh
git diff --check
git diff --stat HEAD
```

Only `docs/roadmap/task-19-auth-identity-design.md` should appear in the diff.
Optionally also `docs/roadmap/current-engineering-snapshot.md`, `docs/roadmap/implementation-status.md`.

## Final Report

Must state:
- Document created (filename, line count)
- Files changed (should be 1-3 docs files only)
- No code/test/migration changes
- Completion label: "Task 19 architecture design document" -- NOT "auth system implementation"
