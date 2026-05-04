import { type ChangeEvent, FormEvent, useEffect, useState } from "react";
import { Navigate, NavLink, Route, Routes, useNavigate, useParams } from "react-router-dom";
import { login, type AuthSession } from "../../api/auth";
import {
  fetchCandidateConsent,
  respondCandidateConsent,
  type CandidateConsentSummary,
} from "../../api/candidateConsent";
import {
  confirmCandidateProfile,
  fetchCandidateDocuments,
  fetchCandidateFollowUp,
  fetchCandidateMe,
  fetchCandidateOpportunityDetail,
  fetchCandidateOpportunities,
  fetchCandidateProfile,
  fetchCandidateTimeline,
  recordCandidateOpportunityInterest,
  submitCandidateFollowUp,
  uploadCandidateDocument,
  type CandidateDocument,
  type CandidateFollowUpForm,
  type CandidateOpportunity,
  type CandidateOpportunityDetail,
  type CandidateProfileReview,
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

function renderJsonValue(jsonValue: string): string {
  try {
    const parsed = JSON.parse(jsonValue) as unknown;
    if (typeof parsed === "string" || typeof parsed === "number" || typeof parsed === "boolean") {
      return String(parsed);
    }
    if (Array.isArray(parsed)) {
      return parsed.map((item) => (typeof item === "string" ? item : JSON.stringify(item))).join(", ");
    }
    return JSON.stringify(parsed, null, 2);
  } catch {
    return jsonValue;
  }
}

function isActionableProfileField(field: CandidateProfileReview["fields"][number]): boolean {
  return field.status === "needs_confirmation" || field.status === "conflicting" || field.status === "stale";
}

function consentActionLabel(opportunity: { consentStatus: string | null }): string {
  if (opportunity.consentStatus === "requested" || opportunity.consentStatus === "viewed_by_candidate") {
    return "Review consent request";
  }
  if (opportunity.consentStatus === "confirmed") {
    return "View confirmed consent";
  }
  if (opportunity.consentStatus === "declined") {
    return "View declined consent";
  }
  return "Open consent details";
}

function interestStatusLabel(status: string): string {
  switch (status) {
    case "open_to_explore":
      return "Open to explore";
    case "interested_confirmed":
      return "Interested and confirmed";
    case "declined":
      return "Declined";
    default:
      return "Not answered";
  }
}

function useCandidateApi<T>(
  fetcher: () => Promise<ApiResult<T>>,
  deps: ReadonlyArray<unknown>,
): { state: ApiResult<T> | { status: "loading" }; refresh: () => void } {
  const [state, setState] = useState<ApiResult<T> | { status: "loading" }>({ status: "loading" });

  async function refresh() {
    setState({ status: "loading" });
    setState(await fetcher());
  }

  useEffect(() => {
    void refresh();
  }, deps);

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

function HomePage() {
  const { state } = useCandidateApi(() => fetchCandidateMe(), []);

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
        <div className="client-action-grid">
          <NavLink className="secondary-link" to="/candidate/profile">
            Review profile
          </NavLink>
          <NavLink className="secondary-link" to="/candidate/follow-up/current-profile">
            Open follow-up form
          </NavLink>
        </div>
      </section>
    </div>
  );
}

function ProfilePage({ session }: { session: CandidateSession }) {
  const { state, refresh } = useCandidateApi(
    () => fetchCandidateProfile(session.userAccountId),
    [session.userAccountId],
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);

  async function handleConfirmField(fieldPath: string) {
    setIsSubmitting(true);
    setSubmitMessage(null);
    const result = await confirmCandidateProfile(session.userAccountId, fieldPath);
    setIsSubmitting(false);
    if (result.status !== "ready") {
      setSubmitMessage(result.error ?? "Profile confirmation failed.");
      return;
    }
    setSubmitMessage(`Confirmed ${fieldPath}.`);
    refresh();
  }

  if (state.status === "loading") {
    return <CandidateState title="Loading profile" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Profile unavailable" detail={state.error} />;
  }
  const profile = state.data;
  const actionableFields = profile.fields.filter(isActionableProfileField);

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
        {actionableFields.length > 0 ? (
          <div className="workspace-stack">
            <p className="helper-copy">
              {actionableFields.length} field{actionableFields.length === 1 ? "" : "s"} still need your confirmation.
            </p>
            <div className="client-action-grid">
              <NavLink className="secondary-link" to="/candidate/follow-up/current-profile">
                Open follow-up form
              </NavLink>
            </div>
            {submitMessage ? <p className="helper-copy">{submitMessage}</p> : null}
          </div>
        ) : (
          <p className="helper-copy">No candidate follow-up items are pending on this profile version.</p>
        )}
      </section>
      {profile.fields.length > 0 ? (
        <div className="workspace-stack">
          {profile.fields.map((field) => (
            <article key={field.fieldPath} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{field.fieldPath}</strong>
                <span className="badge">{field.status}</span>
              </div>
              {isActionableProfileField(field) ? (
                <div className="client-action-grid">
                  <button type="button" onClick={() => void handleConfirmField(field.fieldPath)} disabled={isSubmitting}>
                    {isSubmitting ? "Confirming..." : "Confirm this field"}
                  </button>
                </div>
              ) : null}
              <pre>{renderJsonValue(field.jsonValue)}</pre>
              <p className="helper-copy">
                Source: {field.sourceType} {field.updatedAt ? `· Updated ${formatDate(field.updatedAt)}` : ""}
              </p>
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

function DocumentsPage() {
  const { state, refresh } = useCandidateApi(() => fetchCandidateDocuments(), []);
  const [uploading, setUploading] = useState(false);

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
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
          <input type="file" accept=".pdf,.doc,.docx,.txt" onChange={(event) => void handleFileChange(event)} disabled={uploading} />
          {uploading ? <span>Uploading...</span> : null}
        </label>
      </section>
      {state.status === "ready" && state.data.items.length > 0 ? (
        <div className="workspace-stack">
          {state.data.items.map((doc: CandidateDocument) => (
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

function OpportunitiesPage() {
  const { state } = useCandidateApi(() => fetchCandidateOpportunities(), []);

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
          {state.data.items.map((opp: CandidateOpportunity) => (
            <article key={opp.interactionId} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{opp.jobTitle}</strong>
                <span className="badge">{opp.status}</span>
              </div>
              <p className="helper-copy">{opp.companyName} · {opp.interactionType}</p>
              <p className="helper-copy">Interest status: {interestStatusLabel(opp.interestStatus)}</p>
              <p className="helper-copy">Consent status: {opp.consentStatus ?? "not requested"}</p>
              <div className="client-action-grid">
                <NavLink className="secondary-link" to={`/candidate/opportunities/${encodeURIComponent(opp.interactionId)}`}>
                  View opportunity
                </NavLink>
                {opp.consentRecordRef ? (
                  <NavLink className="secondary-link" to={`/candidate/consent/${encodeURIComponent(opp.consentRecordRef)}`}>
                    {consentActionLabel(opp)}
                  </NavLink>
                ) : null}
              </div>
              {!opp.consentRecordRef ? (
                <p className="helper-copy">No consent request is available for this opportunity yet.</p>
              ) : null}
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

function FollowUpFieldInput({
  item,
  value,
  disabled,
  onChange,
}: {
  item: CandidateFollowUpForm["items"][number];
  value: string;
  disabled: boolean;
  onChange: (nextValue: string) => void;
}) {
  if (item.inputType === "textarea" || item.inputType === "list") {
    return (
      <textarea
        value={value}
        disabled={disabled}
        rows={item.inputType === "list" ? 3 : 4}
        onChange={(event) => onChange(event.target.value)}
        placeholder={item.inputType === "list" ? "Enter comma-separated answers" : "Enter your answer"}
      />
    );
  }
  return (
    <input
      type="text"
      value={value}
      disabled={disabled}
      onChange={(event) => onChange(event.target.value)}
      placeholder="Enter your answer"
    />
  );
}

function FollowUpPage({ session }: { session: CandidateSession }) {
  const { formId } = useParams();
  const requestedFormId = formId ?? "";
  const { state, refresh } = useCandidateApi(
    () => fetchCandidateFollowUp(session.userAccountId, requestedFormId),
    [session.userAccountId, requestedFormId],
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});

  useEffect(() => {
    if (state.status !== "ready") {
      return;
    }
    setAnswers(
      Object.fromEntries(
        state.data.items.map((item) => [item.fieldPath, item.currentAnswer === "Not answered yet" ? "" : item.currentAnswer]),
      ),
    );
  }, [state]);

  if (state.status === "loading") {
    return <CandidateState title="Loading follow-up form" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Follow-up form unavailable" detail={state.error} />;
  }

  const followUp = state.data;

  async function handleSubmit(fieldPath: string) {
    setIsSubmitting(true);
    setSubmitMessage(null);
    const result = await submitCandidateFollowUp(
      session.userAccountId,
      followUp.formId,
      fieldPath,
      answers[fieldPath] ?? "",
    );
    setIsSubmitting(false);
    if (result.status !== "ready") {
      setSubmitMessage(result.error ?? "Unable to submit this follow-up form.");
      return;
    }
    setSubmitMessage(`Follow-up submitted for ${fieldPath}.`);
    refresh();
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Follow-up form</span>
            <h2>Answer the follow-up questions for your current profile</h2>
            <p className="helper-copy">
              This form asks for updated answers on profile fields that still need confirmation, clarification, or stale refresh.
            </p>
          </div>
        </div>
        <dl className="mini-meta key-value-list">
          <div><dt>Form ID</dt><dd>{followUp.formId}</dd></div>
          <div><dt>Profile version</dt><dd>{followUp.profileVersion}</dd></div>
          <div><dt>Pending items</dt><dd>{followUp.items.length}</dd></div>
        </dl>
      </section>
      {followUp.items.length > 0 ? (
        <div className="workspace-stack">
          {followUp.items.map((item) => (
            <article key={item.fieldPath} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{item.fieldPath}</strong>
                <span className="badge">{item.status}</span>
              </div>
              <p className="helper-copy">{item.prompt}</p>
              <FollowUpFieldInput
                item={item}
                value={answers[item.fieldPath] ?? ""}
                disabled={isSubmitting}
                onChange={(nextValue) =>
                  setAnswers((current) => ({
                    ...current,
                    [item.fieldPath]: nextValue,
                  }))
                }
              />
              <p className="helper-copy">
                Source: {item.sourceType} {item.updatedAt ? `· Updated ${formatDate(item.updatedAt)}` : ""}
              </p>
              <div className="client-action-grid">
                <button
                  type="button"
                  onClick={() => void handleSubmit(item.fieldPath)}
                  disabled={isSubmitting || !(answers[item.fieldPath] ?? "").trim()}
                >
                  {isSubmitting ? "Submitting..." : "Submit answer"}
                </button>
              </div>
            </article>
          ))}
          <section className="portal-panel">
            <div className="client-action-grid">
              <NavLink className="secondary-link" to="/candidate/profile">
                Back to profile review
              </NavLink>
            </div>
            {submitMessage ? <p className="helper-copy">{submitMessage}</p> : null}
          </section>
        </div>
      ) : (
        <section className="portal-panel">
          <p className="helper-copy">No candidate follow-up items are pending right now.</p>
        </section>
      )}
    </div>
  );
}

function OpportunityDetailPage({ session }: { session: CandidateSession }) {
  const { interactionId } = useParams();
  const requestedInteractionId = interactionId ?? "";
  const { state, refresh } = useCandidateApi(
    () => fetchCandidateOpportunityDetail(session.userAccountId, requestedInteractionId),
    [session.userAccountId, requestedInteractionId],
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [interestNote, setInterestNote] = useState("");
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);

  async function handleInterest(interestStatus: string) {
    if (!requestedInteractionId) {
      return;
    }
    setIsSubmitting(true);
    setSubmitMessage(null);
    const result = await recordCandidateOpportunityInterest(
      session.userAccountId,
      requestedInteractionId,
      interestStatus,
      interestNote,
    );
    setIsSubmitting(false);
    if (result.status !== "ready") {
      setSubmitMessage(result.error ?? "Unable to update your interest.");
      return;
    }
    setSubmitMessage(`Interest updated to ${interestStatusLabel(result.data.interestStatus)}.`);
    refresh();
  }

  if (!requestedInteractionId) {
    return <CandidateState title="Opportunity unavailable" detail="An opportunity reference is required." />;
  }
  if (state.status === "loading") {
    return <CandidateState title="Loading opportunity" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Opportunity unavailable" detail={state.error} />;
  }

  const opportunity: CandidateOpportunityDetail = state.data;

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Opportunity view</span>
            <h2>{opportunity.jobTitle}</h2>
            <p className="helper-copy">{opportunity.companyName}</p>
          </div>
          <NavLink className="secondary-link" to="/candidate/opportunities">
            Back to opportunities
          </NavLink>
        </div>
        <dl className="mini-meta key-value-list">
          <div><dt>Workflow status</dt><dd>{opportunity.status}</dd></div>
          <div><dt>Interest</dt><dd>{interestStatusLabel(opportunity.interestStatus)}</dd></div>
          <div><dt>Location</dt><dd>{opportunity.location}</dd></div>
          <div><dt>Compensation</dt><dd>{opportunity.compensation}</dd></div>
        </dl>
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Role summary</span>
            <h2>Why this opportunity may fit</h2>
          </div>
        </div>
        <p>{opportunity.roleSummary}</p>
        <p className="helper-copy">{opportunity.fitExplanation}</p>
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Your response</span>
            <h2>Confirm whether you want to move forward</h2>
          </div>
        </div>
        <label className="workspace-stack">
          <span>Optional note for the recruiting team</span>
          <textarea
            rows={4}
            value={interestNote}
            disabled={isSubmitting}
            onChange={(event) => setInterestNote(event.target.value)}
            placeholder="Share context such as timing, salary, or concerns."
          />
        </label>
        <div className="client-action-grid">
          <button type="button" onClick={() => void handleInterest("open_to_explore")} disabled={isSubmitting}>
            Open to explore
          </button>
          <button type="button" onClick={() => void handleInterest("interested_confirmed")} disabled={isSubmitting}>
            Confirm interest
          </button>
          <button type="button" onClick={() => void handleInterest("declined")} disabled={isSubmitting}>
            Decline opportunity
          </button>
        </div>
        {submitMessage ? <p className="helper-copy">{submitMessage}</p> : null}
      </section>
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Consent</span>
            <h2>Opportunity-linked consent request</h2>
          </div>
        </div>
        {opportunity.consentRecordRef ? (
          <div className="client-action-grid">
            <NavLink className="secondary-link" to={`/candidate/consent/${encodeURIComponent(opportunity.consentRecordRef)}`}>
              {consentActionLabel(opportunity)}
            </NavLink>
          </div>
        ) : (
          <p className="helper-copy">No consent request is available for this opportunity yet.</p>
        )}
      </section>
    </div>
  );
}

function TimelinePage({ session }: { session: CandidateSession }) {
  const { state } = useCandidateApi(() => fetchCandidateTimeline(session.userAccountId), [session.userAccountId]);

  if (state.status === "loading") {
    return <CandidateState title="Loading timeline" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Timeline unavailable" detail={state.error} />;
  }
  const timeline: CandidateTimeline = state.data;

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

function ConsentInboxPage() {
  const { state } = useCandidateApi(() => fetchCandidateOpportunities(), []);

  if (state.status === "loading") {
    return <CandidateState title="Loading consent requests" />;
  }
  if (state.status !== "ready") {
    return <CandidateState title="Consent requests unavailable" detail={state.error} />;
  }

  const consentItems = state.data.items.filter((item: CandidateOpportunity) => item.consentRecordRef);

  return (
    <div className="workspace-stack">
      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Consent request</span>
            <h2>Open a consent request from your active opportunities</h2>
            <p className="helper-copy">
              Each consent request stays bound to a specific opportunity, profile reference, profile version, consent text version, and shared-field preview.
            </p>
          </div>
        </div>
      </section>
      {consentItems.length > 0 ? (
        <div className="workspace-stack">
          {consentItems.map((opp) => (
            <article key={opp.interactionId} className="portal-panel client-nested-panel">
              <div className="section-header">
                <strong>{opp.jobTitle}</strong>
                <span className="badge">{opp.consentStatus}</span>
              </div>
              <p className="helper-copy">{opp.companyName}</p>
              <NavLink className="secondary-link" to={`/candidate/consent/${encodeURIComponent(opp.consentRecordRef ?? "")}`}>
                {consentActionLabel(opp)}
              </NavLink>
            </article>
          ))}
        </div>
      ) : (
        <section className="portal-panel">
          <p className="helper-copy">No consent-linked opportunities are available yet.</p>
        </section>
      )}
    </div>
  );
}

function ConsentRequestPage({ session }: { session: CandidateSession }) {
  const { consentRecordRef } = useParams();
  const [state, setState] = useState<ApiResult<CandidateConsentSummary> | { status: "loading" }>({ status: "loading" });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!consentRecordRef) {
      return;
    }
    const requestId = consentRecordRef;
    let cancelled = false;
    async function refresh() {
      setState({ status: "loading" });
      const result = await fetchCandidateConsent(session.userAccountId, requestId);
      if (!cancelled) {
        setState(result);
      }
    }
    void refresh();
    return () => {
      cancelled = true;
    };
  }, [session.userAccountId, consentRecordRef]);

  async function handleDecision(approve: boolean) {
    if (!consentRecordRef) return;
    setIsSubmitting(true);
    const result = await respondCandidateConsent(session.userAccountId, consentRecordRef, approve);
    setIsSubmitting(false);
    setState(result);
  }

  if (!consentRecordRef) {
    return <CandidateState title="Consent request unavailable" detail="A consent request reference is required." />;
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
            Back to consent inbox
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
                <pre>{renderJsonValue(field.jsonValue)}</pre>
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

function ConsentPage({ session }: { session: CandidateSession }) {
  const { consentRecordRef } = useParams();
  if (!consentRecordRef) {
    return <ConsentInboxPage />;
  }
  return <ConsentRequestPage session={session} />;
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
        <NavLink to="/candidate/follow-up/current-profile">Follow-up</NavLink>
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
          element={session ? <HomePage /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="profile"
          element={session ? <ProfilePage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="follow-up/:formId"
          element={session ? <FollowUpPage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="documents"
          element={session ? <DocumentsPage /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="opportunities"
          element={session ? <OpportunitiesPage /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="opportunities/:interactionId"
          element={session ? <OpportunityDetailPage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="timeline"
          element={session ? <TimelinePage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="consent"
          element={session ? <ConsentPage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route
          path="consent/:consentRecordRef"
          element={session ? <ConsentPage session={session} /> : <Navigate to="/candidate/sign-in" replace />}
        />
        <Route path="*" element={<Navigate to="/candidate" replace />} />
      </Routes>
    </section>
  );
}
