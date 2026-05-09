package com.recruitingtransactionos.coreapi.workflowautomation;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Instant;
import java.util.UUID;

public record WorkflowAutomationAssessment(
    UUID workflowEventId,
    String entityType,
    UUID entityId,
    String actionCode,
    String workflowFamily,
    PortalRole ownerRole,
    Instant occurredAt,
    Instant dueAt,
    Instant reminderAt,
    Instant escalationAt,
    WorkflowAutomationStatus status,
    String blockerCode,
    String nextBestAction) {}
