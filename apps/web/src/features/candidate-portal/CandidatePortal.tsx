import { FormEvent, useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes, useNavigate, useSearchParams } from "react-router-dom";
import { login, type AuthSession } from "../../api/auth";
import {
  fetchCandidateConsent,
  respondCandidateConsent,
  type CandidateConsentSummary,
} from "../../api/candidateConsent";
import { type ApiResult } from "../../api/http";
import { loadAccessToken, saveAccessToken } from "../../auth/accessTokenStorage";

type CandidateSession = Pick<
  AuthSession,
  "organizationId" | "userAccountId" | "displayName" | "portalRole" | "accessTokenExpiresAt"
>;

const CANDIDATE_SESSION_STORAGE_KEY = "rto.candidatePortalSession";

function loadCandidateSession(): CandidateSession | null {
  if (typeof window === "undefined") {
    return null;
  }
  const raw = window.localStorage.getItem(CANDIDATE_SESSION_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as Partial<CandidateSession>;
    if (!parsed.userAccountId || parsed.portalRole !== "candidate") {
      return null;
    }
    return {
      organizationId: parsed.organizationId ?? "",
      userAccountId: parsed.userAccountId,
      displayName: parsed.displayName ?? "Candidate",
      portalRole: "candidate",
      accessTokenExpiresAt: parsed.accessTokenExpiresAt ?? "",
    };
  } catch {
    return null;
  }
}

function saveCandidateSession(session: AuthSession): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(
    CANDIDATE_SESSION_STORAGE_KEY,
    JSON.stringify({
      organizationId: session.organizationId,
      userAccountId: session.userAccountId,
      displayName: session.displayName,
      portalRole: session.portalRole,
      accessTokenExpiresAt: session.accessTokenExpiresAt,
    } satisfies CandidateSession),
  );
}

function clearCandidateSession(): void {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(CANDIDATE_SESSION_STORAGE_KEY);
  saveAccessToken("", "candidate");
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

function CandidateState({
  title,
  detail,
}: {
  title: string;
  detail?: string;
}) {
  return (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">Candidate consent</span>
          <h2>{title}</h2>
        </div>
      </div>
      <p className="helper-copy">{detail ?? "Candidate consent stays versioned and fail-closed."}</p>
    </section>
  );
}

function CandidateSignIn({
  onSignedIn,
}: {
  onSignedIn: (session: AuthSession) => void;
}) {
  const [organizationId, setOrganizationId] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    const result = await login({
      organizationId,
      email,
      password,
      portalRole: "candidate",
    });
    setIsSubmitting(false);
    if (result.status !== "ready") {
      setError(result.error ?? "Candidate sign in failed.");
      return;
    }
    saveAccessToken(result.data.accessToken, "candidate");
    saveCandidateSession(result.data);
    onSignedIn(result.data);
  }

  return (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">Candidate portal</span>
          <h2>Sign in to review consent</h2>
        </div>
      </div>
      <form className="workspace-stack" onSubmit={(event) => void handleSubmit(event)}>
        <label>
          Organization ID
          <input value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} />
        </label>
        <label>
          Email
          <input value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
        </label>
        <button type="submit" disabled={isSubmitting || !organizationId.trim() || !email.trim() || !password.trim()}>
          {isSubmitting ? "Signing in..." : "Sign in"}
        </button>
        {error ? <p className="helper-copy">{error}</p> : null}
      </form>
    </section>
  );
}

function ConsentPage({ session }: { session: CandidateSession }) {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const candidateRef = searchParams.get("candidateRef")?.trim() || session.userAccountId;
  const candidateProfileRef = searchParams.get("candidateProfileRef")?.trim() || "";
  const jobRef = searchParams.get("jobRef")?.trim() || "";
  const [state, setState] = useState<ApiResult<CandidateConsentSummary> | { status: "loading" }>({
    status: "loading",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function refresh() {
    if (!candidateProfileRef || !jobRef) {
      setState({
        status: "invalid_request",
        error: "candidateProfileRef and jobRef are required in the URL query.",
      });
      return;
    }
    setState({ status: "loading" });
    setState(await fetchCandidateConsent(candidateRef, candidateProfileRef, jobRef));
  }

  useEffect(() => {
    void refresh();
  }, [candidateRef, candidateProfileRef, jobRef]);

  async function handleDecision(approve: boolean) {
    setIsSubmitting(true);
    const result = await respondCandidateConsent(candidateRef, candidateProfileRef, jobRef, approve);
    setIsSubmitting(false);
    setState(result);
  }

  if (state.status === "loading") {
    return <CandidateState title="Loading consent request" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Consent request unavailable" detail={state.error} />;
  }

  const consent = state.data;

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Candidate participation portal</span>
            <h2>Review and confirm consent</h2>
            <p className="helper-copy">
              Confirming consent authorizes controlled disclosure only for the reviewed profile version.
            </p>
          </div>
          <button type="button" className="secondary-link" onClick={() => navigate("/candidate/sign-in")}>
            Switch account
          </button>
        </div>
        <dl className="mini-meta key-value-list">
          <div><dt>Candidate</dt><dd>{session.displayName}</dd></div>
          <div><dt>Job</dt><dd>{consent.jobTitle}</dd></div>
          <div><dt>Status</dt><dd>{consent.consentStatus}</dd></div>
          <div><dt>Consent text</dt><dd>{consent.consentTextVersion}</dd></div>
          <div><dt>Profile version</dt><dd>{consent.currentProfileVersion}</dd></div>
          <div><dt>Version match</dt><dd>{consent.profileVersionMatches ? "matched" : "mismatch"}</dd></div>
          <div><dt>Expires</dt><dd>{formatDate(consent.expiresAt)}</dd></div>
        </dl>
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Shared-field preview</span>
            <h2>Fields that may be disclosed after approval</h2>
          </div>
        </div>
        {consent.sharedFields.length > 0 ? (
          <div className="workspace-stack">
            {consent.sharedFields.map((field) => (
              <article key={field.fieldPath} className="portal-panel client-nested-panel">
                <strong>{field.fieldPath}</strong>
                <pre>{field.jsonValue}</pre>
              </article>
            ))}
          </div>
        ) : (
          <p className="helper-copy">No shared fields are currently attached to this consent request.</p>
        )}
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Decision</span>
            <h2>Respond to this consent request</h2>
          </div>
        </div>
        <div className="client-action-grid">
          <button type="button" onClick={() => void handleDecision(true)} disabled={isSubmitting}>
            Confirm consent
          </button>
          <button type="button" onClick={() => void handleDecision(false)} disabled={isSubmitting}>
            Decline consent
          </button>
        </div>
        {consent.revoked ? <p className="helper-copy">This consent has already been revoked.</p> : null}
      </section>
    </div>
  );
}

export function CandidatePortal() {
  const [session, setSession] = useState<CandidateSession | null>(() => loadCandidateSession());

  useEffect(() => {
    if (!loadAccessToken("candidate")) {
      setSession(null);
    }
  }, []);

  return (
    <section className="portal-layout" aria-labelledby="candidate-portal-title">
      <header className="portal-heading">
        <span className="portal-eyebrow">Candidate participation portal</span>
        <h1 id="candidate-portal-title">Candidate</h1>
      </header>
      <nav className="portal-nav" aria-label="Candidate portal">
        <NavLink to="/candidate/consent">Consent</NavLink>
        <button
          type="button"
          className="secondary-link"
          onClick={() => {
            clearCandidateSession();
            setSession(null);
          }}
        >
          Sign out
        </button>
      </nav>
      <Routes>
        <Route
          path="/"
          element={<Navigate to={session ? "/candidate/consent" : "/candidate/sign-in"} replace />}
        />
        <Route
          path="sign-in"
          element={session ? <Navigate to="/candidate/consent" replace /> : <CandidateSignIn onSignedIn={setSession} />}
        />
        <Route
          path="consent"
          element={session ? <ConsentPage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route path="*" element={<Navigate to="/candidate" replace />} />
      </Routes>
    </section>
  );
}
