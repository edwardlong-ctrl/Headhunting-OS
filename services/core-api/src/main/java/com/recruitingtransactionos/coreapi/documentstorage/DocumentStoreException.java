package com.recruitingtransactionos.coreapi.documentstorage;

public final class DocumentStoreException extends RuntimeException {

  public DocumentStoreException(String message) {
    super(message);
  }

  public DocumentStoreException(String message, Throwable cause) {
    super(message, cause);
  }
}
