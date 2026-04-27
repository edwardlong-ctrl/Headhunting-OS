package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketCreateCommand;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InformationPacketPersistencePort {

  InformationPacket create(InformationPacketCreateCommand command);

  Optional<InformationPacket> findById(UUID organizationId, InformationPacketId informationPacketId);

  boolean hasSourceItem(
      UUID organizationId,
      InformationPacketId informationPacketId,
      SourceItemId sourceItemId);

  void attachSourceItem(
      UUID organizationId,
      InformationPacketId informationPacketId,
      SourceItemId sourceItemId);

  List<SourceItem> listSourceItems(UUID organizationId, InformationPacketId informationPacketId);
}
