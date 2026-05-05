import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadAccessToken, saveAccessToken } from "../../auth/accessTokenStorage";
import { loadClientPortalSession, saveClientPortalSession, signOutClientSession } from "./clientSession";
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

function createClientSession(): AuthSession {
  return {
    organizationId: "org-client",
    userAccountId: "client-1",
    displayName: "Client Example",
    portalRole: "client",
    tokenType: "Bearer",
    accessToken: "client-access-token",
    refreshToken: "client-refresh-token",
    accessTokenExpiresAt: "2026-05-02T00:00:00Z",
    refreshTokenExpiresAt: "2026-05-03T00:00:00Z",
  };
}

describe("client session", () => {
  beforeEach(() => {
    window.localStorage.clear();
    logoutMock.mockReset().mockResolvedValue({ status: "ready", data: { loggedOut: true } });
  });

  it("clears client storage after a successful sign out", async () => {
    const session = createClientSession();
    saveClientPortalSession(session);
    saveAccessToken(session.accessToken, "client");

    const result = await signOutClientSession(session);

    expect(result).toEqual({ status: "ready", data: { signedOut: true } });
    expect(loadClientPortalSession()).toBeNull();
    expect(loadAccessToken("client")).toBeNull();
  });

  it("preserves client storage when logout fails", async () => {
    const session = createClientSession();
    saveClientPortalSession(session);
    saveAccessToken(session.accessToken, "client");
    logoutMock.mockResolvedValue({ status: "unavailable", error: "The backend is unavailable." });

    const result = await signOutClientSession(session);

    expect(result).toEqual({ status: "unavailable", error: "The backend is unavailable." });
    expect(loadClientPortalSession()?.refreshToken).toBe("client-refresh-token");
    expect(loadAccessToken("client")).toBe("client-access-token");
  });
});
