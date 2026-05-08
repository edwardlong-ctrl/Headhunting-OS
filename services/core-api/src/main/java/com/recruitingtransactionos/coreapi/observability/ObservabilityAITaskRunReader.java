package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunRecord;
import java.util.List;

@FunctionalInterface
public interface ObservabilityAITaskRunReader {
  List<AITaskRunRecord> search(ObservabilityAITaskRunQuery query);
}
