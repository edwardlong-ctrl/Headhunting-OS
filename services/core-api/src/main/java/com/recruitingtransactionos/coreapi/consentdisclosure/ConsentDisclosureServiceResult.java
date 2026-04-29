package com.recruitingtransactionos.coreapi.consentdisclosure;

import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ConsentDisclosureServiceResult(
    ConsentDisclosureServiceStatus status,
    Optional<DisclosureLevel> allowedLevel,
    List<String> reasonCodes,
    Optional<WorkflowEventId> workflowEventId,
    Optional<String> resultingDisclosureRecordRef) {

  public ConsentDisclosureServiceResult {
    Objects.requireNonNull(status, "status must not be null");
    allowedLevel = Objects.requireNonNull(allowedLevel, "allowedLevel must not be null");
    reasonCodes = List.copyOf(Objects.requireNonNull(reasonCodes, "reasonCodes must not be null"));
    workflowEventId = Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    resultingDisclosureRecordRef = Objects.requireNonNull(
        resultingDisclosureRecordRef,
        "resultingDisclosureRecordRef must not be null");
  }

  static ConsentDisclosureServiceResult allowed(
      DisclosureLevel level,
      WorkflowEventId workflowEventId,
      String disclosureRecordRef) {
    return new ConsentDisclosureServiceResult(
        ConsentDisclosureServiceStatus.ALLOWED,
        Optional.of(level),
        List.of(),
        Optional.of(workflowEventId),
        Optional.of(disclosureRecordRef));
  }

  static ConsentDisclosureServiceResult denied(List<String> reasonCodes) {
    return new ConsentDisclosureServiceResult(
        ConsentDisclosureServiceStatus.DENIED,
        Optional.empty(),
        reasonCodes,
        Optional.empty(),
        Optional.empty());
  }

  static ConsentDisclosureServiceResult requiresReview(List<String> reasonCodes) {
    return new ConsentDisclosureServiceResult(
        ConsentDisclosureServiceStatus.REQUIRES_REVIEW,
        Optional.empty(),
        reasonCodes,
        Optional.empty(),
        Optional.empty());
  }
}
