package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakeQueueItemResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakeQueueResponse;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCanonicalWriteBridgeStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeCleanFactCandidate;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedField;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedFieldStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionSourceSnapshot;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.ReviewEventForCanonicalWrite;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedAiIntakeOrchestrator;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewDecisionService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewQueryService;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecisionType;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.test.context.TestPropertySource(properties = {
    "rto.auth.jwt.secret=0123456789abcdef0123456789abcdef",
    "rto.auth.jwt.issuer=test"
})
@WebMvcTest(ConsultantIntakeController.class)
@Import({
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class ConsultantIntakeControllerTest {

  private static final UUID ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a001");
  private static final UUID USER_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a002");
  private static final UUID PACKET_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a003");
  private static final UUID RUN_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a004");
  private static final UUID SOURCE_ITEM_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a005");
  private static final UUID PARSED_DOCUMENT_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a006");
  private static final UUID CHUNK_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a007");
  private static final UUID AI_TASK_RUN_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a008");
  private static final UUID REVIEW_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a009");
  private static final UUID WORKFLOW_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a00a");
  private static final UUID CLAIM_ID =
      UUID.fromString("00000000-0000-0000-0000-00000023a00b");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private GovernedAiIntakeOrchestrator governedAiIntakeOrchestrator;

  @MockBean
  private IntakeReviewQueryService intakeReviewQueryService;

  @MockBean
  private IntakeReviewDecisionService intakeReviewDecisionService;

  @MockBean
  private ConsultantIntakeQueueQueryService consultantIntakeQueueQueryService;

  @MockBean
  private InformationPacketPersistencePort informationPacketPersistencePort;

  @MockBean
  private ClaimLedgerItemReviewLookupPort claimLedgerItemReviewLookupPort;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @Test
  void extractRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/consultant/intake/packets/" + PACKET_ID + "/extract"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"))
        .andExpect(jsonPath("$.error.safeReason").value("authentication_required"));
  }

  @Test
  void reviewRejectsWrongPortalRole() throws Exception {
    mockMvc.perform(get("/api/consultant/intake/packets/" + PACKET_ID + "/review")
            .with(authentication(auth("client"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"));
  }

  @Test
  void extractReturnsApiSafeRunEnvelope() throws Exception {
    when(governedAiIntakeOrchestrator.extract(
        eq(ORG_ID),
        eq(new InformationPacketId(PACKET_ID)),
        eq(USER_ID),
        eq(ActorRole.CONSULTANT)))
            .thenReturn(sampleRun());

    mockMvc.perform(post("/api/consultant/intake/packets/" + PACKET_ID + "/extract")
            .with(authentication(auth("consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.extractionRunId").value(RUN_ID.toString()))
        .andExpect(jsonPath("$.data.informationPacketId").value(PACKET_ID.toString()))
        .andExpect(jsonPath("$.data.intendedEntityType").value("CANDIDATE"))
        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.data.cleanFactCount").value(1))
        .andExpect(jsonPath("$.data.aiTaskRunIds[0]").value(AI_TASK_RUN_ID.toString()));
  }

  @Test
  void reviewReturnsCleanFactsAndSourceHighlight() throws Exception {
    when(intakeReviewQueryService.reviewView(eq(ORG_ID), eq(new InformationPacketId(PACKET_ID))))
        .thenReturn(new IntakeReviewQueryService.IntakeReviewView(
            sampleRun(),
            List.of(new IntakeReviewQueryService.ReviewedCleanFact(
                sampleCandidate(),
                new ClaimId(CLAIM_ID),
                sampleReview()))));

    mockMvc.perform(get("/api/consultant/intake/packets/" + PACKET_ID + "/review")
            .with(authentication(auth("consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.extractionRunId").value(RUN_ID.toString()))
        .andExpect(jsonPath("$.data.cleanFactCount").value(1))
        .andExpect(jsonPath("$.data.cleanFacts[0].claimId").value(CLAIM_ID.toString()))
        .andExpect(jsonPath("$.data.cleanFacts[0].claimFieldName").value("candidate_headline"))
        .andExpect(jsonPath("$.data.cleanFacts[0].latestDecisionId").value(REVIEW_EVENT_ID.toString()))
        .andExpect(jsonPath("$.data.cleanFacts[0].sourceHighlight.sourceItemId").value(SOURCE_ITEM_ID.toString()))
        .andExpect(jsonPath("$.data.cleanFacts[0].sourceHighlight.locator").value("page 1 offsets 5-32"));
  }

  @Test
  void queueReturnsPortalBackedItems() throws Exception {
    when(consultantIntakeQueueQueryService.listQueue(eq(ORG_ID), eq(12)))
        .thenReturn(new ConsultantIntakeQueueResponse(List.of(
            new ConsultantIntakeQueueItemResponse(
                PACKET_ID.toString(),
                "resume.pdf",
                "CV",
                "CANDIDATE",
                "ready_for_publish",
                "All current facts are approved.",
                "2026-05-02T01:00:00Z",
                "2026-05-02T02:00:00Z"))));

    mockMvc.perform(get("/api/consultant/intake/queue?limit=12")
            .with(authentication(auth("consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].informationPacketId").value(PACKET_ID.toString()))
        .andExpect(jsonPath("$.data.items[0].title").value("resume.pdf"))
        .andExpect(jsonPath("$.data.items[0].stage").value("ready_for_publish"));
  }

  @Test
  void decideRejectsUnknownDecisionValue() throws Exception {
    String body = objectMapper.writeValueAsString(new ConsultantIntakeDecisionRequest(
        "not_a_real_decision",
        RiskTier.T1_LOW_RISK.wireValue(),
        "needs review",
        Boolean.FALSE));

    mockMvc.perform(post("/api/consultant/intake/claims/" + CLAIM_ID + "/decisions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .with(authentication(auth("consultant"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.errorCode").value("validation_failed"));
  }

  @Test
  void publishReturnsCanonicalWriteSummary() throws Exception {
    when(intakeReviewDecisionService.publish(eq(ORG_ID), eq(new InformationPacketId(PACKET_ID)),
        eq(USER_ID), any(IntakeReviewDecisionService.PublishRequest.class)))
            .thenReturn(new IntakeReviewDecisionService.PublishResult(
                List.of(new IntakeCanonicalWriteBridgeResult(
                    ORG_ID,
                    new ClaimId(CLAIM_ID),
                    new com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId(REVIEW_EVENT_ID),
                    new com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId(
                        WORKFLOW_EVENT_ID),
                    CanonicalWriteDecisionType.ALLOW,
                    true,
                    "persisted",
                    IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED,
                    null,
                    "candidate profile field updated")),
                List.of("candidate_profile:" + CLAIM_ID)));

    String body = objectMapper.writeValueAsString(new ConsultantIntakePublishRequest(
        UUID.fromString("00000000-0000-0000-0000-00000023a00c").toString(),
        null,
        null,
        null,
        "publish reviewed fields"));

    mockMvc.perform(post("/api/consultant/intake/packets/" + PACKET_ID + "/publish")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .with(authentication(auth("consultant"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.informationPacketId").value(PACKET_ID.toString()))
        .andExpect(jsonPath("$.data.canonicalWriteCount").value(1))
        .andExpect(jsonPath("$.data.canonicalWriteStatuses[0]").value("GATE_ALLOWED_AUDITED"))
        .andExpect(jsonPath("$.data.directWrites[0]").value("candidate_profile:" + CLAIM_ID));
  }

  private static IntakeExtractionRun sampleRun() {
    Instant now = Instant.parse("2026-05-02T01:00:00Z");
    IntakeExtractionRunId extractionRunId = new IntakeExtractionRunId(RUN_ID);
    return new IntakeExtractionRun(
        extractionRunId,
        ORG_ID,
        new InformationPacketId(PACKET_ID),
        IntakeExtractionMode.GOVERNED_AI_V1,
        IntakeExtractionStatus.SUCCEEDED,
        "intake-input.v1",
        "intake-output.v3",
        "governed-ai-v1",
        "snapshot-hash-23a",
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(new IntakeExtractionOutputEnvelope(
            extractionRunId,
            ORG_ID,
            new InformationPacketId(PACKET_ID),
            InformationPacketType.CANDIDATE,
            IntendedEntityType.CANDIDATE,
            "intake-output.v3",
            List.of(new SourceItemId(SOURCE_ITEM_ID)),
            List.of(AI_TASK_RUN_ID),
            List.of(new IntakeExtractionSourceSnapshot(
                new SourceItemId(SOURCE_ITEM_ID),
                SourceItemType.CV,
                "resume.pdf",
                "hash-23a",
                "ext-23a")),
            List.of(new IntakeExtractedField(
                "candidate_headline",
                "Senior AI engineer",
                new SourceItemId(SOURCE_ITEM_ID),
                0.91d,
                IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
                "evidence-backed extraction",
                "target_field:experience.current_title|chunk:" + CHUNK_ID + "|offsets:5-32")),
            List.of(sampleCandidate()),
            List.of(),
            List.of(),
            now)));
  }

  private static IntakeCleanFactCandidate sampleCandidate() {
    return new IntakeCleanFactCandidate(
        "candidate_headline",
        "candidate_profile",
        "experience.current_title",
        "Senior AI engineer",
        new SourceItemId(SOURCE_ITEM_ID),
        PARSED_DOCUMENT_ID,
        CHUNK_ID,
        1,
        5,
        32,
        0.91d,
        VerificationStatus.AI_EXTRACTED,
        RiskTier.T1_LOW_RISK,
        "EXISTING_MATCH",
        UUID.fromString("00000000-0000-0000-0000-00000023a00d").toString(),
        "target_field:experience.current_title|chunk:" + CHUNK_ID + "|offsets:5-32|claim_ordinal:1",
        false,
        "Clear title signal from uploaded CV",
        "Senior AI engineer");
  }

  private static ReviewEventForCanonicalWrite sampleReview() {
    return ReviewEventForCanonicalWrite.builder()
        .reviewEventId(new com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId(
            REVIEW_EVENT_ID))
        .organizationId(ORG_ID)
        .targetEntity(new com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef(
            "candidate_profile",
            UUID.fromString("00000000-0000-0000-0000-00000023a00e")))
        .targetFieldPath("experience.current_title")
        .claimLedgerItemId(new ClaimId(CLAIM_ID))
        .sourceSpanReference("page:1:5-32")
        .decision(com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision.APPROVED)
        .riskTier(RiskTier.T1_LOW_RISK)
        .bulkApproval(false)
        .reviewerId(USER_ID)
        .reason("approved after evidence review")
        .build();
  }

  private static Authentication auth(String portalRole) {
    return new RtoAuthenticationToken(principal(portalRole));
  }

  private static RtoAuthenticatedPrincipal principal(String portalRole) {
    return new RtoAuthenticatedPrincipal(
        USER_ID,
        ORG_ID,
        PortalRole.valueOf(portalRole.toUpperCase()),
        "Task23 Intake Tester",
        UUID.fromString("00000000-0000-0000-0000-00000023a00f"));
  }
}
