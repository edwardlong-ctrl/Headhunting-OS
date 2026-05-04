import { NavLink, useParams } from "react-router-dom";
import {
  fetchClientDisclosedCandidate,
  type ClientDisclosedCandidate,
} from "../../api/clientDisclosedCandidates";
import { type ApiResult } from "../../api/http";
import { useEffect, useState } from "react";

function formatJsonValue(jsonValue: string): string {
  try {
    return JSON.stringify(JSON.parse(jsonValue), null, 2);
  } catch {
    return jsonValue;
  }
}

export function ClientDisclosedCandidatePage() {
  const { shortlistId = "", cardId = "" } = useParams();
  const [state, setState] = useState<ApiResult<ClientDisclosedCandidate> | { status: "loading" }>({
    status: "loading",
  });

  useEffect(() => {
    let active = true;
    setState({ status: "loading" });
    void fetchClientDisclosedCandidate(shortlistId, cardId).then((result) => {
      if (active) {
        setState(result);
      }
    });
    return () => {
      active = false;
    };
  }, [shortlistId, cardId]);

  return (
    <div className="workspace-stack">
      <section className="portal-panel shell-header-panel">
        <div className="section-header">
          <div>
            <span className="portal-eyebrow">Identity disclosed</span>
            <h2>Disclosed candidate detail</h2>
          </div>
          <NavLink className="secondary-link" to={`/client/shortlists/${encodeURIComponent(shortlistId)}`}>
            Back to shortlist
          </NavLink>
        </div>
      </section>
      {"status" in state && state.status === "loading" ? (
        <section className="portal-panel"><p className="helper-copy">Loading disclosed candidate...</p></section>
      ) : state.status !== "ready" ? (
        <section className="portal-panel"><p className="helper-copy">{state.error ?? "Disclosed candidate unavailable."}</p></section>
      ) : (
        <section className="portal-panel">
          <div className="section-header">
            <div>
              <span className="portal-eyebrow">{state.data.candidateStatus}</span>
              <h2>{state.data.candidateId}</h2>
            </div>
          </div>
          <dl className="mini-meta key-value-list">
            <div><dt>Disclosure record</dt><dd>{state.data.disclosureRecordRef}</dd></div>
            <div><dt>Candidate profile</dt><dd>{state.data.candidateProfileId}</dd></div>
            <div><dt>Profile version</dt><dd>{state.data.profileVersion}</dd></div>
          </dl>
          <div className="workspace-stack">
            {state.data.disclosedFields.map((field) => (
              <article key={field.fieldPath} className="portal-panel client-nested-panel">
                <strong>{field.fieldPath}</strong>
                <pre>{formatJsonValue(field.jsonValue)}</pre>
              </article>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
