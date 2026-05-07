package com.recruitingtransactionos.coreapi.pilotdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PilotDataCommandTest {

  @Test
  void parsesSupportedCommands() {
    assertThat(PilotDataCommand.parse("rebuild")).isEqualTo(PilotDataCommand.REBUILD);
    assertThat(PilotDataCommand.parse("reset")).isEqualTo(PilotDataCommand.RESET);
    assertThat(PilotDataCommand.parse("import")).isEqualTo(PilotDataCommand.IMPORT);
    assertThat(PilotDataCommand.parse("validate")).isEqualTo(PilotDataCommand.VALIDATE);
    assertThat(PilotDataCommand.parse("export")).isEqualTo(PilotDataCommand.EXPORT);
  }

  @Test
  void rejectsUnknownCommands() {
    assertThatThrownBy(() -> PilotDataCommand.parse("seed-shortcut"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported pilot data command");
  }

  @Test
  void resetRequiresExplicitAllowFlag() {
    PilotDataCommandOptions resetWithoutFlag = new PilotDataCommandOptions(PilotDataCommand.RESET, false);
    PilotDataCommandOptions resetWithFlag = new PilotDataCommandOptions(PilotDataCommand.RESET, true);

    assertThatThrownBy(resetWithoutFlag::requireResetAllowed)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("pilot_data_reset_requires_RTO_PILOT_DATA_ALLOW_RESET_true");
    resetWithFlag.requireResetAllowed();
  }
}
