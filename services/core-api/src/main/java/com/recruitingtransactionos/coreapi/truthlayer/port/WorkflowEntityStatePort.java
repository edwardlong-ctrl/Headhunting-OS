package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowEntityStatePort {
  Optional<String> getCurrentStateJson(UUID organizationId, String entityNamespace, String entityType, UUID entityId);
}
