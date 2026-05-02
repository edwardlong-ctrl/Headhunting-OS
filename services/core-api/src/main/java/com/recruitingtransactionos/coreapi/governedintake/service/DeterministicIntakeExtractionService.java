package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedField;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedFieldStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionFinding;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionMode;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionSourceSnapshot;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class DeterministicIntakeExtractionService {

  private static final String INPUT_SCHEMA_VERSION = "intake-source-packet.v1";
  private static final String OUTPUT_SCHEMA_VERSION = "intake-extraction-envelope.v1";
  private static final String EXTRACTOR_VERSION = "deterministic-placeholder.v1";
  private static final String EMPTY_SOURCE_FAILURE =
      "information packet has no attached source items";

  private final InformationPacketPersistencePort informationPacketPersistencePort;
  private final IntakeExtractionRunPort intakeExtractionRunPort;
  private final Clock clock;
  private final Supplier<UUID> extractionRunIdSupplier;

  public DeterministicIntakeExtractionService(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort) {
    this(
        informationPacketPersistencePort,
        intakeExtractionRunPort,
        Clock.systemUTC(),
        UUID::randomUUID);
  }

  public DeterministicIntakeExtractionService(
      InformationPacketPersistencePort informationPacketPersistencePort,
      IntakeExtractionRunPort intakeExtractionRunPort,
      Clock clock,
      Supplier<UUID> extractionRunIdSupplier) {
    this.informationPacketPersistencePort = Objects.requireNonNull(
        informationPacketPersistencePort,
        "informationPacketPersistencePort must not be null");
    this.intakeExtractionRunPort = Objects.requireNonNull(
        intakeExtractionRunPort,
        "intakeExtractionRunPort must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.extractionRunIdSupplier = Objects.requireNonNull(
        extractionRunIdSupplier,
        "extractionRunIdSupplier must not be null");
  }

  public IntakeExtractionRun extract(
      UUID organizationId,
      InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    InformationPacket packet = informationPacketPersistencePort
        .findById(organizationId, informationPacketId)
        .orElseThrow(() -> new IllegalArgumentException(
            "information packet not found in organization"));
    List<SourceItem> sourceItems = informationPacketPersistencePort.listSourceItems(
        organizationId,
        informationPacketId);
    ensureSourceOrganization(organizationId, sourceItems);

    IntakeExtractionRunId extractionRunId = new IntakeExtractionRunId(
        extractionRunIdSupplier.get());
    Instant now = clock.instant();
    String sourceSnapshotHash = sourceSnapshotHash(packet, sourceItems);

    if (sourceItems.isEmpty()) {
      return intakeExtractionRunPort.save(new IntakeExtractionRun(
          extractionRunId,
          organizationId,
          informationPacketId,
          IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
          IntakeExtractionStatus.FAILED,
          INPUT_SCHEMA_VERSION,
          OUTPUT_SCHEMA_VERSION,
          EXTRACTOR_VERSION,
          sourceSnapshotHash,
          now,
          Optional.of(now),
          Optional.of(EMPTY_SOURCE_FAILURE),
          Optional.empty()));
    }

    IntakeExtractionOutputEnvelope outputEnvelope = outputEnvelope(
        extractionRunId,
        organizationId,
        packet,
        sourceItems,
        now);

    return intakeExtractionRunPort.save(new IntakeExtractionRun(
        extractionRunId,
        organizationId,
        informationPacketId,
        IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
        IntakeExtractionStatus.SUCCEEDED,
        INPUT_SCHEMA_VERSION,
        OUTPUT_SCHEMA_VERSION,
        EXTRACTOR_VERSION,
        sourceSnapshotHash,
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(outputEnvelope)));
  }

  private static IntakeExtractionOutputEnvelope outputEnvelope(
      IntakeExtractionRunId extractionRunId,
      UUID organizationId,
      InformationPacket packet,
      List<SourceItem> sourceItems,
      Instant createdAt) {
    return new IntakeExtractionOutputEnvelope(
        extractionRunId,
        organizationId,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        OUTPUT_SCHEMA_VERSION,
        sourceItems.stream().map(SourceItem::sourceItemId).toList(),
        List.of(),
        sourceItems.stream().map(DeterministicIntakeExtractionService::sourceSnapshot).toList(),
        extractedFields(packet, sourceItems),
        List.of(),
        findings(sourceItems),
        List.of(),
        createdAt);
  }

  private static List<IntakeExtractedField> extractedFields(
      InformationPacket packet,
      List<SourceItem> sourceItems) {
    List<IntakeExtractedField> fields = new ArrayList<>();
    fields.add(field("packet_type", packet.packetType().wireValue()));
    fields.add(field("intended_entity_type", packet.intendedEntityType().wireValue()));
    fields.add(field("source_count", String.valueOf(sourceItems.size())));
    fields.add(field("source_types", sourceTypes(sourceItems), IntakeExtractedFieldStatus.NEEDS_FUTURE_AI));
    fields.add(field("extraction_mode", IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER.wireValue()));
    fields.add(field("real_ai_extraction_performed", "false"));
    fields.add(field("semantic_parsing_performed", "false"));
    fields.add(field("claim_ledger_append_allowed", "false"));
    fields.add(field("canonical_write_allowed", "false"));
    fields.add(field("needs_future_extraction", "true"));
    fields.add(field("extraction_output_is_canonical_fact", "false"));
    fields.add(field("extraction_output_is_claim_ledger", "false"));
    fields.add(field("extraction_output_is_review_event", "false"));
    return List.copyOf(fields);
  }

  private static IntakeExtractedField field(String name, String value) {
    return field(name, value, IntakeExtractedFieldStatus.PLACEHOLDER);
  }

  private static IntakeExtractedField field(
      String name,
      String value,
      IntakeExtractedFieldStatus status) {
    return new IntakeExtractedField(
        name,
        value,
        null,
        1.0d,
        status,
        "Deterministic placeholder metadata only; not a canonical fact or claim.",
        null);
  }

  private static List<IntakeExtractionFinding> findings(List<SourceItem> sourceItems) {
    List<IntakeExtractionFinding> findings = new ArrayList<>();
    findings.add(new IntakeExtractionFinding(
        "DETERMINISTIC_PLACEHOLDER_ONLY",
        "No real AI extraction or semantic parsing was performed.",
        null));
    for (SourceItem sourceItem : sourceItems) {
      findings.add(new IntakeExtractionFinding(
          "SOURCE_TYPE_NEEDS_FUTURE_AI",
          "Source type " + sourceItem.sourceType().wireValue()
              + " is recorded as provenance only until future extraction work.",
          sourceItem.sourceItemId()));
    }
    return List.copyOf(findings);
  }

  private static IntakeExtractionSourceSnapshot sourceSnapshot(SourceItem sourceItem) {
    return new IntakeExtractionSourceSnapshot(
        sourceItem.sourceItemId(),
        sourceItem.sourceType(),
        sourceItem.title(),
        sourceItem.contentHash(),
        sourceItem.externalRef());
  }

  private static String sourceTypes(List<SourceItem> sourceItems) {
    return sourceItems.stream()
        .map(SourceItem::sourceType)
        .map(SourceItemType::wireValue)
        .distinct()
        .sorted()
        .collect(Collectors.joining(","));
  }

  private static void ensureSourceOrganization(UUID organizationId, List<SourceItem> sourceItems) {
    for (SourceItem sourceItem : sourceItems) {
      if (!sourceItem.organizationId().equals(organizationId)) {
        throw new IllegalStateException("source item outside organization");
      }
    }
  }

  private static String sourceSnapshotHash(
      InformationPacket packet,
      List<SourceItem> sourceItems) {
    List<SourceItem> sortedSourceItems = sourceItems.stream()
        .sorted(Comparator.comparing(sourceItem -> sourceItem.sourceItemId().value()))
        .toList();
    StringBuilder builder = new StringBuilder();
    builder.append(packet.organizationId()).append('|')
        .append(packet.informationPacketId().value()).append('|')
        .append(packet.packetType().wireValue()).append('|')
        .append(packet.intendedEntityType().wireValue());
    for (SourceItem sourceItem : sortedSourceItems) {
      builder.append('|')
          .append(sourceItem.sourceItemId().value()).append(':')
          .append(sourceItem.sourceType().wireValue()).append(':')
          .append(nullToEmpty(sourceItem.title())).append(':')
          .append(nullToEmpty(sourceItem.contentHash())).append(':')
          .append(nullToEmpty(sourceItem.externalRef()));
    }
    return "sha256:" + sha256(builder.toString());
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
