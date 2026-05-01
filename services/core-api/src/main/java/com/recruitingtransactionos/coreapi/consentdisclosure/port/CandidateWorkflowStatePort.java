package com.recruitingtransactionos.coreapi.consentdisclosure.port;

import java.time.Instant;
import java.util.UUID;

public interface CandidateWorkflowStatePort {

  void transitionToIdentityDisclosed(
      UUID organizationId,
      String candidateRef,
      Instant disclosedAt);
}
