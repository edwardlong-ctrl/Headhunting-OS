import { ApiResult, PagedResult, apiRequest, asJson } from "./http";

export type ConsultantCommission = {
  commissionId: string;
  version: number;
  placementId: string;
  consultantId: string;
  status: string;
  commissionType: string;
  amount: number | null;
  currency: string | null;
  splitPercentage: number | null;
  salaryAmount: number | null;
  feeRatePercentage: number | null;
  paidAt: string | null;
  withheldReason: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ConsultantCommissionCreatePayload = {
  placementId: string;
  commissionType: string;
  amount?: number | null;
  currency?: string | null;
  splitPercentage?: number | null;
  salaryAmount?: number | null;
  feeRatePercentage?: number | null;
};

export type ConsultantCommissionWithholdPayload = {
  version: number;
  reason: string;
};

export function listConsultantCommission(limit = 20, offset = 0): Promise<ApiResult<PagedResult<ConsultantCommission>>> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  return apiRequest<PagedResult<ConsultantCommission>>(`/api/consultant/commissions?${params.toString()}`);
}

export function createConsultantCommission(payload: ConsultantCommissionCreatePayload) {
  return apiRequest<ConsultantCommission>("/api/consultant/commissions", asJson(payload));
}

export function markConsultantCommissionPaid(commissionId: string, version: number) {
  return apiRequest<ConsultantCommission>(`/api/consultant/commissions/${encodeURIComponent(commissionId)}/mark-paid`, asJson({ version }));
}

export function withholdConsultantCommission(commissionId: string, payload: ConsultantCommissionWithholdPayload) {
  return apiRequest<ConsultantCommission>(`/api/consultant/commissions/${encodeURIComponent(commissionId)}/withhold`, asJson(payload));
}
