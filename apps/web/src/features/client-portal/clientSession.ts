import { logout, type AuthSession } from "../../api/auth";
import { saveAccessToken } from "../../auth/accessTokenStorage";
import {
  clearScopedPortalSession,
  loadScopedPortalSession,
  saveScopedPortalSession,
} from "../../auth/scopedPortalSessionStorage";
import type { ApiResult } from "../../api/http";

export type ClientPortalSession = AuthSession;

export function loadClientPortalSession(): ClientPortalSession | null {
  return loadScopedPortalSession("client");
}

export function saveClientPortalSession(session: AuthSession): void {
  saveScopedPortalSession("client", session);
}

export function clearClientPortalSession(): void {
  saveAccessToken("", "client");
  clearScopedPortalSession("client");
}

export async function signOutClientSession(
  session: ClientPortalSession | null,
): Promise<ApiResult<{ signedOut: boolean }>> {
  if (!session) {
    clearClientPortalSession();
    return { status: "ready", data: { signedOut: true } };
  }
  if (!session.refreshToken?.trim()) {
    return { status: "failed", error: "Client sign out requires a valid refresh token." };
  }
  const result = await logout({ refreshToken: session.refreshToken });
  if (result.status !== "ready") {
    return result;
  }
  clearClientPortalSession();
  return { status: "ready", data: { signedOut: true } };
}
