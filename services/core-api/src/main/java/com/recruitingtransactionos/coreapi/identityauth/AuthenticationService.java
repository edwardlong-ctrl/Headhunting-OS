package com.recruitingtransactionos.coreapi.identityauth;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public final class AuthenticationService {

  private final IdentityAuthenticationPort port;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final Clock clock;

  public AuthenticationService(
      IdentityAuthenticationPort port,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      Clock clock) {
    this.port = Objects.requireNonNull(port, "port must not be null");
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    this.jwtService = Objects.requireNonNull(jwtService, "jwtService must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  public AuthenticatedSession login(
      UUID organizationId,
      String email,
      String password,
      PortalRole requestedRole) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    String normalizedEmail = requireNonBlank(email, "email").toLowerCase();
    String normalizedPassword = requireNonBlank(password, "password");
    PortalRole role = requireLoginRole(requestedRole);

    IdentityUserAccount account = port.findByOrganizationIdAndEmail(organizationId, normalizedEmail)
        .orElseThrow(AuthenticationFailureException::invalidCredentials);
    if (!account.isActiveForLogin()) {
      throw AuthenticationFailureException.accountInactive();
    }
    if (!account.hasPasswordHash()
        || !passwordEncoder.matches(normalizedPassword, account.passwordHash())) {
      throw AuthenticationFailureException.invalidCredentials();
    }
    if (!port.hasActiveRoleAssignment(organizationId, account.userAccountId(), role)) {
      throw AuthenticationFailureException.roleAssignmentRequired();
    }

    Instant now = Instant.now(clock);
    UUID sessionId = UUID.randomUUID();
    String refreshToken = JwtService.generateOpaqueToken();
    String refreshTokenHash = hashToken(refreshToken);
    IdentityAuthSession session = new IdentityAuthSession(
        sessionId,
        organizationId,
        account.userAccountId(),
        role,
        refreshTokenHash,
        jwtService.refreshTokenExpiresAt(now),
        null,
        now,
        now,
        1);
    port.createSession(session);
    port.updateLastLoginAt(organizationId, account.userAccountId(), now);

    RtoAuthenticatedPrincipal principal = new RtoAuthenticatedPrincipal(
        account.userAccountId(),
        organizationId,
        role,
        account.displayName(),
        sessionId);
    return new AuthenticatedSession(
        organizationId,
        account.userAccountId(),
        account.displayName(),
        role,
        jwtService.issueAccessToken(principal, now),
        refreshToken,
        jwtService.accessTokenExpiresAt(now),
        session.expiresAt());
  }

  public AuthenticatedSession refresh(String refreshToken) {
    String rawRefreshToken = requireNonBlank(refreshToken, "refreshToken");
    Instant now = Instant.now(clock);
    String refreshTokenHash = hashToken(rawRefreshToken);
    IdentityAuthSession session = port.findActiveSessionByRefreshTokenHash(refreshTokenHash, now)
        .orElseThrow(AuthenticationFailureException::invalidRefreshToken);
    IdentityUserAccount account = port.findByOrganizationIdAndUserAccountId(
            session.organizationId(), session.userAccountId())
        .orElseThrow(AuthenticationFailureException::invalidRefreshToken);
    if (!account.isActiveForLogin()) {
      throw AuthenticationFailureException.accountInactive();
    }
    if (!port.hasActiveRoleAssignment(
        session.organizationId(), session.userAccountId(), session.portalRole())) {
      throw AuthenticationFailureException.roleAssignmentRequired();
    }

    String rotatedRefreshToken = JwtService.generateOpaqueToken();
    IdentityAuthSession rotated = port.rotateSessionAtomically(
            refreshTokenHash,
            new IdentityAuthSession(
                UUID.randomUUID(),
                session.organizationId(),
                session.userAccountId(),
                session.portalRole(),
                hashToken(rotatedRefreshToken),
                jwtService.refreshTokenExpiresAt(now),
                null,
                now,
                now,
                1),
            now)
        .orElseThrow(AuthenticationFailureException::invalidRefreshToken);

    RtoAuthenticatedPrincipal principal = new RtoAuthenticatedPrincipal(
        account.userAccountId(),
        account.organizationId(),
        rotated.portalRole(),
        account.displayName(),
        rotated.sessionId());
    return new AuthenticatedSession(
        account.organizationId(),
        account.userAccountId(),
        account.displayName(),
        rotated.portalRole(),
        jwtService.issueAccessToken(principal, now),
        rotatedRefreshToken,
        jwtService.accessTokenExpiresAt(now),
        rotated.expiresAt());
  }

  public LoggedOutSession logout(String refreshToken) {
    String rawRefreshToken = requireNonBlank(refreshToken, "refreshToken");
    Instant now = Instant.now(clock);
    port.revokeSessionByRefreshTokenHash(hashToken(rawRefreshToken), now);
    return new LoggedOutSession("logged_out", now);
  }

  private static PortalRole requireLoginRole(PortalRole portalRole) {
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    if (portalRole == PortalRole.UNKNOWN
        || portalRole == PortalRole.SYSTEM
        || portalRole == PortalRole.AI_ASSISTANT) {
      throw new IllegalArgumentException("Invalid portal role.");
    }
    return portalRole;
  }

  private static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.strip();
  }

  static String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }

  public record AuthenticatedSession(
      UUID organizationId,
      UUID userAccountId,
      String displayName,
      PortalRole portalRole,
      String accessToken,
      String refreshToken,
      Instant accessTokenExpiresAt,
      Instant refreshTokenExpiresAt) {}

  public record LoggedOutSession(String status, Instant loggedOutAt) {}
}
