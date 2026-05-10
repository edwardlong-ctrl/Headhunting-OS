package com.recruitingtransactionos.coreapi.importmigration;

import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DataLifecycleEntitySnapshot;
import com.recruitingtransactionos.coreapi.datalifecycle.DataLifecycleModels.DuplicateDecision;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImportMigrationModels {

  private ImportMigrationModels() {
  }

  public enum ImportSourceType {
    CSV_CANDIDATE,
    CSV_COMPANY,
    CSV_JOB,
    LEGACY_ATS,
    LEGACY_CRM,
    RESUME_DOCUMENT_BATCH
  }

  public enum LegacySystemKind {
    ATS,
    CRM
  }

  public enum ImportEntityType {
    CANDIDATE,
    COMPANY,
    JOB
  }

  public enum ImportValidationStatus {
    VALIDATION_FAILED,
    IMPORTED_WITH_REVIEW,
    ROLLED_BACK
  }

  public enum ImportRollbackStatus {
    NOT_REQUESTED,
    COMPLETED,
    REJECTED
  }

  public record ImportEntityReference(
      UUID organizationId,
      UUID entityId,
      ImportEntityType entityType) {

    public ImportEntityReference {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(entityId, "entityId must not be null");
      Objects.requireNonNull(entityType, "entityType must not be null");
    }
  }

  public record ImportRowError(
      int rowNumber,
      String field,
      String code,
      String message) {

    public ImportRowError {
      if (rowNumber <= 1) {
        throw new IllegalArgumentException("rowNumber must refer to a data row");
      }
      field = requireNonBlank(field, "field");
      code = requireNonBlank(code, "code");
      message = requireNonBlank(message, "message");
    }
  }

  public record ImportDuplicateDecision(
      int rowNumber,
      DuplicateDecision decision,
      Optional<UUID> matchedEntityId,
      String justification) {

    public ImportDuplicateDecision {
      if (rowNumber <= 1) {
        throw new IllegalArgumentException("rowNumber must refer to a data row");
      }
      Objects.requireNonNull(decision, "decision must not be null");
      matchedEntityId = matchedEntityId == null ? Optional.empty() : matchedEntityId;
      justification = requireNonBlank(justification, "justification");
    }
  }

  public record ValidationReport(
      int accepted,
      int rejected,
      int duplicate,
      int conflict,
      int pendingReview,
      List<ImportRowError> rowErrors,
      List<ImportDuplicateDecision> duplicateDecisions) {

    public ValidationReport {
      if (accepted < 0 || rejected < 0 || duplicate < 0 || conflict < 0 || pendingReview < 0) {
        throw new IllegalArgumentException("validation counts must not be negative");
      }
      rowErrors = List.copyOf(Objects.requireNonNull(rowErrors, "rowErrors must not be null"));
      duplicateDecisions = List.copyOf(Objects.requireNonNull(
          duplicateDecisions, "duplicateDecisions must not be null"));
    }

    public static ValidationReport empty() {
      return new ValidationReport(0, 0, 0, 0, 0, List.of(), List.of());
    }
  }

  public record ImportDraftRecord(
      UUID recordId,
      UUID organizationId,
      int rowNumber,
      ImportEntityType entityType,
      String externalId,
      LegacySystemKind legacySystemKind,
      Map<String, String> fields,
      Map<String, String> externalFields,
      SourceItemType sourceItemType,
      InformationPacketType informationPacketType,
      IntendedEntityType intendedEntityType) {

    public ImportDraftRecord {
      Objects.requireNonNull(recordId, "recordId must not be null");
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      if (rowNumber <= 1) {
        throw new IllegalArgumentException("rowNumber must refer to a data row");
      }
      Objects.requireNonNull(entityType, "entityType must not be null");
      externalId = requireNonBlank(externalId, "externalId");
      Objects.requireNonNull(legacySystemKind, "legacySystemKind must not be null");
      fields = Map.copyOf(Objects.requireNonNull(fields, "fields must not be null"));
      externalFields = Map.copyOf(Objects.requireNonNull(
          externalFields, "externalFields must not be null"));
      Objects.requireNonNull(sourceItemType, "sourceItemType must not be null");
      Objects.requireNonNull(informationPacketType, "informationPacketType must not be null");
      Objects.requireNonNull(intendedEntityType, "intendedEntityType must not be null");
    }
  }

  public record DocumentImportDraft(
      String externalId,
      String originalFilename,
      String mimeType,
      byte[] content,
      ImportEntityType entityType,
      Map<String, String> metadata) {

    public DocumentImportDraft {
      externalId = requireNonBlank(externalId, "externalId");
      originalFilename = requireNonBlank(originalFilename, "originalFilename");
      mimeType = requireNonBlank(mimeType, "mimeType");
      content = Objects.requireNonNull(content, "content must not be null").clone();
      if (content.length == 0) {
        throw new IllegalArgumentException("content must not be empty");
      }
      Objects.requireNonNull(entityType, "entityType must not be null");
      metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
    }

    @Override
    public byte[] content() {
      return content.clone();
    }
  }

  public record GovernedImportLineage(
      UUID batchId,
      UUID organizationId,
      UUID importRecordId,
      Optional<String> importExternalId,
      UUID sourceItemId,
      UUID informationPacketId,
      UUID extractionRunId,
      Optional<String> documentOriginalFilename,
      boolean claimLedgerPathUsed,
      boolean canonicalMutationPerformed,
      boolean confirmedFactWritePerformed) {

    public GovernedImportLineage {
      Objects.requireNonNull(batchId, "batchId must not be null");
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(importRecordId, "importRecordId must not be null");
      importExternalId = importExternalId == null
          ? Optional.empty()
          : importExternalId.map(externalId -> requireNonBlank(externalId, "importExternalId"));
      Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
      Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
      documentOriginalFilename = documentOriginalFilename == null
          ? Optional.empty()
          : documentOriginalFilename.map(filename -> requireNonBlank(
              filename, "documentOriginalFilename"));
      if (canonicalMutationPerformed || confirmedFactWritePerformed) {
        throw new IllegalArgumentException(
            "import/migration lineage must not represent confirmed canonical writes");
      }
    }

    public static GovernedImportLineage forDraft(
        UUID batchId,
        UUID organizationId,
        UUID importRecordId,
        String importExternalId,
        UUID sourceItemId,
        UUID informationPacketId,
        UUID extractionRunId,
        boolean claimLedgerPathUsed,
        boolean canonicalMutationPerformed,
        boolean confirmedFactWritePerformed) {
      return new GovernedImportLineage(
          batchId,
          organizationId,
          importRecordId,
          Optional.of(importExternalId),
          sourceItemId,
          informationPacketId,
          extractionRunId,
          Optional.empty(),
          claimLedgerPathUsed,
          canonicalMutationPerformed,
          confirmedFactWritePerformed);
    }

    public static GovernedImportLineage forDocument(
        UUID batchId,
        UUID organizationId,
        UUID importRecordId,
        String importExternalId,
        UUID sourceItemId,
        UUID informationPacketId,
        String originalFilename,
        boolean claimLedgerPathUsed,
        boolean canonicalMutationPerformed,
        boolean confirmedFactWritePerformed) {
      return new GovernedImportLineage(
          batchId,
          organizationId,
          importRecordId,
          Optional.of(importExternalId),
          sourceItemId,
          informationPacketId,
          null,
          Optional.of(originalFilename),
          claimLedgerPathUsed,
          canonicalMutationPerformed,
          confirmedFactWritePerformed);
    }
  }

  public record ImportBatchReport(
      UUID batchId,
      UUID organizationId,
      UUID actorId,
      ActorRole actorRole,
      ImportSourceType sourceType,
      ImportValidationStatus validationStatus,
      int rowCount,
      int documentCount,
      int acceptedCount,
      int rejectedCount,
      int duplicateCount,
      int conflictCount,
      int pendingReviewCount,
      ImportRollbackStatus rollbackStatus,
      ValidationReport validationReport,
      List<GovernedImportLineage> lineages,
      Instant createdAt,
      Optional<Instant> rolledBackAt) {

    public ImportBatchReport {
      Objects.requireNonNull(batchId, "batchId must not be null");
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(actorId, "actorId must not be null");
      Objects.requireNonNull(actorRole, "actorRole must not be null");
      Objects.requireNonNull(sourceType, "sourceType must not be null");
      Objects.requireNonNull(validationStatus, "validationStatus must not be null");
      if (rowCount < 0 || documentCount < 0 || acceptedCount < 0 || rejectedCount < 0
          || duplicateCount < 0 || conflictCount < 0 || pendingReviewCount < 0) {
        throw new IllegalArgumentException("import batch counts must not be negative");
      }
      Objects.requireNonNull(rollbackStatus, "rollbackStatus must not be null");
      Objects.requireNonNull(validationReport, "validationReport must not be null");
      lineages = List.copyOf(Objects.requireNonNull(lineages, "lineages must not be null"));
      Objects.requireNonNull(createdAt, "createdAt must not be null");
      rolledBackAt = rolledBackAt == null ? Optional.empty() : rolledBackAt;
    }

    public ImportBatchReport rolledBack(Instant occurredAt) {
      return new ImportBatchReport(
          batchId,
          organizationId,
          actorId,
          actorRole,
          sourceType,
          ImportValidationStatus.ROLLED_BACK,
          rowCount,
          documentCount,
          acceptedCount,
          rejectedCount,
          duplicateCount,
          conflictCount,
          pendingReviewCount,
          ImportRollbackStatus.COMPLETED,
          validationReport,
          lineages,
          createdAt,
          Optional.of(occurredAt));
    }
  }

  public record ImportRollbackCommand(
      UUID organizationId,
      UUID batchId,
      UUID actorId,
      ActorRole actorRole,
      String reason,
      Instant occurredAt) {

    public ImportRollbackCommand {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(batchId, "batchId must not be null");
      Objects.requireNonNull(actorId, "actorId must not be null");
      Objects.requireNonNull(actorRole, "actorRole must not be null");
      reason = requireNonBlank(reason, "reason");
      Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
  }

  public record CsvImportCommand(
      UUID organizationId,
      UUID actorId,
      ActorRole actorRole,
      ImportSourceType sourceType,
      LegacyAtsCrmMapping mapping,
      String csvContent,
      List<DataLifecycleEntitySnapshot> existingSnapshots,
      Map<String, ImportEntityReference> knownExternalReferences,
      Instant occurredAt) {

    public CsvImportCommand {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(actorId, "actorId must not be null");
      Objects.requireNonNull(actorRole, "actorRole must not be null");
      Objects.requireNonNull(sourceType, "sourceType must not be null");
      Objects.requireNonNull(mapping, "mapping must not be null");
      csvContent = requireNonBlank(csvContent, "csvContent");
      existingSnapshots = existingSnapshots == null ? List.of() : List.copyOf(existingSnapshots);
      knownExternalReferences = knownExternalReferences == null
          ? Map.of()
          : Map.copyOf(knownExternalReferences);
      Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private UUID organizationId;
      private UUID actorId;
      private ActorRole actorRole;
      private ImportSourceType sourceType;
      private LegacyAtsCrmMapping mapping;
      private String csvContent;
      private List<DataLifecycleEntitySnapshot> existingSnapshots = List.of();
      private Map<String, ImportEntityReference> knownExternalReferences = Map.of();
      private Instant occurredAt;

      private Builder() {
      }

      public Builder organizationId(UUID organizationId) {
        this.organizationId = organizationId;
        return this;
      }

      public Builder actorId(UUID actorId) {
        this.actorId = actorId;
        return this;
      }

      public Builder actorRole(ActorRole actorRole) {
        this.actorRole = actorRole;
        return this;
      }

      public Builder sourceType(ImportSourceType sourceType) {
        this.sourceType = sourceType;
        return this;
      }

      public Builder mapping(LegacyAtsCrmMapping mapping) {
        this.mapping = mapping;
        return this;
      }

      public Builder csvContent(String csvContent) {
        this.csvContent = csvContent;
        return this;
      }

      public Builder existingSnapshots(List<DataLifecycleEntitySnapshot> existingSnapshots) {
        this.existingSnapshots = existingSnapshots;
        return this;
      }

      public Builder knownExternalReferences(
          Map<String, ImportEntityReference> knownExternalReferences) {
        this.knownExternalReferences = knownExternalReferences;
        return this;
      }

      public Builder occurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
        return this;
      }

      public CsvImportCommand build() {
        return new CsvImportCommand(
            organizationId,
            actorId,
            actorRole,
            sourceType,
            mapping,
            csvContent,
            existingSnapshots,
            knownExternalReferences,
            occurredAt);
      }
    }
  }

  public record DocumentBatchImportCommand(
      UUID organizationId,
      UUID actorId,
      ActorRole actorRole,
      ImportSourceType sourceType,
      List<DocumentImportDraft> documents,
      Instant occurredAt) {

    public DocumentBatchImportCommand {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(actorId, "actorId must not be null");
      Objects.requireNonNull(actorRole, "actorRole must not be null");
      Objects.requireNonNull(sourceType, "sourceType must not be null");
      documents = List.copyOf(Objects.requireNonNull(documents, "documents must not be null"));
      Objects.requireNonNull(occurredAt, "occurredAt must not be null");
      if (documents.isEmpty()) {
        throw new IllegalArgumentException("documents must not be empty");
      }
    }
  }

  public record LegacyAtsCrmMapping(
      LegacySystemKind systemKind,
      ImportEntityType entityType,
      Map<String, String> fieldMap) {

    public LegacyAtsCrmMapping {
      Objects.requireNonNull(systemKind, "systemKind must not be null");
      Objects.requireNonNull(entityType, "entityType must not be null");
      fieldMap = Map.copyOf(Objects.requireNonNull(fieldMap, "fieldMap must not be null"));
      if (!fieldMap.containsKey("external_id")) {
        throw new IllegalArgumentException("fieldMap must include external_id");
      }
    }

    public static LegacyAtsCrmMapping candidateMapping(LegacySystemKind systemKind) {
      return new LegacyAtsCrmMapping(systemKind, ImportEntityType.CANDIDATE, Map.of(
          "external_id", "externalId",
          "full_name", "full_name",
          "email", "email",
          "identity_fingerprint_hash", "identityFingerprintHash"));
    }

    public static LegacyAtsCrmMapping companyMapping(LegacySystemKind systemKind) {
      return new LegacyAtsCrmMapping(systemKind, ImportEntityType.COMPANY, Map.of(
          "external_id", "externalId",
          "name", "name",
          "website", "website"));
    }

    public static LegacyAtsCrmMapping jobMapping(LegacySystemKind systemKind) {
      return new LegacyAtsCrmMapping(systemKind, ImportEntityType.JOB, Map.of(
          "external_id", "externalId",
          "title", "title",
          "company_external_id", "companyExternalId"));
    }

    public ImportDraftRecord toDraftRecord(
        UUID organizationId,
        Map<String, String> externalRow,
        int rowNumber) {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      Objects.requireNonNull(externalRow, "externalRow must not be null");
      Map<String, String> normalizedExternal = normalizeKeys(externalRow);
      String externalId = normalizedExternal.get("external_id");
      Map<String, String> fields = new LinkedHashMap<>();
      for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
        String sourceField = entry.getKey();
        String targetField = entry.getValue();
        if ("externalId".equals(targetField)) {
          continue;
        }
        String value = normalizeValue(targetField, normalizedExternal.get(sourceField));
        if (value != null) {
          fields.put(targetField, value);
        }
      }
      return new ImportDraftRecord(
          UUID.randomUUID(),
          organizationId,
          rowNumber,
          entityType,
          normalizeValue("externalId", externalId),
          systemKind,
          fields,
          normalizedExternal,
          SourceItemType.OLD_SYSTEM_EXPORT,
          packetType(entityType),
          intendedEntityType(entityType));
    }

    private static Map<String, String> normalizeKeys(Map<String, String> row) {
      Map<String, String> normalized = new LinkedHashMap<>();
      row.forEach((key, value) -> normalized.put(
          key.trim().toLowerCase(Locale.ROOT),
          value == null ? null : value.strip()));
      return normalized;
    }

    private static String normalizeValue(String targetField, String value) {
      if (value == null || value.isBlank()) {
        return null;
      }
      String stripped = value.strip();
      if ("email".equals(targetField) || "website".equals(targetField)) {
        return stripped.toLowerCase(Locale.ROOT);
      }
      return stripped;
    }
  }

  static InformationPacketType packetType(ImportEntityType entityType) {
    return switch (entityType) {
      case CANDIDATE -> InformationPacketType.CANDIDATE;
      case COMPANY -> InformationPacketType.COMPANY;
      case JOB -> InformationPacketType.JOB;
    };
  }

  static IntendedEntityType intendedEntityType(ImportEntityType entityType) {
    return switch (entityType) {
      case CANDIDATE -> IntendedEntityType.CANDIDATE;
      case COMPANY -> IntendedEntityType.COMPANY;
      case JOB -> IntendedEntityType.JOB;
    };
  }

  static String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
