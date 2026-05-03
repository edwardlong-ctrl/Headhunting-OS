import { ApiResult, apiRequest, asMethodJson } from "./http";

export type ClientPreferenceItem = {
  preferenceKey: string;
  preferenceValue: string;
  notes: string | null;
};

export type ClientPreferenceResponse = {
  companyId: string;
  preferences: ClientPreferenceItem[];
};

export function fetchClientPreferences(): Promise<ApiResult<ClientPreferenceResponse>> {
  return apiRequest<ClientPreferenceResponse>("/api/client/company-profile/preferences", undefined, "client");
}

export function saveClientPreferences(preferences: ClientPreferenceItem[]) {
  return apiRequest<ClientPreferenceResponse>(
    "/api/client/company-profile/preferences",
    asMethodJson("PUT", { preferences }),
    "client",
  );
}
