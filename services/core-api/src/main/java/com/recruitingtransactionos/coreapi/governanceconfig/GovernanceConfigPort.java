package com.recruitingtransactionos.coreapi.governanceconfig;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GovernanceConfigPort {

  Optional<GovernanceConfigRecord> findByTypeAndKey(
      UUID organizationId,
      String configType,
      String configKey);

  List<GovernanceConfigRecord> listByType(UUID organizationId, String configType);

  GovernanceConfigRecord upsert(
      UUID organizationId,
      String configType,
      String configKey,
      String payloadJson,
      boolean enabled,
      UUID actorUserId,
      PortalRole actorRole);
}
