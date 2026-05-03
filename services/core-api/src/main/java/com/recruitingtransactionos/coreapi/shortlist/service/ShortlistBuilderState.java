package com.recruitingtransactionos.coreapi.shortlist.service;

import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import java.util.List;
import java.util.Objects;

public record ShortlistBuilderState(
    Shortlist shortlist,
    List<ShortlistCandidateCard> cards,
    List<ShortlistPreSendCheck> preSendChecks,
    ShortlistDeliveryPreview deliveryPreview) {

  public ShortlistBuilderState {
    Objects.requireNonNull(shortlist, "shortlist must not be null");
    cards = List.copyOf(Objects.requireNonNull(cards, "cards must not be null"));
    preSendChecks =
        List.copyOf(Objects.requireNonNull(preSendChecks, "preSendChecks must not be null"));
    Objects.requireNonNull(deliveryPreview, "deliveryPreview must not be null");
  }

  public boolean canSend() {
    return shortlist.status() == ShortlistStatus.READY_FOR_REVIEW
        && preSendChecks.stream().allMatch(ShortlistPreSendCheck::passed);
  }
}
