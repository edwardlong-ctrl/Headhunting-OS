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
  candidateProfileRef: string;
  jobRef: string;
  consentStatus: string | null;
  consentRecordRef: string | null;
  interestStatus: string;
  startedAt: string;
  updatedAt: string;
};

export type CandidateFollowUpItem = {
  fieldPath: string;
  prompt: string;
  inputType: string;
  currentAnswer: string;
  status: string;
  sourceType: string;
  updatedAt: string | null;
};

export type CandidateFollowUpForm = {
  candidateRef: string;
  formId: string;
  profileVersion: string;
  items: CandidateFollowUpItem[];
};

export type CandidateOpportunityDetail = {
  interactionId: string;
  jobTitle: string;
  companyName: string;
  status: string;
  interactionType: string;
  candidateProfileRef: string;
  jobRef: string;
  consentRecordRef: string | null;
  consentStatus: string | null;
  roleSummary: string;
  location: string;
  compensation: string;
  fitExplanation: string;
  interestStatus: string;
  interestUpdatedAt: string | null;
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

export function confirmCandidateProfile(
  candidateRef: string,
  fieldPath: string,
): Promise<ApiResult<CandidateProfileReview>> {
  return apiRequest<CandidateProfileReview>(
    `/api/candidate/profile/${encodeURIComponent(candidateRef)}/confirm`,
    asMethodJson("POST", { fieldPath }),
    "candidate",
  );
}

export function fetchCandidateFollowUp(
  candidateRef: string,
  formId: string,
): Promise<ApiResult<CandidateFollowUpForm>> {
  return apiRequest<CandidateFollowUpForm>(
    `/api/candidate/follow-up/${encodeURIComponent(candidateRef)}/${encodeURIComponent(formId)}`,
    undefined,
    "candidate",
  );
}

export function submitCandidateFollowUp(
  candidateRef: string,
  formId: string,
  fieldPath: string,
  answer: string,
): Promise<ApiResult<CandidateFollowUpForm>> {
  return apiRequest<CandidateFollowUpForm>(
    `/api/candidate/follow-up/${encodeURIComponent(candidateRef)}/${encodeURIComponent(formId)}/submit`,
    asMethodJson("POST", { fieldPath, answer }),
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

export function fetchCandidateOpportunityDetail(
  candidateRef: string,
  interactionId: string,
): Promise<ApiResult<CandidateOpportunityDetail>> {
  return apiRequest<CandidateOpportunityDetail>(
    `/api/candidate/opportunities/${encodeURIComponent(candidateRef)}/${encodeURIComponent(interactionId)}`,
    undefined,
    "candidate",
  );
}

export function recordCandidateOpportunityInterest(
  candidateRef: string,
  interactionId: string,
  interestStatus: string,
  note: string,
): Promise<ApiResult<CandidateOpportunityDetail>> {
  return apiRequest<CandidateOpportunityDetail>(
    `/api/candidate/opportunities/${encodeURIComponent(candidateRef)}/${encodeURIComponent(interactionId)}/interest`,
    asMethodJson("POST", { interestStatus, note }),
    "candidate",
  );
}

export function fetchCandidateTimeline(candidateRef: string): Promise<ApiResult<CandidateTimeline>> {
  return apiRequest<CandidateTimeline>(
    `/api/candidate/timeline/${encodeURIComponent(candidateRef)}`,
    undefined,
    "candidate",
  );
}
