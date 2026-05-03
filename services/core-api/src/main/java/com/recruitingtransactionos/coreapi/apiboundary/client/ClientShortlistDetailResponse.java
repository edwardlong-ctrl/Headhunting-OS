package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import java.util.List;
import java.util.Objects;

public record ClientShortlistDetailResponse(
    String shortlistId,
    String jobId,
    String title,
    String status,
    String sentAt,
    String clientViewedAt,
    String createdAt,
    String updatedAt,
    List<Card> cards) implements ApiSafeResponseBody {

  public ClientShortlistDetailResponse {
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    cards = List.copyOf(Objects.requireNonNull(cards, "cards must not be null"));
  }

  public record Card(
      String shortlistCandidateCardId,
      String anonymousCardRef,
      String status,
      String generalizedHeadline,
      String generalizedRoleFamily,
      String generalizedSeniorityBand,
      String generalizedLocationRegion,
      String safeSummary,
      String safeSkillSummary,
      Integer overallScore,
      String confidence,
      String reidentificationRiskSignal,
      String clientNotes,
      String unlockRequestStatus,
      String unlockDecisionRef,
      String approvedDisclosureRecordRef) {

    public Card {
      shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
      anonymousCardRef = ApiBoundaryContractRules.requireNonBlank(anonymousCardRef, "anonymousCardRef");
      status = ApiBoundaryContractRules.requireNonBlank(status, "status");
      generalizedHeadline = ApiBoundaryContractRules.requireNonBlank(generalizedHeadline, "generalizedHeadline");
      generalizedRoleFamily = ApiBoundaryContractRules.requireNonBlank(generalizedRoleFamily, "generalizedRoleFamily");
      generalizedSeniorityBand = ApiBoundaryContractRules.requireNonBlank(generalizedSeniorityBand, "generalizedSeniorityBand");
      generalizedLocationRegion = ApiBoundaryContractRules.requireNonBlank(generalizedLocationRegion, "generalizedLocationRegion");
      safeSummary = ApiBoundaryContractRules.requireNonBlank(safeSummary, "safeSummary");
      safeSkillSummary = ApiBoundaryContractRules.requireNonBlank(safeSkillSummary, "safeSkillSummary");
      confidence = ApiBoundaryContractRules.requireNonBlank(confidence, "confidence");
      reidentificationRiskSignal = ApiBoundaryContractRules.requireNonBlank(reidentificationRiskSignal, "reidentificationRiskSignal");
      clientNotes = clientNotes;
      unlockRequestStatus = unlockRequestStatus;
      unlockDecisionRef = unlockDecisionRef;
      approvedDisclosureRecordRef = approvedDisclosureRecordRef;
    }
  }
}
