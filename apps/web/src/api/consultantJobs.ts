import { ApiResult, PagedResult, apiRequest, asJson, asMethodJson } from "./http";

export type ConsultantJobListFilters = {
  status?: string;
  companyId?: string;
  limit?: number;
  offset?: number;
};

export type ConsultantJobSummary = {
  jobId: string;
  title: string;
  companyId: string;
  status: string;
  createdAt: string;
};

export type ConsultantJobDetail = {
  jobId: string;
  version: number;
  companyId: string;
  title: string;
  description: string | null;
  location: string | null;
  seniorityBand: string | null;
  roleFamily: string | null;
  employmentType: string | null;
  compensation: string | null;
  commercialTerms: string | null;
  status: string;
  ownerConsultantId: string | null;
  activatedAt: string | null;
  closedAt: string | null;
  closeReason: string | null;
  createdAt: string;
  updatedAt: string;
  requirements: Array<{ requirementId: string; requirementType: string; label: string; importance: string; detail: string | null; sortOrder: number }>;
  scorecard: null | { scorecardId: string; dimensions: string | null; scoringGuidance: string | null; status: string };
};

export type ConsultantJobActivationGate = {
  jobId: string;
  activationAllowed: boolean;
  clarificationQuestions: string[];
  blockerReasons: string[];
  hasScorecard: boolean;
  hasRequirements: boolean;
  hasCommercialTermsPlaceholder: boolean;
};

export type ConsultantJobCreatePayload = {
  companyId: string;
  title: string;
  status: string;
};

export type ConsultantJobUpdatePayload = {
  companyId: string;
  version: number;
  title: string;
  description?: string | null;
  location?: string | null;
  seniorityBand?: string | null;
  roleFamily?: string | null;
  employmentType?: string | null;
  compensation?: string | null;
  commercialTerms?: string | null;
  status: string;
};

export function createConsultantJobUpdatePayload(
  job: Pick<ConsultantJobDetail, "companyId" | "version">,
  fields: Omit<ConsultantJobUpdatePayload, "companyId" | "version">,
): ConsultantJobUpdatePayload {
  return {
    companyId: job.companyId,
    version: job.version,
    ...fields,
  };
}

export type ConsultantJobRequirementCreatePayload = {
  requirementType: string;
  label: string;
  importance: string;
  detail?: string | null;
  sortOrder: number;
};

export type ConsultantJobScorecardCreatePayload = {
  dimensions?: string | null;
  scoringGuidance?: string | null;
  status: string;
};

export function listConsultantJobs(
  filters: ConsultantJobListFilters = {},
): Promise<ApiResult<PagedResult<ConsultantJobSummary>>> {
  const params = new URLSearchParams();
  if (filters.status) params.set("status", filters.status);
  if (filters.companyId) params.set("companyId", filters.companyId);
  if (typeof filters.limit === "number") params.set("limit", String(filters.limit));
  if (typeof filters.offset === "number") params.set("offset", String(filters.offset));
  const suffix = params.size ? `?${params.toString()}` : "";
  return apiRequest<PagedResult<ConsultantJobSummary>>(`/api/consultant/jobs${suffix}`);
}

export function fetchConsultantJob(jobId: string): Promise<ApiResult<ConsultantJobDetail>> {
  return apiRequest<ConsultantJobDetail>(`/api/consultant/jobs/${encodeURIComponent(jobId)}`);
}

export function createConsultantJob(payload: ConsultantJobCreatePayload) {
  return apiRequest<ConsultantJobDetail>("/api/consultant/jobs", asJson(payload));
}

export function updateConsultantJob(jobId: string, payload: ConsultantJobUpdatePayload) {
  return apiRequest<ConsultantJobDetail>(
    `/api/consultant/jobs/${encodeURIComponent(jobId)}`,
    asMethodJson("PUT", payload),
  );
}

export function createConsultantJobRequirement(jobId: string, payload: ConsultantJobRequirementCreatePayload) {
  return apiRequest<ConsultantJobDetail>(
    `/api/consultant/jobs/${encodeURIComponent(jobId)}/requirements`,
    asJson(payload),
  );
}

export function createConsultantJobScorecard(jobId: string, payload: ConsultantJobScorecardCreatePayload) {
  return apiRequest<ConsultantJobDetail>(
    `/api/consultant/jobs/${encodeURIComponent(jobId)}/scorecard`,
    asJson(payload),
  );
}

export function fetchConsultantJobActivationGate(jobId: string) {
  return apiRequest<ConsultantJobActivationGate>(
    `/api/consultant/jobs/${encodeURIComponent(jobId)}/activation-gate`,
  );
}

export function activateConsultantJob(jobId: string, reason?: string | null) {
  return apiRequest<ConsultantJobDetail>(
    `/api/consultant/jobs/${encodeURIComponent(jobId)}/activate`,
    asJson({ reason: reason ?? null }),
  );
}
