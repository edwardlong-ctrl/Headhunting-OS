package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public final class ClientSafeCandidateCardApiQueryService {

  private final ClientSafeCandidateCardQueryPort queryPort;
  private final PermissionEnforcer permissionEnforcer;

  public ClientSafeCandidateCardApiQueryService(ClientSafeCandidateCardQueryPort queryPort) {
    this(queryPort, new PermissionEnforcer(new PermissionEvaluator()));
  }

  ClientSafeCandidateCardApiQueryService(
      ClientSafeCandidateCardQueryPort queryPort,
      PermissionEnforcer permissionEnforcer) {
    this.queryPort = Objects.requireNonNull(queryPort, "queryPort must not be null");
    this.permissionEnforcer =
        Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public Optional<ClientSafeCandidateCardResponse> findClientSafeCandidateCard(
      AccessRequest accessRequest,
      ClientSafeCandidateCardQueryScope queryScope,
      AnonymousCandidateCardId cardId) {
    permissionEnforcer.requireAllowed(accessRequest);
    requireClientSafeCandidateCardReadContext(accessRequest);
    return queryPort.findByAnonymousCardId(
            Objects.requireNonNull(queryScope, "queryScope must not be null"),
            Objects.requireNonNull(cardId, "cardId must not be null"))
        .map(ClientSafeCandidateCardResponseMapper::from);
  }

  private static void requireClientSafeCandidateCardReadContext(AccessRequest accessRequest) {
    if (accessRequest.resourceType() != ResourceType.CLIENT_SAFE_CANDIDATE_CARD
        || accessRequest.action() != AccessAction.READ) {
      throw new AccessDeniedException(new AccessDecision(
          false,
          "client_safe_candidate_card_access_context_required",
          "Client-safe candidate card API requires a client-safe card read context."));
    }
  }
}
