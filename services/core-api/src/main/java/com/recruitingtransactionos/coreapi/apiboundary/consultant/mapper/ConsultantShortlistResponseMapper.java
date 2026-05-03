package com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantShortlistSummaryResponse;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardViewMetadata;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistBuilderState;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistDeliveryPreview;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistPreSendCheck;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ConsultantShortlistResponseMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

  public static ConsultantShortlistDetailResponse toDetail(ShortlistBuilderState state) {
    Objects.requireNonNull(state, "state must not be null");
    Shortlist shortlist = state.shortlist();
    List<ConsultantShortlistDetailResponse.PreSendCheck> preSendChecks = state.preSendChecks()
        .stream()
        .map(ConsultantShortlistResponseMapper::toPreSendCheck)
        .toList();
    List<ConsultantShortlistDetailResponse.Card> cards = state.cards().stream()
        .map(ConsultantShortlistResponseMapper::toCardDto)
        .toList();
    return new ConsultantShortlistDetailResponse(
        shortlist.shortlistId().value().toString(),
        shortlist.version(),
        shortlist.jobId().value().toString(),
        shortlist.title(),
        shortlist.status().wireValue(),
        optionalInstant(shortlist.sentAt()),
        optionalInstant(shortlist.clientViewedAt()),
        shortlist.ownerConsultantId() != null
            ? shortlist.ownerConsultantId().toString() : null,
        shortlist.createdAt().toString(),
        shortlist.updatedAt().toString(),
        preSendChecks,
        toDeliveryPreview(state.deliveryPreview()),
        cards);
  }

  public static ConsultantShortlistDetailResponse toDetail(
      Shortlist shortlist, List<ShortlistCandidateCard> cards) {
    Objects.requireNonNull(shortlist, "shortlist must not be null");
    Objects.requireNonNull(cards, "cards must not be null");
    List<ShortlistCandidateCardViewMetadata> includedMetadata = cards.stream()
        .filter(card -> card.status() == ShortlistCandidateCardStatus.INCLUDED)
        .map(ConsultantShortlistResponseMapper::metadata)
        .flatMap(Optional::stream)
        .toList();
    boolean hasIncludedCards = cards.stream()
        .anyMatch(card -> card.status() == ShortlistCandidateCardStatus.INCLUDED);
    boolean readyForReview = hasReachedReviewGateStatus(shortlist.status());
    boolean anonymousGenerated = cards.stream()
        .filter(card -> card.status() == ShortlistCandidateCardStatus.INCLUDED)
        .allMatch(card -> metadata(card).isPresent());
    boolean deliveryPreviewReady = !includedMetadata.isEmpty();
    return new ConsultantShortlistDetailResponse(
        shortlist.shortlistId().value().toString(),
        shortlist.version(),
        shortlist.jobId().value().toString(),
        shortlist.title(),
        shortlist.status().wireValue(),
        optionalInstant(shortlist.sentAt()),
        optionalInstant(shortlist.clientViewedAt()),
        shortlist.ownerConsultantId() != null ? shortlist.ownerConsultantId().toString() : null,
        shortlist.createdAt().toString(),
        shortlist.updatedAt().toString(),
        List.of(
            new ConsultantShortlistDetailResponse.PreSendCheck(
                "status_ready_for_review", "Shortlist status is ready for review", readyForReview),
            new ConsultantShortlistDetailResponse.PreSendCheck(
                "has_included_cards", "Shortlist includes at least one candidate", hasIncludedCards),
            new ConsultantShortlistDetailResponse.PreSendCheck(
                "anonymous_cards_generated",
                "All included cards have anonymous client-safe summaries",
                anonymousGenerated),
            new ConsultantShortlistDetailResponse.PreSendCheck(
                "delivery_preview_ready",
                "Client-safe delivery preview can be generated",
                deliveryPreviewReady)),
        toDeliveryPreview(defaultPreview(shortlist, includedMetadata)),
        cards.stream().map(ConsultantShortlistResponseMapper::toCardDto).toList());
  }

  private static ConsultantShortlistDetailResponse.Card toCardDto(ShortlistCandidateCard card) {
    ShortlistCandidateCardViewMetadata metadata = metadata(card)
        .orElseGet(() -> emptyMetadata(card));
    return new ConsultantShortlistDetailResponse.Card(
        card.shortlistCandidateCardId().value().toString(),
        toOpaqueCardId(card),
        card.version(),
        card.sortOrder(),
        card.status().wireValue(),
        toOpaqueMatchReportId(card.matchReportId()),
        metadata.anonymousCandidateRef(),
        metadata.generalizedHeadline(),
        metadata.generalizedRoleFamily(),
        metadata.generalizedSeniorityBand(),
        metadata.generalizedLocationRegion(),
        metadata.safeSummary(),
        metadata.safeSkillSummary(),
        metadata.safeEvidenceSummaries(),
        metadata.safeMatchNarratives(),
        metadata.overallScore(),
        metadata.confidence(),
        metadata.reidentificationRiskSignal(),
        metadata.dimensionScores().stream()
            .map(score -> new ConsultantShortlistDetailResponse.DimensionScore(
                score.dimension(), score.score()))
            .toList(),
        card.clientNotes());
  }

  private static ConsultantShortlistDetailResponse.PreSendCheck toPreSendCheck(
      ShortlistPreSendCheck check) {
    return new ConsultantShortlistDetailResponse.PreSendCheck(
        check.code(), check.label(), check.passed());
  }

  private static ConsultantShortlistDetailResponse.DeliveryPreview toDeliveryPreview(
      ShortlistDeliveryPreview preview) {
    return new ConsultantShortlistDetailResponse.DeliveryPreview(
        preview.clientSafeSummary(),
        preview.pdfSummary(),
        preview.emailSummary(),
        preview.wechatSummary());
  }

  private static ShortlistDeliveryPreview defaultPreview(
      Shortlist shortlist, List<ShortlistCandidateCardViewMetadata> metadata) {
    if (metadata.isEmpty()) {
      String empty = "No client-safe shortlist summary is available until at least one candidate card is included.";
      return new ShortlistDeliveryPreview(empty, empty, empty, empty);
    }
    int candidateCount = metadata.size();
    String role = shortlist.title() != null && !shortlist.title().isBlank()
        ? shortlist.title().strip()
        : "this role";
    String firstHeadline = metadata.get(0).generalizedHeadline();
    return new ShortlistDeliveryPreview(
        "Consultant-reviewed shortlist for " + role + " with " + candidateCount
            + " anonymous candidate profiles. Identity remains protected until unlock.",
        "PDF placeholder: deliver " + candidateCount + " anonymous candidate cards for "
            + role + ", led by " + firstHeadline + ".",
        "Email placeholder: " + candidateCount
            + " consultant-reviewed anonymous profiles are ready for client review for " + role + ".",
        "WeChat placeholder: shortlist ready with " + candidateCount
            + " anonymous candidates for " + role + ".");
  }

  private static Optional<ShortlistCandidateCardViewMetadata> metadata(ShortlistCandidateCard card) {
    if (card.metadata() == null || card.metadata().isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(OBJECT_MAPPER.readValue(
          card.metadata(), ShortlistCandidateCardViewMetadata.class));
    } catch (JsonProcessingException exception) {
      return Optional.empty();
    }
  }

  private static ShortlistCandidateCardViewMetadata emptyMetadata(ShortlistCandidateCard card) {
    return new ShortlistCandidateCardViewMetadata(
        "anon_candidate_pending",
        "task29-shortlist-v1",
        "l2_client_safe",
        "Consultant-reviewed candidate",
        "Confidential role family",
        "Consultant-reviewed shortlist level",
        "Location shared after identity unlock",
        "Client-safe summary will be generated after shortlist card enrichment.",
        "Skill summary placeholder is pending shortlist card enrichment.",
        Collections.singletonList("Client-safe evidence summary is pending."),
        Collections.singletonList("Client-safe comparison narrative is pending."),
        null,
        "unknown",
        "not_assessed",
        List.of());
  }

  private static String toOpaqueCardId(ShortlistCandidateCard card) {
    return "card_" + card.anonymousCandidateCardId().toString().replace("-", "");
  }

  private static String toOpaqueMatchReportId(java.util.UUID matchReportId) {
    return matchReportId == null
        ? null
        : "match_report_" + matchReportId.toString().replace("-", "");
  }

  private static String optionalInstant(Instant instant) {
    return instant != null ? instant.toString() : null;
  }

  private static boolean hasReachedReviewGateStatus(ShortlistStatus status) {
    return status != ShortlistStatus.DRAFT;
  }
}
