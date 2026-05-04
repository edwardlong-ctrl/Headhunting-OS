import { type ApiResult, apiRequest, asJson } from "./http";

export type CandidateConsentSharedField = {
  fieldPath: string;
  jsonValue: string;
};

export type CandidateConsentSummary = {
  candidateRef: string;
  candidateProfileRef: string;
  jobRef: string;
  jobTitle: string;
  consentRecordRef: string;
  consentStatus: string;
  consentTextVersion: string;
  currentProfileVersion: string;
  profileVersionMatches: boolean;
  revoked: boolean;
  expiresAt: string | null;
  sharedFields: CandidateConsentSharedField[];
};

export function fetchCandidateConsent(
  candidateRef: string,
  candidateProfileRef: string,
  jobRef: string,
): Promise<ApiResult<CandidateConsentSummary>> {
  return apiRequest<CandidateConsentSummary>(
    `/api/candidate/consent/${encodeURIComponent(candidateRef)}/${encodeURIComponent(candidateProfileRef)}/${encodeURIComponent(jobRef)}`,
    undefined,
    "candidate",
  );
}

export function respondCandidateConsent(
  candidateRef: string,
  candidateProfileRef: string,
  jobRef: string,
  approve: boolean,
): Promise<ApiResult<CandidateConsentSummary>> {
  return apiRequest<CandidateConsentSummary>(
    `/api/candidate/consent/${encodeURIComponent(candidateRef)}/${encodeURIComponent(candidateProfileRef)}/${encodeURIComponent(jobRef)}/respond`,
    asJson({ approve }),
    "candidate",
  );
}
