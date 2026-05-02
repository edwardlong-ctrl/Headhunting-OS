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
  clearPortalSession,
  loadPortalSession,
  savePortalSession,
} from "../../auth/authSessionStorage";
import {
  fetchConsultantDashboard,
  type ConsultantDashboard,
} from "../../api/consultantDashboard";
import {
  createConsultantCompanyContact,
  createConsultantCompany,
  fetchConsultantCompany,
  listConsultantCompanies,
  type ConsultantCompanyListFilters,
  updateConsultantCompany,
  type ConsultantCompanyDetail,
  type ConsultantCompanySummary,
} from "../../api/consultantCompanies";
import {
  activateConsultantJob,
  createConsultantJobRequirement,
  createConsultantJobScorecard,
  createConsultantJob,
  createConsultantJobUpdatePayload,
  fetchConsultantJobActivationGate,
  fetchConsultantJob,
  listConsultantJobs,
  type ConsultantJobActivationGate,
  type ConsultantJobListFilters,
  updateConsultantJob,
  type ConsultantJobDetail,
  type ConsultantJobSummary,
} from "../../api/consultantJobs";
import {
  fetchConsultantCandidate,
  listConsultantCandidates,
  type ConsultantCandidateListFilters,
  type ConsultantCandidateDetail,
  type ConsultantCandidateSummary,
} from "../../api/consultantCandidates";
import {
  createConsultantShortlistUpdatePayload,
  createConsultantShortlist,
  fetchConsultantShortlist,
  listConsultantShortlists,
  type ConsultantShortlistListFilters,
  updateConsultantShortlist,
  type ConsultantShortlistDetail,
  type ConsultantShortlistSummary,
} from "../../api/consultantShortlists";
import {
  fetchConsultantWorkflow,
  fetchConsultantAuditDrawer,
  fetchConsultantWorkflowEntityState,
  type ConsultantWorkflowFilters,
  type ConsultantAuditDrawer,
  type ConsultantWorkflowEntityState,
  type ConsultantWorkflowEvent,
  type ConsultantWorkflowTimeline,
} from "../../api/consultantWorkflow";
import { listConsultantFollowUps, type ConsultantFollowUp } from "../../api/consultantFollowUps";
import {
  CONSULTANT_MATCH_DIMENSIONS,
  createConsultantMatchGenerationPayload,
  generateConsultantMatch,
  listConsultantMatchReports,
  type ConsultantMatchReport,
} from "../../api/consultantMatching";
import {
  extractConsultantIntake,
  fetchConsultantIntakeReview,
  listConsultantIntakeQueue,
  publishConsultantIntake,
  submitConsultantIntakeDecision,
  type ConsultantCleanFact,
  type ConsultantIntakePublish,
  type ConsultantIntakeQueueItem,
  type ConsultantIntakeReview,
  type ConsultantIntakeRun,
} from "../../api/consultantIntake";
import {
  parseConsultantDocument,
  uploadConsultantDocument,
  type ConsultantDocumentUpload,
  type ConsultantParsedDocument,
} from "../../api/consultantDocuments";
import { type ApiResult } from "../../api/http";
import {
  SHORTLIST_BUILDER_INITIAL_STATUS,
  CONSULTANT_WORKFLOW_ENTITY_TYPE_OPTIONS,
  canSaveShortlistBuilder,
  describeWorkflowPageWindow,
  isShortlistBuilderEditable,
} from "./consultantPortalUtils";

type Loadable<T> = ApiResult<T> | { status: "idle" | "loading" };

type NavItem = { to: string; label: string };
type IntakeLane = "candidate" | "company" | "job" | "call-note" | "feedback";
const DEFAULT_PAGE_SIZE = 10;

const navItems: NavItem[] = [
  { to: "/consultant/dashboard", label: "Dashboard" },
  { to: "/consultant/intake", label: "AI Intake" },
  { to: "/consultant/talent", label: "Talent Pool" },
  { to: "/consultant/companies", label: "Companies" },
  { to: "/consultant/jobs", label: "Jobs" },
  { to: "/consultant/matching", label: "Matching" },
  { to: "/consultant/outreach", label: "Outreach" },
  { to: "/consultant/shortlists", label: "Shortlists" },
  { to: "/consultant/follow-ups", label: "Follow-ups" },
  { to: "/consultant/workflow", label: "Workflow" },
  { to: "/consultant/placements", label: "Placements" },
  { to: "/consultant/commission", label: "Commission" },
  { to: "/consultant/reports", label: "Reports" },
  { to: "/consultant/settings", label: "Settings" },
];

const intakeLaneConfig: Record<
  IntakeLane,
  {
    label: string;
    route: string;
    title: string;
    description: string;
    sourceType: string;
    packetType: string;
    intendedEntityType: "CANDIDATE" | "COMPANY" | "JOB";
    titlePlaceholder: string;
    allowedSourceTypes: Array<{ value: string; label: string }>;
  }
> = {
  candidate: {
    label: "Candidate packet",
    route: "/consultant/intake/upload/candidate",
    title: "Upload candidate material",
    description: "CVs, notes, LinkedIn exports, and legacy evidence all enter the governed candidate path here.",
    sourceType: "candidate_resume",
    packetType: "candidate",
    intendedEntityType: "CANDIDATE",
    titlePlaceholder: "Candidate CV / notes",
    allowedSourceTypes: [
      { value: "candidate_resume", label: "Candidate resume" },
      { value: "linkedin_text", label: "LinkedIn text" },
      { value: "wechat_note", label: "Chat or note" },
      { value: "old_system_export", label: "Legacy export" },
    ],
  },
  company: {
    label: "Company packet",
    route: "/consultant/intake/upload/company",
    title: "Upload company material",
    description: "Account research, company briefs, and target account evidence stay inside the same governed queue.",
    sourceType: "company_material",
    packetType: "company",
    intendedEntityType: "COMPANY",
    titlePlaceholder: "Company research / brief",
    allowedSourceTypes: [
      { value: "company_material", label: "Company material" },
    ],
  },
  job: {
    label: "Job packet",
    route: "/consultant/intake/upload/job",
    title: "Upload job material",
    description: "Job descriptions, intake scorecards, and search brief evidence feed the governed job path.",
    sourceType: "jd",
    packetType: "job",
    intendedEntityType: "JOB",
    titlePlaceholder: "Job description / intake brief",
    allowedSourceTypes: [
      { value: "jd", label: "Job description" },
    ],
  },
  "call-note": {
    label: "Call note packet",
    route: "/consultant/intake/upload/call-note",
    title: "Upload consultant call notes",
    description: "Call summaries remain packetized and can be targeted to candidate, company, or job review contexts.",
    sourceType: "call_note",
    packetType: "call_note",
    intendedEntityType: "CANDIDATE",
    titlePlaceholder: "Call note / conversation summary",
    allowedSourceTypes: [
      { value: "call_note", label: "Call note" },
    ],
  },
  feedback: {
    label: "Feedback packet",
    route: "/consultant/intake/upload/feedback",
    title: "Upload interview feedback",
    description: "Interview feedback remains governed evidence and can route into candidate, company, or job review lanes.",
    sourceType: "interview_feedback",
    packetType: "feedback",
    intendedEntityType: "CANDIDATE",
    titlePlaceholder: "Interview feedback / evaluation",
    allowedSourceTypes: [
      { value: "interview_feedback", label: "Interview feedback" },
    ],
  },
};

type BoardContext = {
  title: string;
  subtitle: string;
};

type CommercialTermsDraft = {
  feeModel: string;
  feeRangeOrRate: string;
  paymentTerms: string;
  contractStatus: string;
  notes: string;
};

const EMPTY_COMMERCIAL_TERMS: CommercialTermsDraft = {
  feeModel: "",
  feeRangeOrRate: "",
  paymentTerms: "",
  contractStatus: "",
  notes: "",
};

function resolveBoardContext(pathname: string): BoardContext {
  if (
    pathname.includes("/consultant/intake")
    || pathname.includes("/consultant/talent")
  ) {
    return {
      title: "Intake 与 Candidate Intelligence",
      subtitle: "Consultant Portal — Intake & Talent",
    };
  }
  if (
    pathname.includes("/consultant/companies")
    || pathname.includes("/consultant/jobs")
    || pathname.includes("/consultant/matching")
    || pathname.includes("/consultant/outreach")
    || pathname.includes("/consultant/shortlists")
    || pathname.includes("/consultant/follow-ups")
    || pathname.includes("/consultant/workflow")
    || pathname.includes("/consultant/placements")
    || pathname.includes("/consultant/commission")
    || pathname.includes("/consultant/reports")
  ) {
    return {
      title: "Jobs / Matching / Workflow",
      subtitle: "Consultant Portal — Jobs & Deal Flow",
    };
  }
  return {
    title: "Consultant Dashboard",
    subtitle: "Consultant Portal — Unified Operating Surface",
  };
}

function compactCount(value: number): string {
  return new Intl.NumberFormat(undefined, { notation: "compact" }).format(value);
}

function percentOf(value: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return Math.max(8, Math.round((value / total) * 100));
}

function useLoadable<T>(loader: () => Promise<ApiResult<T>>, deps: unknown[]): Loadable<T> {
  const [state, setState] = useState<Loadable<T>>({ status: "loading" });

  useEffect(() => {
    let current = true;
    setState({ status: "loading" });
    void loader().then((result) => {
      if (current) {
        setState(result);
      }
    });
    return () => {
      current = false;
    };
  }, deps);

  return state;
}

function formatDate(value?: string | null): string {
  if (!value) return "Not available";
  try {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(value));
  } catch {
    return value;
  }
}

function describeWorkflowTransition(item: ConsultantWorkflowEvent): string {
  const before = item.beforeStatus ?? "unknown";
  const after = item.afterStatus ?? "unknown";
  return `${before} -> ${after}`;
}

function describeTransitionOption(option: ConsultantWorkflowEntityState["transitionOptions"][number]): string {
  const target = option.targetStatus ?? "unknown";
  if (option.allowed) {
    return `${option.actionCode} -> ${target}`;
  }
  const blocker = option.blockers[0];
  return `${option.actionCode} blocked -> ${target}${blocker?.safeReason ? ` (${blocker.safeReason})` : ""}`;
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
  if (!Object.values(payload).some((item) => item.length > 0)) {
    return null;
  }
  return JSON.stringify(payload);
}

function commercialTermsSummary(value?: string | null): string[] {
  const parsed = parseCommercialTerms(value);
  return [
    parsed.feeModel ? `Fee model: ${parsed.feeModel}` : null,
    parsed.feeRangeOrRate ? `Fee range or rate: ${parsed.feeRangeOrRate}` : null,
    parsed.paymentTerms ? `Payment terms: ${parsed.paymentTerms}` : null,
    parsed.contractStatus ? `Contract status: ${parsed.contractStatus}` : null,
    parsed.notes ? `Notes: ${parsed.notes}` : null,
  ].filter((item): item is string => item !== null);
}

function publishSurfaceLabel(review: ConsultantIntakeReview): string {
  switch (review.intendedEntityType) {
    case "COMPANY":
      return "company";
    case "JOB":
      return "job";
    default:
      return "candidate";
  }
}

function publishTargetLabel(review: ConsultantIntakeReview): string {
  const existingMatch = review.cleanFacts.find((fact) => fact.resolvedEntityId);
  if (existingMatch?.resolvedEntityId) {
    return `Existing ${publishSurfaceLabel(review)} ${existingMatch.resolvedEntityId}`;
  }
  if (review.intendedEntityType === "COMPANY") {
    return "A governed company write-back will be created";
  }
  if (review.intendedEntityType === "JOB") {
    return "A governed job write-back will be created";
  }
  return "A governed candidate profile will be created";
}

function reviewReadyForPublish(review: ConsultantIntakeReview): boolean {
  return review.cleanFacts.some((fact) => fact.canonicalWriteStatus === "ready_for_publish");
}

function renderLoadable<T>(state: Loadable<T>, renderReady: (data: T) => React.ReactNode) {
  if (state.status === "idle" || state.status === "loading") {
    return <SafeState title="Loading consultant workspace" tone="neutral" />;
  }
  if (state.status !== "ready") {
    return <ApiState status={state.status} error={loadableError(state)} />;
  }
  return renderReady(state.data);
}

function loadableError(state: Loadable<unknown>): string | undefined {
  return "error" in state ? state.error : undefined;
}

function ApiState({
  status,
  error,
}: {
  status: Exclude<Loadable<unknown>["status"], "ready" | "idle" | "loading">;
  error?: string;
}) {
  const title =
    status === "unauthenticated"
      ? "Consultant session required"
      : status === "denied"
        ? "Access denied for this consultant view"
        : status === "invalid_request"
          ? "The request is invalid"
          : status === "unavailable"
            ? "This consultant surface is unavailable"
            : "The request failed";
  return (
    <SafeState
      title={title}
      tone={status === "failed" ? "warning" : "neutral"}
      detail={error}
    />
  );
}

function ConsultantPortalLayout({
  children,
  session,
  onLogout,
}: {
  children: React.ReactNode;
  session: AuthSession;
  onLogout: () => void;
}) {
  const location = useLocation();
  const board = resolveBoardContext(location.pathname);

  return (
    <section className="portal-layout consultant-shell">
      <header className="consultant-shell-header">
        <div className="consultant-brand-lockup">
          <div className="consultant-mark">A</div>
          <div className="consultant-heading-copy">
            <h1>Consultant 端 · {board.title}</h1>
            <p>{board.subtitle}</p>
          </div>
        </div>
        <div className="consultant-top-badges">
          <span className="consultant-top-badge">Trust &amp; Compliance</span>
          <span className="consultant-top-badge">Protected by Design</span>
          <span className="consultant-top-badge">AI-Powered</span>
        </div>
      </header>

      <div className="consultant-frame">
        <aside className="consultant-sidebar">
          <section className="consultant-user-panel">
            <div className="consultant-nav-brand">
              <strong>Consultant</strong>
              <span>{board.subtitle}</span>
            </div>
            <div className="consultant-user-meta">
              <span className="portal-eyebrow">Current consultant</span>
              <h2>{session.displayName}</h2>
            </div>
            <p>
              {session.portalRole} session active for {session.organizationId}.
            </p>
            <button type="button" className="secondary-button" onClick={onLogout}>
              Sign out
            </button>
          </section>
          <nav className="consultant-subnav" aria-label="Consultant workspace routes">
            {navItems.map((item, index) => (
              <NavLink key={item.to} to={item.to}>
                <span className="consultant-nav-index">{index + 1}</span>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>
        <div className="consultant-content">{children}</div>
      </div>
    </section>
  );
}

function ConsultantSignInPage({
  onSignedIn,
}: {
  onSignedIn: (session: AuthSession) => void;
}) {
  const [organizationId, setOrganizationId] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [result, setResult] = useState<Loadable<AuthSession>>({ status: "idle" });
  const navigate = useNavigate();
  const signInError = result.status === "ready" || result.status === "idle" || result.status === "loading"
    ? undefined
    : loadableError(result);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    const next = await login({
      organizationId: organizationId.trim() || undefined,
      email: email.trim(),
      password,
      portalRole: "consultant",
    });
    setResult(next);
    if (next.status === "ready") {
      onSignedIn(next.data);
      navigate("/consultant/dashboard", { replace: true });
    }
  }

  return (
    <section className="portal-layout consultant-auth-shell">
      <header className="portal-heading">
        <span className="portal-eyebrow">Consultant Portal</span>
        <h1>Unified consultant session handoff</h1>
        <p className="portal-copy">
          Enter through the consultant sign-in boundary first, then continue inside the governed
          operating workspace. Tokens remain behind the session boundary and are never pasted into
          the portal UI.
        </p>
      </header>
      <div className="consultant-auth-grid">
        <section className="portal-panel auth-value-panel">
          <span className="portal-eyebrow">Task 24 operating surface</span>
          <h2>One portal, one session, one intake queue</h2>
          <p className="helper-copy">
            Consultant Board A and Board B stay inside the same portal context. Intake, talent,
            companies, jobs, follow-ups, and workflow now all assume a real session takeover path.
          </p>
          <div className="card-list quick-action-grid">
            <article className="data-card compact-card">
              <h3>Daily operating surface</h3>
              <p>Dashboard, intake queue, and follow-up work start after sign-in instead of inside a debug form.</p>
            </article>
            <article className="data-card compact-card">
              <h3>Organization-safe boundary</h3>
              <p>The backend now resolves the consultant organization boundary instead of asking users to paste an ID.</p>
            </article>
          </div>
        </section>
        <section className="portal-panel sign-in-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Consultant session</span>
              <h2>Sign in to continue</h2>
            </div>
            <StatusBadge value="consultant" />
          </div>
          <form className="stack-form sign-in-form" onSubmit={onSubmit}>
            <label>
              Organization ID
              <input
                value={organizationId}
                onChange={(event) => setOrganizationId(event.target.value)}
                placeholder="Optional unless your account needs an org context"
              />
            </label>
            <label>
              Work email
              <input
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="consultant@company.com"
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
            <button
              type="submit"
              disabled={!email.trim() || !password}
            >
              Enter Consultant Portal
            </button>
          </form>
          <p className="helper-copy">
            Leave organization blank for single-org accounts. If local dev login fails closed, enter the
            consultant org UUID explicitly.
          </p>
          <p className="helper-copy">
            Local consultant dev org: <code>00000000-0000-0000-0000-000000240001</code>
          </p>
          {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? (
            <ApiState status={result.status} error={loadableError(result)} />
          ) : null}
        </section>
      </div>
    </section>
  );
}

function DashboardPage() {
  const state = useLoadable(fetchConsultantDashboard, []);
  const queue = useLoadable(() => listConsultantIntakeQueue(6), []);
  const followUps = useLoadable(listConsultantFollowUps, []);
  const candidates = useLoadable(listConsultantCandidates, []);
  const jobs = useLoadable(listConsultantJobs, []);
  const workflow = useLoadable(fetchConsultantWorkflow, []);
  return renderLoadable(state, (dashboard) => (
    <DashboardContent
      dashboard={dashboard}
      queueItems={queue.status === "ready" ? queue.data.items : []}
      followUps={followUps.status === "ready" ? followUps.data.items : []}
      candidateItems={candidates.status === "ready" ? candidates.data.items : []}
      jobItems={jobs.status === "ready" ? jobs.data.items : []}
      workflowItems={workflow.status === "ready" ? workflow.data.items : []}
    />
  ));
}

function DashboardContent({
  dashboard,
  queueItems,
  followUps,
  candidateItems,
  jobItems,
  workflowItems,
}: {
  dashboard: ConsultantDashboard;
  queueItems: ConsultantIntakeQueueItem[];
  followUps: ConsultantFollowUp[];
  candidateItems: ConsultantCandidateSummary[];
  jobItems: ConsultantJobSummary[];
  workflowItems: ConsultantWorkflowEvent[];
}) {
  const reviewCount = queueItems.filter((item) => item.stage === "in_review" || item.stage === "extracted").length;
  const readyCount = queueItems.filter((item) => item.stage === "ready_for_publish").length;
  const focusItems = dashboard.blockedActions.slice(0, 4);
  const nextFollowUps = followUps.slice(0, 4);
  const candidatePreview = candidateItems.slice(0, 4);
  const jobPreview = jobItems.slice(0, 4);
  const workflowPreview = workflowItems.slice(0, 5);
  const queuePreview = queueItems.slice(0, 4);
  const funnelTotal = Math.max(
    dashboard.pendingFollowUpCount,
    dashboard.shortlistCount,
    dashboard.activeJobCount,
    dashboard.candidateCount,
    1,
  );

  return (
    <div className="workspace-stack">
      <section className="portal-panel board-hero-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Consultant dashboard</span>
            <h2>Unified operating board</h2>
            <p className="helper-copy shell-description">
              Intake, review, talent, jobs, matching, shortlist, and workflow stay inside one
              consultant workspace and follow the v2.0 board language.
            </p>
          </div>
          <StatusBadge value="This week" />
        </div>
        <div className="summary-grid board-summary-grid">
          <MetricCard label="Total candidates" value={dashboard.candidateCount} />
          <MetricCard label="Under review" value={reviewCount} />
          <MetricCard label="Approved" value={readyCount} />
          <MetricCard label="Talent pool" value={dashboard.candidateCount} />
        </div>
        <div className="dashboard-insight-grid">
          <article className="data-card compact-card emphasis-card">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Intake funnel</span>
                <h3>Board health</h3>
              </div>
            </div>
            <div className="mini-chart-list">
              <MetricBar label="Candidates sourced" value={dashboard.candidateCount} total={funnelTotal} tone="violet" />
              <MetricBar label="Active jobs" value={dashboard.activeJobCount} total={funnelTotal} tone="blue" />
              <MetricBar label="Shortlists" value={dashboard.shortlistCount} total={funnelTotal} tone="purple" />
              <MetricBar label="Follow-up load" value={dashboard.pendingFollowUpCount} total={funnelTotal} tone="teal" />
            </div>
          </article>
          <article className="data-card compact-card">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Needs attention</span>
                <h3>Priority actions</h3>
              </div>
            </div>
            {focusItems.length === 0 ? (
              <EmptyState title="No blocked consultant actions" />
            ) : (
              <div className="board-list">
                {focusItems.map((item) => (
                  <Link
                    key={`${item.entityType}-${item.entityId}-${item.reasonCode}`}
                    to={item.route}
                    className="board-list-item"
                  >
                    <div>
                      <strong>{item.title}</strong>
                      <p>{item.safeReason}</p>
                    </div>
                    <StatusBadge value={item.severity} />
                  </Link>
                ))}
              </div>
            )}
          </article>
          <article className="data-card compact-card">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Recent follow-up</span>
                <h3>Next moves</h3>
              </div>
              <NavLink className="text-link" to="/consultant/follow-ups">
                View all
              </NavLink>
            </div>
            {nextFollowUps.length === 0 ? (
              <EmptyState title="No live follow-up items" />
            ) : (
              <div className="board-list">
                {nextFollowUps.map((item) => (
                  <Link key={`${item.entityType}-${item.entityId}-${item.followUpType}`} to={item.route} className="board-list-item">
                    <div>
                      <strong>{item.title}</strong>
                      <p>{item.safeReason}</p>
                    </div>
                    <StatusBadge value={item.status} />
                  </Link>
                ))}
              </div>
            )}
          </article>
        </div>
      </section>

      <section className="consultant-board-grid">
        <BoardPreviewPanel
          number={2}
          eyebrow="AI Intake Center"
          title="Governed queue"
          action={<NavLink className="text-link" to="/consultant/intake">Open intake</NavLink>}
        >
          <div className="portal-chip-row">
            {Object.values(intakeLaneConfig).slice(0, 5).map((lane) => (
              <span key={lane.route} className="portal-chip">
                {lane.label}
              </span>
            ))}
          </div>
          {queuePreview.length > 0 ? (
            <MiniDataTable
              columns={["Packet", "Stage"]}
              rows={queuePreview.map((item) => [
                <Link
                  key={item.informationPacketId}
                  to={`/consultant/intake/review/${encodeURIComponent(item.informationPacketId)}`}
                  className="text-link"
                >
                  {item.title}
                </Link>,
                <StatusBadge value={item.stage} />,
              ])}
            />
          ) : (
            <EmptyState title="No intake packets in queue" />
          )}
        </BoardPreviewPanel>

        <BoardPreviewPanel
          number={3}
          eyebrow="Talent Pool"
          title="Structured profiles"
          action={<NavLink className="text-link" to="/consultant/talent">View pool</NavLink>}
        >
          {candidatePreview.length > 0 ? (
            <MiniDataTable
              columns={["Candidate", "Status", "Privacy"]}
              rows={candidatePreview.map((item) => [
                <Link key={item.candidateId} to={`/consultant/talent/${item.candidateId}`} className="text-link">
                  {item.candidateId}
                </Link>,
                <StatusBadge value={item.status} />,
                item.privacyStatus,
              ])}
            />
          ) : (
            <EmptyState title="No talent profiles available" />
          )}
        </BoardPreviewPanel>

        <BoardPreviewPanel
          number={4}
          eyebrow="Review Control"
          title="Approval discipline"
        >
          <SafeList
            title="Gate rules"
            items={[
              "Clean facts and source highlight stay visible in the same review surface.",
              "Bulk approve is allowed only for low-risk fields and remains auditable.",
              "Client-visible changes stay blocked until canonical write readiness is true.",
            ]}
          />
        </BoardPreviewPanel>

        <BoardPreviewPanel
          number={5}
          eyebrow="Jobs List"
          title="Open roles and matching entry"
          action={<NavLink className="text-link" to="/consultant/jobs">Open jobs</NavLink>}
        >
          {jobPreview.length > 0 ? (
            <MiniDataTable
              columns={["Job", "Status", "Company"]}
              rows={jobPreview.map((item) => [
                <Link key={item.jobId} to={`/consultant/jobs/${item.jobId}`} className="text-link">
                  {item.title}
                </Link>,
                <StatusBadge value={item.status} />,
                item.companyId,
              ])}
            />
          ) : (
            <EmptyState title="No jobs open yet" />
          )}
        </BoardPreviewPanel>

        <BoardPreviewPanel
          number={6}
          eyebrow="Follow-up Center"
          title="Recovery and reminders"
          action={<NavLink className="text-link" to="/consultant/follow-ups">View queue</NavLink>}
        >
          {nextFollowUps.length > 0 ? (
            <MiniDataTable
              columns={["Title", "Type", "Status"]}
              rows={nextFollowUps.map((item) => [
                <Link key={`${item.entityId}-${item.followUpType}`} to={item.route} className="text-link">
                  {item.title}
                </Link>,
                item.followUpType,
                <StatusBadge value={item.status} />,
              ])}
            />
          ) : (
            <EmptyState title="No follow-up items waiting" />
          )}
        </BoardPreviewPanel>

        <BoardPreviewPanel
          number={7}
          eyebrow="Workflow Timeline"
          title="Recent governed actions"
          action={<NavLink className="text-link" to="/consultant/workflow">Open timeline</NavLink>}
        >
          {workflowPreview.length > 0 ? (
            <div className="workflow-preview-list">
              {workflowPreview.map((item) => (
                <div key={`${item.entityType}-${item.entityId}-${item.occurredAt}`} className="workflow-preview-item">
                  <div>
                    <strong>{item.actionCode}</strong>
                    <p>{item.entityType}:{item.entityId}</p>
                  </div>
                  <span>{formatDate(item.occurredAt)}</span>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="No workflow events yet" />
          )}
        </BoardPreviewPanel>
      </section>

      <section className="portal-panel board-footer-panel">
        <div className="board-footer-grid">
          <BoardActionCard
            title="Manage Companies"
            description="Grow and maintain client relationships."
            route="/consultant/companies"
          />
          <BoardActionCard
            title="Manage Jobs"
            description="Intake, scope, and publish roles."
            route="/consultant/jobs"
          />
          <BoardActionCard
            title="AI Matching"
            description="Score 1-5 to find best-fit talent."
            route="/consultant/matching"
          />
          <BoardActionCard
            title="Outreach & Engagement"
            description="Connect with candidates and keep momentum."
            route="/consultant/outreach"
          />
          <BoardActionCard
            title="Shortlist & Send"
            description="Client-safe previews and pre-send checks."
            route="/consultant/shortlists"
          />
          <BoardActionCard
            title="Workflow & Audit"
            description="Track every event and decision."
            route="/consultant/workflow"
          />
        </div>
      </section>
    </div>
  );
}

function IntakeHomePage() {
  const navigate = useNavigate();
  const queue = useLoadable(() => listConsultantIntakeQueue(12), []);
  const queueItems = queue.status === "ready" ? queue.data.items : [];
  const uploadedCount = queueItems.filter((item) => item.stage === "uploaded").length;
  const reviewCount = queueItems.filter((item) => item.stage === "in_review" || item.stage === "extracted").length;
  const readyCount = queueItems.filter((item) => item.stage === "ready_for_publish").length;
  const failedCount = queueItems.filter((item) => item.stage === "extract_failed").length;

  return (
    <div className="workspace-stack">
      <section className="portal-panel dashboard-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">AI intake center</span>
            <h2>Turn messy evidence into structured review tasks</h2>
          </div>
          <StatusBadge value="New intake" />
        </div>
        <div className="summary-grid board-summary-grid">
          <MetricCard label="Uploaded" value={uploadedCount} />
          <MetricCard label="In review" value={reviewCount} />
          <MetricCard label="Approved" value={readyCount} />
          <MetricCard label="Needs fix" value={failedCount} />
        </div>
        <div className="intake-quick-actions">
          {Object.values(intakeLaneConfig).map((lane) => (
            <button key={lane.route} type="button" className="intake-action-card" onClick={() => navigate(lane.route)}>
              <strong>{lane.label}</strong>
              <span>{lane.description}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Intake queue</span>
            <h2>Review packets already inside the portal</h2>
          </div>
        </div>
        {renderLoadable(queue, (result) =>
          result.items.length === 0 ? (
            <EmptyState title="No consultant intake packets yet" />
          ) : (
            <DataTable
              headers={["Title", "Stage", "Source", "Updated"]}
              rows={result.items.map((item) => [
                <button
                  type="button"
                  className="inline-link-button"
                  onClick={() => navigate(`/consultant/intake/review/${encodeURIComponent(item.informationPacketId)}`)}
                >
                  {item.title}
                </button>,
                <QueueStageBadge item={item} />,
                item.sourceType,
                formatDate(item.updatedAt),
              ])}
            />
          ),
        )}
      </section>
    </div>
  );
}

function IntakeUploadPage() {
  const { lane = "candidate" } = useParams<{ lane: IntakeLane }>();
  const navigate = useNavigate();
  const preset = intakeLaneConfig[(lane in intakeLaneConfig ? lane : "candidate") as IntakeLane];
  const [sourceType, setSourceType] = useState(preset.sourceType);
  const [origin, setOrigin] = useState("consultant_upload");
  const [packetType, setPacketType] = useState(preset.packetType);
  const [intendedEntityType, setIntendedEntityType] = useState(preset.intendedEntityType);
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [upload, setUpload] = useState<Loadable<ConsultantDocumentUpload>>({ status: "idle" });
  const [parsed, setParsed] = useState<Loadable<ConsultantParsedDocument>>({ status: "idle" });
  const [extractResult, setExtractResult] = useState<Loadable<ConsultantIntakeRun>>({ status: "idle" });

  useEffect(() => {
    setSourceType(preset.sourceType);
    setPacketType(preset.packetType);
    setIntendedEntityType(preset.intendedEntityType);
    setTitle("");
    setFile(null);
    setUpload({ status: "idle" });
    setParsed({ status: "idle" });
    setExtractResult({ status: "idle" });
  }, [preset]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) return;
    const formData = new FormData();
    formData.set("file", file);
    formData.set("sourceType", sourceType);
    formData.set("origin", origin);
    formData.set("packetType", packetType);
    formData.set("intendedEntityType", intendedEntityType);
    if (title.trim()) formData.set("title", title.trim());
    setUpload({ status: "loading" });
    const next = await uploadConsultantDocument(formData);
    setUpload(next);
    setParsed({ status: "idle" });
    setExtractResult({ status: "idle" });
  }

  async function onParse() {
    if (upload.status !== "ready") return;
    setParsed({ status: "loading" });
    const next = await parseConsultantDocument(upload.data.sourceItemId);
    setParsed(next);
  }

  async function onExtract() {
    if (upload.status !== "ready" || !upload.data.informationPacketId) return;
    setExtractResult({ status: "loading" });
    const next = await extractConsultantIntake(upload.data.informationPacketId);
    setExtractResult(next);
    if (next.status === "ready") {
      navigate(`/consultant/intake/review/${encodeURIComponent(next.data.informationPacketId)}`);
    }
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel portal-form-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Talent intake · upload</span>
            <h2>{preset.title}</h2>
          </div>
          <StatusBadge value={preset.packetType} />
        </div>
        <p className="helper-copy">{preset.description}</p>
        <form className="stack-form" onSubmit={onSubmit}>
          <label>
            Source type
            <select value={sourceType} onChange={(event) => setSourceType(event.target.value)}>
              {preset.allowedSourceTypes.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Origin
            <input value={origin} onChange={(event) => setOrigin(event.target.value)} />
          </label>
          {(packetType === "call_note" || packetType === "feedback") ? (
            <label>
              Target entity lane
              <select value={intendedEntityType} onChange={(event) => setIntendedEntityType(event.target.value as "CANDIDATE" | "COMPANY" | "JOB")}>
                <option value="CANDIDATE">Candidate</option>
                <option value="COMPANY">Company</option>
                <option value="JOB">Job</option>
              </select>
            </label>
          ) : null}
          <label>
            Title
            <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder={preset.titlePlaceholder} />
          </label>
          <label>
            Document
            <input type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
          </label>
          <button type="submit" disabled={!file}>Upload document</button>
        </form>
      </section>

      {upload.status === "ready" && (
        <section className="portal-panel">
          <h2>Upload result</h2>
          <dl className="mini-meta">
            <div><dt>Source item</dt><dd>{upload.data.sourceItemId}</dd></div>
            <div><dt>Packet</dt><dd>{upload.data.informationPacketId ?? "No packet id returned"}</dd></div>
            <div><dt>Packet type</dt><dd>{packetType}</dd></div>
            <div><dt>Target lane</dt><dd>{intendedEntityType}</dd></div>
            <div><dt>Scan</dt><dd>{upload.data.scanStatus}</dd></div>
          </dl>
          <p className="helper-copy">
            This packet is now stored in the backend intake queue for the signed-in consultant organization.
          </p>
          <div className="button-row">
            <button onClick={onParse}>Parse document</button>
            {upload.data.informationPacketId ? (
              <button className="secondary-button" onClick={onExtract}>
                Run extract and open review
              </button>
            ) : null}
          </div>
        </section>
      )}
      {upload.status !== "idle" && upload.status !== "loading" && upload.status !== "ready" ? <ApiState status={upload.status} error={loadableError(upload)} /> : null}

      {parsed.status === "ready" && (
        <section className="portal-panel">
          <h2>Parse status</h2>
          <dl className="mini-meta">
            <div><dt>Status</dt><dd>{parsed.data.processingStatus}</dd></div>
            <div><dt>Parser</dt><dd>{parsed.data.parserName} {parsed.data.parserVersion}</dd></div>
            <div><dt>Chunks</dt><dd>{parsed.data.chunkCount}</dd></div>
          </dl>
        </section>
      )}
      {extractResult.status !== "idle" && extractResult.status !== "loading" && extractResult.status !== "ready" ? <ApiState status={extractResult.status} error={loadableError(extractResult)} /> : null}
    </div>
  );
}

function IntakeReviewPage() {
  const { packetId = "" } = useParams();
  const state = useLoadable(() => fetchConsultantIntakeReview(packetId), [packetId]);
  return renderLoadable(state, (review) => <ReviewContent review={review} packetId={packetId} />);
}

function ReviewContent({ review, packetId }: { review: ConsultantIntakeReview; packetId: string }) {
  const [publish, setPublish] = useState<Loadable<ConsultantIntakePublish>>({ status: "idle" });
  const [reason, setReason] = useState("consultant_review_approved");
  const [refreshNonce, setRefreshNonce] = useState(0);
  const refreshed = useLoadable(() => fetchConsultantIntakeReview(packetId), [packetId, refreshNonce]);
  const currentReview = refreshed.status === "ready" ? refreshed.data : review;

  async function onPublish(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPublish({ status: "loading" });
    const next = await publishConsultantIntake(packetId, {
      reason,
    });
    setPublish(next);
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel review-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Intake review</span>
            <h2>Governed clean facts</h2>
          </div>
          <StatusBadge value={currentReview.intendedEntityType} />
        </div>
        <div className="review-stepper">
          <span className="review-step review-step-active">Intake</span>
          <span className="review-step review-step-active">Review</span>
          <span className="review-step">Confirm</span>
          <span className="review-step">Add to pool</span>
        </div>
        <dl className="mini-meta">
          <div><dt>Packet</dt><dd>{currentReview.informationPacketId}</dd></div>
          <div><dt>Run</dt><dd>{currentReview.extractionRunId}</dd></div>
          <div><dt>Facts</dt><dd>{currentReview.cleanFactCount}</dd></div>
        </dl>
      </section>

      <section className="card-list">
        {currentReview.cleanFacts.map((fact, index) => (
          <FactCard key={`${fact.claimId ?? fact.claimFieldName}-${index}`} fact={fact} onSubmitted={() => setRefreshNonce((value) => value + 1)} />
        ))}
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Publish gate</span>
            <h2>Governed {publishSurfaceLabel(currentReview)} write-back</h2>
          </div>
        </div>
        <form className="stack-form" onSubmit={onPublish}>
          <div className="publish-summary">
            <div>
              <span className="mini-label">Publish target</span>
              <strong>{publishTargetLabel(currentReview)}</strong>
            </div>
            <div>
              <span className="mini-label">Write readiness</span>
              <strong>{reviewReadyForPublish(currentReview) ? "Ready for approved facts" : "Blocked until review issues are resolved"}</strong>
            </div>
          </div>
          <label>
            Reason
            <input value={reason} onChange={(event) => setReason(event.target.value)} />
          </label>
          <button type="submit" disabled={!reviewReadyForPublish(currentReview)}>
            Publish {publishSurfaceLabel(currentReview)} path
          </button>
        </form>
        <p className="helper-copy">
          Approved {publishSurfaceLabel(currentReview)} facts stay inside the governed review-to-write path and only land in canonical records after this gate passes.
        </p>
        {publish.status === "ready" && (
          <article className="data-card compact-card">
            <h3>Publish result</h3>
            <dl className="mini-meta">
              <div><dt>Packet</dt><dd>{publish.data.informationPacketId}</dd></div>
              <div><dt>Canonical writes</dt><dd>{publish.data.canonicalWriteCount}</dd></div>
            </dl>
            <SafeList title="Canonical write statuses" items={publish.data.canonicalWriteStatuses} />
            <SafeList title="Direct writes" items={publish.data.directWrites} />
          </article>
        )}
        {publish.status !== "idle" && publish.status !== "loading" && publish.status !== "ready" ? <ApiState status={publish.status} error={loadableError(publish)} /> : null}
      </section>
    </div>
  );
}

function FactCard({ fact, onSubmitted }: { fact: ConsultantCleanFact; onSubmitted: () => void }) {
  const [decision, setDecision] = useState("approved");
  const [riskTier, setRiskTier] = useState(fact.suggestedRiskTier || "T1_LOW_RISK");
  const [reason, setReason] = useState("consultant_reviewed");
  const [bulkFlag, setBulkFlag] = useState(false);
  const [result, setResult] = useState<Loadable<{ reviewEventId: string }>>({ status: "idle" });

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!fact.claimId) return;
    setResult({ status: "loading" });
    const next = await submitConsultantIntakeDecision(fact.claimId, {
      decision,
      riskTier,
      reason,
      bulkFlag,
    });
    setResult(next as Loadable<{ reviewEventId: string }>);
    if (next.status === "ready") {
      onSubmitted();
    }
  }

  return (
    <article className="data-card fact-card">
      <div className="fact-card-main">
        <section className="fact-card-column">
          <div className="card-row">
            <div>
              <span className="portal-eyebrow">{fact.targetFieldPath}</span>
              <h3>{fact.claimFieldName}</h3>
              <p>{fact.rationale ?? "No consultant-safe rationale provided."}</p>
            </div>
            <div className="status-group">
              <StatusBadge value={fact.suggestedRiskTier} />
              {fact.conflictsWithCanonical ? <StatusBadge value="conflict" tone="warning" /> : null}
            </div>
          </div>
          <dl className="mini-meta fact-meta-grid">
            <div><dt>Proposed value</dt><dd>{fact.proposedValue}</dd></div>
            <div><dt>Verification</dt><dd>{fact.suggestedVerificationStatus}</dd></div>
            <div><dt>Resolution</dt><dd>{fact.entityResolutionStatus}</dd></div>
            <div><dt>Latest review</dt><dd>{fact.latestReviewDecision ?? "Not reviewed"}</dd></div>
            <div><dt>Canonical write</dt><dd>{fact.canonicalWriteStatus}</dd></div>
            <div><dt>Blocked reason</dt><dd>{fact.publishBlockedReason ?? "Ready"}</dd></div>
          </dl>
        </section>
        <section className="highlight-block fact-card-column">
          <h4>Source &amp; reasoning</h4>
          <p>{fact.sourceHighlight.safeSnippet}</p>
          <span>{fact.sourceHighlight.locator}</span>
        </section>
        <form className="stack-form fact-decision-panel" onSubmit={onSubmit}>
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Approve flow</span>
              <h4>Consultant decision</h4>
            </div>
          </div>
          <label>
            Decision
            <select value={decision} onChange={(event) => setDecision(event.target.value)} disabled={!fact.claimId}>
              <option value="approved">approved</option>
              <option value="rejected">rejected</option>
              <option value="escalated">escalated</option>
              <option value="needs_confirmation">needs_confirmation</option>
            </select>
          </label>
          <label>
            Risk tier
            <select value={riskTier} onChange={(event) => setRiskTier(event.target.value)} disabled={!fact.claimId}>
              <option value="T0_AUTOMATED_CLEANUP">T0_AUTOMATED_CLEANUP</option>
              <option value="T1_LOW_RISK">T1_LOW_RISK</option>
              <option value="T2_MEDIUM_RISK">T2_MEDIUM_RISK</option>
              <option value="T3_HIGH_RISK">T3_HIGH_RISK</option>
              <option value="T4_TRANSACTION_LEGAL_BLOCKING">T4_TRANSACTION_LEGAL_BLOCKING</option>
            </select>
          </label>
          <label>
            Reason
            <input value={reason} onChange={(event) => setReason(event.target.value)} disabled={!fact.claimId} />
          </label>
          <label className="checkbox-field">
            <input type="checkbox" checked={bulkFlag} onChange={(event) => setBulkFlag(event.target.checked)} disabled={!fact.claimId} />
            Bulk approve low-risk fields
          </label>
          <button type="submit" disabled={!fact.claimId}>Approve / Next</button>
        </form>
      </div>
      {!fact.claimId ? <p className="helper-copy">This fact is view-only because no claim id is available for decision write-back.</p> : null}
      {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? <ApiState status={result.status} error={loadableError(result)} /> : null}
    </article>
  );
}

function TalentListPage() {
  const [statusFilter, setStatusFilter] = useState("");
  const [offset, setOffset] = useState(0);
  const filters: ConsultantCandidateListFilters = {
    status: statusFilter || undefined,
    limit: DEFAULT_PAGE_SIZE,
    offset,
  };
  const state = useLoadable(() => listConsultantCandidates(filters), [statusFilter, offset]);
  return renderLoadable(state, (result) => (
    <ListPageShell
      title="Talent Pool"
      eyebrow="Consultant candidate index"
      description="Structured candidate profiles that already passed the governed intake boundary."
      actions={<NavLink className="secondary-link" to="/consultant/intake/upload/candidate">Add Profile</NavLink>}
    >
      <ListToolbar>
        <label>
          Status
          <select
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setOffset(0);
            }}
          >
            <option value="">All statuses</option>
            <option value="active">active</option>
            <option value="draft">draft</option>
            <option value="merged">merged</option>
            <option value="archived">archived</option>
          </select>
        </label>
      </ListToolbar>
      <PaginationSummary totalCount={result.totalCount} limit={result.limit} offset={result.offset} />
      <DataTable
        headers={["Candidate", "Status", "Privacy", "Created"]}
        rows={result.items.map((item) => [
          <Link to={`/consultant/talent/${item.candidateId}`} className="text-link">{item.candidateId}</Link>,
          <StatusBadge value={item.status} />,
          item.privacyStatus,
          formatDate(item.createdAt),
        ])}
      />
      <PaginationControls
        hasMore={result.hasMore}
        offset={result.offset}
        limit={result.limit}
        onPrevious={() => setOffset((value) => Math.max(0, value - DEFAULT_PAGE_SIZE))}
        onNext={() => setOffset((value) => value + DEFAULT_PAGE_SIZE)}
      />
    </ListPageShell>
  ));
}

function TalentDetailPage() {
  const { candidateId = "" } = useParams();
  const state = useLoadable(() => fetchConsultantCandidate(candidateId), [candidateId]);
  return renderLoadable(state, (candidate) => (
    <DetailPageShell title="Candidate detail" eyebrow="Consultant talent view">
      <KeyValueList
        items={[
          ["Candidate id", candidate.candidateId],
          ["Status", candidate.status],
          ["Privacy", candidate.privacyStatus],
          ["Current profile", candidate.currentProfileId],
          ["Profile version", candidate.profileVersion],
          ["Owner consultant", candidate.ownerConsultantId],
          ["Last activity", formatDate(candidate.lastActivityAt)],
          ["Do not contact", candidate.doNotContactReason],
          ["Merged into", candidate.mergedIntoCandidateId],
        ]}
      />
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Overview</span>
            <h2>Current profile facts</h2>
          </div>
        </div>
        <DataTable
          headers={["Field", "Value", "Status", "Reviewed"]}
          rows={candidate.overview.map((item) => [
            item.label,
            item.value ?? "Not available",
            <StatusBadge value={item.status} />,
            formatDate(item.lastReviewedAt),
          ])}
        />
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Evidence</span>
            <h2>Source-backed lineage</h2>
          </div>
        </div>
        <DataTable
          headers={["Field", "Source", "Trust", "Created"]}
          rows={candidate.evidence.map((item) => [
            item.fieldPath,
            `${item.sourceType} · ${item.sourceId}`,
            item.sourceTrust ?? item.provenanceLabel ?? "Not available",
            formatDate(item.createdAt),
          ])}
        />
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Conflicts</span>
            <h2>Source conflicts</h2>
          </div>
        </div>
        {candidate.conflicts.length > 0 ? (
          <DataTable
            headers={["Field", "Severity", "Resolution", "Values"]}
            rows={candidate.conflicts.map((item) => [
              item.fieldPath,
              <StatusBadge value={item.severity} />,
              item.resolutionStatus,
              item.conflictingValues.join(" / "),
            ])}
          />
        ) : <EmptyState title="No active candidate conflicts" />}
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Stale Info</span>
            <h2>Refresh-required fields</h2>
          </div>
        </div>
        {candidate.staleInfo.length > 0 ? (
          <DataTable
            headers={["Field", "Reason", "Review by", "Last confirmed"]}
            rows={candidate.staleInfo.map((item) => [
              item.fieldPath,
              item.staleReason,
              formatDate(item.reviewBy),
              formatDate(item.lastConfirmedAt),
            ])}
          />
        ) : <EmptyState title="No stale candidate fields" />}
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Follow-ups</span>
            <h2>Recommended next actions</h2>
          </div>
        </div>
        {candidate.followUps.length > 0 ? (
          <DataTable
            headers={["Field", "Type", "Reason", "Action"]}
            rows={candidate.followUps.map((item) => [
              item.fieldPath,
              item.followUpType,
              item.reason,
              item.recommendedAction,
            ])}
          />
        ) : <EmptyState title="No follow-up prompts for this candidate" />}
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">History</span>
            <h2>Candidate intelligence history</h2>
          </div>
        </div>
        <DataTable
          headers={["Event", "Field", "Description", "Occurred"]}
          rows={candidate.history.map((item) => [
            item.eventType,
            item.fieldPath ?? "Profile",
            item.description,
            formatDate(item.occurredAt),
          ])}
        />
      </section>
      <AuditDrawerButton entityType="candidate" entityId={candidate.candidateId} />
    </DetailPageShell>
  ));
}

function CompaniesPage() {
  const [statusFilter, setStatusFilter] = useState("");
  const [offset, setOffset] = useState(0);
  const filters: ConsultantCompanyListFilters = {
    status: statusFilter || undefined,
    limit: DEFAULT_PAGE_SIZE,
    offset,
  };
  const state = useLoadable(() => listConsultantCompanies(filters), [statusFilter, offset]);
  const [result, setResult] = useState<Loadable<ConsultantCompanyDetail>>({ status: "idle" });
  const [name, setName] = useState("");

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    setResult(await createConsultantCompany({ name, status: "active" }));
  }

  return (
    <div className="workspace-stack">
      {renderLoadable(state, (result) => (
        <ListPageShell
          title="Company Pool"
          eyebrow="Consultant company workspace"
          description="Target accounts, relationship health, and company-level detail stay inside the same board."
        >
          <ListToolbar>
            <label>
              Status
              <select
                value={statusFilter}
                onChange={(event) => {
                  setStatusFilter(event.target.value);
                  setOffset(0);
                }}
              >
                <option value="">All statuses</option>
                <option value="active">active</option>
                <option value="prospect">prospect</option>
                <option value="inactive">inactive</option>
              </select>
            </label>
          </ListToolbar>
          <PaginationSummary totalCount={result.totalCount} limit={result.limit} offset={result.offset} />
          <DataTable
            headers={["Name", "Status", "Contacts", "Jobs"]}
            rows={result.items.map((item) => [
              <Link to={`/consultant/companies/${item.companyId}`} className="text-link">{item.name}</Link>,
              <StatusBadge value={item.status} />,
              item.contactCount,
              item.jobCount,
            ])}
          />
          <PaginationControls
            hasMore={result.hasMore}
            offset={result.offset}
            limit={result.limit}
            onPrevious={() => setOffset((value) => Math.max(0, value - DEFAULT_PAGE_SIZE))}
            onNext={() => setOffset((value) => value + DEFAULT_PAGE_SIZE)}
          />
        </ListPageShell>
      ))}
      <section className="portal-panel">
        <h2>Create company</h2>
        <form className="stack-form" onSubmit={onCreate}>
          <label>
            Name
            <input value={name} onChange={(event) => setName(event.target.value)} />
          </label>
          <button type="submit" disabled={!name.trim()}>Create</button>
        </form>
        {result.status === "ready" ? <p className="helper-copy">Created {result.data.name}.</p> : null}
      </section>
    </div>
  );
}

function CompanyDetailPage() {
  const { companyId = "" } = useParams();
  const state = useLoadable(() => fetchConsultantCompany(companyId), [companyId]);
  return renderLoadable(state, (company) => <CompanyDetailWorkspace initialCompany={company} />);
}

function JobsPage() {
  const [statusFilter, setStatusFilter] = useState("");
  const [companyFilter, setCompanyFilter] = useState("");
  const [offset, setOffset] = useState(0);
  const jobFilters: ConsultantJobListFilters = {
    status: statusFilter || undefined,
    companyId: companyFilter || undefined,
    limit: DEFAULT_PAGE_SIZE,
    offset,
  };
  const jobs = useLoadable(() => listConsultantJobs(jobFilters), [statusFilter, companyFilter, offset]);
  const companies = useLoadable(() => listConsultantCompanies({ limit: 100, offset: 0 }), []);
  const [companyId, setCompanyId] = useState("");
  const [title, setTitle] = useState("");
  const [result, setResult] = useState<Loadable<ConsultantJobDetail>>({ status: "idle" });

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    setResult(await createConsultantJob({ companyId, title, status: "submitted" }));
  }

  return (
    <div className="workspace-stack">
      {renderLoadable(jobs, (result) => (
        <ListPageShell
          title="Jobs"
          eyebrow="Consultant job workspace"
          description="Track open roles, matching readiness, and shortlist entry from one table."
        >
          <ListToolbar>
            <label>
              Status
              <select
                value={statusFilter}
                onChange={(event) => {
                  setStatusFilter(event.target.value);
                  setOffset(0);
                }}
              >
                <option value="">All statuses</option>
                <option value="submitted">submitted</option>
                <option value="open">open</option>
                <option value="closed">closed</option>
              </select>
            </label>
            {companies.status === "ready" ? (
              <label>
                Company
                <select
                  value={companyFilter}
                  onChange={(event) => {
                    setCompanyFilter(event.target.value);
                    setOffset(0);
                  }}
                >
                  <option value="">All companies</option>
                  {companies.data.items.map((company) => (
                    <option key={company.companyId} value={company.companyId}>{company.name}</option>
                  ))}
                </select>
              </label>
            ) : null}
          </ListToolbar>
          <PaginationSummary totalCount={result.totalCount} limit={result.limit} offset={result.offset} />
          <DataTable
            headers={["Title", "Status", "Company", "Matching"]}
            rows={result.items.map((item) => [
              <Link to={`/consultant/jobs/${item.jobId}`} className="text-link">{item.title}</Link>,
              <StatusBadge value={item.status} />,
              item.companyId,
              <Link to={`/consultant/jobs/${item.jobId}/matching`} className="text-link">Open matching</Link>,
            ])}
          />
          <PaginationControls
            hasMore={result.hasMore}
            offset={result.offset}
            limit={result.limit}
            onPrevious={() => setOffset((value) => Math.max(0, value - DEFAULT_PAGE_SIZE))}
            onNext={() => setOffset((value) => value + DEFAULT_PAGE_SIZE)}
          />
        </ListPageShell>
      ))}
      {renderLoadable(companies, (companyResult) => (
        <section className="portal-panel">
          <h2>Create job</h2>
          <form className="stack-form" onSubmit={onCreate}>
            <label>
              Title
              <input value={title} onChange={(event) => setTitle(event.target.value)} />
            </label>
            <label>
              Company
              <select value={companyId} onChange={(event) => setCompanyId(event.target.value)}>
                <option value="">Select a company</option>
                {companyResult.items.map((company) => (
                  <option key={company.companyId} value={company.companyId}>{company.name}</option>
                ))}
              </select>
            </label>
            <button type="submit" disabled={!title.trim() || !companyId}>Create</button>
          </form>
          {result.status === "ready" ? <p className="helper-copy">Created {result.data.title}.</p> : null}
        </section>
      ))}
    </div>
  );
}

function JobDetailPage() {
  const { jobId = "" } = useParams();
  const state = useLoadable(() => fetchConsultantJob(jobId), [jobId]);
  return renderLoadable(state, (job) => <JobDetailWorkspace initialJob={job} />);
}

function JobIntakePage() {
  const { jobId = "" } = useParams();
  const state = useLoadable(() => fetchConsultantJob(jobId), [jobId]);
  return renderLoadable(state, (job) => {
    return <JobIntakeWorkspace initialJob={job} />;
  });
}

function JobIntakeWorkspace({ initialJob }: { initialJob: ConsultantJobDetail }) {
  const [job, setJob] = useState(initialJob);
  const [gateState, setGateState] = useState<Loadable<ConsultantJobActivationGate>>({ status: "idle" });
  const [activateState, setActivateState] = useState<Loadable<ConsultantJobDetail>>({ status: "idle" });
  const [activationReason, setActivationReason] = useState("");

  useEffect(() => {
    let cancelled = false;
    setGateState({ status: "loading" });
    void fetchConsultantJobActivationGate(initialJob.jobId).then((next) => {
      if (!cancelled) {
        setGateState(next);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [initialJob.jobId, job.updatedAt]);

  async function onActivate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActivateState({ status: "loading" });
    const next = await activateConsultantJob(job.jobId, emptyToNull(activationReason));
    setActivateState(next);
    if (next.status === "ready") {
      setJob(next.data);
      setActivationReason("");
    }
  }

  const fallbackClarifications = [
    !job.description ? "Missing AI-parsed job description." : null,
    job.requirements.length === 0 ? "No requirements captured yet." : null,
    !job.scorecard?.dimensions ? "Scorecard dimensions still need confirmation." : null,
    !job.location ? "Location is not confirmed." : null,
    !job.commercialTerms ? "Commercial terms placeholder has not been recorded." : null,
  ].filter((item): item is string => item !== null);

  const activationChecklist = [
    job.description ? "Role brief is present." : "Role brief is still missing.",
    job.requirements.length > 0 ? "At least one requirement exists." : "Requirements are still empty.",
    job.scorecard ? "Scorecard has been created." : "Scorecard has not been created yet.",
    job.commercialTerms ? "Commercial placeholder has been captured." : "Commercial placeholder is still missing.",
    job.status === "activated" ? "Job is activated." : `Current status is ${job.status}.`,
  ];

  const gate = gateState.status === "ready" ? gateState.data : null;
  const clarificationItems = gate?.clarificationQuestions?.length
    ? gate.clarificationQuestions
    : fallbackClarifications;

  return (
    <DetailPageShell title={`${job.title} intake`} eyebrow="Job intake review">
      <section className="portal-panel">
        <h2>AI parsed profile</h2>
        <KeyValueList
          items={[
            ["Title", job.title],
            ["Description", job.description],
            ["Location", job.location],
            ["Seniority", job.seniorityBand],
            ["Role family", job.roleFamily],
            ["Employment", job.employmentType],
            ["Compensation", job.compensation],
            ["Commercial terms", job.commercialTerms ? "Structured placeholder captured" : null],
          ]}
        />
        <SafeList title="Commercial placeholder" items={commercialTermsSummary(job.commercialTerms)} />
      </section>
      <section className="portal-panel">
        <h2>Scorecard</h2>
        {job.scorecard ? (
          <KeyValueList
            items={[
              ["Status", job.scorecard.status],
              ["Dimensions", job.scorecard.dimensions],
              ["Guidance", job.scorecard.scoringGuidance],
            ]}
          />
        ) : <EmptyState title="No scorecard created for this job" />}
      </section>
      <section className="portal-panel">
        <h2>Clarification</h2>
        <SafeList title="Missing or uncertain items" items={clarificationItems} />
        {clarificationItems.length === 0 ? <EmptyState title="No clarification blockers for this job" /> : null}
      </section>
      <section className="portal-panel">
        <h2>Activation gate</h2>
        {gateState.status === "ready" && gate ? (
          <>
            <KeyValueList
              items={[
                ["Activation allowed", gate.activationAllowed ? "Yes" : "No"],
                ["Requirements captured", gate.hasRequirements ? "Yes" : "No"],
                ["Scorecard captured", gate.hasScorecard ? "Yes" : "No"],
                ["Commercial placeholder", gate.hasCommercialTermsPlaceholder ? "Yes" : "No"],
              ]}
            />
            <SafeList title="Blocker reasons" items={gate.blockerReasons} />
          </>
        ) : gateState.status === "loading" ? (
          <EmptyState title="Checking activation gate..." />
        ) : (
          <EmptyState title="Activation gate is unavailable right now" />
        )}
      </section>
      <section className="portal-panel">
        <h2>Activation checklist</h2>
        <SafeList title="Checklist" items={activationChecklist} />
        <form className="stack-form" onSubmit={onActivate}>
          <label>
            Activation note
            <textarea
              rows={3}
              value={activationReason}
              onChange={(event) => setActivationReason(event.target.value)}
              placeholder="Record why this job is ready to activate"
            />
          </label>
          <button
            type="submit"
            disabled={
              gateState.status !== "ready"
              || !gateState.data.activationAllowed
              || activateState.status === "loading"
            }
          >
            Activate job
          </button>
        </form>
        {activateState.status !== "idle" && activateState.status !== "loading" && activateState.status !== "ready" ? (
          <ApiState status={activateState.status} error={loadableError(activateState)} />
        ) : null}
        {activateState.status === "ready" ? (
          <p className="helper-copy">Job activated and workflow transition recorded.</p>
        ) : null}
      </section>
      <div className="button-row">
        <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}`}>Back to detail</NavLink>
        <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/matching`}>Open matching</NavLink>
        <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/outreach`}>Open outreach</NavLink>
      </div>
      <AuditDrawerButton entityType="job" entityId={job.jobId} />
    </DetailPageShell>
  );
}

function MatchingPage() {
  const { jobId = "" } = useParams();
  const candidates = useLoadable(() => listConsultantCandidates({ limit: 50, offset: 0 }), []);
  const [refreshToken, setRefreshToken] = useState(0);
  const shortlistCards = useLoadable(async () => {
    const shortlists = await listConsultantShortlists({ jobId, limit: 50, offset: 0 });
    if (shortlists.status !== "ready") {
      return {
        status: shortlists.status,
        error: shortlists.error,
      };
    }
    const details = await Promise.all(shortlists.data.items.map((item) => fetchConsultantShortlist(item.shortlistId)));
    const firstFailure = details.find((item) => item.status !== "ready");
    if (firstFailure) {
      return {
        status: firstFailure.status,
        error: firstFailure.error,
      };
    }
    return {
      status: "ready" as const,
      data: shortlists.data.items.flatMap((summary, index) => {
        const detail = details[index];
        if (detail.status !== "ready") {
          return [];
        }
        return detail.data.cards.map((card) => ({
          shortlistCandidateCardId: card.cardId,
          shortlistId: summary.shortlistId,
          shortlistTitle: summary.title,
          anonymousCandidateCardId: card.anonymousCandidateCardId,
          status: card.status,
          matchReportId: card.matchReportId,
        }));
      }),
    };
  }, [jobId]);
  const reports = useLoadable(() => listConsultantMatchReports(jobId), [jobId, refreshToken]);
  const [selectionMode, setSelectionMode] = useState<"talent" | "shortlist">("talent");
  const [candidateId, setCandidateId] = useState("");
  const [shortlistCandidateCardId, setShortlistCandidateCardId] = useState("");
  const [result, setResult] = useState<Loadable<ConsultantMatchReport>>({ status: "idle" });

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    const nextResult = await generateConsultantMatch(jobId, createConsultantMatchGenerationPayload({
      candidateId: selectionMode === "talent" ? candidateId : undefined,
      shortlistCandidateCardId: selectionMode === "shortlist" ? shortlistCandidateCardId : undefined,
    }));
    setResult(nextResult);
    if (nextResult.status === "ready") {
      setRefreshToken((value) => value + 1);
    }
  }

  return (
    <DetailPageShell
      title="Matching Console"
      eyebrow="Evidence-aware report generation"
      description="Score candidates from talent or shortlist context and keep evidence/risk visible."
    >
      <section className="portal-panel matching-console-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Context-bound subject selection</span>
            <h2>Select a candidate from talent or shortlist context</h2>
          </div>
        </div>
        <form className="stack-form" onSubmit={onSubmit}>
          <label>
            Selection mode
            <select value={selectionMode} onChange={(event) => setSelectionMode(event.target.value as "talent" | "shortlist")}>
              <option value="talent">Talent pool candidate</option>
              <option value="shortlist">Shortlist card</option>
            </select>
          </label>
          {selectionMode === "talent" ? renderLoadable(candidates, (candidateResult) => (
            <label>
              Candidate
              <select value={candidateId} onChange={(event) => setCandidateId(event.target.value)}>
                <option value="">Select a candidate</option>
                {candidateResult.items.map((candidate) => (
                  <option key={candidate.candidateId} value={candidate.candidateId}>
                    {candidate.candidateId} · {candidate.status}
                  </option>
                ))}
              </select>
            </label>
          )) : renderLoadable(shortlistCards, (cards) => (
            <label>
              Shortlist card
              <select value={shortlistCandidateCardId} onChange={(event) => setShortlistCandidateCardId(event.target.value)}>
                <option value="">Select a shortlisted card</option>
                {cards.map((card) => (
                  <option key={card.shortlistCandidateCardId} value={card.shortlistCandidateCardId}>
                    {card.shortlistTitle} · {card.anonymousCandidateCardId} · {card.status}
                  </option>
                ))}
              </select>
            </label>
          ))}
          {selectionMode === "shortlist" ? (
            renderLoadable(shortlistCards, (cards) => {
              const detailLinks = cards.map((card) => (
                <Link key={card.anonymousCandidateCardId} to={`/consultant/shortlists/${card.shortlistId}`} className="text-link">
                  Open shortlist {card.shortlistTitle}
                </Link>
              ));
              return detailLinks.length > 0 ? <div className="link-row">{detailLinks}</div> : <EmptyState title="No shortlists for this job yet" />;
            })
          ) : null}
          <div className="portal-chip-row">
            {CONSULTANT_MATCH_DIMENSIONS.map((dimension) => (
              <span key={dimension} className="portal-chip">
                {dimension}
              </span>
            ))}
          </div>
          <p className="helper-copy">
            Match generation now sends only the selected subject. Scoring, evidence coverage, provenance,
            and authenticity risk are assembled by the backend.
          </p>
          <button type="submit" disabled={selectionMode === "talent" ? !candidateId : !shortlistCandidateCardId}>
            Generate report
          </button>
        </form>
      </section>
      {result.status === "ready" && (
        <article className="data-card compact-card">
          <h3>Match report</h3>
          <KeyValueList
            items={[
              ["Report", result.data.matchReportId],
              ["Subject", result.data.subjectRef],
              ["Score", String(result.data.finalScore)],
              ["Confidence", result.data.confidence],
              ["Cap reason", result.data.capReason],
              ["Cap explanation", result.data.capSafeExplanation],
              ["Authenticity risk", result.data.authenticityRisk],
              ["Coverage", `${result.data.evidenceCoverage.coverageLevel} (${Math.round(result.data.evidenceCoverage.coverageRatio * 100)}%)`],
              ["Generated", result.data.generatedAt],
            ]}
          />
          <div className="portal-chip-row">
            {result.data.dimensionScores.map((dimension) => (
              <span key={dimension.dimension} className="portal-chip">
                {dimension.dimension}: {dimension.score}
              </span>
            ))}
          </div>
          <ul className="timeline-list">
            {result.data.explanations.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
          <ul className="timeline-list">
            {result.data.interviewQuestions.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </article>
      )}
      {renderLoadable(reports, (loaded) => (
        loaded.reports.length > 0 ? (
          <article className="data-card compact-card">
            <h3>Recent reports</h3>
            <DataTable
              headers={["Report", "Subject", "Score", "Confidence", "Coverage", "Generated"]}
              rows={loaded.reports.map((report) => [
                report.matchReportId,
                report.subjectRef,
                report.finalScore,
                report.confidence,
                `${report.evidenceCoverage.coverageLevel} (${Math.round(report.evidenceCoverage.coverageRatio * 100)}%)`,
                report.generatedAt,
              ])}
            />
          </article>
        ) : (
          <EmptyState title="No match reports yet" />
        )
      ))}
      {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? <ApiState status={result.status} error={loadableError(result)} /> : null}
    </DetailPageShell>
  );
}

function MatchingHubPage() {
  const jobs = useLoadable(listConsultantJobs, []);
  return renderLoadable(jobs, (result) => (
    <ListPageShell
      title="Matching Console"
      eyebrow="Job-first matching operating surface"
      description="Open a job, review fit evidence, and start the matching pass from the correct context."
    >
      <DataTable
        headers={["Job", "Status", "Company", "Open matching"]}
        rows={result.items.map((item) => [
          item.title,
          <StatusBadge value={item.status} />,
          item.companyId,
          <Link to={`/consultant/jobs/${item.jobId}/matching`} className="text-link">Open matching</Link>,
        ])}
      />
    </ListPageShell>
  ));
}

function ShortlistsPage() {
  const [jobIdFilter, setJobIdFilter] = useState("");
  const [offset, setOffset] = useState(0);
  const filters: ConsultantShortlistListFilters = {
    jobId: jobIdFilter.trim() || undefined,
    limit: DEFAULT_PAGE_SIZE,
    offset,
  };
  const state = useLoadable(() => listConsultantShortlists(filters), [jobIdFilter, offset]);
  return renderLoadable(state, (result) => (
    <ListPageShell
      title="Shortlist Builder"
      eyebrow="Consultant shortlist workspace"
      description="Review shortlist drafts, candidate counts, and client-send readiness."
    >
      <ListToolbar>
        <label>
          Job ID
          <input
            value={jobIdFilter}
            onChange={(event) => {
              setJobIdFilter(event.target.value);
              setOffset(0);
            }}
            placeholder="Filter by job id"
          />
        </label>
      </ListToolbar>
      <PaginationSummary totalCount={result.totalCount} limit={result.limit} offset={result.offset} />
      <DataTable
        headers={["Title", "Status", "Candidates", "Job"]}
        rows={result.items.map((item) => [
          <Link to={`/consultant/shortlists/${item.shortlistId}`} className="text-link">{item.title}</Link>,
          <StatusBadge value={item.status} />,
          item.candidateCount,
          item.jobId,
        ])}
      />
      <PaginationControls
        hasMore={result.hasMore}
        offset={result.offset}
        limit={result.limit}
        onPrevious={() => setOffset((value) => Math.max(0, value - DEFAULT_PAGE_SIZE))}
        onNext={() => setOffset((value) => value + DEFAULT_PAGE_SIZE)}
      />
    </ListPageShell>
  ));
}

function ShortlistDetailPage() {
  const { shortlistId = "" } = useParams();
  const state = useLoadable(() => fetchConsultantShortlist(shortlistId), [shortlistId]);
  return renderLoadable(state, (shortlist) => <ShortlistDetailWorkspace initialShortlist={shortlist} />);
}

function ShortlistBuilderPage() {
  const { jobId = "" } = useParams();
  const [title, setTitle] = useState("");
  const [result, setResult] = useState<Loadable<ConsultantShortlistDetail>>({ status: "idle" });
  const existing = useLoadable(
    () => listConsultantShortlists({ jobId, limit: DEFAULT_PAGE_SIZE, offset: 0 }),
    [jobId],
  );

  async function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult({ status: "loading" });
    setResult(await createConsultantShortlist({ jobId, title, status: SHORTLIST_BUILDER_INITIAL_STATUS }));
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel">
        <h2>Shortlist builder entry</h2>
        <form className="stack-form" onSubmit={onCreate}>
          <label>
            Title
            <input value={title} onChange={(event) => setTitle(event.target.value)} />
          </label>
          <button type="submit" disabled={!title.trim()}>Create shortlist draft</button>
        </form>
        {result.status === "ready" ? <p className="helper-copy">Created shortlist {result.data.title}.</p> : null}
        {result.status === "ready" ? (
          <KeyValueList
            items={[
              ["Status", result.data.status],
              ["Cards", String(result.data.cards.length)],
              ["Pre-send check", result.data.cards.length > 0 ? "Candidate cards attached" : "No candidate cards attached yet"],
            ]}
          />
        ) : null}
      </section>
      {renderLoadable(existing, (result) => (
        <ListPageShell title="Existing shortlists for job" eyebrow="Current shortlist state">
          <DataTable
            headers={["Title", "Status", "Candidates"]}
            rows={result.items.map((item) => [
              <Link to={`/consultant/shortlists/${item.shortlistId}`} className="text-link">{item.title}</Link>,
              <StatusBadge value={item.status} />,
              item.candidateCount,
            ])}
          />
        </ListPageShell>
      ))}
    </div>
  );
}

function CompanyDetailWorkspace({ initialCompany }: { initialCompany: ConsultantCompanyDetail }) {
  const [company, setCompany] = useState(initialCompany);
  const [saveState, setSaveState] = useState<Loadable<ConsultantCompanyDetail>>({ status: "idle" });
  const [contactState, setContactState] = useState<Loadable<ConsultantCompanyDetail>>({ status: "idle" });
  const [name, setName] = useState(initialCompany.name);
  const [displayName, setDisplayName] = useState(initialCompany.displayName ?? "");
  const [industry, setIndustry] = useState(initialCompany.industry ?? "");
  const [website, setWebsite] = useState(initialCompany.website ?? "");
  const [headquartersLocation, setHeadquartersLocation] = useState(initialCompany.headquartersLocation ?? "");
  const [sizeBand, setSizeBand] = useState(initialCompany.sizeBand ?? "");
  const [status, setStatus] = useState(initialCompany.status);
  const [paymentReliability, setPaymentReliability] = useState(initialCompany.paymentReliability ?? "");
  const [contactName, setContactName] = useState("");
  const [contactTitle, setContactTitle] = useState("");
  const [contactEmail, setContactEmail] = useState("");

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaveState({ status: "loading" });
    const next = await updateConsultantCompany(company.companyId, {
      version: company.version,
      name,
      displayName: emptyToNull(displayName),
      industry: emptyToNull(industry),
      website: emptyToNull(website),
      headquartersLocation: emptyToNull(headquartersLocation),
      sizeBand: emptyToNull(sizeBand),
      status,
      paymentReliability: emptyToNull(paymentReliability),
    });
    setSaveState(next);
    if (next.status === "ready") {
      setCompany(next.data);
    }
  }

  async function onAddContact(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setContactState({ status: "loading" });
    const next = await createConsultantCompanyContact(company.companyId, {
      name: contactName,
      title: emptyToNull(contactTitle),
      email: emptyToNull(contactEmail),
      isPrimary: company.contacts.length === 0,
      status: "active",
    });
    setContactState(next);
    if (next.status === "ready") {
      setCompany(next.data);
      setContactName("");
      setContactTitle("");
      setContactEmail("");
    }
  }

  return (
    <DetailPageShell title={company.name} eyebrow="Consultant company detail">
      <KeyValueList
        items={[
          ["Status", company.status],
          ["Industry", company.industry],
          ["Website", company.website],
          ["HQ", company.headquartersLocation],
          ["Jobs", String(company.jobCount)],
          ["Updated", formatDate(company.updatedAt)],
        ]}
      />
      <section className="portal-panel">
        <h3>Update company</h3>
        <form className="stack-form" onSubmit={onSave}>
          <label>Name<input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label>Display name<input value={displayName} onChange={(event) => setDisplayName(event.target.value)} /></label>
          <label>Industry<input value={industry} onChange={(event) => setIndustry(event.target.value)} /></label>
          <label>Website<input value={website} onChange={(event) => setWebsite(event.target.value)} /></label>
          <label>Headquarters<input value={headquartersLocation} onChange={(event) => setHeadquartersLocation(event.target.value)} /></label>
          <label>Size band<input value={sizeBand} onChange={(event) => setSizeBand(event.target.value)} /></label>
          <label>Payment reliability<input value={paymentReliability} onChange={(event) => setPaymentReliability(event.target.value)} /></label>
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="active">active</option>
              <option value="prospect">prospect</option>
              <option value="inactive">inactive</option>
            </select>
          </label>
          <button type="submit" disabled={!name.trim()}>Save company update</button>
        </form>
        {saveState.status !== "idle" && saveState.status !== "loading" && saveState.status !== "ready" ? <ApiState status={saveState.status} error={loadableError(saveState)} /> : null}
      </section>
      <section className="portal-panel">
        <h3>Add contact</h3>
        <form className="stack-form" onSubmit={onAddContact}>
          <label>Name<input value={contactName} onChange={(event) => setContactName(event.target.value)} /></label>
          <label>Title<input value={contactTitle} onChange={(event) => setContactTitle(event.target.value)} /></label>
          <label>Email<input value={contactEmail} onChange={(event) => setContactEmail(event.target.value)} /></label>
          <button type="submit" disabled={!contactName.trim()}>Create contact</button>
        </form>
        <SafeList title="Contacts" items={company.contacts.map((contact) => `${contact.name} · ${contact.title ?? "Unknown title"} · ${contact.email ?? "No email"}`)} />
        {contactState.status !== "idle" && contactState.status !== "loading" && contactState.status !== "ready" ? <ApiState status={contactState.status} error={loadableError(contactState)} /> : null}
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Audit coverage</span>
            <h2>Workflow-backed drawer availability</h2>
          </div>
        </div>
        <p className="helper-copy">
          Workflow audit drawers currently open from candidate, job, shortlist, and intake-backed
          entities that have supported workflow entity types in the backend read model.
        </p>
      </section>
    </DetailPageShell>
  );
}

function JobDetailWorkspace({ initialJob }: { initialJob: ConsultantJobDetail }) {
  const [job, setJob] = useState(initialJob);
  const [updateState, setUpdateState] = useState<Loadable<ConsultantJobDetail>>({ status: "idle" });
  const [requirementState, setRequirementState] = useState<Loadable<ConsultantJobDetail>>({ status: "idle" });
  const [scorecardState, setScorecardState] = useState<Loadable<ConsultantJobDetail>>({ status: "idle" });
  const [title, setTitle] = useState(initialJob.title);
  const [description, setDescription] = useState(initialJob.description ?? "");
  const [location, setLocation] = useState(initialJob.location ?? "");
  const [seniorityBand, setSeniorityBand] = useState(initialJob.seniorityBand ?? "");
  const [roleFamily, setRoleFamily] = useState(initialJob.roleFamily ?? "");
  const [employmentType, setEmploymentType] = useState(initialJob.employmentType ?? "");
  const [compensation, setCompensation] = useState(initialJob.compensation ?? "");
  const [commercialTerms, setCommercialTerms] = useState<CommercialTermsDraft>(() => parseCommercialTerms(initialJob.commercialTerms));
  const [status, setStatus] = useState(initialJob.status);
  const [requirementLabel, setRequirementLabel] = useState("");
  const [requirementType, setRequirementType] = useState("must_have");
  const [requirementImportance, setRequirementImportance] = useState("high");
  const [requirementDetail, setRequirementDetail] = useState("");
  const [scorecardDimensions, setScorecardDimensions] = useState(initialJob.scorecard?.dimensions ?? "");
  const [scoringGuidance, setScoringGuidance] = useState(initialJob.scorecard?.scoringGuidance ?? "");

  async function onSaveJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setUpdateState({ status: "loading" });
    const next = await updateConsultantJob(job.jobId, createConsultantJobUpdatePayload(job, {
      title,
      description: emptyToNull(description),
      location: emptyToNull(location),
      seniorityBand: emptyToNull(seniorityBand),
      roleFamily: emptyToNull(roleFamily),
      employmentType: emptyToNull(employmentType),
      compensation: emptyToNull(compensation),
      commercialTerms: serializeCommercialTerms(commercialTerms),
      status,
    }));
    setUpdateState(next);
    if (next.status === "ready") {
      setJob(next.data);
    }
  }

  async function onAddRequirement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRequirementState({ status: "loading" });
    const next = await createConsultantJobRequirement(job.jobId, {
      requirementType,
      label: requirementLabel,
      importance: requirementImportance,
      detail: emptyToNull(requirementDetail),
      sortOrder: job.requirements.length,
    });
    setRequirementState(next);
    if (next.status === "ready") {
      setJob(next.data);
      setRequirementLabel("");
      setRequirementDetail("");
    }
  }

  async function onSaveScorecard(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setScorecardState({ status: "loading" });
    const next = await createConsultantJobScorecard(job.jobId, {
      dimensions: emptyToNull(scorecardDimensions),
      scoringGuidance: emptyToNull(scoringGuidance),
      status: "draft",
    });
    setScorecardState(next);
    if (next.status === "ready") {
      setJob(next.data);
    }
  }

  return (
    <DetailPageShell title={job.title} eyebrow="Consultant job detail">
      <KeyValueList
        items={[
          ["Status", job.status],
          ["Company", job.companyId],
          ["Location", job.location],
          ["Seniority", job.seniorityBand],
          ["Role family", job.roleFamily],
          ["Employment", job.employmentType],
          ["Commercial terms", job.commercialTerms ? "Structured placeholder captured" : null],
        ]}
      />
      <section className="portal-panel">
        <h3>Update job</h3>
        <form className="stack-form" onSubmit={onSaveJob}>
          <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} /></label>
          <label>Description<textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={4} /></label>
          <label>Location<input value={location} onChange={(event) => setLocation(event.target.value)} /></label>
          <label>Seniority band<input value={seniorityBand} onChange={(event) => setSeniorityBand(event.target.value)} /></label>
          <label>Role family<input value={roleFamily} onChange={(event) => setRoleFamily(event.target.value)} /></label>
          <label>Employment type<input value={employmentType} onChange={(event) => setEmploymentType(event.target.value)} /></label>
          <label>Compensation<input value={compensation} onChange={(event) => setCompensation(event.target.value)} /></label>
          <div className="portal-panel client-nested-panel">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Commercial placeholder</span>
                <h3>Activation-gate contract minimum</h3>
              </div>
            </div>
            <CommercialTermsEditor value={commercialTerms} onChange={setCommercialTerms} />
            <SafeList title="Current structured terms" items={commercialTermsSummary(serializeCommercialTerms(commercialTerms))} />
          </div>
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="submitted">submitted</option>
              <option value="open">open</option>
              <option value="closed">closed</option>
            </select>
          </label>
          <button type="submit" disabled={!title.trim()}>Save job update</button>
        </form>
      </section>
      <section className="portal-panel">
        <h3>Requirements</h3>
        <SafeList title="Current requirements" items={job.requirements.map((item) => `${item.label} · ${item.importance}${item.detail ? ` · ${item.detail}` : ""}`)} />
        <form className="stack-form" onSubmit={onAddRequirement}>
          <label>Label<input value={requirementLabel} onChange={(event) => setRequirementLabel(event.target.value)} /></label>
          <label>Requirement type<input value={requirementType} onChange={(event) => setRequirementType(event.target.value)} /></label>
          <label>Importance<input value={requirementImportance} onChange={(event) => setRequirementImportance(event.target.value)} /></label>
          <label>Detail<textarea value={requirementDetail} onChange={(event) => setRequirementDetail(event.target.value)} rows={3} /></label>
          <button type="submit" disabled={!requirementLabel.trim()}>Add requirement</button>
        </form>
      </section>
      <section className="portal-panel">
        <h3>Scorecard</h3>
        <KeyValueList
          items={[
            ["Scorecard status", job.scorecard?.status ?? "Not created"],
            ["Dimensions", job.scorecard?.dimensions],
          ]}
        />
        <form className="stack-form" onSubmit={onSaveScorecard}>
          <label>Dimensions<textarea value={scorecardDimensions} onChange={(event) => setScorecardDimensions(event.target.value)} rows={4} /></label>
          <label>Scoring guidance<textarea value={scoringGuidance} onChange={(event) => setScoringGuidance(event.target.value)} rows={4} /></label>
          <button type="submit">Save scorecard</button>
        </form>
      </section>
      <div className="button-row">
        <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/intake`}>Job intake review</NavLink>
        <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/matching`}>Matching review</NavLink>
        <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/outreach`}>Outreach workspace</NavLink>
        <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/shortlist`}>Shortlist builder</NavLink>
      </div>
      {updateState.status !== "idle" && updateState.status !== "loading" && updateState.status !== "ready" ? <ApiState status={updateState.status} error={loadableError(updateState)} /> : null}
      {requirementState.status !== "idle" && requirementState.status !== "loading" && requirementState.status !== "ready" ? <ApiState status={requirementState.status} error={loadableError(requirementState)} /> : null}
      {scorecardState.status !== "idle" && scorecardState.status !== "loading" && scorecardState.status !== "ready" ? <ApiState status={scorecardState.status} error={loadableError(scorecardState)} /> : null}
      <AuditDrawerButton entityType="job" entityId={job.jobId} />
    </DetailPageShell>
  );
}

function ShortlistDetailWorkspace({ initialShortlist }: { initialShortlist: ConsultantShortlistDetail }) {
  const [shortlist, setShortlist] = useState(initialShortlist);
  const [updateState, setUpdateState] = useState<Loadable<ConsultantShortlistDetail>>({ status: "idle" });
  const [title, setTitle] = useState(initialShortlist.title);
  const [status, setStatus] = useState(initialShortlist.status);
  const statusIsEditable = isShortlistBuilderEditable(status);
  const currentStatusIsBeyondBuilder = !isShortlistBuilderEditable(shortlist.status);

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setUpdateState({ status: "loading" });
    const next = await updateConsultantShortlist(shortlist.shortlistId, createConsultantShortlistUpdatePayload(shortlist, {
      title,
      status,
    }));
    setUpdateState(next);
    if (next.status === "ready") {
      setShortlist(next.data);
    }
  }

  const preSendChecks = [
    shortlist.cards.length > 0 ? "Candidate cards present" : "No candidate cards attached",
    shortlist.status === "ready_for_review"
      ? "Status is ready for consultant review."
      : shortlist.status === "sent_to_client"
        ? "Shortlist has already moved beyond builder scope."
        : "Status is still inside pre-send builder work.",
  ];

  return (
    <DetailPageShell title={shortlist.title} eyebrow="Consultant shortlist detail">
      <KeyValueList
        items={[
          ["Status", shortlist.status],
          ["Job", shortlist.jobId],
          ["Sent at", formatDate(shortlist.sentAt)],
          ["Client viewed", formatDate(shortlist.clientViewedAt)],
        ]}
      />
      <section className="portal-panel">
        <h3>Update shortlist</h3>
        <form className="stack-form" onSubmit={onSave}>
          <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} /></label>
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value)} disabled={currentStatusIsBeyondBuilder}>
              <option value="draft">draft</option>
              <option value="ready_for_review">ready_for_review</option>
              {currentStatusIsBeyondBuilder ? (
                <option value={shortlist.status}>{shortlist.status}</option>
              ) : null}
            </select>
          </label>
          <button
            type="submit"
            disabled={!canSaveShortlistBuilder(shortlist.status, status, title) || !statusIsEditable || currentStatusIsBeyondBuilder}
          >
            Save shortlist
          </button>
        </form>
        <p className="helper-copy">
          Task 24 only supports shortlist builder states. Client send/view transitions remain outside
          this page and are not simulated here.
        </p>
      </section>
      <SafeList title="Cards" items={shortlist.cards.map((card) => `${card.anonymousCandidateCardId} · ${card.status}`)} />
      <SafeList title="Pre-send checks" items={preSendChecks} />
      {updateState.status !== "idle" && updateState.status !== "loading" && updateState.status !== "ready" ? <ApiState status={updateState.status} error={loadableError(updateState)} /> : null}
      <AuditDrawerButton entityType="shortlist" entityId={shortlist.shortlistId} />
    </DetailPageShell>
  );
}

function FollowUpsPage() {
  const state = useLoadable(listConsultantFollowUps, []);
  return renderLoadable(state, (result) => (
    <ListPageShell
      title="Follow-up Center"
      eyebrow="Candidate, client, and write-back follow-ups"
      description="One queue for recovery, review, reminders, and next actions."
    >
      <DataTable
        headers={["Title", "Type", "Status", "Occurred"]}
        rows={result.items.map((item) => [
          <Link to={item.route} className="text-link">{item.title}</Link>,
          item.followUpType,
          <StatusBadge value={item.status} />,
          formatDate(item.occurredAt),
        ])}
      />
    </ListPageShell>
  ));
}

function JobOutreachPage() {
  const { jobId = "" } = useParams();
  const jobState = useLoadable(() => fetchConsultantJob(jobId), [jobId]);
  const followUpsState = useLoadable(listConsultantFollowUps, []);
  return renderLoadable(jobState, (job) => {
    const outreachItems = followUpsState.status === "ready"
      ? followUpsState.data.items.filter((item) => item.entityId === job.jobId || item.route.includes(job.jobId))
      : [];
    const callScript = [
      `Open with the role context: ${job.title}.`,
      job.requirements.length > 0
        ? `Probe evidence on ${job.requirements[0].label}.`
        : "Validate the highest-priority requirement before presenting the role.",
      !job.location ? "Confirm the location and relocation expectation." : `Confirm fit for ${job.location}.`,
      "Confirm candidate consent wording before any client-visible move.",
    ];
    const missingQuestions = [
      !job.description ? "What business problem is this hire solving?" : null,
      !job.compensation ? "What compensation range can be discussed safely?" : null,
      !job.scorecard?.dimensions ? "Which scorecard dimensions are mandatory for approval?" : null,
    ].filter((item): item is string => item !== null);
    return (
      <DetailPageShell title={`${job.title} outreach`} eyebrow="Candidate communication and consent">
        <section className="portal-panel">
          <h2>Call script</h2>
          <SafeList title="Suggested prompts" items={callScript} />
        </section>
        <section className="portal-panel">
          <h2>Missing questions</h2>
          {missingQuestions.length > 0
            ? <SafeList title="Questions to resolve" items={missingQuestions} />
            : <EmptyState title="No unresolved outreach questions for this job" />}
        </section>
        <section className="portal-panel">
          <h2>Consent wording</h2>
          <p className="helper-copy">
            Confirm that the candidate is open to this role, understands the client-safe shortlist stage,
            and approves any identity disclosure only after the governed unlock flow.
          </p>
        </section>
        <section className="portal-panel">
          <h2>Outreach queue</h2>
          {followUpsState.status === "ready" ? (
            outreachItems.length > 0 ? (
              <DataTable
                headers={["Title", "Type", "Status", "Route"]}
                rows={outreachItems.map((item) => [
                  item.title,
                  item.followUpType,
                  <StatusBadge value={item.status} />,
                  <Link to={item.route} className="text-link">Open</Link>,
                ])}
              />
            ) : <EmptyState title="No job-linked outreach tasks yet" />
          ) : followUpsState.status === "loading" || followUpsState.status === "idle" ? (
            <p className="helper-copy">Loading outreach queue.</p>
          ) : (
            <ApiState status={followUpsState.status} error={loadableError(followUpsState)} />
          )}
        </section>
        <div className="button-row">
          <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/intake`}>Back to intake review</NavLink>
          <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/matching`}>Open matching</NavLink>
          <NavLink className="secondary-link" to={`/consultant/jobs/${job.jobId}/shortlist`}>Open shortlist</NavLink>
        </div>
        <AuditDrawerButton entityType="job" entityId={job.jobId} />
      </DetailPageShell>
    );
  });
}

function OutreachPage() {
  const state = useLoadable(listConsultantFollowUps, []);
  return renderLoadable(state, (result) => (
    <ListPageShell
      title="Outreach Workspace"
      eyebrow="Consultant response and follow-through lane"
      description="Use the same follow-up tasks as the operating lane for outreach and response handling."
    >
      <DataTable
        headers={["Title", "Type", "Status", "Route"]}
        rows={result.items.map((item) => [
          item.title,
          item.followUpType,
          <StatusBadge value={item.status} />,
          <Link to={item.route} className="text-link">Open</Link>,
        ])}
      />
    </ListPageShell>
  ));
}

function WorkflowPage() {
  const [entityTypeFilter, setEntityTypeFilter] = useState("");
  const [entityIdFilter, setEntityIdFilter] = useState("");
  const [offset, setOffset] = useState(0);
  const entityState = useLoadable(
    () => {
      if (!entityTypeFilter || !entityIdFilter.trim()) {
        return Promise.resolve({
          status: "ready" as const,
          data: {
            entityType: entityTypeFilter,
            entityId: entityIdFilter.trim(),
            currentStatus: null,
            transitionOptions: [],
          },
        });
      }
      return fetchConsultantWorkflowEntityState(entityTypeFilter, entityIdFilter.trim());
    },
    [entityTypeFilter, entityIdFilter],
  );
  const filters: ConsultantWorkflowFilters = {
    entityType: entityTypeFilter || undefined,
    entityId: entityIdFilter.trim() || undefined,
    limit: DEFAULT_PAGE_SIZE,
    offset,
  };
  const state = useLoadable(() => fetchConsultantWorkflow(filters), [entityTypeFilter, entityIdFilter, offset]);
  return renderLoadable(state, (result) => (
    <ListPageShell
      title="Workflow Timeline"
      eyebrow="Workflow audit read model"
      description="Every governed action stays visible here as a consultant-safe event trail."
    >
      <ListToolbar>
        <label>
          Entity type
          <select
            value={entityTypeFilter}
            onChange={(event) => {
              setEntityTypeFilter(event.target.value);
              setOffset(0);
            }}
          >
            <option value="">All entities</option>
            {CONSULTANT_WORKFLOW_ENTITY_TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Entity ID
          <input
            value={entityIdFilter}
            onChange={(event) => {
              setEntityIdFilter(event.target.value);
              setOffset(0);
            }}
            placeholder="Filter by entity id"
          />
        </label>
      </ListToolbar>
      <p className="helper-copy pagination-summary">
        {describeWorkflowPageWindow(result.items.length, result.offset)}
      </p>
      {entityTypeFilter && entityIdFilter.trim() ? (
        renderLoadable(entityState, (entityWorkflow) => (
          <section className="portal-panel">
            <div className="section-header">
              <div>
                <span className="portal-eyebrow">Workflow state</span>
                <h3>{entityWorkflow.entityType}:{entityWorkflow.entityId}</h3>
              </div>
              <StatusBadge value={entityWorkflow.currentStatus ?? "unknown"} />
            </div>
            {entityWorkflow.transitionOptions.length > 0 ? (
              <SafeList
                title="Legal next actions"
                items={entityWorkflow.transitionOptions.map((option) => describeTransitionOption(option))}
              />
            ) : (
              <p className="helper-copy">Enter both entity type and entity id to preview allowed workflow actions.</p>
            )}
          </section>
        ))
      ) : (
        <p className="helper-copy">Add both entity type and entity id to preview current workflow state and blockers.</p>
      )}
      <DataTable
        headers={["Action", "Entity", "Transition", "Risk", "Occurred"]}
        rows={result.items.map((item) => [
          item.actionCode,
          `${item.entityType}:${item.entityId}`,
          describeWorkflowTransition(item),
          <StatusBadge value={item.riskTier} />,
          formatDate(item.occurredAt),
        ])}
      />
      <PaginationControls
        hasMore={result.hasMore}
        offset={result.offset}
        limit={result.limit}
        onPrevious={() => setOffset((value) => Math.max(0, value - DEFAULT_PAGE_SIZE))}
        onNext={() => setOffset((value) => value + DEFAULT_PAGE_SIZE)}
      />
    </ListPageShell>
  ));
}

function PlacementsPage() {
  return (
    <ListPageShell title="Placements" eyebrow="Unified consultant portal">
      <EmptyState title="No placement workflow events are available for this consultant organization yet" />
    </ListPageShell>
  );
}

function CommissionPage() {
  return (
    <ListPageShell title="Commission" eyebrow="Unified consultant portal">
      <EmptyState title="Commission tracking opens after governed placements are recorded" />
    </ListPageShell>
  );
}

function ReportsPage() {
  const dashboard = useLoadable(fetchConsultantDashboard, []);
  return renderLoadable(dashboard, (result) => (
    <ListPageShell title="Reports" eyebrow="Operational portfolio summary">
      <KeyValueList
        items={[
          ["Candidates", String(result.candidateCount)],
          ["Active jobs", String(result.activeJobCount)],
          ["Companies", String(result.companyCount)],
          ["Shortlists", String(result.shortlistCount)],
          ["Follow-ups", String(result.pendingFollowUpCount)],
          ["Workflow events", String(result.recentTimelineCount)],
        ]}
      />
    </ListPageShell>
  ));
}

function SettingsPage({ session }: { session: AuthSession }) {
  return (
    <ListPageShell title="Settings" eyebrow="Consultant session and boundary">
      <KeyValueList
        items={[
          ["Display name", session.displayName],
          ["Portal role", session.portalRole],
          ["Organization", session.organizationId],
          ["User account", session.userAccountId],
          ["Access token expiry", formatDate(session.accessTokenExpiresAt)],
          ["Refresh token expiry", formatDate(session.refreshTokenExpiresAt)],
        ]}
      />
    </ListPageShell>
  );
}

function AuditDrawerButton({ entityType, entityId }: { entityType: string; entityId: string }) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState<Loadable<ConsultantAuditDrawer>>({ status: "idle" });
  const [entityState, setEntityState] = useState<Loadable<ConsultantWorkflowEntityState>>({ status: "idle" });

  useEffect(() => {
    let active = true;
    if (!open) {
      return () => {
        active = false;
      };
    }
    setState({ status: "loading" });
    setEntityState({ status: "loading" });
    fetchConsultantAuditDrawer(entityType, entityId).then((next) => {
      if (active) {
        setState(next);
      }
    });
    fetchConsultantWorkflowEntityState(entityType, entityId).then((next) => {
      if (active) {
        setEntityState(next);
      }
    });
    return () => {
      active = false;
    };
  }, [entityId, entityType, open]);

  return (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">Audit drawer</span>
          <h2>Entity audit trail</h2>
        </div>
        <button type="button" className="secondary-button" onClick={() => setOpen((value) => !value)}>
          {open ? "Close drawer" : "Open drawer"}
        </button>
      </div>
      {!open ? <p className="helper-copy">Open the drawer to inspect workflow events and reasons for this entity.</p> : null}
      {open ? renderLoadable(state, (drawer) => (
        <div className="portal-panel">
          {renderLoadable(entityState, (entityWorkflow) => (
            <SafeList
              title={`Current state: ${entityWorkflow.currentStatus ?? "unknown"}`}
              items={entityWorkflow.transitionOptions.map((option) => describeTransitionOption(option))}
            />
          ))}
          {drawer.items.length === 0 ? (
            <EmptyState title="No audit events for this entity" />
          ) : (
            <SafeList title="Recent workflow events" items={drawer.items.map((item) => `${describeWorkflowTransition(item)} · ${item.actionCode} · ${formatDate(item.occurredAt)} · ${item.reason}`)} />
          )}
        </div>
      )) : null}
    </section>
  );
}

function ListPageShell({
  title,
  eyebrow,
  description,
  actions,
  children,
}: {
  title: string;
  eyebrow: string;
  description?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="portal-panel shell-header-panel">
      <div className="section-header shell-header-row">
        <div>
          <span className="portal-eyebrow">{eyebrow}</span>
          <h2>{title}</h2>
          {description ? <p className="helper-copy shell-description">{description}</p> : null}
        </div>
        {actions}
      </div>
      {children}
    </section>
  );
}

function DetailPageShell({
  title,
  eyebrow,
  description,
  children,
}: {
  title: string;
  eyebrow: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <span className="portal-eyebrow">{eyebrow}</span>
        <h2>{title}</h2>
        {description ? <p className="helper-copy shell-description">{description}</p> : null}
      </section>
      {children}
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{compactCount(value)}</strong>
      <small>{value.toLocaleString()} total</small>
    </article>
  );
}

function MetricBar({
  label,
  value,
  total,
  tone,
}: {
  label: string;
  value: number;
  total: number;
  tone: "violet" | "purple" | "blue" | "teal";
}) {
  return (
    <div className="metric-bar">
      <div className="card-row">
        <strong>{label}</strong>
        <span>{value.toLocaleString()}</span>
      </div>
      <div className="metric-bar-track">
        <span className={`metric-bar-fill metric-bar-${tone}`} style={{ width: `${percentOf(value, total)}%` }} />
      </div>
    </div>
  );
}

function BoardActionCard({
  title,
  description,
  route,
}: {
  title: string;
  description: string;
  route: string;
}) {
  return (
    <Link to={route} className="board-action-card">
      <strong>{title}</strong>
      <span>{description}</span>
    </Link>
  );
}

function StatusBadge({ value, tone }: { value: string; tone?: "warning" | "neutral" }) {
  return <span className={`status-badge ${tone ? `status-badge-${tone}` : badgeToneForValue(value)}`}>{value}</span>;
}

function QueueStageBadge({ item }: { item: ConsultantIntakeQueueItem }) {
  const tone = item.stage === "extract_failed" ? "warning" : undefined;
  return (
    <span title={item.stageDetail}>
      <StatusBadge value={item.stage} tone={tone} />
    </span>
  );
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

function CommercialTermsEditor({
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
          placeholder="30 days from invoice"
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
          placeholder="Optional commercial notes kept in the consultant-safe surface"
        />
      </label>
    </div>
  );
}

function DataTable({
  headers,
  rows,
}: {
  headers: string[];
  rows: React.ReactNode[][];
}) {
  if (rows.length === 0) {
    return <EmptyState title="No rows returned" />;
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

function ListToolbar({ children }: { children: React.ReactNode }) {
  return <div className="list-toolbar">{children}</div>;
}

function PaginationSummary({
  totalCount,
  limit,
  offset,
}: {
  totalCount: number;
  limit: number;
  offset: number;
}) {
  const start = totalCount === 0 ? 0 : offset + 1;
  const end = Math.min(offset + limit, totalCount);
  return (
    <p className="helper-copy pagination-summary">
      Showing {start}-{end} of {totalCount}
    </p>
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

function MiniDataTable({ columns, rows }: { columns: string[]; rows: React.ReactNode[][] }) {
  return (
    <div className="mini-table" style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}>
      <div className="mini-table-head" style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}>
        {columns.map((column) => (
          <span key={column}>{column}</span>
        ))}
      </div>
      <div className="mini-table-body">
        {rows.map((row, rowIndex) => (
          <div
            key={rowIndex}
            className="mini-table-row"
            style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}
          >
            {row.map((cell, cellIndex) => (
              <div key={`${rowIndex}-${cellIndex}`}>{cell}</div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

function BoardPreviewPanel({
  number,
  eyebrow,
  title,
  action,
  children,
}: {
  number: number;
  eyebrow: string;
  title: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="portal-panel board-preview-panel">
      <div className="section-header board-preview-header">
        <div className="board-preview-title">
          <span className="board-preview-number">{number}</span>
          <div>
            <span className="portal-eyebrow">{eyebrow}</span>
            <h2>{title}</h2>
          </div>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

function EmptyState({ title }: { title: string }) {
  return <div className="empty-state">{title}</div>;
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function SafeState({
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
      <p>{detail ?? "Only consultant-safe operational data is shown in this workspace."}</p>
    </div>
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

function badgeToneForValue(value: string): string {
  const normalized = value.toLowerCase();
  if (
    normalized.includes("approved")
    || normalized.includes("active")
    || normalized.includes("ready")
    || normalized.includes("strong")
    || normalized.includes("open")
    || normalized.includes("sent")
  ) {
    return "status-badge-positive";
  }
  if (
    normalized.includes("review")
    || normalized.includes("pending")
    || normalized.includes("submitted")
    || normalized.includes("draft")
    || normalized.includes("loading")
    || normalized.includes("medium")
  ) {
    return "status-badge-accent";
  }
  if (
    normalized.includes("failed")
    || normalized.includes("blocked")
    || normalized.includes("warning")
    || normalized.includes("conflict")
    || normalized.includes("denied")
    || normalized.includes("stale")
    || normalized.includes("high")
  ) {
    return "status-badge-warning";
  }
  if (
    normalized.includes("inactive")
    || normalized.includes("closed")
    || normalized.includes("archived")
    || normalized.includes("unknown")
  ) {
    return "status-badge-neutral";
  }
  return "";
}

export function ConsultantPortal() {
  const location = useLocation();
  const [session, setSession] = useState<AuthSession | null>(() => loadPortalSession());

  useEffect(() => {
    function syncSession() {
      setSession(loadPortalSession());
    }
    window.addEventListener("storage", syncSession);
    return () => window.removeEventListener("storage", syncSession);
  }, []);

  function onSignedIn(nextSession: AuthSession) {
    savePortalSession(nextSession);
    setSession(nextSession);
  }

  function onLogout() {
    clearPortalSession();
    setSession(null);
  }

  if (location.pathname === "/consultant/sign-in") {
    return session ? (
      <Navigate to="/consultant/dashboard" replace />
    ) : (
      <ConsultantSignInPage onSignedIn={onSignedIn} />
    );
  }

  if (!session) {
    return <Navigate to="/consultant/sign-in" replace />;
  }

  return (
    <ConsultantPortalLayout session={session} onLogout={onLogout}>
      <Routes>
        <Route path="/" element={<Navigate to="/consultant/dashboard" replace />} />
        <Route path="sign-in" element={<Navigate to="/consultant/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="intake" element={<IntakeHomePage />} />
        <Route path="intake/talent" element={<Navigate to="/consultant/intake/upload/candidate" replace />} />
        <Route path="intake/upload/:lane" element={<IntakeUploadPage />} />
        <Route path="intake/review/:packetId" element={<IntakeReviewPage />} />
        <Route path="talent" element={<TalentListPage />} />
        <Route path="talent/:candidateId" element={<TalentDetailPage />} />
        <Route path="companies" element={<CompaniesPage />} />
        <Route path="companies/:companyId" element={<CompanyDetailPage />} />
        <Route path="jobs" element={<JobsPage />} />
        <Route path="matching" element={<MatchingHubPage />} />
        <Route path="jobs/:jobId" element={<JobDetailPage />} />
        <Route path="jobs/:jobId/intake" element={<JobIntakePage />} />
        <Route path="jobs/:jobId/matching" element={<MatchingPage />} />
        <Route path="jobs/:jobId/outreach" element={<JobOutreachPage />} />
        <Route path="jobs/:jobId/shortlist" element={<ShortlistBuilderPage />} />
        <Route path="shortlists" element={<ShortlistsPage />} />
        <Route path="shortlists/:shortlistId" element={<ShortlistDetailPage />} />
        <Route path="follow-ups" element={<FollowUpsPage />} />
        <Route path="outreach" element={<OutreachPage />} />
        <Route path="workflow" element={<WorkflowPage />} />
        <Route path="placements" element={<PlacementsPage />} />
        <Route path="commission" element={<CommissionPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="settings" element={<SettingsPage session={session} />} />
        <Route path="*" element={<Navigate to="/consultant/dashboard" replace />} />
      </Routes>
    </ConsultantPortalLayout>
  );
}
