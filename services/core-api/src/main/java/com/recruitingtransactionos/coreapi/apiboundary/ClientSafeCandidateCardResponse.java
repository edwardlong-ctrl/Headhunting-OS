package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ClientSafeCandidateCardResponse(
    String anonymousCardRef,
    String clientAlias,
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
    clientAlias = ApiBoundaryContractRules.requireApiSafeExternalText(clientAlias, "clientAlias");
    projectionVersion =
        ApiBoundaryContractRules.requireNonBlank(projectionVersion, "projectionVersion");
    redactionLevel =
        ApiBoundaryContractRules.requireAnonymousClientSafeRedactionLevel(redactionLevel);
    generalizedHeadline =
        ApiBoundaryContractRules.requireApiSafeExternalText(
            generalizedHeadline, "generalizedHeadline");
    generalizedRoleFamily =
        ApiBoundaryContractRules.requireApiSafeExternalText(
            generalizedRoleFamily, "generalizedRoleFamily");
    generalizedSeniorityBand =
        ApiBoundaryContractRules.requireApiSafeExternalText(
            generalizedSeniorityBand, "generalizedSeniorityBand");
    generalizedLocationRegion =
        ApiBoundaryContractRules.requireApiSafeExternalText(
            generalizedLocationRegion, "generalizedLocationRegion");
    safeSummary = ApiBoundaryContractRules.requireApiSafeExternalText(safeSummary, "safeSummary");
    safeSkillSummary =
        ApiBoundaryContractRules.requireApiSafeExternalText(
            safeSkillSummary, "safeSkillSummary");
    safeEvidenceSummaries =
        ApiBoundaryContractRules.copyApiSafeExternalTextList(
            safeEvidenceSummaries, "safeEvidenceSummaries");
    safeMatchNarratives =
        ApiBoundaryContractRules.copyApiSafeExternalTextList(
            safeMatchNarratives, "safeMatchNarratives");
  }
}
