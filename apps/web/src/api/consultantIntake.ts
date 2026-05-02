import { ApiResult, apiRequest, asJson } from "./http";

export type ConsultantSourceHighlight = {
  sourceItemId: string;
  parsedDocumentId: string;
  parsedDocumentChunkId: string;
  pageNumber: number | null;
  startOffset: number;
  endOffset: number;
  safeSnippet: string;
  locator: string;
};

export type ConsultantCleanFact = {
  claimId: string | null;
  claimFieldName: string;
  targetEntityType: string;
  targetFieldPath: string;
  proposedValue: string;
  suggestedVerificationStatus: string;
  suggestedRiskTier: string;
  entityResolutionStatus: string;
  resolvedEntityId: string | null;
  latestReviewDecision: string | null;
  latestDecisionId: string | null;
  conflictsWithCanonical: boolean;
  canonicalWriteStatus: string;
  publishBlockedReason: string | null;
  rationale: string | null;
  sourceHighlight: ConsultantSourceHighlight;
};

export type ConsultantIntakeRun = {
  extractionRunId: string;
  informationPacketId: string;
  intendedEntityType: string;
  status: string;
  outputSchemaVersion: string;
  cleanFactCount: number;
  aiTaskRunIds: string[];
};

export type ConsultantIntakeReview = {
  extractionRunId: string;
  informationPacketId: string;
  intendedEntityType: string;
  cleanFactCount: number;
  cleanFacts: ConsultantCleanFact[];
};

export type ConsultantIntakePublish = {
  informationPacketId: string;
  canonicalWriteCount: number;
  canonicalWriteStatuses: string[];
  directWrites: string[];
};

export type ConsultantIntakeQueueItem = {
  informationPacketId: string;
  title: string;
  sourceType: string;
  intendedEntityType: string;
  stage: string;
  stageDetail: string;
  createdAt: string;
  updatedAt: string;
};

export type ConsultantIntakeQueue = {
  items: ConsultantIntakeQueueItem[];
};

export function extractConsultantIntake(packetId: string): Promise<ApiResult<ConsultantIntakeRun>> {
  return apiRequest<ConsultantIntakeRun>(`/api/consultant/intake/packets/${encodeURIComponent(packetId)}/extract`, { method: "POST" });
}

export function fetchConsultantIntakeReview(packetId: string): Promise<ApiResult<ConsultantIntakeReview>> {
  return apiRequest<ConsultantIntakeReview>(`/api/consultant/intake/packets/${encodeURIComponent(packetId)}/review`);
}

export function listConsultantIntakeQueue(limit = 12): Promise<ApiResult<ConsultantIntakeQueue>> {
  return apiRequest<ConsultantIntakeQueue>(`/api/consultant/intake/queue?limit=${encodeURIComponent(String(limit))}`);
}

export function submitConsultantIntakeDecision(claimId: string, payload: Record<string, unknown>) {
  return apiRequest<{ reviewEventId: string }>(`/api/consultant/intake/claims/${encodeURIComponent(claimId)}/decisions`, asJson(payload));
}

export function publishConsultantIntake(packetId: string, payload: Record<string, unknown>) {
  return apiRequest<ConsultantIntakePublish>(`/api/consultant/intake/packets/${encodeURIComponent(packetId)}/publish`, asJson(payload));
}
