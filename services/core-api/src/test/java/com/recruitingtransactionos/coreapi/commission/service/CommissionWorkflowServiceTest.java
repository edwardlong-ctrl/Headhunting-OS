package com.recruitingtransactionos.coreapi.commission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionCalculationDetails;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.placement.service.PlacementService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CommissionWorkflowServiceTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-00000000c101");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-00000000c102");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000c103");
  private static final PlacementId PLACEMENT_ID = new PlacementId(UUID.fromString("00000000-0000-0000-0000-00000000c104"));

  @Test
  void createCommissionRejectsPlacementOutsideOrganizationScope() {
    CommissionService commissionService = mock(CommissionService.class);
    PlacementService placementService = mock(PlacementService.class);
    JobService jobService = mock(JobService.class);
    WorkflowTransitionAuditService workflowTransitionAuditService = mock(WorkflowTransitionAuditService.class);
    CommissionWorkflowService service = new CommissionWorkflowService(
        commissionService,
        placementService,
        jobService,
        workflowTransitionAuditService);

    when(placementService.findPlacementByIdAndOrganizationId(ORG_ID, PLACEMENT_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createCommission(new CommissionCreateCommand(
        ORG_ID,
        PLACEMENT_ID,
        CONSULTANT_ID,
        ACTOR_ID,
        CommissionType.FULL_FEE,
        new BigDecimal("24000.00"),
        "USD",
        null,
        new BigDecimal("120000.00"),
        new BigDecimal("20.0"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("placement_not_found_for_commission");

    verifyNoInteractions(commissionService, workflowTransitionAuditService, jobService);
  }

  @Test
  void markPaidRejectsCommissionWithoutAmount() {
    CommissionService commissionService = mock(CommissionService.class);
    PlacementService placementService = mock(PlacementService.class);
    JobService jobService = mock(JobService.class);
    WorkflowTransitionAuditService workflowTransitionAuditService = mock(WorkflowTransitionAuditService.class);
    CommissionWorkflowService service = new CommissionWorkflowService(
        commissionService,
        placementService,
        jobService,
        workflowTransitionAuditService);

    Commission existing = commission(
        "00000000-0000-0000-0000-00000000c105",
        null,
        CommissionStatus.PENDING);
    when(commissionService.findCommissionByIdAndOrganizationId(ORG_ID, existing.commissionId()))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.markPaid(
        ORG_ID,
        existing.commissionId(),
        ACTOR_ID,
        existing.version()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("commission_amount_required_for_paid_status");

    verify(commissionService, never()).updateCommission(any());
    verifyNoInteractions(workflowTransitionAuditService, placementService, jobService);
    assertThat(existing.status()).isEqualTo(CommissionStatus.PENDING);
  }

  @Test
  void ensurePendingCommissionCarriesCommercialCalculationInputsFromPlacementFeeAgreement() {
    CommissionService commissionService = mock(CommissionService.class);
    PlacementService placementService = mock(PlacementService.class);
    JobService jobService = mock(JobService.class);
    WorkflowTransitionAuditService workflowTransitionAuditService = mock(WorkflowTransitionAuditService.class);
    CommissionWorkflowService service = new CommissionWorkflowService(
        commissionService,
        placementService,
        jobService,
        workflowTransitionAuditService);
    Placement placement = Placement.builder()
        .placementId(PLACEMENT_ID)
        .organizationId(ORG_ID)
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-00000000c106")))
        .candidateId(new com.recruitingtransactionos.coreapi.candidateprofile.CandidateId(UUID.fromString("00000000-0000-0000-0000-00000000c107")))
        .companyId(new com.recruitingtransactionos.coreapi.company.CompanyId(UUID.fromString("00000000-0000-0000-0000-00000000c108")))
        .status(PlacementStatus.INVOICE_READY)
        .offerDetails("""
            {
              "salaryAmount":120000.00,
              "salaryCurrency":"USD",
              "feeRatePercentage":25.0,
              "notes":"offer",
              "feeAgreementActive":true,
              "feeAgreementReference":"MSA-2026-05",
              "paymentTerms":"net_30"
            }
            """)
        .startDate(LocalDate.parse("2026-05-01"))
        .guaranteeDays(90)
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-02T00:00:00Z"))
        .version(1)
        .build();
    Job job = Job.builder()
        .jobId(placement.jobId())
        .organizationId(ORG_ID)
        .companyId(placement.companyId())
        .title("ASIC Verification Lead")
        .status(JobStatus.ACTIVATED)
        .ownerConsultantId(CONSULTANT_ID)
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .build();
    when(commissionService.findCommissionsByPlacementIdAndOrganizationId(ORG_ID, PLACEMENT_ID))
        .thenReturn(java.util.List.of());
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, placement.jobId()))
        .thenReturn(Optional.of(job));
    when(placementService.findPlacementByIdAndOrganizationId(ORG_ID, PLACEMENT_ID))
        .thenReturn(Optional.of(placement));
    when(commissionService.createCommission(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.ensurePendingCommissionForPlacement(placement, ACTOR_ID);

    ArgumentCaptor<Commission> captor = ArgumentCaptor.forClass(Commission.class);
    verify(commissionService).createCommission(captor.capture());
    Commission created = captor.getValue();
    CommissionCalculationDetails details = CommissionCalculationDetails.fromJson(created.calculationDetails());
    assertThat(created.amount()).isEqualByComparingTo("30000.00");
    assertThat(details.salaryAmount()).isEqualByComparingTo("120000.00");
    assertThat(details.feeRatePercentage()).isEqualByComparingTo("25.0");
    assertThat(details.expectedFeeAmount()).isEqualByComparingTo("30000.00");
    assertThat(details.feeAgreementReference()).isEqualTo("MSA-2026-05");
    assertThat(details.paymentTerms()).isEqualTo("net_30");
    assertThat(details.calculationSource()).isEqualTo("placement_fee_agreement_snapshot");
  }

  private static Commission commission(
      String commissionId,
      String amount,
      CommissionStatus status) {
    var builder = Commission.builder()
        .commissionId(new CommissionId(UUID.fromString(commissionId)))
        .organizationId(ORG_ID)
        .placementId(PLACEMENT_ID)
        .consultantId(CONSULTANT_ID)
        .status(status)
        .commissionType(CommissionType.FULL_FEE)
        .currency("USD")
        .calculationDetails("{}")
        .metadata("{}")
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1);
    if (amount != null) {
      builder.amount(new BigDecimal(amount));
    }
    return builder.build();
  }
}
