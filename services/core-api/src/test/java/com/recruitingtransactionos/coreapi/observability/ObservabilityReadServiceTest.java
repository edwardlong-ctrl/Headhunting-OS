package com.recruitingtransactionos.coreapi.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityAITaskRunSearchResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityDisclosureAuditExportResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ObservabilityReviewEventSearchResponse;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureReviewStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecision;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecisionStatus;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ModelRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WriteBackTarget;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ObservabilityReadServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000400201");
  private static final UUID RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000400202");
  private static final UUID REQUESTED_BY = UUID.fromString("00000000-0000-0000-0000-000000400203");
  private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000400204");
  private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000400205");
  private static final UUID CAUSATION_ID = UUID.fromString("00000000-0000-0000-0000-000000400206");

  @Test
  void aiTaskRunTraceOmitsRawPayloadsAndPreservesTraceMetadata() {
    ObservabilityReadService service = new ObservabilityReadService(
        query -> List.of(),
        query -> List.of(),
        query -> List.of(aiTaskRun()),
        (organizationId, disclosureRef) -> Optional.empty());

    ObservabilityAITaskRunSearchResponse response = service.searchAiTaskRuns(
        new ObservabilityAITaskRunQuery(
            ORGANIZATION_ID,
            null,
            null,
            null,
            null,
            CORRELATION_ID,
            CAUSATION_ID,
            null,
            null,
            50,
            0));

    assertThat(response.items()).hasSize(1);
    ObservabilityAITaskRunResponse item = response.items().getFirst();
    assertThat(item.aiTaskRunId()).isEqualTo(RUN_ID.toString());
    assertThat(item.inputSchemaVersion()).isEqualTo("candidate-profile-parser-input-v1");
    assertThat(item.outputSchemaVersion()).isEqualTo("candidate-profile-parser-output-v1");
    assertThat(item.promptVersion()).isEqualTo("prompt-v1");
    assertThat(item.status()).isEqualTo("succeeded");
    assertThat(item.modelProvider()).isEqualTo("deepseek");
    assertThat(item.modelName()).isEqualTo("deepseek-v4-pro");
    assertThat(item.costUnits()).isEqualByComparingTo("12.500000");
    assertThat(item.latencyMs()).isEqualTo(3000L);
    assertThat(item.traceRef()).isEqualTo("trace-task40");
    assertThat(item.correlationId()).isEqualTo(CORRELATION_ID.toString());
    assertThat(item.causationId()).isEqualTo(CAUSATION_ID.toString());
    assertThat(item.toString())
        .doesNotContain("candidate@example.com")
        .doesNotContain("Bearer")
        .doesNotContain("sk-");
  }

  @Test
  void reviewEventSearchRedactsFreeTextReasonAndPreservesReasonCodesOnly() {
    ObservabilityReadService service = new ObservabilityReadService(
        query -> List.of(),
        query -> List.of(new ObservabilityReviewEventRecord(
            UUID.fromString("00000000-0000-0000-0000-000000400207"),
            REQUESTED_BY,
            "CLAIM",
            TARGET_ID,
            "headline",
            "T2_MEDIUM_RISK",
            "approved",
            "approved",
            null,
            "source-span-ref",
            "candidate@example.com said raw private source text",
            Instant.parse("2026-05-08T01:00:00Z"))),
        query -> List.of(),
        (organizationId, disclosureRef) -> Optional.empty());

    ObservabilityReviewEventSearchResponse response = service.searchReviewEvents(new ObservabilityReviewEventQuery(
        ORGANIZATION_ID,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        50,
        0));

    assertThat(response.items().getFirst().reason()).isEqualTo("reason_redacted");
    assertThat(response.toString()).doesNotContain("candidate@example.com").doesNotContain("raw private source text");
  }

  @Test
  void disclosureAuditExportReportsMissingDisclosureWithoutInference() {
    ObservabilityReadService service = new ObservabilityReadService(
        query -> List.of(),
        query -> List.of(),
        query -> List.of(),
        (organizationId, disclosureRef) -> Optional.empty());

    ObservabilityDisclosureAuditExportResponse response = service.disclosureAuditExport(
        new ObservabilityDisclosureAuditExportQuery(ORGANIZATION_ID, "disclosure_missing"));

    assertThat(response.disclosureRecordRef()).isEqualTo("disclosure_missing");
    assertThat(response.missingReasonCodes()).containsExactly("missing_disclosure_record");
    assertThat(response.workflowEvents()).isEmpty();
    assertThat(response.aiTaskRuns()).isEmpty();
    assertThat(response.reviewEvents()).isEmpty();
  }

  @Test
  void disclosureAuditExportUsesExplicitDisclosureEntityIdForRelatedRecords() {
    String disclosureRef = "disclosure_task40";
    UUID disclosureEntityId = ConsentDisclosureWorkflowEntityIds.disclosureEntityId(
        ORGANIZATION_ID,
        disclosureRef);
    AtomicReference<ObservabilityWorkflowEventQuery> workflowQuery = new AtomicReference<>();
    AtomicReference<ObservabilityAITaskRunQuery> aiQuery = new AtomicReference<>();
    AtomicReference<ObservabilityReviewEventQuery> reviewQuery = new AtomicReference<>();
    ObservabilityReadService service = new ObservabilityReadService(
        query -> {
          workflowQuery.set(query);
          return List.of();
        },
        query -> {
          reviewQuery.set(query);
          return List.of();
        },
        query -> {
          aiQuery.set(query);
          return List.of();
        },
        (organizationId, requestedDisclosureRef) -> Optional.of(disclosureRecord(disclosureRef)));

    service.disclosureAuditExport(new ObservabilityDisclosureAuditExportQuery(ORGANIZATION_ID, disclosureRef));

    assertThat(workflowQuery.get().entityType()).isEqualTo("DISCLOSURE");
    assertThat(workflowQuery.get().entityId()).isEqualTo(disclosureEntityId);
    assertThat(aiQuery.get().targetEntityType()).isEqualTo("DISCLOSURE");
    assertThat(aiQuery.get().targetEntityId()).isEqualTo(disclosureEntityId);
    assertThat(reviewQuery.get().targetEntityType()).isEqualTo("DISCLOSURE");
    assertThat(reviewQuery.get().targetEntityId()).isEqualTo(disclosureEntityId);
  }

  @Test
  void disclosureAuditExportIncludesConsentVersionAndApproverMetadataWhenLinked() {
    String disclosureRef = "disclosure_task40";
    ObservabilityReadService service = new ObservabilityReadService(
        query -> List.of(),
        query -> List.of(),
        query -> List.of(),
        (organizationId, requestedDisclosureRef) -> Optional.of(disclosureRecord(disclosureRef)),
        (organizationId, consentRecordRef) -> Optional.of(consentRecord(consentRecordRef)),
        (organizationId, unlockDecisionRef) -> Optional.of(unlockDecision(unlockDecisionRef)));

    ObservabilityDisclosureAuditExportResponse response = service.disclosureAuditExport(
        new ObservabilityDisclosureAuditExportQuery(ORGANIZATION_ID, disclosureRef));

    assertThat(response.consentRecordRef()).isEqualTo("consent_ref_task40");
    assertThat(response.consentStatus()).isEqualTo("confirmed");
    assertThat(response.consentTextVersion()).isEqualTo("consent-text-v7");
    assertThat(response.profileVersion()).isEqualTo("42");
    assertThat(response.approverRole()).isEqualTo("admin");
    assertThat(response.approverUserId()).isEqualTo(REQUESTED_BY.toString());
    assertThat(response.requesterRole()).isNull();
    assertThat(response.requesterUserId()).isNull();
    assertThat(response.missingReasonCodes())
        .contains("missing_requester_link")
        .doesNotContain("missing_consent_record", "missing_unlock_decision");
  }

  @Test
  void aiTaskRunSearchFailsClosedForInvertedTimeRange() {
    ObservabilityReadService service = new ObservabilityReadService(
        query -> List.of(),
        query -> List.of(),
        query -> List.of(),
        (organizationId, disclosureRef) -> Optional.empty());

    assertThatThrownBy(() -> service.searchAiTaskRuns(new ObservabilityAITaskRunQuery(
        ORGANIZATION_ID,
        null,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2026-05-08T02:00:00Z"),
        Instant.parse("2026-05-08T01:00:00Z"),
        50,
        0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("startedFrom must be before or equal to startedTo");
  }

  private static AITaskRunRecord aiTaskRun() {
    return new AITaskRunRecord(
        new AITaskRunId(RUN_ID),
        ORGANIZATION_ID,
        "candidate_profile_parser",
        "v1",
        "candidate-profile-parser-input-v1",
        "candidate-profile-parser-output-v1",
        "prompt-v1",
        new ModelRef("deepseek", "deepseek-v4-pro", null),
        AITaskRunStatus.SUCCEEDED,
        "not_required",
        new WriteBackTarget("no_write_back"),
        "{\"email\":\"candidate@example.com\",\"token\":\"Bearer sk-secret\"}",
        "{\"safe\":\"ok\"}",
        "[]",
        new BigDecimal("12.500000"),
        "trace-task40",
        null,
        "{}",
        null,
        new ActorRef(REQUESTED_BY, ActorRole.ADMIN),
        new WorkflowCorrelationId(CORRELATION_ID),
        new WorkflowCausationId(CAUSATION_ID),
        new EntityRef("DISCLOSURE", TARGET_ID),
        List.of(),
        Instant.parse("2026-05-08T01:00:00Z"),
        Instant.parse("2026-05-08T01:00:03Z"),
        null,
        Instant.parse("2026-05-08T01:00:00Z"));
  }

  private static DisclosureRecord disclosureRecord(String disclosureRef) {
    return new DisclosureRecord(
        disclosureRef,
        ORGANIZATION_ID,
        "candidate_ref_task40",
        "candidate_profile_ref_task40",
        "job_ref_task40",
        "client_ref_task40",
        DisclosureStatus.CONSULTANT_APPROVED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        "unlock_ref_task40",
        "consent_ref_task40",
        Optional.empty(),
        Instant.parse("2026-05-08T01:00:00Z"));
  }

  private static ConsentRecord consentRecord(String consentRecordRef) {
    return new ConsentRecord(
        consentRecordRef,
        ORGANIZATION_ID,
        "candidate_ref_task40",
        "candidate_profile_ref_task40",
        "job_ref_task40",
        "42",
        "consent-text-v7",
        ConsentStatus.CONFIRMED,
        Set.of(DisclosureLevel.L4_IDENTITY_DISCLOSED),
        Instant.parse("2026-05-08T00:30:00Z"),
        null,
        false);
  }

  private static UnlockDecision unlockDecision(String unlockDecisionRef) {
    return new UnlockDecision(
        unlockDecisionRef,
        ORGANIZATION_ID,
        "candidate_ref_task40",
        "candidate_profile_ref_task40",
        "job_ref_task40",
        "client_ref_task40",
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        UnlockDecisionStatus.APPROVED,
        DisclosureReviewStatus.HUMAN_APPROVED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        new ActorRef(REQUESTED_BY, ActorRole.ADMIN),
        Instant.parse("2026-05-08T00:45:00Z"));
  }
}
