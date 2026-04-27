import { Navigate, NavLink, Route, Routes } from "react-router-dom";

const portalRoutes = [
  { path: "/owner", label: "Owner" },
  { path: "/consultant", label: "Consultant" },
  { path: "/client", label: "Client" },
  { path: "/candidate", label: "Candidate" },
  { path: "/admin", label: "Admin" },
] as const;

function PortalShell({ label }: { label: string }) {
  return (
    <section className="portal-shell" aria-labelledby="portal-title">
      <h1 id="portal-title">{label}</h1>
    </section>
  );
}

export default function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="brand">Recruiting Transaction OS</span>
        <nav className="portal-nav" aria-label="Portals">
          {portalRoutes.map((route) => (
            <NavLink key={route.path} to={route.path}>
              {route.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/consultant" replace />} />
          {portalRoutes.map((route) => (
            <Route
              key={route.path}
              path={route.path}
              element={<PortalShell label={route.label} />}
            />
          ))}
          <Route path="*" element={<Navigate to="/consultant" replace />} />
        </Routes>
      </main>
    </div>
  );
}
