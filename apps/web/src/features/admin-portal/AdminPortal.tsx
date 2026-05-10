import { FormEvent, useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { login, type AuthSession } from "../../api/auth";
import {
  fetchAdminGovernanceSection,
  saveAdminGovernanceSection,
  type GovernanceConfigUpdate,
  type GovernanceSection,
} from "../../api/governance";
import { type ApiResult } from "../../api/http";
import { saveAccessToken } from "../../auth/accessTokenStorage";
import { loadAdminSession, saveAdminSession, signOutAdminSession } from "./adminSession";

type Loadable<T> = ApiResult<T> | { status: "idle" | "loading" };

type SectionKey =
  | "eval-dashboard"
  | "negative-cases"
  | "review-quality"
  | "claim-ledger"
  | "ontology-governance"
  | "ontology-drift"
  | "privacy-redaction"
  | "redaction-incidents"
  | "model-routing"
  | "cost-latency"
  | "ai-resume-authenticity-risk"
  | "eval-feedback"
  | "ai-policy"
  | "ai-task-registry"
  | "industry-packs"
  | "schema"
  | "workflow-rules"
  | "permissions"
  | "audit-log"
  | "integrations"
  | "security";

const ADMIN_SECTIONS: Array<{ key: SectionKey; label: string }> = [
  { key: "eval-dashboard", label: "Eval Dashboard" },
  { key: "negative-cases", label: "Negative Cases" },
  { key: "review-quality", label: "Review Quality" },
  { key: "claim-ledger", label: "Claim Ledger" },
  { key: "ontology-governance", label: "Ontology" },
  { key: "ontology-drift", label: "Ontology Drift" },
  { key: "privacy-redaction", label: "Privacy" },
  { key: "redaction-incidents", label: "Redaction Incidents" },
  { key: "model-routing", label: "Model Routing" },
  { key: "cost-latency", label: "Cost & Latency" },
  { key: "ai-resume-authenticity-risk", label: "Resume Authenticity" },
  { key: "eval-feedback", label: "Eval Feedback" },
  { key: "ai-policy", label: "AI Policy" },
  { key: "ai-task-registry", label: "Task Registry" },
  { key: "industry-packs", label: "Industry Packs" },
  { key: "schema", label: "Schema" },
  { key: "workflow-rules", label: "Workflow Rules" },
  { key: "permissions", label: "Permissions" },
  { key: "audit-log", label: "Audit Log" },
  { key: "integrations", label: "Integrations" },
  { key: "security", label: "Security" },
];

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

function StatusBadge({ value }: { value: string | null | undefined }) {
  return <span className="status-badge">{value ?? "n/a"}</span>;
}

function renderLoadable<T>(state: Loadable<T>, renderReady: (data: T) => React.ReactNode) {
  if (state.status === "loading" || state.status === "idle") {
    return <p className="helper-copy">Loading admin governance data...</p>;
  }
  if (state.status !== "ready") {
    return <p className="helper-copy">{"error" in state ? state.error ?? "Admin API unavailable." : "Admin API unavailable."}</p>;
  }
  return renderReady(state.data);
}

function PortalShell({ title, eyebrow, children }: { title: string; eyebrow: string; children: React.ReactNode }) {
  return (
    <section className="portal-layout" aria-label="Admin portal">
      <header className="portal-heading">
        <span className="portal-eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
      </header>
      <nav className="portal-nav" aria-label="Admin sections">
        {ADMIN_SECTIONS.map((section) => (
          <NavLink key={section.key} to={`/admin/${section.key}`}>
            {section.label}
          </NavLink>
        ))}
      </nav>
      <div className="workspace-stack">{children}</div>
    </section>
  );
}

function GovernanceSectionPage({ sectionKey }: { sectionKey: SectionKey }) {
  const state = useLoadable(() => fetchAdminGovernanceSection(sectionKey), [sectionKey]);
  const [configDraft, setConfigDraft] = useState("{}");
  const [saveState, setSaveState] = useState<Loadable<GovernanceConfigUpdate>>({ status: "idle" });

  useEffect(() => {
    if (state.status === "ready") {
      setConfigDraft(state.data.configJson || "{}");
    }
  }, [state]);

  async function onSave() {
    setSaveState({ status: "loading" });
    const result = await saveAdminGovernanceSection(sectionKey, configDraft, true);
    setSaveState(result);
  }

  return renderLoadable(state, (section: GovernanceSection) => (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">{section.sectionKey}</span>
          <h2>{section.title}</h2>
          <p className="helper-copy">{section.description}</p>
        </div>
        <StatusBadge value={section.editable ? "editable" : "read-only"} />
      </div>
      <div className="portal-grid" aria-label={`${section.title} metrics`}>
        {section.metrics.map((metric) => (
          <section key={metric.key} className="portal-panel">
            <span className="portal-eyebrow">{metric.label}</span>
            <h3>{metric.value}</h3>
            <p className="helper-copy">{metric.helperText}</p>
          </section>
        ))}
      </div>
      <div className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Records</span>
            <h3>Recent governance rows</h3>
          </div>
        </div>
        {section.items.length === 0 ? <p className="helper-copy">No events returned by this read model; instrumentation gaps appear as warning metrics instead of hidden zeroes.</p> : (
          <div className="stack-form">
            {section.items.map((item, index) => (
              <article key={`${item.primaryText}-${index}`} className="portal-panel">
                <div className="section-header">
                  <div>
                    <h3>{item.primaryText}</h3>
                    <p className="helper-copy">{item.secondaryText}</p>
                  </div>
                  <StatusBadge value={item.status} />
                </div>
                <p className="helper-copy">{item.detail}</p>
              </article>
            ))}
          </div>
        )}
      </div>
      {section.editable ? (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Config overlay</span>
              <h3>JSON configuration</h3>
              <p className="helper-copy">Last updated: {section.updatedAt || "not saved yet"}</p>
            </div>
          </div>
          <textarea
            value={configDraft}
            onChange={(event) => setConfigDraft(event.target.value)}
            rows={14}
            style={{ width: "100%", fontFamily: "monospace" }}
          />
          <div className="pagination-controls">
            <button type="button" onClick={onSave}>Save governance config</button>
            {saveState.status === "ready" ? <span className="pagination-chip">Saved</span> : null}
            {saveState.status !== "idle" && saveState.status !== "loading" && saveState.status !== "ready" ? (
              <span className="pagination-chip">{"error" in saveState ? saveState.error ?? "Save failed" : "Save failed"}</span>
            ) : null}
          </div>
        </section>
      ) : null}
      {section.warnings.map((warning) => (
        <p key={warning} className="helper-copy">{warning}</p>
      ))}
    </section>
  ));
}

function AdminSignInPage({ onSignedIn }: { onSignedIn: (session: AuthSession) => void }) {
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
      portalRole: "admin",
    });
    setResult(next);
    if (next.status === "ready") {
      saveAccessToken(next.data.accessToken, "admin");
      saveAdminSession(next.data);
      onSignedIn(next.data);
      navigate("/admin/eval-dashboard", { replace: true });
    }
  }

  return (
    <section className="portal-layout">
      <header className="portal-heading">
        <span className="portal-eyebrow">Governance and platform control</span>
        <h1>Admin / System</h1>
        <p className="portal-copy">Sign in with an admin role to review governance metrics and manage runtime overlays.</p>
      </header>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Admin session</span>
            <h2>Sign in to continue</h2>
          </div>
          <StatusBadge value="admin" />
        </div>
        <form className="stack-form sign-in-form" onSubmit={onSubmit}>
          <label>
            Organization ID
            <input value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} />
          </label>
          <label>
            Work email
            <input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="admin@company.com" />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
          </label>
          <button type="submit" disabled={!email.trim() || !password}>Enter Admin Portal</button>
          {result.status !== "idle" && result.status !== "loading" && result.status !== "ready" ? (
            <p className="helper-copy">{"error" in result ? result.error ?? "Admin sign in failed." : "Admin sign in failed."}</p>
          ) : null}
        </form>
      </section>
    </section>
  );
}

export function AdminPortal() {
  const [session, setSession] = useState<AuthSession | null>(() => loadAdminSession());
  const [signingOut, setSigningOut] = useState(false);
  const [signOutError, setSignOutError] = useState<string | null>(null);

  async function onSignOut() {
    setSigningOut(true);
    setSignOutError(null);
    try {
      const result = await signOutAdminSession(session);
      if (result.status === "ready") {
        setSession(null);
        return;
      }
      setSignOutError(result.error ?? "Admin sign out failed. Please try again.");
    } finally {
      setSigningOut(false);
    }
  }

  if (!session) {
    return <AdminSignInPage onSignedIn={setSession} />;
  }

  return (
    <PortalShell title="Admin / System" eyebrow="Governance and platform control">
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Session boundary</span>
            <h2>{session.displayName}</h2>
          </div>
          <button type="button" className="secondary-button" onClick={onSignOut} disabled={signingOut}>
            {signingOut ? "Signing out..." : "Sign out"}
          </button>
        </div>
        <p className="helper-copy">{session.portalRole} session active for {session.organizationId || "current organization"}.</p>
        {signOutError ? <p className="helper-copy">{signOutError}</p> : null}
      </section>
      <Routes>
        <Route path="/" element={<Navigate to="eval-dashboard" replace />} />
        {ADMIN_SECTIONS.map((section) => (
          <Route key={section.key} path={section.key} element={<GovernanceSectionPage sectionKey={section.key} />} />
        ))}
        <Route path="*" element={<Navigate to="eval-dashboard" replace />} />
      </Routes>
    </PortalShell>
  );
}
