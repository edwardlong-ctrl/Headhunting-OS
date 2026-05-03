import { createConsultantMatchGenerationPayload } from "./consultantMatching";
import { createConsultantJobUpdatePayload } from "./consultantJobs";
import type { ConsultantWorkflowEvent } from "./consultantWorkflow";
import { createConsultantShortlistUpdatePayload } from "./consultantShortlists";
import {
  SHORTLIST_BUILDER_INITIAL_STATUS,
  describeWorkflowTransition,
} from "../features/consultant-portal/consultantPortalUtils";

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

  it("renders shortlist card composition audit transitions from card status when shortlist status is unchanged", () => {
    const event: ConsultantWorkflowEvent = {
      workflowEventId: "event-1",
      entityType: "SHORTLIST",
      entityId: "shortlist-1",
      actionCode: "SHORTLIST_CARD_REMOVED",
      actorType: "consultant",
      aiInvolvement: "none",
      riskTier: "t2_medium_risk",
      beforeStatus: "draft",
      afterStatus: "draft",
      beforeCardStatus: "included",
      afterCardStatus: "removed",
      reason: "candidate card removed from shortlist builder",
      occurredAt: "2026-05-03T00:00:00Z",
    };

    expect(describeWorkflowTransition(event)).toBe("included -> removed");
  });
});
