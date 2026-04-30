package com.recruitingtransactionos.coreapi.identityauth;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;

public final class JwtService {

  private final String secret;
  private final String issuer;
  private final Duration accessTokenTtl;
  private final Duration refreshTokenTtl;

  public JwtService(
      @Value("${rto.auth.jwt.secret:}") String secret,
      @Value("${rto.auth.jwt.issuer:recruiting-transaction-os}") String issuer,
      @Value("${rto.auth.jwt.access-token-ttl-seconds:1800}") long accessTokenTtlSeconds,
      @Value("${rto.auth.jwt.refresh-token-ttl-seconds:604800}") long refreshTokenTtlSeconds) {
    this.secret = Objects.requireNonNull(secret, "secret must not be null").strip();
    this.issuer = Objects.requireNonNull(issuer, "issuer must not be null").strip();
    this.accessTokenTtl = Duration.ofSeconds(accessTokenTtlSeconds);
    this.refreshTokenTtl = Duration.ofSeconds(refreshTokenTtlSeconds);
  }

  public boolean isConfigured() {
    return !secret.isBlank();
  }

  public Duration accessTokenTtl() {
    return accessTokenTtl;
  }

  public Duration refreshTokenTtl() {
    return refreshTokenTtl;
  }

  public String issueAccessToken(RtoAuthenticatedPrincipal principal, Instant issuedAt) {
    Objects.requireNonNull(principal, "principal must not be null");
    Objects.requireNonNull(issuedAt, "issuedAt must not be null");
    Instant expiresAt = issuedAt.plus(accessTokenTtl);
    return Jwts.builder()
        .issuer(issuer)
        .subject(principal.userAccountId().toString())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .claim("org", principal.organizationId().toString())
        .claim("role", principal.portalRole().wireValue())
        .claim("sid", principal.sessionId().toString())
        .claim("display_name", principal.displayName())
        .signWith(signingKey())
        .compact();
  }

  public ParsedAccessToken parseAccessToken(String token) {
    Objects.requireNonNull(token, "token must not be null");
    Claims claims = Jwts.parser()
        .requireIssuer(issuer)
        .verifyWith(signingKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();

    UUID userAccountId = UUID.fromString(claims.getSubject());
    UUID organizationId = UUID.fromString(claims.get("org", String.class));
    UUID sessionId = UUID.fromString(claims.get("sid", String.class));
    PortalRole portalRole = parsePortalRole(claims.get("role", String.class));
    String displayName = claims.get("display_name", String.class);
    Instant expiresAt = claims.getExpiration().toInstant();
    return new ParsedAccessToken(
        new RtoAuthenticatedPrincipal(userAccountId, organizationId, portalRole, displayName, sessionId),
        expiresAt);
  }

  public Instant accessTokenExpiresAt(Instant issuedAt) {
    return issuedAt.plus(accessTokenTtl);
  }

  public Instant refreshTokenExpiresAt(Instant issuedAt) {
    return issuedAt.plus(refreshTokenTtl);
  }

  public static String generateOpaqueToken() {
    byte[] bytes = new byte[32];
    new java.security.SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private SecretKey signingKey() {
    if (!isConfigured()) {
      throw new IllegalStateException("JWT secret is not configured.");
    }
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException("JWT secret must be at least 32 bytes.");
    }
    return Keys.hmacShaKeyFor(secretBytes);
  }

  private static PortalRole parsePortalRole(String role) {
    for (PortalRole portalRole : PortalRole.values()) {
      if (portalRole.wireValue().equals(role)) {
        return portalRole;
      }
    }
    throw new JwtException("Unknown portal role claim.");
  }

  public record ParsedAccessToken(RtoAuthenticatedPrincipal principal, Instant expiresAt) {
    public ParsedAccessToken {
      Objects.requireNonNull(principal, "principal must not be null");
      Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
  }
}
