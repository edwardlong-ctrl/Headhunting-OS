package com.recruitingtransactionos.coreapi.apiboundary.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.apiboundary.PagedQuery;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.commission.service.CommissionWorkflowService;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementOfferDetails;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.placement.service.PlacementWorkflowService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OwnerPlacementQueryServiceTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-00000000e101");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000e102");

  @Test
  void listPlacementsAggregatesCommissionAmountsAndStatusesPerPlacement() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerPlacementQueryService service = new OwnerPlacementQueryService(
        placementWorkflowService,
        commissionWorkflowService);

    Placement placement = placement(
        "00000000-0000-0000-0000-00000000e103",
        "120000.00",
        "25.0",
        PlacementStatus.INVOICE_SENT);
    Commission paidCommission = commission(
        "00000000-0000-0000-0000-00000000e104",
        placement.placementId(),
        "18000.00",
        CommissionStatus.PAID);
    Commission pendingCommission = commission(
        "00000000-0000-0000-0000-00000000e105",
        placement.placementId(),
        "4000.00",
        CommissionStatus.PENDING);
    Commission calculatedCommission = commission(
        "00000000-0000-0000-0000-00000000e109",
        placement.placementId(),
        "2000.00",
        CommissionStatus.CALCULATED);

    when(placementWorkflowService.listPlacements(ORG_ID)).thenReturn(List.of(placement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(paidCommission, pendingCommission, calculatedCommission));

    var response = service.listPlacements(
        ownerPlacementReadRequest(),
        PagedQuery.builder(ORG_ID).limit(20).offset(0).build());

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().expectedFeeAmount()).isEqualByComparingTo("24000.00");
    assertThat(response.items().getFirst().feeAgreementActive()).isTrue();
    assertThat(response.items().getFirst().feeAgreementReference()).isEqualTo("MSA-2026-05");
    assertThat(response.items().getFirst().paymentTerms()).isEqualTo("net_30");
    assertThat(response.items().getFirst().invoiceReadiness()).isEqualTo("invoice_sent");
    assertThat(response.items().getFirst().accountingExportStatus()).isEqualTo("ready_for_accounting_export");
    assertThat(response.items().getFirst().commissionStatuses())
        .containsExactly("pending", "calculated", "paid");
  }

  @Test
  void listPlacementsFallsBackToPlacementExpectedFeeWhenAnyCommissionAmountIsMissing() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerPlacementQueryService service = new OwnerPlacementQueryService(
        placementWorkflowService,
        commissionWorkflowService);

    Placement placement = placement(
        "00000000-0000-0000-0000-00000000e110",
        "120000.00",
        "25.0",
        PlacementStatus.INVOICE_SENT);
    Commission pricedCommission = commission(
        "00000000-0000-0000-0000-00000000e111",
        placement.placementId(),
        "18000.00",
        CommissionStatus.PAID);
    Commission missingAmountCommission = commission(
        "00000000-0000-0000-0000-00000000e112",
        placement.placementId(),
        null,
        CommissionStatus.PENDING);

    when(placementWorkflowService.listPlacements(ORG_ID)).thenReturn(List.of(placement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(pricedCommission, missingAmountCommission));

    var response = service.listPlacements(
        ownerPlacementReadRequest(),
        PagedQuery.builder(ORG_ID).limit(20).offset(0).build());

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().expectedFeeAmount()).isEqualByComparingTo("30000.00");
    assertThat(response.items().getFirst().commissionStatuses())
        .containsExactly("pending", "paid");
  }

  @Test
  void listPlacementsKeepsUnknownExpectedFeeAsNullInsteadOfZero() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerPlacementQueryService service = new OwnerPlacementQueryService(
        placementWorkflowService,
        commissionWorkflowService);

    Placement placement = placementWithUnknownFee(
        "00000000-0000-0000-0000-00000000e113",
        PlacementStatus.OFFER_ACCEPTED);

    when(placementWorkflowService.listPlacements(ORG_ID)).thenReturn(List.of(placement));
    when(commissionWorkflowService.listCommissions(ORG_ID)).thenReturn(List.of());

    var response = service.listPlacements(
        ownerPlacementReadRequest(),
        PagedQuery.builder(ORG_ID).limit(20).offset(0).build());

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().expectedFeeAmount()).isNull();
    assertThat(response.items().getFirst().commissionStatuses()).isEmpty();
  }

  @Test
  void listPlacementsDoesNotUsePartialCommissionSubtotalWhenPlacementExpectedFeeIsUnknown() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerPlacementQueryService service = new OwnerPlacementQueryService(
        placementWorkflowService,
        commissionWorkflowService);

    Placement placement = placementWithUnknownFee(
        "00000000-0000-0000-0000-00000000e117",
        PlacementStatus.INVOICE_SENT);
    Commission pricedCommission = commission(
        "00000000-0000-0000-0000-00000000e118",
        placement.placementId(),
        "18000.00",
        CommissionStatus.PAID);
    Commission missingAmountCommission = commission(
        "00000000-0000-0000-0000-00000000e119",
        placement.placementId(),
        null,
        CommissionStatus.PENDING);

    when(placementWorkflowService.listPlacements(ORG_ID)).thenReturn(List.of(placement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(pricedCommission, missingAmountCommission));

    var response = service.listPlacements(
        ownerPlacementReadRequest(),
        PagedQuery.builder(ORG_ID).limit(20).offset(0).build());

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().expectedFeeAmount()).isNull();
    assertThat(response.items().getFirst().commissionStatuses())
        .containsExactly("pending", "paid");
  }

  private static AccessRequest ownerPlacementReadRequest() {
    return new AccessRequest(
        PortalRole.OWNER,
        ResourceType.PLACEMENT,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static Placement placement(
      String placementId,
      String salaryAmount,
      String feeRatePercentage,
      PlacementStatus status) {
    return Placement.builder()
        .placementId(new PlacementId(UUID.fromString(placementId)))
        .organizationId(ORG_ID)
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-00000000e106")))
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000000e107")))
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000000e108")))
        .status(status)
        .offerDetails(new PlacementOfferDetails(
            new BigDecimal(salaryAmount),
            "USD",
            new BigDecimal(feeRatePercentage),
            "offer",
            true,
            "MSA-2026-05",
            "net_30").toJson())
        .startDate(LocalDate.parse("2026-05-01"))
        .guaranteeDays(90)
        .guaranteeExpiresAt(LocalDate.parse("2026-07-30"))
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
  }

  private static Placement placementWithUnknownFee(
      String placementId,
      PlacementStatus status) {
    return Placement.builder()
        .placementId(new PlacementId(UUID.fromString(placementId)))
        .organizationId(ORG_ID)
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-00000000e114")))
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000000e115")))
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000000e116")))
        .status(status)
        .offerDetails(new PlacementOfferDetails(
            null,
            "USD",
            null,
            "offer").toJson())
        .startDate(LocalDate.parse("2026-05-01"))
        .guaranteeDays(90)
        .guaranteeExpiresAt(LocalDate.parse("2026-07-30"))
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
  }

  private static Commission commission(
      String commissionId,
      PlacementId placementId,
      String amount,
      CommissionStatus status) {
    var builder = Commission.builder()
        .commissionId(new CommissionId(UUID.fromString(commissionId)))
        .organizationId(ORG_ID)
        .placementId(placementId)
        .consultantId(CONSULTANT_ID)
        .status(status)
        .commissionType(CommissionType.FULL_FEE)
        .currency("USD")
        .calculationDetails("{}")
        .metadata("{}")
        .createdAt(Instant.parse("2026-05-02T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-02T00:00:00Z"))
        .version(1);
    if (amount != null) {
      builder.amount(new BigDecimal(amount));
    }
    return builder.build();
  }
}
