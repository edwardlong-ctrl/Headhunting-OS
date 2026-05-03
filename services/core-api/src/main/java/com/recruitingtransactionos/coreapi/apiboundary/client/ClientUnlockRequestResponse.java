package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;

public record ClientUnlockRequestResponse(
    String clientUnlockRequestId,
    String shortlistId,
    String shortlistCandidateCardId,
    String anonymousCardRef,
    String status,
    String requestReason,
    String createdAt,
    String updatedAt,
    String unlockDecisionRef,
    String approvedDisclosureRecordRef) implements ApiSafeResponseBody {

  public ClientUnlockRequestResponse {
    clientUnlockRequestId = ApiBoundaryContractRules.requireNonBlank(clientUnlockRequestId, "clientUnlockRequestId");
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
    anonymousCardRef = ApiBoundaryContractRules.requireNonBlank(anonymousCardRef, "anonymousCardRef");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    requestReason = ApiBoundaryContractRules.requireNonBlank(requestReason, "requestReason");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    unlockDecisionRef = unlockDecisionRef;
    approvedDisclosureRecordRef = approvedDisclosureRecordRef;
  }
}
