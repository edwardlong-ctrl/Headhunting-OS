package com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistSummaryResponse;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ConsultantShortlistResponseMapper {

  private ConsultantShortlistResponseMapper() {}

  public static ConsultantShortlistSummaryResponse toSummary(Shortlist shortlist, int cardCount) {
    Objects.requireNonNull(shortlist, "shortlist must not be null");
    return new ConsultantShortlistSummaryResponse(
        shortlist.shortlistId().value().toString(),
        shortlist.title(),
        shortlist.jobId().value().toString(),
        shortlist.status().wireValue(),
        cardCount,
        shortlist.createdAt().toString());
  }

  public static ConsultantShortlistDetailResponse toDetail(
      Shortlist shortlist, List<ShortlistCandidateCard> cards) {
    Objects.requireNonNull(shortlist, "shortlist must not be null");
    Objects.requireNonNull(cards, "cards must not be null");

    List<ConsultantShortlistDetailResponse.Card> cardDtos =
        cards.stream()
            .map(ConsultantShortlistResponseMapper::toCardDto)
            .toList();

    return new ConsultantShortlistDetailResponse(
        shortlist.shortlistId().value().toString(),
        shortlist.jobId().value().toString(),
        shortlist.title(),
        shortlist.status().wireValue(),
        optionalInstant(shortlist.sentAt()),
        optionalInstant(shortlist.clientViewedAt()),
        shortlist.ownerConsultantId() != null
            ? shortlist.ownerConsultantId().toString() : null,
        shortlist.createdAt().toString(),
        shortlist.updatedAt().toString(),
        cardDtos);
  }

  private static ConsultantShortlistDetailResponse.Card toCardDto(ShortlistCandidateCard card) {
    return new ConsultantShortlistDetailResponse.Card(
        card.shortlistCandidateCardId().value().toString(),
        card.anonymousCandidateCardId().toString(),
        card.sortOrder(),
        card.status().wireValue(),
        card.matchReportId() != null ? card.matchReportId().toString() : null);
  }

  private static String optionalInstant(Instant instant) {
    return instant != null ? instant.toString() : null;
  }
}
