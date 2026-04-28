package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.List;
import java.util.Objects;

public record ClientSafeCandidateCard(
    AnonymousCandidateCardId cardId,
    AnonymousCandidateRef anonymousCandidateRef,
    String projectionVersion,
    RedactionLevel redactionLevel,
    String generalizedHeadline,
    String generalizedRoleFamily,
    String generalizedSeniorityBand,
    String generalizedLocationRegion,
    String safeSummary,
    String safeSkillSummary,
    List<String> safeEvidenceSummaries,
    List<String> safeMatchNarratives) {

  public ClientSafeCandidateCard {
    Objects.requireNonNull(cardId, "cardId must not be null");
    Objects.requireNonNull(anonymousCandidateRef, "anonymousCandidateRef must not be null");
    projectionVersion =
        ClientSafeProjectionGuards.requireNonBlank(projectionVersion, "projectionVersion");
    Objects.requireNonNull(redactionLevel, "redactionLevel must not be null");
    if (!redactionLevel.isAnonymousClientSafeLevel()) {
      throw new IllegalArgumentException(
          "redactionLevel must be an anonymous client-safe level; L4 is vocabulary only here");
    }
    generalizedHeadline =
        ClientSafeProjectionGuards.requireNonBlank(generalizedHeadline, "generalizedHeadline");
    generalizedRoleFamily =
        ClientSafeProjectionGuards.requireNonBlank(
            generalizedRoleFamily, "generalizedRoleFamily");
    generalizedSeniorityBand =
        ClientSafeProjectionGuards.requireNonBlank(
            generalizedSeniorityBand, "generalizedSeniorityBand");
    generalizedLocationRegion =
        ClientSafeProjectionGuards.requireNonBlank(
            generalizedLocationRegion, "generalizedLocationRegion");
    safeSummary = ClientSafeProjectionGuards.requireNonBlank(safeSummary, "safeSummary");
    safeSkillSummary =
        ClientSafeProjectionGuards.requireNonBlank(safeSkillSummary, "safeSkillSummary");
    safeEvidenceSummaries =
        ClientSafeProjectionGuards.copyNonBlankList(
            safeEvidenceSummaries, "safeEvidenceSummaries");
    safeMatchNarratives =
        ClientSafeProjectionGuards.copyNonBlankList(safeMatchNarratives, "safeMatchNarratives");
  }
}
