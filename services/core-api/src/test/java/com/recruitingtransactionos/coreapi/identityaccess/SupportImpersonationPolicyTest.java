package com.recruitingtransactionos.coreapi.identityaccess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupportImpersonationPolicyTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-00000051a001");
  private static final UUID OTHER_ORG_ID = UUID.fromString("00000000-0000-0000-0000-00000051a002");
  private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-00000051a003");
  private static final UUID TARGET_USER_ID = UUID.fromString("00000000-0000-0000-0000-00000051a004");

  @Test
  void supportImpersonationDeniesCrossOrganizationTargetsAndAuditsTheAttempt() {
    RecordingAccessAuditRecorder recorder = new RecordingAccessAuditRecorder();
    SupportImpersonationPolicy policy = new SupportImpersonationPolicy(recorder);

    SupportImpersonationDecision decision = policy.authorize(new SupportImpersonationRequest(
        ORG_ID,
        ADMIN_USER_ID,
        PortalRole.ADMIN,
        OTHER_ORG_ID,
        TARGET_USER_ID,
        PortalRole.CONSULTANT,
        "SUP-5101",
        "debug client-safe shortlist visibility",
        true));

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("support_impersonation_cross_org_denied");
    assertThat(recorder.events).hasSize(1);
    AccessAuditEvent audit = recorder.events.getFirst();
    assertThat(audit.context().organizationId()).isEqualTo(ORG_ID);
    assertThat(audit.context().actorUserId()).isEqualTo(ADMIN_USER_ID);
    assertThat(audit.context().targetEntityId()).isEqualTo(TARGET_USER_ID);
    assertThat(audit.request().action()).isEqualTo(AccessAction.IMPERSONATE);
    assertThat(audit.request().resourceType()).isEqualTo(ResourceType.USER_ACCOUNT);
    assertThat(audit.decision().allowed()).isFalse();
  }

  @Test
  void supportImpersonationRequiresTicketReasonBreakGlassAndNonGovernanceTargetRole() {
    RecordingAccessAuditRecorder recorder = new RecordingAccessAuditRecorder();
    SupportImpersonationPolicy policy = new SupportImpersonationPolicy(recorder);

    assertThat(policy.authorize(new SupportImpersonationRequest(
        ORG_ID,
        ADMIN_USER_ID,
        PortalRole.ADMIN,
        ORG_ID,
        TARGET_USER_ID,
        PortalRole.CONSULTANT,
        "",
        "debug issue",
        true)).reasonCode()).isEqualTo("support_impersonation_ticket_required");
    assertThat(policy.authorize(new SupportImpersonationRequest(
        ORG_ID,
        ADMIN_USER_ID,
        PortalRole.ADMIN,
        ORG_ID,
        TARGET_USER_ID,
        PortalRole.CONSULTANT,
        "SUP-5102",
        " ",
        true)).reasonCode()).isEqualTo("support_impersonation_reason_required");
    assertThat(policy.authorize(new SupportImpersonationRequest(
        ORG_ID,
        ADMIN_USER_ID,
        PortalRole.ADMIN,
        ORG_ID,
        TARGET_USER_ID,
        PortalRole.CONSULTANT,
        "SUP-5103",
        "debug issue",
        false)).reasonCode()).isEqualTo("support_impersonation_break_glass_required");
    assertThat(policy.authorize(new SupportImpersonationRequest(
        ORG_ID,
        ADMIN_USER_ID,
        PortalRole.ADMIN,
        ORG_ID,
        TARGET_USER_ID,
        PortalRole.ADMIN,
        "SUP-5104",
        "debug issue",
        true)).reasonCode()).isEqualTo("support_impersonation_governance_role_denied");
  }

  @Test
  void sameOrganizationAdminCanImpersonateNonGovernanceRoleOnlyWithExplicitAudit() {
    RecordingAccessAuditRecorder recorder = new RecordingAccessAuditRecorder();
    SupportImpersonationPolicy policy = new SupportImpersonationPolicy(recorder);

    SupportImpersonationDecision decision = policy.authorize(new SupportImpersonationRequest(
        ORG_ID,
        ADMIN_USER_ID,
        PortalRole.ADMIN,
        ORG_ID,
        TARGET_USER_ID,
        PortalRole.CLIENT,
        "SUP-5105",
        "reproduce client feedback submission failure",
        true));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("support_impersonation_same_org_admin_allowed");
    assertThat(recorder.events).hasSize(1);
    assertThat(recorder.events.getFirst().decision().allowed()).isTrue();
    assertThat(recorder.events.getFirst().decision().reasonCode())
        .isEqualTo("support_impersonation_same_org_admin_allowed");
  }

  private static final class RecordingAccessAuditRecorder implements AccessAuditRecorder {
    private final List<AccessAuditEvent> events = new ArrayList<>();

    @Override
    public void record(AccessAuditEvent event) {
      events.add(event);
    }
  }
}
