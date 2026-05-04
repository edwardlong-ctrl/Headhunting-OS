package com.recruitingtransactionos.coreapi.apiboundary;

import java.time.Instant;
import java.util.Objects;

public record CandidateOpportunityDetailResponse(
    String interactionId,
    String jobTitle,
    String companyName,
    String status,
    String interactionType,
    String candidateProfileRef,
    String jobRef,
    String consentRecordRef,
    String consentStatus,
    String roleSummary,
    String location,
    String compensation,
    String fitExplanation,
    String interestStatus,
    Instant interestUpdatedAt,
    Instant startedAt,
    Instant updatedAt) implements ApiSafeResponseBody {

  public CandidateOpportunityDetailResponse {
    interactionId = ApiBoundaryContractRules.requireNonBlank(interactionId, "interactionId");
    jobTitle = ApiBoundaryContractRules.requireNonBlank(jobTitle, "jobTitle");
    companyName = ApiBoundaryContractRules.requireNonBlank(companyName, "companyName");
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    interactionType = ApiBoundaryContractRules.requireNonBlank(interactionType, "interactionType");
    candidateProfileRef = ApiBoundaryContractRules.requireNonBlank(candidateProfileRef, "candidateProfileRef");
    jobRef = ApiBoundaryContractRules.requireNonBlank(jobRef, "jobRef");
    roleSummary = ApiBoundaryContractRules.requireNonBlank(roleSummary, "roleSummary");
    location = ApiBoundaryContractRules.requireNonBlank(location, "location");
    compensation = ApiBoundaryContractRules.requireNonBlank(compensation, "compensation");
    fitExplanation = ApiBoundaryContractRules.requireNonBlank(fitExplanation, "fitExplanation");
    interestStatus = ApiBoundaryContractRules.requireNonBlank(interestStatus, "interestStatus");
    Objects.requireNonNull(startedAt, "startedAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
