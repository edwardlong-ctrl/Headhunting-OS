package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;
import java.util.Objects;

public record CandidateFollowUpFormResponse(
    String candidateRef,
    String formId,
    String profileVersion,
    List<FollowUpItem> items) implements ApiSafeResponseBody {

  public CandidateFollowUpFormResponse {
    candidateRef = ApiBoundaryContractRules.requireNonBlank(candidateRef, "candidateRef");
    formId = ApiBoundaryContractRules.requireNonBlank(formId, "formId");
    profileVersion = ApiBoundaryContractRules.requireNonBlank(profileVersion, "profileVersion");
    items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
  }

  public record FollowUpItem(
      String fieldPath,
      String prompt,
      String inputType,
      String currentAnswer,
      String status,
      String sourceType,
      String updatedAt) {

    public FollowUpItem {
      fieldPath = ApiBoundaryContractRules.requireNonBlank(fieldPath, "fieldPath");
      prompt = ApiBoundaryContractRules.requireNonBlank(prompt, "prompt");
      inputType = ApiBoundaryContractRules.requireNonBlank(inputType, "inputType");
      currentAnswer = ApiBoundaryContractRules.requireNonBlank(currentAnswer, "currentAnswer");
      status = ApiBoundaryContractRules.requireNonBlank(status, "status");
      sourceType = ApiBoundaryContractRules.requireNonBlank(sourceType, "sourceType");
    }
  }
}
