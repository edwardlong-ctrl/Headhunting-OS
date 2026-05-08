package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import java.util.Optional;
import java.util.UUID;

public interface ObservabilityConsentRecordReader {

  Optional<ConsentRecord> findByRef(UUID organizationId, String consentRecordRef);
}
