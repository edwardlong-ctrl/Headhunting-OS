package com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantJobSummaryResponse;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ConsultantJobResponseMapper {

  private ConsultantJobResponseMapper() {}

  public static ConsultantJobSummaryResponse toSummary(Job job) {
    Objects.requireNonNull(job, "job must not be null");
    return new ConsultantJobSummaryResponse(
        job.jobId().value().toString(),
        job.title(),
        job.companyId().value().toString(),
        job.status().wireValue(),
        job.createdAt().toString());
  }

  public static ConsultantJobDetailResponse toDetail(
      Job job,
      List<JobRequirement> requirements,
      Optional<JobScorecard> scorecard) {
    Objects.requireNonNull(job, "job must not be null");
    Objects.requireNonNull(requirements, "requirements must not be null");
    Objects.requireNonNull(scorecard, "scorecard must not be null");

    List<ConsultantJobDetailResponse.Requirement> requirementDtos =
        requirements.stream()
            .map(ConsultantJobResponseMapper::toRequirementDto)
            .toList();

    ConsultantJobDetailResponse.Scorecard scorecardDto =
        scorecard.map(ConsultantJobResponseMapper::toScorecardDto).orElse(null);

    return new ConsultantJobDetailResponse(
        job.jobId().value().toString(),
        job.version(),
        job.companyId().value().toString(),
        job.title(),
        job.description(),
        job.location(),
        job.seniorityBand(),
        job.roleFamily(),
        job.employmentType(),
        job.compensation(),
        job.commercialTerms(),
        job.status().wireValue(),
        job.ownerConsultantId() != null
            ? job.ownerConsultantId().toString() : null,
        optionalInstant(job.activatedAt()),
        optionalInstant(job.closedAt()),
        job.closeReason(),
        job.createdAt().toString(),
        job.updatedAt().toString(),
        requirementDtos,
        scorecardDto);
  }

  private static ConsultantJobDetailResponse.Requirement toRequirementDto(JobRequirement req) {
    return new ConsultantJobDetailResponse.Requirement(
        req.jobRequirementId().value().toString(),
        req.requirementType(),
        req.label(),
        req.importance().wireValue(),
        req.detail(),
        req.sortOrder());
  }

  private static ConsultantJobDetailResponse.Scorecard toScorecardDto(JobScorecard scorecard) {
    return new ConsultantJobDetailResponse.Scorecard(
        scorecard.jobScorecardId().value().toString(),
        scorecard.dimensions(),
        scorecard.scoringGuidance(),
        scorecard.status());
  }

  private static String optionalInstant(Instant instant) {
    return instant != null ? instant.toString() : null;
  }
}
