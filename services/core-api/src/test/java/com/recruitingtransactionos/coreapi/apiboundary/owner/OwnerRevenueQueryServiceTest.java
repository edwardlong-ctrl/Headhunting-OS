package com.recruitingtransactionos.coreapi.apiboundary.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
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

class OwnerRevenueQueryServiceTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-00000000d101");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000d102");

  @Test
  void loadIncludesPlacementExpectedFeeBeforeCommissionExists() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerRevenueQueryService service = new OwnerRevenueQueryService(
        placementWorkflowService,
        commissionWorkflowService,
        new PermissionEnforcer(new PermissionEvaluator()));

    Placement preCommissionPlacement = placement(
        "00000000-0000-0000-0000-00000000d103",
        "100000.00",
        "20.0",
        PlacementStatus.OFFER_ACCEPTED);
    Placement commissionedPlacement = placement(
        "00000000-0000-0000-0000-00000000d104",
        "120000.00",
        "25.0",
        PlacementStatus.INVOICE_SENT);
    Commission commissionedPlacementFee = commission(
        "00000000-0000-0000-0000-00000000d105",
        commissionedPlacement.placementId(),
        "18000.00",
        CommissionStatus.PAID);

    when(placementWorkflowService.listPlacements(ORG_ID))
        .thenReturn(List.of(preCommissionPlacement, commissionedPlacement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(commissionedPlacementFee));

    var response = service.load(ownerRevenueReadRequest(), ORG_ID);

    assertThat(response.totalExpectedFee()).isEqualByComparingTo("38000.00");
    assertThat(response.totalPaidFee()).isEqualByComparingTo("18000.00");
    assertThat(response.placementCount()).isEqualTo(2);
    assertThat(response.unknownExpectedFeePlacementCount()).isEqualTo(0);
    assertThat(response.paidCommissionMissingAmountCount()).isEqualTo(0);
    assertThat(response.invoiceInFlightCount()).isEqualTo(1);
  }

  @Test
  void loadAggregatesAllCommissionAmountsForPlacement() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerRevenueQueryService service = new OwnerRevenueQueryService(
        placementWorkflowService,
        commissionWorkflowService,
        new PermissionEnforcer(new PermissionEvaluator()));

    Placement offerOnlyPlacement = placement(
        "00000000-0000-0000-0000-00000000d109",
        "100000.00",
        "20.0",
        PlacementStatus.OFFER_ACCEPTED);
    Placement splitCommissionPlacement = placement(
        "00000000-0000-0000-0000-00000000d110",
        "120000.00",
        "25.0",
        PlacementStatus.INVOICE_SENT);
    Commission splitA = commission(
        "00000000-0000-0000-0000-00000000d111",
        splitCommissionPlacement.placementId(),
        "18000.00",
        CommissionStatus.PAID);
    Commission splitB = commission(
        "00000000-0000-0000-0000-00000000d112",
        splitCommissionPlacement.placementId(),
        "4000.00",
        CommissionStatus.PENDING);

    when(placementWorkflowService.listPlacements(ORG_ID))
        .thenReturn(List.of(offerOnlyPlacement, splitCommissionPlacement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(splitA, splitB));

    var response = service.load(ownerRevenueReadRequest(), ORG_ID);

    assertThat(response.totalExpectedFee()).isEqualByComparingTo("42000.00");
    assertThat(response.totalPaidFee()).isEqualByComparingTo("18000.00");
    assertThat(response.unknownExpectedFeePlacementCount()).isEqualTo(0);
    assertThat(response.pendingCommissionCount()).isEqualTo(1);
    assertThat(response.paidCommissionCount()).isEqualTo(1);
    assertThat(response.paidCommissionMissingAmountCount()).isEqualTo(0);
  }

  @Test
  void loadFallsBackToPlacementExpectedFeeWhenAnyCommissionAmountIsMissing() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerRevenueQueryService service = new OwnerRevenueQueryService(
        placementWorkflowService,
        commissionWorkflowService,
        new PermissionEnforcer(new PermissionEvaluator()));

    Placement partiallyPricedPlacement = placement(
        "00000000-0000-0000-0000-00000000d113",
        "120000.00",
        "25.0",
        PlacementStatus.INVOICE_SENT);
    Commission pricedSplit = commission(
        "00000000-0000-0000-0000-00000000d114",
        partiallyPricedPlacement.placementId(),
        "18000.00",
        CommissionStatus.PAID);
    Commission unpricedSplit = commission(
        "00000000-0000-0000-0000-00000000d115",
        partiallyPricedPlacement.placementId(),
        null,
        CommissionStatus.PENDING);

    when(placementWorkflowService.listPlacements(ORG_ID))
        .thenReturn(List.of(partiallyPricedPlacement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(pricedSplit, unpricedSplit));

    var response = service.load(ownerRevenueReadRequest(), ORG_ID);

    assertThat(response.totalExpectedFee()).isEqualByComparingTo("30000.00");
    assertThat(response.totalPaidFee()).isEqualByComparingTo("18000.00");
    assertThat(response.unknownExpectedFeePlacementCount()).isEqualTo(0);
    assertThat(response.paidCommissionMissingAmountCount()).isEqualTo(0);
  }

  @Test
  void loadDoesNotCountPartialCommissionSubtotalWhenPlacementExpectedFeeIsUnknown() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerRevenueQueryService service = new OwnerRevenueQueryService(
        placementWorkflowService,
        commissionWorkflowService,
        new PermissionEnforcer(new PermissionEvaluator()));

    Placement unknownFeePlacement = placementWithUnknownFee(
        "00000000-0000-0000-0000-00000000d116",
        PlacementStatus.INVOICE_SENT);
    Commission pricedSplit = commission(
        "00000000-0000-0000-0000-00000000d117",
        unknownFeePlacement.placementId(),
        "18000.00",
        CommissionStatus.PAID);
    Commission unpricedSplit = commission(
        "00000000-0000-0000-0000-00000000d118",
        unknownFeePlacement.placementId(),
        null,
        CommissionStatus.PENDING);

    when(placementWorkflowService.listPlacements(ORG_ID))
        .thenReturn(List.of(unknownFeePlacement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(pricedSplit, unpricedSplit));

    var response = service.load(ownerRevenueReadRequest(), ORG_ID);

    assertThat(response.totalExpectedFee()).isEqualByComparingTo("0.00");
    assertThat(response.totalPaidFee()).isEqualByComparingTo("18000.00");
    assertThat(response.unknownExpectedFeePlacementCount()).isEqualTo(1);
    assertThat(response.paidCommissionMissingAmountCount()).isEqualTo(0);
  }

  @Test
  void loadExcludesPaidCommissionsWithoutAmountFromKnownPaidSubtotalAndCountsThem() {
    PlacementWorkflowService placementWorkflowService = mock(PlacementWorkflowService.class);
    CommissionWorkflowService commissionWorkflowService = mock(CommissionWorkflowService.class);
    OwnerRevenueQueryService service = new OwnerRevenueQueryService(
        placementWorkflowService,
        commissionWorkflowService,
        new PermissionEnforcer(new PermissionEvaluator()));

    Placement paidPlacement = placement(
        "00000000-0000-0000-0000-00000000d122",
        "120000.00",
        "25.0",
        PlacementStatus.INVOICE_SENT);
    Commission paidWithAmount = commission(
        "00000000-0000-0000-0000-00000000d123",
        paidPlacement.placementId(),
        "18000.00",
        CommissionStatus.PAID);
    Commission paidWithoutAmount = commission(
        "00000000-0000-0000-0000-00000000d124",
        paidPlacement.placementId(),
        null,
        CommissionStatus.PAID);

    when(placementWorkflowService.listPlacements(ORG_ID))
        .thenReturn(List.of(paidPlacement));
    when(commissionWorkflowService.listCommissions(ORG_ID))
        .thenReturn(List.of(paidWithAmount, paidWithoutAmount));

    var response = service.load(ownerRevenueReadRequest(), ORG_ID);

    assertThat(response.totalExpectedFee()).isEqualByComparingTo("30000.00");
    assertThat(response.totalPaidFee()).isEqualByComparingTo("18000.00");
    assertThat(response.paidCommissionCount()).isEqualTo(2);
    assertThat(response.paidCommissionMissingAmountCount()).isEqualTo(1);
  }

  private static AccessRequest ownerRevenueReadRequest() {
    return new AccessRequest(
        PortalRole.OWNER,
        ResourceType.REVENUE_REPORT,
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
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-00000000d106")))
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000000d107")))
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000000d108")))
        .status(status)
        .offerDetails(new PlacementOfferDetails(
            new BigDecimal(salaryAmount),
            "USD",
            new BigDecimal(feeRatePercentage),
            "offer").toJson())
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
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-00000000d119")))
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000000d120")))
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000000d121")))
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
