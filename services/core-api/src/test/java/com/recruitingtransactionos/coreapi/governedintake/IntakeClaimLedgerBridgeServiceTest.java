package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReference;
import com.recruitingtransactionos.coreapi.governedintake.port.ClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.IntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeClaimLedgerBridgeService;
import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IntakeClaimLedgerBridgeServiceTest {

  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000170001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000170002");
  private static final UUID ACTOR_ID = uuid("00000000-0000-0000-0000-000000170003");
  private static final IntakeExtractionRunId RUN_ID =
      new IntakeExtractionRunId(uuid("00000000-0000-0000-0000-000000170004"));
  private static final InformationPacketId PACKET_ID =
      new InformationPacketId(uuid("00000000-0000-0000-0000-000000170005"));
  private static final SourceItemId SOURCE_ID =
      new SourceItemId(uuid("00000000-0000-0000-0000-000000170006"));
  private static final Instant NOW = Instant.parse("2026-04-28T12:00:00Z");

  @Test
  void bridgeRequestRequiresOrganizationId() {
    assertThatThrownBy(() -> new IntakeClaimLedgerBridgeRequest(
        null,
        RUN_ID,
        ActorRole.CONSULTANT,
        ACTOR_ID,
        IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
        null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void bridgeRequestRequiresExtractionRunId() {
    assertThatThrownBy(() -> new IntakeClaimLedgerBridgeRequest(
        ORG_A,
        null,
        ActorRole.CONSULTANT,
        ACTOR_ID,
        IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
        null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("extractionRunId must not be null");
  }

  @Test
  void bridgeRejectsMissingOrWrongOrganizationExtractionRun() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(successfulRun(ORG_A, packet, defaultPlaceholderFields()));

    assertThatThrownBy(() -> service(packetPort, extractionPort, new ClaimLedgerStore())
        .bridge(request(new IntakeExtractionRunId(uuid("00000000-0000-0000-0000-000000179999")))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraction run not found in organization");

    assertThatThrownBy(() -> service(packetPort, extractionPort, new ClaimLedgerStore())
        .bridge(new IntakeClaimLedgerBridgeRequest(
            ORG_B,
            RUN_ID,
            ActorRole.CONSULTANT,
            ACTOR_ID,
            IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
            null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraction run not found in organization");
  }

  @Test
  void bridgeRejectsFailedExtractionRun() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(new IntakeExtractionRun(
        RUN_ID,
        ORG_A,
        packet.informationPacketId(),
        IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
        IntakeExtractionStatus.FAILED,
        "intake-source-packet.v1",
        "intake-extraction-envelope.v1",
        "deterministic-placeholder.v1",
        "sha256:failed",
        NOW,
        Optional.of(NOW),
        Optional.of("fixture failure"),
        Optional.empty()));

    assertThatThrownBy(() -> service(new InMemoryInformationPacketPort(packet), extractionPort,
        new ClaimLedgerStore()).bridge(request(RUN_ID)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("failed extraction run cannot be bridged to claim ledger");
  }

  @Test
  void bridgeRejectsMissingOutputEnvelope() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(new IntakeExtractionRun(
        RUN_ID,
        ORG_A,
        packet.informationPacketId(),
        IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
        IntakeExtractionStatus.CREATED,
        "intake-source-packet.v1",
        "intake-extraction-envelope.v1",
        "deterministic-placeholder.v1",
        "sha256:created",
        NOW,
        Optional.empty(),
        Optional.empty(),
        Optional.empty()));

    assertThatThrownBy(() -> service(new InMemoryInformationPacketPort(packet), extractionPort,
        new ClaimLedgerStore()).bridge(request(RUN_ID)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraction run output envelope missing");
  }

  @Test
  void defaultPlaceholderOutputAppendsNoBusinessClaims() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(successfulRun(ORG_A, packet, defaultPlaceholderFields()));
    ClaimLedgerStore store = new ClaimLedgerStore();

    IntakeClaimLedgerBridgeResult result =
        service(new InMemoryInformationPacketPort(packet), extractionPort, store)
            .bridge(request(RUN_ID));

    assertThat(result.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.NO_CLAIMS_APPENDED);
    assertThat(result.appendedClaimIds()).isEmpty();
    assertThat(result.existingClaimIds()).isEmpty();
    assertThat(result.informationPacketId()).isEqualTo(packet.informationPacketId());
    assertThat(result.skippedItems()).extracting(IntakeClaimLedgerBridgeItemDecision::fieldName)
        .contains("source_count", "real_ai_extraction_performed", "canonical_write_allowed");
    assertThat(store.commands).isEmpty();
  }

  @Test
  void bridgeEligibleOperationalFixtureAppendsInternalNonCanonicalClaim() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(successfulRun(ORG_A, packet, List.of(bridgeEligibleField())));
    ClaimLedgerStore store = new ClaimLedgerStore();

    IntakeClaimLedgerBridgeResult result =
        service(new InMemoryInformationPacketPort(packet), extractionPort, store)
            .bridge(request(RUN_ID));

    assertThat(result.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.SUCCEEDED);
    assertThat(result.appendedClaimIds()).hasSize(1);
    assertThat(result.existingClaimIds()).isEmpty();
    assertThat(result.blockedItems()).isEmpty();
    assertThat(store.commands).hasSize(1);

    ClaimLedgerAppendCommand command = store.commands.getFirst();
    assertThat(command.organizationId()).isEqualTo(ORG_A);
    assertThat(command.targetEntity()).isEqualTo(new EntityRef(
        "information_packet",
        PACKET_ID.value()));
    assertThat(command.targetFieldPath()).isEqualTo("intake.bridge_eligible.quality_note");
    assertThat(command.claimValue()).isEqualTo("explicitly marked operational bridge fixture");
    assertThat(command.claimType()).isEqualTo(ClaimType.INFERENCE);
    assertThat(command.assertionStrength()).isEqualTo(AssertionStrength.WEAK_SIGNAL);
    assertThat(command.verificationStatus()).isEqualTo(VerificationStatus.AI_EXTRACTED);
    assertThat(command.clientShareability()).isEqualTo(ClientShareability.INTERNAL_ONLY);
    assertThat(command.speaker()).isEqualTo(ActorRole.AI);
    assertThat(command.sourceItemId()).isNull();
    assertThat(command.aiTaskRunId()).isNull();
    assertThat(command.sourceSpanReference().value())
        .contains("intake.extraction_run:" + RUN_ID.value())
        .contains("intake.information_packet:" + PACKET_ID.value())
        .contains("intake.source_item:" + SOURCE_ID.value())
        .contains("field:intake.bridge_eligible.quality_note");
  }

  @Test
  void repeatedBridgeCallReturnsExistingClaimAndDoesNotAppendDuplicate() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(successfulRun(ORG_A, packet, List.of(bridgeEligibleField())));
    ClaimLedgerStore store = new ClaimLedgerStore();
    IntakeClaimLedgerBridgeService service =
        service(new InMemoryInformationPacketPort(packet), extractionPort, store);

    IntakeClaimLedgerBridgeResult first = service.bridge(request(RUN_ID));
    IntakeClaimLedgerBridgeResult second = service.bridge(request(RUN_ID));

    assertThat(first.appendedClaimIds()).hasSize(1);
    assertThat(second.appendedClaimIds()).isEmpty();
    assertThat(second.existingClaimIds()).containsExactly(first.appendedClaimIds().getFirst());
    assertThat(second.skippedItems()).extracting(IntakeClaimLedgerBridgeItemDecision::reason)
        .contains("duplicate_source_reference_already_appended");
    assertThat(store.commands).hasSize(1);
  }

  @Test
  void differentDiscriminatorsAppendDistinctClaimsForSameFieldName() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(successfulRun(ORG_A, packet, List.of(
        bridgeEligibleField("fixture:quality-note:chunk-1"),
        bridgeEligibleField("fixture:quality-note:chunk-2"))));
    ClaimLedgerStore store = new ClaimLedgerStore();

    IntakeClaimLedgerBridgeResult result =
        service(new InMemoryInformationPacketPort(packet), extractionPort, store)
            .bridge(request(RUN_ID));

    assertThat(result.appendedClaimIds()).hasSize(2);
    assertThat(store.commands).hasSize(2);
    assertThat(store.commands)
        .extracting(command -> command.sourceSpanReference().value())
        .doesNotHaveDuplicates()
        .allMatch(value -> value.contains("|discriminator:fixture:quality-note:chunk-"));
  }

  @Test
  void forbiddenPlaceholderMetadataIsBlockedEvenIfMarkedClaimCandidate() {
    InMemoryExtractionRunPort extractionPort = new InMemoryExtractionRunPort();
    InformationPacket packet = packet(ORG_A);
    extractionPort.save(successfulRun(ORG_A, packet, List.of(new IntakeExtractedField(
        "real_ai_extraction_performed",
        "false",
        SOURCE_ID,
        1.0d,
        IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
        "malformed fixture",
        "fixture:malformed"))));
    ClaimLedgerStore store = new ClaimLedgerStore();

    IntakeClaimLedgerBridgeResult result =
        service(new InMemoryInformationPacketPort(packet), extractionPort, store)
            .bridge(request(RUN_ID));

    assertThat(result.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.NO_CLAIMS_APPENDED);
    assertThat(result.blockedItems()).extracting(IntakeClaimLedgerBridgeItemDecision::reason)
        .contains("placeholder_metadata_cannot_become_claim");
    assertThat(store.commands).isEmpty();
  }

  @Test
  void bridgeServiceDoesNotExposeReviewCanonicalWorkflowOrProfileBehavior() {
    assertThat(publicDeclaredMethodNames(IntakeClaimLedgerBridgeService.class))
        .containsExactly("bridge");
    assertThat(nonStaticFieldTypeNames(IntakeClaimLedgerBridgeService.class))
        .containsExactlyInAnyOrder(
            "ClaimLedgerService",
            "ClaimLedgerSourceReferenceLookupPort",
            "InformationPacketPersistencePort",
            "IntakeExtractionRunPort");
    assertThat(allDeclaredNames(IntakeClaimLedgerBridgeService.class))
        .noneMatch(this::looksLikeForbiddenBoundaryOrWorkflowEngine);
  }

  private static IntakeClaimLedgerBridgeService service(
      InformationPacketPersistencePort packetPort,
      IntakeExtractionRunPort extractionPort,
      ClaimLedgerStore store) {
    return new IntakeClaimLedgerBridgeService(
        extractionPort,
        packetPort,
        new ClaimLedgerService(store),
        store);
  }

  private static IntakeClaimLedgerBridgeRequest request(IntakeExtractionRunId extractionRunId) {
    return new IntakeClaimLedgerBridgeRequest(
        ORG_A,
        extractionRunId,
        ActorRole.CONSULTANT,
        ACTOR_ID,
        IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
        null);
  }

  private static IntakeExtractionRun successfulRun(
      UUID organizationId,
      InformationPacket packet,
      List<IntakeExtractedField> fields) {
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        RUN_ID,
        organizationId,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        "intake-extraction-envelope.v1",
        List.of(SOURCE_ID),
        List.of(),
        List.of(new IntakeExtractionSourceSnapshot(
            SOURCE_ID,
            SourceItemType.CV,
            "Senior DV engineer CV",
            "sha256:170006",
            "ats:170006")),
        fields,
        List.of(),
        List.of(),
        List.of(),
        NOW);
    return new IntakeExtractionRun(
        RUN_ID,
        organizationId,
        packet.informationPacketId(),
        IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
        IntakeExtractionStatus.SUCCEEDED,
        "intake-source-packet.v1",
        "intake-extraction-envelope.v1",
        "deterministic-placeholder.v1",
        "sha256:success",
        NOW,
        Optional.of(NOW),
        Optional.empty(),
        Optional.of(envelope));
  }

  private static List<IntakeExtractedField> defaultPlaceholderFields() {
    return List.of(
        placeholder("source_count", "1"),
        placeholder("real_ai_extraction_performed", "false"),
        placeholder("claim_ledger_append_allowed", "false"),
        placeholder("canonical_write_allowed", "false"),
        placeholder("needs_future_extraction", "true"));
  }

  private static IntakeExtractedField placeholder(String fieldName, String value) {
    return new IntakeExtractedField(
        fieldName,
        value,
        null,
        1.0d,
        IntakeExtractedFieldStatus.PLACEHOLDER,
        "placeholder metadata only",
        null);
  }

  private static IntakeExtractedField bridgeEligibleField() {
    return bridgeEligibleField("fixture:quality-note");
  }

  private static IntakeExtractedField bridgeEligibleField(String discriminator) {
    return new IntakeExtractedField(
        "intake.bridge_eligible.quality_note",
        "explicitly marked operational bridge fixture",
        SOURCE_ID,
        0.5d,
        IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
        "Operational fixture; not a candidate/company/job canonical fact.",
        discriminator);
  }

  private static InformationPacket packet(UUID organizationId) {
    return new InformationPacket(
        PACKET_ID,
        organizationId,
        InformationPacketType.CANDIDATE,
        IntendedEntityType.CANDIDATE,
        null,
        ActorRole.CONSULTANT,
        ACTOR_ID,
        InformationPacketStatus.READY_FOR_EXTRACTION,
        NOW,
        NOW,
        "candidate packet",
        "{\"fixture\":\"task-5c\"}");
  }

  private boolean looksLikeForbiddenBoundaryOrWorkflowEngine(String name) {
    String normalized = normalized(name);
    return normalized.contains("canonicalwrite")
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

  private static final class InMemoryInformationPacketPort
      implements InformationPacketPersistencePort {
    private final Map<InformationPacketId, InformationPacket> packets = new LinkedHashMap<>();

    private InMemoryInformationPacketPort(InformationPacket... packets) {
      for (InformationPacket packet : packets) {
        this.packets.put(packet.informationPacketId(), packet);
      }
    }

    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      throw new UnsupportedOperationException("create is not used by bridge tests");
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
      throw new UnsupportedOperationException("hasSourceItem is not used by bridge tests");
    }

    @Override
    public void attachSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      throw new UnsupportedOperationException("attachSourceItem is not used by bridge tests");
    }

    @Override
    public List<SourceItem> listSourceItems(
        UUID organizationId,
        InformationPacketId informationPacketId) {
      throw new UnsupportedOperationException("listSourceItems is not used by bridge tests");
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

  private static final class ClaimLedgerStore
      implements ClaimLedgerPort, ClaimLedgerSourceReferenceLookupPort {
    private final List<ClaimLedgerAppendCommand> commands = new ArrayList<>();
    private final Map<SourceSpanRef, ClaimLedgerSourceReference> sourceReferences =
        new LinkedHashMap<>();
    private int sequence;

    @Override
    public ClaimLedgerAppendResult append(ClaimLedgerAppendCommand command) {
      commands.add(command);
      ClaimId claimId = new ClaimId(uuid("00000000-0000-0000-0000-0000001701"
          + String.format("%02d", ++sequence)));
      sourceReferences.put(command.sourceSpanReference(), new ClaimLedgerSourceReference(
          claimId,
          command.organizationId(),
          command.targetEntity(),
          command.targetFieldPath(),
          command.sourceSpanReference()));
      return new ClaimLedgerAppendResult(claimId);
    }

    @Override
    public Optional<ClaimLedgerSourceReference> findBySourceSpanReference(
        UUID organizationId,
        SourceSpanRef sourceSpanReference) {
      return Optional.ofNullable(sourceReferences.get(sourceSpanReference))
          .filter(reference -> reference.organizationId().equals(organizationId));
    }
  }
}
