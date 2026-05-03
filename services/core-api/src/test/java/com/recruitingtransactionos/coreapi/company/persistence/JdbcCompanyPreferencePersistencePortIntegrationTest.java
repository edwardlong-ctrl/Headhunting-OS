package com.recruitingtransactionos.coreapi.company.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import com.recruitingtransactionos.coreapi.company.CompanyPreferenceId;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class JdbcCompanyPreferencePersistencePortIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-00000032b001");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000032b002");
  private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000032b003"));
  private static final Instant CREATED_AT = Instant.parse("2026-05-03T11:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;
  @SuppressWarnings("unused")
  private static MigrateResult migrateResult;

  @BeforeAll
  static void migrate() throws SQLException {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    seedOrganizationAndUser();
    seedCompany();
  }

  @Test
  void upsertAndFindByCompanyId_handlesPlainTextPreferenceValues() {
    JdbcCompanyPreferencePersistencePort port = new JdbcCompanyPreferencePersistencePort(dataSource);
    CompanyPreference preference = CompanyPreference.builder()
        .companyPreferenceId(new CompanyPreferenceId(UUID.fromString("00000000-0000-0000-0000-00000032b004")))
        .organizationId(ORGANIZATION_ID)
        .companyId(COMPANY_ID)
        .preferenceKey("communication_preferences")
        .preferenceValue("weekly updates")
        .notes("plain text should round-trip")
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT)
        .version(1)
        .build();

    port.upsert(preference);

    List<CompanyPreference> stored = port.findByCompanyIdAndOrganizationId(ORGANIZATION_ID, COMPANY_ID);
    assertThat(stored).extracting(CompanyPreference::preferenceKey).contains("communication_preferences");
    assertThat(stored).extracting(CompanyPreference::preferenceValue).contains("weekly updates");
  }

  private static void seedOrganizationAndUser() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement organization = connection.prepareStatement("""
             INSERT INTO identity.organization (
               organization_id, legal_name, display_name, status, default_timezone
             ) VALUES (?, ?, ?, 'active', 'UTC')
             ON CONFLICT (organization_id) DO NOTHING
             """);
         PreparedStatement user = connection.prepareStatement("""
             INSERT INTO identity.user_account (
               user_account_id, organization_id, email, display_name, status, password_hash
             ) VALUES (?, ?, ?, ?, 'active', NULL)
             ON CONFLICT (user_account_id) DO NOTHING
             """)) {
      organization.setObject(1, ORGANIZATION_ID);
      organization.setString(2, "client-preference-org");
      organization.setString(3, "Client Preference Org");
      organization.executeUpdate();

      user.setObject(1, CONSULTANT_ID);
      user.setObject(2, ORGANIZATION_ID);
      user.setString(3, "client-preference@example.test");
      user.setString(4, "Client Preference Tester");
      user.executeUpdate();
    }
  }

  private static void seedCompany() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement company = connection.prepareStatement("""
             INSERT INTO recruiting.company (
               company_id, organization_id, name, status, owner_consultant_id
             ) VALUES (?, ?, ?, 'active', ?)
             ON CONFLICT (company_id) DO NOTHING
             """)) {
      company.setObject(1, COMPANY_ID.value());
      company.setObject(2, ORGANIZATION_ID);
      company.setString(3, "Preference Test Co");
      company.setObject(4, CONSULTANT_ID);
      company.executeUpdate();
    }
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
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
      public java.io.PrintWriter getLogWriter() {
        return new java.io.PrintWriter(System.out);
      }

      @Override
      public void setLogWriter(java.io.PrintWriter out) {
      }

      @Override
      public void setLoginTimeout(int seconds) {
      }

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getGlobal();
      }
    };
  }
}
