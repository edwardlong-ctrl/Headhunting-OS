package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class UnlockWorkflowService {

  private final ShortlistService shortlistService;
  private final JobService jobService;
  private final CompanyService companyService;
  private final CandidateProfileService candidateProfileService;
  private final ClientUnlockRequestPort clientUnlockRequestPort;
  private final ConsentRecordPort consentRecordPort;
  private final UnlockDecisionPort unlockDecisionPort;
  private final DisclosureRecordPort disclosureRecordPort;
  private final ConsentDisclosurePrerequisiteEvaluator prerequisiteEvaluator;
  private final ConsentDisclosureService consentDisclosureService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;
  private final CanonicalWriteTransactionBoundary transactionBoundary;
  private final ConsentDisclosureProtectionPolicy protectionPolicy = new ConsentDisclosureProtectionPolicy();

  @Autowired
  public UnlockWorkflowService(
      ShortlistService shortlistService,
      JobService jobService,
      CompanyService companyService,
      CandidateProfileService candidateProfileService,
      ClientUnlockRequestPort clientUnlockRequestPort,
      ConsentRecordPort consentRecordPort,
      UnlockDecisionPort unlockDecisionPort,
      DisclosureRecordPort disclosureRecordPort,
      ConsentDisclosurePrerequisiteEvaluator prerequisiteEvaluator,
      ConsentDisclosureService consentDisclosureService,
      WorkflowTransitionAuditService workflowTransitionAuditService,
      CanonicalWriteTransactionBoundary transactionBoundary) {
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.candidateProfileService = Objects.requireNonNull(
        candidateProfileService,
        "candidateProfileService must not be null");
    this.clientUnlockRequestPort = Objects.requireNonNull(
        clientUnlockRequestPort,
        "clientUnlockRequestPort must not be null");
    this.consentRecordPort = Objects.requireNonNull(consentRecordPort, "consentRecordPort must not be null");
    this.unlockDecisionPort = Objects.requireNonNull(unlockDecisionPort, "unlockDecisionPort must not be null");
    this.disclosureRecordPort = Objects.requireNonNull(
        disclosureRecordPort,
        "disclosureRecordPort must not be null");
    this.prerequisiteEvaluator = Objects.requireNonNull(
        prerequisiteEvaluator,
        "prerequisiteEvaluator must not be null");
    this.consentDisclosureService = Objects.requireNonNull(
        consentDisclosureService,
        "consentDisclosureService must not be null");
    this.workflowTransitionAuditService = Objects.requireNonNull(
        workflowTransitionAuditService,
        "workflowTransitionAuditService must not be null");
    this.transactionBoundary = Objects.requireNonNull(
        transactionBoundary,
        "transactionBoundary must not be null");
  }

  public UnlockWorkflowResult createClientRequest(
      UUID organizationId,
      UUID clientActorId,
      Shortlist shortlist,
      ShortlistCandidateCard card,
      String anonymousCardRef,
      String requestReason) {
    return transactionBoundary.run(() -> createClientRequestInternal(
        organizationId,
        clientActorId,
        shortlist,
        card,
        anonymousCardRef,
        requestReason));
  }

  private UnlockWorkflowResult createClientRequestInternal(
      UUID organizationId,
      UUID clientActorId,
      Shortlist shortlist,
      ShortlistCandidateCard card,
      String anonymousCardRef,
      String requestReason) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(clientActorId, "clientActorId must not be null");
    Objects.requireNonNull(shortlist, "shortlist must not be null");
    Objects.requireNonNull(card, "card must not be null");
    String normalizedReason = requireNonBlank(requestReason, "requestReason");

    Optional<ClientUnlockRequest> existing = clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
        organizationId,
        shortlist.shortlistId(),
        card.shortlistCandidateCardId());
    Instant now = Instant.now();
    List<UnlockBlocker> blockers =
        evaluateBlockers(organizationId, shortlist, card, clientActorId, now);
    if (existing.isPresent()
        && (existing.orElseThrow().status() == ClientUnlockRequestStatus.REQUESTED
        || existing.orElseThrow().status() == ClientUnlockRequestStatus.UNDER_REVIEW
        || existing.orElseThrow().status() == ClientUnlockRequestStatus.APPROVED)) {
      return UnlockWorkflowResult.from(existing.orElseThrow(), blockers);
    }
    if (!blockers.isEmpty()) {
      return UnlockWorkflowResult.blocked(blockers);
    }

    UUID unlockWorkflowEntityId = UUID.randomUUID();
    ClientUnlockRequest created = ClientUnlockRequest.builder()
        .clientUnlockRequestId(new ClientUnlockRequestId(UUID.randomUUID()))
        .workflowEntityId(unlockWorkflowEntityId)
        .organizationId(organizationId)
        .shortlistId(shortlist.shortlistId())
        .shortlistCandidateCardId(card.shortlistCandidateCardId())
        .jobId(shortlist.jobId().value())
        .clientActorId(clientActorId)
        .anonymousCandidateCardRef(anonymousCardRef)
        .requestReason(normalizedReason)
        .status(ClientUnlockRequestStatus.REQUESTED)
        .version(1)
        .createdAt(now)
        .updatedAt(now)
        .build();
    recordWorkflowEvent(
        created.organizationId(),
        WorkflowEntityType.UNLOCK_REQUEST,
        created.workflowEntityId(),
        created.version(),
        WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED,
        ActorRole.CLIENT,
        clientActorId,
        "not_disclosed",
        "requested",
        normalizedReason,
        "client_api",
        created.clientUnlockRequestId().value(),
        now);
    created = clientUnlockRequestPort.create(created);
    return UnlockWorkflowResult.from(created, blockers);
  }

  public List<UnlockReviewItem> listPendingRequests(UUID organizationId, UUID consultantActorId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(consultantActorId, "consultantActorId must not be null");
    return latestRequestsByCard(organizationId).values().stream()
        .filter(request -> request.status() == ClientUnlockRequestStatus.REQUESTED
            || request.status() == ClientUnlockRequestStatus.UNDER_REVIEW)
        .map(request -> toReviewItem(organizationId, request))
        .sorted(Comparator.comparing(UnlockReviewItem::createdAt).reversed())
        .toList();
  }

  public UnlockApprovalResult approveRequest(
      UUID organizationId,
      UUID consultantActorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId cardId,
      String decisionReason) {
    return transactionBoundary.run(() ->
        decideRequest(organizationId, consultantActorId, shortlistId, cardId, true, decisionReason));
  }

  public UnlockApprovalResult rejectRequest(
      UUID organizationId,
      UUID consultantActorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId cardId,
      String decisionReason) {
    return transactionBoundary.run(() ->
        decideRequest(organizationId, consultantActorId, shortlistId, cardId, false, decisionReason));
  }

  private UnlockApprovalResult decideRequest(
      UUID organizationId,
      UUID consultantActorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId cardId,
      boolean approved,
      String decisionReason) {
    String normalizedReason = requireNonBlank(decisionReason, "decisionReason");
    ClientUnlockRequest latestRequest = clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
            organizationId,
            shortlistId,
            cardId)
        .orElseThrow(() -> new IllegalArgumentException("unlock_request_not_found"));
    if (latestRequest.status() == ClientUnlockRequestStatus.APPROVED
        || latestRequest.status() == ClientUnlockRequestStatus.REJECTED) {
      return UnlockApprovalResult.fromLatest(latestRequest, List.of());
    }

    Shortlist shortlist = shortlistService.findShortlistByIdAndOrganizationId(organizationId, shortlistId)
        .orElseThrow(() -> new IllegalArgumentException("shortlist_not_found"));
    ShortlistCandidateCard card = shortlistService.findCardByIdAndOrganizationId(organizationId, cardId)
        .filter(existingCard -> existingCard.shortlistId().equals(shortlistId))
        .orElseThrow(() -> new IllegalArgumentException("shortlist_card_not_found"));

    Instant now = Instant.now();
    List<UnlockBlocker> blockers =
        evaluateBlockers(organizationId, shortlist, card, latestRequest.clientActorId(), now);
    if (approved && !blockers.isEmpty()) {
      return UnlockApprovalResult.blocked(blockers);
    }

    UnlockDecision decision = new UnlockDecision(
        UUID.randomUUID().toString(),
        organizationId,
        card.candidateId().value().toString(),
        card.candidateProfileId().toString(),
        shortlist.jobId().value().toString(),
        latestRequest.clientActorId().toString(),
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        approved ? UnlockDecisionStatus.APPROVED : UnlockDecisionStatus.DENIED,
        approved ? DisclosureReviewStatus.HUMAN_APPROVED : DisclosureReviewStatus.REJECTED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        new com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef(consultantActorId, ActorRole.CONSULTANT),
        now);

    String approvedDisclosureRef = null;
    if (approved) {
      ConsentRecord consentRecord = requiredConsentRecord(organizationId, card, shortlist, now);
      DisclosureRecord approvedDisclosure = new DisclosureRecord(
          UUID.randomUUID().toString(),
          organizationId,
          card.candidateId().value().toString(),
          card.candidateProfileId().toString(),
          shortlist.jobId().value().toString(),
          latestRequest.clientActorId().toString(),
          DisclosureStatus.CONSULTANT_APPROVED,
          DisclosureLevel.L4_IDENTITY_DISCLOSED,
          RedactionLevel.L4_IDENTITY_DISCLOSED,
          decision.unlockDecisionRef(),
          consentRecord.consentRecordRef(),
          Optional.empty(),
          now);
      List<UnlockBlocker> releaseBlockers = evaluateReleaseBlockers(
          organizationId,
          latestRequest.clientActorId(),
          consultantActorId,
          shortlist,
          card,
          consentRecord,
          decision,
          approvedDisclosure,
          normalizedReason,
          now);
      if (!releaseBlockers.isEmpty()) {
        return UnlockApprovalResult.blocked(releaseBlockers);
      }
      approvedDisclosureRef = approvedDisclosure.disclosureRecordRef();
      recordWorkflowEvent(
          organizationId,
          WorkflowEntityType.DISCLOSURE,
          ConsentDisclosureWorkflowEntityIds.disclosureEntityId(
              organizationId,
              approvedDisclosureRef),
          null,
          WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED,
          ActorRole.CONSULTANT,
          consultantActorId,
          "requested",
          "consultant_approved",
          normalizedReason,
          "consultant_api",
          latestRequest.clientUnlockRequestId().value(),
          now);
      decision = unlockDecisionPort.append(decision);
      approvedDisclosure = disclosureRecordPort.append(approvedDisclosure);
      ConsentDisclosureServiceResult disclosureResult =
          consentDisclosureService.evaluateDisclosureAttempt(ConsentDisclosureServiceRequest.builder()
          .organizationId(organizationId)
          .candidateRef(card.candidateId().value().toString())
          .candidateProfileRef(card.candidateProfileId().toString())
          .clientRef(latestRequest.clientActorId().toString())
          .jobRef(shortlist.jobId().value().toString())
          .requestedByRole(com.recruitingtransactionos.coreapi.identityaccess.PortalRole.CONSULTANT)
          .actor(new com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef(
              consultantActorId,
              ActorRole.CONSULTANT))
          .requestedLevel(DisclosureLevel.L4_IDENTITY_DISCLOSED)
          .prerequisites(new ConsentDisclosurePrerequisites(true, true, true, true, true))
          .reason(normalizedReason)
          .requestedAt(now)
          .consentRecordRef(consentRecord.consentRecordRef())
          .unlockDecisionRef(decision.unlockDecisionRef())
          .approvedDisclosureRecordRef(approvedDisclosure.disclosureRecordRef())
          .build());
      if (disclosureResult.status() != ConsentDisclosureServiceStatus.ALLOWED) {
        throw new IllegalStateException("disclosure_release_preflight_mismatch");
      }
      unlockShortlist(shortlist, card, consultantActorId);
    } else {
      decision = unlockDecisionPort.append(decision);
    }

    ClientUnlockRequest updated = ClientUnlockRequest.builder()
        .clientUnlockRequestId(new ClientUnlockRequestId(UUID.randomUUID()))
        .workflowEntityId(latestRequest.workflowEntityId())
        .organizationId(organizationId)
        .shortlistId(shortlistId)
        .shortlistCandidateCardId(cardId)
        .jobId(shortlist.jobId().value())
        .clientActorId(latestRequest.clientActorId())
        .anonymousCandidateCardRef(latestRequest.anonymousCandidateCardRef())
        .requestReason(latestRequest.requestReason())
        .status(approved ? ClientUnlockRequestStatus.APPROVED : ClientUnlockRequestStatus.REJECTED)
        .unlockDecisionRef(decision.unlockDecisionRef())
        .approvedDisclosureRecordRef(approvedDisclosureRef)
        .version(latestRequest.version() + 1)
        .createdAt(now)
        .updatedAt(now)
        .build();
    recordWorkflowEvent(
        organizationId,
        WorkflowEntityType.UNLOCK_REQUEST,
        updated.workflowEntityId(),
        updated.version(),
        approved ? WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED
            : WorkflowActionCode.DISCLOSURE_UNLOCK_REJECTED,
        ActorRole.CONSULTANT,
        consultantActorId,
        latestRequest.status().wireValue(),
        updated.status().wireValue(),
        normalizedReason,
        "consultant_api",
        latestRequest.clientUnlockRequestId().value(),
        now);
    updated = clientUnlockRequestPort.create(updated);
    return UnlockApprovalResult.fromLatest(updated, blockers);
  }

  private void unlockShortlist(
      Shortlist shortlist,
      ShortlistCandidateCard card,
      UUID consultantActorId) {
    if (card.status() != ShortlistCandidateCardStatus.UNLOCKED) {
      shortlistService.updateCandidateCard(ShortlistCandidateCard.builder()
          .shortlistCandidateCardId(card.shortlistCandidateCardId())
          .organizationId(card.organizationId())
          .shortlistId(card.shortlistId())
          .anonymousCandidateCardId(card.anonymousCandidateCardId())
          .candidateId(card.candidateId())
          .candidateProfileId(card.candidateProfileId())
          .sortOrder(card.sortOrder())
          .status(ShortlistCandidateCardStatus.UNLOCKED)
          .matchReportId(card.matchReportId())
          .clientNotes(card.clientNotes())
          .metadata(card.metadata())
          .createdAt(card.createdAt())
          .updatedAt(Instant.now())
          .version(card.version())
          .build());
    }
    if (shortlist.status() != ShortlistStatus.CONTACT_UNLOCKED) {
      Shortlist updated = shortlistService.updateShortlist(Shortlist.builder()
          .shortlistId(shortlist.shortlistId())
          .organizationId(shortlist.organizationId())
          .jobId(shortlist.jobId())
          .title(shortlist.title())
          .status(ShortlistStatus.CONTACT_UNLOCKED)
          .sentAt(shortlist.sentAt())
          .clientViewedAt(shortlist.clientViewedAt())
          .ownerConsultantId(shortlist.ownerConsultantId())
          .metadata(shortlist.metadata())
          .createdAt(shortlist.createdAt())
          .updatedAt(Instant.now())
          .version(shortlist.version())
          .build());
      workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
          .organizationId(updated.organizationId())
          .entityNamespace("recruiting")
          .entityType(WorkflowEntityType.SHORTLIST.wireValue())
          .entityId(updated.shortlistId().value())
          .entityVersion(updated.version())
          .actionCode(WorkflowActionCode.SHORTLIST_CONTACT_UNLOCKED.wireValue())
          .actorType(ActorRole.CONSULTANT)
          .actorId(consultantActorId)
          .aiInvolvement(WorkflowAiInvolvement.NONE)
          .beforeState(snapshot(shortlist.status().wireValue()))
          .afterState(snapshot(ShortlistStatus.CONTACT_UNLOCKED.wireValue()))
          .reason("consultant approved identity disclosure")
          .sourceType("consultant_api")
          .sourceRefId(updated.shortlistId().value())
          .occurredAt(updated.updatedAt())
          .build());
    }
  }

  private UnlockReviewItem toReviewItem(UUID organizationId, ClientUnlockRequest request) {
    Shortlist shortlist = shortlistService.findShortlistByIdAndOrganizationId(organizationId, request.shortlistId())
        .orElseThrow(() -> new IllegalArgumentException("shortlist_not_found"));
    ShortlistCandidateCard card = shortlistService.findCardByIdAndOrganizationId(
            organizationId,
            request.shortlistCandidateCardId())
        .orElseThrow(() -> new IllegalArgumentException("shortlist_card_not_found"));
    Job job = jobService.findJobByIdAndOrganizationId(organizationId, shortlist.jobId())
        .orElseThrow(() -> new IllegalArgumentException("job_not_found"));
    Optional<Company> clientCompany = findClientCompany(organizationId, request.clientActorId());
    Optional<ConsentRecord> latestConsent = consentRecordPort.findLatestByCandidateProfileAndJob(
        organizationId,
        card.candidateId().value().toString(),
        card.candidateProfileId().toString(),
        shortlist.jobId().value().toString());
    return new UnlockReviewItem(
        request.clientUnlockRequestId().value().toString(),
        request.shortlistId().value().toString(),
        request.shortlistCandidateCardId().value().toString(),
        request.status().wireValue(),
        request.requestReason(),
        request.createdAt(),
        request.anonymousCandidateCardRef(),
        job.title(),
        clientCompany.map(Company::name).orElse("Client company"),
        latestConsent.map(record -> record.status().wireValue()).orElse("missing"),
        evaluateBlockers(organizationId, shortlist, card, request.clientActorId(), Instant.now()));
  }

  private Map<String, ClientUnlockRequest> latestRequestsByCard(UUID organizationId) {
    Map<String, ClientUnlockRequest> latestByCard = new LinkedHashMap<>();
    for (Shortlist shortlist : shortlistService.findAllShortlistsByOrganizationId(organizationId)) {
      for (ShortlistCandidateCard card : shortlistService.findCardsByShortlistIdAndOrganizationId(
          organizationId,
          shortlist.shortlistId())) {
        clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
                organizationId,
                shortlist.shortlistId(),
                card.shortlistCandidateCardId())
            .ifPresent(request -> latestByCard.put(
                shortlist.shortlistId().value() + ":" + card.shortlistCandidateCardId().value(),
                request));
      }
    }
    return latestByCard;
  }

  private Optional<Company> findClientCompany(UUID organizationId, UUID actorId) {
    return companyService.findAllCompaniesByOrganizationId(organizationId).stream()
        .filter(company -> company.metadata() != null
            && company.metadata().contains(actorId.toString()))
        .findFirst();
  }

  private ConsentRecord requiredConsentRecord(
      UUID organizationId,
      ShortlistCandidateCard card,
      Shortlist shortlist,
      Instant requestedAt) {
    return consentRecordPort.findLatestByCandidateProfileAndJob(
            organizationId,
            card.candidateId().value().toString(),
            card.candidateProfileId().toString(),
            shortlist.jobId().value().toString())
        .filter(record -> record.isConfirmedFor(DisclosureLevel.L4_IDENTITY_DISCLOSED, requestedAt))
        .filter(record -> profileVersionMatches(organizationId, card, record))
        .orElseThrow(() -> new IllegalArgumentException("confirmed_consent_required"));
  }

  private List<UnlockBlocker> evaluateBlockers(
      UUID organizationId,
      Shortlist shortlist,
      ShortlistCandidateCard card,
      UUID clientActorId,
      Instant requestedAt) {
    List<UnlockBlocker> blockers = new ArrayList<>();
    Optional<ConsentRecord> consent = consentRecordPort.findLatestByCandidateProfileAndJob(
        organizationId,
            card.candidateId().value().toString(),
            card.candidateProfileId().toString(),
        shortlist.jobId().value().toString());
    if (consent.isEmpty()) {
      blockers.add(new UnlockBlocker("CONSENT_MISSING", "候选人尚未确认 consent。"));
      return blockers;
    }
    ConsentRecord latestConsent = consent.orElseThrow();
    if (latestConsent.revoked()) {
      blockers.add(new UnlockBlocker("CONSENT_REVOKED", "候选人已撤回 consent。"));
    }
    if (latestConsent.status() != ConsentStatus.CONFIRMED) {
      blockers.add(new UnlockBlocker("CONSENT_NOT_CONFIRMED", "候选人 consent 仍未确认。"));
    }
    if (latestConsent.isExpiredAt(requestedAt)) {
      blockers.add(new UnlockBlocker("CONSENT_EXPIRED", "候选人 consent 已过期。"));
    }
    if (!latestConsent.permittedDisclosureLevels().contains(DisclosureLevel.L4_IDENTITY_DISCLOSED)) {
      blockers.add(new UnlockBlocker("CONSENT_SCOPE_EXCLUDES_L4", "当前 consent 未授权身份披露。"));
    }
    Optional<com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile> profile =
        candidateProfileService.findCandidateProfileByIdAndOrganizationId(
            organizationId,
            new CandidateProfileId(card.candidateProfileId()));
    if (profile.isPresent()
        && !latestConsent.profileVersion().equals(Integer.toString(profile.orElseThrow().profileVersion().value()))) {
      blockers.add(new UnlockBlocker("CONSENT_PROFILE_VERSION_MISMATCH", "候选人 consent 绑定的 profile version 已过期。"));
    }
    ConsentDisclosurePrerequisites prerequisites = prerequisiteEvaluator.evaluate(
        ConsentDisclosureServiceRequest.builder()
            .organizationId(organizationId)
            .candidateRef(card.candidateId().value().toString())
            .candidateProfileRef(card.candidateProfileId().toString())
            .clientRef(clientActorId.toString())
            .jobRef(shortlist.jobId().value().toString())
            .requestedByRole(com.recruitingtransactionos.coreapi.identityaccess.PortalRole.CONSULTANT)
            .actor(new com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef(
                UUID.randomUUID(),
                ActorRole.CONSULTANT))
            .requestedLevel(DisclosureLevel.L4_IDENTITY_DISCLOSED)
            .prerequisites(new ConsentDisclosurePrerequisites(true, true, true, true, true))
            .reason("unlock_precheck")
            .requestedAt(requestedAt)
            .consentRecordRef(latestConsent.consentRecordRef())
            .unlockDecisionRef("precheck")
            .approvedDisclosureRecordRef("precheck")
            .build(),
        Optional.empty(),
        Optional.empty());
    if (!prerequisites.jobActivated()) {
      blockers.add(new UnlockBlocker("JOB_NOT_ACTIVE", "岗位尚未激活，不能进入 identity disclosure。"));
    }
    if (!prerequisites.feeAgreementActive()) {
      blockers.add(new UnlockBlocker("FEE_PROTECTION_REVIEW_REQUIRED", "Fee protection 尚未就绪。"));
    }
    if (!prerequisites.priorContactCleared()) {
      blockers.add(new UnlockBlocker("PRIOR_CONTACT_REVIEW_REQUIRED", "存在 prior contact，需要顾问人工判定。"));
    }
    if (!prerequisites.priorApplicationCleared()) {
      blockers.add(new UnlockBlocker("PRIOR_APPLICATION_REVIEW_REQUIRED", "存在 prior application，需要顾问人工判定。"));
    }
    return blockers;
  }

  private boolean profileVersionMatches(
      UUID organizationId,
      ShortlistCandidateCard card,
      ConsentRecord consentRecord) {
    return candidateProfileService.findCandidateProfileByIdAndOrganizationId(
            organizationId,
            new CandidateProfileId(card.candidateProfileId()))
        .map(profile -> consentRecord.profileVersion().equals(
            Integer.toString(profile.profileVersion().value())))
        .orElse(false);
  }

  private List<UnlockBlocker> evaluateReleaseBlockers(
      UUID organizationId,
      UUID clientActorId,
      UUID consultantActorId,
      Shortlist shortlist,
      ShortlistCandidateCard card,
      ConsentRecord consentRecord,
      UnlockDecision decision,
      DisclosureRecord disclosureRecord,
      String reason,
      Instant requestedAt) {
    ConsentDisclosureServiceRequest request = ConsentDisclosureServiceRequest.builder()
        .organizationId(organizationId)
        .candidateRef(card.candidateId().value().toString())
        .candidateProfileRef(card.candidateProfileId().toString())
        .clientRef(clientActorId.toString())
        .jobRef(shortlist.jobId().value().toString())
        .requestedByRole(com.recruitingtransactionos.coreapi.identityaccess.PortalRole.CONSULTANT)
        .actor(new com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef(
            consultantActorId,
            ActorRole.CONSULTANT))
        .requestedLevel(DisclosureLevel.L4_IDENTITY_DISCLOSED)
        .prerequisites(new ConsentDisclosurePrerequisites(true, true, true, true, true))
        .reason(reason)
        .requestedAt(requestedAt)
        .consentRecordRef(consentRecord.consentRecordRef())
        .unlockDecisionRef(decision.unlockDecisionRef())
        .approvedDisclosureRecordRef(disclosureRecord.disclosureRecordRef())
        .build();
    UnlockDisclosureDecision releaseDecision = protectionPolicy.decide(new UnlockDisclosureRequest(
        request.organizationId(),
        request.candidateRef(),
        request.candidateProfileRef(),
        request.jobRef(),
        request.clientRef(),
        request.requestedByRole(),
        request.requestedLevel(),
        Optional.of(consentRecord),
        Optional.of(disclosureRecord),
        Optional.of(decision),
        Optional.of(new DisclosureAuditBoundary(
            WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED,
            RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
            Optional.empty())),
        requestedAt));
    if (releaseDecision.status() == UnlockDisclosureDecisionStatus.DENIED) {
      return blockersFromReasonCodes(releaseDecision.reasonCodes());
    }
    ConsentDisclosurePrerequisites prerequisites = prerequisiteEvaluator.evaluate(
        request,
        Optional.of(decision),
        Optional.of(disclosureRecord));
    return blockersFromReasonCodes(blockingGateReasons(prerequisites, request.requestedLevel()));
  }

  private List<UnlockBlocker> blockersFromReasonCodes(List<String> reasonCodes) {
    if (reasonCodes.isEmpty()) {
      return List.of();
    }
    return reasonCodes.stream()
        .map(this::toUnlockBlocker)
        .toList();
  }

  private UnlockBlocker toUnlockBlocker(String reasonCode) {
    return switch (reasonCode) {
      case "consent_expired" -> new UnlockBlocker("CONSENT_EXPIRED", "候选人 consent 已过期。");
      case "consent_scope_excludes_requested_level" ->
          new UnlockBlocker("CONSENT_SCOPE_EXCLUDES_L4", "当前 consent 未授权身份披露。");
      case "missing_confirmed_consent", "consent_not_confirmed" ->
          new UnlockBlocker("CONSENT_NOT_CONFIRMED", "候选人 consent 仍未确认。");
      case "consent_revoked" ->
          new UnlockBlocker("CONSENT_REVOKED", "候选人已撤回 consent。");
      case "job_activation_gate_required" ->
          new UnlockBlocker("JOB_NOT_ACTIVE", "岗位尚未激活，不能进入 identity disclosure。");
      case "fee_agreement_gate_required" ->
          new UnlockBlocker("FEE_PROTECTION_REVIEW_REQUIRED", "Fee protection 尚未就绪。");
      case "prior_contact_review_required" ->
          new UnlockBlocker("PRIOR_CONTACT_REVIEW_REQUIRED", "存在 prior contact，需要顾问人工判定。");
      case "prior_application_review_required" ->
          new UnlockBlocker("PRIOR_APPLICATION_REVIEW_REQUIRED", "存在 prior application，需要顾问人工判定。");
      case "privacy_risk_gate_required" ->
          new UnlockBlocker("PRIVACY_RISK_GATE_REQUIRED", "Identity disclosure 风险尚未通过最终门禁。");
      default -> new UnlockBlocker(reasonCode.toUpperCase(), "Identity disclosure 未通过最终保护策略。");
    };
  }

  private static List<String> blockingGateReasons(
      ConsentDisclosurePrerequisites prerequisites,
      DisclosureLevel requestedLevel) {
    if (!requestedLevel.requiresUnlockAndDisclosure()) {
      return List.of();
    }
    List<String> reasons = new ArrayList<>();
    if (!prerequisites.jobActivated()) {
      reasons.add("job_activation_gate_required");
    }
    if (!prerequisites.feeAgreementActive()) {
      reasons.add("fee_agreement_gate_required");
    }
    if (!prerequisites.priorContactCleared()) {
      reasons.add("prior_contact_review_required");
    }
    if (!prerequisites.priorApplicationCleared()) {
      reasons.add("prior_application_review_required");
    }
    if (!prerequisites.privacyRiskCleared()) {
      reasons.add("privacy_risk_gate_required");
    }
    return reasons;
  }

  private void recordWorkflowEvent(
      UUID organizationId,
      WorkflowEntityType entityType,
      UUID entityId,
      Integer entityVersion,
      WorkflowActionCode actionCode,
      ActorRole actorRole,
      UUID actorId,
      String beforeStatus,
      String afterStatus,
      String reason,
      String sourceType,
      UUID sourceRefId,
      Instant occurredAt) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(organizationId)
        .entityNamespace("workflow")
        .entityType(entityType.wireValue())
        .entityId(entityId)
        .entityVersion(entityVersion)
        .actionCode(actionCode.wireValue())
        .actorType(actorRole)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(snapshot(beforeStatus))
        .afterState(snapshot(afterStatus))
        .reason(reason)
        .sourceType(sourceType)
        .sourceRefId(sourceRefId)
        .occurredAt(occurredAt)
        .build());
  }

  private static WorkflowStateSnapshot snapshot(String status) {
    return new WorkflowStateSnapshot("{\"status\":\"" + status + "\"}");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }

  public record UnlockBlocker(String code, String message) {
    public UnlockBlocker {
      code = requireNonBlank(code, "code");
      message = requireNonBlank(message, "message");
    }
  }

  public record UnlockWorkflowResult(
      ClientUnlockRequest unlockRequest,
      List<UnlockBlocker> blockers,
      boolean created) {

    public UnlockWorkflowResult {
      blockers = List.copyOf(Objects.requireNonNull(blockers, "blockers must not be null"));
    }

    static UnlockWorkflowResult from(ClientUnlockRequest request, List<UnlockBlocker> blockers) {
      return new UnlockWorkflowResult(request, blockers, true);
    }

    static UnlockWorkflowResult blocked(List<UnlockBlocker> blockers) {
      return new UnlockWorkflowResult(null, blockers, false);
    }
  }

  public record UnlockReviewItem(
      String unlockRequestId,
      String shortlistId,
      String shortlistCandidateCardId,
      String status,
      String requestReason,
      Instant createdAt,
      String anonymousCandidateCardRef,
      String jobTitle,
      String clientCompanyName,
      String consentStatus,
      List<UnlockBlocker> blockers) {

    public UnlockReviewItem {
      unlockRequestId = requireNonBlank(unlockRequestId, "unlockRequestId");
      shortlistId = requireNonBlank(shortlistId, "shortlistId");
      shortlistCandidateCardId = requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
      status = requireNonBlank(status, "status");
      requestReason = requireNonBlank(requestReason, "requestReason");
      Objects.requireNonNull(createdAt, "createdAt must not be null");
      anonymousCandidateCardRef = requireNonBlank(anonymousCandidateCardRef, "anonymousCandidateCardRef");
      jobTitle = requireNonBlank(jobTitle, "jobTitle");
      clientCompanyName = requireNonBlank(clientCompanyName, "clientCompanyName");
      consentStatus = requireNonBlank(consentStatus, "consentStatus");
      blockers = List.copyOf(Objects.requireNonNull(blockers, "blockers must not be null"));
    }
  }

  public record UnlockApprovalResult(
      ClientUnlockRequest unlockRequest,
      List<UnlockBlocker> blockers) {

    public UnlockApprovalResult {
      blockers = List.copyOf(Objects.requireNonNull(blockers, "blockers must not be null"));
    }

    static UnlockApprovalResult fromLatest(ClientUnlockRequest request, List<UnlockBlocker> blockers) {
      return new UnlockApprovalResult(request, blockers);
    }

    static UnlockApprovalResult blocked(List<UnlockBlocker> blockers) {
      return new UnlockApprovalResult(null, blockers);
    }
  }
}
