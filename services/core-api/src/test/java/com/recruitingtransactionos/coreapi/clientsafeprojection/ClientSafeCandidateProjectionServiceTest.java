package com.recruitingtransactionos.coreapi.clientsafeprojection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.governedintake.InformationPacket;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;
import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientSafeCandidateProjectionServiceTest {

  private static final String RAW_CANDIDATE_ID =
      "00000000-0000-0000-0000-0000007b0001";
  private static final String RAW_CANDIDATE_PROFILE_ID =
      "00000000-0000-0000-0000-0000007b0002";
  private static final String FULL_NAME = "Jane Alpha Candidate";
  private static final String EMAIL = "jane.alpha@example.com";
  private static final String PHONE = "+86 138 0000 7B7B";
  private static final String LINKEDIN_URL = "https://www.linkedin.com/in/jane-alpha-candidate";
  private static final String EXACT_EMPLOYER = "NebulaChip Systems";
  private static final String EXACT_PROJECT = "Orion-X7 NPU";
  private static final String EXACT_PRODUCT = "VectorSlate PCIe Gen6 Switch";
  private static final String EXACT_CHIP = "NC-9000";
  private static final String RAW_SOURCE_TEXT =
      "Jane Alpha Candidate led Orion-X7 NPU verification at NebulaChip Systems.";
  private static final String CONSULTANT_INTERNAL_NOTES =
      "Do not tell the client that Jane is negotiating with HelioSemi.";

  @Test
  void projectionReturnsOnlyClientSafeCandidateCardWithoutRawCandidateProfileOrSensitiveValues() {
    ClientSafeCandidateProjectionService service = new ClientSafeCandidateProjectionService();

    ClientSafeCandidateCard card = service.project(baseSnapshot());

    assertThat(card).isInstanceOf(ClientSafeCandidateCard.class);
    assertThat(card.cardId().value()).isEqualTo("card_task7b_0001");
    assertThat(card.anonymousCandidateRef().value()).isEqualTo("anon_candidate_task7b_0001");
    assertThat(card.redactionLevel()).isEqualTo(RedactionLevel.L2_CLIENT_SAFE);

    List<String> projectedText = flattenedRecordText(card);
    assertThat(projectedText)
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
            CONSULTANT_INTERNAL_NOTES);
    assertThat(projectedText)
        .noneMatch(value -> value.contains("CandidateProfile"))
        .noneMatch(value -> value.contains("SourceItem"))
        .noneMatch(value -> value.contains("InformationPacket"))
        .noneMatch(value -> value.contains("ClaimLedgerItem"))
        .noneMatch(value -> value.contains("ReviewEvent"))
        .noneMatch(value -> value.contains("WorkflowEvent"));
  }

  @Test
  void projectionRejectsUnknownClientVisibleFieldsByDefault() {
    ClientSafeCandidateProjectionService service = new ClientSafeCandidateProjectionService();

    assertThatThrownBy(() -> service.project(baseSnapshotWithFieldSelection(Set.of(
        "profile.generalized_headline",
        "profile.unreviewed_new_field"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "client-visible candidate field is not allowed: "
                + "profile.unreviewed_new_field (unknown_field_denied_by_default)");
  }

  @Test
  void projectionRejectsForbiddenClientVisibleFieldsEvenWhenSafeFieldsAreAlsoSelected() {
    ClientSafeCandidateProjectionService service = new ClientSafeCandidateProjectionService();

    assertThatThrownBy(() -> service.project(baseSnapshotWithFieldSelection(Set.of(
        "profile.generalized_headline",
        "identity.full_name"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "client-visible candidate field is not allowed: "
                + "identity.full_name (forbidden_client_visible_candidate_field)");
  }

  @Test
  void l4IdentityDisclosureIsRejectedForAnonymousProjectionReadModel() {
    ClientSafeCandidateProjectionService service = new ClientSafeCandidateProjectionService();

    assertThatThrownBy(() -> service.project(baseSnapshotWithRedactionLevel(
        RedactionLevel.L4_IDENTITY_DISCLOSED)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("client-safe projection cannot use L4 identity disclosure");
  }

  @Test
  void projectionRejectsSafeOutputFieldsThatCopyRawSensitiveInputValues() {
    ClientSafeCandidateProjectionService service = new ClientSafeCandidateProjectionService();

    assertThatThrownBy(() -> service.project(baseSnapshotWithSafeSummary(
        "Built " + EXACT_PROJECT + " verification strategy for a regional chip team.")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("projected client-safe text contains raw sensitive input value");
  }

  @Test
  void publicProjectionApiDoesNotReturnOrContainRawInternalEntityTypes() throws Exception {
    Set<Class<?>> forbiddenRawTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class,
        SourceItem.class,
        InformationPacket.class,
        ClaimId.class,
        ReviewEventId.class,
        WorkflowEventId.class);

    for (Method projectMethod : ClientSafeCandidateProjectionService.class.getMethods()) {
      if (!projectMethod.getName().equals("project")) {
        continue;
      }
      assertThat(projectMethod.getReturnType()).isEqualTo(ClientSafeCandidateCard.class);
      assertThat(projectMethod.getReturnType()).isNotIn(forbiddenRawTypes);
      assertThat(projectMethod.getParameterTypes()).doesNotContainAnyElementsOf(forbiddenRawTypes);
    }
    assertThat(ClientSafeCandidateCard.class.getRecordComponents())
        .extracting(RecordComponent::getType)
        .doesNotContainAnyElementsOf(forbiddenRawTypes);
    assertThat(InternalCandidateProjectionSnapshot.class.getRecordComponents())
        .extracting(RecordComponent::getType)
        .doesNotContainAnyElementsOf(forbiddenRawTypes);
  }

  private static InternalCandidateProjectionSnapshot baseSnapshot() {
    return new InternalCandidateProjectionSnapshot(
        RAW_CANDIDATE_ID,
        RAW_CANDIDATE_PROFILE_ID,
        FULL_NAME,
        EMAIL,
        PHONE,
        LINKEDIN_URL,
        EXACT_EMPLOYER,
        List.of(EXACT_PROJECT, EXACT_PRODUCT, EXACT_CHIP),
        RAW_SOURCE_TEXT,
        CONSULTANT_INTERNAL_NOTES,
        AnonymousCandidateCardId.of("card_task7b_0001"),
        AnonymousCandidateRef.of("anon_candidate_task7b_0001"),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
        "Senior verification leader in advanced-chip programs",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Has led complex verification programs without disclosing employer or code names.",
        "SystemVerilog, UVM, coverage closure, and cross-team debug leadership.",
        List.of("Evidence generalized from approved profile signals."),
        List.of("Strong fit for verification leadership based on generalized capability evidence."),
        Set.of(
            "anonymous.card_id",
            "anonymous.candidate_ref",
            "projection.version",
            "redaction.level",
            "profile.generalized_headline",
            "profile.generalized_role_family",
            "profile.generalized_seniority_band",
            "profile.generalized_location_region",
            "summary.safe_summary",
            "summary.safe_skill_summary",
            "summary.safe_evidence_placeholders",
            "summary.safe_match_narrative_placeholders"));
  }

  private static InternalCandidateProjectionSnapshot baseSnapshotWithFieldSelection(
      Set<String> selectedFieldPaths) {
    InternalCandidateProjectionSnapshot snapshot = baseSnapshot();
    return new InternalCandidateProjectionSnapshot(
        snapshot.rawCandidateId(),
        snapshot.rawCandidateProfileId(),
        snapshot.fullName(),
        snapshot.email(),
        snapshot.phone(),
        snapshot.linkedInUrl(),
        snapshot.exactCurrentEmployer(),
        snapshot.exactProjectProductOrChipNames(),
        snapshot.rawSourceText(),
        snapshot.consultantInternalNotes(),
        snapshot.cardId(),
        snapshot.anonymousCandidateRef(),
        snapshot.projectionVersion(),
        snapshot.redactionLevel(),
        snapshot.generalizedHeadline(),
        snapshot.generalizedRoleFamily(),
        snapshot.generalizedSeniorityBand(),
        snapshot.generalizedLocationRegion(),
        snapshot.safeSummary(),
        snapshot.safeSkillSummary(),
        snapshot.safeEvidenceSummaries(),
        snapshot.safeMatchNarratives(),
        selectedFieldPaths);
  }

  private static InternalCandidateProjectionSnapshot baseSnapshotWithRedactionLevel(
      RedactionLevel redactionLevel) {
    InternalCandidateProjectionSnapshot snapshot = baseSnapshot();
    return new InternalCandidateProjectionSnapshot(
        snapshot.rawCandidateId(),
        snapshot.rawCandidateProfileId(),
        snapshot.fullName(),
        snapshot.email(),
        snapshot.phone(),
        snapshot.linkedInUrl(),
        snapshot.exactCurrentEmployer(),
        snapshot.exactProjectProductOrChipNames(),
        snapshot.rawSourceText(),
        snapshot.consultantInternalNotes(),
        snapshot.cardId(),
        snapshot.anonymousCandidateRef(),
        snapshot.projectionVersion(),
        redactionLevel,
        snapshot.generalizedHeadline(),
        snapshot.generalizedRoleFamily(),
        snapshot.generalizedSeniorityBand(),
        snapshot.generalizedLocationRegion(),
        snapshot.safeSummary(),
        snapshot.safeSkillSummary(),
        snapshot.safeEvidenceSummaries(),
        snapshot.safeMatchNarratives(),
        snapshot.selectedClientVisibleFieldPaths());
  }

  private static InternalCandidateProjectionSnapshot baseSnapshotWithSafeSummary(
      String safeSummary) {
    InternalCandidateProjectionSnapshot snapshot = baseSnapshot();
    return new InternalCandidateProjectionSnapshot(
        snapshot.rawCandidateId(),
        snapshot.rawCandidateProfileId(),
        snapshot.fullName(),
        snapshot.email(),
        snapshot.phone(),
        snapshot.linkedInUrl(),
        snapshot.exactCurrentEmployer(),
        snapshot.exactProjectProductOrChipNames(),
        snapshot.rawSourceText(),
        snapshot.consultantInternalNotes(),
        snapshot.cardId(),
        snapshot.anonymousCandidateRef(),
        snapshot.projectionVersion(),
        snapshot.redactionLevel(),
        snapshot.generalizedHeadline(),
        snapshot.generalizedRoleFamily(),
        snapshot.generalizedSeniorityBand(),
        snapshot.generalizedLocationRegion(),
        safeSummary,
        snapshot.safeSkillSummary(),
        snapshot.safeEvidenceSummaries(),
        snapshot.safeMatchNarratives(),
        snapshot.selectedClientVisibleFieldPaths());
  }

  private static List<String> flattenedRecordText(Object record) {
    List<String> values = new ArrayList<>();
    for (RecordComponent component : record.getClass().getRecordComponents()) {
      try {
        Object value = component.getAccessor().invoke(record);
        if (value instanceof Iterable<?> iterable) {
          for (Object item : iterable) {
            values.add(String.valueOf(item));
          }
        } else {
          values.add(String.valueOf(value));
        }
      } catch (ReflectiveOperationException exception) {
        throw new AssertionError(exception);
      }
    }
    return values;
  }
}
