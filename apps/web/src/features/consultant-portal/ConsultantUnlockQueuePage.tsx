import { useEffect, useState } from "react";
import {
  approveConsultantUnlockRequest,
  createConsultantConsentRequest,
  fetchConsultantUnlockQueue,
  rejectConsultantUnlockRequest,
  type ConsultantUnlockDecision,
  type ConsultantUnlockQueueItem,
} from "../../api/consultantUnlocks";
import { type ApiResult } from "../../api/http";

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

export function ConsultantUnlockQueuePage() {
  const [state, setState] = useState<ApiResult<{ items: ConsultantUnlockQueueItem[] }> | { status: "loading" }>({
    status: "loading",
  });
  const [decisionReasons, setDecisionReasons] = useState<Record<string, string>>({});
  const [consentDraft, setConsentDraft] = useState({
    candidateRef: "",
    candidateProfileRef: "",
    jobRef: "",
    consentTextVersion: "v2.1-task33",
    expiresAt: "",
  });
  const [isBusy, setIsBusy] = useState<string | null>(null);
  const [lastDecision, setLastDecision] = useState<ConsultantUnlockDecision | null>(null);
  const [consentMessage, setConsentMessage] = useState<string | null>(null);

  async function refresh() {
    setState({ status: "loading" });
    setState(await fetchConsultantUnlockQueue());
  }

  useEffect(() => {
    void refresh();
  }, []);

  async function handleApprove(item: ConsultantUnlockQueueItem) {
    const reason = (decisionReasons[item.shortlistCandidateCardId] ?? "").trim() || "Consultant approved unlock.";
    setIsBusy(item.shortlistCandidateCardId);
    const result = await approveConsultantUnlockRequest(item.shortlistId, item.shortlistCandidateCardId, reason);
    setIsBusy(null);
    if (result.status === "ready") {
      setLastDecision(result.data);
      await refresh();
    }
  }

  async function handleReject(item: ConsultantUnlockQueueItem) {
    const reason = (decisionReasons[item.shortlistCandidateCardId] ?? "").trim() || "Consultant rejected unlock.";
    setIsBusy(item.shortlistCandidateCardId);
    const result = await rejectConsultantUnlockRequest(item.shortlistId, item.shortlistCandidateCardId, reason);
    setIsBusy(null);
    if (result.status === "ready") {
      setLastDecision(result.data);
      await refresh();
    }
  }

  async function handleRequestConsent() {
    setConsentMessage(null);
    setIsBusy("consent-request");
    const result = await createConsultantConsentRequest({
      candidateRef: consentDraft.candidateRef.trim(),
      candidateProfileRef: consentDraft.candidateProfileRef.trim(),
      jobRef: consentDraft.jobRef.trim(),
      consentTextVersion: consentDraft.consentTextVersion.trim() || "v2.1-task33",
      expiresAt: consentDraft.expiresAt.trim() || null,
    });
    setIsBusy(null);
    setConsentMessage(result.status === "ready" ? "Consent request created." : result.error ?? "Consent request failed.");
  }

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Unlock approval workspace</span>
            <h2>Consent / disclosure / unlock queue</h2>
            <p className="helper-copy">
              Review candidate consent, blockers, and client unlock requests before releasing identity.
            </p>
          </div>
        </div>
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Candidate consent request</span>
            <h2>Create a consent request</h2>
          </div>
        </div>
        <div className="workspace-stack">
          <label>
            Candidate ref
            <input
              value={consentDraft.candidateRef}
              onChange={(event) => setConsentDraft((current) => ({ ...current, candidateRef: event.target.value }))}
            />
          </label>
          <label>
            Candidate profile ref
            <input
              value={consentDraft.candidateProfileRef}
              onChange={(event) => setConsentDraft((current) => ({ ...current, candidateProfileRef: event.target.value }))}
            />
          </label>
          <label>
            Job ref
            <input
              value={consentDraft.jobRef}
              onChange={(event) => setConsentDraft((current) => ({ ...current, jobRef: event.target.value }))}
            />
          </label>
          <label>
            Consent text version
            <input
              value={consentDraft.consentTextVersion}
              onChange={(event) => setConsentDraft((current) => ({ ...current, consentTextVersion: event.target.value }))}
            />
          </label>
          <label>
            Expires at ISO timestamp
            <input
              value={consentDraft.expiresAt}
              onChange={(event) => setConsentDraft((current) => ({ ...current, expiresAt: event.target.value }))}
              placeholder="2026-06-01T00:00:00Z"
            />
          </label>
          <button
            type="button"
            disabled={
              isBusy === "consent-request"
              || !consentDraft.candidateRef.trim()
              || !consentDraft.candidateProfileRef.trim()
              || !consentDraft.jobRef.trim()
            }
            onClick={() => void handleRequestConsent()}
          >
            {isBusy === "consent-request" ? "Submitting..." : "Request candidate consent"}
          </button>
          {consentMessage ? <p className="helper-copy">{consentMessage}</p> : null}
        </div>
      </section>

      <section className="portal-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Pending queue</span>
            <h2>Unlock requests awaiting consultant action</h2>
          </div>
        </div>
        {"status" in state && state.status === "loading" ? (
          <p className="helper-copy">Loading unlock queue...</p>
        ) : state.status !== "ready" ? (
          <p className="helper-copy">{state.error ?? "Unlock queue unavailable."}</p>
        ) : state.data.items.length === 0 ? (
          <p className="helper-copy">No pending unlock requests.</p>
        ) : (
          <div className="workspace-stack">
            {state.data.items.map((item) => (
              <article key={item.unlockRequestId} className="portal-panel client-nested-panel">
                <div className="section-header">
                  <div>
                    <span className="portal-eyebrow">{item.jobTitle}</span>
                    <h3>{item.anonymousCandidateCardRef}</h3>
                  </div>
                  <span className="status-badge status-badge-accent">{item.status}</span>
                </div>
                <dl className="mini-meta key-value-list">
                  <div><dt>Client</dt><dd>{item.clientCompanyName}</dd></div>
                  <div><dt>Consent</dt><dd>{item.consentStatus}</dd></div>
                  <div><dt>Requested at</dt><dd>{formatDate(item.createdAt)}</dd></div>
                  <div><dt>Reason</dt><dd>{item.requestReason}</dd></div>
                </dl>
                {item.blockers.length > 0 ? (
                  <ul className="helper-copy">
                    {item.blockers.map((blocker) => (
                      <li key={`${item.unlockRequestId}-${blocker.code}`}>
                        {blocker.code}: {blocker.message}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="helper-copy">No blockers detected. Consultant approval can generate disclosure.</p>
                )}
                <label>
                  Decision reason
                  <textarea
                    rows={3}
                    value={decisionReasons[item.shortlistCandidateCardId] ?? ""}
                    onChange={(event) =>
                      setDecisionReasons((current) => ({
                        ...current,
                        [item.shortlistCandidateCardId]: event.target.value,
                      }))
                    }
                  />
                </label>
                <div className="client-action-grid">
                  <button
                    type="button"
                    disabled={isBusy === item.shortlistCandidateCardId}
                    onClick={() => void handleApprove(item)}
                  >
                    Approve unlock
                  </button>
                  <button
                    type="button"
                    disabled={isBusy === item.shortlistCandidateCardId}
                    onClick={() => void handleReject(item)}
                  >
                    Reject unlock
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {lastDecision ? (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">Latest consultant decision</span>
              <h2>{lastDecision.status}</h2>
            </div>
          </div>
          <dl className="mini-meta key-value-list">
            <div><dt>Unlock request</dt><dd>{lastDecision.unlockRequestId ?? "not_recorded"}</dd></div>
            <div><dt>Unlock decision</dt><dd>{lastDecision.unlockDecisionRef ?? "not_recorded"}</dd></div>
            <div><dt>Disclosure record</dt><dd>{lastDecision.approvedDisclosureRecordRef ?? "not_recorded"}</dd></div>
          </dl>
        </section>
      ) : null}
    </div>
  );
}
