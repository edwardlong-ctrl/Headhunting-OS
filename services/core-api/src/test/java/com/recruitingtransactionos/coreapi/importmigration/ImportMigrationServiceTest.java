package com.recruitingtransactionos.coreapi.importmigration;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntitySnapshot;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntityType;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleService;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.CsvImportCommand;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.DocumentBatchImportCommand;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.DocumentImportDraft;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.GovernedImportLineage;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportBatchReport;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportDraftRecord;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportEntityReference;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportEntityType;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportRollbackCommand;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportRollbackStatus;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportSourceType;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportValidationStatus;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.LegacyAtsCrmMapping;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.LegacySystemKind;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ValidationReport;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportMigrationServiceTest {

  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000550001");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-000000550002");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000550003");
  private static final UUID EXISTING_CANDIDATE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000550101");
  private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000550201");
  private static final Instant NOW = Instant.parse("2026-05-10T08:00:00Z");

  private final RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
  private final InMemoryImportBatchRepository batches = new InMemoryImportBatchRepository();
  private final RecordingGovernedImportGateway governedGateway = new RecordingGovernedImportGateway();
  private final ImportMigrationService service = new ImportMigrationService(
      batches,
      governedGateway,
      new DataLifecycleService(new WorkflowEventService(workflowEvents)));

  @Test
  void invalidCsvReturnsValidationReportAndDoesNotPartiallyWriteAcceptedRows() {
    String csv = """
        external_id,full_name,email
        cand-1,Alice Zhang,alice@example.com
        cand-2,,missing-name@example.com
        """;

    ImportBatchReport report = service.importCsv(candidateCsv(csv));

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.VALIDATION_FAILED);
    assertThat(report.rowCount()).isEqualTo(2);
    assertThat(report.acceptedCount()).isZero();
    assertThat(report.rejectedCount()).isOne();
    assertThat(report.validationReport().rowErrors())
        .extracting(error -> error.field() + ":" + error.code())
        .contains("full_name:required_field_missing");
    assertThat(governedGateway.createdDrafts).isEmpty();
    assertThat(batches.savedReports()).singleElement().satisfies(saved ->
        assertThat(saved.lineages()).isEmpty());
  }

  @Test
  void crossOrganizationReferencesAreRejectedBeforeGovernedWrites() {
    String csv = """
        external_id,title,company_external_id
        job-1,Senior DV Engineer,company-outside
        """;
    Map<String, ImportEntityReference> refs = Map.of(
        "company-outside",
        new ImportEntityReference(ORG_B, COMPANY_ID, ImportEntityType.COMPANY));

    ImportBatchReport report = service.importCsv(CsvImportCommand.builder()
        .organizationId(ORG_A)
        .actorId(ACTOR_ID)
        .actorRole(ActorRole.CONSULTANT)
        .sourceType(ImportSourceType.CSV_JOB)
        .mapping(LegacyAtsCrmMapping.jobMapping(LegacySystemKind.ATS))
        .csvContent(csv)
        .knownExternalReferences(refs)
        .occurredAt(NOW)
        .build());

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.VALIDATION_FAILED);
    assertThat(report.validationReport().rowErrors())
        .extracting(error -> error.field() + ":" + error.code())
        .contains("company_external_id:cross_org_reference_rejected");
    assertThat(governedGateway.createdDrafts).isEmpty();
  }

  @Test
  void wrongEntityReferencesAreRejectedBeforeGovernedWrites() {
    String csv = """
        external_id,title,company_external_id
        job-1,Senior DV Engineer,candidate-id-used-as-company
        """;
    Map<String, ImportEntityReference> refs = Map.of(
        "candidate-id-used-as-company",
        new ImportEntityReference(ORG_A, EXISTING_CANDIDATE_ID, ImportEntityType.CANDIDATE));

    ImportBatchReport report = service.importCsv(CsvImportCommand.builder()
        .organizationId(ORG_A)
        .actorId(ACTOR_ID)
        .actorRole(ActorRole.CONSULTANT)
        .sourceType(ImportSourceType.CSV_JOB)
        .mapping(LegacyAtsCrmMapping.jobMapping(LegacySystemKind.ATS))
        .csvContent(csv)
        .knownExternalReferences(refs)
        .occurredAt(NOW)
        .build());

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.VALIDATION_FAILED);
    assertThat(report.validationReport().rowErrors())
        .extracting(error -> error.field() + ":" + error.code())
        .contains("company_external_id:wrong_entity_reference_rejected");
    assertThat(governedGateway.createdDrafts).isEmpty();
  }

  @Test
  void duplicateRecordsAreDetectedAndReportedWithTask46DecisionConcepts() {
    String csv = """
        external_id,full_name,email,identity_fingerprint_hash
        cand-dup,Alice Zhang,alice@example.com,fingerprint:alice
        """;

    ImportBatchReport report = service.importCsv(CsvImportCommand.builder()
        .organizationId(ORG_A)
        .actorId(ACTOR_ID)
        .actorRole(ActorRole.CONSULTANT)
        .sourceType(ImportSourceType.CSV_CANDIDATE)
        .mapping(LegacyAtsCrmMapping.candidateMapping(LegacySystemKind.ATS))
        .csvContent(csv)
        .existingSnapshots(List.of(candidateSnapshot(
            EXISTING_CANDIDATE_ID,
            Map.of("identityFingerprintHash", "fingerprint:alice"))))
        .occurredAt(NOW)
        .build());

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.VALIDATION_FAILED);
    assertThat(report.duplicateCount()).isOne();
    assertThat(report.conflictCount()).isOne();
    assertThat(report.validationReport().duplicateDecisions())
        .singleElement()
        .satisfies(decision -> {
          assertThat(decision.rowNumber()).isEqualTo(2);
          assertThat(decision.decision().name()).isEqualTo("HIGH_CONFIDENCE_BLOCK");
          assertThat(decision.justification()).contains("identity fingerprint");
        });
    assertThat(workflowEvents.commands)
        .extracting(WorkflowEventAppendCommand::action)
        .contains("DATA_DUPLICATE_BLOCKED");
    assertThat(governedGateway.createdDrafts).isEmpty();
  }

  @Test
  void acceptedCsvRowsEnterGovernedReviewPathWithoutConfirmedFactWrites() {
    String csv = """
        external_id,full_name,email,identity_fingerprint_hash
        cand-3,Beatrice Lin,bea@example.com,fingerprint:bea
        """;

    ImportBatchReport report = service.importCsv(candidateCsv(csv));

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.IMPORTED_WITH_REVIEW);
    assertThat(report.acceptedCount()).isOne();
    assertThat(report.pendingReviewCount()).isOne();
    assertThat(report.lineages()).singleElement().satisfies(lineage -> {
      assertThat(lineage.sourceItemId()).isNotNull();
      assertThat(lineage.informationPacketId()).isNotNull();
      assertThat(lineage.claimLedgerPathUsed()).isTrue();
      assertThat(lineage.canonicalMutationPerformed()).isFalse();
      assertThat(lineage.confirmedFactWritePerformed()).isFalse();
    });
    assertThat(governedGateway.createdDrafts).singleElement().satisfies(draft -> {
      assertThat(draft.entityType()).isEqualTo(ImportEntityType.CANDIDATE);
      assertThat(draft.sourceItemType()).isEqualTo(SourceItemType.OLD_SYSTEM_EXPORT);
      assertThat(draft.informationPacketType()).isEqualTo(InformationPacketType.CANDIDATE);
      assertThat(draft.intendedEntityType()).isEqualTo(IntendedEntityType.CANDIDATE);
      assertThat(draft.fields()).containsEntry("full_name", "Beatrice Lin");
      assertThat(draft.fields()).doesNotContainKey("confirmed_canonical_fact");
    });
  }

  @Test
  void rollbackOnlyTouchesRecordsFromTheImportBatchAndSameOrganization() {
    ImportBatchReport orgAReport = service.importCsv(candidateCsv("""
        external_id,full_name,email
        cand-a,Alice Zhang,alice@example.com
        """));
    ImportBatchReport orgBReport = service.importCsv(CsvImportCommand.builder()
        .organizationId(ORG_B)
        .actorId(ACTOR_ID)
        .actorRole(ActorRole.CONSULTANT)
        .sourceType(ImportSourceType.CSV_CANDIDATE)
        .mapping(LegacyAtsCrmMapping.candidateMapping(LegacySystemKind.ATS))
        .csvContent("""
            external_id,full_name,email
            cand-b,Bob Wang,bob@example.com
            """)
        .occurredAt(NOW)
        .build());

    ImportBatchReport rollback = service.rollback(new ImportRollbackCommand(
        ORG_A,
        orgAReport.batchId(),
        ACTOR_ID,
        ActorRole.CONSULTANT,
        "operator requested import reset",
        NOW.plusSeconds(60)));

    assertThat(rollback.rollbackStatus()).isEqualTo(ImportRollbackStatus.COMPLETED);
    assertThat(governedGateway.rolledBackLineages)
        .extracting(GovernedImportLineage::batchId)
        .containsExactly(orgAReport.batchId());
    assertThat(governedGateway.rolledBackLineages)
        .noneMatch(lineage -> lineage.batchId().equals(orgBReport.batchId()));
    assertThat(batches.find(ORG_B, orgBReport.batchId()).orElseThrow().rollbackStatus())
        .isEqualTo(ImportRollbackStatus.NOT_REQUESTED);
  }

  @Test
  void batchDocumentImportPreservesDocumentAndSourceLineage() {
    DocumentImportDraft resume = new DocumentImportDraft(
        "resume-1",
        "Li Wei Resume.pdf",
        "application/pdf",
        "resume pdf bytes".getBytes(StandardCharsets.UTF_8),
        ImportEntityType.CANDIDATE,
        Map.of("source_system", "legacy_drive"));

    ImportBatchReport report = service.importDocuments(new DocumentBatchImportCommand(
        ORG_A,
        ACTOR_ID,
        ActorRole.CONSULTANT,
        ImportSourceType.RESUME_DOCUMENT_BATCH,
        List.of(resume),
        NOW));

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.IMPORTED_WITH_REVIEW);
    assertThat(report.documentCount()).isOne();
    assertThat(report.pendingReviewCount()).isOne();
    assertThat(governedGateway.createdDocuments).singleElement().satisfies(document -> {
      assertThat(document.originalFilename()).isEqualTo("Li Wei Resume.pdf");
      assertThat(document.entityType()).isEqualTo(ImportEntityType.CANDIDATE);
      assertThat(document.metadata()).containsEntry("source_system", "legacy_drive");
    });
    assertThat(report.lineages()).singleElement().satisfies(lineage -> {
      assertThat(lineage.sourceItemId()).isNotNull();
      assertThat(lineage.informationPacketId()).isNotNull();
      assertThat(lineage.importExternalId()).contains("resume-1");
      assertThat(lineage.documentOriginalFilename()).contains("Li Wei Resume.pdf");
      assertThat(lineage.claimLedgerPathUsed()).isFalse();
      assertThat(lineage.confirmedFactWritePerformed()).isFalse();
    });
  }

  @Test
  void invalidDocumentBatchReturnsValidationReportAndDoesNotPartiallyWriteDocuments() {
    DocumentImportDraft validResume = new DocumentImportDraft(
        "resume-1",
        "Li Wei Resume.pdf",
        "application/pdf",
        "resume pdf bytes".getBytes(StandardCharsets.UTF_8),
        ImportEntityType.CANDIDATE,
        Map.of("source_system", "legacy_drive"));
    DocumentImportDraft invalidResume = new DocumentImportDraft(
        "resume-2",
        "Malware Blob.bin",
        "application/octet-stream",
        "unsupported bytes".getBytes(StandardCharsets.UTF_8),
        ImportEntityType.CANDIDATE,
        Map.of("source_system", "legacy_drive"));

    ImportBatchReport report = service.importDocuments(new DocumentBatchImportCommand(
        ORG_A,
        ACTOR_ID,
        ActorRole.CONSULTANT,
        ImportSourceType.RESUME_DOCUMENT_BATCH,
        List.of(validResume, invalidResume),
        NOW));

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.VALIDATION_FAILED);
    assertThat(report.acceptedCount()).isZero();
    assertThat(report.rejectedCount()).isOne();
    assertThat(report.validationReport().rowErrors())
        .extracting(error -> error.field() + ":" + error.code())
        .contains("mimeType:unsupported_document_mime_type");
    assertThat(governedGateway.createdDocuments).isEmpty();
  }

  @Test
  void oversizedDocumentBatchReturnsValidationReportAndDoesNotPartiallyWriteDocuments() {
    DocumentImportDraft validResume = new DocumentImportDraft(
        "resume-1",
        "Li Wei Resume.pdf",
        "application/pdf",
        "resume pdf bytes".getBytes(StandardCharsets.UTF_8),
        ImportEntityType.CANDIDATE,
        Map.of());
    DocumentImportDraft oversizedNotes = new DocumentImportDraft(
        "notes-1",
        "Large Notes.txt",
        "text/plain",
        new byte[(5 * 1024 * 1024) + 1],
        ImportEntityType.CANDIDATE,
        Map.of());

    ImportBatchReport report = service.importDocuments(new DocumentBatchImportCommand(
        ORG_A,
        ACTOR_ID,
        ActorRole.CONSULTANT,
        ImportSourceType.RESUME_DOCUMENT_BATCH,
        List.of(validResume, oversizedNotes),
        NOW));

    assertThat(report.validationStatus()).isEqualTo(ImportValidationStatus.VALIDATION_FAILED);
    assertThat(report.validationReport().rowErrors())
        .extracting(error -> error.field() + ":" + error.code())
        .contains("content:document_size_exceeds_limit");
    assertThat(governedGateway.createdDocuments).isEmpty();
  }

  @Test
  void legacyAtsCrmMappingNormalizesExternalFieldsIntoDraftRecordsNotCanonicalFacts() {
    LegacyAtsCrmMapping mapping = LegacyAtsCrmMapping.candidateMapping(LegacySystemKind.CRM);

    ImportDraftRecord draft = mapping.toDraftRecord(
        ORG_A,
        Map.of(
            "external_id", "crm-77",
            "full_name", "  Chen Yu  ",
            "email", "CHEN@example.com",
            "identity_fingerprint_hash", "fingerprint:chen"),
        8);

    assertThat(draft.externalId()).isEqualTo("crm-77");
    assertThat(draft.fields()).containsEntry("full_name", "Chen Yu");
    assertThat(draft.fields()).containsEntry("email", "chen@example.com");
    assertThat(draft.fields()).containsEntry("identityFingerprintHash", "fingerprint:chen");
    assertThat(draft.fields()).doesNotContainKey("canonical_fact_status");
    assertThat(draft.informationPacketType()).isEqualTo(InformationPacketType.CANDIDATE);
    assertThat(draft.intendedEntityType()).isEqualTo(IntendedEntityType.CANDIDATE);
  }

  private static CsvImportCommand candidateCsv(String csv) {
    return CsvImportCommand.builder()
        .organizationId(ORG_A)
        .actorId(ACTOR_ID)
        .actorRole(ActorRole.CONSULTANT)
        .sourceType(ImportSourceType.CSV_CANDIDATE)
        .mapping(LegacyAtsCrmMapping.candidateMapping(LegacySystemKind.ATS))
        .csvContent(csv)
        .occurredAt(NOW)
        .build();
  }

  private static DataLifecycleEntitySnapshot candidateSnapshot(
      UUID id,
      Map<String, String> attributes) {
    return new DataLifecycleEntitySnapshot(
        ORG_A,
        DataLifecycleEntityType.CANDIDATE,
        id,
        1,
        attributes,
        List.of(),
        NOW);
  }

  private static final class RecordingGovernedImportGateway
      implements GovernedImportGateway {

    private final List<ImportDraftRecord> createdDrafts = new ArrayList<>();
    private final List<DocumentImportDraft> createdDocuments = new ArrayList<>();
    private final List<GovernedImportLineage> rolledBackLineages = new ArrayList<>();

    @Override
    public GovernedImportLineage createDraft(
        UUID batchId,
        UUID organizationId,
        UUID actorId,
        ActorRole actorRole,
        ImportDraftRecord draft) {
      createdDrafts.add(draft);
      return GovernedImportLineage.forDraft(
          batchId,
          organizationId,
          draft.recordId(),
          draft.externalId(),
          UUID.randomUUID(),
          UUID.randomUUID(),
          UUID.randomUUID(),
          true,
          false,
          false);
    }

    @Override
    public GovernedImportLineage createDocument(
        UUID batchId,
        UUID organizationId,
        UUID actorId,
        ActorRole actorRole,
        DocumentImportDraft document) {
      createdDocuments.add(document);
      return GovernedImportLineage.forDocument(
          batchId,
          organizationId,
          UUID.randomUUID(),
          document.externalId(),
          UUID.randomUUID(),
          UUID.randomUUID(),
          document.originalFilename(),
          false,
          false,
          false);
    }

    @Override
    public void rollback(UUID organizationId, UUID batchId, GovernedImportLineage lineage) {
      rolledBackLineages.add(lineage);
    }
  }

  private static final class InMemoryImportBatchRepository implements ImportBatchRepository {

    private final Map<String, ImportBatchReport> reports = new HashMap<>();

    @Override
    public ImportBatchReport save(ImportBatchReport report) {
      reports.put(key(report.organizationId(), report.batchId()), report);
      return report;
    }

    @Override
    public Optional<ImportBatchReport> find(UUID organizationId, UUID batchId) {
      return Optional.ofNullable(reports.get(key(organizationId, batchId)));
    }

    @Override
    public List<ImportBatchReport> savedReports() {
      return List.copyOf(reports.values());
    }

    private static String key(UUID organizationId, UUID batchId) {
      return organizationId + ":" + batchId;
    }
  }

  private static final class RecordingWorkflowEventPort implements WorkflowEventPort {
    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey idempotencyKey) {
      return Optional.empty();
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return new WorkflowEventAppendResult(new WorkflowEventId(UUID.randomUUID()));
    }
  }
}
