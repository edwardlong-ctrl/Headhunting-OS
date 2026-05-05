package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import java.util.List;
import java.util.Objects;

public record ClientDashboardResponse(
    String companyId,
    String companyName,
    boolean companyProfileReady,
    int activeJobCount,
    int pendingClarificationCount,
    int unreadNotificationCount,
    int shortlistCount,
    int pendingUnlockRequestCount,
    int feedbackCount,
    List<String> recentNotifications,
    List<ClientShortlistSummaryResponse> recentShortlists) implements ApiSafeResponseBody {

  public ClientDashboardResponse {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    companyName = ApiBoundaryContractRules.requireNonBlank(companyName, "companyName");
    recentNotifications = List.copyOf(Objects.requireNonNull(
        recentNotifications, "recentNotifications must not be null"));
    recentShortlists = List.copyOf(Objects.requireNonNull(recentShortlists, "recentShortlists must not be null"));
  }
}
