package com.recruitingtransactionos.coreapi.clientsafeprojection;

import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.Locale;
import java.util.Objects;

public final class ClientSafeCandidateProjectionService {

  private final PermissionEnforcer permissionEnforcer;

  public ClientSafeCandidateProjectionService() {
    this(new PermissionEnforcer(new PermissionEvaluator()));
  }

  ClientSafeCandidateProjectionService(PermissionEnforcer permissionEnforcer) {
    this.permissionEnforcer =
        Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public ClientSafeCandidateCard project(
      AccessRequest accessRequest,
      InternalCandidateProjectionSnapshot snapshot) {
    permissionEnforcer.requireAllowed(accessRequest);
    requireClientSafeCandidateCardReadContext(accessRequest);
    return projectAfterAccessAllowed(snapshot);
  }

  public ClientSafeCandidateCard project(
      AccessRequest accessRequest,
      InternalCandidateProjectionSnapshot snapshot,
      ReidentificationRiskAssessment reidentificationRiskAssessment) {
    permissionEnforcer.requireAllowed(accessRequest);
    requireClientSafeCandidateCardReadContext(accessRequest);
    Objects.requireNonNull(snapshot, "snapshot must not be null");
    Objects.requireNonNull(
        reidentificationRiskAssessment,
        "reidentificationRiskAssessment must not be null");
    if (!snapshot.cardId().equals(reidentificationRiskAssessment.cardId())) {
      throw new IllegalArgumentException(
          "re-identification assessment card id must match projection snapshot");
    }
    if (snapshot.redactionLevel() != reidentificationRiskAssessment.redactionLevel()) {
      throw new IllegalArgumentException(
          "re-identification assessment redaction level must match projection snapshot");
    }
    if (!reidentificationRiskAssessment.isSafeAnonymousClientOutput()) {
      throw new IllegalArgumentException(
          "re-identification assessment blocks anonymous client projection");
    }
    return projectAfterAccessAllowed(snapshot);
  }

  private static void requireClientSafeCandidateCardReadContext(AccessRequest accessRequest) {
    if (accessRequest.resourceType() != ResourceType.CLIENT_SAFE_CANDIDATE_CARD
        || accessRequest.action() != AccessAction.READ) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "client_safe_candidate_card_access_context_required",
          "Client-safe candidate projection requires a client-safe candidate card read context."));
    }
  }

  private ClientSafeCandidateCard projectAfterAccessAllowed(
      InternalCandidateProjectionSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot must not be null");
    if (snapshot.redactionLevel() == RedactionLevel.L4_IDENTITY_DISCLOSED) {
      throw new IllegalArgumentException(
          "client-safe projection cannot use L4 identity disclosure");
    }
    rejectUnsafeFieldSelections(snapshot);
    rejectExactRawSensitiveValueCarryover(snapshot);

    return new ClientSafeCandidateCard(
        snapshot.cardId(),
        snapshot.anonymousCandidateRef(),
        snapshot.projectionVersion(),
        snapshot.redactionLevel(),
        snapshot.generalizedHeadline(),
        snapshot.generalizedRoleFamily(),
        snapshot.generalizedSeniorityBand(),
        snapshot.generalizedLocationRegion(),
        snapshot.safeSummary(),
        snapshot.safeSkillSummary(),
        snapshot.safeEvidenceSummaries(),
        snapshot.safeMatchNarratives());
  }

  private static void rejectUnsafeFieldSelections(InternalCandidateProjectionSnapshot snapshot) {
    for (String fieldPath : snapshot.selectedClientVisibleFieldPaths()) {
      ClientVisibleCandidateFieldPolicy.Decision decision =
          ClientVisibleCandidateFieldPolicy.decide(fieldPath);
      if (!decision.allowed()) {
        throw new IllegalArgumentException(
            "client-visible candidate field is not allowed: "
                + decision.normalizedFieldPath()
                + " ("
                + decision.reason()
                + ")");
      }
    }
  }

  private static void rejectExactRawSensitiveValueCarryover(
      InternalCandidateProjectionSnapshot snapshot) {
    for (String projectedTextValue : snapshot.projectedTextValues()) {
      String normalizedProjectedValue = projectedTextValue.toLowerCase(Locale.ROOT);
      for (String rawSensitiveValue : snapshot.rawSensitiveValues()) {
        if (normalizedProjectedValue.contains(rawSensitiveValue.toLowerCase(Locale.ROOT))) {
          throw new IllegalArgumentException(
              "projected client-safe text contains raw sensitive input value");
        }
      }
    }
  }
}
