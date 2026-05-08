package com.recruitingtransactionos.coreapi.observability;

import java.util.UUID;

public record ObservabilityDisclosureAuditExportQuery(
    UUID organizationId,
    String disclosureRecordRef) {}
