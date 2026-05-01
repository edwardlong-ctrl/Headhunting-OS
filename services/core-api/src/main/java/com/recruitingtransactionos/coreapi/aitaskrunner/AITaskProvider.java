package com.recruitingtransactionos.coreapi.aitaskrunner;

public interface AITaskProvider {

  String providerKey();

  AITaskProviderResponse execute(AITaskProviderRequest request);
}
