package com.recruitingtransactionos.coreapi.pilotdata;

import java.util.Locale;

public enum PilotDataCommand {
  REBUILD,
  RESET,
  IMPORT,
  VALIDATE,
  EXPORT;

  public static PilotDataCommand parse(String rawCommand) {
    String normalized = PilotDataset.requireNonBlank(rawCommand, "rawCommand")
        .replace('-', '_')
        .toUpperCase(Locale.ROOT);
    try {
      return PilotDataCommand.valueOf(normalized);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unsupported pilot data command: " + rawCommand, exception);
    }
  }
}
