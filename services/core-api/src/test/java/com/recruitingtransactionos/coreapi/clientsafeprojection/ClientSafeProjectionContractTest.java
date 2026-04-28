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
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ClientSafeProjectionContractTest {

  @Test
  void clientSafeCandidateCardCanBeCreatedFromAnonymousGeneralizedFieldsOnly() {
    ClientSafeCandidateCard card = new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of("card_20260428_0001"),
        AnonymousCandidateRef.of("anon_candidate_20260428_0001"),
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

    assertThat(card.cardId().value()).startsWith("card_");
    assertThat(card.anonymousCandidateRef().value()).startsWith("anon_candidate_");
    assertThat(card.redactionLevel()).isEqualTo(RedactionLevel.L2_CLIENT_SAFE);
    assertThat(card.safeEvidenceSummaries()).containsExactly(
        "Evidence summary withheld pending redaction pipeline.");
    assertThat(card.safeMatchNarratives()).containsExactly(
        "Match narrative withheld pending shortlist generator.");
  }

  @Test
  void clientSafeCandidateCardDoesNotRequireOrExposeRawCandidateProfileOrEvidenceTypes() {
    Set<Class<?>> forbiddenTypes = Set.of(
        CandidateId.class,
        CandidateProfile.class,
        CandidateProfileId.class,
        SourceItem.class,
        InformationPacket.class,
        ClaimId.class,
        ReviewEventId.class,
        WorkflowEventId.class);

    assertThat(ClientSafeCandidateCard.class.getRecordComponents())
        .extracting(RecordComponent::getType)
        .doesNotContainAnyElementsOf(forbiddenTypes);
  }

  @Test
  void forbiddenFieldPolicyRecognizesIdentityContactRawSourceInternalAndRareIdentifierFields() {
    assertThat(ClientVisibleCandidateFieldPolicy.forbiddenFieldPaths())
        .contains(
            "full_name",
            "legal_name",
            "preferred_name",
            "email",
            "phone",
            "wechat",
            "whatsapp",
            "personal_messaging_handle",
            "exact_address",
            "linkedin_url",
            "github_url",
            "portfolio_url",
            "personal_website_url",
            "resume_url",
            "raw_document_url",
            "raw_candidate_id",
            "raw_candidate_profile_id",
            "source_item_id",
            "information_packet_id",
            "raw_source_reference",
            "raw_source_text",
            "raw_cv_text",
            "raw_consultant_notes",
            "consultant_internal_notes",
            "other_client_interaction_history",
            "sensitive_compensation_bottom_line",
            "negotiation_notes",
            "consent_internal_audit_records",
            "disclosure_internal_audit_records",
            "exact_current_employer",
            "uniquely_identifying_project_name",
            "chip_product_code_name",
            "patent_identifier",
            "paper_identifier",
            "public_talk_identifier",
            "open_source_identifier",
            "rare_title_exact_year_exact_company");
  }

  @Test
  void directIdentifiersAreRejectedForAnonymousClientProjection() {
    List<String> directIdentifiers = List.of(
        "full_name",
        "legal_name",
        "email",
        "phone",
        "wechat",
        "whatsapp",
        "linkedin_url",
        "raw_candidate_id",
        "raw_candidate_profile_id",
        "raw_source_text",
        "consultant_internal_notes");

    for (String fieldPath : directIdentifiers) {
      ClientVisibleCandidateFieldPolicy.Decision decision =
          ClientVisibleCandidateFieldPolicy.decide(fieldPath);

      assertThat(decision.allowed()).as(fieldPath).isFalse();
      assertThat(decision.reason()).as(fieldPath).isEqualTo("forbidden_client_visible_candidate_field");
      assertThat(ClientVisibleCandidateFieldPolicy.isForbidden(fieldPath)).as(fieldPath).isTrue();
      assertThat(ClientVisibleCandidateFieldPolicy.isAllowedForAnonymousClientProjection(fieldPath))
          .as(fieldPath)
          .isFalse();
    }
  }

  @Test
  void unknownUnsupportedFieldsAreDeniedByDefault() {
    ClientVisibleCandidateFieldPolicy.Decision decision =
        ClientVisibleCandidateFieldPolicy.decide("profile.unreviewed_new_field");

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reason()).isEqualTo("unknown_field_denied_by_default");
    assertThat(ClientVisibleCandidateFieldPolicy.isAllowedForAnonymousClientProjection(
        "profile.unreviewed_new_field")).isFalse();
  }

  @Test
  void explicitSafeAllowlistIsTheOnlyNormalAnonymousClientProjectionSurface() {
    assertThat(ClientVisibleCandidateFieldPolicy.safeAllowlistedFieldPaths())
        .contains(
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
            "summary.safe_match_narrative_placeholders");

    assertThat(ClientVisibleCandidateFieldPolicy.isAllowedForAnonymousClientProjection(
        "profile.generalized_role_family")).isTrue();
    assertThat(ClientVisibleCandidateFieldPolicy.isAllowedForAnonymousClientProjection(
        "identity.full_name")).isFalse();
  }

  @Test
  void redactionLevelsDefineOrderedVocabularyWithoutTreatingL4AsAnonymousClientSafe() {
    assertThat(RedactionLevel.values())
        .containsExactly(
            RedactionLevel.L0_TEASER,
            RedactionLevel.L1_GENERALIZED,
            RedactionLevel.L2_CLIENT_SAFE,
            RedactionLevel.L3_CONSENTED_DETAIL,
            RedactionLevel.L4_IDENTITY_DISCLOSED);

    assertThat(RedactionLevel.L0_TEASER.order()).isZero();
    assertThat(RedactionLevel.L1_GENERALIZED.order()).isEqualTo(1);
    assertThat(RedactionLevel.L2_CLIENT_SAFE.order()).isEqualTo(2);
    assertThat(RedactionLevel.L3_CONSENTED_DETAIL.order()).isEqualTo(3);
    assertThat(RedactionLevel.L4_IDENTITY_DISCLOSED.order()).isEqualTo(4);

    assertThat(RedactionLevel.L2_CLIENT_SAFE.isAnonymousClientSafeLevel()).isTrue();
    assertThat(RedactionLevel.L3_CONSENTED_DETAIL.isAnonymousClientSafeLevel()).isTrue();
    assertThat(RedactionLevel.L4_IDENTITY_DISCLOSED.isAnonymousClientSafeLevel()).isFalse();
    assertThat(RedactionLevel.L4_IDENTITY_DISCLOSED.requiresDisclosureRecord()).isTrue();
    assertThat(RedactionLevel.L4_IDENTITY_DISCLOSED.description())
        .contains("DisclosureRecord");
  }

  @Test
  void l4IdentityDisclosedCannotBeUsedAsNormalClientSafeCardExposure() {
    assertThatThrownBy(() -> new ClientSafeCandidateCard(
        AnonymousCandidateCardId.of("card_20260428_0002"),
        AnonymousCandidateRef.of("anon_candidate_20260428_0002"),
        "projection-v1",
        RedactionLevel.L4_IDENTITY_DISCLOSED,
        "Identity disclosed profile",
        "semiconductor_verification",
        "senior_ic",
        "greater_china",
        "Unsafe summary",
        "Unsafe skills",
        List.of("Unsafe evidence"),
        List.of("Unsafe narrative")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("redactionLevel must be an anonymous client-safe level; L4 is vocabulary only here");
  }

  @Test
  void clientSafeProjectionPackageAddsProjectionServiceButNoControllerPersistenceOrSpringWiring()
      throws IOException {
    List<Path> productionFiles = clientSafeProjectionProductionFiles();

    assertThat(productionFiles)
        .extracting(path -> path.getFileName().toString())
        .contains(
            "ClientSafeCandidateProjectionService.java",
            "InternalCandidateProjectionSnapshot.java")
        .noneMatch(fileName -> fileName.endsWith("Controller.java"))
        .noneMatch(fileName -> fileName.endsWith("Repository.java"))
        .noneMatch(fileName -> fileName.endsWith("Port.java"))
        .noneMatch(fileName -> fileName.contains("Persistence"));

    for (Path file : productionFiles) {
      String source = Files.readString(file);
      assertThat(source)
          .doesNotContain("@RestController")
          .doesNotContain("@Controller")
          .doesNotContain("@Service")
          .doesNotContain("@Repository")
          .doesNotContain("@RequestMapping")
          .doesNotContain("org.springframework")
          .doesNotContain("javax.sql.DataSource")
          .doesNotContain("JdbcTemplate")
          .doesNotContain("new ConsentRecord")
          .doesNotContain("new DisclosureRecord")
          .doesNotContain("UnlockService");
    }
  }

  private static List<Path> clientSafeProjectionProductionFiles() throws IOException {
    Path root = projectPath("src/main/java/com/recruitingtransactionos/coreapi/clientsafeprojection");
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
