package com.recruitingtransactionos.coreapi.interviewfeedback.port;

import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.MatchCalibrationSignal;
import java.util.List;
import java.util.UUID;

public interface MatchCalibrationSignalPort {

  MatchCalibrationSignal create(MatchCalibrationSignal signal);

  List<MatchCalibrationSignal> findByInterviewFeedbackIdAndOrganizationId(
      UUID organizationId, InterviewFeedbackId interviewFeedbackId);
}
