package com.recruitingtransactionos.coreapi.industrypack.service;

import com.recruitingtransactionos.coreapi.industrypack.IndustryPack;
import com.recruitingtransactionos.coreapi.industrypack.IndustryRoleFamilyTemplate;
import com.recruitingtransactionos.coreapi.industrypack.OntologyVersion;
import java.util.Objects;

public record ResolvedIndustryPack(
    IndustryPack industryPack,
    OntologyVersion ontologyVersion,
    IndustryRoleFamilyTemplate roleFamilyTemplate,
    String selectionReason,
    boolean ontologyStale) {

  public ResolvedIndustryPack {
    Objects.requireNonNull(industryPack, "industryPack must not be null");
    Objects.requireNonNull(ontologyVersion, "ontologyVersion must not be null");
    selectionReason = requireNonBlank(selectionReason, "selectionReason");
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    String normalized = value.strip();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
