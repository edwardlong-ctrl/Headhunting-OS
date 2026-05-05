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

export type RefreshPayload = {
  refreshToken: string;
};

export type LogoutPayload = {
  refreshToken: string;
};

export function login(payload: LoginPayload): Promise<ApiResult<AuthSession>> {
  return apiRequest<AuthSession>("/api/auth/login", asJson(payload));
}

export function refresh(payload: RefreshPayload): Promise<ApiResult<AuthSession>> {
  return apiRequest<AuthSession>("/api/auth/refresh", asJson(payload));
}

export function logout(payload: LogoutPayload): Promise<ApiResult<{ loggedOut: boolean }>> {
  return apiRequest<{ loggedOut: boolean }>("/api/auth/logout", asJson(payload));
}
