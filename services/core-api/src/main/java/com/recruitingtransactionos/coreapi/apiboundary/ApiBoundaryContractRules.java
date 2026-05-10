package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ApiBoundaryContractRules {

  private static final Set<String> CLIENT_SAFE_CANDIDATE_CARD_RESPONSE_FIELDS =
      Set.of(
          "anonymousCardRef",
          "clientAlias",
          "projectionVersion",
          "redactionLevel",
          "generalizedHeadline",
          "generalizedRoleFamily",
          "generalizedSeniorityBand",
          "generalizedLocationRegion",
          "safeSummary",
          "safeSkillSummary",
          "safeEvidenceSummaries",
          "safeMatchNarratives");

  private static final Set<String> CONSULTANT_CANDIDATE_SUMMARY_RESPONSE_FIELDS =
      Set.of("candidateId", "status", "privacyStatus", "currentProfileId",
          "ownerConsultantId", "lastActivityAt", "createdAt");

  private static final Set<String> CONSULTANT_CANDIDATE_DETAIL_RESPONSE_FIELDS =
      Set.of("candidateId", "status", "privacyStatus", "currentProfileId",
          "profileVersion", "ownerConsultantId", "lastActivityAt", "doNotContactReason",
          "mergedIntoCandidateId", "defaultIndustryPackId", "createdAt",
          "updatedAt", "overview", "evidence", "conflicts", "staleInfo",
          "followUps", "history");

  private static final Set<String> CONSULTANT_COMPANY_SUMMARY_RESPONSE_FIELDS =
      Set.of("companyId", "name", "status", "contactCount", "jobCount", "createdAt");

  private static final Set<String> CONSULTANT_COMPANY_DETAIL_RESPONSE_FIELDS =
      Set.of(
          "companyId", "version", "name", "displayName", "industry", "website", "headquartersLocation",
          "sizeBand", "status", "paymentReliability", "ownerConsultantId", "createdAt",
          "updatedAt", "contacts", "jobCount");

  private static final Set<String> CONSULTANT_JOB_SUMMARY_RESPONSE_FIELDS =
      Set.of("jobId", "title", "companyId", "status", "createdAt");

  private static final Set<String> CONSULTANT_JOB_DETAIL_RESPONSE_FIELDS =
      Set.of(
          "jobId", "version", "companyId", "title", "description", "location", "seniorityBand",
          "roleFamily", "employmentType", "compensation", "status", "ownerConsultantId",
          "activatedAt", "closedAt", "closeReason", "createdAt", "updatedAt",
          "requirements", "scorecard");

  private static final Set<String> CONSULTANT_PLACEMENT_SUMMARY_RESPONSE_FIELDS =
      Set.of(
          "placementId", "version", "jobId", "candidateId", "companyId", "status",
          "salaryAmount", "salaryCurrency", "feeRatePercentage", "expectedFeeAmount",
          "feeAgreementActive", "feeAgreementReference", "paymentTerms", "invoiceReadiness",
          "startDate", "guaranteeDays", "guaranteeExpiresAt", "offerAcceptedAt", "onboardedAt",
          "createdAt", "updatedAt", "notes");

  private static final Set<String> CONSULTANT_COMMISSION_SUMMARY_RESPONSE_FIELDS =
      Set.of(
          "commissionId", "version", "placementId", "consultantId", "status",
          "commissionType", "amount", "currency", "splitPercentage", "salaryAmount",
          "feeRatePercentage", "paidAt", "withheldReason", "createdAt", "updatedAt");

  private static final Set<String> OWNER_PLACEMENT_SUMMARY_RESPONSE_FIELDS =
      Set.of(
          "placementId", "jobId", "candidateId", "companyId", "status", "salaryAmount",
          "salaryCurrency", "feeRatePercentage", "expectedFeeAmount", "commissionStatuses",
          "feeAgreementActive", "feeAgreementReference", "paymentTerms", "invoiceReadiness",
          "accountingExportStatus", "startDate", "guaranteeDays", "guaranteeExpiresAt",
          "createdAt", "updatedAt");

  private static final Set<String> OWNER_COMMISSION_SUMMARY_RESPONSE_FIELDS =
      Set.of(
          "commissionId", "placementId", "consultantId", "status", "commissionType",
          "amount", "currency", "splitPercentage", "salaryAmount", "feeRatePercentage",
          "expectedFeeAmount", "feeAgreementReference", "paymentTerms", "calculationSource",
          "paidAt", "withheldReason", "createdAt", "updatedAt");

  private static final Set<String> OWNER_REVENUE_SUMMARY_RESPONSE_FIELDS =
      Set.of(
          "totalExpectedFee", "totalPaidFee", "placementCount", "unknownExpectedFeePlacementCount", "pendingCommissionCount",
          "paidCommissionCount", "paidCommissionMissingAmountCount", "activeGuaranteeCount", "replacementRequiredCount",
          "invoiceInFlightCount", "invoiceReadyCount", "invoiceSentCount", "paidPlacementCount",
          "guaranteeCompletedCount");

  private static final Set<String> CONSULTANT_SHORTLIST_SUMMARY_RESPONSE_FIELDS =
      Set.of("shortlistId", "title", "jobId", "status", "candidateCount", "createdAt");

  private static final Set<String> CONSULTANT_SHORTLIST_DETAIL_RESPONSE_FIELDS =
      Set.of(
          "shortlistId", "version", "jobId", "title", "status", "sentAt", "clientViewedAt",
          "ownerConsultantId", "createdAt", "updatedAt", "cards");

  private static final Set<String> CONSULTANT_BLOCKED_ACTION_RESPONSE_FIELDS =
      Set.of("entityType", "entityId", "title", "reasonCode", "safeReason", "severity", "route");

  private static final Set<String> CONSULTANT_DASHBOARD_RESPONSE_FIELDS =
      Set.of(
          "candidateCount",
          "activeJobCount",
          "companyCount",
          "shortlistCount",
          "pendingFollowUpCount",
          "recentTimelineCount",
          "blockedActions");

  private static final Set<String> CONSULTANT_WORKFLOW_EVENT_RESPONSE_FIELDS =
      Set.of(
          "workflowEventId",
          "entityType",
          "entityId",
          "actionCode",
          "actorType",
          "aiInvolvement",
          "riskTier",
          "beforeStatus",
          "afterStatus",
          "reason",
          "occurredAt");

  private static final Set<String> CONSULTANT_WORKFLOW_TIMELINE_RESPONSE_FIELDS =
      Set.of("items", "entityStates", "limit", "offset", "hasMore");

  private static final Set<String> CONSULTANT_WORKFLOW_BLOCKER_RESPONSE_FIELDS =
      Set.of("code", "safeReason");

  private static final Set<String> CONSULTANT_WORKFLOW_TRANSITION_OPTION_RESPONSE_FIELDS =
      Set.of("actionCode", "currentStatus", "targetStatus", "allowed", "blockers");

  private static final Set<String> CONSULTANT_WORKFLOW_ENTITY_STATE_RESPONSE_FIELDS =
      Set.of("entityType", "entityId", "currentStatus", "transitionOptions");

  private static final Set<String> CONSULTANT_AUDIT_DRAWER_RESPONSE_FIELDS =
      Set.of("entityType", "entityId", "items");

  private static final Set<String> CONSULTANT_FOLLOW_UP_SUMMARY_RESPONSE_FIELDS =
      Set.of(
          "followUpType",
          "entityType",
          "entityId",
          "title",
          "status",
          "safeReason",
          "route",
          "occurredAt");

  private static final Set<String> AUTH_SESSION_RESPONSE_FIELDS =
      Set.of(
          "organizationId",
          "userAccountId",
          "displayName",
          "portalRole",
          "tokenType",
          "accessToken",
          "refreshToken",
          "accessTokenExpiresAt",
          "refreshTokenExpiresAt");

  private static final Set<String> AUTH_LOGOUT_RESPONSE_FIELDS =
      Set.of("status", "loggedOutAt");

  private static final Set<String> CONSULTANT_PARSED_DOCUMENT_RESPONSE_FIELDS =
      Set.of(
          "processingStatus",
          "parserName",
          "parserVersion",
          "mediaType",
          "ocrRequired",
          "chunkCount",
          "createdAt",
          "completedAt",
          "failureReason");

  private static final Set<String> CONSULTANT_DOCUMENT_EVIDENCE_RESPONSE_FIELDS =
      Set.of("processingStatus", "query", "totalHits", "hits");

  private static final Set<String> CONSULTANT_INTAKE_RUN_RESPONSE_FIELDS =
      Set.of(
          "extractionRunId",
          "informationPacketId",
          "intendedEntityType",
          "status",
          "outputSchemaVersion",
          "cleanFactCount",
          "aiTaskRunIds");

  private static final Set<String> CONSULTANT_INTAKE_QUEUE_RESPONSE_FIELDS =
      Set.of("items");

  private static final Set<String> CONSULTANT_INTAKE_QUEUE_ITEM_RESPONSE_FIELDS =
      Set.of(
          "informationPacketId",
          "title",
          "sourceType",
          "intendedEntityType",
          "stage",
          "stageDetail",
          "createdAt",
          "updatedAt");

  private static final Set<String> CONSULTANT_INTAKE_REVIEW_RESPONSE_FIELDS =
      Set.of(
          "extractionRunId",
          "informationPacketId",
          "intendedEntityType",
          "cleanFactCount",
          "cleanFacts");

  private static final Set<String> CONSULTANT_INTAKE_PUBLISH_RESPONSE_FIELDS =
      Set.of(
          "informationPacketId",
          "canonicalWriteCount",
          "canonicalWriteStatuses",
          "directWrites");

  private static final Set<String> CANDIDATE_ME_RESPONSE_FIELDS =
      Set.of("candidateRef", "displayName", "organizationId", "currentProfileVersion",
          "documentCount", "activeOpportunityCount", "pendingFollowUpCount");

  private static final Set<String> CANDIDATE_PROFILE_REVIEW_RESPONSE_FIELDS =
      Set.of("candidateRef", "profileVersion", "fields");

  private static final Set<String> CANDIDATE_FOLLOW_UP_FORM_RESPONSE_FIELDS =
      Set.of("candidateRef", "formId", "profileVersion", "items");

  private static final Set<String> CANDIDATE_DOCUMENT_SUMMARY_RESPONSE_FIELDS =
      Set.of("documentId", "documentType", "title", "status", "fileSizeBytes", "mimeType", "uploadedAt");

  private static final Set<String> CANDIDATE_OPPORTUNITY_RESPONSE_FIELDS =
      Set.of(
          "interactionId",
          "jobTitle",
          "companyName",
          "status",
          "interactionType",
          "candidateProfileRef",
          "jobRef",
          "consentStatus",
          "consentRecordRef",
          "interestStatus",
          "startedAt",
          "updatedAt");

  private static final Set<String> CANDIDATE_OPPORTUNITY_DETAIL_RESPONSE_FIELDS =
      Set.of(
          "interactionId",
          "jobTitle",
          "companyName",
          "status",
          "interactionType",
          "candidateProfileRef",
          "jobRef",
          "consentRecordRef",
          "consentStatus",
          "roleSummary",
          "location",
          "compensation",
          "fitExplanation",
          "interestStatus",
          "interestUpdatedAt",
          "startedAt",
          "updatedAt");

  private static final Set<String> CANDIDATE_TIMELINE_RESPONSE_FIELDS =
      Set.of("candidateRef", "events");

  private static final Set<String> ANONYMOUS_CLIENT_SAFE_REDACTION_LEVELS =
      Set.of(
          "l0_teaser",
          "l1_generalized",
          "l2_client_safe",
          "l3_consented_detail");

  private static final Pattern SAFE_REASON_CODE = Pattern.compile("[a-z0-9_.-]+");
  private static final Pattern UUID_PATTERN = Pattern.compile(
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern URL_PATTERN = Pattern.compile(
      "(?i)\\b(?:https?://|www\\.)\\S+");
  private static final Pattern PHONE_PATTERN = Pattern.compile(
      "(?<!\\w)\\+?\\d[\\d\\s().-]{7,}\\d(?!\\w)");
  private static final Pattern CAPITALIZED_IDENTITY_PHRASE = Pattern.compile(
      "\\b[A-Z][A-Za-z0-9]+(?:\\s+[A-Z][A-Za-z0-9]+){1,4}\\b");
  private static final Pattern CODE_NAME_PATTERN = Pattern.compile(
      "\\b[A-Z][A-Za-z]+-[A-Z0-9]+(?:\\s+[A-Z0-9]{2,})?\\b");

  private ApiBoundaryContractRules() {}

  public static boolean isAllowedClientSafeCandidateCardResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CLIENT_SAFE_CANDIDATE_CARD_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> clientSafeCandidateCardResponseFieldNames() {
    return new LinkedHashSet<>(CLIENT_SAFE_CANDIDATE_CARD_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantCandidateSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_CANDIDATE_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantCandidateSummaryResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_CANDIDATE_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantCandidateDetailResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_CANDIDATE_DETAIL_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantCandidateDetailResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_CANDIDATE_DETAIL_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantCompanySummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_COMPANY_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantCompanySummaryResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_COMPANY_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantCompanyDetailResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_COMPANY_DETAIL_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantCompanyDetailResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_COMPANY_DETAIL_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantJobSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_JOB_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantJobSummaryResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_JOB_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantJobDetailResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_JOB_DETAIL_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantJobDetailResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_JOB_DETAIL_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantPlacementSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_PLACEMENT_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantPlacementSummaryResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_PLACEMENT_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantCommissionSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_COMMISSION_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantCommissionSummaryResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_COMMISSION_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedOwnerPlacementSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return OWNER_PLACEMENT_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> ownerPlacementSummaryResponseFieldNames() {
    return new LinkedHashSet<>(OWNER_PLACEMENT_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedOwnerCommissionSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return OWNER_COMMISSION_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> ownerCommissionSummaryResponseFieldNames() {
    return new LinkedHashSet<>(OWNER_COMMISSION_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedOwnerRevenueSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return OWNER_REVENUE_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> ownerRevenueSummaryResponseFieldNames() {
    return new LinkedHashSet<>(OWNER_REVENUE_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantShortlistSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_SHORTLIST_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantShortlistSummaryResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_SHORTLIST_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantShortlistDetailResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_SHORTLIST_DETAIL_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantShortlistDetailResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_SHORTLIST_DETAIL_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantBlockedActionResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_BLOCKED_ACTION_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantBlockedActionResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_BLOCKED_ACTION_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantDashboardResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_DASHBOARD_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantDashboardResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_DASHBOARD_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantWorkflowEventResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_WORKFLOW_EVENT_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantWorkflowEventResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_WORKFLOW_EVENT_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantWorkflowTimelineResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_WORKFLOW_TIMELINE_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantWorkflowTimelineResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_WORKFLOW_TIMELINE_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantWorkflowBlockerResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_WORKFLOW_BLOCKER_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantWorkflowBlockerResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_WORKFLOW_BLOCKER_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantWorkflowTransitionOptionResponseField(
      String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_WORKFLOW_TRANSITION_OPTION_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantWorkflowTransitionOptionResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_WORKFLOW_TRANSITION_OPTION_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantWorkflowEntityStateResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_WORKFLOW_ENTITY_STATE_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantWorkflowEntityStateResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_WORKFLOW_ENTITY_STATE_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantAuditDrawerResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_AUDIT_DRAWER_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantAuditDrawerResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_AUDIT_DRAWER_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantFollowUpSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_FOLLOW_UP_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantFollowUpSummaryResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_FOLLOW_UP_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedAuthSessionResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return AUTH_SESSION_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> authSessionResponseFieldNames() {
    return new LinkedHashSet<>(AUTH_SESSION_RESPONSE_FIELDS);
  }

  public static boolean isAllowedAuthLogoutResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return AUTH_LOGOUT_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> authLogoutResponseFieldNames() {
    return new LinkedHashSet<>(AUTH_LOGOUT_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantParsedDocumentResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_PARSED_DOCUMENT_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantParsedDocumentResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_PARSED_DOCUMENT_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantDocumentEvidenceResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_DOCUMENT_EVIDENCE_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantDocumentEvidenceResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_DOCUMENT_EVIDENCE_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantIntakeRunResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_INTAKE_RUN_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantIntakeRunResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_INTAKE_RUN_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantIntakeQueueResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_INTAKE_QUEUE_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantIntakeQueueResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_INTAKE_QUEUE_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantIntakeQueueItemResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_INTAKE_QUEUE_ITEM_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantIntakeQueueItemResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_INTAKE_QUEUE_ITEM_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantIntakeReviewResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_INTAKE_REVIEW_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantIntakeReviewResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_INTAKE_REVIEW_RESPONSE_FIELDS);
  }

  public static boolean isAllowedConsultantIntakePublishResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CONSULTANT_INTAKE_PUBLISH_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> consultantIntakePublishResponseFieldNames() {
    return new LinkedHashSet<>(CONSULTANT_INTAKE_PUBLISH_RESPONSE_FIELDS);
  }

  public static boolean isAllowedCandidateMeResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CANDIDATE_ME_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> candidateMeResponseFieldNames() {
    return new LinkedHashSet<>(CANDIDATE_ME_RESPONSE_FIELDS);
  }

  public static boolean isAllowedCandidateProfileReviewResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CANDIDATE_PROFILE_REVIEW_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> candidateProfileReviewResponseFieldNames() {
    return new LinkedHashSet<>(CANDIDATE_PROFILE_REVIEW_RESPONSE_FIELDS);
  }

  public static boolean isAllowedCandidateFollowUpFormResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CANDIDATE_FOLLOW_UP_FORM_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> candidateFollowUpFormResponseFieldNames() {
    return new LinkedHashSet<>(CANDIDATE_FOLLOW_UP_FORM_RESPONSE_FIELDS);
  }

  public static boolean isAllowedCandidateDocumentSummaryResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CANDIDATE_DOCUMENT_SUMMARY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> candidateDocumentSummaryResponseFieldNames() {
    return new LinkedHashSet<>(CANDIDATE_DOCUMENT_SUMMARY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedCandidateOpportunityResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CANDIDATE_OPPORTUNITY_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> candidateOpportunityResponseFieldNames() {
    return new LinkedHashSet<>(CANDIDATE_OPPORTUNITY_RESPONSE_FIELDS);
  }

  public static boolean isAllowedCandidateOpportunityDetailResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CANDIDATE_OPPORTUNITY_DETAIL_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> candidateOpportunityDetailResponseFieldNames() {
    return new LinkedHashSet<>(CANDIDATE_OPPORTUNITY_DETAIL_RESPONSE_FIELDS);
  }

  public static boolean isAllowedCandidateTimelineResponseField(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      return false;
    }
    return CANDIDATE_TIMELINE_RESPONSE_FIELDS.contains(fieldName.strip());
  }

  public static Set<String> candidateTimelineResponseFieldNames() {
    return new LinkedHashSet<>(CANDIDATE_TIMELINE_RESPONSE_FIELDS);
  }

  public static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }

  static String requireAnonymousClientSafeRedactionLevel(String redactionLevel) {
    String normalized = requireNonBlank(redactionLevel, "redactionLevel").toLowerCase();
    if (!ANONYMOUS_CLIENT_SAFE_REDACTION_LEVELS.contains(normalized)) {
      throw new IllegalArgumentException(
          "redactionLevel must be an anonymous client-safe API level");
    }
    return normalized;
  }

  static List<String> copyNonBlankList(List<String> values, String fieldName) {
    Objects.requireNonNull(values, fieldName + " must not be null");
    return values.stream()
        .map(value -> requireNonBlank(value, fieldName + " item"))
        .toList();
  }

  static <T> List<T> requireNonNullList(List<T> values, String fieldName) {
    Objects.requireNonNull(values, fieldName + " must not be null");
    return values.stream()
        .map(item -> Objects.requireNonNull(item, fieldName + " item must not be null"))
        .toList();
  }

  public static String requireApiSafeExternalText(String value, String fieldName) {
    String stripped = requireNonBlank(value, fieldName);
    if (containsInternalLeakage(stripped)) {
      throw new IllegalArgumentException(fieldName + " must not contain unsafe API-visible text");
    }
    return stripped;
  }

  static String requireBusinessVisibleText(String value, String fieldName) {
    String stripped = requireNonBlank(value, fieldName);
    if (containsConsultantInternalLeakage(stripped)) {
      throw new IllegalArgumentException(fieldName + " must not contain unsafe business-visible text");
    }
    return stripped;
  }

  static List<String> copyApiSafeExternalTextList(List<String> values, String fieldName) {
    Objects.requireNonNull(values, fieldName + " must not be null");
    return values.stream()
        .map(value -> requireApiSafeExternalText(value, fieldName + " item"))
        .toList();
  }

  static String sanitizeReasonCode(String reasonCode, String fallback) {
    if (reasonCode == null || reasonCode.isBlank()) {
      return fallback;
    }
    String normalized = reasonCode.strip().toLowerCase();
    if (!SAFE_REASON_CODE.matcher(normalized).matches()) {
      return fallback;
    }
    return normalized;
  }

  static String sanitizeApiSafeReasonCode(String reasonCode, String fallback) {
    String normalized = sanitizeReasonCode(reasonCode, fallback);
    if (normalized == null) {
      return null;
    }
    if (containsInternalLeakage(normalized)) {
      return fallback;
    }
    return normalized;
  }

  public static String sanitizeExternalText(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String stripped = value.strip();
    if (containsInternalLeakage(stripped)) {
      return fallback;
    }
    return stripped;
  }

  static String sanitizeConsultantVisibleText(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String stripped = value.strip();
    if (containsConsultantInternalLeakage(stripped)) {
      return fallback;
    }
    return stripped;
  }

  static String sanitizeBusinessVisibleText(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String stripped = value.strip();
    if (containsConsultantInternalLeakage(stripped)) {
      return fallback;
    }
    return stripped;
  }

  private static boolean containsInternalLeakage(String value) {
    String lower = value.toLowerCase();
    return UUID_PATTERN.matcher(value).find()
        || EMAIL_PATTERN.matcher(value).find()
        || URL_PATTERN.matcher(value).find()
        || PHONE_PATTERN.matcher(value).find()
        || CAPITALIZED_IDENTITY_PHRASE.matcher(value).find()
        || CODE_NAME_PATTERN.matcher(value).find()
        || lower.contains("com.recruitingtransactionos")
        || lower.contains("candidateprofile")
        || lower.contains("candidate profile")
        || lower.contains("raw candidate")
        || lower.contains("raw profile")
        || lower.contains("sourceitem")
        || lower.contains("source item")
        || lower.contains("informationpacket")
        || lower.contains("information packet")
        || lower.contains("claimledger")
        || lower.contains("claim ledger")
        || lower.contains("reviewevent")
        || lower.contains("review event")
        || lower.contains("workflowevent")
        || lower.contains("workflow event")
        || lower.contains("consentrecord")
        || lower.contains("disclosurerecord")
        || lower.contains("linkedin")
        || lower.contains("raw source")
        || lower.contains("raw cv")
        || lower.contains("cv text")
        || lower.contains("consultant note")
        || lower.contains("consultant-only")
        || lower.contains("consultant internal")
        || lower.contains("current employer")
        || lower.contains("stack trace")
        || lower.contains("stacktrace")
        || lower.contains("exception")
        || lower.contains("\tat ")
        || lower.contains("\n at ")
        || lower.contains("java.");
  }

  private static boolean containsConsultantInternalLeakage(String value) {
    String lower = value.toLowerCase();
    return lower.contains("com.recruitingtransactionos")
        || lower.contains("candidateprofile")
        || lower.contains("candidate profile")
        || lower.contains("raw candidate")
        || lower.contains("raw profile")
        || lower.contains("sourceitem")
        || lower.contains("source item")
        || lower.contains("informationpacket")
        || lower.contains("information packet")
        || lower.contains("claimledger")
        || lower.contains("claim ledger")
        || lower.contains("reviewevent")
        || lower.contains("review event")
        || lower.contains("workflowevent")
        || lower.contains("workflow event")
        || lower.contains("consentrecord")
        || lower.contains("disclosurerecord")
        || lower.contains("stack trace")
        || lower.contains("stacktrace")
        || lower.contains("exception")
        || lower.contains("\tat ")
        || lower.contains("\n at ")
        || lower.contains("java.");
  }
}
