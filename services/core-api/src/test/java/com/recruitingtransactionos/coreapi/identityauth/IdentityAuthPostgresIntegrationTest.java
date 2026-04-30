package com.recruitingtransactionos.coreapi.identityauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityauth.persistence.JdbcIdentityAuthenticationPort;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class IdentityAuthPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant NOW = Instant.parse("2026-05-01T08:00:00Z");
  private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;
  private static PasswordEncoder passwordEncoder;
  private static JwtService jwtService;

  @BeforeAll
  static void migrate() {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    passwordEncoder = new BCryptPasswordEncoder();
    jwtService = new JwtService(TEST_SECRET, "test-issuer", 1800, 604800);
  }

  @Test
  void migrationAddsPasswordHashAndIdentitySessionTable() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(17);
    assertThat(appliedMigrationVersions()).containsExactly(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17");
    assertThat(columnExists("identity", "user_account", "password_hash")).isTrue();
    assertThat(tableExists("identity", "session")).isTrue();
  }

  @Test
  void loginRefreshAndLogoutPersistRotatedSessionLifecycle() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000190101");
    UUID userAccountId = uuid("00000000-0000-0000-0000-000000190102");
    insertOrganization(organizationId);
    insertUserAccount(organizationId, userAccountId, "consultant@example.com", "Consultant User",
        "active", passwordEncoder.encode("secret123"));
    insertRoleAssignment(organizationId, userAccountId, "consultant");

    AuthenticationService service = service();
    AuthenticationService.AuthenticatedSession login = service.login(
        organizationId,
        "consultant@example.com",
        "secret123",
        PortalRole.CONSULTANT);
    JdbcIdentityAuthenticationPort port = new JdbcIdentityAuthenticationPort(dataSource);
    JwtService.ParsedAccessToken loginAccess = jwtService.parseAccessToken(login.accessToken());

    assertThat(login.portalRole()).isEqualTo(PortalRole.CONSULTANT);
    assertThat(login.accessToken()).isNotBlank();
    assertThat(login.refreshToken()).isNotBlank();
    assertThat(countSessionsForOrganization(organizationId)).isEqualTo(1);
    assertThat(lastLoginAt(organizationId, userAccountId)).isEqualTo(NOW);
    assertThat(port.findActiveSessionBySessionId(loginAccess.principal().sessionId(), NOW)).isPresent();

    AuthenticationService.AuthenticatedSession refresh = service.refresh(login.refreshToken());
    JwtService.ParsedAccessToken refreshAccess = jwtService.parseAccessToken(refresh.accessToken());
    assertThat(refresh.refreshToken()).isNotEqualTo(login.refreshToken());
    assertThat(refresh.userAccountId()).isEqualTo(userAccountId);
    assertThat(refreshAccess.principal().sessionId()).isNotEqualTo(loginAccess.principal().sessionId());
    assertThat(countSessionsForOrganization(organizationId)).isEqualTo(2);
    assertThat(port.findActiveSessionBySessionId(loginAccess.principal().sessionId(), NOW)).isEmpty();
    assertThat(port.findActiveSessionBySessionId(refreshAccess.principal().sessionId(), NOW)).isPresent();

    assertThatThrownBy(() -> service.refresh(login.refreshToken()))
        .isInstanceOf(AuthenticationFailureException.class)
        .hasMessage("invalid_refresh_token");

    AuthenticationService.LoggedOutSession loggedOut = service.logout(refresh.refreshToken());
    assertThat(loggedOut.status()).isEqualTo("logged_out");
    assertThat(port.findActiveSessionBySessionId(refreshAccess.principal().sessionId(), NOW)).isEmpty();
    assertThatThrownBy(() -> service.refresh(refresh.refreshToken()))
        .isInstanceOf(AuthenticationFailureException.class)
        .hasMessage("invalid_refresh_token");
  }

  @Test
  void failedAtomicRefreshRotationRollsBackRevocation() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000190401");
    UUID userAccountId = uuid("00000000-0000-0000-0000-000000190402");
    insertOrganization(organizationId);
    insertUserAccount(organizationId, userAccountId, "rollback@example.com", "Rollback User",
        "active", passwordEncoder.encode("secret123"));
    insertRoleAssignment(organizationId, userAccountId, "consultant");

    AuthenticationService.AuthenticatedSession login = service().login(
        organizationId,
        "rollback@example.com",
        "secret123",
        PortalRole.CONSULTANT);
    JdbcIdentityAuthenticationPort port = new JdbcIdentityAuthenticationPort(dataSource);
    JwtService.ParsedAccessToken loginAccess = jwtService.parseAccessToken(login.accessToken());
    IdentityAuthSession conflictingSession = new IdentityAuthSession(
        loginAccess.principal().sessionId(),
        organizationId,
        userAccountId,
        PortalRole.CONSULTANT,
        AuthenticationService.hashToken(JwtService.generateOpaqueToken()),
        jwtService.refreshTokenExpiresAt(NOW),
        null,
        NOW,
        NOW,
        1);

    assertThatThrownBy(() -> port.rotateSessionAtomically(
        AuthenticationService.hashToken(login.refreshToken()),
        conflictingSession,
        NOW))
        .isInstanceOf(IllegalStateException.class);

    assertThat(countSessionsForOrganization(organizationId)).isEqualTo(1);
    assertThat(port.findActiveSessionBySessionId(loginAccess.principal().sessionId(), NOW)).isPresent();
    assertThat(port.findActiveSessionByRefreshTokenHash(
        AuthenticationService.hashToken(login.refreshToken()),
        NOW)).isPresent();
  }

  @Test
  void suspendedAccountIsDeniedEvenWithCorrectPassword() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000190201");
    UUID userAccountId = uuid("00000000-0000-0000-0000-000000190202");
    insertOrganization(organizationId);
    insertUserAccount(organizationId, userAccountId, "suspended@example.com", "Suspended User",
        "suspended", passwordEncoder.encode("secret123"));
    insertRoleAssignment(organizationId, userAccountId, "consultant");

    assertThatThrownBy(() -> service().login(
        organizationId,
        "suspended@example.com",
        "secret123",
        PortalRole.CONSULTANT))
        .isInstanceOf(AuthenticationFailureException.class)
        .hasMessage("account_inactive");
  }

  @Test
  void missingRoleAssignmentIsDeniedForRequestedPortalRole() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000190301");
    UUID userAccountId = uuid("00000000-0000-0000-0000-000000190302");
    insertOrganization(organizationId);
    insertUserAccount(organizationId, userAccountId, "client@example.com", "Client User",
        "active", passwordEncoder.encode("secret123"));
    insertRoleAssignment(organizationId, userAccountId, "client");

    assertThatThrownBy(() -> service().login(
        organizationId,
        "client@example.com",
        "secret123",
        PortalRole.CONSULTANT))
        .isInstanceOf(AuthenticationFailureException.class)
        .hasMessage("role_assignment_required");
  }

  private static AuthenticationService service() {
    return new AuthenticationService(
        new JdbcIdentityAuthenticationPort(dataSource),
        passwordEncoder,
        jwtService,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static void insertOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO identity.organization (
               organization_id, legal_name, display_name, status, default_timezone
             ) VALUES (?, ?, ?, 'active', 'UTC')
             """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, "org-" + organizationId);
      statement.setString(3, "Org " + organizationId.toString().substring(30));
      statement.executeUpdate();
    }
  }

  private static void insertUserAccount(
      UUID organizationId,
      UUID userAccountId,
      String email,
      String displayName,
      String status,
      String passwordHash) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO identity.user_account (
               user_account_id,
               organization_id,
               email,
               display_name,
               status,
               password_hash
             ) VALUES (?, ?, ?, ?, ?, ?)
             """)) {
      statement.setObject(1, userAccountId);
      statement.setObject(2, organizationId);
      statement.setString(3, email);
      statement.setString(4, displayName);
      statement.setString(5, status);
      statement.setString(6, passwordHash);
      statement.executeUpdate();
    }
  }

  private static void insertRoleAssignment(UUID organizationId, UUID userAccountId, String role)
      throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             INSERT INTO identity.role_assignment (
               role_assignment_id,
               organization_id,
               user_account_id,
               role,
               scope_type,
               status
             ) VALUES (?, ?, ?, ?::governance.actor_role, 'organization', 'active')
             """)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, organizationId);
      statement.setObject(3, userAccountId);
      statement.setString(4, role);
      statement.executeUpdate();
    }
  }

  private static Instant lastLoginAt(UUID organizationId, UUID userAccountId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT last_login_at
             FROM identity.user_account
             WHERE organization_id = ?
               AND user_account_id = ?
             """)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, userAccountId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getTimestamp("last_login_at").toInstant();
      }
    }
  }

  private static int countRows(String tableName) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
         ResultSet resultSet = statement.executeQuery()) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private static int countSessionsForOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT COUNT(*)
             FROM identity.session
             WHERE organization_id = ?
             """)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static boolean columnExists(String schema, String table, String column) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT 1
             FROM information_schema.columns
             WHERE table_schema = ? AND table_name = ? AND column_name = ?
             """)) {
      statement.setString(1, schema);
      statement.setString(2, table);
      statement.setString(3, column);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private static boolean tableExists(String schema, String table) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT 1
             FROM information_schema.tables
             WHERE table_schema = ? AND table_name = ?
             """)) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private static List<String> appliedMigrationVersions() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("""
             SELECT version
             FROM flyway_schema_history
             WHERE success = TRUE
             ORDER BY installed_rank
             """)) {
      try (ResultSet resultSet = statement.executeQuery()) {
        java.util.ArrayList<String> versions = new java.util.ArrayList<>();
        while (resultSet.next()) {
          versions.add(resultSet.getString(1));
        }
        return List.copyOf(versions);
      }
    }
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private static DataSource postgresDataSource() {
    String username = POSTGRES.getUsername();
    String password = POSTGRES.getPassword();
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
      }

      @Override
      public Connection getConnection(String user, String pass) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), user, pass);
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("unwrap not supported");
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }

      @Override
      public PrintWriter getLogWriter() {
        return null;
      }

      @Override
      public void setLogWriter(PrintWriter out) {}

      @Override
      public void setLoginTimeout(int seconds) {}

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("Parent logger not supported");
      }
    };
  }
}
