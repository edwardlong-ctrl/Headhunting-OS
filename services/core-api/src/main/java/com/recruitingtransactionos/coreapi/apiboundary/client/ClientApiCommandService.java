package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ClientInterviewFeedbackResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientPreferenceResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientShortlistCandidateSelectionResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientUnlockRequestResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientCompanyProfileResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientJobSubmissionStatusResponse;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import com.recruitingtransactionos.coreapi.company.CompanyPreferenceId;
import com.recruitingtransactionos.coreapi.company.service.CompanyIntakeApplicationService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestId;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.InteractionStatus;
import com.recruitingtransactionos.coreapi.interaction.InteractionType;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackDecision;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewOutcome;
import com.recruitingtransactionos.coreapi.interviewfeedback.RejectReasonTaxonomy;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackOutcomeLoopResult;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackOutcomeLoopService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardViewMetadata;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientApiCommandService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Set<ShortlistStatus> CLIENT_VISIBLE_SHORTLIST_STATUSES = Set.of(
      ShortlistStatus.SENT_TO_CLIENT,
      ShortlistStatus.CLIENT_VIEWED,
      ShortlistStatus.CLIENT_FEEDBACK_PENDING,
      ShortlistStatus.CANDIDATE_SELECTED,
      ShortlistStatus.CONTACT_UNLOCKED,
      ShortlistStatus.INTERVIEWING,
      ShortlistStatus.CLOSED);
  private static final Set<ShortlistStatus> CLIENT_MUTABLE_SELECTION_SHORTLIST_STATUSES = Set.of(
      ShortlistStatus.SENT_TO_CLIENT,
      ShortlistStatus.CLIENT_VIEWED,
      ShortlistStatus.CLIENT_FEEDBACK_PENDING,
      ShortlistStatus.CANDIDATE_SELECTED);
  private static final Set<ShortlistStatus> CLIENT_MUTABLE_UNLOCK_REQUEST_SHORTLIST_STATUSES = Set.of(
      ShortlistStatus.SENT_TO_CLIENT,
      ShortlistStatus.CLIENT_VIEWED,
      ShortlistStatus.CLIENT_FEEDBACK_PENDING,
      ShortlistStatus.CANDIDATE_SELECTED);

  private final CompanyIntakeApplicationService companyIntakeApplicationService;
  private final JobIntakeApplicationService jobIntakeApplicationService;
  private final CompanyService companyService;
  private final JobService jobService;
  private final ShortlistService shortlistService;
  private final ClientUnlockRequestPort clientUnlockRequestPort;
  private final UnlockWorkflowService unlockWorkflowService;
  private final CandidateCompanyInteractionService interactionService;
  private final InterviewFeedbackService interviewFeedbackService;
  private final InterviewFeedbackOutcomeLoopService interviewFeedbackOutcomeLoopService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ClientApiCommandService(
      CompanyIntakeApplicationService companyIntakeApplicationService,
      JobIntakeApplicationService jobIntakeApplicationService,
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService,
      ClientUnlockRequestPort clientUnlockRequestPort,
      UnlockWorkflowService unlockWorkflowService,
      CandidateCompanyInteractionService interactionService,
      InterviewFeedbackService interviewFeedbackService,
      InterviewFeedbackOutcomeLoopService interviewFeedbackOutcomeLoopService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this(
        companyIntakeApplicationService,
        jobIntakeApplicationService,
        companyService,
        jobService,
        shortlistService,
        clientUnlockRequestPort,
        unlockWorkflowService,
        interactionService,
        interviewFeedbackService,
        interviewFeedbackOutcomeLoopService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ClientApiCommandService(
      CompanyIntakeApplicationService companyIntakeApplicationService,
      JobIntakeApplicationService jobIntakeApplicationService,
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService,
      ClientUnlockRequestPort clientUnlockRequestPort,
      UnlockWorkflowService unlockWorkflowService,
      CandidateCompanyInteractionService interactionService,
      InterviewFeedbackService interviewFeedbackService,
      InterviewFeedbackOutcomeLoopService interviewFeedbackOutcomeLoopService,
      WorkflowTransitionAuditService workflowTransitionAuditService,
      PermissionEnforcer permissionEnforcer) {
    this.companyIntakeApplicationService = Objects.requireNonNull(
        companyIntakeApplicationService, "companyIntakeApplicationService must not be null");
    this.jobIntakeApplicationService = Objects.requireNonNull(
        jobIntakeApplicationService, "jobIntakeApplicationService must not be null");
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.clientUnlockRequestPort = Objects.requireNonNull(
        clientUnlockRequestPort, "clientUnlockRequestPort must not be null");
    this.unlockWorkflowService = Objects.requireNonNull(
        unlockWorkflowService, "unlockWorkflowService must not be null");
    this.interactionService = Objects.requireNonNull(interactionService, "interactionService must not be null");
    this.interviewFeedbackService = Objects.requireNonNull(
        interviewFeedbackService, "interviewFeedbackService must not be null");
    this.interviewFeedbackOutcomeLoopService = Objects.requireNonNull(
        interviewFeedbackOutcomeLoopService, "interviewFeedbackOutcomeLoopService must not be null");
    this.workflowTransitionAuditService = Objects.requireNonNull(
        workflowTransitionAuditService, "workflowTransitionAuditService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public ClientCompanyProfileResponse upsertCompanyProfile(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ClientCompanyProfileCreateRequest request) {
    requireClientContext(accessRequest, ResourceType.COMPANY, AccessAction.CREATE, "company");
    Company company = companyIntakeApplicationService.upsertClientProfile(
        organizationId,
        actorId,
        optionalCompanyId(request.companyId()),
        request.name(),
        request.displayName(),
        request.industry(),
        request.website(),
        request.headquartersLocation(),
        request.sizeBand(),
        request.paymentReliability());
    return ClientApiQueryService.toCompanyProfileResponse(company);
  }

  public ClientPreferenceResponse upsertPreferences(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ClientPreferenceUpsertRequest request) {
    requireClientContext(accessRequest, ResourceType.COMPANY, AccessAction.UPDATE, "company");
    Company company = findClientCompany(organizationId, actorId)
        .orElseThrow(() -> new IllegalArgumentException("client_company_profile_not_found"));
    for (ClientPreferenceUpsertRequest.PreferenceItem item : request.preferences()) {
      companyService.upsertPreference(CompanyPreference.builder()
          .companyPreferenceId(new CompanyPreferenceId(UUID.randomUUID()))
          .organizationId(organizationId)
          .companyId(company.companyId())
          .preferenceKey(item.preferenceKey())
          .preferenceValue(item.preferenceValue())
          .notes(item.notes())
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .build());
    }
    return new ClientPreferenceResponse(
        company.companyId().value().toString(),
        companyService.findPreferencesByCompanyIdAndOrganizationId(organizationId, company.companyId())
            .stream()
            .map(preference -> new ClientPreferenceResponse.PreferenceItem(
                preference.preferenceKey(),
                preference.preferenceValue(),
                preference.notes()))
            .toList());
  }

  public ClientJobSubmissionStatusResponse createJobSubmission(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ClientJobIntakeCreateRequest request) {
    requireClientContext(accessRequest, ResourceType.JOB, AccessAction.CREATE, "job");
    Job job = jobIntakeApplicationService.createClientJobSubmission(
        organizationId,
        actorId,
        new CompanyId(UUID.fromString(request.companyId())),
        request.title(),
        request.description(),
        request.location(),
        request.compensation(),
        request.commercialTerms(),
        request.clarificationQuestions());
    return ClientApiQueryService.toJobStatusResponse(
        job,
        jobIntakeApplicationService.activationGate(organizationId, job.jobId()));
  }

  public ClientJobSubmissionStatusResponse answerClarification(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      JobId jobId,
      ClientJobClarificationRequest request) {
    requireClientContext(accessRequest, ResourceType.JOB, AccessAction.UPDATE, "job");
    Job job = jobIntakeApplicationService.answerClarification(
        organizationId,
        actorId,
        jobId,
        request.clarificationAnswers(),
        request.description(),
        request.location(),
        request.compensation(),
        request.commercialTerms());
    return ClientApiQueryService.toJobStatusResponse(
        job,
        jobIntakeApplicationService.activationGate(organizationId, jobId));
  }

  public ClientShortlistDetailResponse viewShortlist(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.UPDATE, "shortlist");
    Shortlist shortlist = requireVisibleShortlist(organizationId, actorId, shortlistId);
    if (shortlist.status() == ShortlistStatus.SENT_TO_CLIENT) {
      shortlist = transitionShortlist(shortlist, ShortlistStatus.CLIENT_VIEWED, actorId,
          WorkflowActionCode.SHORTLIST_VIEWED_BY_CLIENT);
    }
    return buildShortlistDetail(organizationId, shortlist);
  }

  public ClientShortlistCandidateSelectionResponse selectShortlistCandidate(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId cardId) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.UPDATE, "shortlist");
    Shortlist shortlist = requireVisibleShortlist(organizationId, actorId, shortlistId);
    requireSelectionMutableShortlist(shortlist);
    ShortlistCandidateCard card = requireShortlistCard(organizationId, shortlistId, cardId);
    shortlist = ensureSelectionState(shortlist, actorId);
    if (card.status() != ShortlistCandidateCardStatus.SELECTED
        && card.status() != ShortlistCandidateCardStatus.UNLOCKED) {
      card = shortlistService.updateCandidateCard(copyCardWithStatus(card, ShortlistCandidateCardStatus.SELECTED));
    }
    return new ClientShortlistCandidateSelectionResponse(
        shortlist.shortlistId().value().toString(),
        card.shortlistCandidateCardId().value().toString(),
        shortlist.status().wireValue(),
        card.status().wireValue(),
        opaqueCardRef(card));
  }

  public ClientUnlockRequestResponse createUnlockRequest(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId cardId,
      ClientUnlockRequestCreateRequest request) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.UPDATE, "shortlist");
    Shortlist shortlist = requireVisibleShortlist(organizationId, actorId, shortlistId);
    requireUnlockRequestMutableShortlist(shortlist);
    ShortlistCandidateCard card = requireShortlistCard(organizationId, shortlistId, cardId);
    requireSelectedForUnlockRequest(card);
    shortlist = ensureSelectionState(shortlist, actorId);
    var result = unlockWorkflowService.createClientRequest(
        organizationId,
        actorId,
        shortlist,
        card,
        opaqueCardRef(card),
        request.requestReason());
    return toUnlockRequestResponse(
        result.unlockRequest(),
        result.blockers(),
        shortlistId.value().toString(),
        cardId.value().toString(),
        opaqueCardRef(card));
  }

  public ClientInterviewFeedbackResponse submitInterviewFeedback(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId cardId,
      ClientInterviewFeedbackRequest request) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.UPDATE, "shortlist");
    Shortlist shortlist = requireVisibleShortlist(organizationId, actorId, shortlistId);
    ShortlistCandidateCard card = requireShortlistCard(organizationId, shortlistId, cardId);
    requireInterviewFeedbackEligibility(shortlist, card);
    Job job = jobService.findJobByIdAndOrganizationId(organizationId, shortlist.jobId())
        .orElseThrow(() -> new IllegalArgumentException("job_not_found_in_organization"));
    CandidateCompanyInteraction interaction = resolveInterviewInteraction(
        organizationId,
        job,
        card,
        request.interviewId(),
        request.notes());
    InterviewFeedback feedback = interviewFeedbackService.createFeedback(InterviewFeedback.builder()
        .interviewFeedbackId(new InterviewFeedbackId(UUID.randomUUID()))
        .organizationId(organizationId)
        .candidateCompanyInteractionId(interaction.candidateCompanyInteractionId())
        .jobId(job.jobId())
        .interviewerName(request.interviewerName())
        .interviewerRole(request.interviewerRole())
        .interviewRound(request.interviewRound())
        .interviewDate(request.interviewDate() != null && !request.interviewDate().isBlank()
            ? Instant.parse(request.interviewDate())
            : null)
        .outcome(InterviewOutcome.fromWireValue(request.outcome()))
        .decision(InterviewFeedbackDecision.fromWireValue(request.decision()))
        .rejectReasonTaxonomy(request.rejectReasonTaxonomy() != null && !request.rejectReasonTaxonomy().isBlank()
            ? RejectReasonTaxonomy.fromWireValue(request.rejectReasonTaxonomy())
            : null)
        .ratings(writeRatingsJson(request.ratings()))
        .ratingsSchemaVersion("task35-v1")
        .notes(request.notes())
        .strengths(request.strengths())
        .concerns(request.concerns())
        .submittedByRole("client")
        .submittedByUserId(actorId)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());
    String scorecardJson = jobService.findActiveScorecardByJobIdAndOrganizationId(organizationId, job.jobId())
        .map(this::writeScorecardJson)
        .orElse("{}");
    InterviewFeedbackOutcomeLoopResult outcomeLoopResult = interviewFeedbackOutcomeLoopService.processSubmission(
        organizationId,
        actorId,
        shortlist,
        card,
        job,
        interaction,
        feedback,
        scorecardJson);
    InterviewFeedback persistedFeedback = outcomeLoopResult.feedback();
    return new ClientInterviewFeedbackResponse(
        persistedFeedback.interviewFeedbackId().value().toString(),
        interaction.candidateCompanyInteractionId().value().toString(),
        shortlistId.value().toString(),
        cardId.value().toString(),
        persistedFeedback.outcome().wireValue(),
        persistedFeedback.decision() != null ? persistedFeedback.decision().wireValue() : request.decision(),
        persistedFeedback.rejectReasonTaxonomy() != null
            ? persistedFeedback.rejectReasonTaxonomy().wireValue()
            : request.rejectReasonTaxonomy(),
        persistedFeedback.notes(),
        persistedFeedback.strengths(),
        persistedFeedback.concerns(),
        persistedFeedback.ratings(),
        persistedFeedback.interviewRound(),
        persistedFeedback.interviewerName(),
        persistedFeedback.interviewerRole(),
        outcomeLoopResult.structuredSummary(),
        outcomeLoopResult.suggestions().size(),
        outcomeLoopResult.aiStructured(),
        persistedFeedback.createdAt().toString());
  }

  private void requireClientContext(
      AccessRequest accessRequest,
      ResourceType resourceType,
      AccessAction action,
      String label) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != resourceType || accessRequest.action() != action) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          label + "_client_context_required",
          "Client " + label + " API requires a client-safe " + label + " context."));
    }
  }

  private Optional<Company> findClientCompany(UUID organizationId, UUID actorId) {
    return companyService.findAllCompaniesByOrganizationId(organizationId).stream()
        .filter(company -> CompanyIntakeApplicationService.metadataContainsActor(company.metadata(), actorId))
        .findFirst();
  }

  private Shortlist requireVisibleShortlist(UUID organizationId, UUID actorId, ShortlistId shortlistId) {
    List<JobId> visibleJobIds = jobService.findAllJobsByOrganizationId(organizationId).stream()
        .filter(job -> JobIntakeApplicationService.metadataContainsActor(job.metadata(), actorId))
        .map(Job::jobId)
        .toList();
    return shortlistService.findShortlistByIdAndOrganizationId(organizationId, shortlistId)
        .filter(shortlist -> visibleJobIds.contains(shortlist.jobId()))
        .filter(shortlist -> CLIENT_VISIBLE_SHORTLIST_STATUSES.contains(shortlist.status()))
        .orElseThrow(() -> new IllegalArgumentException("shortlist_not_found_in_client_scope"));
  }

  private ShortlistCandidateCard requireShortlistCard(
      UUID organizationId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId cardId) {
    return shortlistService.findCardByIdAndOrganizationId(organizationId, cardId)
        .filter(card -> card.shortlistId().equals(shortlistId))
        .filter(card -> card.status() != ShortlistCandidateCardStatus.REMOVED)
        .orElseThrow(() -> new IllegalArgumentException("shortlist_card_not_found_in_client_scope"));
  }

  private void requireSelectedForUnlockRequest(ShortlistCandidateCard card) {
    if (card.status() != ShortlistCandidateCardStatus.SELECTED
        && card.status() != ShortlistCandidateCardStatus.UNLOCKED) {
      throw new IllegalArgumentException("shortlist_candidate_must_be_selected_before_unlock_request");
    }
  }

  private void requireSelectionMutableShortlist(Shortlist shortlist) {
    if (!CLIENT_MUTABLE_SELECTION_SHORTLIST_STATUSES.contains(shortlist.status())) {
      throw new IllegalArgumentException("shortlist_selection_not_allowed_in_current_status");
    }
  }

  private void requireUnlockRequestMutableShortlist(Shortlist shortlist) {
    if (!CLIENT_MUTABLE_UNLOCK_REQUEST_SHORTLIST_STATUSES.contains(shortlist.status())) {
      throw new IllegalArgumentException("shortlist_unlock_request_not_allowed_in_current_status");
    }
  }

  private void requireInterviewFeedbackEligibility(Shortlist shortlist, ShortlistCandidateCard card) {
    if (!isInterviewFeedbackEligible(shortlist, card)) {
      throw new IllegalArgumentException("interview_feedback_requires_unlocked_candidate_and_post_unlock_shortlist");
    }
  }

  static boolean isInterviewFeedbackEligible(Shortlist shortlist, ShortlistCandidateCard card) {
    boolean shortlistReadyForInterviewFeedback = shortlist.status() == ShortlistStatus.CONTACT_UNLOCKED
        || shortlist.status() == ShortlistStatus.INTERVIEWING;
    boolean cardReadyForInterviewFeedback = card.status() == ShortlistCandidateCardStatus.UNLOCKED;
    return shortlistReadyForInterviewFeedback && cardReadyForInterviewFeedback;
  }

  private Shortlist ensureSelectionState(Shortlist shortlist, UUID actorId) {
    if (shortlist.status() == ShortlistStatus.SENT_TO_CLIENT) {
      Shortlist viewed = transitionShortlist(shortlist, ShortlistStatus.CLIENT_VIEWED, actorId,
          WorkflowActionCode.SHORTLIST_VIEWED_BY_CLIENT);
      return transitionShortlist(viewed, ShortlistStatus.CANDIDATE_SELECTED, actorId,
          WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED);
    }
    if (shortlist.status() == ShortlistStatus.CLIENT_VIEWED) {
      return transitionShortlist(shortlist, ShortlistStatus.CANDIDATE_SELECTED, actorId,
          WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED);
    }
    if (shortlist.status() == ShortlistStatus.CLIENT_FEEDBACK_PENDING) {
      return transitionShortlist(shortlist, ShortlistStatus.CANDIDATE_SELECTED, actorId,
          WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED);
    }
    return shortlist;
  }

  private Shortlist transitionShortlist(
      Shortlist shortlist,
      ShortlistStatus targetStatus,
      UUID actorId,
      WorkflowActionCode actionCode) {
    Shortlist current = shortlistService.findShortlistByIdAndOrganizationIdForUpdate(
            shortlist.organizationId(), shortlist.shortlistId())
        .orElse(shortlist);
    if (current.status() == targetStatus) {
      return current;
    }
    Instant now = Instant.now();
    try {
      workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
          .organizationId(current.organizationId())
          .entityNamespace("recruiting")
          .entityType(WorkflowEntityType.SHORTLIST.wireValue())
          .entityId(current.shortlistId().value())
          .entityVersion(current.version())
          .actionCode(actionCode.wireValue())
          .actorType(ActorRole.CLIENT)
          .actorId(actorId)
          .aiInvolvement(WorkflowAiInvolvement.NONE)
          .beforeState(snapshot(current.status().wireValue()))
          .afterState(snapshot(targetStatus.wireValue()))
          .reason("client transitioned shortlist to " + targetStatus.wireValue())
          .sourceType("client_api")
          .sourceRefId(current.shortlistId().value())
          .occurredAt(now)
          .build());
    } catch (IllegalArgumentException exception) {
      if (isBeforeStateMismatch(exception)) {
        Optional<Shortlist> latest = shortlistService.findShortlistByIdAndOrganizationId(
            current.organizationId(), current.shortlistId());
        if (latest.isPresent() && latest.get().status() == targetStatus) {
          return latest.get();
        }
      }
      throw exception;
    }
    Shortlist updated = shortlistService.updateShortlist(copyShortlistWithStatus(current, targetStatus, now));
    return updated;
  }

  private static boolean isBeforeStateMismatch(IllegalArgumentException exception) {
    String message = exception.getMessage();
    return message != null
        && message.startsWith("workflow transition beforeState does not match actual entity state:");
  }

  private Shortlist copyShortlistWithStatus(Shortlist shortlist, ShortlistStatus status, Instant now) {
    return Shortlist.builder()
        .shortlistId(shortlist.shortlistId())
        .organizationId(shortlist.organizationId())
        .jobId(shortlist.jobId())
        .title(shortlist.title())
        .status(status)
        .sentAt(shortlist.sentAt())
        .clientViewedAt(status == ShortlistStatus.CLIENT_VIEWED && shortlist.clientViewedAt() == null ? now : shortlist.clientViewedAt())
        .ownerConsultantId(shortlist.ownerConsultantId())
        .metadata(shortlist.metadata())
        .createdAt(shortlist.createdAt())
        .updatedAt(now)
        .version(shortlist.version())
        .build();
  }

  private ShortlistCandidateCard copyCardWithStatus(
      ShortlistCandidateCard card,
      ShortlistCandidateCardStatus status) {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(card.shortlistCandidateCardId())
        .organizationId(card.organizationId())
        .shortlistId(card.shortlistId())
        .anonymousCandidateCardId(card.anonymousCandidateCardId())
        .candidateId(card.candidateId())
        .candidateProfileId(card.candidateProfileId())
        .sortOrder(card.sortOrder())
        .status(status)
        .matchReportId(card.matchReportId())
        .clientNotes(card.clientNotes())
        .metadata(card.metadata())
        .createdAt(card.createdAt())
        .updatedAt(Instant.now())
        .version(card.version())
        .build();
  }

  private CandidateCompanyInteraction resolveInterviewInteraction(
      UUID organizationId,
      Job job,
      ShortlistCandidateCard card,
      String interviewId,
      String notes) {
    if (interviewId != null && !interviewId.isBlank()) {
      CandidateCompanyInteraction interaction = interactionService.findInteractionByIdAndOrganizationId(
              organizationId,
              new CandidateCompanyInteractionId(parseUuid(interviewId, "interviewId")))
          .filter(existing -> existing.interactionType() == InteractionType.INTERVIEW)
          .orElseThrow(() -> new IllegalArgumentException("interview_not_found_in_client_scope"));
      if (!card.candidateId().equals(interaction.candidateId())
          || !job.companyId().equals(interaction.companyId())
          || interaction.jobId() == null
          || !job.jobId().equals(interaction.jobId())) {
        throw new IllegalArgumentException("interview_not_found_in_client_scope");
      }
      return interaction;
    }
    return findOrCreateInteraction(organizationId, job, card, notes);
  }

  private CandidateCompanyInteraction findOrCreateInteraction(
      UUID organizationId,
      Job job,
      ShortlistCandidateCard card,
      String notes) {
    return interactionService.findInteractionsByCandidateAndCompany(
            organizationId,
            card.candidateId(),
            job.companyId())
        .stream()
        .filter(interaction -> interaction.jobId() != null && interaction.jobId().equals(job.jobId()))
        .filter(interaction -> interaction.interactionType() == InteractionType.INTERVIEW)
        .findFirst()
        .orElseGet(() -> interactionService.createInteraction(CandidateCompanyInteraction.builder()
            .candidateCompanyInteractionId(new CandidateCompanyInteractionId(UUID.randomUUID()))
            .organizationId(organizationId)
            .candidateId(card.candidateId())
            .companyId(job.companyId())
            .jobId(job.jobId())
            .interactionType(InteractionType.INTERVIEW)
            .status(InteractionStatus.ACTIVE)
            .startedAt(Instant.now())
            .notes(notes)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build()));
  }

  private static UUID parseUuid(String raw, String fieldName) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(fieldName + "_must_be_a_valid_uuid");
    }
  }

  private String writeRatingsJson(List<ClientInterviewFeedbackRequest.DimensionRating> ratings) {
    try {
      return OBJECT_MAPPER.writeValueAsString(ratings == null ? List.of() : ratings);
    } catch (Exception exception) {
      throw new IllegalArgumentException("ratings_serialization_failed");
    }
  }

  private String writeScorecardJson(JobScorecard scorecard) {
    try {
      return OBJECT_MAPPER.writeValueAsString(scorecard);
    } catch (Exception exception) {
      return "{}";
    }
  }

  private ClientShortlistDetailResponse buildShortlistDetail(UUID organizationId, Shortlist shortlist) {
    List<ClientShortlistDetailResponse.Card> cards = shortlistService.findCardsByShortlistIdAndOrganizationId(
            organizationId,
            shortlist.shortlistId())
        .stream()
        .filter(card -> card.status() != ShortlistCandidateCardStatus.REMOVED)
        .sorted(Comparator.comparingInt(ShortlistCandidateCard::sortOrder))
        .map(card -> {
          ShortlistCandidateCardViewMetadata metadata = metadata(card).orElseGet(() -> emptyMetadata());
          Optional<ClientUnlockRequest> latestUnlockRequest =
              clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
                  organizationId,
                  shortlist.shortlistId(),
                  card.shortlistCandidateCardId());
          return new ClientShortlistDetailResponse.Card(
              card.shortlistCandidateCardId().value().toString(),
              opaqueCardRef(card),
              card.status().wireValue(),
              metadata.generalizedHeadline(),
              metadata.generalizedRoleFamily(),
              metadata.generalizedSeniorityBand(),
              metadata.generalizedLocationRegion(),
              metadata.safeSummary(),
              metadata.safeSkillSummary(),
              metadata.overallScore(),
              metadata.confidence(),
              metadata.reidentificationRiskSignal(),
              null,
              latestUnlockRequest.map(request -> request.status().wireValue()).orElse(null),
              latestUnlockRequest.map(ClientUnlockRequest::unlockDecisionRef).orElse(null),
              latestUnlockRequest.map(ClientUnlockRequest::approvedDisclosureRecordRef).orElse(null));
        })
        .toList();
    return new ClientShortlistDetailResponse(
        shortlist.shortlistId().value().toString(),
        shortlist.jobId().value().toString(),
        shortlist.title(),
        shortlist.status().wireValue(),
        shortlist.sentAt() != null ? shortlist.sentAt().toString() : null,
        shortlist.clientViewedAt() != null ? shortlist.clientViewedAt().toString() : null,
        shortlist.createdAt().toString(),
        shortlist.updatedAt().toString(),
        cards);
  }

  private ClientUnlockRequestResponse toUnlockRequestResponse(
      ClientUnlockRequest request,
      List<UnlockWorkflowService.UnlockBlocker> blockers,
      String shortlistId,
      String cardId,
      String anonymousCardRef) {
    return new ClientUnlockRequestResponse(
        request == null ? null : request.clientUnlockRequestId().value().toString(),
        shortlistId,
        cardId,
        anonymousCardRef,
        request == null ? "blocked" : request.status().wireValue(),
        deriveUnlockStage(request, blockers),
        request == null ? "Unlock blocked by consent/disclosure prerequisites." : request.requestReason(),
        request == null ? Instant.now().toString() : request.createdAt().toString(),
        request == null ? Instant.now().toString() : request.updatedAt().toString(),
        request == null ? null : request.unlockDecisionRef(),
        request == null ? null : request.approvedDisclosureRecordRef(),
        blockers.stream()
            .map(blocker -> new ClientUnlockRequestResponse.Blocker(blocker.code(), blocker.message()))
            .toList());
  }

  private static String deriveUnlockStage(
      ClientUnlockRequest request,
      List<UnlockWorkflowService.UnlockBlocker> blockers) {
    if (request == null) {
      return blockers.isEmpty() ? "blocked" : "blocked_precheck";
    }
    return switch (request.status()) {
      case REQUESTED -> "pending_consultant_review";
      case UNDER_REVIEW -> "under_review";
      case APPROVED -> request.approvedDisclosureRecordRef() == null
          ? "approved_pending_disclosure"
          : "identity_disclosed";
      case REJECTED -> "rejected";
      default -> request.status().wireValue();
    };
  }

  private Optional<ShortlistCandidateCardViewMetadata> metadata(ShortlistCandidateCard card) {
    try {
      if (card.metadata() == null || card.metadata().isBlank()) {
        return Optional.empty();
      }
      return Optional.of(OBJECT_MAPPER.readValue(card.metadata(), ShortlistCandidateCardViewMetadata.class));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private ShortlistCandidateCardViewMetadata emptyMetadata() {
    return new ShortlistCandidateCardViewMetadata(
        "anon_candidate_pending",
        "task29-shortlist-v1",
        "l2_client_safe",
        "Consultant-reviewed candidate",
        "Confidential role family",
        "Consultant-reviewed shortlist level",
        "Location shared after identity unlock",
        "Client-safe summary will be generated after shortlist card enrichment.",
        "Skill summary placeholder is pending shortlist card enrichment.",
        List.of("Client-safe evidence summary is pending."),
        List.of("Client-safe comparison narrative is pending."),
        null,
        "unknown",
        "not_assessed",
        List.of());
  }

  private static Optional<CompanyId> optionalCompanyId(String companyId) {
    if (companyId == null || companyId.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new CompanyId(UUID.fromString(companyId)));
  }

  private static WorkflowStateSnapshot snapshot(String status) {
    return new WorkflowStateSnapshot("{\"status\":\"" + status + "\"}");
  }

  private static String opaqueCardRef(ShortlistCandidateCard card) {
    return "card_" + card.anonymousCandidateCardId().toString().replace("-", "");
  }
}
