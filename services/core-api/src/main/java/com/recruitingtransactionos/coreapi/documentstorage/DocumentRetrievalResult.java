package com.recruitingtransactionos.coreapi.documentstorage;

import java.io.InputStream;
import java.util.Objects;

public record DocumentRetrievalResult(
    InputStream content,
    String mimeType,
    String filename) {

  public DocumentRetrievalResult {
    Objects.requireNonNull(content, "content must not be null");
    mimeType = Objects.requireNonNullElse(mimeType, "application/octet-stream");
    filename = Objects.requireNonNullElse(filename, "file");
  }
}
