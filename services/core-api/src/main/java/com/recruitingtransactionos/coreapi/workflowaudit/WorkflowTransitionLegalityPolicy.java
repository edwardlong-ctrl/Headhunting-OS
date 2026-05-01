package com.recruitingtransactionos.coreapi.workflowaudit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class WorkflowTransitionLegalityPolicy {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final WorkflowTransitionLegalityPolicy STANDARD =
      new WorkflowTransitionLegalityPolicy(Map.ofEntries(
          rule(WorkflowActionCode.JOB_SUBMITTED, Set.of("draft"), "submitted"),
          rule(WorkflowActionCode.JOB_INTAKE_REVIEW_STARTED, Set.of("submitted", "needs_more_info"), "intake_review"),
          rule(WorkflowActionCode.JOB_MORE_INFO_REQUESTED, Set.of("intake_review"), "needs_more_info"),
          rule(WorkflowActionCode.JOB_COMMERCIAL_PENDING, Set.of("submitted", "intake_review", "needs_more_info"), "commercial_pending"),
          rule(WorkflowActionCode.JOB_CONTRACT_PENDING, Set.of("commercial_pending"), "contract_pending"),
          rule(WorkflowActionCode.JOB_ACTIVATED, Set.of("contract_pending"), Set.of("activated", "active")),
          rule(WorkflowActionCode.JOB_SHORTLIST_IN_PROGRESS, Set.of("activated", "shortlist_sent"), "shortlist_in_progress"),
          rule(WorkflowActionCode.JOB_SHORTLIST_SENT, Set.of("shortlist_in_progress"), "shortlist_sent"),
          rule(WorkflowActionCode.JOB_INTERVIEWING, Set.of("shortlist_sent", "offer_pending"), "interviewing"),
          rule(WorkflowActionCode.JOB_OFFER_PENDING, Set.of("interviewing"), "offer_pending"),
          rule(WorkflowActionCode.JOB_CLOSED, Set.of("activated", "shortlist_in_progress", "shortlist_sent", "interviewing", "offer_pending"), "closed"),
          rule(WorkflowActionCode.JOB_PAUSED, Set.of("submitted", "intake_review", "commercial_pending", "contract_pending", "activated", "shortlist_in_progress", "shortlist_sent", "interviewing", "offer_pending"), "paused"),
          rule(WorkflowActionCode.JOB_CANCELLED, Set.of("draft", "submitted", "intake_review", "needs_more_info", "commercial_pending", "contract_pending", "activated", "shortlist_in_progress", "shortlist_sent", "interviewing", "offer_pending", "paused"), "cancelled"),
          rule(WorkflowActionCode.CANDIDATE_PROFILE_PARSED, Set.of("absent"), "parsed"),
          rule(WorkflowActionCode.CANDIDATE_CONSULTANT_REVIEW_STARTED, Set.of("parsed"), "consultant_review"),
          rule(WorkflowActionCode.CANDIDATE_MARKED_AVAILABLE, Set.of("consultant_review"), "available"),
          rule(WorkflowActionCode.CANDIDATE_MATCHED_TO_JOB, Set.of("consultant_review", "available"), "matched_to_job"),
          rule(WorkflowActionCode.CANDIDATE_OUTREACH_STARTED, Set.of("matched_to_job", "available"), "outreach_started"),
          rule(WorkflowActionCode.CANDIDATE_INTEREST_RECORDED, Set.of("outreach_started"), "interest_recorded"),
          rule(WorkflowActionCode.CANDIDATE_CONSENT_PENDING, Set.of("available", "client_review"), "consent_pending"),
          rule(WorkflowActionCode.CANDIDATE_CONSENT_CONFIRMED, Set.of("consent_pending"), "consent_confirmed"),
          rule(WorkflowActionCode.CANDIDATE_SHORTLISTED, Set.of("consultant_review", "available"), "client_review"),
          rule(WorkflowActionCode.CANDIDATE_CLIENT_REVIEW_STARTED, Set.of("consultant_review", "available"), "client_review"),
          rule(WorkflowActionCode.CANDIDATE_IDENTITY_DISCLOSED, Set.of("client_review", "consent_confirmed"), "identity_disclosed"),
          rule(WorkflowActionCode.CANDIDATE_INTERVIEWING, Set.of("client_review", "consent_confirmed", "identity_disclosed"), "interviewing"),
          rule(WorkflowActionCode.CANDIDATE_OFFER_PENDING, Set.of("interviewing"), "offer_pending"),
          rule(WorkflowActionCode.CANDIDATE_PLACED, Set.of("offer_pending"), "placed"),
          rule(WorkflowActionCode.CANDIDATE_REJECTED, Set.of("consultant_review", "available", "client_review", "identity_disclosed", "interviewing", "offer_pending"), "rejected"),
          rule(WorkflowActionCode.CANDIDATE_ARCHIVED, Set.of("parsed", "consultant_review", "available", "client_review", "rejected", "placed"), "archived"),
          rule(WorkflowActionCode.CANDIDATE_DO_NOT_CONTACT_MARKED, Set.of("parsed", "consultant_review", "available", "client_review", "identity_disclosed", "interviewing", "offer_pending", "rejected", "archived"), "do_not_contact"),
          rule(WorkflowActionCode.SHORTLIST_DRAFT_CREATED, Set.of("absent"), "draft"),
          rule(WorkflowActionCode.SHORTLIST_READY_FOR_REVIEW, Set.of("draft"), "ready_for_review"),
          rule(WorkflowActionCode.SHORTLIST_SENT_TO_CLIENT, Set.of("ready_for_review"), "sent_to_client"),
          rule(WorkflowActionCode.SHORTLIST_VIEWED_BY_CLIENT, Set.of("sent_to_client"), "client_viewed"),
          rule(WorkflowActionCode.SHORTLIST_CLIENT_FEEDBACK_PENDING, Set.of("client_viewed"), "client_feedback_pending"),
          rule(WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED, Set.of("client_feedback_pending", "sent_to_client"), "candidate_selected"),
          rule(WorkflowActionCode.SHORTLIST_CONTACT_UNLOCKED, Set.of("candidate_selected"), "contact_unlocked"),
          rule(WorkflowActionCode.SHORTLIST_CLOSED, Set.of("ready_for_review", "sent_to_client", "client_viewed", "client_feedback_pending", "candidate_selected", "contact_unlocked", "interviewing"), "closed"),
          rule(WorkflowActionCode.CONSENT_REQUESTED, Set.of("not_requested"), "requested"),
          rule(WorkflowActionCode.CONSENT_VIEWED_BY_CANDIDATE, Set.of("requested"), "viewed_by_candidate"),
          rule(WorkflowActionCode.CONSENT_CONFIRMED, Set.of("requested", "viewed_by_candidate"), "confirmed"),
          rule(WorkflowActionCode.CONSENT_DECLINED, Set.of("requested", "viewed_by_candidate"), "declined"),
          rule(WorkflowActionCode.CONSENT_EXPIRED, Set.of("requested", "viewed_by_candidate"), "expired"),
          rule(WorkflowActionCode.CONSENT_REVOKED, Set.of("confirmed"), "revoked"),
          rule(WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED, Set.of("not_disclosed", "consent_confirmed"), "requested"),
          rule(WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED, Set.of("requested"), "consultant_approved"),
          rule(WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED, Set.of("consultant_approved", "approved"), "identity_disclosed"),
          rule(WorkflowActionCode.DISCLOSURE_FEE_PROTECTION_ACTIVATED, Set.of("identity_disclosed"), "fee_protection_active"),
          rule(WorkflowActionCode.SOURCE_ITEM_REGISTERED, Set.of("absent"), "registered"),
          rule(WorkflowActionCode.INFORMATION_PACKET_CREATED, Set.of("absent"), "created"),
          rule(WorkflowActionCode.OFFER_ACCEPTED, Set.of("offer_pending"), "offer_accepted"),
          rule(WorkflowActionCode.CANDIDATE_ONBOARDED, Set.of("offer_accepted"), "onboarded"),
          rule(WorkflowActionCode.INVOICE_READY, Set.of("onboarded"), "invoice_ready"),
          rule(WorkflowActionCode.INVOICE_SENT, Set.of("invoice_ready"), "invoice_sent"),
          rule(WorkflowActionCode.PAYMENT_MARKED_PAID, Set.of("invoice_ready", "invoice_sent"), "paid"),
          rule(WorkflowActionCode.GUARANTEE_ACTIVATED, Set.of("paid"), "guarantee_active"),
          rule(WorkflowActionCode.GUARANTEE_COMPLETED, Set.of("guarantee_active"), "guarantee_completed"),
          rule(WorkflowActionCode.REPLACEMENT_REQUIRED, Set.of("guarantee_active", "guarantee_completed"), "replacement_required"),
          rule(WorkflowActionCode.COMMISSION_PENDING, Set.of("calculated"), "pending"),
          rule(WorkflowActionCode.COMMISSION_PAID, Set.of("pending", "calculated"), "paid"),
          rule(WorkflowActionCode.COMMISSION_WITHHELD, Set.of("pending", "calculated"), "withheld")));

  private final Map<WorkflowActionCode, TransitionRule> rules;

  private WorkflowTransitionLegalityPolicy(Map<WorkflowActionCode, TransitionRule> rules) {
    this.rules = Map.copyOf(rules);
  }

  public static WorkflowTransitionLegalityPolicy standard() {
    return STANDARD;
  }

  public void enforce(
      WorkflowActionCode actionCode,
      WorkflowStateSnapshot beforeState,
      WorkflowStateSnapshot afterState) {
    Objects.requireNonNull(actionCode, "actionCode must not be null");
    Objects.requireNonNull(beforeState, "beforeState must not be null");
    Objects.requireNonNull(afterState, "afterState must not be null");

    TransitionRule rule = rules.get(actionCode);
    if (rule == null) {
      return;
    }

    String beforeStatus = extractStatus(beforeState, "beforeState");
    String afterStatus = extractStatus(afterState, "afterState");
    if (!rule.allowedBeforeStatuses().contains(beforeStatus)) {
      throw new IllegalArgumentException(
          "illegal workflow transition before status for "
              + actionCode.wireValue()
              + ": "
              + beforeStatus);
    }
    if (!rule.allowedAfterStatuses().contains(afterStatus)) {
      throw new IllegalArgumentException(
          "illegal workflow transition after status for "
              + actionCode.wireValue()
              + ": "
              + afterStatus);
    }
  }

  private static Map.Entry<WorkflowActionCode, TransitionRule> rule(
      WorkflowActionCode actionCode,
      Set<String> allowedBeforeStatuses,
      String allowedAfterStatus) {
    return rule(actionCode, allowedBeforeStatuses, Set.of(allowedAfterStatus));
  }

  private static Map.Entry<WorkflowActionCode, TransitionRule> rule(
      WorkflowActionCode actionCode,
      Set<String> allowedBeforeStatuses,
      Set<String> allowedAfterStatuses) {
    return Map.entry(
        actionCode,
        new TransitionRule(
            normalize(allowedBeforeStatuses),
            normalize(allowedAfterStatuses)));
  }

  private static Set<String> normalize(Set<String> statuses) {
    return statuses.stream()
        .map(WorkflowTransitionLegalityPolicy::normalize)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String normalize(String status) {
    return status.strip().toLowerCase(Locale.ROOT);
  }

  private static String extractStatus(WorkflowStateSnapshot snapshot, String fieldName) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(snapshot.json());
      JsonNode statusNode = root.get("status");
      if (statusNode == null || !statusNode.isTextual() || statusNode.asText().isBlank()) {
        throw new IllegalArgumentException(fieldName + " must contain a textual status field");
      }
      return normalize(statusNode.asText());
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalArgumentException(fieldName + " must be valid JSON with a status field", exception);
    }
  }

  private record TransitionRule(
      Set<String> allowedBeforeStatuses,
      Set<String> allowedAfterStatuses) {
  }
}
