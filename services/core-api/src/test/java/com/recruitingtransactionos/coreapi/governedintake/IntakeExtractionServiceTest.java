package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.service.DeterministicIntakeExtractionService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IntakeExtractionServiceTest {

  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000150001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000150002");
  private static final UUID ACTOR_ID = uuid("00000000-0000-0000-0000-000000150003");
  private static final Instant NOW = Instant.parse("2026-04-28T10:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void extractionRequiresOrganizationId() {
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();

    assertThatThrownBy(() -> service(packetPort, extractionPort).extract(
        null,
        new InformationPacketId(uuid("00000000-0000-0000-0000-000000150101"))))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void extractionRequiresInformationPacketId() {
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();

    assertThatThrownBy(() -> service(packetPort, extractionPort).extract(ORG_A, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("informationPacketId must not be null");
  }

  @Test
  void extractionRejectsMissingOrWrongOrganizationPacket() {
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packetPort.create(packetCommand(ORG_A).build());

    assertThatThrownBy(() -> service(packetPort, extractionPort).extract(
        ORG_A,
        new InformationPacketId(uuid("00000000-0000-0000-0000-000000159999"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("information packet not found in organization");
    assertThatThrownBy(() -> service(packetPort, extractionPort).extract(
        ORG_B,
        packet.informationPacketId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("information packet not found in organization");
    assertThat(extractionPort.runs).isEmpty();
  }

  @Test
  void noSourcePacketCreatesFailedRunWithoutOutputEnvelope() {
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packetPort.create(packetCommand(ORG_A).build());

    IntakeExtractionRun run =
        service(packetPort, extractionPort).extract(ORG_A, packet.informationPacketId());

    assertThat(run.status()).isEqualTo(IntakeExtractionStatus.FAILED);
    assertThat(run.mode()).isEqualTo(IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER);
    assertThat(run.failureReason()).contains("information packet has no attached source items");
    assertThat(run.outputEnvelope()).isEmpty();
    assertThat(run.completedAt()).contains(NOW);
    assertThat(extractionPort.runs).containsExactly(run);
  }

  @Test
  void extractionSucceedsForPacketWithAttachedSourceItems() {
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packetPort.create(packetCommand(ORG_A).build());
    SourceItem cv = sourceItem("00000000-0000-0000-0000-000000150201", SourceItemType.CV);
    SourceItem email = sourceItem("00000000-0000-0000-0000-000000150202", SourceItemType.EMAIL);
    packetPort.attachSourceItem(ORG_A, packet.informationPacketId(), cv.sourceItemId());
    packetPort.addSourceItem(cv);
    packetPort.attachSourceItem(ORG_A, packet.informationPacketId(), email.sourceItemId());
    packetPort.addSourceItem(email);

    IntakeExtractionRun run =
        service(packetPort, extractionPort).extract(ORG_A, packet.informationPacketId());

    assertThat(run.status()).isEqualTo(IntakeExtractionStatus.SUCCEEDED);
    assertThat(run.inputSchemaVersion()).isEqualTo("intake-source-packet.v1");
    assertThat(run.outputSchemaVersion()).isEqualTo("intake-extraction-envelope.v1");
    assertThat(run.extractorVersion()).isEqualTo("deterministic-placeholder.v1");
    assertThat(run.failureReason()).isEmpty();

    IntakeExtractionOutputEnvelope envelope = run.outputEnvelope().orElseThrow();
    assertThat(envelope.extractionRunId()).isEqualTo(run.extractionRunId());
    assertThat(envelope.organizationId()).isEqualTo(ORG_A);
    assertThat(envelope.informationPacketId()).isEqualTo(packet.informationPacketId());
    assertThat(envelope.packetType()).isEqualTo(InformationPacketType.CANDIDATE);
    assertThat(envelope.intendedEntityType()).isEqualTo(IntendedEntityType.CANDIDATE);
    assertThat(envelope.sourceItemIds()).containsExactly(cv.sourceItemId(), email.sourceItemId());
    assertThat(fieldValue(envelope, "packet_type")).isEqualTo("CANDIDATE");
    assertThat(fieldValue(envelope, "intended_entity_type")).isEqualTo("CANDIDATE");
    assertThat(fieldValue(envelope, "source_count")).isEqualTo("2");
    assertThat(fieldValue(envelope, "source_types")).isEqualTo("CV,EMAIL");
    assertThat(envelope.findings())
        .extracting(IntakeExtractionFinding::code)
        .contains("DETERMINISTIC_PLACEHOLDER_ONLY", "SOURCE_TYPE_NEEDS_FUTURE_AI");
    assertThat(envelope.errors()).isEmpty();
  }

  @Test
  void extractionOutputKeepsTruthLayerAndWriteFlagsDisabled() {
    IntakeExtractionOutputEnvelope envelope = successfulEnvelope();

    assertThat(fieldValue(envelope, "extraction_mode")).isEqualTo("DETERMINISTIC_PLACEHOLDER");
    assertThat(fieldValue(envelope, "real_ai_extraction_performed")).isEqualTo("false");
    assertThat(fieldValue(envelope, "semantic_parsing_performed")).isEqualTo("false");
    assertThat(fieldValue(envelope, "claim_ledger_append_allowed")).isEqualTo("false");
    assertThat(fieldValue(envelope, "canonical_write_allowed")).isEqualTo("false");
    assertThat(fieldValue(envelope, "needs_future_extraction")).isEqualTo("true");
    assertThat(fieldValue(envelope, "extraction_output_is_canonical_fact")).isEqualTo("false");
    assertThat(fieldValue(envelope, "extraction_output_is_claim_ledger")).isEqualTo("false");
    assertThat(fieldValue(envelope, "extraction_output_is_review_event")).isEqualTo("false");
    assertThat(envelope.extractedFields())
        .extracting(IntakeExtractedField::valueStatus)
        .containsOnly(IntakeExtractedFieldStatus.PLACEHOLDER,
            IntakeExtractedFieldStatus.NEEDS_FUTURE_AI);
  }

  @Test
  void extractionOutputDoesNotContainCanonicalCandidateProfileFactsOrRawPayload() {
    IntakeExtractionOutputEnvelope envelope = successfulEnvelope();

    assertThat(envelope.extractedFields())
        .extracting(IntakeExtractedField::fieldName)
        .doesNotContain(
            "candidate_name",
            "candidate_email",
            "phone",
            "current_salary",
            "expected_salary",
            "seniority",
            "skills",
            "job_fit",
            "consent_status",
            "candidate_identity");

    assertThat(envelope.toString())
        .doesNotContain("Jane Candidate")
        .doesNotContain("expected salary")
        .doesNotContain("vault://raw-payload")
        .doesNotContain("s3://raw-document");
  }

  @Test
  void duplicateExtractionIsAllowedButDeterministicForSamePacketAndSources() {
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packetPort.create(packetCommand(ORG_A).build());
    SourceItem sourceItem = sourceItem("00000000-0000-0000-0000-000000150301", SourceItemType.CV);
    packetPort.addSourceItem(sourceItem);
    packetPort.attachSourceItem(ORG_A, packet.informationPacketId(), sourceItem.sourceItemId());

    DeterministicIntakeExtractionService service = service(packetPort, extractionPort);
    IntakeExtractionRun first = service.extract(ORG_A, packet.informationPacketId());
    IntakeExtractionRun second = service.extract(ORG_A, packet.informationPacketId());

    assertThat(first.extractionRunId()).isNotEqualTo(second.extractionRunId());
    assertThat(first.sourceSnapshotHash()).isEqualTo(second.sourceSnapshotHash());
    assertThat(first.outputEnvelope().orElseThrow().sourceItemIds())
        .isEqualTo(second.outputEnvelope().orElseThrow().sourceItemIds());
    assertThat(first.outputEnvelope().orElseThrow().sourceSnapshots())
        .isEqualTo(second.outputEnvelope().orElseThrow().sourceSnapshots());
    assertThat(first.outputEnvelope().orElseThrow().extractedFields())
        .isEqualTo(second.outputEnvelope().orElseThrow().extractedFields());
    assertThat(extractionPort.runs).containsExactly(first, second);
  }

  @Test
  void extractionServiceDoesNotCallClaimLedgerReviewWorkflowCanonicalOrProfileBoundaries() {
    assertThat(publicDeclaredMethodNames(DeterministicIntakeExtractionService.class))
        .containsExactly("extract");
    assertThat(nonStaticFieldTypeNames(DeterministicIntakeExtractionService.class))
        .containsExactlyInAnyOrder(
            "Clock",
            "InformationPacketPersistencePort",
            "IntakeExtractionRunPort",
            "Supplier");
    assertThat(allDeclaredNames(DeterministicIntakeExtractionService.class))
        .noneMatch(this::looksLikeForbiddenBoundaryOrWorkflowEngine);
  }

  private IntakeExtractionOutputEnvelope successfulEnvelope() {
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packetPort.create(packetCommand(ORG_A).build());
    SourceItem sourceItem = sourceItem("00000000-0000-0000-0000-000000150401", SourceItemType.CV);
    packetPort.addSourceItem(sourceItem);
    packetPort.attachSourceItem(ORG_A, packet.informationPacketId(), sourceItem.sourceItemId());
    return service(packetPort, extractionPort)
        .extract(ORG_A, packet.informationPacketId())
        .outputEnvelope()
        .orElseThrow();
  }

  private static DeterministicIntakeExtractionService service(
      InMemoryInformationPacketPort packetPort,
      InMemoryExtractionRunPort extractionPort) {
    return new DeterministicIntakeExtractionService(
        packetPort,
        extractionPort,
        FIXED_CLOCK,
        new SequentialUuidSupplier());
  }

  private static InformationPacketCreateCommand.Builder packetCommand(UUID organizationId) {
    return InformationPacketCreateCommand.builder()
        .organizationId(organizationId)
        .packetType(InformationPacketType.CANDIDATE)
        .intendedEntityType(IntendedEntityType.CANDIDATE)
        .createdByActorType(ActorRole.CONSULTANT)
        .createdByActorId(ACTOR_ID)
        .processingStatus(InformationPacketStatus.READY_FOR_EXTRACTION)
        .notes("candidate packet")
        .metadataJson("{\"packet\":\"task-5b\"}");
  }

  private static SourceItem sourceItem(String sourceItemId, SourceItemType sourceType) {
    return new SourceItem(
        new SourceItemId(uuid(sourceItemId)),
        ORG_A,
        sourceType,
        SourceItemOrigin.CONSULTANT_UPLOAD,
        "Senior DV engineer CV",
        "sha256:" + sourceItemId.substring(sourceItemId.length() - 4),
        "ats:" + sourceItemId.substring(sourceItemId.length() - 4),
        "s3://raw-document/Jane-Candidate-CV.pdf",
        "vault://raw-payload/Jane-Candidate",
        "en",
        ActorRole.CONSULTANT,
        ACTOR_ID,
        NOW,
        NOW,
        "{\"rawCandidatePayload\":\"Jane Candidate expected salary 900k\"}",
        SourceItemStatus.REGISTERED);
  }

  private static String fieldValue(
      IntakeExtractionOutputEnvelope envelope,
      String fieldName) {
    return envelope.extractedFields().stream()
        .filter(field -> field.fieldName().equals(fieldName))
        .findFirst()
        .orElseThrow()
        .fieldValue();
  }

  private boolean looksLikeForbiddenBoundaryOrWorkflowEngine(String name) {
    String normalized = normalized(name);
    return normalized.contains("canonicalwrite")
        || normalized.contains("claimledger")
        || normalized.contains("reviewevent")
        || normalized.contains("workflowevent")
        || normalized.contains("workflowengine")
        || normalized.contains("transitionlegality")
        || normalized.contains("candidateprofile")
        || normalized.contains("rawcandidate");
  }

  private static List<String> publicDeclaredMethodNames(Class<?> type) {
    return Stream.of(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()))
        .map(Method::getName)
        .sorted()
        .toList();
  }

  private static List<String> nonStaticFieldTypeNames(Class<?> type) {
    return Stream.of(type.getDeclaredFields())
        .filter(field -> !Modifier.isStatic(field.getModifiers()))
        .map(field -> field.getType().getSimpleName())
        .sorted()
        .toList();
  }

  private static List<String> allDeclaredNames(Class<?> type) {
    List<String> names = new ArrayList<>();
    Stream.of(type.getDeclaredFields()).forEach(field -> names.add(field.getName()));
    Stream.of(type.getDeclaredMethods()).map(Method::getName).forEach(names::add);
    return names;
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static final class SequentialUuidSupplier implements Supplier<UUID> {
    private int sequence;

    @Override
    public UUID get() {
      return uuid("00000000-0000-0000-0000-0000001509"
          + String.format("%02d", ++sequence));
    }
  }

  private static final class InMemoryInformationPacketPort
      implements InformationPacketPersistencePort {
    private final Map<InformationPacketId, InformationPacket> packets = new LinkedHashMap<>();
    private final Map<SourceItemId, SourceItem> sourceItems = new LinkedHashMap<>();
    private final Map<InformationPacketId, List<SourceItemId>> links = new LinkedHashMap<>();
    private int sequence;

    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      InformationPacket packet = new InformationPacket(
          new InformationPacketId(uuid("00000000-0000-0000-0000-0000001505"
              + String.format("%02d", ++sequence))),
          command.organizationId(),
          command.packetType(),
          command.intendedEntityType(),
          command.intendedEntityId(),
          command.createdByActorType(),
          command.createdByActorId(),
          command.processingStatus(),
          NOW,
          NOW,
          command.notes(),
          command.metadataJson());
      packets.put(packet.informationPacketId(), packet);
      return packet;
    }

    @Override
    public Optional<InformationPacket> findById(
        UUID organizationId,
        InformationPacketId informationPacketId) {
      return Optional.ofNullable(packets.get(informationPacketId))
          .filter(packet -> packet.organizationId().equals(organizationId));
    }

    @Override
    public boolean hasSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      return links.getOrDefault(informationPacketId, List.of()).contains(sourceItemId)
          && findById(organizationId, informationPacketId).isPresent();
    }

    @Override
    public void attachSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      links.computeIfAbsent(informationPacketId, ignored -> new ArrayList<>())
          .add(sourceItemId);
    }

    @Override
    public List<SourceItem> listSourceItems(
        UUID organizationId,
        InformationPacketId informationPacketId) {
      if (findById(organizationId, informationPacketId).isEmpty()) {
        return List.of();
      }
      return links.getOrDefault(informationPacketId, List.of()).stream()
          .map(sourceItems::get)
          .filter(sourceItem -> sourceItem != null
              && sourceItem.organizationId().equals(organizationId))
          .toList();
    }

    void addSourceItem(SourceItem sourceItem) {
      sourceItems.put(sourceItem.sourceItemId(), sourceItem);
    }
  }

  private static final class InMemoryExtractionRunPort implements IntakeExtractionRunPort {
    private final List<IntakeExtractionRun> runs = new ArrayList<>();

    @Override
    public IntakeExtractionRun save(IntakeExtractionRun run) {
      runs.add(run);
      return run;
    }

    @Override
    public Optional<IntakeExtractionRun> findById(
        UUID organizationId,
        IntakeExtractionRunId extractionRunId) {
      return runs.stream()
          .filter(run -> run.organizationId().equals(organizationId))
          .filter(run -> run.extractionRunId().equals(extractionRunId))
          .findFirst();
    }

    @Override
    public List<IntakeExtractionRun> listByInformationPacket(
        UUID organizationId,
        InformationPacketId informationPacketId) {
      return runs.stream()
          .filter(run -> run.organizationId().equals(organizationId))
          .filter(run -> run.informationPacketId().equals(informationPacketId))
          .toList();
    }
  }
}
