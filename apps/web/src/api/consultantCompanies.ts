import { ApiResult, PagedResult, apiRequest, asJson, asMethodJson } from "./http";

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

export function listConsultantCompanies(): Promise<ApiResult<PagedResult<ConsultantCompanySummary>>> {
  return apiRequest<PagedResult<ConsultantCompanySummary>>("/api/consultant/companies");
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
