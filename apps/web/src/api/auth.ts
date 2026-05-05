import { type ApiEnvelope, ApiResult, asJson } from "./http";

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
  return authRequest<AuthSession>("/api/auth/login", payload);
}

export function refresh(payload: RefreshPayload): Promise<ApiResult<AuthSession>> {
  return authRequest<AuthSession>("/api/auth/refresh", payload);
}

export async function logout(payload: LogoutPayload): Promise<ApiResult<{ loggedOut: boolean }>> {
  return authRequest<{ loggedOut: boolean }>("/api/auth/logout", payload);
}

async function authRequest<T>(path: string, payload: unknown): Promise<ApiResult<T>> {
  try {
    const response = await fetch(path, asJson(payload));
    const envelope = await parseEnvelope<T>(response);
    if (!response.ok) {
      return {
        status: mapStatus(response.status),
        error: envelope?.error?.safeMessage ?? envelope?.error?.safeReason,
      };
    }
    if (!envelope?.data) {
      return {
        status: envelope?.error?.errorCode === "access_denied" ? "denied" : "failed",
        error: envelope?.error?.safeMessage,
      };
    }
    return { status: "ready", data: envelope.data };
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      return { status: "failed", error: "The request was cancelled." };
    }
    return { status: "unavailable", error: "The backend is unavailable." };
  }
}

function mapStatus(status: number): Exclude<ApiResult<never>["status"], "ready"> {
  if (status === 400) return "invalid_request";
  if (status === 401) return "unauthenticated";
  if (status === 403) return "denied";
  if (status === 404 || status === 503) return "unavailable";
  return "failed";
}

async function parseEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text();
  if (!text.trim()) {
    return null;
  }
  try {
    return JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}
