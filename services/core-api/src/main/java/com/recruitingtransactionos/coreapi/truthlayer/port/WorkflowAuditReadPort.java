package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowAuditReadPort {

  Optional<WorkflowAuditRecord> findById(UUID organizationId, WorkflowEventId workflowEventId);

  List<WorkflowAuditRecord> search(WorkflowAuditQuery query);
}
