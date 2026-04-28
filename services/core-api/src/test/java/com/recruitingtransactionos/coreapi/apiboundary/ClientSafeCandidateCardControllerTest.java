package com.recruitingtransactionos.coreapi.apiboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateCardId;
import com.recruitingtransactionos.coreapi.clientsafeprojection.AnonymousCandidateRef;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateCard;
import com.recruitingtransactionos.coreapi.clientsafeprojection.RedactionLevel;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
@Import(ClientSafeCandidateCardControllerTest.TestConfig.class)
class ClientSafeCandidateCardControllerTest {

  private static final String ENDPOINT =
      "/api/client-safe/candidate-cards/card_task9b_0001";
  private static final String ROLE_HEADER = "X-RTO-Actor-Role";
  private static final String FIELD_HEADER = "X-RTO-Field-Classification";
  private static final String IDENTITY_DISCLOSURE_HEADER =
      "X-RTO-Identity-Disclosure-Requested";

  private static final String RAW_CANDIDATE_ID =
      "00000000-0000-0000-0000-0000009b0001";
  private static final String RAW_CANDIDATE_PROFILE_ID =
      "00000000-0000-0000-0000-0000009b0002";
  private static final String FULL_NAME = "Jane Task9B Candidate";
  private static final String EMAIL = "jane.task9b@example.com";
  private static final String PHONE = "+86 138 0000 9B9B";
  private static final String LINKEDIN_URL = "https://www.linkedin.com/in/jane-task9b";
  private static final String EXACT_EMPLOYER = "NebulaChip Systems";
  private static final String EXACT_PROJECT = "Orion-X7 NPU";
  private static final String EXACT_PRODUCT = "VectorSlate PCIe Gen6 Switch";
  private static final String EXACT_CHIP = "NC-9000";
  private static final String RAW_SOURCE_TEXT =
      "Jane Task9B Candidate led Orion-X7 NPU verification at NebulaChip Systems.";
  private static final String CONSULTANT_NOTES =
      "Do not tell the client that Jane is negotiating with HelioSemi.";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private RecordingClientSafeCandidateCardQueryPort queryPort;

  @BeforeEach
  void resetQueryPort() {
    queryPort.reset();
  }

  @Test
  void successfulControllerResponseReturnsOnlyClientSafeDtoEnvelope() throws Exception {
    MvcResult result = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.anonymousCardRef").value("card_task9b_0001"))
        .andExpect(jsonPath("$.data.anonymousCandidateRef")
            .value("anon_candidate_task9b_0001"))
        .andExpect(jsonPath("$.data.redactionLevel").value("l2_client_safe"))
        .andExpect(jsonPath("$.data.generalizedHeadline")
            .value("Senior verification leader in advanced-chip programs"))
        .andExpect(jsonPath("$.data.safeEvidenceSummaries[0]")
            .value("Evidence generalized from approved profile signals."))
        .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body)
        .doesNotContain(
            RAW_CANDIDATE_ID,
            RAW_CANDIDATE_PROFILE_ID,
            FULL_NAME,
            EMAIL,
            PHONE,
            LINKEDIN_URL,
            EXACT_EMPLOYER,
            EXACT_PROJECT,
            EXACT_PRODUCT,
            EXACT_CHIP,
            RAW_SOURCE_TEXT,
            CONSULTANT_NOTES,
            "candidateId",
            "candidateProfileId",
            "fullName",
            "email",
            "phone",
            "linkedin",
            "rawSourceText",
            "consultantNotes",
            "identityDisclosed",
            "SourceItem",
            "InformationPacket",
            "ClaimLedgerItem",
            "ReviewEvent",
            "WorkflowEvent");
    assertThat(queryPort.calls).isEqualTo(1);
    assertThat(queryPort.lastCardId).isEqualTo(AnonymousCandidateCardId.of("card_task9b_0001"));
  }

  @Test
  void missingAccessContextFailsClosedWithoutQueryingCard() throws Exception {
    MvcResult result = mockMvc.perform(get(ENDPOINT))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"))
        .andExpect(jsonPath("$.error.safeReason").value("api_access_context_required"))
        .andExpect(jsonPath("$.error.safeMessage").value("Access context is required."))
        .andReturn();

    assertSanitizedDenial(result);
    assertThat(queryPort.calls).isZero();
  }

  @Test
  void clientDeniedPathIsSanitizedAndDoesNotLeakInternalDetails() throws Exception {
    MvcResult result = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "raw_source"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.errorCode").value("access_denied"))
        .andExpect(jsonPath("$.error.safeReason").value("client_unsafe_field_denied"))
        .andReturn();

    assertSanitizedDenial(result);
    assertThat(queryPort.calls).isZero();
  }

  @Test
  void nonClientOrIdentityDisclosedAccessCannotUseEndpointToGetInternalData() throws Exception {
    MvcResult nonClientResult = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "consultant")
            .header(FIELD_HEADER, "client_safe"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("access_denied_by_default"))
        .andReturn();

    MvcResult disclosureAttemptResult = mockMvc.perform(get(ENDPOINT)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe")
            .header(IDENTITY_DISCLOSURE_HEADER, "true"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.safeReason").value("identity_disclosure_not_implemented"))
        .andReturn();

    assertSanitizedDenial(nonClientResult);
    assertSanitizedDenial(disclosureAttemptResult);
    assertThat(queryPort.calls).isZero();
  }

  @Test
  void rawUuidPathIsRejectedAsNonAnonymousCardReference() throws Exception {
    MvcResult result = mockMvc.perform(get(
            "/api/client-safe/candidate-cards/" + RAW_CANDIDATE_ID)
            .header(ROLE_HEADER, "client")
            .header(FIELD_HEADER, "client_safe"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.errorCode").value("validation_failed"))
        .andExpect(jsonPath("$.error.safeReason")
            .value("invalid_anonymous_card_reference"))
        .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body)
        .doesNotContain(
            RAW_CANDIDATE_ID,
            RAW_CANDIDATE_PROFILE_ID,
            "CandidateProfile",
            "com.recruitingtransactionos",
            "stacktrace",
            "Exception");
    assertThat(queryPort.calls).isZero();
  }

  @Test
  void controllerAndFacadeOutputTypesDoNotExposeRawOrGovernanceTypes() {
    Set<Class<?>> forbiddenTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class,
        SourceItem.class,
        InformationPacket.class,
        ClaimId.class,
        ReviewEventId.class,
        WorkflowEventId.class);

    for (Method method : ClientSafeCandidateCardController.class.getDeclaredMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getReturnType()).isEqualTo(ResponseEntity.class);
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenTypes);
    }

    for (Method method : ClientSafeCandidateCardApiQueryService.class.getDeclaredMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getReturnType()).isIn(
          ClientSafeCandidateCardResponse.class,
          Optional.class);
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenTypes);
    }

    for (Method method : ClientSafeCandidateCardQueryPort.class.getDeclaredMethods()) {
      assertThat(method.getReturnType()).isEqualTo(Optional.class);
      assertThat(method.getGenericReturnType().getTypeName())
          .contains(ClientSafeCandidateCard.class.getSimpleName());
      assertThat(method.getParameterTypes()).containsExactly(AnonymousCandidateCardId.class);
    }
  }

  @Test
  void facadeNeverMapsRawCandidateProfileDirectlyIntoApiResponse() throws IOException {
    Set<Path> apiBoundaryFiles = Set.of(
        projectPath(
            "src/main/java/com/recruitingtransactionos/coreapi/apiboundary/"
                + "ClientSafeCandidateCardApiQueryService.java"),
        projectPath(
            "src/main/java/com/recruitingtransactionos/coreapi/apiboundary/"
                + "ClientSafeCandidateCardController.java"),
        projectPath(
            "src/main/java/com/recruitingtransactionos/coreapi/apiboundary/"
                + "ClientSafeCandidateCardQueryPort.java"));

    for (Path file : apiBoundaryFiles) {
      String source = Files.readString(file);
      assertThat(source)
          .doesNotContain("import com.recruitingtransactionos.coreapi.candidateprofile")
          .doesNotContain("CandidateProfile")
          .doesNotContain("CandidateId")
          .doesNotContain("SourceItem")
          .doesNotContain("InformationPacket")
          .doesNotContain("ClaimLedgerItem")
          .doesNotContain("ReviewEvent")
          .doesNotContain("WorkflowEvent");
    }

    for (Method method : ClientSafeCandidateCardResponseMapper.class.getDeclaredMethods()) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getParameterTypes()).containsExactly(ClientSafeCandidateCard.class);
      assertThat(method.getReturnType()).isEqualTo(ClientSafeCandidateCardResponse.class);
    }
  }

  @Test
  void noRawCandidateOrCandidateProfileControllerEndpointsExist() throws IOException {
    List<Path> controllerFiles = productionControllerFiles();

    assertThat(controllerFiles)
        .extracting(path -> path.getFileName().toString())
        .contains("HealthController.java", "ClientSafeCandidateCardController.java")
        .doesNotContain(
            "CandidateController.java",
            "CandidateProfileController.java",
            "RawCandidateController.java",
            "RawCandidateProfileController.java");

    for (Path controllerFile : controllerFiles) {
      String source = Files.readString(controllerFile);
      assertThat(source)
          .doesNotContain("/api/candidates/{candidateId}")
          .doesNotContain("/api/candidate-profiles")
          .doesNotContain("{candidateId}")
          .doesNotContain("{candidateProfileId}")
          .doesNotContain("ResourceType.CANDIDATE_PROFILE")
          .doesNotContain("ResourceType.CANDIDATE");
    }
  }

  private static void assertSanitizedDenial(MvcResult result) throws Exception {
    String body = result.getResponse().getContentAsString();
    assertThat(body)
        .doesNotContain(
            RAW_CANDIDATE_ID,
            RAW_CANDIDATE_PROFILE_ID,
            FULL_NAME,
            EMAIL,
            PHONE,
            LINKEDIN_URL,
            EXACT_EMPLOYER,
            EXACT_PROJECT,
            EXACT_PRODUCT,
            EXACT_CHIP,
            RAW_SOURCE_TEXT,
            CONSULTANT_NOTES,
            "CandidateProfileService",
            "com.recruitingtransactionos",
            "java.",
            "stack trace",
            "stacktrace",
            "\tat ",
            "Exception");
  }

  private static ClientSafeCandidateCard safeCard() {
    return new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of("card_task9b_0001"),
        AnonymousCandidateRef.of("anon_candidate_task9b_0001"),
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
    private AnonymousCandidateCardId lastCardId;
    private Optional<ClientSafeCandidateCard> nextCard = Optional.of(safeCard());

    @Override
    public Optional<ClientSafeCandidateCard> findByAnonymousCardId(
        AnonymousCandidateCardId cardId) {
      calls++;
      lastCardId = cardId;
      return nextCard;
    }

    private void reset() {
      calls = 0;
      lastCardId = null;
      nextCard = Optional.of(safeCard());
    }
  }
}
