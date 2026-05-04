package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowActionPolicyTest {

  private static final List<WorkflowEntityType> REQUIRED_ENTITY_TYPES = List.of(
      WorkflowEntityType.JOB,
      WorkflowEntityType.CANDIDATE,
      WorkflowEntityType.SHORTLIST,
      WorkflowEntityType.CONSENT,
      WorkflowEntityType.UNLOCK_REQUEST,
      WorkflowEntityType.DISCLOSURE,
      WorkflowEntityType.PLACEMENT,
      WorkflowEntityType.COMMISSION,
      WorkflowEntityType.CLAIM_LEDGER_ITEM,
      WorkflowEntityType.REVIEW_EVENT,
      WorkflowEntityType.AI_TASK_RUN,
      WorkflowEntityType.REIDENTIFICATION_ASSESSMENT,
      WorkflowEntityType.CANONICAL_WRITE,
      WorkflowEntityType.INFORMATION_PACKET,
      WorkflowEntityType.SOURCE_ITEM);

  private static final List<WorkflowActionCode> REQUIRED_ACTION_CODES = List.of(
      WorkflowActionCode.CLAIM_LEDGER_ITEM_APPENDED,
      WorkflowActionCode.REVIEW_EVENT_APPENDED,
      WorkflowActionCode.CANONICAL_WRITE_ATTEMPTED,
      WorkflowActionCode.CANONICAL_WRITE_ALLOWED,
      WorkflowActionCode.CANONICAL_WRITE_BLOCKED,
      WorkflowActionCode.JOB_SUBMITTED,
      WorkflowActionCode.JOB_INTAKE_REVIEW_STARTED,
      WorkflowActionCode.JOB_MORE_INFO_REQUESTED,
      WorkflowActionCode.JOB_COMMERCIAL_PENDING,
      WorkflowActionCode.JOB_CONTRACT_PENDING,
      WorkflowActionCode.JOB_ACTIVATED,
      WorkflowActionCode.JOB_SHORTLIST_IN_PROGRESS,
      WorkflowActionCode.JOB_SHORTLIST_SENT,
      WorkflowActionCode.JOB_INTERVIEWING,
      WorkflowActionCode.JOB_OFFER_PENDING,
      WorkflowActionCode.JOB_CLOSED,
      WorkflowActionCode.JOB_PAUSED,
      WorkflowActionCode.JOB_CANCELLED,
      WorkflowActionCode.CANDIDATE_PROFILE_PARSED,
      WorkflowActionCode.CANDIDATE_CONSULTANT_REVIEW_STARTED,
      WorkflowActionCode.CANDIDATE_MARKED_AVAILABLE,
      WorkflowActionCode.CANDIDATE_MATCHED_TO_JOB,
      WorkflowActionCode.CANDIDATE_OUTREACH_STARTED,
      WorkflowActionCode.CANDIDATE_INTEREST_RECORDED,
      WorkflowActionCode.CANDIDATE_CONSENT_PENDING,
      WorkflowActionCode.CANDIDATE_CONSENT_CONFIRMED,
      WorkflowActionCode.CANDIDATE_SHORTLISTED,
      WorkflowActionCode.CANDIDATE_CLIENT_REVIEW_STARTED,
      WorkflowActionCode.CANDIDATE_IDENTITY_DISCLOSED,
      WorkflowActionCode.CANDIDATE_INTERVIEWING,
      WorkflowActionCode.CANDIDATE_OFFER_PENDING,
      WorkflowActionCode.CANDIDATE_PLACED,
      WorkflowActionCode.CANDIDATE_REJECTED,
      WorkflowActionCode.CANDIDATE_ARCHIVED,
      WorkflowActionCode.CANDIDATE_DO_NOT_CONTACT_MARKED,
      WorkflowActionCode.SHORTLIST_DRAFT_CREATED,
      WorkflowActionCode.SHORTLIST_CARD_REMOVED,
      WorkflowActionCode.SHORTLIST_CARD_RESTORED,
      WorkflowActionCode.SHORTLIST_RETURNED_TO_DRAFT,
      WorkflowActionCode.SHORTLIST_READY_FOR_REVIEW,
      WorkflowActionCode.SHORTLIST_SENT_TO_CLIENT,
      WorkflowActionCode.SHORTLIST_VIEWED_BY_CLIENT,
      WorkflowActionCode.SHORTLIST_CLIENT_FEEDBACK_PENDING,
      WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED,
      WorkflowActionCode.SHORTLIST_CONTACT_UNLOCKED,
      WorkflowActionCode.SHORTLIST_CLOSED,
      WorkflowActionCode.CONSENT_REQUESTED,
      WorkflowActionCode.CONSENT_VIEWED_BY_CANDIDATE,
      WorkflowActionCode.CONSENT_CONFIRMED,
      WorkflowActionCode.CONSENT_DECLINED,
      WorkflowActionCode.CONSENT_EXPIRED,
      WorkflowActionCode.CONSENT_REVOKED,
      WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED,
      WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED,
      WorkflowActionCode.DISCLOSURE_UNLOCK_REJECTED,
      WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED,
      WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED,
      WorkflowActionCode.DISCLOSURE_FEE_PROTECTION_ACTIVATED,
      WorkflowActionCode.PRIOR_CONTACT_CLAIM_CREATED,
      WorkflowActionCode.PRIOR_APPLICATION_CLAIM_CREATED,
      WorkflowActionCode.SOURCE_ITEM_REGISTERED,
      WorkflowActionCode.INFORMATION_PACKET_CREATED,
      WorkflowActionCode.OFFER_ACCEPTED,
      WorkflowActionCode.CANDIDATE_ONBOARDED,
      WorkflowActionCode.INVOICE_READY,
      WorkflowActionCode.INVOICE_SENT,
      WorkflowActionCode.PAYMENT_MARKED_PAID,
      WorkflowActionCode.GUARANTEE_ACTIVATED,
      WorkflowActionCode.GUARANTEE_COMPLETED,
      WorkflowActionCode.REPLACEMENT_REQUIRED,
      WorkflowActionCode.COMMISSION_PENDING,
      WorkflowActionCode.COMMISSION_PAID,
      WorkflowActionCode.COMMISSION_WITHHELD,
      WorkflowActionCode.AI_TASK_RUN_RECORDED,
      WorkflowActionCode.AI_RECOMMENDATION_RECORDED,
      WorkflowActionCode.REIDENTIFICATION_RISK_ASSESSED,
      WorkflowActionCode.CLIENT_SAFE_REDACTION_BLOCKED);

  @Test
  void requiredEntityTypeVocabularyExistsAndIsStableForAuditStorage() {
    assertThat(List.of(WorkflowEntityType.values())).containsAll(REQUIRED_ENTITY_TYPES);
    assertThat(Stream.of(WorkflowEntityType.values()).map(WorkflowEntityType::wireValue).toList())
        .containsAll(REQUIRED_ENTITY_TYPES.stream().map(Enum::name).toList());
  }

  @Test
  void requiredActionCodesExistAndAreUniqueStableUppercaseValues() {
    List<String> wireValues = Stream.of(WorkflowActionCode.values())
        .map(WorkflowActionCode::wireValue)
        .toList();

    assertThat(List.of(WorkflowActionCode.values())).containsAll(REQUIRED_ACTION_CODES);
    assertThat(new LinkedHashSet<>(wireValues)).hasSize(wireValues.size());
    assertThat(wireValues)
        .allSatisfy(value -> {
          assertThat(value).isNotBlank();
          assertThat(value).isEqualTo(value.toUpperCase(Locale.ROOT));
          assertThat(value).doesNotContain(" ");
        })
        .containsAll(REQUIRED_ACTION_CODES.stream().map(Enum::name).toList());
  }

  @Test
  void actorAndAiInvolvementVocabularyExists() {
    assertThat(Stream.of(ActorRole.values()).map(Enum::name).toList())
        .containsExactlyInAnyOrder(
            "OWNER", "CONSULTANT", "CLIENT", "CANDIDATE", "ADMIN", "SYSTEM", "AI");

    assertThat(Stream.of(WorkflowAiInvolvement.values()).map(Enum::name).toList())
        .containsExactlyInAnyOrder(
            "NONE",
            "AI_RECOMMENDED",
            "AI_ASSISTED",
            "AI_AUTOMATED_LOW_RISK",
            "AI_BLOCKED_BY_POLICY");
  }

  @Test
  void riskTierVocabularyUsesV21Names() {
    assertThat(Stream.of(RiskTier.values()).map(Enum::name).toList())
        .containsExactlyInAnyOrder(
            "T0_AUTOMATED_CLEANUP",
            "T1_LOW_RISK",
            "T2_MEDIUM_RISK",
            "T3_HIGH_RISK",
            "T4_TRANSACTION_LEGAL_BLOCKING");
    assertThat(Stream.of(RiskTier.values()).map(RiskTier::wireValue).toList())
        .containsExactlyInAnyOrder(
            "T0_AUTOMATED_CLEANUP",
            "T1_LOW_RISK",
            "T2_MEDIUM_RISK",
            "T3_HIGH_RISK",
            "T4_TRANSACTION_LEGAL_BLOCKING");
  }

  @Test
  void registryDefinesExactlyOnePolicyForEveryActionWithAllowedEntityTypes() {
    List<WorkflowActionCode> policyActions = WorkflowActionRegistry.standard().policies().stream()
        .map(WorkflowActionPolicy::actionCode)
        .toList();

    assertThat(policyActions).containsExactlyInAnyOrder(WorkflowActionCode.values());
    assertThat(new LinkedHashSet<>(policyActions)).hasSize(policyActions.size());
    assertThat(WorkflowActionRegistry.standard().policies())
        .allSatisfy(policy -> assertThat(policy.allowedEntityTypes()).isNotEmpty());
  }

  @Test
  void unlockWorkflowActionsAreBoundToUnlockRequestEntityOnly() {
    assertThat(WorkflowActionRegistry.standard()
        .policyFor(WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED)
        .allowedEntityTypes())
        .containsExactly(WorkflowEntityType.UNLOCK_REQUEST);
    assertThat(WorkflowActionRegistry.standard()
        .policyFor(WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED)
        .allowedEntityTypes())
        .containsExactly(WorkflowEntityType.UNLOCK_REQUEST);
    assertThat(WorkflowActionRegistry.standard()
        .policyFor(WorkflowActionCode.DISCLOSURE_UNLOCK_REJECTED)
        .allowedEntityTypes())
        .containsExactly(WorkflowEntityType.UNLOCK_REQUEST);
  }

  @Test
  void unknownActionCodesAreRejectedByPolicyLayer() {
    assertThatThrownBy(() -> WorkflowActionRegistry.standard()
        .policyFor("CANDIDATE_ARBITRARY_AUDIT_RECORD"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown workflow action code");
  }

  @Test
  void t3AndT4PoliciesRequireReasonAndHumanFinalActor() {
    assertThat(WorkflowActionRegistry.standard().policies())
        .filteredOn(policy -> Set.of(
            RiskTier.T3_HIGH_RISK,
            RiskTier.T4_TRANSACTION_LEGAL_BLOCKING).contains(policy.riskTier()))
        .isNotEmpty()
        .allSatisfy(policy -> {
          assertThat(policy.reasonRequired()).isTrue();
          assertThat(policy.humanFinalActorRequired()).isTrue();
          assertThat(policy.aiOnlyFinalizationForbidden()).isTrue();
        });
  }

  @Test
  void t3AndT4ActionsRejectAiOnlyFinalActor() {
    WorkflowAuditPolicyRequest aiRequest = new WorkflowAuditPolicyRequest(
        WorkflowActionCode.CANDIDATE_IDENTITY_DISCLOSED,
        WorkflowEntityType.CANDIDATE,
        ActorRole.AI,
        WorkflowAiInvolvement.AI_AUTOMATED_LOW_RISK,
        new WorkflowStateSnapshot("{\"status\":\"client_review\"}"),
        new WorkflowStateSnapshot("{\"status\":\"identity_disclosed\"}"),
        "identity disclosure requires human approval");
    WorkflowAuditPolicyRequest systemRequest = new WorkflowAuditPolicyRequest(
        WorkflowActionCode.CANDIDATE_IDENTITY_DISCLOSED,
        WorkflowEntityType.CANDIDATE,
        ActorRole.SYSTEM,
        WorkflowAiInvolvement.AI_BLOCKED_BY_POLICY,
        new WorkflowStateSnapshot("{\"status\":\"client_review\"}"),
        new WorkflowStateSnapshot("{\"status\":\"identity_disclosed\"}"),
        "identity disclosure requires human approval");

    assertThatThrownBy(() -> WorkflowActionRegistry.standard().validate(aiRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires a human final actor");
    assertThatThrownBy(() -> WorkflowActionRegistry.standard().validate(systemRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires a human final actor");
  }

  @Test
  void stateTransitionActionsRequireBeforeAndAfterState() {
    WorkflowAuditPolicyRequest missingBefore = new WorkflowAuditPolicyRequest(
        WorkflowActionCode.CANDIDATE_SHORTLISTED,
        WorkflowEntityType.CANDIDATE,
        ActorRole.CONSULTANT,
        WorkflowAiInvolvement.AI_ASSISTED,
        null,
        new WorkflowStateSnapshot("{\"status\":\"client_review\"}"),
        "consultant approved shortlist");
    WorkflowAuditPolicyRequest missingAfter = new WorkflowAuditPolicyRequest(
        WorkflowActionCode.CANDIDATE_SHORTLISTED,
        WorkflowEntityType.CANDIDATE,
        ActorRole.CONSULTANT,
        WorkflowAiInvolvement.AI_ASSISTED,
        new WorkflowStateSnapshot("{\"status\":\"consent_confirmed\"}"),
        null,
        "consultant approved shortlist");

    assertThatThrownBy(() -> WorkflowActionRegistry.standard().validate(missingBefore))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("before_state is required");
    assertThatThrownBy(() -> WorkflowActionRegistry.standard().validate(missingAfter))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("after_state is required");
  }

  @Test
  void appendOnlyAuditActionsCanOmitBeforeAndAfterStateWhenPolicyAllowsIt() {
    WorkflowAuditPolicyRequest request = new WorkflowAuditPolicyRequest(
        WorkflowActionCode.CLAIM_LEDGER_ITEM_APPENDED,
        WorkflowEntityType.CLAIM_LEDGER_ITEM,
        ActorRole.SYSTEM,
        WorkflowAiInvolvement.AI_ASSISTED,
        null,
        null,
        null);

    assertThat(WorkflowActionRegistry.standard().validate(request)).isSameAs(request);
  }
}
