package com.recruitingtransactionos.coreapi.apiboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ClientSafeCandidateCardController.class)
@Import(ApiBoundaryRegressionClosureTest.TestConfig.class)
class ApiBoundaryRegressionClosureTest {

  private static final String ENDPOINT =
      "/api/client-safe/candidate-cards/card_task9c_0001";
  private static final String ROLE_HEADER = "X-RTO-Actor-Role";
  private static final String FIELD_HEADER = "X-RTO-Field-Classification";
  private static final String IDENTITY_DISCLOSURE_HEADER =
      "X-RTO-Identity-Disclosure-Requested";
  private static final String ORGANIZATION_ID_HEADER = "X-RTO-Organization-Id";
  private static final String ORGANIZATION_ID =
      "00000000-0000-0000-0000-0000009c0003";
  private static final String CLIENT_PORTAL_ORGANIZATION_ID =
      "00000000-0000-0000-0000-00000013b001";

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

  @BeforeEach
  void resetQueryPort() {
    queryPort.reset();
  }

  @Test
  void requestPathUsesAnonymousCardRefOnly() throws Exception {
    mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe")
            .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anonymousCardRef").value("card_task9c_0001"));

    assertThat(queryPort.calls).isEqualTo(1);
    assertThat(queryPort.lastScope).isEqualTo(
        ClientSafeCandidateCardQueryScope.of(java.util.UUID.fromString(ORGANIZATION_ID)));
    assertThat(queryPort.lastCardId)
        .isEqualTo(AnonymousCandidateCardId.of("card_task9c_0001"));
  }

  @Test
  void mergedClientPortalFetchHelperSendsTemporaryOrganizationScopeHeader()
      throws IOException {
    String source = Files.readString(Path.of(System.getProperty("user.dir"))
        .getParent()
        .getParent()
        .resolve("apps/web/src/api/clientSafeCandidateCards.ts"));

    assertThat(source)
        .contains("\"X-RTO-Actor-Role\": \"client\"")
        .contains("\"X-RTO-Field-Classification\": \"client_safe\"")
        .contains("\"X-RTO-Identity-Disclosure-Requested\": \"false\"")
        .contains("\"X-RTO-Organization-Id\": CLIENT_PORTAL_ORGANIZATION_ID");
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
              .header(ROLE_HEADER, "client")
              .header(FIELD_HEADER, "client_safe")
              .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
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
  void temporaryAccessContextAdapterFailsClosedForMissingUnknownOrUnsupportedHeaders()
      throws Exception {
    MvcResult missing = mockMvc.perform(get(ENDPOINT))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("api_access_context_required"))
        .andReturn();
    assertSanitizedApiBody(missing.getResponse().getContentAsString());

    MvcResult missingOrganization = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("api_access_context_required"))
        .andReturn();
    assertSanitizedApiBody(missingOrganization.getResponse().getContentAsString());

    for (String role : List.of("owner-plus", "client")) {
      queryPort.reset();
      String field = role.equals("client") ? "identity" : "client_safe";
      MvcResult invalid = mockMvc.perform(get(ENDPOINT)
              .header(ROLE_HEADER, role)
              .header(FIELD_HEADER, field)
              .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.safeReason").value("api_access_context_invalid"))
          .andReturn();

      assertSanitizedApiBody(invalid.getResponse().getContentAsString());
      assertThat(queryPort.calls).isZero();
    }

    MvcResult malformedDisclosure = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe")
            .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID)
            .header(IDENTITY_DISCLOSURE_HEADER, "yes"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("api_access_context_invalid"))
        .andReturn();

    assertSanitizedApiBody(malformedDisclosure.getResponse().getContentAsString());
    assertThat(queryPort.calls).isZero();

    MvcResult malformedOrganization = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe")
            .header(ORGANIZATION_ID_HEADER, "not-a-uuid"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("api_access_context_invalid"))
        .andReturn();

    assertSanitizedApiBody(malformedOrganization.getResponse().getContentAsString());
    assertThat(queryPort.calls).isZero();
  }

  @Test
  void clientAccessIsLimitedToSafeOrGeneralizedCardOutputWithoutL4Disclosure()
      throws Exception {
    for (String allowedField : List.of("client_safe", "generalized")) {
      queryPort.reset();
      MvcResult result = mockMvc.perform(get(ENDPOINT)
              .header(ROLE_HEADER, "client")
              .header(FIELD_HEADER, allowedField)
              .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.redactionLevel").value("l2_client_safe"))
          .andReturn();

      assertSuccessEnvelopeHasOnlyClientSafeCardResponse(result.getResponse().getContentAsString());
      assertThat(queryPort.calls).isEqualTo(1);
    }

    for (String unsafeField : List.of("pii", "raw_source", "consultant_private",
        "consent_disclosure", "system_governance")) {
      queryPort.reset();
      MvcResult result = mockMvc.perform(get(ENDPOINT)
              .header(ROLE_HEADER, "client")
              .header(FIELD_HEADER, unsafeField)
              .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.safeReason").value("client_unsafe_field_denied"))
          .andReturn();

      assertSanitizedApiBody(result.getResponse().getContentAsString());
      assertThat(queryPort.calls).isZero();
    }

    MvcResult identityDisclosure = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe")
            .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID)
            .header(IDENTITY_DISCLOSURE_HEADER, "true"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("identity_disclosure_not_implemented"))
        .andReturn();

    assertSanitizedApiBody(identityDisclosure.getResponse().getContentAsString());
    assertThat(queryPort.calls).isZero();
  }

  @Test
  void nonClientAndAutomationRolesCannotUseApiLayerToExtractCandidateData() throws Exception {
    for (String role : List.of("consultant", "owner", "admin", "system", "ai_assistant")) {
      queryPort.reset();

      MvcResult result = mockMvc.perform(get(ENDPOINT)
              .header(ROLE_HEADER, role)
              .header(FIELD_HEADER, "client_safe")
              .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
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
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe")
            .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
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
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe")
            .header(ORGANIZATION_ID_HEADER, ORGANIZATION_ID))
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
          "anon_candidate_task9c_unsafe",
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
      assertThat(source)
          .as(file.toString())
          .doesNotContain("import com.recruitingtransactionos.coreapi.candidateprofile")
          .doesNotContain("SourceItem")
          .doesNotContain("InformationPacket")
          .doesNotContain("ClaimLedgerItem")
          .doesNotContain("ReviewEvent")
          .doesNotContain("WorkflowEvent");
    }
  }

  @Test
  void endpointSurfaceContainsNoBroadRawDisclosureUnlockOrConsentApi() throws IOException {
    List<Path> controllerFiles = productionControllerFiles();

    assertThat(controllerFiles)
        .extracting(path -> path.getFileName().toString())
        .containsExactly(
            "ClientSafeCandidateCardController.java",
            "HealthController.java");

    for (Path controllerFile : controllerFiles) {
      String source = Files.readString(controllerFile);
      assertThat(source)
          .as(controllerFile.toString())
          .doesNotContain("@PostMapping")
          .doesNotContain("@PutMapping")
          .doesNotContain("@PatchMapping")
          .doesNotContain("@DeleteMapping")
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
          .doesNotContain("ResourceType.CANDIDATE_PROFILE")
          .doesNotContain("ResourceType.CANDIDATE");
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
            "anonymousCandidateRef",
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
