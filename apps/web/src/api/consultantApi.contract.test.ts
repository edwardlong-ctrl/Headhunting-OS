import { createConsultantMatchGenerationPayload } from "./consultantMatching";
import { createConsultantJobUpdatePayload } from "./consultantJobs";
import { createConsultantShortlistUpdatePayload } from "./consultantShortlists";
import { SHORTLIST_BUILDER_INITIAL_STATUS } from "../features/consultant-portal/consultantPortalUtils";

describe("consultant API contract helpers", () => {
  it("builds a matching payload with backend-owned truth removed from the client request", () => {
    const payload = createConsultantMatchGenerationPayload({
      candidateId: "candidate-123",
    });

    expect(payload.candidateId).toBe("candidate-123");
    expect(payload.shortlistCandidateCardId).toBeUndefined();
    expect(Object.keys(payload)).toEqual(["candidateId", "shortlistCandidateCardId"]);
  });

  it("builds a job update payload that preserves required company and version fields", () => {
    const payload = createConsultantJobUpdatePayload(
      { companyId: "company-1", version: 7 },
      {
        title: "Principal Engineer",
        description: "Owns platform delivery",
        location: "Shanghai",
        seniorityBand: "principal",
        roleFamily: "engineering",
        employmentType: "full_time",
        compensation: "competitive",
        status: "open",
      },
    );

    expect(payload).toMatchObject({
      companyId: "company-1",
      version: 7,
      title: "Principal Engineer",
      status: "open",
    });
  });

  it("builds a shortlist update payload that preserves required job and version fields", () => {
    const payload = createConsultantShortlistUpdatePayload(
      { jobId: "job-1", version: 4 },
      {
        title: "April shortlist",
        status: "ready_for_review",
      },
    );

    expect(payload).toEqual({
      jobId: "job-1",
      version: 4,
      title: "April shortlist",
      status: "ready_for_review",
    });
  });

  it("keeps shortlist builder creation inside the draft state", () => {
    expect(SHORTLIST_BUILDER_INITIAL_STATUS).toBe("draft");
    expect(SHORTLIST_BUILDER_INITIAL_STATUS).not.toBe("ready_for_review");
  });
});
