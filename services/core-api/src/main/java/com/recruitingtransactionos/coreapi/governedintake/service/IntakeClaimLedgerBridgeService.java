package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeItemDecision;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgePolicy;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeRequest;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeResult;
import com.recruitingtransactionos.coreapi.governedintake.IntakeClaimLedgerBridgeStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedField;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractedFieldStatus;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionOutputEnvelope;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionStatus;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReference;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class IntakeClaimLedgerBridgeService {

  private static final String OPERATIONAL_FIELD_PREFIX = "intake.bridge_eligible.";
  private static final Set<String> FORBIDDEN_PLACEHOLDER_FIELDS = Set.of(
      "packet_type",
      "intended_entity_type",
      "source_count",
      "source_types",
      "extraction_mode",
      "real_ai_extraction_performed",
      "semantic_parsing_performed",
      "claim_ledger_append_allowed",
      "canonical_write_allowed",
      "needs_future_extraction",
      "extraction_output_is_canonical_fact",
      "extraction_output_is_claim_ledger",
      "extraction_output_is_review_event");

  private final IntakeExtractionRunPort intakeExtractionRunPort;
  private final InformationPacketPersistencePort informationPacketPersistencePort;
  private final ClaimLedgerService claimLedgerService;
  private final ClaimLedgerSourceReferenceLookupPort claimLedgerSourceReferenceLookupPort;

  public IntakeClaimLedgerBridgeService(
      IntakeExtractionRunPort intakeExtractionRunPort,
      InformationPacketPersistencePort informationPacketPersistencePort,
      ClaimLedgerService claimLedgerService,
      ClaimLedgerSourceReferenceLookupPort claimLedgerSourceReferenceLookupPort) {
    this.intakeExtractionRunPort = Objects.requireNonNull(
        intakeExtractionRunPort,
        "intakeExtractionRunPort must not be null");
    this.informationPacketPersistencePort = Objects.requireNonNull(
        informationPacketPersistencePort,
        "informationPacketPersistencePort must not be null");
    this.claimLedgerService = Objects.requireNonNull(
        claimLedgerService,
        "claimLedgerService must not be null");
    this.claimLedgerSourceReferenceLookupPort = Objects.requireNonNull(
        claimLedgerSourceReferenceLookupPort,
        "claimLedgerSourceReferenceLookupPort must not be null");
  }

  public IntakeClaimLedgerBridgeResult bridge(IntakeClaimLedgerBridgeRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    IntakeExtractionRun run = intakeExtractionRunPort
        .findById(request.organizationId(), request.extractionRunId())
        .orElseThrow(() -> new IllegalArgumentException(
            "extraction run not found in organization"));
    if (run.status() == IntakeExtractionStatus.FAILED) {
      throw new IllegalArgumentException(
          "failed extraction run cannot be bridged to claim ledger");
    }
    IntakeExtractionOutputEnvelope envelope = run.outputEnvelope()
        .orElseThrow(() -> new IllegalArgumentException(
            "extraction run output envelope missing"));
    if (run.status() != IntakeExtractionStatus.SUCCEEDED) {
      throw new IllegalArgumentException("extraction run must have SUCCEEDED status");
    }
    validateEnvelopeMatchesRun(run, envelope);
    informationPacketPersistencePort
        .findById(request.organizationId(), run.informationPacketId())
        .orElseThrow(() -> new IllegalArgumentException(
            "information packet not found in organization"));

    List<ClaimId> appendedClaimIds = new ArrayList<>();
    List<ClaimId> existingClaimIds = new ArrayList<>();
    List<IntakeClaimLedgerBridgeItemDecision> skippedItems = new ArrayList<>();
    List<IntakeClaimLedgerBridgeItemDecision> blockedItems = new ArrayList<>();
    for (IntakeExtractedField field : envelope.extractedFields()) {
      if (!isExplicitClaimCandidate(field)) {
        skippedItems.add(decision(
            field,
            "not_marked_claim_candidate"));
        continue;
      }
      if (isForbiddenPlaceholderField(field.fieldName())) {
        blockedItems.add(decision(
            field,
            "placeholder_metadata_cannot_become_claim"));
        continue;
      }
      if (request.bridgePolicy()
          != IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY) {
        blockedItems.add(decision(field, "unsupported_bridge_policy"));
        continue;
      }
      if (!field.fieldName().startsWith(OPERATIONAL_FIELD_PREFIX)) {
        blockedItems.add(decision(field, "only_operational_bridge_fields_allowed"));
        continue;
      }
      if (field.fieldValue().isBlank()) {
        blockedItems.add(decision(field, "claim_candidate_value_must_not_be_blank"));
        continue;
      }
      if (field.sourceItemId() == null || !envelope.sourceItemIds().contains(field.sourceItemId())) {
        blockedItems.add(decision(field, "claim_candidate_requires_intake_source_item_lineage"));
        continue;
      }

      ClaimLedgerAppendCommand command = command(envelope, field);
      claimLedgerSourceReferenceLookupPort
          .findBySourceSpanReference(request.organizationId(), command.sourceSpanReference())
          .ifPresentOrElse(
              existing -> handleExistingSourceReference(
                  existing,
                  command,
                  existingClaimIds,
                  skippedItems,
                  field),
              () -> appendedClaimIds.add(
                  claimLedgerService.append(command).claimId()));
    }

    IntakeClaimLedgerBridgeStatus status =
        appendedClaimIds.isEmpty() && existingClaimIds.isEmpty()
            ? IntakeClaimLedgerBridgeStatus.NO_CLAIMS_APPENDED
            : IntakeClaimLedgerBridgeStatus.SUCCEEDED;
    String summary = status == IntakeClaimLedgerBridgeStatus.NO_CLAIMS_APPENDED
        ? "no bridge-eligible claim candidates were appended"
        : "bridge-eligible claim candidates are represented in ClaimLedger";
    return new IntakeClaimLedgerBridgeResult(
        request.organizationId(),
        request.extractionRunId(),
        run.informationPacketId(),
        appendedClaimIds,
        existingClaimIds,
        skippedItems,
        blockedItems,
        status,
        summary);
  }

  private static void validateEnvelopeMatchesRun(
      IntakeExtractionRun run,
      IntakeExtractionOutputEnvelope envelope) {
    if (!envelope.organizationId().equals(run.organizationId())
        || !envelope.extractionRunId().equals(run.extractionRunId())
        || !envelope.informationPacketId().equals(run.informationPacketId())) {
      throw new IllegalStateException("extraction output envelope does not match run lineage");
    }
  }

  private static boolean isExplicitClaimCandidate(IntakeExtractedField field) {
    return field.valueStatus() == IntakeExtractedFieldStatus.CLAIM_CANDIDATE;
  }

  private static boolean isForbiddenPlaceholderField(String fieldName) {
    return FORBIDDEN_PLACEHOLDER_FIELDS.contains(fieldName);
  }

  private static ClaimLedgerAppendCommand command(
      IntakeExtractionOutputEnvelope envelope,
      IntakeExtractedField field) {
    return new ClaimLedgerAppendCommand(
        envelope.organizationId(),
        new EntityRef("information_packet", envelope.informationPacketId().value()),
        field.fieldName(),
        field.fieldValue(),
        ClaimType.INFERENCE,
        AssertionStrength.WEAK_SIGNAL,
        sourceSpanReference(envelope, field),
        ActorRole.AI,
        VerificationStatus.AI_EXTRACTED,
        ClientShareability.INTERNAL_ONLY,
        null,
        null);
  }

  private static SourceSpanRef sourceSpanReference(
      IntakeExtractionOutputEnvelope envelope,
      IntakeExtractedField field) {
    return new SourceSpanRef(
        "intake.extraction_run:" + envelope.extractionRunId().value()
            + "|intake.information_packet:" + envelope.informationPacketId().value()
            + "|packet_type:" + envelope.packetType().wireValue()
            + "|intended_entity_type:" + envelope.intendedEntityType().wireValue()
            + "|intake.source_item:" + field.sourceItemId().value()
            + "|field:" + field.fieldName());
  }

  private static void handleExistingSourceReference(
      ClaimLedgerSourceReference existing,
      ClaimLedgerAppendCommand command,
      List<ClaimId> existingClaimIds,
      List<IntakeClaimLedgerBridgeItemDecision> skippedItems,
      IntakeExtractedField field) {
    if (!existing.targetEntity().equals(command.targetEntity())
        || !existing.targetFieldPath().equals(command.targetFieldPath())) {
      throw new IllegalStateException("claim ledger source reference conflict");
    }
    existingClaimIds.add(existing.claimId());
    skippedItems.add(decision(
        field,
        "duplicate_source_reference_already_appended"));
  }

  private static IntakeClaimLedgerBridgeItemDecision decision(
      IntakeExtractedField field,
      String reason) {
    return new IntakeClaimLedgerBridgeItemDecision(
        field.fieldName(),
        field.sourceItemId(),
        reason);
  }
}
