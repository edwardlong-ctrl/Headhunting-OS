package com.recruitingtransactionos.coreapi.aitaskrunner;

import java.util.UUID;

public interface AITaskDefinitionCatalog {

  void ensureRegistered(
      UUID organizationId,
      AITaskDefinition definition,
      AITaskModelRoute modelRoute);
}
