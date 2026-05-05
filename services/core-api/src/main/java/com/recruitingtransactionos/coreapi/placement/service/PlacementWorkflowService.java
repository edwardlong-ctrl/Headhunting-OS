package com.recruitingtransactionos.coreapi.placement.service;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.commission.service.CommissionWorkflowService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementOfferDetails;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowAiInvolvement;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowEntityType;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public final class PlacementWorkflowService {

  private final PlacementService placementService;
  private final JobService jobService;
  private final CandidateService candidateService;
  private final CompanyService companyService;
  private final CommissionWorkflowService commissionWorkflowService;
  private final WorkflowTransitionAuditService workflowTransitionAuditService;

  public PlacementWorkflowService(
      PlacementService placementService,
      JobService jobService,
      CandidateService candidateService,
      CompanyService companyService,
      CommissionWorkflowService commissionWorkflowService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    this.placementService = placementService;
    this.jobService = jobService;
    this.candidateService = candidateService;
    this.companyService = companyService;
    this.commissionWorkflowService = commissionWorkflowService;
    this.workflowTransitionAuditService = workflowTransitionAuditService;
  }

  public List<Placement> listPlacements(UUID organizationId) {
    return placementService.findAllPlacementsByOrganizationId(organizationId);
  }

  public Placement getPlacement(UUID organizationId, PlacementId placementId) {
    return placementService.findPlacementByIdAndOrganizationId(organizationId, placementId)
        .orElseThrow(() -> new IllegalArgumentException("placement_not_found"));
  }

  @Transactional
  public Placement createPlacement(PlacementCreateCommand command) {
    Job job = jobService.findJobByIdAndOrganizationId(command.organizationId(), command.jobId())
        .orElseThrow(() -> new IllegalArgumentException("job_not_found"));
    Candidate candidate = candidateService.findCandidateByIdAndOrganizationId(
        command.organizationId(), command.candidateId())
        .orElseThrow(() -> new IllegalArgumentException("candidate_not_found"));
    Company company = companyService.findCompanyByIdAndOrganizationId(
        command.organizationId(), command.companyId())
        .orElseThrow(() -> new IllegalArgumentException("company_not_found"));
    if (!job.companyId().equals(company.companyId())) {
      throw new IllegalArgumentException("placement_company_must_match_job_company");
    }
    Instant now = Instant.now();
    Placement created = placementService.createPlacement(
        Placement.builder()
            .placementId(new PlacementId(UUID.randomUUID()))
            .organizationId(command.organizationId())
            .jobId(job.jobId())
            .candidateId(candidate.candidateId())
            .companyId(company.companyId())
            .status(PlacementStatus.OFFER_PENDING)
            .offerDetails(new PlacementOfferDetails(
                command.salaryAmount(),
                command.salaryCurrency(),
                command.feeRatePercentage(),
                command.notes()).toJson())
            .startDate(command.startDate())
            .guaranteeDays(command.guaranteeDays())
            .guaranteeExpiresAt(guaranteeExpiry(command.startDate(), command.guaranteeDays()))
            .metadata("{}")
            .createdAt(now)
            .updatedAt(now)
            .build());
    auditTransition(
        created,
        "absent",
        created.status().wireValue(),
        WorkflowActionCode.PLACEMENT_RECORDED,
        command.actorId(),
        "placement recorded for candidate " + command.candidateId().value());
    return created;
  }

  @Transactional
  public Placement markOfferAccepted(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    Placement existing = getPlacement(organizationId, placementId);
    requireVersion(existing.version(), expectedVersion);
    if (existing.status() == PlacementStatus.OFFER_ACCEPTED) {
      return existing;
    }
    Instant now = Instant.now();
    Placement updated = placementService.updatePlacement(copyPlacement(
        existing,
        PlacementStatus.OFFER_ACCEPTED,
        now,
        existing.startDate(),
        existing.guaranteeExpiresAt(),
        null,
        null,
        now));
    auditTransition(
        updated,
        existing.status().wireValue(),
        updated.status().wireValue(),
        WorkflowActionCode.OFFER_ACCEPTED,
        actorId,
        "offer accepted");
    return updated;
  }

  @Transactional
  public Placement markOnboarded(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    Placement existing = getPlacement(organizationId, placementId);
    requireVersion(existing.version(), expectedVersion);
    if (existing.status() == PlacementStatus.ONBOARDED) {
      return existing;
    }
    Instant now = Instant.now();
    Placement updated = placementService.updatePlacement(copyPlacement(
        existing,
        PlacementStatus.ONBOARDED,
        existing.offerAcceptedAt(),
        existing.startDate(),
        existing.guaranteeExpiresAt(),
        now,
        null,
        now));
    auditTransition(
        updated,
        existing.status().wireValue(),
        updated.status().wireValue(),
        WorkflowActionCode.CANDIDATE_ONBOARDED,
        actorId,
        "candidate onboarded");
    return updated;
  }

  @Transactional
  public Placement markInvoiceReady(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    Placement existing = getPlacement(organizationId, placementId);
    requireVersion(existing.version(), expectedVersion);
    if (existing.status() == PlacementStatus.INVOICE_READY) {
      return existing;
    }
    Instant now = Instant.now();
    Placement updated = placementService.updatePlacement(copyPlacement(
        existing,
        PlacementStatus.INVOICE_READY,
        existing.offerAcceptedAt(),
        existing.startDate(),
        existing.guaranteeExpiresAt(),
        existing.onboardedAt(),
        null,
        now));
    auditTransition(
        updated,
        existing.status().wireValue(),
        updated.status().wireValue(),
        WorkflowActionCode.INVOICE_READY,
        actorId,
        "invoice ready");
    commissionWorkflowService.ensurePendingCommissionForPlacement(updated, actorId);
    return updated;
  }

  @Transactional
  public Placement markInvoiceSent(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    return transition(
        organizationId,
        placementId,
        actorId,
        expectedVersion,
        PlacementStatus.INVOICE_SENT,
        WorkflowActionCode.INVOICE_SENT,
        "invoice sent");
  }

  @Transactional
  public Placement markPaymentPaid(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    return transition(
        organizationId,
        placementId,
        actorId,
        expectedVersion,
        PlacementStatus.PAID,
        WorkflowActionCode.PAYMENT_MARKED_PAID,
        "payment marked paid");
  }

  @Transactional
  public Placement activateGuarantee(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    Placement existing = getPlacement(organizationId, placementId);
    requireVersion(existing.version(), expectedVersion);
    if (existing.status() == PlacementStatus.GUARANTEE_ACTIVE) {
      return existing;
    }
    LocalDate guaranteeExpiresAt = existing.guaranteeExpiresAt() != null
        ? existing.guaranteeExpiresAt()
        : guaranteeExpiry(existing.startDate(), existing.guaranteeDays());
    Instant now = Instant.now();
    Placement updated = placementService.updatePlacement(copyPlacement(
        existing,
        PlacementStatus.GUARANTEE_ACTIVE,
        existing.offerAcceptedAt(),
        existing.startDate(),
        guaranteeExpiresAt,
        existing.onboardedAt(),
        null,
        now));
    auditTransition(
        updated,
        existing.status().wireValue(),
        updated.status().wireValue(),
        WorkflowActionCode.GUARANTEE_ACTIVATED,
        actorId,
        "guarantee activated");
    return updated;
  }

  @Transactional
  public Placement completeGuarantee(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    return transition(
        organizationId,
        placementId,
        actorId,
        expectedVersion,
        PlacementStatus.GUARANTEE_COMPLETED,
        WorkflowActionCode.GUARANTEE_COMPLETED,
        "guarantee completed");
  }

  @Transactional
  public Placement markReplacementRequired(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion) {
    return transition(
        organizationId,
        placementId,
        actorId,
        expectedVersion,
        PlacementStatus.REPLACEMENT_REQUIRED,
        WorkflowActionCode.REPLACEMENT_REQUIRED,
        "replacement required");
  }

  private Placement transition(
      UUID organizationId,
      PlacementId placementId,
      UUID actorId,
      int expectedVersion,
      PlacementStatus targetStatus,
      WorkflowActionCode actionCode,
      String reason) {
    Placement existing = getPlacement(organizationId, placementId);
    requireVersion(existing.version(), expectedVersion);
    if (existing.status() == targetStatus) {
      return existing;
    }
    Instant now = Instant.now();
    Placement updated = placementService.updatePlacement(copyPlacement(
        existing,
        targetStatus,
        existing.offerAcceptedAt(),
        existing.startDate(),
        existing.guaranteeExpiresAt(),
        existing.onboardedAt(),
        null,
        now));
    auditTransition(
        updated,
        existing.status().wireValue(),
        updated.status().wireValue(),
        actionCode,
        actorId,
        reason);
    return updated;
  }

  private Placement copyPlacement(
      Placement existing,
      PlacementStatus status,
      Instant offerAcceptedAt,
      LocalDate startDate,
      LocalDate guaranteeExpiresAt,
      Instant onboardedAt,
      String cancelReason,
      Instant updatedAt) {
    return Placement.builder()
        .placementId(existing.placementId())
        .organizationId(existing.organizationId())
        .jobId(existing.jobId())
        .candidateId(existing.candidateId())
        .companyId(existing.companyId())
        .status(status)
        .offerDetails(existing.offerDetails())
        .offerAcceptedAt(offerAcceptedAt)
        .startDate(startDate)
        .onboardedAt(onboardedAt)
        .guaranteeDays(existing.guaranteeDays())
        .guaranteeExpiresAt(guaranteeExpiresAt)
        .cancelledAt(existing.cancelledAt())
        .cancelReason(cancelReason != null ? cancelReason : existing.cancelReason())
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(updatedAt)
        .version(existing.version())
        .build();
  }

  private void auditTransition(
      Placement placement,
      String beforeStatus,
      String afterStatus,
      WorkflowActionCode actionCode,
      UUID actorId,
      String reason) {
    workflowTransitionAuditService.record(WorkflowTransitionAuditRequest.builder()
        .organizationId(placement.organizationId())
        .entityNamespace("recruiting")
        .entityType(WorkflowEntityType.PLACEMENT.wireValue())
        .entityId(placement.placementId().value())
        .entityVersion(placement.version())
        .actionCode(actionCode.wireValue())
        .actorType(ActorRole.CONSULTANT)
        .actorId(actorId)
        .aiInvolvement(WorkflowAiInvolvement.NONE)
        .beforeState(snapshot(beforeStatus))
        .afterState(snapshot(afterStatus))
        .reason(reason)
        .sourceType("placement_workflow")
        .sourceRefId(placement.placementId().value())
        .occurredAt(Instant.now())
        .build());
  }

  private static WorkflowStateSnapshot snapshot(String status) {
    return new WorkflowStateSnapshot("{\"status\":\"" + status + "\"}");
  }

  private static LocalDate guaranteeExpiry(LocalDate startDate, Integer guaranteeDays) {
    if (startDate == null || guaranteeDays == null) {
      return null;
    }
    return startDate.plusDays(guaranteeDays.longValue());
  }

  private static void requireVersion(int actualVersion, int expectedVersion) {
    if (actualVersion != expectedVersion) {
      throw new IllegalArgumentException("placement_version_conflict");
    }
  }
}
