import { ApiResult, apiRequest, asJson } from "./http";

export type ClientJobSubmissionStatus = {
  jobId: string;
  companyId: string;
  title: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  clarificationQuestions: string[];
  clarificationAnswers: string[];
  blockerReasons: string[];
  activationAllowed: boolean;
};

export type ClientJobCreatePayload = {
  companyId: string;
  title: string;
  description?: string | null;
  location?: string | null;
  compensation?: string | null;
  commercialTerms?: string | null;
  clarificationQuestions?: string[];
};

export type ClientJobClarificationPayload = {
  clarificationAnswers: string[];
  description?: string | null;
  location?: string | null;
  compensation?: string | null;
  commercialTerms?: string | null;
};

export function fetchClientJob(jobId: string): Promise<ApiResult<ClientJobSubmissionStatus>> {
  return apiRequest<ClientJobSubmissionStatus>(
    `/api/client/jobs/${encodeURIComponent(jobId)}`,
    undefined,
    "client",
  );
}

export function createClientJob(payload: ClientJobCreatePayload) {
  return apiRequest<ClientJobSubmissionStatus>("/api/client/jobs", asJson(payload), "client");
}

export function answerClientJobClarification(
  jobId: string,
  payload: ClientJobClarificationPayload,
) {
  return apiRequest<ClientJobSubmissionStatus>(
    `/api/client/jobs/${encodeURIComponent(jobId)}/clarification`,
    asJson(payload),
    "client",
  );
}
