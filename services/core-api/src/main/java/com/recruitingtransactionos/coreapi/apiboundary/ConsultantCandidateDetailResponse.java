package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantCandidateDetailResponse(
    String candidateId,
    String status,
    String privacyStatus,
    String currentProfileId,
    String profileVersion,
    String ownerConsultantId,
    String lastActivityAt,
    String doNotContactReason,
    String mergedIntoCandidateId,
    String defaultIndustryPackId,
    String createdAt,
    String updatedAt,
    java.util.List<OverviewItem> overview,
    java.util.List<EvidenceItem> evidence,
    java.util.List<ConflictItem> conflicts,
    java.util.List<StaleInfoItem> staleInfo,
    java.util.List<FollowUpItem> followUps,
    java.util.List<HistoryItem> history) implements ApiSafeResponseBody {

  public ConsultantCandidateDetailResponse {
    candidateId = ApiBoundaryContractRules.requireNonBlank(candidateId, "candidateId");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    privacyStatus = ApiBoundaryContractRules.requireNonBlank(privacyStatus, "privacyStatus");
    currentProfileId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(currentProfileId, null);
    profileVersion = ApiBoundaryContractRules.sanitizeConsultantVisibleText(profileVersion, null);
    ownerConsultantId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(ownerConsultantId, null);
    lastActivityAt = ApiBoundaryContractRules.sanitizeConsultantVisibleText(lastActivityAt, null);
    doNotContactReason = ApiBoundaryContractRules.sanitizeConsultantVisibleText(doNotContactReason, null);
    mergedIntoCandidateId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(mergedIntoCandidateId, null);
    defaultIndustryPackId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(defaultIndustryPackId, null);
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    overview = ApiBoundaryContractRules.requireNonNullList(overview, "overview");
    evidence = ApiBoundaryContractRules.requireNonNullList(evidence, "evidence");
    conflicts = ApiBoundaryContractRules.requireNonNullList(conflicts, "conflicts");
    staleInfo = ApiBoundaryContractRules.requireNonNullList(staleInfo, "staleInfo");
    followUps = ApiBoundaryContractRules.requireNonNullList(followUps, "followUps");
    history = ApiBoundaryContractRules.requireNonNullList(history, "history");
  }

  public record OverviewItem(
      String fieldPath,
      String label,
      String value,
      String status,
      String lastReviewedAt,
      String notes) {

    public OverviewItem {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      label = ApiBoundaryContractRules.requireNonBlank(label, "label");
      value = ApiBoundaryContractRules.sanitizeConsultantVisibleText(value, null);
      status = ApiBoundaryContractRules.requireNonBlank(status, "status");
      lastReviewedAt = ApiBoundaryContractRules.sanitizeConsultantVisibleText(lastReviewedAt, null);
      notes = ApiBoundaryContractRules.sanitizeConsultantVisibleText(notes, null);
    }
  }

  public record EvidenceItem(
      String fieldPath,
      String sourceType,
      String sourceId,
      String sourceTrust,
      String provenanceLabel,
      String createdAt) {

    public EvidenceItem {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      sourceType = ApiBoundaryContractRules.requireNonBlank(sourceType, "sourceType");
      sourceId = ApiBoundaryContractRules.requireNonBlank(sourceId, "sourceId");
      sourceTrust = ApiBoundaryContractRules.sanitizeConsultantVisibleText(sourceTrust, null);
      provenanceLabel = ApiBoundaryContractRules.sanitizeConsultantVisibleText(provenanceLabel, null);
      createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    }
  }

  public record ConflictItem(
      String fieldPath,
      String severity,
      String resolutionStatus,
      java.util.List<String> conflictingValues,
      String detectedAt,
      String notes) {

    public ConflictItem {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      severity = ApiBoundaryContractRules.requireNonBlank(severity, "severity");
      resolutionStatus = ApiBoundaryContractRules.requireNonBlank(resolutionStatus, "resolutionStatus");
      conflictingValues = ApiBoundaryContractRules.requireNonNullList(conflictingValues, "conflictingValues");
      detectedAt = ApiBoundaryContractRules.requireNonBlank(detectedAt, "detectedAt");
      notes = ApiBoundaryContractRules.sanitizeConsultantVisibleText(notes, null);
    }
  }

  public record StaleInfoItem(
      String fieldPath,
      String staleReason,
      String reviewBy,
      String lastConfirmedAt,
      String detectedAt) {

    public StaleInfoItem {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      staleReason = ApiBoundaryContractRules.requireNonBlank(staleReason, "staleReason");
      reviewBy = ApiBoundaryContractRules.sanitizeConsultantVisibleText(reviewBy, null);
      lastConfirmedAt = ApiBoundaryContractRules.sanitizeConsultantVisibleText(lastConfirmedAt, null);
      detectedAt = ApiBoundaryContractRules.requireNonBlank(detectedAt, "detectedAt");
    }
  }

  public record FollowUpItem(
      String fieldPath,
      String followUpType,
      String reason,
      String recommendedAction) {

    public FollowUpItem {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      followUpType = ApiBoundaryContractRules.requireNonBlank(followUpType, "followUpType");
      reason = ApiBoundaryContractRules.requireNonBlank(reason, "reason");
      recommendedAction = ApiBoundaryContractRules.requireNonBlank(recommendedAction, "recommendedAction");
    }
  }

  public record HistoryItem(
      String eventType,
      String fieldPath,
      String description,
      String occurredAt) {

    public HistoryItem {
      eventType = ApiBoundaryContractRules.requireNonBlank(eventType, "eventType");
      fieldPath = ApiBoundaryContractRules.sanitizeConsultantVisibleText(fieldPath, null);
      description = ApiBoundaryContractRules.requireNonBlank(description, "description");
      occurredAt = ApiBoundaryContractRules.requireNonBlank(occurredAt, "occurredAt");
    }
  }
}
