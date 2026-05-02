import { ApiResult, apiRequest } from "./http";

export type ConsultantWorkflowFilters = {
  entityType?: string;
  entityId?: string;
  limit?: number;
  offset?: number;
};

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

export type ConsultantWorkflowBlocker = {
  code: string;
  safeReason: string | null;
};

export type ConsultantWorkflowTransitionOption = {
  actionCode: string;
  currentStatus: string | null;
  targetStatus: string | null;
  allowed: boolean;
  blockers: ConsultantWorkflowBlocker[];
};

export type ConsultantWorkflowEntityState = {
  entityType: string;
  entityId: string;
  currentStatus: string | null;
  transitionOptions: ConsultantWorkflowTransitionOption[];
};

export type ConsultantWorkflowTimeline = {
  items: ConsultantWorkflowEvent[];
  entityStates: ConsultantWorkflowEntityState[];
  limit: number;
  offset: number;
  hasMore: boolean;
};

export type ConsultantAuditDrawer = {
  entityType: string;
  entityId: string;
  items: ConsultantWorkflowEvent[];
};

export function fetchConsultantWorkflowEntityState(
  entityType: string,
  entityId: string,
): Promise<ApiResult<ConsultantWorkflowEntityState>> {
  const params = new URLSearchParams({ entityType, entityId });
  return apiRequest<ConsultantWorkflowEntityState>(
    `/api/consultant/workflow/entity-state?${params.toString()}`,
  );
}

export function fetchConsultantWorkflow(
  filters: ConsultantWorkflowFilters = {},
): Promise<ApiResult<ConsultantWorkflowTimeline>> {
  const params = new URLSearchParams();
  if (filters.entityType) params.set("entityType", filters.entityType);
  if (filters.entityId) params.set("entityId", filters.entityId);
  if (typeof filters.limit === "number") params.set("limit", String(filters.limit));
  if (typeof filters.offset === "number") params.set("offset", String(filters.offset));
  const suffix = params.size ? `?${params.toString()}` : "";
  return apiRequest<ConsultantWorkflowTimeline>(`/api/consultant/workflow${suffix}`);
}

export function fetchConsultantAuditDrawer(entityType: string, entityId: string): Promise<ApiResult<ConsultantAuditDrawer>> {
  const params = new URLSearchParams({ entityType, entityId });
  return apiRequest<ConsultantAuditDrawer>(`/api/consultant/workflow/audit?${params.toString()}`);
}
