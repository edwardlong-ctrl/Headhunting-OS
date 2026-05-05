package com.recruitingtransactionos.coreapi.apiboundary.placementsupport;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCommissionSummaryResponse;
import com.recruitingtransactionos.coreapi.apiboundary.OwnerCommissionSummaryResponse;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionCalculationDetails;

public final class CommissionApiViewMapper {

  private CommissionApiViewMapper() {}

  public static ConsultantCommissionSummaryResponse toConsultantSummary(Commission commission) {
    CommissionCalculationDetails details = CommissionCalculationDetails.fromJson(commission.calculationDetails());
    return new ConsultantCommissionSummaryResponse(
        commission.commissionId().value().toString(),
        commission.version(),
        commission.placementId().value().toString(),
        commission.consultantId().toString(),
        commission.status().wireValue(),
        commission.commissionType().wireValue(),
        commission.amount(),
        commission.currency(),
        commission.splitPercentage(),
        details.salaryAmount(),
        details.feeRatePercentage(),
        commission.paidAt(),
        commission.withheldReason(),
        commission.createdAt(),
        commission.updatedAt());
  }

  public static OwnerCommissionSummaryResponse toOwnerSummary(Commission commission) {
    CommissionCalculationDetails details = CommissionCalculationDetails.fromJson(commission.calculationDetails());
    return new OwnerCommissionSummaryResponse(
        commission.commissionId().value().toString(),
        commission.placementId().value().toString(),
        commission.consultantId().toString(),
        commission.status().wireValue(),
        commission.commissionType().wireValue(),
        commission.amount(),
        commission.currency(),
        commission.splitPercentage(),
        details.salaryAmount(),
        details.feeRatePercentage(),
        commission.paidAt(),
        commission.withheldReason(),
        commission.createdAt(),
        commission.updatedAt());
  }
}
