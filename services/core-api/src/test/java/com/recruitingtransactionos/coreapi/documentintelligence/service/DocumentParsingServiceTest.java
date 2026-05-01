package com.recruitingtransactionos.coreapi.documentintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceRetrievalResult;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentChunk;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentSpan;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentParsingServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000221001");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000221002");
  private static final SourceItemId TXT_SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000221003"));
  private static final SourceItemId PDF_SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000221004"));
  private static final SourceItemId DOCX_SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000221005"));
  private static final SourceItemId PNG_SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000221006"));

  private final InMemorySourceItemPort sourceItemPort = new InMemorySourceItemPort();
  private final InMemoryDocumentStore documentStore = new InMemoryDocumentStore();
  private final InMemoryDocumentIntelligencePort persistencePort =
      new InMemoryDocumentIntelligencePort();

  private DocumentParsingService parsingService;

  @BeforeEach
  void setUp() throws Exception {
    sourceItemPort.clear();
    documentStore.clear();
    persistencePort.clear();
    GovernedIntakeService governedIntakeService = new GovernedIntakeService(
        sourceItemPort,
        new NoOpInformationPacketPort());
    parsingService = new DocumentParsingService(
        governedIntakeService,
        documentStore,
        persistencePort,
        new NoOpDocumentConversionWorkerPort());

    registerSource(TXT_SOURCE_ITEM_ID, "text/plain", "candidate.txt", txtBytes());
    registerSource(PDF_SOURCE_ITEM_ID, "application/pdf", "candidate.pdf", pdfBytes());
    registerSource(
        DOCX_SOURCE_ITEM_ID,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "candidate.docx",
        docxBytes());
    registerSource(PNG_SOURCE_ITEM_ID, "image/png", "candidate.png", new byte[] {1, 2, 3});
  }

  @Test
  void parsesTxtIntoChunksAndEvidenceHits() {
    ParsedDocument parsedDocument = parsingService.parseSourceItem(ORGANIZATION_ID, TXT_SOURCE_ITEM_ID);

    assertThat(parsedDocument.processingStatus()).isEqualTo(DocumentProcessingStatus.SUCCEEDED);
    assertThat(persistencePort.listChunksByParsedDocument(
        ORGANIZATION_ID, parsedDocument.parsedDocumentId())).isNotEmpty();

    DocumentEvidenceRetrievalResult result =
        parsingService.retrieveEvidence(ORGANIZATION_ID, TXT_SOURCE_ITEM_ID, "Nebula", 5);
    assertThat(result.hits()).hasSize(1);
    assertThat(result.hits().get(0).excerpt()).contains("Nebula");
  }

  @Test
  void parsesPdfAndCapturesPageNumber() {
    ParsedDocument parsedDocument = parsingService.parseSourceItem(ORGANIZATION_ID, PDF_SOURCE_ITEM_ID);

    List<ParsedDocumentChunk> chunks = persistencePort.listChunksByParsedDocument(
        ORGANIZATION_ID, parsedDocument.parsedDocumentId());
    assertThat(parsedDocument.processingStatus()).isEqualTo(DocumentProcessingStatus.SUCCEEDED);
    assertThat(chunks).isNotEmpty();
    assertThat(chunks).anySatisfy(chunk -> assertThat(chunk.pageNumber()).isEqualTo(1));
  }

  @Test
  void parsesDocxIntoChunks() {
    ParsedDocument parsedDocument = parsingService.parseSourceItem(ORGANIZATION_ID, DOCX_SOURCE_ITEM_ID);

    List<ParsedDocumentChunk> chunks = persistencePort.listChunksByParsedDocument(
        ORGANIZATION_ID, parsedDocument.parsedDocumentId());
    assertThat(parsedDocument.processingStatus()).isEqualTo(DocumentProcessingStatus.SUCCEEDED);
    assertThat(chunks.get(0).chunkText()).contains("resume");
  }

  @Test
  void imagesRemainPendingExternalProcessing() {
    ParsedDocument parsedDocument = parsingService.parseSourceItem(ORGANIZATION_ID, PNG_SOURCE_ITEM_ID);

    assertThat(parsedDocument.processingStatus())
        .isEqualTo(DocumentProcessingStatus.PENDING_EXTERNAL_PROCESSING);
    assertThat(parsedDocument.ocrRequired()).isTrue();
    assertThat(parsingService.retrieveEvidence(ORGANIZATION_ID, PNG_SOURCE_ITEM_ID, "", 5).hits())
        .isEmpty();
  }

  @Test
  void failedExternalProcessingRequestProducesFailedParsedDocument() {
    DocumentParsingService service = new DocumentParsingService(
        new GovernedIntakeService(sourceItemPort, new NoOpInformationPacketPort()),
        documentStore,
        persistencePort,
        sourceItem -> DocumentProcessingStatus.FAILED);

    ParsedDocument parsedDocument = service.parseSourceItem(ORGANIZATION_ID, PNG_SOURCE_ITEM_ID);

    assertThat(parsedDocument.processingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
    assertThat(parsedDocument.failureReason()).hasValue("external_processing_request_failed");
    assertThat(parsedDocument.completedAt()).isPresent();
  }

  @Test
  void succeededExternalProcessingWithoutArtifactsFailsClosed() {
    DocumentParsingService service = new DocumentParsingService(
        new GovernedIntakeService(sourceItemPort, new NoOpInformationPacketPort()),
        documentStore,
        persistencePort,
        sourceItem -> DocumentProcessingStatus.SUCCEEDED);

    ParsedDocument parsedDocument = service.parseSourceItem(ORGANIZATION_ID, PNG_SOURCE_ITEM_ID);

    assertThat(parsedDocument.processingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
    assertThat(parsedDocument.failureReason())
        .hasValue("external_processing_returned_succeeded_without_artifacts");
    assertThat(parsedDocument.completedAt()).isPresent();
  }

  @Test
  void failedArtifactPersistenceDoesNotLeaveSucceededParsedDocumentBehind() {
    persistencePort.failArtifactSave = true;

    ParsedDocument parsedDocument = parsingService.parseSourceItem(ORGANIZATION_ID, TXT_SOURCE_ITEM_ID);

    assertThat(parsedDocument.processingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
    assertThat(persistencePort.documents).hasSize(1);
    assertThat(persistencePort.documents.get(0).processingStatus()).isEqualTo(DocumentProcessingStatus.FAILED);
    assertThat(persistencePort.chunks).isEmpty();
    assertThat(persistencePort.spans).isEmpty();
  }

  private void registerSource(SourceItemId sourceItemId, String mimeType, String filename, byte[] bytes) {
    DocumentStoreKey key = new DocumentStoreKey(ORGANIZATION_ID, sourceItemId.value(), "sha256_test", filename);
    documentStore.store(key, new ByteArrayInputStream(bytes), bytes.length);
    sourceItemPort.put(new SourceItem(
        sourceItemId,
        ORGANIZATION_ID,
        SourceItemType.CV,
        SourceItemOrigin.CONSULTANT_UPLOAD,
        filename,
        "sha256:test",
        null,
        key.storagePath(),
        null,
        "en",
        ActorRole.CONSULTANT,
        ACTOR_ID,
        Instant.parse("2026-05-01T00:00:00Z"),
        Instant.parse("2026-05-01T00:00:00Z"),
        "{}",
        SourceItemStatus.RECEIVED,
        mimeType,
        (long) bytes.length,
        filename,
        "clean"));
  }

  private static byte[] txtBytes() {
    return "Nebula resume candidate with evidence retrieval text.".getBytes();
  }

  private static byte[] pdfBytes() throws IOException {
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      document.addPage(new PDPage());
      try (PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(50, 700);
        content.showText("PDF evidence for Nebula program.");
        content.endText();
      }
      document.save(outputStream);
      return outputStream.toByteArray();
    }
  }

  private static byte[] docxBytes() throws IOException {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      XWPFParagraph paragraph = document.createParagraph();
      XWPFRun run = paragraph.createRun();
      run.setText("DOCX resume with candidate evidence.");
      document.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private static final class InMemorySourceItemPort implements SourceItemPersistencePort {
    private final Map<UUID, SourceItem> items = new HashMap<>();

    @Override
    public SourceItem append(SourceItemRegistrationCommand command) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<SourceItem> findById(UUID organizationId, SourceItemId sourceItemId) {
      SourceItem item = items.get(sourceItemId.value());
      if (item == null || !organizationId.equals(item.organizationId())) {
        return Optional.empty();
      }
      return Optional.of(item);
    }

    @Override
    public Optional<SourceItem> findByContentHash(UUID organizationId, String contentHash) {
      return items.values().stream()
          .filter(item -> organizationId.equals(item.organizationId()))
          .filter(item -> contentHash.equals(item.contentHash()))
          .findFirst();
    }

    private void put(SourceItem item) {
      items.put(item.sourceItemId().value(), item);
    }

    private void clear() {
      items.clear();
    }
  }

  private static final class NoOpInformationPacketPort implements InformationPacketPersistencePort {
    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<InformationPacket> findById(UUID organizationId, InformationPacketId informationPacketId) {
      return Optional.empty();
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
      return List.of();
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
      return new ByteArrayInputStream(content.get(key.storagePath()));
    }

    @Override
    public boolean exists(DocumentStoreKey key) {
      return content.containsKey(key.storagePath());
    }

    @Override
    public void delete(DocumentStoreKey key) {
      content.remove(key.storagePath());
    }

    private void clear() {
      content.clear();
    }
  }

  private static final class InMemoryDocumentIntelligencePort implements DocumentIntelligencePersistencePort {
    private final List<ParsedDocument> documents = new ArrayList<>();
    private final Map<UUID, List<ParsedDocumentChunk>> chunks = new HashMap<>();
    private final Map<UUID, List<ParsedDocumentSpan>> spans = new HashMap<>();
    private boolean failArtifactSave;

    @Override
    public ParsedDocument saveParsedDocument(ParsedDocument parsedDocument) {
      documents.add(parsedDocument);
      return parsedDocument;
    }

    @Override
    public ParsedDocument saveParsedDocumentWithArtifacts(
        ParsedDocument parsedDocument,
        List<ParsedDocumentChunk> replacementChunks,
        List<ParsedDocumentSpan> replacementSpans) {
      if (failArtifactSave) {
        throw new IllegalStateException("artifact_persistence_failed");
      }
      documents.add(parsedDocument);
      chunks.put(parsedDocument.parsedDocumentId(), List.copyOf(replacementChunks));
      spans.put(parsedDocument.parsedDocumentId(), List.copyOf(replacementSpans));
      return parsedDocument;
    }

    @Override
    public void replaceChunksAndSpans(UUID organizationId, UUID parsedDocumentId, List<ParsedDocumentChunk> replacementChunks, List<ParsedDocumentSpan> replacementSpans) {
      chunks.put(parsedDocumentId, List.copyOf(replacementChunks));
      spans.put(parsedDocumentId, List.copyOf(replacementSpans));
    }

    @Override
    public Optional<ParsedDocument> findLatestParsedDocumentBySourceItem(UUID organizationId, SourceItemId sourceItemId) {
      return documents.stream()
          .filter(document -> organizationId.equals(document.organizationId()))
          .filter(document -> sourceItemId.equals(document.sourceItemId()))
          .max(Comparator.comparing(ParsedDocument::createdAt));
    }

    @Override
    public List<ParsedDocumentChunk> listChunksByParsedDocument(UUID organizationId, UUID parsedDocumentId) {
      return chunks.getOrDefault(parsedDocumentId, List.of());
    }

    @Override
    public List<ParsedDocumentSpan> listSpansByParsedDocument(UUID organizationId, UUID parsedDocumentId) {
      return spans.getOrDefault(parsedDocumentId, List.of());
    }

    private void clear() {
      documents.clear();
      chunks.clear();
      spans.clear();
      failArtifactSave = false;
    }
  }
}
