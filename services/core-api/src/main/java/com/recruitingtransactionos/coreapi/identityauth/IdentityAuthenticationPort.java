package com.recruitingtransactionos.coreapi.identityauth;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdentityAuthenticationPort {

  Optional<IdentityUserAccount> findByOrganizationIdAndEmail(UUID organizationId, String email);

  Optional<IdentityUserAccount> findByOrganizationIdAndUserAccountId(
      UUID organizationId,
      UUID userAccountId);

  boolean hasActiveRoleAssignment(UUID organizationId, UUID userAccountId, PortalRole portalRole);

  IdentityAuthSession createSession(IdentityAuthSession session);

  Optional<IdentityAuthSession> findActiveSessionByRefreshTokenHash(
      String refreshTokenHash,
      Instant now);

  Optional<IdentityAuthSession> findActiveSessionBySessionId(
      UUID sessionId,
      Instant now);

  Optional<IdentityAuthSession> rotateSessionAtomically(
      String currentRefreshTokenHash,
      IdentityAuthSession newSession,
      Instant revokedAt);

  IdentityAuthSession rotateSession(
      UUID sessionId,
      String refreshTokenHash,
      Instant expiresAt,
      Instant lastUsedAt);

  void revokeSessionByRefreshTokenHash(String refreshTokenHash, Instant revokedAt);

  void updateLastLoginAt(UUID organizationId, UUID userAccountId, Instant lastLoginAt);
}
