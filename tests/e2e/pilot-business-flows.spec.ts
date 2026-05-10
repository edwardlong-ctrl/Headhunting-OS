import { expect, type APIRequestContext, type Page, test } from "@playwright/test";

const PILOT_ORGANIZATION_ID =
  process.env.RTO_PILOT_ORG_ID ?? "00000000-0000-0000-0000-000000380001";
const PILOT_PASSWORD = process.env.RTO_PILOT_PASSWORD ?? "PilotData38!";
const CANDIDATE_PORTAL_CANDIDATE_ID =
  process.env.RTO_PILOT_E2E_CANDIDATE_ID ?? "1c72996e-8668-3492-98a8-4b276002bf57";

type PortalRole = "consultant" | "client" | "candidate";

type ApiEnvelope<T> =
  | { data: T; error: null }
  | { data: null; error: { errorCode: string; safeMessage?: string; safeReason?: string } };

type AuthSession = {
  accessToken: string;
  displayName: string;
  userAccountId: string;
};

type PagedResult<T> = {
  items: T[];
};

type ConsultantCandidateDetail = {
  candidateId: string;
  currentProfileId: string | null;
};

type ConsultantCompanyDetail = {
  companyId: string;
  version: number;
};

type ConsultantJobDetail = {
  jobId: string;
  version: number;
  companyId: string;
  title: string;
  description: string | null;
  location: string | null;
  seniorityBand: string | null;
  roleFamily: string | null;
  employmentType: string | null;
  compensation: string | null;
  commercialTerms: string | null;
  status: string;
  industryPackKey: string | null;
};

type ConsultantShortlistDetail = {
  shortlistId: string;
  version: number;
  jobId: string;
  title: string;
  status: string;
  cards: Array<{
    cardId: string;
    anonymousCandidateCardId: string;
    anonymousCandidateRef: string;
    status: string;
  }>;
  preSendChecks: Array<{ code: string; passed: boolean }>;
};

const accounts: Record<PortalRole, { path: string; email: string; submitButton: string; signedIn: RegExp }> = {
  consultant: {
    path: "/consultant",
    email: "consultant@pilot.example.test",
    submitButton: "Enter Consultant Portal",
    signedIn: /consultant session active/i,
  },
  client: {
    path: "/client",
    email: "client@pilot.example.test",
    submitButton: "Enter Client Portal",
    signedIn: /client session active/i,
  },
  candidate: {
    path: "/candidate",
    email: "candidate@pilot.example.test",
    submitButton: "Sign in",
    signedIn: /Welcome back, Pilot Talent 01/i,
  },
};

const auth: Partial<Record<PortalRole, AuthSession>> = {};
const flow = {
  candidateId: CANDIDATE_PORTAL_CANDIDATE_ID,
  candidateProfileId: "",
  companyId: "",
  jobId: "",
  shortlistId: "",
  shortlistCardId: "",
  consentRecordRef: "",
  interviewId: "",
};

test.describe.serial("Task 42 pilot business browser E2E flows S01-S08", () => {
  test.beforeAll(async ({ request }) => {
    auth.consultant = await loginApi(request, "consultant");
    auth.client = await loginApi(request, "client");
    auth.candidate = await loginApi(request, "candidate");

    const candidate = await api<ConsultantCandidateDetail>(
      request,
      auth.consultant.accessToken,
      "GET",
      `/api/consultant/candidates/${flow.candidateId}`,
    );
    expect(candidate.currentProfileId, "candidate seed account must resolve to a current profile").toBeTruthy();
    flow.candidateProfileId = candidate.currentProfileId ?? "";
  });

  test("S01 consultant uploads CV, reviews AI claims, and publishes through governed intake", async ({ page }) => {
    await signInThroughUi(page, "consultant");
    await page.goto("/consultant/intake/upload/candidate");

    await page.getByLabel(/Title/i).fill(`S01 pilot CV ${Date.now()}`);
    await page.getByLabel(/Document/i).setInputFiles({
      name: "s01-pilot-candidate.txt",
      mimeType: "text/plain",
      buffer: Buffer.from([
        "Pilot S01 Candidate",
        "Role family: ASIC verification",
        "Skills: SystemVerilog, UVM, PCIe, low-power verification",
        "Location: Shanghai",
        "Salary expectation: 780k RMB",
        "WeChat note: can take a look at suitable semiconductor opportunities.",
      ].join("\n")),
    });
    await page.getByRole("button", { name: "Upload document" }).click();
    await expect(page.getByText(/Upload result/i)).toBeVisible();
    await page.getByRole("button", { name: /Run extract and open review/i }).click();

    await expect(page.getByRole("heading", { name: "Governed clean facts" })).toBeVisible();
    await expect(page.getByText(/Source & reasoning/i).first()).toBeVisible();
    await page.getByLabel(/Bulk approve low-risk fields/i).first().check();
    await page.getByRole("button", { name: /Approve \/ Next/i }).first().click();
    await expect(page.getByText(/ready_for_publish/i).first()).toBeVisible();
    await page.getByRole("button", { name: /Publish candidate path/i }).click();
    await expect(page.getByText(/Publish result/i)).toBeVisible();
    await expect(page.getByText(/Canonical write statuses/i)).toBeVisible();
  });

  test("S02 client submits job context and consultant activates the job with scorecard and terms", async ({ page, request }) => {
    const companyName = `S02 Pilot Client ${Date.now()}`;
    const company = await api<ConsultantCompanyDetail>(
      request,
      auth.client.accessToken,
      "POST",
      "/api/client/company-profile",
      {
        name: companyName,
        displayName: companyName,
        industry: "semiconductor",
        headquartersLocation: "Shanghai",
        sizeBand: "500-1000",
      },
    );
    flow.companyId = company.companyId;

    const clientJob = await api<ConsultantJobDetail>(
      request,
      auth.client.accessToken,
      "POST",
      "/api/client/jobs",
      {
        companyId: flow.companyId,
        title: `S02 Staff ASIC Verification Lead ${Date.now()}`,
        description: "Lead UVM, SoC verification, PCIe validation, and verification planning.",
        location: "Shanghai",
        compensation: "700k-950k RMB",
        commercialTerms: "{\"feeRate\":\"25%\",\"replacementDays\":90}",
        clarificationQuestions: ["Confirm team size", "Confirm low-power verification depth"],
      },
    );
    flow.jobId = clientJob.jobId;
    await api(request, auth.client.accessToken, "POST", `/api/client/jobs/${flow.jobId}/clarification`, {
      clarificationAnswers: ["Team size is 12.", "UPF and low-power verification are mandatory."],
      commercialTerms: "{\"feeRate\":\"25%\",\"replacementDays\":90}",
    });

    let consultantJob = await api<ConsultantJobDetail>(
      request,
      auth.consultant.accessToken,
      "GET",
      `/api/consultant/jobs/${flow.jobId}`,
    );
    consultantJob = await api<ConsultantJobDetail>(
      request,
      auth.consultant.accessToken,
      "PUT",
      `/api/consultant/jobs/${flow.jobId}`,
      {
        companyId: consultantJob.companyId,
        version: consultantJob.version,
        title: consultantJob.title,
        description: consultantJob.description,
        location: consultantJob.location,
        seniorityBand: "staff",
        roleFamily: "ASIC verification",
        employmentType: "full_time",
        compensation: consultantJob.compensation,
        commercialTerms: "{\"feeRate\":\"25%\",\"replacementDays\":90,\"approval\":\"consultant_confirmed\"}",
        status: consultantJob.status,
        industryPackKey: "semiconductor",
      },
    );
    await api(request, auth.consultant.accessToken, "POST", `/api/consultant/jobs/${flow.jobId}/requirements`, {
      requirementType: "must_have",
      label: "UVM verification leadership",
      importance: "high",
      detail: "Must have led SoC verification plans and UVM environments.",
      sortOrder: 0,
    });
    await api(request, auth.consultant.accessToken, "POST", `/api/consultant/jobs/${flow.jobId}/scorecard`, {
      dimensions: JSON.stringify({
        dimensions: [
          { label: "Technical fit", requirementSummary: "UVM, SystemVerilog, PCIe, low-power verification" },
          { label: "Leadership fit", requirementSummary: "Owns verification plan and cross-team reviews" },
        ],
      }),
      scoringGuidance: "Prioritize evidence-backed semiconductor verification experience.",
      status: "confirmed",
    });
    await api<ConsultantJobDetail>(
      request,
      auth.consultant.accessToken,
      "POST",
      `/api/consultant/jobs/${flow.jobId}/activate`,
      { reason: "Task 42 S02 activation evidence" },
    );

    await signInThroughUi(page, "consultant");
    await page.goto(`/consultant/jobs/${flow.jobId}/intake`);
    await expect(page.getByText(/Job intake review/i)).toBeVisible();
    await expect(page.getByRole("heading", { name: "Activation gate" })).toBeVisible();
    await expect(page.getByText("Activation allowed")).toBeVisible();
    await expect(page.getByText("Job is activated.")).toBeVisible();
    await page.goto(`/consultant/jobs/${flow.jobId}`);
    await expect(page.getByText(/Structured placeholder captured/i)).toBeVisible();
    await expect(page.getByText("Semiconductor (production)")).toBeVisible();
  });

  test("S03 consultant generates an evidence-backed MatchReport with score-cap metadata", async ({ page }) => {
    await signInThroughUi(page, "consultant");
    await page.goto(`/consultant/jobs/${flow.jobId}/matching`);

    await expect(page.getByRole("heading", { name: "Matching Console" })).toBeVisible();
    const candidateSelect = page.getByRole("combobox", { name: "Candidate" });
    await expect(candidateSelect).toBeVisible();
    const availableCandidateId = await candidateSelect.locator("option").evaluateAll((options) => {
      const option = options.find((item) => item.textContent?.includes("available")) as HTMLOptionElement | undefined;
      return option?.value ?? "";
    });
    expect(availableCandidateId, "matching page must expose an available candidate option").toBeTruthy();
    await candidateSelect.selectOption(availableCandidateId);
    await page.getByRole("button", { name: "Generate report" }).click();

    await expect(page.getByRole("heading", { name: "Match report" })).toBeVisible();
    await expect(page.getByRole("term").filter({ hasText: "Coverage" })).toBeVisible();
    await expect(page.getByText(/Cap reason/i).first()).toBeVisible();
    await expect(page.getByText(/TECHNICAL_FIT/i).first()).toBeVisible();
  });

  test("S04 consultant builds, reviews, and manually sends an anonymous shortlist", async ({ page, request }) => {
    const shortlist = await api<ConsultantShortlistDetail>(
      request,
      auth.consultant.accessToken,
      "POST",
      "/api/consultant/shortlists",
      {
        jobId: flow.jobId,
        title: "Pilot controlled shortlist",
        status: "draft",
      },
    );
    flow.shortlistId = shortlist.shortlistId;

    let detail = await api<ConsultantShortlistDetail>(
      request,
      auth.consultant.accessToken,
      "POST",
      `/api/consultant/shortlists/${flow.shortlistId}/cards`,
      {
        candidateId: flow.candidateId,
        clientNotes: "Client-safe evidence card.",
      },
    );
    flow.shortlistCardId = detail.cards[0].cardId;
    detail = await api<ConsultantShortlistDetail>(
      request,
      auth.consultant.accessToken,
      "PUT",
      `/api/consultant/shortlists/${flow.shortlistId}`,
      {
        jobId: flow.jobId,
        version: detail.version,
        title: detail.title,
        status: "ready_for_review",
      },
    );
    expect(detail.preSendChecks.every((check) => check.passed)).toBe(true);

    await signInThroughUi(page, "consultant");
    await page.goto(`/consultant/shortlists/${flow.shortlistId}`);
    await expect(page.getByRole("heading", { name: "Pre-send checks" })).toBeVisible();
    await expect(page.getByText(/PASS/i).first()).toBeVisible();
    await expect(page.getByRole("heading", { name: "Delivery preview" })).toBeVisible();
    await page.getByRole("button", { name: "Approve and mark sent_to_client" }).click();
    await expect(page.getByText(/sent_to_client/i).first()).toBeVisible();
    await expect(page.getByText(/Pilot Talent|@candidate|linkedin/i)).toHaveCount(0);
  });

  test("S05 client reviews anonymous shortlist, selects candidate, and requests unlock", async ({ page }) => {
    await signInThroughUi(page, "client");
    await page.goto(`/client/shortlists/${flow.shortlistId}`);

    await expect(page.getByRole("heading", { name: "Cards on this shortlist" })).toBeVisible();
    await expect(page.getByRole("term").filter({ hasText: "Anonymous card" })).toBeVisible();
    await expect(page.getByText(/Pilot Talent|@candidate|linkedin/i)).toHaveCount(0);
    await page.getByRole("button", { name: "Select candidate" }).first().click();
    await expect(page.getByText(/selected/i).first()).toBeVisible();
    await page.getByLabel(/Unlock request reason/i).first().fill("Client wants to interview this anonymous candidate.");
    await page.getByRole("button", { name: "Request unlock" }).first().click();
    await expect(page.getByText(/Latest unlock request/i)).toBeVisible();
    await expect(page.getByText(/CONSENT_MISSING/i)).toBeVisible();
  });

  test("S06 candidate reviews opportunity-linked consent and confirms shared fields", async ({ page, request }) => {
    const consent = await api<{ consentRecordRef: string }>(
      request,
      auth.consultant.accessToken,
      "POST",
      "/api/consultant/unlock-requests/consent-requests",
      {
        candidateRef: flow.candidateId,
        candidateProfileRef: flow.candidateProfileId,
        jobRef: flow.jobId,
        consentTextVersion: "v2.1-task42-e2e",
        expiresAt: null,
      },
    );
    flow.consentRecordRef = consent.consentRecordRef;

    await signInThroughUi(page, "candidate");
    await page.goto(`/candidate/consent/${flow.consentRecordRef}`);
    await expect(page.getByRole("heading", { name: "Review and confirm consent" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "Fields that may be disclosed after approval" })).toBeVisible();
    await page.getByRole("button", { name: "Confirm consent" }).click();
    await expect(page.getByText("confirmed", { exact: true }).first()).toBeVisible();
  });

  test("S07 consultant approves unlock and client can open identity-disclosed detail", async ({ page }) => {
    await signInThroughUi(page, "consultant");
    await page.goto("/consultant/unlocks");

    await expect(page.getByRole("heading", { name: "Unlock requests awaiting consultant action" })).toBeVisible();
    await expect(page.getByText(/No blockers detected|consent_confirmed/i)).toBeVisible();
    await page.getByLabel(/Decision reason/i).first().fill("Consent confirmed and client request is approved for interview.");
    await page.getByRole("button", { name: "Approve unlock" }).first().click();
    await expect(page.getByRole("heading", { name: /approved|contact_unlocked|identity_disclosed/i })).toBeVisible();
    await expect(page.getByText(/Disclosure record/i)).toBeVisible();

    await signInThroughUi(page, "client");
    await page.goto(`/client/shortlists/${flow.shortlistId}`);
    await expect(page.getByText(/Identity access/i).first()).toBeVisible();
    await page.getByRole("link", { name: /Open disclosed detail/i }).first().click();
    await expect(page.getByRole("heading", { name: "Disclosed candidate detail" })).toBeVisible();
    await expect(page.getByText(flow.candidateProfileId)).toBeVisible();
  });

  test("S08 client submits interview feedback and consultant sees the review follow-up", async ({ page }) => {
    await signInThroughUi(page, "client");
    await page.goto(`/client/shortlists/${flow.shortlistId}`);

    await page.getByLabel(/Feedback notes/i).first().fill(
      "Strong technical evidence, but the compensation expectation needs role-specific review.",
    );
    await page.getByLabel(/Feedback outcome/i).first().selectOption("yes");
    await page.getByRole("button", { name: "Submit feedback" }).first().click();
    await expect(page.getByText(/Feedback decision/i)).toBeVisible();
    await expect(page.getByText(/AI structured/i)).toBeVisible();
    const feedbackLink = page.getByRole("link", { name: /Open feedback workspace/i }).first();
    await expect(feedbackLink).toBeVisible();
    const href = await feedbackLink.getAttribute("href");
    flow.interviewId = href?.split("/").pop() ?? "";
    expect(flow.interviewId).toBeTruthy();

    await signInThroughUi(page, "consultant");
    await page.goto("/consultant/follow-ups");
    await expect(page.getByRole("heading", { name: "Follow-up Center" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Review interview feedback outcome" })).toBeVisible();
    await expect(page.getByText("interview_feedback_review").first()).toBeVisible();
    await expect(page.getByText("pending_review").first()).toBeVisible();
  });
});

async function loginApi(request: APIRequestContext, role: PortalRole): Promise<AuthSession> {
  const account = accounts[role];
  return api<AuthSession>(request, null, "POST", "/api/auth/login", {
    organizationId: PILOT_ORGANIZATION_ID,
    email: account.email,
    password: PILOT_PASSWORD,
    portalRole: role,
  });
}

async function api<T>(
  request: APIRequestContext,
  token: string | null | undefined,
  method: "GET" | "POST" | "PUT",
  path: string,
  body?: unknown,
): Promise<T> {
  const response =
    method === "GET"
      ? await request.get(path, { headers: authHeaders(token) })
      : method === "POST"
        ? await request.post(path, { headers: authHeaders(token), data: body })
        : await request.put(path, { headers: authHeaders(token), data: body });
  const envelope = (await response.json()) as ApiEnvelope<T>;
  expect(response.ok(), `${method} ${path} failed: ${JSON.stringify(envelope)}`).toBe(true);
  expect(envelope.data, `${method} ${path} returned no data`).toBeTruthy();
  return envelope.data as T;
}

function authHeaders(token: string | null | undefined): Record<string, string> {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function signInThroughUi(page: Page, role: PortalRole): Promise<void> {
  const account = accounts[role];
  await page.context().clearCookies();
  await page.goto("/");
  await page.evaluate(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
  });
  await page.goto(account.path);
  await page.getByLabel(/Organization ID/i).fill(PILOT_ORGANIZATION_ID);
  await page.getByLabel(/Work email|Email/i).fill(account.email);
  await page.getByLabel(/Password/i).fill(PILOT_PASSWORD);
  await page.getByRole("button", { name: account.submitButton }).click();
  await expect(page.getByText(account.signedIn)).toBeVisible();
}
