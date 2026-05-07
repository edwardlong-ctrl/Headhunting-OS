package com.recruitingtransactionos.coreapi.pilotdata;

public record PilotDataCommandOptions(PilotDataCommand command, boolean resetAllowed) {

  public void requireResetAllowed() {
    if ((command == PilotDataCommand.RESET || command == PilotDataCommand.REBUILD)
        && !resetAllowed) {
      throw new IllegalStateException("pilot_data_reset_requires_RTO_PILOT_DATA_ALLOW_RESET_true");
    }
  }
}
