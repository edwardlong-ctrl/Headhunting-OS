package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import java.util.Objects;

public final class ClientSafeCandidateCardResponseMapper {

  private ClientSafeCandidateCardResponseMapper() {}

  public static ClientSafeCandidateCardResponse from(ClientSafeCandidateCard card) {
    Objects.requireNonNull(card, "card must not be null");
    return new ClientSafeCandidateCardResponse(
        card.cardId().value(),
        card.anonymousCandidateRef().value(),
        card.projectionVersion(),
        card.redactionLevel().wireValue(),
        card.generalizedHeadline(),
        card.generalizedRoleFamily(),
        card.generalizedSeniorityBand(),
        card.generalizedLocationRegion(),
        card.safeSummary(),
        card.safeSkillSummary(),
        card.safeEvidenceSummaries(),
        card.safeMatchNarratives());
  }
}
