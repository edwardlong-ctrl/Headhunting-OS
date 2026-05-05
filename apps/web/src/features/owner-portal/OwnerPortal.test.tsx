import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

(globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true;

const {
  listOwnerPlacementsMock,
  listOwnerCommissionMock,
  fetchOwnerRevenueSummaryMock,
  loadOwnerSessionMock,
  signOutOwnerSessionMock,
  loginMock,
  saveAccessTokenMock,
  saveOwnerSessionMock,
} = vi.hoisted(() => ({
  listOwnerPlacementsMock: vi.fn(),
  listOwnerCommissionMock: vi.fn(),
  fetchOwnerRevenueSummaryMock: vi.fn(),
  loadOwnerSessionMock: vi.fn(),
  signOutOwnerSessionMock: vi.fn(),
  loginMock: vi.fn(),
  saveAccessTokenMock: vi.fn(),
  saveOwnerSessionMock: vi.fn(),
}));

vi.mock("../../api/ownerPlacements", () => ({
  listOwnerPlacements: listOwnerPlacementsMock,
  listOwnerCommission: listOwnerCommissionMock,
}));

vi.mock("../../api/ownerRevenue", () => ({
  fetchOwnerRevenueSummary: fetchOwnerRevenueSummaryMock,
}));

vi.mock("../../api/auth", () => ({
  login: loginMock,
}));

vi.mock("../../auth/accessTokenStorage", () => ({
  saveAccessToken: saveAccessTokenMock,
}));

vi.mock("./ownerSession", () => ({
  loadOwnerSession: loadOwnerSessionMock,
  saveOwnerSession: saveOwnerSessionMock,
  signOutOwnerSession: signOutOwnerSessionMock,
}));

import { OwnerPortal } from "./OwnerPortal";

describe("OwnerPortal placements page", () => {
  let container: HTMLDivElement;
  let root: Root;

  beforeEach(() => {
    container = document.createElement("div");
    document.body.appendChild(container);
    root = createRoot(container);

    loadOwnerSessionMock.mockReturnValue({
      accessToken: "owner-access-token",
      refreshToken: "owner-refresh-token",
      portalRole: "owner",
      displayName: "Owner User",
      organizationId: "org-1",
    });
    listOwnerCommissionMock.mockResolvedValue({
      status: "ready",
      data: { items: [], totalCount: 0, limit: 10, offset: 0, hasMore: false },
    });
    fetchOwnerRevenueSummaryMock.mockResolvedValue({
      status: "ready",
      data: {
        totalExpectedFee: 0,
        totalPaidFee: 0,
        placementCount: 0,
        unknownExpectedFeePlacementCount: 0,
        pendingCommissionCount: 0,
        paidCommissionCount: 0,
        paidCommissionMissingAmountCount: 0,
        activeGuaranteeCount: 0,
        replacementRequiredCount: 0,
        invoiceInFlightCount: 0,
      },
    });
    signOutOwnerSessionMock.mockResolvedValue({
      status: "ready",
      data: { signedOut: true },
    });
  });

  afterEach(async () => {
    await act(async () => {
      root.unmount();
    });
    container.remove();
    vi.clearAllMocks();
  });

  async function renderOwnerRoute(initialEntry: string) {
    await act(async () => {
      root.render(
        <MemoryRouter initialEntries={[initialEntry]}>
          <Routes>
            <Route path="/owner/*" element={<OwnerPortal />} />
          </Routes>
        </MemoryRouter>,
      );
    });
    await act(async () => {
      await Promise.resolve();
    });
  }

  async function renderOwnerPlacementsPage() {
    await renderOwnerRoute("/owner/placements");
  }

  it("renders a real zero expected fee instead of Pending", async () => {
    listOwnerPlacementsMock.mockResolvedValue({
      status: "ready",
      data: {
        items: [
          {
            placementId: "placement-1",
            jobId: "job-1",
            candidateId: "candidate-1",
            companyId: "company-1",
            status: "invoice_sent",
            salaryAmount: 100000,
            salaryCurrency: "USD",
            feeRatePercentage: 0,
            expectedFeeAmount: 0,
            commissionStatuses: [],
            startDate: "2026-05-01",
            guaranteeDays: 90,
            guaranteeExpiresAt: "2026-07-30",
            createdAt: "2026-05-01T00:00:00Z",
            updatedAt: "2026-05-01T00:00:00Z",
          },
        ],
        totalCount: 1,
        limit: 10,
        offset: 0,
        hasMore: false,
      },
    });

    await renderOwnerPlacementsPage();

    expect(container.textContent).toContain("0 USD");
  });

  it("renders all commission status badges and falls back to n/a for empty arrays", async () => {
    listOwnerPlacementsMock.mockResolvedValue({
      status: "ready",
      data: {
        items: [
          {
            placementId: "placement-1",
            jobId: "job-1",
            candidateId: "candidate-1",
            companyId: "company-1",
            status: "invoice_sent",
            salaryAmount: 100000,
            salaryCurrency: "USD",
            feeRatePercentage: 25,
            expectedFeeAmount: 25000,
            commissionStatuses: ["pending", "paid"],
            startDate: "2026-05-01",
            guaranteeDays: 90,
            guaranteeExpiresAt: "2026-07-30",
            createdAt: "2026-05-01T00:00:00Z",
            updatedAt: "2026-05-01T00:00:00Z",
          },
          {
            placementId: "placement-2",
            jobId: "job-2",
            candidateId: "candidate-2",
            companyId: "company-2",
            status: "offer_accepted",
            salaryAmount: 120000,
            salaryCurrency: "USD",
            feeRatePercentage: 20,
            expectedFeeAmount: null,
            commissionStatuses: [],
            startDate: null,
            guaranteeDays: null,
            guaranteeExpiresAt: null,
            createdAt: "2026-05-01T00:00:00Z",
            updatedAt: "2026-05-01T00:00:00Z",
          },
        ],
        totalCount: 2,
        limit: 10,
        offset: 0,
        hasMore: false,
      },
    });

    await renderOwnerPlacementsPage();

    const rows = Array.from(container.querySelectorAll("tbody tr"));
    const badgeTexts = Array.from(container.querySelectorAll(".status-badge"))
      .map((element) => element.textContent?.trim())
      .filter((value): value is string => Boolean(value));

    expect(rows[1]?.textContent).toContain("Pending");
    expect(badgeTexts).toContain("pending");
    expect(badgeTexts).toContain("paid");
    expect(badgeTexts).toContain("n/a");
  });

  it("flags paid commissions that are missing an amount", async () => {
    listOwnerCommissionMock.mockResolvedValue({
      status: "ready",
      data: {
        items: [
          {
            commissionId: "commission-1",
            placementId: "placement-1",
            consultantId: "consultant-1",
            status: "paid",
            commissionType: "full_fee",
            amount: null,
            currency: "USD",
            splitPercentage: null,
            salaryAmount: 120000,
            feeRatePercentage: 20,
            paidAt: "2026-05-02T00:00:00Z",
            withheldReason: null,
            createdAt: "2026-05-01T00:00:00Z",
            updatedAt: "2026-05-02T00:00:00Z",
          },
        ],
        totalCount: 1,
        limit: 10,
        offset: 0,
        hasMore: false,
      },
    });

    await renderOwnerRoute("/owner/commission");

    expect(container.textContent).toContain("Pending");
    expect(container.textContent).toContain("amount missing");
    expect(container.textContent).toContain("paid");
  });

  it("labels revenue as known subtotal when expected fee data is still pending", async () => {
    listOwnerPlacementsMock.mockResolvedValue({
      status: "ready",
      data: { items: [], totalCount: 0, limit: 10, offset: 0, hasMore: false },
    });
    fetchOwnerRevenueSummaryMock.mockResolvedValue({
      status: "ready",
      data: {
        totalExpectedFee: 18000,
        totalPaidFee: 12000,
        placementCount: 3,
        unknownExpectedFeePlacementCount: 2,
        pendingCommissionCount: 1,
        paidCommissionCount: 1,
        paidCommissionMissingAmountCount: 0,
        activeGuaranteeCount: 0,
        replacementRequiredCount: 0,
        invoiceInFlightCount: 1,
      },
    });

    await renderOwnerRoute("/owner/revenue");

    expect(container.textContent).toContain("Expected fee (known)");
    expect(container.textContent).toContain("Expected fee pending");
    expect(container.textContent).toContain("2");
    expect(container.textContent).toContain("excluded from the known expected fee subtotal");
  });

  it("labels paid fee as known subtotal when paid commissions are missing amount data", async () => {
    listOwnerPlacementsMock.mockResolvedValue({
      status: "ready",
      data: { items: [], totalCount: 0, limit: 10, offset: 0, hasMore: false },
    });
    fetchOwnerRevenueSummaryMock.mockResolvedValue({
      status: "ready",
      data: {
        totalExpectedFee: 30000,
        totalPaidFee: 18000,
        placementCount: 2,
        unknownExpectedFeePlacementCount: 0,
        pendingCommissionCount: 0,
        paidCommissionCount: 2,
        paidCommissionMissingAmountCount: 1,
        activeGuaranteeCount: 0,
        replacementRequiredCount: 0,
        invoiceInFlightCount: 1,
      },
    });

    await renderOwnerRoute("/owner/revenue");

    expect(container.textContent).toContain("Paid fee (known)");
    expect(container.textContent).toContain("Paid amount missing");
    expect(container.textContent).toContain("excluded from the known paid fee subtotal");
  });
});
