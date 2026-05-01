package com.recruitingtransactionos.coreapi.documentintelligence.service;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
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
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class DocumentIntelligenceExtractionService {

  private static final String INPUT_SCHEMA_VERSION = "intake-source-packet.v1";
  private static final String OUTPUT_SCHEMA_VERSION = "intake-extraction-envelope.v2";
  private static final String EXTRACTOR_VERSION = "document-intelligence.v1";

  private final InformationPacketPersistencePort informationPacketPersistencePort;
  private final IntakeExtractionRunPort intakeExtractionRunPort;
  private final DocumentParsingService documentParsingService;
  private final DocumentIntelligencePersistencePort persistencePort;
  private final Clock clock;
  private final Supplier<UUID> extractionRunIdSupplier;

  public DocumentIntelligenceExtractionService(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      DocumentParsingService documentParsingService,
      DocumentIntelligencePersistencePort persistencePort) {
    this(
        informationPacketPersistencePort,
        intakeExtractionRunPort,
        documentParsingService,
        persistencePort,
        Clock.systemUTC(),
        UUID::randomUUID);
  }

  DocumentIntelligenceExtractionService(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      DocumentParsingService documentParsingService,
      DocumentIntelligencePersistencePort persistencePort,
      Clock clock,
      Supplier<UUID> extractionRunIdSupplier) {
    this.informationPacketPersistencePort = Objects.requireNonNull(
        informationPacketPersistencePort, "informationPacketPersistencePort must not be null");
    this.intakeExtractionRunPort = Objects.requireNonNull(
        intakeExtractionRunPort, "intakeExtractionRunPort must not be null");
    this.documentParsingService = Objects.requireNonNull(
        documentParsingService, "documentParsingService must not be null");
    this.persistencePort = Objects.requireNonNull(persistencePort, "persistencePort must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.extractionRunIdSupplier = Objects.requireNonNull(
        extractionRunIdSupplier, "extractionRunIdSupplier must not be null");
  }

  public IntakeExtractionRun extract(UUID organizationId, InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    InformationPacket packet = informationPacketPersistencePort.findById(organizationId, informationPacketId)
        .orElseThrow(() -> new IllegalArgumentException("information packet not found in organization"));
    List<SourceItem> sourceItems = informationPacketPersistencePort.listSourceItems(organizationId, informationPacketId);
    ensureSourceOrganization(organizationId, sourceItems);
    String sourceSnapshotHash = sourceSnapshotHash(packet, sourceItems);
    if (sourceItems.isEmpty()) {
      return intakeExtractionRunPort.save(new IntakeExtractionRun(
          new IntakeExtractionRunId(extractionRunIdSupplier.get()),
          organizationId,
          informationPacketId,
          IntakeExtractionMode.DOCUMENT_INTELLIGENCE_V1,
          IntakeExtractionStatus.FAILED,
          INPUT_SCHEMA_VERSION,
          OUTPUT_SCHEMA_VERSION,
          EXTRACTOR_VERSION,
          sourceSnapshotHash,
          clock.instant(),
          Optional.of(clock.instant()),
          Optional.of("information packet has no attached source items"),
          Optional.empty()));
    }
    List<ParsedDocument> parsedDocuments = sourceItems.stream()
        .map(documentParsingService::parseSourceItem)
        .toList();
    IntakeExtractionRunId runId = new IntakeExtractionRunId(extractionRunIdSupplier.get());
    Instant now = clock.instant();
    IntakeExtractionStatus status = extractionStatus(parsedDocuments);
    return intakeExtractionRunPort.save(new IntakeExtractionRun(
        runId,
        organizationId,
        informationPacketId,
        IntakeExtractionMode.DOCUMENT_INTELLIGENCE_V1,
        status,
        INPUT_SCHEMA_VERSION,
        OUTPUT_SCHEMA_VERSION,
        EXTRACTOR_VERSION,
        sourceSnapshotHash,
        now,
        Optional.of(now),
        failureReason(parsedDocuments, status),
        outputEnvelope(runId, organizationId, packet, sourceItems, parsedDocuments, now, status)));
  }

  private List<IntakeExtractedField> extractedFields(List<ParsedDocument> parsedDocuments) {
    long succeeded = parsedDocuments.stream()
        .filter(document -> document.processingStatus() == DocumentProcessingStatus.SUCCEEDED)
        .count();
    long pending = parsedDocuments.stream()
        .filter(document -> document.processingStatus() == DocumentProcessingStatus.PENDING_EXTERNAL_PROCESSING)
        .count();
    long unsupported = parsedDocuments.stream()
        .filter(document -> document.processingStatus() == DocumentProcessingStatus.UNSUPPORTED_FOR_V1)
        .count();
    long failed = parsedDocuments.stream()
        .filter(document -> document.processingStatus() == DocumentProcessingStatus.FAILED)
        .count();
    int chunkCount = parsedDocuments.stream()
        .mapToInt(document -> persistencePort.listChunksByParsedDocument(
            document.organizationId(),
            document.parsedDocumentId()).size())
        .sum();
    return List.of(
        field("document_intelligence_version", EXTRACTOR_VERSION),
        field("parsed_document_count", String.valueOf(parsedDocuments.size())),
        field("parsed_document_succeeded_count", String.valueOf(succeeded)),
        field("parsed_document_pending_external_processing_count", String.valueOf(pending), IntakeExtractedFieldStatus.NEEDS_FUTURE_AI),
        field("parsed_document_unsupported_count", String.valueOf(unsupported), IntakeExtractedFieldStatus.UNSUPPORTED_SOURCE_TYPE),
        field("parsed_document_failed_count", String.valueOf(failed), IntakeExtractedFieldStatus.INVALID_SOURCE_REFERENCE),
        field("parsed_chunk_count", String.valueOf(chunkCount)),
        field("claim_ledger_append_allowed", "true"),
        field("canonical_write_allowed", "true"),
        field("evidence_retrieval_ready", String.valueOf(succeeded > 0)));
  }

  private static IntakeExtractedField field(String name, String value) {
    return field(name, value, IntakeExtractedFieldStatus.PLACEHOLDER);
  }

  private static IntakeExtractedField field(String name, String value, IntakeExtractedFieldStatus status) {
    return new IntakeExtractedField(name, value, null, 1.0d, status,
        "Task 22 document-intelligence extraction metadata only.");
  }

  private static List<IntakeExtractionFinding> findings(List<ParsedDocument> parsedDocuments) {
    List<IntakeExtractionFinding> findings = new ArrayList<>();
    findings.add(new IntakeExtractionFinding(
        "DOCUMENT_INTELLIGENCE_V1_EXECUTED",
        "Document intelligence v1 parsed the packet into evidence artifacts, not canonical facts.",
        null));
    for (ParsedDocument document : parsedDocuments) {
      findings.add(new IntakeExtractionFinding(
          "PARSED_DOCUMENT_STATUS_" + document.processingStatus().wireValue(),
          "Parsed document status: " + document.processingStatus().wireValue(),
          document.sourceItemId()));
    }
    return List.copyOf(findings);
  }

  private static IntakeExtractionStatus extractionStatus(List<ParsedDocument> parsedDocuments) {
    boolean anySucceeded = parsedDocuments.stream()
        .anyMatch(document -> document.processingStatus() == DocumentProcessingStatus.SUCCEEDED);
    return anySucceeded ? IntakeExtractionStatus.SUCCEEDED : IntakeExtractionStatus.FAILED;
  }

  private static Optional<String> failureReason(
      List<ParsedDocument> parsedDocuments,
      IntakeExtractionStatus status) {
    if (status == IntakeExtractionStatus.SUCCEEDED) {
      return Optional.empty();
    }
    boolean anyPending = parsedDocuments.stream()
        .anyMatch(document -> document.processingStatus() == DocumentProcessingStatus.PENDING_EXTERNAL_PROCESSING);
    boolean anyUnsupported = parsedDocuments.stream()
        .anyMatch(document -> document.processingStatus() == DocumentProcessingStatus.UNSUPPORTED_FOR_V1);
    if (anyPending) {
      return Optional.of("no_documents_ready_for_evidence_retrieval_pending_external_processing");
    }
    if (anyUnsupported) {
      return Optional.of("no_documents_supported_for_v1_evidence_retrieval");
    }
    return Optional.of("document_intelligence_produced_no_retrievable_documents");
  }

  private static void ensureSourceOrganization(UUID organizationId, List<SourceItem> sourceItems) {
    for (SourceItem sourceItem : sourceItems) {
      if (!sourceItem.organizationId().equals(organizationId)) {
        throw new IllegalStateException("source item outside organization");
      }
    }
  }

  private static String sourceSnapshotHash(
      InformationPacket packet,
      List<SourceItem> sourceItems) {
    List<SourceItem> sortedSourceItems = sourceItems.stream()
        .sorted(Comparator.comparing(sourceItem -> sourceItem.sourceItemId().value()))
        .toList();
    StringBuilder builder = new StringBuilder();
    builder.append(packet.organizationId()).append('|')
        .append(packet.informationPacketId().value()).append('|')
        .append(packet.packetType().wireValue()).append('|')
        .append(packet.intendedEntityType().wireValue());
    for (SourceItem sourceItem : sortedSourceItems) {
      builder.append('|')
          .append(sourceItem.sourceItemId().value()).append(':')
          .append(sourceItem.sourceType().wireValue()).append(':')
          .append(nullToEmpty(sourceItem.title())).append(':')
          .append(nullToEmpty(sourceItem.contentHash())).append(':')
          .append(nullToEmpty(sourceItem.externalRef()));
    }
    return "sha256:" + sha256(builder.toString());
  }

  private Optional<IntakeExtractionOutputEnvelope> outputEnvelope(
      IntakeExtractionRunId runId,
      UUID organizationId,
      InformationPacket packet,
      List<SourceItem> sourceItems,
      List<ParsedDocument> parsedDocuments,
      Instant now,
      IntakeExtractionStatus status) {
    if (status == IntakeExtractionStatus.FAILED) {
      return Optional.empty();
    }
    return Optional.of(new IntakeExtractionOutputEnvelope(
        runId,
        organizationId,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        OUTPUT_SCHEMA_VERSION,
        sourceItems.stream().map(SourceItem::sourceItemId).toList(),
        sourceItems.stream().map(DocumentIntelligenceExtractionService::snapshot).toList(),
        extractedFields(parsedDocuments),
        findings(parsedDocuments),
        List.of(),
        now));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  private static IntakeExtractionSourceSnapshot snapshot(SourceItem sourceItem) {
    return new IntakeExtractionSourceSnapshot(
        sourceItem.sourceItemId(),
        sourceItem.sourceType(),
        sourceItem.title(),
        sourceItem.contentHash(),
        sourceItem.externalRef());
  }
}
