package com.recruitingtransactionos.coreapi.pilotdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class PilotDataPostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    MigrateResult result = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    assertThat(result.migrationsExecuted).isEqualTo(34);
    dataSource = postgresDataSource();
  }

  @Test
  void rebuildImportsAndValidatesPilotDataWithoutDisclosureOrShortlistShortcuts()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    PilotDataReport report = service.rebuild(dataset);

    assertThat(report.command()).isEqualTo("rebuild");
    assertThat(report.valid()).isTrue();
    assertThat(report.counts().get("candidates")).isEqualTo(75);
    assertThat(report.counts().get("activeJobs")).isEqualTo(5);
    assertThat(report.counts().get("underReviewJobs")).isEqualTo(3);
    assertThat(report.counts().get("sourceDocuments")).isGreaterThanOrEqualTo(83);
    assertThat(report.counts().get("seededShortlists")).isZero();
    assertThat(report.counts().get("seededDisclosureRecords")).isZero();
    assertThat(report.counts().get("canonicalWriteAttempts")).isZero();
    assertThat(report.privacyChecks().get("passed")).isEqualTo(true);
    assertThat(report.seededAccountChecks().get("accountsPresent")).isEqualTo(true);
    assertThat(report.workflowAuditChecks().get("noShortcutShortlists")).isEqualTo(true);
    assertThat(report.workflowAuditChecks().get("noShortcutDisclosureRecords")).isEqualTo(true);
    assertThat(report.workflowAuditChecks().get("candidateCurrentProfilesLinked")).isEqualTo(true);
    assertThat(report.failedGateReasons()).isEmpty();
    assertThat(countRows("identity.user_account", dataset.organization().organizationId())).isEqualTo(5);
    assertThatSeededAccountsCanAuthenticate(dataset);
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isEqualTo(75);
    assertThat(countRowsWhere(
        "recruiting.candidate",
        dataset.organization().organizationId(),
        "current_profile_id IS NOT NULL")).isEqualTo(75);
    assertThat(singleUuid("""
        SELECT current_profile_id
        FROM recruiting.candidate
        WHERE organization_id = ? AND candidate_id = ?
        """,
        dataset.organization().organizationId(),
        UUID.fromString(dataset.candidates().getFirst().candidateId())))
        .isEqualTo(UUID.fromString(dataset.candidates().getFirst().profileId()));
    assertThat(countRows("recruiting.company", dataset.organization().organizationId())).isEqualTo(4);
    assertThat(countRowsWhere(
        "recruiting.company",
        dataset.organization().organizationId(),
        "metadata->>'clientActorId' IS NOT NULL")).isEqualTo(4);
    assertThat(countRowsWhere(
        "recruiting.job",
        dataset.organization().organizationId(),
        "metadata->>'clientActorId' IS NOT NULL")).isEqualTo(8);
    assertThat(countRows("intake.source_item", dataset.organization().organizationId()))
        .isGreaterThanOrEqualTo(83);

    PilotDataReport exported = service.export(dataset.organization().organizationId());
    assertThat(exported.valid()).isTrue();
    assertThat(exported.counts().get("candidates")).isEqualTo(75);

    PilotDataReport reset = service.reset(dataset.organization().organizationId(), true);
    assertThat(reset.valid()).isTrue();
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isZero();
  }

  @Test
  void resetFailsClosedWithoutExplicitAllowFlag() {
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
        service.reset(UUID.fromString("00000000-0000-0000-0000-000000380001"), false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("pilot_data_reset_requires_RTO_PILOT_DATA_ALLOW_RESET_true");
  }

  @Test
  void importFailsClosedBeforeWritesWhenDatasetViolatesPrivacyRules()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataset.CandidateSeed first = dataset.candidates().getFirst();
    PilotDataset invalidDataset = dataset.withCandidates(List.of(new PilotDataset.CandidateSeed(
        first.candidateId(),
        first.profileId(),
        first.syntheticName(),
        "not-synthetic@example.com",
        first.roleFamily(),
        first.seniorityBand(),
        first.locationRegion(),
        first.status(),
        first.skills(),
        first.summary(),
        first.sourceDocumentRef(),
        first.metadata())));
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.reset(dataset.organization().organizationId(), true);
    PilotDataReport report = service.importDataset(invalidDataset);

    assertThat(report.valid()).isFalse();
    assertThat(report.failedGateReasons()).contains("unsupported_email_domain");
    assertThat(countRows("identity.organization", dataset.organization().organizationId())).isZero();
    assertThat(countRows("identity.user_account", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isZero();
  }

  @Test
  void importFailsClosedBeforeWritesWhenDatasetReferencesMissingSourceDocuments()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataset.CandidateSeed first = dataset.candidates().getFirst();
    PilotDataset invalidDataset = dataset.withCandidates(List.of(new PilotDataset.CandidateSeed(
        first.candidateId(),
        first.profileId(),
        first.syntheticName(),
        first.email(),
        first.roleFamily(),
        first.seniorityBand(),
        first.locationRegion(),
        first.status(),
        first.skills(),
        first.summary(),
        "missing-source-document",
        first.metadata())));
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.reset(dataset.organization().organizationId(), true);
    PilotDataReport report = service.importDataset(invalidDataset);

    assertThat(report.valid()).isFalse();
    assertThat(report.failedGateReasons()).contains("candidate_missing_source_document");
    assertThat(countRows("identity.organization", dataset.organization().organizationId())).isZero();
    assertThat(countRows("identity.user_account", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.company", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.job", dataset.organization().organizationId())).isZero();
    assertThat(countRows("intake.source_item", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isZero();
  }

  @Test
  void importFailsClosedBeforeWritesWhenDatasetOwnerReferencesAreMalformed()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataset.CompanySeed firstCompany = dataset.companies().getFirst();
    PilotDataset invalidDataset = new PilotDataset(
        dataset.version(),
        dataset.organization(),
        dataset.accounts(),
        List.of(new PilotDataset.CompanySeed(
            firstCompany.companyId(),
            firstCompany.name(),
            firstCompany.industry(),
            firstCompany.headquartersLocation(),
            firstCompany.sizeBand(),
            "not-a-uuid",
            firstCompany.metadata())),
        dataset.jobs(),
        dataset.candidates(),
        dataset.sourceDocuments());
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.reset(dataset.organization().organizationId(), true);
    PilotDataReport report = service.importDataset(invalidDataset);

    assertThat(report.valid()).isFalse();
    assertThat(report.failedGateReasons()).contains("company_owner_account_malformed");
    assertThat(countRows("identity.organization", dataset.organization().organizationId())).isZero();
    assertThat(countRows("identity.user_account", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.company", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.job", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isZero();
  }

  @Test
  void importFailsClosedBeforeWritesWhenDatasetRuntimeParseFieldsAreInvalid()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataset.JobSeed firstJob = dataset.jobs().getFirst();
    PilotDataset.CandidateSeed firstCandidate = dataset.candidates().getFirst();
    PilotDataset.SourceDocumentSeed firstSourceDocument = dataset.sourceDocuments().getFirst();
    List<PilotDataset.SourceDocumentSeed> sourceDocuments = new ArrayList<>(dataset.sourceDocuments());
    sourceDocuments.set(0, new PilotDataset.SourceDocumentSeed(
        firstSourceDocument.sourceItemId(),
        firstSourceDocument.documentRef(),
        "NOT_A_SOURCE_TYPE",
        firstSourceDocument.title(),
        firstSourceDocument.filename(),
        firstSourceDocument.body(),
        firstSourceDocument.metadata()));
    PilotDataset invalidDataset = new PilotDataset(
        dataset.version(),
        dataset.organization(),
        dataset.accounts(),
        dataset.companies(),
        List.of(new PilotDataset.JobSeed(
            firstJob.jobId(),
            firstJob.companyId(),
            firstJob.title(),
            "not_a_job_status",
            firstJob.roleFamily(),
            firstJob.seniorityBand(),
            firstJob.location(),
            firstJob.compensation(),
            firstJob.ownerConsultantId(),
            firstJob.sourceDocumentRef(),
            firstJob.metadata())),
        List.of(new PilotDataset.CandidateSeed(
            "not-a-candidate-uuid",
            "not-a-profile-uuid",
            firstCandidate.syntheticName(),
            firstCandidate.email(),
            firstCandidate.roleFamily(),
            firstCandidate.seniorityBand(),
            firstCandidate.locationRegion(),
            "not_a_candidate_status",
            firstCandidate.skills(),
            firstCandidate.summary(),
            firstCandidate.sourceDocumentRef(),
            firstCandidate.metadata())),
        sourceDocuments);
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.reset(dataset.organization().organizationId(), true);
    PilotDataReport report = service.importDataset(invalidDataset);

    assertThat(report.valid()).isFalse();
    assertThat(report.failedGateReasons()).contains(
        "job_status_unsupported",
        "candidate_id_malformed",
        "candidate_profile_id_malformed",
        "candidate_status_unsupported",
        "source_document_type_unsupported");
    assertThat(countRows("identity.organization", dataset.organization().organizationId())).isZero();
    assertThat(countRows("identity.user_account", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.company", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.job", dataset.organization().organizationId())).isZero();
    assertThat(countRows("intake.source_item", dataset.organization().organizationId())).isZero();
    assertThat(countRows("recruiting.candidate", dataset.organization().organizationId())).isZero();
  }

  @Test
  void importFailsClosedBeforeWritesWhenSeededIdentityIdsAlreadyBelongToAnotherOrganization()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    UUID organizationId = dataset.organization().organizationId();
    UUID otherOrganizationId = UUID.fromString("00000000-0000-0000-0000-00000038f001");
    UUID reusedAccountId = UUID.fromString("00000000-0000-0000-0000-00000038f002");
    List<PilotDataset.AccountSeed> accounts = new ArrayList<>(dataset.accounts());
    accounts.add(new PilotDataset.AccountSeed(
        reusedAccountId,
        "cross-org-seed@example.test",
        "Cross Org Seed",
        "client",
        "pilotpass123"));
    PilotDataset invalidDataset = new PilotDataset(
        dataset.version(),
        dataset.organization(),
        accounts,
        dataset.companies(),
        dataset.jobs(),
        dataset.candidates(),
        dataset.sourceDocuments());
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());
    service.reset(organizationId, true);
    executeUpdate("""
        INSERT INTO identity.organization (
          organization_id, legal_name, display_name, status, default_timezone
        ) VALUES (?, 'Other Pilot Org', 'Other Pilot Org', 'active', 'UTC')
        ON CONFLICT DO NOTHING
        """, otherOrganizationId);
    executeUpdate("""
        INSERT INTO identity.user_account (
          user_account_id, organization_id, email, display_name, status, password_hash
        ) VALUES (?, ?, 'already-bound@example.test', 'Already Bound', 'active', 'hash')
        ON CONFLICT DO NOTHING
        """, reusedAccountId, otherOrganizationId);

    PilotDataReport report = service.importDataset(invalidDataset);

    assertThat(report.valid()).isFalse();
    assertThat(report.failedGateReasons()).contains("seed_account_id_cross_org");
    assertThat(countRows("identity.user_account", organizationId)).isZero();
    assertThat(countRows("identity.role_assignment", organizationId)).isZero();
    assertThat(countRows("recruiting.company", organizationId)).isZero();
    assertThat(countRows("recruiting.job", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("identity.user_account", otherOrganizationId)).isEqualTo(1);
  }

  @Test
  void validateFailsClosedWhenStoredCandidateProfileFieldsContainUnsupportedEmailDomains()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.rebuild(dataset);
    executeUpdate(
        """
        UPDATE recruiting.candidate_profile
        SET metadata = replace(metadata::text, '@candidate.example.test', '@gmail.com')::jsonb
        WHERE organization_id = ?
        """,
        dataset.organization().organizationId());

    PilotDataReport report = service.validate(dataset.organization().organizationId());

    assertThat(report.valid()).isFalse();
    assertThat(report.privacyChecks().get("passed")).isEqualTo(false);
    assertThat(report.failedGateReasons()).contains("candidate_profile_unsupported_email_domain");
  }

  @Test
  void resetRemovesOrganizationScopedRowsCreatedByDownstreamWorkflowTools()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.rebuild(dataset);
    UUID organizationId = dataset.organization().organizationId();
    UUID userAccountId = dataset.accounts().getFirst().userAccountId();
    UUID jobId = dataset.jobs().getFirst().jobId();
    UUID candidateId = UUID.fromString(dataset.candidates().getFirst().candidateId());
    UUID candidateProfileId = singleUuid("""
        SELECT current_profile_id
        FROM recruiting.candidate
        WHERE organization_id = ? AND candidate_id = ?
        """, organizationId, candidateId);
    UUID informationPacketId = UUID.fromString("00000000-0000-0000-0000-000000389001");
    UUID sourceItemId = UUID.fromString("00000000-0000-0000-0000-000000389002");

    executeUpdate("""
        INSERT INTO recruiting.match_report (
          match_report_id, organization_id, job_id, candidate_id, subject_type,
          match_subject_ref, proposed_score, overall_score, score_confidence,
          cap_reason, cap_safe_explanation, authenticity_risk,
          reidentification_risk_signal, ontology_version, industry_pack_version,
          generated_at
        ) VALUES (
          '00000000-0000-0000-0000-000000389003', ?, ?, ?, 'candidate',
          'candidate:test', 3, 3, 'MEDIUM', 'synthetic cap',
          'synthetic explanation', 'LOW', 'LOW', 'pilot-ontology-v1',
          'semiconductor-pilot-v1', now()
        )
        """, organizationId, jobId, candidateId);
    executeUpdate("""
        INSERT INTO recruiting.follow_up_submission (
          follow_up_submission_id, organization_id, candidate_id, candidate_profile_id,
          form_id, field_path, answer_json, status, submitted_by_user_id, submitted_at
        ) VALUES (
          '00000000-0000-0000-0000-000000389004', ?, ?, ?,
          'pilot-follow-up', 'profile.summary', '{"value":"synthetic"}',
          'submitted', ?, now()
        )
        """, organizationId, candidateId, candidateProfileId, userAccountId);
    executeUpdate("""
        INSERT INTO recruiting.information_packet (
          information_packet_id, organization_id, packet_type, processing_status
        ) VALUES (?, ?, 'resume', 'uploaded')
        """, informationPacketId, organizationId);
    executeUpdate("""
        INSERT INTO recruiting.source_item (
          source_item_id, organization_id, information_packet_id, source_type,
          origin_actor_type, status, received_at
        ) VALUES (?, ?, ?, 'resume', 'consultant'::governance.actor_role, 'uploaded', now())
        """, sourceItemId, organizationId, informationPacketId);
    executeUpdate("""
        INSERT INTO audit.audit_log (
          audit_log_id, organization_id, actor_user_id, actor_role, action,
          target_entity_type, target_entity_id, result, occurred_at
        ) VALUES (
          '00000000-0000-0000-0000-000000389005', ?, ?, 'consultant',
          'pilot.workflow.generated', 'candidate', ?, 'success', now()
        )
        """, organizationId, userAccountId, candidateId);

    PilotDataReport reset = service.reset(organizationId, true);

    assertThat(reset.valid()).isTrue();
    assertThat(countRows("recruiting.match_report", organizationId)).isZero();
    assertThat(countRows("recruiting.follow_up_submission", organizationId)).isZero();
    assertThat(countRows("recruiting.source_item", organizationId)).isZero();
    assertThat(countRows("recruiting.information_packet", organizationId)).isZero();
    assertThat(countRows("audit.audit_log", organizationId)).isZero();
    assertThat(countRows("identity.organization", organizationId)).isZero();
  }

  @Test
  void resetRemovesWorkflowEventsBeforeTheirGovernanceParents()
      throws SQLException {
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    service.rebuild(dataset);
    UUID organizationId = dataset.organization().organizationId();
    UUID userAccountId = dataset.accounts().getFirst().userAccountId();
    UUID claimLedgerItemId = UUID.fromString("00000000-0000-0000-0000-000000389006");
    UUID reviewEventId = UUID.fromString("00000000-0000-0000-0000-000000389007");
    UUID workflowEventId = UUID.fromString("00000000-0000-0000-0000-000000389008");
    UUID aiTaskDefinitionId = UUID.fromString("00000000-0000-0000-0000-000000389009");
    UUID aiTaskRunId = UUID.fromString("00000000-0000-0000-0000-000000389010");

    executeUpdate("""
        INSERT INTO governance.ai_task_definition (
          ai_task_definition_id, organization_id, task_key, task_version,
          status, input_schema_version, output_schema_version,
          human_review_policy, write_back_target
        ) VALUES (
          ?, ?, 'pilot-reset-ordering', '1.0', 'active',
          'pilot-input.v1', 'pilot-output.v1', '{}'::jsonb, 'workflow_event'
        )
        """, aiTaskDefinitionId, organizationId);
    executeUpdate("""
        INSERT INTO governance.ai_task_run (
          ai_task_run_id, organization_id, ai_task_definition_id, task_version,
          status, input_schema_version, output_schema_version, prompt_version,
          model_provider, model_name, target_entity_type, target_entity_id,
          write_back_target, human_review_status, started_at, completed_at
        ) VALUES (
          ?, ?, ?, '1.0', 'succeeded', 'pilot-input.v1', 'pilot-output.v1',
          'pilot-reset-ordering.v1', 'test-provider', 'test-model',
          'candidate', ?, 'workflow_event', 'reviewed', now(), now()
        )
        """, aiTaskRunId, organizationId, aiTaskDefinitionId, userAccountId);

    executeUpdate("""
        INSERT INTO governance.claim_ledger_item (
          claim_ledger_item_id, organization_id, entity_type, entity_id,
          claim_type, assertion_strength, source_span_ref, speaker,
          verification_status, ai_task_run_id, confidence, created_by
        ) VALUES (
          ?, ?, 'candidate', ?, 'fact'::governance.claim_type,
          'explicit'::governance.assertion_strength, 'pilot-span-1',
          'consultant'::governance.actor_role,
          'human_acknowledged'::governance.verification_status, ?, 0.80, ?
        )
        """, claimLedgerItemId, organizationId, userAccountId, aiTaskRunId, userAccountId);
    executeUpdate("""
        INSERT INTO governance.review_event (
          review_event_id, organization_id, reviewer_user_id, target_entity_type,
          target_entity_id, field_path, risk_tier, decision, duration_ms,
          claim_ledger_item_id
        ) VALUES (
          ?, ?, ?, 'candidate', ?, 'profile.summary',
          'T2_MEDIUM_RISK'::governance.risk_tier, 'approved', 10, ?
        )
        """, reviewEventId, organizationId, userAccountId, userAccountId, claimLedgerItemId);
    executeUpdate("""
        INSERT INTO workflow.workflow_event (
          workflow_event_id, organization_id, entity_namespace, entity_type,
          entity_id, action, before_state, after_state, actor_role,
          actor_user_id, source_type, ai_task_run_id, review_event_id, reason,
          occurred_at
        ) VALUES (
          ?, ?, 'governance', 'review_event', ?, 'review.approved',
          '{"status":"draft"}'::jsonb, '{"status":"approved"}'::jsonb,
          'consultant', ?, 'test', ?, ?, 'pilot reset ordering test', now()
        )
        """, workflowEventId, organizationId, reviewEventId, userAccountId, aiTaskRunId, reviewEventId);
    executeUpdate("""
        UPDATE governance.claim_ledger_item
        SET review_event_id = ?
        WHERE organization_id = ? AND claim_ledger_item_id = ?
        """, reviewEventId, organizationId, claimLedgerItemId);

    PilotDataReport reset = service.reset(organizationId, true);

    assertThat(reset.valid()).isTrue();
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(countRows("governance.review_event", organizationId)).isZero();
    assertThat(countRows("governance.claim_ledger_item", organizationId)).isZero();
    assertThat(countRows("governance.ai_task_run", organizationId)).isZero();
    assertThat(countRows("governance.ai_task_definition", organizationId)).isZero();
    assertThat(countRows("identity.organization", organizationId)).isZero();
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    return countRowsWhere(tableName, organizationId, "true");
  }

  private static int countRowsWhere(
      String tableName,
      UUID organizationId,
      String whereClause) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         var statement = connection.prepareStatement(
             "SELECT count(*) FROM " + tableName + " WHERE organization_id = ? AND " + whereClause)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static void assertThatSeededAccountsCanAuthenticate(PilotDataset dataset) throws SQLException {
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    try (Connection connection = dataSource.getConnection();
         var statement = connection.prepareStatement("""
             SELECT password_hash
             FROM identity.user_account
             WHERE organization_id = ? AND email = ?
             """)) {
      for (PilotDataset.AccountSeed account : dataset.accounts()) {
        statement.setObject(1, dataset.organization().organizationId());
        statement.setString(2, account.email());
        try (ResultSet resultSet = statement.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(passwordEncoder.matches(account.password(), resultSet.getString("password_hash")))
              .isTrue();
          assertThat(resultSet.next()).isFalse();
        }
      }
    }
  }

  private static UUID singleUuid(String sql, Object... parameters) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         var statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < parameters.length; index++) {
        statement.setObject(index + 1, parameters[index]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getObject(1, UUID.class);
      }
    }
  }

  private static void executeUpdate(String sql, Object... parameters) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         var statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < parameters.length; index++) {
        statement.setObject(index + 1, parameters[index]);
      }
      statement.executeUpdate();
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
      public PrintWriter getLogWriter() throws SQLException {
        return null;
      }

      @Override
      public void setLogWriter(PrintWriter out) throws SQLException {
      }

      @Override
      public void setLoginTimeout(int seconds) throws SQLException {
      }

      @Override
      public int getLoginTimeout() throws SQLException {
        return 0;
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
      }
    };
  }
}
