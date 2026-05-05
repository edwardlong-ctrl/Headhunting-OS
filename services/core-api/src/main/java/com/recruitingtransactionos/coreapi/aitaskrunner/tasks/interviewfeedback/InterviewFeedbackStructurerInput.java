package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback;

import java.util.List;

public record InterviewFeedbackStructurerInput(
    String shortlistId,
    String shortlistCardId,
    String interviewInteractionId,
    String jobId,
    String candidateId,
    String interviewFeedbackId,
    String decision,
    String outcome,
    String rejectReasonTaxonomy,
    String interviewerName,
    String interviewerRole,
    Integer interviewRound,
    String interviewDate,
    String ratingsJson,
    String strengths,
    String concerns,
    String notes,
    String scorecardJson,
    List<String> evidence) {
}
