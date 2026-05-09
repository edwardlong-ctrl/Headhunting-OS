package com.recruitingtransactionos.coreapi.workflowautomation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkflowAutomationPolicyTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000450001");
  private static final UUID ENTITY_ID =
      UUID.fromString("00000000-0000-0000-0000-000000450002");

  @Test
  void standardRulesCoverTask45WorkflowFamilies() {
    WorkflowAutomationPolicy policy = WorkflowAutomationPolicy.standard();

    assertThat(policy.rules())
        .extracting(WorkflowAutomationRule::workflowFamily)
        .contains(
            "consent",
            "clarification",
            "feedback",
            "interview",
            "offer",
            "invoice",
            "guarantee");
  }

  @Test
  void assessmentSurfacesStalledConsentWithDueDatesEscalationAndNextBestAction() {
    WorkflowAutomationPolicy policy = WorkflowAutomationPolicy.standard();
    WorkflowAuditRecord record = workflowEvent(
        "CONSENT_REQUESTED",
        "CANDIDATE",
        "{\"status\":\"not_requested\"}",
        "{\"status\":\"requested\"}",
        Instant.parse("2026-05-01T00:00:00Z"));

    WorkflowAutomationAssessment assessment = policy.assess(
        record,
        Instant.parse("2026-05-04T01:00:00Z")).orElseThrow();

    assertThat(assessment.status()).isEqualTo(WorkflowAutomationStatus.ESCALATED);
    assertThat(assessment.ownerRole()).isEqualTo(PortalRole.CONSULTANT);
    assertThat(assessment.dueAt()).isEqualTo(Instant.parse("2026-05-03T00:00:00Z"));
    assertThat(assessment.reminderAt()).isEqualTo(Instant.parse("2026-05-02T00:00:00Z"));
    assertThat(assessment.escalationAt()).isEqualTo(Instant.parse("2026-05-04T00:00:00Z"));
    assertThat(assessment.blockerCode()).isEqualTo("sla_escalated");
    assertThat(assessment.nextBestAction())
        .isEqualTo("Send a consent follow-up and verify the candidate-visible disclosure scope.");
  }

  @Test
  void manualOverrideRequiresAuditableReason() {
    assertThatThrownBy(() -> WorkflowManualOverrideRequest.create(
        ORG_ID,
        ENTITY_ID,
        "CONSENT_REQUESTED",
        PortalRole.CONSULTANT,
        " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("override reason");

    WorkflowManualOverrideRequest request = WorkflowManualOverrideRequest.create(
        ORG_ID,
        ENTITY_ID,
        "CONSENT_REQUESTED",
        PortalRole.CONSULTANT,
        "Candidate confirmed consent out of band; attaching call note.");

    assertThat(request.reason())
        .isEqualTo("Candidate confirmed consent out of band; attaching call note.");
  }

  @Test
  void automationRuleRequiresReminderBeforeDue() {
    assertThatThrownBy(() -> new WorkflowAutomationRule(
        "bad-reminder",
        "Bad Reminder",
        "consent",
        "CONSENT_REQUESTED",
        PortalRole.CONSULTANT,
        java.time.Duration.ofHours(24),
        java.time.Duration.ofHours(48),
        java.time.Duration.ofHours(72),
        "Send a consent follow-up.",
        "sla_overdue"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("reminderAfter");
  }

  @Test
  void timelineExportIncludesAutomationEvidenceWithoutRawCandidateData() {
    WorkflowAuditRecord record = workflowEvent(
        "SHORTLIST_CLIENT_FEEDBACK_PENDING",
        "SHORTLIST",
        "{\"status\":\"client_viewed\"}",
        "{\"status\":\"client_feedback_pending\"}",
        Instant.parse("2026-05-01T00:00:00Z"));
    WorkflowAutomationAssessment assessment = WorkflowAutomationPolicy.standard()
        .assess(record, Instant.parse("2026-05-04T00:00:00Z"))
        .orElseThrow();

    WorkflowTimelineExport export = WorkflowTimelineExport.from(List.of(record), List.of(assessment));

    assertThat(export.format()).isEqualTo("csv");
    assertThat(export.content())
        .contains("workflow_event_id,entity_type,entity_id,action_code,occurred_at,due_at,automation_status,next_best_action")
        .contains("SHORTLIST_CLIENT_FEEDBACK_PENDING")
        .contains("2026-05-03T00:00:00Z")
        .contains("Ask the client for shortlist feedback and record any blocker before advancing the workflow.");
    assertThat(export.content()).doesNotContain("raw_candidate");
  }

  private static WorkflowAuditRecord workflowEvent(
      String actionCode,
      String entityType,
      String beforeState,
      String afterState,
      Instant occurredAt) {
    return new WorkflowAuditRecord(
        new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000450101")),
        ORG_ID,
        "workflow",
        entityType,
        ENTITY_ID,
        actionCode,
        ActorRole.CONSULTANT,
        UUID.fromString("00000000-0000-0000-0000-000000450102"),
        WorkflowAiInvolvement.AI_ASSISTED,
        RiskTier.T2_MEDIUM_RISK,
        new WorkflowStateSnapshot(beforeState),
        new WorkflowStateSnapshot(afterState),
        "Task45 workflow automation test event",
        new WorkflowIdempotencyKey("task45-workflow-automation-test"),
        WorkflowCorrelationId.fromWireValue("00000000-0000-0000-0000-000000450103"),
        WorkflowCausationId.fromWireValue("00000000-0000-0000-0000-000000450104"),
        null,
        "task45_test",
        UUID.fromString("00000000-0000-0000-0000-000000450105"),
        occurredAt,
        occurredAt);
  }
}
