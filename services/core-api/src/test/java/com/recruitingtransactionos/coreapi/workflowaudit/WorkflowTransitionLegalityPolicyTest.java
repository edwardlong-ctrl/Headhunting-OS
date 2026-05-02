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
}
