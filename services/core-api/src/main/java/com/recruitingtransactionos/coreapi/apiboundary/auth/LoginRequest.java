package com.recruitingtransactionos.coreapi.apiboundary.auth;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import java.util.Locale;
import java.util.regex.Pattern;

public record LoginRequest(
    String organizationId,
    String email,
    String password,
    String portalRole) {

  private static final Pattern SAFE_EMAIL = Pattern.compile(
      "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,24}$");

  public LoginRequest {
    organizationId = optionalNonBlank(organizationId);
    email = ApiBoundaryContractRules.requireNonBlank(email, "email").toLowerCase(Locale.ROOT);
    if (!SAFE_EMAIL.matcher(email).matches()) {
      throw new IllegalArgumentException("Invalid email.");
    }
    password = ApiBoundaryContractRules.requireNonBlank(password, "password");
    if (password.length() < 8 || password.length() > 128 || containsControlCharacter(password)) {
      throw new IllegalArgumentException("Invalid password.");
    }
    portalRole = ApiBoundaryContractRules.requireNonBlank(portalRole, "portalRole")
        .toLowerCase(Locale.ROOT);
  }

  private static String optionalNonBlank(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.strip();
    return normalized.isEmpty() ? null : normalized;
  }

  private static boolean containsControlCharacter(String value) {
    for (int index = 0; index < value.length(); index++) {
      if (Character.isISOControl(value.charAt(index))) {
        return true;
      }
    }
    return false;
  }
}
