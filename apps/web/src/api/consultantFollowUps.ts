import { ApiResult, PagedResult, apiRequest } from "./http";

export type ConsultantFollowUp = {
  followUpType: string;
  entityType: string;
  entityId: string;
  title: string;
  status: string;
  safeReason: string;
  route: string;
  occurredAt: string;
};

export function listConsultantFollowUps(): Promise<ApiResult<PagedResult<ConsultantFollowUp>>> {
  return apiRequest<PagedResult<ConsultantFollowUp>>("/api/consultant/follow-ups");
}

export type ConsultantInterviewFeedbackSuggestion = {
  suggestionId: string;
  interviewFeedbackId: string;
  interviewId: string;
  jobId: string;
  candidateId: string | null;
  scope: string;
  suggestionType: string;
  status: string;
  outcomeLabel: string | null;
  rejectReasonTaxonomy: string | null;
  title: string;
  rationale: string | null;
  payload: string;
  createdAt: string;
  reviewedAt: string | null;
};

export type ConsultantInterviewFeedbackReviewResponse = {
  suggestionId: string;
  status: string;
  reviewedAt: string;
  interactionId: string;
  outcomeLabel: string | null;
  rejectReasonTaxonomy: string | null;
  interactionUpdated: boolean;
};

export function fetchConsultantInterviewFeedbackSuggestion(
  suggestionId: string,
): Promise<ApiResult<ConsultantInterviewFeedbackSuggestion>> {
  return apiRequest<ConsultantInterviewFeedbackSuggestion>(
    `/api/consultant/interview-feedback-suggestions/${encodeURIComponent(suggestionId)}`,
  );
}

export function reviewConsultantInterviewFeedbackSuggestion(
  suggestionId: string,
  decision: "approve" | "reject" | "defer",
  note?: string,
): Promise<ApiResult<ConsultantInterviewFeedbackReviewResponse>> {
  return apiRequest<ConsultantInterviewFeedbackReviewResponse>(
    `/api/consultant/interview-feedback-suggestions/${encodeURIComponent(suggestionId)}/review`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ decision, note: note ?? null }),
    },
  );
}
