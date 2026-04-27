package com.recruitingtransactionos.coreapi.governedintake;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record IntakeExtractionOutputEnvelope(
    IntakeExtractionRunId extractionRunId,
    UUID organizationId,
    InformationPacketId informationPacketId,
    InformationPacketType packetType,
    IntendedEntityType intendedEntityType,
    String outputSchemaVersion,
    List<SourceItemId> sourceItemIds,
    List<IntakeExtractionSourceSnapshot> sourceSnapshots,
    List<IntakeExtractedField> extractedFields,
    List<IntakeExtractionFinding> findings,
    List<IntakeExtractionError> errors,
    Instant createdAt) {

  public IntakeExtractionOutputEnvelope {
    Objects.requireNonNull(extractionRunId, "extractionRunId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    Objects.requireNonNull(packetType, "packetType must not be null");
    Objects.requireNonNull(intendedEntityType, "intendedEntityType must not be null");
    outputSchemaVersion = GovernedIntakeGuards.requireNonBlank(
        outputSchemaVersion,
        "outputSchemaVersion");
    sourceItemIds = List.copyOf(Objects.requireNonNull(
        sourceItemIds,
        "sourceItemIds must not be null"));
    sourceSnapshots = List.copyOf(Objects.requireNonNull(
        sourceSnapshots,
        "sourceSnapshots must not be null"));
    extractedFields = List.copyOf(Objects.requireNonNull(
        extractedFields,
        "extractedFields must not be null"));
    findings = List.copyOf(Objects.requireNonNull(findings, "findings must not be null"));
    errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    if (sourceItemIds.isEmpty()) {
      throw new IllegalArgumentException("sourceItemIds must not be empty");
    }
    if (extractedFields.isEmpty()) {
      throw new IllegalArgumentException("extractedFields must not be empty");
    }
  }
}
