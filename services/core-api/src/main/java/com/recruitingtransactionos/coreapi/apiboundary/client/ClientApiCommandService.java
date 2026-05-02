package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ClientCompanyProfileResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientJobSubmissionStatusResponse;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyIntakeApplicationService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ClientApiCommandService {

  private final CompanyIntakeApplicationService companyIntakeApplicationService;
  private final JobIntakeApplicationService jobIntakeApplicationService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ClientApiCommandService(
      CompanyIntakeApplicationService companyIntakeApplicationService,
      JobIntakeApplicationService jobIntakeApplicationService) {
    this(
        companyIntakeApplicationService,
        jobIntakeApplicationService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ClientApiCommandService(
      CompanyIntakeApplicationService companyIntakeApplicationService,
      JobIntakeApplicationService jobIntakeApplicationService,
      PermissionEnforcer permissionEnforcer) {
    this.companyIntakeApplicationService = Objects.requireNonNull(
        companyIntakeApplicationService, "companyIntakeApplicationService must not be null");
    this.jobIntakeApplicationService = Objects.requireNonNull(
        jobIntakeApplicationService, "jobIntakeApplicationService must not be null");
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

  private static Optional<CompanyId> optionalCompanyId(String companyId) {
    if (companyId == null || companyId.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new CompanyId(UUID.fromString(companyId)));
  }
}
