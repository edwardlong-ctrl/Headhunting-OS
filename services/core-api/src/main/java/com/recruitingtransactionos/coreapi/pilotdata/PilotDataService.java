package com.recruitingtransactionos.coreapi.pilotdata;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.persistence.JdbcCandidatePersistencePort;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceReference;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.persistence.JdbcCandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CreateCandidateProfileRequest;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  }

  public PilotDataReport rebuild(PilotDataset dataset) {
    reset(dataset.organization().organizationId(), true);
    importDataset(dataset);
    PilotDataReport validation = validate(dataset.organization().organizationId());
    return new PilotDataReport("rebuild", validation.valid(), validation.counts(), validation.issues());
  }

  public PilotDataReport importDataset(PilotDataset dataset) {
    Objects.requireNonNull(dataset, "dataset must not be null");
    PilotDataValidationResult privacyResult = privacyValidator.validate(dataset);
    if (!privacyResult.valid()) {
      return new PilotDataReport("import", false, Map.of(), privacyResult.issues());
    }
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        insertOrganization(connection, dataset.organization());
        insertAccounts(connection, dataset);
        insertCompanies(connection, dataset);
        insertJobs(connection, dataset);
        insertSourceDocuments(connection, dataset);
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
    insertCandidates(dataset);
    return validate(dataset.organization().organizationId()).withCommand("import");
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
      boolean valid = counts.get("accounts") == 5
          && counts.get("seededRoleAssignments") == 5
          && counts.get("candidates") == 75
          && counts.get("activeJobs") == 5
          && counts.get("underReviewJobs") == 3
          && counts.get("sourceDocuments") >= 83
          && counts.get("seededShortlists") == 0
          && counts.get("seededDisclosureRecords") == 0
          && counts.get("canonicalWriteAttempts") == 0;
      return new PilotDataReport("validate", valid, counts, List.of());
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to validate pilot dataset", exception);
    }
  }

  public PilotDataReport export(UUID organizationId) {
    PilotDataReport report = validate(organizationId);
    return new PilotDataReport("export", report.valid(), report.counts(), report.issues());
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

  private static void insertCompanies(Connection connection, PilotDataset dataset) throws SQLException {
    for (PilotDataset.CompanySeed company : dataset.companies()) {
      try (PreparedStatement statement = connection.prepareStatement("""
          INSERT INTO recruiting.company (
            company_id, organization_id, name, display_name, industry, headquarters_location,
            size_band, status, owner_consultant_id, metadata
          ) VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?, ?::jsonb)
          ON CONFLICT (company_id) DO NOTHING
          """)) {
        statement.setObject(1, company.companyId());
        statement.setObject(2, dataset.organization().organizationId());
        statement.setString(3, company.name());
        statement.setString(4, company.name());
        statement.setString(5, company.industry());
        statement.setString(6, company.headquartersLocation());
        statement.setString(7, company.sizeBand());
        statement.setObject(8, UUID.fromString(company.ownerConsultantId()));
        statement.setString(9, company.metadata());
        statement.executeUpdate();
      }
    }
  }

  private static void insertJobs(Connection connection, PilotDataset dataset) throws SQLException {
    UUID semiconductorIndustryPackId = UUID.fromString("00000000-0000-0000-0000-000000280002");
    for (PilotDataset.JobSeed job : dataset.jobs()) {
      try (PreparedStatement statement = connection.prepareStatement("""
          INSERT INTO recruiting.job (
            job_id, organization_id, company_id, title, description, location, seniority_band,
            role_family, employment_type, compensation, status, commercial_terms,
            owner_consultant_id, industry_pack_id, metadata
          ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, 'full_time', ?::jsonb, ?, ?::jsonb, ?, ?, ?::jsonb)
          ON CONFLICT (job_id) DO NOTHING
          """)) {
        statement.setObject(1, job.jobId());
        statement.setObject(2, dataset.organization().organizationId());
        statement.setObject(3, job.companyId());
        statement.setString(4, job.title());
        statement.setString(5, "Synthetic pilot job for " + job.roleFamily());
        statement.setString(6, job.location());
        statement.setString(7, job.seniorityBand());
        statement.setString(8, job.roleFamily());
        statement.setString(9, job.compensation());
        statement.setString(10, job.status());
        statement.setString(11, "{\"feeRate\":\"25%\",\"synthetic\":true}");
        statement.setObject(12, UUID.fromString(job.ownerConsultantId()));
        statement.setObject(13, semiconductorIndustryPackId);
        statement.setString(14, job.metadata());
        statement.executeUpdate();
      }
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
        candidateProfileService.createCandidateProfile(new CreateCandidateProfileRequest(
            dataset.organization().organizationId(),
            typedCandidateId,
            new CandidateProfileVersion(1),
            candidateProfileFields(candidate, sourceItemId, ownerConsultantId)));
      }
    }
  }

  private static void insertSourceDocuments(Connection connection, PilotDataset dataset) throws SQLException {
    UUID consultantActorId = dataset.accounts().stream()
        .filter(account -> "consultant".equals(account.role()))
        .findFirst()
        .orElseThrow()
        .userAccountId();
    for (PilotDataset.SourceDocumentSeed sourceDocument : dataset.sourceDocuments()) {
      try (PreparedStatement statement = connection.prepareStatement("""
          INSERT INTO intake.source_item (
            source_item_id, organization_id, source_type, origin, title, content_hash,
            external_ref, storage_ref, raw_ref, language, uploaded_by_actor_type,
            uploaded_by_actor_id, received_at, metadata_json, status, mime_type,
            file_size_bytes, original_filename, scan_status
          ) VALUES (?, ?, ?, 'SYSTEM_IMPORT', ?, ?, ?, ?, ?, 'en', 'consultant',
            ?, now(), ?::jsonb, 'REGISTERED', 'text/plain', ?, ?, 'not_scanned')
          ON CONFLICT (source_item_id) DO NOTHING
          """)) {
        statement.setObject(1, sourceDocument.sourceItemId());
        statement.setObject(2, dataset.organization().organizationId());
        statement.setString(3, sourceDocument.sourceType());
        statement.setString(4, sourceDocument.title());
        statement.setString(5, sha256(sourceDocument.body()));
        statement.setString(6, sourceDocument.documentRef());
        statement.setString(7, "pilotdata/" + sourceDocument.filename());
        statement.setString(8, sourceDocument.documentRef());
        statement.setObject(9, consultantActorId);
        statement.setString(10, sourceDocument.metadata());
        statement.setLong(11, sourceDocument.body().getBytes(StandardCharsets.UTF_8).length);
        statement.setString(12, sourceDocument.filename());
        statement.executeUpdate();
      }
    }
  }

  private static void deletePilotOrganizationRows(Connection connection, UUID organizationId)
      throws SQLException {
    String[] tables = {
        "operations.notification_delivery_attempt",
        "operations.notification_schedule",
        "operations.notification_preference",
        "operations.notification",
        "privacy.client_unlock_request",
        "privacy.disclosure_record",
        "privacy.unlock_decision",
        "privacy.consent_record",
        "privacy.reidentification_risk_assessment",
        "recruiting.commission",
        "recruiting.placement",
        "recruiting.interview_feedback_suggestion",
        "recruiting.match_calibration_signal",
        "recruiting.interview_feedback",
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
        "governance.canonical_write_attempt",
        "governance.ai_task_run",
        "governance.ai_task_definition",
        "governance.review_event",
        "governance.claim_ledger_item",
        "governance.config_entry",
        "workflow.workflow_event",
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
