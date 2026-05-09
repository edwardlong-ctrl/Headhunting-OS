import { defineConfig, devices } from "@playwright/test";

const webPort = Number(process.env.RTO_E2E_WEB_PORT ?? "4192");
const apiPort = Number(process.env.RTO_E2E_API_PORT ?? "8092");

export default defineConfig({
  testDir: ".",
  timeout: 60_000,
  expect: {
    timeout: 15_000,
  },
  fullyParallel: false,
  reporter: [["list"], ["html", { outputFolder: "test-results/pilot-e2e-report", open: "never" }]],
  use: {
    baseURL: `http://127.0.0.1:${webPort}`,
    trace: "retain-on-failure",
  },
  webServer: {
    command: `VITE_API_PROXY_TARGET=http://127.0.0.1:${apiPort} npm --workspace @rto/web run dev -- --host 127.0.0.1 --port ${webPort} --strictPort`,
    url: `http://127.0.0.1:${webPort}`,
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
