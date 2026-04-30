package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantJobDetailResponse(
    String jobId,
    String companyId,
    String title,
    String description,
    String location,
    String seniorityBand,
    String roleFamily,
    String employmentType,
    String compensation,
    String status,
    String ownerConsultantId,
    String activatedAt,
    String closedAt,
    String closeReason,
    String createdAt,
    String updatedAt,
    List<Requirement> requirements,
    Scorecard scorecard) implements ApiSafeResponseBody {

  public ConsultantJobDetailResponse {
    jobId = ApiBoundaryContractRules.requireNonBlank(jobId, "jobId");
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    title = ApiBoundaryContractRules.requireApiSafeExternalText(title, "title");
    description = ApiBoundaryContractRules.sanitizeExternalText(description, null);
    location = ApiBoundaryContractRules.sanitizeExternalText(location, null);
    seniorityBand = ApiBoundaryContractRules.sanitizeExternalText(seniorityBand, null);
    roleFamily = ApiBoundaryContractRules.sanitizeExternalText(roleFamily, null);
    employmentType = ApiBoundaryContractRules.sanitizeExternalText(employmentType, null);
    compensation = ApiBoundaryContractRules.sanitizeExternalText(compensation, null);
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    requirements = ApiBoundaryContractRules.requireNonNullList(requirements, "requirements");
  }

  public record Requirement(
      String requirementId,
      String requirementType,
      String label,
      String importance,
      String detail,
      int sortOrder) {}

  public record Scorecard(
      String scorecardId,
      String dimensions,
      String scoringGuidance,
      String status) {}
}
