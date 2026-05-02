import { ApiResult, apiRequest } from "./http";

export type ConsultantBlockedAction = {
  entityType: string;
  entityId: string;
  title: string;
  reasonCode: string;
  safeReason: string;
  severity: string;
  route: string;
};

export type ConsultantDashboard = {
  candidateCount: number;
  activeJobCount: number;
  companyCount: number;
  shortlistCount: number;
  pendingFollowUpCount: number;
  recentTimelineCount: number;
  blockedActions: ConsultantBlockedAction[];
};

export function fetchConsultantDashboard(): Promise<ApiResult<ConsultantDashboard>> {
  return apiRequest<ConsultantDashboard>("/api/consultant/dashboard");
}
