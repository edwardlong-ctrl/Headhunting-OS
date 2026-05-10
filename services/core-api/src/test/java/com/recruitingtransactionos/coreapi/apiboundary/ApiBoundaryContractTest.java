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
    assertThat(response.clientAlias()).startsWith("alias-");
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
  void clientAliasIsStableAndNotCollapsedIntoTinyAliasSpace() {
    ClientSafeCandidateCardResponse first = ClientSafeCandidateCardResponseMapper.from(clientSafeCard());
    ClientSafeCandidateCardResponse second = ClientSafeCandidateCardResponseMapper.from(new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of("card_api_20260428_0002"),
        AnonymousCandidateRef.of("anon_candidate_api_20260428_0002"),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led verification work for complex chip programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence summary withheld pending redaction pipeline."),
        List.of("Match narrative withheld pending shortlist generator.")));

    assertThat(ClientSafeCandidateCardResponseMapper.from(clientSafeCard()).clientAlias())
        .isEqualTo(first.clientAlias());
    assertThat(first.clientAlias()).startsWith("alias-");
    assertThat(first.clientAlias()).hasSize("alias-".length() + 8);
    assertThat(second.clientAlias()).isNotEqualTo(first.clientAlias());
  }

  @Test
  void clientSafeCandidateCardResponseContainsOnlyAllowedApiSafeFields() {
    assertThat(ClientSafeCandidateCardResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
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
        "alias-aa",
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

    assertThatThrownBy(() -> new ClientSafeCandidateCardResponse(
        "card_api_20260428_0003",
        "alias-bb",
        "projection-v1",
        "l2_client_safe",
        "Jane Candidate",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Unsafe summary",
        "Unsafe skills",
        List.of("Unsafe evidence"),
        List.of("Unsafe narrative")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("generalizedHeadline must not contain unsafe API-visible text");
  }

  @Test
  void businessPortalDtosAllowCapitalizedBusinessNamesWithoutRelaxingInternalLeakageGuards() {
    String id = "00000000-0000-0000-0000-000000000001";
    String createdAt = "2026-05-08T00:00:00Z";

    assertThat(new ConsultantCompanySummaryResponse(
        id,
        "Pilot Aurora Micro Systems",
        "active",
        0,
        0,
        createdAt).name())
        .isEqualTo("Pilot Aurora Micro Systems");
    assertThat(new ConsultantCompanyDetailResponse(
        id,
        1,
        "Pilot Aurora Micro Systems",
        "Pilot Aurora Micro Systems",
        "semiconductor",
        null,
        "Shanghai",
        "500-1000",
        "active",
        null,
        null,
        createdAt,
        createdAt,
        List.of(),
        0).name())
        .isEqualTo("Pilot Aurora Micro Systems");
    assertThat(new ConsultantJobSummaryResponse(
        id,
        "Senior Analog Layout Engineer",
        id,
        "activated",
        "semiconductor",
        "Semiconductor",
        createdAt).title())
        .isEqualTo("Senior Analog Layout Engineer");
    assertThat(new ConsultantJobDetailResponse(
        id,
        1,
        id,
        "Senior Analog Layout Engineer",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "activated",
        "semiconductor",
        "Semiconductor",
        "seeded",
        null,
        null,
        null,
        null,
        createdAt,
        createdAt,
        List.of(),
        null).title())
        .isEqualTo("Senior Analog Layout Engineer");
    assertThat(new ConsultantShortlistSummaryResponse(
        id,
        "Senior Analog Layout Shortlist",
        id,
        "ready_for_review",
        0,
        createdAt).title())
        .isEqualTo("Senior Analog Layout Shortlist");
    assertThat(new ClientCompanyProfileResponse(
        id,
        1,
        "Pilot Aurora Micro Systems",
        "Pilot Aurora Micro Systems",
        "semiconductor",
        null,
        "Shanghai",
        "500-1000",
        null,
        "active",
        createdAt).name())
        .isEqualTo("Pilot Aurora Micro Systems");
    assertThat(new ClientJobSubmissionStatusResponse(
        id,
        id,
        "Senior Analog Layout Engineer",
        "activated",
        createdAt,
        createdAt,
        List.of(),
        List.of(),
        List.of(),
        true).title())
        .isEqualTo("Senior Analog Layout Engineer");

    assertThatThrownBy(() -> new ConsultantJobSummaryResponse(
        id,
        "com.recruitingtransactionos.StackTrace",
        id,
        "activated",
        null,
        null,
        createdAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("title must not contain unsafe business-visible text");
  }

  @Test
  void consultantShortlistBuilderResponseAllowsOperationalLabelsAndPreviewCopy() {
    String id = "00000000-0000-0000-0000-000000000001";
    String createdAt = "2026-05-08T00:00:00Z";

    ConsultantShortlistDetailResponse response = new ConsultantShortlistDetailResponse(
        id,
        1,
        id,
        "shortlist alpha",
        "draft",
        null,
        null,
        null,
        createdAt,
        createdAt,
        List.of(
            new ConsultantShortlistDetailResponse.PreSendCheck(
                "status_ready_for_review",
                "Shortlist status is ready for review",
                false),
            new ConsultantShortlistDetailResponse.PreSendCheck(
                "delivery_preview_ready",
                "Client-safe delivery preview can be generated",
                false)),
        new ConsultantShortlistDetailResponse.DeliveryPreview(
            "No client-safe shortlist summary is available until at least one candidate card is included.",
            "No client-safe shortlist summary is available until at least one candidate card is included.",
            "No client-safe shortlist summary is available until at least one candidate card is included.",
            "No client-safe shortlist summary is available until at least one candidate card is included."),
        List.of());

    assertThat(response.preSendChecks()).hasSize(2);
    assertThat(response.deliveryPreview().clientSafeSummary()).contains("client-safe shortlist summary");
  }

  @Test
  void apiBoundaryDtosDoNotContainInternalEntityTypesOrUnsafeFieldNames() {
    Set<Class<?>> apiDtos = Set.of(
        ApiResponseEnvelope.class,
        ApiErrorResponse.class,
        ApiAccessDeniedResponse.class,
        ApiValidationErrorResponse.class,
        ClientSafeCandidateCardResponse.class,
        ConsultantParsedDocumentResponse.class,
        ConsultantDocumentEvidenceResponse.class,
        ObservabilityWorkflowEventSearchResponse.class,
        ObservabilityReviewEventSearchResponse.class,
        ObservabilityAITaskRunSearchResponse.class,
        AuthSessionResponse.class,
        AuthLogoutResponse.class);

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
  void consultantDocumentDtosContainOnlyCurrentApiSafeFields() {
    assertThat(ConsultantDocumentUploadResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly("sourceItemId", "informationPacketId", "scanStatus");

    assertThat(ConsultantParsedDocumentResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
            "processingStatus",
            "parserName",
            "parserVersion",
            "mediaType",
            "ocrRequired",
            "chunkCount",
            "createdAt",
            "completedAt",
            "failureReason");
    for (RecordComponent component : ConsultantParsedDocumentResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedConsultantParsedDocumentResponseField(
          component.getName())).as(component.getName()).isTrue();
    }

    assertThat(ConsultantDocumentEvidenceResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly("processingStatus", "query", "totalHits", "hits");
    for (RecordComponent component : ConsultantDocumentEvidenceResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedConsultantDocumentEvidenceResponseField(
          component.getName())).as(component.getName()).isTrue();
    }
  }

  @Test
  void placementAndRevenueDtosExposeOnlyAllowlistedFields() {
    assertThat(ConsultantPlacementSummaryResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
            "placementId",
            "version",
            "jobId",
            "candidateId",
            "companyId",
            "status",
            "salaryAmount",
            "salaryCurrency",
            "feeRatePercentage",
            "expectedFeeAmount",
            "feeAgreementActive",
            "feeAgreementReference",
            "paymentTerms",
            "invoiceReadiness",
            "startDate",
            "guaranteeDays",
            "guaranteeExpiresAt",
            "offerAcceptedAt",
            "onboardedAt",
            "createdAt",
            "updatedAt",
            "notes");
    for (RecordComponent component : ConsultantPlacementSummaryResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedConsultantPlacementSummaryResponseField(
          component.getName())).as(component.getName()).isTrue();
    }

    assertThat(ConsultantCommissionSummaryResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
            "commissionId",
            "version",
            "placementId",
            "consultantId",
            "status",
            "commissionType",
            "amount",
            "currency",
            "splitPercentage",
            "salaryAmount",
            "feeRatePercentage",
            "paidAt",
            "withheldReason",
            "createdAt",
            "updatedAt");
    for (RecordComponent component : ConsultantCommissionSummaryResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedConsultantCommissionSummaryResponseField(
          component.getName())).as(component.getName()).isTrue();
    }

    assertThat(OwnerPlacementSummaryResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
            "placementId",
            "jobId",
            "candidateId",
            "companyId",
            "status",
            "salaryAmount",
            "salaryCurrency",
            "feeRatePercentage",
            "expectedFeeAmount",
            "feeAgreementActive",
            "feeAgreementReference",
            "paymentTerms",
            "invoiceReadiness",
            "accountingExportStatus",
            "commissionStatuses",
            "startDate",
            "guaranteeDays",
            "guaranteeExpiresAt",
            "createdAt",
            "updatedAt");
    for (RecordComponent component : OwnerPlacementSummaryResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedOwnerPlacementSummaryResponseField(
          component.getName())).as(component.getName()).isTrue();
    }
    for (RecordComponent component : OwnerCommissionSummaryResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedOwnerCommissionSummaryResponseField(
          component.getName())).as(component.getName()).isTrue();
    }
    assertThat(OwnerRevenueSummaryResponse.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly(
            "totalExpectedFee",
            "totalPaidFee",
            "placementCount",
            "unknownExpectedFeePlacementCount",
            "pendingCommissionCount",
            "paidCommissionCount",
            "paidCommissionMissingAmountCount",
            "activeGuaranteeCount",
            "replacementRequiredCount",
            "invoiceInFlightCount",
            "invoiceReadyCount",
            "invoiceSentCount",
            "paidPlacementCount",
            "guaranteeCompletedCount");
    for (RecordComponent component : OwnerRevenueSummaryResponse.class.getRecordComponents()) {
      assertThat(ApiBoundaryContractRules.isAllowedOwnerRevenueSummaryResponseField(
          component.getName())).as(component.getName()).isTrue();
    }
  }

  @Test
  void consultantNestedDtosDropInternalLeakageTextButPreserveBusinessFields() {
    ConsultantCompanyDetailResponse.Contact contact = new ConsultantCompanyDetailResponse.Contact(
        "contact-1",
        "Alice Zhang",
        "com.recruitingtransactionos.coreapi.SecretTitle",
        "alice@example.com",
        "+86 138 0000 1234",
        "primary",
        true,
        "java.lang.Exception");
    assertThat(contact.name()).isEqualTo("Alice Zhang");
    assertThat(contact.email()).isEqualTo("alice@example.com");
    assertThat(contact.phone()).isEqualTo("+86 138 0000 1234");
    assertThat(contact.title()).isNull();
    assertThat(contact.status()).isEqualTo("active");

    ConsultantJobDetailResponse.Requirement requirement =
        new ConsultantJobDetailResponse.Requirement(
            "req-1",
            "skill",
            "Java",
            "must_have",
            "stack trace leaked from com.recruitingtransactionos.coreapi.Parser",
            0);
    ConsultantJobDetailResponse.Scorecard scorecard =
        new ConsultantJobDetailResponse.Scorecard(
            "scorecard-1",
            "technical_fit",
            "java.lang.IllegalStateException: bad",
            "draft");
    ConsultantShortlistDetailResponse.Card card = new ConsultantShortlistDetailResponse.Card(
        "card-1",
        "anon-card-1",
        1,
        0,
        "draft",
        "workflowEvent leaked",
        "anon_candidate_1",
        "Consultant-reviewed candidate",
        "Confidential role family",
        "Consultant-reviewed shortlist level",
        "Location shared after identity unlock",
        "safe summary",
        "safe skill summary",
        List.of("evidence"),
        List.of("narrative"),
        null,
        "unknown",
        "not_assessed",
        List.of(),
        null);

    assertThat(requirement.detail()).isNull();
    assertThat(scorecard.dimensions()).isEqualTo("technical_fit");
    assertThat(scorecard.scoringGuidance()).isNull();
    assertThat(card.matchReportId()).isNull();
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
    assertThat(ApiSafeResponseBody.class).isAssignableFrom(AuthSessionResponse.class);
    assertThat(ApiSafeResponseBody.class).isAssignableFrom(AuthLogoutResponse.class);
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
