package com.recruitingtransactionos.coreapi.clientsafeprojection;

public enum ForbiddenClientCandidateFieldCategory {
  DIRECT_IDENTITY("direct_identity"),
  CONTACT("contact"),
  PERSONAL_MESSAGING("personal_messaging"),
  EXACT_LOCATION("exact_location"),
  IDENTITY_REVEALING_URL("identity_revealing_url"),
  RAW_DOCUMENT("raw_document"),
  RAW_BACKEND_IDENTIFIER("raw_backend_identifier"),
  RAW_SOURCE_REFERENCE("raw_source_reference"),
  RAW_SOURCE_CONTENT("raw_source_content"),
  CONSULTANT_INTERNAL("consultant_internal"),
  OTHER_CLIENT_INTERACTION("other_client_interaction"),
  COMPENSATION_AND_NEGOTIATION("compensation_and_negotiation"),
  INTERNAL_AUDIT_RECORD("internal_audit_record"),
  EMPLOYER_OR_PROJECT_IDENTIFIER("employer_or_project_identifier"),
  PUBLIC_IDENTIFIER_BEFORE_CONSENT("public_identifier_before_consent"),
  PRECISE_RARE_COMBINATION("precise_rare_combination");

  private final String wireValue;

  ForbiddenClientCandidateFieldCategory(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
