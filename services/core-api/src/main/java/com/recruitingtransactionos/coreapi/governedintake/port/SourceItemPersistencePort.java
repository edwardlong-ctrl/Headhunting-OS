package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import java.util.Optional;
import java.util.UUID;

public interface SourceItemPersistencePort {

  SourceItem append(SourceItemRegistrationCommand command);

  Optional<SourceItem> findById(UUID organizationId, SourceItemId sourceItemId);

  Optional<SourceItem> findByContentHash(UUID organizationId, String contentHash);
}
