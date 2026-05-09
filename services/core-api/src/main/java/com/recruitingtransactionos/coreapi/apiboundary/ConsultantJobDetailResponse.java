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
    String industryPackKey,
    String industryPackLabel,
    String industryPackMaturity,
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
    title = ApiBoundaryContractRules.requireBusinessVisibleText(title, "title");
    description = ApiBoundaryContractRules.sanitizeBusinessVisibleText(description, null);
    location = ApiBoundaryContractRules.sanitizeBusinessVisibleText(location, null);
    seniorityBand = ApiBoundaryContractRules.sanitizeBusinessVisibleText(seniorityBand, null);
    roleFamily = ApiBoundaryContractRules.sanitizeBusinessVisibleText(roleFamily, null);
    employmentType = ApiBoundaryContractRules.sanitizeBusinessVisibleText(employmentType, null);
    compensation = ApiBoundaryContractRules.sanitizeBusinessVisibleText(compensation, null);
    commercialTerms = ApiBoundaryContractRules.sanitizeConsultantVisibleText(commercialTerms, null);
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    industryPackKey = ApiBoundaryContractRules.sanitizeBusinessVisibleText(industryPackKey, null);
    industryPackLabel = ApiBoundaryContractRules.sanitizeBusinessVisibleText(industryPackLabel, null);
    industryPackMaturity = ApiBoundaryContractRules.sanitizeBusinessVisibleText(industryPackMaturity, null);
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    requirements = ApiBoundaryContractRules.requireNonNullList(requirements, "requirements");
  }

  public ConsultantJobDetailResponse(
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
      Scorecard scorecard) {
    this(
        jobId,
        version,
        companyId,
        title,
        description,
        location,
        seniorityBand,
        roleFamily,
        employmentType,
        compensation,
        commercialTerms,
        status,
        null,
        null,
        null,
        ownerConsultantId,
        activatedAt,
        closedAt,
        closeReason,
        createdAt,
        updatedAt,
        requirements,
        scorecard);
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
