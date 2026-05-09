import adminPortalSource from "./features/admin-portal/AdminPortal.tsx?raw";
import candidatePortalSource from "./features/candidate-portal/CandidatePortal.tsx?raw";
import clientPortalSource from "./features/client-portal/ClientPortal.tsx?raw";
import consultantPortalSource from "./features/consultant-portal/ConsultantPortal.tsx?raw";
import ownerPortalSource from "./features/owner-portal/OwnerPortal.tsx?raw";

describe("Task 43 portal route contract", () => {
  it("keeps every v2.1/v2.0 owner route named in the spec reachable from the owner portal", () => {
    for (const routePath of [
      "dashboard",
      "pipeline",
      "consultants",
      "clients",
      "revenue",
      "placements",
      "commission",
      "risk",
      "data-quality",
      "ai-quality",
      "audit",
    ]) {
      expect(ownerPortalSource).toContain(`path="${routePath}"`);
    }
  });

  it("keeps every v2.1/v2.0 consultant route named in the spec reachable from the unified consultant portal", () => {
    for (const routePath of [
      "dashboard",
      "intake",
      "intake/talent",
      "intake/review/:packetId",
      "talent",
      "talent/:candidateId",
      "companies",
      "companies/:companyId",
      "jobs",
      "jobs/:jobId/intake",
      "jobs/:jobId/matching",
      "jobs/:jobId/outreach",
      "jobs/:jobId/shortlist",
      "follow-ups",
      "workflow",
    ]) {
      expect(consultantPortalSource).toContain(`path="${routePath}"`);
    }
  });

  it("keeps every v2.1/v2.0 client route named in the spec reachable from the client portal", () => {
    expect(clientPortalSource).toContain('path="dashboard"');
    expect(clientPortalSource).toContain('path="jobs/new"');
    expect(clientPortalSource).toContain('path="jobs/new/ai-intake"');
    expect(clientPortalSource).toContain('path="jobs/:jobId"');
    expect(clientPortalSource).toContain('path="jobs/:jobId/clarification"');
    expect(clientPortalSource).toContain('path="jobs/:jobId/shortlist"');
    expect(clientPortalSource).toContain('path="candidates/:anonymousCandidateId"');
    expect(clientPortalSource).toContain('path="unlock/:candidateId"');
    expect(clientPortalSource).toContain('path="feedback/:interviewId"');
    expect(clientPortalSource).toContain('path="follow-ups"');
    expect(clientPortalSource).toContain('path="profile"');
  });

  it("keeps every v2.1/v2.0 candidate route named in the spec reachable from the candidate portal", () => {
    expect(candidatePortalSource).toContain('path="home"');
    expect(candidatePortalSource).toContain('path="upload"');
    expect(candidatePortalSource).toContain('path="profile/ai-review"');
    expect(candidatePortalSource).toContain('path="follow-up/:formId"');
    expect(candidatePortalSource).toContain('path="opportunities/:opportunityId"');
    expect(candidatePortalSource).toContain('path="consent/:requestId"');
    expect(candidatePortalSource).toContain('path="status"');
  });

  it("keeps every v2.1/v2.0 admin route named in the spec reachable from the admin portal", () => {
    for (const sectionKey of [
      "ai-policy",
      "ai-task-registry",
      "industry-packs",
      "schema",
      "workflow-rules",
      "permissions",
      "audit-log",
      "integrations",
      "security",
      "claim-ledger",
      "review-quality",
      "ontology-governance",
      "privacy-redaction",
      "model-routing",
      "eval-feedback",
    ]) {
      expect(adminPortalSource).toContain(`"${sectionKey}"`);
    }
  });
});
