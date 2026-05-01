package com.recruitingtransactionos.coreapi.consentdisclosure.port;

import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DisclosureRecordPort {

  DisclosureRecord append(DisclosureRecord disclosureRecord);

  Optional<DisclosureRecord> findByRefAndOrganizationId(
      UUID organizationId,
      String disclosureRecordRef);

  DisclosureRecord transitionToIdentityDisclosed(
      UUID organizationId,
      String disclosureRecordRef,
      WorkflowEventId workflowEventId,
      Instant decidedAt);

  default DisclosureRecord appendIfAbsent(DisclosureRecord disclosureRecord) {
    return findByRefAndOrganizationId(
        disclosureRecord.organizationId(),
        disclosureRecord.disclosureRecordRef())
        .orElseGet(() -> append(disclosureRecord));
  }
}
