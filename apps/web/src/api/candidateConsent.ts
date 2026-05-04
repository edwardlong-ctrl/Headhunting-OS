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
  consentRecordRef: string,
): Promise<ApiResult<CandidateConsentSummary>> {
  return apiRequest<CandidateConsentSummary>(
    `/api/candidate/consent/${encodeURIComponent(candidateRef)}/requests/${encodeURIComponent(consentRecordRef)}`,
    undefined,
    "candidate",
  );
}

export function respondCandidateConsent(
  candidateRef: string,
  consentRecordRef: string,
  approve: boolean,
): Promise<ApiResult<CandidateConsentSummary>> {
  return apiRequest<CandidateConsentSummary>(
    `/api/candidate/consent/${encodeURIComponent(candidateRef)}/requests/${encodeURIComponent(consentRecordRef)}/respond`,
    asJson({ approve }),
    "candidate",
  );
}
