import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Navigate,
  NavLink,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";
import {
  ClientSafeCandidateCard,
  ClientSafeCandidateCardResult,
  fetchClientSafeCandidateCard,
  isAnonymousCardRef,
} from "./api/clientSafeCandidateCards";
import { loadAccessToken, saveAccessToken } from "./auth/accessTokenStorage";
import { ConsultantPortal } from "./features/consultant-portal/ConsultantPortal";

type PortalKey = "owner" | "consultant" | "client" | "candidate" | "admin";

type PortalDefinition = {
  key: PortalKey;
  path: `/${PortalKey}`;
  label: string;
  title: string;
  eyebrow: string;
  primaryModules: string[];
  governanceModules: string[];
};

const portalRoutes: PortalDefinition[] = [
  {
    key: "owner",
    path: "/owner",
    label: "Owner",
    title: "Owner / Partner",
    eyebrow: "Business command portal",
    primaryModules: ["Dashboard", "Pipeline", "Consultants", "Clients", "Revenue"],
    governanceModules: ["Risk", "Data quality", "AI quality", "Audit"],
  },
  {
    key: "consultant",
    path: "/consultant",
    label: "Consultant",
    title: "Consultant",
    eyebrow: "Unified consultant portal",
    primaryModules: [
      "Dashboard",
      "AI Intake",
      "Talent Pool",
      "Companies",
      "Jobs",
      "Matching",
      "Shortlist",
    ],
    governanceModules: ["Follow-ups", "Workflow", "Placements", "Commission", "Reports"],
  },
  {
    key: "client",
    path: "/client",
    label: "Client",
    title: "Client",
    eyebrow: "Client hiring workspace",
    primaryModules: ["Dashboard", "Jobs", "Clarification", "Shortlist", "Feedback"],
    governanceModules: ["Client-safe candidate card", "Disclosure request pending future gate"],
  },
  {
    key: "candidate",
    path: "/candidate",
    label: "Candidate",
    title: "Candidate",
    eyebrow: "Candidate participation portal",
    primaryModules: ["Profile review", "Follow-up answers", "Opportunities", "Consent"],
    governanceModules: ["Versioned profile confirmation", "Shared-field preview"],
  },
  {
    key: "admin",
    path: "/admin",
    label: "Admin",
    title: "Admin / System",
    eyebrow: "Governance and platform control",
    primaryModules: ["AI policy", "Task registry", "Industry packs", "Workflow", "Audit"],
    governanceModules: ["Claim Ledger", "Ontology version", "Redaction policy", "Eval dashboard"],
  },
];

function findPortal(key: PortalKey): PortalDefinition {
  return portalRoutes.find((portal) => portal.key === key) ?? portalRoutes[1];
}

function PortalLayout({
  portal,
  children,
}: {
  portal: PortalDefinition;
  children?: React.ReactNode;
}) {
  return (
    <section className="portal-layout" aria-labelledby={`${portal.key}-portal-title`}>
      <header className="portal-heading">
        <span className="portal-eyebrow">{portal.eyebrow}</span>
        <h1 id={`${portal.key}-portal-title`}>{portal.title}</h1>
      </header>

      <div className="portal-grid" aria-label={`${portal.title} portal modules`}>
        <section className="portal-panel">
          <h2>Core Routes</h2>
          <div className="module-list">
            {portal.primaryModules.map((module) => (
              <span key={module}>{module}</span>
            ))}
          </div>
        </section>
        <section className="portal-panel">
          <h2>Governance Surface</h2>
          <div className="module-list">
            {portal.governanceModules.map((module) => (
              <span key={module}>{module}</span>
            ))}
          </div>
        </section>
      </div>

      {children}
    </section>
  );
}

function StaticPortal({ portalKey }: { portalKey: PortalKey }) {
  return <PortalLayout portal={findPortal(portalKey)} />;
}

function ClientPortal() {
  const [cardRef, setCardRef] = useState("");
  const [accessToken, setAccessToken] = useState(() => loadAccessToken("client") ?? "");
  const navigate = useNavigate();

  function openClientSafeCard(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedCardRef = cardRef.trim();
    if (normalizedCardRef.length > 0) {
      navigate(`/client/candidate-cards/${encodeURIComponent(normalizedCardRef)}`);
    }
  }

  return (
    <PortalLayout portal={findPortal("client")}>
      <section className="client-card-entry" aria-labelledby="client-card-entry-title">
        <div>
          <h2 id="client-card-entry-title">Client-safe Candidate Card</h2>
          <p>
            Backend-approved read boundary for anonymous candidate cards only.
          </p>
        </div>
        <form className="card-ref-form" onSubmit={openClientSafeCard}>
          <label htmlFor="client-access-token">Client access token</label>
          <textarea
            id="client-access-token"
            name="client-access-token"
            rows={3}
                placeholder="Access token from /api/auth/login"
            value={accessToken}
            onChange={(event) => {
              const nextToken = event.target.value;
              setAccessToken(nextToken);
              saveAccessToken(nextToken, "client");
            }}
          />
          <label htmlFor="anonymous-card-ref">Anonymous card ref</label>
          <div className="card-ref-row">
            <input
              id="anonymous-card-ref"
              name="anonymous-card-ref"
              autoComplete="off"
              placeholder="card_..."
              value={cardRef}
              onChange={(event) => setCardRef(event.target.value)}
            />
            <button type="submit" disabled={cardRef.trim().length === 0}>
              Open
            </button>
          </div>
        </form>
      </section>
    </PortalLayout>
  );
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

    let isCurrentRequest = true;
    const abortController = new AbortController();
    setIsLoading(true);
    void fetchClientSafeCandidateCard(decodedCardRef, abortController.signal)
      .then((nextResult) => {
        if (isCurrentRequest) {
          setResult(nextResult);
        }
      })
      .finally(() => {
        if (isCurrentRequest) {
          setIsLoading(false);
        }
      });

    return () => {
      isCurrentRequest = false;
      abortController.abort();
    };
  }, [decodedCardRef]);

  return (
    <PortalLayout portal={findPortal("client")}>
      <section className="candidate-card-surface" aria-labelledby="candidate-card-title">
        <div className="candidate-card-header">
          <div>
            <span className="portal-eyebrow">Read-only client-safe boundary</span>
            <h2 id="candidate-card-title">Anonymous Candidate Card</h2>
          </div>
          <NavLink to="/client" className="secondary-link">
            Back to Client
          </NavLink>
        </div>

        {isLoading ? (
          <SafeState title="Checking availability" tone="neutral" />
        ) : result.status === "ready" ? (
          <ClientSafeCard card={result.card} />
        ) : (
          <SafeStateForResult status={result.status} />
        )}
      </section>
    </PortalLayout>
  );
}

function SafeStateForResult({
  status,
}: {
  status: Exclude<ClientSafeCandidateCardResult["status"], "ready">;
}) {
  if (status === "invalid_ref") {
    return <SafeState title="Invalid anonymous card reference" tone="warning" />;
  }

  if (status === "denied") {
    return <SafeState title="Access denied for this client-safe view" tone="warning" />;
  }

  if (status === "unauthenticated") {
    return <SafeState title="Client session required before loading this card" tone="warning" />;
  }

  if (status === "failed") {
    return <SafeState title="Candidate card could not be loaded" tone="warning" />;
  }

  return <SafeState title="Client-safe candidate card unavailable" tone="neutral" />;
}

function SafeState({ title, tone }: { title: string; tone: "neutral" | "warning" }) {
  return (
    <div className={`safe-state safe-state-${tone}`} role="status">
      <h3>{title}</h3>
      <p>No candidate identity, raw profile, raw source, or internal details are shown.</p>
    </div>
  );
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
      <SafeList title="Match Narrative" items={card.safeMatchNarratives} />
    </article>
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

export default function App() {
  const location = useLocation();
  const consultantRouteActive = location.pathname.startsWith("/consultant");

  return (
    <div className={`app-shell${consultantRouteActive ? " app-shell-consultant" : ""}`}>
      {consultantRouteActive ? null : (
        <header className="app-header">
          <NavLink to="/consultant" className="brand">
            Recruiting Transaction OS
          </NavLink>
          <nav className="portal-nav" aria-label="Portals">
            {portalRoutes.map((route) => (
              <NavLink key={route.path} to={route.path}>
                {route.label}
              </NavLink>
            ))}
          </nav>
        </header>
      )}
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/consultant" replace />} />
          <Route path="/owner/*" element={<StaticPortal portalKey="owner" />} />
          <Route path="/consultant/*" element={<ConsultantPortal />} />
          <Route path="/client" element={<ClientPortal />} />
          <Route
            path="/client/candidate-cards/:anonymousCardRef"
            element={<ClientSafeCandidateCardPage />}
          />
          <Route path="/candidate/*" element={<StaticPortal portalKey="candidate" />} />
          <Route path="/admin/*" element={<StaticPortal portalKey="admin" />} />
          <Route path="*" element={<Navigate to="/consultant" replace />} />
        </Routes>
      </main>
    </div>
  );
}
