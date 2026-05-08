package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.observability.ObservabilityAITaskRunResponse;
import com.recruitingtransactionos.coreapi.observability.ObservabilityReviewEventResponse;
import com.recruitingtransactionos.coreapi.observability.ObservabilityWorkflowEventResponse;
import java.util.List;

public record ObservabilityDisclosureAuditExportResponse(
    String disclosureRecordRef,
    String status,
    String disclosureLevel,
    String redactionLevel,
    String candidateRef,
    String candidateProfileRef,
    String jobRef,
    String clientRef,
    String consentRecordRef,
    String unlockDecisionRef,
    String requesterRole,
    String requesterUserId,
    String approverRole,
    String approverUserId,
    String consentStatus,
    String consentTextVersion,
    String profileVersion,
    String workflowEventId,
    String decidedAt,
    List<String> missingReasonCodes,
    List<ObservabilityWorkflowEventResponse> workflowEvents,
    List<ObservabilityAITaskRunResponse> aiTaskRuns,
    List<ObservabilityReviewEventResponse> reviewEvents) implements ApiSafeResponseBody {

  public ObservabilityDisclosureAuditExportResponse {
    missingReasonCodes = List.copyOf(missingReasonCodes);
    workflowEvents = List.copyOf(workflowEvents);
    aiTaskRuns = List.copyOf(aiTaskRuns);
    reviewEvents = List.copyOf(reviewEvents);
  }
}
