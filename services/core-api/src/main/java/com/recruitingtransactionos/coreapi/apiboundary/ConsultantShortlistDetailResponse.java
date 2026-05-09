package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantShortlistDetailResponse(
    String shortlistId,
    long version,
    String jobId,
    String title,
    String status,
    String sentAt,
    String clientViewedAt,
    String ownerConsultantId,
    String createdAt,
    String updatedAt,
    List<PreSendCheck> preSendChecks,
    DeliveryPreview deliveryPreview,
    List<Card> cards) implements ApiSafeResponseBody {

  public ConsultantShortlistDetailResponse {
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    if (version < 0) {
      throw new IllegalArgumentException("version must be >= 0");
    }
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    title = ApiBoundaryContractRules.requireBusinessVisibleText(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    preSendChecks = ApiBoundaryContractRules.requireNonNullList(preSendChecks, "preSendChecks");
    deliveryPreview = java.util.Objects.requireNonNull(
        deliveryPreview, "deliveryPreview must not be null");
    cards = ApiBoundaryContractRules.requireNonNullList(cards, "cards");
  }

  public record PreSendCheck(
      String code,
      String label,
      boolean passed) {

    public PreSendCheck {
      code = ApiBoundaryContractRules.requireNonBlank(code, "code");
      label = ApiBoundaryContractRules.requireApiSafeExternalText(label, "label");
    }
  }

  public record DeliveryPreview(
      String clientSafeSummary,
      String pdfSummary,
      String emailSummary,
      String wechatSummary) {

    public DeliveryPreview {
      clientSafeSummary = ApiBoundaryContractRules.requireApiSafeExternalText(
          clientSafeSummary, "clientSafeSummary");
      pdfSummary = ApiBoundaryContractRules.requireApiSafeExternalText(pdfSummary, "pdfSummary");
      emailSummary = ApiBoundaryContractRules.requireApiSafeExternalText(
          emailSummary, "emailSummary");
      wechatSummary = ApiBoundaryContractRules.requireApiSafeExternalText(
          wechatSummary, "wechatSummary");
    }
  }

  public record Card(
      String cardId,
      String anonymousCandidateCardId,
      int version,
      int sortOrder,
      String status,
      String matchReportId,
      String anonymousCandidateRef,
      String generalizedHeadline,
      String generalizedRoleFamily,
      String generalizedSeniorityBand,
      String generalizedLocationRegion,
      String safeSummary,
      String safeSkillSummary,
      List<String> safeEvidenceSummaries,
      List<String> safeMatchNarratives,
      Integer overallScore,
      String confidence,
      String reidentificationRiskSignal,
      List<DimensionScore> dimensionScores,
      String clientNotes) {

    public Card {
      cardId = ApiBoundaryContractRules.requireNonBlank(cardId, "cardId");
      anonymousCandidateCardId =
          ApiBoundaryContractRules.requireNonBlank(anonymousCandidateCardId, "anonymousCandidateCardId");
      if (version < 1) {
        throw new IllegalArgumentException("version must be >= 1");
      }
      if (sortOrder < 0) {
        throw new IllegalArgumentException("sortOrder must be >= 0");
      }
      status = ApiBoundaryContractRules.requireNonBlank(status, "status");
      matchReportId = ApiBoundaryContractRules.sanitizeConsultantVisibleText(matchReportId, null);
      anonymousCandidateRef = ApiBoundaryContractRules.requireNonBlank(
          anonymousCandidateRef, "anonymousCandidateRef");
      generalizedHeadline = ApiBoundaryContractRules.requireApiSafeExternalText(
          generalizedHeadline, "generalizedHeadline");
      generalizedRoleFamily = ApiBoundaryContractRules.requireApiSafeExternalText(
          generalizedRoleFamily, "generalizedRoleFamily");
      generalizedSeniorityBand = ApiBoundaryContractRules.requireApiSafeExternalText(
          generalizedSeniorityBand, "generalizedSeniorityBand");
      generalizedLocationRegion = ApiBoundaryContractRules.requireApiSafeExternalText(
          generalizedLocationRegion, "generalizedLocationRegion");
      safeSummary = ApiBoundaryContractRules.requireApiSafeExternalText(safeSummary, "safeSummary");
      safeSkillSummary = ApiBoundaryContractRules.requireApiSafeExternalText(
          safeSkillSummary, "safeSkillSummary");
      safeEvidenceSummaries = ApiBoundaryContractRules.requireNonNullList(
          safeEvidenceSummaries, "safeEvidenceSummaries");
      safeMatchNarratives = ApiBoundaryContractRules.requireNonNullList(
          safeMatchNarratives, "safeMatchNarratives");
      if (overallScore != null && (overallScore < 1 || overallScore > 5)) {
        throw new IllegalArgumentException("overallScore must be between 1 and 5");
      }
      confidence = ApiBoundaryContractRules.requireNonBlank(confidence, "confidence");
      reidentificationRiskSignal = ApiBoundaryContractRules.requireNonBlank(
          reidentificationRiskSignal, "reidentificationRiskSignal");
      dimensionScores = ApiBoundaryContractRules.requireNonNullList(
          dimensionScores, "dimensionScores");
      clientNotes = ApiBoundaryContractRules.sanitizeConsultantVisibleText(clientNotes, null);
    }
  }

  public record DimensionScore(String dimension, int score) {

    public DimensionScore {
      dimension = ApiBoundaryContractRules.requireNonBlank(dimension, "dimension");
      if (score < 1 || score > 5) {
        throw new IllegalArgumentException("score must be between 1 and 5");
      }
    }
  }
}
