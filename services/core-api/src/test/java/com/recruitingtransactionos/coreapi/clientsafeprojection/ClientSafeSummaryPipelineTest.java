package com.recruitingtransactionos.coreapi.clientsafeprojection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientSafeSummaryPipelineTest {

  @Test
  void redactsAcceptanceScenarioInputForChipCompanyAndYear() {
    InternalCandidateProjectionSnapshot snapshot = chipScenarioSnapshot();

    ClientSafeSummaryPipeline.Result result = ClientSafeSummaryPipeline.redact(snapshot);

    assertThat(result.unsafeFeaturesObserved())
        .contains(
            ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER,
            ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME,
            ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR);

    InternalCandidateProjectionSnapshot redacted = result.redactedSnapshot();
    assertThat(redacted.exactCurrentEmployer()).isNull();
    assertThat(redacted.exactProjectProductOrChipNames()).isEmpty();
    assertThat(redacted.safeSummary()).doesNotContain("TSMC");
    assertThat(redacted.safeSummary()).doesNotContain("Orion-X7");
    assertThat(redacted.safeSummary()).doesNotContain("2024");
    assertThat(redacted.safeSummary()).doesNotContain("Chief Verification Architect");
    assertThat(redacted.generalizedHeadline()).doesNotContain("TSMC");

    assertThat(result.redactionExplanations())
        .anySatisfy(explanation ->
            assertThat(explanation).contains("Top semiconductor foundry"));
  }

  @Test
  void leavesAlreadyCleanSnapshotUnchanged() {
    InternalCandidateProjectionSnapshot snapshot = cleanSnapshot();

    ClientSafeSummaryPipeline.Result result = ClientSafeSummaryPipeline.redact(snapshot);

    assertThat(result.unsafeFeaturesObserved()).isEmpty();
    assertThat(result.redactedSnapshot().safeSummary())
        .isEqualTo(snapshot.safeSummary());
  }

  @Test
  void observedFeaturesInDetectsKnownChipFamilyInBuiltCard() {
    ClientSafeCandidateCard card = new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of("card_pipeline_observed_001"),
        AnonymousCandidateRef.of("anon_candidate_pipeline_observed_001"),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Tape-out engineer with H100 program experience",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has worked on H100 datacenter GPU programs.",
        "SystemVerilog and UVM background.",
        List.of("Worked on the H100 platform."),
        List.of("Strong fit based on tape-out experience."));

    Set<ReidentificationRiskFeature> features =
        ClientSafeSummaryPipeline.observedFeaturesIn(card);

    assertThat(features)
        .contains(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME);
  }

  private static InternalCandidateProjectionSnapshot chipScenarioSnapshot() {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000007c0001",
        "00000000-0000-0000-0000-0000007c0002",
        "Jane Alpha Candidate",
        "jane.alpha@example.com",
        "+86 138 0000 7C7C",
        "https://www.linkedin.com/in/jane-alpha",
        "TSMC",
        List.of("Orion-X7 NPU"),
        "Jane Alpha Candidate.",
        "Internal notes.",
        AnonymousCandidateCardId.of("card_pipeline_chip_scenario_001"),
        AnonymousCandidateRef.of("anon_candidate_pipeline_chip_001"),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Chief Verification Architect at TSMC in 2024 leading Orion-X7 NPU verification",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Chief Verification Architect at TSMC in 2024 driving Orion-X7 NPU tape-out across the program.",
        "SystemVerilog, UVM, coverage closure.",
        List.of(
            "Owned Orion-X7 NPU verification at TSMC during 2024 tape-out cycle."),
        List.of(
            "Strong fit at TSMC verification leadership level."),
        ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths());
  }

  private static InternalCandidateProjectionSnapshot cleanSnapshot() {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000007c0011",
        "00000000-0000-0000-0000-0000007c0012",
        "Jane Beta Candidate",
        null,
        null,
        null,
        null,
        List.of(),
        null,
        null,
        AnonymousCandidateCardId.of("card_pipeline_clean_001"),
        AnonymousCandidateRef.of("anon_candidate_pipeline_clean_001"),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led complex verification programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence generalized from approved profile signals."),
        List.of("Strong fit based on generalized capability evidence."),
        ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths());
  }
}
