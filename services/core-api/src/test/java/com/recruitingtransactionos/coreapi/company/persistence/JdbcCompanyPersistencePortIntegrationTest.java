package com.recruitingtransactionos.coreapi.company.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
class JdbcCompanyPersistencePortIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-00000042c001");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000042c002");
  private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000042c003"));

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
  }

  @Test
  void findAllByOrganizationIdNormalizesLegacyBlankOptionalColumnsToNull() throws SQLException {
    seedCompanyWithBlankOptionalColumns();
    JdbcCompanyPersistencePort port = new JdbcCompanyPersistencePort(dataSource);

    var companies = port.findAllByOrganizationId(ORGANIZATION_ID);

    assertThat(companies).singleElement().satisfies(company -> {
      assertThat(company.companyId()).isEqualTo(COMPANY_ID);
      assertThat(company.website()).isNull();
      assertThat(company.paymentReliability()).isNull();
    });
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
      organization.setString(2, "company-port-org");
      organization.setString(3, "Company Port Org");
      organization.executeUpdate();

      user.setObject(1, CONSULTANT_ID);
      user.setObject(2, ORGANIZATION_ID);
      user.setString(3, "company-port@example.test");
      user.setString(4, "Company Port Tester");
      user.executeUpdate();
    }
  }

  private static void seedCompanyWithBlankOptionalColumns() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement allowLegacyBlankWebsite = connection.prepareStatement("""
             ALTER TABLE recruiting.company DROP CONSTRAINT IF EXISTS company_website_check
             """);
         PreparedStatement company = connection.prepareStatement("""
             INSERT INTO recruiting.company (
               company_id, organization_id, name, display_name, industry, website,
               headquarters_location, size_band, status, payment_reliability,
               owner_consultant_id
             ) VALUES (?, ?, ?, ?, ?, '', ?, ?, 'active', NULL, ?)
             ON CONFLICT (company_id) DO NOTHING
             """)) {
      allowLegacyBlankWebsite.executeUpdate();
      company.setObject(1, COMPANY_ID.value());
      company.setObject(2, ORGANIZATION_ID);
      company.setString(3, "Blank Optional Company");
      company.setString(4, "Blank Optional Company");
      company.setString(5, "semiconductor");
      company.setString(6, "Shanghai");
      company.setString(7, "500-1000");
      company.setObject(8, CONSULTANT_ID);
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
