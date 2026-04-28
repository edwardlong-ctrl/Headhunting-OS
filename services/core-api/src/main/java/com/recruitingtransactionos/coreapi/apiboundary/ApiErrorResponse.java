package com.recruitingtransactionos.coreapi.apiboundary;

public record ApiErrorResponse(
    String errorCode,
    String safeReason,
    String safeMessage) implements ApiSafeResponseBody {

  public ApiErrorResponse {
    errorCode = ApiBoundaryContractRules.sanitizeReasonCode(errorCode, "internal_error");
    safeReason = ApiBoundaryContractRules.sanitizeReasonCode(safeReason, "internal_error");
    safeMessage = ApiBoundaryContractRules.sanitizeExternalText(safeMessage, "Request failed.");
  }
}
