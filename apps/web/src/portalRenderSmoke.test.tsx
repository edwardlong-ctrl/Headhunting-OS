import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import App from "./App";

(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true;

type PortalSmokeCase = {
  route: string;
  expectedText: string;
};

const portalSmokeCases: PortalSmokeCase[] = [
  { route: "/owner", expectedText: "Owner / Partner" },
  { route: "/consultant", expectedText: "Unified consultant session handoff" },
  { route: "/client", expectedText: "Governed client handoff" },
  { route: "/candidate", expectedText: "Sign in to review your profile" },
  { route: "/admin", expectedText: "Admin / System" },
];

describe("five-portal render route smoke", () => {
  let container: HTMLDivElement;
  let root: Root;

  beforeEach(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    container = document.createElement("div");
    document.body.appendChild(container);
    root = createRoot(container);
  });

  afterEach(async () => {
    await act(async () => {
      root.unmount();
    });
    container.remove();
  });

  async function renderAppRoute(route: string) {
    await act(async () => {
      root.render(
        <MemoryRouter initialEntries={[route]}>
          <App />
        </MemoryRouter>,
      );
    });

    await act(async () => {
      await Promise.resolve();
    });
  }

  for (const portalCase of portalSmokeCases) {
    it(`renders ${portalCase.route} through the real App route wiring`, async () => {
      await renderAppRoute(portalCase.route);

      expect(container.textContent).toContain(portalCase.expectedText);
    });
  }
});
