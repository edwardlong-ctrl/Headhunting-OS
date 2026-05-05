import { logout, type AuthSession } from "../../api/auth";
import { saveAccessToken } from "../../auth/accessTokenStorage";
import {
  clearScopedPortalSession,
  loadScopedPortalSession,
  saveScopedPortalSession,
} from "../../auth/scopedPortalSessionStorage";
import type { ApiResult } from "../../api/http";

export type CandidateSession = AuthSession;

export function loadCandidateSession(): CandidateSession | null {
  return loadScopedPortalSession("candidate");
}

export function saveCandidateSession(session: AuthSession): void {
  saveScopedPortalSession("candidate", session);
}

export function clearCandidateSession(): void {
  clearScopedPortalSession("candidate");
  saveAccessToken("", "candidate");
}

export async function signOutCandidateSession(
  session: CandidateSession | null,
): Promise<ApiResult<{ signedOut: boolean }>> {
  if (!session) {
    clearCandidateSession();
    return { status: "ready", data: { signedOut: true } };
  }
  if (!session.refreshToken?.trim()) {
    return { status: "failed", error: "Candidate sign out requires a valid refresh token." };
  }
  const result = await logout({ refreshToken: session.refreshToken });
  if (result.status !== "ready") {
    return result;
  }
  clearCandidateSession();
  return { status: "ready", data: { signedOut: true } };
}
