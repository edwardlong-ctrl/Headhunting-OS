# RC1 Pilot Readiness Evidence

## Run Metadata

| Field | Value |
| --- | --- |
| RC1 run started at | Mon Jun  8 14:21:34 CST 2026 |
| RC1 run id | not-recorded |
| Git commit | b15f652a4facbf803a936ede86f1569f09c411bd |
| Working tree status | `rtk git status --short` exit 0 on branch `main` at `b15f652a4facbf803a936ede86f1569f09c411bd`: `D docs/release/RC1-next-execution-checklist.md`; `?? docs/release/RC1-capability-map.md`; `?? docs/release/RC1-pilot-readiness-evidence.md`. All dirty files are RC1 `docs/release/` artifacts; no outside-`docs/release/` conflicts observed. |
| Operator | Codex |
| Scope | Controlled pilot transaction readiness |
| Decision | not-decided |

## Transaction Trace Ledger

Record IDs as they are created so later evidence can be correlated instead of treated as isolated screenshots.

| Entity | ID / Ref | Source Step | Evidence Artifact | Correlation Notes |
| --- | --- | --- | --- | --- |
| Organization | not-recorded | pilot data |  |  |
| Consultant account/session | not-recorded | login/session |  |  |
| Client account/session | not-recorded | login/session |  |  |
| Candidate account/session | not-recorded | login/session |  |  |
| Candidate | not-recorded | intake/seed |  |  |
| Candidate profile | not-recorded | intake/canonical |  |  |
| Source document | not-recorded | candidate intake |  |  |
| AI task run | not-recorded | AI draft claims |  |  |
| Claim ledger item | not-recorded | AI draft claims |  |  |
| Canonical write / reviewed fact | not-recorded | human review |  |  |
| Company | not-recorded | job/client intake |  |  |
| Job | not-recorded | job/client intake |  |  |
| Match report | not-recorded | matching |  |  |
| Shortlist | not-recorded | shortlist |  |  |
| Shortlist card | not-recorded | shortlist |  |  |
| Re-identification assessment | not-recorded | client-safe shortlist/card |  |  |
| Consent record | not-recorded | candidate consent |  |  |
| Unlock request | not-recorded | client unlock |  |  |
| Disclosure record | not-recorded | consultant approval |  |  |
| Interview feedback | not-recorded | feedback |  |  |
| Placement | not-recorded | placement |  |  |
| Commission | not-recorded | commission |  |  |
| Owner revenue/accounting evidence | not-recorded | owner commercial proof |  |  |
| Admin governance/audit evidence | not-recorded | admin governance proof |  |  |
| WorkflowEvent trace | not-recorded | audit |  |  |

## Consent / Unlock / Disclosure Evidence

| Step | Record ID | Profile Version | Consent Text Version | Disclosure Level | Human Approver | Reason | WorkflowEvent ID | Evidence Artifact |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Candidate consent | not-recorded | not-recorded | not-recorded | not-applicable | not-recorded | not-recorded | not-recorded |  |
| Client unlock request | not-recorded | not-applicable | not-applicable | not-recorded | not-recorded | not-recorded | not-recorded |  |
| Consultant disclosure approval | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded |  |

## Claim Ledger v2.1 Evidence

| Claim Ref | Source Span Present | Assertion Strength | Speaker | Verification Status | Canonical Write Allowed | Client Shareability | AITaskRun Ref | Review / WorkflowEvent Ref | Evidence Artifact | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded | not-recorded |  | not-verified |

## Pilot Role / Route Guard Matrix

| Portal Role | Route / API | Expected Outcome | Current Result | Evidence Artifact | Status |
| --- | --- | --- | --- | --- | --- |
| Consultant | candidate intake / review / placement / commission | allowed only for pilot consultant account | not-recorded |  | not-verified |
| Client | pre-unlock candidate card | client-safe only; no raw identity or internal governance data | not-recorded |  | not-verified |
| Client | raw candidate/profile UUID route | denied or safe unavailable | not-recorded |  | not-verified |
| Candidate | opportunity consent | allowed only for scoped candidate session | not-recorded |  | not-verified |
| Owner | placements / commission / revenue / accounting | allowed only for owner account and scoped to pilot organization | not-recorded |  | not-verified |
| Admin | audit / governance / workflow trace | allowed only for admin account and scoped to pilot organization | not-recorded |  | not-verified |

## MatchReport v2.1 Governance Evidence

| Requirement | Minimum Evidence | Status | Evidence Artifact | Blocker |
| --- | --- | --- | --- | --- |
| Evidence coverage is visible | E3/E4 when match report is exercised | not-verified |  |  |
| Provenance weighting is applied or explicitly unsupported | E1 plus E3/E4 | not-verified |  |  |
| Score confidence is visible | E3/E4 | not-verified |  |  |
| Authenticity risk is visible and affects score or next action | E1 plus E3/E4 | not-verified |  |  |
| Score caps are enforced for weak evidence/cold ontology/high risk | E1 plus E3/E4 | not-verified |  |  |
| Ontology / industry pack version and stale warning are visible or risk-accepted | E0 classification plus E3/E4 if implemented | not-verified |  |  |

## Browser Privacy Negative Evidence

| Check | Expected Result | Current Result | Evidence Artifact | Status |
| --- | --- | --- | --- | --- |
| Pre-unlock client page/API excludes name, email, phone, LinkedIn, exact employer, exact project/product/chip, raw source text, consultant notes, candidate/profile UUIDs, `WorkflowEvent`, and `ClaimLedger` | no forbidden strings present | not-recorded |  | not-verified |
| Raw candidate/profile UUID route attempt | safe denial or unavailable response | not-recorded |  | not-verified |
| Cross-organization anonymous-card reference | safe unavailable response, no data leakage | not-recorded |  | not-verified |
| Post-disclosure identity access | requires consent, unlock, consultant approval, and audit evidence | not-recorded |  | not-verified |

## Operations Readiness Evidence

| Check | Command / Action | Status | Evidence Artifact | Risk Note |
| --- | --- | --- | --- | --- |
| Deployment config validation | not-recorded | not-verified |  |  |
| Secrets/provider presence without values | not-recorded | not-verified |  |  |
| External provider posture | deterministic-only / live-configured / manual-channel-approved / out-of-scope | not-verified |  |  |
| Observability incident dry run | not-recorded | not-verified |  |  |
| Backup/restore or explicit not-current classification | not-recorded | not-verified |  |  |
| Rollback target | not-recorded | not-verified |  |  |
| First-week monitoring owner/cadence | not-recorded | not-verified |  |  |
| Dependency/security scan | `rtk npm audit --omit=dev` and dependency-check or bounded risk acceptance | not-verified |  |  |

## Scope Freeze

### Included

- Controlled pilot transaction chain from intake to commission.
- Local release gates and runtime smoke evidence.
- Capability mapping while testing.

### Excluded

- Public SaaS launch.
- Managed cloud signoff.
- Formal certification.
- Customer go-live approval.
- Production provider activation.
- Large new features or agent refactor.

## Spec-To-RC1 Traceability Status

| v2.1 Requirement | RC1 Task / Gate | Status | Evidence Row / Artifact | Risk Note |
| --- | --- | --- | --- | --- |
| v2.1 source of truth and v2.0 UI/portal preservation | RC1-00 / RC1-04 / RC1-10 | not-verified |  |  |
| Five portals and unified consultant portal | RC1-04 / RC1-07 / RC1-09 | not-verified |  |  |
| AI outputs claims, not facts | RC1-05 / RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| Claim Ledger field-level governance | RC1-05 / RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| Backend owns truth and PostgreSQL is source of truth | RC1-02 / RC1-03 / RC1-03A / RC1-05 / RC1-09 | not-verified |  |  |
| Every key transition creates WorkflowEvent | RC1-03 / RC1-05 / RC1-08A / RC1-09 / RC1-10 | not-verified |  |  |
| Risk-tiered review and anti-false-confirmation | RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| MatchReport v2.1 governance | RC1-06 / RC1-07 / RC1-09 / RC1-10 | not-verified |  |  |
| Client-safe shortlist and re-identification risk | RC1-06 / RC1-06B / RC1-07 / RC1-09 | not-verified |  |  |
| Consent, unlock, disclosure, and raw Candidate block | RC1-06 / RC1-06B / RC1-07 / RC1-09 | not-verified |  |  |
| Prior contact/application and transaction protection | RC1-06 / RC1-09 / RC1-10 | not-verified |  |  |
| Placement, commission, revenue/accounting handoff | RC1-08A / RC1-09 / RC1-10 / RC1-11 | not-verified |  |  |
| Owner commercial proof and Admin governance/audit/replay proof | RC1-08A / RC1-08B / RC1-09 / RC1-10 / RC1-11 | not-verified |  |  |
| Operations readiness for controlled local pilot | RC1-08B / RC1-11 | not-verified |  |  |

## Gate Evidence

| Gate | Owner | Command / Action | Run At | Exit Code / Result | Status | Evidence Level | Evidence Artifact | Transaction IDs | Blocker | Rollback / Cleanup | Next Action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Working tree freeze | Release owner | `rtk git rev-parse --abbrev-ref HEAD`; `rtk git rev-parse HEAD`; `rtk git status --short` | Fri Jun 12 14:49:25 CST 2026 | exit 0; branch `main`; commit `b15f652a4facbf803a936ede86f1569f09c411bd`; dirty files: `D docs/release/RC1-next-execution-checklist.md` (existing RC1 docs deletion), `?? docs/release/RC1-capability-map.md` (untracked RC1 evidence/capability artifact), `?? docs/release/RC1-pilot-readiness-evidence.md` (untracked RC1 evidence artifact); no additional dirty files observed | passed | E0 | inline git snapshot summary | not-applicable |  | none; read-only git inspection | Proceed to RC1-02 Docker/Testcontainers gate |
| Docker/Testcontainers | Release owner | `rtk proxy open -a Docker`; `rtk docker version`; `rtk docker info` | Fri Jun 12 15:03:53 CST 2026 | exit 0; Docker client/server reachable; client 29.4.1 on context `desktop-linux`; Docker Desktop 4.71.0 (225177); Engine/Server Version 29.4.1; server `docker-desktop`, Docker Desktop linux/aarch64; `docker info` observed 2 running containers and 5 images, with no containers created or started by this task | passed | E0 | docs/release/rc1-artifacts/RC1-02-docker.txt | not-applicable |  | none; Docker Desktop launch request and read-only Docker inspection; no containers started | Proceed to RC1-03 Migration validation gate |
| Migration validation | Backend owner | `rtk npm run release:migrations` | Fri Jun 12 15:09:35 CST 2026 | exit 0; Flyway filename validation reported 34 migrations contiguous from V1 to V34; `TruthLayerPostgresMigrationIntegrationTest` ran against Testcontainers PostgreSQL 16.13, validated 34 migrations, applied 34 migrations to schema `public` through v34, and reported `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` with `BUILD SUCCESS` | passed | E2 | docs/release/rc1-artifacts/RC1-03-release-migrations.txt | not-applicable |  | Testcontainers disposable PostgreSQL via Ryuk on JVM exit; no manual cleanup performed | Proceed to RC1-03A Pilot data contract gate |
| Pilot data contract | Backend owner | `RC1-03A rebuild + validate` | not-recorded | not-recorded | not-verified | E2 |  | pilot org / dataset refs |  | reset isolated datasource if approved |  |
| Frontend regression | Frontend owner | `rtk npm --workspace @rto/web run test` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | none |  |
| Frontend route preservation | Frontend owner | `portalRouteContract.test.ts` within web test | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | none |  |
| Frontend typecheck | Frontend owner | `rtk npm run typecheck:web` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | none |  |
| Frontend build | Frontend owner | `rtk npm run build:web` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | build artifact cleanup if needed |  |
| Backend regression | Backend owner | `rtk npm run test:core-api` | not-recorded | not-recorded | not-verified | E1/E2 |  | not-applicable |  | test containers cleanup |  |
| Backend build | Backend owner | `rtk npm run build:core-api` | not-recorded | not-recorded | not-verified | E1 |  | not-applicable |  | target cleanup if needed |  |
| Privacy/security negative | Security owner | `rtk npm run release:privacy-security` | not-recorded | not-recorded | not-verified | E1/E2 |  | relevant transaction IDs if exercised |  | test containers cleanup |  |
| Browser privacy negative | Security owner | RC1-06B browser/API checks | not-recorded | not-recorded | not-verified | E3/E4 |  | candidate/profile/shortlist/disclosure refs |  | remove unsafe screenshots/logs |  |
| AI eval regression | AI governance owner | `rtk npm run release:ai-eval` | not-recorded | not-recorded | not-verified | E1 |  | AI task keys |  | none |  |
| Pilot browser E2E | Release owner | `rtk npm run release:e2e:pilot` | not-recorded | not-recorded | not-verified | E4 |  | pilot transaction refs |  | API/web/DB process cleanup |  |
| Commercial closure | Backend / frontend owners | placement and commission runtime/API plus persistence/audit evidence | not-recorded | not-recorded | not-verified | E3 plus persistence/audit |  | placement/commission/WorkflowEvent refs |  | reset pilot data if approved |  |
| Full release gate | Release owner | `rtk npm run release:gate` | not-recorded | not-recorded | not-verified | E4 |  | not-applicable |  | release gate cleanup |  |
| Operations readiness | Ops / release owners | RC1-08B ops checklist | not-recorded | not-recorded | not-verified | E0-E3 |  | not-applicable |  | remove unsafe artifacts |  |

## Golden Path Evidence

| Step | Status | Evidence Level | User Surface | API/Service Evidence | Persistence Evidence | Workflow/Audit Evidence | Blocker | Next Action |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Consultant candidate intake | not-verified | E0 |  |  |  |  |  |  |
| AI draft claims | not-verified | E0 |  |  |  |  |  |  |
| Human review | not-verified | E0 |  |  |  |  |  |  |
| Canonical/truth write | not-verified | E0 |  |  |  |  |  |  |
| Client/job intake | not-verified | E0 |  |  |  |  |  |  |
| Job clarification/activation | not-verified | E0 |  |  |  |  |  |  |
| Match report | not-verified | E0 |  |  |  |  |  |  |
| Anonymous shortlist | not-verified | E0 |  |  |  |  |  |  |
| Candidate consent | not-verified | E0 |  |  |  |  |  |  |
| Client unlock request | not-verified | E0 |  |  |  |  |  |  |
| Consultant disclosure approval | not-verified | E0 |  |  |  |  |  |  |
| Disclosed client view | not-verified | E0 |  |  |  |  |  |  |
| Interview feedback | not-verified | E0 |  |  |  |  |  |  |
| Placement | not-verified | E0 |  |  |  |  |  |  |
| Commission/revenue | not-verified | E0 |  |  |  |  |  |  |
| Owner commercial/accounting proof | not-verified | E0 |  |  |  |  |  |  |
| Admin governance/audit/replay proof | not-verified | E0 |  |  |  |  |  |  |

## P0 Proof Matrix

| Requirement | Minimum Evidence | Status | Evidence | Blocker |
| --- | --- | --- | --- | --- |
| AI cannot directly own canonical facts | E1 plus E3/E4 when exercised | not-verified |  |  |
| Claim Ledger fields before canonical write | E1 plus E3 when exercised; source span, assertion strength, speaker, verification status, canonical-write permission, and client shareability are recorded | not-verified |  |  |
| Client cannot read raw Candidate before unlock/disclosure | E1 plus E4 | not-verified |  |  |
| Consent is required before identity disclosure | E1 plus E4 | not-verified |  |  |
| Unlock/disclosure creates auditable state | E3 plus persistence/audit evidence | not-verified |  |  |
| Canonical write creates or preserves truth-layer evidence | E3 plus persistence/audit evidence | not-verified |  |  |
| Risk-tiered review / bulk approve downgrade | E1 plus persisted review/audit signal when exercised | not-verified |  |  |
| MatchReport v2.1 governance | E1 plus E3/E4 for evidence coverage, provenance, confidence, authenticity risk, score caps, ontology version | not-verified |  |  |
| Client-safe re-identification gate | E1 plus E3/E4 and assessment/WorkflowEvent evidence | not-verified |  |  |
| Prior contact/application protection | E1 plus E3 when exercised, or explicit not-exercised note | not-verified |  |  |
| Placement creates auditable commercial state | E3 plus persistence/audit evidence | not-verified |  |  |
| Commission/revenue surface matches placement evidence | E3 plus persistence/audit evidence | not-verified |  |  |
| Owner commercial/accounting proof correlates to placement/commission | E3 plus persistence/audit evidence | not-verified |  |  |
| Admin governance/audit/replay proof correlates to transaction ledger | E3 plus WorkflowEvent/audit evidence | not-verified |  |  |
| Operations readiness has current bounded evidence | E0-E3 depending on subcheck; no privacy/security/data-loss waiver-only blockers | not-verified |  |  |

## Blocker List

| ID | Severity | Type | Evidence | Owner | Minimal Next Action | Status |
| --- | --- | --- | --- | --- | --- | --- |

## RC1 Decision

- Decision: not-decided
- Passed evidence:
- Blocking evidence:
- Not verified:
- Recommended next action:
