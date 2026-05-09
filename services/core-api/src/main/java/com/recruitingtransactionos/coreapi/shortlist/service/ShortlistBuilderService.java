package com.recruitingtransactionos.coreapi.shortlist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateProjectionService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientVisibleCandidateFieldPolicy;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessment;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessmentService;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consultantmatching.StoredMatchReport;
import com.recruitingtransactionos.coreapi.consultantmatching.port.MatchReportPersistencePort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.matching.MatchDimension;
import com.recruitingtransactionos.coreapi.privacyredaction.RedactionAuditService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardViewMetadata;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ShortlistBuilderService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String PROJECTION_VERSION = "task29-shortlist-v1";
  private static final AccessRequest CLIENT_SAFE_CARD_ACCESS = new AccessRequest(
      PortalRole.CLIENT,
      ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
      AccessAction.READ,
      FieldClassification.CLIENT_SAFE,
      Set.of(RelationshipScope.SAME_ORGANIZATION),
      false);

  private final ShortlistService shortlistService;
  private final CandidateService candidateService;
  private final CandidateProfileService candidateProfileService;
  private final MatchReportPersistencePort matchReportPersistencePort;
  private final ConsentRecordPort consentRecordPort;
  private final JobService jobService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;
  private final ClientSafeCandidateProjectionService clientSafeProjectionService;
  private final ReidentificationRiskAssessmentService reidentificationRiskAssessmentService;
  private final RedactionAuditService redactionAuditService;

  public ShortlistBuilderService(
      ShortlistService shortlistService,
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      MatchReportPersistencePort matchReportPersistencePort,
      ConsentRecordPort consentRecordPort,
      JobService jobService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService,
        new ClientSafeCandidateProjectionService(),
        new ReidentificationRiskAssessmentService(),
        null);
  }

  public ShortlistBuilderService(
      ShortlistService shortlistService,
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      MatchReportPersistencePort matchReportPersistencePort,
      ConsentRecordPort consentRecordPort,
      JobService jobService,
      WorkflowTransitionAuditService workflowTransitionAuditService,
      ClientSafeCandidateProjectionService clientSafeProjectionService,
      ReidentificationRiskAssessmentService reidentificationRiskAssessmentService,
      RedactionAuditService redactionAuditService) {
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.candidateService = Objects.requireNonNull(candidateService, "candidateService must not be null");
    this.candidateProfileService = Objects.requireNonNull(
        candidateProfileService, "candidateProfileService must not be null");
    this.matchReportPersistencePort = Objects.requireNonNull(
        matchReportPersistencePort, "matchReportPersistencePort must not be null");
    this.consentRecordPort = Objects.requireNonNull(
        consentRecordPort, "consentRecordPort must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.workflowTransitionAuditService = Objects.requireNonNull(
        workflowTransitionAuditService, "workflowTransitionAuditService must not be null");
    this.clientSafeProjectionService = Objects.requireNonNull(
        clientSafeProjectionService, "clientSafeProjectionService must not be null");
    this.reidentificationRiskAssessmentService = Objects.requireNonNull(
        reidentificationRiskAssessmentService,
        "reidentificationRiskAssessmentService must not be null");
    this.redactionAuditService = redactionAuditService;
  }

  private record ProjectedAnonymousCard(
      ClientSafeCandidateCard card,
      String reidentificationRiskSignal) {
  }

  private record ProjectionAssessment(
      InternalCandidateProjectionSnapshot redactedSnapshot,
      ReidentificationRiskAssessment assessment,
      boolean blocked) {
  }

  public ShortlistBuilderState getBuilderState(UUID organizationId, ShortlistId shortlistId) {
    Shortlist shortlist = requireShortlist(organizationId, shortlistId);
    return stateFor(shortlist, shortlistService.findCardsByShortlistIdAndOrganizationId(
        organizationId, shortlistId));
  }

  public ShortlistBuilderState addCandidateCard(
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      CandidateId candidateId,
      Integer requestedSortOrder,
      String clientNotes) {
    Shortlist shortlist = requireShortlist(organizationId, shortlistId);
    requireMutableBuilderShortlist(shortlist);
    Candidate candidate = candidateService.findCandidateByIdAndOrganizationId(organizationId, candidateId)
        .orElseThrow(() -> new IllegalArgumentException("candidate_not_found_in_organization"));
    CandidateProfile profile = candidateProfileService
        .findCandidateProfileByCandidateIdAndOrganizationId(organizationId, candidateId)
        .orElseThrow(() -> new IllegalArgumentException("candidate_profile_not_found_in_organization"));

    List<ShortlistCandidateCard> existingCards =
        shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlistId);
    boolean alreadyIncluded = existingCards.stream()
        .anyMatch(card -> card.candidateId().equals(candidateId)
            && card.status() != ShortlistCandidateCardStatus.REMOVED);
    if (alreadyIncluded) {
      throw new IllegalArgumentException("candidate_already_in_shortlist");
    }

    Optional<StoredMatchReport> matchReport =
        matchReportPersistencePort.findLatestByCandidateIdAndJobId(
            organizationId, shortlist.jobId(), candidateId.value());
    UUID anonymousCardId = UUID.randomUUID();
    ProjectedAnonymousCard projectedCard = projectAnonymousCard(
        shortlist, candidate, profile, matchReport, anonymousCardId, actorId);
    ShortlistCandidateCardViewMetadata viewMetadata =
        buildViewMetadata(projectedCard.card(), matchReport.orElse(null), projectedCard.reidentificationRiskSignal());

    Instant now = Instant.now();
    ShortlistCandidateCard persisted = shortlistService.addCandidateCard(ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(UUID.randomUUID()))
        .organizationId(organizationId)
        .shortlistId(shortlistId)
        .anonymousCandidateCardId(anonymousCardId)
        .candidateId(candidateId)
        .candidateProfileId(profile.candidateProfileId().value())
        .sortOrder(resolveSortOrder(existingCards, requestedSortOrder))
        .status(ShortlistCandidateCardStatus.INCLUDED)
        .matchReportId(matchReport
            .map(report -> uuidFromOpaqueRef(report.matchReport().matchReportId().value(), "match_report_"))
            .orElse(null))
        .clientNotes(safeClientNotes(clientNotes))
        .metadata(writeMetadata(viewMetadata))
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build());

    auditCandidateShortlisted(candidate, shortlist, persisted, actorId);
    transitionJobToShortlistInProgressIfNeeded(organizationId, actorId, shortlist.jobId().value());
    return stateFor(shortlist, refreshCards(organizationId, shortlistId));
  }

  public ShortlistBuilderState updateCandidateCard(
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId shortlistCandidateCardId,
      int expectedVersion,
      Integer sortOrder,
      ShortlistCandidateCardStatus status,
      String clientNotes) {
    Shortlist shortlist = requireShortlist(organizationId, shortlistId);
    requireMutableBuilderShortlist(shortlist);
    requireBuilderEditableCardStatus(status);
    ShortlistCandidateCard existing = shortlistService.findCardByIdAndOrganizationId(
            organizationId, shortlistCandidateCardId)
        .orElseThrow(() -> new IllegalArgumentException("shortlist_candidate_card_not_found"));
    if (!existing.shortlistId().equals(shortlistId)) {
      throw new IllegalArgumentException("shortlist_candidate_card_not_found");
    }
    if (existing.version() != expectedVersion) {
      throw new IllegalArgumentException("shortlist_candidate_card_version_conflict");
    }
    ShortlistCandidateCard updated = shortlistService.updateCandidateCard(ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(existing.shortlistCandidateCardId())
        .organizationId(existing.organizationId())
        .shortlistId(existing.shortlistId())
        .anonymousCandidateCardId(existing.anonymousCandidateCardId())
        .candidateId(existing.candidateId())
        .candidateProfileId(existing.candidateProfileId())
        .sortOrder(sortOrder != null ? sortOrder : existing.sortOrder())
        .status(status != null ? status : existing.status())
        .matchReportId(existing.matchReportId())
        .clientNotes(clientNotes != null ? safeClientNotes(clientNotes) : existing.clientNotes())
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(Instant.now())
        .version(existing.version())
        .build());
    auditShortlistCardCompositionChange(shortlist, existing, updated, actorId);
    List<ShortlistCandidateCard> refreshedCards = refreshCards(organizationId, shortlistId).stream()
        .map(card -> card.shortlistCandidateCardId().equals(updated.shortlistCandidateCardId()) ? updated : card)
        .sorted(Comparator.comparingInt(ShortlistCandidateCard::sortOrder))
        .toList();
    return stateFor(shortlist, refreshedCards);
  }

  public ShortlistBuilderState sendToClient(
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId) {
    Shortlist existing = requireShortlist(organizationId, shortlistId);
    List<ShortlistCandidateCard> cards = refreshCards(organizationId, shortlistId);
    ShortlistBuilderState state = stateFor(existing, cards);
    if (!state.canSend() || !allIncludedCardsPassCurrentRiskGate(existing, cards, actorId)) {
      throw new IllegalArgumentException("shortlist_send_blocked");
    }
    Instant now = Instant.now();
    Shortlist sent = Shortlist.builder()
        .shortlistId(existing.shortlistId())
        .organizationId(existing.organizationId())
        .jobId(existing.jobId())
        .title(existing.title())
        .status(ShortlistStatus.SENT_TO_CLIENT)
        .sentAt(now)
        .clientViewedAt(existing.clientViewedAt())
        .ownerConsultantId(existing.ownerConsultantId())
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(now)
        .version(existing.version())
        .build();

    auditShortlistTransition(
        sent, existing.status().wireValue(), ShortlistStatus.SENT_TO_CLIENT.wireValue(),
        WorkflowActionCode.SHORTLIST_SENT_TO_CLIENT, actorId,
        "consultant approved shortlist send after pre-send checks");
    sent = shortlistService.updateShortlist(sent);
    transitionJobToShortlistSent(organizationId, actorId, sent.jobId().value());
    return stateFor(sent, cards);
  }

  private Shortlist requireShortlist(UUID organizationId, ShortlistId shortlistId) {
    return shortlistService.findShortlistByIdAndOrganizationId(organizationId, shortlistId)
        .orElseThrow(() -> new IllegalArgumentException("shortlist_not_found_in_organization"));
  }

  private List<ShortlistCandidateCard> refreshCards(UUID organizationId, ShortlistId shortlistId) {
    return shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlistId);
  }

  private ShortlistBuilderState stateFor(Shortlist shortlist, List<ShortlistCandidateCard> cards) {
    List<ShortlistCandidateCard> orderedCards = cards.stream()
        .sorted(Comparator.comparingInt(ShortlistCandidateCard::sortOrder))
        .toList();
    return new ShortlistBuilderState(
        shortlist,
        orderedCards,
        buildChecks(shortlist, orderedCards),
        buildDeliveryPreview(shortlist, orderedCards));
  }

  private List<ShortlistPreSendCheck> buildChecks(
      Shortlist shortlist, List<ShortlistCandidateCard> cards) {
    List<ShortlistCandidateCard> includedCards = includedCards(cards);
    List<ShortlistCandidateCardViewMetadata> includedMetadata = includedCards.stream()
        .map(this::metadataFor)
        .flatMap(Optional::stream)
        .toList();
    boolean readyForReview = hasReachedReviewGateStatus(shortlist.status());
    boolean hasIncludedCards = !includedCards.isEmpty();
    boolean everyIncludedCardHasAnonymousProjection = hasIncludedCards
        && includedMetadata.size() == includedCards.size();
    boolean deliveryPreviewReady = hasIncludedCards
        && includedMetadata.size() == includedCards.size()
        && includedMetadata.stream().allMatch(metadata -> !metadata.safeSummary().isBlank());
    boolean everyIncludedCardPassesRiskGate = hasIncludedCards && includedCards.stream()
        .allMatch(this::passesReidentificationRiskGate);
    return List.of(
        new ShortlistPreSendCheck("status_ready_for_review", "Shortlist status is ready for review",
            readyForReview),
        new ShortlistPreSendCheck("has_included_cards", "Shortlist includes at least one candidate",
            hasIncludedCards),
        new ShortlistPreSendCheck(
            "anonymous_cards_generated", "All included cards have anonymous client-safe summaries",
            everyIncludedCardHasAnonymousProjection),
        new ShortlistPreSendCheck(
            "delivery_preview_ready", "Client-safe delivery preview can be generated",
            deliveryPreviewReady),
        new ShortlistPreSendCheck(
            "reidentification_risk_within_gate",
            "All included cards remain below the client-send re-identification risk threshold",
            everyIncludedCardPassesRiskGate));
  }

  private ShortlistDeliveryPreview buildDeliveryPreview(
      Shortlist shortlist, List<ShortlistCandidateCard> cards) {
    List<ShortlistCandidateCard> includedCards = includedCards(cards);
    List<ShortlistCandidateCardViewMetadata> metadata = includedCards.stream()
        .map(this::metadataFor)
        .flatMap(Optional::stream)
        .toList();
    int candidateCount = metadata.size();
    String role = shortlist.title() != null && !shortlist.title().isBlank()
        ? shortlist.title().strip()
        : "this role";
    if (metadata.isEmpty() || metadata.size() != includedCards.size()) {
      String empty = "No client-safe shortlist summary is available until at least one candidate card is included.";
      return new ShortlistDeliveryPreview(empty, empty, empty, empty);
    }
    String firstHeadline = metadata.get(0).generalizedHeadline();
    String clientSafeSummary = "Consultant-reviewed shortlist for " + role + " with "
        + candidateCount + " anonymous talent summaries. Identity remains protected until unlock.";
    String pdfSummary = "PDF placeholder: deliver " + candidateCount
        + " anonymous talent cards for " + role + ", led by " + firstHeadline + ".";
    String emailSummary = "Email placeholder: " + candidateCount
        + " consultant-reviewed anonymous talent summaries are ready for client review for " + role + ".";
    String wechatSummary = "WeChat placeholder: shortlist ready with " + candidateCount
        + " anonymous candidates for " + role + ".";
    return new ShortlistDeliveryPreview(clientSafeSummary, pdfSummary, emailSummary, wechatSummary);
  }

  private ProjectedAnonymousCard projectAnonymousCard(
      Shortlist shortlist,
      Candidate candidate,
      CandidateProfile profile,
      Optional<StoredMatchReport> matchReport,
      UUID anonymousCardId,
      UUID actorId) {
    ClientSafeCandidateCard seedCard = new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of(toOpaqueCardId(anonymousCardId)),
        AnonymousCandidateRef.of(toOpaqueCandidateRef(candidate.candidateId())),
        PROJECTION_VERSION,
        RedactionLevel.L2_CLIENT_SAFE,
        generalizedHeadline(shortlist),
        generalizedRoleFamily(shortlist),
        generalizedSeniorityBand(shortlist),
        "Location shared after identity unlock",
        "Evidence-backed talent overview reviewed by a consultant for this role. Identity remains protected until unlock.",
        safeSkillSummary(matchReport.orElse(null)),
        safeEvidenceSummaries(matchReport.orElse(null)),
        safeMatchNarratives(matchReport.orElse(null)));
    if (redactionAuditService == null) {
      return new ProjectedAnonymousCard(
          seedCard,
          matchReport.map(report -> report.reidentificationRiskSignal().wireValue()).orElse("not_assessed"));
    }
    InternalCandidateProjectionSnapshot snapshot = buildProjectionSnapshot(
        shortlist,
        candidate,
        profile,
        matchReport.orElse(null),
        seedCard);
    ProjectionAssessment projectionAssessment = evaluateProjection(
        shortlist,
        candidate,
        snapshot,
        actorId);
    if (projectionAssessment.blocked()) {
      throw new IllegalArgumentException("shortlist_candidate_card_reidentification_blocked");
    }
    return new ProjectedAnonymousCard(
        clientSafeProjectionService.project(CLIENT_SAFE_CARD_ACCESS, projectionAssessment.redactedSnapshot()),
        projectionAssessment.assessment().riskLevel().wireValue());
  }

  private ShortlistCandidateCardViewMetadata buildViewMetadata(
      ClientSafeCandidateCard projectedCard,
      StoredMatchReport matchReport,
      String reidentificationRiskSignal) {
    return new ShortlistCandidateCardViewMetadata(
        projectedCard.anonymousCandidateRef().value(),
        projectedCard.projectionVersion(),
        projectedCard.redactionLevel().wireValue(),
        projectedCard.generalizedHeadline(),
        projectedCard.generalizedRoleFamily(),
        projectedCard.generalizedSeniorityBand(),
        projectedCard.generalizedLocationRegion(),
        projectedCard.safeSummary(),
        projectedCard.safeSkillSummary(),
        projectedCard.safeEvidenceSummaries(),
        projectedCard.safeMatchNarratives(),
        matchReport == null ? null : matchReport.matchReport().overallScore().value(),
        matchReport == null ? "unknown" : matchReport.matchReport().scoreConfidence().wireValue(),
        reidentificationRiskSignal,
        dimensionScoreItems(matchReport));
  }

  private InternalCandidateProjectionSnapshot buildProjectionSnapshot(
      Shortlist shortlist,
      Candidate candidate,
      CandidateProfile profile,
      StoredMatchReport matchReport,
      ClientSafeCandidateCard seedCard) {
    return new InternalCandidateProjectionSnapshot(
        candidate.candidateId().value().toString(),
        profile.candidateProfileId().value().toString(),
        fieldText(profile, "identity.full_name"),
        fieldText(profile, "contact.email"),
        fieldText(profile, "contact.phone"),
        null,
        fieldText(profile, "experience.current_company"),
        fieldList(profile, "experience.projects"),
        null,
        fieldText(profile, "metadata.notes"),
        seedCard.cardId(),
        seedCard.anonymousCandidateRef(),
        seedCard.projectionVersion(),
        seedCard.redactionLevel(),
        seedCard.generalizedHeadline(),
        seedCard.generalizedRoleFamily(),
        seedCard.generalizedSeniorityBand(),
        seedCard.generalizedLocationRegion(),
        seedCard.safeSummary(),
        seedCard.safeSkillSummary(),
        seedCard.safeEvidenceSummaries(),
        seedCard.safeMatchNarratives(),
        ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths());
  }

  private ProjectionAssessment evaluateProjection(
      Shortlist shortlist,
      Candidate candidate,
      InternalCandidateProjectionSnapshot snapshot,
      UUID actorId) {
    if (redactionAuditService != null) {
      RedactionAuditService.RedactionAuditResult auditResult = redactionAuditService.evaluate(
          new RedactionAuditService.RedactionAuditRequest(
              shortlist.organizationId(),
              redactionAssessmentRef(snapshot.cardId()),
              candidate.candidateId().value().toString(),
              shortlist.jobId().value().toString(),
              snapshot,
              actorId,
              ActorRole.CONSULTANT,
              "shortlist_builder",
              "shortlist anonymous candidate card projected",
              Instant.now()));
      return new ProjectionAssessment(
          auditResult.redactedSnapshot(),
          auditResult.assessment(),
          auditResult.blocked());
    }
    ReidentificationRiskAssessmentService.PipelineResult pipelineResult =
        reidentificationRiskAssessmentService.assessWithPipeline(snapshot);
    return new ProjectionAssessment(
        pipelineResult.pipeline().redactedSnapshot(),
        pipelineResult.assessment(),
        pipelineResult.assessment().decision()
            == com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskDecision.BLOCK);
  }

  private String redactionAssessmentRef(AnonymousCandidateCardId cardId) {
    return "shortlist_redaction_"
        + cardId.value()
        + "_"
        + UUID.randomUUID().toString().replace("-", "");
  }

  private List<ShortlistCandidateCardViewMetadata.DimensionScoreItem> dimensionScoreItems(
      StoredMatchReport matchReport) {
    if (matchReport == null) {
      return List.of();
    }
    List<ShortlistCandidateCardViewMetadata.DimensionScoreItem> items = new ArrayList<>();
    for (MatchDimension dimension : MatchDimension.values()) {
      items.add(new ShortlistCandidateCardViewMetadata.DimensionScoreItem(
          dimension.wireValue(),
          matchReport.matchReport().dimensionScores().get(dimension).value()));
    }
    return List.copyOf(items);
  }

  private String generalizedHeadline(Shortlist shortlist) {
    String role = shortlist.title() != null && !shortlist.title().isBlank()
        ? shortlist.title().strip()
        : "this role";
    return "Consultant-reviewed candidate for " + role;
  }

  private String generalizedRoleFamily(Shortlist shortlist) {
    return shortlist.title() != null && !shortlist.title().isBlank()
        ? shortlist.title().strip()
        : "Confidential role family";
  }

  private String generalizedSeniorityBand(Shortlist shortlist) {
    return shortlist.status() == ShortlistStatus.SENT_TO_CLIENT
        ? "Client-ready shortlist level"
        : "Consultant-reviewed shortlist level";
  }

  private String safeSkillSummary(StoredMatchReport matchReport) {
    if (matchReport == null) {
      return "Core skill evidence is available and summarized for consultant review.";
    }
    return "Skill and evidence coverage reviewed with "
        + matchReport.matchReport().evidenceCoverage().coverageLevel().wireValue()
        + " coverage confidence.";
  }

  private List<String> safeEvidenceSummaries(StoredMatchReport matchReport) {
    if (matchReport == null) {
      return List.of(
          "Consultant-reviewed evidence available for this talent summary.",
          "Identity remains protected until unlock is approved.");
    }
    return List.of(
        "Overall match score: " + matchReport.matchReport().overallScore().value() + "/5.",
        "Evidence coverage: "
            + matchReport.matchReport().evidenceCoverage().coverageLevel().wireValue() + ".",
        "Independent evidence items: "
            + matchReport.matchReport().evidenceCoverage().independentEvidenceCount() + ".");
  }

  private List<String> safeMatchNarratives(StoredMatchReport matchReport) {
    if (matchReport == null) {
      return List.of("Consultant review prepared a client-safe comparison narrative placeholder.");
    }
    return matchReport.matchReport().dimensionScores().entrySet().stream()
        .sorted(Map.Entry.comparingByKey(Comparator.comparing(MatchDimension::wireValue)))
        .map(entry -> entry.getKey().wireValue() + ": " + entry.getValue().value() + "/5")
        .toList();
  }

  private boolean allIncludedCardsPassCurrentRiskGate(
      Shortlist shortlist,
      List<ShortlistCandidateCard> cards,
      UUID actorId) {
    if (redactionAuditService == null) {
      return includedCards(cards).stream().allMatch(this::passesReidentificationRiskGate);
    }
    for (ShortlistCandidateCard card : includedCards(cards)) {
      Candidate candidate = candidateService.findCandidateByIdAndOrganizationId(
              shortlist.organizationId(),
              card.candidateId())
          .orElseThrow(() -> new IllegalArgumentException("candidate_not_found_in_organization"));
      CandidateProfile profile = candidateProfileService.findCandidateProfileByIdAndOrganizationId(
              shortlist.organizationId(),
              new CandidateProfileId(card.candidateProfileId()))
          .orElseThrow(() -> new IllegalArgumentException("candidate_profile_not_found_in_organization"));
      Optional<StoredMatchReport> matchReport = matchReportPersistencePort.findLatestByCandidateIdAndJobId(
          shortlist.organizationId(),
          shortlist.jobId(),
          card.candidateId().value());
      ClientSafeCandidateCard seedCard = new ClientSafeCandidateCard(
          AnonymousCandidateCardId.of(toOpaqueCardId(card.anonymousCandidateCardId())),
          AnonymousCandidateRef.of(toOpaqueCandidateRef(candidate.candidateId())),
          PROJECTION_VERSION,
          RedactionLevel.L2_CLIENT_SAFE,
          generalizedHeadline(shortlist),
          generalizedRoleFamily(shortlist),
          generalizedSeniorityBand(shortlist),
          "Location shared after identity unlock",
          "Evidence-backed talent overview reviewed by a consultant for this role. Identity remains protected until unlock.",
          safeSkillSummary(matchReport.orElse(null)),
          safeEvidenceSummaries(matchReport.orElse(null)),
          safeMatchNarratives(matchReport.orElse(null)));
      ProjectionAssessment assessment = evaluateProjection(
          shortlist,
          candidate,
          buildProjectionSnapshot(shortlist, candidate, profile, matchReport.orElse(null), seedCard),
          actorId);
      if (assessment.blocked()) {
        return false;
      }
    }
    return true;
  }

  private int resolveSortOrder(List<ShortlistCandidateCard> existingCards, Integer requestedSortOrder) {
    if (requestedSortOrder != null && requestedSortOrder >= 0) {
      return requestedSortOrder;
    }
    return existingCards.stream()
        .mapToInt(ShortlistCandidateCard::sortOrder)
        .max()
        .orElse(-1) + 1;
  }

  private List<ShortlistCandidateCard> includedCards(List<ShortlistCandidateCard> cards) {
    return cards.stream()
        .filter(card -> card.status() == ShortlistCandidateCardStatus.INCLUDED)
        .toList();
  }

  private Optional<ShortlistCandidateCardViewMetadata> metadataFor(ShortlistCandidateCard card) {
    try {
      if (card.metadata() == null || card.metadata().isBlank()) {
        return Optional.empty();
      }
      return Optional.of(OBJECT_MAPPER.readValue(card.metadata(), ShortlistCandidateCardViewMetadata.class));
    } catch (JsonProcessingException exception) {
      return Optional.empty();
    }
  }

  private String writeMetadata(ShortlistCandidateCardViewMetadata metadata) {
    try {
      return OBJECT_MAPPER.writeValueAsString(metadata);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("shortlist_card_metadata_serialization_failed", exception);
    }
  }

  private boolean passesReidentificationRiskGate(ShortlistCandidateCard card) {
    return metadataFor(card)
        .map(ShortlistCandidateCardViewMetadata::reidentificationRiskSignal)
        .filter(signal -> signal != null && !signal.isBlank())
        .map(String::strip)
        .map(String::toLowerCase)
        .filter(signal -> !signal.equals("high"))
        .filter(signal -> !signal.equals("unknown"))
        .filter(signal -> !signal.equals("not_assessed"))
        .isPresent();
  }

  private void transitionJobToShortlistInProgressIfNeeded(
      UUID organizationId, UUID actorId, UUID jobIdValue) {
    Job job = jobService.findJobByIdAndOrganizationId(
            organizationId, new com.recruitingtransactionos.coreapi.job.JobId(jobIdValue))
        .orElseThrow(() -> new IllegalArgumentException("job_not_found_in_organization"));
    if (job.status() != JobStatus.ACTIVATED) {
      return;
    }
    Instant now = Instant.now();
    Job updated = copyJobWithStatus(job, JobStatus.SHORTLIST_IN_PROGRESS, now);
    auditJobTransition(
        updated, JobStatus.ACTIVATED.wireValue(), JobStatus.SHORTLIST_IN_PROGRESS.wireValue(),
        WorkflowActionCode.JOB_SHORTLIST_IN_PROGRESS, actorId,
        "first shortlist candidate card added");
    jobService.updateJob(updated);
  }

  private void transitionJobToShortlistSent(UUID organizationId, UUID actorId, UUID jobIdValue) {
    Job job = jobService.findJobByIdAndOrganizationId(
            organizationId, new com.recruitingtransactionos.coreapi.job.JobId(jobIdValue))
        .orElseThrow(() -> new IllegalArgumentException("job_not_found_in_organization"));
    if (job.status() == JobStatus.SHORTLIST_SENT) {
      return;
    }
    Instant now = Instant.now();
    String beforeStatus = job.status().wireValue();
    Job updated = copyJobWithStatus(job, JobStatus.SHORTLIST_SENT, now);
    auditJobTransition(
        updated, beforeStatus, JobStatus.SHORTLIST_SENT.wireValue(),
        WorkflowActionCode.JOB_SHORTLIST_SENT, actorId,
        "shortlist moved to sent_to_client");
    jobService.updateJob(updated);
  }

  private Job copyJobWithStatus(Job existing, JobStatus status, Instant now) {
    return Job.builder()
        .jobId(existing.jobId())
        .organizationId(existing.organizationId())
        .companyId(existing.companyId())
        .title(existing.title())
        .description(existing.description())
        .location(existing.location())
        .seniorityBand(existing.seniorityBand())
        .roleFamily(existing.roleFamily())
        .employmentType(existing.employmentType())
        .compensation(existing.compensation())
        .status(status)
        .commercialTerms(existing.commercialTerms())
        .ownerConsultantId(existing.ownerConsultantId())
        .activatedAt(existing.activatedAt())
        .closedAt(existing.closedAt())
        .closeReason(existing.closeReason())
        .industryPackId(existing.industryPackId())
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(now)
        .version(existing.version())
        .build();
  }

  private void auditShortlistTransition(
      Shortlist shortlist,
      String beforeStatus,
      String afterStatus,
      WorkflowActionCode actionCode,
      UUID actorId,
      String reason) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(shortlist.organizationId())
        .entityNamespace("recruiting")
        .entityType(WorkflowEntityType.SHORTLIST.wireValue())
        .entityId(shortlist.shortlistId().value())
        .entityVersion(shortlist.version())
        .actionCode(actionCode.wireValue())
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(snapshot(beforeStatus))
        .afterState(snapshot(afterStatus))
        .reason(reason)
        .sourceType("shortlist_builder")
        .sourceRefId(shortlist.shortlistId().value())
        .occurredAt(Instant.now())
        .build());
  }

  private void auditJobTransition(
      Job job,
      String beforeStatus,
      String afterStatus,
      WorkflowActionCode actionCode,
      UUID actorId,
      String reason) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(job.organizationId())
        .entityNamespace("recruiting")
        .entityType(WorkflowEntityType.JOB.wireValue())
        .entityId(job.jobId().value())
        .entityVersion(job.version())
        .actionCode(actionCode.wireValue())
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(snapshot(beforeStatus))
        .afterState(snapshot(afterStatus))
        .reason(reason)
        .sourceType("shortlist_builder")
        .sourceRefId(job.jobId().value())
        .occurredAt(Instant.now())
        .build());
  }

  private void auditCandidateShortlisted(
      Candidate candidate,
      Shortlist shortlist,
      ShortlistCandidateCard card,
      UUID actorId) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(candidate.organizationId())
        .entityNamespace("workflow")
        .entityType(WorkflowEntityType.CANDIDATE.wireValue())
        .entityId(ConsentDisclosureWorkflowEntityIds.candidateEntityId(
            candidate.organizationId(),
            candidate.candidateId().value().toString()))
        .entityVersion(candidate.version())
        .actionCode(WorkflowActionCode.CANDIDATE_SHORTLISTED.wireValue())
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(snapshot(candidate.status().wireValue()))
        .afterState(snapshot("shortlisted"))
        .reason("candidate added to shortlist " + shortlist.shortlistId().value())
        .sourceType("shortlist_builder")
        .sourceRefId(card.shortlistCandidateCardId().value())
        .occurredAt(Instant.now())
        .build());
  }

  private void auditShortlistCardCompositionChange(
      Shortlist shortlist,
      ShortlistCandidateCard existing,
      ShortlistCandidateCard updated,
      UUID actorId) {
    if (workflowTransitionAuditService == null || existing.status() == updated.status()) {
      return;
    }
    WorkflowActionCode actionCode = switch (updated.status()) {
      case REMOVED -> existing.status() == ShortlistCandidateCardStatus.INCLUDED
          ? WorkflowActionCode.SHORTLIST_CARD_REMOVED
          : null;
      case INCLUDED -> existing.status() == ShortlistCandidateCardStatus.REMOVED
          ? WorkflowActionCode.SHORTLIST_CARD_RESTORED
          : null;
      default -> null;
    };
    if (actionCode == null) {
      return;
    }
    String reason = actionCode == WorkflowActionCode.SHORTLIST_CARD_REMOVED
        ? "candidate card removed from shortlist " + shortlist.shortlistId().value()
        : "candidate card restored to shortlist " + shortlist.shortlistId().value();
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(shortlist.organizationId())
        .entityNamespace("recruiting")
        .entityType(WorkflowEntityType.SHORTLIST.wireValue())
        .entityId(shortlist.shortlistId().value())
        .entityVersion(shortlist.version())
        .actionCode(actionCode.wireValue())
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(cardCompositionSnapshot(shortlist.status().wireValue(), existing.status().wireValue()))
        .afterState(cardCompositionSnapshot(shortlist.status().wireValue(), updated.status().wireValue()))
        .reason(reason)
        .sourceType("shortlist_builder")
        .sourceRefId(updated.shortlistCandidateCardId().value())
        .occurredAt(Instant.now())
        .build());
  }

  private WorkflowStateSnapshot snapshot(String status) {
    return new WorkflowStateSnapshot("{\"status\":\"" + status + "\"}");
  }

  private WorkflowStateSnapshot cardCompositionSnapshot(
      String shortlistStatus,
      String cardStatus) {
    return new WorkflowStateSnapshot(
        "{\"status\":\"" + shortlistStatus + "\",\"cardStatus\":\"" + cardStatus + "\"}");
  }

  private boolean hasReachedReviewGateStatus(ShortlistStatus status) {
    return switch (status) {
      case DRAFT -> false;
      case READY_FOR_REVIEW,
          SENT_TO_CLIENT,
          CLIENT_VIEWED,
          CLIENT_FEEDBACK_PENDING,
          CANDIDATE_SELECTED,
          CONTACT_UNLOCKED,
          INTERVIEWING,
          CLOSED -> true;
    };
  }

  private void requireMutableBuilderShortlist(Shortlist shortlist) {
    if (shortlist.status() != ShortlistStatus.DRAFT
        && shortlist.status() != ShortlistStatus.READY_FOR_REVIEW) {
      throw new IllegalStateException("shortlist_builder_locked_after_send");
    }
  }

  private void requireBuilderEditableCardStatus(ShortlistCandidateCardStatus status) {
    if (status == null) {
      return;
    }
    if (status != ShortlistCandidateCardStatus.INCLUDED
        && status != ShortlistCandidateCardStatus.REMOVED) {
      throw new IllegalArgumentException("shortlist_builder_card_status_requires_dedicated_workflow");
    }
  }

  private UUID uuidFromOpaqueRef(String value, String prefix) {
    String compact = value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    if (compact.length() != 32) {
      throw new IllegalArgumentException("opaque_ref_uuid_payload_required");
    }
    return UUID.fromString(
        compact.substring(0, 8) + "-"
            + compact.substring(8, 12) + "-"
            + compact.substring(12, 16) + "-"
            + compact.substring(16, 20) + "-"
            + compact.substring(20));
  }

  private String safeClientNotes(String clientNotes) {
    if (clientNotes == null) {
      return null;
    }
    return clientNotes.strip();
  }

  private String fieldText(CandidateProfile profile, String fieldPath) {
    return profile.fields().stream()
        .filter(field -> field.fieldPath().value().equals(fieldPath))
        .findFirst()
        .map(CandidateProfileField::value)
        .map(value -> value.jsonValue())
        .map(this::jsonTextValue)
        .orElse("");
  }

  private List<String> fieldList(CandidateProfile profile, String fieldPath) {
    return profile.fields().stream()
        .filter(field -> field.fieldPath().value().equals(fieldPath))
        .findFirst()
        .map(CandidateProfileField::value)
        .map(value -> value.jsonValue())
        .map(this::jsonStringList)
        .orElse(List.of());
  }

  private String jsonTextValue(String rawJson) {
    try {
      JsonNode node = OBJECT_MAPPER.readTree(rawJson);
      if (node.isTextual()) {
        return node.asText("");
      }
      if (node.hasNonNull("value")) {
        return node.get("value").asText("");
      }
      return "";
    } catch (JsonProcessingException exception) {
      return "";
    }
  }

  private List<String> jsonStringList(String rawJson) {
    try {
      JsonNode node = OBJECT_MAPPER.readTree(rawJson);
      List<String> values = new ArrayList<>();
      if (node.isArray()) {
        for (JsonNode child : node) {
          if (child.isTextual()) {
            values.add(child.asText());
          }
        }
      } else if (node.has("items") && node.get("items").isArray()) {
        for (JsonNode child : node.get("items")) {
          if (child.isTextual()) {
            values.add(child.asText());
          } else if (child.hasNonNull("value")) {
            values.add(child.get("value").asText());
          }
        }
      }
      return List.copyOf(values);
    } catch (JsonProcessingException exception) {
      return List.of();
    }
  }

  private String toOpaqueCardId(UUID cardId) {
    return "card_" + cardId.toString().replace("-", "");
  }

  private String toOpaqueCandidateRef(CandidateId candidateId) {
    String compact = candidateId.value().toString().replace("-", "");
    return "anon_candidate_" + compact.substring(Math.max(0, compact.length() - 12));
  }
}
