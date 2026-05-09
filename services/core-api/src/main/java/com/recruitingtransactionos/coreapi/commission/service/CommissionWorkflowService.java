package com.recruitingtransactionos.coreapi.commission.service;

import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionCalculationDetails;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementOfferDetails;
import com.recruitingtransactionos.coreapi.placement.service.PlacementService;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class CommissionWorkflowService {

  private final CommissionService commissionService;
  private final PlacementService placementService;
  private final JobService jobService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;

  public CommissionWorkflowService(
      CommissionService commissionService,
      PlacementService placementService,
      JobService jobService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this.commissionService = commissionService;
    this.placementService = placementService;
    this.jobService = jobService;
    this.workflowTransitionAuditService = workflowTransitionAuditService;
  }

  public List<Commission> listCommissions(UUID organizationId) {
    return commissionService.findAllCommissionsByOrganizationId(organizationId);
  }

  public Commission getCommission(UUID organizationId, CommissionId commissionId) {
    return commissionService.findCommissionByIdAndOrganizationId(organizationId, commissionId)
        .orElseThrow(() -> new IllegalArgumentException("commission_not_found"));
  }

  @Transactional
  public Commission createCommission(CommissionCreateCommand command) {
    placementService.findPlacementByIdAndOrganizationId(command.organizationId(), command.placementId())
        .orElseThrow(() -> new IllegalArgumentException("placement_not_found_for_commission"));
    Instant now = Instant.now();
    Commission created = commissionService.createCommission(
        Commission.builder()
            .commissionId(new CommissionId(UUID.randomUUID()))
            .organizationId(command.organizationId())
            .placementId(command.placementId())
            .consultantId(command.consultantId())
            .status(CommissionStatus.PENDING)
            .commissionType(command.commissionType())
            .amount(command.amount())
            .currency(command.currency())
            .splitPercentage(command.splitPercentage())
            .calculationDetails(new CommissionCalculationDetails(
                command.salaryAmount(),
                command.feeRatePercentage(),
                command.amount()).toJson())
            .metadata("{}")
            .createdAt(now)
            .updatedAt(now)
            .build());
    auditTransition(
        created,
        "absent",
        created.status().wireValue(),
        WorkflowActionCode.COMMISSION_PENDING,
        command.actorId(),
        "commission created for placement " + command.placementId().value());
    return created;
  }

  @Transactional
  public Commission ensurePendingCommissionForPlacement(Placement placement, UUID actorId) {
    List<Commission> commissions = commissionService.findCommissionsByPlacementIdAndOrganizationId(
        placement.organizationId(), placement.placementId());
    if (!commissions.isEmpty()) {
      return commissions.stream()
          .max(Comparator.comparing(Commission::createdAt))
          .orElseThrow();
    }
    Job job = jobService.findJobByIdAndOrganizationId(placement.organizationId(), placement.jobId())
        .orElseThrow(() -> new IllegalArgumentException("job_not_found_for_placement"));
    PlacementOfferDetails offerDetails = PlacementOfferDetails.fromJson(placement.offerDetails());
    UUID consultantId = job.ownerConsultantId() != null ? job.ownerConsultantId() : actorId;
    return createCommission(new CommissionCreateCommand(
        placement.organizationId(),
        placement.placementId(),
        consultantId,
        actorId,
        CommissionType.FULL_FEE,
        offerDetails.expectedFeeAmount(),
        offerDetails.salaryCurrency(),
        null,
        offerDetails.salaryAmount(),
        offerDetails.feeRatePercentage()));
  }

  @Transactional
  public Commission markPaid(
      UUID organizationId,
      CommissionId commissionId,
      UUID actorId,
      int expectedVersion) {
    Commission existing = getCommission(organizationId, commissionId);
    requireVersion(existing.version(), expectedVersion);
    if (existing.status() == CommissionStatus.PAID) {
      return existing;
    }
    if (existing.amount() == null) {
      throw new IllegalArgumentException("commission_amount_required_for_paid_status");
    }
    Instant now = Instant.now();
    Commission updated = commissionService.updateCommission(copyCommission(
        existing,
        CommissionStatus.PAID,
        now,
        now,
        existing.withheldReason()));
    auditTransition(
        updated,
        existing.status().wireValue(),
        updated.status().wireValue(),
        WorkflowActionCode.COMMISSION_PAID,
        actorId,
        "commission marked paid");
    return updated;
  }

  @Transactional
  public Commission withhold(
      UUID organizationId,
      CommissionId commissionId,
      UUID actorId,
      int expectedVersion,
      String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("withheld_reason_required");
    }
    Commission existing = getCommission(organizationId, commissionId);
    requireVersion(existing.version(), expectedVersion);
    if (existing.status() == CommissionStatus.WITHHELD && reason.equals(existing.withheldReason())) {
      return existing;
    }
    Instant now = Instant.now();
    Commission updated = commissionService.updateCommission(copyCommission(
        existing,
        CommissionStatus.WITHHELD,
        null,
        now,
        reason.strip()));
    auditTransition(
        updated,
        existing.status().wireValue(),
        updated.status().wireValue(),
        WorkflowActionCode.COMMISSION_WITHHELD,
        actorId,
        "commission withheld");
    return updated;
  }

  private Commission copyCommission(
      Commission existing,
      CommissionStatus status,
      Instant paidAt,
      Instant updatedAt,
      String withheldReason) {
    return Commission.builder()
        .commissionId(existing.commissionId())
        .organizationId(existing.organizationId())
        .placementId(existing.placementId())
        .consultantId(existing.consultantId())
        .status(status)
        .commissionType(existing.commissionType())
        .amount(existing.amount())
        .currency(existing.currency())
        .splitPercentage(existing.splitPercentage())
        .calculationDetails(existing.calculationDetails())
        .paidAt(paidAt)
        .withheldReason(withheldReason)
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(updatedAt)
        .version(existing.version())
        .build();
  }

  private void auditTransition(
      Commission commission,
      String beforeStatus,
      String afterStatus,
      WorkflowActionCode actionCode,
      UUID actorId,
      String reason) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(commission.organizationId())
        .entityNamespace("recruiting")
        .entityType(WorkflowEntityType.COMMISSION.wireValue())
        .entityId(commission.commissionId().value())
        .entityVersion(commission.version())
        .actionCode(actionCode.wireValue())
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(snapshot(beforeStatus))
        .afterState(snapshot(afterStatus))
        .reason(reason)
        .sourceType("commission_workflow")
        .sourceRefId(commission.commissionId().value())
        .occurredAt(Instant.now())
        .build());
  }

  private WorkflowStateSnapshot snapshot(String status) {
    return new WorkflowStateSnapshot("{\"status\":\"" + status + "\"}");
  }

  private void requireVersion(int actualVersion, int expectedVersion) {
    if (actualVersion != expectedVersion) {
      throw new IllegalArgumentException("commission_version_conflict");
    }
  }
}
