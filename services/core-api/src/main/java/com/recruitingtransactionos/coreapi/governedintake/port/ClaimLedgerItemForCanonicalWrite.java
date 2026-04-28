package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.util.Objects;
import java.util.UUID;

public record ClaimLedgerItemForCanonicalWrite(
    ClaimId claimLedgerItemId,
    UUID organizationId,
    EntityRef targetEntity,
    String targetFieldPath,
    ClaimType claimType,
    AssertionStrength assertionStrength,
    VerificationStatus verificationStatus,
    ClientShareability clientShareability,
    boolean canonicalWriteAllowed,
    String claimValueText,
    SourceSpanRef sourceSpanReference) {

  public ClaimLedgerItemForCanonicalWrite {
    Objects.requireNonNull(claimLedgerItemId, "claimLedgerItemId must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(targetEntity, "targetEntity must not be null");
    targetFieldPath = requireNonBlank(targetFieldPath, "targetFieldPath");
    Objects.requireNonNull(claimType, "claimType must not be null");
    Objects.requireNonNull(assertionStrength, "assertionStrength must not be null");
    Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
    Objects.requireNonNull(clientShareability, "clientShareability must not be null");
    claimValueText = optionalNonBlank(claimValueText, "claimValueText");
    Objects.requireNonNull(sourceSpanReference, "sourceSpanReference must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  private static String optionalNonBlank(String value, String name) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }

  public static final class Builder {
    private ClaimId claimLedgerItemId;
    private UUID organizationId;
    private EntityRef targetEntity;
    private String targetFieldPath;
    private ClaimType claimType;
    private AssertionStrength assertionStrength;
    private VerificationStatus verificationStatus;
    private ClientShareability clientShareability;
    private boolean canonicalWriteAllowed;
    private String claimValueText;
    private SourceSpanRef sourceSpanReference;

    private Builder() {
    }

    public Builder claimLedgerItemId(ClaimId claimLedgerItemId) {
      this.claimLedgerItemId = claimLedgerItemId;
      return this;
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder targetEntity(EntityRef targetEntity) {
      this.targetEntity = targetEntity;
      return this;
    }

    public Builder targetFieldPath(String targetFieldPath) {
      this.targetFieldPath = targetFieldPath;
      return this;
    }

    public Builder claimType(ClaimType claimType) {
      this.claimType = claimType;
      return this;
    }

    public Builder assertionStrength(AssertionStrength assertionStrength) {
      this.assertionStrength = assertionStrength;
      return this;
    }

    public Builder verificationStatus(VerificationStatus verificationStatus) {
      this.verificationStatus = verificationStatus;
      return this;
    }

    public Builder clientShareability(ClientShareability clientShareability) {
      this.clientShareability = clientShareability;
      return this;
    }

    public Builder canonicalWriteAllowed(boolean canonicalWriteAllowed) {
      this.canonicalWriteAllowed = canonicalWriteAllowed;
      return this;
    }

    public Builder claimValueText(String claimValueText) {
      this.claimValueText = claimValueText;
      return this;
    }

    public Builder sourceSpanReference(SourceSpanRef sourceSpanReference) {
      this.sourceSpanReference = sourceSpanReference;
      return this;
    }

    public ClaimLedgerItemForCanonicalWrite build() {
      return new ClaimLedgerItemForCanonicalWrite(
          claimLedgerItemId,
          organizationId,
          targetEntity,
          targetFieldPath,
          claimType,
          assertionStrength,
          verificationStatus,
          clientShareability,
          canonicalWriteAllowed,
          claimValueText,
          sourceSpanReference);
    }
  }
}
