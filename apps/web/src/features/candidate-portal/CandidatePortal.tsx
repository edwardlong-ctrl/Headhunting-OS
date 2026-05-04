import { FormEvent, useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes, useNavigate } from "react-router-dom";
import { login, type AuthSession } from "../../api/auth";
import {
  fetchCandidateConsent,
  respondCandidateConsent,
  type CandidateConsentSummary,
} from "../../api/candidateConsent";
import {
  fetchCandidateMe,
  fetchCandidateProfile,
  fetchCandidateDocuments,
  fetchCandidateOpportunities,
  fetchCandidateTimeline,
  uploadCandidateDocument,
  type CandidateMe,
  type CandidateProfileReview,
  type CandidateDocument,
  type CandidateOpportunity,
  type CandidateTimeline,
} from "../../api/candidatePortal";
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

function useCandidateApi<T>(
  fetcher: () => Promise<ApiResult<T>>,
): { state: ApiResult<T> | { status: "loading" }; refresh: () => void } {
  const [state, setState] = useState<ApiResult<T> | { status: "loading" }>({ status: "loading" });
  async function refresh() {
    setState({ status: "loading" });
    setState(await fetcher());
  }
  useEffect(() => {
    void refresh();
  }, []);
  return { state, refresh };
}

function CandidateState({ title, detail }: { title: string; detail?: string }) {
  return (
    <section className="portal-panel">
      <div className="section-header">
        <div>
          <span className="portal-eyebrow">Candidate portal</span>
          <h2>{title}</h2>
        </div>
      </div>
      <p className="helper-copy">{detail ?? "Loading candidate data..."}</p>
    </section>
  );
}

function CandidateSignIn({ onSignedIn }: { onSignedIn: (session: AuthSession) => void }) {
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
          <h2>Sign in to review your profile</h2>
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

function HomePage({ session }: { session: CandidateSession }) {
  const { state } = useCandidateApi(() => fetchCandidateMe());

  if (state.status === "loading") {
    return <CandidateState title="Loading dashboard" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Dashboard unavailable" detail={state.error} />;
  }
  const me = state.data;

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Candidate participation portal</span>
            <h2>Welcome back, {me.displayName}</h2>
          </div>
        </div>
        <dl className="mini-meta key-value-list">
          <div><dt>Profile version</dt><dd>{me.currentProfileVersion}</dd></div>
          <div><dt>Documents</dt><dd>{me.documentCount}</dd></div>
          <div><dt>Opportunities</dt><dd>{me.activeOpportunityCount}</dd></div>
          <div><dt>Pending follow-ups</dt><dd>{me.pendingFollowUpCount}</dd></div>
        </dl>
      </section>
    </div>
  );
}

function ProfilePage({ session }: { session: CandidateSession }) {
  const { state } = useCandidateApi(() => fetchCandidateProfile(session.userAccountId));

  if (state.status === "loading") {
    return <CandidateState title="Loading profile" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Profile unavailable" detail={state.error} />;
  }
  const profile = state.data;

  return (
    <div className="workspace-stack">
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">AI-extracted profile review</span>
            <h2>Review your profile</h2>
            <p className="helper-copy">
              Review AI-extracted fields and confirm accuracy. Profile version {profile.profileVersion}.
            </p>
          </div>
        </div>
      </section>
      {profile.fields.length > 0 ? (
        <div className="workspace-stack">
          {profile.fields.map((field) => (
            <article key={field.fieldPath} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{field.fieldPath}</strong>
                <span className="badge">{field.status}</span>
              </div>
              <pre>{field.jsonValue}</pre>
              <p className="helper-copy">Source: {field.sourceType} {field.updatedAt ? `· Updated ${formatDate(field.updatedAt)}` : ""}</p>
            </article>
          ))}
        </div>
      ) : (
        <section className="portal-panel">
          <p className="helper-copy">No profile fields available yet.</p>
        </section>
      )}
    </div>
  );
}

function DocumentsPage({ session }: { session: CandidateSession }) {
  const { state, refresh } = useCandidateApi(() => fetchCandidateDocuments());
  const [uploading, setUploading] = useState(false);

  async function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setUploading(true);
    await uploadCandidateDocument(file);
    setUploading(false);
    refresh();
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Resume / profile documents</span>
            <h2>Your documents</h2>
          </div>
        </div>
        <label className="workspace-stack">
          <span>Upload new document</span>
          <input type="file" accept=".pdf,.doc,.docx,.txt" onChange={(e) => void handleFileChange(e)} disabled={uploading} />
          {uploading ? <span>Uploading...</span> : null}
        </label>
      </section>
      {state.status === "ready" && state.data.items.length > 0 ? (
        <div className="workspace-stack">
          {state.data.items.map((doc) => (
            <article key={doc.documentId} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{doc.title}</strong>
                <span className="badge">{doc.status}</span>
              </div>
              <p className="helper-copy">{doc.documentType} · {doc.mimeType} · Uploaded {formatDate(doc.uploadedAt)}</p>
            </article>
          ))}
        </div>
      ) : state.status !== "loading" ? (
        <section className="portal-panel">
          <p className="helper-copy">No documents uploaded yet.</p>
        </section>
      ) : null}
    </div>
  );
}

function OpportunitiesPage({ session }: { session: CandidateSession }) {
  const { state } = useCandidateApi(() => fetchCandidateOpportunities());

  if (state.status === "loading") {
    return <CandidateState title="Loading opportunities" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Opportunities unavailable" detail={state.error} />;
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Active opportunities</span>
            <h2>Your opportunities</h2>
          </div>
        </div>
      </section>
      {state.data.items.length > 0 ? (
        <div className="workspace-stack">
          {state.data.items.map((opp) => (
            <article key={opp.interactionId} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{opp.jobTitle}</strong>
                <span className="badge">{opp.status}</span>
              </div>
              <p className="helper-copy">{opp.companyName} · {opp.interactionType}</p>
            </article>
          ))}
        </div>
      ) : (
        <section className="portal-panel">
          <p className="helper-copy">No active opportunities at this time.</p>
        </section>
      )}
    </div>
  );
}

function TimelinePage({ session }: { session: CandidateSession }) {
  const { state } = useCandidateApi(() => fetchCandidateTimeline(session.userAccountId));

  if (state.status === "loading") {
    return <CandidateState title="Loading timeline" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Timeline unavailable" detail={state.error} />;
  }
  const timeline = state.data;

  return (
    <div className="workspace-stack">
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Status timeline</span>
            <h2>Your activity timeline</h2>
          </div>
        </div>
      </section>
      {timeline.events.length > 0 ? (
        <div className="workspace-stack">
          {timeline.events.map((event, index) => (
            <article key={index} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{event.actionCode}</strong>
                <span className="badge">{event.eventType}</span>
              </div>
              <p className="helper-copy">{event.reason}</p>
              <p className="helper-copy">{formatDate(event.occurredAt)}</p>
            </article>
          ))}
        </div>
      ) : (
        <section className="portal-panel">
          <p className="helper-copy">No timeline events yet.</p>
        </section>
      )}
    </div>
  );
}

function ConsentPage({ session }: { session: CandidateSession }) {
  const [candidateProfileRef, setCandidateProfileRef] = useState("");
  const [jobRef, setJobRef] = useState("");
  const [state, setState] = useState<ApiResult<CandidateConsentSummary> | { status: "loading" } | { status: "idle" }>({ status: "idle" });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  async function refresh() {
    if (!candidateProfileRef || !jobRef) return;
    setState({ status: "loading" });
    setState(await fetchCandidateConsent(session.userAccountId, candidateProfileRef, jobRef));
  }

  async function handleDecision(approve: boolean) {
    if (!candidateProfileRef || !jobRef) return;
    setIsSubmitting(true);
    const result = await respondCandidateConsent(session.userAccountId, candidateProfileRef, jobRef, approve);
    setIsSubmitting(false);
    setState(result);
  }

  if (state.status === "idle") {
    return (
      <div className="workspace-stack">
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Consent request</span>
              <h2>Look up a consent request</h2>
            </div>
          </div>
          <div className="workspace-stack">
            <label>
              Profile Reference
              <input value={candidateProfileRef} onChange={(e) => setCandidateProfileRef(e.target.value)} placeholder="Paste profile reference" />
            </label>
            <label>
              Job Reference
              <input value={jobRef} onChange={(e) => setJobRef(e.target.value)} placeholder="Paste job reference" />
            </label>
            <button type="button" onClick={() => void refresh()} disabled={!candidateProfileRef || !jobRef}>
              Look up
            </button>
          </div>
        </section>
      </div>
    );
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
          <button type="button" className="secondary-link" onClick={() => navigate("/candidate/consent")}>
            Look up another
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
        <NavLink to="/candidate/home">Home</NavLink>
        <NavLink to="/candidate/profile">Profile</NavLink>
        <NavLink to="/candidate/documents">Documents</NavLink>
        <NavLink to="/candidate/opportunities">Opportunities</NavLink>
        <NavLink to="/candidate/timeline">Timeline</NavLink>
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
          element={<Navigate to={session ? "/candidate/home" : "/candidate/sign-in"} replace />}
        />
        <Route
          path="sign-in"
          element={session ? <Navigate to="/candidate/home" replace /> : <CandidateSignIn onSignedIn={setSession} />}
        />
        <Route
          path="home"
          element={session ? <HomePage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="profile"
          element={session ? <ProfilePage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="documents"
          element={session ? <DocumentsPage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="opportunities"
          element={session ? <OpportunitiesPage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="timeline"
          element={session ? <TimelinePage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
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
