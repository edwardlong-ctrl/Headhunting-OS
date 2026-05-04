import { type ApiResult, apiRequest, asMethodJson } from "./http";

export type CandidateMe = {
  candidateRef: string;
  displayName: string;
  organizationId: string;
  currentProfileVersion: string;
  documentCount: number;
  activeOpportunityCount: number;
  pendingFollowUpCount: number;
};

export type ProfileField = {
  fieldPath: string;
  jsonValue: string;
  status: string;
  sourceType: string;
  updatedAt: string | null;
};

export type CandidateProfileReview = {
  candidateRef: string;
  profileVersion: string;
  fields: ProfileField[];
};

export type CandidateDocument = {
  documentId: string;
  documentType: string;
  title: string;
  status: string;
  fileSizeBytes: number;
  mimeType: string;
  uploadedAt: string;
};

export type CandidateOpportunity = {
  interactionId: string;
  jobTitle: string;
  companyName: string;
  status: string;
  interactionType: string;
  startedAt: string;
  updatedAt: string;
};

export type TimelineEvent = {
  eventType: string;
  actionCode: string;
  status: string;
  reason: string;
  occurredAt: string;
};

export type CandidateTimeline = {
  candidateRef: string;
  events: TimelineEvent[];
};

export function fetchCandidateMe(): Promise<ApiResult<CandidateMe>> {
  return apiRequest<CandidateMe>("/api/candidate/me", undefined, "candidate");
}

export function fetchCandidateProfile(candidateRef: string): Promise<ApiResult<CandidateProfileReview>> {
  return apiRequest<CandidateProfileReview>(
    `/api/candidate/profile/${encodeURIComponent(candidateRef)}`,
    undefined,
    "candidate",
  );
}

export function fetchCandidateDocuments(
  limit = 20,
  offset = 0,
): Promise<ApiResult<{ items: CandidateDocument[]; totalCount: number; limit: number; offset: number; hasMore: boolean }>> {
  return apiRequest(
    `/api/candidate/documents?limit=${limit}&offset=${offset}`,
    undefined,
    "candidate",
  );
}

export function uploadCandidateDocument(
  file: File,
  documentType = "resume",
): Promise<ApiResult<{ sourceItemId: string; informationPacketId: string | null; scanStatus: string }>> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("documentType", documentType);
  return apiRequest(
    "/api/candidate/documents/upload",
    { method: "POST", body: formData },
    "candidate",
  );
}

export function fetchCandidateOpportunities(): Promise<ApiResult<{ items: CandidateOpportunity[]; totalCount: number; limit: number; offset: number; hasMore: boolean }>> {
  return apiRequest("/api/candidate/opportunities", undefined, "candidate");
}

export function fetchCandidateTimeline(candidateRef: string): Promise<ApiResult<CandidateTimeline>> {
  return apiRequest<CandidateTimeline>(
    `/api/candidate/timeline/${encodeURIComponent(candidateRef)}`,
    undefined,
    "candidate",
  );
}
