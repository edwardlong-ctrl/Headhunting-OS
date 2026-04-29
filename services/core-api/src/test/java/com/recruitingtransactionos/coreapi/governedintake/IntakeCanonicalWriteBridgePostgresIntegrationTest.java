package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceReference;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceType;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.persistence.JdbcCandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CreateCandidateProfileRequest;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcClaimLedgerItemCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcClaimLedgerItemReviewLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcClaimLedgerSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcInformationPacketPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcIntakeExtractionRunPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcReviewEventCanonicalWriteLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcReviewEventSourceReferenceLookupPort;
import com.recruitingtransactionos.coreapi.governedintake.persistence.JdbcSourceItemPersistencePort;
import com.recruitingtransactionos.coreapi.governedintake.service.GovernedIntakeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeCanonicalWriteBridgeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeClaimLedgerBridgeService;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewBridgeService;
import com.recruitingtransactionos.coreapi.truthlayer.AssertionStrength;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteDecisionType;
import com.recruitingtransactionos.coreapi.truthlayer.CanonicalWriteGate;
import com.recruitingtransactionos.coreapi.truthlayer.ClaimType;
import com.recruitingtransactionos.coreapi.truthlayer.ClientShareability;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.VerificationStatus;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcReviewEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewDecision;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteService;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import com.recruitingtransactionos.coreapi.truthlayer.service.ReviewEventService;
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
import java.util.List;
import java.util.Optional;
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
class IntakeCanonicalWriteBridgePostgresIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000220001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000220002");
  private static final UUID REVIEWER_A = uuid("00000000-0000-0000-0000-000000220003");
  private static final UUID REVIEWER_B = uuid("00000000-0000-0000-0000-000000220004");
  private static final UUID TARGET_CANDIDATE_A =
      uuid("00000000-0000-0000-0000-000000220005");
  private static final Instant RECEIVED_AT = Instant.parse("2026-04-28T16:00:00Z");

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
    insertOrganizationAndReviewer(ORG_A, REVIEWER_A);
    insertOrganizationAndReviewer(ORG_B, REVIEWER_B);
  }

  @Test
  void flywaySchemaSupportsCanonicalWriteBridgeWithoutNewMigration() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(9);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    assertThat(columnExists("governance", "claim_ledger_item", "claim_value_text")).isTrue();
    assertThat(columnExists("governance", "review_event", "claim_ledger_item_id")).isTrue();
    assertThat(columnExists("workflow", "workflow_event", "idempotency_key")).isTrue();
  }

  @Test
  void governedIntakeClaimAndReviewReachCanonicalWriteGateAndRemainNonCanonical()
      throws SQLException {
    ClaimId claimId = appendGovernedIntakeClaim("canonical-gate-blocked");
    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        false,
        RiskTier.T2_MEDIUM_RISK,
        "reviewed governed-intake claim before canonical boundary");
    PersistedClaim claimBefore = findClaim(claimId);
    PersistedReviewEvent reviewBefore = findReview(reviewEventId);
    UUID candidateId = uuid("00000000-0000-0000-0000-000000220055");
    CandidateProfile profile = createCandidateProfile(candidateId, 1);

    IntakeCanonicalWriteBridgeResult result = canonicalBridgeService().bridge(canonicalRequest(
        ORG_A,
        claimId,
        reviewEventId,
        candidateId,
        profile.candidateProfileId(),
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T2_MEDIUM_RISK,
        "attempt governed-intake claim against canonical write gate"));

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_BLOCKED);
    assertThat(result.gateDecision()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(result.blockedReason())
        .contains("system_inference_cannot_be_canonical_fact")
        .contains("ai_extracted_claim_cannot_be_canonical_fact");
    assertThat(result.canonicalPersistencePerformed()).isFalse();
    assertThat(countWorkflowEventsForClaim(claimId)).isZero();
    assertThat(candidateProfileService().listCandidateProfileFields(
        ORG_A,
        profile.candidateProfileId())).isEmpty();
    assertThat(countRows("recruiting.source_item", ORG_A)).isZero();
    assertThat(countRows("recruiting.information_packet", ORG_A)).isZero();
    assertThat(findClaim(claimId)).isEqualTo(claimBefore);
    assertThat(findReview(reviewEventId)).isEqualTo(reviewBefore);
  }

  @Test
  void allowedBoundaryWritesMinimalCandidateProfileField()
      throws SQLException {
    ClaimId claimId = insertAllowedGovernedClaim("allowed-boundary");
    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        false,
        RiskTier.T2_MEDIUM_RISK,
        "approved governed-intake claim for minimal profile write");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000220105");
    CandidateProfile profile = createCandidateProfile(candidateId, 2);

    IntakeCanonicalWriteBridgeResult result = canonicalBridgeService().bridge(canonicalRequest(
        ORG_A,
        claimId,
        reviewEventId,
        candidateId,
        profile.candidateProfileId(),
        VerificationStatus.CONSULTANT_ATTESTED,
        RiskTier.T2_MEDIUM_RISK,
        "governed-intake allowed minimal profile write"));

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(result.gateDecision()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(result.workflowEventId()).isNotNull();
    assertThat(result.canonicalPersistencePerformed()).isTrue();
    assertThat(result.canonicalPersistenceStatus())
        .isEqualTo(CanonicalWriteService.CANDIDATE_PROFILE_FIELD_PERSISTED);
    PersistedWorkflowEvent workflowEvent = findWorkflowEvent(result.workflowEventId().value());
    assertThat(workflowEvent.action()).isEqualTo("CANONICAL_WRITE_ALLOWED");
    assertThat(workflowEvent.sourceRefId()).isEqualTo(claimId.value());
    assertThat(workflowEvent.reviewEventId()).isEqualTo(reviewEventId.value());
    assertThat(workflowEvent.idempotencyKey())
        .contains("intake-canonical-write-bridge")
        .contains(claimId.value().toString())
        .contains(reviewEventId.value().toString());
    CandidateProfileField field = candidateProfileService().listCandidateProfileFields(
        ORG_A,
        profile.candidateProfileId()).getFirst();
    assertThat(field.fieldPath()).isEqualTo(CandidateProfileFieldPath.IDENTITY_FULL_NAME);
    assertThat(field.value()).isEqualTo(CandidateProfileFieldValue.ofString(
        "hash-only requested headline fixture"));
    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.CONSULTANT_ATTESTED);
    assertThat(field.sourceClaimId()).isEqualTo(claimId);
    assertThat(field.sourceReviewEventId()).isEqualTo(reviewEventId);
    assertThat(field.sourceWorkflowEventId()).isEqualTo(result.workflowEventId());
    assertThat(field.lineage().sourceReferences())
        .extracting(CandidateProfileFieldSourceReference::sourceType)
        .contains(
            CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM,
            CandidateProfileFieldSourceType.REVIEW_EVENT,
            CandidateProfileFieldSourceType.WORKFLOW_EVENT,
            CandidateProfileFieldSourceType.SOURCE_SPAN);
    assertThat(field.lineage().sourceReferences().stream()
        .filter(reference -> reference.sourceType() == CandidateProfileFieldSourceType.SOURCE_SPAN)
        .map(CandidateProfileFieldSourceReference::sourceId)
        .findFirst()
        .orElseThrow())
        .contains("intake.extraction_run:")
        .contains("intake.information_packet:")
        .contains("intake.source_item:");
  }

  @Test
  void repeatedIdenticalAllowedAttemptReturnsExistingWorkflowAuditEvent() throws SQLException {
    ClaimId claimId = insertAllowedGovernedClaim("duplicate-allowed");
    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        false,
        RiskTier.T2_MEDIUM_RISK,
        "approved duplicate minimal profile write");
    UUID candidateId = uuid("00000000-0000-0000-0000-000000220205");
    CandidateProfile profile = createCandidateProfile(candidateId, 3);
    IntakeCanonicalWriteBridgeRequest request = canonicalRequest(
        ORG_A,
        claimId,
        reviewEventId,
        candidateId,
        profile.candidateProfileId(),
        VerificationStatus.CONSULTANT_ATTESTED,
        RiskTier.T2_MEDIUM_RISK,
        "duplicate minimal profile write should be deterministic");

    IntakeCanonicalWriteBridgeResult first = canonicalBridgeService().bridge(request);
    IntakeCanonicalWriteBridgeResult second = canonicalBridgeService().bridge(request);

    assertThat(first.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(second.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(second.workflowEventId()).isEqualTo(first.workflowEventId());
    assertThat(countWorkflowEventsForClaim(claimId)).isEqualTo(1);
    assertThat(candidateProfileService().listCandidateProfileFields(
        ORG_A,
        profile.candidateProfileId())).hasSize(1);
  }

  @Test
  void wrongOrganizationCannotAttemptCanonicalWriteBridge() throws SQLException {
    ClaimId claimId = appendGovernedIntakeClaim("wrong-org-canonical");
    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        false,
        RiskTier.T2_MEDIUM_RISK,
        "wrong organization must not cross bridge");

    IntakeCanonicalWriteBridgeResult result = canonicalBridgeService().bridge(canonicalRequest(
        ORG_B,
        claimId,
        reviewEventId,
        uuid("00000000-0000-0000-0000-000000220305"),
        null,
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T2_MEDIUM_RISK,
        "wrong org canonical write attempt must fail"));

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(result.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(countWorkflowEventsForClaim(claimId)).isZero();
    assertThat(countRows("workflow.workflow_event", ORG_B)).isZero();
  }

  @Test
  void bulkReviewCannotProduceExternalVerifiedCanonicalBoundary() throws SQLException {
    ClaimId claimId = insertAllowedGovernedClaim("bulk-external-verified");
    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        true,
        RiskTier.T1_LOW_RISK,
        "bulk review remains evidence only");
    int profileRowsBefore = countRows("recruiting.candidate_profile", ORG_A);

    IntakeCanonicalWriteBridgeResult result = canonicalBridgeService().bridge(canonicalRequest(
        ORG_A,
        claimId,
        reviewEventId,
        uuid("00000000-0000-0000-0000-000000220405"),
        null,
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T1_LOW_RISK,
        "bulk review must not create external verified fact"));

    assertThat(result.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_BLOCKED);
    assertThat(result.blockedReason()).contains("bulk_approve_cannot_create_external_verified");
    assertThat(countWorkflowEventsForClaim(claimId)).isZero();
    assertThat(countRows("recruiting.candidate_profile", ORG_A)).isEqualTo(profileRowsBefore);
  }

  private static ClaimId appendGovernedIntakeClaim(String suffix) {
    IntakeExtractionRun run = saveBridgeEligibleRun(suffix);
    IntakeClaimLedgerBridgeResult result = claimBridgeService().bridge(claimBridgeRequest(
        ORG_A,
        run.extractionRunId()));
    assertThat(result.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.SUCCEEDED);
    assertThat(result.appendedClaimIds()).hasSize(1);
    return result.appendedClaimIds().getFirst();
  }

  private static IntakeExtractionRun saveBridgeEligibleRun(String suffix) {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(sourceCommand(ORG_A, suffix).build());
    InformationPacket packet = intakeService.createInformationPacket(packetCommand(ORG_A).build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));
    IntakeExtractionRunId runId = new IntakeExtractionRunId(uuid(
        "00000000-0000-0000-0000-" + deterministicTail(suffix)));
    Instant now = RECEIVED_AT.plusSeconds(Math.abs(suffix.hashCode() % 1000));
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        runId,
        ORG_A,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        "intake-extraction-envelope.v1",
        List.of(sourceItem.sourceItemId()),
        List.of(new IntakeExtractionSourceSnapshot(
            sourceItem.sourceItemId(),
            sourceItem.sourceType(),
            sourceItem.title(),
            sourceItem.contentHash(),
            sourceItem.externalRef())),
        List.of(new IntakeExtractedField(
            "intake.bridge_eligible.quality_note",
            "explicitly marked operational bridge fixture",
            sourceItem.sourceItemId(),
            0.5d,
            IntakeExtractedFieldStatus.CLAIM_CANDIDATE,
            "Operational fixture only; not a canonical candidate/company/job fact.")),
        List.of(),
        List.of(),
        now);
    return extractionPort().save(new IntakeExtractionRun(
        runId,
        ORG_A,
        packet.informationPacketId(),
        IntakeExtractionMode.DETERMINISTIC_PLACEHOLDER,
        IntakeExtractionStatus.SUCCEEDED,
        "intake-source-packet.v1",
        "intake-extraction-envelope.v1",
        "deterministic-placeholder.v1",
        "sha256:" + suffix,
        now,
        Optional.of(now),
        Optional.empty(),
        Optional.of(envelope)));
  }

  private static ClaimId insertAllowedGovernedClaim(String suffix) throws SQLException {
    UUID claimId = uuid("00000000-0000-0000-0000-" + deterministicTail(suffix));
    UUID packetId = uuid("00000000-0000-0000-0000-" + deterministicTail(suffix + "-packet"));
    UUID sourceId = uuid("00000000-0000-0000-0000-" + deterministicTail(suffix + "-source"));
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO governance.claim_ledger_item (
              claim_ledger_item_id,
              organization_id,
              entity_type,
              entity_id,
              claim_type,
              assertion_strength,
              source_span_ref,
              speaker,
              verification_status,
              canonical_write_allowed,
              client_shareability,
              target_field_path,
              claim_value_text
            )
            VALUES (?, ?, 'information_packet', ?, ?::governance.claim_type,
              ?::governance.assertion_strength, ?, 'consultant',
              ?::governance.verification_status, true,
              ?::governance.client_shareability,
              'intake.bridge_eligible.quality_note', ?)
            """)) {
      statement.setObject(1, claimId);
      statement.setObject(2, ORG_A);
      statement.setObject(3, packetId);
      statement.setString(4, ClaimType.FACT.wireValue());
      statement.setString(5, AssertionStrength.EXPLICIT.wireValue());
      statement.setString(6,
          "intake.extraction_run:" + claimId
              + "|intake.information_packet:" + packetId
              + "|packet_type:CANDIDATE"
              + "|intended_entity_type:CANDIDATE"
              + "|intake.source_item:" + sourceId
              + "|field:intake.bridge_eligible.quality_note");
      statement.setString(7, VerificationStatus.HUMAN_ACKNOWLEDGED.wireValue());
      statement.setString(8, ClientShareability.CLIENT_SAFE.wireValue());
      statement.setString(9, "reviewed operational bridge fixture");
      statement.executeUpdate();
      return new ClaimId(claimId);
    }
  }

  private static ReviewEventId appendReviewEvent(
      ClaimId claimId,
      boolean bulkFlag,
      RiskTier riskTier,
      String reason) {
    IntakeReviewBridgeResult result = reviewBridgeService().bridge(IntakeReviewBridgeRequest
        .builder()
        .organizationId(ORG_A)
        .claimLedgerItemId(claimId)
        .reviewerActorType(ActorRole.CONSULTANT)
        .reviewerActorId(REVIEWER_A)
        .reviewDecision(ReviewDecision.APPROVED)
        .riskTier(riskTier)
        .bulkFlag(bulkFlag)
        .reason(reason)
        .reviewPolicy(IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY)
        .build());
    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    return result.reviewEventId();
  }

  private static IntakeCanonicalWriteBridgeRequest canonicalRequest(
      UUID organizationId,
      ClaimId claimId,
      ReviewEventId reviewEventId,
      UUID targetEntityId,
      com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId candidateProfileId,
      VerificationStatus targetVerificationStatus,
      RiskTier riskTier,
      String reason) {
    return IntakeCanonicalWriteBridgeRequest.builder()
        .organizationId(organizationId)
        .claimLedgerItemId(claimId)
        .reviewEventId(reviewEventId)
        .requestedByActorType(ActorRole.CONSULTANT)
        .requestedByActorId(organizationId.equals(ORG_A) ? REVIEWER_A : REVIEWER_B)
        .candidateProfileId(candidateProfileId)
        .targetEntityType("CANDIDATE")
        .targetEntityId(targetEntityId)
        .targetFieldPath(candidateProfileId == null
            ? "headline"
            : CandidateProfileFieldPath.IDENTITY_FULL_NAME.value())
        .requestedCanonicalValue("hash-only requested headline fixture")
        .targetVerificationStatus(targetVerificationStatus)
        .riskTier(riskTier)
        .clientVisible(false)
        .conflictsWithCanonical(false)
        .transactionLegalApproval(false)
        .reason(reason)
        .correlationId(uuid("00000000-0000-0000-0000-000000220901"))
        .causationId(uuid("00000000-0000-0000-0000-000000220902"))
        .occurredAt(RECEIVED_AT.plusSeconds(3600))
        .bridgePolicy(IntakeCanonicalWriteBridgePolicy.GOVERNED_INTAKE_CLAIM_AND_REVIEW_ONLY)
        .build();
  }

  private static IntakeCanonicalWriteBridgeService canonicalBridgeService() {
    return new IntakeCanonicalWriteBridgeService(
        new JdbcClaimLedgerItemCanonicalWriteLookupPort(dataSource),
        new JdbcReviewEventCanonicalWriteLookupPort(dataSource),
        new CanonicalWriteService(
            new CanonicalWriteGate(),
            new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)),
            new SpringCanonicalWriteTransactionBoundary(
                new DataSourceTransactionManager(dataSource)),
            candidateProfileService()));
  }

  private static CandidateProfileService candidateProfileService() {
    return new CandidateProfileService(new JdbcCandidateProfilePersistencePort(dataSource));
  }

  private static CandidateProfile createCandidateProfile(UUID candidateId, int profileVersion)
      throws SQLException {
    insertCandidate(ORG_A, candidateId);
    return candidateProfileService().createCandidateProfile(new CreateCandidateProfileRequest(
        ORG_A,
        new CandidateId(candidateId),
        new CandidateProfileVersion(profileVersion),
        List.of()));
  }

  private static IntakeReviewBridgeService reviewBridgeService() {
    return new IntakeReviewBridgeService(
        new JdbcClaimLedgerItemReviewLookupPort(dataSource),
        new ReviewEventService(new JdbcReviewEventPort(dataSource)),
        new JdbcReviewEventSourceReferenceLookupPort(dataSource));
  }

  private static IntakeClaimLedgerBridgeService claimBridgeService() {
    return new IntakeClaimLedgerBridgeService(
        extractionPort(),
        new JdbcInformationPacketPersistencePort(dataSource),
        new ClaimLedgerService(new JdbcClaimLedgerPort(dataSource)),
        new JdbcClaimLedgerSourceReferenceLookupPort(dataSource));
  }

  private static GovernedIntakeService intakeService() {
    return new GovernedIntakeService(
        new JdbcSourceItemPersistencePort(dataSource),
        new JdbcInformationPacketPersistencePort(dataSource));
  }

  private static JdbcIntakeExtractionRunPort extractionPort() {
    return new JdbcIntakeExtractionRunPort(dataSource);
  }

  private static IntakeClaimLedgerBridgeRequest claimBridgeRequest(
      UUID organizationId,
      IntakeExtractionRunId extractionRunId) {
    return new IntakeClaimLedgerBridgeRequest(
        organizationId,
        extractionRunId,
        ActorRole.CONSULTANT,
        null,
        IntakeClaimLedgerBridgePolicy.OPERATIONAL_CLAIM_CANDIDATES_ONLY,
        null);
  }

  private static SourceItemRegistrationCommand.Builder sourceCommand(
      UUID organizationId,
      String suffix) {
    String value = organizationId.toString().substring(30) + "-" + suffix;
    return SourceItemRegistrationCommand.builder()
        .organizationId(organizationId)
        .sourceType(SourceItemType.CV)
        .origin(SourceItemOrigin.CONSULTANT_UPLOAD)
        .title("Senior DV engineer CV " + suffix)
        .contentHash("sha256:" + value)
        .externalRef("ats:" + value)
        .storageRef("s3://internal-intake/" + value)
        .rawRef("vault://source-" + value + "/raw")
        .language("en")
        .uploadedByActorType(ActorRole.CONSULTANT)
        .receivedAt(RECEIVED_AT)
        .metadataJson("{\"case\":\"" + value + "\"}")
        .status(SourceItemStatus.REGISTERED);
  }

  private static InformationPacketCreateCommand.Builder packetCommand(UUID organizationId) {
    return InformationPacketCreateCommand.builder()
        .organizationId(organizationId)
        .packetType(InformationPacketType.CANDIDATE)
        .intendedEntityType(IntendedEntityType.CANDIDATE)
        .createdByActorType(ActorRole.CONSULTANT)
        .processingStatus(InformationPacketStatus.READY_FOR_EXTRACTION)
        .notes("candidate packet 220001")
        .metadataJson("{\"packet\":\"220001\"}");
  }

  private static PersistedClaim findClaim(ClaimId claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              organization_id,
              entity_type,
              entity_id,
              claim_type::text AS claim_type,
              assertion_strength::text AS assertion_strength,
              source_span_ref,
              verification_status::text AS verification_status,
              canonical_write_allowed,
              client_shareability::text AS client_shareability,
              target_field_path,
              claim_value_text,
              review_event_id
            FROM governance.claim_ledger_item
            WHERE claim_ledger_item_id = ?
            """)) {
      statement.setObject(1, claimId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedClaim(
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getString("entity_type"),
            resultSet.getObject("entity_id", UUID.class),
            resultSet.getString("claim_type"),
            resultSet.getString("assertion_strength"),
            resultSet.getString("source_span_ref"),
            resultSet.getString("verification_status"),
            resultSet.getBoolean("canonical_write_allowed"),
            resultSet.getString("client_shareability"),
            resultSet.getString("target_field_path"),
            resultSet.getString("claim_value_text"),
            resultSet.getObject("review_event_id", UUID.class));
      }
    }
  }

  private static PersistedReviewEvent findReview(ReviewEventId reviewEventId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              review_event_id,
              organization_id,
              reviewer_user_id,
              target_entity_type,
              target_entity_id,
              field_path,
              risk_tier::text AS risk_tier,
              decision,
              bulk_flag,
              claim_ledger_item_id,
              source_span_ref,
              reason,
              created_at
            FROM governance.review_event
            WHERE review_event_id = ?
            """)) {
      statement.setObject(1, reviewEventId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedReviewEvent(
            resultSet.getObject("review_event_id", UUID.class),
            resultSet.getObject("organization_id", UUID.class),
            resultSet.getObject("reviewer_user_id", UUID.class),
            resultSet.getString("target_entity_type"),
            resultSet.getObject("target_entity_id", UUID.class),
            resultSet.getString("field_path"),
            resultSet.getString("risk_tier"),
            resultSet.getString("decision"),
            resultSet.getBoolean("bulk_flag"),
            resultSet.getObject("claim_ledger_item_id", UUID.class),
            resultSet.getString("source_span_ref"),
            resultSet.getString("reason"),
            resultSet.getObject("created_at", OffsetDateTime.class));
      }
    }
  }

  private static PersistedWorkflowEvent findWorkflowEvent(UUID workflowEventId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT
              action,
              source_ref_id,
              review_event_id,
              idempotency_key
            FROM workflow.workflow_event
            WHERE workflow_event_id = ?
            """)) {
      statement.setObject(1, workflowEventId);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new PersistedWorkflowEvent(
            resultSet.getString("action"),
            resultSet.getObject("source_ref_id", UUID.class),
            resultSet.getObject("review_event_id", UUID.class),
            resultSet.getString("idempotency_key"));
      }
    }
  }

  private static int countWorkflowEventsForClaim(ClaimId claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM workflow.workflow_event
            WHERE source_ref_id = ?
            """)) {
      statement.setObject(1, claimId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
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

  private static List<String> appliedMigrationVersions() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank");
        ResultSet resultSet = statement.executeQuery()) {
      List<String> versions = new java.util.ArrayList<>();
      while (resultSet.next()) {
        versions.add(resultSet.getString("version"));
      }
      return versions;
    }
  }

  private static boolean columnExists(String schema, String table, String column)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS ("
                + "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name = ?)")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      statement.setString(3, column);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private static void insertOrganizationAndReviewer(UUID organizationId, UUID reviewerId)
      throws SQLException {
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
        PreparedStatement reviewer = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id,
              organization_id,
              email,
              display_name,
              status
            )
            VALUES (?, ?, ?, ?, 'active')
            """)) {
      organization.setObject(1, organizationId);
      organization.setString(2, "Task 5E Org " + organizationId);
      organization.setString(3, "Task 5E Org");
      organization.executeUpdate();

      reviewer.setObject(1, reviewerId);
      reviewer.setObject(2, organizationId);
      reviewer.setString(3, "canonical-reviewer-" + reviewerId + "@example.test");
      reviewer.setString(4, "Task 5E Reviewer");
      reviewer.executeUpdate();
    }
  }

  private static void insertCandidate(UUID organizationId, UUID candidateId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO recruiting.candidate (
              candidate_id,
              organization_id,
              status,
              privacy_status
            )
            VALUES (?, ?, 'new', 'internal_only')
            """)) {
      statement.setObject(1, candidateId);
      statement.setObject(2, organizationId);
      statement.executeUpdate();
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

  private static String deterministicTail(String suffix) {
    return switch (suffix) {
      case "canonical-gate-blocked" -> "000000220101";
      case "wrong-org-canonical" -> "000000220102";
      case "allowed-boundary" -> "000000220201";
      case "allowed-boundary-packet" -> "000000220202";
      case "allowed-boundary-source" -> "000000220203";
      case "duplicate-allowed" -> "000000220301";
      case "duplicate-allowed-packet" -> "000000220302";
      case "duplicate-allowed-source" -> "000000220303";
      case "bulk-external-verified" -> "000000220401";
      case "bulk-external-verified-packet" -> "000000220402";
      case "bulk-external-verified-source" -> "000000220403";
      default -> "000000220999";
    };
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
  }

  private record PersistedClaim(
      UUID organizationId,
      String entityType,
      UUID entityId,
      String claimType,
      String assertionStrength,
      String sourceSpanRef,
      String verificationStatus,
      boolean canonicalWriteAllowed,
      String clientShareability,
      String targetFieldPath,
      String claimValueText,
      UUID reviewEventId) {
  }

  private record PersistedReviewEvent(
      UUID reviewEventId,
      UUID organizationId,
      UUID reviewerUserId,
      String targetEntityType,
      UUID targetEntityId,
      String fieldPath,
      String riskTier,
      String decision,
      boolean bulkFlag,
      UUID claimLedgerItemId,
      String sourceSpanRef,
      String reason,
      OffsetDateTime createdAt) {
  }

  private record PersistedWorkflowEvent(
      String action,
      UUID sourceRefId,
      UUID reviewEventId,
      String idempotencyKey) {
  }
}
