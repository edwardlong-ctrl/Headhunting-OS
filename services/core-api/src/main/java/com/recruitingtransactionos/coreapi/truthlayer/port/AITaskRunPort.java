package com.recruitingtransactionos.coreapi.truthlayer.port;

public interface AITaskRunPort {

  AITaskRunAppendResult append(AITaskRunAppendCommand command);
}
