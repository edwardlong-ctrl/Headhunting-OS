package com.recruitingtransactionos.coreapi.apiboundary.placementsupport;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantPlacementSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.OwnerPlacementSummaryResponse;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementOfferDetails;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class PlacementApiViewMapper {

  private PlacementApiViewMapper() {}

  public static ConsultantPlacementSummaryResponse toConsultantSummary(
      Placement placement,
      List<Commission> commissions) {
    PlacementOfferDetails offerDetails = PlacementOfferDetails.fromJson(placement.offerDetails());
    return new ConsultantPlacementSummaryResponse(
        placement.placementId().value().toString(),
        placement.version(),
        placement.jobId().value().toString(),
        placement.candidateId().value().toString(),
        placement.companyId().value().toString(),
        placement.status().wireValue(),
        offerDetails.salaryAmount(),
        offerDetails.salaryCurrency(),
        offerDetails.feeRatePercentage(),
        expectedFeeAmount(placement, commissions),
        offerDetails.hasActiveFeeAgreement(),
        offerDetails.feeAgreementReference(),
        offerDetails.paymentTerms(),
        invoiceReadiness(placement),
        placement.startDate(),
        placement.guaranteeDays(),
        placement.guaranteeExpiresAt(),
        placement.offerAcceptedAt(),
        placement.onboardedAt(),
        placement.createdAt(),
        placement.updatedAt(),
        offerDetails.notes());
  }

  public static OwnerPlacementSummaryResponse toOwnerSummary(
      Placement placement,
      List<Commission> commissions) {
    PlacementOfferDetails offerDetails = PlacementOfferDetails.fromJson(placement.offerDetails());
    return new OwnerPlacementSummaryResponse(
        placement.placementId().value().toString(),
        placement.jobId().value().toString(),
        placement.candidateId().value().toString(),
        placement.companyId().value().toString(),
        placement.status().wireValue(),
        offerDetails.salaryAmount(),
        offerDetails.salaryCurrency(),
        offerDetails.feeRatePercentage(),
        expectedFeeAmount(placement, commissions),
        offerDetails.hasActiveFeeAgreement(),
        offerDetails.feeAgreementReference(),
        offerDetails.paymentTerms(),
        invoiceReadiness(placement),
        accountingExportStatus(placement, offerDetails),
        commissionStatuses(commissions),
        placement.startDate(),
        placement.guaranteeDays(),
        placement.guaranteeExpiresAt(),
        placement.createdAt(),
        placement.updatedAt());
  }

  public static BigDecimal expectedFeeAmount(Placement placement, List<Commission> commissions) {
    PlacementOfferDetails offerDetails = PlacementOfferDetails.fromJson(placement.offerDetails());
    List<Commission> relatedCommissions = commissions != null ? commissions : List.of();
    BigDecimal placementExpectedFee = offerDetails.expectedFeeAmount();
    boolean hasMissingCommissionAmount = relatedCommissions.stream()
        .map(Commission::amount)
        .anyMatch(Objects::isNull);
    boolean hasExplicitCommissionAmount = relatedCommissions.stream()
        .map(Commission::amount)
        .anyMatch(Objects::nonNull);
    if (hasMissingCommissionAmount) {
      return placementExpectedFee;
    }
    if (hasExplicitCommissionAmount) {
      return relatedCommissions.stream()
          .map(Commission::amount)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    return placementExpectedFee;
  }

  public static List<String> commissionStatuses(List<Commission> commissions) {
    List<Commission> relatedCommissions = commissions != null ? commissions : List.of();
    if (relatedCommissions.isEmpty()) {
      return List.of();
    }
    return Arrays.stream(CommissionStatus.values())
        .filter(status -> relatedCommissions.stream().anyMatch(commission -> commission.status() == status))
        .map(CommissionStatus::wireValue)
        .toList();
  }

  public static String invoiceReadiness(Placement placement) {
    return switch (placement.status()) {
      case INVOICE_READY -> "invoice_ready";
      case INVOICE_SENT -> "invoice_sent";
      case PAID, GUARANTEE_ACTIVE, GUARANTEE_COMPLETED, REPLACEMENT_REQUIRED -> "invoice_paid";
      default -> "not_ready";
    };
  }

  public static String accountingExportStatus(
      Placement placement,
      PlacementOfferDetails offerDetails) {
    if (!offerDetails.hasConfirmedFeeAgreement()) {
      return "blocked_fee_agreement_required";
    }
    return switch (placement.status()) {
      case INVOICE_READY, INVOICE_SENT, PAID, GUARANTEE_ACTIVE, GUARANTEE_COMPLETED,
          REPLACEMENT_REQUIRED -> "ready_for_accounting_export";
      default -> "not_ready_for_accounting_export";
    };
  }
}
