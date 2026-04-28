package com.recruitingtransactionos.coreapi.clientsafeprojection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReidentificationRiskAssessmentServiceTest {

  private final ReidentificationRiskAssessmentService service =
      new ReidentificationRiskAssessmentService();

  @Test
  void flagsRequiredUnsafeUniquenessFeatureCategoriesWithDeterministicDecisions() {
    ReidentificationRiskAssessment assessment = service.assess(
        AnonymousCandidateCardId.of("card_task7c_0001"),
        RedactionLevel.L2_CLIENT_SAFE,
        Set.of(
            ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR,
            ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER,
            ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME,
            ReidentificationRiskFeature.PUBLIC_IDENTIFIER_BEFORE_CONSENT,
            ReidentificationRiskFeature.EXACT_LOCATION_OR_ADDRESS,
            ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL,
            ReidentificationRiskFeature.SMALL_TEAM_UNIQUE_OWNERSHIP_CLAIM,
            ReidentificationRiskFeature.OVERLY_SPECIFIC_IDENTIFYING_ACHIEVEMENT_NUMBER));

    assertThat(service.placeholderOnly()).isTrue();
    assertThat(assessment.cardId().value()).isEqualTo("card_task7c_0001");
    assertThat(assessment.redactionLevel()).isEqualTo(RedactionLevel.L2_CLIENT_SAFE);
    assertThat(assessment.riskLevel()).isEqualTo(ReidentificationRiskLevel.HIGH);
    assertThat(assessment.decision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(assessment.unsafeFeatures())
        .containsExactlyInAnyOrder(
            ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR,
            ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER,
            ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME,
            ReidentificationRiskFeature.PUBLIC_IDENTIFIER_BEFORE_CONSENT,
            ReidentificationRiskFeature.EXACT_LOCATION_OR_ADDRESS,
            ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL,
            ReidentificationRiskFeature.SMALL_TEAM_UNIQUE_OWNERSHIP_CLAIM,
            ReidentificationRiskFeature.OVERLY_SPECIFIC_IDENTIFYING_ACHIEVEMENT_NUMBER);
    assertThat(assessment.explanation())
        .contains("deterministic placeholder")
        .contains("not a real scorer");
    assertThat(assessment.isSafeAnonymousClientOutput()).isFalse();

    assertThat(ReidentificationRiskFeature.EXACT_COMPANY_RARE_TITLE_EXACT_YEAR
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.GENERALIZE);
    assertThat(ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(ReidentificationRiskFeature.PUBLIC_IDENTIFIER_BEFORE_CONSENT
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(ReidentificationRiskFeature.EXACT_LOCATION_OR_ADDRESS
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(ReidentificationRiskFeature.SMALL_TEAM_UNIQUE_OWNERSHIP_CLAIM
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.GENERALIZE);
    assertThat(ReidentificationRiskFeature.OVERLY_SPECIFIC_IDENTIFYING_ACHIEVEMENT_NUMBER
        .recommendedDecision()).isEqualTo(ReidentificationRiskDecision.REVIEW);
  }

  @Test
  void l4IdentityDisclosedAssessmentIsNeverAnonymousClientSafe() {
    ReidentificationRiskAssessment assessment = service.assess(
        AnonymousCandidateCardId.of("card_task7c_0002"),
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        Set.of());

    assertThat(assessment.riskLevel()).isEqualTo(ReidentificationRiskLevel.HIGH);
    assertThat(assessment.decision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(assessment.isSafeAnonymousClientOutput()).isFalse();
    assertThatThrownBy(assessment::requireSafeAnonymousClientOutput)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("re-identification assessment is not safe for anonymous client output");
  }

  @Test
  void highRiskAssessmentCannotBeUsedAsClientSafeProjectionApproval() {
    ClientSafeCandidateProjectionService projectionService =
        new ClientSafeCandidateProjectionService();
    ReidentificationRiskAssessment highRiskAssessment = service.assess(
        AnonymousCandidateCardId.of("card_task7c_0003"),
        RedactionLevel.L2_CLIENT_SAFE,
        Set.of(ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER));

    assertThatThrownBy(() -> projectionService.project(
        clientSafeReadRequest(),
        snapshotWithCardId("card_task7c_0003"),
        highRiskAssessment))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("re-identification assessment blocks anonymous client projection");
  }

  @Test
  void snapshotAssessmentFlagsObviousRawEmployerProjectAndContactSignals() {
    ReidentificationRiskAssessment assessment =
        service.assess(snapshotWithCardId("card_task7c_0005"));

    assertThat(assessment.riskLevel()).isEqualTo(ReidentificationRiskLevel.HIGH);
    assertThat(assessment.decision()).isEqualTo(ReidentificationRiskDecision.BLOCK);
    assertThat(assessment.unsafeFeatures())
        .contains(
            ReidentificationRiskFeature.EXACT_CURRENT_EMPLOYER,
            ReidentificationRiskFeature.EXACT_PROJECT_PRODUCT_CHIP_CODE_NAME,
            ReidentificationRiskFeature.DIRECT_CONTACT_OR_PROFILE_URL);
  }

  @Test
  void lowRiskAssessmentCanAccompanyClientSafeProjectionWithoutExposingRawInput() {
    ClientSafeCandidateProjectionService projectionService =
        new ClientSafeCandidateProjectionService();
    ReidentificationRiskAssessment lowRiskAssessment = service.assess(
        AnonymousCandidateCardId.of("card_task7c_0004"),
        RedactionLevel.L2_CLIENT_SAFE,
        Set.of());

    ClientSafeCandidateCard card = projectionService.project(
        clientSafeReadRequest(),
        snapshotWithCardId("card_task7c_0004"),
        lowRiskAssessment);

    assertThat(card).isInstanceOf(ClientSafeCandidateCard.class);
    assertThat(card.cardId().value()).isEqualTo("card_task7c_0004");
    assertThat(lowRiskAssessment.isSafeAnonymousClientOutput()).isTrue();
  }

  private static InternalCandidateProjectionSnapshot snapshotWithCardId(String cardId) {
    return new InternalCandidateProjectionSnapshot(
        "00000000-0000-0000-0000-0000007c0001",
        "00000000-0000-0000-0000-0000007c0002",
        "Jane Alpha Candidate",
        "jane.alpha@example.com",
        "+86 138 0000 7C7C",
        "https://www.linkedin.com/in/jane-alpha-candidate",
        "NebulaChip Systems",
        List.of("Orion-X7 NPU"),
        "Jane Alpha Candidate led Orion-X7 NPU verification at NebulaChip Systems.",
        "Do not share negotiation notes with client.",
        AnonymousCandidateCardId.of(cardId),
        AnonymousCandidateRef.of("anon_candidate_" + cardId),
        "projection-v1",
        RedactionLevel.L2_CLIENT_SAFE,
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

  private static AccessRequest clientSafeReadRequest() {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.CLIENT_SAFE_CANDIDATE_CARD,
        AccessAction.READ,
        FieldClassification.CLIENT_SAFE,
        Set.of(),
        false);
  }
}
