package com.recruitingtransactionos.coreapi.governedintake;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record IntakeExtractionRun(
    IntakeExtractionRunId extractionRunId,
    UUID organizationId,
    InformationPacketId informationPacketId,
    IntakeExtractionMode mode,
    IntakeExtractionStatus status,
    String inputSchemaVersion,
    String outputSchemaVersion,
    String extractorVersion,
    String sourceSnapshotHash,
    Instant createdAt,
    Optional<Instant> completedAt,
    Optional<String> failureReason,
    Optional<IntakeExtractionOutputEnvelope> outputEnvelope) {

  public IntakeExtractionRun {
    Objects.requireNonNull(extractionRunId, "extractionRunId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Objects.requireNonNull(mode, "mode must not be null");
    Objects.requireNonNull(status, "status must not be null");
    inputSchemaVersion = GovernedIntakeGuards.requireNonBlank(
        inputSchemaVersion,
        "inputSchemaVersion");
    outputSchemaVersion = GovernedIntakeGuards.requireNonBlank(
        outputSchemaVersion,
        "outputSchemaVersion");
    extractorVersion = GovernedIntakeGuards.requireNonBlank(
        extractorVersion,
        "extractorVersion");
    sourceSnapshotHash = GovernedIntakeGuards.requireNonBlank(
        sourceSnapshotHash,
        "sourceSnapshotHash");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    completedAt = completedAt == null ? Optional.empty() : completedAt;
    failureReason = failureReason == null
        ? Optional.empty()
        : failureReason.map(reason -> GovernedIntakeGuards.requireNonBlank(
            reason,
            "failureReason"));
    outputEnvelope = outputEnvelope == null ? Optional.empty() : outputEnvelope;
    if (status == IntakeExtractionStatus.SUCCEEDED && outputEnvelope.isEmpty()) {
      throw new IllegalArgumentException("succeeded extraction run requires outputEnvelope");
    }
    if (status == IntakeExtractionStatus.FAILED && failureReason.isEmpty()) {
      throw new IllegalArgumentException("failed extraction run requires failureReason");
    }
  }
}
