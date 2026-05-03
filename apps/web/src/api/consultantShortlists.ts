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
  preSendChecks: Array<{
    code: string;
    label: string;
    passed: boolean;
  }>;
  deliveryPreview: {
    clientSafeSummary: string;
    pdfSummary: string;
    emailSummary: string;
    wechatSummary: string;
  };
  cards: Array<{
    cardId: string;
    anonymousCandidateCardId: string;
    version: number;
    sortOrder: number;
    status: string;
    matchReportId: string | null;
    anonymousCandidateRef: string;
    generalizedHeadline: string;
    generalizedRoleFamily: string;
    generalizedSeniorityBand: string;
    generalizedLocationRegion: string;
    safeSummary: string;
    safeSkillSummary: string;
    safeEvidenceSummaries: string[];
    safeMatchNarratives: string[];
    overallScore: number | null;
    confidence: string;
    reidentificationRiskSignal: string;
    dimensionScores: Array<{ dimension: string; score: number }>;
    clientNotes: string | null;
  }>;
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

export type ConsultantShortlistCardCreatePayload = {
  candidateId: string;
  sortOrder?: number;
  clientNotes?: string | null;
};

export type ConsultantShortlistCardUpdatePayload = {
  version: number;
  sortOrder?: number;
  status?: string | null;
  clientNotes?: string | null;
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

export function addConsultantShortlistCard(
  shortlistId: string,
  payload: ConsultantShortlistCardCreatePayload,
) {
  return apiRequest<ConsultantShortlistDetail>(
    `/api/consultant/shortlists/${encodeURIComponent(shortlistId)}/cards`,
    asJson(payload),
  );
}

export function updateConsultantShortlistCard(
  shortlistId: string,
  cardId: string,
  payload: ConsultantShortlistCardUpdatePayload,
) {
  return apiRequest<ConsultantShortlistDetail>(
    `/api/consultant/shortlists/${encodeURIComponent(shortlistId)}/cards/${encodeURIComponent(cardId)}`,
    asMethodJson("PUT", payload),
  );
}

export function sendConsultantShortlist(shortlistId: string) {
  return apiRequest<ConsultantShortlistDetail>(
    `/api/consultant/shortlists/${encodeURIComponent(shortlistId)}/send`,
    asJson({ approvalConfirmed: true }),
  );
}
