package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ClientDashboardResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientCompanyProfileResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientInterviewFeedbackContextResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientJobSubmissionStatusResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientPreferenceResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientShortlistSummaryResponse;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import com.recruitingtransactionos.coreapi.company.service.CompanyIntakeApplicationService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequestStatus;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.InteractionType;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.notification.NotificationService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardViewMetadata;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ClientApiQueryService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Set<ShortlistStatus> CLIENT_VISIBLE_SHORTLIST_STATUSES = Set.of(
      ShortlistStatus.SENT_TO_CLIENT,
      ShortlistStatus.CLIENT_VIEWED,
      ShortlistStatus.CLIENT_FEEDBACK_PENDING,
      ShortlistStatus.CANDIDATE_SELECTED,
      ShortlistStatus.CONTACT_UNLOCKED,
      ShortlistStatus.INTERVIEWING,
      ShortlistStatus.CLOSED);

  private final CompanyService companyService;
  private final JobService jobService;
  private final JobIntakeApplicationService jobIntakeApplicationService;
  private final ShortlistService shortlistService;
  private final ClientUnlockRequestPort clientUnlockRequestPort;
  private final CandidateCompanyInteractionService interactionService;
  private final InterviewFeedbackService interviewFeedbackService;
  private final NotificationService notificationService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ClientApiQueryService(
      CompanyService companyService,
      JobService jobService,
      JobIntakeApplicationService jobIntakeApplicationService,
      ShortlistService shortlistService,
      ClientUnlockRequestPort clientUnlockRequestPort,
      CandidateCompanyInteractionService interactionService,
      InterviewFeedbackService interviewFeedbackService,
      NotificationService notificationService) {
    this(
        companyService,
        jobService,
        jobIntakeApplicationService,
        shortlistService,
        clientUnlockRequestPort,
        interactionService,
        interviewFeedbackService,
        notificationService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ClientApiQueryService(
      CompanyService companyService,
      JobService jobService,
      JobIntakeApplicationService jobIntakeApplicationService,
      ShortlistService shortlistService,
      ClientUnlockRequestPort clientUnlockRequestPort,
      CandidateCompanyInteractionService interactionService,
      InterviewFeedbackService interviewFeedbackService,
      NotificationService notificationService,
      PermissionEnforcer permissionEnforcer) {
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.jobIntakeApplicationService = Objects.requireNonNull(
        jobIntakeApplicationService, "jobIntakeApplicationService must not be null");
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.clientUnlockRequestPort = Objects.requireNonNull(
        clientUnlockRequestPort, "clientUnlockRequestPort must not be null");
    this.interactionService = Objects.requireNonNull(
        interactionService, "interactionService must not be null");
    this.interviewFeedbackService = Objects.requireNonNull(
        interviewFeedbackService, "interviewFeedbackService must not be null");
    this.notificationService = Objects.requireNonNull(
        notificationService, "notificationService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public Optional<ClientCompanyProfileResponse> getCompanyProfile(
      AccessRequest accessRequest, UUID organizationId, UUID actorId) {
    requireClientContext(accessRequest, ResourceType.COMPANY, AccessAction.READ, "company");
    return findClientCompany(organizationId, actorId).map(ClientApiQueryService::toCompanyProfileResponse);
  }

  public Optional<ClientPreferenceResponse> getPreferences(
      AccessRequest accessRequest, UUID organizationId, UUID actorId) {
    requireClientContext(accessRequest, ResourceType.COMPANY, AccessAction.READ, "company");
    return findClientCompany(organizationId, actorId)
        .map(company -> new ClientPreferenceResponse(
            company.companyId().value().toString(),
            companyService.findPreferencesByCompanyIdAndOrganizationId(organizationId, company.companyId())
                .stream()
                .map(preference -> new ClientPreferenceResponse.PreferenceItem(
                    preference.preferenceKey(),
                    preference.preferenceValue(),
                    preference.notes()))
                .toList()));
  }

  public Optional<ClientJobSubmissionStatusResponse> getJobStatus(
      AccessRequest accessRequest, UUID organizationId, UUID actorId, JobId jobId) {
    requireClientContext(accessRequest, ResourceType.JOB, AccessAction.READ, "job");
    return findVisibleJob(organizationId, actorId, jobId)
        .map(job -> toJobStatusResponse(
            job,
            jobIntakeApplicationService.activationGate(organizationId, jobId)));
  }

  public ClientDashboardResponse getDashboard(
      AccessRequest accessRequest, UUID organizationId, UUID actorId) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.READ, "shortlist");
    Company company = findClientCompany(organizationId, actorId)
        .orElseThrow(() -> new IllegalArgumentException("client_company_profile_not_found"));
    List<Job> jobs = findVisibleJobs(organizationId, actorId);
    List<Shortlist> shortlists = findVisibleShortlists(organizationId, actorId);
    List<ClientShortlistSummaryResponse> recentShortlists = shortlists.stream()
        .sorted(Comparator.comparing(Shortlist::createdAt).reversed())
        .limit(5)
        .map(shortlist -> toShortlistSummary(
            shortlist,
            visibleClientCardCount(
                shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlist.shortlistId()))))
        .toList();
    int pendingClarificationCount = (int) jobs.stream()
        .filter(job -> !jobIntakeApplicationService.activationGate(organizationId, job.jobId()).activationAllowed())
        .count();
    int feedbackCount = jobs.stream()
        .mapToInt(job -> interviewFeedbackService.findFeedbackByJobIdAndOrganizationId(organizationId, job.jobId()).size())
        .sum();
    int pendingUnlockRequestCount = (int) clientUnlockRequestPort.findByClientActorId(organizationId, actorId).stream()
        .filter(request -> request.status() == ClientUnlockRequestStatus.REQUESTED
            || request.status() == ClientUnlockRequestStatus.UNDER_REVIEW)
        .count();
    NotificationService.NotificationPage notifications = notificationService.listNotifications(
        organizationId,
        actorId,
        PortalRole.CLIENT,
        5,
        0);
    int unreadNotificationCount = (int) notifications.items().stream()
        .filter(item -> !"read".equals(item.status()) && !"dismissed".equals(item.status()))
        .count();
    return new ClientDashboardResponse(
        company.companyId().value().toString(),
        company.displayName() != null && !company.displayName().isBlank() ? company.displayName() : company.name(),
        true,
        jobs.size(),
        pendingClarificationCount,
        unreadNotificationCount,
        shortlists.size(),
        pendingUnlockRequestCount,
        feedbackCount,
        notifications.items().stream().map(NotificationService.NotificationRecord::title).toList(),
        recentShortlists);
  }

  public List<ClientShortlistSummaryResponse> listShortlists(
      AccessRequest accessRequest, UUID organizationId, UUID actorId) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.READ, "shortlist");
    return findVisibleShortlists(organizationId, actorId).stream()
        .sorted(Comparator.comparing(Shortlist::createdAt).reversed())
        .map(shortlist -> toShortlistSummary(
            shortlist,
            visibleClientCardCount(
                shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlist.shortlistId()))))
        .toList();
  }

  public Optional<ClientShortlistDetailResponse> getShortlistDetail(
      AccessRequest accessRequest, UUID organizationId, UUID actorId, ShortlistId shortlistId) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.READ, "shortlist");
    return findVisibleShortlist(organizationId, actorId, shortlistId).map(shortlist ->
        toShortlistDetail(
            organizationId,
            shortlist,
            shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlistId)));
  }

  public Optional<ClientInterviewFeedbackContextResponse> getInterviewFeedbackContext(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      CandidateCompanyInteractionId interviewId) {
    requireClientContext(accessRequest, ResourceType.SHORTLIST, AccessAction.READ, "shortlist");
    Optional<CandidateCompanyInteraction> interactionOptional =
        interactionService.findInteractionByIdAndOrganizationId(organizationId, interviewId)
            .filter(interaction -> interaction.interactionType() == InteractionType.INTERVIEW);
    if (interactionOptional.isEmpty()) {
      return Optional.empty();
    }
    CandidateCompanyInteraction interaction = interactionOptional.get();
    Optional<Job> jobOptional = findVisibleJob(organizationId, actorId, interaction.jobId());
    if (jobOptional.isEmpty()) {
      return Optional.empty();
    }
    Optional<ShortlistContext> shortlistContext = shortlistService
        .findShortlistsByJobIdAndOrganizationId(organizationId, interaction.jobId())
        .stream()
        .filter(shortlist -> CLIENT_VISIBLE_SHORTLIST_STATUSES.contains(shortlist.status()))
        .flatMap(shortlist -> shortlistService.findCardsByShortlistIdAndOrganizationId(
                organizationId, shortlist.shortlistId())
            .stream()
            .filter(card -> card.candidateId().equals(interaction.candidateId()))
            .map(card -> new ShortlistContext(shortlist, card)))
        .findFirst();
    if (shortlistContext.isEmpty()) {
      return Optional.empty();
    }
    if (!ClientApiCommandService.isInterviewFeedbackEligible(
        shortlistContext.get().shortlist(),
        shortlistContext.get().card())) {
      return Optional.empty();
    }
    String scorecard = jobService.findActiveScorecardByJobIdAndOrganizationId(organizationId, interaction.jobId())
        .map(this::writeScorecardJson)
        .orElse("{}");
    return Optional.of(new ClientInterviewFeedbackContextResponse(
        interviewId.value().toString(),
        shortlistContext.get().shortlist().shortlistId().value().toString(),
        shortlistContext.get().card().shortlistCandidateCardId().value().toString(),
        interaction.jobId().value().toString(),
        shortlistContext.get().shortlist().status().wireValue(),
        scorecard,
        interviewFeedbackService.findFeedbackByInteractionIdAndOrganizationId(organizationId, interviewId).size(),
        "/client/feedback/" + interviewId.value()));
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

  private List<Job> findVisibleJobs(UUID organizationId, UUID actorId) {
    return jobService.findAllJobsByOrganizationId(organizationId).stream()
        .filter(job -> JobIntakeApplicationService.metadataContainsActor(job.metadata(), actorId))
        .sorted(Comparator.comparing(Job::createdAt).reversed())
        .toList();
  }

  private Optional<Job> findVisibleJob(UUID organizationId, UUID actorId, JobId jobId) {
    return jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .filter(job -> JobIntakeApplicationService.metadataContainsActor(job.metadata(), actorId));
  }

  private List<Shortlist> findVisibleShortlists(UUID organizationId, UUID actorId) {
    Set<JobId> visibleJobIds = findVisibleJobs(organizationId, actorId).stream()
        .map(Job::jobId)
        .collect(java.util.stream.Collectors.toSet());
    return shortlistService.findAllShortlistsByOrganizationId(organizationId).stream()
        .filter(shortlist -> visibleJobIds.contains(shortlist.jobId()))
        .filter(shortlist -> CLIENT_VISIBLE_SHORTLIST_STATUSES.contains(shortlist.status()))
        .toList();
  }

  private Optional<Shortlist> findVisibleShortlist(UUID organizationId, UUID actorId, ShortlistId shortlistId) {
    Set<JobId> visibleJobIds = findVisibleJobs(organizationId, actorId).stream()
        .map(Job::jobId)
        .collect(java.util.stream.Collectors.toSet());
    return shortlistService.findShortlistByIdAndOrganizationId(organizationId, shortlistId)
        .filter(shortlist -> visibleJobIds.contains(shortlist.jobId()))
        .filter(shortlist -> CLIENT_VISIBLE_SHORTLIST_STATUSES.contains(shortlist.status()));
  }

  private ClientShortlistSummaryResponse toShortlistSummary(Shortlist shortlist, int cardCount) {
    return new ClientShortlistSummaryResponse(
        shortlist.shortlistId().value().toString(),
        shortlist.jobId().value().toString(),
        shortlist.title(),
        shortlist.status().wireValue(),
        cardCount,
        optionalInstant(shortlist.sentAt()),
        optionalInstant(shortlist.clientViewedAt()),
        shortlist.createdAt().toString());
  }

  private int visibleClientCardCount(List<ShortlistCandidateCard> cards) {
    return (int) cards.stream()
        .filter(card -> card.status() != ShortlistCandidateCardStatus.REMOVED)
        .count();
  }

  private ClientShortlistDetailResponse toShortlistDetail(
      UUID organizationId,
      Shortlist shortlist,
      List<ShortlistCandidateCard> cards) {
    List<ClientShortlistDetailResponse.Card> cardResponses = cards.stream()
        .filter(card -> card.status() != ShortlistCandidateCardStatus.REMOVED)
        .sorted(Comparator.comparingInt(ShortlistCandidateCard::sortOrder))
        .map(card -> toCardResponse(
            organizationId,
            shortlist,
            card,
            clientUnlockRequestPort.findLatestByShortlistCardAndOrganizationId(
                organizationId,
                shortlist.shortlistId(),
                card.shortlistCandidateCardId())))
        .toList();
    return new ClientShortlistDetailResponse(
        shortlist.shortlistId().value().toString(),
        shortlist.jobId().value().toString(),
        shortlist.title(),
        shortlist.status().wireValue(),
        optionalInstant(shortlist.sentAt()),
        optionalInstant(shortlist.clientViewedAt()),
        shortlist.createdAt().toString(),
        shortlist.updatedAt().toString(),
        cardResponses);
  }

  private ClientShortlistDetailResponse.Card toCardResponse(
      UUID organizationId,
      Shortlist shortlist,
      ShortlistCandidateCard card,
      Optional<ClientUnlockRequest> unlockRequest) {
    ShortlistCandidateCardViewMetadata metadata = metadata(card).orElseGet(() -> emptyMetadata(card));
    return new ClientShortlistDetailResponse.Card(
        card.shortlistCandidateCardId().value().toString(),
        toOpaqueCardId(card),
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
        unlockRequest.map(request -> request.status().wireValue()).orElse(null),
        unlockRequest.map(ClientUnlockRequest::unlockDecisionRef).orElse(null),
        unlockRequest.map(ClientUnlockRequest::approvedDisclosureRecordRef).orElse(null));
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

  private ShortlistCandidateCardViewMetadata emptyMetadata(ShortlistCandidateCard card) {
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

  private String toOpaqueCardId(ShortlistCandidateCard card) {
    return "card_" + card.anonymousCandidateCardId().toString().replace("-", "");
  }

  private String writeScorecardJson(JobScorecard scorecard) {
    try {
      return OBJECT_MAPPER.writeValueAsString(scorecard);
    } catch (Exception exception) {
      return "{}";
    }
  }

  private static String optionalInstant(java.time.Instant instant) {
    return instant != null ? instant.toString() : null;
  }

  static ClientCompanyProfileResponse toCompanyProfileResponse(Company company) {
    return new ClientCompanyProfileResponse(
        company.companyId().value().toString(),
        company.version(),
        company.name(),
        company.displayName(),
        company.industry(),
        company.website(),
        company.headquartersLocation(),
        company.sizeBand(),
        company.paymentReliability(),
        company.status().wireValue(),
        company.updatedAt().toString());
  }

  static ClientJobSubmissionStatusResponse toJobStatusResponse(
      Job job, com.recruitingtransactionos.coreapi.job.service.JobActivationGateResult gateResult) {
    return new ClientJobSubmissionStatusResponse(
        job.jobId().value().toString(),
        job.companyId().value().toString(),
        job.title(),
        job.status().wireValue(),
        job.createdAt().toString(),
        job.updatedAt().toString(),
        gateResult.clarificationQuestions(),
        JobIntakeApplicationService.clarificationAnswersFromMetadata(job.metadata()),
        gateResult.blockerReasons(),
        gateResult.activationAllowed());
  }

  private record ShortlistContext(
      Shortlist shortlist,
      ShortlistCandidateCard card) {
  }
}
