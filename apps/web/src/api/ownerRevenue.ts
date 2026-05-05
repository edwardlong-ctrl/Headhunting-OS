import { ApiResult, apiRequest } from "./http";

export type OwnerRevenueSummary = {
  totalExpectedFee: number;
  totalPaidFee: number;
  placementCount: number;
  unknownExpectedFeePlacementCount: number;
  pendingCommissionCount: number;
  paidCommissionCount: number;
  paidCommissionMissingAmountCount: number;
  activeGuaranteeCount: number;
  replacementRequiredCount: number;
  invoiceInFlightCount: number;
};

export function fetchOwnerRevenueSummary(): Promise<ApiResult<OwnerRevenueSummary>> {
  return apiRequest<OwnerRevenueSummary>("/api/owner/revenue", undefined, "owner");
}
