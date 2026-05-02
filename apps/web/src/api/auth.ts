import { ApiResult, apiRequest, asJson } from "./http";

export type AuthSession = {
  organizationId: string;
  userAccountId: string;
  displayName: string;
  portalRole: string;
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  refreshTokenExpiresAt: string;
};

export type LoginPayload = {
  organizationId?: string;
  email: string;
  password: string;
  portalRole: string;
};

export function login(payload: LoginPayload): Promise<ApiResult<AuthSession>> {
  return apiRequest<AuthSession>("/api/auth/login", asJson(payload));
}
