package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Set;

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
    if (principal == null || principal.organizationId() == null || principal.userAccountId() == null) {
      throw denied(
          "api_access_context_required",
          "Access context is required.");
    }
    return ClientSafeCandidateCardQueryScope.of(
        principal.organizationId(),
        principal.userAccountId(),
        actorRole(principal.portalRole()));
  }

  private static ActorRole actorRole(PortalRole portalRole) {
    return switch (portalRole) {
      case OWNER -> ActorRole.OWNER;
      case CONSULTANT -> ActorRole.CONSULTANT;
      case CLIENT -> ActorRole.CLIENT;
      case CANDIDATE -> ActorRole.CANDIDATE;
      case ADMIN -> ActorRole.ADMIN;
      case SYSTEM, AI_ASSISTANT -> ActorRole.SYSTEM;
      case UNKNOWN -> throw denied(
          "api_access_context_required",
          "Access context is required.");
    };
  }

  private static AccessDeniedException denied(String reasonCode, String safeExplanation) {
    return new AccessDeniedException(new AccessDecision(false, reasonCode, safeExplanation));
  }
}
