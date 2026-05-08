package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.consentdisclosure.UnlockDecision;
import java.util.Optional;
import java.util.UUID;

public interface ObservabilityUnlockDecisionReader {

  Optional<UnlockDecision> findByRef(UUID organizationId, String unlockDecisionRef);
}
