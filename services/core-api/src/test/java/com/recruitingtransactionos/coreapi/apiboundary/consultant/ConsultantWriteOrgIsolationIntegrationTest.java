package com.recruitingtransactionos.coreapi.apiboundary.consultant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyContact;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyPersistencePort;
import com.recruitingtransactionos.coreapi.company.port.CompanyContactPersistencePort;
import com.recruitingtransactionos.coreapi.company.port.CompanyPreferencePersistencePort;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobScorecardPersistencePort;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ConsultantWriteOrgIsolationIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE =
      DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = UUID.fromString("00000000-0000-0000-0000-00000018b001");
  private static final UUID ORG_B = UUID.fromString("00000000-0000-0000-0000-00000018b002");
  private static final Instant NOW = Instant.parse("2026-04-30T12:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = new PostgresDataSource(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    insertOrganization(ORG_A);
    insertOrganization(ORG_B);
  }

  // ── Company org-isolation ──────────────────────────────────────────────────

  @Test
  void createCompanyWithOrgA_cannotReadWithOrgB() {
    CompanyService service = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());

    Company created = service.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Org A Company")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.name()).isEqualTo("Org A Company");

    // Try to read with Org B — should not find it
    Optional<Company> fromB = service.findCompanyByIdAndOrganizationId(ORG_B, companyId);
    assertThat(fromB).isEmpty();

    // Cross-org attempt to update with Org B should fail
    assertThatThrownBy(() -> service.updateCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_B)
        .name("Bad Update")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(created.version())
        .build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("0 rows");
  }

  @Test
  void createCompanyWithOrgA_canReadWithOrgA() {
    CompanyService service = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());

    service.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Readable Company")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    Optional<Company> found = service.findCompanyByIdAndOrganizationId(ORG_A, companyId);
    assertThat(found).isPresent();
    assertThat(found.get().name()).isEqualTo("Readable Company");
  }

  // ── Job org-isolation ─────────────────────────────────────────────────────

  @Test
  void createJobWithOrgA_cannotReadWithOrgB() {
    CompanyService companyService = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Job Host Company")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    JobService jobService = jobService();
    JobId jobId = new JobId(UUID.randomUUID());

    jobService.createJob(Job.builder()
        .jobId(jobId)
        .organizationId(ORG_A)
        .companyId(companyId)
        .title("Senior Engineer")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    // Try to read with Org B — should not find it
    Optional<Job> fromB = jobService.findJobByIdAndOrganizationId(ORG_B, jobId);
    assertThat(fromB).isEmpty();
  }

  // ── Company update optimistic locking ─────────────────────────────────────

  @Test
  void updateCompanyWithWrongVersionFails() {
    CompanyService service = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());

    Company created = service.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Version Test Company")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.version()).isGreaterThanOrEqualTo(1);

    assertThatThrownBy(() -> service.updateCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Bad Version Update")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(created.version() + 99)
        .build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("0 rows");
  }

  @Test
  void updateCompanyWithCorrectVersionSucceeds() {
    CompanyService service = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());

    Company created = service.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Correct Version Company")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    Company updated = service.updateCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Updated Name")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(created.version())
        .build());

    assertThat(updated.name()).isEqualTo("Updated Name");
    assertThat(updated.version()).isEqualTo(created.version() + 1);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static CompanyService companyService() {
    CompanyContactPersistencePort contactPort = new CompanyContactPersistencePort() {
      @Override public CompanyContact create(CompanyContact contact) { return contact; }
      @Override public List<CompanyContact> findByCompanyIdAndOrganizationId(
          UUID organizationId, CompanyId companyId) { return List.of(); }
    };
    CompanyPreferencePersistencePort preferencePort = new CompanyPreferencePersistencePort() {
      @Override public CompanyPreference upsert(CompanyPreference preference) { return preference; }
      @Override public List<CompanyPreference> findByCompanyIdAndOrganizationId(
          UUID organizationId, CompanyId companyId) { return List.of(); }
    };
    return new CompanyService(
        new JdbcCompanyPersistencePort(dataSource), contactPort, preferencePort);
  }

  private static JobService jobService() {
    JobRequirementPersistencePort requirementPort = new JobRequirementPersistencePort() {
      @Override public JobRequirement create(JobRequirement requirement) { return requirement; }
      @Override public List<JobRequirement> findByJobIdAndOrganizationId(
          UUID organizationId, JobId jobId) { return List.of(); }
    };
    JobScorecardPersistencePort scorecardPort = new JobScorecardPersistencePort() {
      @Override public JobScorecard create(JobScorecard scorecard) { return scorecard; }
      @Override public Optional<JobScorecard> findActiveByJobIdAndOrganizationId(
          UUID organizationId, JobId jobId) { return Optional.empty(); }
    };
    return new JobService(
        new JdbcJobPersistencePort(dataSource), requirementPort, scorecardPort);
  }

  private static void insertOrganization(UUID orgId) {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "INSERT INTO identity.organization "
                 + "(organization_id, legal_name, display_name, status, default_timezone) "
                 + "VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING")) {
      stmt.setObject(1, orgId);
      stmt.setString(2, "Test Org " + orgId.toString().substring(0, 8));
      stmt.setString(3, "Test Org " + orgId.toString().substring(0, 8));
      stmt.setString(4, "active");
      stmt.setString(5, "UTC");
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to insert organization", e);
    }
  }

  private static final class PostgresDataSource implements DataSource {

    private final String url;
    private final String user;
    private final String password;

    PostgresDataSource(String url, String user, String password) {
      this.url = url;
      this.user = user;
      this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
      return java.sql.DriverManager.getConnection(url, user, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return java.sql.DriverManager.getConnection(url, username, password);
    }

    @Override public java.io.PrintWriter getLogWriter() { return null; }
    @Override public void setLogWriter(java.io.PrintWriter out) {}
    @Override public void setLoginTimeout(int seconds) {}
    @Override public int getLoginTimeout() { return 0; }

    @Override
    public java.util.logging.Logger getParentLogger() {
      return java.util.logging.Logger.getLogger("PostgresDataSource");
    }

    @Override public <T> T unwrap(Class<T> iface) { throw new UnsupportedOperationException(); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
  }
}
