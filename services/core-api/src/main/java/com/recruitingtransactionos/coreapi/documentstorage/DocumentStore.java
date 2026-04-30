package com.recruitingtransactionos.coreapi.documentstorage;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStore {

  void store(DocumentStoreKey key, InputStream content, long contentLength);

  InputStream retrieve(DocumentStoreKey key);

  boolean exists(DocumentStoreKey key);

  void delete(DocumentStoreKey key);
}
