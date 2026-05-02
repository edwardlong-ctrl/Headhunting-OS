import { ApiResult, apiRequest } from "./http";

export type ConsultantWorkflowEvent = {
  workflowEventId: string;
  entityType: string;
  entityId: string;
  actionCode: string;
  actorType: string;
  aiInvolvement: string;
  riskTier: string;
  beforeStatus: string | null;
  afterStatus: string | null;
  reason: string;
  occurredAt: string;
};

export type ConsultantWorkflowTimeline = {
  items: ConsultantWorkflowEvent[];
  limit: number;
  offset: number;
  hasMore: boolean;
};

export type ConsultantAuditDrawer = {
  entityType: string;
  entityId: string;
  items: ConsultantWorkflowEvent[];
};

export function fetchConsultantWorkflow(entityType?: string, entityId?: string): Promise<ApiResult<ConsultantWorkflowTimeline>> {
  const params = new URLSearchParams();
  if (entityType) params.set("entityType", entityType);
  if (entityId) params.set("entityId", entityId);
  const suffix = params.size ? `?${params.toString()}` : "";
  return apiRequest<ConsultantWorkflowTimeline>(`/api/consultant/workflow${suffix}`);
}

export function fetchConsultantAuditDrawer(entityType: string, entityId: string): Promise<ApiResult<ConsultantAuditDrawer>> {
  const params = new URLSearchParams({ entityType, entityId });
  return apiRequest<ConsultantAuditDrawer>(`/api/consultant/workflow/audit?${params.toString()}`);
}
