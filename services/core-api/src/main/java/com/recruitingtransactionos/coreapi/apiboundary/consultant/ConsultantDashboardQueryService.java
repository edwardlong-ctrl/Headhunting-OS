package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantBlockedActionResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantDashboardResponse;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantDashboardQueryService {

  private final CandidateService candidateService;
  private final CompanyService companyService;
  private final JobService jobService;
  private final ShortlistService shortlistService;
  private final WorkflowAuditQueryService workflowAuditQueryService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantDashboardQueryService(
      CandidateService candidateService,
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService,
      WorkflowAuditQueryService workflowAuditQueryService) {
    this(
        candidateService,
        companyService,
        jobService,
        shortlistService,
        workflowAuditQueryService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantDashboardQueryService(
      CandidateService candidateService,
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService,
      WorkflowAuditQueryService workflowAuditQueryService,
      PermissionEnforcer permissionEnforcer) {
    this.candidateService = Objects.requireNonNull(candidateService, "candidateService must not be null");
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.workflowAuditQueryService = Objects.requireNonNull(workflowAuditQueryService, "workflowAuditQueryService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public ConsultantDashboardResponse load(AccessRequest accessRequest, UUID organizationId) {
    requireRead(accessRequest);
    int candidateCount = candidateService.findAllCandidatesByOrganizationId(organizationId).size();
    int activeJobCount = jobService.findJobsByOrganizationIdAndStatus(organizationId, JobStatus.ACTIVATED).size();
    int companyCount = companyService.findAllCompaniesByOrganizationId(organizationId).size();
    int shortlistCount = shortlistService.findAllShortlistsByOrganizationId(organizationId).size();
    List<ConsultantBlockedActionResponse> blockedActions = buildBlockedActions(organizationId);
    int pendingFollowUpCount = blockedActions.size()
        + candidateService.findCandidatesByOrganizationIdAndStatus(organizationId, CandidateStatus.CONSENT_PENDING).size();
    int recentTimelineCount = workflowAuditQueryService.search(
        WorkflowAuditQuery.builder(organizationId).limit(20).offset(0).build()).size();
    return new ConsultantDashboardResponse(
        candidateCount,
        activeJobCount,
        companyCount,
        shortlistCount,
        pendingFollowUpCount,
        recentTimelineCount,
        blockedActions);
  }

  private List<ConsultantBlockedActionResponse> buildBlockedActions(UUID organizationId) {
    List<ConsultantBlockedActionResponse> items = new ArrayList<>();
    for (Job job : jobService.findJobsByOrganizationIdAndStatus(organizationId, JobStatus.COMMERCIAL_PENDING)) {
      items.add(new ConsultantBlockedActionResponse(
          "job",
          job.jobId().value().toString(),
          job.title(),
          "commercial_pending",
          "Commercial review is still pending before activation.",
          "high",
          "/consultant/jobs/" + job.jobId().value()));
    }
    for (Job job : jobService.findJobsByOrganizationIdAndStatus(organizationId, JobStatus.CONTRACT_PENDING)) {
      items.add(new ConsultantBlockedActionResponse(
          "job",
          job.jobId().value().toString(),
          job.title(),
          "contract_pending",
          "Contract approval is still pending.",
          "high",
          "/consultant/jobs/" + job.jobId().value()));
    }
    for (Shortlist shortlist : shortlistService.findAllShortlistsByOrganizationId(organizationId)) {
      if (shortlist.status() == ShortlistStatus.CLIENT_FEEDBACK_PENDING) {
        items.add(new ConsultantBlockedActionResponse(
            "shortlist",
            shortlist.shortlistId().value().toString(),
            shortlist.title(),
            "client_feedback_pending",
            "Client feedback is pending on this shortlist.",
            "medium",
            "/consultant/shortlists/" + shortlist.shortlistId().value()));
      }
    }
    return items.stream().limit(12).toList();
  }

  private void requireRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.action() != AccessAction.READ) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "dashboard_read_context_required",
          "Consultant dashboard requires a read context."));
    }
  }
}
