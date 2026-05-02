package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantDashboardResponse(
    int candidateCount,
    int activeJobCount,
    int companyCount,
    int shortlistCount,
    int pendingFollowUpCount,
    int recentTimelineCount,
    List<ConsultantBlockedActionResponse> blockedActions) implements ApiSafeResponseBody {

  public ConsultantDashboardResponse {
    blockedActions = List.copyOf(blockedActions == null ? List.of() : blockedActions);
  }
}
