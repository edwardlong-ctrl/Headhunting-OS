import { ApiResult, PagedResult, apiRequest, asJson, asMethodJson } from "./http";

export type ConsultantShortlistListFilters = {
  jobId?: string;
  limit?: number;
  offset?: number;
};

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
  jobId: string;
  version: number;
  title: string;
  status: string;
};

export function createConsultantShortlistUpdatePayload(
  shortlist: Pick<ConsultantShortlistDetail, "jobId" | "version">,
  fields: Omit<ConsultantShortlistUpdatePayload, "jobId" | "version">,
): ConsultantShortlistUpdatePayload {
  return {
    jobId: shortlist.jobId,
    version: shortlist.version,
    ...fields,
  };
}

export function listConsultantShortlists(
  filters: ConsultantShortlistListFilters = {},
): Promise<ApiResult<PagedResult<ConsultantShortlistSummary>>> {
  const params = new URLSearchParams();
  if (filters.jobId) params.set("jobId", filters.jobId);
  if (typeof filters.limit === "number") params.set("limit", String(filters.limit));
  if (typeof filters.offset === "number") params.set("offset", String(filters.offset));
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
