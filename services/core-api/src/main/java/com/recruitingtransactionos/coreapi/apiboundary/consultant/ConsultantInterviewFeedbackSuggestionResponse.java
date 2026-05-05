package com.recruitingtransactionos.coreapi.apiboundary;

public record ConsultantInterviewFeedbackSuggestionResponse(
    String suggestionId,
    String interviewFeedbackId,
    String interviewId,
    String jobId,
    String candidateId,
    String scope,
    String suggestionType,
    String status,
    String outcomeLabel,
    String rejectReasonTaxonomy,
    String title,
    String rationale,
    String payload,
    String createdAt,
    String reviewedAt) implements ApiSafeResponseBody {

  public ConsultantInterviewFeedbackSuggestionResponse {
    suggestionId = ApiBoundaryContractRules.requireNonBlank(suggestionId, "suggestionId");
    interviewFeedbackId = ApiBoundaryContractRules.requireNonBlank(interviewFeedbackId, "interviewFeedbackId");
    interviewId = ApiBoundaryContractRules.requireNonBlank(interviewId, "interviewId");
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    scope = ApiBoundaryContractRules.requireNonBlank(scope, "scope");
    suggestionType = ApiBoundaryContractRules.requireNonBlank(suggestionType, "suggestionType");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    title = ApiBoundaryContractRules.requireNonBlank(title, "title");
    payload = ApiBoundaryContractRules.requireNonBlank(payload, "payload");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
  }
}
