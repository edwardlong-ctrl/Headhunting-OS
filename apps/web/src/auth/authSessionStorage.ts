export type PortalSession = {
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

const SESSION_STORAGE_KEY = "rto.portalSession";

export function loadPortalSession(): PortalSession | null {
  if (typeof window === "undefined") {
    return null;
  }
  const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<PortalSession>;
    if (!parsed.accessToken || !parsed.portalRole) {
      return null;
    }
    return {
      organizationId: parsed.organizationId ?? "",
      userAccountId: parsed.userAccountId ?? "",
      displayName: parsed.displayName ?? "Consultant",
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

export function savePortalSession(session: PortalSession): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function clearPortalSession(): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
}

