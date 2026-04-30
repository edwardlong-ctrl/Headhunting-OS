# Task 19: Auth/Identity Architecture Design Document

**Status:** Design proposal  
**Date:** 2026-04-30  
**Prepared for:** Task 19 implementation  
**Based on:** Main HEAD bdf3b82 (Task 18A)  

---

## 1. Current State Analysis

### 1.1 Current Auth Mechanism: Header-Based Temporary Access Context

The system currently has **no real authentication or identity model**. All access control is driven by HTTP headers validated at the controller layer:

| Header | Purpose | Used By |
|--------|---------|---------|
| `X-RTO-Actor-Role` | Portal role string (e.g., "consultant", "client") | All 4 gated controllers |
| `X-RTO-Organization-Id` | UUID of the organization | All 4 gated controllers |
| `X-RTO-Field-Classification` | Field classification ("client_safe", "generalized") | Client-safe controller only |
| `X-RTO-Identity-Disclosure-Requested` | Boolean flag for identity disclosure | Client-safe controller only |

**What does NOT exist:**
- No `User` entity or `users` table
- No login flow, no password, no session, no JWT token
- No `Authentication` object or `SecurityContext`
- No Spring Security dependency in `pom.xml`
- No `SecurityFilterChain` or filter-based auth
- No organization membership verification against a real data model
- No role assignment persisted in the database

### 1.2 Controller Auth Patterns (All 5 Controllers)

#### Pattern A: Consultant API (3 controllers)
**Files:**
- `ConsultantCompanyController.java` — `GET /api/consultant/companies`, `GET /api/consultant/companies/{companyId}`
- `ConsultantJobController.java` — `GET /api/consultant/jobs`, `GET /api/consultant/jobs/{jobId}`
- `ConsultantShortlistController.java` — `GET /api/consultant/shortlists`, `GET /api/consultant/shortlists/{shortlistId}`

Auth logic (duplicated in each controller):
```java
// requireConsultantRole(actorRole):
if (!"consultant".equals(normalizedRole)) -> throw AccessDeniedException("consultant_role_required")

// parseOrganizationId(orgId):
parse UUID from header, fail on blank/invalid -> 400 BAD_REQUEST
```

#### Pattern B: Client-Safe API (1 controller)
**File:** `ClientSafeCandidateCardController.java` — `GET /api/client-safe/candidate-cards/{anonymousCardRef}`

Auth logic via `ClientSafeCandidateCardApiAccessContextAdapter`:
- Parse all 4 headers, build `AccessRequest` + `QueryScope`
- Only `client` role with `CLIENT_SAFE` or `GENERALIZED` field classification allowed
- Missing/invalid/malformed headers -> `403 FORBIDDEN` with sanitized error

#### Pattern C: Unauthenticated (1 controller)
**File:** `HealthController.java` — `GET /health`
No auth headers required.

### 1.3 Current FieldAccessPolicy Rules

All access decisions flow through `FieldAccessPolicy.decide(AccessRequest)`:

| Role | Resource | Action | Field Classification | Scope | Decision |
|------|----------|--------|---------------------|-------|----------|
| CLIENT | CLIENT_SAFE_CANDIDATE_CARD | READ | CLIENT_SAFE, GENERALIZED | any | ALLOW |
| CLIENT | any other | any | any | any | DENY |
| CANDIDATE | CANDIDATE_PROFILE | READ | CLIENT_SAFE, GENERALIZED | SELF | ALLOW |
| CANDIDATE | CANDIDATE_PROFILE | READ | any other | SELF | DENY |
| CANDIDATE | any other | any | any | any | DENY |
| CONSULTANT | COMPANY, JOB, SHORTLIST | READ | any | any | ALLOW |
| CONSULTANT | any other | any | any | any | DENY |
| OWNER, ADMIN, SYSTEM, AI_ASSISTANT | any | any | any | any | DENY (catch-all) |

Additional global denials:
- Any `UNKNOWN` role/resource/action/field -> DENY
- Any `identityDisclosureRequested=true` -> DENY ("not implemented")
- Any governance/automation role attempting canonical-write-like action -> DENY

### 1.4 Current PortalRole Values and Capabilities

| PortalRole | Wire Value | Current Effective Capabilities | Implied Portal |
|------------|-----------|-------------------------------|----------------|
| UNKNOWN | "unknown" | Denied always | N/A |
| OWNER | "owner" | Denied by catch-all (no allow rules) | Owner portal |
| CONSULTANT | "consultant" | READ on COMPANY, JOB, SHORTLIST (via header) | Consultant portal |
| CLIENT | "client" | READ on CLIENT_SAFE_CANDIDATE_CARD at safe levels (via header) | Client portal |
| CANDIDATE | "candidate" | READ on CANDIDATE_PROFILE at safe levels with SELF scope | Candidate portal |
| ADMIN | "admin" | Denied by catch-all (no allow rules) | Admin portal |
| SYSTEM | "system" | Denied by catch-all (no allow rules) | System automation |
| AI_ASSISTANT | "ai_assistant" | Denied by catch-all (no allow rules) | AI automation |

**Key observation:** Only CONSULTANT, CLIENT, and CANDIDATE have explicit allow rules. OWNER, ADMIN, SYSTEM, and AI_ASSISTANT are intentionally denied at the current evaluator scope — their allow rules will be added when they have real authenticated surfaces.

### 1.5 Existing `identity` Schema Status

**Correction to prompt assumption:** The `identity` schema **does exist** (created in V1 migration `V1__create_schema_namespaces.sql` as `CREATE SCHEMA IF NOT EXISTS identity;`). However, it contains **zero tables** — no subsequent migration has populated it. All 6 other schemas (`governance`, `recruiting`, `workflow`, `privacy`, `audit`, `intake`) are populated with tables.

### 1.6 Current Database Schema Namespaces

```
identity    — V1, empty (target for Task 19 tables)
recruiting  — V2, V10, V12: company, job, candidate, candidate_profile, etc.
governance  — V2, V11: ai_task_run, canonical_write_attempt, claim_ledger_item, etc.
workflow    — V2: workflow_event
privacy     — V8, V9: consent_record, unlock_decision, disclosure_record
audit       — V1, empty
intake      — V4: source_item, information_packet, etc.
```

---

## 2. Data Model Design

### 2.1 Schema: `identity`

Use the existing (but empty) `identity` schema. Proposed migration version: **V14** (after Task 20's expected V13). All identity tables go into the `identity` namespace.

### 2.2 Core Entities

#### 2.2.1 `identity.user`

```sql
CREATE TABLE identity.user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(320) NOT NULL,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash   VARCHAR(256) NOT NULL,
    display_name    VARCHAR(200),
    status          VARCHAR(30) NOT NULL DEFAULT 'active',
        -- active | suspended | deactivated | locked
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_user_email UNIQUE (email)
);
```

**Design decisions:**
- `id` uses UUID (consistent with all other entities in the codebase)
- `email` is UNIQUE across the system — one user identity per email
- `password_hash` stores bcrypt/scrypt/argon2 output (algorithm decided in Section 7)
- `status` supports lifecycle: `active`, `suspended`, `deactivated`, `locked` (for brute-force protection)
- `display_name` is optional and non-unique

#### 2.2.2 `identity.organization`

```sql
CREATE TABLE identity.organization (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    type            VARCHAR(30) NOT NULL,
        -- agency | company | platform
    status          VARCHAR(30) NOT NULL DEFAULT 'active',
        -- active | suspended | deactivated
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Design decisions:**
- `type`: `agency` (consultant firm), `company` (client company), `platform` (the system owner/admin org)
- `metadata` column follows the existing project pattern used in `governance.*` tables
- Organization `id` is the same UUID used across all existing `organization_id` columns in `recruiting.*`, `governance.*`, `privacy.*`, etc.
- No FK from `organization.id` to existing tables initially (the existing org_id columns are raw UUIDs without FK constraints to a common org table). This FK can be added as a follow-up hardening migration.

#### 2.2.3 `identity.membership`

```sql
CREATE TABLE identity.membership (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    organization_id UUID NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'active',
        -- active | suspended | removed
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at         TIMESTAMPTZ,

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id) REFERENCES identity.user(id),
    CONSTRAINT fk_membership_organization
        FOREIGN KEY (organization_id) REFERENCES identity.organization(id),
    CONSTRAINT uq_membership_user_org UNIQUE (user_id, organization_id)
);
```

**Design decisions:**
- Many-to-many: a user can belong to multiple organizations (e.g., a candidate might be a client at another company)
- `status` tracks active membership lifecycle
- `left_at` records when membership ended without deleting the row (auditability)
- The UNIQUE constraint on `(user_id, organization_id)` ensures one membership record per user-org pair

#### 2.2.4 `identity.role_assignment`

```sql
CREATE TABLE identity.role_assignment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    organization_id UUID NOT NULL,
    portal_role     VARCHAR(30) NOT NULL,
        -- owner | consultant | client | candidate | admin
    granted_by      UUID,  -- user_id of the admin who granted this role
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMPTZ,
    status          VARCHAR(30) NOT NULL DEFAULT 'active',
        -- active | revoked

    CONSTRAINT fk_role_assignment_user
        FOREIGN KEY (user_id) REFERENCES identity.user(id),
    CONSTRAINT fk_role_assignment_organization
        FOREIGN KEY (organization_id) REFERENCES identity.organization(id),
    CONSTRAINT uq_role_assignment_user_org_role
        UNIQUE (user_id, organization_id, portal_role)
        WHERE status = 'active'
);
```

**Design decisions:**
- Roles are assigned per-user per-organization (a user is a Consultant in their agency org, but might be a Candidate in their personal context)
- `portal_role` is a VARCHAR, not an ENUM — keeps schema flexible and aligned with the Java enum `PortalRole` wire values
- `SYSTEM` and `AI_ASSISTANT` roles are NOT user-assignable (only system-managed)
- `granted_by` is nullable for initial/seed role assignments
- Partial UNIQUE index on `(user_id, organization_id, portal_role) WHERE status = 'active'` prevents duplicate active role assignments

**Implementation note:** PostgreSQL partial indexes with WHERE clauses are standard. If the team prefers not to use partial indexes, the constraint can be enforced at the application layer.

#### 2.2.5 `identity.session` (JWT strategy — see Section 3)

If using a stateless JWT strategy, a session table may not be needed for authentication purposes. However, for refresh token tracking, audit, and forced logout capability:

```sql
CREATE TABLE identity.session (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    organization_id UUID NOT NULL,  -- "current" org context
    refresh_token_hash VARCHAR(256) NOT NULL,  -- hash of the refresh token
    access_token_id VARCHAR(64) NOT NULL,  -- jti claim from access token
    portal_role     VARCHAR(30) NOT NULL,
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    ip_address      VARCHAR(45),
    user_agent      TEXT,

    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id) REFERENCES identity.user(id),
    CONSTRAINT fk_session_organization
        FOREIGN KEY (organization_id) REFERENCES identity.organization(id)
);

CREATE INDEX idx_session_user_id ON identity.session(user_id);
CREATE INDEX idx_session_refresh_hash ON identity.session(refresh_token_hash);
```

**Design decisions:**
- `access_token_id` stores the JWT `jti` claim for access token revocation support
- `refresh_token_hash` stores a hash (not the raw token) for security
- `organization_id` captures which org the user was acting within for this session
- `portal_role` captures the role used within that organization
- This table enables: session listing, forced logout (revoke all sessions for a user), and audit
- The table approach co-exists with stateless JWT: access tokens remain stateless, but refresh tokens and session tracking are stateful

### 2.3 Entity Relationship Summary

```
User (1) ---< (N) Membership >--- (1) Organization
User (1) ---< (N) RoleAssignment >--- (1) Organization
User (1) ---< (N) Session >--- (1) Organization
```

- A `User` has zero or more `Membership` records to `Organization`s
- A `User` has zero or more `RoleAssignment` records scoped to `Organization`s
- A `User` has zero or more `Session` records for active login sessions
- An `Organization` can have many `User`s through `Membership`
- An `Organization` can have many `RoleAssignment`s for different users

### 2.4 Migration Planning

- **Proposed version:** V14 (assuming Task 20 creates V13)
- **Schema:** All tables in `identity` schema (already created in V1)
- **Migration file:** `V14__create_identity_and_auth_tables.sql`
- **Contents:** CREATE TABLE for `identity.user`, `identity.organization`, `identity.membership`, `identity.role_assignment`, `identity.session` (if session table chosen)
- **Seed data:** May need seed/or bootstrap `organization` row for the platform, and an initial admin user (explore Flyway repeatable migrations or application-level bootstrap)

---

## 3. Spring Security Integration Strategy

### 3.1 Recommended Approach: Spring Security with Stateless JWT + Refresh Tokens

**Rationale:**
- **Stateless JWT** for API access: No server-side session lookup per request. JWT is self-contained with claims (userId, organizationId, portalRole, scopes).
- **Stateful refresh tokens** (stored in `identity.session`): Enables forced logout, session auditing, and token revocation without losing the performance benefits of stateless access tokens.
- Spring Security is the industry standard for Java backend authentication. It provides the filter chain, security context, method security, and integration with the existing Spring Boot stack.

### 3.2 Dependency Addition

Add to `services/core-api/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT library (jjwt is a common choice; final decision in Section 7) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.x</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.x</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.x</version>
    <scope>runtime</scope>
</dependency>
```

### 3.3 Filter Chain Architecture

```
HTTP Request
    |
    v
SecurityFilterChain (Spring Security)
    |-- CorsFilter (CORS config)
    |-- JwtAuthenticationFilter (extract JWT from Authorization header)
    |       |-- Validate signature and expiry
    |       |-- Extract claims: userId, organizationId, portalRole
    |       |-- Create RtoAuthenticationToken (custom Authentication impl)
    |       |-- Set SecurityContextHolder
    |-- ExceptionTranslationFilter (convert auth failures to 403/401)
    |-- AuthorizationFilter (method security via @PreAuthorize etc.)
    v
Controller layer (unchanged from current; auth info from SecurityContext)
```

### 3.4 Custom Authentication Object

Replace the current header-based context with a Spring Security `Authentication`:

```java
public class RtoAuthenticationToken extends AbstractAuthenticationToken {
    private final UUID userId;
    private final UUID organizationId;
    private final PortalRole portalRole;
    // Optionally: Set<RelationshipScope> scopes from role assignment

    // Constructor for authenticated user
    public RtoAuthenticationToken(UUID userId, UUID organizationId,
                                   PortalRole portalRole, Set<RelationshipScope> scopes) {
        super(/* granted authorities derived from portalRole */);
        this.userId = userId;
        this.organizationId = organizationId;
        this.portalRole = portalRole;
        setAuthenticated(true);
    }

    @Override public Object getCredentials() { return null; }
    @Override public Object getPrincipal() { return userId; }
}
```

### 3.5 Replacing Header-Based Context

**Migration strategy: Coexistence with feature flag**

During Task 19 implementation:
1. Introduce a feature flag (e.g., `rto.auth.mode: jwt|header`) in application config
2. When `jwt`: Spring Security filter chain is active, JWT authentication is required, headers are ignored
3. When `header` (temporary, for development/testing): Fall back to current header-based auth
4. This allows incremental migration — controllers can be switched one at a time
5. After all controllers are migrated and tests pass, remove the header-based path entirely

**Controller transition plan:**

| Controller | Transition |
|------------|-----------|
| `HealthController` | No change (remains unauthenticated via `.permitAll()`) |
| `ClientSafeCandidateCardController` | Replace `@RequestHeader` parsing + adapter with `SecurityContext` injection + `PermissionEnforcer` |
| `ConsultantCompanyController` | Replace ` requireConsultantRole()` + `parseOrganizationId()` with `@PreAuthorize` or `SecurityContext` |
| `ConsultantJobController` | Same as above |
| `ConsultantShortlistController` | Same as above |

**Removal targets after migration complete:**
- `ClientSafeCandidateCardApiAccessContextAdapter.java` — no longer needed
- `requireConsultantRole()` and `parseOrganizationId()` helper methods in each consultant controller
- All `@RequestHeader` annotations for auth headers in controllers
- The `X-RTO-*` header parsing code in test setups

### 3.6 Test Compatibility

Tests that currently set auth headers will need to:
1. Either use the JWT approach (create a valid test token, set `Authorization: Bearer <token>` header)
2. Or use Spring Security's `@WithMockUser` / custom `@WithRtoUser` test annotation
3. Or set the `SecurityContext` directly via `SecurityContextHolder` in tests

Recommended approach: Create a test utility that provides either approach, and a `@WithRtoUser(role="CONSULTANT", organizationId="...")` annotation for clean test setup.

**Existing test count impact:** ~595 tests. The auth migration touches controller-level tests (~50-80 tests). Tests that test the PermissionEvaluator/FieldAccessPolicy directly (most of the identityaccess package tests) are unaffected since they operate at the domain layer, not the auth mechanism layer.

---

## 4. Organization Membership Model

### 4.1 Role-to-Organization Mapping

| PortalRole | Organization Type | Membership Pattern | How Organization Is Established |
|------------|------------------|-------------------|--------------------------------|
| CONSULTANT | agency | User is a member of an agency org | Membership + RoleAssignment for `consultant` role in that agency org |
| CLIENT | company | User is a member of a company org via CompanyContact | Membership + RoleAssignment for `client` role; org is the company org |
| CANDIDATE | n/a (self-owned) | Candidate has no required org membership; or uses a per-candidate "shadow" org | Candidate identity is self-contained; `organization_id` in CandidateProfile links to a conceptual "self" organization or the candidate's user id directly |
| OWNER | platform | Platform-level role, not scoped to an external org | RoleAssignment on the platform organization |
| ADMIN | platform | Platform-level role | RoleAssignment on the platform organization |
| SYSTEM | platform | Not user-assignable | System-managed |
| AI_ASSISTANT | platform | Not user-assignable | System-managed |

### 4.2 Candidate Self-Owned Identity

**Recommended approach: Self-org or direct user linkage**

Option A: **Shadow organization per candidate.** Create an organization row of type `candidate` and link membership. Pros: uniform model. Cons: proliferation of single-user orgs.

Option B (recommended): **User-to-candidate profile direct linkage.** Add a `user_id` FK on `recruiting.candidate` or `recruiting.candidate_profile` that links to `identity.user`. The candidate's "organization" context is derived from their user identity, not an organization membership. The `CANDIDATE` role is a role assignment on a conceptual "candidate-space" with no org FK.

**Recommendation: Option B** — simpler, avoids shadow orgs. The candidate's auth context sets `organizationId = null` or a sentinel value, and `portalRole = CANDIDATE`. Service-layer checks verify that the authenticated user's ID matches the candidate profile's `user_id`.

### 4.3 Consultant Agency Membership

```
User (id=u1)
  -> Membership (user_id=u1, organization_id=org_agency_a)
  -> RoleAssignment (user_id=u1, organization_id=org_agency_a, portal_role='consultant')
```

- A consultant authenticates, the system looks up their membership + active role assignment
- The organization_id from the membership becomes the org scope for all consultant API calls
- Multiple agency memberships: If a user belongs to multiple agency orgs, they must select an "active org" at login time or via a session-scoped org selector (MVP: only one active org per session)

### 4.4 Client Company Membership

```
User (id=u2)
  -> Membership (user_id=u2, organization_id=org_company_b)
  -> RoleAssignment (user_id=u2, organization_id=org_company_b, portal_role='client')
```

- Client companies are organizations of type `company`
- `CompanyContact` records in the existing `recruiting` schema can be linked to `identity.user` via a `user_id` FK (optional bridge table or direct FK)
- Client org membership determines which company's jobs and shortlists the client can see

### 4.5 Platform Roles (Owner, Admin)

```
User (id=u3)
  -> RoleAssignment (user_id=u3, organization_id=org_platform, portal_role='owner')
```

- `org_platform` is a bootstrap organization of type `platform`
- Owner and Admin roles are assigned on the platform org
- Owner has full business visibility; Admin has system governance visibility
- `SYSTEM` and `AI_ASSISTANT` are not user roles — they are used for internal service-to-service or background job authentication

### 4.6 Multi-Org Support

**Question: Can a user belong to multiple orgs?**

**Answer: Yes, but with explicit org selection.** The data model supports it (multiple `Membership` rows). The MVP approach:
1. At login, if user has exactly one active membership, auto-select it as session context
2. If user has multiple active memberships, require selection (or default to first)
3. Session records the selected `organization_id`
4. A future "org switcher" UI allows changing org context without re-login, by issuing a new access token with the new org claim

---

## 5. Controller Migration Plan

### 5.1 HealthController

**No changes.** Added to `.permitAll()` in Spring Security config.

### 5.2 ConsultantCompanyController, ConsultantJobController, ConsultantShortlistController

**Current pattern** (to be replaced):
```java
@RequestHeader(name = "X-RTO-Actor-Role", required = false) String actorRole,
@RequestHeader(name = "X-RTO-Organization-Id", required = false) String organizationId
// ... requireConsultantRole(actorRole); UUID orgId = parseOrganizationId(organizationId);
```

**New pattern:**
```java
// Inject via SecurityContext or method parameter
@GetMapping("/api/consultant/companies")
public ResponseEntity<ApiResponseEnvelope<PagedResult<ConsultantCompanySummaryResponse>>> listCompanies(
        @AuthenticationPrincipal RtoAuthenticationToken auth) {
    // auth.getPortalRole() == PortalRole.CONSULTANT (guaranteed by filter)
    // auth.getOrganizationId() is the org scope
    // No need for requireConsultantRole() or parseOrganizationId()
}
```

Alternatively, use a shared `@CurrentUser` or `@RtoContext` annotation to extract auth info:
```java
@GetMapping("/api/consultant/companies")
public ResponseEntity<...> listCompanies(@RtoAuth RtoSecurityContext context) {
    // context.role(), context.organizationId()
}
```

### 5.3 ClientSafeCandidateCardController

**Current pattern:**
```java
@RequestHeader("X-RTO-Actor-Role") String actorRole,
@RequestHeader("X-RTO-Field-Classification") String fieldClassification,
@RequestHeader("X-RTO-Identity-Disclosure-Requested") String identityDisclosureRequested,
@RequestHeader("X-RTO-Organization-Id") String organizationId
// ... adapter.fromHeaders(...), adapter.queryScopeFromHeaders(...)
```

**New pattern:**
```java
@GetMapping("/api/client-safe/candidate-cards/{anonymousCardRef}")
public ResponseEntity<...> getCandidateCard(
        @PathVariable String anonymousCardRef,
        @AuthenticationPrincipal RtoAuthenticationToken auth) {
    // auth.getPortalRole() == PortalRole.CLIENT (guaranteed by filter)
    // auth.getOrganizationId() is the org scope
    // Field classification and identity disclosure derived from role/context
    // PermissionEnforcer check using auth info
}
```

The `FieldClassification` can be determined by the endpoint itself (client-safe controller always uses CLIENT_SAFE). The `identityDisclosureRequested` flag can be a query parameter rather than a header.

### 5.4 Shared Auth Utilities

Extract common auth logic to avoid duplication:
- `RtoSecurityContext` — value object carrying `UUID userId, UUID organizationId, PortalRole portalRole`
- `RtoAuth` annotation + `RtoAuthArgumentResolver` — resolves `RtoSecurityContext` from `SecurityContextHolder`
- `@PreAuthorize("hasRole('CONSULTANT')")` on controller methods for declarative role enforcement

### 5.5 Test Migration Impact

| Test File | Impact |
|-----------|--------|
| `ConsultantControllerLeakageTest.java` | Change header setup to JWT token or `@WithRtoUser` setup |
| `ClientSafeCandidateCardControllerTest.java` | Change header setup to JWT token or `@WithRtoUser` setup |
| `ApiBoundaryRegressionClosureTest.java` | Header-based tests need auth mechanism update |
| `AccessControlContractTest.java` | No change (tests Policy/Evaluator, not auth mechanism) |
| `FivePortalBoundaryRegressionTest.java` | No change (tests Policy/Evaluator, not auth mechanism) |
| `PermissionEnforcerTest.java` | No change (tests enforcer logic, not auth mechanism) |

---

## 6. Cross-Org Boundary Design

### 6.1 Current State: Manual orgId Parameter

Currently, organization scope is enforced by:
1. Controller extracts `organization_id` from the `X-RTO-Organization-Id` header
2. Passes it as a `UUID` parameter through facade -> service -> persistence port
3. Each service/port implementation uses the org_id in WHERE clauses (e.g., `WHERE organization_id = ?`)
4. The `PermissionEnforcer` validates the `AccessRequest` but does NOT enforce org scope

### 6.2 Target State: Automatic Org Scope Injection

**Recommended approach: Service-layer enforcement, not just filter-level.**

After Spring Security integration:
1. JWT already contains `organization_id` as a claim
2. The filter extracts it and places it in `RtoAuthenticationToken`
3. At the **service facade** level (e.g., `ConsultantApiQueryService`), the `organization_id` is extracted from the security context and injected into queries
4. This ensures no service method can accidentally access data outside the authenticated org

**Should Spring Security filter inject org scope automatically?**
No. The filter should only:
- Extract claims from JWT
- Validate token signature/expiry
- Set the `SecurityContext`

Org scope enforcement belongs at the **service layer**, where it can use the org_id from the security context to build org-scoped queries. This keeps the filter simple and the business logic explicit.

### 6.3 Cross-Org Negative Test Strategy

Tests must prove:

| Test Category | What It Proves |
|--------------|----------------|
| **Wrong org JWT** | A JWT for org-A cannot access org-B's data via any API endpoint |
| **Missing org claim** | A JWT without organization_id claim is rejected at the filter level |
| **ID enumeration** | Guessing a valid company/job/candidate ID from another org returns 404 (not 403, to avoid leaking existence) |
| **Role escalation** | A user with `candidate` role cannot access consultant endpoints even with a valid org context |
| **Multi-org isolation** | A user belonging to both org-A and org-B can only access data for their currently selected org |
| **Cross-org write attempts** | Attempting to create/update data in an org the user doesn't belong to is rejected |
| **Null org for candidates** | Candidate endpoints enforce self-scope (user_id matches profile's linked user_id) |

**Test implementation pattern:**
```java
@Test
void consultantInOrgACannotReadJobsInOrgB() {
    // Given: two orgs, one job in each
    // Given: JWT for user in org-A
    // When: requesting GET /api/consultant/jobs/{job_in_org_B}
    // Then: 404 NOT_FOUND (to avoid leaking existence)
}
```

### 6.4 Existing Composite FK Hardening

Task 16-Hardening (V12) already added composite FKs across all product tables ensuring `parent.organization_id = child.organization_id` at the database level. This provides a DB-level safety net — even if the service layer fails to enforce org scope, the database rejects cross-org writes. Task 19 builds on this foundation by adding proper identity-based org scope enforcement at the application layer.

---

## 7. Open Questions

### Q1: JWT library choice — jjwt vs nimbus-jose-jwt vs auth0-jwt?

**Recommendation: jjwt (io.jsonwebtoken)**
- Pure Java, actively maintained, widely used
- Simple API for creating and parsing JWTs
- Built-in support for signing algorithms (HS256, RS256)
- Lighter weight than nimbus-jose-jwt for our use case

### Q2: Password hashing algorithm?

**Recommendation: bcrypt (via Spring Security's BCryptPasswordEncoder)**
- Industry standard, built into Spring Security
- Configurable work factor (default 10 rounds)
- No additional dependencies needed
- Argon2 is newer/better but requires additional dependencies and has less mature Spring Security integration

### Q3: OAuth/OIDC support needed for v1?

**Recommendation: No, defer to future task.**
- Task 19 scope is username/password + JWT
- OAuth adds significant complexity (provider config, callback URLs, user linking)
- Can be added later as a separate auth method alongside password-based login
- Document the OAuth integration point (Spring Security supports it transparently via the same `SecurityFilterChain`)

### Q4: Email verification required for v1?

**Recommendation: Required but with a seed/bootstrap bypass.**
- Production users should verify email before full access
- A `email_verified` flag on `identity.user` supports this
- For seed data and testing, bootstrap users can be pre-verified
- The verification token flow (generate token, send email, verify endpoint) can be a subtask

### Q5: Password reset flow?

**Recommendation: Yes, as a subtask.**
- Essential for real users
- Requires: email sending capability (abstraction), reset token generation, reset confirmation endpoint
- May depend on the email notification infrastructure from Task 34
- For MVP, can be simplified to an admin-initiated reset

### Q6: Session duration and refresh strategy?

**Recommendation:**
- Access token: 15-30 minutes (short-lived, stateless)
- Refresh token: 7 days (longer-lived, stateful, stored in `identity.session`)
- Refresh rotation: Issue new refresh token on each refresh, invalidate old one (prevents replay)
- Max session lifetime: 30 days (after which re-login is required)

### Q7: Rate limiting for login attempts?

**Recommendation: Yes, in scope.**
- Basic brute-force protection: lock account after N failed attempts within a time window
- Captcha or exponential backoff can be deferred
- `identity.user.status = 'locked'` with `locked_until` timestamp
- This is a security hardening subtask

### Q8: API key / service-to-service auth for SYSTEM and AI_ASSISTANT roles?

**Recommendation: Defer but plan for it.**
- SYSTEM and AI_ASSISTANT roles are not user-authenticated
- For internal service-to-service calls, an API key header (`X-RTO-Api-Key`) with a pre-shared secret can authenticate these roles
- Not needed for initial Task 19 scope (these roles have no active controllers yet)
- Document the integration point in the auth architecture

### Q9: Is the `identity` schema already populated?

**Correction to prompt:** The `identity` schema already exists (created in V1) but contains zero tables. Task 19 will populate it with its first tables (user, organization, membership, role_assignment, session). No schema creation step is needed — only table creation inside the existing namespace.

---

## 8. Task 19 Subtask Breakdown

Task 19 is high-risk (RBAC/ABAC, identity disclosure boundary). Per the operating rules, high-risk tasks should remain small-step. **Recommendation: 19A / 19B / 19C split.**

### Task 19A: Identity Data Model + Auth Infrastructure (V14 Migration + Domain + Security Config)

**Scope:** Database + domain model + Spring Security wiring only. No controller changes.

**Deliverables:**
1. V14 migration: `identity.user`, `identity.organization`, `identity.membership`, `identity.role_assignment`, `identity.session` tables
2. Java domain entities/records for User, Organization, Membership, RoleAssignment, Session
3. Persistence ports + JDBC adapters for all identity entities (create, findById, findByEmail, findByOrg, etc.)
4. Spring Security dependency in `pom.xml`
5. `SecurityConfig` class with filter chain configuration
6. `JwtService` (token creation, parsing, validation)
7. `RtoAuthenticationToken` (custom Authentication)
8. `JwtAuthenticationFilter` (extract JWT, set SecurityContext)
9. `RtoUserDetailsService` (load user/password/roles from identity tables)
10. `AuthenticationController` (`POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`)
11. Unit + integration tests for auth flow (login, token refresh, token expiry, invalid token denial)
12. Password encoder bean configuration (BCrypt)

**Forbidden:** No changes to existing controllers. No changes to existing ABAC/FieldAccessPolicy logic.

**Estimated files:** ~25-30 files (migration + domain + port + adapter + service + config + filter + controller + tests)

### Task 19B: Controller Migration + HEADER Removal + Test Migration

**Scope:** Replace header-based auth in all 4 gated controllers with Spring Security context. Remove header parsing code.

**Deliverables:**
1. Update all 4 gated controllers to use `@AuthenticationPrincipal` or `SecurityContextHolder` instead of header parsing
2. Remove `ClientSafeCandidateCardApiAccessContextAdapter.java`
3. Remove `requireConsultantRole()` and `parseOrganizationId()` helper methods from consultant controllers
4. Add Spring Security method security annotations (`@PreAuthorize`) where appropriate
5. Migrate all controller tests from header-based auth to JWT-based or `SecurityContext`-based auth
6. Update API boundary regression tests
7. Ensure all 595+ existing tests pass under the new auth mechanism
8. Feature flag support: `rto.auth.mode=header` fallback for development/testing

**Forbidden:** No changes to ABAC policy logic. No new endpoints. No data model changes.

**Estimated files:** ~20-25 files (controllers + tests + adapter removal)

### Task 19C: Cross-Org Hardening + Negative Tests + Docs Closeout

**Scope:** Security boundary testing and documentation.

**Deliverables:**
1. Cross-org negative integration tests (wrong org JWT, missing org claim, ID enumeration, role escalation, multi-org isolation, cross-org write blocks)
2. Rate limiting for login endpoint (brute-force protection)
3. Email verification flow (token generation + verification endpoint)
4. Password reset flow (token-based)
5. Update `current-engineering-snapshot.md` (advance next task to Task 20)
6. Update `implementation-status.md` (record Task 19A/19B/19C completion)
7. Update `known-gaps.md` (document any deferred identity/auth items)
8. Remove the header-based auth fallback (`rto.auth.mode` feature flag)

**Forbidden:** No new business features. No changes to non-auth controllers.

**Estimated files:** ~15-20 files (tests + docs)

---

## Summary

| Aspect | Current (Task 18A) | Target (Task 19 complete) |
|--------|-------------------|--------------------------|
| Auth mechanism | 4 request headers | JWT in Authorization header |
| User identity | None | `identity.user` table |
| Organization | Raw UUIDs only | `identity.organization` table |
| Login | None | `POST /api/auth/login` with email/password |
| Session | None | JWT access token (15min) + refresh token (7d) |
| Spring Security | Not in pom.xml | Configured SecurityFilterChain |
| Org scope | Manual header parsing | Extracted from JWT claims |
| Cross-org safety | Only V12 DB composite FKs | DB FKs + service-layer org enforcement |
| Role assignment | No persistence | `identity.role_assignment` table |
| Test auth | Header values | JWT tokens or SecurityContext mock |

---

## Validation Checklist for Task 19

1. V14 migration runs cleanly on empty database
2. `POST /api/auth/login` returns access + refresh tokens on valid credentials
3. `POST /api/auth/login` returns 401 on invalid credentials
4. `POST /api/auth/refresh` returns new access token with valid refresh token
5. `POST /api/auth/logout` invalidates refresh token
6. All existing consultant endpoints (6 total) work with JWT auth instead of headers
7. Client-safe card endpoint works with JWT auth instead of headers
8. Cross-org access is denied (org-A user cannot see org-B data)
9. ID enumeration returns consistent sanitized responses (404, not 403)
10. Expired tokens are rejected with 401
11. All 595+ existing tests pass
12. `current-engineering-snapshot.md`, `implementation-status.md`, `known-gaps.md` updated
