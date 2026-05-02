package com.recruitingtransactionos.coreapi.productdatamodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentId;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentStatus;
import com.recruitingtransactionos.coreapi.candidatedocument.persistence.JdbcCandidateDocumentPersistencePort;
import com.recruitingtransactionos.coreapi.candidatedocument.service.CandidateDocumentService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceType;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.ProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.ProfileFieldLineageId;
import com.recruitingtransactionos.coreapi.candidateprofile.persistence.JdbcProfileFieldLineagePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.ProfileFieldLineageService;
import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.commission.persistence.JdbcCommissionPersistencePort;
import com.recruitingtransactionos.coreapi.commission.service.CommissionService;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyContact;
import com.recruitingtransactionos.coreapi.company.CompanyContactId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import com.recruitingtransactionos.coreapi.company.CompanyPreferenceId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyContactPersistencePort;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyPersistencePort;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyPreferencePersistencePort;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.InteractionStatus;
import com.recruitingtransactionos.coreapi.interaction.InteractionType;
import com.recruitingtransactionos.coreapi.interaction.persistence.JdbcCandidateCompanyInteractionPersistencePort;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedback;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewFeedbackId;
import com.recruitingtransactionos.coreapi.interviewfeedback.InterviewOutcome;
import com.recruitingtransactionos.coreapi.interviewfeedback.persistence.JdbcInterviewFeedbackPersistencePort;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobRequirementId;
import com.recruitingtransactionos.coreapi.job.JobRequirementImportance;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobScorecardId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobScorecardPersistencePort;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.PlacementStatus;
import com.recruitingtransactionos.coreapi.placement.persistence.JdbcPlacementPersistencePort;
import com.recruitingtransactionos.coreapi.placement.service.PlacementService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.persistence.JdbcShortlistCandidateCardPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.persistence.JdbcShortlistPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
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
class ProductDataModelCompletionPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE =
      DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_ID = uuid("00000000-0000-0000-0000-001600000001");
  private static final UUID ORG_OTHER = uuid("00000000-0000-0000-0000-001600000002");
  private static final CandidateId CANDIDATE_A =
      new CandidateId(uuid("00000000-0000-0000-0000-001600000010"));
  private static final CandidateProfileId PROFILE_ID =
      new CandidateProfileId(uuid("00000000-0000-0000-0000-001600000020"));
  private static final UUID USER_ACCOUNT_ID = uuid("00000000-0000-0000-0000-001600000030");
  private static final Instant NOW = Instant.parse("2026-04-30T12:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;

  @BeforeAll
  static void migrate() throws SQLException {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
    insertOrganization(ORG_ID);
    insertOrganization(ORG_OTHER);
    insertUserAccount(ORG_ID, USER_ACCOUNT_ID);
    insertCandidate(ORG_ID, CANDIDATE_A);
    insertCandidateProfile(ORG_ID, PROFILE_ID, CANDIDATE_A);
  }

  // ========== 16A: ProfileFieldLineage ==========

  @Test
  void profileFieldLineageCreateAndQueryByProfilePath() {
    ProfileFieldLineageService service = lineageService();
    ProfileFieldLineage lineage = ProfileFieldLineage.builder()
        .profileFieldLineageId(new ProfileFieldLineageId(UUID.randomUUID()))
        .organizationId(ORG_ID)
        .candidateProfileId(PROFILE_ID)
        .candidateId(CANDIDATE_A)
        .fieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME)
        .sourceType(CandidateProfileFieldSourceType.SOURCE_ITEM)
        .sourceId("source-item-123")
        .sourceTrust("high")
        .provenanceLabel("resume_parse")
        .recordedAt(NOW)
        .createdAt(NOW)
        .build();

    service.append(lineage);

    List<ProfileFieldLineage> results = service.findByProfileAndFieldPath(
        ORG_ID, PROFILE_ID, CandidateProfileFieldPath.IDENTITY_FULL_NAME);
    assertThat(results).hasSize(1);
    assertThat(results.getFirst().sourceId()).isEqualTo("source-item-123");
    assertThat(results.getFirst().sourceTrust()).isEqualTo("high");
  }

  @Test
  void profileFieldLineageIsOrganizationScoped() {
    ProfileFieldLineageService service = lineageService();
    ProfileFieldLineageId id = new ProfileFieldLineageId(UUID.randomUUID());
    service.append(ProfileFieldLineage.builder()
        .profileFieldLineageId(id)
        .organizationId(ORG_ID)
        .candidateProfileId(PROFILE_ID)
        .candidateId(CANDIDATE_A)
        .fieldPath(CandidateProfileFieldPath.CONTACT_EMAIL)
        .sourceType(CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM)
        .sourceId("claim-999")
        .recordedAt(NOW)
        .createdAt(NOW)
        .build());

    assertThat(service.findByProfileAndFieldPath(
        ORG_OTHER, PROFILE_ID, CandidateProfileFieldPath.CONTACT_EMAIL))
        .isEmpty();
  }

  // ========== 16B: Company + Job ==========

  @Test
  void companyCreateAndReadBack() {
    CompanyService service = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    Company created = service.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_ID)
        .name("Acme Corp")
        .displayName("Acme Corporation")
        .industry("Technology")
        .website("https://acme.example.com")
        .status(CompanyStatus.ACTIVE)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.companyId()).isEqualTo(companyId);
    assertThat(created.name()).isEqualTo("Acme Corp");
    assertThat(created.status()).isEqualTo(CompanyStatus.ACTIVE);

    assertThat(service.findCompanyByIdAndOrganizationId(ORG_ID, companyId)).isPresent();
    assertThat(service.findCompanyByIdAndOrganizationId(ORG_OTHER, companyId)).isEmpty();
    assertThat(service.findCompaniesByOrganizationIdAndStatus(ORG_ID, CompanyStatus.ACTIVE))
        .extracting(Company::companyId)
        .contains(companyId);
  }

  @Test
  void companyContactCreateAndReadBack() {
    CompanyService service = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    service.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_ID)
        .name("Contact Test Co " + UUID.randomUUID().toString().substring(0, 8))
        .status(CompanyStatus.NEW)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    CompanyContactId contactId = new CompanyContactId(UUID.randomUUID());
    service.createContact(CompanyContact.builder()
        .companyContactId(contactId)
        .organizationId(ORG_ID)
        .companyId(companyId)
        .name("Jane Doe")
        .title("VP Engineering")
        .email("jane@example.com")
        .roleType("hiring_manager")
        .isPrimary(true)
        .status("active")
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    List<CompanyContact> contacts =
        service.findContactsByCompanyIdAndOrganizationId(ORG_ID, companyId);
    assertThat(contacts).hasSize(1);
    assertThat(contacts.getFirst().name()).isEqualTo("Jane Doe");
    assertThat(contacts.getFirst().isPrimary()).isTrue();
  }

  @Test
  void companyPreferenceUpsert() {
    CompanyService service = companyService();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    service.createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_ID)
        .name("Pref Test Co " + UUID.randomUUID().toString().substring(0, 8))
        .status(CompanyStatus.NEW)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    CompanyPreference pref = service.upsertPreference(CompanyPreference.builder()
        .companyPreferenceId(new CompanyPreferenceId(UUID.randomUUID()))
        .organizationId(ORG_ID)
        .companyId(companyId)
        .preferenceKey("sourcing_channel")
        .preferenceValue("{\"preferred\": \"linkedin\"}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(pref.preferenceKey()).isEqualTo("sourcing_channel");
    assertThat(service.findPreferencesByCompanyIdAndOrganizationId(ORG_ID, companyId))
        .hasSize(1);
  }

  @Test
  void jobCreateAndReadBack() {
    JobService service = jobService();
    CompanyId companyId = createTestCompany("Job Test Co");
    JobId jobId = new JobId(UUID.randomUUID());

    Job created = service.createJob(Job.builder()
        .jobId(jobId)
        .organizationId(ORG_ID)
        .companyId(companyId)
        .title("Senior Engineer")
        .description("Build things")
        .status(JobStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.jobId()).isEqualTo(jobId);
    assertThat(created.title()).isEqualTo("Senior Engineer");
    assertThat(service.findJobByIdAndOrganizationId(ORG_ID, jobId)).isPresent();
    assertThat(service.findJobByIdAndOrganizationId(ORG_OTHER, jobId)).isEmpty();
    assertThat(service.findJobsByCompanyIdAndOrganizationId(ORG_ID, companyId))
        .extracting(Job::jobId)
        .contains(jobId);
  }

  @Test
  void jobRequirementCreateAndReadBack() {
    JobService service = jobService();
    CompanyId companyId = createTestCompany("Req Test Co");
    JobId jobId = createTestJob(companyId, "Requirement Test Job");

    service.createRequirement(JobRequirement.builder()
        .jobRequirementId(new JobRequirementId(UUID.randomUUID()))
        .organizationId(ORG_ID)
        .jobId(jobId)
        .requirementType("skill")
        .label("Java 21+")
        .importance(JobRequirementImportance.MUST_HAVE)
        .sortOrder(1)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    List<JobRequirement> reqs =
        service.findRequirementsByJobIdAndOrganizationId(ORG_ID, jobId);
    assertThat(reqs).hasSize(1);
    assertThat(reqs.getFirst().label()).isEqualTo("Java 21+");
    assertThat(reqs.getFirst().importance()).isEqualTo(JobRequirementImportance.MUST_HAVE);
  }

  @Test
  void jobScorecardCreateAndFindActive() {
    JobService service = jobService();
    CompanyId companyId = createTestCompany("Scorecard Test Co");
    JobId jobId = createTestJob(companyId, "Scorecard Test Job");

    service.createScorecard(JobScorecard.builder()
        .jobScorecardId(new JobScorecardId(UUID.randomUUID()))
        .organizationId(ORG_ID)
        .jobId(jobId)
        .dimensions("[{\"name\":\"technical\",\"weight\":40}]")
        .scoringGuidance("Rate 1-5")
        .status("active")
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(service.findActiveScorecardByJobIdAndOrganizationId(ORG_ID, jobId))
        .isPresent();
  }

  // ========== 16C: CandidateDocument + Interaction + Shortlist ==========

  @Test
  void candidateDocumentCreateAndReadBack() {
    CandidateDocumentService service = documentService();
    CandidateDocumentId docId = new CandidateDocumentId(UUID.randomUUID());

    CandidateDocument created = service.createDocument(CandidateDocument.builder()
        .candidateDocumentId(docId)
        .organizationId(ORG_ID)
        .candidateId(CANDIDATE_A)
        .documentType("resume")
        .title("Latest Resume")
        .storageRef("s3://bucket/resume.pdf")
        .contentHash("sha256:abc123")
        .language("en")
        .status(CandidateDocumentStatus.ACTIVE)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.candidateDocumentId()).isEqualTo(docId);
    assertThat(service.findDocumentByIdAndOrganizationId(ORG_ID, docId)).isPresent();
    assertThat(service.findDocumentByIdAndOrganizationId(ORG_OTHER, docId)).isEmpty();
    assertThat(service.findDocumentsByCandidateIdAndOrganizationId(ORG_ID, CANDIDATE_A))
        .extracting(CandidateDocument::candidateDocumentId)
        .contains(docId);
  }

  @Test
  void interactionCreateAndReadBack() {
    CandidateCompanyInteractionService service = interactionService();
    CompanyId companyId = createTestCompany("Interaction Test Co");
    CandidateCompanyInteractionId interactionId =
        new CandidateCompanyInteractionId(UUID.randomUUID());

    CandidateCompanyInteraction created = service.createInteraction(
        CandidateCompanyInteraction.builder()
            .candidateCompanyInteractionId(interactionId)
            .organizationId(ORG_ID)
            .candidateId(CANDIDATE_A)
            .companyId(companyId)
            .interactionType(InteractionType.SUBMISSION)
            .status(InteractionStatus.ACTIVE)
            .startedAt(NOW)
            .notes("Initial submission")
            .metadata("{}")
            .createdAt(NOW)
            .updatedAt(NOW)
            .build());

    assertThat(created.candidateCompanyInteractionId()).isEqualTo(interactionId);
    assertThat(service.findInteractionByIdAndOrganizationId(ORG_ID, interactionId))
        .isPresent();
    assertThat(service.findInteractionByIdAndOrganizationId(ORG_OTHER, interactionId))
        .isEmpty();
    assertThat(service.findInteractionsByCandidateAndCompany(ORG_ID, CANDIDATE_A, companyId))
        .hasSize(1);
  }

  @Test
  void interactionWithJobReference() {
    CandidateCompanyInteractionService service = interactionService();
    CompanyId companyId = createTestCompany("Job Interaction Co");
    JobId jobId = createTestJob(companyId, "Job Interaction Test");

    CandidateCompanyInteraction created = service.createInteraction(
        CandidateCompanyInteraction.builder()
            .candidateCompanyInteractionId(
                new CandidateCompanyInteractionId(UUID.randomUUID()))
            .organizationId(ORG_ID)
            .candidateId(CANDIDATE_A)
            .companyId(companyId)
            .jobId(jobId)
            .interactionType(InteractionType.INTERVIEW)
            .status(InteractionStatus.ACTIVE)
            .startedAt(NOW)
            .metadata("{}")
            .createdAt(NOW)
            .updatedAt(NOW)
            .build());

    assertThat(service.findInteractionsByJobId(ORG_ID, jobId)).hasSize(1);
    assertThat(created.jobId()).isEqualTo(jobId);
  }

  @Test
  void shortlistCreateAndReadBack() {
    ShortlistService service = shortlistService();
    CompanyId companyId = createTestCompany("Shortlist Co");
    JobId jobId = createTestJob(companyId, "Shortlist Test Job");
    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());

    Shortlist created = service.createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_ID)
        .jobId(jobId)
        .title("Round 1 Shortlist")
        .status(ShortlistStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.shortlistId()).isEqualTo(shortlistId);
    assertThat(service.findShortlistByIdAndOrganizationId(ORG_ID, shortlistId)).isPresent();
    assertThat(service.findShortlistByIdAndOrganizationId(ORG_OTHER, shortlistId)).isEmpty();
    assertThat(service.findShortlistsByJobIdAndOrganizationId(ORG_ID, jobId))
        .extracting(Shortlist::shortlistId)
        .contains(shortlistId);
  }

  @Test
  void shortlistCandidateCardCreateAndReadBack() {
    ShortlistService service = shortlistService();
    CompanyId companyId = createTestCompany("Card Test Co");
    JobId jobId = createTestJob(companyId, "Card Test Job");
    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());
    service.createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_ID)
        .jobId(jobId)
        .status(ShortlistStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    UUID anonCardId = UUID.randomUUID();
    ShortlistCandidateCard card = service.addCandidateCard(ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(UUID.randomUUID()))
        .organizationId(ORG_ID)
        .shortlistId(shortlistId)
        .anonymousCandidateCardId(anonCardId)
        .candidateId(CANDIDATE_A)
        .candidateProfileId(PROFILE_ID.value())
        .sortOrder(1)
        .status(ShortlistCandidateCardStatus.INCLUDED)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(card.anonymousCandidateCardId()).isEqualTo(anonCardId);
    List<ShortlistCandidateCard> cards =
        service.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlistId);
    assertThat(cards).hasSize(1);
    assertThat(cards.getFirst().candidateId()).isEqualTo(CANDIDATE_A);
    assertThat(cards.getFirst().status()).isEqualTo(ShortlistCandidateCardStatus.INCLUDED);
  }

  // ========== 16D: InterviewFeedback + Placement + Commission ==========

  @Test
  void interviewFeedbackCreateAndReadBack() {
    InterviewFeedbackService service = feedbackService();
    CompanyId companyId = createTestCompany("Feedback Co");
    JobId jobId = createTestJob(companyId, "Feedback Test Job");
    CandidateCompanyInteractionId interactionId = createTestInteraction(companyId, jobId);

    InterviewFeedbackId fbId = new InterviewFeedbackId(UUID.randomUUID());
    InterviewFeedback created = service.createFeedback(InterviewFeedback.builder()
        .interviewFeedbackId(fbId)
        .organizationId(ORG_ID)
        .candidateCompanyInteractionId(interactionId)
        .jobId(jobId)
        .interviewerName("John Smith")
        .interviewerRole("Engineering Manager")
        .interviewRound(1)
        .interviewDate(NOW)
        .outcome(InterviewOutcome.STRONG_YES)
        .ratings("{\"technical\": 5, \"communication\": 4}")
        .strengths("Great problem solver")
        .submittedByRole("consultant")
        .submittedByUserId(USER_ACCOUNT_ID)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.interviewFeedbackId()).isEqualTo(fbId);
    assertThat(service.findFeedbackByIdAndOrganizationId(ORG_ID, fbId)).isPresent();
    assertThat(service.findFeedbackByIdAndOrganizationId(ORG_OTHER, fbId)).isEmpty();
    assertThat(service.findFeedbackByInteractionIdAndOrganizationId(ORG_ID, interactionId))
        .hasSize(1);
    assertThat(service.findFeedbackByJobIdAndOrganizationId(ORG_ID, jobId))
        .extracting(InterviewFeedback::outcome)
        .contains(InterviewOutcome.STRONG_YES);
  }

  @Test
  void placementCreateAndReadBack() {
    PlacementService service = placementService();
    CompanyId companyId = createTestCompany("Placement Co");
    JobId jobId = createTestJob(companyId, "Placement Test Job");
    PlacementId placementId = new PlacementId(UUID.randomUUID());

    Placement created = service.createPlacement(Placement.builder()
        .placementId(placementId)
        .organizationId(ORG_ID)
        .jobId(jobId)
        .candidateId(CANDIDATE_A)
        .companyId(companyId)
        .status(PlacementStatus.OFFER_PENDING)
        .offerDetails("{\"salary\": 120000, \"currency\": \"USD\"}")
        .startDate(LocalDate.of(2026, 6, 1))
        .guaranteeDays(90)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.placementId()).isEqualTo(placementId);
    assertThat(created.startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    assertThat(created.guaranteeDays()).isEqualTo(90);
    assertThat(service.findPlacementByIdAndOrganizationId(ORG_ID, placementId)).isPresent();
    assertThat(service.findPlacementByIdAndOrganizationId(ORG_OTHER, placementId)).isEmpty();
    assertThat(service.findPlacementsByJobIdAndOrganizationId(ORG_ID, jobId))
        .hasSize(1);
    assertThat(service.findPlacementsByCandidateIdAndOrganizationId(ORG_ID, CANDIDATE_A))
        .extracting(Placement::placementId)
        .contains(placementId);
  }

  @Test
  void commissionCreateAndReadBack() {
    CommissionService service = commissionService();
    CompanyId companyId = createTestCompany("Commission Co");
    JobId jobId = createTestJob(companyId, "Commission Test Job");
    PlacementId placementId = createTestPlacement(jobId, companyId);
    CommissionId commissionId = new CommissionId(UUID.randomUUID());

    Commission created = service.createCommission(Commission.builder()
        .commissionId(commissionId)
        .organizationId(ORG_ID)
        .placementId(placementId)
        .consultantId(USER_ACCOUNT_ID)
        .status(CommissionStatus.PENDING)
        .commissionType(CommissionType.FULL_FEE)
        .amount(new BigDecimal("25000.00"))
        .currency("USD")
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());

    assertThat(created.commissionId()).isEqualTo(commissionId);
    assertThat(created.amount()).isEqualByComparingTo(new BigDecimal("25000.00"));
    assertThat(service.findCommissionByIdAndOrganizationId(ORG_ID, commissionId)).isPresent();
    assertThat(service.findCommissionByIdAndOrganizationId(ORG_OTHER, commissionId)).isEmpty();
    assertThat(service.findCommissionsByPlacementIdAndOrganizationId(ORG_ID, placementId))
        .hasSize(1);
    assertThat(service.findCommissionsByConsultantIdAndOrganizationId(ORG_ID, USER_ACCOUNT_ID))
        .extracting(Commission::commissionId)
        .contains(commissionId);
  }

  // ========== V12 Org-Scope Hardening: Cross-Org Negative Tests ==========

  @Test
  void companyContactCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-CC-Company");
    assertThatThrownBy(() -> insertRawCompanyContact(ORG_OTHER, companyId.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  @Test
  void companyPreferenceCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-CP-Company");
    assertThatThrownBy(() -> insertRawCompanyPreference(ORG_OTHER, companyId.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  @Test
  void jobRequirementCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-JR-Company");
    JobId jobId = createTestJob(companyId, "Neg-JR-Job");
    assertThatThrownBy(() -> insertRawJobRequirement(ORG_OTHER, jobId.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  @Test
  void jobScorecardCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-JS-Company");
    JobId jobId = createTestJob(companyId, "Neg-JS-Job");
    assertThatThrownBy(() -> insertRawJobScorecard(ORG_OTHER, jobId.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  @Test
  void shortlistCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-SL-Company");
    JobId jobId = createTestJob(companyId, "Neg-SL-Job");
    assertThatThrownBy(() -> insertRawShortlist(ORG_OTHER, jobId.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  @Test
  void shortlistCandidateCardCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-SC-Company");
    JobId jobId = createTestJob(companyId, "Neg-SC-Job");
    ShortlistId shortlistId = new ShortlistId(UUID.randomUUID());
    shortlistService().createShortlist(Shortlist.builder()
        .shortlistId(shortlistId)
        .organizationId(ORG_ID)
        .jobId(jobId)
        .status(ShortlistStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());
    assertThatThrownBy(() -> insertRawShortlistCandidateCard(
        ORG_OTHER, shortlistId.value(), CANDIDATE_A.value(), PROFILE_ID.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  @Test
  void placementCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-PL-Company");
    JobId jobId = createTestJob(companyId, "Neg-PL-Job");
    assertThatThrownBy(() -> insertRawPlacement(ORG_OTHER, jobId.value(),
        CANDIDATE_A.value(), companyId.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  @Test
  void commissionCrossOrgShouldReject() throws SQLException {
    CompanyId companyId = createTestCompany("Neg-CM-Company");
    JobId jobId = createTestJob(companyId, "Neg-CM-Job");
    PlacementId placementId = createTestPlacement(jobId, companyId);
    assertThatThrownBy(() -> insertRawCommission(ORG_OTHER, placementId.value()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("violates foreign key");
  }

  // ========== V10 Migration Validation ==========

  @Test
  void v10MigrationCreatesAllExpectedTables() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(22);
    assertThat(tableExists("recruiting", "profile_field_lineage")).isTrue();
    assertThat(tableExists("recruiting", "company")).isTrue();
    assertThat(tableExists("recruiting", "company_contact")).isTrue();
    assertThat(tableExists("recruiting", "company_preference")).isTrue();
    assertThat(tableExists("recruiting", "job")).isTrue();
    assertThat(tableExists("recruiting", "job_requirement")).isTrue();
    assertThat(tableExists("recruiting", "job_scorecard")).isTrue();
    assertThat(tableExists("recruiting", "candidate_document")).isTrue();
    assertThat(tableExists("recruiting", "candidate_company_interaction")).isTrue();
    assertThat(tableExists("recruiting", "shortlist")).isTrue();
    assertThat(tableExists("recruiting", "shortlist_candidate_card")).isTrue();
    assertThat(tableExists("recruiting", "interview_feedback")).isTrue();
    assertThat(tableExists("recruiting", "placement")).isTrue();
    assertThat(tableExists("recruiting", "commission")).isTrue();
  }

  @Test
  void v10MigrationAddsSearchableColumnsToCanidateProfile() throws SQLException {
    assertThat(columnExists("recruiting", "candidate_profile", "name")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "email")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "phone")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "citizenship")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "industry")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "projects")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "portfolio")).isTrue();
    assertThat(columnExists("recruiting", "candidate_profile", "work_history")).isTrue();
  }

  @Test
  void candidateProfileFieldPathContainsNewV21Paths() {
    assertThat(CandidateProfileFieldPath.IDENTITY_CITIZENSHIP).isNotNull();
    assertThat(CandidateProfileFieldPath.EXPERIENCE_PROJECTS).isNotNull();
    assertThat(CandidateProfileFieldPath.EXPERIENCE_PORTFOLIO).isNotNull();
    assertThat(CandidateProfileFieldPath.EXPERIENCE_INDUSTRY).isNotNull();
    assertThat(CandidateProfileFieldPath.INTENT_MOTIVATION_TOWARD_OPPORTUNITY).isNotNull();
  }

  // ========== Helpers ==========

  private CompanyId createTestCompany(String baseName) {
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    companyService().createCompany(Company.builder()
        .companyId(companyId)
        .organizationId(ORG_ID)
        .name(baseName + " " + UUID.randomUUID().toString().substring(0, 8))
        .status(CompanyStatus.ACTIVE)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());
    return companyId;
  }

  private JobId createTestJob(CompanyId companyId, String title) {
    JobId jobId = new JobId(UUID.randomUUID());
    jobService().createJob(Job.builder()
        .jobId(jobId)
        .organizationId(ORG_ID)
        .companyId(companyId)
        .title(title + " " + UUID.randomUUID().toString().substring(0, 8))
        .status(JobStatus.DRAFT)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());
    return jobId;
  }

  private CandidateCompanyInteractionId createTestInteraction(
      CompanyId companyId, JobId jobId) {
    CandidateCompanyInteractionId id =
        new CandidateCompanyInteractionId(UUID.randomUUID());
    interactionService().createInteraction(CandidateCompanyInteraction.builder()
        .candidateCompanyInteractionId(id)
        .organizationId(ORG_ID)
        .candidateId(CANDIDATE_A)
        .companyId(companyId)
        .jobId(jobId)
        .interactionType(InteractionType.INTERVIEW)
        .status(InteractionStatus.ACTIVE)
        .startedAt(NOW)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());
    return id;
  }

  private PlacementId createTestPlacement(JobId jobId, CompanyId companyId) {
    PlacementId placementId = new PlacementId(UUID.randomUUID());
    placementService().createPlacement(Placement.builder()
        .placementId(placementId)
        .organizationId(ORG_ID)
        .jobId(jobId)
        .candidateId(CANDIDATE_A)
        .companyId(companyId)
        .status(PlacementStatus.OFFER_PENDING)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build());
    return placementId;
  }

  private ProfileFieldLineageService lineageService() {
    return new ProfileFieldLineageService(new JdbcProfileFieldLineagePort(dataSource));
  }

  private CompanyService companyService() {
    return new CompanyService(
        new JdbcCompanyPersistencePort(dataSource),
        new JdbcCompanyContactPersistencePort(dataSource),
        new JdbcCompanyPreferencePersistencePort(dataSource));
  }

  private JobService jobService() {
    return new JobService(
        new JdbcJobPersistencePort(dataSource),
        new JdbcJobRequirementPersistencePort(dataSource),
        new JdbcJobScorecardPersistencePort(dataSource));
  }

  private CandidateDocumentService documentService() {
    return new CandidateDocumentService(
        new JdbcCandidateDocumentPersistencePort(dataSource));
  }

  private CandidateCompanyInteractionService interactionService() {
    return new CandidateCompanyInteractionService(
        new JdbcCandidateCompanyInteractionPersistencePort(dataSource));
  }

  private ShortlistService shortlistService() {
    return new ShortlistService(
        new JdbcShortlistPersistencePort(dataSource),
        new JdbcShortlistCandidateCardPersistencePort(dataSource));
  }

  private InterviewFeedbackService feedbackService() {
    return new InterviewFeedbackService(
        new JdbcInterviewFeedbackPersistencePort(dataSource));
  }

  private PlacementService placementService() {
    return new PlacementService(new JdbcPlacementPersistencePort(dataSource));
  }

  private CommissionService commissionService() {
    return new CommissionService(new JdbcCommissionPersistencePort(dataSource));
  }

  private static void insertOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id, legal_name, display_name, status, default_timezone
            ) VALUES (?, ?, ?, 'active', 'UTC')
            """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, "Task 16 Org " + organizationId);
      statement.setString(3, "Task 16 Org");
      statement.executeUpdate();
    }
  }

  private static void insertUserAccount(UUID organizationId, UUID userAccountId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id, organization_id, email, display_name, status
            ) VALUES (?, ?, ?, ?, 'active')
            """)) {
      statement.setObject(1, userAccountId);
      statement.setObject(2, organizationId);
      statement.setString(3, "task16@example.com");
      statement.setString(4, "Task 16 User");
      statement.executeUpdate();
    }
  }

  private static void insertCandidate(UUID organizationId, CandidateId candidateId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO recruiting.candidate (
              candidate_id, organization_id, status, privacy_status
            ) VALUES (?, ?, 'new', 'internal_only')
            """)) {
      statement.setObject(1, candidateId.value());
      statement.setObject(2, organizationId);
      statement.executeUpdate();
    }
  }

  private static void insertCandidateProfile(
      UUID organizationId, CandidateProfileId profileId, CandidateId candidateId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO recruiting.candidate_profile (
              candidate_profile_id, organization_id, candidate_id,
              status, profile_version, field_status_map, metadata
            ) VALUES (?, ?, ?, 'draft', 1, '{}'::jsonb, '{}'::jsonb)
            """)) {
      statement.setObject(1, profileId.value());
      statement.setObject(2, organizationId);
      statement.setObject(3, candidateId.value());
      statement.executeUpdate();
    }
  }

  // ========== Raw JDBC helpers for cross-org negative tests ==========

  private static void insertRawCompanyContact(UUID orgId, UUID companyId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.company_contact (
              company_contact_id, organization_id, company_id, name, status, metadata
            ) VALUES (?, ?, ?, ?, 'active', '{}'::jsonb)
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, companyId);
      ps.setString(4, "Cross-Org Test");
      ps.executeUpdate();
    }
  }

  private static void insertRawCompanyPreference(UUID orgId, UUID companyId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.company_preference (
              company_preference_id, organization_id, company_id,
              preference_key, preference_value
            ) VALUES (?, ?, ?, 'test_key', '{}'::jsonb)
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, companyId);
      ps.executeUpdate();
    }
  }

  private static void insertRawJobRequirement(UUID orgId, UUID jobId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.job_requirement (
              job_requirement_id, organization_id, job_id,
              requirement_type, label, importance
            ) VALUES (?, ?, ?, 'skill', 'Test Skill', 'must_have')
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, jobId);
      ps.executeUpdate();
    }
  }

  private static void insertRawJobScorecard(UUID orgId, UUID jobId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.job_scorecard (
              job_scorecard_id, organization_id, job_id, dimensions, status, metadata
            ) VALUES (?, ?, ?, '[]'::jsonb, 'draft', '{}'::jsonb)
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, jobId);
      ps.executeUpdate();
    }
  }

  private static void insertRawShortlist(UUID orgId, UUID jobId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.shortlist (
              shortlist_id, organization_id, job_id, status, metadata
            ) VALUES (?, ?, ?, 'draft', '{}'::jsonb)
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, jobId);
      ps.executeUpdate();
    }
  }

  private static void insertRawShortlistCandidateCard(
      UUID orgId, UUID shortlistId, UUID candidateId, UUID profileId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.shortlist_candidate_card (
              shortlist_candidate_card_id, organization_id, shortlist_id,
              anonymous_candidate_card_id, candidate_id, candidate_profile_id,
              status, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, 'draft', '{}'::jsonb)
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, shortlistId);
      ps.setObject(4, UUID.randomUUID());
      ps.setObject(5, candidateId);
      ps.setObject(6, profileId);
      ps.executeUpdate();
    }
  }

  private static void insertRawPlacement(
      UUID orgId, UUID jobId, UUID candidateId, UUID companyId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.placement (
              placement_id, organization_id, job_id, candidate_id, company_id, status, metadata
            ) VALUES (?, ?, ?, ?, ?, 'offer_pending', '{}'::jsonb)
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, jobId);
      ps.setObject(4, candidateId);
      ps.setObject(5, companyId);
      ps.executeUpdate();
    }
  }

  private static void insertRawCommission(UUID orgId, UUID placementId) throws SQLException {
    try (Connection c = connection();
        PreparedStatement ps = c.prepareStatement("""
            INSERT INTO recruiting.commission (
              commission_id, organization_id, placement_id, consultant_id,
              status, commission_type, metadata
            ) VALUES (?, ?, ?, ?, 'pending', 'full_fee', '{}'::jsonb)
            """)) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, orgId);
      ps.setObject(3, placementId);
      ps.setObject(4, USER_ACCOUNT_ID);
      ps.executeUpdate();
    }
  }

  private static boolean tableExists(String schema, String table) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (var rs = statement.executeQuery()) {
        rs.next();
        return rs.getBoolean(1);
      }
    }
  }

  private static boolean columnExists(String schema, String table, String column)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name = ?)")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      statement.setString(3, column);
      try (var rs = statement.executeQuery()) {
        rs.next();
        return rs.getBoolean(1);
      }
    }
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static DataSource postgresDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        return connection();
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), username, password);
      }

      @Override
      public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
      }

      @Override
      public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
      }

      @Override
      public void setLoginTimeout(int seconds) {
        DriverManager.setLoginTimeout(seconds);
      }

      @Override
      public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("DriverManager parent logger is not supported");
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
          return iface.cast(this);
        }
        throw new SQLException("DataSource does not wrap " + iface.getName());
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
      }
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }
}
