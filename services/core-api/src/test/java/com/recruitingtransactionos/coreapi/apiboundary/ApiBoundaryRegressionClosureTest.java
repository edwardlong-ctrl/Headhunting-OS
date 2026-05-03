package com.recruitingtransactionos.coreapi.apiboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticatedPrincipal;
import com.recruitingtransactionos.coreapi.identityauth.RtoAuthenticationToken;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import org.springframework.security.core.Authentication;
import java.util.UUID;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthSession;
import com.recruitingtransactionos.coreapi.identityauth.IdentityUserAccount;
import com.recruitingtransactionos.coreapi.identityauth.JwtService;
import com.recruitingtransactionos.coreapi.identityauth.IdentityAuthenticationPort;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@org.springframework.test.context.TestPropertySource(properties = {"rto.auth.jwt.secret=0123456789abcdef0123456789abcdef", "rto.auth.jwt.issuer=test"})
@WebMvcTest(ClientSafeCandidateCardController.class)
@Import({
    ApiBoundaryRegressionClosureTest.TestConfig.class,
    com.recruitingtransactionos.coreapi.identityauth.SecurityConfig.class
})
class ApiBoundaryRegressionClosureTest {

  private static final String ENDPOINT =
      "/api/client-safe/candidate-cards/card_task9c_0001";
  private static final String ROLE_HEADER = "X-RTO-Actor-Role";
  private static final String ORGANIZATION_ID_HEADER = "X-RTO-Organization-Id";
  private static final String ORGANIZATION_ID =
      "00000000-0000-0000-0000-0000009c0003";
  private static final String USER_ACCOUNT_ID =
      "00000000-0000-0000-0000-0000009c0004";

  private static final String RAW_CANDIDATE_ID =
      "00000000-0000-0000-0000-0000009c0001";
  private static final String RAW_CANDIDATE_PROFILE_ID =
      "00000000-0000-0000-0000-0000009c0002";
  private static final String FULL_NAME = "Jane Task9C Candidate";
  private static final String EMAIL = "jane.task9c@example.com";
  private static final String PHONE = "+86 138 0000 9C9C";
  private static final String LINKEDIN_URL = "https://www.linkedin.com/in/jane-task9c";
  private static final String EXACT_EMPLOYER = "NebulaChip Systems";
  private static final String EXACT_PROJECT = "Orion-X7 NPU";
  private static final String EXACT_PRODUCT = "VectorSlate PCIe Gen6 Switch";
  private static final String EXACT_CHIP = "NC-9000";
  private static final String RAW_SOURCE_TEXT =
      "Jane Task9C Candidate led Orion-X7 NPU verification at NebulaChip Systems.";
  private static final String RAW_CV_TEXT =
      "CV text: Jane Task9C Candidate, email jane.task9c@example.com.";
  private static final String CONSULTANT_NOTES =
      "Consultant note: do not reveal that Jane is negotiating with HelioSemi.";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private RecordingClientSafeCandidateCardQueryPort queryPort;

  @MockBean
  private IdentityAuthenticationPort identityAuthenticationPort;

  @BeforeEach
  void resetQueryPort() {
    queryPort.reset();
  }

  @Test
  void requestPathUsesAnonymousCardRefOnly() throws Exception {
    mockMvc.perform(get(ENDPOINT)
            .with(authentication(auth("client")))
            )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anonymousCardRef").value("card_task9c_0001"));

    assertThat(queryPort.calls).isEqualTo(1);
    assertThat(queryPort.lastScope).isEqualTo(
        ClientSafeCandidateCardQueryScope.of(java.util.UUID.fromString(ORGANIZATION_ID), java.util.UUID.fromString(USER_ACCOUNT_ID), com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole.CLIENT));
    assertThat(queryPort.lastCardId)
        .isEqualTo(AnonymousCandidateCardId.of("card_task9c_0001"));
  }

  @Test
  void mergedClientPortalFetchHelperNoLongerDependsOnTemporaryAccessHeaders()
      throws IOException {
    Path webRoot = Path.of(System.getProperty("user.dir"))
        .getParent()
        .getParent()
        .resolve("apps/web/src");
    String source = Files.readString(webRoot.resolve("api/clientSafeCandidateCards.ts"));
    String storage = Files.readString(webRoot.resolve("auth/accessTokenStorage.ts"));
    String app = Files.readString(webRoot.resolve("App.tsx"));

    assertThat(source)
        .doesNotContain("\"X-RTO-Actor-Role\": \"client\"")
        .doesNotContain("\"X-RTO-Field-Classification\": \"client_safe\"")
        .doesNotContain("\"X-RTO-Identity-Disclosure-Requested\": \"false\"")
        .doesNotContain("\"X-RTO-Organization-Id\"")
        .doesNotContain("VITE_RTO_CLIENT_ORGANIZATION_ID")
        .doesNotContain("00000000-0000-0000-0000-00000013b001")
        .contains("Authorization")
        .contains("loadAccessToken");
    assertThat(storage).contains("replace(/^Bearer\\s+/i, \"\")");
    assertThat(app).contains("Access token from /api/auth/login")
        .doesNotContain("Bearer token from /api/auth/login");
  }

  @Test
  void rawUuidAndRawCandidateStylePathRefsAreRejectedBeforeQuery() throws Exception {
    for (String unsafeRef : List.of(
        RAW_CANDIDATE_ID,
        "candidate_" + RAW_CANDIDATE_ID,
        "candidate_profile_" + RAW_CANDIDATE_PROFILE_ID,
        "anon_candidate_task9c_0001")) {
      queryPort.reset();

      MvcResult result = mockMvc.perform(get(
              "/api/client-safe/candidate-cards/" + unsafeRef)
              .with(authentication(auth("client")))
              )
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.errorCode").value("validation_failed"))
          .andExpect(jsonPath("$.error.safeReason")
              .value("invalid_anonymous_card_reference"))
          .andReturn();

      assertSanitizedApiBody(result.getResponse().getContentAsString());
      assertThat(queryPort.calls).as(unsafeRef).isZero();
    }
  }

  @Test
  void clientSafeEndpointDefaultsToPrincipalContextAndRejectsInvalidRoles()
      throws Exception {
    MvcResult missing = mockMvc.perform(get(ENDPOINT))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"))
        .andExpect(jsonPath("$.error.safeReason").value("authentication_required"))
        .andReturn();
    assertSanitizedApiBody(missing.getResponse().getContentAsString());

    mockMvc.perform(get(ENDPOINT)
            .with(authentication(auth("client"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anonymousCardRef").value("card_task9c_0001"));
    assertThat(queryPort.calls).isEqualTo(1);
    queryPort.reset();
  }

  @Test
  void revokedSessionBearerTokenFailsClosedBeforeController() throws Exception {
    RtoAuthenticatedPrincipal principal = principal("client", UUID.fromString(ORGANIZATION_ID));
    Instant issuedAt = Instant.now().minusSeconds(60);
    when(identityAuthenticationPort.findActiveSessionBySessionId(
        eq(principal.sessionId()), any(Instant.class)))
        .thenReturn(Optional.empty());

    mockMvc.perform(get(ENDPOINT)
            .header("Authorization", validBearerToken(principal, issuedAt))
            )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.errorCode").value("authentication_failed"))
        .andExpect(jsonPath("$.error.safeReason").value("invalid_token"));
    assertThat(queryPort.calls).isZero();
  }

  @Test
  void activeBearerTokenAuthenticatesClientSafeCardRequest() throws Exception {
    RtoAuthenticatedPrincipal principal = principal("client", UUID.fromString(ORGANIZATION_ID));
    Instant issuedAt = Instant.now().minusSeconds(60);
    when(identityAuthenticationPort.findActiveSessionBySessionId(
        eq(principal.sessionId()), any(Instant.class)))
        .thenReturn(Optional.of(new IdentityAuthSession(
            principal.sessionId(),
            principal.organizationId(),
            principal.userAccountId(),
            principal.portalRole(),
            "refresh-token-hash",
            issuedAt.plusSeconds(1800),
            null,
            issuedAt,
            issuedAt,
            1)));
    when(identityAuthenticationPort.findByOrganizationIdAndUserAccountId(
        principal.organizationId(), principal.userAccountId()))
        .thenReturn(Optional.of(new IdentityUserAccount(
            principal.userAccountId(),
            principal.organizationId(),
            "client@example.test",
            principal.displayName(),
            "active",
            "$2a$10$abcdefghijklmnopqrstuv",
            issuedAt)));
    when(identityAuthenticationPort.hasActiveRoleAssignment(
        principal.organizationId(),
        principal.userAccountId(),
        principal.portalRole())).thenReturn(true);

    mockMvc.perform(get(ENDPOINT)
            .header("Authorization", validBearerToken(principal, issuedAt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anonymousCardRef").value("card_task9c_0001"));
    assertThat(queryPort.calls).isEqualTo(1);
    queryPort.reset();
  }

  @Test
  void clientAccessIsLimitedToSafeOrGeneralizedCardOutputWithoutL4Disclosure()
      throws Exception {
    queryPort.reset();
    MvcResult result = mockMvc.perform(get(ENDPOINT)
            .with(authentication(auth("client")))
            )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.redactionLevel").value("l2_client_safe"))
        .andReturn();

    assertSuccessEnvelopeHasOnlyClientSafeCardResponse(result.getResponse().getContentAsString());
    assertThat(queryPort.calls).isEqualTo(1);
  }

  @Test
  void nonClientAndAutomationRolesCannotUseApiLayerToExtractCandidateData() throws Exception {
    for (String role : List.of("consultant", "owner", "admin", "system", "ai_assistant")) {
      queryPort.reset();

      MvcResult result = mockMvc.perform(get(ENDPOINT)
              .with(authentication(auth(role)))
              )
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.safeReason").value("access_denied_by_default"))
          .andReturn();

      assertSanitizedApiBody(result.getResponse().getContentAsString());
      assertThat(queryPort.calls).as(role).isZero();
    }
  }

  @Test
  void notFoundReturnsSanitizedApiErrorEnvelope() throws Exception {
    queryPort.nextCard = Optional.empty();

    MvcResult result = mockMvc.perform(get(ENDPOINT)
            .with(authentication(auth("client")))
            )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.errorCode").value("not_found"))
        .andExpect(jsonPath("$.error.safeReason")
            .value("client_safe_candidate_card_unavailable"))
        .andExpect(jsonPath("$.error.safeMessage")
            .value("Client-safe candidate card is unavailable."))
        .andReturn();

    assertThat(queryPort.calls).isEqualTo(1);
    assertSanitizedApiBody(result.getResponse().getContentAsString());
    assertFailureEnvelopeHasNoData(result.getResponse().getContentAsString());
  }

  @Test
  void internalErrorReturnsSanitizedApiErrorEnvelopeWithoutThrowableDetails() throws Exception {
    queryPort.failure = new IllegalStateException(
        "failed for " + RAW_CANDIDATE_ID + " " + FULL_NAME + " " + EMAIL + " "
            + PHONE + " " + LINKEDIN_URL + " at "
            + "com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileService");

    MvcResult result = mockMvc.perform(get(ENDPOINT)
            .with(authentication(auth("client")))
            )
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.errorCode").value("internal_error"))
        .andExpect(jsonPath("$.error.safeReason").value("request_failed"))
        .andExpect(jsonPath("$.error.safeMessage").value("Request failed."))
        .andReturn();

    assertThat(queryPort.calls).isEqualTo(1);
    assertSanitizedApiBody(result.getResponse().getContentAsString());
    assertFailureEnvelopeHasNoData(result.getResponse().getContentAsString());
  }

  @Test
  void apiErrorDtosSanitizePiiStackTracesInternalPackagesAndOperationalDetails() {
    for (String unsafeExternalText : unsafeExternalTextExamples()) {
      ApiErrorResponse error = new ApiErrorResponse(
          "internal_error",
          "request_failed",
          unsafeExternalText);
      ApiValidationErrorResponse validation = ApiValidationErrorResponse.of(
          "invalid_anonymous_card_reference",
          List.of(unsafeExternalText));
      ApiAccessDeniedResponse denied = ApiAccessDeniedResponse.from(new AccessDeniedException(
          new AccessDecision(false, "unsafe_context_denied", unsafeExternalText)));

      assertThat(error.safeMessage()).as(unsafeExternalText).isEqualTo("Request failed.");
      assertThat(validation.validationMessages()).as(unsafeExternalText)
          .containsExactly("Invalid request.");
      assertThat(denied.safeExplanation()).as(unsafeExternalText).isEqualTo("Access denied.");
    }
  }

  @Test
  void clientSafeCandidateCardResponseRejectsUnsafeDisplayText() {
    for (String unsafeText : unsafeExternalTextExamples()) {
      assertThatThrownBy(() -> new ClientSafeCandidateCardResponse(
          "card_task9c_unsafe",
          "alias-aa",
          "projection-v1",
          "l2_client_safe",
          "Senior verification leader in advanced-chip programs",
          "semiconductor_verification",
          "senior_ic",
          "greater_china",
          unsafeText,
          "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
          List.of("Evidence generalized from approved profile signals."),
          List.of("Strong fit based on generalized capability evidence.")))
          .as(unsafeText)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("safeSummary must not contain unsafe API-visible text");
    }
  }

  @Test
  void apiPublicDtoControllerFacadePortAndMapperSurfacesExposeNoRawInternalTypes()
      throws IOException {
    Set<Class<?>> forbiddenTypes = forbiddenApiTypes();

    for (Class<?> dto : Set.of(
        ApiResponseEnvelope.class,
        ApiErrorResponse.class,
        ApiAccessDeniedResponse.class,
        ApiValidationErrorResponse.class,
        ClientSafeCandidateCardResponse.class)) {
      assertThat(dto.isRecord()).as(dto.getSimpleName()).isTrue();
      assertThat(dto.getRecordComponents())
          .as(dto.getSimpleName())
          .extracting(RecordComponent::getType)
          .doesNotContainAnyElementsOf(forbiddenTypes);
      assertThat(dto.getRecordComponents())
          .as(dto.getSimpleName())
          .extracting(RecordComponent::getName)
          .noneMatch(ApiBoundaryRegressionClosureTest::isForbiddenApiSurfaceName);
    }

    for (Method method : ClientSafeCandidateCardController.class.getDeclaredMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getReturnType()).isEqualTo(ResponseEntity.class);
      assertThat(method.getGenericReturnType().getTypeName())
          .contains("ApiResponseEnvelope");
      assertThat(method.getGenericReturnType().getTypeName())
          .contains("ApiSafeResponseBody");
      assertThat(method.getGenericReturnType().getTypeName())
          .doesNotContain("CandidateProfile")
          .doesNotContain("CandidateId");
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenTypes);
    }

    for (Method method : ClientSafeCandidateCardApiQueryService.class.getDeclaredMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getGenericReturnType().getTypeName())
          .contains("ClientSafeCandidateCardResponse")
          .doesNotContain("CandidateProfile")
          .doesNotContain("CandidateId");
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenTypes);
    }

    for (Method method : ClientSafeCandidateCardQueryPort.class.getDeclaredMethods()) {
      assertThat(method.getGenericReturnType().getTypeName())
          .contains("ClientSafeCandidateCard")
          .doesNotContain("CandidateProfile")
          .doesNotContain("CandidateId");
      assertThat(method.getParameterTypes()).containsExactly(
          ClientSafeCandidateCardQueryScope.class,
          AnonymousCandidateCardId.class);
    }

    for (Method method : ClientSafeCandidateCardResponseMapper.class.getDeclaredMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getReturnType()).isEqualTo(ClientSafeCandidateCardResponse.class);
      assertThat(method.getParameterTypes()).containsExactly(ClientSafeCandidateCard.class);
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenTypes);
    }

    for (Path file : apiBoundaryProductionFiles()) {
      String source = Files.readString(file);
      boolean isConsultantIntakeController =
          "ConsultantIntakeController.java".equals(file.getFileName().toString());
      boolean isConsultantIntakeQueueSurface =
          "ConsultantIntakeQueueQueryService.java".equals(file.getFileName().toString());
      boolean isConsultantCandidateSurface =
          file.getFileName().toString().contains("ConsultantCandidate");
      boolean isWorkflowSafeSurface =
          "ApiBoundaryContractRules.java".equals(file.getFileName().toString())
              || "ApiSafeResponseBody.java".equals(file.getFileName().toString())
              || file.getFileName().toString().contains("ConsultantWorkflow")
              || "ConsultantAuditDrawerResponse.java".equals(file.getFileName().toString());
      if (!isConsultantCandidateSurface) {
        assertThat(source)
            .as(file.toString())
            .doesNotContain("import com.recruitingtransactionos.coreapi.candidateprofile");
      }
      if (!isConsultantIntakeController && !isConsultantIntakeQueueSurface) {
        var assertion = assertThat(source)
            .as(file.toString())
            .doesNotContain("SourceItem")
            .doesNotContain("InformationPacket")
            .doesNotContain("ClaimLedgerItem")
            .doesNotContain("ReviewEvent");
        if (!isWorkflowSafeSurface) {
          assertion.doesNotContain("WorkflowEvent");
        }
      }
    }
  }

  @Test
  void endpointSurfaceContainsNoBroadRawDisclosureUnlockOrConsentApi() throws IOException {
    List<Path> controllerFiles = productionControllerFiles();

    assertThat(controllerFiles)
        .extracting(path -> path.getFileName().toString())
        .containsExactlyInAnyOrder(
            "ClientSafeCandidateCardController.java",
            "ClientCompanyController.java",
            "ClientDashboardController.java",
            "ClientJobController.java",
            "ClientShortlistController.java",
            "HealthController.java",
            "ConsultantCandidateController.java",
            "ConsultantCompanyController.java",
            "ConsultantDashboardController.java",
            "ConsultantJobController.java",
            "ConsultantIntakeController.java",
            "ConsultantMatchingController.java",
            "ConsultantShortlistController.java",
            "ConsultantDocumentController.java",
            "ConsultantFollowUpController.java",
            "ConsultantWorkflowController.java",
        "AuthenticationController.java");

    for (Path controllerFile : controllerFiles) {
      String source = Files.readString(controllerFile);
      String fileName = controllerFile.getFileName().toString();
      assertThat(source)
          .as(controllerFile.toString())
          .doesNotContain("/api/candidates")
          .doesNotContain("/api/candidate-profiles")
          .doesNotContain("/api/profiles")
          .doesNotContain("/api/consent")
          .doesNotContain("/api/consents")
          .doesNotContain("/api/disclosure")
          .doesNotContain("/api/disclosures")
          .doesNotContain("/api/unlock")
          .doesNotContain("/api/unlocks")
          .doesNotContain("/api/admin")
          .doesNotContain("{candidateId}")
          .doesNotContain("{candidateProfileId}")
          .doesNotContain("ResourceType.CANDIDATE_PROFILE");
      if (!"ConsultantCandidateController.java".equals(fileName)) {
        assertThat(source)
            .as(controllerFile.toString())
            .doesNotContain("ResourceType.CANDIDATE");
      }

      // Allow @PostMapping on consultant write controllers and document controller
      boolean isConsultantWriteController =
          "ConsultantCompanyController.java".equals(fileName)
              || "ConsultantJobController.java".equals(fileName)
              || "ConsultantMatchingController.java".equals(fileName)
              || "ConsultantShortlistController.java".equals(fileName);
      boolean isConsultantIntakeController =
          "ConsultantIntakeController.java".equals(fileName);
      boolean isDocumentController =
          "ConsultantDocumentController.java".equals(fileName);
      boolean isClientWriteController =
          "ClientCompanyController.java".equals(fileName)
              || "ClientJobController.java".equals(fileName)
              || "ClientShortlistController.java".equals(fileName);
      boolean allowsPutMapping =
          isConsultantWriteController || "ClientCompanyController.java".equals(fileName);

      boolean isAuthenticationController =
          "AuthenticationController.java".equals(fileName);

      if (!isConsultantWriteController
          && !isClientWriteController
          && !isConsultantIntakeController
          && !isDocumentController
          && !isAuthenticationController) {
        assertThat(source)
            .as(controllerFile.toString())
            .doesNotContain("@PostMapping");
      }

      // Allow @PutMapping on consultant write controllers and the client preference/profile surface.
      if (!allowsPutMapping) {
        assertThat(source)
            .as(controllerFile.toString())
            .doesNotContain("@PutMapping");
      }

      assertThat(source)
          .as(controllerFile.toString())
          .doesNotContain("@PatchMapping")
          .doesNotContain("@DeleteMapping");
    }

    String apiControllerSource = Files.readString(projectPath(
        "src/main/java/com/recruitingtransactionos/coreapi/apiboundary/"
            + "ClientSafeCandidateCardController.java"));
    assertThat(apiControllerSource)
        .contains("@RequestMapping(\"/api/client-safe/candidate-cards\")")
        .contains("@GetMapping(\"/{anonymousCardRef}\")");
  }

  private void assertSuccessEnvelopeHasOnlyClientSafeCardResponse(String body) throws IOException {
    JsonNode root = objectMapper.readTree(body);
    assertThat(root.fieldNames()).toIterable()
        .containsExactlyInAnyOrder("data", "error");
    assertThat(root.get("error").isNull()).isTrue();
    assertThat(root.get("data").fieldNames()).toIterable()
        .containsExactlyInAnyOrder(
            "anonymousCardRef",
            "clientAlias",
            "projectionVersion",
            "redactionLevel",
            "generalizedHeadline",
            "generalizedRoleFamily",
            "generalizedSeniorityBand",
            "generalizedLocationRegion",
            "safeSummary",
            "safeSkillSummary",
            "safeEvidenceSummaries",
            "safeMatchNarratives");
    assertSanitizedApiBody(body);
  }

  private void assertFailureEnvelopeHasNoData(String body) throws IOException {
    JsonNode root = objectMapper.readTree(body);
    assertThat(root.fieldNames()).toIterable()
        .containsExactlyInAnyOrder("data", "error");
    assertThat(root.get("data").isNull()).isTrue();
    assertThat(root.get("error").isObject()).isTrue();
  }

  private static void assertSanitizedApiBody(String body) {
    assertThat(body)
        .doesNotContain(
            RAW_CANDIDATE_ID,
            RAW_CANDIDATE_PROFILE_ID,
            FULL_NAME,
            EMAIL,
            PHONE,
            LINKEDIN_URL,
            RAW_SOURCE_TEXT,
            RAW_CV_TEXT,
            CONSULTANT_NOTES,
            EXACT_EMPLOYER,
            EXACT_PROJECT,
            EXACT_PRODUCT,
            EXACT_CHIP,
            "candidateId",
            "candidateProfileId",
            "rawCandidate",
            "rawProfile",
            "fullName",
            "email",
            "phone",
            "linkedin",
            "rawSourceText",
            "rawCvText",
            "consultantNotes",
            "identityDisclosed",
            "CandidateProfile",
            "CandidateProfileService",
            "SourceItem",
            "InformationPacket",
            "ClaimLedgerItem",
            "ReviewEvent",
            "WorkflowEvent",
            "com.recruitingtransactionos",
            "java.",
            "stack trace",
            "stacktrace",
            "\tat ",
            "Exception");
  }

  private static Set<Class<?>> forbiddenApiTypes() {
    return Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class,
        SourceItem.class,
        InformationPacket.class,
        ClaimId.class,
        ReviewEventId.class,
        WorkflowEventId.class);
  }

  private static boolean isForbiddenApiSurfaceName(String name) {
    String lower = name.toLowerCase();
    return lower.contains("candidateid")
        || lower.contains("candidateprofileid")
        || lower.contains("rawcandidate")
        || lower.contains("rawprofile")
        || lower.contains("fullname")
        || lower.contains("email")
        || lower.contains("phone")
        || lower.contains("linkedin")
        || lower.contains("rawsource")
        || lower.contains("rawcv")
        || lower.contains("consultantnote")
        || lower.contains("sourceitem")
        || lower.contains("informationpacket")
        || lower.contains("claimledger")
        || lower.contains("reviewevent")
        || lower.contains("workflowevent")
        || lower.contains("stacktrace")
        || lower.contains("throwable")
        || lower.contains("exception")
        || lower.contains("identitydisclosed")
        || lower.contains("disclosure");
  }

  private static List<String> unsafeExternalTextExamples() {
    return List.of(
        FULL_NAME,
        EMAIL,
        PHONE,
        LINKEDIN_URL,
        RAW_SOURCE_TEXT,
        RAW_CV_TEXT,
        CONSULTANT_NOTES,
        EXACT_EMPLOYER,
        EXACT_PROJECT,
        EXACT_PRODUCT,
        EXACT_CHIP,
        "java.lang.IllegalStateException at "
            + "com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileService");
  }

  private static List<Path> apiBoundaryProductionFiles() throws IOException {
    Path root = projectPath("src/main/java/com/recruitingtransactionos/coreapi/apiboundary");
    try (Stream<Path> stream = Files.walk(root)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  private static List<Path> productionControllerFiles() throws IOException {
    Path root = projectPath("src/main/java/com/recruitingtransactionos/coreapi");
    try (Stream<Path> stream = Files.walk(root)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
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

  private static ClientSafeCandidateCard safeCard() {
    return new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of("card_task9c_0001"),
        AnonymousCandidateRef.of("anon_candidate_task9c_0001"),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led complex verification programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence generalized from approved profile signals."),
        List.of("Strong fit based on generalized capability evidence."));
  }

  private static Authentication auth(String role) {
    return new RtoAuthenticationToken(principal(role, UUID.fromString(ORGANIZATION_ID)));
  }

  private static RtoAuthenticatedPrincipal principal(String role, UUID organizationId) {
    PortalRole portalRole = PortalRole.UNKNOWN;
    for (PortalRole r : PortalRole.values()) {
        if (r.wireValue().equals(role)) {
            portalRole = r;
            break;
        }
    }
    if (portalRole == PortalRole.UNKNOWN && "client".equals(role)) {
        portalRole = PortalRole.CLIENT;
    }
    if (portalRole == PortalRole.UNKNOWN && "consultant".equals(role)) {
        portalRole = PortalRole.CONSULTANT;
    }
    return new RtoAuthenticatedPrincipal(
        UUID.fromString(USER_ACCOUNT_ID),
        organizationId,
        portalRole,
        "Test User",
        UUID.fromString(USER_ACCOUNT_ID)
    );
  }

  private static String validBearerToken(RtoAuthenticatedPrincipal principal, Instant issuedAt) {
    return "Bearer " + new JwtService(
        "0123456789abcdef0123456789abcdef",
        "test",
        1800,
        604800).issueAccessToken(principal, issuedAt);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    RecordingClientSafeCandidateCardQueryPort recordingClientSafeCandidateCardQueryPort() {
      return new RecordingClientSafeCandidateCardQueryPort();
    }

    @Bean
    ClientSafeCandidateCardApiQueryService clientSafeCandidateCardApiQueryService(
        RecordingClientSafeCandidateCardQueryPort queryPort) {
      return new ClientSafeCandidateCardApiQueryService(queryPort);
    }
  }

  static final class RecordingClientSafeCandidateCardQueryPort
      implements ClientSafeCandidateCardQueryPort {

    private int calls;
    private ClientSafeCandidateCardQueryScope lastScope;
    private AnonymousCandidateCardId lastCardId;
    private Optional<ClientSafeCandidateCard> nextCard = Optional.of(safeCard());
    private RuntimeException failure;

    @Override
    public Optional<ClientSafeCandidateCard> findByAnonymousCardId(
        ClientSafeCandidateCardQueryScope scope,
        AnonymousCandidateCardId cardId) {
      calls++;
      lastScope = scope;
      lastCardId = cardId;
      if (failure != null) {
        throw failure;
      }
      return nextCard;
    }

    private void reset() {
      calls = 0;
      lastScope = null;
      lastCardId = null;
      nextCard = Optional.of(safeCard());
      failure = null;
    }
  }
}
