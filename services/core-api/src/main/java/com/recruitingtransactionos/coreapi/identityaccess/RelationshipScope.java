package com.recruitingtransactionos.coreapi.identityaccess;

public enum RelationshipScope {
  SELF("self"),
  SAME_ORGANIZATION("same_organization"),
  ASSIGNED_CONSULTANT("assigned_consultant"),
  OWNED_ACCOUNT("owned_account"),
  GOVERNANCE("governance");

  private final String wireValue;

  RelationshipScope(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
