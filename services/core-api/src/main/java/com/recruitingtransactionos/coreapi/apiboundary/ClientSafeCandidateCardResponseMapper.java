package com.recruitingtransactionos.coreapi.apiboundary;

import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class ClientSafeCandidateCardResponseMapper {

  private ClientSafeCandidateCardResponseMapper() {}

  public static ClientSafeCandidateCardResponse from(ClientSafeCandidateCard card) {
    Objects.requireNonNull(card, "card must not be null");
    return new ClientSafeCandidateCardResponse(
        card.cardId().value(),
        clientAlias(card),
        card.projectionVersion(),
        card.redactionLevel().wireValue(),
        card.generalizedHeadline(),
        card.generalizedRoleFamily(),
        card.generalizedSeniorityBand(),
        card.generalizedLocationRegion(),
        card.safeSummary(),
        card.safeSkillSummary(),
        card.safeEvidenceSummaries(),
        card.safeMatchNarratives());
  }

  private static String clientAlias(ClientSafeCandidateCard card) {
    return "alias-" + aliasSuffix(card.anonymousCandidateRef().value());
  }

  private static String aliasSuffix(String anonymousCandidateRef) {
    byte[] digest = sha256(anonymousCandidateRef);
    return toBase36(digest).substring(0, 8);
  }

  private static byte[] sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
    }
  }

  private static String toBase36(byte[] bytes) {
    java.math.BigInteger positive = new java.math.BigInteger(1, bytes);
    String encoded = positive.toString(36);
    if (encoded.length() >= 8) {
      return encoded;
    }
    return "0".repeat(8 - encoded.length()) + encoded;
  }
}
