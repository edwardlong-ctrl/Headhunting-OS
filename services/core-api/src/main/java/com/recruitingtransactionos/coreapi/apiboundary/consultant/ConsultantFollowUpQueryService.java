package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantBlockedActionResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantIntakeQueueItemResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantFollowUpSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.followup.FollowUpSubmissionService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackSuggestionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantFollowUpQueryService {

  private final ConsultantDashboardQueryService dashboardQueryService;
  private final ConsultantIntakeQueueQueryService intakeQueueQueryService;
  private final CandidateService candidateService;
  private final FollowUpSubmissionService followUpSubmissionService;
  private final InterviewFeedbackSuggestionService interviewFeedbackSuggestionService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantFollowUpQueryService(
      ConsultantDashboardQueryService dashboardQueryService,
      ConsultantIntakeQueueQueryService intakeQueueQueryService,
      CandidateService candidateService,
      FollowUpSubmissionService followUpSubmissionService,
      InterviewFeedbackSuggestionService interviewFeedbackSuggestionService) {
    this(
        dashboardQueryService,
        intakeQueueQueryService,
        candidateService,
        followUpSubmissionService,
        interviewFeedbackSuggestionService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantFollowUpQueryService(
      ConsultantDashboardQueryService dashboardQueryService,
      ConsultantIntakeQueueQueryService intakeQueueQueryService,
      CandidateService candidateService,
      FollowUpSubmissionService followUpSubmissionService,
      InterviewFeedbackSuggestionService interviewFeedbackSuggestionService,
      PermissionEnforcer permissionEnforcer) {
    this.dashboardQueryService = Objects.requireNonNull(dashboardQueryService, "dashboardQueryService must not be null");
    this.intakeQueueQueryService = Objects.requireNonNull(intakeQueueQueryService, "intakeQueueQueryService must not be null");
    this.candidateService = Objects.requireNonNull(candidateService, "candidateService must not be null");
    this.followUpSubmissionService = Objects.requireNonNull(
        followUpSubmissionService, "followUpSubmissionService must not be null");
    this.interviewFeedbackSuggestionService = Objects.requireNonNull(
        interviewFeedbackSuggestionService, "interviewFeedbackSuggestionService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public PagedResult<ConsultantFollowUpSummaryResponse> list(
      AccessRequest accessRequest, PagedQuery pagedQuery) {
    requireRead(accessRequest);
    UUID organizationId = pagedQuery.organizationId();
    List<ConsultantFollowUpSummaryResponse> items = new ArrayList<>();
    ConsultantDashboardResponseSource dashboard = ConsultantDashboardResponseSource.from(
        dashboardQueryService.load(accessRequest, organizationId));
    for (ConsultantIntakeQueueItemResponse item : intakeQueueQueryService.listQueue(organizationId, 25).items()) {
      ConsultantFollowUpSummaryResponse followUp = toFollowUp(item);
      if (followUp != null) {
        items.add(followUp);
      }
    }
    for (ConsultantBlockedActionResponse blockedAction : dashboard.blockedActions()) {
      items.add(new ConsultantFollowUpSummaryResponse(
          "blocked_action",
          blockedAction.entityType(),
          blockedAction.entityId(),
          blockedAction.title(),
          blockedAction.severity(),
          blockedAction.safeReason(),
          blockedAction.route(),
          dashboard.generatedAt()));
    }
    for (Candidate candidate : candidateService.findCandidatesByOrganizationIdAndStatus(
        organizationId, CandidateStatus.CONSENT_PENDING)) {
      items.add(new ConsultantFollowUpSummaryResponse(
          "candidate_consent",
          "candidate",
          candidate.candidateId().value().toString(),
          "Candidate consent follow-up",
          candidate.status().wireValue(),
          "Candidate is waiting for consent confirmation.",
          "/consultant/talent/" + candidate.candidateId().value(),
          candidate.updatedAt().toString()));
    }
    followUpSubmissionService.listPendingByOrganization(organizationId, 25).forEach(submission ->
        items.add(new ConsultantFollowUpSummaryResponse(
            "candidate_follow_up_submission",
            "follow_up_submission",
            submission.followUpSubmissionId().toString(),
            "Candidate follow-up submitted",
            submission.status(),
            "Candidate follow-up answer for " + submission.fieldPath() + " is waiting for consultant review.",
            "/consultant/follow-ups",
            submission.submittedAt().toString())));
    interviewFeedbackSuggestionService.listPendingByOrganization(organizationId, 25).forEach(suggestion ->
        items.add(new ConsultantFollowUpSummaryResponse(
            "interview_feedback_review",
            "interview_feedback_suggestion",
            suggestion.interviewFeedbackSuggestionId().value().toString(),
            suggestion.title(),
            suggestion.status().wireValue(),
            "Interview feedback suggestion is waiting for consultant review.",
            "/consultant/follow-ups?suggestionId=" + suggestion.interviewFeedbackSuggestionId().value(),
            suggestion.createdAt().toString())));
    List<ConsultantFollowUpSummaryResponse> paged = items.stream()
        .sorted(Comparator.comparing(ConsultantFollowUpSummaryResponse::occurredAt).reversed())
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .toList();
    return PagedResult.of(paged, items.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  private void requireRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.action() != AccessAction.READ) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "follow_up_read_context_required",
          "Consultant follow-up API requires a read context."));
    }
  }

  private static ConsultantFollowUpSummaryResponse toFollowUp(ConsultantIntakeQueueItemResponse item) {
    return switch (item.stage()) {
      case "extract_failed" -> new ConsultantFollowUpSummaryResponse(
          "intake_recovery",
          "information_packet",
          item.informationPacketId(),
          item.title(),
          "needs_recovery",
          item.stageDetail(),
          "/consultant/intake",
          item.updatedAt());
      case "extracted", "in_review" -> new ConsultantFollowUpSummaryResponse(
          "write_back_review",
          "information_packet",
          item.informationPacketId(),
          item.title(),
          "review_pending",
          item.stageDetail(),
          "/consultant/intake/review/" + item.informationPacketId(),
          item.updatedAt());
      case "ready_for_publish" -> new ConsultantFollowUpSummaryResponse(
          "publish_review",
          "information_packet",
          item.informationPacketId(),
          item.title(),
          "ready_for_publish",
          item.stageDetail(),
          "/consultant/intake/review/" + item.informationPacketId(),
          item.updatedAt());
      default -> null;
    };
  }

  private record ConsultantDashboardResponseSource(
      List<ConsultantBlockedActionResponse> blockedActions,
      String generatedAt) {
    static ConsultantDashboardResponseSource from(
        com.recruitingtransactionos.coreapi.apiboundary.ConsultantDashboardResponse response) {
      return new ConsultantDashboardResponseSource(response.blockedActions(), java.time.Instant.now().toString());
    }
  }
}
