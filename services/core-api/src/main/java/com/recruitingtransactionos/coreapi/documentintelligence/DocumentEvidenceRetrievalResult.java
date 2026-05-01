package com.recruitingtransactionos.coreapi.documentintelligence;

import java.util.List;
import java.util.Objects;

public record DocumentEvidenceRetrievalResult(
    ParsedDocument parsedDocument,
    List<DocumentEvidenceHit> hits) {

  public DocumentEvidenceRetrievalResult {
    Objects.requireNonNull(parsedDocument, "parsedDocument must not be null");
    hits = List.copyOf(Objects.requireNonNull(hits, "hits must not be null"));
  }
}
