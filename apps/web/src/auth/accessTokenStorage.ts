import { clearPortalSession, loadPortalSession, savePortalSession } from "./authSessionStorage";

const ACCESS_TOKEN_STORAGE_KEY = "rto.portalAccessToken";

export function loadAccessToken(): string | null {
  const session = loadPortalSession();
  if (session?.accessToken) {
    return session.accessToken;
  }
  if (typeof window === "undefined") {
    return null;
  }
  const value = window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
  return value && value.trim().length > 0 ? value.trim() : null;
}

export function saveAccessToken(accessToken: string): void {
  if (typeof window === "undefined") {
    return;
  }
  const normalized = accessToken.trim().replace(/^Bearer\s+/i, "");
  if (normalized.length === 0) {
    window.localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
    clearPortalSession();
    return;
  }
  window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, normalized);
  const current = loadPortalSession();
  if (current) {
    savePortalSession({
      ...current,
      accessToken: normalized,
    });
  }
}
