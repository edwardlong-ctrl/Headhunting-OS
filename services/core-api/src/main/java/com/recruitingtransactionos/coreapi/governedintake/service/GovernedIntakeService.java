package com.recruitingtransactionos.coreapi.governedintake.service;

import com.recruitingtransactionos.coreapi.governedintake.AttachSourceItemToPacketCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.port.InformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.port.SourceItemPersistencePort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GovernedIntakeService {

  private final SourceItemPersistencePort sourceItemPersistencePort;
  private final InformationPacketPersistencePort informationPacketPersistencePort;

  public GovernedIntakeService(
      SourceItemPersistencePort sourceItemPersistencePort,
      InformationPacketPersistencePort informationPacketPersistencePort) {
    this.sourceItemPersistencePort = Objects.requireNonNull(sourceItemPersistencePort,
        "sourceItemPersistencePort must not be null");
    this.informationPacketPersistencePort = Objects.requireNonNull(informationPacketPersistencePort,
        "informationPacketPersistencePort must not be null");
  }

  public SourceItem registerSourceItem(SourceItemRegistrationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return sourceItemPersistencePort.append(command);
  }

  public InformationPacket createInformationPacket(InformationPacketCreateCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    return informationPacketPersistencePort.create(command);
  }

  public void attachSourceItemToPacket(AttachSourceItemToPacketCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    if (findInformationPacket(command.organizationId(), command.informationPacketId()).isEmpty()) {
      throw new IllegalArgumentException("information packet not found in organization");
    }
    if (findSourceItem(command.organizationId(), command.sourceItemId()).isEmpty()) {
      throw new IllegalArgumentException("source item not found in organization");
    }
    if (informationPacketPersistencePort.hasSourceItem(
        command.organizationId(),
        command.informationPacketId(),
        command.sourceItemId())) {
      throw new IllegalArgumentException("source item already attached to information packet");
    }
    informationPacketPersistencePort.attachSourceItem(
        command.organizationId(),
        command.informationPacketId(),
        command.sourceItemId());
  }

  public Optional<SourceItem> findSourceItem(UUID organizationId, SourceItemId sourceItemId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    return sourceItemPersistencePort.findById(organizationId, sourceItemId);
  }

  public Optional<InformationPacket> findInformationPacket(
      UUID organizationId,
      InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    return informationPacketPersistencePort.findById(organizationId, informationPacketId);
  }

  public List<SourceItem> listSourceItemsForPacket(
      UUID organizationId,
      InformationPacketId informationPacketId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(informationPacketId, "informationPacketId must not be null");
    return informationPacketPersistencePort.listSourceItems(organizationId, informationPacketId);
  }
}
