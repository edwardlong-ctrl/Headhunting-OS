package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record ConsultantUnlockQueueResponse(
    List<Item> items) implements ApiSafeResponseBody {

  public ConsultantUnlockQueueResponse {
    items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
  }

  public record Item(
      String unlockRequestId,
      String shortlistId,
      String shortlistCandidateCardId,
      String status,
      String requestReason,
      String createdAt,
      String anonymousCandidateCardRef,
      String jobTitle,
      String clientCompanyName,
      String consentStatus,
      List<Blocker> blockers) {

    public Item {
      unlockRequestId = ApiBoundaryContractRules.requireNonBlank(unlockRequestId, "unlockRequestId");
      shortlistId = ApiBoundaryContractRules.requireNonBlank(shortlistId, "shortlistId");
      shortlistCandidateCardId = ApiBoundaryContractRules.requireNonBlank(shortlistCandidateCardId, "shortlistCandidateCardId");
      status = ApiBoundaryContractRules.requireNonBlank(status, "status");
      requestReason = ApiBoundaryContractRules.requireNonBlank(requestReason, "requestReason");
      createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
      anonymousCandidateCardRef = ApiBoundaryContractRules.requireNonBlank(anonymousCandidateCardRef, "anonymousCandidateCardRef");
      jobTitle = ApiBoundaryContractRules.requireNonBlank(jobTitle, "jobTitle");
      clientCompanyName = ApiBoundaryContractRules.requireNonBlank(clientCompanyName, "clientCompanyName");
      consentStatus = ApiBoundaryContractRules.requireNonBlank(consentStatus, "consentStatus");
      blockers = List.copyOf(Objects.requireNonNull(blockers, "blockers must not be null"));
    }
  }

  public record Blocker(String code, String message) {
    public Blocker {
      code = ApiBoundaryContractRules.requireNonBlank(code, "code");
      message = ApiBoundaryContractRules.requireNonBlank(message, "message");
    }
  }
}
