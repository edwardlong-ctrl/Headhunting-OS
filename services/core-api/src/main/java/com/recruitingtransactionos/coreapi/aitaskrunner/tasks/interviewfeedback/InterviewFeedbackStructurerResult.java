package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import java.util.List;

public record InterviewFeedbackStructurerResult(
    AITaskExecutionResult execution,
    InterviewFeedbackStructurerOutput output,
    List<InterviewFeedbackStructurerOutput.Suggestion> suggestions) {
}
