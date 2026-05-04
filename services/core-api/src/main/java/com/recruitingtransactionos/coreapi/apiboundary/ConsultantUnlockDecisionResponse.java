package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record ConsultantUnlockDecisionResponse(
    String unlockRequestId,
    String status,
    String unlockDecisionRef,
    String approvedDisclosureRecordRef,
    List<ConsultantUnlockQueueResponse.Blocker> blockers) implements ApiSafeResponseBody {

  public ConsultantUnlockDecisionResponse {
    unlockRequestId = unlockRequestId == null ? null : unlockRequestId.strip();
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    unlockDecisionRef = unlockDecisionRef == null ? null : unlockDecisionRef.strip();
    approvedDisclosureRecordRef = approvedDisclosureRecordRef == null
        ? null
        : approvedDisclosureRecordRef.strip();
    blockers = List.copyOf(Objects.requireNonNull(blockers, "blockers must not be null"));
  }
}
