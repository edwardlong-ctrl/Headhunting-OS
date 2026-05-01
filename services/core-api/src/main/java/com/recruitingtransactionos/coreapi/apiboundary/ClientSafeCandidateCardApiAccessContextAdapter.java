package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class ClientSafeCandidateCardApiAccessContextAdapter {

  private ClientSafeCandidateCardApiAccessContextAdapter() {}

  static AccessRequest fromPrincipal(RtoAuthenticatedPrincipal principal) {
    if (principal == null) {
      throw denied(
          "api_access_context_required",
          "Access context is required.");
    }

    PortalRole actorRole = principal.portalRole();
    FieldClassification fieldClassification = FieldClassification.CLIENT_SAFE;
    boolean identityDisclosureRequested = false;

    return new AccessRequest(
        actorRole,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        fieldClassification,
        Set.of(),
        identityDisclosureRequested);
  }

  static ClientSafeCandidateCardQueryScope queryScopeFromPrincipal(RtoAuthenticatedPrincipal principal) {
    if (principal == null || principal.organizationId() == null) {
      throw denied(
          "api_access_context_required",
          "Access context is required.");
    }
    return ClientSafeCandidateCardQueryScope.of(principal.organizationId());
  }

  private static AccessDeniedException denied(String reasonCode, String safeExplanation) {
    return new AccessDeniedException(new AccessDecision(false, reasonCode, safeExplanation));
  }
}
