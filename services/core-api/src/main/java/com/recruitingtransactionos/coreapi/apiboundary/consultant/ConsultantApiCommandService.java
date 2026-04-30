package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanyDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper.ConsultantCompanyResponseMapper;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper.ConsultantJobResponseMapper;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper.ConsultantShortlistResponseMapper;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyContact;
import com.recruitingtransactionos.coreapi.company.CompanyContactId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobRequirementId;
import com.recruitingtransactionos.coreapi.job.JobRequirementImportance;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobScorecardId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantApiCommandService {

  private final CompanyService companyService;
  private final JobService jobService;
  private final ShortlistService shortlistService;
  private final PermissionEnforcer permissionEnforcer;

  public ConsultantApiCommandService(
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService) {
    this(companyService, jobService, shortlistService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantApiCommandService(
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService,
      PermissionEnforcer permissionEnforcer) {
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  // ── Company ─────────────────────────────────────────────────────────────────

  public ConsultantCompanyDetailResponse createCompany(
      AccessRequest accessRequest, UUID organizationId, CompanyCreateRequest request) {
    requireConsultantCompanyWrite(accessRequest, AccessAction.CREATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    CompanyId companyId = new CompanyId(UUID.randomUUID());
    Instant now = Instant.now();

    Company company = Company.builder()
        .companyId(companyId)
        .organizationId(organizationId)
        .name(request.name())
        .displayName(request.displayName())
        .industry(request.industry())
        .website(request.website())
        .headquartersLocation(request.headquartersLocation())
        .sizeBand(request.sizeBand())
        .status(CompanyStatus.fromWireValue(request.status()))
        .paymentReliability(request.paymentReliability())
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    Company created = companyService.createCompany(company);
    return ConsultantCompanyResponseMapper.toDetail(created, Collections.emptyList(), 0);
  }

  public ConsultantCompanyDetailResponse updateCompany(
      AccessRequest accessRequest, UUID organizationId, CompanyId companyId,
      CompanyUpdateRequest request) {
    requireConsultantCompanyWrite(accessRequest, AccessAction.UPDATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    // Verify the company exists in this organization
    Company existing = companyService.findCompanyByIdAndOrganizationId(organizationId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company not found in this organization"));

    Company company = Company.builder()
        .companyId(companyId)
        .organizationId(organizationId)
        .name(request.name())
        .displayName(request.displayName())
        .industry(request.industry())
        .website(request.website())
        .headquartersLocation(request.headquartersLocation())
        .sizeBand(request.sizeBand())
        .status(CompanyStatus.fromWireValue(request.status()))
        .paymentReliability(request.paymentReliability())
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(existing.createdAt())
        .updatedAt(existing.updatedAt())
        .version(request.version())
        .build();

    Company updated = companyService.updateCompany(company);
    List<CompanyContact> contacts = companyService.findContactsByCompanyIdAndOrganizationId(
        organizationId, companyId);
    int jobCount = (int) jobService.findJobsByCompanyIdAndOrganizationId(
        organizationId, companyId).stream().count();
    return ConsultantCompanyResponseMapper.toDetail(updated,
        contacts != null ? contacts : Collections.emptyList(), jobCount);
  }

  // ── Job ─────────────────────────────────────────────────────────────────────

  public ConsultantJobDetailResponse createJob(
      AccessRequest accessRequest, UUID organizationId, JobCreateRequest request) {
    requireConsultantJobWrite(accessRequest, AccessAction.CREATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    CompanyId companyId = new CompanyId(UUID.fromString(request.companyId()));
    companyService.findCompanyByIdAndOrganizationId(organizationId, companyId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Company not found in this organization"));

    JobId jobId = new JobId(UUID.randomUUID());
    Instant now = Instant.now();

    Job job = Job.builder()
        .jobId(jobId)
        .organizationId(organizationId)
        .companyId(companyId)
        .title(request.title())
        .description(request.description())
        .location(request.location())
        .seniorityBand(request.seniorityBand())
        .roleFamily(request.roleFamily())
        .employmentType(request.employmentType())
        .compensation(request.compensation())
        .status(JobStatus.fromWireValue(request.status()))
        .commercialTerms(request.commercialTerms())
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    Job created = jobService.createJob(job);
    return ConsultantJobResponseMapper.toDetail(created,
        Collections.emptyList(), Optional.empty());
  }

  public ConsultantJobDetailResponse updateJob(
      AccessRequest accessRequest, UUID organizationId, JobId jobId,
      JobUpdateRequest request) {
    requireConsultantJobWrite(accessRequest, AccessAction.UPDATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    CompanyId companyId = new CompanyId(UUID.fromString(request.companyId()));
    companyService.findCompanyByIdAndOrganizationId(organizationId, companyId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Company not found in this organization"));

    // Verify the job exists in this organization
    Job existing = jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found in this organization"));

    Job job = Job.builder()
        .jobId(jobId)
        .organizationId(organizationId)
        .companyId(companyId)
        .title(request.title())
        .description(request.description())
        .location(request.location())
        .seniorityBand(request.seniorityBand())
        .roleFamily(request.roleFamily())
        .employmentType(request.employmentType())
        .compensation(request.compensation())
        .status(JobStatus.fromWireValue(request.status()))
        .commercialTerms(request.commercialTerms())
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(existing.createdAt())
        .updatedAt(existing.updatedAt())
        .version(request.version())
        .build();

    Job updated = jobService.updateJob(job);
    List<JobRequirement> requirements = jobService.findRequirementsByJobIdAndOrganizationId(
        organizationId, jobId);
    Optional<JobScorecard> scorecard = jobService.findActiveScorecardByJobIdAndOrganizationId(
        organizationId, jobId);
    return ConsultantJobResponseMapper.toDetail(updated,
        requirements != null ? requirements : Collections.emptyList(), scorecard);
  }

  // ── Shortlist ───────────────────────────────────────────────────────────────

  public ConsultantShortlistDetailResponse createShortlist(
      AccessRequest accessRequest, UUID organizationId, ShortlistCreateRequest request) {
    requireConsultantShortlistWrite(accessRequest, AccessAction.CREATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    JobId jobId = new JobId(UUID.fromString(request.jobId()));
    jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Job not found in this organization"));

    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());
    Instant now = Instant.now();

    Shortlist shortlist = Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(organizationId)
        .jobId(jobId)
        .title(request.title())
        .status(ShortlistStatus.fromWireValue(request.status()))
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    Shortlist created = shortlistService.createShortlist(shortlist);
    return ConsultantShortlistResponseMapper.toDetail(created, Collections.emptyList());
  }

  public ConsultantShortlistDetailResponse updateShortlist(
      AccessRequest accessRequest, UUID organizationId, ShortlistId shortlistId,
      ShortlistUpdateRequest request) {
    requireConsultantShortlistWrite(accessRequest, AccessAction.UPDATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    Shortlist existing = shortlistService.findShortlistByIdAndOrganizationId(
        organizationId, shortlistId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Shortlist not found in this organization"));

    JobId jobId = new JobId(UUID.fromString(request.jobId()));
    jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Job not found in this organization"));

    Shortlist shortlist = Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(organizationId)
        .jobId(jobId)
        .title(request.title())
        .status(ShortlistStatus.fromWireValue(request.status()))
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(existing.createdAt())
        .updatedAt(existing.updatedAt())
        .version(request.version())
        .build();

    Shortlist updated = shortlistService.updateShortlist(shortlist);
    return ConsultantShortlistResponseMapper.toDetail(updated, Collections.emptyList());
  }

  // ── Company Contact (sub-resource) ──────────────────────────────────────────

  public ConsultantCompanyDetailResponse createCompanyContact(
      AccessRequest accessRequest, UUID organizationId, CompanyId companyId,
      CompanyContactCreateRequest request) {
    requireConsultantCompanyWrite(accessRequest, AccessAction.CREATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    companyService.findCompanyByIdAndOrganizationId(organizationId, companyId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Company not found in this organization"));

    CompanyContactId contactId = new CompanyContactId(UUID.randomUUID());
    Instant now = Instant.now();

    CompanyContact contact = CompanyContact.builder()
        .companyContactId(contactId)
        .organizationId(organizationId)
        .companyId(companyId)
        .name(request.name())
        .title(request.title())
        .email(request.email())
        .phone(request.phone())
        .roleType(request.roleType())
        .isPrimary(request.isPrimary())
        .status(request.status() != null ? request.status() : "active")
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    companyService.createContact(contact);
    Company company = companyService.findCompanyByIdAndOrganizationId(
        organizationId, companyId).orElseThrow();
    List<CompanyContact> contacts = companyService.findContactsByCompanyIdAndOrganizationId(
        organizationId, companyId);
    int jobCount = (int) jobService.findJobsByCompanyIdAndOrganizationId(
        organizationId, companyId).stream().count();
    return ConsultantCompanyResponseMapper.toDetail(company,
        contacts != null ? contacts : Collections.emptyList(), jobCount);
  }

  // ── Job Requirement (sub-resource) ──────────────────────────────────────────

  public ConsultantJobDetailResponse createJobRequirement(
      AccessRequest accessRequest, UUID organizationId, JobId jobId,
      JobRequirementCreateRequest request) {
    requireConsultantJobWrite(accessRequest, AccessAction.CREATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Job not found in this organization"));

    JobRequirementId requirementId = new JobRequirementId(UUID.randomUUID());
    Instant now = Instant.now();

    JobRequirement requirement = JobRequirement.builder()
        .jobRequirementId(requirementId)
        .organizationId(organizationId)
        .jobId(jobId)
        .requirementType(request.requirementType())
        .label(request.label())
        .importance(request.importance() != null
            ? JobRequirementImportance.fromWireValue(request.importance())
            : JobRequirementImportance.NICE_TO_HAVE)
        .detail(request.detail())
        .sortOrder(request.sortOrder())
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    jobService.createRequirement(requirement);
    Job job = jobService.findJobByIdAndOrganizationId(organizationId, jobId).orElseThrow();
    List<JobRequirement> requirements = jobService.findRequirementsByJobIdAndOrganizationId(
        organizationId, jobId);
    Optional<JobScorecard> scorecard = jobService.findActiveScorecardByJobIdAndOrganizationId(
        organizationId, jobId);
    return ConsultantJobResponseMapper.toDetail(job,
        requirements != null ? requirements : Collections.emptyList(), scorecard);
  }

  // ── Job Scorecard (sub-resource) ────────────────────────────────────────────

  public ConsultantJobDetailResponse createJobScorecard(
      AccessRequest accessRequest, UUID organizationId, JobId jobId,
      JobScorecardCreateRequest request) {
    requireConsultantJobWrite(accessRequest, AccessAction.CREATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Job not found in this organization"));

    JobScorecardId scorecardId = new JobScorecardId(UUID.randomUUID());
    Instant now = Instant.now();

    JobScorecard scorecard = JobScorecard.builder()
        .jobScorecardId(scorecardId)
        .organizationId(organizationId)
        .jobId(jobId)
        .dimensions(request.dimensions())
        .scoringGuidance(request.scoringGuidance())
        .status(request.status() != null ? request.status() : "draft")
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    jobService.createScorecard(scorecard);
    Job job = jobService.findJobByIdAndOrganizationId(organizationId, jobId).orElseThrow();
    List<JobRequirement> requirements = jobService.findRequirementsByJobIdAndOrganizationId(
        organizationId, jobId);
    Optional<JobScorecard> activeScorecard = jobService.findActiveScorecardByJobIdAndOrganizationId(
        organizationId, jobId);
    return ConsultantJobResponseMapper.toDetail(job,
        requirements != null ? requirements : Collections.emptyList(), activeScorecard);
  }

  // ── Access enforcement ──────────────────────────────────────────────────────

  private void requireConsultantCompanyWrite(
      AccessRequest accessRequest, AccessAction action) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.COMPANY
        || (accessRequest.action() != AccessAction.CREATE
            && accessRequest.action() != AccessAction.UPDATE)) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "company_write_context_required",
          "Consultant company API requires a company create or update context."));
    }
  }

  private void requireConsultantJobWrite(
      AccessRequest accessRequest, AccessAction action) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.JOB
        || (accessRequest.action() != AccessAction.CREATE
            && accessRequest.action() != AccessAction.UPDATE)) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "job_write_context_required",
          "Consultant job API requires a job create or update context."));
    }
  }

  private void requireConsultantShortlistWrite(
      AccessRequest accessRequest, AccessAction action) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.SHORTLIST
        || (accessRequest.action() != AccessAction.CREATE
            && accessRequest.action() != AccessAction.UPDATE)) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "shortlist_write_context_required",
          "Consultant shortlist API requires a shortlist create or update context."));
    }
  }
}
