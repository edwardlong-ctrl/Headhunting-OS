package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantJobDetailResponse(
    String jobId,
    long version,
    String companyId,
    String title,
    String description,
    String location,
    String seniorityBand,
    String roleFamily,
    String employmentType,
    String compensation,
    String commercialTerms,
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
    if (version < 0) {
      throw new IllegalArgumentException("version must be >= 0");
    }
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    title = ApiBoundaryContractRules.requireApiSafeExternalText(title, "title");
    description = ApiBoundaryContractRules.sanitizeExternalText(description, null);
    location = ApiBoundaryContractRules.sanitizeExternalText(location, null);
    seniorityBand = ApiBoundaryContractRules.sanitizeExternalText(seniorityBand, null);
    roleFamily = ApiBoundaryContractRules.sanitizeExternalText(roleFamily, null);
    employmentType = ApiBoundaryContractRules.sanitizeExternalText(employmentType, null);
    compensation = ApiBoundaryContractRules.sanitizeExternalText(compensation, null);
    commercialTerms = ApiBoundaryContractRules.sanitizeConsultantVisibleText(commercialTerms, null);
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
      int sortOrder) {

    public Requirement {
      requirementId = ApiBoundaryContractRules.requireNonBlank(requirementId, "requirementId");
      requirementType = ApiBoundaryContractRules.requireNonBlank(requirementType, "requirementType");
      label = ApiBoundaryContractRules.requireNonBlank(label, "label");
      importance = ApiBoundaryContractRules.requireNonBlank(importance, "importance");
      detail = ApiBoundaryContractRules.sanitizeConsultantVisibleText(detail, null);
      if (sortOrder < 0) {
        throw new IllegalArgumentException("sortOrder must be >= 0");
      }
    }
  }

  public record Scorecard(
      String scorecardId,
      String dimensions,
      String scoringGuidance,
      String status) {

    public Scorecard {
      scorecardId = ApiBoundaryContractRules.requireNonBlank(scorecardId, "scorecardId");
      dimensions = ApiBoundaryContractRules.sanitizeConsultantVisibleText(dimensions, null);
      scoringGuidance = ApiBoundaryContractRules.sanitizeConsultantVisibleText(scoringGuidance, null);
      status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    }
  }
}
