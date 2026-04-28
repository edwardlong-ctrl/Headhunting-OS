package com.recruitingtransactionos.coreapi.candidateprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CreateCandidateProfileRequest;
import com.recruitingtransactionos.coreapi.candidateprofile.service.UpsertCandidateProfileFieldRequest;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacketId;
import com.recruitingtransactionos.coreapi.governedintake.IntakeExtractionRunId;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CandidateProfileServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000280001");
  private static final CandidateProfileId PROFILE_ID = new CandidateProfileId(
      UUID.fromString("00000000-0000-0000-0000-000000280002"));
  private static final CandidateId CANDIDATE_ID = new CandidateId(
      UUID.fromString("00000000-0000-0000-0000-000000280003"));
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000280004");
  private static final Instant NOW = Instant.parse("2026-04-28T21:00:00Z");

  @Test
  void createCandidateProfileRequiresOrganizationId() {
    assertThatThrownBy(() -> new CreateCandidateProfileRequest(
        null,
        CANDIDATE_ID,
        new CandidateProfileVersion(1),
        List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void createCandidateProfileRequiresCandidateId() {
    assertThatThrownBy(() -> new CreateCandidateProfileRequest(
        ORGANIZATION_ID,
        null,
        new CandidateProfileVersion(1),
        List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("candidateId must not be null");
  }

  @Test
  void createCandidateProfileRequiresProfileVersion() {
    assertThatThrownBy(() -> new CreateCandidateProfileRequest(
        ORGANIZATION_ID,
        CANDIDATE_ID,
        null,
        List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("profileVersion must not be null");
  }

  @Test
  void createCandidateProfileDelegatesBackendInternalProfileSkeleton() {
    RecordingCandidateProfilePort port = new RecordingCandidateProfilePort();
    CandidateProfileService service = new CandidateProfileService(port);

    CandidateProfile profile = service.createCandidateProfile(new CreateCandidateProfileRequest(
        ORGANIZATION_ID,
        CANDIDATE_ID,
        new CandidateProfileVersion(1),
        List.of()));

    assertThat(profile.organizationId()).isEqualTo(ORGANIZATION_ID);
    assertThat(profile.candidateId()).isEqualTo(CANDIDATE_ID);
    assertThat(profile.profileVersion()).isEqualTo(new CandidateProfileVersion(1));
    assertThat(profile.fields()).isEmpty();
    assertThat(port.createdProfiles()).containsExactly(profile);
  }

  @Test
  void upsertFieldRequiresOrganizationId() {
    assertThatThrownBy(() -> fieldRequestBuilder().organizationId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("organizationId must not be null");
  }

  @Test
  void upsertFieldRequiresCandidateProfileId() {
    assertThatThrownBy(() -> fieldRequestBuilder().candidateProfileId(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("candidateProfileId must not be null");
  }

  @Test
  void upsertFieldRequiresFieldPath() {
    assertThatThrownBy(() -> fieldRequestBuilder().fieldPath(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("fieldPath must not be null");
  }

  @Test
  void upsertFieldRequiresFieldStatus() {
    assertThatThrownBy(() -> fieldRequestBuilder().fieldStatus(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("fieldStatus must not be null");
  }

  @Test
  void upsertFieldRequiresValue() {
    assertThatThrownBy(() -> fieldRequestBuilder().value(null).build())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("value must not be null");
  }

  @Test
  void humanAcknowledgedIsPersistedButNotVerified() {
    RecordingCandidateProfilePort port = new RecordingCandidateProfilePort();
    CandidateProfileService service = new CandidateProfileService(port);

    CandidateProfileField field = service.upsertCandidateProfileField(fieldRequestBuilder()
        .fieldStatus(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED)
        .bulkApproval(true)
        .build());

    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED);
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(field.fieldStatus()))
        .isFalse();
    assertThat(port.upsertedFields()).containsExactly(field);
  }

  @Test
  void systemInferenceIsAllowedOnlyAsNonFactInternalStatus() {
    CandidateProfileService service = new CandidateProfileService(
        new RecordingCandidateProfilePort());

    CandidateProfileField field = service.upsertCandidateProfileField(fieldRequestBuilder()
        .fieldStatus(CandidateProfileFieldStatus.SYSTEM_INFERENCE)
        .build());

    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.SYSTEM_INFERENCE);
    assertThat(CandidateProfileFieldStatusPolicy.isVerifiedFactEligible(field.fieldStatus()))
        .isFalse();
    assertThat(CandidateProfileFieldStatusPolicy.blocksClientVisibleFactStatement(
        field.fieldStatus())).isTrue();
  }

  @Test
  void candidateConfirmedRequiresCandidateActorAndProfileVersion() {
    CandidateProfileService service = new CandidateProfileService(
        new RecordingCandidateProfilePort());

    assertThatThrownBy(() -> service.upsertCandidateProfileField(fieldRequestBuilder()
        .fieldStatus(CandidateProfileFieldStatus.CANDIDATE_CONFIRMED)
        .confirmedByActorId(null)
        .confirmedAgainstProfileVersion(null)
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("candidate_confirmed field requires confirmedByActorId and confirmedAgainstProfileVersion");
  }

  @Test
  void externalVerifiedRequiresExternalEvidenceLineage() {
    CandidateProfileService service = new CandidateProfileService(
        new RecordingCandidateProfilePort());

    assertThatThrownBy(() -> service.upsertCandidateProfileField(fieldRequestBuilder()
        .fieldStatus(CandidateProfileFieldStatus.EXTERNAL_VERIFIED)
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("external_verified field requires EXTERNAL_EVIDENCE lineage reference");
  }

  @Test
  void bulkApprovalCannotPersistCandidateConfirmedOrExternalVerified() {
    for (CandidateProfileFieldStatus verifiedStatus : List.of(
        CandidateProfileFieldStatus.CANDIDATE_CONFIRMED,
        CandidateProfileFieldStatus.EXTERNAL_VERIFIED)) {
      assertThatThrownBy(() -> fieldRequestBuilder()
          .fieldStatus(verifiedStatus)
          .lineage(externalLineage())
          .confirmedByActorId(ACTOR_ID)
          .confirmedAgainstProfileVersion(new CandidateProfileVersion(1))
          .bulkApproval(true)
          .build())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("bulk approval cannot persist candidate_confirmed or external_verified");
    }
  }

  @Test
  void upsertAcceptsFullLineageStaleAndConflictMetadataWhenValid() {
    RecordingCandidateProfilePort port = new RecordingCandidateProfilePort();
    CandidateProfileService service = new CandidateProfileService(port);
    ClaimId claimId = new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000280111"));
    ReviewEventId reviewEventId = new ReviewEventId(
        UUID.fromString("00000000-0000-0000-0000-000000280112"));
    WorkflowEventId workflowEventId = new WorkflowEventId(
        UUID.fromString("00000000-0000-0000-0000-000000280113"));

    CandidateProfileField field = service.upsertCandidateProfileField(fieldRequestBuilder()
        .fieldPath(CandidateProfileFieldPath.COMPENSATION_EXPECTED_SALARY)
        .value(CandidateProfileFieldValue.ofString("55000 RMB monthly"))
        .fieldStatus(CandidateProfileFieldStatus.CONFLICTING)
        .lineage(fullLineage(claimId, reviewEventId, workflowEventId))
        .conflict(conflict(CandidateProfileFieldPath.COMPENSATION_EXPECTED_SALARY))
        .staleness(staleness())
        .sourceClaimId(claimId)
        .sourceReviewEventId(reviewEventId)
        .sourceWorkflowEventId(workflowEventId)
        .build());

    assertThat(field.fieldStatus()).isEqualTo(CandidateProfileFieldStatus.CONFLICTING);
    assertThat(field.lineage().sourceReferences())
        .extracting(CandidateProfileFieldSourceReference::sourceType)
        .contains(
            CandidateProfileFieldSourceType.CLAIM_LEDGER_ITEM,
            CandidateProfileFieldSourceType.REVIEW_EVENT,
            CandidateProfileFieldSourceType.WORKFLOW_EVENT,
            CandidateProfileFieldSourceType.SOURCE_ITEM,
            CandidateProfileFieldSourceType.INFORMATION_PACKET,
            CandidateProfileFieldSourceType.INTAKE_EXTRACTION_RUN,
            CandidateProfileFieldSourceType.SOURCE_SPAN,
            CandidateProfileFieldSourceType.EXTERNAL_EVIDENCE);
    assertThat(field.conflict().severity()).isEqualTo(CandidateProfileFieldConflictSeverity.HIGH);
    assertThat(field.conflict().resolutionStatus())
        .isEqualTo(CandidateProfileFieldConflictResolutionStatus.UNRESOLVED);
    assertThat(field.staleness().stale()).isTrue();
    assertThat(port.upsertedFields()).containsExactly(field);
  }

  @Test
  void upsertRejectsInvalidMetadataBeforePersistencePortIsCalled() {
    RecordingCandidateProfilePort port = new RecordingCandidateProfilePort();
    CandidateProfileService service = new CandidateProfileService(port);

    assertThatThrownBy(() -> service.upsertCandidateProfileField(fieldRequestBuilder()
        .fieldStatus(CandidateProfileFieldStatus.CONFLICTING)
        .conflict(null)
        .build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("conflicting field requires source-backed conflict metadata");
    assertThat(port.upsertedFields()).isEmpty();
  }

  private static UpsertCandidateProfileFieldRequest.Builder fieldRequestBuilder() {
    return UpsertCandidateProfileFieldRequest.builder()
        .organizationId(ORGANIZATION_ID)
        .candidateProfileId(PROFILE_ID)
        .fieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME)
        .value(CandidateProfileFieldValue.ofString("Jane Candidate"))
        .fieldStatus(CandidateProfileFieldStatus.CONSULTANT_ATTESTED)
        .lineage(sourceSpanLineage())
        .lastReviewedAt(NOW)
        .confirmedByActorId(ACTOR_ID)
        .sourceClaimId(new ClaimId(UUID.fromString("00000000-0000-0000-0000-000000280101")))
        .notes("candidate profile service test");
  }

  private static CandidateProfileFieldLineage sourceSpanLineage() {
    return new CandidateProfileFieldLineage(
        List.of(CandidateProfileFieldSourceReference.sourceSpan(
            "span:service-test:identity.full_name",
            "service_test",
            NOW)),
        "service-test-lineage",
        NOW);
  }

  private static CandidateProfileFieldLineage externalLineage() {
    return new CandidateProfileFieldLineage(
        List.of(CandidateProfileFieldSourceReference.externalEvidence(
            "external-background-check:280001",
            "external_evidence",
            NOW)),
        "external-test-lineage",
        NOW);
  }

  private static CandidateProfileFieldLineage fullLineage(
      ClaimId claimId,
      ReviewEventId reviewEventId,
      WorkflowEventId workflowEventId) {
    return new CandidateProfileFieldLineage(
        List.of(
            CandidateProfileFieldSourceReference.claimLedgerItem(claimId, NOW),
            CandidateProfileFieldSourceReference.reviewEvent(reviewEventId, NOW),
            CandidateProfileFieldSourceReference.workflowEvent(workflowEventId, NOW),
            CandidateProfileFieldSourceReference.sourceItem(
                new SourceItemId(UUID.fromString("00000000-0000-0000-0000-000000280114")),
                NOW),
            CandidateProfileFieldSourceReference.informationPacket(
                new InformationPacketId(UUID.fromString("00000000-0000-0000-0000-000000280115")),
                NOW),
            CandidateProfileFieldSourceReference.intakeExtractionRun(
                new IntakeExtractionRunId(UUID.fromString("00000000-0000-0000-0000-000000280116")),
                NOW),
            CandidateProfileFieldSourceReference.sourceSpan(
                "span:service-test:compensation.expected_salary",
                "consultant_call",
                NOW),
            CandidateProfileFieldSourceReference.externalEvidence(
                "external-reference:market-comp:280117",
                "external_evidence",
                NOW)),
        "service-test-full-lineage",
        NOW);
  }

  private static CandidateProfileFieldConflict conflict(CandidateProfileFieldPath fieldPath) {
    return new CandidateProfileFieldConflict(
        fieldPath,
        List.of(
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("45000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:service-test:conflict-a",
                    "resume",
                    NOW))),
            new CandidateProfileFieldConflictValue(
                CandidateProfileFieldValue.ofString("55000 RMB monthly"),
                List.of(CandidateProfileFieldSourceReference.sourceSpan(
                    "span:service-test:conflict-b",
                    "consultant_call",
                    NOW)))),
        CandidateProfileFieldConflictSeverity.HIGH,
        CandidateProfileFieldConflictResolutionStatus.UNRESOLVED,
        NOW,
        "service test conflict metadata");
  }

  private static CandidateProfileFieldStaleness staleness() {
    return new CandidateProfileFieldStaleness(
        true,
        "compensation data must be reconfirmed before client-visible use",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-02-01T00:00:00Z"),
        Instant.parse("2026-05-05T00:00:00Z"),
        NOW);
  }

  private static final class RecordingCandidateProfilePort
      implements CandidateProfilePersistencePort {
    private final List<CandidateProfile> createdProfiles = new ArrayList<>();
    private final List<CandidateProfileField> upsertedFields = new ArrayList<>();

    List<CandidateProfile> createdProfiles() {
      return List.copyOf(createdProfiles);
    }

    List<CandidateProfileField> upsertedFields() {
      return List.copyOf(upsertedFields);
    }

    @Override
    public CandidateProfile createCandidateProfile(CandidateProfile candidateProfile) {
      createdProfiles.add(candidateProfile);
      return candidateProfile;
    }

    @Override
    public Optional<CandidateProfile> findCandidateProfileByIdAndOrganizationId(
        UUID organizationId,
        CandidateProfileId candidateProfileId) {
      return createdProfiles.stream()
          .filter(profile -> profile.organizationId().equals(organizationId))
          .filter(profile -> profile.candidateProfileId().equals(candidateProfileId))
          .findFirst();
    }

    @Override
    public Optional<CandidateProfile> findCandidateProfileByCandidateIdAndOrganizationId(
        UUID organizationId,
        CandidateId candidateId) {
      return createdProfiles.stream()
          .filter(profile -> profile.organizationId().equals(organizationId))
          .filter(profile -> profile.candidateId().equals(candidateId))
          .findFirst();
    }

    @Override
    public CandidateProfileField upsertCandidateProfileField(
        UUID organizationId,
        CandidateProfileId candidateProfileId,
        CandidateProfileField field) {
      upsertedFields.add(field);
      return field;
    }

    @Override
    public List<CandidateProfileField> listCandidateProfileFields(
        UUID organizationId,
        CandidateProfileId candidateProfileId) {
      return List.copyOf(upsertedFields);
    }
  }
}
