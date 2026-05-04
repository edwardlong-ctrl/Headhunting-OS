const CLIENT_MUTABLE_SELECTION_SHORTLIST_STATUSES = new Set([
  "sent_to_client",
  "client_viewed",
  "client_feedback_pending",
  "candidate_selected",
]);

const CLIENT_MUTABLE_UNLOCK_REQUEST_SHORTLIST_STATUSES = new Set([
  "sent_to_client",
  "client_viewed",
  "client_feedback_pending",
  "candidate_selected",
]);

export function canClientSelectCandidate(shortlistStatus: string): boolean {
  return CLIENT_MUTABLE_SELECTION_SHORTLIST_STATUSES.has(shortlistStatus);
}

export function canClientRequestUnlock(shortlistStatus: string, cardStatus: string): boolean {
  const eligibleCard = cardStatus === "selected" || cardStatus === "unlocked";
  return eligibleCard && CLIENT_MUTABLE_UNLOCK_REQUEST_SHORTLIST_STATUSES.has(shortlistStatus);
}

export function canClientSubmitInterviewFeedback(shortlistStatus: string, cardStatus: string): boolean {
  return cardStatus === "unlocked" && (shortlistStatus === "contact_unlocked" || shortlistStatus === "interviewing");
}

export function deriveIdentityAccessStatus(
  shortlistStatus: string,
  approvedDisclosureRecordRef: string | null,
): string {
  if (approvedDisclosureRecordRef) {
    return "identity_disclosed";
  }
  if (shortlistStatus === "contact_unlocked" || shortlistStatus === "interviewing") {
    return "contact_unlocked";
  }
  return "not_unlocked";
}

export function deriveUnlockStageLabel(
  unlockRequestStatus: string | null,
  approvedDisclosureRecordRef: string | null,
): string {
  if (approvedDisclosureRecordRef) {
    return "identity_disclosed";
  }
  if (unlockRequestStatus === "approved") {
    return "approved_pending_disclosure";
  }
  if (unlockRequestStatus === "requested") {
    return "pending_consultant_review";
  }
  if (unlockRequestStatus === "under_review") {
    return "under_review";
  }
  if (unlockRequestStatus === "rejected") {
    return "rejected";
  }
  return "not_requested";
}

export function shouldWarnApprovedWithoutDisclosure(
  unlockRequestStatus: string | null,
  approvedDisclosureRecordRef: string | null,
): boolean {
  return unlockRequestStatus === "approved" && !approvedDisclosureRecordRef;
}
