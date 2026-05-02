package com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCandidateDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCandidateSummaryResponse;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import java.util.Objects;

public final class ConsultantCandidateResponseMapper {

  private ConsultantCandidateResponseMapper() {}

  public static ConsultantCandidateSummaryResponse toSummary(Candidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    return new ConsultantCandidateSummaryResponse(
        candidate.candidateId().value().toString(),
        candidate.status().wireValue(),
        candidate.privacyStatus(),
        candidate.currentProfileId() != null ? candidate.currentProfileId().value().toString() : null,
        candidate.ownerConsultantId() != null ? candidate.ownerConsultantId().toString() : null,
        candidate.lastActivityAt() != null ? candidate.lastActivityAt().toString() : null,
        candidate.createdAt().toString());
  }

  public static ConsultantCandidateDetailResponse toDetail(Candidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    return new ConsultantCandidateDetailResponse(
        candidate.candidateId().value().toString(),
        candidate.status().wireValue(),
        candidate.privacyStatus(),
        candidate.currentProfileId() != null ? candidate.currentProfileId().value().toString() : null,
        candidate.ownerConsultantId() != null ? candidate.ownerConsultantId().toString() : null,
        candidate.lastActivityAt() != null ? candidate.lastActivityAt().toString() : null,
        candidate.doNotContactReason(),
        candidate.mergedIntoCandidateId() != null
            ? candidate.mergedIntoCandidateId().value().toString() : null,
        candidate.defaultIndustryPackId() != null ? candidate.defaultIndustryPackId().toString() : null,
        candidate.createdAt().toString(),
        candidate.updatedAt().toString());
  }
}
