package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import java.util.Objects;

public record ApiAccessDeniedResponse(
    String errorCode,
    String safeReason,
    String safeExplanation) implements ApiSafeResponseBody {

  public ApiAccessDeniedResponse {
    errorCode = ApiBoundaryContractRules.sanitizeReasonCode(errorCode, "access_denied");
    safeReason = ApiBoundaryContractRules.sanitizeReasonCode(safeReason, "access_denied");
    safeExplanation =
        ApiBoundaryContractRules.sanitizeExternalText(safeExplanation, "Access denied.");
  }

  public static ApiAccessDeniedResponse from(AccessDeniedException accessDenied) {
    Objects.requireNonNull(accessDenied, "accessDenied must not be null");
    return from(accessDenied.decision());
  }

  public static ApiAccessDeniedResponse from(AccessDecision decision) {
    Objects.requireNonNull(decision, "decision must not be null");
    if (decision.allowed()) {
      throw new IllegalArgumentException("decision must be a denial");
    }
    return new ApiAccessDeniedResponse(
        "access_denied",
        decision.reasonCode(),
        decision.safeExplanation());
  }

  public ApiErrorResponse toErrorResponse() {
    return new ApiErrorResponse(errorCode, safeReason, safeExplanation);
  }
}
