package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class CandidateConsentWorkflowService {

  private static final Set<DisclosureLevel> DEFAULT_DISCLOSURE_LEVELS = Set.of(
      DisclosureLevel.L2_CLIENT_SAFE,
      DisclosureLevel.L3_CONSENTED_DETAIL,
      DisclosureLevel.L4_IDENTITY_DISCLOSED);

  private final ConsentRecordPort consentRecordPort;
  private final CandidateProfileService candidateProfileService;
  private final JobService jobService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;

  @Autowired
  public CandidateConsentWorkflowService(
      ConsentRecordPort consentRecordPort,
      CandidateProfileService candidateProfileService,
      JobService jobService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this.consentRecordPort = Objects.requireNonNull(consentRecordPort, "consentRecordPort must not be null");
    this.candidateProfileService = Objects.requireNonNull(
        candidateProfileService,
        "candidateProfileService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.workflowTransitionAuditService = Objects.requireNonNull(
        workflowTransitionAuditService,
        "workflowTransitionAuditService must not be null");
  }

  public ConsentRecord requestConsent(
      UUID organizationId,
      UUID consultantActorId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef,
      String consentTextVersion,
      Instant expiresAt) {
    CandidateProfile profile = requireProfile(organizationId, candidateProfileRef);
    Job job = requireJob(organizationId, jobRef);
    Optional<ConsentRecord> existing = consentRecordPort.findLatestByCandidateProfileAndJob(
        organizationId,
        candidateRef,
        candidateProfileRef,
        jobRef);
    if (existing.isPresent() && existing.orElseThrow().status() == ConsentStatus.REQUESTED) {
      return existing.orElseThrow();
    }
    Instant now = Instant.now();
    ConsentRecord record = consentRecordPort.append(new ConsentRecord(
        UUID.randomUUID().toString(),
        organizationId,
        candidateRef,
        candidateProfileRef,
        job.jobId().value().toString(),
        Integer.toString(profile.profileVersion().value()),
        consentTextVersion,
        ConsentStatus.REQUESTED,
        DEFAULT_DISCLOSURE_LEVELS,
        now,
        expiresAt,
        false));
    recordEvent(
        organizationId,
        UUID.nameUUIDFromBytes(record.consentRecordRef().getBytes()),
        null,
        WorkflowActionCode.CONSENT_REQUESTED,
        ActorRole.CONSULTANT,
        consultantActorId,
        "not_requested",
        ConsentStatus.REQUESTED.wireValue(),
        "consultant requested candidate consent",
        record.confirmedAt());
    return record;
  }

  public CandidateConsentView viewLatestConsent(
      UUID organizationId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef,
      UUID candidateActorId) {
    ConsentRecord latest = latestConsentRecord(
        organizationId,
        candidateRef,
        candidateProfileRef,
        jobRef);
    ConsentRecord current = latest;
    if (latest.status() == ConsentStatus.REQUESTED) {
      Instant now = Instant.now();
      current = consentRecordPort.append(new ConsentRecord(
          UUID.randomUUID().toString(),
          latest.organizationId(),
          latest.candidateRef(),
          latest.candidateProfileRef(),
          latest.jobRef(),
          latest.profileVersion(),
          latest.consentTextVersion(),
          ConsentStatus.VIEWED_BY_CANDIDATE,
          latest.permittedDisclosureLevels(),
          now,
          latest.expiresAt(),
          false));
      recordEvent(
          organizationId,
          UUID.nameUUIDFromBytes(current.consentRecordRef().getBytes()),
          null,
          WorkflowActionCode.CONSENT_VIEWED_BY_CANDIDATE,
          ActorRole.CANDIDATE,
          candidateActorId,
          ConsentStatus.REQUESTED.wireValue(),
          ConsentStatus.VIEWED_BY_CANDIDATE.wireValue(),
          "candidate viewed consent request",
          now);
    }
    return buildConsentView(organizationId, candidateProfileRef, jobRef, current);
  }

  public CandidateConsentView latestConsentSnapshot(
      UUID organizationId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef) {
    return buildConsentView(
        organizationId,
        candidateProfileRef,
        jobRef,
        latestConsentRecord(organizationId, candidateRef, candidateProfileRef, jobRef));
  }

  public ConsentRecord respondToConsent(
      UUID organizationId,
      UUID candidateActorId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef,
      boolean approve) {
    ConsentRecord latest = latestConsentRecord(
        organizationId,
        candidateRef,
        candidateProfileRef,
        jobRef);
    CandidateProfile profile = requireProfile(organizationId, candidateProfileRef);
    Instant now = Instant.now();
    if (latest.revoked() || latest.status() == ConsentStatus.REVOKED) {
      throw new IllegalArgumentException("consent_request_revoked");
    }
    if (latest.isExpiredAt(now)) {
      throw new IllegalArgumentException("consent_request_expired");
    }
    String currentProfileVersion = Integer.toString(profile.profileVersion().value());
    if (!latest.profileVersion().equals(currentProfileVersion)) {
      throw new IllegalArgumentException("consent_profile_version_mismatch");
    }
    ConsentStatus nextStatus = approve ? ConsentStatus.CONFIRMED : ConsentStatus.DECLINED;
    if (latest.status() == nextStatus) {
      return latest;
    }
    if (latest.status() == ConsentStatus.CONFIRMED || latest.status() == ConsentStatus.DECLINED) {
      throw new IllegalArgumentException("consent_already_resolved");
    }
    ConsentRecord updated = consentRecordPort.append(new ConsentRecord(
        UUID.randomUUID().toString(),
        organizationId,
        candidateRef,
        candidateProfileRef,
        jobRef,
        currentProfileVersion,
        latest.consentTextVersion(),
        nextStatus,
        latest.permittedDisclosureLevels(),
        now,
        latest.expiresAt(),
        false));
    recordEvent(
        organizationId,
        UUID.nameUUIDFromBytes(updated.consentRecordRef().getBytes()),
        null,
        approve ? WorkflowActionCode.CONSENT_CONFIRMED : WorkflowActionCode.CONSENT_DECLINED,
        ActorRole.CANDIDATE,
        candidateActorId,
        latest.status().wireValue(),
        nextStatus.wireValue(),
        approve ? "candidate confirmed consent" : "candidate declined consent",
        now);
    return updated;
  }

  private ConsentRecord latestConsentRecord(
      UUID organizationId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef) {
    return consentRecordPort.findLatestByCandidateProfileAndJob(
            organizationId,
            candidateRef,
            candidateProfileRef,
            jobRef)
        .orElseThrow(() -> new IllegalArgumentException("consent_request_not_found"));
  }

  private CandidateConsentView buildConsentView(
      UUID organizationId,
      String candidateProfileRef,
      String jobRef,
      ConsentRecord consentRecord) {
    CandidateProfile profile = requireProfile(organizationId, candidateProfileRef);
    Job job = requireJob(organizationId, jobRef);
    return new CandidateConsentView(
        consentRecord,
        Integer.toString(profile.profileVersion().value()),
        job.title(),
        sharedFieldPreview(profile));
  }

  private CandidateProfile requireProfile(UUID organizationId, String candidateProfileRef) {
    return candidateProfileService.findCandidateProfileByIdAndOrganizationId(
            organizationId,
            new com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId(UUID.fromString(candidateProfileRef)))
        .orElseThrow(() -> new IllegalArgumentException("candidate_profile_not_found"));
  }

  private Job requireJob(UUID organizationId, String jobRef) {
    return jobService.findJobByIdAndOrganizationId(organizationId, new JobId(UUID.fromString(jobRef)))
        .orElseThrow(() -> new IllegalArgumentException("job_not_found"));
  }

  private List<SharedFieldPreview> sharedFieldPreview(CandidateProfile profile) {
    return profile.fields().stream()
        .filter(field -> Set.of(
            "profile.headline",
            "profile.summary",
            "skills.primary_skills",
            "identity.full_name",
            "contact.email",
            "contact.phone",
            "location.current_city").contains(field.fieldPath().value()))
        .limit(8)
        .map(field -> new SharedFieldPreview(field.fieldPath().value(), field.value().jsonValue()))
        .toList();
  }

  private void recordEvent(
      UUID organizationId,
      UUID entityId,
      Integer entityVersion,
      WorkflowActionCode actionCode,
      ActorRole actorRole,
      UUID actorId,
      String beforeStatus,
      String afterStatus,
      String reason,
      Instant occurredAt) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(organizationId)
        .entityNamespace("workflow")
        .entityType(WorkflowEntityType.CONSENT.wireValue())
        .entityId(entityId)
        .entityVersion(entityVersion)
        .actionCode(actionCode.wireValue())
        .actorType(actorRole)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(snapshot(beforeStatus))
        .afterState(snapshot(afterStatus))
        .reason(reason)
        .sourceType("portal_api")
        .sourceRefId(entityId)
        .occurredAt(occurredAt)
        .build());
  }

  private static WorkflowStateSnapshot snapshot(String status) {
    return new WorkflowStateSnapshot("{\"status\":\"" + status + "\"}");
  }

  public record CandidateConsentView(
      ConsentRecord consentRecord,
      String currentProfileVersion,
      String jobTitle,
      List<SharedFieldPreview> sharedFields) {

    public CandidateConsentView {
      Objects.requireNonNull(consentRecord, "consentRecord must not be null");
      currentProfileVersion = requireNonBlank(currentProfileVersion, "currentProfileVersion");
      jobTitle = requireNonBlank(jobTitle, "jobTitle");
      sharedFields = List.copyOf(Objects.requireNonNull(sharedFields, "sharedFields must not be null"));
    }
  }

  public record SharedFieldPreview(String fieldPath, String jsonValue) {
    public SharedFieldPreview {
      fieldPath = requireNonBlank(fieldPath, "fieldPath");
      jsonValue = requireNonBlank(jsonValue, "jsonValue");
    }
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
