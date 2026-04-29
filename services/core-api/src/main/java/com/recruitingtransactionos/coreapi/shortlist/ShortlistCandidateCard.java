package com.recruitingtransactionos.coreapi.shortlist;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ShortlistCandidateCard(
    ShortlistCandidateCardId shortlistCandidateCardId,
    UUID organizationId,
    ShortlistId shortlistId,
    UUID anonymousCandidateCardId,
    CandidateId candidateId,
    UUID candidateProfileId,
    int sortOrder,
    ShortlistCandidateCardStatus status,
    UUID matchReportId,
    String clientNotes,
    String metadata,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public ShortlistCandidateCard {
    Objects.requireNonNull(shortlistCandidateCardId,
        "shortlistCandidateCardId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(shortlistId, "shortlistId must not be null");
    Objects.requireNonNull(anonymousCandidateCardId,
        "anonymousCandidateCardId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(candidateProfileId, "candidateProfileId must not be null");
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
    private ShortlistCandidateCardId shortlistCandidateCardId;
    private UUID organizationId;
    private ShortlistId shortlistId;
    private UUID anonymousCandidateCardId;
    private CandidateId candidateId;
    private UUID candidateProfileId;
    private int sortOrder;
    private ShortlistCandidateCardStatus status;
    private UUID matchReportId;
    private String clientNotes;
    private String metadata = "{}";
    private Instant createdAt;
    private Instant updatedAt;
    private int version = 1;

    private Builder() {
    }

    public Builder shortlistCandidateCardId(ShortlistCandidateCardId id) {
      this.shortlistCandidateCardId = id; return this;
    }
    public Builder organizationId(UUID orgId) { this.organizationId = orgId; return this; }
    public Builder shortlistId(ShortlistId id) { this.shortlistId = id; return this; }
    public Builder anonymousCandidateCardId(UUID id) {
      this.anonymousCandidateCardId = id; return this;
    }
    public Builder candidateId(CandidateId id) { this.candidateId = id; return this; }
    public Builder candidateProfileId(UUID id) { this.candidateProfileId = id; return this; }
    public Builder sortOrder(int order) { this.sortOrder = order; return this; }
    public Builder status(ShortlistCandidateCardStatus status) {
      this.status = status; return this;
    }
    public Builder matchReportId(UUID id) { this.matchReportId = id; return this; }
    public Builder clientNotes(String notes) { this.clientNotes = notes; return this; }
    public Builder metadata(String metadata) { this.metadata = metadata; return this; }
    public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
    public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
    public Builder version(int version) { this.version = version; return this; }

    public ShortlistCandidateCard build() {
      return new ShortlistCandidateCard(shortlistCandidateCardId, organizationId, shortlistId,
          anonymousCandidateCardId, candidateId, candidateProfileId, sortOrder, status,
          matchReportId, clientNotes, metadata, createdAt, updatedAt, version);
    }
  }
}
