import { logout, type AuthSession } from "../../api/auth";
import { saveAccessToken } from "../../auth/accessTokenStorage";
import type { ApiResult } from "../../api/http";
import {
  clearScopedPortalSession,
  loadScopedPortalSession,
  saveScopedPortalSession,
} from "../../auth/scopedPortalSessionStorage";

export function loadOwnerSession(): AuthSession | null {
  return loadScopedPortalSession("owner");
}

export function saveOwnerSession(session: AuthSession): void {
  saveScopedPortalSession("owner", session);
}

export function clearOwnerSession(): void {
  saveAccessToken("", "owner");
  clearScopedPortalSession("owner");
}

export async function signOutOwnerSession(
  session: AuthSession | null,
): Promise<ApiResult<{ signedOut: boolean }>> {
  if (!session) {
    clearOwnerSession();
    return { status: "ready", data: { signedOut: true } };
  }
  if (!session.refreshToken?.trim()) {
    return { status: "failed", error: "Owner sign out requires a valid refresh token." };
  }
  const result = await logout({ refreshToken: session.refreshToken });
  if (result.status !== "ready") {
    return result;
  }
  clearOwnerSession();
  return { status: "ready", data: { signedOut: true } };
}
