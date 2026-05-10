package com.recruitingtransactionos.coreapi.reportingexport;

public enum ReportingExportType {
  OWNER_REPORT("owner_report"),
  CONSULTANT_ACTIVITY("consultant_activity"),
  CLIENT_SHORTLIST_FEEDBACK("client_shortlist_feedback"),
  CANDIDATE_PERSONAL_DATA("candidate_personal_data"),
  DISCLOSURE_AUDIT("disclosure_audit"),
  PLACEMENT_COMMISSION("placement_commission"),
  RETENTION_DELETE_EVIDENCE("retention_delete_evidence");

  private final String wireValue;

  ReportingExportType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
