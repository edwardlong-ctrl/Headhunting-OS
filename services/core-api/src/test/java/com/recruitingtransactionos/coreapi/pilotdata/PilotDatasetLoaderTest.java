package com.recruitingtransactionos.coreapi.pilotdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PilotDatasetLoaderTest {

  @Test
  void defaultSemiconductorPilotDatasetHasRequiredDeterministicShape() {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();

    assertThat(dataset.version()).isEqualTo("semiconductor-pilot-v1");
    assertThat(dataset.organization().organizationId().toString())
        .isEqualTo("00000000-0000-0000-0000-000000380001");
    assertThat(dataset.candidates()).hasSize(75);
    assertThat(dataset.jobs().stream().filter(job -> "activated".equals(job.status()))).hasSize(5);
    assertThat(dataset.jobs().stream().filter(job -> "intake_review".equals(job.status()))).hasSize(3);
    assertThat(dataset.sourceDocuments()).hasSizeGreaterThanOrEqualTo(83);
    assertThat(dataset.accounts()).extracting(PilotDataset.AccountSeed::role)
        .contains("owner", "consultant", "client", "candidate", "admin");

    Set<String> candidateIds = dataset.candidates().stream()
        .map(PilotDataset.CandidateSeed::candidateId)
        .collect(Collectors.toSet());
    assertThat(candidateIds).hasSize(75);
    assertThat(dataset.candidates()).allSatisfy(candidate -> {
      assertThat(candidate.syntheticName()).startsWith("Pilot Talent ");
      assertThat(candidate.email()).endsWith("@candidate.example.test");
      assertThat(candidate.metadata()).contains("\"synthetic\":true");
      assertThat(candidate.sourceDocumentRef()).startsWith("candidate-resume-");
    });
  }

  @Test
  void defaultDatasetUsesOnlyReservedSyntheticEmailDomains() {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();

    assertThat(dataset.accounts()).allSatisfy(account ->
        assertThat(account.email()).endsWith(".example.test"));
    assertThat(dataset.companies()).allSatisfy(company ->
        assertThat(company.name()).startsWith("Pilot "));
  }
}
