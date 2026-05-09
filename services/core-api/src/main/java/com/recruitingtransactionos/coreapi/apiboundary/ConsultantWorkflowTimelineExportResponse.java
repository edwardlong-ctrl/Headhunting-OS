package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantWorkflowTimelineExportResponse(
    String format,
    String content,
    String generatedAt) implements ApiSafeResponseBody {

  public ConsultantWorkflowTimelineExportResponse {
    format = ApiBoundaryContractRules.requireNonBlank(format, "format");
    content = content == null ? "" : content;
    generatedAt = ApiBoundaryContractRules.requireNonBlank(generatedAt, "generatedAt");
  }
}
