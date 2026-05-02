package com.recruitingtransactionos.coreapi.candidateprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CandidateProfileContractTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000270001");
  private static final CandidateProfileId PROFILE_ID = new CandidateProfileId(
      UUID.fromString("00000000-0000-0000-0000-000000270002"));
  private static final CandidateId CANDIDATE_ID = new CandidateId(
      UUID.fromString("00000000-0000-0000-0000-000000270003"));
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000270004");
  private static final Instant NOW = Instant.parse("2026-04-28T20:00:00Z");

  @Test
  void candidateProfileRequiresOrganizationId() {
    assertThatThrownBy(() -> profileBuilder().organizationId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void candidateProfileRequiresCandidateId() {
    assertThatThrownBy(() -> profileBuilder().candidateId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("candidateId must not be null");
  }

  @Test
  void candidateProfileRequiresProfileVersion() {
    assertThatThrownBy(() -> profileBuilder().profileVersion(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("profileVersion must not be null");
  }

  @Test
  void candidateProfileRejectsDuplicateFieldPaths() {
    CandidateProfileField fullName = fieldBuilder()
        .fieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME)
        .build();
    CandidateProfileField replacementName = fieldBuilder()
        .fieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME)
        .value(CandidateProfileFieldValue.ofString("Jane Q. Candidate"))
        .build();

    assertThatThrownBy(() -> profileBuilder().fields(List.of(fullName, replacementName)).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fields must not contain duplicate fieldPath values");
  }

  @Test
  void candidateProfileFieldRequiresFieldPath() {
    assertThatThrownBy(() -> fieldBuilder().fieldPath(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("fieldPath must not be null");
  }

  @Test
  void candidateProfileFieldRequiresFieldStatus() {
    assertThatThrownBy(() -> fieldBuilder().fieldStatus(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("fieldStatus must not be null");
  }

  @Test
  void candidateProfileFieldRequiresNonNullValueAndLineage() {
    assertThatThrownBy(() -> fieldBuilder().value(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value must not be null");
    assertThatThrownBy(() -> fieldBuilder().lineage(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("lineage must not be null");
  }

  @Test
  void candidateProfileFieldPathRejectsBlankAndInvalidPaths() {
    assertThatThrownBy(() -> CandidateProfileFieldPath.of(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fieldPath must not be blank");
    assertThatThrownBy(() -> CandidateProfileFieldPath.of("Full Name"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fieldPath must be a stable dotted lower_snake_case path");
    assertThatThrownBy(() -> CandidateProfileFieldPath.of("identity..full_name"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fieldPath must be a stable dotted lower_snake_case path");
    assertThatThrownBy(() -> CandidateProfileFieldPath.of("identity.full-name"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fieldPath must be a stable dotted lower_snake_case path");
    assertThatThrownBy(() -> CandidateProfileFieldPath.of("full_name"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fieldPath must include at least one namespace segment");
  }

  @Test
  void candidateProfileFieldPathVocabularyContainsInitialStablePaths() {
    assertThat(CandidateProfileFieldPath.initialPathValues())
        .containsExactlyInAnyOrder(
            "identity.full_name",
            "identity.preferred_name",
            "identity.citizenship",
            "contact.email",
            "contact.phone",
            "location.current_location",
            "location.preferred_locations",
            "compensation.current_salary",
            "compensation.expected_salary",
            "availability.notice_period",
            "availability.available_from",
            "experience.current_company",
            "experience.current_title",
            "profile.headline",
            "profile.summary",
            "experience.years_of_experience",
            "experience.work_history",
            "experience.timeline_highlights",
            "experience.projects",
            "experience.portfolio",
            "experience.industry",
            "skills.primary_skills",
            "skills.secondary_skills",
            "education.highest_degree",
            "education.schools",
            "intent.open_to_opportunities",
            "intent.interest_level",
            "intent.motivation_toward_opportunity",
            "consent.latest_profile_version",
            "metadata.notes");
  }

  @Test
  void candidateProfileFieldStatusVocabularyContainsRequiredStatuses() {
    assertThat(Stream.of(CandidateProfileFieldStatus.values())
        .map(CandidateProfileFieldStatus::wireValue))
        .contains(
            "ai_extracted",
            "human_acknowledged",
            "consultant_attested",
            "candidate_confirmed",
            "external_verified",
            "system_inference",
            "conflicting",
            "needs_confirmation",
            "stale",
            "unverified",
            "likely_current");
  }

  @Test
  void fieldStatusPolicyKeepsBulkApprovalBelowVerifiedSemantics() {
    assertThat(CandidateProfileFieldStatusPolicy.bulkApprovalResult())
        .isEqualTo(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED)
        .isNotIn(
            CandidateProfileFieldStatus.CANDIDATE_CONFIRMED,
            CandidateProfileFieldStatus.EXTERNAL_VERIFIED);
  }

  @Test
  void fieldStatusPolicySeparatesHintsAcknowledgementAttestationAndVerification() {
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(
        CandidateProfileFieldStatus.SYSTEM_INFERENCE)).isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(
        CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED)).isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(
        CandidateProfileFieldStatus.CONSULTANT_ATTESTED)).isFalse();

    assertThat(CandidateProfileFieldStatus.CONSULTANT_ATTESTED)
        .isNotEqualTo(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED);
    assertThat(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED)
        .isNotEqualTo(CandidateProfileFieldStatus.EXTERNAL_VERIFIED);

    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(
        CandidateProfileFieldStatus.CANDIDATE_CONFIRMED)).isTrue();
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(
        CandidateProfileFieldStatus.EXTERNAL_VERIFIED)).isTrue();
  }

  @Test
  void fieldStatusPolicyRegressionKeepsNonFactStatesOutOfVerifiedClientAndTransactionUse() {
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(
        CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED)).isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(
        CandidateProfileFieldStatus.SYSTEM_INFERENCE)).isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.isClientFactEligible(
        CandidateProfileFieldStatus.CONFLICTING)).isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.blocksClientVisibleFactStatement(
        CandidateProfileFieldStatus.CONFLICTING)).isTrue();
    assertThat(CandidateProfileFieldStatusPolicy.isTransactionReadyEligible(
        CandidateProfileFieldStatus.NEEDS_CONFIRMATION)).isFalse();

    assertThat(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED)
        .isNotEqualTo(CandidateProfileFieldStatus.EXTERNAL_VERIFIED);
    assertThat(CandidateProfileFieldStatus.CONSULTANT_ATTESTED)
        .isNotEqualTo(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED);
    assertThat(CandidateProfileFieldStatusPolicy.bulkApprovalResult())
        .isEqualTo(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED)
        .isNotIn(
            CandidateProfileFieldStatus.CANDIDATE_CONFIRMED,
            CandidateProfileFieldStatus.EXTERNAL_VERIFIED);
  }

  @Test
  void conflictingAndNeedsConfirmationStatusesBlockLaterFactAndTransactionReadiness() {
    assertThat(CandidateProfileFieldStatusPolicy.isClientFactEligible(
        CandidateProfileFieldStatus.CONFLICTING)).isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.blocksClientVisibleFactStatement(
        CandidateProfileFieldStatus.CONFLICTING)).isTrue();
    assertThat(CandidateProfileFieldStatusPolicy.isTransactionReadyEligible(
        CandidateProfileFieldStatus.NEEDS_CONFIRMATION)).isFalse();
  }

  @Test
  void candidateConfirmedRequiresCandidateActorAndProfileVersionSemantics() {
    assertThatThrownBy(() -> fieldBuilder()
        .fieldStatus(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED)
        .confirmedByActorId(null)
        .confirmedAgainstProfileVersion(null)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("candidate_confirmed field requires confirmedByActorId and confirmedAgainstProfileVersion");
  }

  @Test
  void externalVerifiedRequiresExternalEvidenceLineage() {
    assertThatThrownBy(() -> fieldBuilder()
        .fieldStatus(CandidateProfileFieldStatus.EXTERNAL_VERIFIED)
        .lineage(lineage(
            CandidateProfileFieldSourceReference.claimLedgerItem(
                new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000270101")),
                NOW)))
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("external_verified field requires EXTERNAL_EVIDENCE lineage reference");
  }

  @Test
  void lineageCanReferenceUpstreamEvidenceWithoutQueryingIt() {
    CandidateProfileFieldLineage lineage = lineage(
        CandidateProfileFieldSourceReference.claimLedgerItem(
            new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000270111")),
            NOW),
        CandidateProfileFieldSourceReference.reviewEvent(
            new ReviewEventId(UUID.fromString("00000000-0000-0000-0000-000000270112")),
            NOW),
        CandidateProfileFieldSourceReference.sourceItem(
            new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000270113")),
            NOW),
        CandidateProfileFieldSourceReference.informationPacket(
            new InformationPacketId(UUID.fromString("00000000-0000-0000-0000-000000270114")),
            NOW),
        CandidateProfileFieldSourceReference.intakeExtractionRun(
            new IntakeExtractionRunId(UUID.fromString("00000000-0000-0000-0000-000000270115")),
            NOW),
        CandidateProfileFieldSourceReference.workflowEvent(
            new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000270116")),
            NOW),
        CandidateProfileFieldSourceReference.sourceSpan("span:cv:skills:1", "resume_span", NOW));

    assertThat(lineage.sourceReferences())
        .extracting(CandidateProfileFieldSourceReference::sourceType)
        .containsExactly(
            CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM,
            CandidateProfileFieldSourceType.REVIEW_EVENT,
            CandidateProfileFieldSourceType.SOURCE_ITEM,
            CandidateProfileFieldSourceType.INFORMATION_PACKET,
            CandidateProfileFieldSourceType.INTAKE_EXTRACTION_RUN,
            CandidateProfileFieldSourceType.WORKFLOW_EVENT,
            CandidateProfileFieldSourceType.SOURCE_SPAN);
    assertThat(lineage.hasAnyReference()).isTrue();
    assertThat(lineage.hasReferenceType(CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM))
        .isTrue();
    assertThat(lineage.hasReferenceType(CandidateProfileFieldSourceType.EXTERNAL_EVIDENCE))
        .isFalse();
  }

  @Test
  void lineageRejectsBlankRefsIfPresent() {
    assertThatThrownBy(() -> CandidateProfileFieldSourceReference.sourceSpan(
        " ",
        "resume_span",
        NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sourceId must not be blank");

    assertThatThrownBy(() -> CandidateProfileFieldSourceReference.externalEvidence(
        "external-background-check:270001",
        " ",
        NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sourceTrust must not be blank");
  }

  @Test
  void lineageCanRepresentClaimReviewWorkflowSourcePacketExtractionSpanAndExternalEvidenceRefs() {
    CandidateProfileFieldLineage lineage = lineage(
        CandidateProfileFieldSourceReference.claimLedgerItem(
            new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000270121")),
            NOW),
        CandidateProfileFieldSourceReference.reviewEvent(
            new ReviewEventId(UUID.fromString("00000000-0000-0000-0000-000000270122")),
            NOW),
        CandidateProfileFieldSourceReference.workflowEvent(
            new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000270123")),
            NOW),
        CandidateProfileFieldSourceReference.sourceItem(
            new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000270124")),
            NOW),
        CandidateProfileFieldSourceReference.informationPacket(
            new InformationPacketId(UUID.fromString("00000000-0000-0000-0000-000000270125")),
            NOW),
        CandidateProfileFieldSourceReference.intakeExtractionRun(
            new IntakeExtractionRunId(UUID.fromString("00000000-0000-0000-0000-000000270126")),
            NOW),
        CandidateProfileFieldSourceReference.sourceSpan(
            "span:call-note:intent:open-to-opportunities",
            "consultant_call",
            NOW),
        CandidateProfileFieldSourceReference.externalEvidence(
            "external-reference:certificate:270127",
            "external_evidence",
            NOW));

    assertThat(lineage.sourceReferences())
        .extracting(CandidateProfileFieldSourceReference::sourceType)
        .containsExactly(
            CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM,
            CandidateProfileFieldSourceType.REVIEW_EVENT,
            CandidateProfileFieldSourceType.WORKFLOW_EVENT,
            CandidateProfileFieldSourceType.SOURCE_ITEM,
            CandidateProfileFieldSourceType.INFORMATION_PACKET,
            CandidateProfileFieldSourceType.INTAKE_EXTRACTION_RUN,
            CandidateProfileFieldSourceType.SOURCE_SPAN,
            CandidateProfileFieldSourceType.EXTERNAL_EVIDENCE);
    assertThat(lineage.provenanceLabel()).isEqualTo("contract-test-lineage");
    assertThat(lineage.createdAt()).isEqualTo(NOW);
  }

  @Test
  void conflictMetadataRepresentsMultipleSourceBackedValues() {
    CandidateProfileFieldConflict conflict = new CandidateProfileFieldConflict(
        CandidateProfileFieldPath.COMPENSATION_EXPECTED_SALARY,
        List.of(
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("45000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:cv:compensation:1", "resume", NOW))),
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("55000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:call:compensation:2", "consultant_call", NOW)))),
        CandidateProfileFieldConflictSeverity.HIGH,
        CandidateProfileFieldConflictResolutionStatus.UNRESOLVED,
        NOW,
        "salary expectation conflicts across CV and call note");

    assertThat(conflict.conflictingValues()).hasSize(2);
    assertThat(conflict.severity()).isEqualTo(CandidateProfileFieldConflictSeverity.HIGH);
    assertThat(conflict.resolutionStatus())
        .isEqualTo(CandidateProfileFieldConflictResolutionStatus.UNRESOLVED);
    assertThat(conflict.hasMultipleSourceBackedValues()).isTrue();
  }

  @Test
  void conflictingStatusRequiresSourceBackedConflictMetadataForSameField() {
    assertThatThrownBy(() -> fieldBuilder()
        .fieldStatus(CandidateProfileFieldStatus.CONFLICTING)
        .conflict(null)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("conflicting field requires source-backed conflict metadata");

    CandidateProfileFieldConflict otherFieldConflict = conflict(
        CandidateProfileFieldPath.COMPENSATION_EXPECTED_SALARY);
    assertThatThrownBy(() -> fieldBuilder()
        .fieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME)
        .fieldStatus(CandidateProfileFieldStatus.CONFLICTING)
        .conflict(otherFieldConflict)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("conflict fieldPath must match fieldPath");
  }

  @Test
  void conflictMetadataPreservesSeverityAndResolutionStatusVocabulary() {
    assertThat(CandidateProfileFieldConflictSeverity.HIGH.wireValue()).isEqualTo("high");
    assertThat(CandidateProfileFieldConflictSeverity.BLOCKING.wireValue()).isEqualTo("blocking");
    assertThat(CandidateProfileFieldConflictResolutionStatus.UNRESOLVED.wireValue())
        .isEqualTo("unresolved");
    assertThat(CandidateProfileFieldConflictResolutionStatus.NEEDS_REVIEW.wireValue())
        .isEqualTo("needs_review");
  }

  @Test
  void stalenessMetadataRepresentsReasonAndReviewTiming() {
    CandidateProfileFieldStaleness staleness = new CandidateProfileFieldStaleness(
        true,
        "salary data is older than current search cycle",
        Instant.parse("2025-12-01T00:00:00Z"),
        Instant.parse("2025-12-15T00:00:00Z"),
        Instant.parse("2026-05-05T00:00:00Z"),
        NOW);

    assertThat(staleness.stale()).isTrue();
    assertThat(staleness.staleReason()).contains("older than current search cycle");
    assertThat(staleness.reviewBy()).isEqualTo(Instant.parse("2026-05-05T00:00:00Z"));
    assertThat(staleness.detectedAt()).isEqualTo(NOW);
  }

  @Test
  void staleMetadataRejectsReasonlessStaleFlagAndImpossibleTimeRanges() {
    assertThatThrownBy(() -> new CandidateProfileFieldStaleness(
        true,
        " ",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-02-01T00:00:00Z"),
        NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("staleReason must not be blank");

    assertThatThrownBy(() -> new CandidateProfileFieldStaleness(
        true,
        "confirmation timestamp precedes the observed source timestamp",
        Instant.parse("2026-02-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-03-01T00:00:00Z"),
        NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("lastConfirmedAt must not be before observedAt");

    assertThatThrownBy(() -> new CandidateProfileFieldStaleness(
        true,
        "review deadline is already before stale detection",
        Instant.parse("2025-12-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-04-01T00:00:00Z"),
        NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("reviewBy must not be before detectedAt");
  }

  @Test
  void nonStaleStatusCanCarryStaleMetadataAsHistoricalAuditNote() {
    CandidateProfileField field = fieldBuilder()
        .fieldStatus(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED)
        .staleness(new CandidateProfileFieldStaleness(
            true,
            "salary source was stale before the consultant acknowledged the field",
            Instant.parse("2025-12-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-05-05T00:00:00Z"),
            NOW))
        .build();

    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED);
    assertThat(field.staleness().stale()).isTrue();
  }

  @Test
  void candidateProfilePackageKeepsPersistenceBackendInternalAndNoApiControllerOrUi()
      throws IOException {
    List<Path> candidateProfileFiles = candidateProfileProductionFiles();

    assertThat(candidateProfileFiles)
        .extracting(path -> path.getFileName().toString())
        .contains("JdbcCandidateProfilePersistencePort.java")
        .noneMatch(fileName -> fileName.endsWith("Repository.java"))
        .noneMatch(fileName -> fileName.endsWith("Controller.java"))
        .noneMatch(fileName -> fileName.contains("ClientSafeCandidateCard"));

    for (Path file : candidateProfileFiles) {
      String source = Files.readString(file);
      assertThat(source)
          .doesNotContain("CanonicalWriteService")
          .doesNotContain("CanonicalWriteGate")
          .doesNotContain("StaleDetection")
          .doesNotContain("StalenessDetector")
          .doesNotContain("ConflictResolutionWorkflow")
          .doesNotContain("@RestController")
          .doesNotContain("@Controller")
          .doesNotContain("@RequestMapping")
          .doesNotContain("org.springframework.web")
          .doesNotContain("ClientSafeCandidateCard");
    }
  }

  @Test
  void productionCodeOnlyWritesCandidateProfileInsideCandidateProfileAdapter()
      throws IOException {
    Pattern writePattern = Pattern.compile(
        "(INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)\\s+recruiting\\."
            + "(candidate|candidate_profile)\\b",
        Pattern.CASE_INSENSITIVE);
    List<String> matches = new java.util.ArrayList<>();
    for (Path file : productionJavaFiles()) {
      Matcher matcher = writePattern.matcher(Files.readString(file));
      while (matcher.find()) {
        String relativePath = projectRelative(file);
        if (relativePath.endsWith(
            "candidateprofile/persistence/JdbcCandidateProfilePersistencePort.java")
            && matcher.group(2).equals("candidate_profile")) {
          continue;
        }
        if (relativePath.endsWith(
            "candidate/persistence/JdbcCandidatePersistencePort.java")
            && matcher.group(2).equals("candidate")) {
          continue;
        }
        if (relativePath.endsWith(
            "consentdisclosure/persistence/JdbcCandidateWorkflowStatePort.java")
            && matcher.group(2).equals("candidate")) {
          continue;
        }
        matches.add(relativePath + " contains " + matcher.group());
      }
    }

    assertThat(matches).isEmpty();
  }

  private static CandidateProfile.Builder profileBuilder() {
    return CandidateProfile.builder()
        .candidateProfileId(PROFILE_ID)
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .profileVersion(new CandidateProfileVersion(1))
        .fields(List.of(fieldBuilder().build()))
        .createdAt(NOW)
        .updatedAt(NOW);
  }

  private static CandidateProfileField.Builder fieldBuilder() {
    return CandidateProfileField.builder()
        .fieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME)
        .value(CandidateProfileFieldValue.ofString("Jane Candidate"))
        .fieldStatus(CandidateProfileFieldStatus.CONSULTANT_ATTESTED)
        .lineage(lineage(CandidateProfileFieldSourceReference.sourceSpan(
            "span:call-note:full-name",
            "consultant_call",
            NOW)))
        .lastReviewedAt(NOW)
        .confirmedByActorId(ACTOR_ID)
        .sourceClaimId(new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000270201")))
        .sourceReviewEventId(
            new ReviewEventId(UUID.fromString("00000000-0000-0000-0000-000000270202")))
        .sourceWorkflowEventId(
            new WorkflowEventId(UUID.fromString("00000000-0000-0000-0000-000000270203")))
        .notes("consultant attested field for contract tests");
  }

  private static CandidateProfileFieldLineage lineage(
      CandidateProfileFieldSourceReference... references) {
    return new CandidateProfileFieldLineage(List.of(references), "contract-test-lineage", NOW);
  }

  private static CandidateProfileFieldConflict conflict(CandidateProfileFieldPath fieldPath) {
    return new CandidateProfileFieldConflict(
        fieldPath,
        List.of(
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("45000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:contract-test:conflict-a",
                    "resume",
                    NOW))),
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("55000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:contract-test:conflict-b",
                    "consultant_call",
                    NOW)))),
        CandidateProfileFieldConflictSeverity.HIGH,
        CandidateProfileFieldConflictResolutionStatus.UNRESOLVED,
        NOW,
        "conflict metadata for contract tests");
  }

  private static List<Path> candidateProfileProductionFiles() throws IOException {
    Path root = projectPath("src/main/java/com/recruitingtransactionos/coreapi/candidateprofile");
    if (!Files.exists(root)) {
      return List.of();
    }
    try (Stream<Path> stream = Files.walk(root)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  private static List<Path> productionJavaFiles() throws IOException {
    try (Stream<Path> stream = Files.walk(projectPath("src/main/java"))) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  private static Path projectPath(String relativePath) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    Path direct = userDir.resolve(relativePath);
    if (Files.exists(direct)) {
      return direct;
    }
    return userDir.resolve("services/core-api").resolve(relativePath);
  }

  private static String projectRelative(Path file) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    if (file.startsWith(userDir)) {
      return userDir.relativize(file).toString();
    }
    Path coreApi = userDir.resolve("services/core-api");
    if (file.startsWith(coreApi)) {
      return coreApi.relativize(file).toString();
    }
    return file.toString();
  }
}
