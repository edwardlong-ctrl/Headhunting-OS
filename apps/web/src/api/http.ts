import {
  type AccessTokenScope,
  loadAccessToken,
  saveAccessToken,
} from "../auth/accessTokenStorage";
import {
  clearPortalSession,
  loadPortalSession,
  savePortalSession,
} from "../auth/authSessionStorage";
import {
  clearScopedPortalSession,
  loadScopedPortalSession,
  saveScopedPortalSession,
} from "../auth/scopedPortalSessionStorage";

export type ApiError = {
  errorCode: string;
  safeReason: string;
  safeMessage: string;
};

export type ApiEnvelope<T> =
  | { data: T; error: null }
  | { data: null; error: ApiError };

export type ApiResult<T> =
  | { status: "ready"; data: T }
  | { status: "unauthenticated" | "denied" | "invalid_request" | "unavailable" | "failed"; error?: string };

type ErrorStatus = Exclude<ApiResult<never>["status"], "ready">;

function mapStatus(status: number): ErrorStatus {
  if (status === 400) return "invalid_request";
  if (status === 401) return "unauthenticated";
  if (status === 403) return "denied";
  if (status === 404 || status === 503) return "unavailable";
  return "failed";
}

export async function apiRequest<T>(
  input: string,
  init?: RequestInit,
  scope: AccessTokenScope = "consultant",
): Promise<ApiResult<T>> {
  try {
    let response = await fetchWithScopeToken(input, init, loadAccessToken(scope));
    if (response.status === 401) {
      const refreshedToken = await refreshScopeSession(scope);
      if (refreshedToken) {
        response = await fetchWithScopeToken(input, init, refreshedToken);
      }
    }

    const envelope = await parseEnvelope<T>(response);
    if (!response.ok) {
      return {
        status: mapStatus(response.status),
        error: envelope?.error?.safeMessage ?? envelope?.error?.safeReason,
      };
    }
    if (!envelope) {
      return {
        status: "failed",
        error: `Unexpected empty response from ${input}.`,
      };
    }
    if (!envelope.data) {
      return {
        status: envelope.error?.errorCode === "access_denied" ? "denied" : "failed",
        error: envelope.error?.safeMessage,
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

async function fetchWithScopeToken(
  input: string,
  init: RequestInit | undefined,
  accessToken: string | null,
): Promise<Response> {
  return fetch(input, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
  });
}

async function refreshScopeSession(scope: AccessTokenScope): Promise<string | null> {
  const currentSession = loadSessionForScope(scope);
  if (!currentSession?.refreshToken) {
    clearSessionForScope(scope);
    return null;
  }
  try {
    const response = await fetch("/api/auth/refresh", asMethodJson("POST", {
      refreshToken: currentSession.refreshToken,
    }));
    const envelope = await parseEnvelope<AuthRefreshSession>(response);
    if (!response.ok || !envelope?.data) {
      clearSessionForScope(scope);
      return null;
    }
    saveSessionForScope(scope, envelope.data);
    saveAccessToken(envelope.data.accessToken, scope);
    return envelope.data.accessToken;
  } catch {
    clearSessionForScope(scope);
    return null;
  }
}

type AuthRefreshSession = {
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

function loadSessionForScope(scope: AccessTokenScope): AuthRefreshSession | null {
  if (scope === "consultant") {
    return loadPortalSession();
  }
  if (scope === "candidate") {
    return loadScopedPortalSession("candidate");
  }
  return loadScopedPortalSession("client");
}

function saveSessionForScope(scope: AccessTokenScope, session: AuthRefreshSession): void {
  if (scope === "consultant") {
    savePortalSession(session);
    return;
  }
  if (scope === "candidate") {
    saveScopedPortalSession("candidate", session);
    return;
  }
  saveScopedPortalSession("client", session);
}

function clearSessionForScope(scope: AccessTokenScope): void {
  saveAccessToken("", scope);
  if (scope === "consultant") {
    clearPortalSession();
    return;
  }
  if (scope === "candidate") {
    clearScopedPortalSession("candidate");
    return;
  }
  clearScopedPortalSession("client");
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

export function asJson(body: unknown): RequestInit {
  return asMethodJson("POST", body);
}

export function asMethodJson(method: "POST" | "PUT", body: unknown): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  };
}

export type PagedResult<T> = {
  items: T[];
  totalCount: number;
  limit: number;
  offset: number;
  hasMore: boolean;
};
