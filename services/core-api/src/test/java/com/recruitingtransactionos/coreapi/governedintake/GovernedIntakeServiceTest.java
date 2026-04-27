package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
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

class GovernedIntakeServiceTest {

  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000050001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000050002");
  private static final UUID ACTOR_ID = uuid("00000000-0000-0000-0000-000000050003");
  private static final Instant RECEIVED_AT = Instant.parse("2026-04-28T08:00:00Z");

  @Test
  void sourceItemCreationRequiresOrganizationId() {
    assertThatThrownBy(() -> sourceCommand().organizationId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void sourceItemCreationRequiresSourceType() {
    assertThatThrownBy(() -> sourceCommand().sourceType(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("sourceType must not be null");
  }

  @Test
  void sourceItemRejectsBlankOptionalTitleReferencesAndHash() {
    assertThatThrownBy(() -> sourceCommand().title(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("title must not be blank");
    assertThatThrownBy(() -> sourceCommand().contentHash(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("contentHash must not be blank");
    assertThatThrownBy(() -> sourceCommand().externalRef(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("externalRef must not be blank");
    assertThatThrownBy(() -> sourceCommand().storageRef(" ").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("storageRef must not be blank");
  }

  @Test
  void informationPacketCreationRequiresOrganizationId() {
    assertThatThrownBy(() -> packetCommand().organizationId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void informationPacketCreationRequiresPacketType() {
    assertThatThrownBy(() -> packetCommand().packetType(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("packetType must not be null");
  }

  @Test
  void informationPacketCreationRequiresProcessingStatus() {
    assertThatThrownBy(() -> packetCommand().processingStatus(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("processingStatus must not be null");
  }

  @Test
  void attachSourceItemToInformationPacketSucceedsInsideSameOrganization() {
    InMemorySourceItemPort sourcePort = new InMemorySourceItemPort();
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort(sourcePort);
    GovernedIntakeService service = service(sourcePort, packetPort);
    SourceItem sourceItem = service.registerSourceItem(sourceCommand().build());
    InformationPacket packet = service.createInformationPacket(packetCommand().build());

    service.attachSourceItemToPacket(
        new AttachSourceItemToPacketCommand(ORG_A, packet.informationPacketId(),
            sourceItem.sourceItemId()));

    assertThat(service.listSourceItemsForPacket(ORG_A, packet.informationPacketId()))
        .containsExactly(sourceItem);
  }

  @Test
  void attachSourceItemToInformationPacketRejectsCrossOrganizationAttach() {
    InMemorySourceItemPort sourcePort = new InMemorySourceItemPort();
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort(sourcePort);
    GovernedIntakeService service = service(sourcePort, packetPort);
    SourceItem sourceItem = service.registerSourceItem(sourceCommand().organizationId(ORG_B).build());
    InformationPacket packet = service.createInformationPacket(packetCommand().build());

    assertThatThrownBy(() -> service.attachSourceItemToPacket(
        new AttachSourceItemToPacketCommand(ORG_A, packet.informationPacketId(),
            sourceItem.sourceItemId())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("source item not found in organization");
  }

  @Test
  void duplicateSourceAttachmentIsRejected() {
    InMemorySourceItemPort sourcePort = new InMemorySourceItemPort();
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort(sourcePort);
    GovernedIntakeService service = service(sourcePort, packetPort);
    SourceItem sourceItem = service.registerSourceItem(sourceCommand().build());
    InformationPacket packet = service.createInformationPacket(packetCommand().build());
    AttachSourceItemToPacketCommand command =
        new AttachSourceItemToPacketCommand(ORG_A, packet.informationPacketId(),
            sourceItem.sourceItemId());

    service.attachSourceItemToPacket(command);

    assertThatThrownBy(() -> service.attachSourceItemToPacket(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("source item already attached to information packet");
  }

  @Test
  void findPacketByIdIsOrganizationScoped() {
    InMemorySourceItemPort sourcePort = new InMemorySourceItemPort();
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort(sourcePort);
    GovernedIntakeService service = service(sourcePort, packetPort);
    InformationPacket packet = service.createInformationPacket(packetCommand().build());

    assertThat(service.findInformationPacket(ORG_A, packet.informationPacketId())).contains(packet);
    assertThat(service.findInformationPacket(ORG_B, packet.informationPacketId())).isEmpty();
  }

  @Test
  void listSourceItemsForPacketIsOrganizationScoped() {
    InMemorySourceItemPort sourcePort = new InMemorySourceItemPort();
    InMemoryInformationPacketPort packetPort = new InMemoryInformationPacketPort(sourcePort);
    GovernedIntakeService service = service(sourcePort, packetPort);
    SourceItem sourceItem = service.registerSourceItem(sourceCommand().build());
    InformationPacket packet = service.createInformationPacket(packetCommand().build());

    service.attachSourceItemToPacket(
        new AttachSourceItemToPacketCommand(ORG_A, packet.informationPacketId(),
            sourceItem.sourceItemId()));

    assertThat(service.listSourceItemsForPacket(ORG_A, packet.informationPacketId()))
        .containsExactly(sourceItem);
    assertThat(service.listSourceItemsForPacket(ORG_B, packet.informationPacketId())).isEmpty();
  }

  @Test
  void sourceItemAndInformationPacketAreNotCanonicalFacts() {
    assertThat(recordComponentNames(SourceItem.class))
        .doesNotContain("candidateId", "candidateProfileId", "canonicalWriteAllowed",
            "claimLedgerItemId", "reviewEventId", "workflowEventId");
    assertThat(recordComponentNames(InformationPacket.class))
        .doesNotContain("candidateProfileId", "canonicalWriteAllowed", "claimLedgerItemId",
            "reviewEventId", "workflowEventId");
  }

  @Test
  void governedIntakeServiceDoesNotCallClaimLedgerReviewWorkflowOrCanonicalWriteBoundaries() {
    assertThat(publicDeclaredMethodNames(GovernedIntakeService.class))
        .containsExactly("attachSourceItemToPacket", "createInformationPacket",
            "findInformationPacket", "findSourceItem", "listSourceItemsForPacket",
            "registerSourceItem");
    assertThat(nonStaticFieldTypeNames(GovernedIntakeService.class))
        .containsExactlyInAnyOrder("SourceItemPersistencePort", "InformationPacketPersistencePort");
    assertThat(allDeclaredNames(GovernedIntakeService.class))
        .noneMatch(this::looksLikeForbiddenBoundaryOrWorkflowEngine);
  }

  private static GovernedIntakeService service(
      SourceItemPersistencePort sourcePort,
      InformationPacketPersistencePort packetPort) {
    return new GovernedIntakeService(sourcePort, packetPort);
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand() {
    return SourceItemRegistrationCommand.builder()
        .organizationId(ORG_A)
        .sourceType(SourceItemType.CV)
        .origin(SourceItemOrigin.CONSULTANT_UPLOAD)
        .title("Senior DV engineer CV")
        .contentHash("sha256:source-item-5a")
        .externalRef("ats:source-5a")
        .storageRef("s3://internal-intake/source-5a")
        .rawRef("vault://source-5a/raw")
        .language("en")
        .uploadedByActorType(ActorRole.CONSULTANT)
        .uploadedByActorId(ACTOR_ID)
        .receivedAt(RECEIVED_AT)
        .metadataJson("{\"channel\":\"consultant_upload\"}")
        .status(SourceItemStatus.REGISTERED);
  }

  private static InformationPacketCreateCommand.Builder packetCommand() {
    return InformationPacketCreateCommand.builder()
        .organizationId(ORG_A)
        .packetType(InformationPacketType.CANDIDATE)
        .intendedEntityType(IntendedEntityType.CANDIDATE)
        .intendedEntityId(null)
        .createdByActorType(ActorRole.CONSULTANT)
        .createdByActorId(ACTOR_ID)
        .processingStatus(InformationPacketStatus.CREATED)
        .notes("candidate intake packet")
        .metadataJson("{\"intake\":\"manual\"}");
  }

  private boolean looksLikeForbiddenBoundaryOrWorkflowEngine(String name) {
    String normalized = normalized(name);
    return normalized.contains("canonicalwrite")
        || normalized.contains("claimledger")
        || normalized.contains("reviewevent")
        || normalized.contains("workflowevent")
        || normalized.contains("workflowengine")
        || normalized.contains("transitionlegality")
        || normalized.contains("candidateprofile");
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

  private static List<String> recordComponentNames(Class<? extends Record> recordType) {
    return Stream.of(recordType.getRecordComponents())
        .map(RecordComponent::getName)
        .toList();
  }

  private static String normalized(String value) {
    return value.toLowerCase(Locale.ROOT).replace("_", "");
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static final class InMemorySourceItemPort implements SourceItemPersistencePort {
    private final Map<SourceItemId, SourceItem> sourceItems = new LinkedHashMap<>();
    private int sequence;

    @Override
    public SourceItem append(SourceItemRegistrationCommand command) {
      SourceItem sourceItem = new SourceItem(
          new SourceItemId(uuid("00000000-0000-0000-0000-0000000501"
              + String.format("%02d", ++sequence))),
          command.organizationId(),
          command.sourceType(),
          command.origin(),
          command.title(),
          command.contentHash(),
          command.externalRef(),
          command.storageRef(),
          command.rawRef(),
          command.language(),
          command.uploadedByActorType(),
          command.uploadedByActorId(),
          command.receivedAt(),
          command.receivedAt(),
          command.metadataJson(),
          command.status());
      sourceItems.put(sourceItem.sourceItemId(), sourceItem);
      return sourceItem;
    }

    @Override
    public Optional<SourceItem> findById(UUID organizationId, SourceItemId sourceItemId) {
      return Optional.ofNullable(sourceItems.get(sourceItemId))
          .filter(sourceItem -> sourceItem.organizationId().equals(organizationId));
    }
  }

  private static final class InMemoryInformationPacketPort
      implements InformationPacketPersistencePort {
    private final InMemorySourceItemPort sourceItemPort;
    private final Map<InformationPacketId, InformationPacket> packets = new LinkedHashMap<>();
    private final Map<InformationPacketId, List<SourceItemId>> sourceLinks = new LinkedHashMap<>();
    private int sequence;

    private InMemoryInformationPacketPort(InMemorySourceItemPort sourceItemPort) {
      this.sourceItemPort = sourceItemPort;
    }

    @Override
    public InformationPacket create(InformationPacketCreateCommand command) {
      InformationPacket packet = new InformationPacket(
          new InformationPacketId(uuid("00000000-0000-0000-0000-0000000502"
              + String.format("%02d", ++sequence))),
          command.organizationId(),
          command.packetType(),
          command.intendedEntityType(),
          command.intendedEntityId(),
          command.createdByActorType(),
          command.createdByActorId(),
          command.processingStatus(),
          RECEIVED_AT,
          RECEIVED_AT,
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
      return sourceLinks.getOrDefault(informationPacketId, List.of()).contains(sourceItemId)
          && findById(organizationId, informationPacketId).isPresent();
    }

    @Override
    public void attachSourceItem(
        UUID organizationId,
        InformationPacketId informationPacketId,
        SourceItemId sourceItemId) {
      sourceLinks.computeIfAbsent(informationPacketId, ignored -> new ArrayList<>())
          .add(sourceItemId);
    }

    @Override
    public List<SourceItem> listSourceItems(
        UUID organizationId,
        InformationPacketId informationPacketId) {
      if (findById(organizationId, informationPacketId).isEmpty()) {
        return List.of();
      }
      return sourceLinks.getOrDefault(informationPacketId, List.of()).stream()
          .map(sourceItemId -> sourceItemPort.findById(organizationId, sourceItemId))
          .flatMap(Optional::stream)
          .toList();
    }
  }
}
