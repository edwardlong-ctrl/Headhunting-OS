import { loadAccessToken, saveAccessToken } from "./accessTokenStorage";
import {
  clearPortalSession,
  loadPortalSession,
  savePortalSession,
  type PortalSession,
} from "./authSessionStorage";
import {
  CONSULTANT_WORKFLOW_ENTITY_TYPE_OPTIONS,
  canSaveShortlistBuilder,
  describeWorkflowPageWindow,
  isShortlistBuilderEditable,
} from "../features/consultant-portal/consultantPortalUtils";

function createSession(overrides: Partial<PortalSession> = {}): PortalSession {
  return {
    organizationId: "org-1",
    userAccountId: "user-1",
    displayName: "Consultant Example",
    portalRole: "consultant",
    tokenType: "Bearer",
    accessToken: "consultant-token",
    refreshToken: "refresh-token",
    accessTokenExpiresAt: "2026-05-02T00:00:00Z",
    refreshTokenExpiresAt: "2026-05-03T00:00:00Z",
    ...overrides,
  };
}

describe("auth isolation", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("keeps consultant and client tokens isolated", () => {
    savePortalSession(createSession());
    saveAccessToken("client-token", "client");

    expect(loadAccessToken("consultant")).toBe("consultant-token");
    expect(loadAccessToken("client")).toBe("client-token");
    expect(loadPortalSession()?.accessToken).toBe("consultant-token");
  });

  it("updates consultant token through the consultant session only", () => {
    savePortalSession(createSession());

    saveAccessToken("updated-consultant-token", "consultant");

    expect(loadPortalSession()?.accessToken).toBe("updated-consultant-token");
    expect(window.localStorage.getItem("rto.clientAccessToken")).toBeNull();
  });

  it("clears only consultant storage when consultant token is removed", () => {
    savePortalSession(createSession());
    saveAccessToken("client-token", "client");

    saveAccessToken("", "consultant");

    expect(loadPortalSession()).toBeNull();
    expect(loadAccessToken("client")).toBe("client-token");
  });
});

describe("shortlist builder boundary", () => {
  it("allows only builder statuses", () => {
    expect(isShortlistBuilderEditable("draft")).toBe(true);
    expect(isShortlistBuilderEditable("ready_for_review")).toBe(true);
    expect(isShortlistBuilderEditable("sent_to_client")).toBe(false);
  });

  it("prevents saving when the shortlist is already beyond builder scope", () => {
    expect(canSaveShortlistBuilder("draft", "ready_for_review", "Alpha")).toBe(true);
    expect(canSaveShortlistBuilder("sent_to_client", "ready_for_review", "Alpha")).toBe(false);
    expect(canSaveShortlistBuilder("draft", "sent_to_client", "Alpha")).toBe(false);
    expect(canSaveShortlistBuilder("draft", "draft", "   ")).toBe(false);
  });

  afterEach(() => {
    clearPortalSession();
  });
});

describe("workflow portal helpers", () => {
  it("exposes only backend-supported workflow entity filter values", () => {
    expect(CONSULTANT_WORKFLOW_ENTITY_TYPE_OPTIONS.map((item) => item.value)).toEqual([
      "CANDIDATE",
      "JOB",
      "SHORTLIST",
      "INFORMATION_PACKET",
      "SOURCE_ITEM",
    ]);
  });

  it("describes the workflow page window without inventing a total count", () => {
    expect(describeWorkflowPageWindow(0, 0)).toBe("Showing 0 events in this window");
    expect(describeWorkflowPageWindow(10, 0)).toBe("Showing 1-10 events in this window");
    expect(describeWorkflowPageWindow(4, 20)).toBe("Showing 21-24 events in this window");
  });
});
