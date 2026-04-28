package com.recruitingtransactionos.coreapi.governedintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceType;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
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
import com.recruitingtransactionos.coreapi.governedintake.service.DeterministicIntakeExtractionService;
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
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
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
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
class GovernedIntakeEndToEndRegressionTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final UUID ORG_A = uuid("00000000-0000-0000-0000-000000260001");
  private static final UUID ORG_B = uuid("00000000-0000-0000-0000-000000260002");
  private static final UUID REVIEWER_A = uuid("00000000-0000-0000-0000-000000260003");
  private static final UUID REVIEWER_B = uuid("00000000-0000-0000-0000-000000260004");
  private static final UUID TARGET_CANDIDATE_A =
      uuid("00000000-0000-0000-0000-000000260005");
  private static final Instant RECEIVED_AT = Instant.parse("2026-04-28T18:00:00Z");

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
  void minimalSliceRunsThroughCanonicalGateWithoutCanonicalPersistence()
      throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(7);
    assertThat(appliedMigrationVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7");
    int candidateRowsBefore = countRows("recruiting.candidate", ORG_A);
    int candidateProfileRowsBefore = countRows("recruiting.candidate_profile", ORG_A);

    GovernedIntakeService intakeService = intakeService();
    SourceItem cv = intakeService.registerSourceItem(sourceCommand(ORG_A, "minimal-cv").build());
    SourceItem note = intakeService.registerSourceItem(
        sourceCommand(ORG_A, "minimal-note")
            .sourceType(SourceItemType.CALL_NOTE)
            .title("Consultant call note minimal-note")
            .build());
    InformationPacket packet = intakeService.createInformationPacket(
        packetCommand(ORG_A, "minimal-chain").build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        cv.sourceItemId()));
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        note.sourceItemId()));

    IntakeExtractionRun placeholderRun =
        extractionService().extract(ORG_A, packet.informationPacketId());
    IntakeExtractionOutputEnvelope placeholderOutput =
        placeholderRun.outputEnvelope().orElseThrow();

    assertThat(placeholderRun.status()).isEqualTo(IntakeExtractionStatus.SUCCEEDED);
    assertThat(placeholderOutput.sourceItemIds())
        .containsExactlyInAnyOrder(cv.sourceItemId(), note.sourceItemId());
    assertThat(fieldValue(placeholderOutput, "source_count")).isEqualTo("2");
    assertThat(fieldValue(placeholderOutput, "real_ai_extraction_performed")).isEqualTo("false");
    assertThat(fieldValue(placeholderOutput, "semantic_parsing_performed")).isEqualTo("false");
    assertThat(fieldValue(placeholderOutput, "claim_ledger_append_allowed")).isEqualTo("false");
    assertThat(fieldValue(placeholderOutput, "canonical_write_allowed")).isEqualTo("false");
    assertThat(fieldValue(placeholderOutput, "needs_future_extraction")).isEqualTo("true");

    IntakeClaimLedgerBridgeResult defaultBridge =
        claimBridgeService().bridge(claimBridgeRequest(ORG_A, placeholderRun.extractionRunId()));

    assertThat(defaultBridge.bridgeStatus())
        .isEqualTo(IntakeClaimLedgerBridgeStatus.NO_CLAIMS_APPENDED);
    assertThat(defaultBridge.appendedClaimIds()).isEmpty();
    assertThat(countClaimsForExtractionRun(placeholderRun.extractionRunId())).isZero();

    IntakeExtractionRun eligibleRun =
        saveBridgeEligibleRun("minimal-chain-eligible", packet, List.of(cv, note));
    IntakeClaimLedgerBridgeRequest claimBridgeRequest =
        claimBridgeRequest(ORG_A, eligibleRun.extractionRunId());
    IntakeClaimLedgerBridgeResult claimResult =
        claimBridgeService().bridge(claimBridgeRequest);
    IntakeClaimLedgerBridgeResult duplicateClaimResult =
        claimBridgeService().bridge(claimBridgeRequest);

    assertThat(claimResult.bridgeStatus()).isEqualTo(IntakeClaimLedgerBridgeStatus.SUCCEEDED);
    assertThat(claimResult.appendedClaimIds()).hasSize(1);
    ClaimId claimId = claimResult.appendedClaimIds().getFirst();
    assertThat(duplicateClaimResult.appendedClaimIds()).isEmpty();
    assertThat(duplicateClaimResult.existingClaimIds()).containsExactly(claimId);
    assertThat(countClaimsForExtractionRun(eligibleRun.extractionRunId())).isEqualTo(1);

    PersistedClaim claimBeforeReview = findClaim(claimId);
    assertThat(claimBeforeReview.organizationId()).isEqualTo(ORG_A);
    assertThat(claimBeforeReview.entityType()).isEqualTo("information_packet");
    assertThat(claimBeforeReview.entityId()).isEqualTo(packet.informationPacketId().value());
    assertThat(claimBeforeReview.claimType()).isEqualTo(ClaimType.INFERENCE.wireValue());
    assertThat(claimBeforeReview.assertionStrength())
        .isEqualTo(AssertionStrength.WEAK_SIGNAL.wireValue());
    assertThat(claimBeforeReview.verificationStatus())
        .isEqualTo(VerificationStatus.AI_EXTRACTED.wireValue());
    assertThat(claimBeforeReview.clientShareability())
        .isEqualTo(ClientShareability.INTERNAL_ONLY.wireValue());
    assertThat(claimBeforeReview.canonicalWriteAllowed()).isFalse();
    assertThat(claimBeforeReview.targetFieldPath())
        .isEqualTo("intake.bridge_eligible.quality_note");
    assertThat(claimBeforeReview.sourceItemId()).isNull();
    assertThat(claimBeforeReview.sourceSpanRef())
        .contains("intake.extraction_run:" + eligibleRun.extractionRunId().value())
        .contains("intake.information_packet:" + packet.informationPacketId().value())
        .contains("intake.source_item:" + cv.sourceItemId().value())
        .contains("packet_type:CANDIDATE")
        .contains("intended_entity_type:CANDIDATE");
    assertThat(countWorkflowEventsForClaim(claimId)).isZero();

    IntakeReviewBridgeRequest reviewRequest = reviewRequest(
        ORG_A,
        claimId,
        REVIEWER_A,
        "reviewed governed-intake claim before canonical boundary");
    IntakeReviewBridgeResult reviewResult = reviewBridgeService().bridge(reviewRequest);
    IntakeReviewBridgeResult duplicateReviewResult = reviewBridgeService().bridge(reviewRequest);

    assertThat(reviewResult.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    assertThat(duplicateReviewResult.status())
        .isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_ALREADY_EXISTS);
    assertThat(duplicateReviewResult.existingReviewEventId())
        .isEqualTo(reviewResult.reviewEventId());
    assertThat(countReviewEventsForClaim(claimId)).isEqualTo(1);

    PersistedReviewEvent reviewBeforeCanonical = findReview(reviewResult.reviewEventId());
    assertThat(reviewBeforeCanonical.organizationId()).isEqualTo(ORG_A);
    assertThat(reviewBeforeCanonical.claimLedgerItemId()).isEqualTo(claimId.value());
    assertThat(reviewBeforeCanonical.targetEntityType()).isEqualTo("information_packet");
    assertThat(reviewBeforeCanonical.targetEntityId()).isEqualTo(packet.informationPacketId().value());
    assertThat(reviewBeforeCanonical.fieldPath())
        .isEqualTo("intake.bridge_eligible.quality_note");
    assertThat(reviewBeforeCanonical.decision()).isEqualTo(ReviewDecision.APPROVED.wireValue());
    assertThat(reviewBeforeCanonical.riskTier()).isEqualTo(RiskTier.T2_MEDIUM_RISK.wireValue());
    assertThat(reviewBeforeCanonical.bulkFlag()).isFalse();
    assertThat(reviewBeforeCanonical.sourceSpanRef())
        .contains("intake.review_bridge")
        .contains("claim_ledger_item:" + claimId.value());
    assertThat(findClaim(claimId)).isEqualTo(claimBeforeReview);
    assertThat(countWorkflowEventsForClaim(claimId)).isZero();

    IntakeCanonicalWriteBridgeRequest canonicalRequest = canonicalRequest(
        ORG_A,
        claimId,
        reviewResult.reviewEventId(),
        TARGET_CANDIDATE_A,
        null,
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T2_MEDIUM_RISK,
        "attempt governed-intake claim against mandatory canonical write gate");
    IntakeCanonicalWriteBridgeResult canonicalResult =
        canonicalBridgeService().bridge(canonicalRequest);
    IntakeCanonicalWriteBridgeResult duplicateBlockedResult =
        canonicalBridgeService().bridge(canonicalRequest);

    assertThat(canonicalResult.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_BLOCKED);
    assertThat(canonicalResult.gateDecision()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(canonicalResult.blockedReason())
        .contains("system_inference_cannot_be_canonical_fact")
        .contains("ai_extracted_claim_cannot_be_canonical_fact");
    assertThat(canonicalResult.canonicalPersistencePerformed()).isFalse();
    assertThat(canonicalResult.canonicalPersistenceStatus())
        .isEqualTo("not_attempted_gate_did_not_allow");
    assertThat(duplicateBlockedResult.status()).isEqualTo(canonicalResult.status());
    assertThat(duplicateBlockedResult.blockedReason()).isEqualTo(canonicalResult.blockedReason());
    assertThat(countWorkflowEventsForClaim(claimId)).isZero();
    assertThat(tableExists("audit", "canonical_write_attempt")).isFalse();
    assertThat(tableExists("governance", "canonical_write_attempt")).isFalse();

    assertThat(findClaim(claimId)).isEqualTo(claimBeforeReview);
    assertThat(findReview(reviewResult.reviewEventId())).isEqualTo(reviewBeforeCanonical);
    assertThat(countRows("intake.source_item", ORG_A)).isGreaterThanOrEqualTo(2);
    assertThat(countRows("intake.information_packet", ORG_A)).isGreaterThanOrEqualTo(1);
    assertThat(countRows("intake.information_packet_source_item", ORG_A))
        .isGreaterThanOrEqualTo(2);
    assertThat(countRows("intake.extraction_run", ORG_A)).isGreaterThanOrEqualTo(2);
    assertThat(countRows("recruiting.candidate", ORG_A)).isEqualTo(candidateRowsBefore);
    assertThat(countRows("recruiting.candidate_profile", ORG_A))
        .isEqualTo(candidateProfileRowsBefore);
    assertThat(countRows("recruiting.source_item", ORG_A)).isZero();
    assertThat(countRows("recruiting.information_packet", ORG_A)).isZero();
  }

  @Test
  void allowedFixtureWritesMinimalCandidateProfileFieldAndRemainsIdempotent()
      throws SQLException {
    ClaimId claimId = insertAllowedGovernedClaim("allowed-e2e");
    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        RiskTier.T2_MEDIUM_RISK,
        "approved governed-intake fixture for minimal profile write");
    PersistedClaim claimBeforeCanonical = findClaim(claimId);
    PersistedReviewEvent reviewBeforeCanonical = findReview(reviewEventId);
    CandidateProfile profile = createCandidateProfile(
        deterministicUuid("allowed-e2e-candidate"),
        1);
    IntakeCanonicalWriteBridgeRequest request = canonicalRequest(
        ORG_A,
        claimId,
        reviewEventId,
        profile.candidateId().value(),
        profile.candidateProfileId(),
        VerificationStatus.CONSULTANT_ATTESTED,
        RiskTier.T2_MEDIUM_RISK,
        "allowed fixture writes one minimal candidate profile field");

    IntakeCanonicalWriteBridgeResult first = canonicalBridgeService().bridge(request);
    IntakeCanonicalWriteBridgeResult second = canonicalBridgeService().bridge(request);

    assertThat(first.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.GATE_ALLOWED_AUDITED);
    assertThat(first.gateDecision()).isEqualTo(CanonicalWriteDecisionType.ALLOW);
    assertThat(first.workflowEventId()).isNotNull();
    assertThat(first.canonicalPersistencePerformed()).isTrue();
    assertThat(first.canonicalPersistenceStatus())
        .isEqualTo(CanonicalWriteService.CANDIDATE_PROFILE_FIELD_PERSISTED);
    assertThat(second.workflowEventId()).isEqualTo(first.workflowEventId());
    assertThat(countWorkflowEventsForClaim(claimId)).isEqualTo(1);
    assertThat(second.canonicalPersistencePerformed()).isTrue();

    PersistedWorkflowEvent workflowEvent = findWorkflowEvent(first.workflowEventId().value());
    assertThat(workflowEvent.action()).isEqualTo(WorkflowActionCode.CANONICAL_WRITE_ALLOWED.wireValue());
    assertThat(workflowEvent.sourceRefId()).isEqualTo(claimId.value());
    assertThat(workflowEvent.reviewEventId()).isEqualTo(reviewEventId.value());
    assertThat(workflowEvent.idempotencyKey())
        .contains("intake-canonical-write-bridge")
        .contains(claimId.value().toString())
        .contains(reviewEventId.value().toString());

    assertThat(findClaim(claimId)).isEqualTo(claimBeforeCanonical);
    assertThat(findReview(reviewEventId)).isEqualTo(reviewBeforeCanonical);
    CandidateProfileField field = candidateProfileService().listCandidateProfileFields(
        ORG_A,
        profile.candidateProfileId()).getFirst();
    assertThat(field.fieldPath()).isEqualTo(CandidateProfileFieldPath.IDENTITY_FULL_NAME);
    assertThat(field.value()).isEqualTo(CandidateProfileFieldValue.ofString(
        "hash-only requested headline fixture"));
    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.CONSULTANT_ATTESTED);
    assertThat(field.sourceClaimId()).isEqualTo(claimId);
    assertThat(field.sourceReviewEventId()).isEqualTo(reviewEventId);
    assertThat(field.sourceWorkflowEventId()).isEqualTo(first.workflowEventId());
    assertThat(field.lineage().sourceReferences())
        .extracting(reference -> reference.sourceType())
        .containsExactly(
            CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM,
            CandidateProfileFieldSourceType.REVIEW_EVENT,
            CandidateProfileFieldSourceType.WORKFLOW_EVENT,
            CandidateProfileFieldSourceType.SOURCE_SPAN);
    assertThat(field.lineage().sourceReferences())
        .extracting(reference -> reference.sourceId())
        .contains(
            claimId.value().toString(),
            reviewEventId.value().toString(),
            first.workflowEventId().value().toString());
    assertThat(field.lineage().sourceReferences().stream()
        .filter(reference -> reference.sourceType() == CandidateProfileFieldSourceType.SOURCE_SPAN)
        .map(reference -> reference.sourceId())
        .findFirst()
        .orElseThrow())
        .contains("intake.extraction_run:")
        .contains("intake.information_packet:")
        .contains("intake.source_item:");
    assertThat(candidateProfileService().listCandidateProfileFields(
        ORG_A,
        profile.candidateProfileId())).hasSize(1);
    assertThat(candidateProfileService().findCandidateProfileByIdAndOrganizationId(
        ORG_B,
        profile.candidateProfileId())).isEmpty();
    assertThat(candidateProfileService().listCandidateProfileFields(
        ORG_B,
        profile.candidateProfileId())).isEmpty();
    assertThat(countRows("recruiting.source_item", ORG_A)).isZero();
    assertThat(countRows("recruiting.information_packet", ORG_A)).isZero();
  }

  @Test
  void failingCandidateProfileWriteRollsBackWorkflowAuditFromGovernedIntake()
      throws SQLException {
    ClaimId claimId = insertAllowedGovernedClaim("rollback-e2e");
    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        RiskTier.T2_MEDIUM_RISK,
        "approved governed-intake fixture before induced profile write failure");
    PersistedClaim claimBeforeCanonical = findClaim(claimId);
    PersistedReviewEvent reviewBeforeCanonical = findReview(reviewEventId);
    CandidateProfile existingProfile = createCandidateProfile(
        deterministicUuid("rollback-e2e-candidate"),
        2);
    CandidateProfileId missingProfileId = new CandidateProfileId(
        deterministicUuid("rollback-e2e-missing-profile"));
    IntakeCanonicalWriteBridgeRequest request = canonicalRequest(
        ORG_A,
        claimId,
        reviewEventId,
        existingProfile.candidateId().value(),
        missingProfileId,
        VerificationStatus.CONSULTANT_ATTESTED,
        RiskTier.T2_MEDIUM_RISK,
        "induce candidate profile write failure after allowed canonical audit");

    assertThatThrownBy(() -> canonicalBridgeService().bridge(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("candidate profile not found in organization");

    assertThat(countWorkflowEventsForClaim(claimId)).isZero();
    assertThat(candidateProfileService().listCandidateProfileFields(
        ORG_A,
        existingProfile.candidateProfileId())).isEmpty();
    assertThat(findClaim(claimId)).isEqualTo(claimBeforeCanonical);
    assertThat(findReview(reviewEventId)).isEqualTo(reviewBeforeCanonical);
  }

  @Test
  void wrongOrganizationCannotTraverseGovernedIntakeChain() throws SQLException {
    GovernedIntakeService intakeService = intakeService();
    SourceItem sourceItem = intakeService.registerSourceItem(
        sourceCommand(ORG_A, "wrong-org-source").build());
    InformationPacket packet = intakeService.createInformationPacket(
        packetCommand(ORG_A, "wrong-org-packet").build());
    intakeService.attachSourceItemToPacket(new AttachSourceItemToPacketCommand(
        ORG_A,
        packet.informationPacketId(),
        sourceItem.sourceItemId()));

    assertThatThrownBy(() -> extractionService().extract(ORG_B, packet.informationPacketId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("information packet not found in organization");

    IntakeExtractionRun eligibleRun =
        saveBridgeEligibleRun("wrong-org-eligible", packet, List.of(sourceItem));
    assertThatThrownBy(() -> claimBridgeService().bridge(
        claimBridgeRequest(ORG_B, eligibleRun.extractionRunId())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("extraction run not found in organization");

    ClaimId claimId = claimBridgeService()
        .bridge(claimBridgeRequest(ORG_A, eligibleRun.extractionRunId()))
        .appendedClaimIds()
        .getFirst();

    IntakeReviewBridgeResult wrongOrgReview = reviewBridgeService().bridge(reviewRequest(
        ORG_B,
        claimId,
        REVIEWER_B,
        "wrong organization must not review governed-intake claim"));
    assertThat(wrongOrgReview.status()).isEqualTo(IntakeReviewBridgeStatus.BLOCKED);
    assertThat(wrongOrgReview.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(countRows("governance.review_event", ORG_B)).isZero();

    ReviewEventId reviewEventId = appendReviewEvent(
        claimId,
        RiskTier.T2_MEDIUM_RISK,
        "correct organization review before wrong-org canonical check");
    IntakeCanonicalWriteBridgeResult wrongOrgCanonical =
        canonicalBridgeService().bridge(canonicalRequest(
            ORG_B,
            claimId,
            reviewEventId,
            deterministicUuid("wrong-org-target"),
            null,
            VerificationStatus.HUMAN_ACKNOWLEDGED,
            RiskTier.T2_MEDIUM_RISK,
            "wrong organization must not attempt canonical write"));

    assertThat(wrongOrgCanonical.status()).isEqualTo(IntakeCanonicalWriteBridgeStatus.FAILED);
    assertThat(wrongOrgCanonical.blockedReason())
        .isEqualTo("claim_ledger_item_not_found_in_organization");
    assertThat(countRows("intake.source_item", ORG_B)).isZero();
    assertThat(countRows("intake.information_packet", ORG_B)).isZero();
    assertThat(countRows("intake.extraction_run", ORG_B)).isZero();
    assertThat(countRows("governance.claim_ledger_item", ORG_B)).isZero();
    assertThat(countRows("workflow.workflow_event", ORG_B)).isZero();
    assertThat(countRows("recruiting.candidate", ORG_B)).isZero();
    assertThat(countRows("recruiting.candidate_profile", ORG_B)).isZero();
  }

  @Test
  void bridgeSourcesKeepCanonicalWriteBehindReviewAndDoNotWriteSkeletonTables()
      throws IOException {
    String claimBridgeSource = sourceFile(
        "src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/"
            + "IntakeClaimLedgerBridgeService.java");
    String reviewBridgeSource = sourceFile(
        "src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/"
            + "IntakeReviewBridgeService.java");
    String canonicalBridgeSource = sourceFile(
        "src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/"
            + "IntakeCanonicalWriteBridgeService.java");

    assertThat(claimBridgeSource)
        .doesNotContain("ReviewEventService")
        .doesNotContain("CanonicalWriteService")
        .doesNotContain("recruiting.source_item")
        .doesNotContain("recruiting.information_packet");
    assertThat(reviewBridgeSource)
        .doesNotContain("CanonicalWriteService")
        .doesNotContain("CanonicalWriteGate")
        .doesNotContain("recruiting.source_item")
        .doesNotContain("recruiting.information_packet");
    assertThat(canonicalBridgeSource)
        .contains("CanonicalWriteService")
        .doesNotContain("UPDATE governance.claim_ledger_item")
        .doesNotContain("UPDATE governance.review_event")
        .doesNotContain("INSERT INTO recruiting.candidate")
        .doesNotContain("INSERT INTO recruiting.candidate_profile")
        .doesNotContain("recruiting.source_item")
        .doesNotContain("recruiting.information_packet");
  }

  private static IntakeExtractionRun saveBridgeEligibleRun(
      String suffix,
      InformationPacket packet,
      List<SourceItem> sourceItems) {
    SourceItem primarySource = sourceItems.getFirst();
    IntakeExtractionRunId runId = new IntakeExtractionRunId(deterministicUuid(suffix));
    Instant now = RECEIVED_AT.plusSeconds(Math.abs(suffix.hashCode() % 1000));
    IntakeExtractionOutputEnvelope envelope = new IntakeExtractionOutputEnvelope(
        runId,
        ORG_A,
        packet.informationPacketId(),
        packet.packetType(),
        packet.intendedEntityType(),
        "intake-extraction-envelope.v1",
        sourceItems.stream().map(SourceItem::sourceItemId).toList(),
        sourceItems.stream()
            .map(sourceItem -> new IntakeExtractionSourceSnapshot(
                sourceItem.sourceItemId(),
                sourceItem.sourceType(),
                sourceItem.title(),
                sourceItem.contentHash(),
                sourceItem.externalRef()))
            .toList(),
        List.of(new IntakeExtractedField(
            "intake.bridge_eligible.quality_note",
            "explicitly marked operational bridge fixture",
            primarySource.sourceItemId(),
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
    UUID claimId = deterministicUuid("claim-" + suffix);
    UUID packetId = deterministicUuid("packet-" + suffix);
    UUID sourceId = deterministicUuid("source-" + suffix);
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
      RiskTier riskTier,
      String reason) {
    IntakeReviewBridgeResult result =
        reviewBridgeService().bridge(reviewRequest(ORG_A, claimId, REVIEWER_A, riskTier, reason));
    assertThat(result.status()).isEqualTo(IntakeReviewBridgeStatus.REVIEW_EVENT_APPENDED);
    return result.reviewEventId();
  }

  private static GovernedIntakeService intakeService() {
    return new GovernedIntakeService(
        new JdbcSourceItemPersistencePort(dataSource),
        new JdbcInformationPacketPersistencePort(dataSource));
  }

  private static DeterministicIntakeExtractionService extractionService() {
    return new DeterministicIntakeExtractionService(
        new JdbcInformationPacketPersistencePort(dataSource),
        extractionPort());
  }

  private static IntakeClaimLedgerBridgeService claimBridgeService() {
    return new IntakeClaimLedgerBridgeService(
        extractionPort(),
        new JdbcInformationPacketPersistencePort(dataSource),
        new ClaimLedgerService(new JdbcClaimLedgerPort(dataSource)),
        new JdbcClaimLedgerSourceReferenceLookupPort(dataSource));
  }

  private static IntakeReviewBridgeService reviewBridgeService() {
    return new IntakeReviewBridgeService(
        new JdbcClaimLedgerItemReviewLookupPort(dataSource),
        new ReviewEventService(new JdbcReviewEventPort(dataSource)),
        new JdbcReviewEventSourceReferenceLookupPort(dataSource));
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

  private static IntakeReviewBridgeRequest reviewRequest(
      UUID organizationId,
      ClaimId claimId,
      UUID reviewerId,
      String reason) {
    return reviewRequest(
        organizationId,
        claimId,
        reviewerId,
        RiskTier.T2_MEDIUM_RISK,
        reason);
  }

  private static IntakeReviewBridgeRequest reviewRequest(
      UUID organizationId,
      ClaimId claimId,
      UUID reviewerId,
      RiskTier riskTier,
      String reason) {
    return IntakeReviewBridgeRequest.builder()
        .organizationId(organizationId)
        .claimLedgerItemId(claimId)
        .reviewerActorType(ActorRole.CONSULTANT)
        .reviewerActorId(reviewerId)
        .reviewDecision(ReviewDecision.APPROVED)
        .riskTier(riskTier)
        .bulkFlag(false)
        .reason(reason)
        .reviewPolicy(IntakeReviewBridgePolicy.GOVERNED_INTAKE_CLAIMS_ONLY)
        .build();
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
        .correlationId(deterministicUuid("correlation-" + claimId.value()))
        .causationId(deterministicUuid("causation-" + reviewEventId.value()))
        .occurredAt(RECEIVED_AT.plusSeconds(3600))
        .bridgePolicy(IntakeCanonicalWriteBridgePolicy.GOVERNED_INTAKE_CLAIM_AND_REVIEW_ONLY)
        .build();
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

  private static InformationPacketCreateCommand.Builder packetCommand(
      UUID organizationId,
      String suffix) {
    return InformationPacketCreateCommand.builder()
        .organizationId(organizationId)
        .packetType(InformationPacketType.CANDIDATE)
        .intendedEntityType(IntendedEntityType.CANDIDATE)
        .createdByActorType(ActorRole.CONSULTANT)
        .processingStatus(InformationPacketStatus.READY_FOR_EXTRACTION)
        .notes("candidate packet " + suffix)
        .metadataJson("{\"packet\":\"" + suffix + "\"}");
  }

  private static String fieldValue(
      IntakeExtractionOutputEnvelope envelope,
      String fieldName) {
    return envelope.extractedFields().stream()
        .filter(field -> field.fieldName().equals(fieldName))
        .findFirst()
        .orElseThrow()
        .fieldValue();
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
              source_item_id,
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
            resultSet.getObject("source_item_id", UUID.class),
            resultSet.getObject("review_event_id", UUID.class));
      }
    }
  }

  private static PersistedReviewEvent findReview(ReviewEventId reviewEventId)
      throws SQLException {
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

  private static int countClaimsForExtractionRun(IntakeExtractionRunId extractionRunId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM governance.claim_ledger_item
            WHERE organization_id = ?
              AND source_span_ref LIKE ?
            """)) {
      statement.setObject(1, ORG_A);
      statement.setString(2, "%intake.extraction_run:" + extractionRunId.value() + "%");
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private static int countReviewEventsForClaim(ClaimId claimId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT count(*)
            FROM governance.review_event
            WHERE claim_ledger_item_id = ?
            """)) {
      statement.setObject(1, claimId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
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
      organization.setString(2, "Task 5F Org " + organizationId);
      organization.setString(3, "Task 5F Org");
      organization.executeUpdate();

      reviewer.setObject(1, reviewerId);
      reviewer.setObject(2, organizationId);
      reviewer.setString(3, "reviewer-" + reviewerId + "@example.test");
      reviewer.setString(4, "Task 5F Reviewer");
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

  private static String sourceFile(String relativePath) throws IOException {
    return Files.readString(projectPath(relativePath));
  }

  private static Path projectPath(String relativePath) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    Path direct = userDir.resolve(relativePath);
    if (Files.exists(direct)) {
      return direct;
    }
    return userDir.resolve("services/core-api").resolve(relativePath);
  }

  private static UUID deterministicUuid(String value) {
    return UUID.nameUUIDFromBytes(("task-5f:" + value).getBytes(StandardCharsets.UTF_8));
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
      String claimValue,
      UUID sourceItemId,
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
