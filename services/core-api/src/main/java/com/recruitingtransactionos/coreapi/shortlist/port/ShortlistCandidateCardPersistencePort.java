package com.recruitingtransactionos.coreapi.shortlist.port;

import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import java.util.List;
import java.util.UUID;

public interface ShortlistCandidateCardPersistencePort {

  ShortlistCandidateCard create(ShortlistCandidateCard card);

  List<ShortlistCandidateCard> findByShortlistIdAndOrganizationId(
      UUID organizationId, ShortlistId shortlistId);
}
