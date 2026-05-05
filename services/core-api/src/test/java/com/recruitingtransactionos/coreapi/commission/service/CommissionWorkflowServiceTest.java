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
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.service.PlacementService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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
