package com.recruitingtransactionos.coreapi.observability;

import java.util.List;

@FunctionalInterface
public interface ObservabilityReviewEventReader {
  List<ObservabilityReviewEventRecord> search(ObservabilityReviewEventQuery query);
}
