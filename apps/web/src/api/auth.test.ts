import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadAccessToken, saveAccessToken } from "../auth/accessTokenStorage";
import { login, logout, refresh } from "./auth";

describe("auth logout", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    window.localStorage.clear();
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  it("posts logout without attaching portal authorization state", async () => {
    saveAccessToken("consultant-token", "consultant");
    saveAccessToken("owner-token", "owner");
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      data: { loggedOut: true },
      error: null,
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));

    const result = await logout({ refreshToken: "refresh-token" });

    expect(result).toEqual({ status: "ready", data: { loggedOut: true } });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: "refresh-token" }),
    });
  });

  it("preserves stored tokens when logout returns 401", async () => {
    saveAccessToken("owner-token", "owner");
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      data: null,
      error: {
        errorCode: "invalid_refresh_token",
        safeReason: "invalid_refresh_token",
        safeMessage: "Refresh token is invalid.",
      },
    }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    }));

    const result = await logout({ refreshToken: "bad-refresh-token" });

    expect(result).toEqual({ status: "unauthenticated", error: "Refresh token is invalid." });
    expect(loadAccessToken("owner")).toBe("owner-token");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});

describe("auth login and refresh", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    window.localStorage.clear();
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  it("posts login without attaching portal authorization state", async () => {
    saveAccessToken("consultant-token", "consultant");
    saveAccessToken("owner-token", "owner");
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      data: {
        organizationId: "org-1",
        userAccountId: "user-1",
        displayName: "Owner Example",
        portalRole: "owner",
        tokenType: "Bearer",
        accessToken: "new-owner-token",
        refreshToken: "new-owner-refresh",
        accessTokenExpiresAt: "2026-05-02T00:00:00Z",
        refreshTokenExpiresAt: "2026-05-03T00:00:00Z",
      },
      error: null,
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));

    const result = await login({
      email: "owner@example.com",
      password: "secret",
      portalRole: "owner",
    });

    expect(result.status).toBe("ready");
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: "owner@example.com",
        password: "secret",
        portalRole: "owner",
      }),
    });
  });

  it("preserves stored tokens when refresh returns 401", async () => {
    saveAccessToken("owner-token", "owner");
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      data: null,
      error: {
        errorCode: "invalid_refresh_token",
        safeReason: "invalid_refresh_token",
        safeMessage: "Refresh token is invalid.",
      },
    }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    }));

    const result = await refresh({ refreshToken: "bad-refresh-token" });

    expect(result).toEqual({ status: "unauthenticated", error: "Refresh token is invalid." });
    expect(loadAccessToken("owner")).toBe("owner-token");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: "bad-refresh-token" }),
    });
  });
});
