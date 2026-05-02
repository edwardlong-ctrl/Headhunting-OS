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
