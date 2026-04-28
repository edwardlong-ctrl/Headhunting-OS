package com.recruitingtransactionos.coreapi.candidateprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileAccessService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.UpsertCandidateProfileFieldRequest;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CandidateProfileAccessServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-0000008b0001");
  private static final CandidateProfileId PROFILE_ID = new CandidateProfileId(
      UUID.fromString("00000000-0000-0000-0000-0000008b0002"));
  private static final CandidateId CANDIDATE_ID = new CandidateId(
      UUID.fromString("00000000-0000-0000-0000-0000008b0003"));
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-0000008b0004");
  private static final Instant NOW = Instant.parse("2026-04-28T08:00:00Z");

  @Test
  void clientCannotReadRawCandidateAtServiceBoundary() {
    CandidateProfileAccessService service = accessService(new RecordingCandidateProfilePort());

    assertThatThrownBy(() -> service.requireRawCandidateReadAllowed(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CANDIDATE,
        AccessAction.READ,
        FieldClassification.PII,
        Set.of(),
        false)))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode()).isEqualTo("client_raw_candidate_denied"));
  }

  @Test
  void clientCannotReadRawCandidateProfileOrRawProfileFieldsAtServiceBoundary() {
    RecordingCandidateProfilePort port = new RecordingCandidateProfilePort();
    CandidateProfileAccessService service = accessService(port);

    assertThatThrownBy(() -> service.findRawCandidateProfileByIdAndOrganizationId(
        new AccessRequest(
            PortalRole.CLIENT,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.PII,
            Set.of(),
            false),
        ORGANIZATION_ID,
        PROFILE_ID))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode())
                .isEqualTo("client_raw_candidate_profile_denied"));

    assertThatThrownBy(() -> service.listRawCandidateProfileFields(
        new AccessRequest(
            PortalRole.CLIENT,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.RAW_SOURCE,
            Set.of(),
            false),
        ORGANIZATION_ID,
        PROFILE_ID))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode())
                .isEqualTo("client_raw_candidate_profile_denied"));

    assertThat(port.findByProfileIdCalls).isZero();
    assertThat(port.listFieldCalls).isZero();
  }

  @Test
  void rawProfileBoundaryRejectsSafeFieldContextBeforeReturningRawProfile() {
    RecordingCandidateProfilePort port = new RecordingCandidateProfilePort();
    CandidateProfileAccessService service = accessService(port);

    assertThatThrownBy(() -> service.findRawCandidateProfileByIdAndOrganizationId(
        new AccessRequest(
            PortalRole.CANDIDATE,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.GENERALIZED,
            Set.of(RelationshipScope.SELF),
            false),
        ORGANIZATION_ID,
        PROFILE_ID))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode())
                .isEqualTo("raw_candidate_profile_requires_raw_field_context"));

    assertThat(port.findByProfileIdCalls).isZero();
  }

  @Test
  void candidateRawProfileReadRequiresExplicitSelfScopeWhereApplicable() {
    CandidateProfileAccessService service = accessService(new RecordingCandidateProfilePort());

    assertThatThrownBy(() -> service.findRawCandidateProfileByCandidateIdAndOrganizationId(
        new AccessRequest(
            PortalRole.CANDIDATE,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.RAW_SOURCE,
            Set.of(RelationshipScope.SAME_ORGANIZATION),
            false),
        ORGANIZATION_ID,
        CANDIDATE_ID))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode())
                .isEqualTo("candidate_self_scope_required"));

    assertThatThrownBy(() -> service.findRawCandidateProfileByCandidateIdAndOrganizationId(
        new AccessRequest(
            PortalRole.CANDIDATE,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.RAW_SOURCE,
            Set.of(RelationshipScope.SELF),
            false),
        ORGANIZATION_ID,
        CANDIDATE_ID))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode()).isEqualTo("candidate_unsafe_field_denied"));
  }

  @Test
  void clientCannotReadPiiRawSourceOrConsultantPrivateFieldClasses() {
    CandidateProfileAccessService service = accessService(new RecordingCandidateProfilePort());

    for (FieldClassification fieldClassification : List.of(
        FieldClassification.PII,
        FieldClassification.RAW_SOURCE,
        FieldClassification.CONSULTANT_PRIVATE)) {
      assertThatThrownBy(() -> service.listRawCandidateProfileFields(
          new AccessRequest(
              PortalRole.CLIENT,
              ResourceType.CANDIDATE_PROFILE,
              AccessAction.READ,
              fieldClassification,
              Set.of(),
              false),
          ORGANIZATION_ID,
          PROFILE_ID))
          .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
              assertThat(exception.decision().reasonCode())
                  .isEqualTo("client_raw_candidate_profile_denied"));
    }
  }

  @Test
  void sensitiveActionsAreDeniedWithoutImplementingDisclosureUnlockOrCanonicalWrite() {
    CandidateProfileAccessService service = accessService(new RecordingCandidateProfilePort());

    for (AccessAction action : List.of(AccessAction.DISCLOSE, AccessAction.UNLOCK)) {
      assertThatThrownBy(() -> service.requireSensitiveCandidateActionAllowed(new AccessRequest(
          PortalRole.CLIENT,
          ResourceType.CANDIDATE_PROFILE,
          action,
          FieldClassification.CONSENT_DISCLOSURE,
          Set.of(),
          true)))
          .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
              assertThat(exception.decision().allowed()).isFalse());
    }

    for (AccessAction action : List.of(
        AccessAction.APPROVE,
        AccessAction.DISCLOSE,
        AccessAction.UNLOCK,
        AccessAction.UPDATE)) {
      assertThatThrownBy(() -> service.requireSensitiveCandidateActionAllowed(new AccessRequest(
          PortalRole.AI_ASSISTANT,
          ResourceType.CANDIDATE_PROFILE,
          action,
          FieldClassification.SYSTEM_GOVERNANCE,
          Set.of(RelationshipScope.GOVERNANCE),
          action == AccessAction.DISCLOSE || action == AccessAction.UNLOCK)))
          .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
              assertThat(exception.decision().allowed()).isFalse());
    }

    for (PortalRole role : List.of(PortalRole.ADMIN, PortalRole.SYSTEM)) {
      assertThatThrownBy(() -> service.updateRawCandidateProfileField(
          new AccessRequest(
              role,
              ResourceType.CANDIDATE_PROFILE,
              AccessAction.UPDATE,
              FieldClassification.SYSTEM_GOVERNANCE,
              Set.of(RelationshipScope.GOVERNANCE),
              false),
          fieldRequest()))
          .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
              assertThat(exception.decision().reasonCode())
                  .isEqualTo("domain_gate_required_for_canonical_write"));
    }
  }

  private static CandidateProfileAccessService accessService(RecordingCandidateProfilePort port) {
    return new CandidateProfileAccessService(
        new CandidateProfileService(port),
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  private static UpsertCandidateProfileFieldRequest fieldRequest() {
    return UpsertCandidateProfileFieldRequest.builder()
        .organizationId(ORGANIZATION_ID)
        .candidateProfileId(PROFILE_ID)
        .fieldPath(CandidateProfileFieldPath.IDENTITY_FULL_NAME)
        .value(CandidateProfileFieldValue.ofString("Jane Candidate"))
        .fieldStatus(CandidateProfileFieldStatus.CONSULTANT_ATTESTED)
        .lineage(new CandidateProfileFieldLineage(
            List.of(CandidateProfileFieldSourceReference.sourceSpan(
                "span:task8b:identity.full_name",
                "service-boundary-test",
                NOW)),
            "task8b-service-boundary-test",
            NOW))
        .lastReviewedAt(NOW)
        .confirmedByActorId(ACTOR_ID)
        .notes("Task 8B service boundary test")
        .build();
  }

  private static final class RecordingCandidateProfilePort
      implements CandidateProfilePersistencePort {
    private int findByProfileIdCalls;
    private int findByCandidateIdCalls;
    private int listFieldCalls;
    private int upsertCalls;

    @Override
    public CandidateProfile createCandidateProfile(CandidateProfile candidateProfile) {
      throw new UnsupportedOperationException("not used by Task 8B access tests");
    }

    @Override
    public Optional<CandidateProfile> findCandidateProfileByIdAndOrganizationId(
        UUID organizationId,
        CandidateProfileId candidateProfileId) {
      findByProfileIdCalls++;
      return Optional.empty();
    }

    @Override
    public Optional<CandidateProfile> findCandidateProfileByCandidateIdAndOrganizationId(
        UUID organizationId,
        CandidateId candidateId) {
      findByCandidateIdCalls++;
      return Optional.empty();
    }

    @Override
    public CandidateProfileField upsertCandidateProfileField(
        UUID organizationId,
        CandidateProfileId candidateProfileId,
        CandidateProfileField field) {
      upsertCalls++;
      return field;
    }

    @Override
    public List<CandidateProfileField> listCandidateProfileFields(
        UUID organizationId,
        CandidateProfileId candidateProfileId) {
      listFieldCalls++;
      return new ArrayList<>();
    }
  }
}
