import { ApiResult, PagedResult, apiRequest, asJson } from "./http";

export type ClientShortlistSummary = {
  shortlistId: string;
  jobId: string;
  title: string;
  status: string;
  candidateCount: number;
  sentAt: string | null;
  clientViewedAt: string | null;
  createdAt: string;
};

export type ClientShortlistCard = {
  shortlistCandidateCardId: string;
  anonymousCardRef: string;
  status: string;
  generalizedHeadline: string;
  generalizedRoleFamily: string;
  generalizedSeniorityBand: string;
  generalizedLocationRegion: string;
  safeSummary: string;
  safeSkillSummary: string;
  overallScore: number | null;
  confidence: string;
  reidentificationRiskSignal: string;
  clientNotes: string | null;
  unlockRequestStatus: string | null;
  unlockDecisionRef: string | null;
  approvedDisclosureRecordRef: string | null;
};

export type ClientUnlockBlocker = {
  code: string;
  message: string;
};

export type ClientShortlistDetail = {
  shortlistId: string;
  jobId: string;
  title: string;
  status: string;
  sentAt: string | null;
  clientViewedAt: string | null;
  createdAt: string;
  updatedAt: string;
  cards: ClientShortlistCard[];
};

export type ClientShortlistCandidateSelection = {
  shortlistId: string;
  shortlistCandidateCardId: string;
  shortlistStatus: string;
  cardStatus: string;
  anonymousCardRef: string;
};

export type ClientUnlockRequest = {
  clientUnlockRequestId: string | null;
  shortlistId: string;
  shortlistCandidateCardId: string;
  anonymousCardRef: string;
  status: string;
  stage: string;
  requestReason: string;
  createdAt: string;
  updatedAt: string;
  unlockDecisionRef: string | null;
  approvedDisclosureRecordRef: string | null;
  blockers: ClientUnlockBlocker[];
};

export type ClientDashboard = {
  companyId: string;
  companyName: string;
  companyProfileReady: boolean;
  activeJobCount: number;
  pendingClarificationCount: number;
  unreadNotificationCount: number;
  shortlistCount: number;
  pendingUnlockRequestCount: number;
  feedbackCount: number;
  recentNotifications: string[];
  recentShortlists: ClientShortlistSummary[];
};

export function fetchClientDashboard(): Promise<ApiResult<ClientDashboard>> {
  return apiRequest<ClientDashboard>("/api/client/dashboard", undefined, "client");
}

export function fetchClientShortlists(): Promise<ApiResult<PagedResult<ClientShortlistSummary>>> {
  return apiRequest<PagedResult<ClientShortlistSummary>>("/api/client/shortlists", undefined, "client");
}

export function fetchClientShortlist(shortlistId: string): Promise<ApiResult<ClientShortlistDetail>> {
  return apiRequest<ClientShortlistDetail>(`/api/client/shortlists/${encodeURIComponent(shortlistId)}`, undefined, "client");
}

export function markClientShortlistViewed(shortlistId: string): Promise<ApiResult<ClientShortlistDetail>> {
  return apiRequest<ClientShortlistDetail>(`/api/client/shortlists/${encodeURIComponent(shortlistId)}/view`, { method: "POST" }, "client");
}

export function selectClientShortlistCandidate(
  shortlistId: string,
  shortlistCandidateCardId: string,
): Promise<ApiResult<ClientShortlistCandidateSelection>> {
  return apiRequest<ClientShortlistCandidateSelection>(
    `/api/client/shortlists/${encodeURIComponent(shortlistId)}/cards/${encodeURIComponent(shortlistCandidateCardId)}/select`,
    { method: "POST" },
    "client",
  );
}

export function createClientUnlockRequest(
  shortlistId: string,
  shortlistCandidateCardId: string,
  requestReason: string,
): Promise<ApiResult<ClientUnlockRequest>> {
  return apiRequest<ClientUnlockRequest>(
    `/api/client/shortlists/${encodeURIComponent(shortlistId)}/cards/${encodeURIComponent(shortlistCandidateCardId)}/unlock-requests`,
    asJson({ requestReason }),
    "client",
  );
}
