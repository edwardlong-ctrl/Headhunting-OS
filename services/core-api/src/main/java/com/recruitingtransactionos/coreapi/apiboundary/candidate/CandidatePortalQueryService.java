package com.recruitingtransactionos.coreapi.apiboundary.candidate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateDocumentSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateFollowUpFormResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateMeResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateOpportunityDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateOpportunityResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateProfileReviewResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateTimelineResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantDocumentUploadResponse;
import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.apiboundary.PagedResult;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentId;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentStatus;
import com.recruitingtransactionos.coreapi.candidatedocument.service.CandidateDocumentService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.UpsertCandidateProfileFieldRequest;
import com.recruitingtransactionos.coreapi.clientsafeprojection.CompanyNameGeneralizationPolicy;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.CandidateConsentWorkflowService;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentDisclosureWorkflowEntityIds;
import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public final class CandidatePortalQueryService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String FOLLOW_UP_SUBMISSION_NOTE_KIND = "candidate_follow_up_submission";

  private final CandidateService candidateService;
  private final CandidateProfileService candidateProfileService;
  private final CandidateDocumentService candidateDocumentService;
  private final CandidateCompanyInteractionService interactionService;
  private final DocumentUploadService documentUploadService;
  private final WorkflowAuditQueryService workflowAuditQueryService;
  private final JobService jobService;
  private final CompanyService companyService;
  private final CandidateConsentWorkflowService candidateConsentWorkflowService;
  private final ConsentRecordPort consentRecordPort;
  private final WorkflowEventService workflowEventService;

  @Autowired
  public CandidatePortalQueryService(
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      CandidateDocumentService candidateDocumentService,
      CandidateCompanyInteractionService interactionService,
      DocumentUploadService documentUploadService,
      WorkflowAuditQueryService workflowAuditQueryService,
      JobService jobService,
      CompanyService companyService,
      CandidateConsentWorkflowService candidateConsentWorkflowService,
      ConsentRecordPort consentRecordPort,
      WorkflowEventService workflowEventService) {
    this.candidateService = Objects.requireNonNull(candidateService, "candidateService must not be null");
    this.candidateProfileService = Objects.requireNonNull(
        candidateProfileService, "candidateProfileService must not be null");
    this.candidateDocumentService = Objects.requireNonNull(
        candidateDocumentService, "candidateDocumentService must not be null");
    this.interactionService = Objects.requireNonNull(interactionService, "interactionService must not be null");
    this.documentUploadService = Objects.requireNonNull(
        documentUploadService, "documentUploadService must not be null");
    this.workflowAuditQueryService = Objects.requireNonNull(
        workflowAuditQueryService, "workflowAuditQueryService must not be null");
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    this.candidateConsentWorkflowService = Objects.requireNonNull(
        candidateConsentWorkflowService, "candidateConsentWorkflowService must not be null");
    this.consentRecordPort = Objects.requireNonNull(consentRecordPort, "consentRecordPort must not be null");
    this.workflowEventService = Objects.requireNonNull(
        workflowEventService, "workflowEventService must not be null");
  }

  public CandidateMeResponse buildMe(UUID organizationId, UUID userAccountId, String defaultDisplayName) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    CandidateId candidateId = new CandidateId(userAccountId);
    Optional<Candidate> candidate = candidateService.findCandidateByIdAndOrganizationId(
        organizationId, candidateId);
    Optional<CandidateProfile> profile = candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        organizationId, candidateId);
    int documentCount = candidateDocumentService.findDocumentsByCandidateIdAndOrganizationId(
        organizationId, candidateId).size();
    int opportunityCount = interactionService.findInteractionsByCandidateId(
        organizationId, candidateId).size();
    int pendingFollowUpCount = profile.map(this::countPendingFollowUps).orElse(0);
    String profileVersion = profile
        .map(p -> Integer.toString(p.profileVersion().value()))
        .orElse("unknown");
    String displayName = resolveDisplayName(profile, defaultDisplayName);
    return new CandidateMeResponse(
        userAccountId.toString(),
        displayName,
        organizationId.toString(),
        profileVersion,
        documentCount,
        opportunityCount,
        pendingFollowUpCount);
  }

  public CandidateProfileReviewResponse buildProfileReview(UUID organizationId, String candidateRef) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Optional<CandidateProfile> profile = findCandidateProfile(organizationId, candidateRef);
    if (profile.isEmpty()) {
      return new CandidateProfileReviewResponse(candidateRef, "unknown", List.of());
    }
    CandidateProfile p = profile.orElseThrow();
    List<CandidateProfileReviewResponse.ProfileField> fields = p.fields().stream()
        .map(this::toProfileField)
        .toList();
    return new CandidateProfileReviewResponse(
        candidateRef,
        Integer.toString(p.profileVersion().value()),
        fields);
  }

  public CandidateProfileReviewResponse confirmProfileFields(
      UUID organizationId,
      String candidateRef,
      UUID candidateActorId,
      String requestedFieldPath) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Objects.requireNonNull(candidateActorId, "candidateActorId must not be null");
    CandidateProfile profile = findCandidateProfile(organizationId, candidateRef)
        .orElseThrow(() -> new IllegalArgumentException("candidate_profile_not_found"));
    List<CandidateProfileField> actionableFields = profile.fields().stream()
        .filter(CandidatePortalQueryService::requiresCandidateFollowUp)
        .toList();
    if (actionableFields.isEmpty()) {
      return buildProfileReview(organizationId, candidateRef);
    }
    String targetFieldPath = requestedFieldPath == null ? null : requestedFieldPath.strip();
    if (targetFieldPath == null || targetFieldPath.isEmpty()) {
      throw new IllegalArgumentException("candidate_confirmation_field_path_required");
    }
    CandidateProfileField existing = actionableFields.stream()
        .filter(field -> field.fieldPath().value().equals(targetFieldPath))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "candidate_confirmation_field_not_allowed:" + targetFieldPath));
    Instant confirmedAt = Instant.now();
    int remainingBefore = actionableFields.size();
    candidateProfileService.upsertCandidateProfileField(UpsertCandidateProfileFieldRequest.builder()
        .organizationId(organizationId)
        .candidateProfileId(profile.candidateProfileId())
        .fieldPath(existing.fieldPath())
        .value(existing.value())
        .fieldStatus(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED)
        .lineage(existing.lineage())
        .conflict(existing.conflict())
        .staleness(null)
        .lastReviewedAt(confirmedAt)
        .confirmedByActorId(candidateActorId)
        .confirmedAgainstProfileVersion(profile.profileVersion())
        .sourceClaimId(existing.sourceClaimId())
        .sourceReviewEventId(existing.sourceReviewEventId())
        .sourceWorkflowEventId(existing.sourceWorkflowEventId())
        .notes("candidate confirmed via candidate portal")
        .bulkApproval(false)
        .build());
    int remainingAfter = remainingBefore - 1;
    appendCandidateProfileConfirmedEvent(
        organizationId,
        profile,
        candidateRef,
        candidateActorId,
        existing.fieldPath().value(),
        remainingBefore,
        remainingAfter,
        confirmedAt);
    return buildProfileReview(organizationId, candidateRef);
  }

  public CandidateFollowUpFormResponse buildFollowUpForm(
      UUID organizationId,
      String candidateRef,
      String formId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    String normalizedFormId = requireCurrentProfileFollowUpForm(formId);
    Optional<CandidateProfile> profile = findCandidateProfile(organizationId, candidateRef);
    if (profile.isEmpty()) {
      return new CandidateFollowUpFormResponse(candidateRef, normalizedFormId, "unknown", List.of());
    }
    CandidateProfile currentProfile = profile.orElseThrow();
    List<CandidateFollowUpFormResponse.FollowUpItem> items = currentProfile.fields().stream()
        .filter(CandidatePortalQueryService::requiresCandidateFollowUp)
        .map(this::toFollowUpItem)
        .toList();
    return new CandidateFollowUpFormResponse(
        candidateRef,
        normalizedFormId,
        Integer.toString(currentProfile.profileVersion().value()),
        items);
  }

  public CandidateFollowUpFormResponse submitFollowUpAnswer(
      UUID organizationId,
      String candidateRef,
      UUID candidateActorId,
      String formId,
      String requestedFieldPath,
      String answer) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Objects.requireNonNull(candidateActorId, "candidateActorId must not be null");
    requireCurrentProfileFollowUpForm(formId);
    String targetFieldPath = requestedFieldPath == null ? null : requestedFieldPath.strip();
    if (targetFieldPath == null || targetFieldPath.isEmpty()) {
      throw new IllegalArgumentException("candidate_follow_up_field_path_required");
    }
    String normalizedAnswer = answer == null ? null : answer.strip();
    if (normalizedAnswer == null || normalizedAnswer.isEmpty()) {
      throw new IllegalArgumentException("candidate_follow_up_answer_required");
    }
    CandidateProfile profile = findCandidateProfile(organizationId, candidateRef)
        .orElseThrow(() -> new IllegalArgumentException("candidate_profile_not_found"));
    List<CandidateProfileField> followUpFields = profile.fields().stream()
        .filter(CandidatePortalQueryService::isCandidateVisibleFollowUpField)
        .toList();
    CandidateProfileField existing = followUpFields.stream()
        .filter(field -> field.fieldPath().value().equals(targetFieldPath))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "candidate_follow_up_field_not_allowed:" + targetFieldPath));
    Instant answeredAt = Instant.now();
    candidateProfileService.upsertCandidateProfileField(UpsertCandidateProfileFieldRequest.builder()
        .organizationId(organizationId)
        .candidateProfileId(profile.candidateProfileId())
        .fieldPath(existing.fieldPath())
        .value(existing.value())
        .fieldStatus(existing.fieldStatus())
        .lineage(existing.lineage())
        .conflict(existing.conflict())
        .staleness(existing.staleness())
        .lastReviewedAt(existing.lastReviewedAt())
        .confirmedByActorId(existing.confirmedByActorId())
        .confirmedAgainstProfileVersion(existing.confirmedAgainstProfileVersion())
        .sourceClaimId(existing.sourceClaimId())
        .sourceReviewEventId(existing.sourceReviewEventId())
        .sourceWorkflowEventId(existing.sourceWorkflowEventId())
        .notes(encodeFollowUpSubmissionNotes(existing.notes(), normalizedAnswer, answeredAt))
        .bulkApproval(false)
        .build());
    return buildFollowUpForm(organizationId, candidateRef, formId);
  }

  public PagedResult<CandidateDocumentSummaryResponse> listDocuments(
      UUID organizationId, UUID userAccountId, int limit, int offset) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    CandidateId candidateId = new CandidateId(userAccountId);
    List<CandidateDocument> documents = candidateDocumentService
        .findDocumentsByCandidateIdAndOrganizationId(organizationId, candidateId);
    PagedQuery pagedQuery = PagedQuery.builder(organizationId).limit(limit).offset(offset).build();
    List<CandidateDocumentSummaryResponse> items = documents.stream()
        .skip(pagedQuery.offset())
        .limit(pagedQuery.limit())
        .map(this::toDocumentSummary)
        .toList();
    return PagedResult.of(items, documents.size(), pagedQuery.limit(), pagedQuery.offset());
  }

  public List<CandidateOpportunityResponse> listOpportunities(UUID organizationId, UUID userAccountId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    CandidateId candidateId = new CandidateId(userAccountId);
    Optional<CandidateProfile> profile = candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
        organizationId, candidateId);
    List<CandidateCompanyInteraction> interactions = interactionService
        .findInteractionsByCandidateId(organizationId, candidateId);
    return interactions.stream()
        .map(i -> toOpportunityResponse(organizationId, userAccountId.toString(), profile, i))
        .filter(Objects::nonNull)
        .toList();
  }

  public CandidateOpportunityDetailResponse buildOpportunityDetail(
      UUID organizationId,
      UUID userAccountId,
      String candidateRef,
      String interactionId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Objects.requireNonNull(interactionId, "interactionId must not be null");
    CandidateCompanyInteraction interaction = requireCandidateOpportunity(
        organizationId,
        userAccountId,
        interactionId);
    CandidateProfile profile = findCandidateProfile(organizationId, candidateRef)
        .orElseThrow(() -> new IllegalArgumentException("candidate_profile_not_found"));
    Job job = requireJob(organizationId, interaction.jobId());
    Optional<Company> company = companyService.findCompanyByIdAndOrganizationId(
        organizationId,
        interaction.companyId());
    String companyName = candidateFacingCompanyName(company, job);
    String candidateProfileRef = profile.candidateProfileId().value().toString();
    String jobRef = interaction.jobId().value().toString();
    Optional<CandidateConsentWorkflowService.CandidateConsentView> consentView = findConsentSnapshot(
        organizationId,
        candidateRef,
        candidateProfileRef,
        jobRef);
    OpportunityMetadata metadata = opportunityMetadata(interaction);
    return new CandidateOpportunityDetailResponse(
        interaction.candidateCompanyInteractionId().value().toString(),
        job.title(),
        companyName,
        interaction.status().wireValue(),
        interaction.interactionType().wireValue(),
        candidateProfileRef,
        jobRef,
        consentView.map(view -> view.consentRecord().consentRecordRef()).orElse(null),
        consentView.map(view -> view.consentRecord().status().wireValue()).orElse(null),
        firstNonBlank(job.description(), job.title()),
        firstNonBlank(job.location(), "Location will be confirmed during follow-up."),
        firstNonBlank(job.compensation(), "Compensation details will be shared during the process."),
        resolveFitExplanation(job, metadata),
        metadata.interestStatus(),
        metadata.interestUpdatedAt(),
        interaction.startedAt(),
        interaction.updatedAt());
  }

  public CandidateOpportunityDetailResponse recordOpportunityInterest(
      UUID organizationId,
      UUID userAccountId,
      String candidateRef,
      String interactionId,
      String requestedInterestStatus,
      String note) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    Objects.requireNonNull(interactionId, "interactionId must not be null");
    CandidateCompanyInteraction interaction = requireCandidateOpportunity(
        organizationId,
        userAccountId,
        interactionId);
    String interestStatus = normalizeInterestStatus(requestedInterestStatus);
    Instant recordedAt = Instant.now();
    OpportunityMetadata previous = opportunityMetadata(interaction);
    String normalizedNote = note == null ? "" : note.strip();
    CandidateCompanyInteraction updated = interactionService.updateInteraction(CandidateCompanyInteraction.builder()
        .candidateCompanyInteractionId(interaction.candidateCompanyInteractionId())
        .organizationId(interaction.organizationId())
        .candidateId(interaction.candidateId())
        .companyId(interaction.companyId())
        .jobId(interaction.jobId())
        .interactionType(interaction.interactionType())
        .status(interaction.status())
        .startedAt(interaction.startedAt())
        .endedAt(interaction.endedAt())
        .notes(interaction.notes())
        .metadata(updatedOpportunityMetadataJson(interaction, interestStatus, normalizedNote, recordedAt))
        .createdAt(interaction.createdAt())
        .updatedAt(interaction.updatedAt())
        .version(interaction.version())
        .build());
    appendCandidateInterestRecordedEvent(
        organizationId,
        candidateRef,
        userAccountId,
        updated.candidateCompanyInteractionId().value().toString(),
        previous.interestStatus(),
        interestStatus,
        normalizedNote,
        recordedAt);
    return buildOpportunityDetail(
        organizationId,
        userAccountId,
        candidateRef,
        updated.candidateCompanyInteractionId().value().toString());
  }

  public CandidateTimelineResponse buildTimeline(UUID organizationId, String candidateRef) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    UUID candidateEntityId = ConsentDisclosureWorkflowEntityIds.candidateEntityId(
        organizationId,
        candidateRef);
    List<WorkflowAuditRecord> workflowEvents = new ArrayList<>(workflowAuditQueryService.search(
        WorkflowAuditQuery.builder(organizationId)
            .entityType(WorkflowEntityType.CANDIDATE.wireValue())
            .entityId(candidateEntityId)
            .limit(50)
            .build()));
    for (ConsentRecord consentRecord : consentRecordPort.listByCandidateRef(organizationId, candidateRef)) {
      workflowEvents.addAll(workflowAuditQueryService.search(
          WorkflowAuditQuery.builder(organizationId)
              .entityType(WorkflowEntityType.CONSENT.wireValue())
              .entityId(ConsentDisclosureWorkflowEntityIds.consentEntityId(
                  organizationId,
                  consentRecord.consentRecordRef()))
              .limit(20)
              .build()));
    }

    List<CandidateTimelineResponse.TimelineEvent> events = workflowEvents.stream()
        .sorted(Comparator.comparing(WorkflowAuditRecord::occurredAt).reversed())
        .distinct()
        .map(this::toTimelineEvent)
        .limit(50)
        .toList();

    return new CandidateTimelineResponse(candidateRef, events);
  }

  public ConsultantDocumentUploadResponse uploadDocument(
      UUID organizationId, UUID userAccountId, MultipartFile file, String documentType) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(file, "file must not be null");
    try {
      String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
      String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
      DocumentUploadCommand command = new DocumentUploadCommand.Builder(
          organizationId, SourceItemType.CV, SourceItemOrigin.CANDIDATE_UPLOAD, ActorRole.CANDIDATE)
          .packetType(InformationPacketType.CANDIDATE)
          .intendedEntityType(IntendedEntityType.CANDIDATE)
          .title(originalFilename)
          .uploadedByActorId(userAccountId)
          .originalFilename(originalFilename)
          .mimeType(mimeType)
          .contentLength(file.getSize())
          .build();
      DocumentUploadResult result = documentUploadService.upload(command, file.getInputStream());
      return new ConsultantDocumentUploadResponse(
          result.sourceItemId().value().toString(),
          result.informationPacketId() != null ? result.informationPacketId().toString() : null,
          result.scanStatus());
    } catch (IOException exception) {
      throw new IllegalArgumentException("failed to read uploaded file", exception);
    }
  }

  private CandidateProfileReviewResponse.ProfileField toProfileField(CandidateProfileField field) {
    return new CandidateProfileReviewResponse.ProfileField(
        field.fieldPath().value(),
        field.value().jsonValue(),
        field.fieldStatus().wireValue(),
        field.lineage().sourceReferences().isEmpty() ? "unknown" : field.lineage().sourceReferences().get(0).sourceType().wireValue(),
        field.lastReviewedAt() != null ? field.lastReviewedAt().toString() : null);
  }

  private CandidateDocumentSummaryResponse toDocumentSummary(CandidateDocument doc) {
    return new CandidateDocumentSummaryResponse(
        doc.candidateDocumentId().value(),
        doc.documentType(),
        doc.title() != null ? doc.title() : "Untitled",
        doc.status().wireValue(),
        0L,
        "application/octet-stream",
        doc.createdAt());
  }

  private CandidateOpportunityResponse toOpportunityResponse(
      UUID organizationId,
      String candidateRef,
      Optional<CandidateProfile> profile,
      CandidateCompanyInteraction interaction) {
    if (interaction.jobId() == null || profile.isEmpty()) {
      return null;
    }
    String jobTitle = "Unnamed opportunity";
    Optional<Job> job = jobService.findJobByIdAndOrganizationId(organizationId, interaction.jobId());
    jobTitle = job.map(Job::title).orElse(jobTitle);
    Optional<Company> company = companyService.findCompanyByIdAndOrganizationId(
        organizationId,
        interaction.companyId());
    String companyName = candidateFacingCompanyName(company, job.orElse(null));
    String candidateProfileRef = profile.orElseThrow().candidateProfileId().value().toString();
    String jobRef = interaction.jobId().value().toString();
    Optional<CandidateConsentWorkflowService.CandidateConsentView> consentView = findConsentSnapshot(
        organizationId,
        candidateRef,
        candidateProfileRef,
        jobRef);
    OpportunityMetadata metadata = opportunityMetadata(interaction);
    return new CandidateOpportunityResponse(
        interaction.candidateCompanyInteractionId().value().toString(),
        jobTitle,
        companyName,
        interaction.status().wireValue(),
        interaction.interactionType().wireValue(),
        candidateProfileRef,
        jobRef,
        consentView.map(view -> view.consentRecord().status().wireValue()).orElse(null),
        consentView.map(view -> view.consentRecord().consentRecordRef()).orElse(null),
        metadata.interestStatus(),
        interaction.startedAt(),
        interaction.updatedAt());
  }

  private void appendCandidateProfileConfirmedEvent(
      UUID organizationId,
      CandidateProfile profile,
      String candidateRef,
      UUID candidateActorId,
      String fieldPath,
      int pendingBefore,
      int pendingAfter,
      Instant occurredAt) {
    UUID candidateEntityId = ConsentDisclosureWorkflowEntityIds.candidateEntityId(organizationId, candidateRef);
    workflowEventService.append(new WorkflowEventAppendCommand(
        organizationId,
        "recruiting",
        new EntityRef(WorkflowEntityType.CANDIDATE.wireValue(), candidateEntityId),
        profile.profileVersion().value(),
        WorkflowActionCode.CANDIDATE_PROFILE_CONFIRMED.wireValue(),
        new WorkflowStateSnapshot(candidateProfileConfirmationState(
            "candidate_follow_up_pending",
            fieldPath,
            profile.profileVersion().value(),
            pendingBefore)),
        new WorkflowStateSnapshot(candidateProfileConfirmationState(
            pendingAfter > 0 ? "candidate_follow_up_pending" : "candidate_follow_up_completed",
            fieldPath,
            profile.profileVersion().value(),
            pendingAfter)),
        new ActorRef(candidateActorId, ActorRole.CANDIDATE),
        "candidate_portal",
        profile.candidateProfileId().value(),
        null,
        null,
        "Candidate confirmed profile field " + fieldPath,
        new WorkflowIdempotencyKey(
            "candidate-profile-confirmed:"
                + candidateRef + ":" + profile.profileVersion().value() + ":" + fieldPath),
        null,
        null,
        occurredAt));
  }

  private void appendCandidateInterestRecordedEvent(
      UUID organizationId,
      String candidateRef,
      UUID candidateActorId,
      String interactionId,
      String beforeInterestStatus,
      String afterInterestStatus,
      String note,
      Instant occurredAt) {
    UUID candidateEntityId = ConsentDisclosureWorkflowEntityIds.candidateEntityId(organizationId, candidateRef);
    workflowEventService.append(new WorkflowEventAppendCommand(
        organizationId,
        "recruiting",
        new EntityRef(WorkflowEntityType.CANDIDATE.wireValue(), candidateEntityId),
        null,
        WorkflowActionCode.CANDIDATE_INTEREST_RECORDED.wireValue(),
        new WorkflowStateSnapshot(opportunityInterestState(interactionId, beforeInterestStatus)),
        new WorkflowStateSnapshot(opportunityInterestState(interactionId, afterInterestStatus)),
        new ActorRef(candidateActorId, ActorRole.CANDIDATE),
        "candidate_portal",
        null,
        null,
        null,
        note == null || note.isBlank()
            ? "Candidate recorded opportunity interest"
            : "Candidate recorded opportunity interest: " + note,
        new WorkflowIdempotencyKey(
            "candidate-interest-recorded:" + candidateRef + ":" + interactionId + ":" + afterInterestStatus),
        null,
        null,
        occurredAt));
  }

  private CandidateTimelineResponse.TimelineEvent toTimelineEvent(WorkflowAuditRecord record) {
    return new CandidateTimelineResponse.TimelineEvent(
        record.entityType().toLowerCase(),
        record.actionCode(),
        extractWorkflowStatus(record),
        record.reason(),
        record.occurredAt());
  }

  private Optional<CandidateProfile> findCandidateProfile(UUID organizationId, String candidateRef) {
    CandidateId candidateId = new CandidateId(UUID.fromString(candidateRef));
    return candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(organizationId, candidateId);
  }

  private CandidateCompanyInteraction requireCandidateOpportunity(
      UUID organizationId,
      UUID userAccountId,
      String interactionId) {
    CandidateCompanyInteraction interaction = interactionService.findInteractionByIdAndOrganizationId(
            organizationId,
            new CandidateCompanyInteractionId(UUID.fromString(interactionId)))
        .orElseThrow(() -> new IllegalArgumentException("candidate_opportunity_not_found"));
    if (!interaction.candidateId().value().equals(userAccountId)) {
      throw new IllegalArgumentException("candidate_opportunity_not_found");
    }
    if (interaction.jobId() == null) {
      throw new IllegalArgumentException("candidate_opportunity_not_found");
    }
    return interaction;
  }

  private Job requireJob(UUID organizationId, JobId jobId) {
    return jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException("job_not_found"));
  }

  private static boolean requiresCandidateFollowUp(CandidateProfileField field) {
    if (hasPendingConsultantReviewSubmission(field)) {
      return false;
    }
    return hasFollowUpRequirement(field);
  }

  private static boolean isCandidateVisibleFollowUpField(CandidateProfileField field) {
    return hasFollowUpRequirement(field) || hasPendingConsultantReviewSubmission(field);
  }

  private static boolean hasFollowUpRequirement(CandidateProfileField field) {
    return field.fieldStatus() == CandidateProfileFieldStatus.NEEDS_CONFIRMATION
        || field.fieldStatus() == CandidateProfileFieldStatus.CONFLICTING
        || (field.staleness() != null && field.staleness().stale());
  }

  private int countPendingFollowUps(CandidateProfile profile) {
    return (int) profile.fields().stream()
        .filter(CandidatePortalQueryService::requiresCandidateFollowUp)
        .count();
  }

  private static String requireCurrentProfileFollowUpForm(String formId) {
    String normalized = formId == null ? null : formId.strip();
    if (!"current-profile".equals(normalized)) {
      throw new IllegalArgumentException("candidate_follow_up_form_not_found");
    }
    return normalized;
  }

  private CandidateFollowUpFormResponse.FollowUpItem toFollowUpItem(CandidateProfileField field) {
    Optional<FollowUpSubmission> submission = parseFollowUpSubmission(field.notes());
    return new CandidateFollowUpFormResponse.FollowUpItem(
        field.fieldPath().value(),
        followUpPrompt(field.fieldPath().value()),
        followUpInputType(field.fieldPath().value()),
        submission.map(FollowUpSubmission::answer)
            .orElse(firstNonBlank(renderJsonValue(field.value().jsonValue()), "Pending answer")),
        submission.isPresent() ? "submitted_for_consultant_review" : field.fieldStatus().wireValue(),
        field.lineage().sourceReferences().isEmpty()
            ? "unknown"
            : field.lineage().sourceReferences().get(0).sourceType().wireValue(),
        submission.map(FollowUpSubmission::submittedAt)
            .orElse(field.lastReviewedAt()) != null
                ? submission.map(FollowUpSubmission::submittedAt)
                    .orElse(field.lastReviewedAt())
                    .toString()
                : null);
  }

  private static String followUpPrompt(String fieldPath) {
    return switch (fieldPath) {
      case "compensation.expected_salary" -> "Please confirm your current expected salary range for this search.";
      case "location.current_city" -> "Please confirm the city where you are currently based.";
      case "availability.notice_period" -> "Please confirm your current notice period or earliest start date.";
      case "motivation.current_interest" -> "Please describe how open you are to this type of opportunity right now.";
      case "projects.ownership" -> "Please clarify your ownership level for the project highlighted here.";
      default -> "Please review and answer this follow-up question for " + fieldPath + ".";
    };
  }

  private static String followUpInputType(String fieldPath) {
    if (fieldPath.contains("salary") || fieldPath.contains("compensation")) {
      return "text";
    }
    if (fieldPath.contains("notice_period") || fieldPath.contains("availability")) {
      return "text";
    }
    if (fieldPath.contains("skills")) {
      return "list";
    }
    return "textarea";
  }

  private static String toFollowUpAnswerJson(CandidateProfileField existing, String answer) {
    String inputType = followUpInputType(existing.fieldPath().value());
    if ("list".equals(inputType)) {
      ArrayNode values = OBJECT_MAPPER.createArrayNode();
      for (String item : answer.split(",")) {
        String normalized = item.strip();
        if (!normalized.isEmpty()) {
          values.add(normalized);
        }
      }
      return values.isEmpty() ? OBJECT_MAPPER.createArrayNode().add(answer).toString() : values.toString();
    }
    if (looksLikeBoolean(answer)) {
      return Boolean.toString(Boolean.parseBoolean(answer));
    }
    if (looksLikeNumber(answer)) {
      return answer;
    }
    return OBJECT_MAPPER.valueToTree(answer).toString();
  }

  private static boolean hasPendingConsultantReviewSubmission(CandidateProfileField field) {
    return parseFollowUpSubmission(field.notes()).isPresent();
  }

  private static String encodeFollowUpSubmissionNotes(
      String existingNotes,
      String answer,
      Instant submittedAt) {
    ObjectNode root = OBJECT_MAPPER.createObjectNode();
    root.put("kind", FOLLOW_UP_SUBMISSION_NOTE_KIND);
    root.put("answer", answer);
    root.put("submittedAt", submittedAt.toString());
    if (existingNotes != null && !existingNotes.isBlank()) {
      parseFollowUpSubmission(existingNotes)
          .flatMap(FollowUpSubmission::baseNotes)
          .or(() -> Optional.of(existingNotes))
          .ifPresent(baseNotes -> root.put("baseNotes", baseNotes));
    }
    return root.toString();
  }

  private static Optional<FollowUpSubmission> parseFollowUpSubmission(String notes) {
    if (notes == null || notes.isBlank()) {
      return Optional.empty();
    }
    try {
      JsonNode root = OBJECT_MAPPER.readTree(notes);
      if (!FOLLOW_UP_SUBMISSION_NOTE_KIND.equals(root.path("kind").asText())) {
        return Optional.empty();
      }
      String answer = root.path("answer").asText(null);
      String submittedAt = root.path("submittedAt").asText(null);
      if (answer == null || answer.isBlank() || submittedAt == null || submittedAt.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(new FollowUpSubmission(
          answer,
          Instant.parse(submittedAt),
          root.hasNonNull("baseNotes") ? Optional.of(root.get("baseNotes").asText()) : Optional.empty()));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private String resolveDisplayName(Optional<CandidateProfile> profile, String defaultDisplayName) {
    return profile.flatMap(candidateProfile -> findProfileText(candidateProfile, "identity.preferred_name")
            .or(() -> findProfileText(candidateProfile, "identity.full_name")))
        .orElseGet(() -> {
          if (defaultDisplayName == null || defaultDisplayName.isBlank()) {
            return "Candidate";
          }
          return defaultDisplayName;
        });
  }

  private Optional<String> findProfileText(CandidateProfile profile, String fieldPath) {
    return profile.fields().stream()
        .filter(field -> field.fieldPath().value().equals(fieldPath))
        .findFirst()
        .map(field -> renderJsonValue(field.value().jsonValue()))
        .filter(value -> value != null && !value.isBlank());
  }

  private Optional<CandidateConsentWorkflowService.CandidateConsentView> findConsentSnapshot(
      UUID organizationId,
      String candidateRef,
      String candidateProfileRef,
      String jobRef) {
    try {
      return Optional.of(candidateConsentWorkflowService.latestConsentSnapshot(
          organizationId,
          candidateRef,
          candidateProfileRef,
          jobRef));
    } catch (IllegalArgumentException exception) {
      if ("consent_request_not_found".equals(exception.getMessage())) {
        return Optional.empty();
      }
      throw exception;
    }
  }

  private static OpportunityMetadata opportunityMetadata(CandidateCompanyInteraction interaction) {
    if (interaction.metadata() == null || interaction.metadata().isBlank()) {
      return new OpportunityMetadata("unknown", null, null, null);
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(interaction.metadata());
      return new OpportunityMetadata(
          node.path("candidateInterestStatus").asText("unknown"),
          node.hasNonNull("candidateInterestNote") ? node.get("candidateInterestNote").asText() : null,
          node.hasNonNull("interestUpdatedAt") ? Instant.parse(node.get("interestUpdatedAt").asText()) : null,
          node.hasNonNull("fitExplanation") ? node.get("fitExplanation").asText() : null);
    } catch (Exception exception) {
      return new OpportunityMetadata("unknown", null, null, null);
    }
  }

  private static String updatedOpportunityMetadataJson(
      CandidateCompanyInteraction interaction,
      String interestStatus,
      String note,
      Instant updatedAt) {
    ObjectNode root;
    try {
      root = interaction.metadata() == null || interaction.metadata().isBlank()
          ? OBJECT_MAPPER.createObjectNode()
          : (ObjectNode) OBJECT_MAPPER.readTree(interaction.metadata());
    } catch (Exception exception) {
      root = OBJECT_MAPPER.createObjectNode();
    }
    root.put("candidateInterestStatus", interestStatus);
    if (note != null && !note.isBlank()) {
      root.put("candidateInterestNote", note);
    }
    root.put("interestUpdatedAt", updatedAt.toString());
    return root.toString();
  }

  private static String normalizeInterestStatus(String requestedInterestStatus) {
    String normalized = requestedInterestStatus == null ? null : requestedInterestStatus.strip();
    if (normalized == null || normalized.isEmpty()) {
      throw new IllegalArgumentException("candidate_interest_status_required");
    }
    return switch (normalized) {
      case "open_to_explore", "interested_confirmed", "declined" -> normalized;
      default -> throw new IllegalArgumentException("candidate_interest_status_not_allowed:" + normalized);
    };
  }

  private static String resolveFitExplanation(Job job, OpportunityMetadata metadata) {
    if (metadata.fitExplanation() != null && !metadata.fitExplanation().isBlank()) {
      return metadata.fitExplanation();
    }
    return "This role aligns with your active profile based on the current title, location, and workflow stage.";
  }

  private static String firstNonBlank(String primary, String fallback) {
    if (primary != null && !primary.isBlank()) {
      return primary;
    }
    return fallback;
  }

  private static boolean looksLikeBoolean(String value) {
    return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
  }

  private static boolean looksLikeNumber(String value) {
    return value.matches("-?\\d+(\\.\\d+)?");
  }

  private static String candidateFacingCompanyName(Optional<Company> company, Job job) {
    if (company.isEmpty()) {
      return confidentialCompanyLabel(null, job);
    }
    Company value = company.orElseThrow();
    CompanyNameGeneralizationPolicy.Generalization generalization =
        CompanyNameGeneralizationPolicy.generalize(value.name());
    if (generalization != null) {
      return generalization.generalizedLabel();
    }
    return confidentialCompanyLabel(value.industry(), job);
  }

  private static String confidentialCompanyLabel(String industry, Job job) {
    String normalizedIndustry = firstNonBlank(industry, job != null ? job.roleFamily() : null);
    if (normalizedIndustry != null && !normalizedIndustry.isBlank()) {
      return "Confidential " + normalizedIndustry + " employer";
    }
    return "Confidential employer";
  }

  private static String extractWorkflowStatus(WorkflowAuditRecord record) {
    String status = extractStatusFromSnapshot(record.afterState());
    if (status != null) {
      return status;
    }
    status = extractStatusFromSnapshot(record.beforeState());
    return status == null ? "unknown" : status;
  }

  private static String extractStatusFromSnapshot(
      WorkflowStateSnapshot snapshot) {
    if (snapshot == null || snapshot.json() == null || snapshot.json().isBlank()) {
      return null;
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(snapshot.json());
      if (node.hasNonNull("status")) {
        return node.get("status").asText();
      }
      return snapshot.json();
    } catch (JsonProcessingException exception) {
      return snapshot.json();
    }
  }

  private static String renderJsonValue(String jsonValue) {
    if (jsonValue == null || jsonValue.isBlank()) {
      return null;
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(jsonValue);
      if (node.isTextual() || node.isNumber() || node.isBoolean()) {
        return node.asText();
      }
      return node.toString();
    } catch (JsonProcessingException exception) {
      return jsonValue;
    }
  }

  private static String candidateProfileConfirmationState(
      String status,
      String fieldPath,
      int profileVersion,
      int pendingFollowUpCount) {
    return OBJECT_MAPPER.createObjectNode()
        .put("status", status)
        .put("fieldPath", fieldPath)
        .put("profileVersion", profileVersion)
        .put("pendingFollowUpCount", pendingFollowUpCount)
        .toString();
  }

  private static String opportunityInterestState(String interactionId, String interestStatus) {
    return OBJECT_MAPPER.createObjectNode()
        .put("status", interestStatus)
        .put("interactionId", interactionId)
        .toString();
  }

  private record OpportunityMetadata(
      String interestStatus,
      String interestNote,
      Instant interestUpdatedAt,
      String fitExplanation) {}

  private record FollowUpSubmission(
      String answer,
      Instant submittedAt,
      Optional<String> baseNotes) {}
}
