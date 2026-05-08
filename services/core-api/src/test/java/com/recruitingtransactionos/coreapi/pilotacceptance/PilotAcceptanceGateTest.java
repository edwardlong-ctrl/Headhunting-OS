package com.recruitingtransactionos.coreapi.pilotacceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PilotAcceptanceGateTest {

  @Test
  void task42GateTracksAllPilotFlowsNegativeGatesAndValidationCommandsWithoutReadinessShortcut() {
    PilotAcceptanceReport report = PilotAcceptanceGate.task42Baseline();

    assertThat(report.overallStatus()).isEqualTo(PilotAcceptanceOverallStatus.NOT_READY);
    assertThat(report.readyForControlledPilot()).isFalse();
    assertThat(report.requirements()).hasSize(26);

    assertThat(report.requirements())
        .filteredOn(requirement -> requirement.category() == PilotAcceptanceCategory.PILOT_FLOW)
        .extracting(PilotAcceptanceRequirement::id)
        .containsExactly(
            "flow-1-consultant-cv-ai-review-canonical",
            "flow-2-client-jd-ai-clarification-activation",
            "flow-3-match-report-evidence-score-cap",
            "flow-4-anonymous-shortlist-client-safe-preview",
            "flow-5-candidate-opportunity-consent",
            "flow-6-client-shortlist-unlock-request",
            "flow-7-consultant-unlock-disclosure-identity",
            "flow-8-client-feedback-outcome-review");

    assertThat(report.requirements())
        .filteredOn(requirement -> requirement.category() == PilotAcceptanceCategory.NEGATIVE_GATE)
        .extracting(PilotAcceptanceRequirement::id)
        .containsExactly(
            "negative-client-raw-candidate-denied",
            "negative-anonymous-card-no-raw-id",
            "negative-l4-requires-consent-and-approval",
            "negative-ai-no-direct-canonical-write",
            "negative-ai-no-self-approval",
            "negative-bulk-approve-not-verified-fact",
            "negative-high-reidentification-blocks-send",
            "negative-disclosure-prerequisites-not-bypassed",
            "negative-candidate-self-scope",
            "negative-admin-no-domain-bypass");

    assertThat(report.requirements())
        .filteredOn(requirement -> requirement.category() == PilotAcceptanceCategory.VALIDATION_COMMAND)
        .extracting(PilotAcceptanceRequirement::id)
        .containsExactly(
            "validation-git-diff-check",
            "validation-web-typecheck",
            "validation-web-build",
            "validation-docker-info",
            "validation-core-api-maven-test",
            "validation-browser-e2e",
            "validation-pilot-data-cli",
            "validation-backup-restore");

    assertThat(report.blockingRequirements())
        .extracting(PilotAcceptanceRequirement::id)
        .contains(
            "flow-1-consultant-cv-ai-review-canonical",
            "flow-2-client-jd-ai-clarification-activation",
            "validation-browser-e2e",
            "validation-backup-restore");
  }

  @Test
  void readyStatusRequiresEveryRequirementToBePassedAndEvidenceBacked() {
    PilotAcceptanceRequirement passedFlow = PilotAcceptanceRequirement.passed(
        "flow-test",
        PilotAcceptanceCategory.PILOT_FLOW,
        "Flow",
        Set.of("SomeEvidenceTest"));
    PilotAcceptanceRequirement blockedCommand = PilotAcceptanceRequirement.blocked(
        "validation-test",
        PilotAcceptanceCategory.VALIDATION_COMMAND,
        "Command",
        Set.of("command not implemented"));

    PilotAcceptanceReport blocked = PilotAcceptanceReport.fromRequirements(
        "task42-test",
        "Task 42 test",
        List.of(passedFlow, blockedCommand));
    assertThat(blocked.readyForControlledPilot()).isFalse();
    assertThat(blocked.overallStatus()).isEqualTo(PilotAcceptanceOverallStatus.NOT_READY);

    PilotAcceptanceReport ready = PilotAcceptanceReport.fromRequirements(
        "task42-test",
        "Task 42 test",
        List.of(passedFlow));
    assertThat(ready.readyForControlledPilot()).isTrue();
    assertThat(ready.overallStatus()).isEqualTo(PilotAcceptanceOverallStatus.CONTROLLED_PILOT_READY);
  }

  @Test
  void reportConstructorRejectsReadyStatusWhenAnyRequirementIsNotPassedWithEvidence() {
    PilotAcceptanceRequirement passedFlow = PilotAcceptanceRequirement.passed(
        "flow-test",
        PilotAcceptanceCategory.PILOT_FLOW,
        "Flow",
        Set.of("SomeEvidenceTest"));
    PilotAcceptanceRequirement blockedCommand = PilotAcceptanceRequirement.blocked(
        "validation-test",
        PilotAcceptanceCategory.VALIDATION_COMMAND,
        "Command",
        Set.of("command not implemented"));

    assertThatThrownBy(() -> new PilotAcceptanceReport(
        "task42-test",
        "Task 42 test",
        PilotAcceptanceOverallStatus.CONTROLLED_PILOT_READY,
        List.of(passedFlow, blockedCommand)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("pilot_acceptance_report_status_mismatch");
  }

  @Test
  void passedRequirementsRequireNonBlankEvidenceEntries() {
    assertThatThrownBy(() -> PilotAcceptanceRequirement.passed(
        "validation-test",
        PilotAcceptanceCategory.VALIDATION_COMMAND,
        "Command",
        Set.of(" ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("evidence_entry_required");
  }

  @Test
  void requirementConstructorRejectsContradictoryStatusEvidenceAndBlockerCombinations() {
    assertThatThrownBy(() -> new PilotAcceptanceRequirement(
        "validation-test",
        PilotAcceptanceCategory.VALIDATION_COMMAND,
        "Command",
        PilotAcceptanceRequirementStatus.PASSED,
        Set.of("evidence"),
        Set.of("blocker")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("passed_pilot_acceptance_requirement_cannot_have_blockers");

    assertThatThrownBy(() -> new PilotAcceptanceRequirement(
        "validation-test",
        PilotAcceptanceCategory.VALIDATION_COMMAND,
        "Command",
        PilotAcceptanceRequirementStatus.BLOCKED,
        Set.of("evidence"),
        Set.of("blocker")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("blocked_pilot_acceptance_requirement_cannot_have_evidence");

    assertThatThrownBy(() -> new PilotAcceptanceRequirement(
        "validation-test",
        PilotAcceptanceCategory.VALIDATION_COMMAND,
        "Command",
        PilotAcceptanceRequirementStatus.PARTIAL,
        Set.of(),
        Set.of("blocker")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("partial_pilot_acceptance_requirement_requires_evidence");
  }
}
