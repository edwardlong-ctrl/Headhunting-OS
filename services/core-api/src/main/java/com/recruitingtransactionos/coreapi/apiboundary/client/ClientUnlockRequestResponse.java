package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import java.util.List;
import java.util.Objects;

public record ClientUnlockRequestResponse(
    String clientUnlockRequestId,
    String shortlistId,
    String shortlistCandidateCardId,
    String anonymousCardRef,
    String status,
    String stage,
    String requestReason,
    String createdAt,
    String updatedAt,
    String unlockDecisionRef,
    String approvedDisclosureRecordRef,
    List<Blocker> blockers) implements ApiSafeResponseBody {

  public ClientUnlockRequestResponse {
    clientUnlockRequestId = clientUnlockRequestId == null ? null : clientUnlockRequestId.strip();
    shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
    shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
    anonymousCardRef = ApiBoundaryContractRules.requireNonBlank(anonymousCardRef, "anonymousCardRef");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    stage = ApiBoundaryContractRules.requireNonBlank(stage, "stage");
    requestReason = ApiBoundaryContractRules.requireNonBlank(requestReason, "requestReason");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    unlockDecisionRef = unlockDecisionRef;
    approvedDisclosureRecordRef = approvedDisclosureRecordRef;
    blockers = List.copyOf(Objects.requireNonNull(blockers, "blockers must not be null"));
  }

  public record Blocker(String code, String message) {
    public Blocker {
      code = ApiBoundaryContractRules.requireNonBlank(code, "code");
      message = ApiBoundaryContractRules.requireNonBlank(message, "message");
    }
  }
}
