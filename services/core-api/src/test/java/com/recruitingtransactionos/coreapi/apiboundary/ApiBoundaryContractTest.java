package com.recruitingtransactionos.coreapi.apiboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventAppendCommand;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApiBoundaryContractTest {

  private static final Set<Class<?>> FORBIDDEN_API_DTO_TYPES = Set.of(
      CandidateId.class,
      CandidateProfile.class,
      CandidateProfileId.class,
      SourceItem.class,
      InformationPacket.class,
      ClaimId.class,
      ClaimLedgerAppendCommand.class,
      ReviewEventAppendCommand.class,
      ReviewEventId.class,
      WorkflowEventAppendCommand.class,
      WorkflowEventId.class);

  @Test
  void clientSafeCandidateCardResponseMapsOnlyFromClientSafeCard() {
    ClientSafeCandidateCard card = clientSafeCard();

    ClientSafeCandidateCardResponse response =
        ClientSafeCandidateCardResponseMapper.from(card);

    assertThat(response.anonymousCardRef()).isEqualTo("card_api_20260428_0001");
    assertThat(response.anonymousCandidateRef()).isEqualTo("anon_candidate_api_20260428_0001");
    assertThat(response.projectionVersion()).isEqualTo("projection-v1");
    assertThat(response.redactionLevel()).isEqualTo("l2_client_safe");
    assertThat(response.generalizedHeadline())
        .isEqualTo("Senior verification leader in advanced-chip programs");
    assertThat(response.generalizedRoleFamily()).isEqualTo("semiconductor_verification");
    assertThat(response.generalizedSeniorityBand()).isEqualTo("senior_ic");
    assertThat(response.generalizedLocationRegion()).isEqualTo("greater_china");
    assertThat(response.safeSummary())
        .doesNotContain("Jane Candidate", "jane@example.com", "linkedin.com/in/jane-candidate");
    assertThat(response.safeEvidenceSummaries())
        .containsExactly("Evidence summary withheld pending redaction pipeline.");
    assertThat(response.safeMatchNarratives())
        .containsExactly("Match narrative withheld pending shortlist generator.");
  }

  @Test
  void clientSafeCandidateCardResponseContainsOnlyAllowedApiSafeFields() {
    assertThat(ClientSafeCandidateCardResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
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

    for (RecordComponent component : ClientSafeCandidateCardResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedClientSafeCandidateCardResponseField(
          component.getName())).as(component.getName()).isTrue();
    }
  }

  @Test
  void clientSafeCandidateCardResponseDoesNotRepresentRawIdsPiiRawSourceAuditOrL4Fields() {
    assertThat(ClientSafeCandidateCardResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .noneMatch(name -> name.equals("candidateId"))
        .noneMatch(name -> name.equals("candidateProfileId"))
        .noneMatch(name -> name.contains("rawCandidate"))
        .noneMatch(name -> name.contains("rawProfile"))
        .noneMatch(name -> name.contains("fullName"))
        .noneMatch(name -> name.contains("email"))
        .noneMatch(name -> name.contains("phone"))
        .noneMatch(name -> name.contains("linkedin"))
        .noneMatch(name -> name.contains("sourceItem"))
        .noneMatch(name -> name.contains("informationPacket"))
        .noneMatch(name -> name.contains("claimLedger"))
        .noneMatch(name -> name.contains("reviewEvent"))
        .noneMatch(name -> name.contains("workflowEvent"))
        .noneMatch(name -> name.contains("consultantNotes"))
        .noneMatch(name -> name.contains("identityDisclosed"))
        .noneMatch(name -> name.contains("disclosure"));

    assertThatThrownBy(() -> new ClientSafeCandidateCardResponse(
        "card_api_20260428_0002",
        "anon_api_20260428_0002",
        "projection-v1",
        "l4_identity_disclosed",
        "Identity disclosed profile",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Unsafe summary",
        "Unsafe skills",
        List.of("Unsafe evidence"),
        List.of("Unsafe narrative")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("redactionLevel must be an anonymous client-safe API level");
  }

  @Test
  void apiBoundaryDtosDoNotContainInternalEntityTypesOrUnsafeFieldNames() {
    Set<Class<?>> apiDtos = Set.of(
        ApiResponseEnvelope.class,
        ApiErrorResponse.class,
        ApiAccessDeniedResponse.class,
        ApiValidationErrorResponse.class,
        ClientSafeCandidateCardResponse.class);

    for (Class<?> dto : apiDtos) {
      assertThat(dto.isRecord()).as(dto.getSimpleName()).isTrue();
      assertThat(dto.getRecordComponents())
          .as(dto.getSimpleName())
          .extracting(RecordComponent::getType)
          .doesNotContainAnyElementsOf(FORBIDDEN_API_DTO_TYPES);
      assertThat(dto.getRecordComponents())
          .as(dto.getSimpleName())
          .extracting(RecordComponent::getName)
          .noneMatch(ApiBoundaryContractTest::isForbiddenApiFieldName);
    }
  }

  @Test
  void responseEnvelopeIsBoundToApiSafeResponseBodiesOnly() {
    TypeVariable<Class<ApiResponseEnvelope>>[] typeParameters =
        ApiResponseEnvelope.class.getTypeParameters();

    assertThat(typeParameters).hasSize(1);
    assertThat(typeParameters[0].getBounds()).containsExactly(ApiSafeResponseBody.class);
    assertThat(ApiSafeResponseBody.class).isAssignableFrom(ClientSafeCandidateCardResponse.class);
    assertThat(ApiSafeResponseBody.class).isAssignableFrom(ApiErrorResponse.class);
    assertThat(ApiSafeResponseBody.class).isAssignableFrom(ApiAccessDeniedResponse.class);
    assertThat(ApiSafeResponseBody.class).isAssignableFrom(ApiValidationErrorResponse.class);
    assertThat(ApiSafeResponseBody.class.isAssignableFrom(CandidateProfile.class)).isFalse();
    assertThat(ApiSafeResponseBody.class.isAssignableFrom(CandidateProfileId.class)).isFalse();
    assertThat(ApiSafeResponseBody.class.isAssignableFrom(SourceItem.class)).isFalse();
    assertThat(ApiSafeResponseBody.class.isAssignableFrom(InformationPacket.class)).isFalse();
  }

  @Test
  void mapperAcceptsOnlyClientSafeCandidateCardAndNeverRawCandidateProfileTypes() {
    Method[] methods = ClientSafeCandidateCardResponseMapper.class.getDeclaredMethods();

    assertThat(methods)
        .filteredOn(method -> Modifier.isPublic(method.getModifiers()))
        .extracting(Method::getName)
        .containsExactly("from");

    for (Method method : methods) {
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      assertThat(method.getReturnType()).isEqualTo(ClientSafeCandidateCardResponse.class);
      assertThat(method.getParameterTypes()).containsExactly(ClientSafeCandidateCard.class);
      assertThat(method.getParameterTypes()).doesNotContainAnyElementsOf(FORBIDDEN_API_DTO_TYPES);
    }
  }

  @Test
  void accessDeniedResponseDoesNotLeakStackTraceRawIdsOrInternalEntityDetails() {
    AccessDeniedException exception = new AccessDeniedException(new AccessDecision(
        false,
        "client_raw_candidate_profile_denied",
        "Client role cannot read raw CandidateProfile resources before future disclosure gates. "
            + "internal id 00000000-0000-0000-0000-000000090001 "
            + "at com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileService"));

    ApiAccessDeniedResponse response = ApiAccessDeniedResponse.from(exception);

    assertThat(response.errorCode()).isEqualTo("access_denied");
    assertThat(response.safeReason()).isEqualTo("client_raw_candidate_profile_denied");
    assertThat(response.safeExplanation()).isEqualTo("Access denied.");
    assertThat(response.safeExplanation())
        .doesNotContain(
            "00000000-0000-0000-0000-000000090001",
            "CandidateProfileService",
            "com.recruitingtransactionos",
            "at ");
  }

  @Test
  void safeAccessDeniedReasonIsPreservedWithoutUsingThrowableMessageOrStackTrace() {
    AccessDeniedException exception = new AccessDeniedException(new AccessDecision(
        false,
        "client_safe_candidate_card_access_context_required",
        "Client-safe candidate projection requires a client-safe candidate card read context."));

    ApiAccessDeniedResponse response = ApiAccessDeniedResponse.from(exception);

    assertThat(response.errorCode()).isEqualTo("access_denied");
    assertThat(response.safeReason())
        .isEqualTo("client_safe_candidate_card_access_context_required");
    assertThat(response.safeExplanation())
        .isEqualTo("Client-safe candidate projection requires a client-safe candidate card read context.");
    assertThat(response.toErrorResponse().errorCode()).isEqualTo("access_denied");
  }

  @Test
  void unknownInternalOrForbiddenFieldsAreDeniedByContractRules() {
    for (String unsafeField : List.of(
        "candidateId",
        "candidateProfileId",
        "identity.fullName",
        "contact.email",
        "contact.phone",
        "linkedinUrl",
        "rawSourceText",
        "consultantNotes",
        "sourceItem",
        "informationPacket",
        "claimLedgerItem",
        "reviewEvent",
        "workflowEvent",
        "identityDisclosedFields",
        "newInternalDebugField")) {
      assertThat(ApiBoundaryContractRules.isAllowedClientSafeCandidateCardResponseField(
          unsafeField)).as(unsafeField).isFalse();
    }
  }

  private static ClientSafeCandidateCard clientSafeCard() {
    return new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of("card_api_20260428_0001"),
        AnonymousCandidateRef.of("anon_candidate_api_20260428_0001"),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led verification work for complex chip programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence summary withheld pending redaction pipeline."),
        List.of("Match narrative withheld pending shortlist generator."));
  }

  private static boolean isForbiddenApiFieldName(String fieldName) {
    String lower = fieldName.toLowerCase();
    return lower.contains("candidateid")
        || lower.contains("candidateprofileid")
        || lower.contains("rawcandidate")
        || lower.contains("rawprofile")
        || lower.contains("fullname")
        || lower.contains("email")
        || lower.contains("phone")
        || lower.contains("linkedin")
        || lower.contains("rawsource")
        || lower.contains("consultantnotes")
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
}
