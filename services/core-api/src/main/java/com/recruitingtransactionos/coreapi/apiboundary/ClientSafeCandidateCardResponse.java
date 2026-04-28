package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ClientSafeCandidateCardResponse(
    String anonymousCardRef,
    String anonymousCandidateRef,
    String projectionVersion,
    String redactionLevel,
    String generalizedHeadline,
    String generalizedRoleFamily,
    String generalizedSeniorityBand,
    String generalizedLocationRegion,
    String safeSummary,
    String safeSkillSummary,
    List<String> safeEvidenceSummaries,
    List<String> safeMatchNarratives) implements ApiSafeResponseBody {

  public ClientSafeCandidateCardResponse {
    anonymousCardRef =
        ApiBoundaryContractRules.requireNonBlank(anonymousCardRef, "anonymousCardRef");
    anonymousCandidateRef =
        ApiBoundaryContractRules.requireNonBlank(
            anonymousCandidateRef, "anonymousCandidateRef");
    projectionVersion =
        ApiBoundaryContractRules.requireNonBlank(projectionVersion, "projectionVersion");
    redactionLevel =
        ApiBoundaryContractRules.requireAnonymousClientSafeRedactionLevel(redactionLevel);
    generalizedHeadline =
        ApiBoundaryContractRules.requireNonBlank(
            generalizedHeadline, "generalizedHeadline");
    generalizedRoleFamily =
        ApiBoundaryContractRules.requireNonBlank(
            generalizedRoleFamily, "generalizedRoleFamily");
    generalizedSeniorityBand =
        ApiBoundaryContractRules.requireNonBlank(
            generalizedSeniorityBand, "generalizedSeniorityBand");
    generalizedLocationRegion =
        ApiBoundaryContractRules.requireNonBlank(
            generalizedLocationRegion, "generalizedLocationRegion");
    safeSummary = ApiBoundaryContractRules.requireNonBlank(safeSummary, "safeSummary");
    safeSkillSummary =
        ApiBoundaryContractRules.requireNonBlank(safeSkillSummary, "safeSkillSummary");
    safeEvidenceSummaries =
        ApiBoundaryContractRules.copyNonBlankList(
            safeEvidenceSummaries, "safeEvidenceSummaries");
    safeMatchNarratives =
        ApiBoundaryContractRules.copyNonBlankList(
            safeMatchNarratives, "safeMatchNarratives");
  }
}
