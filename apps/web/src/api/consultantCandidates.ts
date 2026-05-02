import { ApiResult, PagedResult, apiRequest } from "./http";

export type ConsultantCandidateSummary = {
  candidateId: string;
  status: string;
  privacyStatus: string;
  currentProfileId: string | null;
  ownerConsultantId: string | null;
  lastActivityAt: string | null;
  createdAt: string;
};

export type ConsultantCandidateDetail = ConsultantCandidateSummary & {
  profileVersion: string | null;
  doNotContactReason: string | null;
  mergedIntoCandidateId: string | null;
  defaultIndustryPackId: string | null;
  updatedAt: string;
  overview: Array<{
    fieldPath: string;
    label: string;
    value: string | null;
    status: string;
    lastReviewedAt: string | null;
    notes: string | null;
  }>;
  evidence: Array<{
    fieldPath: string;
    sourceType: string;
    sourceId: string;
    sourceTrust: string | null;
    provenanceLabel: string | null;
    createdAt: string;
  }>;
  conflicts: Array<{
    fieldPath: string;
    severity: string;
    resolutionStatus: string;
    conflictingValues: string[];
    detectedAt: string;
    notes: string | null;
  }>;
  staleInfo: Array<{
    fieldPath: string;
    staleReason: string;
    reviewBy: string | null;
    lastConfirmedAt: string | null;
    detectedAt: string;
  }>;
  followUps: Array<{
    fieldPath: string;
    followUpType: string;
    reason: string;
    recommendedAction: string;
  }>;
  history: Array<{
    eventType: string;
    fieldPath: string | null;
    description: string;
    occurredAt: string;
  }>;
};

export function listConsultantCandidates(status?: string): Promise<ApiResult<PagedResult<ConsultantCandidateSummary>>> {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  const suffix = params.size ? `?${params.toString()}` : "";
  return apiRequest<PagedResult<ConsultantCandidateSummary>>(`/api/consultant/candidates${suffix}`);
}

export function fetchConsultantCandidate(candidateId: string): Promise<ApiResult<ConsultantCandidateDetail>> {
  return apiRequest<ConsultantCandidateDetail>(`/api/consultant/candidates/${encodeURIComponent(candidateId)}`);
}
