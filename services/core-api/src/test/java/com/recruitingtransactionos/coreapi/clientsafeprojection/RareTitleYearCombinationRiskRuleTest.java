package com.recruitingtransactionos.coreapi.clientsafeprojection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RareTitleYearCombinationRiskRuleTest {

  @Test
  void detectsChiefArchitectWithExactYear() {
    RareTitleYearCombinationRiskRule.Detection detection =
        RareTitleYearCombinationRiskRule.detect(
            "Chief Verification Architect at TSMC in 2024 driving 3nm tape-out.");

    assertThat(detection.matched()).isTrue();
    assertThat(detection.matchedTitleSpan()).contains("Chief Verification Architect");
    assertThat(detection.matchedYear()).isEqualTo("2024");
  }

  @Test
  void detectsHeadOfTitleWithYear() {
    RareTitleYearCombinationRiskRule.Detection detection =
        RareTitleYearCombinationRiskRule.detect(
            "Head of Verification at NVIDIA from 2019 onward, leading the H100 program.");

    assertThat(detection.matched()).isTrue();
    assertThat(detection.matchedTitleSpan()).contains("Head of Verification");
    assertThat(detection.matchedYear()).isEqualTo("2019");
  }

  @Test
  void detectsFounderTitleWithYear() {
    RareTitleYearCombinationRiskRule.Detection detection =
        RareTitleYearCombinationRiskRule.detect(
            "Founding Engineer of a stealth startup since 2022.");

    assertThat(detection.matched()).isTrue();
    assertThat(detection.matchedTitleSpan()).contains("Founding Engineer");
    assertThat(detection.matchedYear()).isEqualTo("2022");
  }

  @Test
  void doesNotMatchSeniorEngineerWithYear() {
    RareTitleYearCombinationRiskRule.Detection detection =
        RareTitleYearCombinationRiskRule.detect(
            "Senior software engineer since 2018 working on web platforms.");

    assertThat(detection.matched()).isFalse();
  }

  @Test
  void doesNotMatchRareTitleWithoutYear() {
    RareTitleYearCombinationRiskRule.Detection detection =
        RareTitleYearCombinationRiskRule.detect(
            "Chief Verification Architect at a top semiconductor foundry.");

    assertThat(detection.matched()).isFalse();
  }

  @Test
  void doesNotMatchYearWithoutRareTitle() {
    RareTitleYearCombinationRiskRule.Detection detection =
        RareTitleYearCombinationRiskRule.detect(
            "Worked at a top semiconductor foundry from 2018 to 2024.");

    assertThat(detection.matched()).isFalse();
  }

  @Test
  void detectsAcrossListOfTexts() {
    RareTitleYearCombinationRiskRule.Detection detection =
        RareTitleYearCombinationRiskRule.detect(List.of(
            "Solid functional verification engineer.",
            "Distinguished Engineer at a top GPU and AI accelerator company since 2020."));

    assertThat(detection.matched()).isTrue();
    assertThat(detection.matchedYear()).isEqualTo("2020");
  }

  @Test
  void blankAndNullInputDoNotMatch() {
    assertThat(RareTitleYearCombinationRiskRule.detect((String) null).matched()).isFalse();
    assertThat(RareTitleYearCombinationRiskRule.detect("").matched()).isFalse();
    assertThat(RareTitleYearCombinationRiskRule.detect("   ").matched()).isFalse();
    assertThat(RareTitleYearCombinationRiskRule.detect((List<String>) null).matched()).isFalse();
    assertThat(RareTitleYearCombinationRiskRule.detect(List.of()).matched()).isFalse();
  }
}
