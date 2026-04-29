package com.recruitingtransactionos.coreapi.consentdisclosure.port;

import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecision;
import java.util.Optional;
import java.util.UUID;

public interface UnlockDecisionPort {

  UnlockDecision append(UnlockDecision unlockDecision);

  Optional<UnlockDecision> findByRefAndOrganizationId(UUID organizationId, String unlockDecisionRef);
}
