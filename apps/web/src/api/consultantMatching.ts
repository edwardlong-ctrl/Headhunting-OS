import { ApiResult, apiRequest, asJson } from "./http";

export type ConsultantMatchReport = {
  matchReportId: string;
  finalScore: number;
  capApplied: boolean;
  capReason: string;
  confidence: string;
};

export type ConsultantMatchGenerationPayload = {
  candidateId?: string;
  anonymousCandidateCardId?: string;
  candidateCardRef?: string;
  requestedOverallScore: number;
  requestedDimensionScores: Record<string, number>;
  industryPackMaturity: string;
  keywordOnlyEvidence: boolean;
  projectEvidencePresent: boolean;
  candidateIntentSignalStrength: string;
  ontologyStale: boolean;
  industryPackVersionStale: boolean;
  authenticityRisk: string;
  reidentificationRiskSignal: string;
  ontologyVersion: string;
  industryPackVersion: string;
};

export function generateConsultantMatch(
  jobId: string,
  payload: ConsultantMatchGenerationPayload,
): Promise<ApiResult<ConsultantMatchReport>> {
  return apiRequest<ConsultantMatchReport>(`/api/consultant/jobs/${encodeURIComponent(jobId)}/matching/generate`, asJson(payload));
}
