package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.persistence.JdbcCandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CreateCandidateProfileRequest;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcCanonicalWriteAttemptPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendResult;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowIdempotencyKey;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowStateSnapshot;
import com.recruitingtransactionos.coreapi.truthlayer.service.CandidateProfileCanonicalWriteTarget;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteCommand;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteResult;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteReviewEvidence;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class CanonicalWriteTransactionBoundaryIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T03:30:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static MigrateResult migrateResult;
  private static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    migrateResult = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    dataSource = postgresDataSource();
  }

  @Test
  void successfulTransactionCommitsWorkflowEventAppend() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080301");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080302");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080303");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080304");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);

    WorkflowEventAppendResult result = boundary().run(() ->
        workflowEventService().append(workflowCommand(
            organizationId,
            actorId,
            candidateId,
            reviewEventId,
            "canonical-write-transaction-commit-" + organizationId)));

    assertThat(result.workflowEventId()).isNotNull();
    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(1);
  }

  @Test
  void failedTransactionRollsBackWorkflowEventAppend() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080401");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080402");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080403");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080404");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);

    assertThatThrownBy(() -> boundary().run(() -> {
      workflowEventService().append(workflowCommand(
          organizationId,
          actorId,
          candidateId,
          reviewEventId,
          "canonical-write-transaction-rollback-" + organizationId));
      throw new DeliberateCanonicalWriteFailure("force canonical transaction rollback");
    })).isInstanceOf(DeliberateCanonicalWriteFailure.class);

    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
  }

  @Test
  void allowedBoundaryAppendsWorkflowEventInPostgres() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080001");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080002");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080003");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080004");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);

    CanonicalWriteResult result = service().attempt(commandBuilder(
        organizationId,
        actorId,
        candidateId,
        reviewEventId)
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventAppended()).isTrue();
    assertThat(result.workflowEventId()).isNotNull();
    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(1);
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
    assertThat(findWorkflowAction(result.workflowEventId().value()))
        .isEqualTo("CANONICAL_WRITE_ALLOWED");
  }

  @Test
  void blockedBoundaryDoesNotAppendWorkflowEventInPostgres() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080101");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080102");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080103");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080104");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);

    CanonicalWriteResult result = service().attempt(commandBuilder(
        organizationId,
        actorId,
        candidateId,
        reviewEventId)
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.workflowEventAppended()).isFalse();
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
  }

  @Test
  void canonicalPersistenceRemainsExplicitlyDeferred() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080201");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080202");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080203");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080204");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);

    CanonicalWriteResult result = service().attempt(commandBuilder(
        organizationId,
        actorId,
        candidateId,
        reviewEventId)
        .build());

    assertThat(tableExists("recruiting", "candidate_profile")).isTrue();
    assertThat(result.canonicalPersistencePerformed()).isFalse();
    assertThat(result.canonicalPersistenceStatus())
        .isEqualTo("not_implemented_no_safe_canonical_write_target_in_task_3d");
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
  }

  @Test
  void allowedCandidateProfileTargetPersistsFieldAndAuditInOneTransaction()
      throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080601");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080602");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080603");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080604");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);
    insertCandidate(organizationId, candidateId);
    CandidateProfile profile = candidateProfileService().createCandidateProfile(
        new CreateCandidateProfileRequest(
            organizationId,
            new CandidateId(candidateId),
            new CandidateProfileVersion(1),
            java.util.List.of()));

    CanonicalWriteResult result = serviceWithCandidateProfile().attempt(commandBuilder(
        organizationId,
        actorId,
        candidateId,
        reviewEventId)
        .targetVerificationStatus(VerificationStatus.CONSULTANT_ATTESTED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .targetFieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME.value())
        .idempotencyKey("candidate-profile-write-" + organizationId)
        .candidateProfileWriteTarget(candidateProfileWriteTarget(
            profile.candidateProfileId(),
            "Jane Candidate"))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventAppended()).isTrue();
    assertThat(result.workflowEventId()).isNotNull();
    assertThat(result.canonicalPersistencePerformed()).isTrue();
    assertThat(result.canonicalPersistenceStatus()).isEqualTo("candidate_profile_field_persisted");
    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(1);
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isEqualTo(1);
    CandidateProfileField field = candidateProfileService()
        .listCandidateProfileFields(organizationId, profile.candidateProfileId())
        .getFirst();
    assertThat(field.fieldPath()).isEqualTo(CandidateProfileFieldPath.IDENTITY_FULL_NAME);
    assertThat(field.value()).isEqualTo(CandidateProfileFieldValue.ofString("Jane Candidate"));
    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.CONSULTANT_ATTESTED);
    assertThat(field.sourceClaimId()).isEqualTo(new ClaimId(
        uuid("00000000-0000-0000-0000-000000080999")));
    assertThat(field.sourceReviewEventId()).isEqualTo(new ReviewEventId(reviewEventId));
    assertThat(field.sourceWorkflowEventId()).isEqualTo(result.workflowEventId());
    assertThat(findWorkflowAction(result.workflowEventId().value()))
        .isEqualTo("CANONICAL_WRITE_ALLOWED");
  }

  @Test
  void profileWriteFailureRollsBackAllowedWorkflowAudit() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080701");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080702");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080703");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080704");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);

    assertThatThrownBy(() -> serviceWithCandidateProfile().attempt(commandBuilder(
        organizationId,
        actorId,
        candidateId,
        reviewEventId)
        .targetVerificationStatus(VerificationStatus.CONSULTANT_ATTESTED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .targetFieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME.value())
        .idempotencyKey("candidate-profile-rollback-" + organizationId)
        .candidateProfileWriteTarget(candidateProfileWriteTarget(
            new CandidateProfileId(uuid("00000000-0000-0000-0000-000000080799")),
            "Jane Candidate"))
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("candidate profile not found in organization");

    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
  }

  @Test
  void workflowAuditFailureDoesNotWriteCandidateProfileField() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080801");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080802");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080803");
    UUID missingReviewEventId = uuid("00000000-0000-0000-0000-000000080804");
    insertOrganization(organizationId);
    insertUser(organizationId, actorId);
    insertCandidate(organizationId, candidateId);
    CandidateProfile profile = candidateProfileService().createCandidateProfile(
        new CreateCandidateProfileRequest(
            organizationId,
            new CandidateId(candidateId),
            new CandidateProfileVersion(1),
            java.util.List.of()));

    assertThatThrownBy(() -> serviceWithCandidateProfile().attempt(commandBuilder(
        organizationId,
        actorId,
        candidateId,
        missingReviewEventId)
        .targetVerificationStatus(VerificationStatus.CONSULTANT_ATTESTED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .targetFieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME.value())
        .idempotencyKey("candidate-profile-audit-fails-" + organizationId)
        .candidateProfileWriteTarget(candidateProfileWriteTarget(
            profile.candidateProfileId(),
            "Jane Candidate"))
        .build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("workflow event");

    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(candidateProfileService()
        .listCandidateProfileFields(organizationId, profile.candidateProfileId()))
        .isEmpty();
  }

  @Test
  void repeatedAllowedCandidateProfileWriteIsIdempotentForAuditAndField() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080901");
    UUID actorId = uuid("00000000-0000-0000-0000-000000080902");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080903");
    UUID reviewEventId = uuid("00000000-0000-0000-0000-000000080904");
    insertOrganizationUserAndReview(organizationId, actorId, candidateId, reviewEventId);
    insertCandidate(organizationId, candidateId);
    CandidateProfile profile = candidateProfileService().createCandidateProfile(
        new CreateCandidateProfileRequest(
            organizationId,
            new CandidateId(candidateId),
            new CandidateProfileVersion(1),
            java.util.List.of()));
    CanonicalWriteCommand command = commandBuilder(
        organizationId,
        actorId,
        candidateId,
        reviewEventId)
        .targetVerificationStatus(VerificationStatus.CONSULTANT_ATTESTED)
        .targetRiskTier(RiskTier.T2_MEDIUM_RISK)
        .targetFieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME.value())
        .idempotencyKey("candidate-profile-idempotent-" + organizationId)
        .candidateProfileWriteTarget(candidateProfileWriteTarget(
            profile.candidateProfileId(),
            "Jane Candidate"))
        .build();

    CanonicalWriteResult first = serviceWithCandidateProfile().attempt(command);
    CanonicalWriteResult second = serviceWithCandidateProfile().attempt(command);

    assertThat(first.canonicalPersistencePerformed()).isTrue();
    assertThat(second.canonicalPersistencePerformed()).isTrue();
    assertThat(second.workflowEventId()).isEqualTo(first.workflowEventId());
    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(1);
    assertThat(candidateProfileService()
        .listCandidateProfileFields(organizationId, profile.candidateProfileId()))
        .hasSize(1);
  }

  @Test
  void candidateProfileServiceStillWorksIndependently() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-000000080501");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000080503");
    insertOrganization(organizationId);
    insertCandidate(organizationId, candidateId);

    CandidateProfile profile = candidateProfileService().createCandidateProfile(
        new CreateCandidateProfileRequest(
            organizationId,
            new CandidateId(candidateId),
            new CandidateProfileVersion(1),
            java.util.List.of()));

    assertThat(profile.candidateId()).isEqualTo(new CandidateId(candidateId));
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isEqualTo(1);
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
  }

  @Test
  void fullFlywayMigrationStillAppliesBeforeCanonicalWriteBoundaryTest()
      throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(24);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "20", "21", "22", "23", "24");
  }

  private static CanonicalWriteService service() {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        workflowEventService(),
        boundary(),
        null,
        new JdbcCanonicalWriteAttemptPort(dataSource));
  }

  private static CanonicalWriteService serviceWithCandidateProfile() {
    return new CanonicalWriteService(
        new CanonicalWriteGate(),
        workflowEventService(),
        boundary(),
        candidateProfileService(),
        new JdbcCanonicalWriteAttemptPort(dataSource));
  }

  @Test
  void blockedAttemptPersistsAuditRecordButNotWorkflowEvent() throws SQLException {
    UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000080999");
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000081000");
    UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000081001");
    UUID reviewEventId = UUID.fromString("00000000-0000-0000-0000-000000081002");
    insertOrganizationUserAndReview(organizationId, userId, candidateId, reviewEventId);

    CanonicalWriteResult result = service().attempt(commandBuilder(
        organizationId, userId, candidateId, reviewEventId)
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false))
        .build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.workflowEventAppended()).isFalse();
    assertThat(result.workflowEventId()).isNull();
    assertThat(result.canonicalWriteAttemptId()).isNotNull();
    assertThat(countRows("governance.canonical_write_attempt", organizationId)).isEqualTo(1);
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
  }

  @Test
  void allowedAttemptPersistsAttemptRecordInSameTransactionAsWorkflowEvent()
      throws SQLException {
    UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000081003");
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000081004");
    UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000081005");
    UUID reviewEventId = UUID.fromString("00000000-0000-0000-0000-000000081006");
    insertOrganizationUserAndReview(organizationId, userId, candidateId, reviewEventId);

    CanonicalWriteResult result = service().attempt(
        commandBuilder(organizationId, userId, candidateId, reviewEventId).build());

    assertThat(result.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventAppended()).isTrue();
    assertThat(result.workflowEventId()).isNotNull();
    assertThat(result.canonicalWriteAttemptId()).isNotNull();
    assertThat(countRows("governance.canonical_write_attempt", organizationId)).isEqualTo(1);
    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(1);
  }

  @Test
  void repeatedBlockedAttemptIsIdempotentForAttemptRecord() throws SQLException {
    UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000081007");
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000081008");
    UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000081009");
    UUID reviewEventId = UUID.fromString("00000000-0000-0000-0000-000000081010");
    insertOrganizationUserAndReview(organizationId, userId, candidateId, reviewEventId);

    CanonicalWriteCommand.Builder builder = commandBuilder(
        organizationId, userId, candidateId, reviewEventId)
        .claim(new ClaimInput(
            ClaimType.INFERENCE,
            AssertionStrength.IMPLIED,
            VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY,
            false));

    CanonicalWriteResult first = service().attempt(builder.build());
    CanonicalWriteResult second = service().attempt(builder.build());

    assertThat(first.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(second.decision().type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(first.canonicalWriteAttemptId()).isNotNull();
    assertThat(first.canonicalWriteAttemptId()).isEqualTo(second.canonicalWriteAttemptId());
    assertThat(countRows("governance.canonical_write_attempt", organizationId)).isEqualTo(1);
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
  }

  @Test
  void repeatedAllowedAttemptIsIdempotentForAttemptRecord() throws SQLException {
    UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000081011");
    UUID userId = UUID.fromString("00000000-0000-0000-0000-000000081012");
    UUID candidateId = UUID.fromString("00000000-0000-0000-0000-000000081013");
    UUID reviewEventId = UUID.fromString("00000000-0000-0000-0000-000000081014");
    insertOrganizationUserAndReview(organizationId, userId, candidateId, reviewEventId);

    CanonicalWriteCommand command = commandBuilder(
        organizationId, userId, candidateId, reviewEventId).build();

    CanonicalWriteResult first = service().attempt(command);
    CanonicalWriteResult second = service().attempt(command);

    assertThat(first.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(second.decision().type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(first.canonicalWriteAttemptId()).isNotNull();
    assertThat(second.canonicalWriteAttemptId()).isEqualTo(first.canonicalWriteAttemptId());
    assertThat(countRows("governance.canonical_write_attempt", organizationId)).isEqualTo(1);
    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(1);
  }

  private static WorkflowEventService workflowEventService() {
    return new WorkflowEventService(new JdbcWorkflowEventPort(dataSource));
  }

  private static CanonicalWriteTransactionBoundary boundary() {
    return new SpringCanonicalWriteTransactionBoundary(
        new DataSourceTransactionManager(dataSource));
  }

  private static CandidateProfileService candidateProfileService() {
    return new CandidateProfileService(new JdbcCandidateProfilePersistencePort(dataSource));
  }

  private static CandidateProfileCanonicalWriteTarget candidateProfileWriteTarget(
      CandidateProfileId profileId,
      String value) {
    return new CandidateProfileCanonicalWriteTarget(
        profileId,
        CandidateProfileFieldPath.IDENTITY_FULL_NAME,
        CandidateProfileFieldValue.ofString(value),
        CandidateProfileFieldStatus.CONSULTANT_ATTESTED);
  }

  private static WorkflowEventAppendCommand workflowCommand(
      UUID organizationId,
      UUID actorId,
      UUID candidateId,
      UUID reviewEventId,
      String idempotencyKey) {
    return new WorkflowEventAppendCommand(
        organizationId,
        "recruiting",
        new EntityRef("CANDIDATE", candidateId),
        null,
        WorkflowActionCode.CANONICAL_WRITE_ALLOWED.wireValue(),
        new WorkflowStateSnapshot("{\"boundary\":\"canonical_write\",\"status\":\"requested\"}"),
        new WorkflowStateSnapshot(
            "{\"boundary\":\"canonical_write\",\"status\":\"allowed_audit_appended\"}"),
        new ActorRef(actorId, ActorRole.CONSULTANT),
        "canonical_write_transaction_boundary_test",
        uuid("00000000-0000-0000-0000-000000080988"),
        null,
        new ReviewEventId(reviewEventId),
        "reviewed source span before canonical boundary",
        new WorkflowIdempotencyKey(idempotencyKey),
        new WorkflowCorrelationId(uuid("00000000-0000-0000-0000-000000080987")),
        new WorkflowCausationId(uuid("00000000-0000-0000-0000-000000080986")),
        OCCURRED_AT);
  }

  private static CanonicalWriteCommand.Builder commandBuilder(
      UUID organizationId,
      UUID actorId,
      UUID candidateId,
      UUID reviewEventId) {
    return CanonicalWriteCommand.builder()
        .organizationId(organizationId)
        .targetEntity(new EntityRef("CANDIDATE", candidateId))
        .targetFieldPath("headline")
        .proposedValueRef("claim-value:headline:v1")
        .claimId(new ClaimId(uuid("00000000-0000-0000-0000-000000080999")))
        .claim(new ClaimInput(
            ClaimType.FACT,
            AssertionStrength.EXPLICIT,
            VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CLIENT_SAFE,
            false))
        .canonicalWriteAllowed(true)
        .reviewEvidence(new CanonicalWriteReviewEvidence(
            new ReviewEventId(reviewEventId),
            ReviewDecision.APPROVED,
            false,
            false,
            "reviewed source span before canonical boundary"))
        .targetVerificationStatus(VerificationStatus.HUMAN_ACKNOWLEDGED)
        .targetRiskTier(RiskTier.T1_LOW_RISK)
        .clientVisible(false)
        .conflictsWithCanonical(false)
        .actor(new ActorRef(actorId, ActorRole.CONSULTANT))
        .reason("reviewed source span before canonical boundary")
        .correlationId(uuid("00000000-0000-0000-0000-000000080998"))
        .causationId(uuid("00000000-0000-0000-0000-000000080997"))
        .idempotencyKey("canonical-write-boundary-" + organizationId)
        .occurredAt(OCCURRED_AT);
  }

  private static void insertOrganizationUserAndReview(
      UUID organizationId,
      UUID userId,
      UUID candidateId,
      UUID reviewEventId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement organization = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            )
            VALUES (?, ?, ?, 'active', 'UTC')
            """);
        PreparedStatement user = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id,
              organization_id,
              email,
              display_name,
              status
            )
            VALUES (?, ?, ?, ?, 'active')
            """);
        PreparedStatement review = connection.prepareStatement("""
            INSERT INTO governance.review_event (
              review_event_id,
              organization_id,
              reviewer_user_id,
              target_entity_type,
              target_entity_id,
              field_path,
              risk_tier,
              decision,
              bulk_flag,
              duration_ms,
              reason
            )
            VALUES (?, ?, ?, 'candidate', ?, 'headline', ?::governance.risk_tier,
              'approved', false, 700, 'reviewed source span before canonical boundary')
            """)) {
      organization.setObject(1, organizationId);
      organization.setString(2, "Task 3D Org " + organizationId);
      organization.setString(3, "Task 3D Org");
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "canonical-boundary-" + userId + "@example.test");
      user.setString(4, "Task 3D Reviewer");
      user.executeUpdate();

      review.setObject(1, reviewEventId);
      review.setObject(2, organizationId);
      review.setObject(3, userId);
      review.setObject(4, candidateId);
      review.setString(5, RiskTier.T1_LOW_RISK.wireValue());
      review.executeUpdate();
    }
  }

  private static void insertOrganization(UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            )
            VALUES (?, ?, ?, 'active', 'UTC')
            """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, "Task 6C Profile Org " + organizationId);
      statement.setString(3, "Task 6C Profile Org");
      statement.executeUpdate();
    }
  }

  private static void insertUser(UUID organizationId, UUID userId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id,
              organization_id,
              email,
              display_name,
              status
            )
            VALUES (?, ?, ?, ?, 'active')
            """)) {
      statement.setObject(1, userId);
      statement.setObject(2, organizationId);
      statement.setString(3, "canonical-boundary-" + userId + "@example.test");
      statement.setString(4, "Task 6D Reviewer");
      statement.executeUpdate();
    }
  }

  private static void insertCandidate(UUID organizationId, UUID candidateId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO recruiting.candidate (
              candidate_id,
              organization_id,
              status
            )
            VALUES (?, ?, 'new')
            """)) {
      statement.setObject(1, candidateId);
      statement.setObject(2, organizationId);
      statement.executeUpdate();
    }
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT count(*) FROM " + tableName + " WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static String findWorkflowAction(UUID workflowEventId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT action
            FROM workflow.workflow_event
            WHERE workflow_event_id = ?
            """)) {
      statement.setObject(1, workflowEventId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getString("action");
      }
    }
  }

  private static java.util.List<String> appliedMigrationVersions() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank");
        ResultSet resultSet = statement.executeQuery()) {
      java.util.List<String> versions = new java.util.ArrayList<>();
      while (resultSet.next()) {
        versions.add(resultSet.getString("version"));
      }
      return versions;
    }
  }

  private static boolean tableExists(String schema, String table) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
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

  private static final class DeliberateCanonicalWriteFailure extends RuntimeException {
    private DeliberateCanonicalWriteFailure(String message) {
      super(message);
    }
  }
}
