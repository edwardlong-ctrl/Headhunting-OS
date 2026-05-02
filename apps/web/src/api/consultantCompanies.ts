import { ApiResult, PagedResult, apiRequest, asJson, asMethodJson } from "./http";

export type ConsultantCompanyListFilters = {
  status?: string;
  limit?: number;
  offset?: number;
};

export type ConsultantCompanySummary = {
  companyId: string;
  name: string;
  status: string;
  contactCount: number;
  jobCount: number;
  createdAt: string;
};

export type ConsultantCompanyDetail = {
  companyId: string;
  version: number;
  name: string;
  displayName: string | null;
  industry: string | null;
  website: string | null;
  headquartersLocation: string | null;
  sizeBand: string | null;
  status: string;
  paymentReliability: string | null;
  ownerConsultantId: string | null;
  createdAt: string;
  updatedAt: string;
  contacts: Array<{ contactId: string; name: string; title: string | null; email: string | null; phone: string | null; roleType: string | null; isPrimary: boolean; status: string | null }>;
  jobCount: number;
};

export type ConsultantCompanyCreatePayload = {
  name: string;
  status: string;
};

export type ConsultantCompanyUpdatePayload = {
  version: number;
  name: string;
  displayName?: string | null;
  industry?: string | null;
  website?: string | null;
  headquartersLocation?: string | null;
  sizeBand?: string | null;
  status: string;
  paymentReliability?: string | null;
};

export type ConsultantCompanyContactCreatePayload = {
  name: string;
  title?: string | null;
  email?: string | null;
  phone?: string | null;
  roleType?: string | null;
  isPrimary: boolean;
  status?: string | null;
};

export function listConsultantCompanies(
  filters: ConsultantCompanyListFilters = {},
): Promise<ApiResult<PagedResult<ConsultantCompanySummary>>> {
  const params = new URLSearchParams();
  if (filters.status) params.set("status", filters.status);
  if (typeof filters.limit === "number") params.set("limit", String(filters.limit));
  if (typeof filters.offset === "number") params.set("offset", String(filters.offset));
  const suffix = params.size ? `?${params.toString()}` : "";
  return apiRequest<PagedResult<ConsultantCompanySummary>>(`/api/consultant/companies${suffix}`);
}

export function fetchConsultantCompany(companyId: string): Promise<ApiResult<ConsultantCompanyDetail>> {
  return apiRequest<ConsultantCompanyDetail>(`/api/consultant/companies/${encodeURIComponent(companyId)}`);
}

export function createConsultantCompany(payload: ConsultantCompanyCreatePayload) {
  return apiRequest<ConsultantCompanyDetail>("/api/consultant/companies", asJson(payload));
}

export function updateConsultantCompany(companyId: string, payload: ConsultantCompanyUpdatePayload) {
  return apiRequest<ConsultantCompanyDetail>(
    `/api/consultant/companies/${encodeURIComponent(companyId)}`,
    asMethodJson("PUT", payload),
  );
}

export function createConsultantCompanyContact(companyId: string, payload: ConsultantCompanyContactCreatePayload) {
  return apiRequest<ConsultantCompanyDetail>(
    `/api/consultant/companies/${encodeURIComponent(companyId)}/contacts`,
    asJson(payload),
  );
}
