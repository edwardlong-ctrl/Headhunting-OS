package com.recruitingtransactionos.coreapi.documentintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentChunk;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentSpan;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentIntelligenceExtractionServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000223001");
  private static final UUID OTHER_ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000223099");
  private static final UUID USER_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000223002");
  private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");

  @Test
  void extractFailsWhenNoDocumentBecomesRetrievable() {
    Scenario scenario = new Scenario();
    InformationPacketId packetId = scenario.seedPacketWithSourceItems(List.of(
        scenario.seedSourceItem("candidate.png", "image/png", new byte[] {1, 2, 3})));

    IntakeExtractionRun run = scenario.service.extract(ORGANIZATION_ID, packetId);

    assertThat(run.status()).isEqualTo(IntakeExtractionStatus.FAILED);
    assertThat(run.failureReason()).hasValueSatisfying(
        failureReason -> assertThat(failureReason).contains("pending_external_processing"));
    assertThat(run.outputEnvelope()).isEmpty();
  }

  @Test
  void extractSucceedsWhenAtLeastOneDocumentBecomesRetrievable() {
    Scenario scenario = new Scenario();
    InformationPacketId packetId = scenario.seedPacketWithSourceItems(List.of(
        scenario.seedSourceItem("candidate.txt", "text/plain", "Nebula profile".getBytes(StandardCharsets.UTF_8)),
        scenario.seedSourceItem("candidate.png", "image/png", new byte[] {1, 2, 3})));

    IntakeExtractionRun run = scenario.service.extract(ORGANIZATION_ID, packetId);

    assertThat(run.status()).isEqualTo(IntakeExtractionStatus.SUCCEEDED);
    assertThat(run.failureReason()).isEmpty();
    assertThat(run.outputEnvelope()).isPresent();
    assertThat(run.outputEnvelope().orElseThrow().extractedFields())
        .anySatisfy(field -> {
          if (field.fieldName().equals("parsed_document_succeeded_count")) {
            assertThat(field.fieldValue()).isEqualTo("1");
          }
        });
  }

  @Test
  void extractRejectsSourceItemsOutsideOrganization() {
    Scenario scenario = new Scenario();
    InformationPacketId packetId = scenario.seedPacketWithSourceItems(List.of(
        scenario.seedSourceItem(
            OTHER_ORGANIZATION_ID,
            "foreign.txt",
            "text/plain",
            "Foreign profile".getBytes(StandardCharsets.UTF_8))));

    assertThatThrownBy(() -> scenario.service.extract(ORGANIZATION_ID, packetId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("source item outside organization");
  }

  private static final class Scenario {
    private final InMemorySourceItemPort sourceItemPort = new InMemorySourceItemPort();
    private final InMemoryInformationPacketPort informationPacketPort = new InMemoryInformationPacketPort();
    private final InMemoryDocumentStore documentStore = new InMemoryDocumentStore();
    private final InMemoryDocumentIntelligencePort documentIntelligencePort =
        new InMemoryDocumentIntelligencePort();
    private final InMemoryExtractionRunPort extractionRunPort = new InMemoryExtractionRunPort();
    private final DocumentParsingService parsingService = new DocumentParsingService(
        new GovernedIntakeService(sourceItemPort, informationPacketPort),
        documentStore,
        documentIntelligencePort,
        new NoOpDocumentConversionWorkerPort());
    private final DocumentIntelligenceExtractionService service =
        new DocumentIntelligenceExtractionService(
            informationPacketPort,
            extractionRunPort,
            parsingService,
            documentIntelligencePort);

    private SourceItem seedSourceItem(String filename, String mimeType, byte[] bytes) {
      return seedSourceItem(ORGANIZATION_ID, filename, mimeType, bytes);
    }

    private SourceItem seedSourceItem(
        UUID organizationId,
        String filename,
        String mimeType,
        byte[] bytes) {
      SourceItemId sourceItemId = new SourceItemId(UUID.randomUUID());
      DocumentStoreKey key = new DocumentStoreKey(organizationId, sourceItemId.value(), "sha256_test", filename);
      documentStore.store(key, new ByteArrayInputStream(bytes), bytes.length);
      SourceItem sourceItem = new SourceItem(
          sourceItemId,
          organizationId,
          SourceItemType.CV,
          SourceItemOrigin.CONSULTANT_UPLOAD,
          filename,
          "sha256:test",
          null,
          key.storagePath(),
          null,
          "en",
          ActorRole.CONSULTANT,
          USER_ACCOUNT_ID,
          NOW,
          NOW,
          "{}",
          SourceItemStatus.RECEIVED,
          mimeType,
          (long) bytes.length,
          filename,
          "clean");
      sourceItemPort.put(sourceItem);
      return sourceItem;
    }

    private InformationPacketId seedPacketWithSourceItems(List<SourceItem> sourceItems) {
      return informationPacketPort.seedPacketWithSourceItems(sourceItems);
    }
  }

  private static final class InMemorySourceItemPort implements SourceItemPersistencePort {
    private final Map<UUID, SourceItem> items = new HashMap<>();

    @Override
    public SourceItem append(com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand command) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<SourceItem> findById(UUID organizationId, SourceItemId sourceItemId) {
      SourceItem sourceItem = items.get(sourceItemId.value());
      if (sourceItem == null || !sourceItem.organizationId().equals(organizationId)) {
        return Optional.empty();
      }
      return Optional.of(sourceItem);
    }

    @Override
    public Optional<SourceItem> findByContentHash(UUID organizationId, String contentHash) {
      return Optional.empty();
    }

    private void put(SourceItem sourceItem) {
      items.put(sourceItem.sourceItemId().value(), sourceItem);
    }
  }

  private static final class InMemoryInformationPacketPort implements InformationPacketPersistencePort {
    private final Map<InformationPacketId, InformationPacket> packets = new LinkedHashMap<>();
    private final Map<InformationPacketId, List<SourceItem>> links = new LinkedHashMap<>();
    private int sequence;

    private InformationPacketId seedPacketWithSourceItems(List<SourceItem> sourceItems) {
      InformationPacketId packetId = new InformationPacketId(UUID.fromString(
          "00000000-0000-0000-0000-0000002231" + String.format("%02d", ++sequence)));
      packets.put(packetId, new InformationPacket(
          packetId,
          ORGANIZATION_ID,
          InformationPacketType.CANDIDATE,
          IntendedEntityType.CANDIDATE,
          null,
          ActorRole.CONSULTANT,
          USER_ACCOUNT_ID,
          InformationPacketStatus.READY_FOR_EXTRACTION,
          NOW,
          NOW,
          null,
          "{}"));
      links.put(packetId, List.copyOf(sourceItems));
      return packetId;
    }

    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<InformationPacket> findById(UUID organizationId, InformationPacketId informationPacketId) {
      return Optional.ofNullable(packets.get(informationPacketId))
          .filter(packet -> packet.organizationId().equals(organizationId));
    }

    @Override
    public boolean hasSourceItem(UUID organizationId, InformationPacketId informationPacketId, SourceItemId sourceItemId) {
      return false;
    }

    @Override
    public void attachSourceItem(UUID organizationId, InformationPacketId informationPacketId, SourceItemId sourceItemId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<SourceItem> listSourceItems(UUID organizationId, InformationPacketId informationPacketId) {
      return links.getOrDefault(informationPacketId, List.of());
    }
  }

  private static final class InMemoryDocumentStore implements DocumentStore {
    private final Map<String, byte[]> content = new HashMap<>();

    @Override
    public void store(DocumentStoreKey key, InputStream inputStream, long contentLength) {
      try {
        content.put(key.storagePath(), inputStream.readAllBytes());
      } catch (IOException exception) {
        throw new IllegalStateException(exception);
      }
    }

    @Override
    public InputStream retrieve(DocumentStoreKey key) {
      return new ByteArrayInputStream(content.getOrDefault(key.storagePath(), new byte[0]));
    }

    @Override
    public boolean exists(DocumentStoreKey key) {
      return content.containsKey(key.storagePath());
    }

    @Override
    public void delete(DocumentStoreKey key) {
      content.remove(key.storagePath());
    }
  }

  private static final class InMemoryDocumentIntelligencePort implements DocumentIntelligencePersistencePort {
    private final Map<UUID, ParsedDocument> documents = new LinkedHashMap<>();
    private final Map<UUID, List<ParsedDocumentChunk>> chunks = new LinkedHashMap<>();
    private final Map<UUID, List<ParsedDocumentSpan>> spans = new LinkedHashMap<>();

    @Override
    public ParsedDocument saveParsedDocument(ParsedDocument parsedDocument) {
      documents.put(parsedDocument.parsedDocumentId(), parsedDocument);
      return parsedDocument;
    }

    @Override
    public ParsedDocument saveParsedDocumentWithArtifacts(
        ParsedDocument parsedDocument,
        List<ParsedDocumentChunk> chunkList,
        List<ParsedDocumentSpan> spanList) {
      documents.put(parsedDocument.parsedDocumentId(), parsedDocument);
      chunks.put(parsedDocument.parsedDocumentId(), List.copyOf(chunkList));
      spans.put(parsedDocument.parsedDocumentId(), List.copyOf(spanList));
      return parsedDocument;
    }

    @Override
    public void replaceChunksAndSpans(UUID organizationId, UUID parsedDocumentId, List<ParsedDocumentChunk> chunkList, List<ParsedDocumentSpan> spanList) {
      chunks.put(parsedDocumentId, List.copyOf(chunkList));
      spans.put(parsedDocumentId, List.copyOf(spanList));
    }

    @Override
    public Optional<ParsedDocument> findLatestParsedDocumentBySourceItem(UUID organizationId, SourceItemId sourceItemId) {
      return documents.values().stream()
          .filter(document -> document.organizationId().equals(organizationId))
          .filter(document -> document.sourceItemId().equals(sourceItemId))
          .reduce((first, second) -> second);
    }

    @Override
    public List<ParsedDocumentChunk> listChunksByParsedDocument(UUID organizationId, UUID parsedDocumentId) {
      return chunks.getOrDefault(parsedDocumentId, List.of());
    }

    @Override
    public List<ParsedDocumentSpan> listSpansByParsedDocument(UUID organizationId, UUID parsedDocumentId) {
      return spans.getOrDefault(parsedDocumentId, List.of());
    }
  }

  private static final class InMemoryExtractionRunPort implements IntakeExtractionRunPort {
    @Override
    public IntakeExtractionRun save(IntakeExtractionRun run) {
      return run;
    }

    @Override
    public Optional<IntakeExtractionRun> findById(UUID organizationId, IntakeExtractionRunId extractionRunId) {
      return Optional.empty();
    }

    @Override
    public List<IntakeExtractionRun> listByInformationPacket(UUID organizationId, InformationPacketId informationPacketId) {
      return List.of();
    }
  }
}
