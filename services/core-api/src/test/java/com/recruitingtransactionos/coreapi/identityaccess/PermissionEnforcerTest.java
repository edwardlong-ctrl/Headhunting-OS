package com.recruitingtransactionos.coreapi.identityaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PermissionEnforcerTest {

  private final PermissionEnforcer enforcer = new PermissionEnforcer(new PermissionEvaluator());

  @Test
  void requireAllowedReturnsExplicitAllowDecisionForClientSafeCandidateCardRead() {
    AccessDecision decision = enforcer.requireAllowed(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("client_safe_candidate_card_read_allowed");
    assertThat(decision.safeExplanation()).isNotBlank();
  }

  @Test
  void requireAllowedReturnsExplicitAllowDecisionForClientSafeShortlistRead() {
    AccessDecision decision = enforcer.requireAllowed(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.SHORTLIST,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("client_safe_shortlist_read_allowed");
    assertThat(decision.safeExplanation()).isNotBlank();
  }

  @Test
  void requireAllowedReturnsExplicitAllowDecisionForClientSafeShortlistUpdate() {
    AccessDecision decision = enforcer.requireAllowed(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.SHORTLIST,
        AccessAction.UPDATE,
        FieldClassification.CLIENT_SAFE,
        Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
        false));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("client_safe_shortlist_update_allowed");
    assertThat(decision.safeExplanation()).isNotBlank();
  }

  @Test
  void deniedAccessThrowsAndPreservesEvaluatorDecisionReasonCodeAndExplanation() {
    AccessRequest deniedRequest = new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.PII,
        Set.of(),
        false);

    assertThatThrownBy(() -> enforcer.requireAllowed(deniedRequest))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception -> {
          assertThat(exception.decision().allowed()).isFalse();
          assertThat(exception.decision().reasonCode())
              .isEqualTo("client_raw_candidate_profile_denied");
          assertThat(exception.decision().safeExplanation()).isNotBlank();
          assertThat(exception.getMessage()).contains("client_raw_candidate_profile_denied");
        });
  }

  @Test
  void auditedRequireAllowedRecordsAllowedAndDeniedAccessDecisionsBeforeReturningOrThrowing() {
    RecordingAccessAuditRecorder recorder = new RecordingAccessAuditRecorder();
    PermissionEnforcer auditedEnforcer = new PermissionEnforcer(new PermissionEvaluator(), recorder);
    AccessAuditContext auditContext = new AccessAuditContext(
        UUID.fromString("00000000-0000-0000-0000-000000410001"),
        UUID.fromString("00000000-0000-0000-0000-000000410002"),
        UUID.fromString("00000000-0000-0000-0000-000000410003"),
        "client_safe");

    auditedEnforcer.requireAllowed(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false), auditContext);

    assertThatThrownBy(() -> auditedEnforcer.requireAllowed(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.PII,
        Set.of(),
        false), auditContext))
        .isInstanceOf(AccessDeniedException.class);

    assertThat(recorder.events).hasSize(2);
    assertThat(recorder.events.get(0).decision().allowed()).isTrue();
    assertThat(recorder.events.get(0).request().resourceType())
        .isEqualTo(ResourceType.CLIENT_SAFE_CANDIDATE_CARD);
    assertThat(recorder.events.get(1).decision().allowed()).isFalse();
    assertThat(recorder.events.get(1).decision().reasonCode())
        .isEqualTo("client_raw_candidate_profile_denied");
  }

  @Test
  void nullAccessRequestFailsClosedWithExplicitDenial() {
    AccessDecision decision = enforcer.evaluate(null);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("access_request_required");

    assertThatThrownBy(() -> enforcer.requireAllowed(null))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception ->
            assertThat(exception.decision().reasonCode()).isEqualTo("access_request_required"));
  }

  @Test
  void sensitiveActionsRemainDeniedThroughEnforcerForClientAndAutomationRoles() {
    for (AccessAction action : List.of(
        AccessAction.DISCLOSE,
        AccessAction.UNLOCK,
        AccessAction.EXPORT,
        AccessAction.UPDATE)) {
      assertDenied(new AccessRequest(
          PortalRole.CLIENT,
          ResourceType.CANDIDATE_PROFILE,
          action,
          FieldClassification.PII,
          Set.of(),
          action == AccessAction.DISCLOSE || action == AccessAction.UNLOCK));
    }

    for (PortalRole role : List.of(
        PortalRole.AI_ASSISTANT,
        PortalRole.SYSTEM,
        PortalRole.ADMIN)) {
      assertDenied(new AccessRequest(
          role,
          ResourceType.CANDIDATE_PROFILE,
          AccessAction.APPROVE,
          FieldClassification.SYSTEM_GOVERNANCE,
          Set.of(RelationshipScope.GOVERNANCE),
          false));
      assertDenied(new AccessRequest(
          role,
          ResourceType.DISCLOSURE_RECORD,
          AccessAction.DISCLOSE,
          FieldClassification.CONSENT_DISCLOSURE,
          Set.of(RelationshipScope.GOVERNANCE),
          true));
      assertDenied(new AccessRequest(
          role,
          ResourceType.CANDIDATE_PROFILE,
          AccessAction.UNLOCK,
          FieldClassification.CONSENT_DISCLOSURE,
          Set.of(RelationshipScope.GOVERNANCE),
          true));
    }
  }

  @Test
  void consultantProductAccessRequiresExplicitSameOrganizationScope() {
    for (ResourceType resourceType : List.of(
        ResourceType.COMPANY,
        ResourceType.JOB,
        ResourceType.SHORTLIST,
        ResourceType.PLACEMENT,
        ResourceType.COMMISSION)) {
      assertDenied(new AccessRequest(
          PortalRole.CONSULTANT,
          resourceType,
          AccessAction.READ,
          FieldClassification.INTERNAL,
          Set.of(),
          false));
      assertDenied(new AccessRequest(
          PortalRole.CONSULTANT,
          resourceType,
          AccessAction.CREATE,
          FieldClassification.INTERNAL,
          Set.of(),
          false));

      AccessDecision read = enforcer.requireAllowed(new AccessRequest(
          PortalRole.CONSULTANT,
          resourceType,
          AccessAction.READ,
          FieldClassification.INTERNAL,
          Set.of(RelationshipScope.SAME_ORGANIZATION),
          false));
      assertThat(read.reasonCode()).isEqualTo("consultant_read_allowed");

      AccessDecision write = enforcer.requireAllowed(new AccessRequest(
          PortalRole.CONSULTANT,
          resourceType,
          AccessAction.CREATE,
          FieldClassification.INTERNAL,
          Set.of(RelationshipScope.SAME_ORGANIZATION),
          false));
      assertThat(write.reasonCode()).isEqualTo("consultant_write_allowed");
    }
  }

  @Test
  void candidateProfileAccessRequiresSelfScopeAndStillDeniesUnsafeFields() {
    assertDenied(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false));

    assertDenied(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.RAW_SOURCE,
        Set.of(RelationshipScope.SELF),
        false));

    AccessDecision selfScopedSafeRead = enforcer.requireAllowed(new AccessRequest(
        PortalRole.CANDIDATE,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(RelationshipScope.SELF),
        false));
    assertThat(selfScopedSafeRead.reasonCode()).isEqualTo("candidate_self_profile_read_allowed");
  }

  @Test
  void unknownVocabularyFailsClosedThroughEnforcer() {
    assertDenied(new AccessRequest(
        PortalRole.UNKNOWN,
        ResourceType.CANDIDATE_PROFILE,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(),
        false));
    assertDenied(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.UNKNOWN,
        AccessAction.READ,
        FieldClassification.GENERALIZED,
        Set.of(),
        false));
    assertDenied(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.UNKNOWN,
        FieldClassification.GENERALIZED,
        Set.of(),
        false));
    assertDenied(new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.UNKNOWN,
        Set.of(),
        false));
  }

  @Test
  void enforcerIntroducesNoSpringSecurityApiControllerOrDatabaseDependency() throws IOException {
    for (Path file : identityAccessProductionFiles()) {
      String source = Files.readString(file);
      assertThat(source)
          .doesNotContain("@RestController")
          .doesNotContain("@Controller")
          .doesNotContain("@RequestMapping")
          .doesNotContain("org.springframework")
          .doesNotContain("SpringSecurity")
          .doesNotContain("SecurityContext")
          .doesNotContain("Authentication")
          .doesNotContain("javax.sql.DataSource")
          .doesNotContain("JdbcTemplate")
          .doesNotContain("EntityManager")
          .doesNotContain("WebClient")
          .doesNotContain("RestTemplate");
    }
  }

  private void assertDenied(AccessRequest request) {
    assertThatThrownBy(() -> enforcer.requireAllowed(request))
        .isInstanceOfSatisfying(AccessDeniedException.class, exception -> {
          assertThat(exception.decision().allowed()).isFalse();
          assertThat(exception.decision().reasonCode()).isNotBlank();
          assertThat(exception.decision().safeExplanation()).isNotBlank();
        });
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

  private static final class RecordingAccessAuditRecorder implements AccessAuditRecorder {
    private final List<AccessAuditEvent> events = new ArrayList<>();

    @Override
    public void record(AccessAuditEvent event) {
      events.add(event);
    }
  }
}
