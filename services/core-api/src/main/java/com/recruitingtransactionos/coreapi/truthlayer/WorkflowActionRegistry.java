package com.recruitingtransactionos.coreapi.truthlayer;

import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorkflowActionRegistry {

  private static final WorkflowActionRegistry STANDARD = buildStandard();

  private final Map<WorkflowActionCode, WorkflowActionPolicy> policies;

  private WorkflowActionRegistry(Map<WorkflowActionCode, WorkflowActionPolicy> policies) {
    this.policies = Map.copyOf(policies);
    if (this.policies.size() != WorkflowActionCode.values().length) {
      throw new IllegalStateException("every workflow action code must have exactly one policy");
    }
  }

  public static WorkflowActionRegistry standard() {
    return STANDARD;
  }

  public Collection<WorkflowActionPolicy> policies() {
    return policies.values();
  }

  public WorkflowActionPolicy policyFor(String actionCode) {
    return policyFor(WorkflowActionCode.fromWireValue(actionCode));
  }

  public WorkflowActionPolicy policyFor(WorkflowActionCode actionCode) {
    Objects.requireNonNull(actionCode, "actionCode must not be null");
    WorkflowActionPolicy policy = policies.get(actionCode);
    if (policy == null) {
      throw new IllegalArgumentException("unknown workflow action code: " + actionCode.wireValue());
    }
    return policy;
  }

  public WorkflowAuditPolicyRequest validate(WorkflowAuditPolicyRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    WorkflowActionPolicy policy = policyFor(request.actionCode());
    if (!policy.allowedEntityTypes().contains(request.entityType())) {
      throw new IllegalArgumentException("workflow action " + request.actionCode().wireValue()
          + " is not allowed for entity type " + request.entityType().wireValue());
    }
    if (policy.beforeStateRequired() && request.beforeState() == null) {
      throw new IllegalArgumentException("before_state is required for workflow action "
          + request.actionCode().wireValue());
    }
    if (policy.afterStateRequired() && request.afterState() == null) {
      throw new IllegalArgumentException("after_state is required for workflow action "
          + request.actionCode().wireValue());
    }
    if (policy.reasonRequired()
        && (request.reason() == null || request.reason().isBlank())) {
      throw new IllegalArgumentException("reason is required for workflow action "
          + request.actionCode().wireValue());
    }
    if (policy.humanFinalActorRequired() && !isHumanActor(request.actorRole())) {
      throw new IllegalArgumentException("workflow action " + request.actionCode().wireValue()
          + " requires a human final actor");
    }
    if (policy.aiOnlyFinalizationForbidden() && request.actorRole() == ActorRole.AI) {
      throw new IllegalArgumentException("workflow action " + request.actionCode().wireValue()
          + " forbids AI-only finalization");
    }
    return request;
  }

  private static boolean isHumanActor(ActorRole actorRole) {
    return actorRole == ActorRole.OWNER
        || actorRole == ActorRole.CONSULTANT
        || actorRole == ActorRole.CLIENT
        || actorRole == ActorRole.CANDIDATE
        || actorRole == ActorRole.ADMIN;
  }

  private static WorkflowActionRegistry buildStandard() {
    PolicyMap policies = new PolicyMap();

    policies.add(auditOnly(
        WorkflowActionCode.CLAIM_LEDGER_ITEM_APPENDED,
        RiskTier.T1_LOW_RISK,
        false,
        false,
        "Claim ledger item was appended for later review.",
        WorkflowEntityType.CLAIM_LEDGER_ITEM));
    policies.add(auditOnly(
        WorkflowActionCode.REVIEW_EVENT_APPENDED,
        RiskTier.T2_MEDIUM_RISK,
        true,
        true,
        "Human review event was appended.",
        WorkflowEntityType.REVIEW_EVENT));
    policies.add(auditOnly(
        WorkflowActionCode.CANONICAL_WRITE_ATTEMPTED,
        RiskTier.T2_MEDIUM_RISK,
        true,
        true,
        "Canonical write gate was attempted.",
        canonicalWriteEntities()));
    policies.add(transition(
        WorkflowActionCode.CANONICAL_WRITE_ALLOWED,
        RiskTier.T3_HIGH_RISK,
        "Canonical write gate allowed the audit boundary.",
        canonicalWriteEntities()));
    policies.add(auditOnly(
        WorkflowActionCode.CANONICAL_WRITE_BLOCKED,
        RiskTier.T3_HIGH_RISK,
        true,
        true,
        "Canonical write gate blocked the attempt.",
        canonicalWriteEntities()));

    policies.addAll(transitions(WorkflowEntityType.JOB, RiskTier.T2_MEDIUM_RISK,
        List.of(
            WorkflowActionCode.JOB_SUBMITTED,
            WorkflowActionCode.JOB_INTAKE_REVIEW_STARTED,
            WorkflowActionCode.JOB_MORE_INFO_REQUESTED)));
    policies.addAll(transitions(WorkflowEntityType.JOB, RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        List.of(
            WorkflowActionCode.JOB_COMMERCIAL_PENDING,
            WorkflowActionCode.JOB_CONTRACT_PENDING,
            WorkflowActionCode.JOB_OFFER_PENDING)));
    policies.addAll(transitions(WorkflowEntityType.JOB, RiskTier.T3_HIGH_RISK,
        List.of(
            WorkflowActionCode.JOB_ACTIVATED,
            WorkflowActionCode.JOB_SHORTLIST_IN_PROGRESS,
            WorkflowActionCode.JOB_SHORTLIST_SENT,
            WorkflowActionCode.JOB_INTERVIEWING,
            WorkflowActionCode.JOB_CLOSED,
            WorkflowActionCode.JOB_PAUSED,
            WorkflowActionCode.JOB_CANCELLED)));

    policies.addAll(transitions(WorkflowEntityType.CANDIDATE, RiskTier.T2_MEDIUM_RISK,
        List.of(
            WorkflowActionCode.CANDIDATE_PROFILE_PARSED,
            WorkflowActionCode.CANDIDATE_CONSULTANT_REVIEW_STARTED,
            WorkflowActionCode.CANDIDATE_MATCHED_TO_JOB,
            WorkflowActionCode.CANDIDATE_OUTREACH_STARTED,
            WorkflowActionCode.CANDIDATE_INTEREST_RECORDED)));
    policies.addAll(transitions(WorkflowEntityType.CANDIDATE, RiskTier.T3_HIGH_RISK,
        List.of(
            WorkflowActionCode.CANDIDATE_MARKED_AVAILABLE,
            WorkflowActionCode.CANDIDATE_CONSENT_PENDING,
            WorkflowActionCode.CANDIDATE_CONSENT_CONFIRMED,
            WorkflowActionCode.CANDIDATE_SHORTLISTED,
            WorkflowActionCode.CANDIDATE_CLIENT_REVIEW_STARTED,
            WorkflowActionCode.CANDIDATE_INTERVIEWING,
            WorkflowActionCode.CANDIDATE_OFFER_PENDING,
            WorkflowActionCode.CANDIDATE_REJECTED,
            WorkflowActionCode.CANDIDATE_ARCHIVED,
            WorkflowActionCode.CANDIDATE_DO_NOT_CONTACT_MARKED)));
    policies.addAll(transitions(WorkflowEntityType.CANDIDATE,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        List.of(
            WorkflowActionCode.CANDIDATE_IDENTITY_DISCLOSED,
            WorkflowActionCode.CANDIDATE_PLACED)));

    policies.add(transition(
        WorkflowActionCode.SHORTLIST_DRAFT_CREATED,
        RiskTier.T2_MEDIUM_RISK,
        "Shortlist draft was created.",
        WorkflowEntityType.SHORTLIST));
    policies.addAll(transitions(WorkflowEntityType.SHORTLIST, RiskTier.T3_HIGH_RISK,
        List.of(
            WorkflowActionCode.SHORTLIST_READY_FOR_REVIEW,
            WorkflowActionCode.SHORTLIST_SENT_TO_CLIENT,
            WorkflowActionCode.SHORTLIST_VIEWED_BY_CLIENT,
            WorkflowActionCode.SHORTLIST_CLIENT_FEEDBACK_PENDING,
            WorkflowActionCode.SHORTLIST_CANDIDATE_SELECTED,
            WorkflowActionCode.SHORTLIST_CLOSED)));
    policies.add(transition(
        WorkflowActionCode.SHORTLIST_CONTACT_UNLOCKED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        "Shortlist contact access was recorded after protection checks.",
        WorkflowEntityType.SHORTLIST));

    policies.addAll(transitions(WorkflowEntityType.CONSENT, RiskTier.T2_MEDIUM_RISK,
        List.of(
            WorkflowActionCode.CONSENT_REQUESTED,
            WorkflowActionCode.CONSENT_VIEWED_BY_CANDIDATE)));
    policies.addAll(transitions(WorkflowEntityType.CONSENT, RiskTier.T3_HIGH_RISK,
        List.of(
            WorkflowActionCode.CONSENT_CONFIRMED,
            WorkflowActionCode.CONSENT_DECLINED,
            WorkflowActionCode.CONSENT_EXPIRED,
            WorkflowActionCode.CONSENT_REVOKED)));

    policies.addAll(transitions(WorkflowEntityType.DISCLOSURE,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        List.of(
            WorkflowActionCode.DISCLOSURE_UNLOCK_REQUESTED,
            WorkflowActionCode.DISCLOSURE_CONSULTANT_APPROVED,
            WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED,
            WorkflowActionCode.DISCLOSURE_FEE_PROTECTION_ACTIVATED,
            WorkflowActionCode.PRIOR_CONTACT_CLAIM_CREATED,
            WorkflowActionCode.PRIOR_APPLICATION_CLAIM_CREATED)));

    policies.add(transition(
        WorkflowActionCode.SOURCE_ITEM_REGISTERED,
        RiskTier.T2_MEDIUM_RISK,
        "Source item intake was registered with preserved provenance.",
        WorkflowEntityType.SOURCE_ITEM));
    policies.add(transition(
        WorkflowActionCode.INFORMATION_PACKET_CREATED,
        RiskTier.T2_MEDIUM_RISK,
        "Information packet intake was created and linked to uploaded evidence.",
        WorkflowEntityType.INFORMATION_PACKET));

    policies.addAll(transitions(WorkflowEntityType.PLACEMENT,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        List.of(
            WorkflowActionCode.OFFER_ACCEPTED,
            WorkflowActionCode.CANDIDATE_ONBOARDED,
            WorkflowActionCode.INVOICE_READY,
            WorkflowActionCode.INVOICE_SENT,
            WorkflowActionCode.PAYMENT_MARKED_PAID,
            WorkflowActionCode.GUARANTEE_ACTIVATED,
            WorkflowActionCode.GUARANTEE_COMPLETED,
            WorkflowActionCode.REPLACEMENT_REQUIRED)));
    policies.addAll(transitions(WorkflowEntityType.COMMISSION,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        List.of(
            WorkflowActionCode.COMMISSION_PENDING,
            WorkflowActionCode.COMMISSION_PAID,
            WorkflowActionCode.COMMISSION_WITHHELD)));

    policies.add(auditOnly(
        WorkflowActionCode.AI_TASK_RUN_RECORDED,
        RiskTier.T1_LOW_RISK,
        false,
        false,
        "AI task run observability was recorded.",
        WorkflowEntityType.AI_TASK_RUN));
    policies.add(auditOnly(
        WorkflowActionCode.AI_RECOMMENDATION_RECORDED,
        RiskTier.T1_LOW_RISK,
        false,
        false,
        "AI recommendation was recorded as metadata, not fact.",
        WorkflowEntityType.AI_TASK_RUN));

    return new WorkflowActionRegistry(policies.values());
  }

  private static Set<WorkflowEntityType> canonicalWriteEntities() {
    return Set.of(
        WorkflowEntityType.CANONICAL_WRITE,
        WorkflowEntityType.CANDIDATE,
        WorkflowEntityType.JOB,
        WorkflowEntityType.SHORTLIST,
        WorkflowEntityType.PLACEMENT,
        WorkflowEntityType.COMMISSION);
  }

  private static List<WorkflowActionPolicy> transitions(
      WorkflowEntityType entityType,
      RiskTier riskTier,
      List<WorkflowActionCode> actionCodes) {
    return actionCodes.stream()
        .map(actionCode -> transition(
            actionCode,
            riskTier,
            actionCode.wireValue().toLowerCase().replace('_', ' ') + ".",
            entityType))
        .toList();
  }

  private static WorkflowActionPolicy transition(
      WorkflowActionCode actionCode,
      RiskTier riskTier,
      String description,
      WorkflowEntityType... entityTypes) {
    return transition(actionCode, riskTier, description, Set.of(entityTypes));
  }

  private static WorkflowActionPolicy transition(
      WorkflowActionCode actionCode,
      RiskTier riskTier,
      String description,
      Set<WorkflowEntityType> entityTypes) {
    boolean highRisk = riskTier.requiresHumanFinalActor();
    return new WorkflowActionPolicy(
        actionCode,
        entityTypes,
        riskTier,
        true,
        true,
        highRisk,
        highRisk,
        highRisk,
        true,
        description);
  }

  private static WorkflowActionPolicy auditOnly(
      WorkflowActionCode actionCode,
      RiskTier riskTier,
      boolean reasonRequired,
      boolean humanFinalActorRequired,
      String description,
      WorkflowEntityType... entityTypes) {
    return auditOnly(actionCode, riskTier, reasonRequired, humanFinalActorRequired, description,
        Set.of(entityTypes));
  }

  private static WorkflowActionPolicy auditOnly(
      WorkflowActionCode actionCode,
      RiskTier riskTier,
      boolean reasonRequired,
      boolean humanFinalActorRequired,
      String description,
      Set<WorkflowEntityType> entityTypes) {
    boolean highRisk = riskTier.requiresHumanFinalActor();
    boolean requiresHumanActor = humanFinalActorRequired || highRisk;
    return new WorkflowActionPolicy(
        actionCode,
        entityTypes,
        riskTier,
        false,
        false,
        reasonRequired || highRisk,
        requiresHumanActor,
        requiresHumanActor,
        false,
        description);
  }

  private static final class PolicyMap {
    private final EnumMap<WorkflowActionCode, WorkflowActionPolicy> values =
        new EnumMap<>(WorkflowActionCode.class);

    private void add(WorkflowActionPolicy policy) {
      WorkflowActionPolicy previous = values.putIfAbsent(policy.actionCode(), policy);
      if (previous != null) {
        throw new IllegalStateException(
            "duplicate policy for workflow action code: " + policy.actionCode().wireValue());
      }
    }

    private void addAll(List<WorkflowActionPolicy> policies) {
      policies.forEach(this::add);
    }

    private Map<WorkflowActionCode, WorkflowActionPolicy> values() {
      return values;
    }
  }
}
