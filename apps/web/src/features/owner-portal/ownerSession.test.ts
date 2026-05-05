import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadAccessToken, saveAccessToken } from "../../auth/accessTokenStorage";
import { loadOwnerSession, saveOwnerSession, signOutOwnerSession } from "./ownerSession";
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

function createOwnerSession(): AuthSession {
  return {
    organizationId: "org-owner",
    userAccountId: "owner-1",
    displayName: "Owner Example",
    portalRole: "owner",
    tokenType: "Bearer",
    accessToken: "owner-access-token",
    refreshToken: "owner-refresh-token",
    accessTokenExpiresAt: "2026-05-02T00:00:00Z",
    refreshTokenExpiresAt: "2026-05-03T00:00:00Z",
  };
}

describe("owner session", () => {
  beforeEach(() => {
    window.localStorage.clear();
    logoutMock.mockReset().mockResolvedValue({ status: "ready", data: { loggedOut: true } });
  });

  it("revokes the refresh token and clears owner storage on sign out", async () => {
    const session = createOwnerSession();
    saveOwnerSession(session);
    saveAccessToken(session.accessToken, "owner");

    const result = await signOutOwnerSession(session);

    expect(result).toEqual({ status: "ready", data: { signedOut: true } });
    expect(logoutMock).toHaveBeenCalledWith({ refreshToken: "owner-refresh-token" });
    expect(loadOwnerSession()).toBeNull();
    expect(loadAccessToken("owner")).toBeNull();
  });

  it("preserves owner storage when logout revocation fails", async () => {
    const session = createOwnerSession();
    saveOwnerSession(session);
    saveAccessToken(session.accessToken, "owner");
    logoutMock.mockResolvedValue({ status: "unavailable", error: "The backend is unavailable." });

    const result = await signOutOwnerSession(session);

    expect(result).toEqual({ status: "unavailable", error: "The backend is unavailable." });
    expect(loadOwnerSession()?.refreshToken).toBe("owner-refresh-token");
    expect(loadAccessToken("owner")).toBe("owner-access-token");
  });
});
