package com.recruitingtransactionos.coreapi.consentdisclosure.port;

import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import java.util.Optional;
import java.util.UUID;

public interface DisclosureRecordPort {

  DisclosureRecord append(DisclosureRecord disclosureRecord);

  Optional<DisclosureRecord> findByRefAndOrganizationId(
      UUID organizationId,
      String disclosureRecordRef);

  default DisclosureRecord appendIfAbsent(DisclosureRecord disclosureRecord) {
    return findByRefAndOrganizationId(
        disclosureRecord.organizationId(),
        disclosureRecord.disclosureRecordRef())
        .orElseGet(() -> append(disclosureRecord));
  }
}
