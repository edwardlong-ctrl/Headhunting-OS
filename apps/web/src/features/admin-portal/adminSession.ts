import { logout, type AuthSession } from "../../api/auth";
import type { ApiResult } from "../../api/http";
import { saveAccessToken } from "../../auth/accessTokenStorage";
import {
  clearScopedPortalSession,
  loadScopedPortalSession,
  saveScopedPortalSession,
} from "../../auth/scopedPortalSessionStorage";

export function loadAdminSession(): AuthSession | null {
  return loadScopedPortalSession("admin");
}

export function saveAdminSession(session: AuthSession): void {
  saveScopedPortalSession("admin", session);
}

export function clearAdminSession(): void {
  saveAccessToken("", "admin");
  clearScopedPortalSession("admin");
}

export async function signOutAdminSession(
  session: AuthSession | null,
): Promise<ApiResult<{ signedOut: boolean }>> {
  if (!session) {
    clearAdminSession();
    return { status: "ready", data: { signedOut: true } };
  }
  if (!session.refreshToken?.trim()) {
    return { status: "failed", error: "Admin sign out requires a valid refresh token." };
  }
  const result = await logout({ refreshToken: session.refreshToken });
  if (result.status !== "ready") {
    return result;
  }
  clearAdminSession();
  return { status: "ready", data: { signedOut: true } };
}
