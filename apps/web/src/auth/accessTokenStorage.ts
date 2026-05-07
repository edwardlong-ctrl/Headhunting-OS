import { clearPortalSession, loadPortalSession, savePortalSession } from "./authSessionStorage";
import {
  clearScopedPortalSession,
  loadScopedPortalSession,
  saveScopedPortalSession,
} from "./scopedPortalSessionStorage";

export type AccessTokenScope = "consultant" | "client" | "candidate" | "owner" | "admin";

const CLIENT_ACCESS_TOKEN_STORAGE_KEY = "rto.clientAccessToken";
const CANDIDATE_ACCESS_TOKEN_STORAGE_KEY = "rto.candidateAccessToken";
const OWNER_ACCESS_TOKEN_STORAGE_KEY = "rto.ownerAccessToken";
const ADMIN_ACCESS_TOKEN_STORAGE_KEY = "rto.adminAccessToken";
const LEGACY_ACCESS_TOKEN_STORAGE_KEY = "rto.portalAccessToken";

export function loadAccessToken(scope: AccessTokenScope): string | null {
  if (scope === "consultant") {
    const session = loadPortalSession();
    return normalizeAccessToken(session?.accessToken ?? null);
  }
  if (typeof window === "undefined") {
    return null;
  }
  if (scope === "candidate") {
    return normalizeAccessToken(
      loadScopedPortalSession("candidate")?.accessToken
        ?? window.localStorage.getItem(CANDIDATE_ACCESS_TOKEN_STORAGE_KEY),
    );
  }
  if (scope === "owner") {
    return normalizeAccessToken(
      loadScopedPortalSession("owner")?.accessToken
        ?? window.localStorage.getItem(OWNER_ACCESS_TOKEN_STORAGE_KEY),
    );
  }
  if (scope === "admin") {
    return normalizeAccessToken(
      loadScopedPortalSession("admin")?.accessToken
        ?? window.localStorage.getItem(ADMIN_ACCESS_TOKEN_STORAGE_KEY),
    );
  }
  return normalizeAccessToken(
    loadScopedPortalSession("client")?.accessToken
      ?? window.localStorage.getItem(CLIENT_ACCESS_TOKEN_STORAGE_KEY),
  );
}

export function saveAccessToken(accessToken: string, scope: AccessTokenScope): void {
  if (typeof window === "undefined") {
    return;
  }
  const normalized = normalizeAccessToken(accessToken);
  if (!normalized) {
    if (scope === "consultant") {
      clearPortalSession();
    } else if (scope === "candidate") {
      clearScopedPortalSession("candidate");
      window.localStorage.removeItem(CANDIDATE_ACCESS_TOKEN_STORAGE_KEY);
    } else if (scope === "owner") {
      clearScopedPortalSession("owner");
      window.localStorage.removeItem(OWNER_ACCESS_TOKEN_STORAGE_KEY);
    } else if (scope === "admin") {
      clearScopedPortalSession("admin");
      window.localStorage.removeItem(ADMIN_ACCESS_TOKEN_STORAGE_KEY);
    } else {
      clearScopedPortalSession("client");
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
  if (scope === "candidate") {
    const current = loadScopedPortalSession("candidate");
    if (current) {
      saveScopedPortalSession("candidate", {
        ...current,
        accessToken: normalized,
      });
    }
    window.localStorage.setItem(CANDIDATE_ACCESS_TOKEN_STORAGE_KEY, normalized);
    return;
  }
  if (scope === "owner") {
    const current = loadScopedPortalSession("owner");
    if (current) {
      saveScopedPortalSession("owner", {
        ...current,
        accessToken: normalized,
      });
    }
    window.localStorage.setItem(OWNER_ACCESS_TOKEN_STORAGE_KEY, normalized);
    return;
  }
  if (scope === "admin") {
    const current = loadScopedPortalSession("admin");
    if (current) {
      saveScopedPortalSession("admin", {
        ...current,
        accessToken: normalized,
      });
    }
    window.localStorage.setItem(ADMIN_ACCESS_TOKEN_STORAGE_KEY, normalized);
    return;
  }
  const current = loadScopedPortalSession("client");
  if (current) {
    saveScopedPortalSession("client", {
      ...current,
      accessToken: normalized,
    });
  }
  window.localStorage.setItem(CLIENT_ACCESS_TOKEN_STORAGE_KEY, normalized);
  window.localStorage.removeItem(LEGACY_ACCESS_TOKEN_STORAGE_KEY);
}

function normalizeAccessToken(accessToken: string | null | undefined): string | null {
  const normalized = accessToken?.trim().replace(/^Bearer\s+/i, "") ?? "";
  return normalized.length > 0 ? normalized : null;
}
