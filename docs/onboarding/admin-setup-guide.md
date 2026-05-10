# Admin Setup Guide

## Goal

Set up a new pilot customer organization with the correct users, role
boundaries, governance visibility, integration status, and launch controls
without changing product code.

## Before Setup

Read:

- `docs/onboarding/customer-onboarding-checklist.md`
- `docs/onboarding/risk-review-guide.md`
- `docs/roadmap/pilot-readiness-checklist.md`
- `docs/security/client-safe-data-boundary.md`
- `docs/security/task-41-security-privacy-hardening-v1.md`

Confirm:

- Customer pilot scope is approved.
- Data import decision is approved, deferred, or out of scope.
- Integration expectations are documented.
- The environment is the intended pilot environment.
- The launch owner knows this setup does not certify public production
  readiness.

## Step 1: Organization Setup

1. Create or select the customer organization.
2. Record organization name, owner, billing/contact owner, security contact,
   and pilot launch owner.
3. Select the initial industry pack.
4. Record whether the selected pack is production-calibrated, seeded, cold, or
   pending review in the current baseline.
5. Confirm organization-scoped access and audit assumptions with the risk
   reviewer.

## Step 2: Role Setup

Create only approved pilot accounts.

| Role | Setup rule |
| --- | --- |
| Owner / Partner | Can review business pipeline, risk, revenue, team quality, and audit summaries according to policy |
| Consultant | Main transaction operator; can work candidate, company, job, matching, shortlist, workflow, follow-up, placement, and commission surfaces in one unified portal |
| Client | Scoped to its company/job/shortlist context; cannot access raw Candidate objects before disclosure |
| Candidate | Self-scoped to own profile, opportunities, consent, follow-up, upload/profile review, and status surfaces |
| Admin / System | Configures governance surfaces; cannot bypass canonical-write, disclosure, consent, or domain-service gates by role alone |

Access review:

- Confirm no shared pilot accounts.
- Confirm client users are not assigned Consultant or Admin roles.
- Confirm candidates cannot see other candidates, client-internal notes, or
  commercial terms.
- Confirm Admin use is limited to setup, governance review, and audit support.

## Step 3: Portal Surface Check

Verify the five portal taxonomy remains intact:

- Owner: `/owner/dashboard`, `/owner/pipeline`, `/owner/risk`,
  `/owner/data-quality`, `/owner/ai-quality`, `/owner/audit`, `/owner/revenue`,
  `/owner/placements`, and `/owner/commission`.
- Consultant: `/consultant/dashboard`, `/consultant/intake`,
  `/consultant/talent`, `/consultant/companies`, `/consultant/jobs`,
  `/consultant/matching`, `/consultant/shortlists`, `/consultant/follow-ups`,
  `/consultant/workflow`, `/consultant/placements`, `/consultant/commission`.
- Client: `/client/dashboard`, `/client/profile`, `/client/preferences`,
  `/client/jobs/new`, `/client/jobs/:jobId`, `/client/jobs/:jobId/clarification`,
  `/client/jobs/:jobId/shortlist`, `/client/shortlists`,
  `/client/shortlists/:shortlistId`, `/client/unlock/:candidateId`,
  `/client/feedback/:interviewId`, `/client/follow-ups`, and disclosed-candidate
  read surfaces after approved unlock.
- Candidate: `/candidate/home`, `/candidate/upload`,
  `/candidate/profile/ai-review`, `/candidate/opportunities/:opportunityId`,
  `/candidate/consent/:requestId`, `/candidate/status`, and
  `/candidate/follow-up/:formId`.
- Admin: `/admin/ai-task-registry`, `/admin/workflow-rules`,
  `/admin/integrations`, `/admin/security`, `/admin/audit-log`,
  `/admin/industry-packs`, `/admin/model-routing`, `/admin/privacy-redaction`,
  and related governance sections. Deeper production console behavior remains
  Task 50 scope when not present in the current branch.

Do not create a sixth portal for onboarding or governance. v2.1 governance
belongs inside the existing five-portal model.

## Step 4: AI Governance Setup

1. Review AI task registry status in Admin.
2. Confirm model routing for the organization where Admin model routing is
   available.
3. Confirm no AI task route is configured to write confirmed facts directly.
4. Confirm AI task outputs are reviewed as claims, suggestions, or audited task
   outputs before write-back.
5. Confirm prompt/schema/task versions are visible for pilot tasks in
   `/admin/ai-task-registry`; if a task lacks the needed governance evidence,
   record it as a Task 50 or Task 58 dependency before launch signoff.
6. Record any model/provider dependency that requires Task 49 or operations
   setup.

## Step 5: Workflow and Privacy Setup

1. Review workflow rules and SLA visibility.
2. Confirm shortlist send, consent, disclosure, unlock, feedback, interview,
   offer, invoice, and guarantee workflow expectations are documented.
3. Confirm client-safe redaction rules with the privacy reviewer.
4. Confirm L4 disclosure cannot happen without consent and consultant approval.
5. Confirm prior-contact, prior-application, and fee-protection review
   expectations.

## Step 6: Integration Setup

Record each integration as one of:

- configured and verified for pilot.
- manual channel approved for pilot.
- deferred until Task 49.
- out of pilot scope.

Integration classes:

- Email provider.
- SMS provider or a Task 49-approved manual/deferred communication path.
- Calendar integration.
- OCR/STT service.
- ATS/HRIS import/export.
- WeChat or email-safe summary export.
- Webhook/event integration.

No integration may write confirmed facts directly or send unaudited outbound
sensitive data.

## Step 7: Security Setup

Confirm Task 41 controlled-pilot controls are enabled where applicable:

- Login payload validation.
- Rate limiting for auth and consultant document endpoints.
- Unsafe upload filename rejection.
- URL path masking for UUID and email-like segments in request logs.
- Explicit Admin disclosure-audit export permission.
- Persistent access audit for wired sensitive document/export surfaces.

Record remaining security gaps as dependencies on Task 52. Do not call this a
production security certification.

## Step 8: Data Setup

For deterministic dry run, use the Task 38 pilot scenario:

```sh
rtk npm run pilot:data:rebuild
rtk npm run pilot:data:validate
rtk npm run pilot:data:export
```

For real customer data, follow `data-import-guide.md`. Do not use direct
database edits as onboarding setup.

## Admin Setup Signoff

Setup is ready for training only when:

- Organization and role roster match the approved scope.
- Portal access has been checked for all five user classes.
- Admin governance, workflow, AI routing, integration status, and security
  status have written setup notes.
- Import decision is approved, deferred, or out of scope.
- Risk review has no launch-blocking setup issue.
