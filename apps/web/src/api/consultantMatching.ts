import { ApiResult, apiRequest, asJson } from "./http";

export type ConsultantMatchReport = {
  matchReportId: string;
  subjectType: string;
  subjectRef: string;
  finalScore: number;
  capApplied: boolean;
  capReason: string;
  capSafeExplanation: string;
  confidence: string;
  authenticityRisk: string;
  reidentificationRiskSignal: string;
  industryPackKey: string | null;
  industryPackMaturity: string | null;
  ontologyStale: boolean | null;
  selectionReason: string | null;
  ontologyVersion: string;
  industryPackVersion: string;
  generatedAt: string;
  dimensionScores: Array<{ dimension: string; score: number }>;
  evidenceCoverage: {
    coverageRatio: number;
    coverageLevel: string;
    independentEvidenceCount: number;
    independentHighTrustEvidenceCount: number;
  };
  provenanceSummary: {
    strongestProvenanceCategory: string;
    strongestSourceStrength: string;
    provenanceWeight: number;
    assertionStrength: string;
  };
  antiPatternWarnings: string[];
  explanations: string[];
  interviewQuestions: string[];
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
  shortlistCandidateCardId?: string;
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
  shortlistCandidateCardId?: string;
}): ConsultantMatchGenerationPayload {
  return {
    candidateId: subject.candidateId,
    shortlistCandidateCardId: subject.shortlistCandidateCardId,
  };
}

export function generateConsultantMatch(
  jobId: string,
  payload: ConsultantMatchGenerationPayload,
): Promise<ApiResult<ConsultantMatchReport>> {
  return apiRequest<ConsultantMatchReport>(`/api/consultant/jobs/${encodeURIComponent(jobId)}/matching/generate`, asJson(payload));
}

export function listConsultantMatchReports(
  jobId: string,
): Promise<ApiResult<{ reports: ConsultantMatchReport[] }>> {
  return apiRequest<{ reports: ConsultantMatchReport[] }>(
    `/api/consultant/jobs/${encodeURIComponent(jobId)}/matching`,
  );
}
