package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.CandidateWorkflowStatePort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventIdempotencyRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ConsentDisclosureServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-00000012b001");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-00000012b002");
  private static final String CANDIDATE_REF = "candidate_ref_task12b_0001";
  private static final String PROFILE_REF = "profile_ref_task12b_0001";
  private static final String JOB_REF = "job_ref_task12b_0001";
  private static final String CLIENT_REF = "client_ref_task12b_0001";
  private static final String PROFILE_VERSION = "profile-version-12b";
  private static final String CONSENT_TEXT_VERSION = "consent-text-12b";
  private static final Instant NOW = Instant.parse("2026-04-29T12:30:00Z");

  @Test
  void deniesWhenRequiredPersistedConsentUnlockOrDisclosureRecordsAreMissing() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult result =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.DENIED);
    assertThat(result.reasonCodes())
        .contains(
            "missing_confirmed_consent",
            "missing_approved_unlock_decision",
            "missing_approved_disclosure_record");
    assertThat(result.workflowEventId()).isEmpty();
    assertThat(result.resultingDisclosureRecordRef()).isEmpty();
    assertThat(workflowEvents.commands()).isEmpty();
    assertThat(disclosureRecords.appendedRecords()).isEmpty();
  }

  @Test
  void deniesWhenBlockingDisclosurePrerequisitesAreNotExplicitlySatisfied() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));
    unlockDecisions.append(unlockDecision(UnlockDecisionStatus.APPROVED, ActorRole.CONSULTANT));
    disclosureRecords.append(approvedDisclosureRecord());

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult result = service.evaluateDisclosureAttempt(
        identityDisclosureRequestBuilder()
            .prerequisites(new ConsentDisclosurePrerequisites(
                false,
                false,
                false,
                false,
                false))
            .build());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.DENIED);
    assertThat(result.reasonCodes())
        .containsExactlyInAnyOrder(
            "job_activation_gate_required",
            "fee_agreement_gate_required",
            "prior_contact_review_required",
            "prior_application_review_required",
            "privacy_risk_gate_required");
    assertThat(result.workflowEventId()).isEmpty();
    assertThat(result.resultingDisclosureRecordRef()).isEmpty();
    assertThat(workflowEvents.commands()).isEmpty();
    assertThat(disclosureRecords.appendedRecords())
        .extracting(DisclosureRecord::status)
        .containsExactly(DisclosureStatus.APPROVED);
  }

  @Test
  void allowedIdentityDisclosureAppendsWorkflowEventAndResultingDisclosureBoundaryOnlyOnce() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));
    unlockDecisions.append(unlockDecision(UnlockDecisionStatus.APPROVED, ActorRole.CONSULTANT));
    disclosureRecords.append(approvedDisclosureRecord());

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult result =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.ALLOWED);
    assertThat(result.allowedLevel()).contains(DisclosureLevel.L4_IDENTITY_DISCLOSED);
    assertThat(result.workflowEventId()).isPresent();
    assertThat(result.resultingDisclosureRecordRef()).isPresent();
    assertThat(workflowEvents.commands()).hasSize(2);
    WorkflowEventAppendCommand disclosureAuditCommand = workflowEvents.commands().get(0);
    WorkflowEventAppendCommand candidateAuditCommand = workflowEvents.commands().get(1);
    assertThat(disclosureAuditCommand.action())
        .isEqualTo(WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED.wireValue());
    assertThat(disclosureAuditCommand.entity()).isEqualTo(new EntityRef(
        "DISCLOSURE",
        ConsentDisclosureService.disclosureEntityId(
            ORGANIZATION_ID,
            "disclosure_record_task12b_approved_0001")));
    assertThat(disclosureAuditCommand.actor()).isEqualTo(new ActorRef(ACTOR_ID, ActorRole.CONSULTANT));
    assertThat(candidateAuditCommand.action())
        .isEqualTo(WorkflowActionCode.CANDIDATE_IDENTITY_DISCLOSED.wireValue());
    assertThat(candidateAuditCommand.entity()).isEqualTo(new EntityRef(
        "CANDIDATE",
        ConsentDisclosureService.candidateEntityId(ORGANIZATION_ID, CANDIDATE_REF)));
    assertThat(disclosureRecords.appendedRecords()).hasSize(2);
    assertThat(disclosureRecords.findByRefAndOrganizationId(
        ORGANIZATION_ID,
        "disclosure_record_task12b_approved_0001")
        .map(DisclosureRecord::status))
        .contains(DisclosureStatus.IDENTITY_DISCLOSED);
    DisclosureRecord appendedBoundary = disclosureRecords.appendedRecords().getLast();
    assertThat(appendedBoundary.disclosureRecordRef())
        .isEqualTo(result.resultingDisclosureRecordRef().orElseThrow());
    assertThat(appendedBoundary.status()).isEqualTo(DisclosureStatus.IDENTITY_DISCLOSED);
    assertThat(appendedBoundary.workflowEventId()).contains(result.workflowEventId().orElseThrow());
    assertThat(appendedBoundary.disclosureLevel())
        .isEqualTo(DisclosureLevel.L4_IDENTITY_DISCLOSED);
    assertThat(appendedBoundary.redactionLevel())
        .isEqualTo(RedactionLevel.L4_IDENTITY_DISCLOSED);
  }

  @Test
  void l3ConsentedDetailDoesNotAppendIdentityDisclosureAuditOrState() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult result = service.evaluateDisclosureAttempt(
        identityDisclosureRequestBuilder()
            .requestedLevel(DisclosureLevel.L3_CONSENTED_DETAIL)
            .build());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.ALLOWED);
    assertThat(result.allowedLevel()).contains(DisclosureLevel.L3_CONSENTED_DETAIL);
    assertThat(result.workflowEventId()).isEmpty();
    assertThat(result.resultingDisclosureRecordRef()).isEmpty();
    assertThat(workflowEvents.commands()).isEmpty();
    assertThat(disclosureRecords.appendedRecords()).isEmpty();
  }

  @Test
  void l3ConsentedDetailDoesNotBindToIdentityDisclosureApprovalChain() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));
    disclosureRecords.append(new DisclosureRecord(
        "disclosure_record_task12b_approved_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        DisclosureStatus.APPROVED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        "unlock_decision_task12b_other",
        "consent_record_task12b_other",
        Optional.empty(),
        NOW.minusSeconds(900)));

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult result = service.evaluateDisclosureAttempt(
        identityDisclosureRequestBuilder()
            .requestedLevel(DisclosureLevel.L3_CONSENTED_DETAIL)
            .build());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.ALLOWED);
    assertThat(result.allowedLevel()).contains(DisclosureLevel.L3_CONSENTED_DETAIL);
    assertThat(result.workflowEventId()).isEmpty();
    assertThat(result.resultingDisclosureRecordRef()).isEmpty();
    assertThat(workflowEvents.commands()).isEmpty();
    assertThat(disclosureRecords.appendedRecords())
        .extracting(DisclosureRecord::status)
        .containsExactly(DisclosureStatus.APPROVED);
  }

  @Test
  void deniesWhenApprovedDisclosureRecordDoesNotReferenceRequestedConsentAndUnlockRefs() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));
    unlockDecisions.append(unlockDecision(UnlockDecisionStatus.APPROVED, ActorRole.CONSULTANT));
    disclosureRecords.append(new DisclosureRecord(
        "disclosure_record_task12b_approved_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        DisclosureStatus.APPROVED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        "unlock_decision_task12b_other",
        "consent_record_task12b_other",
        Optional.empty(),
        NOW.minusSeconds(900)));

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult result =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.DENIED);
    assertThat(result.reasonCodes())
        .contains(
            "disclosure_consent_record_ref_mismatch",
            "disclosure_unlock_decision_ref_mismatch");
    assertThat(result.workflowEventId()).isEmpty();
    assertThat(result.resultingDisclosureRecordRef()).isEmpty();
    assertThat(workflowEvents.commands()).isEmpty();
    assertThat(disclosureRecords.appendedRecords())
        .extracting(DisclosureRecord::status)
        .containsExactly(DisclosureStatus.APPROVED);
  }

  @Test
  void deniesIdentityDisclosureWhenUnlockApproverIsNotInRequestOrganization() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));
    unlockDecisions.append(unlockDecision(UnlockDecisionStatus.APPROVED, ActorRole.CONSULTANT));
    unlockDecisions.markApproverOutsideOrganization(
        ORGANIZATION_ID,
        "unlock_decision_task12b_0001");
    disclosureRecords.append(approvedDisclosureRecord());

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult result =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.DENIED);
    assertThat(result.reasonCodes())
        .containsExactly("unlock_approver_organization_mismatch");
    assertThat(result.workflowEventId()).isEmpty();
    assertThat(result.resultingDisclosureRecordRef()).isEmpty();
    assertThat(workflowEvents.commands()).isEmpty();
    assertThat(disclosureRecords.appendedRecords())
        .extracting(DisclosureRecord::status)
        .containsExactly(DisclosureStatus.APPROVED);
  }

  @Test
  void repeatedAllowedIdentityDisclosureRequestReusesExistingWorkflowAndDisclosureBoundary() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));
    unlockDecisions.append(unlockDecision(UnlockDecisionStatus.APPROVED, ActorRole.CONSULTANT));
    disclosureRecords.append(approvedDisclosureRecord());

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult first =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());
    ConsentDisclosureServiceResult second =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());

    assertThat(second).isEqualTo(first);
    assertThat(workflowEvents.commands()).hasSize(2);
    assertThat(disclosureRecords.appendedRecords())
        .extracting(DisclosureRecord::status)
        .containsExactly(DisclosureStatus.IDENTITY_DISCLOSED, DisclosureStatus.IDENTITY_DISCLOSED);
  }

  @Test
  void repeatedAllowedIdentityDisclosureRequestSurvivesDuplicateFinalDisclosureAppend() {
    RecordingWorkflowEventPort workflowEvents = new RecordingWorkflowEventPort();
    InMemoryConsentRecordPort consentRecords = new InMemoryConsentRecordPort();
    InMemoryUnlockDecisionPort unlockDecisions = new InMemoryUnlockDecisionPort();
    InMemoryDisclosureRecordPort disclosureRecords = new InMemoryDisclosureRecordPort();
    consentRecords.append(consent(ConsentStatus.CONFIRMED));
    unlockDecisions.append(unlockDecision(UnlockDecisionStatus.APPROVED, ActorRole.CONSULTANT));
    disclosureRecords.append(approvedDisclosureRecord());

    ConsentDisclosureService service = service(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        workflowEvents);

    ConsentDisclosureServiceResult first =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());
    disclosureRecords.simulateIdentityDisclosureDuplicateOnNextAppendAfterMiss();
    ConsentDisclosureServiceResult second =
        service.evaluateDisclosureAttempt(identityDisclosureRequest());

    assertThat(second).isEqualTo(first);
    assertThat(workflowEvents.commands()).hasSize(2);
    assertThat(disclosureRecords.appendedRecords())
        .extracting(DisclosureRecord::status)
        .containsExactly(DisclosureStatus.IDENTITY_DISCLOSED, DisclosureStatus.IDENTITY_DISCLOSED);
  }

  @Test
  void consentDisclosureServiceSurfaceRemainsBackendInternalAndRawCandidateFree() {
    assertThat(recordComponentTypes(ConsentDisclosureServiceRequest.class))
        .doesNotContain(
            com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile.class,
            com.recruitingtransactionos.coreapi.candidateprofile.CandidateId.class,
            com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId.class);
    assertThat(recordComponentTypes(ConsentDisclosureServiceResult.class))
        .doesNotContain(
            com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile.class,
            com.recruitingtransactionos.coreapi.candidateprofile.CandidateId.class,
            com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId.class);
  }

  private static ConsentDisclosureService service(
      ConsentRecordPort consentRecords,
      UnlockDecisionPort unlockDecisions,
      DisclosureRecordPort disclosureRecords,
      RecordingWorkflowEventPort workflowEvents) {
    return new ConsentDisclosureService(
        consentRecords,
        unlockDecisions,
        disclosureRecords,
        new ConsentDisclosureProtectionPolicy(),
        (request, unlockDecision, disclosureRecord) -> request.prerequisites(),
        new InMemoryCandidateWorkflowStatePort(),
        new WorkflowTransitionAuditService(new WorkflowEventService(workflowEvents), new com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEntityStatePort() {
          @Override
          public Optional<String> getCurrentStateJson(UUID orgId, String ns, String type, UUID id) { return Optional.empty(); }
        }),
        CanonicalWriteTransactionBoundary.immediate());
  }

  private static ConsentDisclosureServiceRequest identityDisclosureRequest() {
    return identityDisclosureRequestBuilder().build();
  }

  private static ConsentDisclosureServiceRequest.Builder identityDisclosureRequestBuilder() {
    return ConsentDisclosureServiceRequest.builder()
        .organizationId(ORGANIZATION_ID)
        .candidateRef(CANDIDATE_REF)
        .candidateProfileRef(PROFILE_REF)
        .jobRef(JOB_REF)
        .clientRef(CLIENT_REF)
        .consentRecordRef("consent_record_task12b_0001")
        .unlockDecisionRef("unlock_decision_task12b_0001")
        .approvedDisclosureRecordRef("disclosure_record_task12b_approved_0001")
        .requestedByRole(PortalRole.CONSULTANT)
        .actor(new ActorRef(ACTOR_ID, ActorRole.CONSULTANT))
        .requestedLevel(DisclosureLevel.L4_IDENTITY_DISCLOSED)
        .requestedAt(NOW)
        .reason("consultant approved identity disclosure after consent and unlock review")
        .prerequisites(new ConsentDisclosurePrerequisites(
            true,
            true,
            true,
            true,
            true));
  }

  private static ConsentRecord consent(ConsentStatus status) {
    return new ConsentRecord(
        "consent_record_task12b_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        PROFILE_VERSION,
        CONSENT_TEXT_VERSION,
        status,
        Set.of(DisclosureLevel.L3_CONSENTED_DETAIL, DisclosureLevel.L4_IDENTITY_DISCLOSED),
        NOW.minusSeconds(3_600),
        NOW.plusSeconds(3_600),
        false);
  }

  private static UnlockDecision unlockDecision(
      UnlockDecisionStatus status,
      ActorRole approverRole) {
    return new UnlockDecision(
        "unlock_decision_task12b_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        status,
        DisclosureReviewStatus.HUMAN_APPROVED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        new ActorRef(ACTOR_ID, approverRole),
        NOW.minusSeconds(1_800));
  }

  private static DisclosureRecord approvedDisclosureRecord() {
    return new DisclosureRecord(
        "disclosure_record_task12b_approved_0001",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        CLIENT_REF,
        DisclosureStatus.APPROVED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        "unlock_decision_task12b_0001",
        "consent_record_task12b_0001",
        Optional.empty(),
        NOW.minusSeconds(900));
  }

  private static Set<Class<?>> recordComponentTypes(Class<?> type) {
    return Stream.of(type.getRecordComponents())
        .map(RecordComponent::getType)
        .collect(java.util.stream.Collectors.toSet());
  }

  private static final class RecordingWorkflowEventPort
      implements com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventPort {

    private final List<WorkflowEventAppendCommand> commands = new ArrayList<>();

    @Override
    public Optional<WorkflowEventIdempotencyRecord> findByIdempotencyKey(
        UUID organizationId,
        WorkflowIdempotencyKey idempotencyKey) {
      return commands.stream()
          .filter(command -> command.organizationId().equals(organizationId))
          .filter(command -> idempotencyKey.equals(command.idempotencyKey()))
          .findFirst()
          .map(command -> new WorkflowEventIdempotencyRecord(
              new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-00000012b0ff")),
              command));
    }

    @Override
    public WorkflowEventAppendResult append(WorkflowEventAppendCommand command) {
      commands.add(command);
      return new WorkflowEventAppendResult(new WorkflowEventId(
          UUID.fromString("00000000-0000-0000-0000-00000012b0ff")));
    }

    List<WorkflowEventAppendCommand> commands() {
      return commands;
    }
  }

  private static final class InMemoryConsentRecordPort implements ConsentRecordPort {

    private final List<ConsentRecord> appendedRecords = new ArrayList<>();

    @Override
    public ConsentRecord append(ConsentRecord consentRecord) {
      appendedRecords.add(consentRecord);
      return consentRecord;
    }

    @Override
    public Optional<ConsentRecord> findByRefAndOrganizationId(
        UUID organizationId,
        String consentRecordRef) {
      return appendedRecords.stream()
          .filter(record -> record.organizationId().equals(organizationId))
          .filter(record -> record.consentRecordRef().equals(consentRecordRef))
          .findFirst();
    }

    @Override
    public Optional<ConsentRecord> findByWorkflowEntityId(UUID organizationId, UUID workflowEntityId) {
      return appendedRecords.stream()
          .filter(record -> record.organizationId().equals(organizationId))
          .filter(record -> ConsentDisclosureWorkflowEntityIds.consentEntityId(
              organizationId,
              record.consentRecordRef()).equals(workflowEntityId))
          .findFirst();
    }

    @Override
    public Optional<ConsentRecord> findLatestByCandidateProfileAndJob(
        UUID organizationId,
        String candidateRef,
        String candidateProfileRef,
        String jobRef) {
      return appendedRecords.stream()
          .filter(record -> record.organizationId().equals(organizationId))
          .filter(record -> record.candidateRef().equals(candidateRef))
          .filter(record -> record.candidateProfileRef().equals(candidateProfileRef))
          .filter(record -> record.jobRef().equals(jobRef))
          .reduce((first, second) -> second);
    }
  }

  private static final class InMemoryUnlockDecisionPort implements UnlockDecisionPort {

    private final List<UnlockDecision> appendedRecords = new ArrayList<>();
    private final List<String> outsideOrganizationApprovals = new ArrayList<>();

    @Override
    public UnlockDecision append(UnlockDecision unlockDecision) {
      appendedRecords.add(unlockDecision);
      return unlockDecision;
    }

    @Override
    public Optional<UnlockDecision> findByRefAndOrganizationId(
        UUID organizationId,
        String unlockDecisionRef) {
      return appendedRecords.stream()
          .filter(record -> record.organizationId().equals(organizationId))
          .filter(record -> record.unlockDecisionRef().equals(unlockDecisionRef))
          .findFirst();
    }

    public boolean approvedByBelongsToOrganization(
        UUID organizationId,
        String unlockDecisionRef) {
      return !outsideOrganizationApprovals.contains(organizationId + "|" + unlockDecisionRef);
    }

    void markApproverOutsideOrganization(UUID organizationId, String unlockDecisionRef) {
      outsideOrganizationApprovals.add(organizationId + "|" + unlockDecisionRef);
    }
  }

  private static final class InMemoryDisclosureRecordPort implements DisclosureRecordPort {

    private final List<DisclosureRecord> appendedRecords = new ArrayList<>();
    private boolean hideNextIdentityDisclosureLookup;
    private boolean failNextIdentityDisclosureAppendAsDuplicate;

    @Override
    public DisclosureRecord append(DisclosureRecord disclosureRecord) {
      if (failNextIdentityDisclosureAppendAsDuplicate
          && disclosureRecord.status() == DisclosureStatus.IDENTITY_DISCLOSED) {
        failNextIdentityDisclosureAppendAsDuplicate = false;
        throw new IllegalStateException("duplicate disclosure record");
      }
      appendedRecords.add(disclosureRecord);
      return disclosureRecord;
    }

    @Override
    public Optional<DisclosureRecord> findByRefAndOrganizationId(
        UUID organizationId,
        String disclosureRecordRef) {
      if (hideNextIdentityDisclosureLookup
          && appendedRecords.stream()
              .anyMatch(record -> record.organizationId().equals(organizationId)
                  && record.disclosureRecordRef().equals(disclosureRecordRef)
                  && record.status() == DisclosureStatus.IDENTITY_DISCLOSED)) {
        hideNextIdentityDisclosureLookup = false;
        return Optional.empty();
      }
      return appendedRecords.stream()
          .filter(record -> record.organizationId().equals(organizationId))
          .filter(record -> record.disclosureRecordRef().equals(disclosureRecordRef))
          .findFirst();
    }

    @Override
    public Optional<DisclosureRecord> findByWorkflowEntityId(
        UUID organizationId,
        UUID workflowEntityId) {
      return appendedRecords.stream()
          .filter(record -> record.organizationId().equals(organizationId))
          .filter(record -> ConsentDisclosureWorkflowEntityIds.disclosureEntityId(
              organizationId,
              record.disclosureRecordRef()).equals(workflowEntityId))
          .findFirst();
    }

    @Override
    public DisclosureRecord transitionToIdentityDisclosed(
        UUID organizationId,
        String disclosureRecordRef,
        WorkflowEventId workflowEventId,
        Instant decidedAt) {
      for (int index = 0; index < appendedRecords.size(); index++) {
        DisclosureRecord existing = appendedRecords.get(index);
        if (existing.organizationId().equals(organizationId)
            && existing.disclosureRecordRef().equals(disclosureRecordRef)) {
          DisclosureRecord updated = new DisclosureRecord(
              existing.disclosureRecordRef(),
              existing.organizationId(),
              existing.candidateRef(),
              existing.candidateProfileRef(),
              existing.jobRef(),
              existing.clientRef(),
              DisclosureStatus.IDENTITY_DISCLOSED,
              existing.disclosureLevel(),
              existing.redactionLevel(),
              existing.unlockDecisionRef(),
              existing.consentRecordRef(),
              Optional.of(workflowEventId),
              decidedAt);
          appendedRecords.set(index, updated);
          return updated;
        }
      }
      throw new IllegalStateException("missing disclosure record");
    }

    void simulateIdentityDisclosureDuplicateOnNextAppendAfterMiss() {
      hideNextIdentityDisclosureLookup = true;
      failNextIdentityDisclosureAppendAsDuplicate = true;
    }

    List<DisclosureRecord> appendedRecords() {
      return appendedRecords;
    }
  }

  private static final class InMemoryCandidateWorkflowStatePort
      implements CandidateWorkflowStatePort {

    @Override
    public void transitionToIdentityDisclosed(
        UUID organizationId,
        String candidateRef,
        Instant disclosedAt) {
    }
  }
}
