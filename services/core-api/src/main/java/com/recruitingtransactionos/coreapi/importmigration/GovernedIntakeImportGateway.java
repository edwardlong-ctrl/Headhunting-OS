package com.recruitingtransactionos.coreapi.importmigration;

import com.recruitingtransactionos.coreapi.governedintake.AttachSourceItemToPacketCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedField;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedFieldStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionFinding;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionSourceSnapshot;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeClaimLedgerBridgeService;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.DocumentImportDraft;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.GovernedImportLineage;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportDraftRecord;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GovernedIntakeImportGateway implements GovernedImportGateway {

  private static final String INPUT_SCHEMA_VERSION = "legacy-import-draft.v1";
  private static final String OUTPUT_SCHEMA_VERSION = "legacy-import-claims.v1";
  private static final String EXTRACTOR_VERSION = "task-55-legacy-import-mapper.v1";

  private final GovernedIntakeService governedIntakeService;
  private final IntakeExtractionRunPort intakeExtractionRunPort;
  private final IntakeClaimLedgerBridgeService claimLedgerBridgeService;
  private final DocumentUploadService documentUploadService;
  private final Clock clock;

  public GovernedIntakeImportGateway(
      GovernedIntakeService governedIntakeService,
      IntakeExtractionRunPort intakeExtractionRunPort,
      IntakeClaimLedgerBridgeService claimLedgerBridgeService,
      DocumentUploadService documentUploadService) {
    this(
        governedIntakeService,
        intakeExtractionRunPort,
        claimLedgerBridgeService,
        documentUploadService,
        Clock.systemUTC());
  }

  public GovernedIntakeImportGateway(
      GovernedIntakeService governedIntakeService,
      IntakeExtractionRunPort intakeExtractionRunPort,
      IntakeClaimLedgerBridgeService claimLedgerBridgeService,
      DocumentUploadService documentUploadService,
      Clock clock) {
    this.governedIntakeService = Objects.requireNonNull(
        governedIntakeService, "governedIntakeService must not be null");
    this.intakeExtractionRunPort = Objects.requireNonNull(
        intakeExtractionRunPort, "intakeExtractionRunPort must not be null");
    this.claimLedgerBridgeService = Objects.requireNonNull(
        claimLedgerBridgeService, "claimLedgerBridgeService must not be null");
    this.documentUploadService = Objects.requireNonNull(
        documentUploadService, "documentUploadService must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public GovernedImportLineage createDraft(
      UUID batchId,
      UUID organizationId,
      UUID actorId,
      ActorRole actorRole,
      ImportDraftRecord draft) {
    Objects.requireNonNull(draft, "draft must not be null");
    SourceItem sourceItem = governedIntakeService.registerSourceItem(
        SourceItemRegistrationCommand.builder()
            .organizationId(organizationId)
            .sourceType(SourceItemType.OLD_SYSTEM_EXPORT)
            .origin(SourceItemOrigin.ADMIN_IMPORT)
            .title("Legacy " + draft.entityType().name().toLowerCase() + " import "
                + draft.externalId())
            .contentHash(contentHash(draft.fields().toString() + draft.externalFields().toString()))
            .externalRef(draft.legacySystemKind().name().toLowerCase() + ":" + draft.externalId())
            .rawRef("import-batch:" + batchId + ":row:" + draft.rowNumber())
            .language("und")
            .uploadedByActorType(actorRole)
            .uploadedByActorId(actorId)
            .receivedAt(clock.instant())
            .metadataJson(metadataJson(batchId, draft))
            .status(SourceItemStatus.REGISTERED)
            .sourceItemId(new SourceItemId(UUID.randomUUID()))
            .build());
    InformationPacket packet = governedIntakeService.createInformationPacket(
        InformationPacketCreateCommand.builder()
            .organizationId(organizationId)
            .packetType(draft.informationPacketType())
            .intendedEntityType(draft.intendedEntityType())
            .createdByActorType(actorRole)
            .createdByActorId(actorId)
            .processingStatus(InformationPacketStatus.CREATED)
            .notes("Task 55 legacy import draft; review required before canonical write.")
            .metadataJson(metadataJson(batchId, draft))
            .build());
    governedIntakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        organizationId,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    IntakeExtractionRun extractionRun = saveExtractionRun(
        organizationId,
        batchId,
        draft,
        sourceItem,
        packet);
    IntakeClaimLedgerBridgeResult bridgeResult = claimLedgerBridgeService.bridge(
        new IntakeClaimLedgerBridgeRequest(
            organizationId,
            extractionRun.extractionRunId(),
            actorRole,
            actorId,
            IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
            batchId));
    boolean claimLedgerPathUsed = !bridgeResult.appendedClaimIds().isEmpty()
        || !bridgeResult.existingClaimIds().isEmpty();
    return GovernedImportLineage.forDraft(
        batchId,
        organizationId,
        draft.recordId(),
        draft.externalId(),
        sourceItem.sourceItemId().value(),
        packet.informationPacketId().value(),
        extractionRun.extractionRunId().value(),
        claimLedgerPathUsed,
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
    Objects.requireNonNull(document, "document must not be null");
    DocumentUploadResult result = documentUploadService.upload(
        new DocumentUploadCommand(
            organizationId,
            sourceItemType(document.entityType()),
            SourceItemOrigin.ADMIN_IMPORT,
            ImportMigrationModels.packetType(document.entityType()),
            ImportMigrationModels.intendedEntityType(document.entityType()),
            document.originalFilename(),
            actorRole,
            actorId,
            document.originalFilename(),
            document.mimeType(),
            document.content().length),
        new ByteArrayInputStream(document.content()));
    return GovernedImportLineage.forDocument(
        batchId,
        organizationId,
        UUID.randomUUID(),
        document.externalId(),
        result.sourceItemId().value(),
        result.informationPacketId(),
        document.originalFilename(),
        false,
        false,
        false);
  }

  @Override
  public void rollback(UUID organizationId, UUID batchId, GovernedImportLineage lineage) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(batchId, "batchId must not be null");
    Objects.requireNonNull(lineage, "lineage must not be null");
    if (!lineage.organizationId().equals(organizationId) || !lineage.batchId().equals(batchId)) {
      throw new IllegalArgumentException("rollback lineage must belong to batch and organization");
    }
    // Existing governed intake rows are append-only; Task 55 rollback is recorded at batch scope.
  }

  private IntakeExtractionRun saveExtractionRun(
      UUID organizationId,
      UUID batchId,
      ImportDraftRecord draft,
      SourceItem sourceItem,
      InformationPacket packet) {
    IntakeExtractionRunId extractionRunId = new IntakeExtractionRunId(UUID.randomUUID());
    Instant now = clock.instant();
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        extractionRunId,
        organizationId,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        OUTPUT_SCHEMA_VERSION,
        List.of(sourceItem.sourceItemId()),
        List.of(),
        List.of(new IntakeExtractionSourceSnapshot(
            sourceItem.sourceItemId(),
            sourceItem.sourceType(),
            sourceItem.title(),
            sourceItem.contentHash(),
            sourceItem.externalRef())),
        extractedFields(sourceItem.sourceItemId(), draft),
        List.of(),
        List.of(new IntakeExtractionFinding(
            "LEGACY_IMPORT_REVIEW_REQUIRED",
            "Legacy import values are claim candidates and require governed review.",
            sourceItem.sourceItemId())),
        List.of(),
        now);
    return intakeExtractionRunPort.save(new IntakeExtractionRun(
        extractionRunId,
        organizationId,
        packet.informationPacketId(),
        IntakeExtractionMode.GOVERNED_AI_V1,
        IntakeExtractionStatus.SUCCEEDED,
        INPUT_SCHEMA_VERSION,
        OUTPUT_SCHEMA_VERSION,
        EXTRACTOR_VERSION,
        contentHash(batchId + ":" + draft.recordId() + ":" + draft.fields()),
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(envelope)));
  }

  private static List<IntakeExtractedField> extractedFields(
      SourceItemId sourceItemId,
      ImportDraftRecord draft) {
    return draft.fields().entrySet().stream()
        .map(entry -> new IntakeExtractedField(
            "intake.bridge_eligible." + draft.entityType().name().toLowerCase() + "."
                + entry.getKey(),
            entry.getValue(),
            sourceItemId,
            0.95d,
            IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
            "Mapped from legacy " + draft.legacySystemKind().name()
                + " import; not a confirmed canonical fact.",
            draft.externalId() + ":" + entry.getKey()))
        .toList();
  }

  private static SourceItemType sourceItemType(ImportEntityType entityType) {
    return switch (entityType) {
      case CANDIDATE -> SourceItemType.CV;
      case COMPANY -> SourceItemType.COMPANY_MATERIAL;
      case JOB -> SourceItemType.JD;
    };
  }

  private static String metadataJson(UUID batchId, ImportDraftRecord draft) {
    return "{\"task\":\"55\",\"importBatchId\":\"" + batchId
        + "\",\"externalId\":\"" + jsonEscape(draft.externalId())
        + "\",\"legacySystem\":\"" + draft.legacySystemKind().name().toLowerCase() + "\"}";
  }

  private static String jsonEscape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String contentHash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return "sha256:" + HexFormat.of().formatHex(
          digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm not available", exception);
    }
  }
}
