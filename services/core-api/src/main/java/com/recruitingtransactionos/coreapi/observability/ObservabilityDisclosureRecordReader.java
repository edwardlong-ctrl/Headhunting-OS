package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.consentdisclosure.DisclosureRecord;
import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface ObservabilityDisclosureRecordReader {
  Optional<DisclosureRecord> findByRef(UUID organizationId, String disclosureRecordRef);
}
