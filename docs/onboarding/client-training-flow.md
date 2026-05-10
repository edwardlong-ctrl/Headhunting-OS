# Client Training Flow

## Goal

Train client users to collaborate in the Recruiting Transaction OS without
receiving raw candidate data before consent and disclosure gates. Client users
should be able to submit jobs, answer clarification, review anonymous
shortlists, request unlock, and provide feedback without engineering help.

## Prerequisites

- Client user account is scoped to the correct company and pilot organization.
- The client knows which jobs are in pilot scope.
- The consultant has explained the anonymous shortlist and unlock model.
- Any outbound email/calendar/SMS workflow that the client expects is either
  configured through integration work or replaced by an agreed manual channel.

## Training Agenda

| Module | Time | Surface | Outcome |
| --- | --- | --- | --- |
| Client role and privacy boundary | 15 min | Client-safe data boundary | Client understands anonymous review before disclosure |
| Client workspace tour | 15 min | `/client/dashboard`, `/client/profile`, `/client/preferences` | Client can find company profile, preferences, and work queue |
| Job submission and clarification | 25 min | `/client/jobs/new`, `/client/jobs/:jobId`, `/client/follow-ups` | Client can submit a job and answer clarifying questions |
| Shortlist review | 30 min | `/client/shortlists`, `/client/shortlists/:shortlistId`, candidate card surfaces | Client can read match rationale without raw identity clues |
| Unlock request | 20 min | `/client/unlock/:candidateId` route alias and shortlist card unlock action where available | Client knows unlock is a request, not automatic identity access |
| Feedback loop | 20 min | Client feedback surface | Client can provide interview and shortlist feedback |
| Escalation | 10 min | Support and consultant contact path | Client knows how to report access, data, or workflow issues |

## Exercise 1: Submit or Review a Job

1. Open the Client portal.
2. Review company profile and hiring preferences.
3. Submit a job or review an existing job.
4. Add business context, must-have skills, interview process, and timing.
5. Answer clarification questions through the client follow-up surface.
6. Confirm the client does not expect the job to become active until consultant
   and system gates allow activation.

Passing criteria:

- Client can provide job context without sending candidate personal data.
- Client understands clarification answers may become claims and require review.
- Client knows urgent changes should go through the consultant or approved
  client workflow, not side-channel database edits.

## Exercise 2: Review an Anonymous Shortlist

1. Open a shortlist from the Client portal.
2. Review each anonymous candidate card.
3. Read broad seniority, role family, skill categories, redacted strengths,
   risk notes, score cap reason, and follow-up questions.
4. Confirm that missing identity details are intentional before disclosure.
5. Select candidates for interview interest or request more information through
   the provided workflow.

Passing criteria:

- Client does not ask for raw candidate id, resume, exact employer, exact
  project, contact details, raw source, or consultant notes before disclosure.
- Client can explain that redaction protects candidates and fee protection.
- Client can identify when more detail requires consent or unlock.

## Exercise 3: Request Unlock

1. Select a candidate card and request unlock where the product surface allows.
2. Confirm the request is tied to a specific job and shortlist card.
3. Understand that the request may be denied or delayed by missing consent,
   privacy risk, prior contact, prior application, commercial prerequisite, or
   consultant approval.
4. Wait for approved disclosure before contacting or identifying the candidate.

Passing criteria:

- Client understands unlock is not automatic.
- Client understands L3 consented detail is not L4 identity disclosure.
- Client does not attempt to infer identity from redacted evidence.

## Exercise 4: Submit Feedback

1. Open the feedback surface for a reviewed candidate or interview.
2. Submit clear feedback about fit, gaps, evidence, interview result, next
   steps, and reason codes.
3. Avoid adding unrelated personal or sensitive data.
4. Confirm the feedback enters the workflow and may require consultant review
   before it changes profile, job, or company records.

Passing criteria:

- Client can submit useful structured feedback.
- Client understands feedback does not directly overwrite confirmed facts.
- Client knows how to flag inaccurate or unsafe content.

## Client Readiness Signoff

Client users are ready for pilot work when they can:

- Submit or clarify jobs in the Client portal.
- Review anonymous shortlists without asking for pre-disclosure identity.
- Request unlock through the correct workflow.
- Provide feedback that is useful for the transaction and future matching.
- Escalate support questions through the agreed channel.

