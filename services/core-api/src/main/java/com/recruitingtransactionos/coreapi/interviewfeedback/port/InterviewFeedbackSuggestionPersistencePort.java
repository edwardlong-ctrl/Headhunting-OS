package com.recruitingtransactionos.coreapi.interviewfeedback.port;

import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestion;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackSuggestionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewFeedbackSuggestionPersistencePort {

  InterviewFeedbackSuggestion create(InterviewFeedbackSuggestion suggestion);

  InterviewFeedbackSuggestion update(InterviewFeedbackSuggestion suggestion);

  Optional<InterviewFeedbackSuggestion> findByIdAndOrganizationId(
      UUID organizationId, InterviewFeedbackSuggestionId suggestionId);

  List<InterviewFeedbackSuggestion> findByInteractionIdAndOrganizationId(
      UUID organizationId, CandidateCompanyInteractionId interactionId);

  List<InterviewFeedbackSuggestion> listPendingByOrganization(UUID organizationId, int limit);
}
