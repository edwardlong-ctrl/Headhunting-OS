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
    int shortlistCount,
    int pendingUnlockRequestCount,
    int feedbackCount,
    List<ClientShortlistSummaryResponse> recentShortlists) implements ApiSafeResponseBody {

  public ClientDashboardResponse {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    companyName = ApiBoundaryContractRules.requireNonBlank(companyName, "companyName");
    recentShortlists = List.copyOf(Objects.requireNonNull(recentShortlists, "recentShortlists must not be null"));
  }
}
