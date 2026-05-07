import { apiRequest, asMethodJson, type ApiResult } from "./http";

export type GovernanceMetric = {
  key: string;
  label: string;
  value: string;
  severity: string;
  helperText: string;
};

export type GovernanceItem = {
  primaryText: string;
  secondaryText: string;
  status: string;
  detail: string;
  route: string;
};

export type GovernanceSection = {
  sectionKey: string;
  title: string;
  description: string;
  metrics: GovernanceMetric[];
  items: GovernanceItem[];
  warnings: string[];
  editable: boolean;
  configJson: string;
  updatedAt: string;
};

export type GovernanceConfigUpdate = {
  sectionKey: string;
  status: string;
  updatedAt: string;
};

export async function fetchOwnerGovernanceSection(sectionKey: string): Promise<ApiResult<GovernanceSection>> {
  return apiRequest<GovernanceSection>(`/api/owner/${sectionKey}`, undefined, "owner");
}

export async function fetchAdminGovernanceSection(sectionKey: string): Promise<ApiResult<GovernanceSection>> {
  return apiRequest<GovernanceSection>(`/api/admin/${sectionKey}`, undefined, "admin");
}

export async function saveAdminGovernanceSection(
  sectionKey: string,
  payloadJson: string,
  enabled = true,
): Promise<ApiResult<GovernanceConfigUpdate>> {
  return apiRequest<GovernanceConfigUpdate>(
    `/api/admin/${sectionKey}`,
    asMethodJson("PUT", { payloadJson, enabled }),
    "admin",
  );
}
