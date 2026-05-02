package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;
import java.util.Set;

public final class FieldAccessPolicy {

  private static final Set<FieldClassification> CLIENT_SAFE_FIELD_LEVELS =
      Set.of(FieldClassification.CLIENT_SAFE, FieldClassification.GENERALIZED);

  private static final Set<FieldClassification> CLIENT_FORBIDDEN_FIELD_LEVELS =
      Set.of(
          FieldClassification.INTERNAL,
          FieldClassification.PII,
          FieldClassification.RAW_SOURCE,
          FieldClassification.CONSULTANT_PRIVATE,
          FieldClassification.AUDIT,
          FieldClassification.CONSENT_DISCLOSURE,
          FieldClassification.SYSTEM_GOVERNANCE);

  public AccessDecision decide(AccessRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    if (hasUnknownVocabulary(request)) {
      return deny(
          "unknown_access_denied_by_default",
          "Unknown role, resource, action, or field vocabulary is denied by default.");
    }

    if (request.identityDisclosureRequested()) {
      return deny(
          "identity_disclosure_not_implemented",
          "Identity-disclosed access requires future consent, disclosure, and unlock gates.");
    }

    if (request.actorRole().isGovernanceOrAutomationRole()
        && isCanonicalWriteLikeRequest(request)) {
      return deny(
          "domain_gate_required_for_canonical_write",
          "Governance and automation roles do not bypass canonical-write domain gates.");
    }

    if (request.actorRole() == PortalRole.CLIENT) {
      return decideClientAccess(request);
    }

    if (request.actorRole() == PortalRole.CANDIDATE) {
      return decideCandidateAccess(request);
    }

    if (request.actorRole() == PortalRole.CONSULTANT) {
      return decideConsultantAccess(request);
    }

    return deny(
        "access_denied_by_default",
        "No allow rule exists for this role, action, resource, and field classification.");
  }

  public boolean isAllowed(AccessRequest request) {
    return decide(request).allowed();
  }

  private static AccessDecision decideClientAccess(AccessRequest request) {
    if (request.resourceType() == ResourceType.CANDIDATE) {
      return deny(
          "client_raw_candidate_denied",
          "Client role cannot read raw Candidate resources before future disclosure gates.");
    }
    if (request.resourceType() == ResourceType.CANDIDATE_PROFILE) {
      return deny(
          "client_raw_candidate_profile_denied",
          "Client role cannot read raw CandidateProfile resources before future disclosure gates.");
    }
    if (request.resourceType() == ResourceType.CONSENT_RECORD
        || request.resourceType() == ResourceType.DISCLOSURE_RECORD) {
      return deny(
          "client_consent_disclosure_record_denied",
          "Client role cannot read internal consent or disclosure records in this evaluator.");
    }
    if (CLIENT_FORBIDDEN_FIELD_LEVELS.contains(request.fieldClassification())) {
      return deny(
          "client_unsafe_field_denied",
          "Client role can read only client-safe or generalized field classifications here.");
    }
    if (request.action() == AccessAction.READ
        && request.resourceType() == ResourceType.CLIENT_SAFE_CANDIDATE_CARD
        && CLIENT_SAFE_FIELD_LEVELS.contains(request.fieldClassification())) {
      return AccessDecision.allow(
          "client_safe_candidate_card_read_allowed",
          "Client role may read anonymous client-safe candidate cards at safe field levels.");
    }
    if ((request.resourceType() == ResourceType.COMPANY || request.resourceType() == ResourceType.JOB)
        && CLIENT_SAFE_FIELD_LEVELS.contains(request.fieldClassification())
        && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
      if (request.action() == AccessAction.READ) {
        return AccessDecision.allow(
            "client_same_org_safe_read_allowed",
            "Client role may read same-organization client-safe company and job intake surfaces.");
      }
      if (request.action() == AccessAction.CREATE || request.action() == AccessAction.UPDATE) {
        return AccessDecision.allow(
            "client_same_org_safe_write_allowed",
            "Client role may submit same-organization client-safe company and job intake data.");
      }
    }
    return deny(
        "access_denied_by_default",
        "No client allow rule exists for this request.");
  }

  private static AccessDecision decideCandidateAccess(AccessRequest request) {
    if (request.action() != AccessAction.READ
        || request.resourceType() != ResourceType.CANDIDATE_PROFILE) {
      return deny(
          "access_denied_by_default",
          "No candidate allow rule exists for this request.");
    }
    if (!request.hasRelationshipScope(RelationshipScope.SELF)) {
      return deny(
          "candidate_self_scope_required",
          "Candidate profile reads require explicit SELF relationship scope.");
    }
    if (CLIENT_SAFE_FIELD_LEVELS.contains(request.fieldClassification())) {
      return AccessDecision.allow(
          "candidate_self_profile_read_allowed",
          "Candidate role may read explicitly self-scoped safe profile fields.");
    }
    return deny(
        "candidate_unsafe_field_denied",
        "Candidate SELF scope does not allow unsafe profile field classifications here.");
  }

  private static boolean hasUnknownVocabulary(AccessRequest request) {
    return request.actorRole() == PortalRole.UNKNOWN
        || request.resourceType() == ResourceType.UNKNOWN
        || request.action() == AccessAction.UNKNOWN
        || request.fieldClassification() == FieldClassification.UNKNOWN;
  }

  private static boolean isCanonicalWriteLikeRequest(AccessRequest request) {
    return isCanonicalWriteLikeAction(request.action())
        && isCanonicalWriteLikeResource(request.resourceType());
  }

  private static boolean isCanonicalWriteLikeAction(AccessAction action) {
    return action == AccessAction.CREATE
        || action == AccessAction.UPDATE
        || action == AccessAction.APPROVE;
  }

  private static boolean isCanonicalWriteLikeResource(ResourceType resourceType) {
    return resourceType == ResourceType.CANDIDATE
        || resourceType == ResourceType.CANDIDATE_PROFILE
        || resourceType == ResourceType.JOB
        || resourceType == ResourceType.COMPANY
        || resourceType == ResourceType.MATCH_REPORT
        || resourceType == ResourceType.CLAIM_LEDGER_ITEM;
  }

  private static AccessDecision deny(String reasonCode, String safeExplanation) {
    return AccessDecision.deny(reasonCode, safeExplanation);
  }

  private static AccessDecision decideConsultantAccess(AccessRequest request) {
    if (request.action() == AccessAction.READ) {
      if (request.resourceType() == ResourceType.CANDIDATE
          && request.fieldClassification() == FieldClassification.INTERNAL
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
        return AccessDecision.allow(
            "consultant_candidate_read_allowed",
            "Consultant role may read same-organization candidate resources.");
      }
      if (request.resourceType() == ResourceType.COMPANY
          || request.resourceType() == ResourceType.JOB
          || request.resourceType() == ResourceType.SHORTLIST) {
        return AccessDecision.allow(
            "consultant_read_allowed",
            "Consultant role may read company, job, and shortlist resources.");
      }
      if (request.resourceType() == ResourceType.INFORMATION_PACKET
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)
          && (request.fieldClassification() == FieldClassification.INTERNAL
              || request.fieldClassification() == FieldClassification.SYSTEM_GOVERNANCE)) {
        return AccessDecision.allow(
            "consultant_information_packet_read_allowed",
            "Consultant role may read same-organization governed intake packets.");
      }
      if (request.resourceType() == ResourceType.WORKFLOW_EVENT
          && request.fieldClassification() == FieldClassification.INTERNAL
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
        return AccessDecision.allow(
            "consultant_workflow_event_read_allowed",
            "Consultant role may read same-organization workflow and operational views.");
      }
      if (request.resourceType() == ResourceType.SOURCE_ITEM
          && request.fieldClassification() == FieldClassification.RAW_SOURCE
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
        return AccessDecision.allow(
            "consultant_source_item_read_allowed",
            "Consultant role may read same-organization raw source items.");
      }
      if (request.resourceType() == ResourceType.MATCH_REPORT
          && request.fieldClassification() == FieldClassification.CONSULTANT_PRIVATE
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
        return AccessDecision.allow(
            "consultant_match_report_read_allowed",
            "Consultant role may read same-organization consultant-private match reports.");
      }
    }
    if (request.action() == AccessAction.CREATE
        || request.action() == AccessAction.UPDATE) {
      if (request.resourceType() == ResourceType.COMPANY
          || request.resourceType() == ResourceType.JOB
          || request.resourceType() == ResourceType.SHORTLIST) {
        return AccessDecision.allow(
            "consultant_write_allowed",
            "Consultant role may create and update company and job resources.");
      }
      if (request.resourceType() == ResourceType.INFORMATION_PACKET
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)
          && (request.fieldClassification() == FieldClassification.INTERNAL
              || request.fieldClassification() == FieldClassification.SYSTEM_GOVERNANCE)) {
        return AccessDecision.allow(
            "consultant_information_packet_update_allowed",
            "Consultant role may operate on same-organization governed intake packets.");
      }
      if (request.action() == AccessAction.UPDATE
          && request.resourceType() == ResourceType.SOURCE_ITEM
          && request.fieldClassification() == FieldClassification.RAW_SOURCE
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
        return AccessDecision.allow(
            "consultant_source_item_update_allowed",
            "Consultant role may operate on same-organization raw source items.");
      }
      if (request.action() == AccessAction.CREATE
          && request.resourceType() == ResourceType.SOURCE_ITEM
          && request.fieldClassification() == FieldClassification.RAW_SOURCE
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
        return AccessDecision.allow(
            "consultant_source_item_create_allowed",
            "Consultant role may upload same-organization raw source items.");
      }
      if (request.action() == AccessAction.CREATE
          && request.resourceType() == ResourceType.MATCH_REPORT
          && request.fieldClassification() == FieldClassification.CONSULTANT_PRIVATE
          && request.hasRelationshipScope(RelationshipScope.SAME_ORGANIZATION)) {
        return AccessDecision.allow(
            "consultant_match_report_create_allowed",
            "Consultant role may create same-organization consultant-private match reports.");
      }
    }
    return deny(
        "access_denied_by_default",
        "No consultant allow rule exists for this request.");
  }
}
