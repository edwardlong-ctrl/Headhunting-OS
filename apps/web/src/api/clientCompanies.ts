import { ApiResult, apiRequest, asJson } from "./http";

export type ClientCompanyProfile = {
  companyId: string;
  version: number;
  name: string;
  displayName: string | null;
  industry: string | null;
  website: string | null;
  headquartersLocation: string | null;
  sizeBand: string | null;
  paymentReliability: string | null;
  status: string;
  updatedAt: string;
};

export type ClientCompanyProfilePayload = {
  companyId?: string | null;
  name: string;
  displayName?: string | null;
  industry?: string | null;
  website?: string | null;
  headquartersLocation?: string | null;
  sizeBand?: string | null;
  paymentReliability?: string | null;
};

export function fetchClientCompanyProfile(): Promise<ApiResult<ClientCompanyProfile>> {
  return apiRequest<ClientCompanyProfile>("/api/client/company-profile", undefined, "client");
}

export function saveClientCompanyProfile(payload: ClientCompanyProfilePayload) {
  return apiRequest<ClientCompanyProfile>("/api/client/company-profile", asJson(payload), "client");
}
