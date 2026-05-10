package com.recruitingtransactionos.coreapi.workflowaudit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import org.junit.jupiter.api.Test;

class WorkflowTransitionLegalityPolicyTest {

  private final WorkflowTransitionLegalityPolicy policy = WorkflowTransitionLegalityPolicy.standard();

  @Test
  void candidateVocabularyUsesCurrentWireStatuses() {
    policy.enforce(
        WorkflowActionCode.CANDIDATE_PROFILE_PARSED,
        new WorkflowStateSnapshot("{\"status\":\"new\"}"),
        new WorkflowStateSnapshot("{\"status\":\"profile_parsed\"}"));
    policy.enforce(
        WorkflowActionCode.CANDIDATE_OUTREACH_STARTED,
        new WorkflowStateSnapshot("{\"status\":\"matched_to_job\"}"),
        new WorkflowStateSnapshot("{\"status\":\"outreach\"}"));
    policy.enforce(
        WorkflowActionCode.CANDIDATE_INTEREST_RECORDED,
        new WorkflowStateSnapshot("{\"status\":\"outreach\"}"),
        new WorkflowStateSnapshot("{\"status\":\"interested\"}"));
  }

  @Test
  void legacyCandidateStatusesAreRejected() {
    assertThatThrownBy(() -> policy.enforce(
        WorkflowActionCode.CANDIDATE_PROFILE_PARSED,
        new WorkflowStateSnapshot("{\"status\":\"absent\"}"),
        new WorkflowStateSnapshot("{\"status\":\"parsed\"}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("illegal workflow transition after status");
  }

  @Test
  void previewTransitionsReturnAllowedAndBlockedActionsForCurrentState() {
    assertThat(policy.previewTransitions("candidate", "consultant_review"))
        .anySatisfy(decision -> {
          if ("CANDIDATE_MARKED_AVAILABLE".equals(decision.actionCode())) {
            assertThat(decision.allowed()).isTrue();
            assertThat(decision.targetStatus()).isEqualTo("available");
          }
        })
        .anySatisfy(decision -> {
          if ("CANDIDATE_SHORTLISTED".equals(decision.actionCode())) {
            assertThat(decision.targetStatus()).isEqualTo("shortlisted");
          }
        })
        .anySatisfy(decision -> {
          if ("CANDIDATE_PLACED".equals(decision.actionCode())) {
            assertThat(decision.allowed()).isFalse();
            assertThat(decision.hasBlocker("illegal_before_status")).isTrue();
          }
        });
  }

  @Test
  void jobActivatedPreviewUsesCanonicalTargetStatus() {
    assertThat(policy.previewTransitions("job", "contract_pending"))
        .anySatisfy(decision -> {
          if ("JOB_ACTIVATED".equals(decision.actionCode())) {
            assertThat(decision.allowed()).isTrue();
            assertThat(decision.targetStatus()).isEqualTo("activated");
          }
        });
  }

  @Test
  void shortlistCandidateSelected_requiresViewedStateInsteadOfAllowingDirectSentTransition() {
    policy.enforce(
        WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED,
        new WorkflowStateSnapshot("{\"status\":\"client_viewed\"}"),
        new WorkflowStateSnapshot("{\"status\":\"candidate_selected\"}"));

    assertThatThrownBy(() -> policy.enforce(
        WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED,
        new WorkflowStateSnapshot("{\"status\":\"sent_to_client\"}"),
        new WorkflowStateSnapshot("{\"status\":\"candidate_selected\"}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("illegal workflow transition before status");
  }

  @Test
  void disclosureUnlockApprovalAndRejectionUseCanonicalRequestedTerminalStates() {
    policy.enforce(
        WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED,
        new WorkflowStateSnapshot("{\"status\":\"requested\"}"),
        new WorkflowStateSnapshot("{\"status\":\"approved\"}"));
    policy.enforce(
        WorkflowActionCode.DISCLOSURE_UNLOCK_REJECTED,
        new WorkflowStateSnapshot("{\"status\":\"requested\"}"),
        new WorkflowStateSnapshot("{\"status\":\"rejected\"}"));

    assertThatThrownBy(() -> policy.enforce(
        WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED,
        new WorkflowStateSnapshot("{\"status\":\"requested\"}"),
        new WorkflowStateSnapshot("{\"status\":\"consultant_approved\"}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("illegal workflow transition after status");
  }

  @Test
  void disclosureConsultantApprovalUsesConsultantApprovedIntermediateState() {
    policy.enforce(
        WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED,
        new WorkflowStateSnapshot("{\"status\":\"requested\"}"),
        new WorkflowStateSnapshot("{\"status\":\"consultant_approved\"}"));
    policy.enforce(
        WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED,
        new WorkflowStateSnapshot("{\"status\":\"consultant_approved\"}"),
        new WorkflowStateSnapshot("{\"status\":\"identity_disclosed\"}"));

    assertThatThrownBy(() -> policy.enforce(
        WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED,
        new WorkflowStateSnapshot("{\"status\":\"requested\"}"),
        new WorkflowStateSnapshot("{\"status\":\"approved\"}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("illegal workflow transition after status");
  }

  @Test
  void unlockRequestPreviewListsUnlockActionsWhileDisclosurePreviewDoesNot() {
    assertThat(policy.previewTransitions("unlock_request", "requested"))
        .extracting(WorkflowTransitionDecision::actionCode)
        .containsExactlyInAnyOrder(
            WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED.wireValue(),
            WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED.wireValue(),
            WorkflowActionCode.DISCLOSURE_UNLOCK_REJECTED.wireValue());

    assertThat(policy.previewTransitions("disclosure", "requested"))
        .extracting(WorkflowTransitionDecision::actionCode)
        .doesNotContain(
            WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED.wireValue(),
            WorkflowActionCode.DISCLOSURE_UNLOCK_APPROVED.wireValue(),
            WorkflowActionCode.DISCLOSURE_UNLOCK_REJECTED.wireValue());
  }

  @Test
  void placementPaymentRequiresInvoiceSentInsteadOfSkippingInvoiceSentState() {
    policy.enforce(
        WorkflowActionCode.PAYMENT_MARKED_PAID,
        new WorkflowStateSnapshot("{\"status\":\"invoice_sent\"}"),
        new WorkflowStateSnapshot("{\"status\":\"paid\"}"));

    assertThatThrownBy(() -> policy.enforce(
        WorkflowActionCode.PAYMENT_MARKED_PAID,
        new WorkflowStateSnapshot("{\"status\":\"invoice_ready\"}"),
        new WorkflowStateSnapshot("{\"status\":\"paid\"}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("illegal workflow transition before status");
  }
}
