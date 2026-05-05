import { ApiResult, apiRequest, asJson } from "./http";

export type ClientFeedbackPayload = {
  outcome: string;
  decision: string;
  interviewId?: string | null;
  rejectReasonTaxonomy?: string | null;
  notes: string;
  strengths?: string | null;
  concerns?: string | null;
  interviewDate?: string | null;
  interviewRound?: number | null;
  interviewerName?: string | null;
  interviewerRole?: string | null;
  ratings: Array<{
    dimensionKey: string;
    label: string;
    score: number;
    notes?: string | null;
  }>;
};

export type ClientFeedbackResponse = {
  interviewFeedbackId: string;
  interviewId: string;
  shortlistId: string;
  shortlistCandidateCardId: string;
  outcome: string;
  decision: string;
  rejectReasonTaxonomy: string | null;
  notes: string;
  strengths: string | null;
  concerns: string | null;
  ratings: string;
  interviewRound: number | null;
  interviewerName: string | null;
  interviewerRole: string | null;
  structuredSummary: string | null;
  pendingSuggestionCount: number | null;
  aiStructured: boolean;
  createdAt: string;
};

export type ClientFeedbackContextResponse = {
  interviewId: string;
  shortlistId: string;
  shortlistCandidateCardId: string;
  jobId: string;
  shortlistStatus: string;
  scorecard: string;
  existingFeedbackCount: number;
  routeHint: string;
};

export function submitClientFeedback(
  shortlistId: string,
  shortlistCandidateCardId: string,
  payload: ClientFeedbackPayload,
): Promise<ApiResult<ClientFeedbackResponse>> {
  return apiRequest<ClientFeedbackResponse>(
    `/api/client/shortlists/${encodeURIComponent(shortlistId)}/cards/${encodeURIComponent(shortlistCandidateCardId)}/feedback`,
    asJson(payload),
    "client",
  );
}

export function fetchClientFeedbackContext(
  interviewId: string,
): Promise<ApiResult<ClientFeedbackContextResponse>> {
  return apiRequest<ClientFeedbackContextResponse>(
    `/api/client/interviews/${encodeURIComponent(interviewId)}/feedback`,
    undefined,
    "client",
  );
}
