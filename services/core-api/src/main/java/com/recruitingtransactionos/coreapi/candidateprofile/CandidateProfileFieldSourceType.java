package com.recruitingtransactionos.coreapi.candidateprofile;

public enum CandidateProfileFieldSourceType {
  CLAIM_LEDGER_ITEM("claim_ledger_item"),
  REVIEW_EVENT("review_event"),
  SOURCE_ITEM("source_item"),
  INFORMATION_PACKET("information_packet"),
  INTAKE_EXTRACTION_RUN("intake_extraction_run"),
  WORKFLOW_EVENT("workflow_event"),
  SOURCE_SPAN("source_span"),
  EXTERNAL_EVIDENCE("external_evidence");

  private final String wireValue;

  CandidateProfileFieldSourceType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
