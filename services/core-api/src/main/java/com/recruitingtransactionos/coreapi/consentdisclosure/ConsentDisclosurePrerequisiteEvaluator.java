package com.recruitingtransactionos.coreapi.consentdisclosure;

import java.util.Optional;

public interface ConsentDisclosurePrerequisiteEvaluator {

  ConsentDisclosurePrerequisites evaluate(
      ConsentDisclosureServiceRequest request,
      Optional<UnlockDecision> unlockDecision,
      Optional<DisclosureRecord> disclosureRecord);
}
