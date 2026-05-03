package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.UUID;

public record ClientSafeCandidateCardQueryScope(
    UUID organizationId,
    UUID actorId,
    ActorRole actorRole) {

  public ClientSafeCandidateCardQueryScope {
    if (organizationId == null) {
      throw new NullPointerException("organizationId must not be null");
    }
    if (actorId == null) {
      throw new NullPointerException("actorId must not be null");
    }
    if (actorRole == null) {
      throw new NullPointerException("actorRole must not be null");
    }
  }

  public static ClientSafeCandidateCardQueryScope of(
      UUID organizationId,
      UUID actorId,
      ActorRole actorRole) {
    return new ClientSafeCandidateCardQueryScope(organizationId, actorId, actorRole);
  }
}
