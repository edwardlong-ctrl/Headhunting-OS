package com.recruitingtransactionos.coreapi.candidatedocument;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CandidateDocument(
    CandidateDocumentId candidateDocumentId,
    UUID organizationId,
    CandidateId candidateId,
    String documentType,
    String title,
    String storageRef,
    String contentHash,
    UUID sourceItemId,
    String language,
    CandidateDocumentStatus status,
    UUID supersededByDocumentId,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public CandidateDocument {
    Objects.requireNonNull(candidateDocumentId, "candidateDocumentId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(documentType, "documentType must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (version < 1) {
      throw new IllegalArgumentException("version must be >= 1");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private CandidateDocumentId candidateDocumentId;
    private UUID organizationId;
    private CandidateId candidateId;
    private String documentType;
    private String title;
    private String storageRef;
    private String contentHash;
    private UUID sourceItemId;
    private String language;
    private CandidateDocumentStatus status;
    private UUID supersededByDocumentId;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder candidateDocumentId(CandidateDocumentId id) { this.candidateDocumentId = id; return this; }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder candidateId(CandidateId candidateId) { this.candidateId = candidateId; return this; }
    public Builder documentType(String type) { this.documentType = type; return this; }
    public Builder title(String title) { this.title = title; return this; }
    public Builder storageRef(String ref) { this.storageRef = ref; return this; }
    public Builder contentHash(String hash) { this.contentHash = hash; return this; }
    public Builder sourceItemId(UUID id) { this.sourceItemId = id; return this; }
    public Builder language(String language) { this.language = language; return this; }
    public Builder status(CandidateDocumentStatus status) { this.status = status; return this; }
    public Builder supersededByDocumentId(UUID id) { this.supersededByDocumentId = id; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public CandidateDocument build() {
      return new CandidateDocument(candidateDocumentId, organizationId, candidateId,
          documentType, title, storageRef, contentHash, sourceItemId, language, status,
          supersededByDocumentId, metadata, createdAt, updatedAt, version);
    }
  }
}
