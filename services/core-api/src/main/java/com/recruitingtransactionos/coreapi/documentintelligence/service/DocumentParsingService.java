package com.recruitingtransactionos.coreapi.documentintelligence.service;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceHit;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceRetrievalResult;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentChunk;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentSpan;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStore;
import com.recruitingtransactionos.coreapi.documentstorage.DocumentStoreKey;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public final class DocumentParsingService {

  private static final int MAX_CHUNK_LENGTH = 800;
  private static final int CHUNK_OVERLAP = 120;
  private static final Pattern MULTI_NEWLINE = Pattern.compile("\n{3,}");

  private final GovernedIntakeService governedIntakeService;
  private final DocumentStore documentStore;
  private final DocumentIntelligencePersistencePort persistencePort;
  private final DocumentConversionWorkerPort workerPort;
  private final Clock clock;
  private final List<DocumentParser> parsers;

  public DocumentParsingService(
      GovernedIntakeService governedIntakeService,
      DocumentStore documentStore,
      DocumentIntelligencePersistencePort persistencePort,
      DocumentConversionWorkerPort workerPort) {
    this(
        governedIntakeService,
        documentStore,
        persistencePort,
        workerPort,
        Clock.systemUTC(),
        List.of(new TxtDocumentParser(), new PdfDocumentParser(), new DocxDocumentParser()));
  }

  DocumentParsingService(
      GovernedIntakeService governedIntakeService,
      DocumentStore documentStore,
      DocumentIntelligencePersistencePort persistencePort,
      DocumentConversionWorkerPort workerPort,
      Clock clock,
      List<DocumentParser> parsers) {
    this.governedIntakeService = Objects.requireNonNull(governedIntakeService,
        "governedIntakeService must not be null");
    this.documentStore = Objects.requireNonNull(documentStore, "documentStore must not be null");
    this.persistencePort = Objects.requireNonNull(persistencePort, "persistencePort must not be null");
    this.workerPort = Objects.requireNonNull(workerPort, "workerPort must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.parsers = List.copyOf(Objects.requireNonNull(parsers, "parsers must not be null"));
  }

  public ParsedDocument parseSourceItem(UUID organizationId, SourceItemId sourceItemId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    SourceItem sourceItem = governedIntakeService.findSourceItem(organizationId, sourceItemId)
        .orElseThrow(() -> new DocumentResourceNotFoundException("source item not found in organization"));
    return parseSourceItem(sourceItem);
  }

  public ParsedDocument parseSourceItem(UUID organizationId, UUID sourceItemId) {
    return parseSourceItem(organizationId, new SourceItemId(sourceItemId));
  }

  public ParsedDocument parseDocument(UUID organizationId, UUID documentId) {
    return parseSourceItem(organizationId, documentId);
  }

  public ParsedDocument parseSourceItem(SourceItem sourceItem) {
    Objects.requireNonNull(sourceItem, "sourceItem must not be null");
    Instant now = clock.instant();
    String mediaType = requireMediaType(sourceItem);
    if (requiresExternalProcessing(mediaType)) {
      DocumentProcessingStatus workerStatus = workerPort.requestProcessing(sourceItem);
      return persistencePort.saveParsedDocument(
          externalProcessingDocument(sourceItem, mediaType, now, workerStatus));
    }
    DocumentParser parser = parsers.stream()
        .filter(candidate -> candidate.supports(mediaType))
        .findFirst()
        .orElse(null);
    if (parser == null) {
      return persistencePort.saveParsedDocument(new ParsedDocument(
          UUID.randomUUID(),
          sourceItem.organizationId(),
          sourceItem.sourceItemId(),
          DocumentProcessingStatus.UNSUPPORTED_FOR_V1,
          "unsupported-media",
          "v1",
          mediaType,
          sourceItem.contentHash(),
          sourceItem.language(),
          false,
          now,
          Optional.of(now),
          Optional.of("unsupported_media_type_for_v1")));
    }
    try {
      byte[] bytes = readBytes(sourceItem);
      ParseArtifacts artifacts = parser.parse(bytes, sourceItem, now);
      if (artifacts.chunkDrafts().isEmpty()) {
        return persistencePort.saveParsedDocument(new ParsedDocument(
            UUID.randomUUID(),
            sourceItem.organizationId(),
            sourceItem.sourceItemId(),
            DocumentProcessingStatus.FAILED,
            parser.parserName(),
            parser.parserVersion(),
            mediaType,
            sourceItem.contentHash(),
            sourceItem.language(),
            false,
            now,
            Optional.of(now),
            Optional.of("document_parse_produced_no_text")));
      }
      UUID parsedDocumentId = UUID.randomUUID();
      ParsedDocument parsedDocument = new ParsedDocument(
          parsedDocumentId,
          sourceItem.organizationId(),
          sourceItem.sourceItemId(),
          DocumentProcessingStatus.SUCCEEDED,
          parser.parserName(),
          parser.parserVersion(),
          mediaType,
          sourceItem.contentHash(),
          sourceItem.language(),
          false,
          now,
          Optional.of(now),
          Optional.empty());
      List<ParsedDocumentChunk> chunks = artifacts.chunkDrafts().stream()
          .map(draft -> new ParsedDocumentChunk(
              UUID.randomUUID(),
              sourceItem.organizationId(),
              parsedDocument.parsedDocumentId(),
              draft.chunkIndex(),
              draft.pageNumber(),
              draft.startOffset(),
              draft.endOffset(),
              draft.chunkText(),
              now))
          .toList();
      List<ParsedDocumentSpan> spans = artifacts.spanDrafts().stream()
          .map(draft -> new ParsedDocumentSpan(
              UUID.randomUUID(),
              sourceItem.organizationId(),
              parsedDocument.parsedDocumentId(),
              chunks.get(draft.chunkIndex()).parsedDocumentChunkId(),
              draft.spanIndex(),
              draft.pageNumber(),
              draft.startOffset(),
              draft.endOffset(),
              now))
          .toList();
      return persistencePort.saveParsedDocumentWithArtifacts(parsedDocument, chunks, spans);
    } catch (RuntimeException exception) {
      return persistencePort.saveParsedDocument(new ParsedDocument(
          UUID.randomUUID(),
          sourceItem.organizationId(),
          sourceItem.sourceItemId(),
          DocumentProcessingStatus.FAILED,
          parser.parserName(),
          parser.parserVersion(),
          mediaType,
          sourceItem.contentHash(),
          sourceItem.language(),
          false,
          now,
          Optional.of(now),
          Optional.of(safeFailureReason(exception))));
    }
  }

  public Optional<ParsedDocument> findLatestParsedDocument(UUID organizationId, SourceItemId sourceItemId) {
    return persistencePort.findLatestParsedDocumentBySourceItem(organizationId, sourceItemId);
  }

  public Optional<ParsedDocument> findLatestParsedDocument(UUID organizationId, UUID sourceItemId) {
    return findLatestParsedDocument(organizationId, new SourceItemId(sourceItemId));
  }

  public Optional<ParsedDocument> findLatestParsedDocumentByDocumentId(
      UUID organizationId,
      UUID documentId) {
    return findLatestParsedDocument(organizationId, documentId);
  }

  public int countChunksForSourceItem(UUID organizationId, SourceItemId sourceItemId) {
    ParsedDocument parsedDocument = findLatestParsedDocument(organizationId, sourceItemId).orElse(null);
    if (parsedDocument == null) {
      return 0;
    }
    return persistencePort.listChunksByParsedDocument(
        organizationId,
        parsedDocument.parsedDocumentId()).size();
  }

  public int countChunksForSourceItem(UUID organizationId, UUID sourceItemId) {
    return countChunksForSourceItem(organizationId, new SourceItemId(sourceItemId));
  }

  public int countChunksForDocument(UUID organizationId, UUID documentId) {
    return countChunksForSourceItem(organizationId, documentId);
  }

  public DocumentEvidenceRetrievalResult retrieveEvidence(
      UUID organizationId,
      SourceItemId sourceItemId,
      String query,
      int limit) {
    if (limit < 1 || limit > 20) {
      throw new IllegalArgumentException("limit must be between 1 and 20");
    }
    ParsedDocument parsedDocument = findLatestParsedDocument(organizationId, sourceItemId)
        .orElseThrow(() -> new DocumentResourceNotFoundException("parsed document not found"));
    if (parsedDocument.processingStatus() != DocumentProcessingStatus.SUCCEEDED) {
      return new DocumentEvidenceRetrievalResult(parsedDocument, List.of());
    }
    List<ParsedDocumentChunk> chunks = persistencePort.listChunksByParsedDocument(
        organizationId, parsedDocument.parsedDocumentId());
    String normalizedQuery = query == null ? "" : query.strip();
    List<DocumentEvidenceHit> hits = chunks.stream()
        .map(chunk -> toHit(chunk, normalizedQuery))
        .filter(hit -> hit != null)
        .sorted(Comparator.comparing(DocumentEvidenceHit::score).reversed()
            .thenComparing(DocumentEvidenceHit::chunkIndex))
        .limit(limit)
        .toList();
    return new DocumentEvidenceRetrievalResult(parsedDocument, hits);
  }

  public DocumentEvidenceRetrievalResult retrieveEvidence(
      UUID organizationId,
      UUID sourceItemId,
      String query,
      int limit) {
    return retrieveEvidence(organizationId, new SourceItemId(sourceItemId), query, limit);
  }

  public DocumentEvidenceRetrievalResult retrieveDocumentEvidence(
      UUID organizationId,
      UUID documentId,
      String query,
      int limit) {
    return retrieveEvidence(organizationId, documentId, query, limit);
  }

  private DocumentEvidenceHit toHit(ParsedDocumentChunk chunk, String query) {
    if (query.isBlank()) {
      return new DocumentEvidenceHit(
          chunk.parsedDocumentChunkId(),
          chunk.chunkIndex(),
          chunk.pageNumber(),
          chunk.startOffset(),
          chunk.endOffset(),
          1.0d,
          excerpt(chunk.chunkText(), 0, 260));
    }
    String normalizedText = chunk.chunkText().toLowerCase(Locale.ROOT);
    String normalizedQuery = query.toLowerCase(Locale.ROOT);
    int matchIndex = normalizedText.indexOf(normalizedQuery);
    double score = score(normalizedText, normalizedQuery);
    if (score <= 0.0d || matchIndex < 0) {
      return null;
    }
    return new DocumentEvidenceHit(
        chunk.parsedDocumentChunkId(),
        chunk.chunkIndex(),
        chunk.pageNumber(),
        chunk.startOffset(),
        chunk.endOffset(),
        score,
        excerpt(chunk.chunkText(), matchIndex, 260));
  }

  private static double score(String normalizedText, String normalizedQuery) {
    int occurrences = 0;
    int cursor = normalizedText.indexOf(normalizedQuery);
    while (cursor >= 0) {
      occurrences++;
      cursor = normalizedText.indexOf(normalizedQuery, cursor + normalizedQuery.length());
    }
    if (occurrences == 0) {
      return 0.0d;
    }
    return occurrences;
  }

  private static String excerpt(String text, int matchIndex, int maxLength) {
    int start = Math.max(0, matchIndex - 60);
    int end = Math.min(text.length(), start + maxLength);
    String value = text.substring(start, end).replaceAll("\\s+", " ").trim();
    if (value.length() > maxLength) {
      return value.substring(0, maxLength).strip();
    }
    return value;
  }

  private byte[] readBytes(SourceItem sourceItem) {
    if (sourceItem.storageRef() == null) {
      throw new DocumentParsingException("source_item_has_no_storage_ref");
    }
    DocumentStoreKey key = DocumentStoreKey.fromStorageRef(sourceItem.storageRef());
    try (InputStream inputStream = documentStore.retrieve(key);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      inputStream.transferTo(outputStream);
      return outputStream.toByteArray();
    } catch (IOException exception) {
      throw new DocumentParsingException("document_retrieval_failed", exception);
    }
  }

  private static boolean requiresExternalProcessing(String mediaType) {
    return mediaType.startsWith("image/") || mediaType.startsWith("audio/");
  }

  private static ParsedDocument externalProcessingDocument(
      SourceItem sourceItem,
      String mediaType,
      Instant now,
      DocumentProcessingStatus workerStatus) {
    return switch (workerStatus) {
      case PENDING_EXTERNAL_PROCESSING -> new ParsedDocument(
          UUID.randomUUID(),
          sourceItem.organizationId(),
          sourceItem.sourceItemId(),
          DocumentProcessingStatus.PENDING_EXTERNAL_PROCESSING,
          "ocr-boundary",
          "v1",
          mediaType,
          sourceItem.contentHash(),
          sourceItem.language(),
          true,
          now,
          Optional.empty(),
          Optional.empty());
      case FAILED -> new ParsedDocument(
          UUID.randomUUID(),
          sourceItem.organizationId(),
          sourceItem.sourceItemId(),
          DocumentProcessingStatus.FAILED,
          "ocr-boundary",
          "v1",
          mediaType,
          sourceItem.contentHash(),
          sourceItem.language(),
          true,
          now,
          Optional.of(now),
          Optional.of("external_processing_request_failed"));
      case UNSUPPORTED_FOR_V1 -> new ParsedDocument(
          UUID.randomUUID(),
          sourceItem.organizationId(),
          sourceItem.sourceItemId(),
          DocumentProcessingStatus.UNSUPPORTED_FOR_V1,
          "ocr-boundary",
          "v1",
          mediaType,
          sourceItem.contentHash(),
          sourceItem.language(),
          true,
          now,
          Optional.of(now),
          Optional.of("external_processing_unsupported_for_v1"));
      case SUCCEEDED -> new ParsedDocument(
          UUID.randomUUID(),
          sourceItem.organizationId(),
          sourceItem.sourceItemId(),
          DocumentProcessingStatus.FAILED,
          "ocr-boundary",
          "v1",
          mediaType,
          sourceItem.contentHash(),
          sourceItem.language(),
          true,
          now,
          Optional.of(now),
          Optional.of("external_processing_returned_succeeded_without_artifacts"));
    };
  }

  private static String requireMediaType(SourceItem sourceItem) {
    if (sourceItem.mimeType() == null || sourceItem.mimeType().isBlank()) {
      throw new IllegalArgumentException("source item has no mimeType");
    }
    return sourceItem.mimeType().strip().toLowerCase(Locale.ROOT);
  }

  private static String safeFailureReason(RuntimeException exception) {
    if (exception.getMessage() == null || exception.getMessage().isBlank()) {
      return "document_parse_failed";
    }
    String normalized = exception.getMessage().strip().toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9_.-]+", "_");
    return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
  }

  private static String normalizeText(String value) {
    String normalized = value.replace('\r', '\n');
    normalized = normalized.replace("\u0000", "");
    normalized = MULTI_NEWLINE.matcher(normalized).replaceAll("\n\n");
    return normalized.strip();
  }

  private static ParseArtifacts buildTextChunks(
      SourceItem sourceItem,
      Instant now,
      String parserName,
      String parserVersion,
      List<PageText> pages) {
    List<ChunkDraft> chunks = new ArrayList<>();
    List<SpanDraft> spans = new ArrayList<>();
    int chunkIndex = 0;
    for (PageText pageText : pages) {
      String normalized = normalizeText(pageText.text());
      if (normalized.isBlank()) {
        continue;
      }
      int cursor = 0;
      while (cursor < normalized.length()) {
        int end = Math.min(normalized.length(), cursor + MAX_CHUNK_LENGTH);
        String piece = normalized.substring(cursor, end).strip();
        if (!piece.isBlank()) {
          chunks.add(new ChunkDraft(
              chunkIndex,
              pageText.pageNumber(),
              cursor,
              end,
              piece));
          spans.add(new SpanDraft(
              chunkIndex,
              0,
              pageText.pageNumber(),
              cursor,
              end));
          chunkIndex++;
        }
        if (end == normalized.length()) {
          break;
        }
        cursor = Math.max(end - CHUNK_OVERLAP, cursor + 1);
      }
    }
    return new ParseArtifacts(parserName, parserVersion, chunks, spans);
  }

  private interface DocumentParser {

    boolean supports(String mediaType);

    String parserName();

    String parserVersion();

    ParseArtifacts parse(byte[] bytes, SourceItem sourceItem, Instant now);
  }

  private record ParseArtifacts(
      String parserName,
      String parserVersion,
      List<ChunkDraft> chunkDrafts,
      List<SpanDraft> spanDrafts) {}

  private record ChunkDraft(
      int chunkIndex,
      Integer pageNumber,
      int startOffset,
      int endOffset,
      String chunkText) {}

  private record SpanDraft(
      int chunkIndex,
      int spanIndex,
      Integer pageNumber,
      int startOffset,
      int endOffset) {}

  private record PageText(Integer pageNumber, String text) {}

  private static final class TxtDocumentParser implements DocumentParser {
    @Override
    public boolean supports(String mediaType) {
      return mediaType.equals("text/plain") || mediaType.equals("text/markdown");
    }

    @Override
    public String parserName() {
      return "txt-parser";
    }

    @Override
    public String parserVersion() {
      return "v1";
    }

    @Override
    public ParseArtifacts parse(byte[] bytes, SourceItem sourceItem, Instant now) {
      String text = new String(bytes, StandardCharsets.UTF_8);
      return buildTextChunks(sourceItem, now, parserName(), parserVersion(), List.of(new PageText(null, text)));
    }
  }

  private static final class DocxDocumentParser implements DocumentParser {
    @Override
    public boolean supports(String mediaType) {
      return mediaType.equals(
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Override
    public String parserName() {
      return "docx-parser";
    }

    @Override
    public String parserVersion() {
      return "v1";
    }

    @Override
    public ParseArtifacts parse(byte[] bytes, SourceItem sourceItem, Instant now) {
      try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(bytes));
          XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
        return buildTextChunks(sourceItem, now, parserName(), parserVersion(),
            List.of(new PageText(null, extractor.getText())));
      } catch (IOException exception) {
        throw new DocumentParsingException("docx_parse_failed", exception);
      }
    }
  }

  private static final class PdfDocumentParser implements DocumentParser {
    @Override
    public boolean supports(String mediaType) {
      return mediaType.equals("application/pdf");
    }

    @Override
    public String parserName() {
      return "pdf-parser";
    }

    @Override
    public String parserVersion() {
      return "v1";
    }

    @Override
    public ParseArtifacts parse(byte[] bytes, SourceItem sourceItem, Instant now) {
      try (PDDocument document = Loader.loadPDF(bytes)) {
        List<PageText> pages = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper();
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
          stripper.setStartPage(page);
          stripper.setEndPage(page);
          pages.add(new PageText(page, stripper.getText(document)));
        }
        return buildTextChunks(sourceItem, now, parserName(), parserVersion(), pages);
      } catch (IOException exception) {
        throw new DocumentParsingException("pdf_parse_failed", exception);
      }
    }
  }
}
