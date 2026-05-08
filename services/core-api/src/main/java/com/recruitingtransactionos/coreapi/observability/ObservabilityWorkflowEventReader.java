package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import java.util.List;

@FunctionalInterface
public interface ObservabilityWorkflowEventReader {
  List<WorkflowAuditRecord> search(ObservabilityWorkflowEventQuery query);
}
