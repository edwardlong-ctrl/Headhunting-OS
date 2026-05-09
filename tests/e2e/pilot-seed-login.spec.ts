import { expect, type Page, test } from "@playwright/test";

const PILOT_ORGANIZATION_ID =
  process.env.RTO_PILOT_ORG_ID ?? "00000000-0000-0000-0000-000000380001";
const PILOT_PASSWORD = process.env.RTO_PILOT_PASSWORD ?? "PilotData38!";

type PilotPortalAccount = {
  portal: "consultant" | "client" | "candidate" | "owner" | "admin";
  path: string;
  email: string;
  submitButton: string;
  signedInEvidence: RegExp;
  apiBackedLandmark: RegExp;
};

const pilotAccounts: PilotPortalAccount[] = [
  {
    portal: "consultant",
    path: "/consultant",
    email: "consultant@pilot.example.test",
    submitButton: "Enter Consultant Portal",
    signedInEvidence: /consultant session active for 00000000-0000-0000-0000-000000380001/i,
    apiBackedLandmark: /Unified operating board/i,
  },
  {
    portal: "client",
    path: "/client",
    email: "client@pilot.example.test",
    submitButton: "Enter Client Portal",
    signedInEvidence: /client session active for 00000000-0000-0000-0000-000000380001/i,
    apiBackedLandmark: /Governed hiring intake workspace/i,
  },
  {
    portal: "candidate",
    path: "/candidate",
    email: "candidate@pilot.example.test",
    submitButton: "Sign in",
    signedInEvidence: /Welcome back, Pilot Talent 01/i,
    apiBackedLandmark: /Welcome back, Pilot Talent 01/i,
  },
  {
    portal: "owner",
    path: "/owner",
    email: "owner@pilot.example.test",
    submitButton: "Enter Owner Portal",
    signedInEvidence: /owner session active for 00000000-0000-0000-0000-000000380001/i,
    apiBackedLandmark: /Owner Dashboard/i,
  },
  {
    portal: "admin",
    path: "/admin",
    email: "admin@pilot.example.test",
    submitButton: "Enter Admin Portal",
    signedInEvidence: /admin session active for 00000000-0000-0000-0000-000000380001/i,
    apiBackedLandmark: /Review quality/i,
  },
];

test.describe("Task 42 pilot seed browser entry", () => {
  for (const account of pilotAccounts) {
    test(`${account.portal} seed account signs in through the real portal UI`, async ({ page }) => {
      await clearBrowserState(page);
      await page.goto(account.path);

      await fillFirstMatchingInput(page, /Organization ID/i, PILOT_ORGANIZATION_ID);
      await fillFirstMatchingInput(page, /Work email|Email/i, account.email);
      await fillFirstMatchingInput(page, /Password/i, PILOT_PASSWORD);
      await page.getByRole("button", { name: account.submitButton }).click();

      await expect(page.getByText(account.signedInEvidence)).toBeVisible();
      await expect(page.getByText(account.apiBackedLandmark).first()).toBeVisible();
      await expect(page.getByText(/backend is unavailable|sign in failed|access denied/i)).toHaveCount(0);
    });
  }
});

async function clearBrowserState(page: Page): Promise<void> {
  await page.context().clearCookies();
  await page.goto("/");
  await page.evaluate(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
  });
}

async function fillFirstMatchingInput(page: Page, label: RegExp, value: string): Promise<void> {
  await page.getByLabel(label).first().fill(value);
}
