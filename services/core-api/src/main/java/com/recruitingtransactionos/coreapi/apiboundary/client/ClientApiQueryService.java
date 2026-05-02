package com.recruitingtransactionos.coreapi.apiboundary.client;

import com.recruitingtransactionos.coreapi.apiboundary.ClientCompanyProfileResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ClientJobSubmissionStatusResponse;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyIntakeApplicationService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobActivationGateResult;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ClientApiQueryService {

  private final CompanyService companyService;
  private final JobService jobService;
  private final JobIntakeApplicationService jobIntakeApplicationService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ClientApiQueryService(
      CompanyService companyService,
      JobService jobService,
      JobIntakeApplicationService jobIntakeApplicationService) {
    this(
        companyService,
        jobService,
        jobIntakeApplicationService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ClientApiQueryService(
      CompanyService companyService,
      JobService jobService,
      JobIntakeApplicationService jobIntakeApplicationService,
      PermissionEnforcer permissionEnforcer) {
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.jobIntakeApplicationService = Objects.requireNonNull(
        jobIntakeApplicationService, "jobIntakeApplicationService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public Optional<ClientCompanyProfileResponse> getCompanyProfile(
      AccessRequest accessRequest, UUID organizationId, UUID actorId) {
    requireClientContext(accessRequest, ResourceType.COMPANY, AccessAction.READ, "company");
    return companyService.findAllCompaniesByOrganizationId(organizationId).stream()
        .filter(company -> CompanyIntakeApplicationService.metadataContainsActor(
            company.metadata(),
            actorId))
        .findFirst()
        .map(ClientApiQueryService::toCompanyProfileResponse);
  }

  public Optional<ClientJobSubmissionStatusResponse> getJobStatus(
      AccessRequest accessRequest, UUID organizationId, UUID actorId, JobId jobId) {
    requireClientContext(accessRequest, ResourceType.JOB, AccessAction.READ, "job");
    return jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .filter(job -> JobIntakeApplicationService.metadataContainsActor(job.metadata(), actorId))
        .map(job -> toJobStatusResponse(
            job,
            jobIntakeApplicationService.activationGate(organizationId, jobId)));
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
      Job job, JobActivationGateResult gateResult) {
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
}
