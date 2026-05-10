package com.recruitingtransactionos.coreapi.reportingexport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.apiboundary.ReportingExportResult;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportingExportServicePolicyTest {

  private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000570001");
  private static final UUID OTHER_ORG = UUID.fromString("00000000-0000-0000-0000-000000570099");
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-05-10T09:30:00Z"), ZoneOffset.UTC);

  @Test
  void crossOrganizationExportsFailClosedBeforeAdapterLookup() {
    RecordingAdapter adapter = new RecordingAdapter(safePayload("owner-report"));
    ReportingExportService service = service(adapter);

    ReportingExportRequest request = request(
        PortalRole.OWNER,
        ReportingExportType.OWNER_REPORT,
        FieldVisibilityPolicy.OWNER_INTERNAL,
        new ReportingExportTargetScope(OTHER_ORG, "organization", ORG.toString(), "owner-1", null));

    assertThatThrownBy(() -> service.export(request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("export_not_available")
        .hasMessageNotContaining(ORG.toString())
        .hasMessageNotContaining(OTHER_ORG.toString());
    assertThat(adapter.calls).isZero();
  }

  @Test
  void roleIneligibleExportsDoNotRevealRecordExistence() {
    RecordingAdapter adapter = new RecordingAdapter(safePayload("owner-report"));
    ReportingExportService service = service(adapter);

    ReportingExportRequest request = request(
        PortalRole.CLIENT,
        ReportingExportType.OWNER_REPORT,
        FieldVisibilityPolicy.OWNER_INTERNAL,
        new ReportingExportTargetScope(ORG, "organization", "org-report-57", "client-1", null));

    assertThatThrownBy(() -> service.export(request))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("export_not_available")
        .hasMessageNotContaining("org-report-57");
    assertThat(adapter.calls).isZero();
  }

  @Test
  void clientShortlistExportDropsRawUndisclosedCandidateFields() {
    ReportingExportPayload payload = new ReportingExportPayload(
        "json",
        "client_safe_shortlist_feedback_export",
        "client_safe_review_package",
        false,
        List.of(new ReportingExportSection("shortlist", List.of(
            new ReportingExportField("anonymousCandidateRef", "anon-57", FieldVisibilityPolicy.CLIENT_SAFE),
            new ReportingExportField("rawCandidateName", "Jane Raw Candidate", FieldVisibilityPolicy.RAW_PII),
            new ReportingExportField("safeFeedbackSummary", "Strong evidence coverage", FieldVisibilityPolicy.CLIENT_SAFE)))),
        List.of("client-safe projection policy applied"),
        List.of());
    ReportingExportService service = service(new RecordingAdapter(payload));

    ReportingExportResult result = service.export(request(
        PortalRole.CLIENT,
        ReportingExportType.CLIENT_SHORTLIST_FEEDBACK,
        FieldVisibilityPolicy.CLIENT_SAFE,
        new ReportingExportTargetScope(ORG, "shortlist", "shortlist-57", "client-1", null)));

    assertThat(result.sections())
        .flatExtracting(ReportingExportSection::fields)
        .extracting(ReportingExportField::value)
        .doesNotContain("Jane Raw Candidate");
    assertThat(result.redactedOrWithheldFields()).contains("rawCandidateName");
    assertThat(result.fieldVisibilityPolicy()).isEqualTo("client_safe");
  }

  @Test
  void candidatePersonalDataExportCannotTargetOtherCandidateOrOtherOrganization() {
    RecordingAdapter adapter = new RecordingAdapter(safePayload("candidate-export"));
    ReportingExportService service = service(adapter);

    ReportingExportRequest otherCandidate = request(
        PortalRole.CANDIDATE,
        ReportingExportType.CANDIDATE_PERSONAL_DATA,
        FieldVisibilityPolicy.CANDIDATE_SELF,
        new ReportingExportTargetScope(ORG, "candidate", "candidate-2", "candidate-1", null));

    assertThatThrownBy(() -> service.export(otherCandidate))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("export_not_available")
        .hasMessageNotContaining("candidate-2");

    ReportingExportRequest otherOrg = request(
        PortalRole.CANDIDATE,
        ReportingExportType.CANDIDATE_PERSONAL_DATA,
        FieldVisibilityPolicy.CANDIDATE_SELF,
        new ReportingExportTargetScope(OTHER_ORG, "candidate", "candidate-1", "candidate-1", null));

    assertThatThrownBy(() -> service.export(otherOrg))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("export_not_available")
        .hasMessageNotContaining(OTHER_ORG.toString());
    assertThat(adapter.calls).isZero();
  }

  @Test
  void placementCommissionExportKeepsReadOnlyAccountingHandoffSemantics() {
    ReportingExportPayload payload = new ReportingExportPayload(
        "csv",
        "read_only_accounting_handoff",
        "owner_commercial_supervision",
        false,
        List.of(new ReportingExportSection("accounting-handoff", List.of(
            new ReportingExportField("placementId", "placement-57", FieldVisibilityPolicy.COMMERCIAL_READ_ONLY),
            new ReportingExportField("accountingExportStatus", "ready_for_handoff", FieldVisibilityPolicy.COMMERCIAL_READ_ONLY)))),
        List.of("OwnerRevenueQueryService.exportAccountingHandoff"),
        List.of());
    ReportingExportService service = service(new RecordingAdapter(payload));

    ReportingExportResult result = service.export(request(
        PortalRole.OWNER,
        ReportingExportType.PLACEMENT_COMMISSION,
        FieldVisibilityPolicy.COMMERCIAL_READ_ONLY,
        new ReportingExportTargetScope(ORG, "placement", "placement-57", "owner-1", null)));

    assertThat(result.mutationPerformed()).isFalse();
    assertThat(result.semantics()).isEqualTo("read_only_accounting_handoff");
    assertThat(result.provenance()).contains("OwnerRevenueQueryService.exportAccountingHandoff");
  }

  @Test
  void disclosureLegalPackageRequiresAuditAndProvenanceEvidence() {
    ReportingExportPayload payload = new ReportingExportPayload(
        "json",
        "disclosure_legal_audit_package",
        "legal_audit",
        false,
        List.of(new ReportingExportSection("disclosure-audit", List.of(
            new ReportingExportField("disclosureRecordRef", "disclosure-57", FieldVisibilityPolicy.SYSTEM_GOVERNANCE),
            new ReportingExportField("workflowEventId", "workflow-event-57", FieldVisibilityPolicy.SYSTEM_GOVERNANCE)))),
        List.of("ObservabilityReadService.disclosureAuditExport", "workflow-event-57"),
        List.of());
    ReportingExportService service = service(new RecordingAdapter(payload));

    ReportingExportResult result = service.export(request(
        PortalRole.ADMIN,
        ReportingExportType.DISCLOSURE_AUDIT,
        FieldVisibilityPolicy.SYSTEM_GOVERNANCE,
        new ReportingExportTargetScope(ORG, "disclosure", "disclosure-57", "admin-1", null)));

    assertThat(result.auditId()).isEqualTo("audit-57");
    assertThat(result.provenance()).contains("ObservabilityReadService.disclosureAuditExport");
    assertThat(result.sections())
        .flatExtracting(ReportingExportSection::fields)
        .extracting(ReportingExportField::name)
        .contains("workflowEventId");
  }

  @Test
  void retentionPackageDocumentsEvidenceWithoutSilentDeletion() {
    ReportingExportPayload payload = new ReportingExportPayload(
        "json",
        "retention_delete_evidence_package",
        "retention_policy_evidence",
        false,
        List.of(new ReportingExportSection("retention", List.of(
            new ReportingExportField("eligibility", "blocked_confirmed_facts", FieldVisibilityPolicy.RETENTION_EVIDENCE),
            new ReportingExportField("deleteMutationPerformed", "false", FieldVisibilityPolicy.RETENTION_EVIDENCE)))),
        List.of("DataLifecycleService retention decision evidence"),
        List.of());
    ReportingExportService service = service(new RecordingAdapter(payload));

    ReportingExportResult result = service.export(request(
        PortalRole.ADMIN,
        ReportingExportType.RETENTION_DELETE_EVIDENCE,
        FieldVisibilityPolicy.RETENTION_EVIDENCE,
        new ReportingExportTargetScope(ORG, "candidate", "candidate-57", "admin-1", null)));

    assertThat(result.mutationPerformed()).isFalse();
    assertThat(result.sections())
        .flatExtracting(ReportingExportSection::fields)
        .extracting(ReportingExportField::name, ReportingExportField::value)
        .contains(org.assertj.core.groups.Tuple.tuple("deleteMutationPerformed", "false"));
  }

  private static ReportingExportService service(ReportingExportAdapter adapter) {
    return new ReportingExportService(ReportingExportAdapterRegistry.single(adapter), FIXED_CLOCK);
  }

  private static ReportingExportRequest request(
      PortalRole role,
      ReportingExportType exportType,
      FieldVisibilityPolicy policy,
      ReportingExportTargetScope targetScope) {
    return new ReportingExportRequest(ORG, role, targetScope, exportType, policy, "audit-57");
  }

  private static ReportingExportPayload safePayload(String name) {
    return new ReportingExportPayload(
        "json",
        name,
        "test",
        false,
        List.of(new ReportingExportSection(name, List.of(
            new ReportingExportField("safeField", "safe-value", FieldVisibilityPolicy.OWNER_INTERNAL)))),
        List.of("test-provenance"),
        List.of());
  }

  private static final class RecordingAdapter implements ReportingExportAdapter {
    private final ReportingExportPayload payload;
    private int calls;

    private RecordingAdapter(ReportingExportPayload payload) {
      this.payload = payload;
    }

    @Override
    public ReportingExportPayload export(ReportingExportRequest request) {
      calls++;
      return payload;
    }
  }
}
