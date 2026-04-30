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
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.persistence.JdbcShortlistPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistCandidateCardPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  // ── Shortlist org-isolation ─────────────────────────────────────────────────

  @Test
  void createShortlistWithOrgA_cannotReadWithOrgB() {
    CompanyService companyService = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Shortlist Host Company")
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
        .title("Shortlist Job")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    ShortlistService shortlistService = shortlistService();
    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());

    shortlistService.createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_A)
        .jobId(jobId)
        .title("Org A Shortlist")
        .status(ShortlistStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    // Try to read with Org B — should not find it
    Optional<Shortlist> fromB = shortlistService.findShortlistByIdAndOrganizationId(ORG_B, shortlistId);
    assertThat(fromB).isEmpty();

    // Cross-org attempt to update with Org B should fail
    assertThatThrownBy(() -> shortlistService.updateShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_B)
        .jobId(jobId)
        .title("Bad Update")
        .status(ShortlistStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("0 rows");
  }

  @Test
  void createShortlistWithOrgA_canReadWithOrgA() {
    CompanyService companyService = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Shortlist Readable Host")
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
        .title("Readable Shortlist Job")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    ShortlistService shortlistService = shortlistService();
    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());

    shortlistService.createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_A)
        .jobId(jobId)
        .title("Readable Shortlist")
        .status(ShortlistStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    Optional<Shortlist> found = shortlistService.findShortlistByIdAndOrganizationId(ORG_A, shortlistId);
    assertThat(found).isPresent();
    assertThat(found.get().title()).isEqualTo("Readable Shortlist");
  }

  // ── Shortlist update optimistic locking ─────────────────────────────────────

  @Test
  void updateShortlistWithWrongVersionFails() {
    CompanyService companyService = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Version Test Shortlist Host")
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
        .title("Version Test Shortlist Job")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    ShortlistService shortlistService = shortlistService();
    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());

    Shortlist created = shortlistService.createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_A)
        .jobId(jobId)
        .title("Version Test Shortlist")
        .status(ShortlistStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.version()).isGreaterThanOrEqualTo(1);

    assertThatThrownBy(() -> shortlistService.updateShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_A)
        .jobId(jobId)
        .title("Bad Version Update")
        .status(ShortlistStatus.SENT_TO_CLIENT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(created.version() + 99)
        .build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("0 rows");
  }

  @Test
  void updateShortlistWithCorrectVersionSucceeds() {
    CompanyService companyService = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Correct Version Shortlist Host")
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
        .title("Correct Version Shortlist Job")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    ShortlistService shortlistService = shortlistService();
    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());

    Shortlist created = shortlistService.createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_A)
        .jobId(jobId)
        .title("Correct Version Shortlist")
        .status(ShortlistStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    Shortlist updated = shortlistService.updateShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_A)
        .jobId(jobId)
        .title("Updated Shortlist Name")
        .status(ShortlistStatus.READY_FOR_REVIEW)
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(created.version())
        .build());

    assertThat(updated.title()).isEqualTo("Updated Shortlist Name");
    assertThat(updated.status()).isEqualTo(ShortlistStatus.READY_FOR_REVIEW);
    assertThat(updated.version()).isEqualTo(created.version() + 1);
  }

  // ── Cross-org parent-chain validation ──────────────────────────────────────

  @Test
  void createJobWithCrossOrgCompanyThrowsIllegalArgumentException() {
    ConsultantApiCommandService commandService = commandService();
    CompanyId companyInOrgB = new CompanyId(UUID.randomUUID());
    companyService().createCompany(Company.builder()
        .companyId(companyInOrgB)
        .organizationId(ORG_B)
        .name("Company In Org B")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThatThrownBy(() -> commandService.createJob(
        jobCreateAccessRequest(),
        ORG_A,
        new JobCreateRequest(
            companyInOrgB.value().toString(), "Cross-org Job", null, null,
            null, null, null, null, "draft", null, null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Company not found in this organization");
  }

  @Test
  void updateJobWithCrossOrgCompanyThrowsIllegalArgumentException() {
    ConsultantApiCommandService commandService = commandService();

    CompanyId companyInOrgB = new CompanyId(UUID.randomUUID());
    companyService().createCompany(Company.builder()
        .companyId(companyInOrgB)
        .organizationId(ORG_B)
        .name("Company In Org B")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    CompanyId companyInOrgA = new CompanyId(UUID.randomUUID());
    companyService().createCompany(Company.builder()
        .companyId(companyInOrgA)
        .organizationId(ORG_A)
        .name("Company In Org A")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    JobId jobId = new JobId(UUID.randomUUID());
    jobService().createJob(Job.builder()
        .jobId(jobId)
        .organizationId(ORG_A)
        .companyId(companyInOrgA)
        .title("Job In Org A")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThatThrownBy(() -> commandService.updateJob(
        jobUpdateAccessRequest(),
        ORG_A,
        jobId,
        new JobUpdateRequest(
            companyInOrgB.value().toString(), "Cross-org Update", null, null,
            null, null, null, null, "draft", null, null, null, 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Company not found in this organization");
  }

  @Test
  void createShortlistWithCrossOrgJobThrowsIllegalArgumentException() {
    ConsultantApiCommandService commandService = commandService();

    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService().createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_B)
        .name("Company In Org B")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    JobId jobInOrgB = new JobId(UUID.randomUUID());
    jobService().createJob(Job.builder()
        .jobId(jobInOrgB)
        .organizationId(ORG_B)
        .companyId(companyId)
        .title("Job In Org B")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThatThrownBy(() -> commandService.createShortlist(
        shortlistCreateAccessRequest(),
        ORG_A,
        new ShortlistCreateRequest(
            jobInOrgB.value().toString(), "Cross-org Shortlist", "draft", null, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Job not found in this organization");
  }

  @Test
  void updateShortlistWithCrossOrgJobThrowsIllegalArgumentException() {
    ConsultantApiCommandService commandService = commandService();

    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService().createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_A)
        .name("Company In Org A")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    JobId jobInOrgA = new JobId(UUID.randomUUID());
    jobService().createJob(Job.builder()
        .jobId(jobInOrgA)
        .organizationId(ORG_A)
        .companyId(companyId)
        .title("Job In Org A")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    JobId jobInOrgB = new JobId(UUID.randomUUID());
    CompanyId companyInOrgB = new CompanyId(UUID.randomUUID());
    companyService().createCompany(Company.builder()
        .companyId(companyInOrgB)
        .organizationId(ORG_B)
        .name("Company In Org B")
        .status(CompanyStatus.ACTIVE)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());
    jobService().createJob(Job.builder()
        .jobId(jobInOrgB)
        .organizationId(ORG_B)
        .companyId(companyInOrgB)
        .title("Job In Org B")
        .status(JobStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());
    shortlistService().createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_A)
        .jobId(jobInOrgA)
        .title("Shortlist In Org A")
        .status(ShortlistStatus.DRAFT)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThatThrownBy(() -> commandService.updateShortlist(
        shortlistUpdateAccessRequest(),
        ORG_A,
        shortlistId,
        new ShortlistUpdateRequest(
            jobInOrgB.value().toString(), "Cross-org Update", "draft", null, null, 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Job not found in this organization");
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static ConsultantApiCommandService commandService() {
    return new ConsultantApiCommandService(
        companyService(), jobService(), shortlistService());
  }

  private static AccessRequest jobCreateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT, ResourceType.JOB, AccessAction.CREATE,
        FieldClassification.CLIENT_SAFE, Set.of(), false);
  }

  private static AccessRequest jobUpdateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT, ResourceType.JOB, AccessAction.UPDATE,
        FieldClassification.CLIENT_SAFE, Set.of(), false);
  }

  private static AccessRequest shortlistCreateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT, ResourceType.SHORTLIST, AccessAction.CREATE,
        FieldClassification.CLIENT_SAFE, Set.of(), false);
  }

  private static AccessRequest shortlistUpdateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT, ResourceType.SHORTLIST, AccessAction.UPDATE,
        FieldClassification.CLIENT_SAFE, Set.of(), false);
  }

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

  private static ShortlistService shortlistService() {
    ShortlistCandidateCardPersistencePort cardPort = new ShortlistCandidateCardPersistencePort() {
      @Override public ShortlistCandidateCard create(ShortlistCandidateCard card) { return card; }
      @Override public List<ShortlistCandidateCard> findByShortlistIdAndOrganizationId(
          UUID organizationId, ShortlistId shortlistId) { return List.of(); }
    };
    return new ShortlistService(
        new JdbcShortlistPersistencePort(dataSource), cardPort);
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
