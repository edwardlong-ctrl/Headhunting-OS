package com.recruitingtransactionos.coreapi.pilotdata;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.persistence.JdbcCandidatePersistencePort;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceReference;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.persistence.JdbcCandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CreateCandidateProfileRequest;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyContactPersistencePort;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyPersistencePort;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyPreferencePersistencePort;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemOrigin;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemRegistrationCommand;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemType;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobScorecardPersistencePort;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class PilotDataService {

  private static final String PILOT_DATASET_KEY = "semiconductor-pilot-v1";

  private final DataSource dataSource;
  private final PasswordEncoder passwordEncoder;
  private final PilotDataPrivacyValidator privacyValidator;
  private final CandidateService candidateService;
  private final CandidateProfileService candidateProfileService;
  private final CompanyService companyService;
  private final JobService jobService;
  private final GovernedIntakeService governedIntakeService;

  public PilotDataService(
      DataSource dataSource,
      PasswordEncoder passwordEncoder,
      PilotDataPrivacyValidator privacyValidator) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    this.privacyValidator = Objects.requireNonNull(privacyValidator, "privacyValidator must not be null");
    this.candidateService = new CandidateService(new JdbcCandidatePersistencePort(dataSource));
    this.candidateProfileService = new CandidateProfileService(
        new JdbcCandidateProfilePersistencePort(dataSource));
    this.companyService = new CompanyService(
        new JdbcCompanyPersistencePort(dataSource),
        new JdbcCompanyContactPersistencePort(dataSource),
        new JdbcCompanyPreferencePersistencePort(dataSource));
    this.jobService = new JobService(
        new JdbcJobPersistencePort(dataSource),
        new JdbcJobRequirementPersistencePort(dataSource),
        new JdbcJobScorecardPersistencePort(dataSource));
    this.governedIntakeService = new GovernedIntakeService(
        new JdbcSourceItemPersistencePort(dataSource),
        new JdbcInformationPacketPersistencePort(dataSource));
  }

  public PilotDataReport rebuild(PilotDataset dataset) {
    reset(dataset.organization().organizationId(), true);
    importDataset(dataset);
    PilotDataReport validation = validate(dataset.organization().organizationId());
    return validation.withCommand("rebuild");
  }

  public PilotDataReport importDataset(PilotDataset dataset) {
    Objects.requireNonNull(dataset, "dataset must not be null");
    PilotDataValidationResult structureResult = validateImportPreconditions(dataset);
    if (!structureResult.valid()) {
      return invalidImportReport(structureResult);
    }
    PilotDataValidationResult privacyResult = privacyValidator.validate(dataset);
    if (!privacyResult.valid()) {
      return invalidImportReport(privacyResult);
    }
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        insertOrganization(connection, dataset.organization());
        insertAccounts(connection, dataset);
        connection.commit();
      } catch (RuntimeException | SQLException exception) {
        connection.rollback();
        throw exception;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to import pilot dataset", exception);
    }
    insertCompanies(dataset);
    insertJobs(dataset);
    insertSourceDocuments(dataset);
    insertCandidates(dataset);
    return validate(dataset.organization().organizationId()).withCommand("import");
  }

  private static PilotDataValidationResult validateImportPreconditions(PilotDataset dataset) {
    List<PilotDataValidationResult.Issue> issues = new ArrayList<>();
    Set<UUID> accountIds = new HashSet<>();
    boolean consultantPresent = false;
    for (PilotDataset.AccountSeed account : dataset.accounts()) {
      accountIds.add(account.userAccountId());
      if ("consultant".equals(account.role())) {
        consultantPresent = true;
      }
    }
    if (!consultantPresent) {
      issues.add(new PilotDataValidationResult.Issue(
          "pilot_consultant_account_missing",
          "accounts",
          "Pilot import requires one consultant account for source lineage and ownership"));
    }

    Set<UUID> companyIds = new HashSet<>();
    for (PilotDataset.CompanySeed company : dataset.companies()) {
      companyIds.add(company.companyId());
      UUID ownerConsultantId = parseUuid(
          company.ownerConsultantId(),
          "company_owner_account_malformed",
          company.companyId().toString(),
          "Company ownerConsultantId must be a UUID",
          issues);
      if (ownerConsultantId != null && !accountIds.contains(ownerConsultantId)) {
        issues.add(new PilotDataValidationResult.Issue(
            "company_owner_account_missing",
            company.companyId().toString(),
            "Company ownerConsultantId is not present in seeded accounts"));
      }
    }

    for (PilotDataset.JobSeed job : dataset.jobs()) {
      if (!companyIds.contains(job.companyId())) {
        issues.add(new PilotDataValidationResult.Issue(
            "job_company_missing",
            job.jobId().toString(),
            "Job references a company that is not present in seeded companies"));
      }
      validateJobStatus(job.status(), job.jobId().toString(), issues);
      UUID ownerConsultantId = parseUuid(
          job.ownerConsultantId(),
          "job_owner_account_malformed",
          job.jobId().toString(),
          "Job ownerConsultantId must be a UUID",
          issues);
      if (ownerConsultantId != null && !accountIds.contains(ownerConsultantId)) {
        issues.add(new PilotDataValidationResult.Issue(
            "job_owner_account_missing",
            job.jobId().toString(),
            "Job ownerConsultantId is not present in seeded accounts"));
      }
    }

    Set<String> sourceDocumentRefs = new HashSet<>();
    for (PilotDataset.SourceDocumentSeed sourceDocument : dataset.sourceDocuments()) {
      sourceDocumentRefs.add(sourceDocument.documentRef());
      validateSourceDocumentType(
          sourceDocument.sourceType(),
          sourceDocument.sourceItemId().toString(),
          issues);
    }
    for (PilotDataset.CandidateSeed candidate : dataset.candidates()) {
      parseUuid(
          candidate.candidateId(),
          "candidate_id_malformed",
          candidate.candidateId(),
          "Candidate candidateId must be a UUID",
          issues);
      parseUuid(
          candidate.profileId(),
          "candidate_profile_id_malformed",
          candidate.candidateId(),
          "Candidate profileId must be a UUID",
          issues);
      validateCandidateStatus(candidate.status(), candidate.candidateId(), issues);
      if (!sourceDocumentRefs.contains(candidate.sourceDocumentRef())) {
        issues.add(new PilotDataValidationResult.Issue(
            "candidate_missing_source_document",
            candidate.candidateId(),
            "Candidate sourceDocumentRef is not present in seeded source documents"));
      }
    }

    return new PilotDataValidationResult(issues.isEmpty(), issues);
  }

  private static UUID parseUuid(
      String rawValue,
      String code,
      String subject,
      String message,
      List<PilotDataValidationResult.Issue> issues) {
    try {
      return UUID.fromString(rawValue);
    } catch (IllegalArgumentException exception) {
      issues.add(new PilotDataValidationResult.Issue(code, subject, message));
      return null;
    }
  }

  private static void validateJobStatus(
      String rawValue,
      String subject,
      List<PilotDataValidationResult.Issue> issues) {
    try {
      JobStatus.fromWireValue(rawValue);
    } catch (IllegalArgumentException exception) {
      issues.add(new PilotDataValidationResult.Issue(
          "job_status_unsupported",
          subject,
          "Job status is not supported by the domain model"));
    }
  }

  private static void validateCandidateStatus(
      String rawValue,
      String subject,
      List<PilotDataValidationResult.Issue> issues) {
    try {
      CandidateStatus.fromWireValue(rawValue);
    } catch (IllegalArgumentException exception) {
      issues.add(new PilotDataValidationResult.Issue(
          "candidate_status_unsupported",
          subject,
          "Candidate status is not supported by the domain model"));
    }
  }

  private static void validateSourceDocumentType(
      String rawValue,
      String subject,
      List<PilotDataValidationResult.Issue> issues) {
    try {
      SourceItemType.fromWireValue(rawValue);
    } catch (IllegalArgumentException exception) {
      issues.add(new PilotDataValidationResult.Issue(
          "source_document_type_unsupported",
          subject,
          "Source document type is not supported by governed intake"));
    }
  }

  private static PilotDataReport invalidImportReport(PilotDataValidationResult validationResult) {
    return new PilotDataReport(
        "import",
        false,
        Map.of(),
        Map.of("passed", false),
        Map.of(),
        Map.of(),
        validationResult.issues().stream()
            .map(PilotDataValidationResult.Issue::code)
            .distinct()
            .toList(),
        validationResult.issues());
  }

  public PilotDataReport validate(UUID organizationId) {
    try (Connection connection = dataSource.getConnection()) {
      Map<String, Integer> counts = new LinkedHashMap<>();
      counts.put("accounts", count(connection, "identity.user_account", organizationId));
      counts.put("seededRoleAssignments", count(connection, "identity.role_assignment", organizationId));
      counts.put("candidates", count(connection, "recruiting.candidate", organizationId));
      counts.put("companies", count(connection, "recruiting.company", organizationId));
      counts.put("jobs", count(connection, "recruiting.job", organizationId));
      counts.put("activeJobs", countWhere(connection, "recruiting.job", organizationId, "status = 'activated'"));
      counts.put(
          "underReviewJobs",
          countWhere(connection, "recruiting.job", organizationId, "status = 'intake_review'"));
      counts.put("sourceDocuments", count(connection, "intake.source_item", organizationId));
      counts.put("seededShortlists", count(connection, "recruiting.shortlist", organizationId));
      counts.put("seededDisclosureRecords", count(connection, "privacy.disclosure_record", organizationId));
      counts.put("canonicalWriteAttempts", count(connection, "governance.canonical_write_attempt", organizationId));
      counts.put(
          "candidatesWithCurrentProfiles",
          countWhere(connection, "recruiting.candidate", organizationId, "current_profile_id IS NOT NULL"));
      counts.put(
          "unsupportedAccountEmailDomains",
          countWhere(
              connection,
              "identity.user_account",
              organizationId,
              "email NOT LIKE '%.example.test'"));
      List<PilotDataValidationResult.Issue> storedProfilePrivacyIssues =
          storedProfilePrivacyIssues(organizationId);
      counts.put("candidateProfilePrivacyIssues", storedProfilePrivacyIssues.size());

      Map<String, Boolean> privacyChecks = new LinkedHashMap<>();
      privacyChecks.put("passed",
          counts.get("unsupportedAccountEmailDomains") == 0
              && storedProfilePrivacyIssues.isEmpty());
      privacyChecks.put("reservedAccountEmailDomains", counts.get("unsupportedAccountEmailDomains") == 0);
      privacyChecks.put("candidateProfileFields", storedProfilePrivacyIssues.isEmpty());

      Map<String, Boolean> seededAccountChecks = new LinkedHashMap<>();
      seededAccountChecks.put("accountsPresent", counts.get("accounts") == 5);
      seededAccountChecks.put("roleAssignmentsPresent", counts.get("seededRoleAssignments") == 5);

      Map<String, Boolean> workflowAuditChecks = new LinkedHashMap<>();
      workflowAuditChecks.put("candidateCurrentProfilesLinked", counts.get("candidatesWithCurrentProfiles") == 75);
      workflowAuditChecks.put("noShortcutShortlists", counts.get("seededShortlists") == 0);
      workflowAuditChecks.put("noShortcutDisclosureRecords", counts.get("seededDisclosureRecords") == 0);
      workflowAuditChecks.put("noCanonicalWriteAttemptsSeeded", counts.get("canonicalWriteAttempts") == 0);
      workflowAuditChecks.put("sourceDocumentsPresent", counts.get("sourceDocuments") >= 83);

      List<String> failedGateReasons = new ArrayList<>(failedGateReasons(
          counts,
          privacyChecks,
          workflowAuditChecks,
          seededAccountChecks));
      storedProfilePrivacyIssues.stream()
          .map(PilotDataValidationResult.Issue::code)
          .distinct()
          .forEach(failedGateReasons::add);
      boolean valid = failedGateReasons.isEmpty();
      return new PilotDataReport(
          "validate",
          valid,
          counts,
          privacyChecks,
          workflowAuditChecks,
          seededAccountChecks,
          failedGateReasons,
          storedProfilePrivacyIssues);
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to validate pilot dataset", exception);
    }
  }

  public PilotDataReport export(UUID organizationId) {
    PilotDataReport report = validate(organizationId);
    return report.withCommand("export");
  }

  public PilotDataReport reset(UUID organizationId, boolean allowReset) {
    new PilotDataCommandOptions(PilotDataCommand.RESET, allowReset).requireResetAllowed();
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        deletePilotOrganizationRows(connection, organizationId);
        connection.commit();
      } catch (RuntimeException | SQLException exception) {
        connection.rollback();
        throw exception;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to reset pilot dataset", exception);
    }
    return new PilotDataReport("reset", true, Map.of(), List.of());
  }

  private static void insertOrganization(Connection connection, PilotDataset.OrganizationSeed organization)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO identity.organization (
          organization_id, legal_name, display_name, status, default_timezone, metadata
        ) VALUES (?, ?, ?, 'active', ?, ?::jsonb)
        ON CONFLICT (organization_id) DO UPDATE SET
          legal_name = EXCLUDED.legal_name,
          display_name = EXCLUDED.display_name,
          status = 'active',
          default_timezone = EXCLUDED.default_timezone,
          metadata = EXCLUDED.metadata,
          updated_at = now(),
          version = identity.organization.version + 1
        """)) {
      statement.setObject(1, organization.organizationId());
      statement.setString(2, organization.legalName());
      statement.setString(3, organization.displayName());
      statement.setString(4, organization.defaultTimezone());
      statement.setString(5, metadata("organization", organization.organizationId().toString()));
      statement.executeUpdate();
    }
  }

  private void insertAccounts(Connection connection, PilotDataset dataset) throws SQLException {
    for (PilotDataset.AccountSeed account : dataset.accounts()) {
      try (PreparedStatement statement = connection.prepareStatement("""
          INSERT INTO identity.user_account (
            user_account_id, organization_id, email, display_name, status, password_hash, metadata
          ) VALUES (?, ?, ?, ?, 'active', ?, ?::jsonb)
          ON CONFLICT (user_account_id) DO UPDATE SET
            email = EXCLUDED.email,
            display_name = EXCLUDED.display_name,
            status = 'active',
            password_hash = EXCLUDED.password_hash,
            metadata = EXCLUDED.metadata,
            updated_at = now(),
            version = identity.user_account.version + 1
          """)) {
        statement.setObject(1, account.userAccountId());
        statement.setObject(2, dataset.organization().organizationId());
        statement.setString(3, account.email());
        statement.setString(4, account.displayName());
        statement.setString(5, passwordEncoder.encode(account.password()));
        statement.setString(6, metadata("account", account.role()));
        statement.executeUpdate();
      }
      try (PreparedStatement statement = connection.prepareStatement("""
          INSERT INTO identity.role_assignment (
            role_assignment_id, organization_id, user_account_id, role, scope_type, scope_id,
            status, reason, metadata
          ) VALUES (?, ?, ?, ?::governance.actor_role, 'organization', ?, 'active', ?, ?::jsonb)
          ON CONFLICT (role_assignment_id) DO UPDATE SET
            status = 'active',
            reason = EXCLUDED.reason,
            metadata = EXCLUDED.metadata,
            updated_at = now(),
            version = identity.role_assignment.version + 1
          """)) {
        statement.setObject(1, uuid("role-" + account.userAccountId() + "-" + account.role()));
        statement.setObject(2, dataset.organization().organizationId());
        statement.setObject(3, account.userAccountId());
        statement.setString(4, account.role());
        statement.setObject(5, dataset.organization().organizationId());
        statement.setString(6, "Task 38 pilot data bootstrap");
        statement.setString(7, metadata("role", account.role()));
        statement.executeUpdate();
      }
    }
  }

  private void insertCompanies(PilotDataset dataset) {
    for (PilotDataset.CompanySeed company : dataset.companies()) {
      CompanyId companyId = new CompanyId(company.companyId());
      if (companyService.findCompanyByIdAndOrganizationId(
          dataset.organization().organizationId(),
          companyId).isPresent()) {
        continue;
      }
      Instant now = Instant.now();
      companyService.createCompany(Company.builder()
          .companyId(companyId)
          .organizationId(dataset.organization().organizationId())
          .name(company.name())
          .displayName(company.name())
          .industry(company.industry())
          .headquartersLocation(company.headquartersLocation())
          .sizeBand(company.sizeBand())
          .status(CompanyStatus.ACTIVE)
          .ownerConsultantId(UUID.fromString(company.ownerConsultantId()))
          .metadata(company.metadata())
          .createdAt(now)
          .updatedAt(now)
          .version(1)
          .build());
    }
  }

  private void insertJobs(PilotDataset dataset) {
    UUID semiconductorIndustryPackId = UUID.fromString("00000000-0000-0000-0000-000000280002");
    for (PilotDataset.JobSeed job : dataset.jobs()) {
      JobId jobId = new JobId(job.jobId());
      if (jobService.findJobByIdAndOrganizationId(
          dataset.organization().organizationId(),
          jobId).isPresent()) {
        continue;
      }
      Instant now = Instant.now();
      jobService.createJob(Job.builder()
          .jobId(jobId)
          .organizationId(dataset.organization().organizationId())
          .companyId(new CompanyId(job.companyId()))
          .title(job.title())
          .description("Synthetic pilot job for " + job.roleFamily())
          .location(job.location())
          .seniorityBand(job.seniorityBand())
          .roleFamily(job.roleFamily())
          .employmentType("full_time")
          .compensation(job.compensation())
          .status(JobStatus.fromWireValue(job.status()))
          .commercialTerms("{\"feeRate\":\"25%\",\"synthetic\":true}")
          .ownerConsultantId(UUID.fromString(job.ownerConsultantId()))
          .activatedAt("activated".equals(job.status()) ? now : null)
          .industryPackId(semiconductorIndustryPackId)
          .metadata(job.metadata())
          .createdAt(now)
          .updatedAt(now)
          .version(1)
          .build());
    }
  }

  private void insertCandidates(PilotDataset dataset) {
    UUID semiconductorIndustryPackId = UUID.fromString("00000000-0000-0000-0000-000000280002");
    UUID ownerConsultantId = dataset.accounts().stream()
        .filter(account -> "consultant".equals(account.role()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("pilot consultant account missing"))
        .userAccountId();
    Map<String, UUID> sourceDocumentIds = new LinkedHashMap<>();
    for (PilotDataset.SourceDocumentSeed sourceDocument : dataset.sourceDocuments()) {
      sourceDocumentIds.put(sourceDocument.documentRef(), sourceDocument.sourceItemId());
    }
    for (PilotDataset.CandidateSeed candidate : dataset.candidates()) {
      UUID candidateId = UUID.fromString(candidate.candidateId());
      CandidateId typedCandidateId = new CandidateId(candidateId);
      if (candidateService.findCandidateByIdAndOrganizationId(
          dataset.organization().organizationId(),
          typedCandidateId).isEmpty()) {
        Instant now = Instant.now();
        candidateService.createCandidate(Candidate.builder()
            .candidateId(typedCandidateId)
            .organizationId(dataset.organization().organizationId())
            .status(CandidateStatus.fromWireValue(candidate.status()))
            .privacyStatus("internal_only")
            .ownerConsultantId(ownerConsultantId)
            .lastActivityAt(now)
            .defaultIndustryPackId(semiconductorIndustryPackId)
            .metadata(candidate.metadata())
            .createdAt(now)
            .updatedAt(now)
            .version(1)
            .build());
      }
      if (candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
          dataset.organization().organizationId(),
          typedCandidateId).isEmpty()) {
        UUID sourceItemId = sourceDocumentIds.get(candidate.sourceDocumentRef());
        CandidateProfileId profileId = candidateProfileService.createCandidateProfile(new CreateCandidateProfileRequest(
            dataset.organization().organizationId(),
            typedCandidateId,
            new CandidateProfileVersion(1),
            candidateProfileFields(candidate, sourceItemId, ownerConsultantId)))
            .candidateProfileId();
        candidateService.linkCurrentProfile(
            dataset.organization().organizationId(),
            typedCandidateId,
            profileId);
      } else {
        CandidateProfileId profileId = candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
            dataset.organization().organizationId(),
            typedCandidateId)
            .orElseThrow()
            .candidateProfileId();
        candidateService.findCandidateByIdAndOrganizationId(
            dataset.organization().organizationId(),
            typedCandidateId)
            .filter(existing -> existing.currentProfileId() == null)
            .ifPresent(existing -> candidateService.linkCurrentProfile(
                dataset.organization().organizationId(),
                typedCandidateId,
                profileId));
      }
    }
  }

  private void insertSourceDocuments(PilotDataset dataset) {
    UUID consultantActorId = dataset.accounts().stream()
        .filter(account -> "consultant".equals(account.role()))
        .findFirst()
        .orElseThrow()
        .userAccountId();
    for (PilotDataset.SourceDocumentSeed sourceDocument : dataset.sourceDocuments()) {
      SourceItemId sourceItemId = new SourceItemId(sourceDocument.sourceItemId());
      if (governedIntakeService.findSourceItem(
          dataset.organization().organizationId(),
          sourceItemId).isPresent()) {
        continue;
      }
      governedIntakeService.registerSourceItem(SourceItemRegistrationCommand.builder()
          .organizationId(dataset.organization().organizationId())
          .sourceType(SourceItemType.fromWireValue(sourceDocument.sourceType()))
          .origin(SourceItemOrigin.SYSTEM_IMPORT)
          .title(sourceDocument.title())
          .contentHash(sha256(sourceDocument.body()))
          .externalRef(sourceDocument.documentRef())
          .storageRef("pilotdata/" + sourceDocument.filename())
          .rawRef(sourceDocument.documentRef())
          .language("en")
          .uploadedByActorType(ActorRole.CONSULTANT)
          .uploadedByActorId(consultantActorId)
          .receivedAt(Instant.now())
          .metadataJson(sourceDocument.metadata())
          .status(SourceItemStatus.REGISTERED)
          .mimeType("text/plain")
          .fileSizeBytes((long) sourceDocument.body().getBytes(StandardCharsets.UTF_8).length)
          .originalFilename(sourceDocument.filename())
          .scanStatus("not_scanned")
          .sourceItemId(sourceItemId)
          .build());
    }
  }

  private static void deletePilotOrganizationRows(Connection connection, UUID organizationId)
      throws SQLException {
    String[] tables = {
        "operations.notification_delivery_attempt",
        "operations.notification_schedule",
        "operations.notification_preference",
        "operations.notification",
        "audit.audit_log",
        "privacy.client_unlock_request",
        "privacy.disclosure_record",
        "privacy.unlock_decision",
        "privacy.consent_record",
        "privacy.reidentification_risk_assessment",
        "recruiting.follow_up_submission",
        "recruiting.commission",
        "recruiting.placement",
        "recruiting.interview_feedback_suggestion",
        "recruiting.match_calibration_signal",
        "recruiting.interview_feedback",
        "recruiting.match_report",
        "recruiting.shortlist_candidate_card",
        "recruiting.shortlist",
        "recruiting.candidate_company_interaction",
        "recruiting.candidate_document",
        "recruiting.job_scorecard",
        "recruiting.job_requirement",
        "recruiting.job",
        "recruiting.company_preference",
        "recruiting.company_contact",
        "recruiting.company",
        "recruiting.profile_field_lineage",
        "recruiting.candidate_profile",
        "recruiting.candidate",
        "intake.parsed_document_span",
        "intake.parsed_document_chunk",
        "intake.parsed_document",
        "intake.extraction_run",
        "intake.information_packet_source_item",
        "intake.source_item",
        "intake.information_packet",
        "recruiting.source_item",
        "recruiting.information_packet",
        "governance.canonical_write_attempt",
        "workflow.workflow_event",
        "governance.review_event",
        "governance.claim_ledger_item",
        "governance.ai_task_run",
        "governance.ai_task_definition",
        "governance.config_entry",
        "identity.session",
        "identity.role_assignment",
        "identity.user_account",
        "identity.organization"
    };
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE recruiting." + "candidate SET current_profile_id = NULL WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      statement.executeUpdate();
    } catch (SQLException exception) {
      if (!"42P01".equals(exception.getSQLState())) {
        throw exception;
      }
    }
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE governance.claim_ledger_item SET review_event_id = NULL WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      statement.executeUpdate();
    } catch (SQLException exception) {
      if (!"42P01".equals(exception.getSQLState())) {
        throw exception;
      }
    }
    for (String table : tables) {
      try (PreparedStatement statement = connection.prepareStatement(
          "DELETE FROM " + table + " WHERE organization_id = ?")) {
        statement.setObject(1, organizationId);
        statement.executeUpdate();
      } catch (SQLException exception) {
        if (!"42P01".equals(exception.getSQLState())) {
          throw exception;
        }
      }
    }
  }

  private List<PilotDataValidationResult.Issue> storedProfilePrivacyIssues(UUID organizationId) {
    List<PilotDataValidationResult.Issue> issues = new ArrayList<>();
    for (Candidate candidate : candidateService.findAllCandidatesByOrganizationId(organizationId)) {
      candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(
              organizationId,
              candidate.candidateId())
          .map(CandidateProfile::fields)
          .ifPresent(fields -> issues.addAll(privacyValidator.validateCandidateProfileFields(
              candidate.candidateId().value().toString(),
              fields)));
    }
    return List.copyOf(issues);
  }

  private static int count(Connection connection, String tableName, UUID organizationId) throws SQLException {
    return countWhere(connection, tableName, organizationId, "true");
  }

  private static int countWhere(
      Connection connection,
      String tableName,
      UUID organizationId,
      String whereClause) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT count(*) FROM " + tableName + " WHERE organization_id = ? AND " + whereClause)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static List<String> failedGateReasons(
      Map<String, Integer> counts,
      Map<String, Boolean> privacyChecks,
      Map<String, Boolean> workflowAuditChecks,
      Map<String, Boolean> seededAccountChecks) {
    List<String> reasons = new ArrayList<>();
    requireCount(counts, "accounts", 5, reasons);
    requireCount(counts, "seededRoleAssignments", 5, reasons);
    requireCount(counts, "candidates", 75, reasons);
    requireCount(counts, "activeJobs", 5, reasons);
    requireCount(counts, "underReviewJobs", 3, reasons);
    requireAtLeast(counts, "sourceDocuments", 83, reasons);
    collectFailedChecks("privacy", privacyChecks, reasons);
    collectFailedChecks("workflow", workflowAuditChecks, reasons);
    collectFailedChecks("seeded_accounts", seededAccountChecks, reasons);
    return List.copyOf(reasons);
  }

  private static void requireCount(
      Map<String, Integer> counts,
      String key,
      int expected,
      List<String> reasons) {
    if (counts.getOrDefault(key, -1) != expected) {
      reasons.add(key + "_expected_" + expected);
    }
  }

  private static void requireAtLeast(
      Map<String, Integer> counts,
      String key,
      int minimum,
      List<String> reasons) {
    if (counts.getOrDefault(key, -1) < minimum) {
      reasons.add(key + "_below_" + minimum);
    }
  }

  private static void collectFailedChecks(
      String namespace,
      Map<String, Boolean> checks,
      List<String> reasons) {
    checks.entrySet().stream()
        .filter(entry -> !entry.getValue())
        .map(entry -> namespace + "_" + entry.getKey())
        .forEach(reasons::add);
  }

  private static UUID uuid(String seed) {
    return UUID.nameUUIDFromBytes(("rto-task38-" + seed).getBytes(StandardCharsets.UTF_8));
  }

  private static String metadata(String kind, String ref) {
    return "{\"synthetic\":true,\"pilotDataset\":\"" + PILOT_DATASET_KEY
        + "\",\"kind\":\"" + kind + "\",\"ref\":\"" + ref + "\"}";
  }

  private static List<CandidateProfileField> candidateProfileFields(
      PilotDataset.CandidateSeed candidate,
      UUID sourceItemId,
      UUID consultantActorId) {
    Instant now = Instant.now();
    CandidateProfileFieldLineage lineage = new CandidateProfileFieldLineage(
        List.of(CandidateProfileFieldSourceReference.sourceItem(
            new SourceItemId(Objects.requireNonNull(sourceItemId, "sourceItemId must not be null")),
            now)),
        "task-38-pilot-source-document",
        now);
    return List.of(
        profileField(
            CandidateProfileFieldPath.IDENTITY_FULL_NAME,
            CandidateProfileFieldValue.ofString(candidate.syntheticName()),
            lineage,
            consultantActorId,
            now),
        profileField(
            CandidateProfileFieldPath.CONTACT_EMAIL,
            CandidateProfileFieldValue.ofString(candidate.email()),
            lineage,
            consultantActorId,
            now),
        profileField(
            CandidateProfileFieldPath.PROFILE_HEADLINE,
            CandidateProfileFieldValue.ofString(candidate.seniorityBand() + " " + candidate.roleFamily()),
            lineage,
            consultantActorId,
            now),
        profileField(
            CandidateProfileFieldPath.PROFILE_SUMMARY,
            CandidateProfileFieldValue.ofString(candidate.summary()),
            lineage,
            consultantActorId,
            now),
        profileField(
            CandidateProfileFieldPath.LOCATION_CURRENT_LOCATION,
            CandidateProfileFieldValue.ofString(candidate.locationRegion()),
            lineage,
            consultantActorId,
            now),
        profileField(
            CandidateProfileFieldPath.SKILLS_PRIMARY_SKILLS,
            CandidateProfileFieldValue.ofJson(toJsonArray(candidate.skills())),
            lineage,
            consultantActorId,
            now),
        profileField(
            CandidateProfileFieldPath.EXPERIENCE_INDUSTRY,
            CandidateProfileFieldValue.ofString("semiconductor"),
            lineage,
            consultantActorId,
            now));
  }

  private static CandidateProfileField profileField(
      CandidateProfileFieldPath fieldPath,
      CandidateProfileFieldValue value,
      CandidateProfileFieldLineage lineage,
      UUID consultantActorId,
      Instant reviewedAt) {
    return CandidateProfileField.builder()
        .fieldPath(fieldPath)
        .value(value)
        .fieldStatus(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED)
        .lineage(lineage)
        .lastReviewedAt(reviewedAt)
        .confirmedByActorId(consultantActorId)
        .notes("Task 38 synthetic pilot seed field")
        .build();
  }

  private static String toJsonArray(List<String> values) {
    return values.stream()
        .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
        .reduce("[", (left, right) -> "[".equals(left) ? left + right : left + "," + right)
        + "]";
  }

  private static String sha256(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 not available", exception);
    }
  }
}
