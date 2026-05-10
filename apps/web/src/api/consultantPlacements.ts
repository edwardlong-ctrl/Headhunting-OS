import { ApiResult, PagedResult, apiRequest, asJson } from "./http";

export type ConsultantPlacement = {
  placementId: string;
  version: number;
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
  startDate: string | null;
  guaranteeDays: number | null;
  guaranteeExpiresAt: string | null;
  offerAcceptedAt: string | null;
  onboardedAt: string | null;
  createdAt: string;
  updatedAt: string;
  notes: string | null;
};

export type ConsultantPlacementCreatePayload = {
  jobId: string;
  candidateId: string;
  companyId: string;
  salaryAmount?: number | null;
  salaryCurrency?: string | null;
  feeRatePercentage?: number | null;
  startDate?: string | null;
  guaranteeDays?: number | null;
  notes?: string | null;
  feeAgreementActive?: boolean | null;
  feeAgreementReference?: string | null;
  paymentTerms?: string | null;
};

export type ConsultantVersionedCommandPayload = {
  version: number;
};

export function listConsultantPlacements(limit = 20, offset = 0): Promise<ApiResult<PagedResult<ConsultantPlacement>>> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) });
  return apiRequest<PagedResult<ConsultantPlacement>>(`/api/consultant/placements?${params.toString()}`);
}

export function createConsultantPlacement(payload: ConsultantPlacementCreatePayload) {
  return apiRequest<ConsultantPlacement>("/api/consultant/placements", asJson(payload));
}

function command(path: string, payload: ConsultantVersionedCommandPayload) {
  return apiRequest<ConsultantPlacement>(path, asJson(payload));
}

export function markPlacementOfferAccepted(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/offer-accepted`, payload);
}

export function markPlacementOnboarded(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/onboarded`, payload);
}

export function markPlacementInvoiceReady(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/invoice-ready`, payload);
}

export function markPlacementInvoiceSent(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/invoice-sent`, payload);
}

export function markPlacementPaid(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/payment-paid`, payload);
}

export function activatePlacementGuarantee(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/guarantee-activated`, payload);
}

export function completePlacementGuarantee(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/guarantee-completed`, payload);
}

export function requirePlacementReplacement(placementId: string, payload: ConsultantVersionedCommandPayload) {
  return command(`/api/consultant/placements/${encodeURIComponent(placementId)}/replacement-required`, payload);
}
