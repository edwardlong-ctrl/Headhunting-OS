package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class ClientSafeCandidateCardApiAccessContextAdapter {

  static final String ACTOR_ROLE_HEADER = "X-RTO-Actor-Role";
  static final String FIELD_CLASSIFICATION_HEADER = "X-RTO-Field-Classification";
  static final String IDENTITY_DISCLOSURE_HEADER = "X-RTO-Identity-Disclosure-Requested";
  static final String ORGANIZATION_ID_HEADER = "X-RTO-Organization-Id";

  private ClientSafeCandidateCardApiAccessContextAdapter() {}

  static AccessRequest fromHeaders(
      String actorRoleHeader,
      String fieldClassificationHeader,
      String identityDisclosureRequestedHeader) {
    if (isBlank(actorRoleHeader) || isBlank(fieldClassificationHeader)) {
      throw denied(
          "api_access_context_required",
          "Access context is required.");
    }

    PortalRole actorRole = parsePortalRole(actorRoleHeader);
    FieldClassification fieldClassification =
        parseFieldClassification(fieldClassificationHeader);
    boolean identityDisclosureRequested =
        parseIdentityDisclosureRequested(identityDisclosureRequestedHeader);

    return new AccessRequest(
        actorRole,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        fieldClassification,
        Set.of(),
        identityDisclosureRequested);
  }

  static ClientSafeCandidateCardQueryScope queryScopeFromHeaders(String organizationIdHeader) {
    if (isBlank(organizationIdHeader)) {
      return ClientSafeCandidateCardQueryScope.unscoped();
    }
    try {
      return ClientSafeCandidateCardQueryScope.of(UUID.fromString(organizationIdHeader.strip()));
    } catch (IllegalArgumentException exception) {
      throw denied("api_access_context_invalid", "Access context is invalid.");
    }
  }

  private static PortalRole parsePortalRole(String value) {
    String normalized = normalize(value);
    for (PortalRole role : PortalRole.values()) {
      if (role.wireValue().equals(normalized)) {
        return role;
      }
    }
    throw denied("api_access_context_invalid", "Access context is invalid.");
  }

  private static FieldClassification parseFieldClassification(String value) {
    String normalized = normalize(value);
    for (FieldClassification fieldClassification : FieldClassification.values()) {
      if (fieldClassification.wireValue().equals(normalized)) {
        return fieldClassification;
      }
    }
    throw denied("api_access_context_invalid", "Access context is invalid.");
  }

  private static boolean parseIdentityDisclosureRequested(String value) {
    if (isBlank(value)) {
      return false;
    }
    String normalized = normalize(value);
    if ("true".equals(normalized)) {
      return true;
    }
    if ("false".equals(normalized)) {
      return false;
    }
    throw denied("api_access_context_invalid", "Access context is invalid.");
  }

  private static String normalize(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static AccessDeniedException denied(String reasonCode, String safeExplanation) {
    return new AccessDeniedException(new AccessDecision(false, reasonCode, safeExplanation));
  }
}
