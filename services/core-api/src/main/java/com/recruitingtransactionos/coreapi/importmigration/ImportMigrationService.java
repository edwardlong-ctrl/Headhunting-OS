package com.recruitingtransactionos.coreapi.importmigration;

import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntitySnapshot;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntityType;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDecision;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDetectionCommand;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDetectionResult;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleService;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.CsvImportCommand;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.DocumentBatchImportCommand;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.DocumentImportDraft;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.GovernedImportLineage;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportBatchReport;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportDraftRecord;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportDuplicateDecision;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportEntityReference;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportEntityType;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportRollbackCommand;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportRollbackStatus;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportRowError;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportValidationStatus;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ValidationReport;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImportMigrationService {

  private final ImportBatchRepository importBatchRepository;
  private final GovernedImportGateway governedImportGateway;
  private final DataLifecycleService dataLifecycleService;

  public ImportMigrationService(
      ImportBatchRepository importBatchRepository,
      GovernedImportGateway governedImportGateway,
      DataLifecycleService dataLifecycleService) {
    this.importBatchRepository = Objects.requireNonNull(
        importBatchRepository, "importBatchRepository must not be null");
    this.governedImportGateway = Objects.requireNonNull(
        governedImportGateway, "governedImportGateway must not be null");
    this.dataLifecycleService = Objects.requireNonNull(
        dataLifecycleService, "dataLifecycleService must not be null");
  }

  public ImportBatchReport importCsv(CsvImportCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    UUID batchId = UUID.randomUUID();
    ParsedCsv parsedCsv = parseCsv(command.csvContent());
    List<ImportRowError> rowErrors = new ArrayList<>();
    List<ImportDuplicateDecision> duplicateDecisions = new ArrayList<>();
    List<ImportDraftRecord> acceptedDrafts = new ArrayList<>();

    for (ParsedCsvRow row : parsedCsv.rows()) {
      ImportDraftRecord draft;
      try {
        draft = command.mapping().toDraftRecord(
            command.organizationId(),
            row.values(),
            row.rowNumber());
      } catch (IllegalArgumentException exception) {
        rowErrors.add(new ImportRowError(
            row.rowNumber(),
            "row",
            "invalid_legacy_row",
            exception.getMessage()));
        continue;
      }
      validateRequiredFields(draft, rowErrors);
      validateSameOrganizationReferences(command, draft, rowErrors);
      if (hasRowError(rowErrors, row.rowNumber())) {
        continue;
      }
      DuplicateDetectionResult duplicateResult = dataLifecycleService.evaluateDuplicates(
          new DuplicateDetectionCommand(
              command.organizationId(),
              lifecycleSnapshot(command.organizationId(), draft, command.occurredAt()),
              command.existingSnapshots(),
              new ActorRef(command.actorId(), command.actorRole()),
              command.occurredAt(),
              "historical import duplicate screen"));
      if (duplicateResult.decision() != DuplicateDecision.NO_DUPLICATE) {
        duplicateDecisions.add(new ImportDuplicateDecision(
            row.rowNumber(),
            duplicateResult.decision(),
            duplicateResult.match().map(match -> match.matchedEntityId()),
            duplicateResult.match()
                .map(match -> match.justification())
                .orElse("duplicate decision recorded")));
      }
      if (duplicateResult.decision() == DuplicateDecision.HIGH_CONFIDENCE_BLOCK) {
        rowErrors.add(new ImportRowError(
            row.rowNumber(),
            "duplicate",
            "high_confidence_duplicate_blocked",
            "High-confidence duplicate requires review before import."));
        continue;
      }
      acceptedDrafts.add(draft);
    }

    int conflictCount = (int) duplicateDecisions.stream()
        .filter(decision -> decision.decision() == DuplicateDecision.HIGH_CONFIDENCE_BLOCK)
        .count();
    boolean validationFailed = !rowErrors.isEmpty();
    List<GovernedImportLineage> lineages = new ArrayList<>();
    if (!validationFailed) {
      for (ImportDraftRecord draft : acceptedDrafts) {
        lineages.add(governedImportGateway.createDraft(
            batchId,
            command.organizationId(),
            command.actorId(),
            command.actorRole(),
            draft));
      }
    }

    int acceptedCount = validationFailed ? 0 : acceptedDrafts.size();
    int rejectedCount = rowErrors.size();
    ValidationReport validationReport = new ValidationReport(
        acceptedCount,
        rejectedCount,
        duplicateDecisions.size(),
        conflictCount,
        acceptedCount,
        rowErrors,
        duplicateDecisions);
    ImportBatchReport report = new ImportBatchReport(
        batchId,
        command.organizationId(),
        command.actorId(),
        command.actorRole(),
        command.sourceType(),
        validationFailed
            ? ImportValidationStatus.VALIDATION_FAILED
            : ImportValidationStatus.IMPORTED_WITH_REVIEW,
        parsedCsv.rows().size(),
        0,
        acceptedCount,
        rejectedCount,
        duplicateDecisions.size(),
        conflictCount,
        acceptedCount,
        ImportRollbackStatus.NOT_REQUESTED,
        validationReport,
        lineages,
        command.occurredAt(),
        Optional.empty());
    return importBatchRepository.save(report);
  }

  public ImportBatchReport importDocuments(DocumentBatchImportCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    UUID batchId = UUID.randomUUID();
    List<GovernedImportLineage> lineages = new ArrayList<>();
    for (DocumentImportDraft document : command.documents()) {
      lineages.add(governedImportGateway.createDocument(
          batchId,
          command.organizationId(),
          command.actorId(),
          command.actorRole(),
          document));
    }
    ValidationReport validationReport = new ValidationReport(
        command.documents().size(),
        0,
        0,
        0,
        command.documents().size(),
        List.of(),
        List.of());
    ImportBatchReport report = new ImportBatchReport(
        batchId,
        command.organizationId(),
        command.actorId(),
        command.actorRole(),
        command.sourceType(),
        ImportValidationStatus.IMPORTED_WITH_REVIEW,
        0,
        command.documents().size(),
        command.documents().size(),
        0,
        0,
        0,
        command.documents().size(),
        ImportRollbackStatus.NOT_REQUESTED,
        validationReport,
        lineages,
        command.occurredAt(),
        Optional.empty());
    return importBatchRepository.save(report);
  }

  public ImportBatchReport rollback(ImportRollbackCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    ImportBatchReport report = importBatchRepository
        .find(command.organizationId(), command.batchId())
        .orElseThrow(() -> new IllegalArgumentException("import batch not found in organization"));
    for (GovernedImportLineage lineage : report.lineages()) {
      if (lineage.organizationId().equals(command.organizationId())
          && lineage.batchId().equals(command.batchId())) {
        governedImportGateway.rollback(command.organizationId(), command.batchId(), lineage);
      }
    }
    return importBatchRepository.save(report.rolledBack(command.occurredAt()));
  }

  private static void validateRequiredFields(
      ImportDraftRecord draft,
      List<ImportRowError> rowErrors) {
    requireField(draft, rowErrors, "externalId", draft.externalId());
    switch (draft.entityType()) {
      case CANDIDATE -> requireField(draft, rowErrors, "full_name", draft.fields().get("full_name"));
      case COMPANY -> requireField(draft, rowErrors, "name", draft.fields().get("name"));
      case JOB -> {
        requireField(draft, rowErrors, "title", draft.fields().get("title"));
        requireField(
            draft,
            rowErrors,
            "company_external_id",
            draft.fields().get("companyExternalId"));
      }
    }
  }

  private static void requireField(
      ImportDraftRecord draft,
      List<ImportRowError> rowErrors,
      String field,
      String value) {
    if (value == null || value.isBlank()) {
      rowErrors.add(new ImportRowError(
          draft.rowNumber(),
          field,
          "required_field_missing",
          "Required import field is missing."));
    }
  }

  private static void validateSameOrganizationReferences(
      CsvImportCommand command,
      ImportDraftRecord draft,
      List<ImportRowError> rowErrors) {
    if (draft.entityType() != ImportEntityType.JOB) {
      return;
    }
    String companyExternalId = draft.fields().get("companyExternalId");
    if (companyExternalId == null) {
      return;
    }
    ImportEntityReference reference = command.knownExternalReferences().get(companyExternalId);
    if (reference != null && !reference.organizationId().equals(command.organizationId())) {
      rowErrors.add(new ImportRowError(
          draft.rowNumber(),
          "company_external_id",
          "cross_org_reference_rejected",
          "Referenced company belongs to a different organization."));
    }
  }

  private static boolean hasRowError(List<ImportRowError> rowErrors, int rowNumber) {
    return rowErrors.stream().anyMatch(error -> error.rowNumber() == rowNumber);
  }

  private static DataLifecycleEntitySnapshot lifecycleSnapshot(
      UUID organizationId,
      ImportDraftRecord draft,
      Instant occurredAt) {
    return new DataLifecycleEntitySnapshot(
        organizationId,
        lifecycleEntityType(draft.entityType()),
        draft.recordId(),
        1,
        duplicateAttributes(draft),
        List.of(),
        occurredAt);
  }

  private static DataLifecycleEntityType lifecycleEntityType(ImportEntityType entityType) {
    return switch (entityType) {
      case CANDIDATE -> DataLifecycleEntityType.CANDIDATE;
      case COMPANY -> DataLifecycleEntityType.COMPANY;
      case JOB -> DataLifecycleEntityType.JOB;
    };
  }

  private static Map<String, String> duplicateAttributes(ImportDraftRecord draft) {
    return switch (draft.entityType()) {
      case CANDIDATE -> attributeMap(
          "identityFingerprintHash",
          draft.fields().get("identityFingerprintHash"));
      case COMPANY -> attributeMap(
          "name",
          draft.fields().get("name"),
          "website",
          draft.fields().get("website"));
      case JOB -> attributeMap(
          "companyId",
          draft.fields().get("companyExternalId"),
          "title",
          draft.fields().get("title"));
    };
  }

  private static Map<String, String> attributeMap(String key, String value) {
    return value == null ? Map.of() : Map.of(key, value);
  }

  private static Map<String, String> attributeMap(
      String keyA,
      String valueA,
      String keyB,
      String valueB) {
    Map<String, String> attributes = new LinkedHashMap<>();
    if (valueA != null) {
      attributes.put(keyA, valueA);
    }
    if (valueB != null) {
      attributes.put(keyB, valueB);
    }
    return attributes;
  }

  private static ParsedCsv parseCsv(String csvContent) {
    List<String> lines = csvContent.lines()
        .filter(line -> !line.isBlank())
        .toList();
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("csvContent must include a header row");
    }
    List<String> headers = parseCsvLine(lines.get(0)).stream()
        .map(header -> header.trim().toLowerCase(Locale.ROOT))
        .toList();
    if (headers.isEmpty() || headers.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException("csv header names must not be blank");
    }
    List<ParsedCsvRow> rows = new ArrayList<>();
    for (int index = 1; index < lines.size(); index++) {
      List<String> values = parseCsvLine(lines.get(index));
      Map<String, String> row = new LinkedHashMap<>();
      for (int headerIndex = 0; headerIndex < headers.size(); headerIndex++) {
        String value = headerIndex < values.size() ? values.get(headerIndex) : "";
        row.put(headers.get(headerIndex), value == null ? "" : value.strip());
      }
      rows.add(new ParsedCsvRow(index + 1, row));
    }
    return new ParsedCsv(rows);
  }

  private static List<String> parseCsvLine(String line) {
    List<String> values = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    for (int index = 0; index < line.length(); index++) {
      char character = line.charAt(index);
      if (character == '"') {
        if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
          current.append('"');
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (character == ',' && !quoted) {
        values.add(current.toString());
        current.setLength(0);
      } else {
        current.append(character);
      }
    }
    values.add(current.toString());
    return values;
  }

  private record ParsedCsv(List<ParsedCsvRow> rows) {

    private ParsedCsv {
      rows = List.copyOf(rows);
    }
  }

  private record ParsedCsvRow(int rowNumber, Map<String, String> values) {

    private ParsedCsvRow {
      values = Map.copyOf(values);
    }
  }
}
