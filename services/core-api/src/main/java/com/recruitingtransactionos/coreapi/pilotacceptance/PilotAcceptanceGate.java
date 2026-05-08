package com.recruitingtransactionos.coreapi.pilotacceptance;

import java.util.List;
import java.util.Set;

public final class PilotAcceptanceGate {

  private PilotAcceptanceGate() {
  }

  public static PilotAcceptanceReport task42Baseline() {
    return PilotAcceptanceReport.fromRequirements(
        "task-42-pilot-e2e-acceptance-gate",
        "Task 42 Pilot E2E Acceptance Gate",
        List.of(
            PilotAcceptanceRequirement.partial(
                "flow-1-consultant-cv-ai-review-canonical",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Consultant uploads CV and note -> AI claims -> review -> canonical profile",
                Set.of(
                    "DocumentUploadService",
                    "DocumentIntelligenceExtractionService",
                    "CandidateProfileParserTaskServiceTest",
                    "IntakeCanonicalWriteBridgePostgresIntegrationTest"),
                Set.of("No single browser/API E2E proves the full consultant CV-to-canonical flow on pilot seed data.")),
            PilotAcceptanceRequirement.partial(
                "flow-2-client-jd-ai-clarification-activation",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Client/company uploads JD -> AI job draft -> clarification -> consultant activation",
                Set.of(
                    "ClientApiCommandServiceTest",
                    "JobIntakeApplicationServiceTest",
                    "ClientApiQueryServiceTest"),
                Set.of("JD file upload and AI job-draft extraction are not covered by a single pilot E2E.")),
            PilotAcceptanceRequirement.partial(
                "flow-3-match-report-evidence-score-cap",
                PilotAcceptanceCategory.PILOT_FLOW,
                "MatchReport -> evidence-backed explanation -> score cap",
                Set.of(
                    "MatchReportGenerationServiceTest",
                    "ConsultantMatchingControllerTest",
                    "JdbcMatchReportPersistencePortIntegrationTest"),
                Set.of("No browser E2E proves the match report surface against the pilot walkthrough.")),
            PilotAcceptanceRequirement.partial(
                "flow-4-anonymous-shortlist-client-safe-preview",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Consultant creates anonymous shortlist -> client-safe preview",
                Set.of(
                    "ShortlistBuilderServiceTest",
                    "ClientApiCommandServiceTest",
                    "ClientSafeCandidateCardPostgresQueryPortTest"),
                Set.of("No browser E2E proves consultant send plus client preview as one pilot flow.")),
            PilotAcceptanceRequirement.partial(
                "flow-5-candidate-opportunity-consent",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Candidate receives opportunity/consent -> confirms authorization",
                Set.of(
                    "CandidatePortalQueryServiceTest",
                    "CandidateConsentControllerTest",
                    "CandidateConsentWorkflowServiceTest"),
                Set.of("No pilot E2E proves candidate opportunity plus consent confirmation from a seeded account.")),
            PilotAcceptanceRequirement.partial(
                "flow-6-client-shortlist-unlock-request",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Client reviews shortlist -> requests unlock",
                Set.of(
                    "ClientApiCommandServiceTest",
                    "UnlockWorkflowServiceTest"),
                Set.of("No browser E2E proves client review, selection, and unlock request together.")),
            PilotAcceptanceRequirement.partial(
                "flow-7-consultant-unlock-disclosure-identity",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Consultant approves unlock -> DisclosureRecord -> identity disclosed",
                Set.of(
                    "ConsultantUnlockControllerTest",
                    "UnlockWorkflowServiceTest",
                    "ClientDisclosedCandidateControllerTest"),
                Set.of("No pilot E2E proves consultant approval and client identity read together.")),
            PilotAcceptanceRequirement.partial(
                "flow-8-client-feedback-outcome-review",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Client submits interview feedback -> outcome label -> suggested updates enter review",
                Set.of(
                    "ClientApiCommandServiceTest",
                    "ConsultantInterviewFeedbackReviewControllerTest",
                    "InterviewFeedbackReviewServiceTest"),
                Set.of("No E2E proves feedback, outcome loop, and consultant review queue on one pilot path.")),
            PilotAcceptanceRequirement.passed(
                "negative-client-raw-candidate-denied",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Client cannot fetch raw candidate by candidate id",
                Set.of("FivePortalBoundaryRegressionTest", "ConsultantControllerLeakageTest")),
            PilotAcceptanceRequirement.passed(
                "negative-anonymous-card-no-raw-id",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Client cannot infer raw candidate id from anonymous card response",
                Set.of("ClientSafeCandidateCardControllerTest", "ClientApiCommandServiceTest")),
            PilotAcceptanceRequirement.passed(
                "negative-l4-requires-consent-and-approval",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Client cannot request L4 identity disclosure without consent and consultant approval",
                Set.of("ConsentDisclosureProtectionPolicyTest", "UnlockWorkflowServiceTest")),
            PilotAcceptanceRequirement.passed(
                "negative-ai-no-direct-canonical-write",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "AI cannot write canonical facts directly",
                Set.of("AITaskRunnerServiceTest", "CanonicalWriteServiceTest")),
            PilotAcceptanceRequirement.passed(
                "negative-ai-no-self-approval",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "AI cannot approve its own write-back",
                Set.of("AITaskWriteBackPolicyTest", "TruthLayerCanonicalWriteGateTest")),
            PilotAcceptanceRequirement.passed(
                "negative-bulk-approve-not-verified-fact",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Bulk approve cannot produce candidate_confirmed or external_verified",
                Set.of("CandidateProfileContractTest", "TruthLayerCanonicalWriteGateTest")),
            PilotAcceptanceRequirement.partial(
                "negative-high-reidentification-blocks-send",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Shortlist cannot be sent when re-identification risk is high and unresolved",
                Set.of("RedactionAuditPostgresIntegrationTest", "ShortlistBuilderServiceTest"),
                Set.of("Coverage exists at redaction/shortlist seams; no pilot E2E proves unresolved high risk blocks send.")),
            PilotAcceptanceRequirement.passed(
                "negative-disclosure-prerequisites-not-bypassed",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Disclosure cannot bypass prior-contact/prior-application review when claims exist",
                Set.of("JdbcConsentDisclosurePrerequisiteEvaluatorTest", "UnlockWorkflowServiceTest")),
            PilotAcceptanceRequirement.passed(
                "negative-candidate-self-scope",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Candidate cannot see other candidates, client-internal notes, or commercial terms",
                Set.of("CandidatePortalControllerTest", "CandidatePortalQueryServiceTest")),
            PilotAcceptanceRequirement.passed(
                "negative-admin-no-domain-bypass",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Admin cannot mutate facts outside domain services",
                Set.of("AdminGovernanceControllerMappingTest", "OwnerGovernanceControllerPolicyTest")),
            PilotAcceptanceRequirement.passed(
                "validation-git-diff-check",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "rtk git diff --check",
                Set.of("Passed for the Task 42 gate patch on 2026-05-08.")),
            PilotAcceptanceRequirement.passed(
                "validation-web-typecheck",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "rtk npm run typecheck:web",
                Set.of("Passed for the Task 42 gate patch on 2026-05-08.")),
            PilotAcceptanceRequirement.passed(
                "validation-web-build",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "rtk npm run build:web",
                Set.of("Passed for the Task 42 gate patch on 2026-05-08.")),
            PilotAcceptanceRequirement.passed(
                "validation-docker-info",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "rtk docker info",
                Set.of("Docker Desktop 29.4.1 server reachable on 2026-05-08.")),
            PilotAcceptanceRequirement.passed(
                "validation-core-api-maven-test",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "PATH=/opt/homebrew/bin:$PATH rtk mvn -f services/core-api/pom.xml test",
                Set.of("Passed for the Task 42 gate patch on 2026-05-08 with the full core-api suite.")),
            PilotAcceptanceRequirement.blocked(
                "validation-browser-e2e",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "Browser E2E tests for the eight pilot flows",
                Set.of("No Playwright/Cypress/browser E2E harness exists in root or web package scripts.")),
            PilotAcceptanceRequirement.blocked(
                "validation-pilot-data-cli",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "Task 38 pilot data rebuild/validate/export/reset gate",
                Set.of("Pilot data focused tests exist, but the current Task 42 gate has not rerun the CLI commands.")),
            PilotAcceptanceRequirement.blocked(
                "validation-backup-restore",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "Backup/restore validation",
                Set.of("Task 39 runbooks exist, but no current Task 42 backup/restore execution evidence is recorded."))));
  }
}
