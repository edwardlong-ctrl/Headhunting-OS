package com.recruitingtransactionos.coreapi.clientsafeprojection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompanyNameGeneralizationPolicyTest {

  @Test
  void generalizesCuratedSemiconductorFoundryNames() {
    assertThat(CompanyNameGeneralizationPolicy.generalize("TSMC"))
        .isNotNull()
        .satisfies(generalization -> assertThat(generalization.generalizedLabel())
            .isEqualTo("Top semiconductor foundry"));
    assertThat(CompanyNameGeneralizationPolicy.generalize("Taiwan Semiconductor Manufacturing Company"))
        .isNotNull()
        .satisfies(generalization -> assertThat(generalization.category())
            .isEqualTo(CompanyNameGeneralizationPolicy.CompanyGeneralizationCategory.SEMICONDUCTOR_FOUNDRY));
    assertThat(CompanyNameGeneralizationPolicy.generalize("smic")).isNotNull();
    assertThat(CompanyNameGeneralizationPolicy.generalize("Samsung Foundry")).isNotNull();
  }

  @Test
  void generalizesCuratedAiAcceleratorNames() {
    assertThat(CompanyNameGeneralizationPolicy.generalize("NVIDIA"))
        .isNotNull()
        .satisfies(generalization -> {
          assertThat(generalization.generalizedLabel())
              .isEqualTo("Top GPU and AI accelerator company");
          assertThat(generalization.category())
              .isEqualTo(CompanyNameGeneralizationPolicy.CompanyGeneralizationCategory.GPU_COMPANY);
        });
    assertThat(CompanyNameGeneralizationPolicy.generalize("Cerebras"))
        .satisfies(generalization -> assertThat(generalization.category())
            .isEqualTo(CompanyNameGeneralizationPolicy.CompanyGeneralizationCategory.AI_CHIP_COMPANY));
  }

  @Test
  void caseInsensitiveAndWhitespaceTolerant() {
    assertThat(CompanyNameGeneralizationPolicy.generalize("  apple  ")).isNotNull();
    assertThat(CompanyNameGeneralizationPolicy.generalize("APPLE")).isNotNull();
    assertThat(CompanyNameGeneralizationPolicy.generalize("Apple Inc.")).isNotNull();
  }

  @Test
  void returnsNullForUncuratedNames() {
    assertThat(CompanyNameGeneralizationPolicy.generalize("Some Random Mid-Cap Software Inc.")).isNull();
    assertThat(CompanyNameGeneralizationPolicy.generalize(null)).isNull();
    assertThat(CompanyNameGeneralizationPolicy.generalize("")).isNull();
    assertThat(CompanyNameGeneralizationPolicy.generalize("   ")).isNull();
  }

  @Test
  void isKnownTopTierCompanyMatchesGeneralizeBehavior() {
    assertThat(CompanyNameGeneralizationPolicy.isKnownTopTierCompany("TSMC")).isTrue();
    assertThat(CompanyNameGeneralizationPolicy.isKnownTopTierCompany("Some Random Mid-Cap Software Inc.")).isFalse();
  }
}
