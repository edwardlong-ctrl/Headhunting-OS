package com.recruitingtransactionos.coreapi.apiboundary.owner;

import com.recruitingtransactionos.coreapi.apiboundary.OwnerAccountingExportResponse;
import com.recruitingtransactionos.coreapi.apiboundary.OwnerRevenueSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.placementsupport.PlacementApiViewMapper;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.placement.PlacementOfferDetails;
import com.recruitingtransactionos.coreapi.commission.service.CommissionWorkflowService;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.placement.service.PlacementWorkflowService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class OwnerRevenueQueryService {

  private final PlacementWorkflowService placementWorkflowService;
  private final CommissionWorkflowService commissionWorkflowService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public OwnerRevenueQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionWorkflowService commissionWorkflowService) {
    this(placementWorkflowService, commissionWorkflowService, new PermissionEnforcer(new PermissionEvaluator()));
  }

  OwnerRevenueQueryService(
      PlacementWorkflowService placementWorkflowService,
      CommissionWorkflowService commissionWorkflowService,
      PermissionEnforcer permissionEnforcer) {
    this.placementWorkflowService = Objects.requireNonNull(placementWorkflowService, "placementWorkflowService must not be null");
    this.commissionWorkflowService = Objects.requireNonNull(commissionWorkflowService, "commissionWorkflowService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public OwnerRevenueSummaryResponse load(AccessRequest accessRequest, java.util.UUID organizationId) {
    requireRevenueRead(accessRequest);
    List<Placement> placements = sameOrganizationPlacements(
        placementWorkflowService.listPlacements(organizationId),
        organizationId);
    List<Commission> commissions = sameOrganizationCommissions(
        commissionWorkflowService.listCommissions(organizationId),
        organizationId);
    Map<PlacementId, List<Commission>> commissionsByPlacement = commissions.stream()
        .collect(Collectors.groupingBy(Commission::placementId));
    BigDecimal totalExpectedFee = placements.stream()
        .map(placement -> expectedFeeAmount(placement, commissionsByPlacement))
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    int unknownExpectedFeePlacementCount = (int) placements.stream()
        .filter(placement -> expectedFeeAmount(placement, commissionsByPlacement) == null)
        .count();
    List<Commission> paidCommissions = commissions.stream()
        .filter(commission -> commission.status() == CommissionStatus.PAID)
        .toList();
    BigDecimal totalPaidFee = paidCommissions.stream()
        .map(Commission::amount)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    int paidCommissionMissingAmountCount = (int) paidCommissions.stream()
        .filter(commission -> commission.amount() == null)
        .count();
    int activeGuaranteeCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.GUARANTEE_ACTIVE).count();
    int guaranteeCompletedCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.GUARANTEE_COMPLETED).count();
    int replacementRequiredCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.REPLACEMENT_REQUIRED).count();
    int invoiceReadyCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.INVOICE_READY).count();
    int invoiceSentCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.INVOICE_SENT).count();
    int paidPlacementCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.PAID).count();
    int invoiceInFlightCount = (int) placements.stream().filter(placement -> placement.status() == PlacementStatus.INVOICE_READY || placement.status() == PlacementStatus.INVOICE_SENT).count();
    int paidCommissionCount = paidCommissions.size();
    int pendingCommissionCount = (int) commissions.stream().filter(commission -> commission.status() == CommissionStatus.PENDING || commission.status() == CommissionStatus.CALCULATED).count();
    return new OwnerRevenueSummaryResponse(
        totalExpectedFee,
        totalPaidFee,
        placements.size(),
        unknownExpectedFeePlacementCount,
        pendingCommissionCount,
        paidCommissionCount,
        paidCommissionMissingAmountCount,
        activeGuaranteeCount,
        replacementRequiredCount,
        invoiceInFlightCount,
        invoiceReadyCount,
        invoiceSentCount,
        paidPlacementCount,
        guaranteeCompletedCount);
  }

  public OwnerAccountingExportResponse exportAccountingHandoff(
      AccessRequest accessRequest,
      java.util.UUID organizationId) {
    requireRevenueRead(accessRequest);
    List<Placement> placements = sameOrganizationPlacements(
        placementWorkflowService.listPlacements(organizationId),
        organizationId);
    List<Commission> commissions = sameOrganizationCommissions(
        commissionWorkflowService.listCommissions(organizationId),
        organizationId);
    Map<PlacementId, List<Commission>> commissionsByPlacement = commissions.stream()
        .collect(Collectors.groupingBy(Commission::placementId));
    StringBuilder csv = new StringBuilder();
    csv.append("placement_id,status,invoice_readiness,fee_agreement_active,fee_agreement_reference,")
        .append("payment_terms,expected_fee_amount,currency,commission_statuses,known_commission_amount,")
        .append("guarantee_expires_at,accounting_export_status\n");
    placements.stream()
        .filter(OwnerRevenueQueryService::isAccountingExportCandidate)
        .forEach(placement -> {
          PlacementOfferDetails offerDetails = PlacementOfferDetails.fromJson(placement.offerDetails());
          List<Commission> relatedCommissions = commissionsByPlacement.getOrDefault(placement.placementId(), List.of());
          csv.append(csv(placement.placementId().value().toString())).append(',')
              .append(csv(placement.status().wireValue())).append(',')
              .append(csv(PlacementApiViewMapper.invoiceReadiness(placement))).append(',')
              .append(csv(String.valueOf(offerDetails.hasActiveFeeAgreement()))).append(',')
              .append(csv(offerDetails.feeAgreementReference())).append(',')
              .append(csv(offerDetails.paymentTerms())).append(',')
              .append(csv(stringValue(expectedFeeAmount(placement, commissionsByPlacement)))).append(',')
              .append(csv(offerDetails.salaryCurrency())).append(',')
              .append(csv(String.join("|", PlacementApiViewMapper.commissionStatuses(relatedCommissions)))).append(',')
              .append(csv(stringValue(knownCommissionAmount(relatedCommissions)))).append(',')
              .append(csv(placement.guaranteeExpiresAt() == null ? null : placement.guaranteeExpiresAt().toString())).append(',')
              .append(csv(PlacementApiViewMapper.accountingExportStatus(placement, offerDetails)))
              .append('\n');
        });
    return new OwnerAccountingExportResponse(
        "csv",
        "read_only_accounting_handoff",
        "This export is an auditable operating handoff and does not replace the official accounting system.",
        java.time.Instant.now().toString(),
        csv.toString());
  }

  private static BigDecimal expectedFeeAmount(
      Placement placement,
      Map<PlacementId, List<Commission>> commissionsByPlacement) {
    return PlacementApiViewMapper.expectedFeeAmount(
        placement,
        commissionsByPlacement.getOrDefault(placement.placementId(), List.of()));
  }

  private static boolean isAccountingExportCandidate(Placement placement) {
    return switch (placement.status()) {
      case INVOICE_READY, INVOICE_SENT, PAID, GUARANTEE_ACTIVE, GUARANTEE_COMPLETED,
          REPLACEMENT_REQUIRED -> true;
      default -> false;
    };
  }

  private static BigDecimal knownCommissionAmount(List<Commission> commissions) {
    return commissions.stream()
        .map(Commission::amount)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static String stringValue(BigDecimal value) {
    return value == null ? "" : value.toPlainString();
  }

  private static String csv(String value) {
    if (value == null) {
      return "";
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private static List<Placement> sameOrganizationPlacements(
      List<Placement> placements,
      java.util.UUID organizationId) {
    return placements.stream()
        .filter(placement -> organizationId.equals(placement.organizationId()))
        .toList();
  }

  private static List<Commission> sameOrganizationCommissions(
      List<Commission> commissions,
      java.util.UUID organizationId) {
    return commissions.stream()
        .filter(commission -> organizationId.equals(commission.organizationId()))
        .toList();
  }

  private void requireRevenueRead(AccessRequest accessRequest) {
    permissionEnforcer.requireAllowed(accessRequest);
    if (accessRequest.resourceType() != ResourceType.REVENUE_REPORT
        || accessRequest.action() != AccessAction.READ
        || accessRequest.fieldClassification() != FieldClassification.INTERNAL) {
      throw new AccessDeniedException(new AccessDecision(false, "owner_revenue_read_context_required", "Owner revenue API requires a revenue read context."));
    }
  }
}
