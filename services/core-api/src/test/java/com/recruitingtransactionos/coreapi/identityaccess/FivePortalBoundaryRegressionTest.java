package com.recruitingtransactionos.coreapi.identityaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileAccessService;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateProjectionService;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientVisibleCandidateFieldPolicy;
import com.recruitingtransactionos.coreapi.clientsafeprojection.InternalCandidateProjectionSnapshot;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessment;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FivePortalBoundaryRegressionTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-0000008c0001");
  private static final CandidateProfileId PROFILE_ID = new CandidateProfileId(
      UUID.fromString("00000000-0000-0000-0000-0000008c0002"));

  private final PermissionEvaluator evaluator = new PermissionEvaluator();
  private final PermissionEnforcer enforcer = new PermissionEnforcer(evaluator);

  @Test
  void fivePortalAndAutomationRolesCannotReadRawCandidateOrProfileByRoleAlone() {
    for (PortalRole role : productAndAutomationRoles()) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          role,
          ResourceType.CANDIDATE,
          AccessAction.READ,
          FieldClassification.PII,
          scopesForRole(role),
          false)));

      assertDenied(evaluator.evaluate(new AccessRequest(
          role,
          ResourceType.CANDIDATE_PROFILE,
          AccessAction.READ,
          FieldClassification.RAW_SOURCE,
          scopesForRole(role),
          false)));
    }
  }

  @Test
  void clientBoundaryAllowsOnlySafeCardReadAndDeniesRawUnsafeDisclosureAndSensitiveActions() {
    for (FieldClassification safeField : List.of(
        FieldClassification.CLIENT_SAFE,
        FieldClassification.GENERALIZED)) {
      assertAllowed(evaluator.evaluate(request(
          PortalRole.CLIENT,
          ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
          AccessAction.READ,
          safeField)));
    }

    assertDenied(evaluator.evaluate(request(
        PortalRole.CLIENT,
        ResourceType.CANDIDATE,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE)), "client_raw_candidate_denied");
    assertDenied(evaluator.evaluate(request(
        PortalRole.CLIENT,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED)), "client_raw_candidate_profile_denied");

    for (FieldClassification unsafeField : List.of(
        FieldClassification.INTERNAL,
        FieldClassification.PII,
        FieldClassification.RAW_SOURCE,
        FieldClassification.CONSULTANT_PRIVATE,
        FieldClassification.AUDIT,
        FieldClassification.CONSENT_DISCLOSURE,
        FieldClassification.SYSTEM_GOVERNANCE)) {
      assertDenied(evaluator.evaluate(request(
          PortalRole.CLIENT,
          ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
          AccessAction.READ,
          unsafeField)), "client_unsafe_field_denied");
    }

    for (ResourceType internalRecord : List.of(
        ResourceType.CONSENT_RECORD,
        ResourceType.DISCLOSURE_RECORD)) {
      assertDenied(evaluator.evaluate(request(
          PortalRole.CLIENT,
          internalRecord,
          AccessAction.READ,
          FieldClassification.CONSENT_DISCLOSURE)), "client_consent_disclosure_record_denied");
    }

    for (AccessAction sensitiveAction : List.of(
        AccessAction.DISCLOSE,
        AccessAction.UNLOCK,
        AccessAction.EXPORT,
        AccessAction.UPDATE,
        AccessAction.APPROVE)) {
      assertDeniedByEnforcer(new AccessRequest(
          PortalRole.CLIENT,
          ResourceType.CANDIDATE_PROFILE,
          sensitiveAction,
          FieldClassification.PII,
          Set.of(),
          sensitiveAction == AccessAction.DISCLOSE || sensitiveAction == AccessAction.UNLOCK));
    }
  }

  @Test
  void clientCannotUseL4IdentityDisclosedAsAnonymousAccessOrProjectionOutput() {
    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        true)), "identity_disclosure_not_implemented");

    ClientSafeCandidateProjectionService projectionService =
        new ClientSafeCandidateProjectionService();
    assertThatThrownBy(() -> projectionService.project(
        clientSafeReadRequest(),
        snapshotWithRedactionLevel(RedactionLevel.L4_IDENTITY_DISCLOSED)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("client-safe projection cannot use L4 identity disclosure");
  }

  @Test
  void candidateBoundaryRequiresSelfScopeAndDeniesUnsafeSourceAndGovernanceAccess() {
    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(),
        false)), "candidate_self_scope_required");

    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false)), "candidate_self_scope_required");

    assertAllowed(evaluator.evaluate(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(RelationshipScope.SELF),
        false)));

    for (FieldClassification unsafeField : List.of(
        FieldClassification.CONSULTANT_PRIVATE,
        FieldClassification.RAW_SOURCE)) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          PortalRole.CANDIDATE,
          ResourceType.CANDIDATE_PROFILE,
          AccessAction.READ,
          unsafeField,
          Set.of(RelationshipScope.SELF),
          false)), "candidate_unsafe_field_denied");
    }

    for (ResourceType nonCandidateSelfResource : List.of(
        ResourceType.COMPANY,
        ResourceType.JOB,
        ResourceType.ADMIN_GOVERNANCE,
        ResourceType.CONSENT_RECORD,
        ResourceType.DISCLOSURE_RECORD)) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          PortalRole.CANDIDATE,
          nonCandidateSelfResource,
          AccessAction.READ,
          FieldClassification.GENERALIZED,
          Set.of(RelationshipScope.SELF),
          false)));
    }
  }

  @Test
  void consultantBoundaryDoesNotGrantDisclosureUnlockCanonicalWriteOrL4ProjectionByRoleAlone() {
    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.ASSIGNED_CONSULTANT),
        false)));

    for (AccessAction disclosureAction : List.of(AccessAction.DISCLOSE, AccessAction.UNLOCK)) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          PortalRole.CONSULTANT,
          ResourceType.DISCLOSURE_RECORD,
          disclosureAction,
          FieldClassification.CONSENT_DISCLOSURE,
          Set.of(RelationshipScope.ASSIGNED_CONSULTANT),
          true)), "identity_disclosure_not_implemented");
    }

    for (AccessAction canonicalWriteAction : List.of(
        AccessAction.UPDATE,
        AccessAction.APPROVE)) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          PortalRole.CONSULTANT,
          ResourceType.CANDIDATE_PROFILE,
          canonicalWriteAction,
          FieldClassification.INTERNAL,
          Set.of(RelationshipScope.ASSIGNED_CONSULTANT),
          false)));
    }

    ClientSafeCandidateProjectionService projectionService =
        new ClientSafeCandidateProjectionService();
    assertThatThrownBy(() -> projectionService.project(
        new AccessRequest(
            PortalRole.CONSULTANT,
            ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
            AccessAction.READ,
            FieldClassification.CLIENT_SAFE,
            Set.of(RelationshipScope.ASSIGNED_CONSULTANT),
            false),
        baseSnapshot()))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().allowed()).isFalse());

    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.ASSIGNED_CONSULTANT),
        true)), "identity_disclosure_not_implemented");
  }

  @Test
  void ownerAdminSystemAndAiDoNotBypassCanonicalWriteOrDisclosureSemanticsByRoleAlone() {
    for (PortalRole role : List.of(
        PortalRole.OWNER,
        PortalRole.ADMIN,
        PortalRole.SYSTEM,
        PortalRole.AI_ASSISTANT)) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          role,
          ResourceType.CANDIDATE_PROFILE,
          AccessAction.UPDATE,
          FieldClassification.SYSTEM_GOVERNANCE,
          Set.of(RelationshipScope.GOVERNANCE),
          false)));

      assertDenied(evaluator.evaluate(new AccessRequest(
          role,
          ResourceType.DISCLOSURE_RECORD,
          AccessAction.DISCLOSE,
          FieldClassification.CONSENT_DISCLOSURE,
          Set.of(RelationshipScope.GOVERNANCE),
          true)), "identity_disclosure_not_implemented");

      assertDenied(evaluator.evaluate(new AccessRequest(
          role,
          ResourceType.ADMIN_GOVERNANCE,
          AccessAction.AUDIT,
          FieldClassification.SYSTEM_GOVERNANCE,
          Set.of(RelationshipScope.GOVERNANCE),
          false)));
    }

    for (AccessAction aiSensitiveAction : List.of(
        AccessAction.APPROVE,
        AccessAction.DISCLOSE,
        AccessAction.UNLOCK,
        AccessAction.EXPORT,
        AccessAction.UPDATE)) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          PortalRole.AI_ASSISTANT,
          ResourceType.CANDIDATE_PROFILE,
          aiSensitiveAction,
          FieldClassification.SYSTEM_GOVERNANCE,
          Set.of(RelationshipScope.GOVERNANCE),
          aiSensitiveAction == AccessAction.DISCLOSE || aiSensitiveAction == AccessAction.UNLOCK)));
    }
  }

  @Test
  void unknownRolesResourcesActionsAndFieldsRemainDenied() {
    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.UNKNOWN,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false)), "unknown_access_denied_by_default");
    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.UNKNOWN,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false)), "unknown_access_denied_by_default");
    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.UNKNOWN,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false)), "unknown_access_denied_by_default");
    assertDenied(evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.UNKNOWN,
        Set.of(),
        false)), "unknown_access_denied_by_default");
  }

  @Test
  void crossBoundaryMatrixKeepsClientSafeCardAsOnlyClientReadableCandidateOutput() {
    for (ResourceType candidateFacingResource : List.of(
        ResourceType.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        ResourceType.MATCH_REPORT,
        ResourceType.SOURCE_ITEM,
        ResourceType.INFORMATION_PACKET,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD)) {
      AccessDecision decision = evaluator.evaluate(new AccessRequest(
          PortalRole.CLIENT,
          candidateFacingResource,
          AccessAction.READ,
          FieldClassification.CLIENT_SAFE,
          Set.of(),
          false));

      if (candidateFacingResource == ResourceType.CLIENT_SAFE_CANDIDATE_CARD) {
        assertAllowed(decision);
      } else {
        assertDenied(decision);
      }
    }

    for (PortalRole role : List.of(
        PortalRole.OWNER,
        PortalRole.CONSULTANT,
        PortalRole.CANDIDATE,
        PortalRole.ADMIN,
        PortalRole.SYSTEM,
        PortalRole.AI_ASSISTANT)) {
      assertDenied(evaluator.evaluate(new AccessRequest(
          role,
          ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
          AccessAction.READ,
          FieldClassification.CLIENT_SAFE,
          scopesForRole(role),
          false)));
    }
  }

  @Test
  void enforcerProjectionAndProfileAccessFacadesFailClosedAndRequireExplicitAccessRequests() {
    assertDenied(enforcer.evaluate(null), "access_request_required");

    ClientSafeCandidateProjectionService projectionService =
        new ClientSafeCandidateProjectionService();
    assertThatThrownBy(() -> projectionService.project(null, baseSnapshot()))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode()).isEqualTo("access_request_required"));

    RecordingCandidateProfilePort port = new RecordingCandidateProfilePort();
    CandidateProfileAccessService profileAccessService = accessService(port);

    assertThatThrownBy(() -> profileAccessService.findRawCandidateProfileByIdAndOrganizationId(
        request(
            PortalRole.CLIENT,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.PII),
        ORGANIZATION_ID,
        PROFILE_ID))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode())
                .isEqualTo("client_raw_candidate_profile_denied"));

    assertThatThrownBy(() -> profileAccessService.findRawCandidateProfileByIdAndOrganizationId(
        null,
        ORGANIZATION_ID,
        PROFILE_ID))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode()).isEqualTo("access_request_required"));

    assertThat(port.findByProfileIdCalls).isZero();
  }

  @Test
  void publicAccessAndProjectionOutputTypesDoNotExposeRawCandidateOrProfileTypes() {
    Set<Class<?>> forbiddenRawTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class);

    for (Method method : PermissionEvaluator.class.getDeclaredMethods()) {
      if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getReturnType()).isNotIn(forbiddenRawTypes);
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenRawTypes);
    }

    for (Method method : ClientSafeCandidateProjectionService.class.getMethods()) {
      if (!method.getName().equals("project")) {
        continue;
      }
      assertThat(method.getReturnType()).isEqualTo(ClientSafeCandidateCard.class);
      assertThat(method.getReturnType()).isNotIn(forbiddenRawTypes);
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenRawTypes);
    }

    for (Class<?> outputType : List.of(
        AccessDecision.class,
        ClientSafeCandidateCard.class,
        ReidentificationRiskAssessment.class)) {
      assertThat(outputType.getRecordComponents())
          .as(outputType.getSimpleName())
          .extracting(RecordComponent::getType)
          .doesNotContainAnyElementsOf(forbiddenRawTypes);
      assertThat(outputType.getRecordComponents())
          .as(outputType.getSimpleName())
          .extracting(RecordComponent::getName)
          .noneMatch(name -> name.contains("rawCandidate"))
          .noneMatch(name -> name.contains("rawCandidateProfile"));
    }
  }

  private static List<PortalRole> productAndAutomationRoles() {
    return List.of(
        PortalRole.OWNER,
        PortalRole.CONSULTANT,
        PortalRole.CLIENT,
        PortalRole.CANDIDATE,
        PortalRole.ADMIN,
        PortalRole.SYSTEM,
        PortalRole.AI_ASSISTANT);
  }

  private static Set<RelationshipScope> scopesForRole(PortalRole role) {
    return switch (role) {
      case CANDIDATE -> Set.of(RelationshipScope.SELF);
      case CONSULTANT -> Set.of(RelationshipScope.ASSIGNED_CONSULTANT);
      case OWNER -> Set.of(RelationshipScope.OWNED_ACCOUNT);
      case ADMIN, SYSTEM, AI_ASSISTANT -> Set.of(RelationshipScope.GOVERNANCE);
      default -> Set.of();
    };
  }

  private static AccessRequest request(
      PortalRole role,
      ResourceType resourceType,
      AccessAction action,
      FieldClassification fieldClassification) {
    return new AccessRequest(role, resourceType, action, fieldClassification, Set.of(), false);
  }

  private static AccessRequest clientSafeReadRequest() {
    return request(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE);
  }

  private static InternalCandidateProjectionSnapshot baseSnapshot() {
    return snapshotWithRedactionLevel(RedactionLevel.L2_CLIENT_SAFE);
  }

  private static InternalCandidateProjectionSnapshot snapshotWithRedactionLevel(
      RedactionLevel redactionLevel) {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000008c1001",
        "00000000-0000-0000-0000-0000008c1002",
        "Task Eight Candidate",
        "task.eight@example.com",
        "+86 138 0000 8C8C",
        "https://www.linkedin.com/in/task-eight-candidate",
        "NebulaChip Systems",
        List.of("Orion-X7 NPU"),
        "Task Eight Candidate led Orion-X7 NPU verification at NebulaChip Systems.",
        "Consultant-only negotiation note.",
        AnonymousCandidateCardId.of("card_task8c_0001"),
        AnonymousCandidateRef.of("anon_candidate_task8c_0001"),
        "projection-v1",
        redactionLevel,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led complex verification programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence generalized from approved profile signals."),
        List.of("Strong fit based on generalized capability evidence."),
        ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths());
  }

  private static CandidateProfileAccessService accessService(RecordingCandidateProfilePort port) {
    return new CandidateProfileAccessService(
        new CandidateProfileService(port),
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  private void assertDeniedByEnforcer(AccessRequest request) {
    assertThatThrownBy(() -> enforcer.requireAllowed(request))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception -> {
          assertThat(exception.decision().allowed()).isFalse();
          assertThat(exception.decision().reasonCode()).isNotBlank();
          assertThat(exception.decision().safeExplanation()).isNotBlank();
        });
  }

  private static void assertAllowed(AccessDecision decision) {
    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isNotBlank();
    assertThat(decision.safeExplanation()).isNotBlank();
  }

  private static void assertDenied(AccessDecision decision) {
    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isNotBlank();
    assertThat(decision.safeExplanation()).isNotBlank();
  }

  private static void assertDenied(AccessDecision decision, String reasonCode) {
    assertDenied(decision);
    assertThat(decision.reasonCode()).isEqualTo(reasonCode);
  }

  private static final class RecordingCandidateProfilePort
      implements CandidateProfilePersistencePort {
    private int findByProfileIdCalls;

    @Override
    public CandidateProfile createCandidateProfile(CandidateProfile candidateProfile) {
      throw new UnsupportedOperationException("not used by Task 8C boundary tests");
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
      throw new UnsupportedOperationException("not used by Task 8C boundary tests");
    }

    @Override
    public CandidateProfileField upsertCandidateProfileField(
        UUID organizationId,
        CandidateProfileId candidateProfileId,
        CandidateProfileField field) {
      throw new UnsupportedOperationException("not used by Task 8C boundary tests");
    }

    @Override
    public List<CandidateProfileField> listCandidateProfileFields(
        UUID organizationId,
        CandidateProfileId candidateProfileId) {
      throw new UnsupportedOperationException("not used by Task 8C boundary tests");
    }
  }
}
