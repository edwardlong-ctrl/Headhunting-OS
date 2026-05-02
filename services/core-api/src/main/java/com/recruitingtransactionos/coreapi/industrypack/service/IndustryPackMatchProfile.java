package com.recruitingtransactionos.coreapi.industrypack.service;

import java.util.List;

public record IndustryPackMatchProfile(
    List<String> antiPatternWarnings,
    boolean antiPatternTriggered,
    boolean requiredSkillEvidencePresent,
    boolean roleFamilyTemplateApplied) {

  public IndustryPackMatchProfile {
    antiPatternWarnings = List.copyOf(antiPatternWarnings == null ? List.of() : antiPatternWarnings);
  }
}
