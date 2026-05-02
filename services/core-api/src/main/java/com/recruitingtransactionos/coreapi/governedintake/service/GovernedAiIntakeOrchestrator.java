package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserInput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake.CompanyIntakeInput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake.CompanyIntakeResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake.CompanyIntakeTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake.JobIntakeInput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake.JobIntakeResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake.JobIntakeTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.shared.AITaskClaimCandidate;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentChunk;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCleanFactCandidate;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedField;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedFieldStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionFinding;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionSourceSnapshot;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class GovernedAiIntakeOrchestrator {

  private static final String INPUT_SCHEMA_VERSION = "intake-source-packet.v1";
  private static final String OUTPUT_SCHEMA_VERSION = "intake-extraction-envelope.v3";
  private static final String EXTRACTOR_VERSION = "governed-ai-intake.v1";

  private final InformationPacketPersistencePort informationPacketPersistencePort;
  private final IntakeExtractionRunPort intakeExtractionRunPort;
  private final DocumentParsingService documentParsingService;
  private final DocumentIntelligencePersistencePort documentIntelligencePersistencePort;
  private final CandidateProfileParserTaskService candidateProfileParserTaskService;
  private final CompanyIntakeTaskService companyIntakeTaskService;
  private final JobIntakeTaskService jobIntakeTaskService;
  private final IntakeClaimLedgerBridgeService intakeClaimLedgerBridgeService;
  private final Clock clock;
  private final Supplier<UUID> extractionRunIdSupplier;

  public GovernedAiIntakeOrchestrator(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      DocumentParsingService documentParsingService,
      DocumentIntelligencePersistencePort documentIntelligencePersistencePort,
      CandidateProfileParserTaskService candidateProfileParserTaskService,
      CompanyIntakeTaskService companyIntakeTaskService,
      JobIntakeTaskService jobIntakeTaskService,
      IntakeClaimLedgerBridgeService intakeClaimLedgerBridgeService) {
    this(
        informationPacketPersistencePort,
        intakeExtractionRunPort,
        documentParsingService,
        documentIntelligencePersistencePort,
        candidateProfileParserTaskService,
        companyIntakeTaskService,
        jobIntakeTaskService,
        intakeClaimLedgerBridgeService,
        Clock.systemUTC(),
        UUID::randomUUID);
  }

  GovernedAiIntakeOrchestrator(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      DocumentParsingService documentParsingService,
      DocumentIntelligencePersistencePort documentIntelligencePersistencePort,
      CandidateProfileParserTaskService candidateProfileParserTaskService,
      CompanyIntakeTaskService companyIntakeTaskService,
      JobIntakeTaskService jobIntakeTaskService,
      IntakeClaimLedgerBridgeService intakeClaimLedgerBridgeService,
      Clock clock,
      Supplier<UUID> extractionRunIdSupplier) {
    this.informationPacketPersistencePort = Objects.requireNonNull(
        informationPacketPersistencePort, "informationPacketPersistencePort must not be null");
    this.intakeExtractionRunPort = Objects.requireNonNull(
        intakeExtractionRunPort, "intakeExtractionRunPort must not be null");
    this.documentParsingService = Objects.requireNonNull(
        documentParsingService, "documentParsingService must not be null");
    this.documentIntelligencePersistencePort = Objects.requireNonNull(
        documentIntelligencePersistencePort, "documentIntelligencePersistencePort must not be null");
    this.candidateProfileParserTaskService = Objects.requireNonNull(
        candidateProfileParserTaskService, "candidateProfileParserTaskService must not be null");
    this.companyIntakeTaskService = Objects.requireNonNull(
        companyIntakeTaskService, "companyIntakeTaskService must not be null");
    this.jobIntakeTaskService = Objects.requireNonNull(
        jobIntakeTaskService, "jobIntakeTaskService must not be null");
    this.intakeClaimLedgerBridgeService = Objects.requireNonNull(
        intakeClaimLedgerBridgeService, "intakeClaimLedgerBridgeService must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.extractionRunIdSupplier = Objects.requireNonNull(
        extractionRunIdSupplier, "extractionRunIdSupplier must not be null");
  }

  public IntakeExtractionRun extract(
      UUID organizationId,
      InformationPacketId informationPacketId,
      UUID requestedByActorId,
      ActorRole requestedByActorRole) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Objects.requireNonNull(requestedByActorId, "requestedByActorId must not be null");
    Objects.requireNonNull(requestedByActorRole, "requestedByActorRole must not be null");

    InformationPacket packet = informationPacketPersistencePort.findById(organizationId, informationPacketId)
        .orElseThrow(() -> new IllegalArgumentException("information packet not found in organization"));
    List<SourceItem> sourceItems = informationPacketPersistencePort.listSourceItems(organizationId, informationPacketId);
    if (sourceItems.isEmpty()) {
      return failedRun(packet, sourceItems, "information packet has no attached source items");
    }
    List<ParsedDocument> parsedDocuments = sourceItems.stream()
        .map(documentParsingService::parseSourceItem)
        .toList();
    List<ParsedDocument> succeeded = parsedDocuments.stream()
        .filter(document -> document.processingStatus() == DocumentProcessingStatus.SUCCEEDED)
        .toList();
    if (succeeded.isEmpty()) {
      return failedRun(packet, sourceItems, "no_parsed_documents_available_for_governed_ai_v1");
    }

    AITaskBundle bundle = executeTask(
        organizationId,
        packet,
        sourceItems,
        succeeded,
        requestedByActorId,
        requestedByActorRole);
    IntakeExtractionRunId runId = new IntakeExtractionRunId(extractionRunIdSupplier.get());
    Instant now = clock.instant();
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        runId,
        organizationId,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        OUTPUT_SCHEMA_VERSION,
        sourceItems.stream().map(SourceItem::sourceItemId).toList(),
        List.of(bundle.aiTaskRunId()),
        sourceItems.stream().map(GovernedAiIntakeOrchestrator::snapshot).toList(),
        extractedFields(packet, bundle.cleanFacts()),
        bundle.cleanFacts(),
        findings(packet, parsedDocuments, bundle.cleanFacts()),
        List.of(),
        now);
    IntakeExtractionRun run = intakeExtractionRunPort.save(new IntakeExtractionRun(
        runId,
        organizationId,
        informationPacketId,
        IntakeExtractionMode.GOVERNED_AI_V1,
        IntakeExtractionStatus.SUCCEEDED,
        INPUT_SCHEMA_VERSION,
        OUTPUT_SCHEMA_VERSION,
        EXTRACTOR_VERSION,
        sourceSnapshotHash(packet, sourceItems),
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(envelope)));
    intakeClaimLedgerBridgeService.bridge(new IntakeClaimLedgerBridgeRequest(
        organizationId,
        run.extractionRunId(),
        requestedByActorRole,
        requestedByActorId,
        IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
        null));
    return run;
  }

  private IntakeExtractionRun failedRun(
      InformationPacket packet,
      List<SourceItem> sourceItems,
      String reason) {
    Instant now = clock.instant();
    return intakeExtractionRunPort.save(new IntakeExtractionRun(
        new IntakeExtractionRunId(extractionRunIdSupplier.get()),
        packet.organizationId(),
        packet.informationPacketId(),
        IntakeExtractionMode.GOVERNED_AI_V1,
        IntakeExtractionStatus.FAILED,
        INPUT_SCHEMA_VERSION,
        OUTPUT_SCHEMA_VERSION,
        EXTRACTOR_VERSION,
        sourceSnapshotHash(packet, sourceItems),
        now,
        Optional.of(now),
        Optional.of(reason),
        Optional.empty()));
  }

  private AITaskBundle executeTask(
      UUID organizationId,
      InformationPacket packet,
      List<SourceItem> sourceItems,
      List<ParsedDocument> parsedDocuments,
      UUID requestedByActorId,
      ActorRole requestedByActorRole) {
    String combinedText = combinedText(parsedDocuments);
    ActorRef actor = new ActorRef(requestedByActorId, requestedByActorRole);
    EntityRef targetEntity = new EntityRef("information_packet", packet.informationPacketId().value());
    List<UUID> sourceReferenceIds = sourceItems.stream().map(item -> item.sourceItemId().value()).toList();
    return switch (packet.intendedEntityType()) {
      case CANDIDATE -> {
        CandidateProfileParserResult result = candidateProfileParserTaskService.execute(
            organizationId,
            actor,
            targetEntity,
            sourceReferenceIds,
            new CandidateProfileParserInput(sourceSummary(sourceItems), combinedText, null, null, null),
            null,
            null);
        yield new AITaskBundle(
            result.execution().runRecord().aiTaskRunId().value(),
            cleanFacts(packet, parsedDocuments, result.claimCandidates(), "candidate_profile"));
      }
      case COMPANY -> {
        CompanyIntakeResult result = companyIntakeTaskService.execute(
            organizationId,
            actor,
            targetEntity,
            sourceReferenceIds,
            new CompanyIntakeInput(sourceSummary(sourceItems), combinedText, null, null),
            null,
            null);
        yield new AITaskBundle(
            result.execution().runRecord().aiTaskRunId().value(),
            cleanFacts(packet, parsedDocuments, result.output().claimCandidates(), "company"));
      }
      case JOB -> {
        JobIntakeResult result = jobIntakeTaskService.execute(
            organizationId,
            actor,
            targetEntity,
            sourceReferenceIds,
            new JobIntakeInput(sourceSummary(sourceItems), combinedText, null, null),
            null,
            null);
        yield new AITaskBundle(
            result.execution().runRecord().aiTaskRunId().value(),
            cleanFacts(packet, parsedDocuments, result.output().claimCandidates(), "job"));
      }
      default -> throw new IllegalArgumentException(
          "governed_ai_v1_not_supported_for_entity_type_" + packet.intendedEntityType().wireValue());
    };
  }

  private List<IntakeCleanFactCandidate> cleanFacts(
      InformationPacket packet,
      List<ParsedDocument> parsedDocuments,
      List<AITaskClaimCandidate> claimCandidates,
      String targetEntityType) {
    List<EvidenceChunk> evidenceChunks = evidenceChunks(parsedDocuments);
    if (evidenceChunks.isEmpty()) {
      throw new IllegalStateException("governed_ai_v1_requires_parsed_document_chunk");
    }
    String entityResolutionStatus = packet.intendedEntityType() == IntendedEntityType.CANDIDATE
        ? "NEW_ENTITY_REVIEW_REQUIRED"
        : "EXISTING_OR_NEW_ENTITY_REVIEW_REQUIRED";
    List<IntakeCleanFactCandidate> cleanFacts = new ArrayList<>();
    Set<String> consumedEvidenceLocations = new HashSet<>();
    int claimOrdinal = 0;
    for (AITaskClaimCandidate candidate : claimCandidates) {
      claimOrdinal++;
      String targetFieldPath = canonicalTargetFieldPath(targetEntityType, candidate.fieldName());
      String claimFieldName = "intake.bridge_eligible." + targetEntityType + "." + targetFieldPath;
      EvidenceMatch evidence = locateEvidence(evidenceChunks, candidate, consumedEvidenceLocations);
      cleanFacts.add(new IntakeCleanFactCandidate(
          claimFieldName,
          targetEntityType,
          targetFieldPath,
          candidate.fieldValue(),
          evidence.sourceItemId(),
          evidence.parsedDocumentId(),
          evidence.parsedDocumentChunkId(),
          evidence.pageNumber(),
          evidence.startOffset(),
          evidence.endOffset(),
          0.72d,
          VerificationStatus.AI_EXTRACTED,
          RiskTier.T1_LOW_RISK,
          entityResolutionStatus,
          null,
          sourceSpanDiscriminator(
              targetFieldPath,
              evidence.parsedDocumentChunkId(),
              evidence.startOffset(),
              evidence.endOffset(),
              claimOrdinal),
          false,
          candidate.rationale(),
          evidence.safeSnippet()));
    }
    return List.copyOf(cleanFacts);
  }

  private List<EvidenceChunk> evidenceChunks(List<ParsedDocument> parsedDocuments) {
    List<EvidenceChunk> evidenceChunks = new ArrayList<>();
    for (ParsedDocument parsedDocument : parsedDocuments) {
      List<ParsedDocumentChunk> chunks = documentIntelligencePersistencePort.listChunksByParsedDocument(
          parsedDocument.organizationId(),
          parsedDocument.parsedDocumentId());
      chunks.stream()
          .sorted(Comparator.comparingInt(ParsedDocumentChunk::chunkIndex))
          .map(chunk -> new EvidenceChunk(parsedDocument, chunk))
          .forEach(evidenceChunks::add);
    }
    return List.copyOf(evidenceChunks);
  }

  private static EvidenceMatch locateEvidence(
      List<EvidenceChunk> evidenceChunks,
      AITaskClaimCandidate candidate,
      Set<String> consumedEvidenceLocations) {
    for (String needle : evidenceNeedles(candidate)) {
      Optional<EvidenceMatch> match = locateNeedle(evidenceChunks, needle, consumedEvidenceLocations);
      if (match.isPresent()) {
        return match.orElseThrow();
      }
    }
    throw new IllegalStateException(
        "governed_ai_v1_requires_matchable_evidence_quote:" + candidate.fieldName());
  }

  private static List<String> evidenceNeedles(AITaskClaimCandidate candidate) {
    List<String> needles = new ArrayList<>();
    if (candidate.evidenceQuote() != null) {
      needles.add(candidate.evidenceQuote());
    }
    for (String fragment : candidate.fieldValue().split("\\|")) {
      String value = fragment.strip();
      if (!value.isEmpty() && !needles.contains(value)) {
        needles.add(value);
      }
    }
    return List.copyOf(needles);
  }

  private static String canonicalTargetFieldPath(String targetEntityType, String fieldName) {
    if (!"candidate_profile".equals(targetEntityType)) {
      return fieldName;
    }
    return switch (fieldName) {
      case "headline" -> CandidateProfileFieldPath.PROFILE_HEADLINE.value();
      case "summary" -> CandidateProfileFieldPath.PROFILE_SUMMARY.value();
      case "primary_skills" -> CandidateProfileFieldPath.SKILLS_PRIMARY_SKILLS.value();
      case "projects" -> CandidateProfileFieldPath.EXPERIENCE_PROJECTS.value();
      case "timeline_highlights" -> CandidateProfileFieldPath.EXPERIENCE_TIMELINE_HIGHLIGHTS.value();
      default -> throw new IllegalArgumentException(
          "unsupported_candidate_profile_claim_field:" + fieldName);
    };
  }

  private static Optional<EvidenceMatch> locateNeedle(
      List<EvidenceChunk> evidenceChunks,
      String needle,
      Set<String> consumedEvidenceLocations) {
    String lowerNeedle = needle.toLowerCase();
    for (EvidenceChunk evidenceChunk : evidenceChunks) {
      String chunkText = evidenceChunk.chunk().chunkText();
      String lowerChunkText = chunkText.toLowerCase();
      int fromIndex = 0;
      while (fromIndex < lowerChunkText.length()) {
        int relativeStart = lowerChunkText.indexOf(lowerNeedle, fromIndex);
        if (relativeStart < 0) {
          break;
        }
        int relativeEnd = relativeStart + needle.length();
        String locationKey = evidenceChunk.chunk().parsedDocumentChunkId()
            + ":" + relativeStart + ":" + relativeEnd;
        fromIndex = relativeStart + 1;
        if (consumedEvidenceLocations.contains(locationKey)) {
          continue;
        }
        consumedEvidenceLocations.add(locationKey);
        return Optional.of(new EvidenceMatch(
            evidenceChunk.parsedDocument().sourceItemId(),
            evidenceChunk.parsedDocument().parsedDocumentId(),
            evidenceChunk.chunk().parsedDocumentChunkId(),
            evidenceChunk.chunk().pageNumber(),
            evidenceChunk.chunk().startOffset() + relativeStart,
            evidenceChunk.chunk().startOffset() + relativeEnd,
            snippetAround(chunkText, relativeStart, relativeEnd)));
      }
    }
    return Optional.empty();
  }

  private static String snippetAround(String chunkText, int relativeStart, int relativeEnd) {
    int window = 80;
    int snippetStart = Math.max(0, relativeStart - window);
    int snippetEnd = Math.min(chunkText.length(), relativeEnd + window);
    String snippet = chunkText.substring(snippetStart, snippetEnd).strip();
    if (snippetStart > 0) {
      snippet = "..." + snippet;
    }
    if (snippetEnd < chunkText.length()) {
      snippet = snippet + "...";
    }
    return snippet;
  }

  private static List<IntakeExtractedField> extractedFields(
      InformationPacket packet,
      List<IntakeCleanFactCandidate> cleanFacts) {
    return cleanFacts.stream()
        .map(candidate -> new IntakeExtractedField(
            candidate.claimFieldName(),
            candidate.proposedValue(),
            candidate.sourceItemId(),
            candidate.confidence(),
            IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
            packet.intendedEntityType().wireValue() + ":" + candidate.targetFieldPath(),
            candidate.sourceSpanDiscriminator()))
        .toList();
  }

  private static String sourceSpanDiscriminator(
      String targetFieldPath,
      UUID parsedDocumentChunkId,
      int startOffset,
      int endOffset,
      int claimOrdinal) {
    return "target_field:" + targetFieldPath
        + "|chunk:" + parsedDocumentChunkId
        + "|offsets:" + startOffset + "-" + endOffset
        + "|claim_ordinal:" + claimOrdinal;
  }

  private static List<IntakeExtractionFinding> findings(
      InformationPacket packet,
      List<ParsedDocument> parsedDocuments,
      List<IntakeCleanFactCandidate> cleanFacts) {
    List<IntakeExtractionFinding> findings = new ArrayList<>();
    findings.add(new IntakeExtractionFinding(
        "GOVERNED_AI_V1_EXECUTED",
        "Governed AI intake produced claim candidates that still require review before canonical write.",
        null));
    findings.add(new IntakeExtractionFinding(
        "CLEAN_FACT_CANDIDATE_COUNT",
        "Generated " + cleanFacts.size() + " clean fact candidates for " + packet.intendedEntityType().wireValue(),
        null));
    for (ParsedDocument document : parsedDocuments) {
      findings.add(new IntakeExtractionFinding(
          "PARSED_DOCUMENT_STATUS_" + document.processingStatus().wireValue(),
          "Parsed document status: " + document.processingStatus().wireValue(),
          document.sourceItemId()));
    }
    return List.copyOf(findings);
  }

  private static IntakeExtractionSourceSnapshot snapshot(SourceItem sourceItem) {
    return new IntakeExtractionSourceSnapshot(
        sourceItem.sourceItemId(),
        sourceItem.sourceType(),
        sourceItem.title(),
        sourceItem.contentHash(),
        sourceItem.externalRef());
  }

  private String combinedText(List<ParsedDocument> parsedDocuments) {
    List<String> texts = new ArrayList<>();
    for (ParsedDocument document : parsedDocuments) {
      List<ParsedDocumentChunk> chunks = documentIntelligencePersistencePort.listChunksByParsedDocument(
          document.organizationId(),
          document.parsedDocumentId());
      for (ParsedDocumentChunk chunk : chunks) {
        texts.add(chunk.chunkText());
      }
    }
    return String.join("\n\n", texts).strip();
  }

  private static String safeSnippet(String value) {
    String stripped = value == null ? "" : value.strip();
    if (stripped.length() <= 220) {
      return stripped;
    }
    return stripped.substring(0, 220).strip() + "...";
  }

  private static String sourceSummary(List<SourceItem> sourceItems) {
    return sourceItems.stream()
        .sorted(Comparator.comparing(sourceItem -> sourceItem.sourceItemId().value()))
        .map(sourceItem -> sourceItem.sourceType().wireValue() + ":" + (sourceItem.title() == null ? "untitled" : sourceItem.title()))
        .reduce((left, right) -> left + ", " + right)
        .orElse("source-items");
  }

  private static String sourceSnapshotHash(InformationPacket packet, List<SourceItem> sourceItems) {
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

  private record AITaskBundle(UUID aiTaskRunId, List<IntakeCleanFactCandidate> cleanFacts) {}

  private record EvidenceChunk(ParsedDocument parsedDocument, ParsedDocumentChunk chunk) {}

  private record EvidenceMatch(
      com.recruitingtransactionos.coreapi.governedintake.SourceItemId sourceItemId,
      UUID parsedDocumentId,
      UUID parsedDocumentChunkId,
      Integer pageNumber,
      int startOffset,
      int endOffset,
      String safeSnippet) {}
}
