package com.recruitingtransactionos.coreapi.job.service;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.port.JobPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobScorecardPersistencePort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JobService {

  private final JobPersistencePort jobPort;
  private final JobRequirementPersistencePort requirementPort;
  private final JobScorecardPersistencePort scorecardPort;

  public JobService(
      JobPersistencePort jobPort,
      JobRequirementPersistencePort requirementPort,
      JobScorecardPersistencePort scorecardPort) {
    this.jobPort = Objects.requireNonNull(jobPort, "jobPort must not be null");
    this.requirementPort = Objects.requireNonNull(
        requirementPort, "requirementPort must not be null");
    this.scorecardPort = Objects.requireNonNull(
        scorecardPort, "scorecardPort must not be null");
  }

  public Job createJob(Job job) {
    Objects.requireNonNull(job, "job must not be null");
    return jobPort.create(job);
  }

  public Optional<Job> findJobByIdAndOrganizationId(UUID organizationId, JobId jobId) {
    return jobPort.findByIdAndOrganizationId(organizationId, jobId);
  }

  public List<Job> findJobsByOrganizationIdAndStatus(UUID organizationId, JobStatus status) {
    return jobPort.findByOrganizationIdAndStatus(organizationId, status);
  }

  public List<Job> findJobsByCompanyIdAndOrganizationId(
      UUID organizationId, CompanyId companyId) {
    return jobPort.findByCompanyIdAndOrganizationId(organizationId, companyId);
  }

  public List<Job> findAllJobsByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    return jobPort.findAllByOrganizationId(organizationId);
  }

  public JobRequirement createRequirement(JobRequirement requirement) {
    return requirementPort.create(requirement);
  }

  public List<JobRequirement> findRequirementsByJobIdAndOrganizationId(
      UUID organizationId, JobId jobId) {
    return requirementPort.findByJobIdAndOrganizationId(organizationId, jobId);
  }

  public JobScorecard createScorecard(JobScorecard scorecard) {
    return scorecardPort.create(scorecard);
  }

  public Optional<JobScorecard> findActiveScorecardByJobIdAndOrganizationId(
      UUID organizationId, JobId jobId) {
    return scorecardPort.findActiveByJobIdAndOrganizationId(organizationId, jobId);
  }
}
