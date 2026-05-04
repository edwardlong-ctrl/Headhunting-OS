package com.recruitingtransactionos.coreapi.apiboundary.candidate;

import com.recruitingtransactionos.coreapi.apiboundary.CandidateDocumentSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.CandidateMeResponse;
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
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadCommand;
import com.recruitingtransactionos.coreapi.governedintake.DocumentUploadResult;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketType;
import com.recruitingtransactionos.coreapi.governedintake.IntendedEntityType;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.service.DocumentUploadService;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public final class CandidatePortalQueryService {

  private final CandidateService candidateService;
  private final CandidateProfileService candidateProfileService;
  private final CandidateDocumentService candidateDocumentService;
  private final CandidateCompanyInteractionService interactionService;
  private final DocumentUploadService documentUploadService;
  private final WorkflowAuditQueryService workflowAuditQueryService;
  private final JobService jobService;
  private final CompanyService companyService;

  @Autowired
  public CandidatePortalQueryService(
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      CandidateDocumentService candidateDocumentService,
      CandidateCompanyInteractionService interactionService,
      DocumentUploadService documentUploadService,
      WorkflowAuditQueryService workflowAuditQueryService,
      JobService jobService,
      CompanyService companyService) {
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
  }

  public CandidateMeResponse buildMe(UUID organizationId, UUID userAccountId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    CandidateId candidateId = new CandidateId(userAccountId);
    Optional<Candidate> candidate = candidateService.findCandidateByIdAndOrganizationId(
        organizationId, candidateId);
    int documentCount = candidateDocumentService.findDocumentsByCandidateIdAndOrganizationId(
        organizationId, candidateId).size();
    int opportunityCount = interactionService.findInteractionsByCandidateId(
        organizationId, candidateId).size();
    int pendingFollowUpCount = 0;
    String profileVersion = candidate.flatMap(c ->
            candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
                organizationId, candidateId))
        .map(p -> Integer.toString(p.profileVersion().value()))
        .orElse("unknown");
    String displayName = candidate.map(Candidate::privacyStatus).orElse("Candidate");
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
    CandidateId candidateId = new CandidateId(UUID.fromString(candidateRef));
    Optional<CandidateProfile> profile = candidateProfileService
        .findCandidateProfileByCandidateIdAndOrganizationId(organizationId, candidateId);
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
    List<CandidateCompanyInteraction> interactions = interactionService
        .findInteractionsByCandidateId(organizationId, candidateId);
    return interactions.stream()
        .map(i -> toOpportunityResponse(organizationId, i))
        .filter(Objects::nonNull)
        .toList();
  }

  public CandidateTimelineResponse buildTimeline(UUID organizationId, String candidateRef) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateRef, "candidateRef must not be null");
    UUID candidateId = UUID.fromString(candidateRef);

    List<WorkflowAuditRecord> workflowEvents = workflowAuditQueryService.search(
        WorkflowAuditQuery.builder(organizationId)
            .entityType(WorkflowEntityType.CONSENT.wireValue())
            .limit(50)
            .build());

    List<CandidateTimelineResponse.TimelineEvent> events = workflowEvents.stream()
        .filter(e -> e.entityId().equals(candidateId))
        .map(this::toTimelineEvent)
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
      UUID organizationId, CandidateCompanyInteraction interaction) {
    String jobTitle = "Unnamed opportunity";
    if (interaction.jobId() != null) {
      jobTitle = jobService.findJobByIdAndOrganizationId(organizationId, interaction.jobId())
          .map(j -> j.title())
          .orElse(jobTitle);
    }
    String companyName = companyService.findCompanyByIdAndOrganizationId(
            organizationId, interaction.companyId())
        .map(c -> c.name())
        .orElse("Unknown company");
    return new CandidateOpportunityResponse(
        interaction.candidateCompanyInteractionId().value().toString(),
        jobTitle,
        companyName,
        interaction.status().wireValue(),
        interaction.interactionType().wireValue(),
        interaction.startedAt(),
        interaction.updatedAt());
  }

  private CandidateTimelineResponse.TimelineEvent toTimelineEvent(WorkflowAuditRecord record) {
    return new CandidateTimelineResponse.TimelineEvent(
        record.entityType(),
        record.actionCode(),
        record.afterState() != null ? record.afterState().json() : record.beforeState().json(),
        record.reason(),
        record.occurredAt());
  }
}
