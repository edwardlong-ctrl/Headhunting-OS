package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import java.util.List;
import java.util.Objects;

public record ClientPreferenceResponse(
    String companyId,
    List<PreferenceItem> preferences) implements ApiSafeResponseBody {

  public ClientPreferenceResponse {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    preferences = List.copyOf(Objects.requireNonNull(preferences, "preferences must not be null"));
  }

  public record PreferenceItem(String preferenceKey, String preferenceValue, String notes) {
    public PreferenceItem {
      preferenceKey = ApiBoundaryContractRules.requireNonBlank(preferenceKey, "preferenceKey");
      preferenceValue = preferenceValue == null ? "{}" : preferenceValue;
      notes = notes;
    }
  }
}
