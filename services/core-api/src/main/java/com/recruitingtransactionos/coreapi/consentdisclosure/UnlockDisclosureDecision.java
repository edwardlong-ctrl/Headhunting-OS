package com.recruitingtransactionos.coreapi.consentdisclosure;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record UnlockDisclosureDecision(
    UnlockDisclosureDecisionStatus status,
    Optional<DisclosureLevel> allowedLevel,
    List<String> reasonCodes,
    boolean rawCandidateExposureAllowed,
    boolean roleAloneGrant,
    boolean auditRequiredBeforeRelease,
    Optional<DisclosureAuditCommand> auditCommand) {

  public UnlockDisclosureDecision {
    Objects.requireNonNull(status, "status must not be null");
    allowedLevel = Objects.requireNonNull(allowedLevel, "allowedLevel must not be null");
    reasonCodes = List.copyOf(Objects.requireNonNull(reasonCodes,
        "reasonCodes must not be null"));
    auditCommand = Objects.requireNonNull(auditCommand, "auditCommand must not be null");
  }

  static UnlockDisclosureDecision allow(
      DisclosureLevel level,
      boolean auditRequiredBeforeRelease,
      Optional<DisclosureAuditCommand> auditCommand) {
    return new UnlockDisclosureDecision(
        UnlockDisclosureDecisionStatus.ALLOWED,
        Optional.of(level),
        List.of(),
        false,
        false,
        auditRequiredBeforeRelease,
        auditCommand);
  }

  static UnlockDisclosureDecision deny(List<String> reasonCodes) {
    return new UnlockDisclosureDecision(
        UnlockDisclosureDecisionStatus.DENIED,
        Optional.empty(),
        reasonCodes,
        false,
        false,
        true,
        Optional.empty());
  }
}
