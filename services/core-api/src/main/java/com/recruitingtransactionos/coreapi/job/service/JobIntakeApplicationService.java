package com.recruitingtransactionos.coreapi.job.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewQueryService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JobIntakeApplicationService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final JobService jobService;
  private final CompanyService companyService;
  private final JobActivationGateService activationGateService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;

  public JobIntakeApplicationService(
      JobService jobService,
      CompanyService companyService,
      JobActivationGateService activationGateService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.activationGateService = Objects.requireNonNull(
        activationGateService, "activationGateService must not be null");
    this.workflowTransitionAuditService = Objects.requireNonNull(
        workflowTransitionAuditService, "workflowTransitionAuditService must not be null");
  }

  public Job createClientJobSubmission(
      UUID organizationId,
      UUID actorId,
      CompanyId companyId,
      String title,
      String description,
      String location,
      String compensation,
      String commercialTerms,
      List<String> clarificationQuestions) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    var company = companyService.findCompanyByIdAndOrganizationId(organizationId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("company_not_found_in_organization"));
    if (!metadataContainsActor(company.metadata(), actorId)) {
      throw new IllegalArgumentException("client_company_profile_not_owned_by_actor");
    }

    Instant now = Instant.now();
    Job job = Job.builder()
        .jobId(new JobId(UUID.randomUUID()))
        .organizationId(organizationId)
        .companyId(companyId)
        .title(title)
        .description(description)
        .location(location)
        .compensation(compensation)
        .status(JobStatus.SUBMITTED)
        .commercialTerms(commercialTerms)
        .metadata(mergeMetadata(null, Map.of(
            "submissionSource", "client_portal",
            "clientActorId", actorId.toString(),
            "clarificationQuestions", clarificationQuestions,
            "clientSubmissionAt", now.toString())))
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build();
    Job created = jobService.createJob(job);
    auditStatusTransition(
        created,
        "draft",
        JobStatus.SUBMITTED,
        WorkflowActionCode.JOB_SUBMITTED,
        ActorRole.CLIENT,
        actorId,
        "client portal job intake submitted");
    return created;
  }

  public Job answerClarification(
      UUID organizationId,
      UUID actorId,
      JobId jobId,
      List<String> answers,
      String description,
      String location,
      String compensation,
      String commercialTerms) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Job existing = jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException("job_not_found_in_organization"));
    if (!metadataContainsActor(existing.metadata(), actorId)) {
      throw new IllegalArgumentException("client_job_submission_not_owned_by_actor");
    }
    Instant now = Instant.now();
    Job updated = Job.builder()
        .jobId(existing.jobId())
        .organizationId(existing.organizationId())
        .companyId(existing.companyId())
        .title(existing.title())
        .description(nonBlankOrFallback(description, existing.description()))
        .location(nonBlankOrFallback(location, existing.location()))
        .seniorityBand(existing.seniorityBand())
        .roleFamily(existing.roleFamily())
        .employmentType(existing.employmentType())
        .compensation(nonBlankOrFallback(compensation, existing.compensation()))
        .status(JobStatus.INTAKE_REVIEW)
        .commercialTerms(nonBlankOrFallback(commercialTerms, existing.commercialTerms()))
        .ownerConsultantId(existing.ownerConsultantId())
        .activatedAt(existing.activatedAt())
        .closedAt(existing.closedAt())
        .closeReason(existing.closeReason())
        .industryPackId(existing.industryPackId())
        .metadata(mergeMetadata(existing.metadata(), Map.of(
            "clarificationAnswers", answers,
            "clarificationAnsweredAt", now.toString(),
            "clarificationActorId", actorId.toString())))
        .createdAt(existing.createdAt())
        .updatedAt(now)
        .version(existing.version())
        .build();
    Job persisted = jobService.updateJob(updated);
    auditStatusTransition(
        persisted,
        existing.status().wireValue(),
        JobStatus.INTAKE_REVIEW,
        WorkflowActionCode.JOB_INTAKE_REVIEW_STARTED,
        ActorRole.CLIENT,
        actorId,
        "client clarification answers moved job back into intake review");
    return persisted;
  }

  public Job applyReviewedFacts(
      UUID organizationId,
      UUID actorId,
      Optional<JobId> requestedJobId,
      Optional<CompanyId> requestedCompanyId,
      List<IntakeReviewQueryService.ReviewedCleanFact> facts,
      String reason) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Instant now = Instant.now();

    Job base = requestedJobId.flatMap(id -> jobService.findJobByIdAndOrganizationId(organizationId, id))
        .orElseGet(() -> Job.builder()
            .jobId(new JobId(UUID.randomUUID()))
            .organizationId(organizationId)
            .companyId(requestedCompanyId.orElseThrow(() -> new IllegalArgumentException(
                "job_publish_requires_company_id")))
            .title("Pending intake job")
            .status(JobStatus.INTAKE_REVIEW)
            .metadata("{}")
            .createdAt(now)
            .updatedAt(now)
            .version(1)
            .build());

    CompanyId companyId = requestedCompanyId.orElse(base.companyId());
    companyService.findCompanyByIdAndOrganizationId(organizationId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("job_publish_company_not_found"));

    Job merged = Job.builder()
        .jobId(base.jobId())
        .organizationId(organizationId)
        .companyId(companyId)
        .title(firstApprovedValue(facts, "title", "job.title").orElse(base.title()))
        .description(firstApprovedValue(facts, "description", "job.description")
            .orElse(base.description()))
        .location(firstApprovedValue(facts, "location", "job.location").orElse(base.location()))
        .seniorityBand(firstApprovedValue(facts, "seniorityBand", "job.seniority_band")
            .orElse(base.seniorityBand()))
        .roleFamily(firstApprovedValue(facts, "roleFamily", "job.role_family")
            .orElse(base.roleFamily()))
        .employmentType(firstApprovedValue(facts, "employmentType", "job.employment_type")
            .orElse(base.employmentType()))
        .compensation(firstApprovedValue(facts, "compensation", "job.compensation")
            .orElse(base.compensation()))
        .status(JobStatus.INTAKE_REVIEW)
        .commercialTerms(firstApprovedValue(facts, "commercialTerms", "job.commercial_terms")
            .orElse(base.commercialTerms()))
        .ownerConsultantId(base.ownerConsultantId())
        .activatedAt(base.activatedAt())
        .closedAt(base.closedAt())
        .closeReason(base.closeReason())
        .industryPackId(base.industryPackId())
        .metadata(mergeMetadata(base.metadata(), Map.of(
            "intakePublishedAt", now.toString(),
            "intakePublishedByActorId", actorId.toString(),
            "intakePublishReason", safeReason(reason))))
        .createdAt(base.createdAt())
        .updatedAt(now)
        .version(base.version())
        .build();

    return requestedJobId.isPresent() ? jobService.updateJob(merged) : jobService.createJob(merged);
  }

  public JobActivationGateResult activationGate(UUID organizationId, JobId jobId) {
    Job job = jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException("job_not_found_in_organization"));
    List<JobRequirement> requirements =
        jobService.findRequirementsByJobIdAndOrganizationId(organizationId, jobId);
    Optional<JobScorecard> scorecard =
        jobService.findActiveScorecardByJobIdAndOrganizationId(organizationId, jobId);
    return activationGateService.evaluate(job, requirements, scorecard);
  }

  public Job activateJob(UUID organizationId, UUID actorId, JobId jobId, String reason) {
    Job existing = jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException("job_not_found_in_organization"));
    JobActivationGateResult gate = activationGate(organizationId, jobId);
    if (!gate.activationAllowed()) {
      throw new IllegalArgumentException(
          "job_activation_blocked:" + String.join(",", gate.blockerReasons()));
    }
    Instant now = Instant.now();
    Job statusBase = existing;
    if (existing.status() != JobStatus.CONTRACT_PENDING) {
      Job contractPending = withStatus(existing, JobStatus.CONTRACT_PENDING, now, existing.activatedAt());
      statusBase = jobService.updateJob(contractPending);
      auditStatusTransition(
          statusBase,
          existing.status().wireValue(),
          JobStatus.CONTRACT_PENDING,
          WorkflowActionCode.JOB_CONTRACT_PENDING,
          ActorRole.CONSULTANT,
          actorId,
          "job activation gate satisfied; commercial and contract placeholder recorded");
    }

    Job activated = Job.builder()
        .jobId(statusBase.jobId())
        .organizationId(statusBase.organizationId())
        .companyId(statusBase.companyId())
        .title(statusBase.title())
        .description(statusBase.description())
        .location(statusBase.location())
        .seniorityBand(statusBase.seniorityBand())
        .roleFamily(statusBase.roleFamily())
        .employmentType(statusBase.employmentType())
        .compensation(statusBase.compensation())
        .status(JobStatus.ACTIVATED)
        .commercialTerms(statusBase.commercialTerms())
        .ownerConsultantId(statusBase.ownerConsultantId())
        .activatedAt(now)
        .closedAt(statusBase.closedAt())
        .closeReason(statusBase.closeReason())
        .industryPackId(statusBase.industryPackId())
        .metadata(mergeMetadata(statusBase.metadata(), Map.of(
            "activatedAt", now.toString(),
            "activatedByActorId", actorId.toString())))
        .createdAt(statusBase.createdAt())
        .updatedAt(now)
        .version(statusBase.version())
        .build();
    Job persisted = jobService.updateJob(activated);
    auditStatusTransition(
        persisted,
        statusBase.status().wireValue(),
        JobStatus.ACTIVATED,
        WorkflowActionCode.JOB_ACTIVATED,
        ActorRole.CONSULTANT,
        actorId,
        safeReason(reason));
    return persisted;
  }

  private static Job withStatus(Job existing, JobStatus status, Instant updatedAt, Instant activatedAt) {
    return Job.builder()
        .jobId(existing.jobId())
        .organizationId(existing.organizationId())
        .companyId(existing.companyId())
        .title(existing.title())
        .description(existing.description())
        .location(existing.location())
        .seniorityBand(existing.seniorityBand())
        .roleFamily(existing.roleFamily())
        .employmentType(existing.employmentType())
        .compensation(existing.compensation())
        .status(status)
        .commercialTerms(existing.commercialTerms())
        .ownerConsultantId(existing.ownerConsultantId())
        .activatedAt(activatedAt)
        .closedAt(existing.closedAt())
        .closeReason(existing.closeReason())
        .industryPackId(existing.industryPackId())
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(updatedAt)
        .version(existing.version())
        .build();
  }

  private void auditStatusTransition(
      Job job,
      String beforeStatus,
      JobStatus afterStatus,
      WorkflowActionCode actionCode,
      ActorRole actorRole,
      UUID actorId,
      String reason) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(job.organizationId())
        .entityNamespace("recruiting")
        .entityType(WorkflowEntityType.JOB.wireValue())
        .entityId(job.jobId().value())
        .entityVersion(job.version())
        .actionCode(actionCode.wireValue())
        .actorType(actorRole)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState("{\"status\":\"" + beforeStatus + "\"}")
        .afterState("{\"status\":\"" + afterStatus.wireValue() + "\"}")
        .reason(reason)
        .idempotencyKey("job-status-" + job.jobId().value() + "-" + afterStatus.wireValue()
            + "-" + actorId)
        .sourceType("job_intake_application")
        .sourceRefId(job.jobId().value())
        .occurredAt(Instant.now())
        .build());
  }

  private static Optional<String> firstApprovedValue(
      List<IntakeReviewQueryService.ReviewedCleanFact> facts,
      String... acceptedFieldPaths) {
    for (IntakeReviewQueryService.ReviewedCleanFact fact : facts) {
      if (fact.latestReview() == null || fact.claimId() == null) {
        continue;
      }
      if (!"APPROVED".equals(fact.latestReview().decision().name())) {
        continue;
      }
      for (String acceptedFieldPath : acceptedFieldPaths) {
        if (acceptedFieldPath.equalsIgnoreCase(fact.candidate().targetFieldPath())) {
          return Optional.ofNullable(fact.candidate().proposedValue());
        }
      }
    }
    return Optional.empty();
  }

  private static String mergeMetadata(String existingJson, Map<String, Object> patch) {
    try {
      Map<String, Object> merged = new LinkedHashMap<>();
      if (existingJson != null && !existingJson.isBlank()) {
        merged.putAll(OBJECT_MAPPER.readValue(existingJson, MAP_TYPE));
      }
      merged.putAll(patch);
      return OBJECT_MAPPER.writeValueAsString(merged);
    } catch (Exception exception) {
      throw new IllegalArgumentException("invalid_job_metadata_json", exception);
    }
  }

  public static List<String> clarificationAnswersFromMetadata(String metadataJson) {
    return stringListFromMetadata(metadataJson, "clarificationAnswers");
  }

  public static List<String> clarificationQuestionsFromMetadata(String metadataJson) {
    return stringListFromMetadata(metadataJson, "clarificationQuestions");
  }

  private static List<String> stringListFromMetadata(String metadataJson, String fieldName) {
    if (metadataJson == null || metadataJson.isBlank()) {
      return List.of();
    }
    try {
      Map<String, Object> metadata = OBJECT_MAPPER.readValue(metadataJson, MAP_TYPE);
      Object raw = metadata.get(fieldName);
      if (!(raw instanceof List<?> list)) {
        return List.of();
      }
      List<String> values = new ArrayList<>();
      for (Object entry : list) {
        if (entry instanceof String text && !text.isBlank()) {
          values.add(text.strip());
        }
      }
      return values;
    } catch (Exception ignored) {
      return List.of();
    }
  }

  public static boolean metadataContainsActor(String metadataJson, UUID actorId) {
    if (actorId == null) {
      return false;
    }
    return clientActorIdFromMetadata(metadataJson)
        .map(actorId::equals)
        .orElse(false);
  }

  private static Optional<UUID> clientActorIdFromMetadata(String metadataJson) {
    if (metadataJson == null || metadataJson.isBlank()) {
      return Optional.empty();
    }
    try {
      Map<String, Object> metadata = OBJECT_MAPPER.readValue(metadataJson, MAP_TYPE);
      Object value = metadata.get("clientActorId");
      if (!(value instanceof String text) || text.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(UUID.fromString(text.strip()));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private static String safeReason(String reason) {
    return reason == null || reason.isBlank() ? "job intake action" : reason.strip();
  }

  private static String nonBlankOrFallback(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.strip();
  }
}
