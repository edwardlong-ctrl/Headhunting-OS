package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.companyintake;

public record CompanyIntakeInput(
    String sourceSummary,
    String companyOverviewText,
    String hiringContextText,
    String consultantNotes) {

  public CompanyIntakeInput {
    requireAtLeastOneValue(sourceSummary, companyOverviewText, hiringContextText, consultantNotes);
  }

  private static void requireAtLeastOneValue(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return;
      }
    }
    throw new IllegalArgumentException("company_intake_input_requires_content");
  }
}
