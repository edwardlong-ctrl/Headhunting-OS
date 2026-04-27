package com.recruitingtransactionos.coreapi.governedintake;

public enum SourceItemType {
  CV("CV"),
  LINKEDIN_TEXT("LINKEDIN_TEXT"),
  WECHAT_NOTE("WECHAT_NOTE"),
  CALL_NOTE("CALL_NOTE"),
  EMAIL("EMAIL"),
  INTERVIEW_FEEDBACK("INTERVIEW_FEEDBACK"),
  JD("JD"),
  COMPANY_MATERIAL("COMPANY_MATERIAL"),
  OLD_SYSTEM_EXPORT("OLD_SYSTEM_EXPORT"),
  OTHER("OTHER");

  private final String wireValue;

  SourceItemType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static SourceItemType fromWireValue(String wireValue) {
    for (SourceItemType value : values()) {
      if (value.wireValue.equals(wireValue)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown source item type: " + wireValue);
  }
}
