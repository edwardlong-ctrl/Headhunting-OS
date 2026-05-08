package com.recruitingtransactionos.coreapi.documentstorage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import java.util.Objects;

public final class MinioDocumentStore implements DocumentStore {

  private final MinioClient client;
  private final String bucket;

  public MinioDocumentStore(String endpoint, String bucket, String accessKey, String secretKey) {
    this.bucket = requireText(bucket, "bucket");
    this.client = MinioClient.builder()
        .endpoint(requireText(endpoint, "endpoint"))
        .credentials(requireText(accessKey, "accessKey"), requireText(secretKey, "secretKey"))
        .build();
  }

  @Override
  public void store(DocumentStoreKey key, InputStream content, long contentLength) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(content, "content must not be null");
    try {
      client.putObject(PutObjectArgs.builder()
          .bucket(bucket)
          .object(key.storagePath())
          .stream(content, contentLength, -1)
          .build());
    } catch (Exception exception) {
      throw new DocumentStoreException("Failed to store document at " + key.storagePath(), exception);
    }
  }

  @Override
  public InputStream retrieve(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      return client.getObject(GetObjectArgs.builder()
          .bucket(bucket)
          .object(key.storagePath())
          .build());
    } catch (Exception exception) {
      throw new DocumentStoreException("Failed to retrieve document at " + key.storagePath(), exception);
    }
  }

  @Override
  public boolean exists(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      client.statObject(StatObjectArgs.builder()
          .bucket(bucket)
          .object(key.storagePath())
          .build());
      return true;
    } catch (ErrorResponseException exception) {
      if ("NoSuchKey".equals(exception.errorResponse().code())
          || "NoSuchObject".equals(exception.errorResponse().code())) {
        return false;
      }
      throw new DocumentStoreException("Failed to check document at " + key.storagePath(), exception);
    } catch (Exception exception) {
      throw new DocumentStoreException("Failed to check document at " + key.storagePath(), exception);
    }
  }

  @Override
  public void delete(DocumentStoreKey key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      client.removeObject(RemoveObjectArgs.builder()
          .bucket(bucket)
          .object(key.storagePath())
          .build());
    } catch (Exception exception) {
      throw new DocumentStoreException("Failed to delete document at " + key.storagePath(), exception);
    }
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
