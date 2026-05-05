import { FormEvent, useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { login, type AuthSession } from "../../api/auth";
import { fetchOwnerRevenueSummary, type OwnerRevenueSummary } from "../../api/ownerRevenue";
import { listOwnerCommission, listOwnerPlacements, type OwnerCommission, type OwnerPlacement } from "../../api/ownerPlacements";
import { type ApiResult, type PagedResult } from "../../api/http";
import { saveAccessToken } from "../../auth/accessTokenStorage";
import { loadOwnerSession, saveOwnerSession, signOutOwnerSession } from "./ownerSession";

const DEFAULT_PAGE_SIZE = 10;

type Loadable<T> = ApiResult<T> | { status: "idle" | "loading" };

function useLoadable<T>(loader: () => Promise<ApiResult<T>>, deps: unknown[]): Loadable<T> {
  const [state, setState] = useState<Loadable<T>>({ status: "loading" });

  useEffect(() => {
    let active = true;
    setState({ status: "loading" });
    loader().then((result) => {
      if (active) {
        setState(result);
      }
    });
    return () => {
      active = false;
    };
  }, deps);

  return state;
}

function renderLoadable<T>(state: Loadable<T>, renderReady: (data: T) => React.ReactNode) {
  if (state.status === "loading" || state.status === "idle") {
    return <p className="helper-copy">Loading owner data...</p>;
  }
  if (state.status !== "ready") {
    return <p className="helper-copy">{"error" in state ? state.error ?? "Owner API unavailable." : "Owner API unavailable."}</p>;
  }
  return renderReady(state.data);
}

function StatusBadge({ value }: { value: string | null | undefined }) {
  return <span className="status-badge">{value ?? "n/a"}</span>;
}

function DataTable({ headers, rows }: { headers: string[]; rows: React.ReactNode[][] }) {
  if (rows.length === 0) {
    return <p className="helper-copy">No rows returned.</p>;
  }
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            {headers.map((header) => (
              <th key={header}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => (
                <td key={`${rowIndex}-${cellIndex}`}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function PaginationControls({
  hasMore,
  offset,
  limit,
  onPrevious,
  onNext,
}: {
  hasMore: boolean;
  offset: number;
  limit: number;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <div className="pagination-controls">
      <button type="button" className="secondary-button" onClick={onPrevious} disabled={offset === 0}>
        Previous
      </button>
      <span className="pagination-chip">Page {Math.floor(offset / limit) + 1}</span>
      <button type="button" className="secondary-button" onClick={onNext} disabled={!hasMore}>
        Next
      </button>
    </div>
  );
}

function formatDate(value: string | null | undefined): string {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function formatMoney(
  amount: number | null | undefined,
  currency: string | null | undefined,
  fallback = "Pending",
): string {
  return amount == null ? fallback : `${amount.toLocaleString()} ${currency ?? ""}`.trim();
}

function renderCommissionAmount(
  amount: number | null | undefined,
  currency: string | null | undefined,
  status: string | null | undefined,
) {
  const value = formatMoney(amount, currency);
  if (status === "paid" && amount == null) {
    return (
      <div>
        <div>{value}</div>
        <div className="helper-copy">amount missing</div>
      </div>
    );
  }
  return value;
}

function PortalShell({ title, eyebrow, children }: { title: string; eyebrow: string; children: React.ReactNode }) {
  return (
    <section className="portal-layout" aria-label="Owner portal">
      <header className="portal-heading">
        <span className="portal-eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
      </header>
      <nav className="portal-nav" aria-label="Owner sections">
        <NavLink to="/owner/placements">Placements</NavLink>
        <NavLink to="/owner/commission">Commission</NavLink>
        <NavLink to="/owner/revenue">Revenue</NavLink>
      </nav>
      <div className="workspace-stack">{children}</div>
    </section>
  );
}

function OwnerSignInPage({ onSignedIn }: { onSignedIn: (session: AuthSession) => void }) {
  const [organizationId, setOrganizationId] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [result, setResult] = useState<Loadable<AuthSession>>({ status: "idle" });
  const navigate = useNavigate();

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    const next = await login({
      organizationId: organizationId.trim() || undefined,
      email: email.trim(),
      password,
      portalRole: "owner",
    });
    setResult(next);
    if (next.status === "ready") {
      saveAccessToken(next.data.accessToken, "owner");
      saveOwnerSession(next.data);
      onSignedIn(next.data);
      navigate("/owner/placements", { replace: true });
    }
  }

  return (
    <section className="portal-layout">
      <header className="portal-heading">
        <span className="portal-eyebrow">Business command portal</span>
        <h1>Owner / Partner</h1>
        <p className="portal-copy">
          Sign in with an owner role before viewing placement, commission, and revenue supervision data.
        </p>
      </header>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Owner session</span>
            <h2>Sign in to continue</h2>
          </div>
          <StatusBadge value="owner" />
        </div>
        <form className="stack-form sign-in-form" onSubmit={onSubmit}>
          <label>
            Organization ID
            <input
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value)}
              placeholder="Optional unless your account requires an org context"
            />
          </label>
          <label>
            Work email
            <input
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="owner@company.com"
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Password"
            />
          </label>
          <button type="submit" disabled={!email.trim() || !password}>
            Enter Owner Portal
          </button>
          {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? (
            <p className="helper-copy">{"error" in result ? result.error ?? "Owner sign in failed." : "Owner sign in failed."}</p>
          ) : null}
        </form>
      </section>
    </section>
  );
}

function OwnerPlacementsPage() {
  const [offset, setOffset] = useState(0);
  const state = useLoadable(() => listOwnerPlacements(DEFAULT_PAGE_SIZE, offset), [offset]);

  return renderLoadable(state, (result: PagedResult<OwnerPlacement>) => (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">Placement oversight</span>
          <h2>Expected fee, invoice and guarantee state</h2>
        </div>
      </div>
      <DataTable
        headers={["Placement", "Status", "Expected fee", "Start", "Guarantee", "Commission"]}
        rows={result.items.map((placement) => [
          <div>
            <strong>{placement.placementId.slice(0, 8)}</strong>
            <div className="helper-copy">job {placement.jobId.slice(0, 8)} · candidate {placement.candidateId.slice(0, 8)}</div>
          </div>,
          <StatusBadge value={placement.status} />,
          formatMoney(placement.expectedFeeAmount, placement.salaryCurrency),
          placement.startDate ? formatDate(placement.startDate) : "TBD",
          <div>
            <div>{placement.guaranteeDays ? `${placement.guaranteeDays} days` : "None"}</div>
            <div className="helper-copy">{placement.guaranteeExpiresAt ? formatDate(placement.guaranteeExpiresAt) : "not active"}</div>
          </div>,
          placement.commissionStatuses.length > 0 ? (
            <div style={{ display: "flex", flexWrap: "wrap", gap: "0.35rem" }}>
              {placement.commissionStatuses.map((status) => (
                <StatusBadge key={`${placement.placementId}-${status}`} value={status} />
              ))}
            </div>
          ) : (
            <StatusBadge value={null} />
          ),
        ])}
      />
      <PaginationControls
        hasMore={result.hasMore}
        offset={result.offset}
        limit={result.limit}
        onPrevious={() => setOffset((value) => Math.max(0, value - DEFAULT_PAGE_SIZE))}
        onNext={() => setOffset((value) => value + DEFAULT_PAGE_SIZE)}
      />
    </section>
  ));
}

function OwnerCommissionPage() {
  const [offset, setOffset] = useState(0);
  const state = useLoadable(() => listOwnerCommission(DEFAULT_PAGE_SIZE, offset), [offset]);

  return renderLoadable(state, (result: PagedResult<OwnerCommission>) => (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">Commission oversight</span>
          <h2>Revenue source data and payout state</h2>
        </div>
      </div>
      <DataTable
        headers={["Commission", "Status", "Amount", "Fee basis", "Paid at", "Withheld"]}
        rows={result.items.map((commission) => [
          <div>
            <strong>{commission.commissionId.slice(0, 8)}</strong>
            <div className="helper-copy">placement {commission.placementId.slice(0, 8)}</div>
          </div>,
          <StatusBadge value={commission.status} />,
          renderCommissionAmount(commission.amount, commission.currency, commission.status),
          <div>
            <div>{formatMoney(commission.salaryAmount, commission.currency, "n/a")}</div>
            <div className="helper-copy">fee {commission.feeRatePercentage ?? "n/a"}%</div>
          </div>,
          formatDate(commission.paidAt),
          commission.withheldReason ?? "-",
        ])}
      />
      <PaginationControls
        hasMore={result.hasMore}
        offset={result.offset}
        limit={result.limit}
        onPrevious={() => setOffset((value) => Math.max(0, value - DEFAULT_PAGE_SIZE))}
        onNext={() => setOffset((value) => value + DEFAULT_PAGE_SIZE)}
      />
    </section>
  ));
}

function OwnerRevenuePage() {
  const state = useLoadable(fetchOwnerRevenueSummary, []);

  return renderLoadable(state, (summary: OwnerRevenueSummary) => (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">Revenue summary</span>
          <h2>Expected fee, paid fee, invoice and guarantee counts</h2>
        </div>
      </div>
      {summary.unknownExpectedFeePlacementCount > 0 ? (
        <p className="helper-copy">
          {summary.unknownExpectedFeePlacementCount} placement(s) still have pending expected fee data and are excluded from the known expected fee subtotal.
        </p>
      ) : null}
      {summary.paidCommissionMissingAmountCount > 0 ? (
        <p className="helper-copy">
          {summary.paidCommissionMissingAmountCount} paid commission(s) are missing amount data and are excluded from the known paid fee subtotal.
        </p>
      ) : null}
      <div className="portal-grid" aria-label="Owner revenue metrics">
        {[
          [
            summary.unknownExpectedFeePlacementCount > 0 ? "Expected fee (known)" : "Expected fee",
            summary.totalExpectedFee.toLocaleString(),
          ],
          [
            summary.paidCommissionMissingAmountCount > 0 ? "Paid fee (known)" : "Paid fee",
            summary.totalPaidFee.toLocaleString(),
          ],
          ["Placements", String(summary.placementCount)],
          ["Expected fee pending", String(summary.unknownExpectedFeePlacementCount)],
          ["Pending commission", String(summary.pendingCommissionCount)],
          ["Paid commission", String(summary.paidCommissionCount)],
          ["Paid amount missing", String(summary.paidCommissionMissingAmountCount)],
          ["Active guarantee", String(summary.activeGuaranteeCount)],
          ["Replacement required", String(summary.replacementRequiredCount)],
          ["Invoice in flight", String(summary.invoiceInFlightCount)],
        ].map(([label, value]) => (
          <section key={label} className="portal-panel">
            <span className="portal-eyebrow">{label}</span>
            <h3>{value}</h3>
          </section>
        ))}
      </div>
    </section>
  ));
}

export function OwnerPortal() {
  const [session, setSession] = useState<AuthSession | null>(() => loadOwnerSession());
  const [signingOut, setSigningOut] = useState(false);
  const [signOutError, setSignOutError] = useState<string | null>(null);

  async function onSignOut() {
    setSigningOut(true);
    setSignOutError(null);
    try {
      const result = await signOutOwnerSession(session);
      if (result.status === "ready") {
        setSession(null);
        return;
      }
      setSignOutError(result.error ?? "Owner sign out failed. Please try again.");
    } finally {
      setSigningOut(false);
    }
  }

  if (!session) {
    return <OwnerSignInPage onSignedIn={setSession} />;
  }

  return (
    <PortalShell title="Owner / Partner" eyebrow="Business command portal">
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Session boundary</span>
            <h2>{session.displayName}</h2>
          </div>
          <button
            type="button"
            className="secondary-button"
            onClick={onSignOut}
            disabled={signingOut}
          >
            {signingOut ? "Signing out..." : "Sign out"}
          </button>
        </div>
        <p className="helper-copy">
          {session.portalRole} session active for {session.organizationId || "current organization"}.
        </p>
        {signOutError ? <p className="helper-copy">{signOutError}</p> : null}
      </section>
      <Routes>
        <Route path="/" element={<Navigate to="placements" replace />} />
        <Route path="placements" element={<OwnerPlacementsPage />} />
        <Route path="commission" element={<OwnerCommissionPage />} />
        <Route path="revenue" element={<OwnerRevenuePage />} />
        <Route path="*" element={<Navigate to="placements" replace />} />
      </Routes>
    </PortalShell>
  );
}
