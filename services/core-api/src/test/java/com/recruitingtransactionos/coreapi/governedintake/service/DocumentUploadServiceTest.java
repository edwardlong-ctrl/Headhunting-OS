package com.recruitingtransactionos.coreapi.governedintake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.documentstorage.InMemoryDocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.NoOpVirusScanPort;
import com.recruitingtransactionos.coreapi.documentstorage.VirusScanPort;
import com.recruitingtransactionos.coreapi.governedintake.AttachSourceItemToPacketCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentUploadServiceTest {

  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-000000200001");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-000000200002");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000200003");

  private static final byte[] FILE_CONTENT = "Jane Candidate CV content".getBytes(StandardCharsets.UTF_8);

  private InMemorySourceItemPort sourceItemPort;
  private InMemoryInformationPacketPort packetPort;
  private GovernedIntakeService governedIntakeService;
  private InMemoryDocumentStore documentStore;
  private NoOpVirusScanPort virusScanPort;
  private DocumentUploadService uploadService;

  @BeforeEach
  void setUp() {
    sourceItemPort = new InMemorySourceItemPort();
    packetPort = new InMemoryInformationPacketPort(sourceItemPort);
    governedIntakeService = new GovernedIntakeService(sourceItemPort, packetPort);
    documentStore = new InMemoryDocumentStore();
    virusScanPort = new NoOpVirusScanPort();
    uploadService = new DocumentUploadService(
        sourceItemPort, governedIntakeService, documentStore, virusScanPort);
  }

  @Test
  void uploadsDocumentAndCreatesSourceItemAndPacket() {
    DocumentUploadCommand command = uploadCommand(ORG_A).build();
    InputStream content = new ByteArrayInputStream(FILE_CONTENT);

    DocumentUploadResult result = uploadService.upload(command, content);

    assertThat(result.sourceItemId()).isNotNull();
    assertThat(result.informationPacketId()).isNotNull();
    assertThat(result.contentHash()).startsWith("sha256:");
    assertThat(result.storageRef()).isNotNull();
    assertThat(result.scanStatus()).isEqualTo("clean");

    SourceItem saved = sourceItemPort.findById(ORG_A, result.sourceItemId()).orElseThrow();
    assertThat(saved.mimeType()).isEqualTo("application/pdf");
    assertThat(saved.fileSizeBytes()).isEqualTo((long) FILE_CONTENT.length);
    assertThat(saved.originalFilename()).isEqualTo("cv.pdf");
    assertThat(saved.status()).isEqualTo(SourceItemStatus.REGISTERED);
    assertThat(saved.scanStatus()).isEqualTo("clean");

    assertThat(documentStore.exists(new DocumentStoreKey(
        ORG_A, result.sourceItemId().value(),
        result.contentHash().substring(0, 16), "cv.pdf")))
        .isTrue();

    List<SourceItem> packetSources = governedIntakeService.listSourceItemsForPacket(
        ORG_A, new InformationPacketId(result.informationPacketId()));
    assertThat(packetSources).hasSize(1);
    assertThat(packetSources.get(0).sourceItemId()).isEqualTo(saved.sourceItemId());
  }

  @Test
  void duplicateUploadReusesStoredBlobButCreatesIndependentSourceItemAndPacket() {
    DocumentUploadCommand command = uploadCommand(ORG_A).build();
    InputStream content = new ByteArrayInputStream(FILE_CONTENT);

    DocumentUploadResult first = uploadService.upload(command, content);
    DocumentUploadResult second = uploadService.upload(command,
        new ByteArrayInputStream(FILE_CONTENT));

    assertThat(second.sourceItemId()).isNotEqualTo(first.sourceItemId());
    assertThat(second.informationPacketId()).isNotNull();
    assertThat(second.contentHash()).isEqualTo(first.contentHash());
    assertThat(second.storageRef()).isEqualTo(first.storageRef());

    SourceItem firstItem = sourceItemPort.findById(ORG_A, first.sourceItemId()).orElseThrow();
    SourceItem secondItem = sourceItemPort.findById(ORG_A, second.sourceItemId()).orElseThrow();
    assertThat(firstItem.storageRef()).isEqualTo(secondItem.storageRef());

    assertThat(documentStore.exists(DocumentStoreKey.fromStorageRef(first.storageRef()))).isTrue();
    assertThat(documentStore.exists(new DocumentStoreKey(
        ORG_A,
        second.sourceItemId().value(),
        second.contentHash().substring(0, 16),
        "cv.pdf"))).isFalse();

    assertThat(governedIntakeService.listSourceItemsForPacket(
        ORG_A, new InformationPacketId(first.informationPacketId())))
        .extracting(SourceItem::sourceItemId)
        .containsExactly(first.sourceItemId());
    assertThat(governedIntakeService.listSourceItemsForPacket(
        ORG_A, new InformationPacketId(second.informationPacketId())))
        .extracting(SourceItem::sourceItemId)
        .containsExactly(second.sourceItemId());
  }

  @Test
  void retrieveDocumentUsesPersistedStorageRefForDeduplicatedSourceItems() throws Exception {
    DocumentUploadCommand command = uploadCommand(ORG_A).build();

    DocumentUploadResult first = uploadService.upload(command, new ByteArrayInputStream(FILE_CONTENT));
    DocumentUploadResult second = uploadService.upload(command, new ByteArrayInputStream(FILE_CONTENT));

    byte[] retrieved = uploadService.retrieveDocument(ORG_A, second.sourceItemId().value())
        .content()
        .readAllBytes();

    assertThat(retrieved).isEqualTo(FILE_CONTENT);
    assertThat(second.storageRef()).isEqualTo(first.storageRef());
  }

  @Test
  void uploadFailureAfterStoreCleansUpNewBlob() {
    InMemorySourceItemPort failingSourceItemPort = new InMemorySourceItemPort();
    FailingAttachInformationPacketPort failingPacketPort =
        new FailingAttachInformationPacketPort(failingSourceItemPort);
    GovernedIntakeService failingGovernedIntakeService =
        new GovernedIntakeService(failingSourceItemPort, failingPacketPort);
    InMemoryDocumentStore failingDocumentStore = new InMemoryDocumentStore();
    DocumentUploadService failingUploadService = new DocumentUploadService(
        failingSourceItemPort,
        failingGovernedIntakeService,
        failingDocumentStore,
        new NoOpVirusScanPort());

    assertThatThrownBy(() -> failingUploadService.upload(
        uploadCommand(ORG_A).build(),
        new ByteArrayInputStream(FILE_CONTENT)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("attach failed");

    SourceItem storedItem = failingSourceItemPort.allSourceItems().get(0);
    assertThat(failingDocumentStore.exists(
        DocumentStoreKey.fromStorageRef(storedItem.storageRef()))).isFalse();
  }

  @Test
  void infectedUploadIsRejectedBeforeStorageOrPersistence() {
    DocumentUploadService infectedUploadService = new DocumentUploadService(
        sourceItemPort,
        governedIntakeService,
        documentStore,
        content -> VirusScanPort.ScanResult.INFECTED);

    assertThatThrownBy(() -> infectedUploadService.upload(
        uploadCommand(ORG_A).build(),
        new ByteArrayInputStream(FILE_CONTENT)))
        .isInstanceOf(DocumentUploadException.class)
        .hasMessage("Uploaded file failed virus scan");

    assertThat(sourceItemPort.allSourceItems()).isEmpty();
  }

  @Test
  void scanErrorIsRejectedBeforeStorageOrPersistence() {
    DocumentUploadService scanErrorUploadService = new DocumentUploadService(
        sourceItemPort,
        governedIntakeService,
        documentStore,
        content -> VirusScanPort.ScanResult.ERROR);

    assertThatThrownBy(() -> scanErrorUploadService.upload(
        uploadCommand(ORG_A).build(),
        new ByteArrayInputStream(FILE_CONTENT)))
        .isInstanceOf(DocumentUploadException.class)
        .hasMessage("Virus scan failed");

    assertThat(sourceItemPort.allSourceItems()).isEmpty();
  }

  @Test
  void uploadRequiresAuthenticatedActorIdForWorkflowAudit() {
    DocumentUploadCommand command = new DocumentUploadCommand.Builder(
        ORG_A,
        SourceItemType.CV,
        SourceItemOrigin.CONSULTANT_UPLOAD,
        ActorRole.CONSULTANT)
        .title("Candidate CV")
        .originalFilename("cv.pdf")
        .mimeType("application/pdf")
        .contentLength(FILE_CONTENT.length)
        .build();

    assertThatThrownBy(() -> uploadService.upload(
        command,
        new ByteArrayInputStream(FILE_CONTENT)))
        .isInstanceOf(DocumentUploadException.class)
        .hasMessage("Document upload requires an authenticated actor id");
  }

  @Test
  void rejectsUnsupportedMimeType() {
    DocumentUploadCommand command = uploadCommand(ORG_A)
        .mimeType("application/octet-stream")
        .contentLength(100)
        .build();

    assertThatThrownBy(() -> uploadService.upload(command,
        new ByteArrayInputStream(FILE_CONTENT)))
        .isInstanceOf(DocumentUploadException.class)
        .hasMessageContaining("Unsupported MIME type");
  }

  @Test
  void rejectsOversizedUpload() {
    DocumentUploadCommand command = uploadCommand(ORG_A)
        .contentLength(50L * 1024 * 1024)
        .build();

    assertThatThrownBy(() -> uploadService.upload(command,
        new ByteArrayInputStream(FILE_CONTENT)))
        .isInstanceOf(DocumentUploadException.class)
        .hasMessageContaining("File size exceeds maximum");
  }

  @Test
  void requiresOrganizationId() {
    assertThatThrownBy(() -> new DocumentUploadCommand.Builder(null,
        SourceItemType.CV, SourceItemOrigin.CONSULTANT_UPLOAD, ActorRole.CONSULTANT))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void requiresSourceType() {
    assertThatThrownBy(() -> new DocumentUploadCommand.Builder(
        ORG_A, null, SourceItemOrigin.CONSULTANT_UPLOAD, ActorRole.CONSULTANT))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("sourceType must not be null");
  }

  @Test
  void requiresOrigin() {
    assertThatThrownBy(() -> new DocumentUploadCommand.Builder(
        ORG_A, SourceItemType.CV, null, ActorRole.CONSULTANT))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("origin must not be null");
  }

  @Test
  void requiresMimeType() {
    assertThatThrownBy(() -> uploadCommand(ORG_A).mimeType(null).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("mimeType must not be null or blank");
  }

  @Test
  void requiresPositiveContentLength() {
    assertThatThrownBy(() -> uploadCommand(ORG_A).contentLength(0).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("contentLength must be positive");
  }

  @Test
  void documentStoreRoundTripWorks() {
    DocumentUploadCommand command = uploadCommand(ORG_A).build();

    DocumentUploadResult result = uploadService.upload(command,
        new ByteArrayInputStream(FILE_CONTENT));

    String hashPrefix = result.contentHash().substring(0, 16);
    DocumentStoreKey key = new DocumentStoreKey(
        ORG_A, result.sourceItemId().value(), hashPrefix, "cv.pdf");
    InputStream retrieved = documentStore.retrieve(key);
    byte[] retrievedBytes;
    try {
      retrievedBytes = retrieved.readAllBytes();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThat(retrievedBytes).isEqualTo(FILE_CONTENT);
  }

  @Test
  void sourceItemIsNotCanonicalFact() {
    DocumentUploadCommand command = uploadCommand(ORG_A).build();

    DocumentUploadResult result = uploadService.upload(command,
        new ByteArrayInputStream(FILE_CONTENT));
    SourceItem saved = sourceItemPort.findById(ORG_A, result.sourceItemId()).orElseThrow();

    assertThat(saved.getClass().getRecordComponents())
        .extracting("name")
        .doesNotContain("candidateId", "candidateProfileId", "canonicalWriteAllowed",
            "claimLedgerItemId", "reviewEventId", "workflowEventId");
  }

  private static DocumentUploadCommand.Builder uploadCommand(UUID organizationId) {
    return new DocumentUploadCommand.Builder(
        organizationId,
        SourceItemType.CV,
        SourceItemOrigin.CONSULTANT_UPLOAD,
        ActorRole.CONSULTANT)
        .title("Senior DV engineer CV")
        .originalFilename("cv.pdf")
        .mimeType("application/pdf")
        .contentLength(FILE_CONTENT.length)
        .uploadedByActorId(ACTOR_ID);
  }

  private static final class InMemorySourceItemPort implements SourceItemPersistencePort {
    private final Map<SourceItemId, SourceItem> sourceItems = new LinkedHashMap<>();
    private int sequence;

    @Override
    public SourceItem append(SourceItemRegistrationCommand command) {
      SourceItemId sid = command.sourceItemId() != null
          ? command.sourceItemId()
          : new SourceItemId(UUID.fromString(
              "00000000-0000-0000-0000-0000002001"
                  + String.format("%02d", ++sequence)));
      SourceItem sourceItem = new SourceItem(
          sid,
          command.organizationId(),
          command.sourceType(),
          command.origin(),
          command.title(),
          command.contentHash(),
          command.externalRef(),
          command.storageRef(),
          command.rawRef(),
          command.language(),
          command.uploadedByActorType(),
          command.uploadedByActorId(),
          command.receivedAt(),
          command.receivedAt(),
          command.metadataJson(),
          command.status(),
          command.mimeType(),
          command.fileSizeBytes(),
          command.originalFilename(),
          command.scanStatus());
      sourceItems.put(sid, sourceItem);
      return sourceItem;
    }

    @Override
    public Optional<SourceItem> findById(UUID organizationId, SourceItemId sourceItemId) {
      return Optional.ofNullable(sourceItems.get(sourceItemId))
          .filter(sourceItem -> sourceItem.organizationId().equals(organizationId));
    }

    @Override
    public Optional<SourceItem> findByContentHash(UUID organizationId, String contentHash) {
      return sourceItems.values().stream()
          .filter(sourceItem -> sourceItem.organizationId().equals(organizationId)
              && contentHash.equals(sourceItem.contentHash()))
          .findFirst();
    }

    private List<SourceItem> allSourceItems() {
      return List.copyOf(sourceItems.values());
    }
  }

  private static final class InMemoryInformationPacketPort
      implements InformationPacketPersistencePort {
    private final InMemorySourceItemPort sourceItemPort;
    private final Map<InformationPacketId, InformationPacket> packets = new LinkedHashMap<>();
    private final Map<InformationPacketId, List<SourceItemId>> sourceLinks = new LinkedHashMap<>();
    private int sequence;

    private InMemoryInformationPacketPort(InMemorySourceItemPort sourceItemPort) {
      this.sourceItemPort = sourceItemPort;
    }

    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      InformationPacket packet = new InformationPacket(
          new InformationPacketId(UUID.fromString(
              "00000000-0000-0000-0000-0000002002"
                  + String.format("%02d", ++sequence))),
          command.organizationId(),
          command.packetType(),
          command.intendedEntityType(),
          command.intendedEntityId(),
          command.createdByActorType(),
          command.createdByActorId(),
          command.processingStatus(),
          Instant.now(),
          Instant.now(),
          command.notes(),
          command.metadataJson());
      packets.put(packet.informationPacketId(), packet);
      return packet;
    }

    @Override
    public Optional<InformationPacket> findById(
        UUID organizationId, InformationPacketId informationPacketId) {
      return Optional.ofNullable(packets.get(informationPacketId))
          .filter(packet -> packet.organizationId().equals(organizationId));
    }

    @Override
    public boolean hasSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      return sourceLinks.getOrDefault(informationPacketId, List.of()).contains(sourceItemId)
          && findById(organizationId, informationPacketId).isPresent();
    }

    @Override
    public void attachSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      sourceLinks.computeIfAbsent(informationPacketId, ignored -> new ArrayList<>())
          .add(sourceItemId);
    }

    @Override
    public List<SourceItem> listSourceItems(
        UUID organizationId, InformationPacketId informationPacketId) {
      if (findById(organizationId, informationPacketId).isEmpty()) {
        return List.of();
      }
      return sourceLinks.getOrDefault(informationPacketId, List.of()).stream()
          .map(sourceItemId -> sourceItemPort.findById(organizationId, sourceItemId))
          .flatMap(Optional::stream)
          .toList();
    }
  }

  private static final class FailingAttachInformationPacketPort
      implements InformationPacketPersistencePort {
    private final InMemoryInformationPacketPort delegate;

    private FailingAttachInformationPacketPort(InMemorySourceItemPort sourceItemPort) {
      this.delegate = new InMemoryInformationPacketPort(sourceItemPort);
    }

    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      return delegate.create(command);
    }

    @Override
    public Optional<InformationPacket> findById(
        UUID organizationId, InformationPacketId informationPacketId) {
      return delegate.findById(organizationId, informationPacketId);
    }

    @Override
    public boolean hasSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      return delegate.hasSourceItem(organizationId, informationPacketId, sourceItemId);
    }

    @Override
    public void attachSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      throw new IllegalStateException("attach failed");
    }

    @Override
    public List<SourceItem> listSourceItems(
        UUID organizationId, InformationPacketId informationPacketId) {
      return delegate.listSourceItems(organizationId, informationPacketId);
    }
  }
}
