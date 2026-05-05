package com.recruitingtransactionos.coreapi.interviewfeedback.service;

public final class InterviewFeedbackSuggestionNotFoundException extends RuntimeException {

  public InterviewFeedbackSuggestionNotFoundException() {
    super("interview_feedback_suggestion_not_found");
  }
}
