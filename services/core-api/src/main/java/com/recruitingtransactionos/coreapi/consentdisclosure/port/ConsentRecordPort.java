package com.recruitingtransactionos.coreapi.consentdisclosure.port;

import com.recruitingtransactionos.coreapi.consentdisclosure.ConsentRecord;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordPort {

  ConsentRecord append(ConsentRecord consentRecord);

  Optional<ConsentRecord> findByRefAndOrganizationId(UUID organizationId, String consentRecordRef);
}
