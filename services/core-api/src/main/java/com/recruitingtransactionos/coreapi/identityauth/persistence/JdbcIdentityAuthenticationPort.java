package com.recruitingtransactionos.coreapi.identityauth.persistence;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthSession;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.identityauth.IdentityUserAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

@Component
public final class JdbcIdentityAuthenticationPort implements IdentityAuthenticationPort {

  private static final String FIND_USER_BY_ORG_AND_EMAIL_SQL = """
      SELECT user_account_id, organization_id, email, display_name, status, password_hash, last_login_at
      FROM identity.user_account
      WHERE organization_id = ?
        AND lower(email) = lower(?)
      """;

  private static final String FIND_USER_BY_ORG_AND_ID_SQL = """
      SELECT user_account_id, organization_id, email, display_name, status, password_hash, last_login_at
      FROM identity.user_account
      WHERE organization_id = ?
        AND user_account_id = ?
      """;

  private static final String HAS_ACTIVE_ROLE_SQL = """
      SELECT 1
      FROM identity.role_assignment
      WHERE organization_id = ?
        AND user_account_id = ?
        AND role = ?::governance.actor_role
        AND status = 'active'
        AND (expires_at IS NULL OR expires_at > now())
      """;

  private static final String INSERT_SESSION_SQL = """
      INSERT INTO identity.session (
        session_id,
        organization_id,
        user_account_id,
        role,
        refresh_token_hash,
        expires_at,
        revoked_at,
        created_at,
        last_used_at,
        version
      ) VALUES (?, ?, ?, ?::governance.actor_role, ?, ?, ?, ?, ?, ?)
      """;

  private static final String FIND_ACTIVE_SESSION_BY_HASH_SQL = """
      SELECT session_id, organization_id, user_account_id, role, refresh_token_hash,
             expires_at, revoked_at, created_at, last_used_at, version
      FROM identity.session
      WHERE refresh_token_hash = ?
        AND revoked_at IS NULL
        AND expires_at > ?
      """;

  private static final String FIND_ACTIVE_SESSION_BY_HASH_FOR_UPDATE_SQL = """
      SELECT session_id, organization_id, user_account_id, role, refresh_token_hash,
             expires_at, revoked_at, created_at, last_used_at, version
      FROM identity.session
      WHERE refresh_token_hash = ?
        AND revoked_at IS NULL
        AND expires_at > ?
      FOR UPDATE
      """;

  private static final String FIND_SESSION_BY_ID_SQL = """
      SELECT session_id, organization_id, user_account_id, role, refresh_token_hash,
             expires_at, revoked_at, created_at, last_used_at, version
      FROM identity.session
      WHERE session_id = ?
      """;

  private static final String FIND_ACTIVE_SESSION_BY_ID_SQL = """
      SELECT session_id, organization_id, user_account_id, role, refresh_token_hash,
             expires_at, revoked_at, created_at, last_used_at, version
      FROM identity.session
      WHERE session_id = ?
        AND revoked_at IS NULL
        AND expires_at > ?
      """;

  private static final String ROTATE_SESSION_SQL = """
      UPDATE identity.session
      SET refresh_token_hash = ?,
          expires_at = ?,
          last_used_at = ?,
          version = version + 1
      WHERE session_id = ?
        AND revoked_at IS NULL
      """;

  private static final String REVOKE_SESSION_BY_HASH_SQL = """
      UPDATE identity.session
      SET revoked_at = ?,
          last_used_at = ?,
          version = version + 1
      WHERE refresh_token_hash = ?
        AND revoked_at IS NULL
      """;

  private static final String REVOKE_SESSION_BY_ID_SQL = """
      UPDATE identity.session
      SET revoked_at = ?,
          last_used_at = ?,
          version = version + 1
      WHERE session_id = ?
        AND revoked_at IS NULL
        AND expires_at > ?
      """;

  private static final String UPDATE_LAST_LOGIN_SQL = """
      UPDATE identity.user_account
      SET last_login_at = ?,
          updated_at = ?,
          version = version + 1
      WHERE organization_id = ?
        AND user_account_id = ?
      """;

  private final DataSource dataSource;

  public JdbcIdentityAuthenticationPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<IdentityUserAccount> findByOrganizationIdAndEmail(UUID organizationId, String email) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_ORG_AND_EMAIL_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, email);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toUserAccount(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find user_account by organization and email", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<IdentityUserAccount> findByOrganizationIdAndUserAccountId(
      UUID organizationId,
      UUID userAccountId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_ORG_AND_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, userAccountId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toUserAccount(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find user_account by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public boolean hasActiveRoleAssignment(UUID organizationId, UUID userAccountId, PortalRole portalRole) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(portalRole, "portalRole must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(HAS_ACTIVE_ROLE_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, userAccountId);
      statement.setString(3, toDatabaseRole(portalRole));
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to verify active role_assignment", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public IdentityAuthSession createSession(IdentityAuthSession session) {
    Objects.requireNonNull(session, "session must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SESSION_SQL)) {
      statement.setObject(1, session.sessionId());
      statement.setObject(2, session.organizationId());
      statement.setObject(3, session.userAccountId());
      statement.setString(4, toDatabaseRole(session.portalRole()));
      statement.setString(5, session.refreshTokenHash());
      statement.setObject(6, OffsetDateTime.ofInstant(session.expiresAt(), ZoneOffset.UTC));
      statement.setObject(7, toOffsetDateTime(session.revokedAt()));
      statement.setObject(8, OffsetDateTime.ofInstant(session.createdAt(), ZoneOffset.UTC));
      statement.setObject(9, OffsetDateTime.ofInstant(session.lastUsedAt(), ZoneOffset.UTC));
      statement.setInt(10, session.version());
      statement.executeUpdate();
      return findSessionById(session.sessionId()).orElseThrow(() ->
          new IllegalStateException("identity.session not readable after insert"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create identity.session", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<IdentityAuthSession> findActiveSessionByRefreshTokenHash(
      String refreshTokenHash,
      Instant now) {
    Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null");
    Objects.requireNonNull(now, "now must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_SESSION_BY_HASH_SQL)) {
      statement.setString(1, refreshTokenHash);
      statement.setObject(2, OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSession(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find active identity.session by refresh token", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<IdentityAuthSession> rotateSessionAtomically(
      String currentRefreshTokenHash,
      IdentityAuthSession newSession,
      Instant revokedAt) {
    Objects.requireNonNull(currentRefreshTokenHash, "currentRefreshTokenHash must not be null");
    Objects.requireNonNull(newSession, "newSession must not be null");
    Objects.requireNonNull(revokedAt, "revokedAt must not be null");

    Connection connection = DataSourceUtils.getConnection(dataSource);
    boolean originalAutoCommit = true;
    try {
      originalAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);

      Optional<IdentityAuthSession> currentSession = findActiveSessionByRefreshTokenHash(
          connection,
          FIND_ACTIVE_SESSION_BY_HASH_FOR_UPDATE_SQL,
          currentRefreshTokenHash,
          revokedAt);
      if (currentSession.isEmpty()) {
        connection.rollback();
        return Optional.empty();
      }

      if (revokeSessionById(connection, currentSession.get().sessionId(), revokedAt) != 1) {
        connection.rollback();
        return Optional.empty();
      }

      insertSession(connection, newSession);
      IdentityAuthSession rotated = findSessionById(connection, newSession.sessionId()).orElseThrow(() ->
          new IllegalStateException("identity.session not readable after atomic rotation"));
      connection.commit();
      return Optional.of(rotated);
    } catch (SQLException exception) {
      rollbackQuietly(connection);
      throw new IllegalStateException("Failed to atomically rotate identity.session", exception);
    } catch (RuntimeException exception) {
      rollbackQuietly(connection);
      throw exception;
    } finally {
      restoreAutoCommit(connection, originalAutoCommit);
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<IdentityAuthSession> findActiveSessionBySessionId(UUID sessionId, Instant now) {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(now, "now must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_SESSION_BY_ID_SQL)) {
      statement.setObject(1, sessionId);
      statement.setObject(2, OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSession(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find active identity.session by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public IdentityAuthSession rotateSession(
      UUID sessionId,
      String refreshTokenHash,
      Instant expiresAt,
      Instant lastUsedAt) {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null");
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    Objects.requireNonNull(lastUsedAt, "lastUsedAt must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(ROTATE_SESSION_SQL)) {
      statement.setString(1, refreshTokenHash);
      statement.setObject(2, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
      statement.setObject(3, OffsetDateTime.ofInstant(lastUsedAt, ZoneOffset.UTC));
      statement.setObject(4, sessionId);
      int updated = statement.executeUpdate();
      if (updated != 1) {
        throw new IllegalStateException("identity.session not found for rotation");
      }
      return findSessionById(sessionId).orElseThrow(() ->
          new IllegalStateException("identity.session not readable after rotation"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to rotate identity.session", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public void revokeSessionByRefreshTokenHash(String refreshTokenHash, Instant revokedAt) {
    Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null");
    Objects.requireNonNull(revokedAt, "revokedAt must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(REVOKE_SESSION_BY_HASH_SQL)) {
      OffsetDateTime revoked = OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC);
      statement.setObject(1, revoked);
      statement.setObject(2, revoked);
      statement.setString(3, refreshTokenHash);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to revoke identity.session", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public void updateLastLoginAt(UUID organizationId, UUID userAccountId, Instant lastLoginAt) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userAccountId, "userAccountId must not be null");
    Objects.requireNonNull(lastLoginAt, "lastLoginAt must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(UPDATE_LAST_LOGIN_SQL)) {
      OffsetDateTime loginAt = OffsetDateTime.ofInstant(lastLoginAt, ZoneOffset.UTC);
      statement.setObject(1, loginAt);
      statement.setObject(2, loginAt);
      statement.setObject(3, organizationId);
      statement.setObject(4, userAccountId);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to update user_account last_login_at", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private Optional<IdentityAuthSession> findSessionById(UUID sessionId) {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_SESSION_BY_ID_SQL)) {
      statement.setObject(1, sessionId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSession(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find identity.session by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private Optional<IdentityAuthSession> findSessionById(Connection connection, UUID sessionId) {
    try (PreparedStatement statement = connection.prepareStatement(FIND_SESSION_BY_ID_SQL)) {
      statement.setObject(1, sessionId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSession(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find identity.session by id", exception);
    }
  }

  private Optional<IdentityAuthSession> findActiveSessionByRefreshTokenHash(
      Connection connection,
      String sql,
      String refreshTokenHash,
      Instant now) {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, refreshTokenHash);
      statement.setObject(2, OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toSession(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find active identity.session by refresh token", exception);
    }
  }

  private void insertSession(Connection connection, IdentityAuthSession session) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SESSION_SQL)) {
      statement.setObject(1, session.sessionId());
      statement.setObject(2, session.organizationId());
      statement.setObject(3, session.userAccountId());
      statement.setString(4, toDatabaseRole(session.portalRole()));
      statement.setString(5, session.refreshTokenHash());
      statement.setObject(6, OffsetDateTime.ofInstant(session.expiresAt(), ZoneOffset.UTC));
      statement.setObject(7, toOffsetDateTime(session.revokedAt()));
      statement.setObject(8, OffsetDateTime.ofInstant(session.createdAt(), ZoneOffset.UTC));
      statement.setObject(9, OffsetDateTime.ofInstant(session.lastUsedAt(), ZoneOffset.UTC));
      statement.setInt(10, session.version());
      statement.executeUpdate();
    }
  }

  private int revokeSessionById(Connection connection, UUID sessionId, Instant revokedAt)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(REVOKE_SESSION_BY_ID_SQL)) {
      OffsetDateTime revoked = OffsetDateTime.ofInstant(revokedAt, ZoneOffset.UTC);
      statement.setObject(1, revoked);
      statement.setObject(2, revoked);
      statement.setObject(3, sessionId);
      statement.setObject(4, revoked);
      return statement.executeUpdate();
    }
  }

  private static void rollbackQuietly(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException ignored) {
      // Keep the original failure as the primary signal.
    }
  }

  private static void restoreAutoCommit(Connection connection, boolean originalAutoCommit) {
    try {
      connection.setAutoCommit(originalAutoCommit);
    } catch (SQLException ignored) {
      // Connection is being released immediately after this call.
    }
  }

  private static IdentityUserAccount toUserAccount(ResultSet rs) throws SQLException {
    Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
    return new IdentityUserAccount(
        rs.getObject("user_account_id", UUID.class),
        rs.getObject("organization_id", UUID.class),
        rs.getString("email"),
        rs.getString("display_name"),
        rs.getString("status"),
        rs.getString("password_hash"),
        lastLoginAt == null ? null : lastLoginAt.toInstant());
  }

  private static IdentityAuthSession toSession(ResultSet rs) throws SQLException {
    OffsetDateTime revokedAt = rs.getObject("revoked_at", OffsetDateTime.class);
    OffsetDateTime lastUsedAt = rs.getObject("last_used_at", OffsetDateTime.class);
    return new IdentityAuthSession(
        rs.getObject("session_id", UUID.class),
        rs.getObject("organization_id", UUID.class),
        rs.getObject("user_account_id", UUID.class),
        toPortalRole(rs.getString("role")),
        rs.getString("refresh_token_hash"),
        rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
        revokedAt == null ? null : revokedAt.toInstant(),
        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
        lastUsedAt == null ? rs.getObject("created_at", OffsetDateTime.class).toInstant() : lastUsedAt.toInstant(),
        rs.getInt("version"));
  }

  private static String toDatabaseRole(PortalRole portalRole) {
    return switch (portalRole) {
      case OWNER, CONSULTANT, CLIENT, CANDIDATE, ADMIN -> portalRole.wireValue();
      default -> throw new IllegalArgumentException("Unsupported database-backed portal role: " + portalRole);
    };
  }

  private static PortalRole toPortalRole(String role) {
    for (PortalRole portalRole : PortalRole.values()) {
      if (portalRole.wireValue().equals(role)) {
        return portalRole;
      }
    }
    throw new IllegalArgumentException("Unsupported portal role: " + role);
  }

  private static OffsetDateTime toOffsetDateTime(Instant value) {
    return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
  }
}
