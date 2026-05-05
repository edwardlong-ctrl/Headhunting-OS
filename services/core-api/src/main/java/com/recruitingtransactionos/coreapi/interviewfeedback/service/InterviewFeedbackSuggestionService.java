package com.recruitingtransactionos.coreapi.interviewfeedback.service;

import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestion;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.InterviewFeedbackSuggestionPersistencePort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InterviewFeedbackSuggestionService {

  private final InterviewFeedbackSuggestionPersistencePort persistencePort;

  public InterviewFeedbackSuggestionService(
      InterviewFeedbackSuggestionPersistencePort persistencePort) {
    this.persistencePort = persistencePort;
  }

  public InterviewFeedbackSuggestion create(InterviewFeedbackSuggestion suggestion) {
    return persistencePort.create(suggestion);
  }

  public InterviewFeedbackSuggestion update(InterviewFeedbackSuggestion suggestion) {
    return persistencePort.update(suggestion);
  }

  public Optional<InterviewFeedbackSuggestion> findByIdAndOrganizationId(
      UUID organizationId,
      InterviewFeedbackSuggestionId suggestionId) {
    return persistencePort.findByIdAndOrganizationId(organizationId, suggestionId);
  }

  public List<InterviewFeedbackSuggestion> findByInteractionIdAndOrganizationId(
      UUID organizationId,
      CandidateCompanyInteractionId interactionId) {
    return persistencePort.findByInteractionIdAndOrganizationId(organizationId, interactionId);
  }

  public List<InterviewFeedbackSuggestion> listPendingByOrganization(UUID organizationId, int limit) {
    return persistencePort.listPendingByOrganization(organizationId, limit);
  }
}
