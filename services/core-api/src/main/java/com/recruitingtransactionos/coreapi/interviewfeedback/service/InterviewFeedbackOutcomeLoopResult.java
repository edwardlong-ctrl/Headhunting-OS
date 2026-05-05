package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestion;
import com.recruitingtransactionos.coreapi.interviewfeedback.MatchCalibrationSignal;
import java.util.List;

public record InterviewFeedbackOutcomeLoopResult(
    InterviewFeedback feedback,
    List<InterviewFeedbackSuggestion> suggestions,
    List<MatchCalibrationSignal> calibrationSignals,
    String structuredSummary,
    boolean aiStructured) {
}
