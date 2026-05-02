import { clearPortalSession, loadPortalSession, savePortalSession } from "./authSessionStorage";

export type AccessTokenScope = "consultant" | "client";

const CLIENT_ACCESS_TOKEN_STORAGE_KEY = "rto.clientAccessToken";
const LEGACY_ACCESS_TOKEN_STORAGE_KEY = "rto.portalAccessToken";

export function loadAccessToken(scope: AccessTokenScope): string | null {
  if (scope === "consultant") {
    const session = loadPortalSession();
    return normalizeAccessToken(session?.accessToken ?? null);
  }
  if (typeof window === "undefined") {
    return null;
  }
  return normalizeAccessToken(window.localStorage.getItem(CLIENT_ACCESS_TOKEN_STORAGE_KEY));
}

export function saveAccessToken(accessToken: string, scope: AccessTokenScope): void {
  if (typeof window === "undefined") {
    return;
  }
  const normalized = normalizeAccessToken(accessToken);
  if (!normalized) {
    if (scope === "consultant") {
      clearPortalSession();
    } else {
      window.localStorage.removeItem(CLIENT_ACCESS_TOKEN_STORAGE_KEY);
      window.localStorage.removeItem(LEGACY_ACCESS_TOKEN_STORAGE_KEY);
    }
    return;
  }
  if (scope === "consultant") {
    const current = loadPortalSession();
    if (current) {
      savePortalSession({
        ...current,
        accessToken: normalized,
      });
    }
    return;
  }
  window.localStorage.setItem(CLIENT_ACCESS_TOKEN_STORAGE_KEY, normalized);
  window.localStorage.removeItem(LEGACY_ACCESS_TOKEN_STORAGE_KEY);
}

function normalizeAccessToken(accessToken: string | null | undefined): string | null {
  const normalized = accessToken?.trim().replace(/^Bearer\s+/i, "") ?? "";
  return normalized.length > 0 ? normalized : null;
}
