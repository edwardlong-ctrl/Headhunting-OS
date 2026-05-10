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
  invoiceReadyCount: number;
  invoiceSentCount: number;
  paidPlacementCount: number;
  guaranteeCompletedCount: number;
};

export type OwnerAccountingExport = {
  format: "csv";
  process: string;
  disclaimer: string;
  generatedAt: string;
  content: string;
};

export function fetchOwnerRevenueSummary(): Promise<ApiResult<OwnerRevenueSummary>> {
  return apiRequest<OwnerRevenueSummary>("/api/owner/revenue", undefined, "owner");
}

export function fetchOwnerAccountingExport(): Promise<ApiResult<OwnerAccountingExport>> {
  return apiRequest<OwnerAccountingExport>("/api/owner/revenue/accounting-export", undefined, "owner");
}
