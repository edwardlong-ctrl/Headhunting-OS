package com.recruitingtransactionos.coreapi.identityaccess;

public enum ResourceType {
  UNKNOWN("unknown"),
  CANDIDATE("candidate"),
  CANDIDATE_PROFILE("candidate_profile"),
  CLIENT_SAFE_CANDIDATE_CARD("client_safe_candidate_card"),
  SOURCE_ITEM("source_item"),
  INFORMATION_PACKET("information_packet"),
  CLAIM_LEDGER_ITEM("claim_ledger_item"),
  REVIEW_EVENT("review_event"),
  WORKFLOW_EVENT("workflow_event"),
  CONSENT_RECORD("consent_record"),
  DISCLOSURE_RECORD("disclosure_record"),
  JOB("job"),
  COMPANY("company"),
  MATCH_REPORT("match_report"),
  SHORTLIST("shortlist"),
  ADMIN_GOVERNANCE("admin_governance");

  private final String wireValue;

  ResourceType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
