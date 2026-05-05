import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadAccessToken, saveAccessToken } from "../../auth/accessTokenStorage";
import { loadCandidateSession, saveCandidateSession, signOutCandidateSession } from "./candidateSession";
import type { AuthSession } from "../../api/auth";

const { logoutMock } = vi.hoisted(() => ({
  logoutMock: vi.fn(),
}));

vi.mock("../../api/auth", async () => {
  const actual = await vi.importActual<typeof import("../../api/auth")>("../../api/auth");
  return {
    ...actual,
    logout: logoutMock,
  };
});

function createCandidateSession(): AuthSession {
  return {
    organizationId: "org-candidate",
    userAccountId: "candidate-1",
    displayName: "Candidate Example",
    portalRole: "candidate",
    tokenType: "Bearer",
    accessToken: "candidate-access-token",
    refreshToken: "candidate-refresh-token",
    accessTokenExpiresAt: "2026-05-02T00:00:00Z",
    refreshTokenExpiresAt: "2026-05-03T00:00:00Z",
  };
}

describe("candidate session", () => {
  beforeEach(() => {
    window.localStorage.clear();
    logoutMock.mockReset().mockResolvedValue({ status: "ready", data: { loggedOut: true } });
  });

  it("clears candidate storage after a successful sign out", async () => {
    const session = createCandidateSession();
    saveCandidateSession(session);
    saveAccessToken(session.accessToken, "candidate");

    const result = await signOutCandidateSession(session);

    expect(result).toEqual({ status: "ready", data: { signedOut: true } });
    expect(loadCandidateSession()).toBeNull();
    expect(loadAccessToken("candidate")).toBeNull();
  });

  it("preserves candidate storage when logout fails", async () => {
    const session = createCandidateSession();
    saveCandidateSession(session);
    saveAccessToken(session.accessToken, "candidate");
    logoutMock.mockResolvedValue({ status: "unavailable", error: "The backend is unavailable." });

    const result = await signOutCandidateSession(session);

    expect(result).toEqual({ status: "unavailable", error: "The backend is unavailable." });
    expect(loadCandidateSession()?.refreshToken).toBe("candidate-refresh-token");
    expect(loadAccessToken("candidate")).toBe("candidate-access-token");
  });
});
