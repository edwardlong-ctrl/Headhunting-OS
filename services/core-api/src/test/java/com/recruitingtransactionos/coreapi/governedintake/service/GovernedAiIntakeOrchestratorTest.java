package com.recruitingtransactionos.coreapi.governedintake.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserOutput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake.CompanyIntakeTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.jobintake.JobIntakeTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.shared.AITaskClaimCandidate;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentIntelligencePersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentChunk;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocumentSpan;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCleanFactCandidate;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunId;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GovernedAiIntakeOrchestratorTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000231001");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000231002");
  private static final InformationPacketId PACKET_ID =
      new InformationPacketId(UUID.fromString("00000000-0000-0000-0000-000000231003"));
  private static final SourceItemId SOURCE_ITEM_ID =
      new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000231004"));
  private static final UUID PARSED_DOCUMENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000231005");
  private static final UUID CHUNK_ID =
      UUID.fromString("00000000-0000-0000-0000-000000231006");
  private static final UUID SPAN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000231007");
  private static final UUID AI_TASK_RUN_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000231008");
  private static final UUID EXTRACTION_RUN_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000231009");
  private static final Instant NOW = Instant.parse("2026-05-02T04:00:00Z");

  @Test
  void extractAssignsDistinctDiscriminatorsToRepeatedClaimFields() {
    InformationPacketPersistencePort informationPacketPersistencePort =
        mock(InformationPacketPersistencePort.class);
    IntakeExtractionRunPort intakeExtractionRunPort = mock(IntakeExtractionRunPort.class);
    DocumentParsingService documentParsingService = mock(DocumentParsingService.class);
    DocumentIntelligencePersistencePort documentIntelligencePersistencePort =
        mock(DocumentIntelligencePersistencePort.class);
    CandidateProfileParserTaskService candidateProfileParserTaskService =
        mock(CandidateProfileParserTaskService.class);
    CompanyIntakeTaskService companyIntakeTaskService = mock(CompanyIntakeTaskService.class);
    JobIntakeTaskService jobIntakeTaskService = mock(JobIntakeTaskService.class);
    IntakeClaimLedgerBridgeService intakeClaimLedgerBridgeService =
        mock(IntakeClaimLedgerBridgeService.class);

    InformationPacket packet = new InformationPacket(
        PACKET_ID,
        ORG_ID,
        InformationPacketType.CANDIDATE,
        IntendedEntityType.CANDIDATE,
        null,
        ActorRole.CONSULTANT,
        ACTOR_ID,
        InformationPacketStatus.READY_FOR_EXTRACTION,
        NOW,
        NOW,
        "candidate packet",
        "{\"case\":\"orchestrator-test\"}");
    SourceItem sourceItem = new SourceItem(
        SOURCE_ITEM_ID,
        ORG_ID,
        SourceItemType.CV,
        SourceItemOrigin.CONSULTANT_UPLOAD,
        "resume.pdf",
        "sha256:231004",
        "ext-231004",
        "s3://resume.pdf",
        "vault://resume.pdf",
        "en",
        ActorRole.CONSULTANT,
        ACTOR_ID,
        NOW,
        NOW,
        "{\"source\":\"resume\"}",
        SourceItemStatus.REGISTERED,
        "application/pdf",
        128L,
        "resume.pdf",
        "clean");
    ParsedDocument parsedDocument = new ParsedDocument(
        PARSED_DOCUMENT_ID,
        ORG_ID,
        SOURCE_ITEM_ID,
        DocumentProcessingStatus.SUCCEEDED,
        "parser",
        "v1",
        "application/pdf",
        "sha256:parsed-231005",
        "en",
        false,
        NOW,
        Optional.of(NOW),
        Optional.empty());
    ParsedDocumentChunk chunk = new ParsedDocumentChunk(
        CHUNK_ID,
        ORG_ID,
        PARSED_DOCUMENT_ID,
        0,
        1,
        0,
        92,
        "Senior AI engineer built ranking workflows. Staff AI engineer led marketplace redesign.",
        NOW);
    ParsedDocumentSpan span = new ParsedDocumentSpan(
        SPAN_ID,
        ORG_ID,
        PARSED_DOCUMENT_ID,
        CHUNK_ID,
        0,
        1,
        5,
        32,
        NOW);
    CandidateProfileParserResult parserResult = parserResult();

    when(informationPacketPersistencePort.findById(ORG_ID, PACKET_ID)).thenReturn(Optional.of(packet));
    when(informationPacketPersistencePort.listSourceItems(ORG_ID, PACKET_ID)).thenReturn(List.of(sourceItem));
    when(documentParsingService.parseSourceItem(sourceItem)).thenReturn(parsedDocument);
    when(documentIntelligencePersistencePort.listChunksByParsedDocument(ORG_ID, PARSED_DOCUMENT_ID))
        .thenReturn(List.of(chunk));
    when(documentIntelligencePersistencePort.listSpansByParsedDocument(ORG_ID, PARSED_DOCUMENT_ID))
        .thenReturn(List.of(span));
    when(candidateProfileParserTaskService.execute(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(parserResult);
    when(intakeExtractionRunPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(intakeClaimLedgerBridgeService.bridge(any())).thenReturn(new IntakeClaimLedgerBridgeResult(
        ORG_ID,
        new IntakeExtractionRunId(EXTRACTION_RUN_UUID),
        PACKET_ID,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        IntakeClaimLedgerBridgeStatus.NO_CLAIMS_APPENDED,
        "fixture"));

    GovernedAiIntakeOrchestrator orchestrator = new GovernedAiIntakeOrchestrator(
        informationPacketPersistencePort,
        intakeExtractionRunPort,
        documentParsingService,
        documentIntelligencePersistencePort,
        candidateProfileParserTaskService,
        companyIntakeTaskService,
        jobIntakeTaskService,
        intakeClaimLedgerBridgeService,
        Clock.fixed(NOW, ZoneOffset.UTC),
        () -> EXTRACTION_RUN_UUID);

    IntakeExtractionRun run = orchestrator.extract(
        ORG_ID,
        PACKET_ID,
        ACTOR_ID,
        ActorRole.CONSULTANT);

    List<IntakeCleanFactCandidate> candidates = run.outputEnvelope().orElseThrow().cleanFactCandidates();

    assertThat(candidates).hasSize(2);
    assertThat(candidates)
        .extracting(IntakeCleanFactCandidate::targetFieldPath)
        .containsExactly("profile.headline", "profile.headline");
    assertThat(candidates)
        .extracting(IntakeCleanFactCandidate::sourceSpanDiscriminator)
        .containsExactly(
            "target_field:profile.headline|chunk:" + CHUNK_ID + "|offsets:0-18|claim_ordinal:1",
            "target_field:profile.headline|chunk:" + CHUNK_ID + "|offsets:44-61|claim_ordinal:2");
    assertThat(candidates)
        .extracting(IntakeCleanFactCandidate::startOffset)
        .containsExactly(0, 44);
    assertThat(candidates)
        .extracting(IntakeCleanFactCandidate::safeSnippet)
        .allSatisfy(snippet -> assertThat(snippet).contains("AI engineer"));
    assertThat(run.outputEnvelope().orElseThrow().extractedFields())
        .extracting(field -> field.sourceSpanDiscriminator())
        .containsExactly(
            "target_field:profile.headline|chunk:" + CHUNK_ID + "|offsets:0-18|claim_ordinal:1",
            "target_field:profile.headline|chunk:" + CHUNK_ID + "|offsets:44-61|claim_ordinal:2");
    org.mockito.Mockito.verify(intakeClaimLedgerBridgeService).bridge(any(IntakeClaimLedgerBridgeRequest.class));
  }

  @Test
  void extractFailsClosedWhenEvidenceQuoteCannotBeMatched() {
    InformationPacketPersistencePort informationPacketPersistencePort =
        mock(InformationPacketPersistencePort.class);
    IntakeExtractionRunPort intakeExtractionRunPort = mock(IntakeExtractionRunPort.class);
    DocumentParsingService documentParsingService = mock(DocumentParsingService.class);
    DocumentIntelligencePersistencePort documentIntelligencePersistencePort =
        mock(DocumentIntelligencePersistencePort.class);
    CandidateProfileParserTaskService candidateProfileParserTaskService =
        mock(CandidateProfileParserTaskService.class);

    InformationPacket packet = new InformationPacket(
        PACKET_ID,
        ORG_ID,
        InformationPacketType.CANDIDATE,
        IntendedEntityType.CANDIDATE,
        null,
        ActorRole.CONSULTANT,
        ACTOR_ID,
        InformationPacketStatus.READY_FOR_EXTRACTION,
        NOW,
        NOW,
        "candidate packet",
        "{\"case\":\"orchestrator-test\"}");
    SourceItem sourceItem = new SourceItem(
        SOURCE_ITEM_ID,
        ORG_ID,
        SourceItemType.CV,
        SourceItemOrigin.CONSULTANT_UPLOAD,
        "resume.pdf",
        "sha256:231004",
        "ext-231004",
        "s3://resume.pdf",
        "vault://resume.pdf",
        "en",
        ActorRole.CONSULTANT,
        ACTOR_ID,
        NOW,
        NOW,
        "{\"source\":\"resume\"}",
        SourceItemStatus.REGISTERED,
        "application/pdf",
        128L,
        "resume.pdf",
        "clean");
    ParsedDocument parsedDocument = new ParsedDocument(
        PARSED_DOCUMENT_ID,
        ORG_ID,
        SOURCE_ITEM_ID,
        DocumentProcessingStatus.SUCCEEDED,
        "parser",
        "v1",
        "application/pdf",
        "sha256:parsed-231005",
        "en",
        false,
        NOW,
        Optional.of(NOW),
        Optional.empty());
    ParsedDocumentChunk chunk = new ParsedDocumentChunk(
        CHUNK_ID,
        ORG_ID,
        PARSED_DOCUMENT_ID,
        0,
        1,
        0,
        24,
        "Senior AI engineer role.",
        NOW);
    CandidateProfileParserResult parserResult = unmatchedEvidenceParserResult();

    when(informationPacketPersistencePort.findById(ORG_ID, PACKET_ID)).thenReturn(Optional.of(packet));
    when(informationPacketPersistencePort.listSourceItems(ORG_ID, PACKET_ID)).thenReturn(List.of(sourceItem));
    when(documentParsingService.parseSourceItem(sourceItem)).thenReturn(parsedDocument);
    when(documentIntelligencePersistencePort.listChunksByParsedDocument(ORG_ID, PARSED_DOCUMENT_ID))
        .thenReturn(List.of(chunk));
    when(candidateProfileParserTaskService.execute(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(parserResult);

    GovernedAiIntakeOrchestrator orchestrator = new GovernedAiIntakeOrchestrator(
        informationPacketPersistencePort,
        intakeExtractionRunPort,
        documentParsingService,
        documentIntelligencePersistencePort,
        candidateProfileParserTaskService,
        mock(CompanyIntakeTaskService.class),
        mock(JobIntakeTaskService.class),
        mock(IntakeClaimLedgerBridgeService.class),
        Clock.fixed(NOW, ZoneOffset.UTC),
        () -> EXTRACTION_RUN_UUID);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> orchestrator.extract(
        ORG_ID,
        PACKET_ID,
        ACTOR_ID,
        ActorRole.CONSULTANT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("governed_ai_v1_requires_matchable_evidence_quote:headline");
  }

  private static CandidateProfileParserResult parserResult() {
    AITaskRunRecord runRecord = mock(AITaskRunRecord.class);
    when(runRecord.aiTaskRunId()).thenReturn(new AITaskRunId(AI_TASK_RUN_UUID));
    AITaskExecutionResult execution = new AITaskExecutionResult(
        runRecord,
        JsonNodeFactory.instance.objectNode(),
        Duration.ofSeconds(1));
    return new CandidateProfileParserResult(
        execution,
        mock(CandidateProfileParserOutput.class),
        List.of(
            new AITaskClaimCandidate(
                "headline",
                "Senior AI engineer",
                "first title mention",
                "Senior AI engineer"),
            new AITaskClaimCandidate(
                "headline",
                "Staff AI engineer",
                "second title mention",
                "Staff AI engineer")));
  }

  private static CandidateProfileParserResult unmatchedEvidenceParserResult() {
    AITaskRunRecord runRecord = mock(AITaskRunRecord.class);
    when(runRecord.aiTaskRunId()).thenReturn(new AITaskRunId(AI_TASK_RUN_UUID));
    AITaskExecutionResult execution = new AITaskExecutionResult(
        runRecord,
        JsonNodeFactory.instance.objectNode(),
        Duration.ofSeconds(1));
    return new CandidateProfileParserResult(
        execution,
        mock(CandidateProfileParserOutput.class),
        List.of(new AITaskClaimCandidate(
            "headline",
            "AI headline",
            "missing evidence",
            "No literal match in source")));
  }
}
