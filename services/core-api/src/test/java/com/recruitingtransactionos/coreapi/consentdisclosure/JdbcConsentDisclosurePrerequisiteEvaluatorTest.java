package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JdbcConsentDisclosurePrerequisiteEvaluatorTest {

  @Test
  void commercialTermsGateRequiresExplicitActiveSignal() {
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive(null)).isFalse();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive("")).isFalse();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive("{}")).isFalse();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive(
        "{\"feeAgreementActive\":false}")).isFalse();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive(
        "{\"status\":\"draft\"}")).isFalse();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive(
        "{\"status\":\"active\"}")).isTrue();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive(
        "{\"fee_agreement_active\":true}")).isTrue();
  }
}
