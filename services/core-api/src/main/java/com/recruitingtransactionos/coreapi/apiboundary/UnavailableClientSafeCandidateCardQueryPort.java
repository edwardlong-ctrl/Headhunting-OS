package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ClientSafeCandidateCardQueryPort.class)
public final class UnavailableClientSafeCandidateCardQueryPort
    implements ClientSafeCandidateCardQueryPort {

  @Override
  public Optional<ClientSafeCandidateCard> findByAnonymousCardId(AnonymousCandidateCardId cardId) {
    Objects.requireNonNull(cardId, "cardId must not be null");
    return Optional.empty();
  }
}
