package com.recruitingtransactionos.coreapi.clientsafeprojection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectChipNameRedactionPolicyTest {

  @Test
  void redactsDeclaredExactCodeName() {
    ProjectChipNameRedactionPolicy.Redaction redaction =
        ProjectChipNameRedactionPolicy.redact(
            "Led Orion-X7 NPU verification at NebulaChip Systems.",
            List.of("Orion-X7 NPU"));

    assertThat(redaction.redactedExactNames()).contains("Orion-X7 NPU");
    assertThat(redaction.redactedText())
        .contains(ProjectChipNameRedactionPolicy.REDACTED_CHIP_CODE_NAME_TOKEN)
        .doesNotContain("Orion-X7 NPU");
    assertThat(redaction.anyRedaction()).isTrue();
  }

  @Test
  void redactsKnownAppleAndNvidiaFamilies() {
    ProjectChipNameRedactionPolicy.Redaction redaction =
        ProjectChipNameRedactionPolicy.redact(
            "Worked on the M3 Max design and tape-out as well as the H100 datacenter chip.",
            List.of());

    assertThat(redaction.knownChipFamilyMatched()).isTrue();
    assertThat(redaction.redactedText())
        .doesNotContain("M3 Max")
        .doesNotContain("H100")
        .contains(ProjectChipNameRedactionPolicy.REDACTED_CHIP_CODE_NAME_TOKEN);
  }

  @Test
  void redactsGenericChipShapeWhenChipContextDetected() {
    ProjectChipNameRedactionPolicy.Redaction redaction =
        ProjectChipNameRedactionPolicy.redact(
            "Owned NPU verification of Orion-X7 across the whole tape-out cycle.",
            List.of());

    assertThat(redaction.genericChipShapeMatched()).isTrue();
    assertThat(redaction.redactedText()).doesNotContain("Orion-X7");
  }

  @Test
  void leavesGenericShapeAloneOutsideChipContext() {
    ProjectChipNameRedactionPolicy.Redaction redaction =
        ProjectChipNameRedactionPolicy.redact(
            "We named our pet Orion-X7 because we like astronomy.",
            List.of());

    assertThat(redaction.genericChipShapeMatched()).isFalse();
    assertThat(redaction.redactedText()).contains("Orion-X7");
  }

  @Test
  void mentionsChipContextDetectsCommonVocabulary() {
    assertThat(ProjectChipNameRedactionPolicy.mentionsChipContext(
        "Worked on RTL synthesis and physical design")).isTrue();
    assertThat(ProjectChipNameRedactionPolicy.mentionsChipContext(
        "Wrote SystemVerilog testbenches and UVM agents")).isTrue();
    assertThat(ProjectChipNameRedactionPolicy.mentionsChipContext(
        "Tape-out lead for the program")).isTrue();
    assertThat(ProjectChipNameRedactionPolicy.mentionsChipContext(
        "Built a CRUD web app")).isFalse();
    assertThat(ProjectChipNameRedactionPolicy.mentionsChipContext(null)).isFalse();
    assertThat(ProjectChipNameRedactionPolicy.mentionsChipContext("")).isFalse();
  }

  @Test
  void blankOrNullInputReturnsEmptyRedaction() {
    ProjectChipNameRedactionPolicy.Redaction redaction =
        ProjectChipNameRedactionPolicy.redact(null, List.of());

    assertThat(redaction.anyRedaction()).isFalse();
    assertThat(redaction.redactedText()).isEmpty();
  }
}
