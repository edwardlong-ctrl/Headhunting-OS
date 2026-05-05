package com.recruitingtransactionos.coreapi.placement.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record PlacementCreateCommand(
    UUID organizationId,
    JobId jobId,
    CandidateId candidateId,
    CompanyId companyId,
    UUID actorId,
    BigDecimal salaryAmount,
    String salaryCurrency,
    BigDecimal feeRatePercentage,
    LocalDate startDate,
    Integer guaranteeDays,
    String notes) {

  public PlacementCreateCommand {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    if (guaranteeDays != null && guaranteeDays < 0) {
      throw new IllegalArgumentException("guaranteeDays must be >= 0");
    }
  }
}
