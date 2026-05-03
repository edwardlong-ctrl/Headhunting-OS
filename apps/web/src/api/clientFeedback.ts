import { ApiResult, apiRequest, asJson } from "./http";

export type ClientFeedbackPayload = {
  outcome: string;
  notes: string;
  strengths?: string | null;
  concerns?: string | null;
  interviewRound?: number | null;
  interviewerName?: string | null;
  interviewerRole?: string | null;
};

export type ClientFeedbackResponse = {
  interviewFeedbackId: string;
  shortlistId: string;
  shortlistCandidateCardId: string;
  outcome: string;
  notes: string;
  strengths: string | null;
  concerns: string | null;
  interviewRound: number | null;
  interviewerName: string | null;
  interviewerRole: string | null;
  createdAt: string;
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
