package com.recruitingtransactionos.coreapi.workflowautomation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WorkflowAutomationPolicy {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final WorkflowAutomationPolicy STANDARD = new WorkflowAutomationPolicy(List.of(
      rule(
          "consent-sla",
          "Consent SLA",
          "consent",
          "CONSENT_REQUESTED",
          PortalRole.CONSULTANT,
          Duration.ofHours(48),
          Duration.ofHours(24),
          Duration.ofHours(72),
          "Send a consent follow-up and verify the candidate-visible disclosure scope."),
      rule(
          "clarification-sla",
          "Clarification SLA",
          "clarification",
          "CLIENT_CLARIFICATION_REQUESTED",
          PortalRole.CLIENT,
          Duration.ofHours(24),
          Duration.ofHours(12),
          Duration.ofHours(36),
          "Ask the client for missing clarification and keep the job intake blocked until reviewed."),
      rule(
          "feedback-sla",
          "Feedback SLA",
          "feedback",
          "SHORTLIST_CLIENT_FEEDBACK_PENDING",
          PortalRole.CLIENT,
          Duration.ofHours(48),
          Duration.ofHours(24),
          Duration.ofHours(72),
          "Ask the client for shortlist feedback and record any blocker before advancing the workflow."),
      rule(
          "interview-sla",
          "Interview SLA",
          "interview",
          "CANDIDATE_INTERVIEWING",
          PortalRole.CONSULTANT,
          Duration.ofHours(72),
          Duration.ofHours(48),
          Duration.ofHours(96),
          "Confirm interview status with both sides and update the timeline before offer handling."),
      rule(
          "offer-sla",
          "Offer SLA",
          "offer",
          "CANDIDATE_OFFER_PENDING",
          PortalRole.CONSULTANT,
          Duration.ofHours(48),
          Duration.ofHours(24),
          Duration.ofHours(72),
          "Collect offer terms, validate sensitive commitments, and route for human approval."),
      rule(
          "invoice-sla",
          "Invoice SLA",
          "invoice",
          "INVOICE_READY",
          PortalRole.OWNER,
          Duration.ofHours(24),
          Duration.ofHours(12),
          Duration.ofHours(48),
          "Send or review the invoice action and preserve the commercial audit trail."),
      rule(
          "guarantee-sla",
          "Guarantee SLA",
          "guarantee",
          "GUARANTEE_ACTIVATED",
          PortalRole.CONSULTANT,
          Duration.ofDays(7),
          Duration.ofDays(3),
          Duration.ofDays(10),
          "Check guarantee health, record client/candidate risk, and escalate replacement risk if needed.")));

  private final List<WorkflowAutomationRule> rules;

  public WorkflowAutomationPolicy(List<WorkflowAutomationRule> rules) {
    this.rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
  }

  public static WorkflowAutomationPolicy standard() {
    return STANDARD;
  }

  public List<WorkflowAutomationRule> rules() {
    return rules;
  }

  public Optional<WorkflowAutomationAssessment> assess(WorkflowAuditRecord record, Instant now) {
    Objects.requireNonNull(record, "record must not be null");
    Objects.requireNonNull(now, "now must not be null");
    return rules.stream()
        .filter(rule -> rule.actionCode().equals(record.actionCode()))
        .findFirst()
        .map(rule -> assessment(rule, record, now));
  }

  private static WorkflowAutomationAssessment assessment(
      WorkflowAutomationRule rule,
      WorkflowAuditRecord record,
      Instant now) {
    Instant dueAt = record.occurredAt().plus(rule.dueAfter());
    Instant reminderAt = record.occurredAt().plus(rule.reminderAfter());
    Instant escalationAt = record.occurredAt().plus(rule.escalationAfter());
    WorkflowAutomationStatus status = status(now, reminderAt, dueAt, escalationAt);
    return new WorkflowAutomationAssessment(
        record.workflowEventId().value(),
        record.entityType(),
        record.entityId(),
        record.actionCode(),
        rule.workflowFamily(),
        rule.ownerRole(),
        record.occurredAt(),
        dueAt,
        reminderAt,
        escalationAt,
        status,
        blockerCode(rule, status),
        rule.nextBestAction());
  }

  private static WorkflowAutomationStatus status(
      Instant now,
      Instant reminderAt,
      Instant dueAt,
      Instant escalationAt) {
    if (!now.isBefore(escalationAt)) {
      return WorkflowAutomationStatus.ESCALATED;
    }
    if (!now.isBefore(dueAt)) {
      return WorkflowAutomationStatus.STALLED;
    }
    if (!now.isBefore(reminderAt)) {
      return WorkflowAutomationStatus.REMINDER_DUE;
    }
    return WorkflowAutomationStatus.PENDING;
  }

  private static String blockerCode(WorkflowAutomationRule rule, WorkflowAutomationStatus status) {
    return switch (status) {
      case ESCALATED -> "sla_escalated";
      case STALLED -> rule.blockerCode();
      case REMINDER_DUE -> "sla_reminder_due";
      case PENDING -> "none";
    };
  }

  public static Optional<String> extractStatus(String stateJson) {
    if (stateJson == null || stateJson.isBlank()) {
      return Optional.empty();
    }
    try {
      JsonNode status = OBJECT_MAPPER.readTree(stateJson).get("status");
      return status == null || status.isNull() ? Optional.empty() : Optional.of(status.asText());
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private static WorkflowAutomationRule rule(
      String key,
      String label,
      String workflowFamily,
      String actionCode,
      PortalRole ownerRole,
      Duration dueAfter,
      Duration reminderAfter,
      Duration escalationAfter,
      String nextBestAction) {
    return new WorkflowAutomationRule(
        key,
        label,
        workflowFamily,
        actionCode,
        ownerRole,
        dueAfter,
        reminderAfter,
        escalationAfter,
        nextBestAction,
        "sla_overdue");
  }
}
