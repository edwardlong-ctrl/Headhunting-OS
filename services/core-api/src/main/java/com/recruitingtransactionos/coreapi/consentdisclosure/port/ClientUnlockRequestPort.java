package com.recruitingtransactionos.coreapi.consentdisclosure.port;

import com.recruitingtransactionos.coreapi.consentdisclosure.ClientUnlockRequest;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientUnlockRequestPort {

  ClientUnlockRequest create(ClientUnlockRequest clientUnlockRequest);

  Optional<ClientUnlockRequest> findLatestByShortlistCardAndOrganizationId(
      UUID organizationId,
      ShortlistId shortlistId,
      ShortlistCandidateCardId shortlistCandidateCardId);

  List<ClientUnlockRequest> findByOrganizationId(UUID organizationId);

  List<ClientUnlockRequest> findByClientActorId(UUID organizationId, UUID clientActorId);

  List<ClientUnlockRequest> findByShortlistId(UUID organizationId, ShortlistId shortlistId);
}
