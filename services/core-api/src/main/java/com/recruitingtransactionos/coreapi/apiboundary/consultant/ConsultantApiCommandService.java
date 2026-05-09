package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.recruitingtransactionos.coreapi.industrypack.IndustryPack;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackId;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackKey;
import com.recruitingtransactionos.coreapi.industrypack.IndustryRoleFamilyTemplate;
import com.recruitingtransactionos.coreapi.industrypack.OntologyVersion;
import com.recruitingtransactionos.coreapi.industrypack.port.IndustryPackReadPort;
import com.recruitingtransactionos.coreapi.industrypack.service.IndustryPackService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobRequirementId;
import com.recruitingtransactionos.coreapi.job.JobRequirementImportance;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobScorecardId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistBuilderService;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantApiCommandService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final CompanyService companyService;
  private final JobService jobService;
  private final JobIntakeApplicationService jobIntakeApplicationService;
  private final ShortlistService shortlistService;
  private final ShortlistBuilderService shortlistBuilderService;
  private final IndustryPackService industryPackService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantApiCommandService(
      CompanyService companyService,
      JobService jobService,
      JobIntakeApplicationService jobIntakeApplicationService,
      ShortlistService shortlistService,
      ShortlistBuilderService shortlistBuilderService,
      IndustryPackService industryPackService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this(
        companyService,
        jobService,
        jobIntakeApplicationService,
        shortlistService,
        shortlistBuilderService,
        industryPackService,
        workflowTransitionAuditService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  public ConsultantApiCommandService(
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService) {
    this(companyService, jobService, shortlistService, defaultIndustryPackService());
  }

  public ConsultantApiCommandService(
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService,
      IndustryPackService industryPackService) {
    this(
        companyService,
        jobService,
        null,
        shortlistService,
        null,
        industryPackService,
        null,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantApiCommandService(
      CompanyService companyService,
      JobService jobService,
      JobIntakeApplicationService jobIntakeApplicationService,
      ShortlistService shortlistService,
      ShortlistBuilderService shortlistBuilderService,
      IndustryPackService industryPackService,
      WorkflowTransitionAuditService workflowTransitionAuditService,
      PermissionEnforcer permissionEnforcer) {
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.jobIntakeApplicationService = jobIntakeApplicationService;
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.shortlistBuilderService = shortlistBuilderService;
    this.industryPackService = Objects.requireNonNull(industryPackService, "industryPackService must not be null");
    this.workflowTransitionAuditService = workflowTransitionAuditService;
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
    UUID industryPackId = resolveIndustryPackId(request.industryPackKey());

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
        .commercialTerms(canonicalCommercialTerms(request.commercialTerms()))
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .industryPackId(industryPackId)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    Job created = jobService.createJob(job);
    return ConsultantJobResponseMapper.toDetail(created,
        Collections.emptyList(),
        Optional.empty(),
        industryPackService.findIndustryPackById(created.industryPackId()));
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
    UUID industryPackId = request.industryPackKey() == null
        ? existing.industryPackId()
        : resolveIndustryPackId(request.industryPackKey());

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
        .commercialTerms(canonicalCommercialTerms(request.commercialTerms()))
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .industryPackId(industryPackId)
        .metadata(mergeMetadata(existing.metadata(), request.metadata()))
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
        requirements != null ? requirements : Collections.emptyList(),
        scorecard,
        industryPackService.findIndustryPackById(updated.industryPackId()));
  }

  // ── Shortlist ───────────────────────────────────────────────────────────────

  public ConsultantShortlistDetailResponse createShortlist(
      AccessRequest accessRequest, UUID organizationId, ShortlistCreateRequest request) {
    return createShortlist(
        accessRequest,
        organizationId,
        new UUID(0L, 0L),
        request);
  }

  public ConsultantShortlistDetailResponse createShortlist(
      AccessRequest accessRequest, UUID organizationId, UUID actorId, ShortlistCreateRequest request) {
    requireConsultantShortlistWrite(accessRequest, AccessAction.CREATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    JobId jobId = new JobId(UUID.fromString(request.jobId()));
    jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Job not found in this organization"));
    ShortlistStatus requestedStatus = ShortlistStatus.fromWireValue(request.status());
    if (requestedStatus != ShortlistStatus.DRAFT) {
      throw new IllegalArgumentException("shortlist_create_requires_draft_status");
    }

    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());
    Instant now = Instant.now();

    Shortlist shortlist = Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(organizationId)
        .jobId(jobId)
        .title(request.title())
        .status(requestedStatus)
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();

    Shortlist created = shortlistService.createShortlist(shortlist);
    recordShortlistTransition(
        created,
        "absent",
        created.status().wireValue(),
        WorkflowActionCode.SHORTLIST_DRAFT_CREATED,
        actorId,
        "shortlist draft created from consultant portal");
    if (shortlistBuilderService != null) {
      return ConsultantShortlistResponseMapper.toDetail(
          shortlistBuilderService.getBuilderState(organizationId, created.shortlistId()));
    }
    return ConsultantShortlistResponseMapper.toDetail(created, Collections.emptyList());
  }

  public ConsultantShortlistDetailResponse updateShortlist(
      AccessRequest accessRequest, UUID organizationId, ShortlistId shortlistId,
      ShortlistUpdateRequest request) {
    return updateShortlist(
        accessRequest,
        organizationId,
        new UUID(0L, 0L),
        shortlistId,
        request);
  }

  public ConsultantShortlistDetailResponse updateShortlist(
      AccessRequest accessRequest, UUID organizationId, UUID actorId, ShortlistId shortlistId,
      ShortlistUpdateRequest request) {
    requireConsultantShortlistWrite(accessRequest, AccessAction.UPDATE);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    Objects.requireNonNull(request, "request must not be null");

    Shortlist existing = shortlistService.findShortlistByIdAndOrganizationId(
        organizationId, shortlistId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Shortlist not found in this organization"));
    requireMutableBuilderShortlist(existing);

    JobId jobId = new JobId(UUID.fromString(request.jobId()));
    jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Job not found in this organization"));

    ShortlistStatus requestedStatus = ShortlistStatus.fromWireValue(request.status());
    if (requestedStatus != ShortlistStatus.DRAFT
        && requestedStatus != ShortlistStatus.READY_FOR_REVIEW) {
      throw new IllegalArgumentException("shortlist_status_change_requires_send_command");
    }

    Shortlist shortlist = Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(organizationId)
        .jobId(jobId)
        .title(request.title())
        .status(requestedStatus)
        .ownerConsultantId(request.ownerConsultantId() != null
            ? UUID.fromString(request.ownerConsultantId()) : null)
        .metadata(request.metadata() != null ? request.metadata() : "{}")
        .createdAt(existing.createdAt())
        .updatedAt(existing.updatedAt())
        .version(request.version())
        .build();

    if (existing.status() != shortlist.status()) {
      if (shortlist.status() == ShortlistStatus.READY_FOR_REVIEW) {
        recordShortlistTransition(
            shortlist,
            existing.status().wireValue(),
            shortlist.status().wireValue(),
            WorkflowActionCode.SHORTLIST_READY_FOR_REVIEW,
            actorId,
            "shortlist promoted to ready_for_review");
      } else if (shortlist.status() == ShortlistStatus.DRAFT
          && existing.status() == ShortlistStatus.READY_FOR_REVIEW) {
        recordShortlistTransition(
            shortlist,
            existing.status().wireValue(),
            shortlist.status().wireValue(),
            WorkflowActionCode.SHORTLIST_RETURNED_TO_DRAFT,
            actorId,
            "shortlist returned to draft for additional consultant edits");
      }
    }
    Shortlist updated = shortlistService.updateShortlist(shortlist);
    if (shortlistBuilderService != null) {
      return ConsultantShortlistResponseMapper.toDetail(
          shortlistBuilderService.getBuilderState(organizationId, shortlistId));
    }
    return ConsultantShortlistResponseMapper.toDetail(
        updated,
        shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlistId));
  }

  public ConsultantShortlistDetailResponse addShortlistCandidateCard(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardCreateRequest request) {
    requireConsultantShortlistWrite(accessRequest, AccessAction.UPDATE);
    requireShortlistBuilderService();
    return ConsultantShortlistResponseMapper.toDetail(shortlistBuilderService.addCandidateCard(
        organizationId,
        actorId,
        shortlistId,
        new com.recruitingtransactionos.coreapi.candidateprofile.CandidateId(
            UUID.fromString(request.candidateId())),
        request.sortOrder(),
        request.clientNotes()));
  }

  public ConsultantShortlistDetailResponse updateShortlistCandidateCard(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId shortlistCandidateCardId,
      ShortlistCandidateCardUpdateRequest request) {
    requireConsultantShortlistWrite(accessRequest, AccessAction.UPDATE);
    requireShortlistBuilderService();
    ShortlistCandidateCardStatus status = request.status() == null
        ? null
        : ShortlistCandidateCardStatus.fromWireValue(request.status());
    requireBuilderCardStatus(status);
    return ConsultantShortlistResponseMapper.toDetail(shortlistBuilderService.updateCandidateCard(
        organizationId,
        actorId,
        shortlistId,
        shortlistCandidateCardId,
        request.version(),
        request.sortOrder(),
        status,
        request.clientNotes()));
  }

  public ConsultantShortlistDetailResponse sendShortlist(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      ShortlistId shortlistId,
      ShortlistSendRequest request) {
    requireConsultantShortlistWrite(accessRequest, AccessAction.UPDATE);
    requireShortlistBuilderService();
    Objects.requireNonNull(request, "request must not be null");
    return ConsultantShortlistResponseMapper.toDetail(shortlistBuilderService.sendToClient(
        organizationId,
        actorId,
        shortlistId));
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
        .importance(requirementImportance(request.importance()))
        .detail(jsonTextOrStructuredValue(request.detail()))
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
        requirements != null ? requirements : Collections.emptyList(),
        scorecard,
        industryPackService.findIndustryPackById(job.industryPackId()));
  }

  private static JobRequirementImportance requirementImportance(String rawImportance) {
    if (rawImportance == null || rawImportance.isBlank()) {
      return JobRequirementImportance.NICE_TO_HAVE;
    }
    return switch (rawImportance.strip().toLowerCase(java.util.Locale.ROOT)) {
      case "high" -> JobRequirementImportance.MUST_HAVE;
      case "medium" -> JobRequirementImportance.PREFERRED;
      case "low" -> JobRequirementImportance.NICE_TO_HAVE;
      default -> JobRequirementImportance.fromWireValue(rawImportance);
    };
  }

  private static String jsonTextOrStructuredValue(String value) {
    return jsonTextOrStructuredValue(value, "invalid_job_requirement_detail_json");
  }

  private static String jsonTextOrStructuredValue(String value, String errorCode) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String trimmed = value.strip();
    try {
      OBJECT_MAPPER.readTree(trimmed);
      return trimmed;
    } catch (Exception ignored) {
      try {
        return OBJECT_MAPPER.writeValueAsString(trimmed);
      } catch (Exception exception) {
        throw new IllegalArgumentException(errorCode, exception);
      }
    }
  }

  private static String scorecardStatus(String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return "draft";
    }
    return switch (rawStatus.strip().toLowerCase(java.util.Locale.ROOT)) {
      case "confirmed" -> "active";
      case "draft", "active", "archived" -> rawStatus.strip().toLowerCase(java.util.Locale.ROOT);
      default -> rawStatus;
    };
  }

  private static String canonicalCommercialTerms(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String trimmed = value.strip();
    try {
      JsonNode node = OBJECT_MAPPER.readTree(trimmed);
      if (!node.isObject()) {
        return trimmed;
      }
      ObjectNode object = (ObjectNode) node.deepCopy();
      if (missingText(object, "feeModel") && hasText(object, "feeRate")) {
        object.put("feeModel", "success_fee");
      }
      if (missingText(object, "feeRangeOrRate") && hasText(object, "feeRate")) {
        object.put("feeRangeOrRate", object.get("feeRate").asText());
      }
      if (missingText(object, "paymentTerms") && object.hasNonNull("replacementDays")) {
        object.put("paymentTerms", "replacement_days:" + object.get("replacementDays").asText());
      }
      if (missingText(object, "contractStatus")) {
        if (hasText(object, "approval")) {
          object.put("contractStatus", object.get("approval").asText());
        } else {
          object.put("contractStatus", "placeholder");
        }
      }
      return OBJECT_MAPPER.writeValueAsString(object);
    } catch (Exception ignored) {
      return value;
    }
  }

  private static String mergeMetadata(String existingJson, String patchJson) {
    try {
      ObjectNode merged = OBJECT_MAPPER.createObjectNode();
      copyObjectFields(existingJson, merged);
      copyObjectFields(patchJson, merged);
      return OBJECT_MAPPER.writeValueAsString(merged);
    } catch (Exception exception) {
      throw new IllegalArgumentException("invalid_job_metadata_json", exception);
    }
  }

  private static void copyObjectFields(String json, ObjectNode target) throws java.io.IOException {
    if (json == null || json.isBlank()) {
      return;
    }
    JsonNode node = OBJECT_MAPPER.readTree(json);
    if (!node.isObject()) {
      throw new IllegalArgumentException("job_metadata_must_be_json_object");
    }
    node.fields().forEachRemaining(entry -> target.set(entry.getKey(), entry.getValue()));
  }

  private static boolean missingText(ObjectNode object, String fieldName) {
    return !hasText(object, fieldName);
  }

  private static boolean hasText(ObjectNode object, String fieldName) {
    JsonNode value = object.get(fieldName);
    return value != null && value.isTextual() && !value.asText().isBlank();
  }

  public ConsultantJobDetailResponse activateJob(
      AccessRequest accessRequest,
      UUID organizationId,
      UUID actorId,
      JobId jobId,
      String reason) {
    requireConsultantJobWrite(accessRequest, AccessAction.UPDATE);
    if (jobIntakeApplicationService == null) {
      throw new IllegalStateException("jobIntakeApplicationService_not_configured");
    }
    Job activated = jobIntakeApplicationService.activateJob(organizationId, actorId, jobId, reason);
    List<JobRequirement> requirements = jobService.findRequirementsByJobIdAndOrganizationId(
        organizationId, jobId);
    Optional<JobScorecard> scorecard = jobService.findActiveScorecardByJobIdAndOrganizationId(
        organizationId, jobId);
    return ConsultantJobResponseMapper.toDetail(activated,
        requirements != null ? requirements : Collections.emptyList(),
        scorecard,
        industryPackService.findIndustryPackById(activated.industryPackId()));
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
        .dimensions(jsonTextOrStructuredValue(request.dimensions(), "invalid_job_scorecard_dimensions_json"))
        .scoringGuidance(request.scoringGuidance())
        .status(scorecardStatus(request.status()))
        .metadata(jsonTextOrStructuredValue(
            request.metadata() != null ? request.metadata() : "{}",
            "invalid_job_scorecard_metadata_json"))
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
        requirements != null ? requirements : Collections.emptyList(),
        activeScorecard,
        industryPackService.findIndustryPackById(job.industryPackId()));
  }

  private static UUID parseUuid(String value) {
    return value != null ? UUID.fromString(value) : null;
  }

  private UUID resolveIndustryPackId(String industryPackKey) {
    if (industryPackKey == null || industryPackKey.isBlank()) {
      return null;
    }
    return industryPackService.findIndustryPackByKey(industryPackKey)
        .orElseThrow(() -> new IllegalArgumentException("industryPackKey is not recognized"))
        .industryPackId()
        .value();
  }

  private static IndustryPackService defaultIndustryPackService() {
    IndustryPack generalPack = new IndustryPack(
        new IndustryPackId(UUID.fromString("00000000-0000-0000-0000-000000280001")),
        new IndustryPackKey("general"),
        "General",
        IndustryPackMaturity.COLD,
        true);
    OntologyVersion ontologyVersion = new OntologyVersion(
        UUID.fromString("00000000-0000-0000-0000-000000280011"),
        generalPack.industryPackId(),
        "ontology-general-v1",
        "fallback",
        "system",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2027-01-01T00:00:00Z"),
        null);
    return new IndustryPackService(new IndustryPackReadPort() {
      @Override
      public Optional<IndustryPack> findById(IndustryPackId industryPackId) {
        return generalPack.industryPackId().equals(industryPackId) ? Optional.of(generalPack) : Optional.empty();
      }

      @Override
      public Optional<IndustryPack> findByKey(IndustryPackKey packKey) {
        return generalPack.packKey().equals(packKey) ? Optional.of(generalPack) : Optional.empty();
      }

      @Override
      public Optional<OntologyVersion> findActiveOntologyVersion(IndustryPackId industryPackId, Instant asOf) {
        return generalPack.industryPackId().equals(industryPackId) ? Optional.of(ontologyVersion) : Optional.empty();
      }

      @Override
      public Optional<IndustryRoleFamilyTemplate> findRoleFamilyTemplate(
          IndustryPackId industryPackId,
          UUID ontologyVersionId,
          String roleFamily) {
        return Optional.empty();
      }
    });
  }

  private void requireShortlistBuilderService() {
    if (shortlistBuilderService == null) {
      throw new IllegalStateException("shortlistBuilderService_not_configured");
    }
  }

  private void recordShortlistTransition(
      Shortlist shortlist,
      String beforeStatus,
      String afterStatus,
      WorkflowActionCode actionCode,
      UUID actorId,
      String reason) {
    if (workflowTransitionAuditService == null) {
      return;
    }
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
        .beforeState(new WorkflowStateSnapshot("{\"status\":\"" + beforeStatus + "\"}"))
        .afterState(new WorkflowStateSnapshot("{\"status\":\"" + afterStatus + "\"}"))
        .reason(reason)
        .sourceType("consultant_shortlist_api")
        .sourceRefId(shortlist.shortlistId().value())
        .occurredAt(Instant.now())
        .build());
  }

  // ── Access enforcement ──────────────────────────────────────────────────────

  private void requireConsultantCompanyWrite(
      AccessRequest accessRequest, AccessAction action) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.COMPANY
        || accessRequest.action() != action) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "company_write_context_required",
          "Consultant company API requires a matching company write context."));
    }
  }

  private void requireConsultantJobWrite(
      AccessRequest accessRequest, AccessAction action) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.JOB
        || accessRequest.action() != action) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "job_write_context_required",
          "Consultant job API requires a matching job write context."));
    }
  }

  private void requireConsultantShortlistWrite(
      AccessRequest accessRequest, AccessAction action) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.SHORTLIST
        || accessRequest.action() != action) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "shortlist_write_context_required",
          "Consultant shortlist API requires a matching shortlist write context."));
    }
  }

  private void requireMutableBuilderShortlist(Shortlist shortlist) {
    if (shortlist.status() != ShortlistStatus.DRAFT
        && shortlist.status() != ShortlistStatus.READY_FOR_REVIEW) {
      throw new IllegalStateException("shortlist_builder_locked_after_send");
    }
  }

  private void requireBuilderCardStatus(ShortlistCandidateCardStatus status) {
    if (status == null) {
      return;
    }
    if (status != ShortlistCandidateCardStatus.INCLUDED
        && status != ShortlistCandidateCardStatus.REMOVED) {
      throw new IllegalArgumentException("shortlist_builder_card_status_requires_dedicated_workflow");
    }
  }
}
