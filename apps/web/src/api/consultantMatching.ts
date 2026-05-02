import { ApiResult, apiRequest, asJson } from "./http";

export type ConsultantMatchReport = {
  matchReportId: string;
  finalScore: number;
  capApplied: boolean;
  capReason: string;
  confidence: string;
};

export const CONSULTANT_MATCH_DIMENSIONS = [
  "TECHNICAL_FIT",
  "INDUSTRY_FIT",
  "SENIORITY_FIT",
  "SALARY_FIT",
  "LOCATION_FIT",
  "MOTIVATION_FIT",
  "AVAILABILITY_FIT",
  "EVIDENCE_STRENGTH",
  "CULTURE_OR_MANAGER_FIT",
] as const;

export type ConsultantMatchDimension = (typeof CONSULTANT_MATCH_DIMENSIONS)[number];
export type ConsultantMatchDimensionScores = Record<ConsultantMatchDimension, number>;

export type ConsultantMatchGenerationPayload = {
  candidateId?: string;
  anonymousCandidateCardId?: string;
  candidateCardRef?: string;
  requestedOverallScore: number;
  requestedDimensionScores: ConsultantMatchDimensionScores;
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

export function createDefaultRequestedDimensionScores(
  overrides: Partial<ConsultantMatchDimensionScores> = {},
): ConsultantMatchDimensionScores {
  return {
    TECHNICAL_FIT: 80,
    INDUSTRY_FIT: 76,
    SENIORITY_FIT: 82,
    SALARY_FIT: 72,
    LOCATION_FIT: 75,
    MOTIVATION_FIT: 78,
    AVAILABILITY_FIT: 74,
    EVIDENCE_STRENGTH: 79,
    CULTURE_OR_MANAGER_FIT: 73,
    ...overrides,
  };
}

export function createConsultantMatchGenerationPayload(subject: {
  candidateId?: string;
  anonymousCandidateCardId?: string;
  candidateCardRef?: string;
}): ConsultantMatchGenerationPayload {
  return {
    candidateId: subject.candidateId,
    anonymousCandidateCardId: subject.anonymousCandidateCardId,
    candidateCardRef: subject.candidateCardRef,
    requestedOverallScore: 80,
    requestedDimensionScores: createDefaultRequestedDimensionScores(),
    industryPackMaturity: "MATURE",
    keywordOnlyEvidence: false,
    projectEvidencePresent: true,
    candidateIntentSignalStrength: "MEDIUM",
    ontologyStale: false,
    industryPackVersionStale: false,
    authenticityRisk: "LOW",
    reidentificationRiskSignal: "LOW",
    ontologyVersion: "v2.1",
    industryPackVersion: "default",
  };
}

export function generateConsultantMatch(
  jobId: string,
  payload: ConsultantMatchGenerationPayload,
): Promise<ApiResult<ConsultantMatchReport>> {
  return apiRequest<ConsultantMatchReport>(`/api/consultant/jobs/${encodeURIComponent(jobId)}/matching/generate`, asJson(payload));
}
