package com.recruitingtransactionos.coreapi.placement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.commission.service.CommissionWorkflowService;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlacementWorkflowServiceTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-00000000a101");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-00000000a102");
  private static final PlacementId PLACEMENT_ID = new PlacementId(UUID.fromString("00000000-0000-0000-0000-00000000a103"));
  private static final LocalDate START_DATE = LocalDate.parse("2026-05-01");
  private static final LocalDate GUARANTEE_EXPIRES_AT = LocalDate.parse("2026-07-30");

  @Test
  void activateGuaranteePreservesPlacementBasedExpiryWindow() {
    PlacementService placementService = mock(PlacementService.class);
    Placement existing = Placement.builder()
        .placementId(PLACEMENT_ID)
        .organizationId(ORG_ID)
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-00000000a104")))
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000000a105")))
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000000a106")))
        .status(PlacementStatus.PAID)
        .offerDetails("{}")
        .startDate(START_DATE)
        .guaranteeDays(90)
        .guaranteeExpiresAt(GUARANTEE_EXPIRES_AT)
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-10T00:00:00Z"))
        .version(3)
        .build();
    when(placementService.findPlacementByIdAndOrganizationId(ORG_ID, PLACEMENT_ID))
        .thenReturn(Optional.of(existing));
    when(placementService.updatePlacement(any())).thenAnswer(invocation -> invocation.getArgument(0));

    PlacementWorkflowService service = new PlacementWorkflowService(
        placementService,
        mock(JobService.class),
        mock(CandidateService.class),
        mock(CompanyService.class),
        mock(CommissionWorkflowService.class),
        mock(WorkflowTransitionAuditService.class));

    Placement updated = service.activateGuarantee(ORG_ID, PLACEMENT_ID, ACTOR_ID, 3);

    assertThat(updated.status()).isEqualTo(PlacementStatus.GUARANTEE_ACTIVE);
    assertThat(updated.guaranteeExpiresAt()).isEqualTo(GUARANTEE_EXPIRES_AT);
  }
}
