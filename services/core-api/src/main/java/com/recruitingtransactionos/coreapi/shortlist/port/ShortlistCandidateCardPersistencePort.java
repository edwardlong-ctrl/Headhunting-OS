package com.recruitingtransactionos.coreapi.shortlist.port;

import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShortlistCandidateCardPersistencePort {

  ShortlistCandidateCard create(ShortlistCandidateCard card);

  ShortlistCandidateCard update(ShortlistCandidateCard card);

  Optional<ShortlistCandidateCard> findByIdAndOrganizationId(
      UUID organizationId, ShortlistCandidateCardId shortlistCandidateCardId);

  List<ShortlistCandidateCard> findByShortlistIdAndOrganizationId(
      UUID organizationId, ShortlistId shortlistId);
}
