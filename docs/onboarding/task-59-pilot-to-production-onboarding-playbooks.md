# Task 59 Pilot-to-Production Onboarding Playbooks

## Purpose

Task 59 turns the controlled-pilot operating model into repeatable customer
onboarding. The target is not public SaaS launch. The target is a new pilot
customer that can be onboarded by implementation, operations, and customer
success staff without engineering intervention except:

- configured integrations approved under Task 49.
- approved data import executed through governed import tooling under Task 55.

The product direction remains consultant-first AI Recruiting Transaction OS:
AI assists intake, matching, follow-up, and workflow coordination, while
humans and backend service gates own facts, consent, disclosure, commercial
terms, and audit-sensitive state transitions.

## Source Documents

- `docs/specs/CURRENT_SPEC.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/specs/v2.0/product-spec-v2.0.md`
- `docs/roadmap/productization-roadmap.md`
- `docs/roadmap/current-engineering-snapshot.md`
- `docs/roadmap/pilot-readiness-checklist.md`
- `docs/pilot/task-38-pilot-scenario.md`
- `docs/security/client-safe-data-boundary.md`
- `docs/security/task-41-security-privacy-hardening-v1.md`

## Playbook Packet

| Playbook | Audience | When used |
| --- | --- | --- |
| `customer-onboarding-checklist.md` | Implementation lead, customer owner | From signed pilot scope through first controlled workflow |
| `consultant-training-flow.md` | Customer consultants, team leads | Before consultants run real candidate, company, job, shortlist, and feedback work |
| `client-training-flow.md` | Client HR, TA, hiring managers | Before client users submit jobs, review shortlists, request unlock, and give feedback |
| `candidate-consent-faq.md` | Candidate-facing support and consultants | Before sending opportunity and consent requests |
| `admin-setup-guide.md` | System admin, customer success, implementation | Before inviting users or enabling customer-specific configuration |
| `data-import-guide.md` | Implementation, operations, data owner | Before any historical data or document import |
| `risk-review-guide.md` | Owner, admin, compliance, implementation | Before pilot go-live and before high-risk disclosure or import decisions |
| `go-live-checklist.md` | Pilot launch owner | Final go/no-go and first-week operating checks |

## Current Capability Boundaries

Task 42 established `CONTROLLED_PILOT_READY` for the Task 42 usable-v1 gate.
That does not equal public production readiness.

| Area | Current onboarding stance |
| --- | --- |
| Five portals | Route-depth and controlled-pilot surfaces are available in the current baseline; preserve Owner, Consultant, Client, Candidate, and Admin as the five portal taxonomy. |
| Consultant workflow | Use the unified Consultant portal for intake, jobs, matching, shortlist, workflow, follow-ups, placements, and commission surfaces that exist in the current product slice. |
| Client workflow | Use the Client portal for company profile/preferences, job submission, clarification, shortlist review, unlock request, and feedback surfaces that exist in the current product slice. |
| Candidate workflow | Use Candidate portal surfaces for `/candidate/opportunities/:opportunityId`, `/candidate/consent/:requestId`, `/candidate/follow-up/:formId`, `/candidate/upload`, `/candidate/profile/ai-review`, and `/candidate/status`; do not describe broader candidate self-registration or external notification delivery as complete. |
| Admin governance | Use Admin governance sections for `/admin/ai-task-registry`, `/admin/workflow-rules`, `/admin/integrations`, `/admin/security`, `/admin/audit-log`, `/admin/model-routing`, and `/admin/industry-packs`; if a setup decision needs deeper governance console behavior, record the dependency instead of involving engineering ad hoc. |
| Security | Task 41 is a controlled-pilot security baseline only. Task 52 remains required before production security claims. |
| Data import | Task 38 deterministic pilot data exists. Real customer import and migration depend on Task 55 tooling and approval. |
| Integrations | Real email, SMS, calendar, OCR/STT, ATS/HRIS, webhook, and safe outbound integration depth depends on Task 49. |
| Commercial operations | Placement, invoice, guarantee, commission, and accounting export depth depends on Task 48. |
| DR and performance | Production DR and load/cost certification depend on Tasks 53 and 54. |
| Support tooling | Support action tooling, resend/retry, and audited correction workflows depend on Task 56. |
| Reports and exports | Productized report/legal-audit packages depend on Task 57. |
| Release gate | Repeatable release evidence depends on Task 58. |

## Onboarding Sequence

1. Confirm customer pilot scope, roles, data classes, integrations, and risk
   assumptions using `customer-onboarding-checklist.md`.
2. Configure the customer organization, users, role assignments, AI routing,
   industry pack, and visible Admin governance surfaces using
   `admin-setup-guide.md`.
3. Run data discovery and import readiness using `data-import-guide.md`. Do not
   import production customer data until the import is approved and the Task 55
   path is available for the requested source type.
4. Train consultants with `consultant-training-flow.md`; consultants must pass
   the privacy, review, shortlist, and disclosure exercises before real work.
5. Train client users with `client-training-flow.md`; client users must
   understand anonymous review, unlock request, feedback, and data boundaries.
6. Use `candidate-consent-faq.md` as the approved candidate-facing explanation
   source for opportunity and consent questions.
7. Complete risk review using `risk-review-guide.md`.
8. Launch only after `go-live-checklist.md` has no open launch-blocking items.

## Operating Rules

- No customer-facing onboarding material may say AI writes facts directly.
- No training may instruct a user to bypass review, consent, disclosure,
  prior-contact, or fee-protection gates.
- No client training may show raw candidate identity or raw evidence before the
  correct consent and disclosure level.
- No real customer import may use direct database edits as an onboarding path.
- No launch may claim production security, DR, performance, integrations,
  import, support, reporting, or release readiness unless the corresponding
  task evidence exists in the current branch.
