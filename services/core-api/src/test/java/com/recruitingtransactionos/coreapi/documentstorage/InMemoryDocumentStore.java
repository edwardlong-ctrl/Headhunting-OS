package com.recruitingtransactionos.coreapi.documentstorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDocumentStore implements DocumentStore {

  private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

  @Override
  public void store(DocumentStoreKey key, InputStream content, long contentLength) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(content, "content must not be null");
    try {
      byte[] bytes = content.readAllBytes();
      storage.put(key.storagePath(), bytes);
    } catch (IOException e) {
      throw new DocumentStoreException("Failed to store document", e);
    }
  }

  @Override
  public InputStream retrieve(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    byte[] bytes = storage.get(key.storagePath());
    if (bytes == null) {
      throw new DocumentStoreException("Document not found for key: " + key.storagePath());
    }
    return new ByteArrayInputStream(bytes);
  }

  @Override
  public boolean exists(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    return storage.containsKey(key.storagePath());
  }

  @Override
  public void delete(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    storage.remove(key.storagePath());
  }
}
