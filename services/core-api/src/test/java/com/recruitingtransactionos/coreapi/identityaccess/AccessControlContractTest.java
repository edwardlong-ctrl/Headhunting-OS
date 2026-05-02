package com.recruitingtransactionos.coreapi.identityaccess;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AccessControlContractTest {

  private final PermissionEvaluator evaluator = new PermissionEvaluator();

  @Test
  void accessControlVocabularyCoversRequiredRolesResourcesActionsAndFields() {
    assertThat(PortalRole.values())
        .contains(
            PortalRole.OWNER,
            PortalRole.CONSULTANT,
            PortalRole.CLIENT,
            PortalRole.CANDIDATE,
            PortalRole.ADMIN,
            PortalRole.SYSTEM,
            PortalRole.AI_ASSISTANT);

    assertThat(ResourceType.values())
        .contains(
            ResourceType.CANDIDATE,
            ResourceType.CANDIDATE_PROFILE,
            ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
            ResourceType.SOURCE_ITEM,
            ResourceType.INFORMATION_PACKET,
            ResourceType.CLAIM_LEDGER_ITEM,
            ResourceType.REVIEW_EVENT,
            ResourceType.WORKFLOW_EVENT,
            ResourceType.CONSENT_RECORD,
            ResourceType.DISCLOSURE_RECORD,
            ResourceType.JOB,
            ResourceType.COMPANY,
            ResourceType.MATCH_REPORT,
            ResourceType.ADMIN_GOVERNANCE);

    assertThat(AccessAction.values())
        .contains(
            AccessAction.READ,
            AccessAction.CREATE,
            AccessAction.UPDATE,
            AccessAction.APPROVE,
            AccessAction.DISCLOSE,
            AccessAction.UNLOCK,
            AccessAction.AUDIT,
            AccessAction.EXPORT);

    assertThat(FieldClassification.values())
        .contains(
            FieldClassification.CLIENT_SAFE,
            FieldClassification.GENERALIZED,
            FieldClassification.INTERNAL,
            FieldClassification.PII,
            FieldClassification.RAW_SOURCE,
            FieldClassification.CONSULTANT_PRIVATE,
            FieldClassification.AUDIT,
            FieldClassification.COMMERCIAL,
            FieldClassification.CONSENT_DISCLOSURE,
            FieldClassification.SYSTEM_GOVERNANCE);
  }

  @Test
  void unknownAccessIsDeniedByDefault() {
    AccessDecision decision = evaluator.evaluate(new AccessRequest(
        PortalRole.UNKNOWN,
        ResourceType.UNKNOWN,
        AccessAction.UNKNOWN,
        FieldClassification.UNKNOWN,
        Set.of(),
        false));

    assertDenied(decision, "unknown_access_denied_by_default");
  }

  @Test
  void clientCannotReadRawCandidateOrCandidateProfile() {
    assertDenied(
        evaluator.evaluate(request(
            PortalRole.CLIENT,
            ResourceType.CANDIDATE,
            AccessAction.READ,
            FieldClassification.CLIENT_SAFE)),
        "client_raw_candidate_denied");

    assertDenied(
        evaluator.evaluate(request(
            PortalRole.CLIENT,
            ResourceType.CANDIDATE_PROFILE,
            AccessAction.READ,
            FieldClassification.GENERALIZED)),
        "client_raw_candidate_profile_denied");
  }

  @Test
  void clientCannotReadUnsafeCandidateFields() {
    for (FieldClassification unsafeField : List.of(
        FieldClassification.PII,
        FieldClassification.RAW_SOURCE,
        FieldClassification.CONSULTANT_PRIVATE,
        FieldClassification.INTERNAL,
        FieldClassification.AUDIT,
        FieldClassification.CONSENT_DISCLOSURE)) {
      AccessDecision decision = evaluator.evaluate(request(
          PortalRole.CLIENT,
          ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
          AccessAction.READ,
          unsafeField));

      assertDenied(decision, "client_unsafe_field_denied");
    }
  }

  @Test
  void clientCanReadClientSafeCandidateCardOnlyAtClientSafeOrGeneralizedLevels() {
    for (FieldClassification safeField : List.of(
        FieldClassification.CLIENT_SAFE,
        FieldClassification.GENERALIZED)) {
      AccessDecision decision = evaluator.evaluate(request(
          PortalRole.CLIENT,
          ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
          AccessAction.READ,
          safeField));

      assertAllowed(decision, "client_safe_candidate_card_read_allowed");
    }
  }

  @Test
  void clientCannotUseIdentityDisclosedOrInternalConsentDisclosureAccess() {
    AccessDecision l4Attempt = evaluator.evaluate(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        true));

    assertDenied(l4Attempt, "identity_disclosure_not_implemented");

    for (ResourceType resourceType : List.of(
        ResourceType.CONSENT_RECORD,
        ResourceType.DISCLOSURE_RECORD)) {
      AccessDecision decision = evaluator.evaluate(request(
          PortalRole.CLIENT,
          resourceType,
          AccessAction.READ,
          FieldClassification.CONSENT_DISCLOSURE));

      assertDenied(decision, "client_consent_disclosure_record_denied");
    }
  }

  @Test
  void candidateProfileReadRequiresExplicitSelfScope() {
    AccessDecision noScope = evaluator.evaluate(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false));

    assertDenied(noScope, "candidate_self_scope_required");

    for (FieldClassification safeField : List.of(
        FieldClassification.CLIENT_SAFE,
        FieldClassification.GENERALIZED)) {
      AccessDecision selfScoped = evaluator.evaluate(new AccessRequest(
          PortalRole.CANDIDATE,
          ResourceType.CANDIDATE_PROFILE,
          AccessAction.READ,
          safeField,
          Set.of(RelationshipScope.SELF),
          false));

      assertAllowed(selfScoped, "candidate_self_profile_read_allowed");
    }
  }

  @Test
  void candidateSelfScopeDoesNotAllowUnsafeProfileFields() {
    AccessDecision decision = evaluator.evaluate(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.RAW_SOURCE,
        Set.of(RelationshipScope.SELF),
        false));

    assertDenied(decision, "candidate_unsafe_field_denied");
  }

  @Test
  void consultantCandidateReadsRequireCandidateResourceType() {
    AccessDecision allowed = evaluator.evaluate(new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.CANDIDATE,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false));

    assertAllowed(allowed, "consultant_candidate_read_allowed");

    AccessDecision denied = evaluator.evaluate(new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.WORKFLOW_EVENT,
        AccessAction.READ,
        FieldClassification.INTERNAL,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false));

    assertAllowed(denied, "consultant_workflow_event_read_allowed");
  }

  @Test
  void consultantCanReadAndCreateSameOrganizationRawSourceItemsOnly() {
    for (AccessAction action : List.of(AccessAction.READ, AccessAction.CREATE)) {
      AccessDecision allowed = evaluator.evaluate(new AccessRequest(
          PortalRole.CONSULTANT,
          ResourceType.SOURCE_ITEM,
          action,
          FieldClassification.RAW_SOURCE,
          Set.of(RelationshipScope.SAME_ORGANIZATION),
          false));

      assertAllowed(
          allowed,
          action == AccessAction.READ
              ? "consultant_source_item_read_allowed"
              : "consultant_source_item_create_allowed");

      AccessDecision denied = evaluator.evaluate(new AccessRequest(
          PortalRole.CONSULTANT,
          ResourceType.SOURCE_ITEM,
          action,
          FieldClassification.RAW_SOURCE,
          Set.of(RelationshipScope.ASSIGNED_CONSULTANT),
          false));

      assertDenied(denied, "access_denied_by_default");
    }
  }

  @Test
  void adminSystemAndAiDoNotBypassCanonicalWriteOrDisclosureSemanticsByRoleAlone() {
    for (PortalRole governanceRole : List.of(
        PortalRole.ADMIN,
        PortalRole.SYSTEM,
        PortalRole.AI_ASSISTANT)) {
      AccessDecision canonicalWriteAttempt = evaluator.evaluate(new AccessRequest(
          governanceRole,
          ResourceType.CANDIDATE_PROFILE,
          AccessAction.APPROVE,
          FieldClassification.SYSTEM_GOVERNANCE,
          Set.of(RelationshipScope.GOVERNANCE),
          false));

      assertDenied(canonicalWriteAttempt, "domain_gate_required_for_canonical_write");

      AccessDecision disclosureAttempt = evaluator.evaluate(new AccessRequest(
          governanceRole,
          ResourceType.DISCLOSURE_RECORD,
          AccessAction.DISCLOSE,
          FieldClassification.CONSENT_DISCLOSURE,
          Set.of(RelationshipScope.GOVERNANCE),
          true));

      assertDenied(disclosureAttempt, "identity_disclosure_not_implemented");
    }
  }

  @Test
  void evaluatorHasNoApiControllerSpringSecurityOrDatabaseDependency() throws IOException {
    List<Path> productionFiles = identityAccessProductionFiles();

    assertThat(productionFiles)
        .extracting(path -> path.getFileName().toString())
        .contains(
            "AccessRequest.java",
            "AccessDecision.java",
            "PortalRole.java",
            "ResourceType.java",
            "AccessAction.java",
            "FieldClassification.java",
            "RelationshipScope.java",
            "FieldAccessPolicy.java",
            "PermissionEvaluator.java")
        .noneMatch(fileName -> fileName.endsWith("Controller.java"))
        .noneMatch(fileName -> fileName.endsWith("Repository.java"));

    for (Path file : productionFiles) {
      String source = Files.readString(file);
      assertThat(source)
          .doesNotContain("@RestController")
          .doesNotContain("@Controller")
          .doesNotContain("@Service")
          .doesNotContain("@Repository")
          .doesNotContain("@RequestMapping")
          .doesNotContain("org.springframework")
          .doesNotContain("SpringSecurity")
          .doesNotContain("SecurityContext")
          .doesNotContain("javax.sql.DataSource")
          .doesNotContain("JdbcTemplate")
          .doesNotContain("EntityManager")
          .doesNotContain("WebClient")
          .doesNotContain("RestTemplate");
    }
  }

  @Test
  void publicClientAccessContractDoesNotReturnRawCandidateOrCandidateProfileTypes() {
    Set<Class<?>> forbiddenRawTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class);

    for (Method method : PermissionEvaluator.class.getDeclaredMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getReturnType()).isEqualTo(AccessDecision.class);
      assertThat(method.getReturnType()).isNotIn(forbiddenRawTypes);
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenRawTypes);
    }

    for (Class<?> outputType : Set.of(AccessDecision.class)) {
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

  private static AccessRequest request(
      PortalRole role,
      ResourceType resourceType,
      AccessAction action,
      FieldClassification fieldClassification) {
    return new AccessRequest(role, resourceType, action, fieldClassification, Set.of(), false);
  }

  private static void assertAllowed(AccessDecision decision, String reasonCode) {
    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo(reasonCode);
    assertThat(decision.safeExplanation()).isNotBlank();
  }

  private static void assertDenied(AccessDecision decision, String reasonCode) {
    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo(reasonCode);
    assertThat(decision.safeExplanation()).isNotBlank();
  }

  private static List<Path> identityAccessProductionFiles() throws IOException {
    Path root = projectPath("src/main/java/com/recruitingtransactionos/coreapi/identityaccess");
    try (Stream<Path> stream = Files.walk(root)) {
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
}
