import {
  Navigate,
  NavLink,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import { ClientPortal } from "./features/client-portal/ClientPortal";
import { CandidatePortal } from "./features/candidate-portal/CandidatePortal";
import { ConsultantPortal } from "./features/consultant-portal/ConsultantPortal";
import { OwnerPortal } from "./features/owner-portal/OwnerPortal";

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
    governanceModules: [
      "Client-safe candidate card",
      "Access token from /api/auth/login",
      "Disclosure request pending future gate",
    ],
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

export default function App() {
  const location = useLocation();
  const fullPortalRouteActive = location.pathname.startsWith("/owner")
    || location.pathname.startsWith("/consultant")
    || location.pathname.startsWith("/client");

  return (
    <div className={`app-shell${fullPortalRouteActive ? " app-shell-consultant" : ""}`}>
      {fullPortalRouteActive ? null : (
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
          <Route path="/owner/*" element={<OwnerPortal />} />
          <Route path="/consultant/*" element={<ConsultantPortal />} />
          <Route path="/client/*" element={<ClientPortal />} />
          <Route path="/candidate/*" element={<CandidatePortal />} />
          <Route path="/admin/*" element={<StaticPortal portalKey="admin" />} />
          <Route path="*" element={<Navigate to="/consultant" replace />} />
        </Routes>
      </main>
    </div>
  );
}
