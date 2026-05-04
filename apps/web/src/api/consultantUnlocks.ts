import { type ApiResult, apiRequest, asJson } from "./http";

export type ConsultantUnlockBlocker = {
  code: string;
  message: string;
};

export type ConsultantUnlockQueueItem = {
  unlockRequestId: string;
  shortlistId: string;
  shortlistCandidateCardId: string;
  status: string;
  requestReason: string;
  createdAt: string;
  anonymousCandidateCardRef: string;
  jobTitle: string;
  clientCompanyName: string;
  consentStatus: string;
  blockers: ConsultantUnlockBlocker[];
};

export type ConsultantUnlockQueue = {
  items: ConsultantUnlockQueueItem[];
};

export type ConsultantUnlockDecision = {
  unlockRequestId: string | null;
  status: string;
  unlockDecisionRef: string | null;
  approvedDisclosureRecordRef: string | null;
  blockers: ConsultantUnlockBlocker[];
};

export type ConsultantConsentRequestPayload = {
  candidateRef: string;
  candidateProfileRef: string;
  jobRef: string;
  consentTextVersion: string;
  expiresAt: string | null;
};

export function fetchConsultantUnlockQueue(): Promise<ApiResult<ConsultantUnlockQueue>> {
  return apiRequest<ConsultantUnlockQueue>("/api/consultant/unlock-requests");
}

export function approveConsultantUnlockRequest(
  shortlistId: string,
  shortlistCandidateCardId: string,
  reason: string,
): Promise<ApiResult<ConsultantUnlockDecision>> {
  return apiRequest<ConsultantUnlockDecision>(
    `/api/consultant/unlock-requests/${encodeURIComponent(shortlistId)}/${encodeURIComponent(shortlistCandidateCardId)}/approve`,
    asJson({ reason }),
  );
}

export function rejectConsultantUnlockRequest(
  shortlistId: string,
  shortlistCandidateCardId: string,
  reason: string,
): Promise<ApiResult<ConsultantUnlockDecision>> {
  return apiRequest<ConsultantUnlockDecision>(
    `/api/consultant/unlock-requests/${encodeURIComponent(shortlistId)}/${encodeURIComponent(shortlistCandidateCardId)}/reject`,
    asJson({ reason }),
  );
}

export function createConsultantConsentRequest(
  payload: ConsultantConsentRequestPayload,
): Promise<ApiResult<unknown>> {
  return apiRequest<unknown>(
    "/api/consultant/unlock-requests/consent-requests",
    asJson(payload),
  );
}
