import { ApiResult, PagedResult, apiRequest, asJson, asMethodJson } from "./http";

export type ConsultantShortlistSummary = {
  shortlistId: string;
  title: string;
  jobId: string;
  status: string;
  candidateCount: number;
  createdAt: string;
};

export type ConsultantShortlistDetail = {
  shortlistId: string;
  version: number;
  jobId: string;
  title: string;
  status: string;
  sentAt: string | null;
  clientViewedAt: string | null;
  ownerConsultantId: string | null;
  createdAt: string;
  updatedAt: string;
  cards: Array<{ cardId: string; anonymousCandidateCardId: string; sortOrder: number; status: string; matchReportId: string | null }>;
};

export type ConsultantShortlistCreatePayload = {
  jobId: string;
  title: string;
  status: string;
};

export type ConsultantShortlistUpdatePayload = {
  version: number;
  title: string;
  status: string;
};

export function listConsultantShortlists(jobId?: string): Promise<ApiResult<PagedResult<ConsultantShortlistSummary>>> {
  const params = new URLSearchParams();
  if (jobId) params.set("jobId", jobId);
  const suffix = params.size ? `?${params.toString()}` : "";
  return apiRequest<PagedResult<ConsultantShortlistSummary>>(`/api/consultant/shortlists${suffix}`);
}

export function fetchConsultantShortlist(shortlistId: string): Promise<ApiResult<ConsultantShortlistDetail>> {
  return apiRequest<ConsultantShortlistDetail>(`/api/consultant/shortlists/${encodeURIComponent(shortlistId)}`);
}

export function createConsultantShortlist(payload: ConsultantShortlistCreatePayload) {
  return apiRequest<ConsultantShortlistDetail>("/api/consultant/shortlists", asJson(payload));
}

export function updateConsultantShortlist(shortlistId: string, payload: ConsultantShortlistUpdatePayload) {
  return apiRequest<ConsultantShortlistDetail>(
    `/api/consultant/shortlists/${encodeURIComponent(shortlistId)}`,
    asMethodJson("PUT", payload),
  );
}
