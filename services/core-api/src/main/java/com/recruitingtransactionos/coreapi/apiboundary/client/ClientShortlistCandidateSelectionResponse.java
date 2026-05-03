package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;

public record ClientShortlistCandidateSelectionResponse(
    String shortlistId,
    String shortlistCandidateCardId,
    String shortlistStatus,
    String cardStatus,
    String anonymousCardRef) implements ApiSafeResponseBody {

  public ClientShortlistCandidateSelectionResponse {
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
    shortlistStatus = ApiBoundaryContractRules.requireNonBlank(shortlistStatus, "shortlistStatus");
    cardStatus = ApiBoundaryContractRules.requireNonBlank(cardStatus, "cardStatus");
    anonymousCardRef = ApiBoundaryContractRules.requireNonBlank(anonymousCardRef, "anonymousCardRef");
  }
}
