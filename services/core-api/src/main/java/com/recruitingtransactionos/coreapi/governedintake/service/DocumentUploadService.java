package com.recruitingtransactionos.coreapi.governedintake.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentRetrievalResult;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreException;
import com.recruitingtransactionos.coreapi.documentstorage.VirusScanPort;
import com.recruitingtransactionos.coreapi.governedintake.AttachSourceItemToPacketCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DocumentUploadService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Map<String, Long> MAX_SIZE_BY_MIME_CATEGORY = Map.of(
      "application/pdf", 25L * 1024 * 1024,
      "application/msword", 25L * 1024 * 1024,
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 25L * 1024 * 1024,
      "image/png", 10L * 1024 * 1024,
      "image/jpeg", 10L * 1024 * 1024,
      "image/webp", 10L * 1024 * 1024,
      "text/plain", 5L * 1024 * 1024,
      "text/markdown", 5L * 1024 * 1024);

  private static final Set<String> ALLOWED_MIME_TYPES = MAX_SIZE_BY_MIME_CATEGORY.keySet();

  private final SourceItemPersistencePort sourceItemPersistencePort;
  private final GovernedIntakeService governedIntakeService;
  private final DocumentStore documentStore;
  private final VirusScanPort virusScanPort;
  private final CanonicalWriteTransactionBoundary transactionBoundary;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;

  public DocumentUploadService(
      SourceItemPersistencePort sourceItemPersistencePort,
      GovernedIntakeService governedIntakeService,
      DocumentStore documentStore,
      VirusScanPort virusScanPort) {
    this(
        sourceItemPersistencePort,
        governedIntakeService,
        documentStore,
        virusScanPort,
        CanonicalWriteTransactionBoundary.immediate(),
        noOpWorkflowTransitionAuditService());
  }

  public DocumentUploadService(
      SourceItemPersistencePort sourceItemPersistencePort,
      GovernedIntakeService governedIntakeService,
      DocumentStore documentStore,
      VirusScanPort virusScanPort,
      CanonicalWriteTransactionBoundary transactionBoundary,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this.sourceItemPersistencePort = Objects.requireNonNull(sourceItemPersistencePort,
        "sourceItemPersistencePort must not be null");
    this.governedIntakeService = Objects.requireNonNull(governedIntakeService,
        "governedIntakeService must not be null");
    this.documentStore = Objects.requireNonNull(documentStore, "documentStore must not be null");
    this.virusScanPort = Objects.requireNonNull(virusScanPort, "virusScanPort must not be null");
    this.transactionBoundary = Objects.requireNonNull(
        transactionBoundary, "transactionBoundary must not be null");
    this.workflowTransitionAuditService = Objects.requireNonNull(
        workflowTransitionAuditService, "workflowTransitionAuditService must not be null");
  }

  public DocumentUploadResult upload(DocumentUploadCommand command, InputStream content) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(content, "content must not be null");

    validateMimeType(command.mimeType());
    validateFileSize(command.mimeType(), command.contentLength());

    byte[] fileBytes;
    try {
      fileBytes = content.readAllBytes();
    } catch (Exception e) {
      throw new DocumentUploadException("Failed to read upload content", e);
    }

    String contentHash = computeSha256(fileBytes);
    String scanStatus = scanStatusFor(fileBytes);

    SourceItemId sourceItemId = new SourceItemId(UUID.randomUUID());
    StorageAllocation allocation = allocateStorage(command, fileBytes, sourceItemId, contentHash);

    try {
      return transactionBoundary.run(() -> {
        SourceItemRegistrationCommand registrationCommand = SourceItemRegistrationCommand.builder()
            .organizationId(command.organizationId())
            .sourceType(command.sourceType())
            .origin(command.origin())
            .title(command.title())
            .contentHash(contentHash)
            .storageRef(allocation.storageRef())
            .uploadedByActorType(command.uploadedByActorType())
            .uploadedByActorId(command.uploadedByActorId())
            .receivedAt(Instant.now())
            .metadataJson("{}")
            .status(SourceItemStatus.REGISTERED)
            .mimeType(command.mimeType())
            .fileSizeBytes(command.contentLength())
            .originalFilename(command.originalFilename())
            .scanStatus(scanStatus)
            .sourceItemId(sourceItemId)
            .build();

        SourceItem sourceItem = governedIntakeService.registerSourceItem(registrationCommand);

        var packet = governedIntakeService.createInformationPacket(
            new InformationPacketCreateCommand(
                command.organizationId(),
                InformationPacketType.CANDIDATE,
                IntendedEntityType.CANDIDATE,
                null,
                command.uploadedByActorType(),
                command.uploadedByActorId(),
                InformationPacketStatus.CREATED,
                null,
                "{}"));

        governedIntakeService.attachSourceItemToPacket(
            new AttachSourceItemToPacketCommand(
                command.organizationId(),
                packet.informationPacketId(),
                sourceItem.sourceItemId()));

        recordSourceItemRegistered(command, sourceItem, contentHash, scanStatus);
        recordInformationPacketCreated(command, packet, sourceItem, scanStatus);

        return new DocumentUploadResult(
            sourceItem.sourceItemId(),
            packet.informationPacketId().value(),
            contentHash,
            allocation.storageRef(),
            scanStatus);
      });
    } catch (RuntimeException exception) {
      try {
        allocation.cleanupIfNew(documentStore);
      } catch (RuntimeException cleanupException) {
        exception.addSuppressed(cleanupException);
      }
      throw exception;
    }
  }

  public DocumentRetrievalResult retrieveDocument(UUID organizationId, UUID sourceItemIdValue) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceItemIdValue, "sourceItemIdValue must not be null");

    SourceItemId sourceItemId = new SourceItemId(sourceItemIdValue);
    SourceItem sourceItem = governedIntakeService.findSourceItem(organizationId, sourceItemId)
        .orElseThrow(() -> new DocumentUploadException("Document not found"));

    if (!sourceItem.organizationId().equals(organizationId)) {
      throw new DocumentUploadException("Document not found in organization");
    }

    String storageRef = sourceItem.storageRef();
    if (storageRef == null) {
      throw new DocumentUploadException("Document has no storage reference");
    }
    DocumentStoreKey storageKey;
    try {
      storageKey = DocumentStoreKey.fromStorageRef(storageRef);
    } catch (IllegalArgumentException exception) {
      throw new DocumentUploadException("Document has invalid storage reference", exception);
    }

    try {
      return new DocumentRetrievalResult(
          documentStore.retrieve(storageKey),
          sourceItem.mimeType(),
          sourceItem.originalFilename());
    } catch (DocumentStoreException e) {
      throw new DocumentUploadException("Document not found in storage", e);
    }
  }

  private String scanStatusFor(byte[] fileBytes) {
    VirusScanPort.ScanResult scanResult = virusScanPort.scan(new ByteArrayInputStream(fileBytes));
    return switch (scanResult) {
      case CLEAN -> "clean";
      case INFECTED -> throw new DocumentUploadException("Uploaded file failed virus scan");
      case ERROR -> throw new DocumentUploadException("Virus scan failed");
    };
  }

  private void recordSourceItemRegistered(
      DocumentUploadCommand command,
      SourceItem sourceItem,
      String contentHash,
      String scanStatus) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(command.organizationId())
        .entityNamespace("intake")
        .entityType(WorkflowEntityType.SOURCE_ITEM.wireValue())
        .entityId(sourceItem.sourceItemId().value())
        .actionCode(WorkflowActionCode.SOURCE_ITEM_REGISTERED.wireValue())
        .actorType(command.uploadedByActorType())
        .actorId(requireActorId(command))
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState("{\"status\":\"absent\"}")
        .afterState(jsonState(Map.of(
            "status", sourceItem.status().wireValue(),
            "sourceType", sourceItem.sourceType().wireValue(),
            "origin", sourceItem.origin().wireValue(),
            "contentHash", contentHash,
            "scanStatus", scanStatus,
            "storageRef", sourceItem.storageRef())))
        .reason("Document upload registered a governed source item.")
        .idempotencyKey("source-item-register-" + sourceItem.sourceItemId().value())
        .sourceType("document_upload")
        .sourceRefId(sourceItem.sourceItemId().value())
        .occurredAt(sourceItem.createdAt())
        .build());
  }

  private void recordInformationPacketCreated(
      DocumentUploadCommand command,
      InformationPacket packet,
      SourceItem sourceItem,
      String scanStatus) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(command.organizationId())
        .entityNamespace("intake")
        .entityType(WorkflowEntityType.INFORMATION_PACKET.wireValue())
        .entityId(packet.informationPacketId().value())
        .actionCode(WorkflowActionCode.INFORMATION_PACKET_CREATED.wireValue())
        .actorType(command.uploadedByActorType())
        .actorId(requireActorId(command))
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState("{\"status\":\"absent\"}")
        .afterState(jsonState(Map.of(
            "status", packet.processingStatus().wireValue(),
            "packetType", packet.packetType().wireValue(),
            "intendedEntityType", packet.intendedEntityType().wireValue(),
            "attachedSourceItemId", sourceItem.sourceItemId().value().toString(),
            "scanStatus", scanStatus)))
        .reason("Document upload created an information packet for source evidence.")
        .idempotencyKey("information-packet-create-" + packet.informationPacketId().value())
        .sourceType("document_upload")
        .sourceRefId(packet.informationPacketId().value())
        .occurredAt(packet.createdAt())
        .build());
  }

  private static UUID requireActorId(DocumentUploadCommand command) {
    if (command.uploadedByActorId() == null) {
      throw new DocumentUploadException("Document upload requires an authenticated actor id");
    }
    return command.uploadedByActorId();
  }

  private static String jsonState(Map<String, Object> state) {
    try {
      return OBJECT_MAPPER.writeValueAsString(state);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize workflow state snapshot", exception);
    }
  }

  private static WorkflowTransitionAuditService noOpWorkflowTransitionAuditService() {
    return new WorkflowTransitionAuditService(new WorkflowEventService(new WorkflowEventPort() {
      @Override
      public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
          UUID organizationId,
          com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey idempotencyKey) {
        return Optional.empty();
      }

      @Override
      public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
        return new WorkflowEventAppendResult(new WorkflowEventId(UUID.randomUUID()));
      }
    }), new com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort() {
      @Override
      public Optional<String> getCurrentStateJson(UUID orgId, String ns, String type, UUID id) {
        return Optional.empty();
      }
      @Override
      public void updateStateJson(UUID orgId, String ns, String type, UUID id, String state) {}
    });
  }

  private static void validateMimeType(String mimeType) {
    if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
      throw new DocumentUploadException("Unsupported MIME type: " + mimeType);
    }
  }

  private static void validateFileSize(String mimeType, long contentLength) {
    Long maxSize = MAX_SIZE_BY_MIME_CATEGORY.get(mimeType);
    if (maxSize != null && contentLength > maxSize) {
      throw new DocumentUploadException(
          "File size exceeds maximum for " + mimeType + ": "
              + contentLength + " > " + maxSize);
    }
  }

  private static String computeSha256(byte[] content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content);
      return "sha256:" + HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new DocumentUploadException("SHA-256 algorithm not available", e);
    }
  }

  private StorageAllocation allocateStorage(
      DocumentUploadCommand command,
      byte[] fileBytes,
      SourceItemId sourceItemId,
      String contentHash) {
    Optional<SourceItem> existing = sourceItemPersistencePort.findByContentHash(
        command.organizationId(), contentHash);
    if (existing.isPresent()) {
      SourceItem existingItem = existing.get();
      String existingStorageRef = existingItem.storageRef();
      if (existingStorageRef != null) {
        try {
          DocumentStoreKey existingKey = DocumentStoreKey.fromStorageRef(existingStorageRef);
          if (documentStore.exists(existingKey)) {
            return StorageAllocation.reused(existingStorageRef);
          }
        } catch (IllegalArgumentException ignored) {
          // Fall through to allocate a new storage location when legacy refs are malformed.
        }
      }
    }

    DocumentStoreKey storageKey = new DocumentStoreKey(
        command.organizationId(),
        sourceItemId.value(),
        contentHash.length() >= 16 ? contentHash.substring(0, 16) : contentHash,
        command.originalFilename());
    documentStore.store(storageKey, new ByteArrayInputStream(fileBytes), command.contentLength());
    return StorageAllocation.created(storageKey);
  }

  private record StorageAllocation(DocumentStoreKey key, String storageRef, boolean newlyStored) {

    private static StorageAllocation created(DocumentStoreKey key) {
      return new StorageAllocation(key, key.storagePath(), true);
    }

    private static StorageAllocation reused(String storageRef) {
      return new StorageAllocation(null, storageRef, false);
    }

    private void cleanupIfNew(DocumentStore documentStore) {
      if (newlyStored && key != null) {
        documentStore.delete(key);
      }
    }
  }
}
