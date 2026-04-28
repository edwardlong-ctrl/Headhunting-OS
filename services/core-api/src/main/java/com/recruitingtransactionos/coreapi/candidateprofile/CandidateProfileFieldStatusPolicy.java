package com.recruitingtransactionos.coreapi.candidateprofile;

import java.util.Objects;

public final class CandidateProfileFieldStatusPolicy {

  private CandidateProfileFieldStatusPolicy() {
  }

  public static CandidateProfileFieldStatus bulkApprovalResult() {
    return CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED;
  }

  public static boolean isVerifiedFactEligible(CandidateProfileFieldStatus status) {
    Objects.requireNonNull(status, "status must not be null");
    return status == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED
        || status == CandidateProfileFieldStatus.EXTERNAL_VERIFIED;
  }

  public static boolean isClientFactEligible(CandidateProfileFieldStatus status) {
    return isVerifiedFactEligible(status);
  }

  public static boolean isCanonicalFieldCandidateInLaterTasks(CandidateProfileFieldStatus status) {
    Objects.requireNonNull(status, "status must not be null");
    return switch (status) {
      case HUMAN_ACKNOWLEDGED, CONSULTANT_ATTESTED, CANDIDATE_CONFIRMED, EXTERNAL_VERIFIED,
          LIKELY_CURRENT -> true;
      case AI_EXTRACTED, SYSTEM_INFERENCE, CONFLICTING, NEEDS_CONFIRMATION, STALE,
          UNVERIFIED -> false;
    };
  }

  public static boolean blocksClientVisibleFactStatement(CandidateProfileFieldStatus status) {
    Objects.requireNonNull(status, "status must not be null");
    return !isClientFactEligible(status);
  }

  public static boolean isTransactionReadyEligible(CandidateProfileFieldStatus status) {
    Objects.requireNonNull(status, "status must not be null");
    return switch (status) {
      case CONSULTANT_ATTESTED, CANDIDATE_CONFIRMED, EXTERNAL_VERIFIED -> true;
      case AI_EXTRACTED, HUMAN_ACKNOWLEDGED, SYSTEM_INFERENCE, CONFLICTING, NEEDS_CONFIRMATION,
          STALE, UNVERIFIED, LIKELY_CURRENT -> false;
    };
  }

  public static boolean requiresReview(CandidateProfileFieldStatus status) {
    Objects.requireNonNull(status, "status must not be null");
    return switch (status) {
      case CANDIDATE_CONFIRMED, EXTERNAL_VERIFIED -> false;
      case AI_EXTRACTED, HUMAN_ACKNOWLEDGED, CONSULTANT_ATTESTED, SYSTEM_INFERENCE, CONFLICTING,
          NEEDS_CONFIRMATION, STALE, UNVERIFIED, LIKELY_CURRENT -> true;
    };
  }
}
