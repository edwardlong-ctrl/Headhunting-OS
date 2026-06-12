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
            PilotAcceptanceRequirement.passed(
                "flow-1-consultant-cv-ai-review-canonical",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Consultant uploads CV and note -> AI claims -> review -> canonical profile",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S01",
                    "rtk npm run test:e2e:pilot",
                    "DocumentUploadService",
                    "DocumentIntelligenceExtractionService",
                    "CandidateProfileParserTaskServiceTest",
                    "IntakeCanonicalWriteBridgePostgresIntegrationTest")),
            PilotAcceptanceRequirement.passed(
                "flow-2-client-jd-ai-clarification-activation",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Client/company uploads JD -> AI job draft -> clarification -> consultant activation",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S02",
                    "rtk npm run test:e2e:pilot",
                    "ClientApiCommandServiceTest",
                    "JobIntakeApplicationServiceTest",
                    "ClientApiQueryServiceTest")),
            PilotAcceptanceRequirement.passed(
                "flow-3-match-report-evidence-score-cap",
                PilotAcceptanceCategory.PILOT_FLOW,
                "MatchReport -> evidence-backed explanation -> score cap",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S03",
                    "rtk npm run test:e2e:pilot",
                    "MatchReportGenerationServiceTest",
                    "ConsultantMatchingControllerTest",
                    "JdbcMatchReportPersistencePortIntegrationTest")),
            PilotAcceptanceRequirement.passed(
                "flow-4-anonymous-shortlist-client-safe-preview",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Consultant creates anonymous shortlist -> client-safe preview",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S04",
                    "rtk npm run test:e2e:pilot",
                    "ShortlistBuilderServiceTest",
                    "ClientApiCommandServiceTest",
                    "ClientSafeCandidateCardPostgresQueryPortTest")),
            PilotAcceptanceRequirement.passed(
                "flow-5-candidate-opportunity-consent",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Candidate receives opportunity/consent -> confirms authorization",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S05-S06",
                    "rtk npm run test:e2e:pilot",
                    "CandidatePortalQueryServiceTest",
                    "CandidateConsentControllerTest",
                    "CandidateConsentWorkflowServiceTest")),
            PilotAcceptanceRequirement.passed(
                "flow-6-client-shortlist-unlock-request",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Client reviews shortlist -> requests unlock",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S05",
                    "rtk npm run test:e2e:pilot",
                    "ClientApiCommandServiceTest",
                    "UnlockWorkflowServiceTest")),
            PilotAcceptanceRequirement.passed(
                "flow-7-consultant-unlock-disclosure-identity",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Consultant approves unlock -> DisclosureRecord -> identity disclosed",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S06-S07",
                    "rtk npm run test:e2e:pilot",
                    "ConsultantUnlockControllerTest",
                    "UnlockWorkflowServiceTest",
                    "ClientDisclosedCandidateControllerTest")),
            PilotAcceptanceRequirement.passed(
                "flow-8-client-feedback-outcome-review",
                PilotAcceptanceCategory.PILOT_FLOW,
                "Client submits interview feedback -> outcome label -> suggested updates enter review",
                Set.of(
                    "tests/e2e/pilot-business-flows.spec.ts S08",
                    "rtk npm run test:e2e:pilot",
                    "ClientApiCommandServiceTest",
                    "ConsultantInterviewFeedbackReviewControllerTest",
                    "InterviewFeedbackReviewServiceTest")),
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
            PilotAcceptanceRequirement.passed(
                "negative-high-reidentification-blocks-send",
                PilotAcceptanceCategory.NEGATIVE_GATE,
                "Shortlist cannot be sent when re-identification risk is high and unresolved",
                Set.of(
                    "ReidentificationRiskAssessmentServiceTest",
                    "ShortlistBuilderServiceTest#sendToClientFailsWhenIncludedCardHasHighReidentificationRisk",
                    "RedactionAuditPostgresIntegrationTest")),
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
                Set.of("Docker Desktop 4.77.0 / Engine 29.5.3 server reachable on 2026-06-12.")),
            PilotAcceptanceRequirement.passed(
                "validation-core-api-maven-test",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "PATH=/opt/homebrew/bin:$PATH rtk mvn -f services/core-api/pom.xml test",
                Set.of("Passed for the Task 42 gate patch on 2026-05-08 with the full core-api suite.")),
            PilotAcceptanceRequirement.passed(
                "validation-browser-e2e",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "Browser E2E tests for the eight pilot flows",
                Set.of("rtk npm run test:e2e:pilot")),
            PilotAcceptanceRequirement.passed(
                "validation-pilot-data-cli",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "Task 38 pilot data rebuild/validate/export/reset gate",
                Set.of(
                    "rtk npm run pilot:data:rebuild",
                    "rtk npm run pilot:data:validate",
                    "rtk npm run pilot:data:export",
                    "RTO_PILOT_DATA_ALLOW_RESET=true rtk npm run pilot:data:reset")),
            PilotAcceptanceRequirement.passed(
                "validation-backup-restore",
                PilotAcceptanceCategory.VALIDATION_COMMAND,
                "Backup/restore validation",
                Set.of("artifacts/task42-backup-restore-20260509/evidence.md"))));
  }
}
