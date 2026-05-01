package com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity;

import com.recruitingtransactionos.coreapi.matching.AuthenticityRiskLevel;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record AuthenticityRiskAssessorOutput(
    String authenticityRisk,
    int specificityScore,
    boolean independentEvidenceGap,
    List<String> flags) {

  public AuthenticityRiskAssessorOutput {
    authenticityRisk = requireNonBlank(authenticityRisk, "authenticityRisk").toLowerCase(Locale.ROOT);
    if (!("low".equals(authenticityRisk) || "medium".equals(authenticityRisk) || "high".equals(authenticityRisk))) {
      throw new IllegalArgumentException("authenticityRisk must be low, medium, or high");
    }
    if (specificityScore < 0 || specificityScore > 100) {
      throw new IllegalArgumentException("specificityScore must be between 0 and 100");
    }
    flags = List.copyOf(flags == null ? List.of() : flags);
    flags.forEach(flag -> {
      if (flag == null || flag.isBlank()) {
        throw new IllegalArgumentException("flags must not contain blank values");
      }
    });
  }

  public AuthenticityRiskLevel toRiskLevel() {
    return switch (authenticityRisk) {
      case "low" -> AuthenticityRiskLevel.LOW;
      case "medium" -> AuthenticityRiskLevel.MEDIUM;
      case "high" -> AuthenticityRiskLevel.HIGH;
      default -> throw new IllegalStateException("unsupported authenticity risk: " + authenticityRisk);
    };
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
