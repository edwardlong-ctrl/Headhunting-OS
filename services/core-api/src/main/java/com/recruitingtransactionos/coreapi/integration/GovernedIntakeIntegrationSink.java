package com.recruitingtransactionos.coreapi.integration;

import com.recruitingtransactionos.coreapi.governedintake.AttachSourceItemToPacketCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

public final class GovernedIntakeIntegrationSink implements InboundIntegrationSink {

  private final GovernedIntakeService governedIntakeService;

  public GovernedIntakeIntegrationSink(GovernedIntakeService governedIntakeService) {
    this.governedIntakeService = Objects.requireNonNull(
        governedIntakeService, "governedIntakeService must not be null");
  }

  @Override
  public InboundIntakeReceipt acceptForReview(InboundIntegrationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    SourceItem sourceItem = governedIntakeService.registerSourceItem(
        SourceItemRegistrationCommand.builder()
            .organizationId(command.organizationId())
            .sourceType(command.sourceItemType())
            .origin(SourceItemOrigin.SYSTEM_IMPORT)
            .title(command.providerKey() + " " + command.externalRef())
            .contentHash("sha256:" + sha256(command.rawPayloadJson()))
            .externalRef(command.externalRef())
            .rawRef("integration://" + command.providerKey() + "/" + command.externalRef())
            .uploadedByActorType(ActorRole.SYSTEM)
            .uploadedByActorId(command.actorId())
            .receivedAt(Instant.now())
            .metadataJson(command.metadataJson())
            .status(SourceItemStatus.REGISTERED)
            .build());

    InformationPacket packet = governedIntakeService.createInformationPacket(
        InformationPacketCreateCommand.builder()
            .organizationId(command.organizationId())
            .packetType(command.packetType())
            .intendedEntityType(command.intendedEntityType())
            .createdByActorType(ActorRole.SYSTEM)
            .createdByActorId(command.actorId())
            .processingStatus(InformationPacketStatus.CREATED)
            .notes("Integration payload accepted for governed review.")
            .metadataJson(command.metadataJson())
            .build());

    governedIntakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        command.organizationId(),
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    return new InboundIntakeReceipt(
        sourceItem.sourceItemId().value(),
        packet.informationPacketId().value());
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }
}
