package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.job.JobStatus;
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
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive(
        "{\"contractStatus\":\"consultant_confirmed\",\"approval\":\"consultant_confirmed\"}")).isTrue();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isCommercialTermsActive(
        "{\"contractStatus\": \"consultant_confirmed\", \"approval\": \"consultant_confirmed\"}")).isTrue();
  }

  @Test
  void jobActivationGateAllowsPostActivationTransactionStates() {
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isJobActivatedForDisclosure(
        JobStatus.ACTIVATED)).isTrue();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isJobActivatedForDisclosure(
        JobStatus.SHORTLIST_IN_PROGRESS)).isTrue();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isJobActivatedForDisclosure(
        JobStatus.SHORTLIST_SENT)).isTrue();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isJobActivatedForDisclosure(
        JobStatus.INTERVIEWING)).isTrue();
    assertThat(JdbcConsentDisclosurePrerequisiteEvaluator.isJobActivatedForDisclosure(
        JobStatus.CONTRACT_PENDING)).isFalse();
  }
}
