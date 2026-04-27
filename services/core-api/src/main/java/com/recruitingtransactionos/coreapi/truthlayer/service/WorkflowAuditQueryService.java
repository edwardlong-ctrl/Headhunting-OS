package com.recruitingtransactionos.coreapi.truthlayer.service;

import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditReadPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WorkflowAuditQueryService {

  private final WorkflowAuditReadPort workflowAuditReadPort;

  public WorkflowAuditQueryService(WorkflowAuditReadPort workflowAuditReadPort) {
    this.workflowAuditReadPort = Objects.requireNonNull(workflowAuditReadPort,
        "workflowAuditReadPort must not be null");
  }

  public Optional<WorkflowAuditRecord> findById(
      UUID organizationId,
      WorkflowEventId workflowEventId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(workflowEventId, "workflowEventId must not be null");
    return workflowAuditReadPort.findById(organizationId, workflowEventId);
  }

  public List<WorkflowAuditRecord> search(WorkflowAuditQuery query) {
    validate(query);
    return workflowAuditReadPort.search(query);
  }

  private static void validate(WorkflowAuditQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(query.organizationId(), "organizationId must not be null");
    requireNonBlankIfPresent(query.entityType(), "entityType");
    requireNonBlankIfPresent(query.actionCode(), "actionCode");
    if (query.limit() <= 0) {
      throw new IllegalArgumentException("limit must be greater than zero");
    }
    if (query.limit() > WorkflowAuditQuery.MAX_LIMIT) {
      throw new IllegalArgumentException(
          "limit must be " + WorkflowAuditQuery.MAX_LIMIT + " or fewer");
    }
    if (query.offset() < 0) {
      throw new IllegalArgumentException("offset must not be negative");
    }
    if (query.occurredFrom() != null
        && query.occurredTo() != null
        && query.occurredFrom().isAfter(query.occurredTo())) {
      throw new IllegalArgumentException("occurredFrom must be before or equal to occurredTo");
    }
  }

  private static void requireNonBlankIfPresent(String value, String name) {
    if (value != null && value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }
}
