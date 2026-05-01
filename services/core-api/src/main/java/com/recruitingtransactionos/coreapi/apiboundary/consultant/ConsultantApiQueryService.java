package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanyDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanySummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper.ConsultantCompanyResponseMapper;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper.ConsultantJobResponseMapper;
import com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper.ConsultantShortlistResponseMapper;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyContact;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantApiQueryService {

  private final CompanyService companyService;
  private final JobService jobService;
  private final ShortlistService shortlistService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantApiQueryService(
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService) {
    this(companyService, jobService, shortlistService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantApiQueryService(
      CompanyService companyService,
      JobService jobService,
      ShortlistService shortlistService,
      PermissionEnforcer permissionEnforcer) {
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  // ── Companies ──────────────────────────────────────────────────────────────

  public PagedResult<ConsultantCompanySummaryResponse> listCompanies(
      AccessRequest accessRequest, PagedQuery pagedQuery, String statusFilter) {
    requireConsultantCompanyRead(accessRequest);
    UUID organizationId = pagedQuery.organizationId();

    CompanyStatus status = statusFilter != null
        ? CompanyStatus.fromWireValue(statusFilter) : null;
    List<Company> all = (status != null)
        ? companyService.findCompaniesByOrganizationIdAndStatus(organizationId, status)
        : allCompanies(organizationId);

    List<ConsultantCompanySummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(c -> {
          int contactCount = countContacts(organizationId, c.companyId());
          int jobCount = countJobsByCompany(organizationId, c.companyId());
          return ConsultantCompanyResponseMapper.toSummary(c, contactCount, jobCount);
        })
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  public Optional<ConsultantCompanyDetailResponse> getCompanyDetail(
      AccessRequest accessRequest, UUID organizationId, CompanyId companyId) {
    requireConsultantCompanyRead(accessRequest);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");

    return companyService.findCompanyByIdAndOrganizationId(organizationId, companyId)
        .map(company -> {
          List<CompanyContact> contacts =
              companyService.findContactsByCompanyIdAndOrganizationId(organizationId, companyId);
          int jobCount = countJobsByCompany(organizationId, companyId);
          return ConsultantCompanyResponseMapper.toDetail(company, contacts, jobCount);
        });
  }

  // ── Jobs ────────────────────────────────────────────────────────────────────

  public PagedResult<ConsultantJobSummaryResponse> listJobs(
      AccessRequest accessRequest, PagedQuery pagedQuery, String statusFilter,
      String companyIdFilter) {
    requireConsultantJobRead(accessRequest);
    UUID organizationId = pagedQuery.organizationId();

    List<Job> all;
    if (companyIdFilter != null) {
      CompanyId cid = new CompanyId(UUID.fromString(companyIdFilter));
      all = jobService.findJobsByCompanyIdAndOrganizationId(organizationId, cid);
    } else if (statusFilter != null) {
      JobStatus status = JobStatus.fromWireValue(statusFilter);
      all = jobService.findJobsByOrganizationIdAndStatus(organizationId, status);
    } else {
      all = allJobs(organizationId);
    }

    List<ConsultantJobSummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(ConsultantJobResponseMapper::toSummary)
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  public Optional<ConsultantJobDetailResponse> getJobDetail(
      AccessRequest accessRequest, UUID organizationId, JobId jobId) {
    requireConsultantJobRead(accessRequest);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");

    return jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .map(job -> {
          List<JobRequirement> requirements =
              jobService.findRequirementsByJobIdAndOrganizationId(organizationId, jobId);
          Optional<JobScorecard> scorecard =
              jobService.findActiveScorecardByJobIdAndOrganizationId(organizationId, jobId);
          return ConsultantJobResponseMapper.toDetail(job, requirements, scorecard);
        });
  }

  // ── Shortlists ──────────────────────────────────────────────────────────────

  public PagedResult<ConsultantShortlistSummaryResponse> listShortlists(
      AccessRequest accessRequest, PagedQuery pagedQuery, String jobIdFilter) {
    requireConsultantShortlistRead(accessRequest);
    UUID organizationId = pagedQuery.organizationId();

    List<Shortlist> all;
    if (jobIdFilter != null) {
      JobId jid = new JobId(UUID.fromString(jobIdFilter));
      all = shortlistService.findShortlistsByJobIdAndOrganizationId(organizationId, jid);
    } else {
      all = allShortlists(organizationId);
    }

    List<ConsultantShortlistSummaryResponse> items = all.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(s -> {
          int cardCount = countCards(organizationId, s.shortlistId());
          return ConsultantShortlistResponseMapper.toSummary(s, cardCount);
        })
        .toList();
    return PagedResult.of(items, all.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  public Optional<ConsultantShortlistDetailResponse> getShortlistDetail(
      AccessRequest accessRequest, UUID organizationId, ShortlistId shortlistId) {
    requireConsultantShortlistRead(accessRequest);
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");

    return shortlistService.findShortlistByIdAndOrganizationId(organizationId, shortlistId)
        .map(shortlist -> {
          List<ShortlistCandidateCard> cards =
              shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlistId);
          return ConsultantShortlistResponseMapper.toDetail(shortlist, cards);
        });
  }

  // ── Access enforcement ──────────────────────────────────────────────────────

  private void requireConsultantCompanyRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    requireResourceAndAction(accessRequest, ResourceType.COMPANY, "company");
  }

  private void requireConsultantJobRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    requireResourceAndAction(accessRequest, ResourceType.JOB, "job");
  }

  private void requireConsultantShortlistRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    requireResourceAndAction(accessRequest, ResourceType.SHORTLIST, "shortlist");
  }

  private static void requireResourceAndAction(
      AccessRequest accessRequest, ResourceType expectedType, String label) {
    if (accessRequest.resourceType() != expectedType
        || accessRequest.action() != AccessAction.READ) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          label + "_read_context_required",
          "Consultant " + label + " API requires a " + label + " read context."));
    }
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  private List<Company> allCompanies(UUID organizationId) {
    return companyService.findAllCompaniesByOrganizationId(organizationId);
  }

  private List<Job> allJobs(UUID organizationId) {
    return jobService.findAllJobsByOrganizationId(organizationId);
  }

  private List<Shortlist> allShortlists(UUID organizationId) {
    return shortlistService.findAllShortlistsByOrganizationId(organizationId);
  }

  private int countContacts(UUID organizationId, CompanyId companyId) {
    return companyService.findContactsByCompanyIdAndOrganizationId(organizationId, companyId).size();
  }

  private int countJobsByCompany(UUID organizationId, CompanyId companyId) {
    return jobService.findJobsByCompanyIdAndOrganizationId(organizationId, companyId).size();
  }

  private int countCards(UUID organizationId, ShortlistId shortlistId) {
    return shortlistService.findCardsByShortlistIdAndOrganizationId(organizationId, shortlistId).size();
  }
}
