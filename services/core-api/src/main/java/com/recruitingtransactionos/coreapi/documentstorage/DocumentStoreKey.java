package com.recruitingtransactionos.coreapi.documentstorage;

import java.util.Objects;
import java.util.UUID;

public record DocumentStoreKey(
    UUID organizationId,
    UUID sourceItemId,
    String contentHashPrefix,
    String originalFilename) {

  public DocumentStoreKey {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
    contentHashPrefix = requireNonBlank(contentHashPrefix, "contentHashPrefix");
    originalFilename = sanitizeFilename(originalFilename);
  }

  public String storagePath() {
    return organizationId + "/" + sourceItemId + "/" + contentHashPrefix + "/"
        + (originalFilename != null ? originalFilename : "file");
  }

  public static DocumentStoreKey fromStorageRef(String storageRef) {
    String normalized = requireNonBlank(storageRef, "storageRef");
    String[] segments = normalized.split("/", 4);
    if (segments.length != 4) {
      throw new IllegalArgumentException("storageRef must have 4 path segments");
    }
    return new DocumentStoreKey(
        UUID.fromString(segments[0]),
        UUID.fromString(segments[1]),
        segments[2],
        segments[3]);
  }

  public static String safeDownloadFilename(String filename) {
    String sanitized = sanitizeFilename(filename);
    return sanitized != null ? sanitized : "file";
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static String sanitizeFilename(String filename) {
    if (filename == null) {
      return null;
    }
    String sanitized = filename.strip()
        .replaceAll("[^a-zA-Z0-9._-]", "_")
        .replaceAll("_+", "_")
        .replaceFirst("^[._-]+", "");
    if (sanitized.length() > 255) {
      sanitized = sanitized.substring(0, 255);
    }
    if (sanitized.isEmpty()) {
      return null;
    }
    return sanitized;
  }
}
