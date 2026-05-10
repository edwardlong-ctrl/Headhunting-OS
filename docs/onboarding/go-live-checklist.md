# Go-Live Checklist

## Goal

Give the launch owner a final controlled-pilot go/no-go checklist for a new
customer onboarding. This checklist does not certify public production launch.

## T-5 to T-3 Business Days: Scope and Setup

| Check | Owner | Status |
| --- | --- | --- |
| Customer pilot scope is approved and limited to named users, jobs, companies, and data classes. | Launch owner | Not started / In progress / Complete |
| Organization setup is complete. | Admin | Not started / In progress / Complete |
| User roster and role assignment review is complete. | Admin | Not started / In progress / Complete |
| Industry pack selection and maturity are recorded. | Implementation lead | Not started / In progress / Complete |
| Integration status is recorded as configured, manual channel, deferred, or out of scope. | Admin | Not started / In progress / Complete |
| Data import decision is approved, deferred, or out of pilot scope. | Data owner | Not started / In progress / Complete |

## T-3 to T-2 Business Days: Training and Dry Run

| Check | Owner | Status |
| --- | --- | --- |
| Consultant training is complete and signed off. | Training owner | Not started / In progress / Complete |
| Client training is complete and signed off. | Training owner | Not started / In progress / Complete |
| Candidate-facing FAQ is reviewed by consultants/support. | Training owner | Not started / In progress / Complete |
| Admin setup walkthrough is complete. | Admin | Not started / In progress / Complete |
| Synthetic or sanitized dry run completed. | Implementation lead | Not started / In progress / Complete |
| Dry-run privacy checks found no raw candidate leakage to Client role. | Security approver | Not started / In progress / Complete |

## T-2 to T-1 Business Days: Risk Review

| Check | Owner | Status |
| --- | --- | --- |
| Risk review has no red risks. | Security approver | Not started / In progress / Complete |
| Yellow risks have owners, mitigations, and dates. | Launch owner | Not started / In progress / Complete |
| Consent/disclosure path is verified for pilot flow. | Implementation lead | Not started / In progress / Complete |
| Prior-contact, prior-application, and fee-protection review expectations are documented. | Customer owner | Not started / In progress / Complete |
| Support escalation path is documented without direct database fixes. | Implementation lead | Not started / In progress / Complete |
| First-week operating cadence is scheduled. | Launch owner | Not started / In progress / Complete |

## Launch Day

| Check | Owner | Status |
| --- | --- | --- |
| Environment and login access are available. | Admin | Not started / In progress / Complete |
| Pilot users can reach their correct portal. | Training owner | Not started / In progress / Complete |
| Consultant can see assigned jobs, candidates, and workflow queue. | Consultant lead | Not started / In progress / Complete |
| Client can submit or review a pilot job. | Client lead | Not started / In progress / Complete |
| Candidate can open opportunity/consent or follow-up surface where used. | Consultant lead | Not started / In progress / Complete |
| Admin can review `/admin/ai-task-registry`, `/admin/workflow-rules`, `/admin/integrations`, `/admin/audit-log`, `/admin/security`, and dependency notes for any governance console behavior not present in the current branch. | Admin | Not started / In progress / Complete |
| Launch owner confirms open dependencies are not launch blockers. | Launch owner | Not started / In progress / Complete |

## First Transaction Checks

Complete these during the first real transaction:

1. Source data enters through the approved intake or import path.
2. AI-generated or imported information is reviewed as claims before canonical
   trust.
3. Matching output includes evidence and score-cap review.
4. Shortlist card is anonymous and client-safe.
5. Candidate consent is tied to the correct opportunity, profile version,
   consent text version, and shared fields.
6. Client unlock request is reviewed before disclosure.
7. Consultant approval does not bypass prior-contact, prior-application,
   privacy, or commercial prerequisites.
8. Disclosure creates the required audit evidence.
9. Client feedback returns through the workflow and does not directly overwrite
   confirmed facts.
10. Support issues follow the documented escalation path.

## No-Go Conditions

Do not launch if any are true:

- Client can access raw candidate identity before approved disclosure.
- Candidate consent wording or shared-fields preview is unclear.
- Real customer import is required but not approved or not supported by governed
  tooling.
- Required messaging/integration path is not configured and no manual channel
  is approved.
- Consultants did not complete privacy, shortlist, and disclosure training.
- Customer expects production security, DR, support, performance, reporting, or
  release guarantees that are not evidenced in the current branch.
- Support plan relies on direct database fixes.
- Any red risk remains open.

## First-Week Monitoring

Review daily for the first pilot week:

- Login/access issues by role.
- Intake packets waiting for review.
- Claims rejected or held for follow-up.
- Shortlist cards blocked for privacy risk.
- Consent requests viewed, confirmed, declined, expired, or revoked.
- Unlock requests approved, blocked, or delayed.
- Client feedback submitted and routed.
- AI task failures or schema validation failures.
- Integration or notification failures.
- Support escalations and resolution owner.

## Launch Closeout

At the end of week one, record:

- Number of jobs, candidates, shortlists, consent requests, unlock requests,
  disclosures, feedback events, and support issues.
- Any raw leakage, consent, disclosure, prior-contact, commercial, import,
  integration, or access incident.
- Training gaps.
- Product gaps that map to Tasks 48-58.
- Decision: continue pilot, restrict scope, pause, or escalate before broader
  rollout.
