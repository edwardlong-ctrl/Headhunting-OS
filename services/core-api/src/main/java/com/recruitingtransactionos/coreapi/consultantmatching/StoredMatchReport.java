package com.recruitingtransactionos.coreapi.consultantmatching;

import com.recruitingtransactionos.coreapi.matching.MatchReport;
import com.recruitingtransactionos.coreapi.matching.ReidentificationRiskSignal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record StoredMatchReport(
    UUID organizationId,
    MatchReport matchReport,
    String subjectType,
    UUID candidateId,
    UUID shortlistCandidateCardId,
    ReidentificationRiskSignal reidentificationRiskSignal,
    List<String> explanations,
    List<String> interviewQuestions) {

  public StoredMatchReport {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(matchReport, "matchReport must not be null");
    Objects.requireNonNull(subjectType, "subjectType must not be null");
    Objects.requireNonNull(reidentificationRiskSignal, "reidentificationRiskSignal must not be null");
    if (subjectType.isBlank()) {
      throw new IllegalArgumentException("subjectType must not be blank");
    }
    subjectType = subjectType.strip();
    explanations = List.copyOf(explanations == null ? List.of() : explanations);
    interviewQuestions = List.copyOf(interviewQuestions == null ? List.of() : interviewQuestions);
  }
}
