# Usable v1 End-to-End Acceptance Scenarios

## Purpose

This document defines the concrete end-to-end scenarios that must pass before
the system can be called **Usable v1 / Controlled Pilot Ready**.

Each scenario covers a complete user-facing workflow from precondition through
expected outcome, including safety assertions and the roadmap tasks required to
enable it.

## Source of Truth

- v2.1 (`docs/specs/v2.1/product-spec-v2.1.md`) is the authoritative
  capability list.
- `docs/roadmap/productization-roadmap.md` provides the task numbering.
- `docs/roadmap/v2.1-capability-split.md` provides the capability status.

## Definition of Done

A scenario is considered **passed** when all of the following are true:

1. Every step can be executed through the production UI and/or API without
   direct database manipulation, seed data shortcuts, or hidden manual fixes.
2. Every expected outcome is observable through the appropriate portal.
3. Every safety assertion holds: blocked paths are blocked, denied access is
   denied, audit records exist, and no invariant is violated.
4. The scenario can be repeated with different data and still pass.
5. Negative/safety sub-scenarios within each family also pass.

A scenario is **not passed** if:

- Any step requires Postman, direct SQL, or developer intervention.
- Any safety assertion fails even once.
- Any audit record is missing for a key state transition.
- Any invariant from the hard invariant list is violated.

---

## Scenario Families

### S01: Consultant Intake -- Upload CV to Canonical Profile

**Scenario ID**: S01
**Title**: Consultant candidate intake: upload CV -> AI extraction -> claim
ledger -> human review -> canonical profile write
**Actor(s)**: Consultant
**v2.1 Spec Sections**: 7.2, 11.1-11.5, 12 (Tasks 0.1-0.5, 3, 14), 13
(CandidateProfile, ClaimLedgerItem, ReviewEvent), 14 (Candidate state machine)

**Preconditions**:
- Consultant is authenticated and has an active organization membership.
- The system has a configured AI provider and prompt registry.
- At least one industry pack is seeded (semiconductor or general).

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Consultant | Opens /consultant/intake/talent and uploads a candidate CV file plus a WeChat conversation screenshot. | System creates SourceItem records for each file. System creates an InformationPacket grouping both sources. File hash and MIME validation pass. |
| 2 | System | Runs AI Source Classifier (Task 0.1). | Classifies CV as resume, WeChat as consultant_note. Confidence and language recorded. |
| 3 | System | Runs AI Candidate Profile Parser (Task 3) and Consultant Note Structurer (Task 6). | Extracts structured fields from CV (skills, experience, education, location) and WeChat (motivation hint, salary expectation, availability). Creates AITaskRun records with model/prompt/schema versions. |
| 4 | System | Runs Claim Ledger Builder (Task 14). | Creates ClaimLedgerItem records for each extracted field. Each claim records: claim_type, assertion_strength, source_span, speaker, verification_status=ai_extracted, client_shareability. WeChat motivation hint gets assertion_strength=weak_signal. |
| 5 | System | Runs Conflict Detector (Task 0.4). | Detects salary conflict between CV (listed range) and WeChat note (different number mentioned). Creates conflict metadata. |
| 6 | Consultant | Opens /consultant/intake/review/:packetId. | Review page shows Clean Facts mode and Source Highlight mode. Each field shows claim strength, source span, and risk tier. |
| 7 | Consultant | Switches between Clean Facts and Source Highlight modes. | Clean Facts shows the canonical draft with field statuses. Source Highlight shows original documents with extracted fields highlighted and linked to claims. |
| 8 | Consultant | Bulk-approves T0/T1 low-risk fields (skill synonyms, school names, industry labels). | Fields move to human_acknowledged. System records review_velocity and bulk_flag=true in ReviewEvent. Bulk approve does NOT produce candidate_confirmed or external_verified. |
| 9 | Consultant | Reviews T2 salary field individually. Sees source span from both CV and WeChat with conflict marker. Selects the WeChat value as consultant_attested with reason. | Salary field moves to consultant_attested. ReviewEvent records reviewer, risk_tier=T2, decision=attested, reason, duration. Conflict is resolved for this field. |
| 10 | Consultant | Reviews T2 motivation field. WeChat says "can take a look at opportunities". Consultant marks as needs_confirmation. | Field stays needs_confirmation. System generates a follow-up task for candidate confirmation. No canonical write for this field. |
| 11 | Consultant | Confirms remaining T2 fields individually. | Each reviewed field gets a ReviewEvent. Risk-tier-appropriate review is recorded. |
| 12 | System | Submits reviewed claims through IntakeCanonicalWriteBridgeService to CanonicalWriteService. | CanonicalWriteGate checks each claim: risk tier, source trust, review evidence. Approved claims write CandidateProfile fields inside the transaction boundary. WorkflowEvent audit records are created for each canonical write. Gate-blocked claims do not mutate CandidateProfile. |
| 13 | Consultant | Publishes candidate to talent pool. | Candidate state transitions: new -> profile_parsed -> consultant_review -> available. WorkflowEvent records each transition. |

**Expected Outcomes**:
- CandidateProfile exists with field-level status, source lineage, and version.
- ClaimLedgerItem records are immutable and linked to source spans.
- ReviewEvent records exist for every reviewed field.
- WorkflowEvent records exist for canonical writes and state transitions.
- AITaskRun records exist for each AI task with model/prompt/schema versions.
- needs_confirmation fields are not written to canonical profile.
- Weak-signal motivation is not written as confirmed intent.

**Safety Assertions**:
- [ ] Bulk approve produces only human_acknowledged, never candidate_confirmed or external_verified.
- [ ] AI extraction output enters ClaimLedger, not canonical profile directly.
- [ ] CanonicalWriteGate blocks claims without sufficient review evidence.
- [ ] Salary conflict is visible and requires individual review before canonical write.
- [ ] "Can take a look at opportunities" is not written as "confirmed interest".
- [ ] All AI tasks have AITaskRun records with write_back_target metadata.
- [ ] Candidate cannot be directly created by AI without consultant review.

**Required Roadmap Tasks**: 16, 17, 18, 19, 20, 21, 22, 23, 24

---

### S02: Consultant Job/Company Intake

**Scenario ID**: S02
**Title**: Create company -> create job -> job requirements -> scorecard
**Actor(s)**: Consultant, Client
**v2.1 Spec Sections**: 7.3, 8 (Client job creation), 12 (Tasks 1, 2),
13 (Company, Job, JobScorecard), 14 (Job state machine)

**Preconditions**:
- Consultant and Client are authenticated with appropriate roles.
- Client company organization exists.
- At least one industry pack is seeded.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Consultant | Creates company record at /consultant/companies with name, industry, and contact. | Company persisted with organization scope. CompanyContact created. |
| 2 | Client | Opens /client/jobs/new/ai-intake and uploads a JD document. | System creates SourceItem and InformationPacket for JD. |
| 3 | System | Runs Job Intake Parser (Task 1). | Extracts job requirements, generates scorecard draft, identifies missing information, creates AITaskRun. |
| 4 | System | Generates clarification questions. | AI identifies ambiguous requirements (e.g., "senior" without level definition, salary range missing, team size unclear). Questions delivered to /client/jobs/:jobId/clarification. |
| 5 | Client | Answers clarification questions at /client/jobs/:jobId/clarification. | Client answers create claims, not facts. System records answers as SourceItem with speaker=client. |
| 6 | Consultant | Reviews job draft at /consultant/jobs/:jobId/intake. Sees AI-parsed profile, scorecard, clarification answers, and activation checklist. | Consultant sees incomplete items: missing commercial terms, industry pack selection, scorecard confirmation. |
| 7 | Consultant | Selects semiconductor industry pack. Confirms scorecard dimensions. Adds commercial terms placeholder. | JobScorecard finalized. Industry pack bound to job. Commercial terms recorded. |
| 8 | Consultant | Activates job. | Job state: draft -> submitted -> intake_review -> activated. Each transition creates WorkflowEvent. Activation requires: scorecard confirmed, industry pack selected, commercial terms present, consultant approval. |

**Expected Outcomes**:
- Company exists with contacts and organization scope.
- Job exists with requirements, scorecard, and industry pack binding.
- Client clarification answers are source records, not direct job facts.
- Job activation requires consultant approval and activation checklist completion.
- WorkflowEvent records exist for all state transitions.

**Safety Assertions**:
- [ ] Job cannot be activated without consultant review and approval.
- [ ] Client clarification answers do not directly overwrite job facts.
- [ ] Raw JD is not used as the final scorecard.
- [ ] Commercial terms cannot be committed by AI alone.
- [ ] Job state transitions are recorded in WorkflowEvent.

**Required Roadmap Tasks**: 16, 18, 19, 20, 21, 24, 25, 26

---

### S03: Consultant Matching

**Scenario ID**: S03
**Title**: Run match -> review evidence -> score-cap policy applied -> match
report persisted
**Actor(s)**: Consultant
**v2.1 Spec Sections**: 7.3, 12 (Task 8), 13 (MatchReport), 15 (Match Score),
16 (Industry Pack)

**Preconditions**:
- Job is activated with confirmed scorecard (S02 passed).
- At least one candidate is available in the talent pool (S01 passed).
- Industry pack is seeded with minimum depth for the job's industry.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Consultant | Opens /consultant/jobs/:jobId/matching and triggers match generation. | System identifies eligible candidates based on job requirements and talent pool availability. |
| 2 | System | Runs Match Report Generator (Task 8) for each candidate-job pair. | For each candidate: scores 9 dimensions against JobScorecard using CandidateProfile evidence, generates overall 1-5 score, calculates evidence_coverage, applies provenance_weighting, checks authenticity_risk, records ontology_version and industry_pack_version. Creates AITaskRun for each match generation. |
| 3 | System | Applies ScoreCapPolicy. | Caps applied: cold industry pack -> max 3; keyword-only skills without project evidence -> max 3; weak-signal intent -> max 3; insufficient independent high-trust evidence -> max 4. Cap reasons recorded. |
| 4 | System | Persists MatchReport records. | Each MatchReport stores: overall score, dimension scores, score_confidence, evidence_coverage, provenance summary, cap decision, ontology_version, and generated_at. |
| 5 | Consultant | Reviews match results at /consultant/jobs/:jobId/matching. | Consultant sees ranked candidates with scores, dimension breakdowns, evidence explanations, confidence levels, cap reasons, and risk flags. |
| 6 | Consultant | Drills into a candidate's match detail. Sees evidence for each dimension with source references. | Evidence panel shows claim sources, trust levels, and provenance weights. Weak evidence is marked. |
| 7 | Consultant | Notes that one candidate has high score but low evidence coverage. Reviews cap reason: "insufficient independent high-trust evidence". | Consultant can request additional evidence gathering or follow-up before including in shortlist. |

**Expected Outcomes**:
- MatchReport records are persisted with full scoring metadata.
- Score caps are applied and visible with reasons.
- Evidence coverage and confidence are displayed alongside scores.
- Provenance weighting differentiates evidence sources.
- Ontology and industry pack versions are recorded on each report.

**Safety Assertions**:
- [ ] Candidate with only CV keywords and no project evidence cannot receive 5 for Technical Fit.
- [ ] Cold industry pack caps overall score at 3.
- [ ] Weak-signal intent caps Motivation Fit at 3.
- [ ] Score caps are visible to consultant with safe reason explanations.
- [ ] MatchReport records score_confidence, evidence_coverage, and provenance metadata.
- [ ] Each match generation creates an AITaskRun record.

**Required Roadmap Tasks**: 16, 18, 21, 22, 23, 24, 25, 26, 27, 28

---

### S04: Consultant Shortlist

**Scenario ID**: S04
**Title**: Build shortlist -> redact -> re-identification check -> send to client
**Actor(s)**: Consultant
**v2.1 Spec Sections**: 7.3, 12 (Tasks 10, 17, 18), 13 (Shortlist,
ShortlistCandidateCard), 14 (Shortlist state machine), 17.3 (re-identification)

**Preconditions**:
- Match reports exist for the job (S03 passed).
- Candidates have sufficient review status for shortlist inclusion.
- Industry pack re-identification rules are configured.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Consultant | Opens /consultant/jobs/:jobId/shortlist and selects 3-5 candidates from match results. | System creates Shortlist in draft state. |
| 2 | System | Generates anonymous candidate cards for each selected candidate. | For each candidate: runs ClientSafeCandidateProjectionService, applies forbidden-field policy, generates L2 client-safe summary by default. |
| 3 | System | Runs Re-identification Risk Scorer (Task 17) on each card. | Checks each card for: exact company + rare title + exact year, chip/project code names, patents/papers, small-team unique ownership, overly specific achievements. Returns risk_score and unsafe_features per card. |
| 4 | System | Applies redaction based on risk assessment. | High-risk features: exact company generalized to "top semiconductor company", chip code name removed, unique team description changed to "contributed to team-level effort", specific achievement numbers generalized to ranges. |
| 5 | Consultant | Reviews shortlist at /consultant/jobs/:jobId/shortlist. Sees comparison table with anonymous cards, scores, dimensions, evidence summaries, and risk flags. | Pre-send checks visible: re-identification risk status, consent status per candidate, score confidence warnings. |
| 6 | Consultant | Notes one candidate has unresolved high re-identification risk. | System blocks this candidate from shortlist send until risk is resolved (generalized further or candidate authorizes). |
| 7 | Consultant | Resolves remaining pre-send checks. Reviews client-safe preview. | Preview shows exactly what client will see: anonymous cards, comparison table, generalized summaries. |
| 8 | Consultant | Manually confirms and sends shortlist. | Shortlist state: draft -> ready_for_review -> sent_to_client. WorkflowEvent records. Shortlist is not auto-sent by AI. |

**Expected Outcomes**:
- Shortlist contains only anonymous, redacted candidate cards.
- Re-identification risk is assessed and high-risk features are generalized.
- Consultant must manually approve before send.
- Pre-send checks block send when unresolved risks exist.
- WorkflowEvent records exist for shortlist state transitions.

**Safety Assertions**:
- [ ] No real name, contact info, or full LinkedIn appears in shortlist.
- [ ] "Top chip company + unique title + exact year + chip code name" is generalized or blocked.
- [ ] High re-identification risk blocks shortlist send until resolved.
- [ ] AI cannot auto-send shortlist.
- [ ] Consultant must manually confirm before shortlist is sent.
- [ ] Patents, papers, public talks, and unique achievements are hidden before candidate authorization.

**Required Roadmap Tasks**: 16, 18, 19, 24, 27, 29, 30

---

### S05: Client Review

**Scenario ID**: S05
**Title**: Receive shortlist -> view anonymous cards -> request unlock ->
provide feedback
**Actor(s)**: Client
**v2.1 Spec Sections**: 8, 17.3

**Preconditions**:
- Shortlist has been sent to client (S04 passed).
- Client is authenticated with company organization membership.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Client | Opens /client/dashboard. Sees notification of new shortlist for their job. | Dashboard shows shortlist with candidate count and job reference. |
| 2 | Client | Opens /client/jobs/:jobId/shortlist. | Comparison table shows anonymous candidates with: generalized role/headline, overall score, dimension scores, client-safe evidence summaries, risk/gap indicators. No real names, contacts, or identifying details. |
| 3 | Client | Clicks on a candidate to view detail at /client/candidates/:anonymousCandidateId. | Detail page shows: L2 client-safe summary, dimension score breakdowns, evidence explanations (with high-risk evidence suppressed), interview question suggestions, and risk flags. |
| 4 | Client | Interested in this candidate. Clicks "Request Unlock" at /client/unlock/:candidateId. | System shows unlock prerequisites: consent status, fee agreement status, prior-contact check status. System performs pre-checks. |
| 5 | System | Runs unlock pre-checks. | Checks: candidate consent confirmed? Fee agreement active? Prior contact claims? Prior application claims? All checks must pass or return explicit review reasons. |
| 6 | Client | Provides feedback on other candidates at /client/feedback/:interviewId. | Structured feedback form with dimension ratings. Feedback creates source records, not fact overwrites. |

**Expected Outcomes**:
- Client sees only anonymous, client-safe candidate information.
- Unlock request triggers pre-checks before proceeding.
- Client feedback is structured and recorded as source material.
- No identity information is visible before unlock approval.

**Safety Assertions**:
- [ ] Client cannot see real candidate name, email, phone, or LinkedIn.
- [ ] Client cannot see consultant internal notes or other client interaction history.
- [ ] Client cannot see exact employer name, project code names, or unique achievements before L3/L4.
- [ ] Unlock request is blocked if consent is missing, expired, or revoked.
- [ ] Client feedback creates source records, not direct profile overwrites.
- [ ] Anonymous card response does not contain raw candidate/profile IDs.

**Required Roadmap Tasks**: 16, 18, 19, 24, 29, 30, 32, 33

---

### S06: Candidate Consent

**Scenario ID**: S06
**Title**: Receive opportunity -> review profile -> grant/revoke consent
**Actor(s)**: Candidate
**v2.1 Spec Sections**: 9, 14 (Consent state machine), 17

**Preconditions**:
- Candidate has a canonical profile (S01 passed).
- Consultant has matched candidate to a job (S03 passed).
- Candidate is authenticated with self-owned identity.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Candidate | Receives notification of new opportunity. Opens /candidate/opportunities/:opportunityId. | Opportunity view shows: anonymous/semi-anonymous company description, role description, salary range, fit explanation. No internal consultant notes or other candidate data visible. |
| 2 | Candidate | Reviews opportunity and is interested. Clicks to proceed to consent. | System opens /candidate/consent/:requestId. |
| 3 | Candidate | Reviews consent page. | Consent page shows: consent text (versioned), profile version being shared, specific fields that will be shared with the client, prior application declaration form. |
| 4 | Candidate | Reviews shared-fields preview. Sees exactly which profile fields will be visible to the client at each disclosure level. | Preview shows L2 client-safe summary and L3 consented detail fields. Candidate can see what is generalized and what requires explicit authorization. |
| 5 | Candidate | Confirms consent. | ConsentRecord created binding: opportunity, profile_version, consent_text_version, shared_fields. Consent state: not_requested -> requested -> candidate_viewed -> consent_confirmed. WorkflowEvent records each transition. |
| 6 | (Alternative) Candidate | Declines consent. | Consent state: not_requested -> requested -> candidate_viewed -> consent_declined. Shortlist and disclosure workflow blocked. WorkflowEvent records decline. |
| 7 | (Alternative) Candidate | Later revokes consent. | Consent state: consent_confirmed -> revoked. Any pending disclosure or unlock is blocked. WorkflowEvent records revocation. |

**Expected Outcomes**:
- Consent binds opportunity, profile version, consent text version, and shared fields.
- Candidate can see exactly what will be shared before consenting.
- Consent is immutable once created (new records for changes, not mutations).
- Decline and revocation block downstream disclosure.
- WorkflowEvent records exist for all consent state transitions.

**Safety Assertions**:
- [ ] Consent binds specific profile version and consent text version.
- [ ] Candidate can see shared-fields preview before consenting.
- [ ] Consent decline blocks shortlist inclusion and disclosure.
- [ ] Consent revocation blocks any pending or future disclosure.
- [ ] Candidate cannot see other candidates, client internal notes, or commercial terms.
- [ ] Consent cannot be granted by AI, system, or consultant on behalf of candidate.

**Required Roadmap Tasks**: 16, 18, 19, 26, 31, 33, 34

---

### S07: Disclosure / Unlock

**Scenario ID**: S07
**Title**: Consent confirmed -> unlock decision -> disclosure record ->
identity disclosed -> WorkflowEvent audit
**Actor(s)**: Client, Consultant, System
**v2.1 Spec Sections**: 14 (Disclosure state machine), 17 (17.1, 17.2, 17.3)

**Preconditions**:
- Candidate consent is confirmed (S06 passed).
- Client has requested unlock (S05 step 4 passed).
- Job is activated with active fee agreement.
- No unresolved prior-contact or prior-application claims exist (or they have been reviewed).

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | System | Receives client unlock request. Runs disclosure pre-checks. | Checks: consent_confirmed and not expired/revoked? Job activated? Fee agreement active? Prior-contact claims reviewed? Prior-application claims reviewed? |
| 2 | System | Prior-contact check. | If client has PriorContactClaim for this candidate: blocks automatic disclosure, creates review task for consultant. If no claim: passes. |
| 3 | System | Prior-application check. | If candidate has PriorApplicationClaim for same company/same job: blocks shortlist and disclosure until reviewed. Same company/different job: warning + review. |
| 4 | Consultant | Receives unlock approval request. Reviews at consultant workflow. | Consultant sees: candidate details, client request, consent status, prior-contact/application status, fee agreement status, re-identification risk summary. |
| 5 | Consultant | Approves unlock with reason. | UnlockDecision created with: approver, reason, consent reference, job reference. WorkflowEvent records approval. |
| 6 | System | Generates DisclosureRecord. | DisclosureRecord created binding: requester (client), approver (consultant), consent_record_id, unlock_decision_id, client_safe_card_version, disclosure_level=L4. WorkflowEvent: DISCLOSURE_IDENTITY_DISCLOSED with full audit metadata. |
| 7 | System | Makes identity available to client. | Client can now see: real name, contact information, full profile (within consented shared fields). Disclosure state: not_disclosed -> consent_confirmed -> client_requested_unlock -> consultant_approved -> identity_disclosed. |
| 8 | Client | Views disclosed candidate at /client/candidates/:candidateId. | Full identity visible. Fee protection active. |

**Expected Outcomes**:
- DisclosureRecord chain proves: who requested, who approved, what consent
  version, what client-safe card version, which WorkflowEvents participated.
- Identity is disclosed only after all pre-checks pass.
- Fee protection is activated upon disclosure.
- Complete audit trail exists for the entire unlock/disclosure chain.

**Safety Assertions**:
- [ ] Disclosure cannot bypass candidate consent.
- [ ] Disclosure cannot bypass consultant approval.
- [ ] Prior-contact claims block automatic disclosure and require review.
- [ ] Prior-application claims for same company/same job block disclosure until reviewed.
- [ ] L4 identity disclosure requires confirmed non-expired/non-revoked consent, approved unlock, approved disclosure record, and WorkflowEvent.
- [ ] L3 consented detail does not automatically become L4 identity disclosure.
- [ ] DisclosureRecord is immutable.
- [ ] AI cannot approve unlock or disclose identity.
- [ ] Client cannot bypass consultant approval for disclosure.

**Required Roadmap Tasks**: 16, 18, 19, 26, 29, 30, 31, 32, 33

---

### S08: Interview and Feedback

**Scenario ID**: S08
**Title**: Schedule -> feedback -> outcome
**Actor(s)**: Client, Consultant
**v2.1 Spec Sections**: 8, 12 (Task 11), 13 (InterviewFeedback)

**Preconditions**:
- Candidate identity has been disclosed (S07 passed).
- Interview has been scheduled (system or external).

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Client | Opens /client/feedback/:interviewId after interview. | Structured feedback form with dimension ratings aligned to JobScorecard. |
| 2 | Client | Provides dimension ratings and written feedback. | InterviewFeedback record created with: dimension_ratings, feedback_text, decision (proceed/reject/hold), reject_reason_taxonomy if applicable. |
| 3 | System | Runs Interview Feedback Structurer (Task 11). | Structures feedback into: profile update suggestions, company preference update suggestions, outcome label. Creates AITaskRun. Suggestions are claims, not facts. |
| 4 | System | Generates suggested updates. | "Good technical fit but salary mismatch" -> suggests updating CandidateCompanyInteraction outcome, NOT overwriting candidate's global salary expectation for all jobs. |
| 5 | Consultant | Reviews suggested updates at /consultant/follow-ups. | Consultant sees interaction-scoped suggestions vs global profile suggestions. Must approve before any canonical write. |
| 6 | Consultant | Approves interaction-scoped outcome label. Rejects global profile salary overwrite. | Interaction outcome updated. CandidateProfile salary field unchanged. ReviewEvent and WorkflowEvent recorded. |

**Expected Outcomes**:
- Interview feedback is structured and persisted.
- AI suggestions are interaction-scoped, not global fact overwrites.
- Consultant reviews and approves before any canonical write.
- Outcome labels feed back into match calibration dataset.

**Safety Assertions**:
- [ ] Client feedback creates source records, not direct canonical overwrites.
- [ ] "Good technical fit but salary mismatch" updates that interaction, not global suitability.
- [ ] AI cannot automatically reject candidates based on feedback.
- [ ] One client's feedback does not permanently pollute global candidate profile or ontology.
- [ ] Consultant must review AI-suggested updates before canonical write.
- [ ] AI cannot automatically overwrite human-confirmed ability facts.

**Required Roadmap Tasks**: 16, 18, 19, 24, 26, 32, 33, 35

---

### S09: Placement and Commission

**Scenario ID**: S09
**Title**: Offer -> placement -> commission baseline
**Actor(s)**: Consultant, Owner
**v2.1 Spec Sections**: 7.3, 6, 14 (Placement/Commission state machine)

**Preconditions**:
- Candidate has been through interview process (S08 passed).
- Client has decided to make an offer.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Consultant | Records offer at /consultant/placements. | Placement record: offer_pending. Includes: salary, start date, fee rate, guarantee period. WorkflowEvent created. |
| 2 | Consultant | Updates: offer accepted by candidate. | Placement state: offer_pending -> offer_accepted. WorkflowEvent records. |
| 3 | Consultant | Records onboarding confirmation. | Placement state: offer_accepted -> onboarded. Start date confirmed. |
| 4 | Consultant | Marks invoice ready. | Placement state: onboarded -> invoice_ready. Commission record created with: fee calculation, expected amount. |
| 5 | Owner | Views /owner/placements and /owner/commission. | Sees: placement table, fee status, guarantee period, commission status (pending). |
| 6 | Owner | Views /owner/revenue. | Revenue dashboard shows: expected fee, invoice status, pipeline value. |

**Expected Outcomes**:
- Placement lifecycle is tracked from offer to onboarding.
- Commission record exists with fee calculation inputs.
- Owner can see expected fee, invoice status, and guarantee status.
- WorkflowEvent records exist for all placement state transitions.

**Safety Assertions**:
- [ ] Offer, placement, and commission cannot be confirmed by AI alone.
- [ ] Each placement state transition requires explicit human action.
- [ ] Commission is not a full accounting system replacement.
- [ ] WorkflowEvent records exist for every state transition.
- [ ] Placement does not automatically mean invoice or payment.

**Required Roadmap Tasks**: 16, 18, 19, 24, 26, 36

---

### S10: Owner Dashboard

**Scenario ID**: S10
**Title**: View business metrics, quality indicators, risk flags
**Actor(s)**: Owner
**v2.1 Spec Sections**: 6

**Preconditions**:
- System has operational data from scenarios S01-S09.
- Owner is authenticated with Owner role.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Owner | Opens /owner/dashboard. | Dashboard shows: KPI summary, active jobs count, shortlist acceptance rate, placement count, revenue summary, AI alerts. |
| 2 | Owner | Opens /owner/pipeline. | Kanban view of job pipeline stages with blockers and candidate counts. Filterable by consultant, client, industry, risk. |
| 3 | Owner | Opens /owner/consultants. | Consultant list with: active jobs, shortlist acceptance rate, revenue contribution, data quality score, review quality indicators. |
| 4 | Owner | Notices a consultant with high bulk-approve ratio. | System shows: review_velocity, bulk_approve_ratio for that consultant. Owner can see open sample audit records. |
| 5 | Owner | Opens /owner/risk. | Risk dashboard shows: duplicate candidates, prior-contact claims, consent issues, client bypass attempts, payment risks. |
| 6 | Owner | Opens /owner/ai-quality. | AI quality dashboard shows: schema validity, override rate, hallucination risk flags, task failure rate. |
| 7 | Owner | Opens /owner/audit. | Audit search with filters: actor, entity, action, time range. Shows before/after state and AI involvement. |

**Expected Outcomes**:
- Owner has visibility into all key business metrics.
- Quality indicators surface confirmation fatigue and data quality risks.
- Risk dashboard provides actionable risk identification.
- All dashboard data comes from backend-approved queries, not frontend-generated metrics.

**Safety Assertions**:
- [ ] Dashboard data reflects backend truth, not frontend-computed estimates.
- [ ] Owner can see a consultant's high bulk-approve ratio and open sample audit records.
- [ ] Owner cannot mutate facts outside domain services.
- [ ] No vanity dashboard with fake frontend-only data.

**Required Roadmap Tasks**: 16, 18, 19, 24, 26, 36, 37

---

### S11: Admin Governance

**Scenario ID**: S11
**Title**: Audit trail review, AI task review, permission management,
workflow event inspection
**Actor(s)**: Admin
**v2.1 Spec Sections**: 10, 10.1

**Preconditions**:
- System has operational data from scenarios S01-S09.
- Admin is authenticated with Admin role.

**Steps**:

| # | Actor | Action | System Behavior |
| --- | --- | --- | --- |
| 1 | Admin | Opens /admin/audit-log. | Full audit log search: filter by actor, entity type, action, time range, AI involvement. Shows WorkflowEvent records with before/after state. |
| 2 | Admin | Searches for all disclosure events. | Finds DisclosureRecord-related WorkflowEvents with: requester, approver, consent version, candidate reference, timestamps. |
| 3 | Admin | Opens /admin/ai-task-registry. | Lists all AI task types with: version, input/output schema, execution count, failure rate, human-review policy, write-back target policy. |
| 4 | Admin | Inspects a specific AITaskRun. | Shows: model provider, prompt version, schema version, input, output, cost, latency, human_review_status, write_back_target, correlation/causation chain. |
| 5 | Admin | Opens /admin/permissions. | Permission matrix: roles, resources, actions, field-level visibility policies. |
| 6 | Admin | Opens /admin/claim-ledger. | Claim ledger search: filter by entity, claim type, assertion strength, verification status. |
| 7 | Admin | Opens /admin/review-quality. | Review quality signals: review velocity, bulk approve ratio, sample audit queue, false confirmation rate by consultant. |
| 8 | Admin | Opens /admin/workflow-rules. | Workflow state machines with transition rules and required checks. |

**Expected Outcomes**:
- Admin can inspect any WorkflowEvent, AITaskRun, ClaimLedgerItem, or ReviewEvent.
- Disclosure audit chain is fully traceable.
- AI task governance is visible and inspectable.
- Permission configuration is manageable through the admin portal.

**Safety Assertions**:
- [ ] Admin cannot mutate facts outside domain services.
- [ ] Admin cannot bypass canonical-write gates by role alone.
- [ ] Admin cannot bypass disclosure gates by role alone.
- [ ] Admin can view but not delete WorkflowEvent, ConsentRecord, or DisclosureRecord.
- [ ] All governance data comes from backend persistence, not frontend state.

**Required Roadmap Tasks**: 16, 17, 18, 19, 26, 37, 40, 44

---

### S12: Negative / Safety Scenarios

**Scenario ID**: S12
**Title**: Security and safety negative scenarios
**Actor(s)**: Client, AI/System, various
**v2.1 Spec Sections**: 4, 11, 14.1, 17

**Preconditions**:
- System has operational data.
- All portal roles are available for testing.

#### S12-A: Client Attempts Raw Candidate Access

| # | Actor | Action | Expected Result |
| --- | --- | --- | --- |
| 1 | Client | Attempts GET /api/consultant/talent/:candidateId | **BLOCKED**: 403 Access Denied. Client role cannot access raw Candidate. |
| 2 | Client | Attempts GET /api/candidate-profiles/:profileId | **BLOCKED**: 403 Access Denied. Client cannot read raw CandidateProfile. |
| 3 | Client | Attempts to infer candidate ID from anonymous card response | **BLOCKED**: Anonymous card response contains only opaque `card_` references, not raw UUIDs. |
| 4 | Client | Attempts URL manipulation with guessed candidate UUID | **BLOCKED**: Raw UUID path rejected. Only `card_` anonymous references accepted. |
| 5 | Client | Attempts to read error messages for internal IDs | **BLOCKED**: API error responses are sanitized. No raw IDs, stack traces, or internal entity details leaked. |

#### S12-B: AI Attempts Canonical Write

| # | Actor | Action | Expected Result |
| --- | --- | --- | --- |
| 1 | AI/System | AI task attempts to write directly to CandidateProfile | **BLOCKED**: All canonical writes must go through CanonicalWriteService and CanonicalWriteGate. AI cannot bypass gate. |
| 2 | AI/System | AI task output attempts write_back_target=CANONICAL_CANDIDATE_PROFILE without human review | **BLOCKED**: AITaskGovernancePolicy denies canonical targets without APPROVED human_review_status. |
| 3 | AI/System | AI attempts to approve its own write-back | **BLOCKED**: AI/System self-approval is denied by governance policy. |
| 4 | AI/System | AI attempts to set field status to candidate_confirmed | **BLOCKED**: Only candidate-sourced confirmation can produce candidate_confirmed. AI cannot fake confirmation source. |

#### S12-C: Cross-Organization Data Access

| # | Actor | Action | Expected Result |
| --- | --- | --- | --- |
| 1 | Consultant (Org A) | Attempts to read candidate from Org B | **BLOCKED**: All queries are organization-scoped. Cross-org access denied. |
| 2 | Client (Company X) | Attempts to read shortlist for Company Y's job | **BLOCKED**: Organization boundary enforcement. |
| 3 | Admin | Attempts cross-org data export | **BLOCKED**: Tenant-aware queries enforce organization scope. |

#### S12-D: Expired Consent Disclosure

| # | Actor | Action | Expected Result |
| --- | --- | --- | --- |
| 1 | Client | Requests unlock for candidate whose consent has expired | **BLOCKED**: ConsentDisclosureProtectionPolicy denies expired consent. Unlock request blocked. |
| 2 | Client | Requests unlock for candidate who revoked consent | **BLOCKED**: Revoked consent blocks all disclosure. |
| 3 | System | Attempts disclosure with mismatched consent/profile version | **BLOCKED**: Consent record must match the profile version being disclosed. |

#### S12-E: Bulk Approve Attempts CANDIDATE_CONFIRMED

| # | Actor | Action | Expected Result |
| --- | --- | --- | --- |
| 1 | Consultant | Bulk-approves T1 fields and expects candidate_confirmed | **BLOCKED**: CandidateProfileFieldStatusPolicy caps bulk approve at human_acknowledged. |
| 2 | Consultant | Bulk-approves T3/T4 fields | **BLOCKED**: T3/T4 actions cannot be bulk-approved. Individual review required. |
| 3 | Consultant | Attempts to mark field as external_verified through bulk approve | **BLOCKED**: external_verified requires actual external evidence, not bulk approve. |

**Safety Assertions for All S12 Sub-scenarios**:
- [ ] Client cannot fetch raw candidate by candidate id.
- [ ] Client cannot infer raw candidate id from anonymous card response.
- [ ] Client cannot request L4 identity disclosure without consent and consultant approval.
- [ ] AI cannot write canonical facts directly.
- [ ] AI cannot approve its own write-back.
- [ ] Bulk approve cannot produce candidate_confirmed or external_verified.
- [ ] Shortlist cannot be sent when re-identification risk is high and unresolved.
- [ ] Disclosure cannot bypass prior-contact/prior-application review when claims exist.
- [ ] Candidate cannot see other candidates, client-internal notes, or commercial terms.
- [ ] Admin cannot mutate facts outside domain services.
- [ ] Cross-organization data access is denied at all boundaries.
- [ ] Expired/revoked consent blocks disclosure.

**Required Roadmap Tasks**: 16, 17, 18, 19, 26, 29, 30, 33, 41

---

## Scenario Dependency Graph

The following table shows which roadmap tasks are required for each scenario.
A scenario cannot pass until all its required tasks are complete.

| Task | S01 | S02 | S03 | S04 | S05 | S06 | S07 | S08 | S09 | S10 | S11 | S12 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 16: Data Model | X | X | X | X | X | X | X | X | X | X | X | X |
| 17: Write Audit | X | | | | | | | | | | X | X |
| 18: Product API | X | X | X | X | X | X | X | X | X | X | X | X |
| 19: Auth/RBAC | X | X | X | X | X | X | X | X | X | X | X | X |
| 20: Doc Storage | X | X | | | | | | | | | | |
| 21: AI Runner | X | X | X | | | | | | | | | |
| 22: Doc Intelligence | X | | X | | | | | | | | | |
| 23: AI Intake E2E | X | | | | | | | | | | | |
| 24: Consultant Portal | X | X | X | X | | | | X | X | X | | |
| 25: Company/Job | | X | X | | | | | | | | | |
| 26: Workflow Engine | | X | X | | | X | X | X | X | X | X | X |
| 27: Matching | | | X | X | | | | | | | | |
| 28: Semiconductor Pack | | | X | | | | | | | | | |
| 29: Shortlist Builder | | | | X | X | | X | | | | | X |
| 30: Privacy Redaction | | | | X | X | | X | | | | | X |
| 31: Candidate Portal | | | | | | X | X | | | | | |
| 32: Client Portal | | | | | X | | X | X | | | | |
| 33: Consent/Disclosure | | | | | X | X | X | X | | | | X |
| 34: Notification | | | | | | X | | | | | | |
| 35: Interview Feedback | | | | | | | | X | | | | |
| 36: Placement/Commission | | | | | | | | | X | X | | |
| 37: Owner/Admin Gov | | | | | | | | | | X | X | |
| 40: Observability | | | | | | | | | | | X | |
| 41: Security Hardening | | | | | | | | | | | | X |
| 44: AI Task Registry | | | | | | | | | | | X | |

### Critical Path

The minimum task set that enables the first complete end-to-end flow
(S01 through S07, the core recruiting transaction):

```
16 -> 17 -> 18 -> 19 -> 20
-> 21 -> 22 -> 23 -> 24
-> 25 -> 26 -> 27 -> 28 -> 29 -> 30 -> 31 -> 32 -> 33
```

This aligns with the productization roadmap recommended execution order.

### Pilot Gate (Task 42)

Task 42 requires all of S01 through S08 plus the S12 negative scenarios to
pass. Scenarios S09 (placement/commission), S10 (owner dashboard), and S11
(admin governance depth) may be staged to P1 priority unless the pilot scope
requires them.

---

## Scenario-to-Spec Traceability

| Scenario | v2.1 Spec Sections |
| --- | --- |
| S01: Consultant Intake | 7.2, 11.1-11.5, 12 (Tasks 0.1-6, 14), 13 (CandidateProfile, ClaimLedgerItem, ReviewEvent), 14 (Candidate SM) |
| S02: Job/Company Intake | 7.3, 8, 12 (Tasks 1, 2), 13 (Company, Job, JobScorecard), 14 (Job SM) |
| S03: Matching | 7.3, 12 (Task 8), 13 (MatchReport), 15, 16 |
| S04: Shortlist | 7.3, 12 (Tasks 10, 17, 18), 13 (Shortlist), 14 (Shortlist SM), 17.3 |
| S05: Client Review | 8, 17.3 |
| S06: Candidate Consent | 9, 14 (Consent SM), 17 |
| S07: Disclosure/Unlock | 14 (Disclosure SM), 17 (17.1, 17.2, 17.3) |
| S08: Interview Feedback | 8, 12 (Task 11), 13 (InterviewFeedback) |
| S09: Placement/Commission | 7.3, 6, 14 (Placement/Commission SM) |
| S10: Owner Dashboard | 6, 5.1 (v2.1 governance overlay) |
| S11: Admin Governance | 10, 10.1 |
| S12: Negative/Safety | 4, 11, 14.1, 17 |
