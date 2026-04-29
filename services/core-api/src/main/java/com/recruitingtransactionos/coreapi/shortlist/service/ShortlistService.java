package com.recruitingtransactionos.coreapi.shortlist.service;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistCandidateCardPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistPersistencePort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ShortlistService {

  private final ShortlistPersistencePort shortlistPort;
  private final ShortlistCandidateCardPersistencePort cardPort;

  public ShortlistService(
      ShortlistPersistencePort shortlistPort,
      ShortlistCandidateCardPersistencePort cardPort) {
    this.shortlistPort = Objects.requireNonNull(shortlistPort, "shortlistPort must not be null");
    this.cardPort = Objects.requireNonNull(cardPort, "cardPort must not be null");
  }

  public Shortlist createShortlist(Shortlist shortlist) {
    Objects.requireNonNull(shortlist, "shortlist must not be null");
    return shortlistPort.create(shortlist);
  }

  public Optional<Shortlist> findShortlistByIdAndOrganizationId(
      UUID organizationId, ShortlistId shortlistId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    return shortlistPort.findByIdAndOrganizationId(organizationId, shortlistId);
  }

  public List<Shortlist> findShortlistsByJobIdAndOrganizationId(
      UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    return shortlistPort.findByJobIdAndOrganizationId(organizationId, jobId);
  }

  public ShortlistCandidateCard addCandidateCard(ShortlistCandidateCard card) {
    Objects.requireNonNull(card, "card must not be null");
    return cardPort.create(card);
  }

  public List<ShortlistCandidateCard> findCardsByShortlistIdAndOrganizationId(
      UUID organizationId, ShortlistId shortlistId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    return cardPort.findByShortlistIdAndOrganizationId(organizationId, shortlistId);
  }
}
