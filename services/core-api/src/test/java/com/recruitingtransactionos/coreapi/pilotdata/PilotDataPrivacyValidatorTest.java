package com.recruitingtransactionos.coreapi.pilotdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PilotDataPrivacyValidatorTest {

  @Test
  void defaultDatasetPassesSyntheticPrivacyValidation() {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();

    PilotDataValidationResult result = new PilotDataPrivacyValidator().validate(dataset);

    assertThat(result.valid()).isTrue();
    assertThat(result.issues()).isEmpty();
  }

  @Test
  void validatorRejectsPublicProfileUrlsRealDomainsAndRealCompanyNames() {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    List<PilotDataset.CandidateSeed> candidates = new ArrayList<>(dataset.candidates());
    PilotDataset.CandidateSeed first = candidates.get(0);
    candidates.set(0, new PilotDataset.CandidateSeed(
        first.candidateId(),
        first.profileId(),
        first.syntheticName(),
        "real.person@gmail.com",
        first.roleFamily(),
        first.seniorityBand(),
        first.locationRegion(),
        first.status(),
        first.skills(),
        "Worked at Intel and see https://linkedin.com/in/real-person",
        first.sourceDocumentRef(),
        first.metadata()));

    PilotDataset unsafe = dataset.withCandidates(candidates);
    PilotDataValidationResult result = new PilotDataPrivacyValidator().validate(unsafe);

    assertThat(result.valid()).isFalse();
    assertThat(result.issues()).anySatisfy(issue ->
        assertThat(issue.code()).isEqualTo("unsupported_email_domain"));
    assertThat(result.issues()).anySatisfy(issue ->
        assertThat(issue.code()).isEqualTo("public_profile_url_forbidden"));
    assertThat(result.issues()).anySatisfy(issue ->
        assertThat(issue.code()).isEqualTo("real_company_name_forbidden"));
  }
}
