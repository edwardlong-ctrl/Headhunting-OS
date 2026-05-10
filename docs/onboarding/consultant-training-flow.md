# Consultant Training Flow

## Goal

Train consultants to run the AI Recruiting Transaction OS as a governed
transaction workflow, not as a chat assistant or generic ATS. A consultant can
graduate from training only after demonstrating privacy-safe intake, review,
matching, shortlist, consent, disclosure, and feedback behavior.

## Prerequisites

- Consultant account exists and is scoped to the pilot organization.
- Pilot job and candidate data are available through the controlled pilot setup
  or an approved customer import.
- Trainer has access to the customer onboarding checklist, risk review guide,
  and candidate consent FAQ.
- Integration-dependent outbound messaging is either configured through Task 49
  work or explicitly replaced by manual customer-approved communication.

## Training Agenda

| Module | Time | Surface | Outcome |
| --- | --- | --- | --- |
| Product operating model | 15 min | Spec and checklist | Consultant can explain AI claims vs facts and human/audit governance |
| Consultant workspace | 20 min | `/consultant/dashboard`, `/consultant/intake`, `/consultant/talent`, `/consultant/jobs` | Consultant can find daily work, candidate, company, job, and intake surfaces |
| Governed intake review | 35 min | Intake review and source highlight surfaces | Consultant can approve, reject, or request follow-up without overstating claim status |
| Job and scorecard workflow | 25 min | `/consultant/jobs`, job detail, activation gate | Consultant can prepare a job without bypassing activation or missing prerequisites |
| Matching and evidence | 30 min | Job matching surface | Consultant can read score caps, evidence coverage, provenance, authenticity risk, and ontology status |
| Shortlist and privacy | 35 min | `/consultant/shortlists`, card builder/send flow | Consultant can build a client-safe anonymous shortlist and block unsafe cards |
| Consent and disclosure | 35 min | Candidate opportunity/consent, client unlock, consultant approval | Consultant can distinguish L2, L3, and L4 and explain why disclosure needs consent and approval |
| Feedback and outcome loop | 25 min | Client feedback, consultant follow-ups, workflow timeline | Consultant can route feedback into review instead of direct fact mutation |
| Escalation and support | 15 min | Admin/support path | Consultant knows when to escalate and does not request direct database fixes |

## Exercise 1: Intake Review

1. Open the Consultant portal.
2. Create or select an intake packet.
3. Review Clean Facts mode for readability.
4. Review Source Highlight mode for evidence.
5. Mark low-risk fields according to their evidence.
6. Do not bulk-approve client-visible, consent, disclosure, commercial, or
   identity-sensitive fields.
7. For weak or conflicting information, choose reject or needs-follow-up.
8. Publish only through the governed path when the gate allows it.

Passing criteria:

- Consultant can identify at least one claim that is not a confirmed fact.
- Consultant can explain why source spans and provenance matter.
- Consultant does not create or instruct any direct canonical write outside the
  reviewed workflow.

## Exercise 2: Job Activation and Matching

1. Open `/consultant/jobs`.
2. Select an active or under-review job.
3. Confirm company, role family, scorecard, must-have requirements, and
   commercial prerequisites.
4. Run or review matching where available.
5. Read evidence coverage, score confidence, score cap reason, industry-pack
   metadata, and risk indicators.
6. Use missing-evidence questions to plan candidate or client follow-up.

Passing criteria:

- Consultant can explain why a score cap can prevent a high final score.
- Consultant can distinguish evidence-backed match explanation from keyword
  similarity.
- Consultant can describe when a cold or stale industry pack requires review.

## Exercise 3: Client-Safe Shortlist

1. Create or open a shortlist for the job.
2. Add candidate cards through the shortlist builder.
3. Review each card for redaction level and re-identification risk.
4. Remove or revise any card with unsafe identity clues.
5. Confirm the client-safe summary contains no raw candidate id, name, contact,
   exact employer, exact project, precise timeline, public artifact, consultant
   note, or raw source text.
6. Send only after manual review and workflow gate approval.

Passing criteria:

- Consultant can name the difference between L2 client-safe detail and L3
  consented detail.
- Consultant can identify high-signal re-identification risks.
- Consultant does not send shortlist content automatically through AI.

## Exercise 4: Consent, Unlock, and Disclosure

1. Open a candidate opportunity or consent request.
2. Confirm the consent request is bound to opportunity, profile version, consent
   text version, and shared fields.
3. Review a client unlock request.
4. Check candidate consent, job state, privacy risk, prior-contact or
   prior-application flags, and commercial or fee-protection prerequisites.
5. Approve only if the disclosure gate allows it.
6. Confirm L4 identity disclosure is recorded through DisclosureRecord and
   WorkflowEvent evidence.

Passing criteria:

- Consultant can explain that consent is job/profile-version specific.
- Consultant can explain why L3 does not equal L4.
- Consultant refuses unlock when consent is missing, expired, revoked,
  mismatched, or privacy/commercial review is unresolved.

## Exercise 5: Feedback and Follow-Up

1. Review client feedback or interview outcome.
2. Identify suggested profile, job, or company updates.
3. Send any candidate or client follow-up only through the approved channel for
   the pilot.
4. Confirm suggested updates enter human review before canonical write.
5. Review workflow timeline for actor, status, blocker, and next action.

Passing criteria:

- Consultant does not let feedback overwrite confirmed facts directly.
- Consultant can use the timeline to explain transaction state.
- Consultant escalates integration, notification, or support issues through the
  approved path instead of making manual database changes.

## Consultant Readiness Signoff

The consultant is ready for pilot work when all are true:

- Completed all exercises.
- Demonstrated no raw candidate leakage to client role.
- Demonstrated no AI-final fact behavior.
- Demonstrated no disclosure without consent and approval.
- Demonstrated no direct import or database workaround.
- Knows escalation owners for integrations, support, data correction, security,
  and import approval.

