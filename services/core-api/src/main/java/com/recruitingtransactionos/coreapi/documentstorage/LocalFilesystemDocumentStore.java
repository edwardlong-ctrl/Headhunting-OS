package com.recruitingtransactionos.coreapi.documentstorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class LocalFilesystemDocumentStore implements DocumentStore {

  private final Path rootDirectory;

  public LocalFilesystemDocumentStore(Path rootDirectory) {
    this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory must not be null")
        .toAbsolutePath()
        .normalize();
  }

  @Override
  public void store(DocumentStoreKey key, InputStream content, long contentLength) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Path path = resolve(key);
    try {
      Files.createDirectories(path.getParent());
      Files.copy(content, path);
    } catch (IOException exception) {
      throw new DocumentStoreException("Failed to store document at " + path, exception);
    }
  }

  @Override
  public InputStream retrieve(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    Path path = resolve(key);
    try {
      return Files.newInputStream(path);
    } catch (IOException exception) {
      throw new DocumentStoreException("Failed to retrieve document at " + path, exception);
    }
  }

  @Override
  public boolean exists(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    return Files.exists(resolve(key));
  }

  @Override
  public void delete(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    Path path = resolve(key);
    try {
      Files.deleteIfExists(path);
    } catch (IOException exception) {
      throw new DocumentStoreException("Failed to delete document at " + path, exception);
    }
  }

  private Path resolve(DocumentStoreKey key) {
    Path resolved = rootDirectory.resolve(key.storagePath()).normalize();
    if (!resolved.startsWith(rootDirectory)) {
      throw new DocumentStoreException("Resolved document path escapes configured root");
    }
    return resolved;
  }
}
