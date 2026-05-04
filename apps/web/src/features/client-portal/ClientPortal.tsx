import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Link,
  NavLink,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";
import { login, type AuthSession } from "../../api/auth";
import {
  fetchClientCompanyProfile,
  saveClientCompanyProfile,
  type ClientCompanyProfile,
} from "../../api/clientCompanies";
import {
  answerClientJobClarification,
  createClientJob,
  fetchClientJob,
  type ClientJobSubmissionStatus,
} from "../../api/clientJobs";
import {
  fetchClientPreferences,
  saveClientPreferences,
  type ClientPreferenceItem,
  type ClientPreferenceResponse,
} from "../../api/clientPreferences";
import {
  fetchClientSafeCandidateCard,
  isAnonymousCardRef,
  type ClientSafeCandidateCard,
  type ClientSafeCandidateCardResult,
} from "../../api/clientSafeCandidateCards";
import {
  createClientUnlockRequest,
  fetchClientDashboard,
  fetchClientShortlist,
  fetchClientShortlists,
  markClientShortlistViewed,
  selectClientShortlistCandidate,
  type ClientDashboard,
  type ClientShortlistCard,
  type ClientShortlistDetail,
  type ClientShortlistSummary,
  type ClientUnlockRequest,
} from "../../api/clientShortlists";
import {
  submitClientFeedback,
  type ClientFeedbackResponse,
} from "../../api/clientFeedback";
import { type ApiResult } from "../../api/http";
import { loadAccessToken, saveAccessToken } from "../../auth/accessTokenStorage";
import {
  canClientRequestUnlock,
  canClientSelectCandidate,
  canClientSubmitInterviewFeedback,
  deriveIdentityAccessStatus,
  deriveUnlockStageLabel,
  shouldWarnApprovedWithoutDisclosure,
} from "./clientPortalShortlistUtils";
import { ClientDisclosedCandidatePage } from "./ClientDisclosedCandidatePage";

type Loadable<T> = ApiResult<T> | { status: "idle" | "loading" };

type ClientPortalSession = Pick<
  AuthSession,
  "organizationId" | "userAccountId" | "displayName" | "portalRole" | "accessTokenExpiresAt"
>;

type CommercialTermsDraft = {
  feeModel: string;
  feeRangeOrRate: string;
  paymentTerms: string;
  contractStatus: string;
  notes: string;
};

const CLIENT_SESSION_STORAGE_KEY = "rto.clientPortalSession";
const CLIENT_LAST_JOB_ID_STORAGE_KEY = "rto.clientLastJobId";

const CLIENT_NAV_ITEMS = [
  { to: "/client", label: "Dashboard" },
  { to: "/client/company-profile", label: "Company Profile" },
  { to: "/client/preferences", label: "Preferences" },
  { to: "/client/jobs/new", label: "Submit Job" },
  { to: "/client/shortlists", label: "Shortlists" },
] as const;

const EMPTY_COMMERCIAL_TERMS: CommercialTermsDraft = {
  feeModel: "",
  feeRangeOrRate: "",
  paymentTerms: "",
  contractStatus: "",
  notes: "",
};

function loadClientPortalSession(): ClientPortalSession | null {
  if (typeof window === "undefined") {
    return null;
  }
  const raw = window.localStorage.getItem(CLIENT_SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<ClientPortalSession>;
    if (!parsed.portalRole) {
      return null;
    }
    return {
      organizationId: parsed.organizationId ?? "",
      userAccountId: parsed.userAccountId ?? "",
      displayName: parsed.displayName ?? "Client",
      portalRole: parsed.portalRole,
      accessTokenExpiresAt: parsed.accessTokenExpiresAt ?? "",
    };
  } catch {
    return null;
  }
}

function saveClientPortalSession(session: AuthSession): void {
  if (typeof window === "undefined") {
    return;
  }
  const payload: ClientPortalSession = {
    organizationId: session.organizationId,
    userAccountId: session.userAccountId,
    displayName: session.displayName,
    portalRole: session.portalRole,
    accessTokenExpiresAt: session.accessTokenExpiresAt,
  };
  window.localStorage.setItem(CLIENT_SESSION_STORAGE_KEY, JSON.stringify(payload));
}

function clearClientPortalSession(): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(CLIENT_SESSION_STORAGE_KEY);
}

function loadLastJobId(): string {
  if (typeof window === "undefined") {
    return "";
  }
  return window.localStorage.getItem(CLIENT_LAST_JOB_ID_STORAGE_KEY) ?? "";
}

function saveLastJobId(jobId: string): void {
  if (typeof window === "undefined") {
    return;
  }
  const normalized = jobId.trim();
  if (!normalized) {
    window.localStorage.removeItem(CLIENT_LAST_JOB_ID_STORAGE_KEY);
    return;
  }
  window.localStorage.setItem(CLIENT_LAST_JOB_ID_STORAGE_KEY, normalized);
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function linesToList(value: string): string[] {
  return value
    .split("\n")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function listToLines(values: string[]): string {
  return values.join("\n");
}

function formatDate(value?: string | null): string {
  if (!value) {
    return "Not available";
  }
  try {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(value));
  } catch {
    return value;
  }
}

function parseCommercialTerms(value?: string | null): CommercialTermsDraft {
  if (!value || !value.trim()) {
    return { ...EMPTY_COMMERCIAL_TERMS };
  }
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    return {
      feeModel: typeof parsed.feeModel === "string" ? parsed.feeModel : "",
      feeRangeOrRate: typeof parsed.feeRangeOrRate === "string" ? parsed.feeRangeOrRate : "",
      paymentTerms: typeof parsed.paymentTerms === "string" ? parsed.paymentTerms : "",
      contractStatus: typeof parsed.contractStatus === "string" ? parsed.contractStatus : "",
      notes: typeof parsed.notes === "string" ? parsed.notes : "",
    };
  } catch {
    return {
      ...EMPTY_COMMERCIAL_TERMS,
      notes: value,
    };
  }
}

function serializeCommercialTerms(draft: CommercialTermsDraft): string | null {
  const payload = {
    feeModel: draft.feeModel.trim(),
    feeRangeOrRate: draft.feeRangeOrRate.trim(),
    paymentTerms: draft.paymentTerms.trim(),
    contractStatus: draft.contractStatus.trim(),
    notes: draft.notes.trim(),
  };
  if (!Object.values(payload).some((value) => value.length > 0)) {
    return null;
  }
  return JSON.stringify(payload);
}

function summarizeCommercialTerms(value?: string | null): Array<[string, string | null]> {
  const parsed = parseCommercialTerms(value);
  return [
    ["Fee model", emptyToNull(parsed.feeModel)],
    ["Fee range / rate", emptyToNull(parsed.feeRangeOrRate)],
    ["Payment terms", emptyToNull(parsed.paymentTerms)],
    ["Contract status", emptyToNull(parsed.contractStatus)],
    ["Notes", emptyToNull(parsed.notes)],
  ];
}

function useLoadable<T>(loader: () => Promise<ApiResult<T>>, deps: unknown[]): Loadable<T> {
  const [state, setState] = useState<Loadable<T>>({ status: "loading" });

  useEffect(() => {
    let active = true;
    setState({ status: "loading" });
    void loader().then((result) => {
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

function loadableError(state: Loadable<unknown>): string | undefined {
  return "error" in state ? state.error : undefined;
}

function renderLoadable<T>(state: Loadable<T>, renderReady: (data: T) => React.ReactNode) {
  if (state.status === "idle" || state.status === "loading") {
    return <ClientSafeState title="Loading client workspace" tone="neutral" />;
  }
  if (state.status !== "ready") {
    return <ClientApiState status={state.status} error={loadableError(state)} />;
  }
  return renderReady(state.data);
}

function ClientApiState({
  status,
  error,
}: {
  status: Exclude<Loadable<unknown>["status"], "ready" | "idle" | "loading">;
  error?: string;
}) {
  const title =
    status === "unauthenticated"
      ? "Client session required"
      : status === "denied"
        ? "Access denied for this client view"
        : status === "invalid_request"
          ? "The client request is invalid"
          : status === "unavailable"
            ? "This client surface is unavailable"
            : "The request failed";
  return <ClientSafeState title={title} tone={status === "failed" ? "warning" : "neutral"} detail={error} />;
}

function ClientSafeState({
  title,
  tone,
  detail,
}: {
  title: string;
  tone: "neutral" | "warning";
  detail?: string;
}) {
  return (
    <div className={`safe-state safe-state-${tone}`} role="status">
      <h3>{title}</h3>
      <p>{detail ?? "Only client-safe company, job, and shortlist information is shown here."}</p>
    </div>
  );
}

function StatusBadge({ value }: { value: string }) {
  return <span className={`status-badge ${badgeToneForValue(value)}`}>{value}</span>;
}

function badgeToneForValue(value: string): string {
  const normalized = value.toLowerCase();
  if (
    normalized.includes("active")
    || normalized.includes("approved")
    || normalized.includes("allowed")
    || normalized.includes("open")
    || normalized.includes("ready")
    || normalized.includes("sent")
  ) {
    return "status-badge-positive";
  }
  if (
    normalized.includes("draft")
    || normalized.includes("pending")
    || normalized.includes("review")
    || normalized.includes("submitted")
  ) {
    return "status-badge-accent";
  }
  if (
    normalized.includes("blocked")
    || normalized.includes("denied")
    || normalized.includes("failed")
    || normalized.includes("warning")
  ) {
    return "status-badge-warning";
  }
  if (normalized.includes("inactive") || normalized.includes("closed")) {
    return "status-badge-neutral";
  }
  return "";
}

function KeyValueList({ items }: { items: Array<[string, string | null | undefined]> }) {
  return (
    <dl className="mini-meta key-value-list">
      {items.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value && value.length > 0 ? value : "Not available"}</dd>
        </div>
      ))}
    </dl>
  );
}

function SafeList({ title, items }: { title: string; items: string[] }) {
  if (items.length === 0) {
    return null;
  }
  return (
    <section>
      <h4>{title}</h4>
      <ul className="safe-list">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </section>
  );
}

function CommercialTermsFields({
  value,
  onChange,
}: {
  value: CommercialTermsDraft;
  onChange: (next: CommercialTermsDraft) => void;
}) {
  function update<K extends keyof CommercialTermsDraft>(key: K, nextValue: CommercialTermsDraft[K]) {
    onChange({
      ...value,
      [key]: nextValue,
    });
  }

  return (
    <div className="client-commercial-grid">
      <label>
        Fee model
        <input
          value={value.feeModel}
          onChange={(event) => update("feeModel", event.target.value)}
          placeholder="retained / contingent"
        />
      </label>
      <label>
        Fee range or rate
        <input
          value={value.feeRangeOrRate}
          onChange={(event) => update("feeRangeOrRate", event.target.value)}
          placeholder="20%-25% annual base"
        />
      </label>
      <label>
        Payment terms
        <input
          value={value.paymentTerms}
          onChange={(event) => update("paymentTerms", event.target.value)}
          placeholder="50% on shortlist, 50% on start"
        />
      </label>
      <label>
        Contract status
        <input
          value={value.contractStatus}
          onChange={(event) => update("contractStatus", event.target.value)}
          placeholder="placeholder_confirmed"
        />
      </label>
      <label className="wide-field">
        Notes
        <textarea
          rows={3}
          value={value.notes}
          onChange={(event) => update("notes", event.target.value)}
          placeholder="Optional client-safe notes for the consultant"
        />
      </label>
    </div>
  );
}

function ClientPortalLayout({
  children,
  session,
  onLogout,
}: {
  children: React.ReactNode;
  session: ClientPortalSession;
  onLogout: () => void;
}) {
  const location = useLocation();
  const navigate = useNavigate();
  const [accessToken, setAccessToken] = useState(() => loadAccessToken("client") ?? "");
  const [jobId, setJobId] = useState(() => loadLastJobId());
  const [cardRef, setCardRef] = useState("");

  useEffect(() => {
    setAccessToken(loadAccessToken("client") ?? "");
  }, [location.pathname]);

  function openJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalized = jobId.trim();
    if (!normalized) {
      return;
    }
    saveLastJobId(normalized);
    navigate(`/client/jobs/${encodeURIComponent(normalized)}`);
  }

  function openCard(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalized = cardRef.trim();
    if (!normalized) {
      return;
    }
    navigate(`/client/candidate-cards/${encodeURIComponent(normalized)}`);
  }

  return (
    <section className="portal-layout client-portal-shell">
      <header className="client-shell-header">
        <div>
          <span className="portal-eyebrow">Client hiring workspace</span>
          <h1>Client 端</h1>
          <p className="helper-copy">
            Submit company context, open jobs, answer clarification, and review client-safe shortlist surfaces.
          </p>
        </div>
        <div className="client-shell-badges">
          <span className="status-badge status-badge-positive">Client-safe only</span>
          <span className="status-badge status-badge-accent">Governed intake</span>
        </div>
      </header>

      <div className="client-shell-grid">
        <aside className="portal-panel client-sidebar">
          <div className="client-sidebar-block">
            <span className="portal-eyebrow">Current client</span>
            <h2>{session.displayName}</h2>
            <p className="helper-copy">
              {session.portalRole} session active for {session.organizationId || "current organization"}.
            </p>
            <button type="button" className="secondary-button" onClick={onLogout}>
              Sign out
            </button>
          </div>

          <nav className="client-nav" aria-label="Client workspace routes">
            {CLIENT_NAV_ITEMS.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.to === "/client"}>
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="client-sidebar-block">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Session token</span>
                <h3>Client API boundary</h3>
              </div>
            </div>
            <label>
              Access token
              <textarea
                rows={3}
                value={accessToken}
                onChange={(event) => {
                  const nextToken = event.target.value;
                  setAccessToken(nextToken);
                  saveAccessToken(nextToken, "client");
                }}
                placeholder="Client bearer token"
              />
            </label>
            <p className="helper-copy">
              Use login below or paste a dev token. All client API requests reuse this protected token scope.
            </p>
          </div>

          <div className="client-sidebar-block">
            <h3>Open job status</h3>
            <form className="stack-form" onSubmit={openJob}>
              <label>
                Job ID
                <input
                  value={jobId}
                  onChange={(event) => setJobId(event.target.value)}
                  placeholder="Paste a client-visible job id"
                />
              </label>
              <button type="submit" disabled={!jobId.trim()}>
                Open job
              </button>
            </form>
          </div>

          <div className="client-sidebar-block">
            <h3>Open candidate card</h3>
            <form className="stack-form" onSubmit={openCard}>
              <label>
                Anonymous card ref
                <input
                  value={cardRef}
                  onChange={(event) => setCardRef(event.target.value)}
                  placeholder="card_..."
                />
              </label>
              <button type="submit" disabled={!cardRef.trim()}>
                Open card
              </button>
            </form>
          </div>
        </aside>

        <div className="client-content">{children}</div>
      </div>
    </section>
  );
}

function ClientSignInPage({
  onSignedIn,
}: {
  onSignedIn: (session: AuthSession) => void;
}) {
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
      portalRole: "client",
    });
    setResult(next);
    if (next.status === "ready") {
      onSignedIn(next.data);
      navigate("/client", { replace: true });
    }
  }

  return (
    <section className="portal-layout client-auth-shell">
      <header className="portal-heading">
        <span className="portal-eyebrow">Client Portal</span>
        <h1>Governed client handoff</h1>
        <p className="portal-copy">
          Sign in first, then continue in the client-safe workspace for company profile, job intake, clarification, and shortlist review.
        </p>
      </header>
      <div className="client-auth-grid">
        <section className="portal-panel">
          <span className="portal-eyebrow">Task 25 delivery</span>
          <h2>One workspace for client intake</h2>
          <SafeList
            title="Available routes"
            items={[
              "Company profile create and update.",
              "Manual job intake with commercial placeholder.",
              "Clarification answer loop and activation visibility.",
              "Client-safe anonymous candidate card preview.",
            ]}
          />
        </section>
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Client session</span>
              <h2>Sign in to continue</h2>
            </div>
            <StatusBadge value="client" />
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
                placeholder="client@company.com"
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
              Enter Client Portal
            </button>
          </form>
          {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? (
            <ClientApiState status={result.status} error={loadableError(result)} />
          ) : null}
        </section>
      </div>
    </section>
  );
}

function DashboardPage({ session }: { session: ClientPortalSession }) {
  const companyState = useLoadable(fetchClientCompanyProfile, []);
  const dashboardState = useLoadable(fetchClientDashboard, []);
  const lastJobId = loadLastJobId();
  const recentShortlists =
    dashboardState.status === "ready" ? dashboardState.data.recentShortlists : [];
  const primaryShortlistRef =
    recentShortlists.length > 0 ? `/client/shortlists/${encodeURIComponent(recentShortlists[0].shortlistId)}` : "/client/shortlists";

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Client dashboard</span>
            <h2>Governed hiring intake workspace</h2>
            <p className="helper-copy shell-description">
              Keep the company profile current, submit new roles, answer clarification, and inspect activation blockers without leaving the client-safe boundary.
            </p>
          </div>
          <StatusBadge value={session.displayName} />
        </div>
        <div className="summary-grid board-summary-grid">
          <article className="metric-card">
            <span>Company profile</span>
            <strong>{companyState.status === "ready" ? "Ready" : "Draft"}</strong>
            <small>{companyState.status === "ready" ? companyState.data.name : "Create or refresh profile"}</small>
          </article>
          <article className="metric-card">
            <span>Active jobs</span>
            <strong>{dashboardState.status === "ready" ? dashboardState.data.activeJobCount : "..."}</strong>
            <small>Governed jobs currently visible in this client workspace</small>
          </article>
          <article className="metric-card">
            <span>Clarification</span>
            <strong>
              {dashboardState.status === "ready"
                ? dashboardState.data.pendingClarificationCount
                : lastJobId
                  ? "In progress"
                  : "Pending"}
            </strong>
            <small>
              {lastJobId
                ? `Latest job: ${lastJobId}`
                : "Open a job to answer follow-up questions"}
            </small>
          </article>
          <article className="metric-card">
            <span>Shortlists</span>
            <strong>{dashboardState.status === "ready" ? dashboardState.data.shortlistCount : "..."}</strong>
            <small>
              {dashboardState.status === "ready"
                ? `${dashboardState.data.pendingUnlockRequestCount} pending unlock requests`
                : "Client-safe shortlist review queue"}
            </small>
          </article>
        </div>
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Quick actions</span>
            <h2>Continue the hiring workflow</h2>
          </div>
        </div>
        <div className="client-action-grid">
          <Link className="board-action-card" to="/client/company-profile">
            <strong>Company profile</strong>
            <span>Create or refresh the client-owned company profile.</span>
          </Link>
          <Link className="board-action-card" to="/client/jobs/new">
            <strong>Submit job</strong>
            <span>Send a governed job intake with structured commercial terms.</span>
          </Link>
          <Link className="board-action-card" to={lastJobId ? `/client/jobs/${encodeURIComponent(lastJobId)}` : "/client/jobs/new"}>
            <strong>Track job status</strong>
            <span>Check blockers, clarification answers, and activation readiness.</span>
          </Link>
          <Link className="board-action-card" to={primaryShortlistRef}>
            <strong>Review shortlists</strong>
            <span>Open anonymous candidate cards, request unlock, and share feedback.</span>
          </Link>
          <Link className="board-action-card" to="/client/preferences">
            <strong>Preferences</strong>
            <span>Capture hiring and communication preferences for downstream consultant work.</span>
          </Link>
        </div>
      </section>

      {companyState.status === "ready" ? (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Current company</span>
              <h2>{companyState.data.name}</h2>
            </div>
            <NavLink className="secondary-link" to="/client/company-profile">
              Edit profile
            </NavLink>
          </div>
          <KeyValueList
            items={[
              ["Company ID", companyState.data.companyId],
              ["Industry", companyState.data.industry],
              ["HQ", companyState.data.headquartersLocation],
              ["Status", companyState.data.status],
              ["Updated", formatDate(companyState.data.updatedAt)],
            ]}
          />
        </section>
      ) : null}

      {dashboardState.status === "ready" ? (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Recent shortlists</span>
              <h2>Client review queue</h2>
            </div>
            <NavLink className="secondary-link" to="/client/shortlists">
              View all
            </NavLink>
          </div>
          {dashboardState.data.recentShortlists.length > 0 ? (
            <div className="client-action-grid">
              {dashboardState.data.recentShortlists.map((shortlist) => (
                <Link
                  key={shortlist.shortlistId}
                  className="board-action-card"
                  to={`/client/shortlists/${encodeURIComponent(shortlist.shortlistId)}`}
                >
                  <strong>{shortlist.title}</strong>
                  <span>{shortlist.status} · {shortlist.candidateCount} candidates</span>
                  <small>{formatDate(shortlist.sentAt ?? shortlist.createdAt)}</small>
                </Link>
              ))}
            </div>
          ) : (
            <ClientSafeState
              title="No client-visible shortlist yet"
              tone="neutral"
              detail="Shortlists appear here after consultant send-to-client."
            />
          )}
        </section>
      ) : null}
    </div>
  );
}

function CompanyProfilePage() {
  const state = useLoadable(fetchClientCompanyProfile, []);
  const [saveState, setSaveState] = useState<Loadable<ClientCompanyProfile>>({ status: "idle" });
  const [companyId, setCompanyId] = useState("");
  const [name, setName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [industry, setIndustry] = useState("");
  const [website, setWebsite] = useState("");
  const [headquartersLocation, setHeadquartersLocation] = useState("");
  const [sizeBand, setSizeBand] = useState("");
  const [paymentReliability, setPaymentReliability] = useState("");

  useEffect(() => {
    if (state.status !== "ready") {
      return;
    }
    setCompanyId(state.data.companyId);
    setName(state.data.name);
    setDisplayName(state.data.displayName ?? "");
    setIndustry(state.data.industry ?? "");
    setWebsite(state.data.website ?? "");
    setHeadquartersLocation(state.data.headquartersLocation ?? "");
    setSizeBand(state.data.sizeBand ?? "");
    setPaymentReliability(state.data.paymentReliability ?? "");
  }, [state]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaveState({ status: "loading" });
    const next = await saveClientCompanyProfile({
      companyId: emptyToNull(companyId),
      name,
      displayName: emptyToNull(displayName),
      industry: emptyToNull(industry),
      website: emptyToNull(website),
      headquartersLocation: emptyToNull(headquartersLocation),
      sizeBand: emptyToNull(sizeBand),
      paymentReliability: emptyToNull(paymentReliability),
    });
    setSaveState(next);
    if (next.status === "ready") {
      setCompanyId(next.data.companyId);
    }
  }

  const resolvedProfile = saveState.status === "ready"
    ? saveState.data
    : state.status === "ready"
      ? state.data
      : null;

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <span className="portal-eyebrow">Client company workspace</span>
        <h2>Company profile</h2>
        <p className="helper-copy shell-description">
          Keep the company context current so consultant review and downstream matching start from governed facts instead of ad hoc chat context.
        </p>
      </section>

      <section className="portal-panel">
        <form className="stack-form" onSubmit={onSubmit}>
          <label>
            Company ID
            <input
              value={companyId}
              onChange={(event) => setCompanyId(event.target.value)}
              placeholder="Leave blank to create or keep existing company"
            />
          </label>
          <label>
            Company name
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Example Tech Holdings"
            />
          </label>
          <label>
            Display name
            <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} />
          </label>
          <label>
            Industry
            <input value={industry} onChange={(event) => setIndustry(event.target.value)} />
          </label>
          <label>
            Website
            <input value={website} onChange={(event) => setWebsite(event.target.value)} />
          </label>
          <label>
            Headquarters location
            <input value={headquartersLocation} onChange={(event) => setHeadquartersLocation(event.target.value)} />
          </label>
          <label>
            Size band
            <input value={sizeBand} onChange={(event) => setSizeBand(event.target.value)} placeholder="500-1000 employees" />
          </label>
          <label>
            Payment reliability
            <input
              value={paymentReliability}
              onChange={(event) => setPaymentReliability(event.target.value)}
              placeholder="Known payer / new client / pending"
            />
          </label>
          <button type="submit" disabled={!name.trim()}>
            Save company profile
          </button>
        </form>
        {saveState.status !== "idle" && saveState.status !== "loading" && saveState.status !== "ready" ? (
          <ClientApiState status={saveState.status} error={loadableError(saveState)} />
        ) : null}
      </section>

      {resolvedProfile ? (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Current safe snapshot</span>
              <h2>{resolvedProfile.name}</h2>
            </div>
            <StatusBadge value={resolvedProfile.status} />
          </div>
          <KeyValueList
            items={[
              ["Company ID", resolvedProfile.companyId],
              ["Industry", resolvedProfile.industry],
              ["Website", resolvedProfile.website],
              ["HQ", resolvedProfile.headquartersLocation],
              ["Size band", resolvedProfile.sizeBand],
              ["Updated", formatDate(resolvedProfile.updatedAt)],
            ]}
          />
        </section>
      ) : state.status !== "loading" ? (
        <ClientSafeState
          title="No client-owned company profile yet"
          tone="neutral"
          detail="Save the form once to establish the governed company profile that future job submissions will reuse."
        />
      ) : null}
    </div>
  );
}

function NewJobPage() {
  const navigate = useNavigate();
  const companyState = useLoadable(fetchClientCompanyProfile, []);
  const [companyId, setCompanyId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [compensation, setCompensation] = useState("");
  const [clarificationQuestions, setClarificationQuestions] = useState("");
  const [commercialTerms, setCommercialTerms] = useState<CommercialTermsDraft>({ ...EMPTY_COMMERCIAL_TERMS });
  const [result, setResult] = useState<Loadable<ClientJobSubmissionStatus>>({ status: "idle" });

  useEffect(() => {
    if (companyState.status === "ready" && !companyId) {
      setCompanyId(companyState.data.companyId);
    }
  }, [companyId, companyState]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    const next = await createClientJob({
      companyId,
      title,
      description: emptyToNull(description),
      location: emptyToNull(location),
      compensation: emptyToNull(compensation),
      commercialTerms: serializeCommercialTerms(commercialTerms),
      clarificationQuestions: linesToList(clarificationQuestions),
    });
    setResult(next);
    if (next.status === "ready") {
      saveLastJobId(next.data.jobId);
      navigate(`/client/jobs/${encodeURIComponent(next.data.jobId)}`);
    }
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <span className="portal-eyebrow">Manual job intake</span>
        <h2>Submit a new role</h2>
        <p className="helper-copy shell-description">
          This form sends a governed client job submission, including commercial placeholder fields that the activation gate requires later.
        </p>
      </section>

      <section className="portal-panel portal-form-panel">
        <form className="stack-form" onSubmit={onSubmit}>
          <label>
            Company ID
            <input
              value={companyId}
              onChange={(event) => setCompanyId(event.target.value)}
              placeholder={companyState.status === "ready" ? companyState.data.companyId : "Client company id"}
            />
          </label>
          <label>
            Job title
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Head of Product"
            />
          </label>
          <label>
            Role brief
            <textarea
              rows={4}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Business context, hiring mandate, and top priorities"
            />
          </label>
          <label>
            Location
            <input value={location} onChange={(event) => setLocation(event.target.value)} placeholder="Shanghai / Singapore / remote" />
          </label>
          <label>
            Compensation
            <input value={compensation} onChange={(event) => setCompensation(event.target.value)} placeholder="Base + bonus or safe range" />
          </label>
          <label>
            Clarification questions
            <textarea
              rows={4}
              value={clarificationQuestions}
              onChange={(event) => setClarificationQuestions(event.target.value)}
              placeholder="One question per line"
            />
          </label>
          <div className="portal-panel client-nested-panel">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Commercial placeholder</span>
                <h3>Required for consultant activation gate</h3>
              </div>
            </div>
            <CommercialTermsFields value={commercialTerms} onChange={setCommercialTerms} />
            <KeyValueList items={summarizeCommercialTerms(serializeCommercialTerms(commercialTerms))} />
          </div>
          <button type="submit" disabled={!companyId.trim() || !title.trim()}>
            Submit job intake
          </button>
        </form>
        {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? (
          <ClientApiState status={result.status} error={loadableError(result)} />
        ) : null}
      </section>
    </div>
  );
}

function JobStatusPage() {
  const { jobId = "" } = useParams();
  const state = useLoadable(() => fetchClientJob(jobId), [jobId]);

  useEffect(() => {
    if (state.status === "ready") {
      saveLastJobId(state.data.jobId);
    }
  }, [state]);

  return renderLoadable(state, (job) => (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Client job status</span>
            <h2>{job.title}</h2>
            <p className="helper-copy shell-description">
              Track consultant follow-up, current blockers, and whether this job is ready to activate.
            </p>
          </div>
          <StatusBadge value={job.status} />
        </div>
      </section>

      <section className="portal-panel">
        <KeyValueList
          items={[
            ["Job ID", job.jobId],
            ["Company ID", job.companyId],
            ["Status", job.status],
            ["Created", formatDate(job.createdAt)],
            ["Updated", formatDate(job.updatedAt)],
            ["Activation allowed", job.activationAllowed ? "Yes" : "No"],
          ]}
        />
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Clarification loop</span>
            <h2>Questions and answers</h2>
          </div>
          <NavLink className="secondary-link" to={`/client/jobs/${encodeURIComponent(job.jobId)}/clarification`}>
            Answer clarification
          </NavLink>
        </div>
        <SafeList title="Clarification questions" items={job.clarificationQuestions} />
        <SafeList title="Submitted answers" items={job.clarificationAnswers} />
        {job.clarificationQuestions.length === 0 && job.clarificationAnswers.length === 0 ? (
          <ClientSafeState title="No clarification items yet" tone="neutral" />
        ) : null}
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Activation blockers</span>
            <h2>Readiness summary</h2>
          </div>
        </div>
        {job.blockerReasons.length > 0 ? (
          <SafeList title="Current blocker reasons" items={job.blockerReasons} />
        ) : (
          <ClientSafeState
            title={job.activationAllowed ? "Activation gate is clear" : "No explicit blocker reasons returned"}
            tone="neutral"
          />
        )}
      </section>
    </div>
  ));
}

function ClarificationPage() {
  const { jobId = "" } = useParams();
  const state = useLoadable(() => fetchClientJob(jobId), [jobId]);
  const [answersText, setAnswersText] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [compensation, setCompensation] = useState("");
  const [commercialTerms, setCommercialTerms] = useState<CommercialTermsDraft>({ ...EMPTY_COMMERCIAL_TERMS });
  const [result, setResult] = useState<Loadable<ClientJobSubmissionStatus>>({ status: "idle" });

  useEffect(() => {
    if (state.status !== "ready") {
      return;
    }
    setAnswersText(listToLines(state.data.clarificationAnswers));
  }, [state]);

  const resolvedJob = result.status === "ready"
    ? result.data
    : state.status === "ready"
      ? state.data
      : null;

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    const next = await answerClientJobClarification(jobId, {
      clarificationAnswers: linesToList(answersText),
      description: emptyToNull(description),
      location: emptyToNull(location),
      compensation: emptyToNull(compensation),
      commercialTerms: serializeCommercialTerms(commercialTerms),
    });
    setResult(next);
  }

  return renderLoadable(state, (job) => (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Clarification answers</span>
            <h2>{job.title}</h2>
            <p className="helper-copy shell-description">
              Respond with safe job details so consultant review can close blockers and continue the activation path.
            </p>
          </div>
          <StatusBadge value={job.status} />
        </div>
      </section>

      <section className="portal-panel">
        <SafeList title="Current questions" items={job.clarificationQuestions} />
      </section>

      <section className="portal-panel portal-form-panel">
        <form className="stack-form" onSubmit={onSubmit}>
          <label>
            Clarification answers
            <textarea
              rows={6}
              value={answersText}
              onChange={(event) => setAnswersText(event.target.value)}
              placeholder="One answer per line"
            />
          </label>
          <label>
            Updated role brief
            <textarea
              rows={4}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Optional: add missing context discovered during clarification"
            />
          </label>
          <label>
            Updated location
            <input value={location} onChange={(event) => setLocation(event.target.value)} />
          </label>
          <label>
            Updated compensation
            <input value={compensation} onChange={(event) => setCompensation(event.target.value)} />
          </label>
          <div className="portal-panel client-nested-panel">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Commercial placeholder</span>
                <h3>Refresh fee and contract assumptions</h3>
              </div>
            </div>
            <CommercialTermsFields value={commercialTerms} onChange={setCommercialTerms} />
            <KeyValueList items={summarizeCommercialTerms(serializeCommercialTerms(commercialTerms))} />
          </div>
          <button type="submit" disabled={linesToList(answersText).length === 0}>
            Submit clarification
          </button>
        </form>
        {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? (
          <ClientApiState status={result.status} error={loadableError(result)} />
        ) : null}
      </section>

      {resolvedJob ? (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Updated status</span>
              <h2>Current client-safe snapshot</h2>
            </div>
            <NavLink className="secondary-link" to={`/client/jobs/${encodeURIComponent(resolvedJob.jobId)}`}>
              Back to job
            </NavLink>
          </div>
          <KeyValueList
            items={[
              ["Status", resolvedJob.status],
              ["Activation allowed", resolvedJob.activationAllowed ? "Yes" : "No"],
              ["Updated", formatDate(resolvedJob.updatedAt)],
            ]}
          />
          <SafeList title="Answers on record" items={resolvedJob.clarificationAnswers} />
          <SafeList title="Remaining blockers" items={resolvedJob.blockerReasons} />
        </section>
      ) : null}
    </div>
  ));
}

function PreferencesPage() {
  const state = useLoadable(fetchClientPreferences, []);
  const [saveState, setSaveState] = useState<Loadable<ClientPreferenceResponse>>({ status: "idle" });
  const [hiringPreferences, setHiringPreferences] = useState("");
  const [communicationPreferences, setCommunicationPreferences] = useState("");
  const [commercialNotes, setCommercialNotes] = useState("");

  useEffect(() => {
    if (state.status !== "ready") {
      return;
    }
    const byKey = new Map(state.data.preferences.map((item) => [item.preferenceKey, item]));
    setHiringPreferences(byKey.get("hiring_preferences")?.preferenceValue ?? "");
    setCommunicationPreferences(byKey.get("communication_preferences")?.preferenceValue ?? "");
    setCommercialNotes(byKey.get("commercial_notes")?.preferenceValue ?? "");
  }, [state]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const preferences: ClientPreferenceItem[] = [
      { preferenceKey: "hiring_preferences", preferenceValue: hiringPreferences.trim(), notes: null },
      { preferenceKey: "communication_preferences", preferenceValue: communicationPreferences.trim(), notes: null },
      { preferenceKey: "commercial_notes", preferenceValue: commercialNotes.trim(), notes: null },
    ].filter((item) => item.preferenceValue.length > 0);
    setSaveState({ status: "loading" });
    setSaveState(await saveClientPreferences(preferences));
  }

  const resolvedState = saveState.status === "ready" ? saveState : state;

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <span className="portal-eyebrow">Client profile preferences</span>
        <h2>Preferences</h2>
        <p className="helper-copy shell-description">
          Capture durable hiring and communication preferences so consultant work stays aligned with backend-owned truth.
        </p>
      </section>

      <section className="portal-panel portal-form-panel">
        <form className="stack-form" onSubmit={onSubmit}>
          <label>
            Hiring preferences
            <textarea
              rows={4}
              value={hiringPreferences}
              onChange={(event) => setHiringPreferences(event.target.value)}
              placeholder="Preferred seniority, target profiles, must-have experience, geography, compensation posture"
            />
          </label>
          <label>
            Communication preferences
            <textarea
              rows={4}
              value={communicationPreferences}
              onChange={(event) => setCommunicationPreferences(event.target.value)}
              placeholder="Stakeholders, cadence, approval rhythm, escalation rules"
            />
          </label>
          <label>
            Commercial notes
            <textarea
              rows={4}
              value={commercialNotes}
              onChange={(event) => setCommercialNotes(event.target.value)}
              placeholder="Commercial assumptions, rate expectations, payment guardrails"
            />
          </label>
          <button type="submit">Save preferences</button>
        </form>
        {saveState.status !== "idle" && saveState.status !== "loading" && saveState.status !== "ready" ? (
          <ClientApiState status={saveState.status} error={loadableError(saveState)} />
        ) : null}
      </section>

      {resolvedState.status === "ready" ? (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Current snapshot</span>
              <h2>Saved preference keys</h2>
            </div>
          </div>
          {resolvedState.data.preferences.length > 0 ? (
            <KeyValueList
              items={resolvedState.data.preferences.map((item) => [item.preferenceKey, item.preferenceValue])}
            />
          ) : (
            <ClientSafeState
              title="No preferences saved yet"
              tone="neutral"
              detail="Save at least one preference block to populate the client profile layer."
            />
          )}
        </section>
      ) : null}
    </div>
  );
}

function ShortlistsPage() {
  const state = useLoadable(fetchClientShortlists, []);

  return renderLoadable(state, (page) => (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <span className="portal-eyebrow">Client shortlist review</span>
        <h2>Shortlists</h2>
        <p className="helper-copy shell-description">
          Review consultant-sent shortlists, open anonymous candidate cards, and progress unlock or feedback decisions.
        </p>
      </section>

      <section className="portal-panel">
        {page.items.length > 0 ? (
          <div className="client-action-grid">
            {page.items.map((shortlist) => (
              <Link
                key={shortlist.shortlistId}
                className="board-action-card"
                to={`/client/shortlists/${encodeURIComponent(shortlist.shortlistId)}`}
              >
                <strong>{shortlist.title}</strong>
                <span>{shortlist.status}</span>
                <small>{shortlist.candidateCount} candidates · sent {formatDate(shortlist.sentAt ?? shortlist.createdAt)}</small>
              </Link>
            ))}
          </div>
        ) : (
          <ClientSafeState
            title="No client-visible shortlists yet"
            tone="neutral"
            detail="Shortlists appear here after consultant send-to-client."
          />
        )}
      </section>
    </div>
  ));
}

function ShortlistDetailPage() {
  const { shortlistId = "" } = useParams();
  const [state, setState] = useState<Loadable<ClientShortlistDetail>>({ status: "loading" });
  const [unlockDrafts, setUnlockDrafts] = useState<Record<string, string>>({});
  const [unlockResults, setUnlockResults] = useState<Record<string, ClientUnlockRequest>>({});
  const [feedbackNotes, setFeedbackNotes] = useState<Record<string, string>>({});
  const [feedbackOutcomes, setFeedbackOutcomes] = useState<Record<string, string>>({});
  const [feedbackResults, setFeedbackResults] = useState<Record<string, ClientFeedbackResponse>>({});
  const [busyCardId, setBusyCardId] = useState<string | null>(null);

  async function refresh(markViewed = false) {
    if (!shortlistId) {
      setState({ status: "invalid_request", error: "Missing shortlist id." });
      return;
    }
    setState({ status: "loading" });
    const next = markViewed ? await markClientShortlistViewed(shortlistId) : await fetchClientShortlist(shortlistId);
    if (next.status === "ready" && !markViewed && next.data.status === "sent_to_client" && !next.data.clientViewedAt) {
      setState(await markClientShortlistViewed(shortlistId));
      return;
    }
    setState(next);
  }

  useEffect(() => {
    void refresh(false);
  }, [shortlistId]);

  async function handleSelect(card: ClientShortlistCard) {
    setBusyCardId(card.shortlistCandidateCardId);
    const result = await selectClientShortlistCandidate(shortlistId, card.shortlistCandidateCardId);
    setBusyCardId(null);
    if (result.status === "ready") {
      await refresh(false);
    }
  }

  async function handleUnlock(card: ClientShortlistCard) {
    const requestReason = (unlockDrafts[card.shortlistCandidateCardId] ?? "").trim();
    if (!requestReason) {
      return;
    }
    setBusyCardId(card.shortlistCandidateCardId);
    const result = await createClientUnlockRequest(shortlistId, card.shortlistCandidateCardId, requestReason);
    setBusyCardId(null);
    if (result.status === "ready") {
      setUnlockResults((current) => ({ ...current, [card.shortlistCandidateCardId]: result.data }));
      await refresh(false);
    }
  }

  async function handleFeedback(card: ClientShortlistCard) {
    const notes = (feedbackNotes[card.shortlistCandidateCardId] ?? "").trim();
    const outcome = feedbackOutcomes[card.shortlistCandidateCardId] ?? "maybe";
    if (!notes) {
      return;
    }
    setBusyCardId(card.shortlistCandidateCardId);
    const result = await submitClientFeedback(shortlistId, card.shortlistCandidateCardId, {
      outcome,
      notes,
      strengths: null,
      concerns: null,
      interviewRound: 1,
      interviewerName: null,
      interviewerRole: null,
    });
    setBusyCardId(null);
    if (result.status === "ready") {
      setFeedbackResults((current) => ({ ...current, [card.shortlistCandidateCardId]: result.data }));
      await refresh(false);
    }
  }

  return renderLoadable(state, (shortlist) => (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Client shortlist detail</span>
            <h2>{shortlist.title}</h2>
            <p className="helper-copy shell-description">
              Stay inside the client-safe boundary while deciding which anonymous candidates to progress.
            </p>
          </div>
          <StatusBadge value={shortlist.status} />
        </div>
        <KeyValueList
          items={[
            ["Shortlist ID", shortlist.shortlistId],
            ["Job ID", shortlist.jobId],
            ["Sent", formatDate(shortlist.sentAt)],
            ["Viewed", formatDate(shortlist.clientViewedAt)],
            ["Updated", formatDate(shortlist.updatedAt)],
          ]}
        />
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Anonymous candidates</span>
            <h2>Cards on this shortlist</h2>
          </div>
          <NavLink className="secondary-link" to="/client/shortlists">
            Back to shortlists
          </NavLink>
        </div>
        {shortlist.cards.length > 0 ? (
          <div className="workspace-stack">
            {shortlist.cards.map((card) => {
              const selectionAllowed = canClientSelectCandidate(shortlist.status);
              const unlockEligible = canClientRequestUnlock(shortlist.status, card.status);
              const interviewFeedbackEligible = canClientSubmitInterviewFeedback(shortlist.status, card.status);
              const identityAccessStatus = deriveIdentityAccessStatus(shortlist.status, card.approvedDisclosureRecordRef);
              const unlockBlockedByLifecycle =
                !canClientSelectCandidate(shortlist.status) && (card.status === "selected" || card.status === "unlocked");
              return (
              <article key={card.shortlistCandidateCardId} className="portal-panel client-nested-panel">
                <div className="section-header">
                  <div>
                    <span className="portal-eyebrow">{card.generalizedRoleFamily}</span>
                    <h3>{card.generalizedHeadline}</h3>
                  </div>
                  <StatusBadge value={card.status} />
                </div>
                <KeyValueList
                  items={[
                    ["Anonymous card", card.anonymousCardRef],
                    ["Seniority", card.generalizedSeniorityBand],
                    ["Region", card.generalizedLocationRegion],
                    ["Confidence", card.confidence],
                    ["Re-identification risk", card.reidentificationRiskSignal],
                    ["Unlock request status", card.unlockRequestStatus ?? "not_requested"],
                    ["Unlock stage", deriveUnlockStageLabel(card.unlockRequestStatus, card.approvedDisclosureRecordRef)],
                    ["Identity access", identityAccessStatus],
                    ["Unlock decision", card.unlockDecisionRef ?? "not_recorded"],
                    ["Disclosure record", card.approvedDisclosureRecordRef ?? "not_recorded"],
                  ]}
                />
                <p>{card.safeSummary}</p>
                <p className="helper-copy">{card.safeSkillSummary}</p>
                {shouldWarnApprovedWithoutDisclosure(card.unlockRequestStatus, card.approvedDisclosureRecordRef) ? (
                  <p className="helper-copy">
                    Unlock approval does not disclose identity until a disclosure record is issued and the shortlist enters contact unlocked.
                  </p>
                ) : null}
                <div className="client-action-grid">
                  <Link
                    className="board-action-card"
                    to={`/client/candidates/${encodeURIComponent(card.anonymousCardRef)}`}
                  >
                    <strong>Open anonymous detail</strong>
                    <span>Inspect the client-safe candidate card.</span>
                  </Link>
                  <button
                    type="button"
                    className="board-action-card"
                    onClick={() => void handleSelect(card)}
                    disabled={!selectionAllowed || busyCardId === card.shortlistCandidateCardId}
                  >
                    <strong>Select candidate</strong>
                    <span>Mark this card for deeper review and unlock handling.</span>
                  </button>
                  {card.approvedDisclosureRecordRef ? (
                    <Link
                      className="board-action-card"
                      to={`/client/disclosed-candidates/${encodeURIComponent(shortlist.shortlistId)}/${encodeURIComponent(card.shortlistCandidateCardId)}`}
                    >
                      <strong>Open disclosed detail</strong>
                      <span>Read identity-disclosed fields after workflow release.</span>
                    </Link>
                  ) : null}
                </div>
                {!selectionAllowed ? (
                  <p className="helper-copy">
                    Candidate selection is only available before the shortlist reaches contact unlocked, interviewing, or closed.
                  </p>
                ) : null}
                <label>
                  Unlock request reason
                  <textarea
                    rows={3}
                    disabled={!unlockEligible}
                    value={unlockDrafts[card.shortlistCandidateCardId] ?? ""}
                    onChange={(event) =>
                      setUnlockDrafts((current) => ({
                        ...current,
                        [card.shortlistCandidateCardId]: event.target.value,
                      }))
                    }
                    placeholder={
                      unlockEligible
                        ? "Explain why identity disclosure is required for this candidate."
                        : unlockBlockedByLifecycle
                          ? "Unlock requests close after the shortlist reaches contact unlocked, interviewing, or closed."
                          : "Select this candidate first to enable unlock request."
                    }
                  />
                </label>
                <button
                  type="button"
                  onClick={() => void handleUnlock(card)}
                  disabled={
                    !unlockEligible
                    || busyCardId === card.shortlistCandidateCardId
                    || !(unlockDrafts[card.shortlistCandidateCardId] ?? "").trim()
                  }
                >
                  Request unlock
                </button>
                {!unlockEligible ? (
                  <p className="helper-copy">
                    {unlockBlockedByLifecycle
                      ? "Unlock requests are only available before the shortlist reaches contact unlocked, interviewing, or closed."
                      : "Select this candidate first before requesting identity unlock."}
                  </p>
                ) : null}
                {unlockResults[card.shortlistCandidateCardId] ? (
                  <>
                    <KeyValueList
                      items={[
                        ["Latest unlock request", unlockResults[card.shortlistCandidateCardId].clientUnlockRequestId ?? "not_created"],
                        ["Request status", unlockResults[card.shortlistCandidateCardId].status],
                        ["Unlock stage", unlockResults[card.shortlistCandidateCardId].stage],
                        ["Requested at", formatDate(unlockResults[card.shortlistCandidateCardId].createdAt)],
                        ["Unlock decision", unlockResults[card.shortlistCandidateCardId].unlockDecisionRef ?? "not_recorded"],
                        ["Disclosure record", unlockResults[card.shortlistCandidateCardId].approvedDisclosureRecordRef ?? "not_recorded"],
                      ]}
                    />
                    {unlockResults[card.shortlistCandidateCardId].blockers.length > 0 ? (
                      <ul className="helper-copy">
                        {unlockResults[card.shortlistCandidateCardId].blockers.map((blocker) => (
                          <li key={`${card.shortlistCandidateCardId}-${blocker.code}`}>
                            {blocker.code}: {blocker.message}
                          </li>
                        ))}
                      </ul>
                    ) : null}
                  </>
                ) : null}
                <label>
                  Feedback outcome
                  <select
                    disabled={!interviewFeedbackEligible}
                    value={feedbackOutcomes[card.shortlistCandidateCardId] ?? "maybe"}
                    onChange={(event) =>
                      setFeedbackOutcomes((current) => ({
                        ...current,
                        [card.shortlistCandidateCardId]: event.target.value,
                      }))
                    }
                  >
                    <option value="strong_yes">Strong yes</option>
                    <option value="yes">Yes</option>
                    <option value="maybe">Maybe</option>
                    <option value="weak_no">Weak no</option>
                    <option value="strong_no">Strong no</option>
                  </select>
                </label>
                <label>
                  Feedback notes
                  <textarea
                    rows={4}
                    disabled={!interviewFeedbackEligible}
                    value={feedbackNotes[card.shortlistCandidateCardId] ?? ""}
                    onChange={(event) =>
                      setFeedbackNotes((current) => ({
                        ...current,
                        [card.shortlistCandidateCardId]: event.target.value,
                      }))
                    }
                    placeholder={
                      interviewFeedbackEligible
                        ? "Structured interview feedback after identity unlock."
                        : "Interview feedback opens after identity unlock and interview-stage progression."
                    }
                  />
                </label>
                <button
                  type="button"
                  onClick={() => void handleFeedback(card)}
                  disabled={
                    !interviewFeedbackEligible
                    || busyCardId === card.shortlistCandidateCardId
                    || !(feedbackNotes[card.shortlistCandidateCardId] ?? "").trim()
                  }
                >
                  Submit feedback
                </button>
                {!interviewFeedbackEligible ? (
                  <p className="helper-copy">
                    Interview feedback is only available after identity unlock, when the shortlist has entered contact unlocked or interviewing.
                  </p>
                ) : null}
                {feedbackResults[card.shortlistCandidateCardId] ? (
                  <KeyValueList
                    items={[
                      ["Feedback outcome", feedbackResults[card.shortlistCandidateCardId].outcome],
                      ["Feedback created", formatDate(feedbackResults[card.shortlistCandidateCardId].createdAt)],
                    ]}
                  />
                ) : null}
              </article>
              );
            })}
          </div>
        ) : (
          <ClientSafeState title="No shortlist cards available" tone="neutral" />
        )}
      </section>
    </div>
  ));
}

function ClientSafeCandidateCardPage() {
  const { anonymousCardRef = "" } = useParams();
  const decodedCardRef = useMemo(() => {
    try {
      return decodeURIComponent(anonymousCardRef);
    } catch {
      return anonymousCardRef;
    }
  }, [anonymousCardRef]);
  const [result, setResult] = useState<ClientSafeCandidateCardResult>({ status: "unavailable" });
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (!isAnonymousCardRef(decodedCardRef)) {
      setResult({ status: "invalid_ref" });
      setIsLoading(false);
      return;
    }

    let current = true;
    const abortController = new AbortController();
    setIsLoading(true);
    void fetchClientSafeCandidateCard(decodedCardRef, abortController.signal)
      .then((next) => {
        if (current) {
          setResult(next);
        }
      })
      .finally(() => {
        if (current) {
          setIsLoading(false);
        }
      });

    return () => {
      current = false;
      abortController.abort();
    };
  }, [decodedCardRef]);

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Shortlist preview</span>
            <h2>Client-safe anonymous candidate card</h2>
          </div>
          <NavLink to="/client/shortlists" className="secondary-link">
            Back to shortlists
          </NavLink>
        </div>
      </section>
      <section className="candidate-card-surface">
        {isLoading ? (
          <ClientSafeState title="Checking candidate card availability" tone="neutral" />
        ) : result.status === "ready" ? (
          <ClientSafeCard card={result.card} />
        ) : (
          <SafeStateForResult status={result.status} />
        )}
      </section>
    </div>
  );
}

function SafeStateForResult({
  status,
}: {
  status: Exclude<ClientSafeCandidateCardResult["status"], "ready">;
}) {
  if (status === "invalid_ref") {
    return <ClientSafeState title="Invalid anonymous card reference" tone="warning" />;
  }
  if (status === "denied") {
    return <ClientSafeState title="Access denied for this client-safe view" tone="warning" />;
  }
  if (status === "unauthenticated") {
    return <ClientSafeState title="Client session required before loading this card" tone="warning" />;
  }
  if (status === "failed") {
    return <ClientSafeState title="Candidate card could not be loaded" tone="warning" />;
  }
  return <ClientSafeState title="Client-safe candidate card unavailable" tone="neutral" />;
}

function ClientSafeCard({ card }: { card: ClientSafeCandidateCard }) {
  return (
    <article className="candidate-card">
      <div className="candidate-card-meta" aria-label="Client-safe card metadata">
        <span>{card.clientAlias}</span>
        <span>{card.redactionLevel}</span>
        <span>{card.projectionVersion}</span>
      </div>
      <h3>{card.generalizedHeadline}</h3>
      <dl className="safe-fields">
        <div>
          <dt>Role family</dt>
          <dd>{card.generalizedRoleFamily}</dd>
        </div>
        <div>
          <dt>Seniority</dt>
          <dd>{card.generalizedSeniorityBand}</dd>
        </div>
        <div>
          <dt>Region</dt>
          <dd>{card.generalizedLocationRegion}</dd>
        </div>
      </dl>
      <section>
        <h4>Summary</h4>
        <p>{card.safeSummary}</p>
      </section>
      <section>
        <h4>Skills</h4>
        <p>{card.safeSkillSummary}</p>
      </section>
      <SafeList title="Evidence" items={card.safeEvidenceSummaries} />
      <SafeList title="Match narrative" items={card.safeMatchNarratives} />
    </article>
  );
}

export function ClientPortal() {
  const location = useLocation();
  const [session, setSession] = useState<ClientPortalSession | null>(() => loadClientPortalSession());

  useEffect(() => {
    function syncSession() {
      setSession(loadClientPortalSession());
    }
    window.addEventListener("storage", syncSession);
    return () => window.removeEventListener("storage", syncSession);
  }, []);

  function onSignedIn(nextSession: AuthSession) {
    saveAccessToken(nextSession.accessToken, "client");
    saveClientPortalSession(nextSession);
    setSession(loadClientPortalSession());
  }

  function onLogout() {
    saveAccessToken("", "client");
    clearClientPortalSession();
    saveLastJobId("");
    setSession(null);
  }

  if (location.pathname === "/client/sign-in") {
    return session ? <Navigate to="/client" replace /> : <ClientSignInPage onSignedIn={onSignedIn} />;
  }

  if (!session) {
    return <Navigate to="/client/sign-in" replace />;
  }

  return (
    <ClientPortalLayout session={session} onLogout={onLogout}>
      <Routes>
        <Route index element={<DashboardPage session={session} />} />
        <Route path="company-profile" element={<CompanyProfilePage />} />
        <Route path="preferences" element={<PreferencesPage />} />
        <Route path="jobs/new" element={<NewJobPage />} />
        <Route path="jobs/:jobId" element={<JobStatusPage />} />
        <Route path="jobs/:jobId/clarification" element={<ClarificationPage />} />
        <Route path="shortlists" element={<ShortlistsPage />} />
        <Route path="shortlists/:shortlistId" element={<ShortlistDetailPage />} />
        <Route path="candidates/:anonymousCardRef" element={<ClientSafeCandidateCardPage />} />
        <Route path="candidate-cards/:anonymousCardRef" element={<ClientSafeCandidateCardPage />} />
        <Route path="disclosed-candidates/:shortlistId/:cardId" element={<ClientDisclosedCandidatePage />} />
        <Route path="*" element={<Navigate to="/client" replace />} />
      </Routes>
    </ClientPortalLayout>
  );
}
