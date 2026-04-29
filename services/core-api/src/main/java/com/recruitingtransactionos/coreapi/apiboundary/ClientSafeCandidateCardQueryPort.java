package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import java.util.Optional;

public interface ClientSafeCandidateCardQueryPort {

  Optional<ClientSafeCandidateCard> findByAnonymousCardId(
      ClientSafeCandidateCardQueryScope scope,
      AnonymousCandidateCardId cardId);
}
