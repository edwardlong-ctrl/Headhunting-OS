package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ApiValidationErrorResponse(
    String errorCode,
    String safeReason,
    List<String> validationMessages) implements ApiSafeResponseBody {

  public ApiValidationErrorResponse {
    errorCode = ApiBoundaryContractRules.sanitizeReasonCode(errorCode, "validation_failed");
    safeReason = ApiBoundaryContractRules.sanitizeReasonCode(safeReason, "validation_failed");
    validationMessages =
        ApiBoundaryContractRules.copyNonBlankList(validationMessages, "validationMessages")
            .stream()
            .map(message ->
                ApiBoundaryContractRules.sanitizeExternalText(message, "Invalid request."))
            .toList();
  }

  public static ApiValidationErrorResponse of(String safeReason, List<String> validationMessages) {
    return new ApiValidationErrorResponse(
        "validation_failed",
        safeReason,
        validationMessages);
  }

  public ApiErrorResponse toErrorResponse() {
    return new ApiErrorResponse(errorCode, safeReason, "Invalid request.");
  }
}
