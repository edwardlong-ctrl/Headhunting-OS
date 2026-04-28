package com.recruitingtransactionos.coreapi.candidateprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CreateCandidateProfileRequest;
import com.recruitingtransactionos.coreapi.candidateprofile.service.UpsertCandidateProfileFieldRequest;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
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
