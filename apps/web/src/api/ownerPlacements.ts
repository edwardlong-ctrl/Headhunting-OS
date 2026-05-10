import { ApiResult, PagedResult, apiRequest } from "./http";

export type OwnerPlacement = {
  placementId: string;
  jobId: string;
  candidateId: string;
  companyId: string;
  status: string;
  salaryAmount: number | null;
  salaryCurrency: string | null;
  feeRatePercentage: number | null;
  expectedFeeAmount: number | null;
  feeAgreementActive: boolean;
  feeAgreementReference: string | null;
  paymentTerms: string | null;
  invoiceReadiness: string;
  accountingExportStatus: string;
  commissionStatuses: string[];
  startDate: string | null;
  guaranteeDays: number | null;
  guaranteeExpiresAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type OwnerCommission = {
  commissionId: string;
  placementId: string;
  consultantId: string;
  status: string;
  commissionType: string;
  amount: number | null;
  currency: string | null;
  splitPercentage: number | null;
  salaryAmount: number | null;
  feeRatePercentage: number | null;
  expectedFeeAmount: number | null;
  feeAgreementReference: string | null;
  paymentTerms: string | null;
  calculationSource: string | null;
  paidAt: string | null;
  withheldReason: string | null;
  createdAt: string;
  updatedAt: string;
};

export function listOwnerPlacements(limit = 20, offset = 0): Promise<ApiResult<PagedResult<OwnerPlacement>>> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  return apiRequest<PagedResult<OwnerPlacement>>(`/api/owner/placements?${params.toString()}`, undefined, "owner");
}

export function listOwnerCommission(limit = 20, offset = 0): Promise<ApiResult<PagedResult<OwnerCommission>>> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  return apiRequest<PagedResult<OwnerCommission>>(`/api/owner/commission?${params.toString()}`, undefined, "owner");
}
