package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcCandidateWorkflowStatePort;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcDisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcUnlockDecisionPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.truthlayer.RiskTier;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.persistence.JdbcWorkflowEventPort;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.service.CanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.SpringCanonicalWriteTransactionBoundary;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import com.recruitingtransactionos.coreapi.workflowaudit.persistence.JdbcWorkflowEntityStatePort;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class ConsentDisclosurePostgresPersistenceIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Instant NOW = Instant.parse("2026-04-29T13:45:00Z");

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
  void appendAndReadBackConsentUnlockAndDisclosureWithinOrganizationScope() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b101");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-00000012b102");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b103");
    insertOrganizationAndUser(organizationId, consultantId);
    insertOrganizationAndUser(otherOrganizationId, uuid("00000000-0000-0000-0000-00000012b104"));

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);
    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);
    DisclosureRecordPort disclosurePort = new JdbcDisclosureRecordPort(dataSource);

    ConsentRecord consent = consentPort.append(consent(
        organizationId,
        "consent_record_task12b_pg_0001",
        "candidate_ref_task12b_pg_0001",
        "profile_ref_task12b_pg_0001",
        "job_ref_task12b_pg_0001"));
    UnlockDecision unlock = unlockPort.append(unlockDecision(
        organizationId,
        consultantId,
        "unlock_decision_task12b_pg_0001",
        "candidate_ref_task12b_pg_0001",
        "profile_ref_task12b_pg_0001",
        "job_ref_task12b_pg_0001",
        "client_ref_task12b_pg_0001"));
    DisclosureRecord disclosure = disclosurePort.append(approvedDisclosureRecord(
        organizationId,
        "disclosure_record_task12b_pg_approved_0001",
        "candidate_ref_task12b_pg_0001",
        "profile_ref_task12b_pg_0001",
        "job_ref_task12b_pg_0001",
        "client_ref_task12b_pg_0001",
        unlock.unlockDecisionRef(),
        consent.consentRecordRef()));

    assertThat(consentPort.findByRefAndOrganizationId(
        organizationId,
        consent.consentRecordRef())).contains(consent);
    assertThat(consentPort.findByRefAndOrganizationId(
        otherOrganizationId,
        consent.consentRecordRef())).isEmpty();
    assertThat(unlockPort.findByRefAndOrganizationId(
        organizationId,
        unlock.unlockDecisionRef())).contains(unlock);
    assertThat(disclosurePort.findByRefAndOrganizationId(
        organizationId,
        disclosure.disclosureRecordRef())).contains(disclosure);
    assertThat(countRows("privacy.consent_record", organizationId)).isEqualTo(1);
    assertThat(countRows("privacy.unlock_decision", organizationId)).isEqualTo(1);
    assertThat(countRows("privacy.disclosure_record", organizationId)).isEqualTo(1);
  }

  @Test
  void findsLatestConsentByCandidateProfileAndJobWithinOrganizationScope() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b151");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-00000012b152");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b153");
    insertOrganizationAndUser(organizationId, consultantId);
    insertOrganizationAndUser(otherOrganizationId, uuid("00000000-0000-0000-0000-00000012b154"));

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);

    ConsentRecord older = consentPort.append(new ConsentRecord(
        "consent_record_task12b_pg_latest_old",
        organizationId,
        "candidate_ref_task12b_pg_latest",
        "profile_ref_task12b_pg_latest",
        "job_ref_task12b_pg_latest",
        "1",
        "consent-v1",
        ConsentStatus.CONFIRMED,
        Set.of(DisclosureLevel.L2_CLIENT_SAFE),
        Instant.parse("2026-05-01T00:00:00Z"),
        Instant.parse("2026-06-01T00:00:00Z"),
        false));
    ConsentRecord newer = consentPort.append(new ConsentRecord(
        "consent_record_task12b_pg_latest_new",
        organizationId,
        "candidate_ref_task12b_pg_latest",
        "profile_ref_task12b_pg_latest",
        "job_ref_task12b_pg_latest",
        "2",
        "consent-v2",
        ConsentStatus.CONFIRMED,
        Set.of(DisclosureLevel.L2_CLIENT_SAFE),
        Instant.parse("2026-05-02T00:00:00Z"),
        Instant.parse("2026-06-02T00:00:00Z"),
        false));
    consentPort.append(new ConsentRecord(
        "consent_record_task12b_pg_latest_other_org",
        otherOrganizationId,
        "candidate_ref_task12b_pg_latest",
        "profile_ref_task12b_pg_latest",
        "job_ref_task12b_pg_latest",
        "9",
        "consent-v9",
        ConsentStatus.CONFIRMED,
        Set.of(DisclosureLevel.L2_CLIENT_SAFE),
        Instant.parse("2026-05-03T00:00:00Z"),
        Instant.parse("2026-06-03T00:00:00Z"),
        false));

    assertThat(consentPort.findLatestByCandidateProfileAndJob(
        organizationId,
        "candidate_ref_task12b_pg_latest",
        "profile_ref_task12b_pg_latest",
        "job_ref_task12b_pg_latest")).contains(newer);
    assertThat(consentPort.findLatestByCandidateProfileAndJob(
        otherOrganizationId,
        "candidate_ref_task12b_pg_latest",
        "profile_ref_task12b_pg_latest",
        "job_ref_task12b_pg_latest"))
        .hasValueSatisfying(value -> assertThat(value.consentRecordRef())
            .isEqualTo("consent_record_task12b_pg_latest_other_org"));
    assertThat(newer).isNotEqualTo(older);
  }

  @Test
  void consentUnlockAndDisclosureRefsAreReusableAcrossOrganizations() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b301");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-00000012b302");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b303");
    UUID otherConsultantId = uuid("00000000-0000-0000-0000-00000012b304");
    insertOrganizationAndUser(organizationId, consultantId);
    insertOrganizationAndUser(otherOrganizationId, otherConsultantId);

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);
    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);
    DisclosureRecordPort disclosurePort = new JdbcDisclosureRecordPort(dataSource);

    String consentRecordRef = "consent_record_task12b_pg_reusable_ref";
    String unlockDecisionRef = "unlock_decision_task12b_pg_reusable_ref";
    String disclosureRecordRef = "disclosure_record_task12b_pg_reusable_ref";

    ConsentRecord consent = consentPort.append(consent(
        organizationId,
        consentRecordRef,
        "candidate_ref_task12b_pg_reusable",
        "profile_ref_task12b_pg_reusable",
        "job_ref_task12b_pg_reusable"));
    UnlockDecision unlock = unlockPort.append(unlockDecision(
        organizationId,
        consultantId,
        unlockDecisionRef,
        "candidate_ref_task12b_pg_reusable",
        "profile_ref_task12b_pg_reusable",
        "job_ref_task12b_pg_reusable",
        "client_ref_task12b_pg_reusable"));
    DisclosureRecord disclosure = disclosurePort.append(approvedDisclosureRecord(
        organizationId,
        disclosureRecordRef,
        "candidate_ref_task12b_pg_reusable",
        "profile_ref_task12b_pg_reusable",
        "job_ref_task12b_pg_reusable",
        "client_ref_task12b_pg_reusable",
        unlock.unlockDecisionRef(),
        consent.consentRecordRef()));

    ConsentRecord otherConsent = consentPort.append(consent(
        otherOrganizationId,
        consentRecordRef,
        "candidate_ref_task12b_pg_reusable",
        "profile_ref_task12b_pg_reusable",
        "job_ref_task12b_pg_reusable"));
    UnlockDecision otherUnlock = unlockPort.append(unlockDecision(
        otherOrganizationId,
        otherConsultantId,
        unlockDecisionRef,
        "candidate_ref_task12b_pg_reusable",
        "profile_ref_task12b_pg_reusable",
        "job_ref_task12b_pg_reusable",
        "client_ref_task12b_pg_reusable"));
    DisclosureRecord otherDisclosure = disclosurePort.append(approvedDisclosureRecord(
        otherOrganizationId,
        disclosureRecordRef,
        "candidate_ref_task12b_pg_reusable",
        "profile_ref_task12b_pg_reusable",
        "job_ref_task12b_pg_reusable",
        "client_ref_task12b_pg_reusable",
        otherUnlock.unlockDecisionRef(),
        otherConsent.consentRecordRef()));

    assertThat(consentPort.findByRefAndOrganizationId(organizationId, consentRecordRef))
        .contains(consent);
    assertThat(consentPort.findByRefAndOrganizationId(otherOrganizationId, consentRecordRef))
        .contains(otherConsent);
    assertThat(unlockPort.findByRefAndOrganizationId(organizationId, unlockDecisionRef))
        .contains(unlock);
    assertThat(unlockPort.findByRefAndOrganizationId(otherOrganizationId, unlockDecisionRef))
        .contains(otherUnlock);
    assertThat(disclosurePort.findByRefAndOrganizationId(organizationId, disclosureRecordRef))
        .contains(disclosure);
    assertThat(disclosurePort.findByRefAndOrganizationId(otherOrganizationId, disclosureRecordRef))
        .contains(otherDisclosure);
  }

  @Test
  void disclosureRecordRejectsConsentAndUnlockRefsFromAnotherOrganization() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b401");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-00000012b402");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b403");
    insertOrganizationAndUser(organizationId, consultantId);
    insertOrganizationAndUser(otherOrganizationId, uuid("00000000-0000-0000-0000-00000012b404"));

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);
    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);
    DisclosureRecordPort disclosurePort = new JdbcDisclosureRecordPort(dataSource);

    ConsentRecord consent = consentPort.append(consent(
        organizationId,
        "consent_record_task12b_pg_cross_org_fk",
        "candidate_ref_task12b_pg_cross_org_fk",
        "profile_ref_task12b_pg_cross_org_fk",
        "job_ref_task12b_pg_cross_org_fk"));
    UnlockDecision unlock = unlockPort.append(unlockDecision(
        organizationId,
        consultantId,
        "unlock_decision_task12b_pg_cross_org_fk",
        "candidate_ref_task12b_pg_cross_org_fk",
        "profile_ref_task12b_pg_cross_org_fk",
        "job_ref_task12b_pg_cross_org_fk",
        "client_ref_task12b_pg_cross_org_fk"));

    assertThatThrownBy(() -> disclosurePort.append(approvedDisclosureRecord(
        otherOrganizationId,
        "disclosure_record_task12b_pg_cross_org_fk",
        "candidate_ref_task12b_pg_cross_org_fk",
        "profile_ref_task12b_pg_cross_org_fk",
        "job_ref_task12b_pg_cross_org_fk",
        "client_ref_task12b_pg_cross_org_fk",
        unlock.unlockDecisionRef(),
        consent.consentRecordRef())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to append disclosure record");
  }

  @Test
  void disclosureRecordRejectsSameOrganizationConsentAndUnlockRefsFromDifferentScope()
      throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b501");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b503");
    insertOrganizationAndUser(organizationId, consultantId);

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);
    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);
    DisclosureRecordPort disclosurePort = new JdbcDisclosureRecordPort(dataSource);

    ConsentRecord candidateOneConsent = consentPort.append(consent(
        organizationId,
        "consent_record_task14_pg_same_org_wrong_scope",
        "candidate_ref_task14_pg_scope_one",
        "profile_ref_task14_pg_scope_one",
        "job_ref_task14_pg_scope_one"));
    UnlockDecision candidateOneUnlock = unlockPort.append(unlockDecision(
        organizationId,
        consultantId,
        "unlock_decision_task14_pg_same_org_wrong_scope",
        "candidate_ref_task14_pg_scope_one",
        "profile_ref_task14_pg_scope_one",
        "job_ref_task14_pg_scope_one",
        "client_ref_task14_pg_scope_one"));

    assertThatThrownBy(() -> disclosurePort.append(approvedDisclosureRecord(
        organizationId,
        "disclosure_record_task14_pg_same_org_wrong_scope",
        "candidate_ref_task14_pg_scope_two",
        "profile_ref_task14_pg_scope_two",
        "job_ref_task14_pg_scope_two",
        "client_ref_task14_pg_scope_two",
        candidateOneUnlock.unlockDecisionRef(),
        candidateOneConsent.consentRecordRef())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to append disclosure record");
  }

  @Test
  void unlockDecisionApproverMustBelongToSameOrganization() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b601");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-00000012b602");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b603");
    UUID otherConsultantId = uuid("00000000-0000-0000-0000-00000012b604");
    insertOrganizationAndUser(organizationId, consultantId);
    insertOrganizationAndUser(otherOrganizationId, otherConsultantId);

    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);

    assertThatThrownBy(() -> unlockPort.append(unlockDecision(
        organizationId,
        otherConsultantId,
        "unlock_decision_task14_pg_cross_org_approver",
        "candidate_ref_task14_pg_cross_org_approver",
        "profile_ref_task14_pg_cross_org_approver",
        "job_ref_task14_pg_cross_org_approver",
        "client_ref_task14_pg_cross_org_approver")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to append unlock decision");
  }

  @Test
  void successfulAuditedIdentityDisclosureAppendsWorkflowEventAndFinalDisclosureRecord()
      throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b201");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b202");
    insertOrganizationAndUser(organizationId, consultantId);

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);
    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);
    DisclosureRecordPort disclosurePort = new JdbcDisclosureRecordPort(dataSource);

    ConsentRecord consent = consentPort.append(consent(
        organizationId,
        "consent_record_task12b_pg_1001",
        "candidate_ref_task12b_pg_1001",
        "profile_ref_task12b_pg_1001",
        "job_ref_task12b_pg_1001"));
    UnlockDecision unlock = unlockPort.append(unlockDecision(
        organizationId,
        consultantId,
        "unlock_decision_task12b_pg_1001",
        "candidate_ref_task12b_pg_1001",
        "profile_ref_task12b_pg_1001",
        "job_ref_task12b_pg_1001",
        "client_ref_task12b_pg_1001"));
    DisclosureRecord approvedDisclosure = disclosurePort.append(approvedDisclosureRecord(
        organizationId,
        "disclosure_record_task12b_pg_approved_1001",
        "candidate_ref_task12b_pg_1001",
        "profile_ref_task12b_pg_1001",
        "job_ref_task12b_pg_1001",
        "client_ref_task12b_pg_1001",
        unlock.unlockDecisionRef(),
        consent.consentRecordRef()));

    ConsentDisclosureService service = new ConsentDisclosureService(
        consentPort,
        unlockPort,
        disclosurePort,
        new ConsentDisclosureProtectionPolicy(),
        (request, unlockDecision, disclosureRecord) -> request.prerequisites(),
        new JdbcCandidateWorkflowStatePort(dataSource),
        new WorkflowTransitionAuditService(
            new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)),
            new JdbcWorkflowEntityStatePort(dataSource)),
        transactionBoundary());

    ConsentDisclosureServiceRequest request = ConsentDisclosureServiceRequest.builder()
        .organizationId(organizationId)
        .candidateRef("candidate_ref_task12b_pg_1001")
        .candidateProfileRef("profile_ref_task12b_pg_1001")
        .jobRef("job_ref_task12b_pg_1001")
        .clientRef("client_ref_task12b_pg_1001")
        .consentRecordRef(consent.consentRecordRef())
        .unlockDecisionRef(unlock.unlockDecisionRef())
        .approvedDisclosureRecordRef(approvedDisclosure.disclosureRecordRef())
        .requestedByRole(PortalRole.CONSULTANT)
        .actor(new ActorRef(consultantId, ActorRole.CONSULTANT))
        .requestedLevel(DisclosureLevel.L4_IDENTITY_DISCLOSED)
        .prerequisites(new ConsentDisclosurePrerequisites(true, true, true, true, true))
        .reason("consultant released identity after consent, unlock, and disclosure approval")
        .requestedAt(NOW)
        .build();

    ConsentDisclosureServiceResult result = service.evaluateDisclosureAttempt(request);
    ConsentDisclosureServiceResult replayed = service.evaluateDisclosureAttempt(request);

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.ALLOWED);
    assertThat(replayed).isEqualTo(result);
    assertThat(result.workflowEventId()).isPresent();
    assertThat(result.resultingDisclosureRecordRef()).isPresent();
    assertThat(countRows("privacy.consent_record", organizationId)).isEqualTo(1);
    assertThat(countRows("privacy.unlock_decision", organizationId)).isEqualTo(1);
    assertThat(countRows("privacy.disclosure_record", organizationId)).isEqualTo(2);
    assertThat(countRows("workflow.workflow_event", organizationId)).isEqualTo(2);
    assertThat(countRows("recruiting.candidate", organizationId)).isEqualTo(1);
    assertThat(countRows("recruiting.candidate_profile", organizationId)).isZero();
    assertThat(findCandidateStatus(
        organizationId,
        ConsentDisclosureService.candidateEntityId(
            organizationId,
            "candidate_ref_task12b_pg_1001")))
        .isEqualTo("identity_disclosed");
    assertThat(findDisclosureStatus(
        organizationId,
        approvedDisclosure.disclosureRecordRef()))
        .isEqualTo(DisclosureStatus.IDENTITY_DISCLOSED.wireValue());
    assertThat(findDisclosureStatus(
        organizationId,
        result.resultingDisclosureRecordRef().orElseThrow()))
        .isEqualTo(DisclosureStatus.IDENTITY_DISCLOSED.wireValue());
    assertThat(findWorkflowAction(result.workflowEventId().orElseThrow().value()))
        .isEqualTo(WorkflowActionCode.DISCLOSURE_IDENTITY_DISCLOSED.wireValue());
  }

  @Test
  void serviceDeniesLegacyCrossOrganizationUnlockApproverAfterMigration() throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b801");
    UUID otherOrganizationId = uuid("00000000-0000-0000-0000-00000012b802");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b803");
    UUID otherConsultantId = uuid("00000000-0000-0000-0000-00000012b804");
    insertOrganizationAndUser(organizationId, consultantId);
    insertOrganizationAndUser(otherOrganizationId, otherConsultantId);

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);
    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);
    DisclosureRecordPort disclosurePort = new JdbcDisclosureRecordPort(dataSource);

    ConsentRecord consent = consentPort.append(consent(
        organizationId,
        "consent_record_task14_pg_legacy_cross_org_approver",
        "candidate_ref_task14_pg_legacy_cross_org_approver",
        "profile_ref_task14_pg_legacy_cross_org_approver",
        "job_ref_task14_pg_legacy_cross_org_approver"));
    dropUnlockDecisionApproverOrgConstraint();
    try {
      insertUnlockDecisionWithCrossOrgApprover(
          organizationId,
          otherConsultantId,
          "unlock_decision_task14_pg_legacy_cross_org_approver",
          "candidate_ref_task14_pg_legacy_cross_org_approver",
          "profile_ref_task14_pg_legacy_cross_org_approver",
          "job_ref_task14_pg_legacy_cross_org_approver",
          "client_ref_task14_pg_legacy_cross_org_approver");
    } finally {
      restoreUnlockDecisionApproverOrgConstraint();
    }
    DisclosureRecord approvedDisclosure = disclosurePort.append(approvedDisclosureRecord(
        organizationId,
        "disclosure_record_task14_pg_legacy_cross_org_approver",
        "candidate_ref_task14_pg_legacy_cross_org_approver",
        "profile_ref_task14_pg_legacy_cross_org_approver",
        "job_ref_task14_pg_legacy_cross_org_approver",
        "client_ref_task14_pg_legacy_cross_org_approver",
        "unlock_decision_task14_pg_legacy_cross_org_approver",
        consent.consentRecordRef()));

    ConsentDisclosureService service = new ConsentDisclosureService(
        consentPort,
        unlockPort,
        disclosurePort,
        new ConsentDisclosureProtectionPolicy(),
        (request, unlockDecision, disclosureRecord) -> request.prerequisites(),
        new JdbcCandidateWorkflowStatePort(dataSource),
        new WorkflowTransitionAuditService(
            new WorkflowEventService(new JdbcWorkflowEventPort(dataSource)),
            new JdbcWorkflowEntityStatePort(dataSource)),
        transactionBoundary());

    ConsentDisclosureServiceResult result = service.evaluateDisclosureAttempt(
        ConsentDisclosureServiceRequest.builder()
            .organizationId(organizationId)
            .candidateRef("candidate_ref_task14_pg_legacy_cross_org_approver")
            .candidateProfileRef("profile_ref_task14_pg_legacy_cross_org_approver")
            .jobRef("job_ref_task14_pg_legacy_cross_org_approver")
            .clientRef("client_ref_task14_pg_legacy_cross_org_approver")
            .consentRecordRef(consent.consentRecordRef())
            .unlockDecisionRef("unlock_decision_task14_pg_legacy_cross_org_approver")
            .approvedDisclosureRecordRef(approvedDisclosure.disclosureRecordRef())
            .requestedByRole(PortalRole.CONSULTANT)
            .actor(new ActorRef(consultantId, ActorRole.CONSULTANT))
            .requestedLevel(DisclosureLevel.L4_IDENTITY_DISCLOSED)
            .prerequisites(new ConsentDisclosurePrerequisites(true, true, true, true, true))
            .reason("legacy cross-org approver must not release identity")
            .requestedAt(NOW)
            .build());

    assertThat(result.status()).isEqualTo(ConsentDisclosureServiceStatus.DENIED);
    assertThat(result.reasonCodes())
        .containsExactly("unlock_approver_organization_mismatch");
    assertThat(countRows("workflow.workflow_event", organizationId)).isZero();
    assertThat(countRows("privacy.disclosure_record", organizationId)).isEqualTo(1);
  }

  @Test
  void jdbcDisclosureAppendIfAbsentReturnsExistingFinalRecordWithoutDuplicateInsert()
      throws SQLException {
    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b701");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b702");
    insertOrganizationAndUser(organizationId, consultantId);

    ConsentRecordPort consentPort = new JdbcConsentRecordPort(dataSource);
    UnlockDecisionPort unlockPort = new JdbcUnlockDecisionPort(dataSource);
    DisclosureRecordPort disclosurePort = new JdbcDisclosureRecordPort(dataSource);

    ConsentRecord consent = consentPort.append(consent(
        organizationId,
        "consent_record_task14_pg_idempotent",
        "candidate_ref_task14_pg_idempotent",
        "profile_ref_task14_pg_idempotent",
        "job_ref_task14_pg_idempotent"));
    UnlockDecision unlock = unlockPort.append(unlockDecision(
        organizationId,
        consultantId,
        "unlock_decision_task14_pg_idempotent",
        "candidate_ref_task14_pg_idempotent",
        "profile_ref_task14_pg_idempotent",
        "job_ref_task14_pg_idempotent",
        "client_ref_task14_pg_idempotent"));
    DisclosureRecord finalRecord = new DisclosureRecord(
        "disclosure_record_task14_pg_idempotent_final",
        organizationId,
        "candidate_ref_task14_pg_idempotent",
        "profile_ref_task14_pg_idempotent",
        "job_ref_task14_pg_idempotent",
        "client_ref_task14_pg_idempotent",
        DisclosureStatus.IDENTITY_DISCLOSED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        unlock.unlockDecisionRef(),
        consent.consentRecordRef(),
        Optional.empty(),
        NOW);

    DisclosureRecord first = disclosurePort.appendIfAbsent(finalRecord);
    DisclosureRecord second = disclosurePort.appendIfAbsent(finalRecord);

    assertThat(second).isEqualTo(first);
    assertThat(countRows("privacy.disclosure_record", organizationId)).isEqualTo(1);
  }

  @Test
  void fullFlywayMigrationAddsTask12bPrivacyPersistenceTables() throws SQLException {
    assertThat(migrateResult.migrationsExecuted).isEqualTo(34);
    assertThat(appliedMigrationVersions()).containsExactly(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34");
    assertThat(tableExists("privacy", "consent_record")).isTrue();
    assertThat(tableExists("privacy", "unlock_decision")).isTrue();
    assertThat(tableExists("privacy", "disclosure_record")).isTrue();
  }

  @Test
  void v28MigratesLegacyApprovedDisclosureRowsToConsultantApproved() throws SQLException {
    String databaseName = "task33_disclosure_v28";
    createDatabase(databaseName);

    Flyway.configure()
        .dataSource(databaseJdbcUrl(databaseName), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .target("27")
        .cleanDisabled(true)
        .load()
        .migrate();

    UUID organizationId = uuid("00000000-0000-0000-0000-00000012b901");
    UUID consultantId = uuid("00000000-0000-0000-0000-00000012b902");
    try (Connection connection = connection(databaseName)) {
      insertOrganizationAndUser(connection, organizationId, consultantId);
      insertConsentRecord(connection,
          organizationId,
          "consent_record_task12b_v28_0001",
          "candidate_ref_task12b_v28_0001",
          "profile_ref_task12b_v28_0001",
          "job_ref_task12b_v28_0001");
      insertUnlockDecision(connection,
          organizationId,
          consultantId,
          "unlock_decision_task12b_v28_0001",
          "candidate_ref_task12b_v28_0001",
          "profile_ref_task12b_v28_0001",
          "job_ref_task12b_v28_0001",
          "client_ref_task12b_v28_0001");
      insertLegacyApprovedDisclosureRecord(connection,
          organizationId,
          "disclosure_record_task12b_v28_0001",
          "candidate_ref_task12b_v28_0001",
          "profile_ref_task12b_v28_0001",
          "job_ref_task12b_v28_0001",
          "client_ref_task12b_v28_0001",
          "unlock_decision_task12b_v28_0001",
          "consent_record_task12b_v28_0001");
    }

    Flyway.configure()
        .dataSource(databaseJdbcUrl(databaseName), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    try (Connection connection = connection(databaseName)) {
      assertThat(findDisclosureStatus(
          connection,
          organizationId,
          "disclosure_record_task12b_v28_0001")).isEqualTo("consultant_approved");
    }
  }

  private static CanonicalWriteTransactionBoundary transactionBoundary() {
    return new SpringCanonicalWriteTransactionBoundary(new DataSourceTransactionManager(dataSource));
  }

  private static ConsentRecord consent(
      UUID organizationId,
      String consentRecordRef,
      String candidateRef,
      String profileRef,
      String jobRef) {
    return new ConsentRecord(
        consentRecordRef,
        organizationId,
        candidateRef,
        profileRef,
        jobRef,
        "profile-version-12b-pg",
        "consent-text-12b-pg",
        ConsentStatus.CONFIRMED,
        java.util.Set.of(DisclosureLevel.L3_CONSENTED_DETAIL, DisclosureLevel.L4_IDENTITY_DISCLOSED),
        NOW.minusSeconds(3_600),
        NOW.plusSeconds(3_600),
        false);
  }

  private static UnlockDecision unlockDecision(
      UUID organizationId,
      UUID consultantId,
      String unlockDecisionRef,
      String candidateRef,
      String profileRef,
      String jobRef,
      String clientRef) {
    return new UnlockDecision(
        unlockDecisionRef,
        organizationId,
        candidateRef,
        profileRef,
        jobRef,
        clientRef,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        UnlockDecisionStatus.APPROVED,
        DisclosureReviewStatus.HUMAN_APPROVED,
        RiskTier.T4_TRANSACTION_LEGAL_BLOCKING,
        new ActorRef(consultantId, ActorRole.CONSULTANT),
        NOW.minusSeconds(1_800));
  }

  private static DisclosureRecord approvedDisclosureRecord(
      UUID organizationId,
      String disclosureRecordRef,
      String candidateRef,
      String profileRef,
      String jobRef,
      String clientRef,
      String unlockDecisionRef,
      String consentRecordRef) {
    return new DisclosureRecord(
        disclosureRecordRef,
        organizationId,
        candidateRef,
        profileRef,
        jobRef,
        clientRef,
        DisclosureStatus.CONSULTANT_APPROVED,
        DisclosureLevel.L4_IDENTITY_DISCLOSED,
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        unlockDecisionRef,
        consentRecordRef,
        Optional.empty(),
        NOW.minusSeconds(900));
  }

  private static void insertOrganizationAndUser(UUID organizationId, UUID userId)
      throws SQLException {
    try (Connection connection = connection();
        ) {
      insertOrganizationAndUser(connection, organizationId, userId);
    }
  }

  private static void insertOrganizationAndUser(Connection connection, UUID organizationId, UUID userId)
      throws SQLException {
    try (PreparedStatement organization = connection.prepareStatement("""
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
            """)) {
      organization.setObject(1, organizationId);
      organization.setString(2, "Org " + organizationId);
      organization.setString(3, "Org " + organizationId);
      organization.executeUpdate();

      user.setObject(1, userId);
      user.setObject(2, organizationId);
      user.setString(3, "user-" + userId + "@example.com");
      user.setString(4, "User " + userId);
      user.executeUpdate();
    }
  }

  private static String findDisclosureStatus(UUID organizationId, String disclosureRecordRef)
      throws SQLException {
    try (Connection connection = connection();
        ) {
      return findDisclosureStatus(connection, organizationId, disclosureRecordRef);
    }
  }

  private static String findDisclosureStatus(
      Connection connection,
      UUID organizationId,
      String disclosureRecordRef) throws SQLException {
    try (
        PreparedStatement statement = connection.prepareStatement("""
            SELECT status
            FROM privacy.disclosure_record
            WHERE organization_id = ?
              AND disclosure_record_ref = ?
            """)) {
      statement.setObject(1, organizationId);
      statement.setString(2, disclosureRecordRef);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new AssertionError("disclosure record not found");
        }
        return resultSet.getString("status");
      }
    }
  }

  private static void insertConsentRecord(
      Connection connection,
      UUID organizationId,
      String consentRecordRef,
      String candidateRef,
      String profileRef,
      String jobRef) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO privacy.consent_record (
          consent_record_ref,
          organization_id,
          candidate_ref,
          candidate_profile_ref,
          job_ref,
          profile_version,
          consent_text_version,
          status,
          permitted_disclosure_levels,
          confirmed_at,
          expires_at,
          revoked
        )
        VALUES (?, ?, ?, ?, ?, '1', 'consent-v1', 'confirmed',
            ARRAY['l4_identity_disclosed']::text[], ?, ?, FALSE)
        """)) {
      statement.setString(1, consentRecordRef);
      statement.setObject(2, organizationId);
      statement.setString(3, candidateRef);
      statement.setString(4, profileRef);
      statement.setString(5, jobRef);
      statement.setObject(6, java.time.OffsetDateTime.ofInstant(NOW.minusSeconds(3_600), java.time.ZoneOffset.UTC));
      statement.setObject(7, java.time.OffsetDateTime.ofInstant(NOW.plusSeconds(3_600), java.time.ZoneOffset.UTC));
      statement.executeUpdate();
    }
  }

  private static void insertUnlockDecision(
      Connection connection,
      UUID organizationId,
      UUID consultantId,
      String unlockDecisionRef,
      String candidateRef,
      String profileRef,
      String jobRef,
      String clientRef) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO privacy.unlock_decision (
          unlock_decision_ref,
          organization_id,
          candidate_ref,
          candidate_profile_ref,
          job_ref,
          client_ref,
          requested_disclosure_level,
          status,
          review_status,
          risk_tier,
          approved_by_user_id,
          approved_by_role,
          decided_at
        )
        VALUES (?, ?, ?, ?, ?, ?, 'l4_identity_disclosed', 'approved', 'human_approved',
            'T4_TRANSACTION_LEGAL_BLOCKING'::governance.risk_tier, ?, 'consultant'::governance.actor_role, ?)
        """)) {
      statement.setString(1, unlockDecisionRef);
      statement.setObject(2, organizationId);
      statement.setString(3, candidateRef);
      statement.setString(4, profileRef);
      statement.setString(5, jobRef);
      statement.setString(6, clientRef);
      statement.setObject(7, consultantId);
      statement.setObject(8, java.time.OffsetDateTime.ofInstant(NOW.minusSeconds(1_800), java.time.ZoneOffset.UTC));
      statement.executeUpdate();
    }
  }

  private static void insertLegacyApprovedDisclosureRecord(
      Connection connection,
      UUID organizationId,
      String disclosureRecordRef,
      String candidateRef,
      String profileRef,
      String jobRef,
      String clientRef,
      String unlockDecisionRef,
      String consentRecordRef) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("""
        INSERT INTO privacy.disclosure_record (
          disclosure_record_ref,
          organization_id,
          candidate_ref,
          candidate_profile_ref,
          job_ref,
          client_ref,
          status,
          disclosure_level,
          redaction_level,
          unlock_decision_ref,
          consent_record_ref,
          workflow_event_id,
          decided_at
        )
        VALUES (?, ?, ?, ?, ?, ?, 'approved', 'l4_identity_disclosed', 'l4_identity_disclosed',
            ?, ?, NULL, ?)
        """)) {
      statement.setString(1, disclosureRecordRef);
      statement.setObject(2, organizationId);
      statement.setString(3, candidateRef);
      statement.setString(4, profileRef);
      statement.setString(5, jobRef);
      statement.setString(6, clientRef);
      statement.setString(7, unlockDecisionRef);
      statement.setString(8, consentRecordRef);
      statement.setObject(9, java.time.OffsetDateTime.ofInstant(NOW.minusSeconds(900), java.time.ZoneOffset.UTC));
      statement.executeUpdate();
    }
  }

  private static String findCandidateStatus(UUID organizationId, UUID candidateId)
      throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT status
            FROM recruiting.candidate
            WHERE organization_id = ?
              AND candidate_id = ?
            """)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, candidateId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          throw new AssertionError("candidate row not found");
        }
        return resultSet.getString("status");
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
        if (!resultSet.next()) {
          throw new AssertionError("workflow event not found");
        }
        return resultSet.getString("action");
      }
    }
  }

  private static int countRows(String tableName, UUID organizationId) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT count(*) FROM " + tableName + " WHERE organization_id = ?")) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private static void dropUnlockDecisionApproverOrgConstraint() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            ALTER TABLE privacy.unlock_decision
              DROP CONSTRAINT unlock_decision_approver_org_fk
            """)) {
      statement.executeUpdate();
    }
  }

  private static void insertUnlockDecisionWithCrossOrgApprover(
      UUID organizationId,
      UUID approverUserId,
      String unlockDecisionRef,
      String candidateRef,
      String profileRef,
      String jobRef,
      String clientRef) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            INSERT INTO privacy.unlock_decision (
              unlock_decision_ref,
              organization_id,
              candidate_ref,
              candidate_profile_ref,
              job_ref,
              client_ref,
              requested_disclosure_level,
              status,
              review_status,
              risk_tier,
              approved_by_user_id,
              approved_by_role,
              decided_at
            )
            VALUES (
              ?, ?, ?, ?, ?, ?, 'l4_identity_disclosed', 'approved', 'human_approved',
              'T4_TRANSACTION_LEGAL_BLOCKING'::governance.risk_tier,
              ?, 'consultant'::governance.actor_role, ?
            )
            """)) {
      statement.setString(1, unlockDecisionRef);
      statement.setObject(2, organizationId);
      statement.setString(3, candidateRef);
      statement.setString(4, profileRef);
      statement.setString(5, jobRef);
      statement.setString(6, clientRef);
      statement.setObject(7, approverUserId);
      statement.setObject(8, java.time.OffsetDateTime.ofInstant(
          NOW.minusSeconds(1_800),
          java.time.ZoneOffset.UTC));
      statement.executeUpdate();
    }
  }

  private static void restoreUnlockDecisionApproverOrgConstraint() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            ALTER TABLE privacy.unlock_decision
              ADD CONSTRAINT unlock_decision_approver_org_fk
              FOREIGN KEY (approved_by_user_id, organization_id)
              REFERENCES identity.user_account (user_account_id, organization_id)
              NOT VALID
            """)) {
      statement.executeUpdate();
    }
  }

  private static boolean tableExists(String schemaName, String tableName) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = ?
              AND table_name = ?
            """)) {
      statement.setString(1, schemaName);
      statement.setString(2, tableName);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  private static List<String> appliedMigrationVersions() throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement = connection.prepareStatement("""
            SELECT version
            FROM flyway_schema_history
            WHERE success = TRUE
            ORDER BY installed_rank
            """);
        ResultSet resultSet = statement.executeQuery()) {
      java.util.ArrayList<String> versions = new java.util.ArrayList<>();
      while (resultSet.next()) {
        versions.add(resultSet.getString("version"));
      }
      return versions;
    }
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
  }

  private static Connection connection(String databaseName) throws SQLException {
    return DriverManager.getConnection(
        databaseJdbcUrl(databaseName),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
  }

  private static void createDatabase(String databaseName) throws SQLException {
    try (Connection connection = DriverManager.getConnection(
        databaseJdbcUrl("postgres"),
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
        PreparedStatement statement = connection.prepareStatement("CREATE DATABASE " + databaseName)) {
      statement.executeUpdate();
    }
  }

  private static String databaseJdbcUrl(String databaseName) {
    String jdbcUrl = POSTGRES.getJdbcUrl();
    int queryIndex = jdbcUrl.indexOf('?');
    String base = queryIndex >= 0 ? jdbcUrl.substring(0, queryIndex) : jdbcUrl;
    String query = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";
    int lastSlash = base.lastIndexOf('/');
    return base.substring(0, lastSlash + 1) + databaseName + query;
  }

  private static UUID uuid(String value) {
    return UUID.fromString(value);
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
      public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
      }

      @Override
      public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
          return iface.cast(this);
        }
        throw new SQLException("DataSource does not wrap " + iface.getName());
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return iface.isAssignableFrom(getClass());
      }
    };
  }
}
