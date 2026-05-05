package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.MatchCalibrationSignal;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.MatchCalibrationSignalPort;
import java.util.List;
import java.util.UUID;

public final class MatchCalibrationSignalService {

  private final MatchCalibrationSignalPort port;

  public MatchCalibrationSignalService(MatchCalibrationSignalPort port) {
    this.port = port;
  }

  public MatchCalibrationSignal create(MatchCalibrationSignal signal) {
    return port.create(signal);
  }

  public List<MatchCalibrationSignal> findByInterviewFeedbackIdAndOrganizationId(
      UUID organizationId,
      InterviewFeedbackId interviewFeedbackId) {
    return port.findByInterviewFeedbackIdAndOrganizationId(organizationId, interviewFeedbackId);
  }
}
