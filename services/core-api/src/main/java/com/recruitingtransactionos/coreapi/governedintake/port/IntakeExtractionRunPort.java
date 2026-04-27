package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRun;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntakeExtractionRunPort {

  IntakeExtractionRun save(IntakeExtractionRun run);

  Optional<IntakeExtractionRun> findById(
      UUID organizationId,
      IntakeExtractionRunId extractionRunId);

  List<IntakeExtractionRun> listByInformationPacket(
      UUID organizationId,
      InformationPacketId informationPacketId);
}
