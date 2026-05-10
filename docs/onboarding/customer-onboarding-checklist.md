# Customer Onboarding Checklist

## Goal

Give the implementation lead a repeatable checklist for moving a new customer
from pilot kickoff to first controlled recruiting transaction without
engineering intervention, except approved integration configuration and
approved governed data import.

## Phase 0: Pilot Scope Lock

| Check | Owner | Evidence |
| --- | --- | --- |
| Name the customer organization and pilot sponsor. | Implementation lead | Signed pilot scope or written approval |
| Select 1-3 consultants, 1-3 client companies, and invited candidate users. | Customer owner | User roster with role and organization scope |
| Select one initial industry pack. | Customer owner | Industry pack decision; use `semiconductor` only when the pilot actually matches it |
| Identify pilot job families and excluded job families. | Customer owner | Pilot scope note |
| Confirm the pilot is controlled-pilot scope, not public production launch. | Implementation lead | Scope note links to `docs/roadmap/pilot-readiness-checklist.md` |
| Assign launch owner, admin owner, data owner, security approver, and escalation contact. | Customer owner | RACI table |

## Phase 1: Data and Privacy Intake

| Check | Owner | Evidence |
| --- | --- | --- |
| List source systems and document classes: CVs, JD files, notes, feedback, ATS/CRM exports, email summaries. | Data owner | Source inventory |
| Classify each source as candidate data, client/company data, job data, feedback, commercial data, or audit-sensitive data. | Data owner | Data classification sheet |
| Confirm whether real candidate personal data is in scope for this pilot. | Security approver | Written data approval |
| Confirm no production data import will happen before Task 55-compatible import approval. | Implementation lead | Import approval decision |
| Confirm client users must never see raw Candidate objects pre-disclosure. | Implementation lead | Privacy boundary acknowledgement |
| Confirm consent text version and shared-fields preview process for candidates. | Customer owner | Consent workflow approval |

## Phase 2: Organization and Access Setup

| Check | Owner | Evidence |
| --- | --- | --- |
| Create or select the customer organization. | Admin | Organization reference |
| Create Owner, Consultant, Client, Candidate, and Admin accounts only for approved pilot users. | Admin | User roster |
| Assign each user to exactly the required pilot role. | Admin | Role assignment record |
| Confirm Client users are tied to the correct company/job context. | Admin | Client access review |
| Confirm Candidate users are self-scoped only. | Admin | Candidate access review |
| Confirm Admin cannot be used to bypass canonical-write, disclosure, or domain-service gates. | Implementation lead | Risk review note |

## Phase 3: Configuration Setup

| Check | Owner | Evidence |
| --- | --- | --- |
| Select AI task routing and model configuration through Admin governance where available. | Admin | Model routing review |
| Confirm prompt/schema/task registry status for pilot tasks. | Admin | AI task registry review |
| Select workflow rules and SLA visibility that will be used in pilot. | Admin | Workflow rules review |
| Configure available integration status. | Admin | Admin `/admin/integrations` review |
| Record integration gaps that depend on Task 49. | Implementation lead | Dependency log |
| Confirm security controls from Task 41 are enabled for the environment used. | Security approver | Security setup note |

## Phase 4: Training

| Check | Owner | Evidence |
| --- | --- | --- |
| Consultants complete `consultant-training-flow.md`. | Training owner | Attendance and exercise results |
| Client users complete `client-training-flow.md`. | Training owner | Attendance and role exercise results |
| Candidate-facing staff can answer questions using `candidate-consent-faq.md`. | Training owner | FAQ review signoff |
| Admins complete `admin-setup-guide.md` walkthrough. | Training owner | Admin setup signoff |
| Everyone understands AI outputs claims, not facts. | Implementation lead | Training quiz or live confirmation |
| Everyone understands L3 consented detail is not L4 identity disclosure. | Implementation lead | Training quiz or live confirmation |

## Phase 5: Dry Run

Use the Task 38 deterministic pilot scenario before any real customer data.

```sh
rtk npm run pilot:data:rebuild
rtk npm run pilot:data:validate
rtk npm run pilot:data:export
RTO_PILOT_DATA_ALLOW_RESET=true rtk npm run pilot:data:reset
```

Dry-run flow:

1. Consultant logs in and reviews the candidate and job lists.
2. Consultant runs intake/review for a safe source packet where available.
3. Consultant generates or reviews matching for an active job.
4. Consultant builds an anonymous shortlist and checks redaction/risk status.
5. Candidate reviews opportunity/consent and confirms or declines.
6. Client reviews shortlist, requests unlock, and submits feedback.
7. Consultant approves only if consent, privacy, prior-contact, and commercial
   prerequisites are satisfied.
8. Admin reviews relevant audit, workflow, AI task, and integration surfaces.

## Phase 6: Launch Readiness

| Check | Owner | Evidence |
| --- | --- | --- |
| `risk-review-guide.md` has no launch-blocking unresolved risks. | Security approver | Risk decision |
| `data-import-guide.md` import decision is approved, deferred, or out of scope. | Data owner | Import decision |
| `go-live-checklist.md` is complete. | Launch owner | Go-live signoff |
| Support path is defined without direct database fixes. | Implementation lead | Support escalation path |
| Rollback/reset decision is understood for pilot data. | Launch owner | Reset/rollback note |
| First-week check-in schedule is booked. | Launch owner | Calendar or written schedule |

## Launch-Blocking Conditions

- A client user can access raw candidate identity before approved disclosure.
- A candidate consent request lacks opportunity, profile version, consent text
  version, or shared-fields preview.
- A consultant is instructed to treat AI output as confirmed fact.
- Real customer import requires direct database edits or unapproved tooling.
- Integration-dependent messaging is required but Task 49 configuration is not
  ready.
- The customer expects production security, DR, support, performance, reporting,
  or release guarantees that are not evidenced in the current branch.

