import {
  canClientRequestUnlock,
  canClientSelectCandidate,
  canClientSubmitInterviewFeedback,
  deriveIdentityAccessStatus,
  shouldWarnApprovedWithoutDisclosure,
} from "./clientPortalShortlistUtils";

describe("client portal shortlist utils", () => {
  it("keeps shortlist visible states narrower than mutable selection states after unlock or closure", () => {
    expect(canClientSelectCandidate("sent_to_client")).toBe(true);
    expect(canClientSelectCandidate("candidate_selected")).toBe(true);
    expect(canClientSelectCandidate("contact_unlocked")).toBe(false);
    expect(canClientSelectCandidate("interviewing")).toBe(false);
    expect(canClientSelectCandidate("closed")).toBe(false);
  });

  it("blocks new unlock requests once the shortlist has moved into post-selection stages", () => {
    expect(canClientRequestUnlock("candidate_selected", "selected")).toBe(true);
    expect(canClientRequestUnlock("candidate_selected", "unlocked")).toBe(true);
    expect(canClientRequestUnlock("contact_unlocked", "unlocked")).toBe(false);
    expect(canClientRequestUnlock("interviewing", "unlocked")).toBe(false);
    expect(canClientRequestUnlock("closed", "selected")).toBe(false);
  });

  it("allows interview feedback only after identity unlock and interview-stage progression", () => {
    expect(canClientSubmitInterviewFeedback("contact_unlocked", "unlocked")).toBe(true);
    expect(canClientSubmitInterviewFeedback("interviewing", "unlocked")).toBe(true);
    expect(canClientSubmitInterviewFeedback("candidate_selected", "unlocked")).toBe(false);
    expect(canClientSubmitInterviewFeedback("contact_unlocked", "selected")).toBe(false);
  });

  it("distinguishes approved unlock requests from actual identity disclosure", () => {
    expect(deriveIdentityAccessStatus("candidate_selected", null)).toBe("not_unlocked");
    expect(deriveIdentityAccessStatus("contact_unlocked", null)).toBe("contact_unlocked");
    expect(deriveIdentityAccessStatus("contact_unlocked", "disclosure_record_001")).toBe("identity_disclosed");
    expect(shouldWarnApprovedWithoutDisclosure("approved", null)).toBe(true);
    expect(shouldWarnApprovedWithoutDisclosure("approved", "disclosure_record_001")).toBe(false);
    expect(shouldWarnApprovedWithoutDisclosure("under_review", null)).toBe(false);
  });
});
