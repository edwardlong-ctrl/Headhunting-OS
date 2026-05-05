import type { AuthSession } from "../api/auth";

export type ScopedPortalScope = "candidate" | "client";

export type ScopedPortalSession = AuthSession;

const STORAGE_KEYS: Record<ScopedPortalScope, string> = {
  candidate: "rto.candidatePortalSession",
  client: "rto.clientPortalSession",
};

export function loadScopedPortalSession(scope: ScopedPortalScope): ScopedPortalSession | null {
  if (typeof window === "undefined") {
    return null;
  }
  const raw = window.localStorage.getItem(STORAGE_KEYS[scope]);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<ScopedPortalSession>;
    if (!parsed.portalRole || !parsed.userAccountId || !parsed.accessToken) {
      return null;
    }
    return {
      organizationId: parsed.organizationId ?? "",
      userAccountId: parsed.userAccountId,
      displayName: parsed.displayName ?? (scope === "candidate" ? "Candidate" : "Client"),
      portalRole: parsed.portalRole,
      tokenType: parsed.tokenType ?? "Bearer",
      accessToken: parsed.accessToken,
      refreshToken: parsed.refreshToken ?? "",
      accessTokenExpiresAt: parsed.accessTokenExpiresAt ?? "",
      refreshTokenExpiresAt: parsed.refreshTokenExpiresAt ?? "",
    };
  } catch {
    return null;
  }
}

export function saveScopedPortalSession(scope: ScopedPortalScope, session: AuthSession): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(STORAGE_KEYS[scope], JSON.stringify(session));
}

export function clearScopedPortalSession(scope: ScopedPortalScope): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(STORAGE_KEYS[scope]);
}
