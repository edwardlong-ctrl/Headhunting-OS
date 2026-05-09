package com.recruitingtransactionos.coreapi.workflowautomation;

import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditRecord;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record WorkflowTimelineExport(String format, String content) {

  public static WorkflowTimelineExport from(
      List<WorkflowAuditRecord> records,
      List<WorkflowAutomationAssessment> assessments) {
    Map<UUID, WorkflowAutomationAssessment> assessmentByEvent = assessments.stream()
        .collect(Collectors.toMap(WorkflowAutomationAssessment::workflowEventId, Function.identity()));
    StringBuilder builder = new StringBuilder();
    builder.append("workflow_event_id,entity_type,entity_id,action_code,occurred_at,due_at,automation_status,next_best_action\n");
    for (WorkflowAuditRecord record : records) {
      WorkflowAutomationAssessment assessment = assessmentByEvent.get(record.workflowEventId().value());
      builder.append(csv(record.workflowEventId().value().toString())).append(',')
          .append(csv(record.entityType())).append(',')
          .append(csv(record.entityId().toString())).append(',')
          .append(csv(record.actionCode())).append(',')
          .append(csv(record.occurredAt().toString())).append(',')
          .append(csv(assessment == null ? "" : assessment.dueAt().toString())).append(',')
          .append(csv(assessment == null ? "" : assessment.status().name())).append(',')
          .append(csv(assessment == null ? "" : assessment.nextBestAction()))
          .append('\n');
    }
    return new WorkflowTimelineExport("csv", builder.toString());
  }

  private static String csv(String value) {
    String safe = value == null ? "" : value;
    if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
      return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    return safe;
  }
}
