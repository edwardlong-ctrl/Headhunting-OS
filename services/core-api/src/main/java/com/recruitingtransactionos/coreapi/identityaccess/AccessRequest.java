package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;
import java.util.Set;

public record AccessRequest(
    PortalRole actorRole,
    ResourceType resourceType,
    AccessAction action,
    FieldClassification fieldClassification,
    Set<RelationshipScope> relationshipScopes,
    boolean identityDisclosureRequested) {

  public AccessRequest {
    Objects.requireNonNull(actorRole, "actorRole must not be null");
    Objects.requireNonNull(resourceType, "resourceType must not be null");
    Objects.requireNonNull(action, "action must not be null");
    Objects.requireNonNull(fieldClassification, "fieldClassification must not be null");
    relationshipScopes =
        Set.copyOf(Objects.requireNonNull(relationshipScopes, "relationshipScopes must not be null"));
    relationshipScopes.forEach(scope ->
        Objects.requireNonNull(scope, "relationshipScopes must not contain null values"));
  }

  public boolean hasRelationshipScope(RelationshipScope scope) {
    return relationshipScopes.contains(Objects.requireNonNull(scope, "scope must not be null"));
  }
}
