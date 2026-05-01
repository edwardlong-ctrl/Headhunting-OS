package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TruthLayerAlignmentTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Path CONTRACT_SCHEMA_DIR = Path.of("../../packages/contracts/schemas");
  private static final Path MIGRATION =
      Path.of("src/main/resources/db/migration/V2__create_truth_layer_core_tables.sql");

  /*
   * Alignment matrix:
   * Java domain concept -> JSON schema field/enum -> V2 migration column/check.
   * ClaimType -> claim_type / $defs.claim_type -> governance.claim_type + claim_type.
   * AssertionStrength -> assertion_strength / $defs.assertion_strength
   *     -> governance.assertion_strength + assertion_strength.
   * Source span -> source_span_ref -> source_span_ref.
   * Speaker -> speaker / $defs.actor_role -> speaker governance.actor_role.
   * VerificationStatus -> verification_status / $defs.verification_status
   *     -> governance.verification_status + verification_status.
   * ClientShareability -> client_shareability / $defs.client_shareability
   *     -> governance.client_shareability + client_shareability.
   * RiskTier -> risk_tier / $defs.risk_tier -> governance.risk_tier + review_event.risk_tier.
   * CanonicalWriteDecisionType -> allow/block/require_review contract vocabulary
   *     -> gate decision values, not a database column in V2.
   */
  private static final Map<String, String> CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD =
      new LinkedHashMap<>();

  static {
    CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD.put("claim_type", "claim_type");
    CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD.put("assertion_strength", "assertion_strength");
    CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD.put("source_span", "source_span_ref");
    CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD.put("speaker", "speaker");
    CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD.put("verification_status", "verification_status");
    CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD.put("client_shareability", "client_shareability");
  }

  @Test
  void jsonContractFilesAreParseableAndNonEmpty() throws IOException {
    assertThat(CONTRACT_SCHEMA_DIR).exists().isDirectory();

    List<Path> schemaPaths;
    try (Stream<Path> paths = Files.list(CONTRACT_SCHEMA_DIR)) {
      schemaPaths = paths
          .filter(path -> path.getFileName().toString().endsWith(".json"))
          .sorted()
          .toList();
    }

    assertThat(schemaPaths).as("contract schema files").isNotEmpty();
    for (Path schemaPath : schemaPaths) {
      assertThat(Files.size(schemaPath)).as("%s should not be empty", schemaPath).isPositive();
      JsonNode schema = OBJECT_MAPPER.readTree(schemaPath.toFile());

      assertThat(schema.isObject()).as("%s should parse as a JSON object", schemaPath).isTrue();
      assertThat(schema.path("$schema").asText()).as("%s should declare JSON schema", schemaPath)
          .isNotBlank();
    }
  }

  @Test
  void claimLedgerContractExposesV21CoreFields() throws IOException {
    JsonNode claimLedger = readSchema("claim-ledger-item.schema.json");
    JsonNode properties = claimLedger.path("properties");
    Set<String> required = stringSet(claimLedger.path("required"));

    for (Map.Entry<String, String> field : CLAIM_LEDGER_CONCEPT_TO_SCHEMA_FIELD.entrySet()) {
      assertThat(properties.has(field.getValue()))
          .as("ClaimLedgerItem v2.1 concept %s should be exposed as JSON field %s",
              field.getKey(), field.getValue())
          .isTrue();
      assertThat(required)
          .as("ClaimLedgerItem JSON field %s should be required", field.getValue())
          .contains(field.getValue());
    }
  }

  @Test
  void javaEnumsAlignWithContractEnumVocabulary() throws IOException {
    assertThat(wireValues(ClaimType.values(), ClaimType::wireValue))
        .containsExactlyInAnyOrderElementsOf(contractEnum("claim_type"));
    assertThat(wireValues(AssertionStrength.values(), AssertionStrength::wireValue))
        .containsExactlyInAnyOrderElementsOf(contractEnum("assertion_strength"));
    assertThat(wireValues(VerificationStatus.values(), VerificationStatus::wireValue))
        .containsExactlyInAnyOrderElementsOf(contractEnum("verification_status"));
    assertThat(wireValues(ClientShareability.values(), ClientShareability::wireValue))
        .containsExactlyInAnyOrderElementsOf(contractEnum("client_shareability"));
    assertThat(wireValues(RiskTier.values(), RiskTier::wireValue))
        .containsExactlyInAnyOrderElementsOf(contractEnum("risk_tier"));
  }

  @Test
  void canonicalWriteDecisionValuesStayExplicitAndContractSafe() {
    assertThat(wireValues(CanonicalWriteDecisionType.values(), CanonicalWriteDecisionType::wireValue))
        .containsExactlyInAnyOrder("allow", "block", "require_review");
  }

  @Test
  void v2MigrationEnumVocabularyMatchesJsonContracts() throws IOException {
    assertThat(sqlEnumValues("governance.claim_type"))
        .containsExactlyInAnyOrderElementsOf(contractEnum("claim_type"));
    assertThat(sqlEnumValues("governance.assertion_strength"))
        .containsExactlyInAnyOrderElementsOf(contractEnum("assertion_strength"));
    assertThat(sqlEnumValues("governance.verification_status"))
        .containsExactlyInAnyOrderElementsOf(contractEnum("verification_status"));
    assertThat(sqlEnumValues("governance.client_shareability"))
        .containsExactlyInAnyOrderElementsOf(contractEnum("client_shareability"));
    assertThat(sqlEnumValues("governance.risk_tier"))
        .containsExactlyInAnyOrderElementsOf(contractEnum("risk_tier"));
  }

  @Test
  void v2MigrationKeepsCoreTruthLayerTablesAndClaimLedgerColumns() throws IOException {
    String sql = normalizedMigrationSql();

    assertThat(sql).contains("create table governance.claim_ledger_item");
    assertThat(sql).contains("create table workflow.workflow_event");
    assertThat(sql).contains("create table governance.ai_task_run");
    assertThat(sql).contains("create table governance.review_event");

    assertThat(sql).contains("claim_type governance.claim_type not null");
    assertThat(sql).contains("assertion_strength governance.assertion_strength not null");
    assertThat(sql).contains("source_span_ref text not null");
    assertThat(sql).contains("speaker governance.actor_role not null");
    assertThat(sql).contains("verification_status governance.verification_status not null");
    assertThat(sql).contains("canonical_write_allowed boolean not null default false");
    assertThat(sql).contains("client_shareability governance.client_shareability not null");
    assertThat(sql).contains("risk_tier governance.risk_tier not null");
  }

  @Disabled("Task 2E found current V2 has no consent_record/disclosure_record tables; changing V2 or adding V3 is forbidden here.")
  @Test
  void v2MigrationContainsConsentAndDisclosureRecordTablesRequiredByV21() throws IOException {
    String sql = normalizedMigrationSql();

    assertThat(sql).contains("create table privacy.consent_record");
    assertThat(sql).contains("create table privacy.disclosure_record");
  }

  @Test
  void verificationVocabularyDoesNotRegressToBareConfirmedOrTreatInferenceAsFact()
      throws IOException {
    Set<String> contractEnumValues = allContractEnumValues();
    Set<String> javaVerificationValues =
        wireValues(VerificationStatus.values(), VerificationStatus::wireValue);

    assertThat(contractEnumValues).doesNotContain("confirmed");
    assertThat(javaVerificationValues).doesNotContain("confirmed");
    assertThat(contractEnum("verification_status"))
        .contains("candidate_confirmed", "external_verified", "system_inference")
        .doesNotContain("confirmed");

    CanonicalWriteDecision decision = gate().decide(new CanonicalWriteRequest(
        claim(ClaimType.INFERENCE, AssertionStrength.IMPLIED, VerificationStatus.SYSTEM_INFERENCE,
            ClientShareability.INTERNAL_ONLY, false),
        true,
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW_RISK,
        false,
        false,
        true));

    assertThat(decision.type()).isEqualTo(CanonicalWriteDecisionType.BLOCK);
    assertThat(decision.reasons()).contains("system_inference_cannot_be_canonical_fact");
  }

  @Test
  void bulkApprovalAndClientShareabilityKeepCanonicalAndClientVisibleClaimsSafe() {
    CanonicalWriteGate gate = gate();
    ClaimInput bulkAcknowledged = claim(ClaimType.FACT, AssertionStrength.EXPLICIT,
        VerificationStatus.HUMAN_ACKNOWLEDGED, ClientShareability.CLIENT_SAFE, true);

    assertThat(gate.decide(new CanonicalWriteRequest(
        bulkAcknowledged,
        true,
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW_RISK,
        false,
        false,
        false)).type()).isEqualTo(CanonicalWriteDecisionType.ALLOW);

    assertThat(gate.decide(new CanonicalWriteRequest(
        bulkAcknowledged,
        true,
        VerificationStatus.CANDIDATE_CONFIRMED,
        RiskTier.T1_LOW_RISK,
        false,
        false,
        true)).reasons()).contains("bulk_approve_cannot_create_candidate_confirmed");

    assertThat(gate.decide(new CanonicalWriteRequest(
        bulkAcknowledged,
        true,
        VerificationStatus.EXTERNAL_VERIFIED,
        RiskTier.T1_LOW_RISK,
        false,
        false,
        true)).reasons()).contains("bulk_approve_cannot_create_external_verified");

    assertThat(gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.INTERNAL_ONLY, false),
        true,
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW_RISK,
        true,
        false,
        true)).reasons()).contains("internal_only_claim_cannot_be_client_visible");

    assertThat(gate.decide(new CanonicalWriteRequest(
        claim(ClaimType.FACT, AssertionStrength.EXPLICIT, VerificationStatus.HUMAN_ACKNOWLEDGED,
            ClientShareability.CONSENT_REQUIRED, false),
        true,
        VerificationStatus.HUMAN_ACKNOWLEDGED,
        RiskTier.T1_LOW_RISK,
        true,
        false,
        false)).type()).isEqualTo(CanonicalWriteDecisionType.REQUIRE_REVIEW);
  }

  private static CanonicalWriteGate gate() {
    return new CanonicalWriteGate();
  }

  private static ClaimInput claim(
      ClaimType type,
      AssertionStrength strength,
      VerificationStatus verificationStatus,
      ClientShareability shareability,
      boolean bulkApproved) {
    return new ClaimInput(type, strength, verificationStatus, shareability, bulkApproved);
  }

  private static JsonNode readSchema(String fileName) throws IOException {
    return OBJECT_MAPPER.readTree(CONTRACT_SCHEMA_DIR.resolve(fileName).toFile());
  }

  private static Set<String> contractEnum(String defName) throws IOException {
    return stringSet(readSchema("truth-layer.enums.schema.json")
        .path("$defs")
        .path(defName)
        .path("enum"));
  }

  private static Set<String> allContractEnumValues() throws IOException {
    Set<String> values = new LinkedHashSet<>();
    collectEnumValues(readSchema("truth-layer.enums.schema.json"), values);
    return values;
  }

  private static void collectEnumValues(JsonNode node, Set<String> values) {
    if (node == null || node.isMissingNode()) {
      return;
    }
    if (node.isObject() && node.has("enum")) {
      values.addAll(stringSet(node.path("enum")));
    }
    if (node.isContainerNode()) {
      node.elements().forEachRemaining(child -> collectEnumValues(child, values));
    }
  }

  private static Set<String> sqlEnumValues(String qualifiedTypeName) throws IOException {
    Pattern pattern = Pattern.compile(
        "CREATE\\s+TYPE\\s+" + Pattern.quote(qualifiedTypeName) + "\\s+AS\\s+ENUM\\s*\\((.*?)\\);",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Matcher matcher = pattern.matcher(readMigrationSql());
    assertThat(matcher.find()).as("SQL enum %s should exist", qualifiedTypeName).isTrue();

    Matcher valueMatcher = Pattern.compile("'([^']+)'").matcher(matcher.group(1));
    Set<String> values = new LinkedHashSet<>();
    while (valueMatcher.find()) {
      values.add(valueMatcher.group(1));
    }
    return values;
  }

  private static String readMigrationSql() throws IOException {
    assertThat(MIGRATION).exists().isRegularFile();
    return Files.readString(MIGRATION);
  }

  private static String normalizedMigrationSql() throws IOException {
    return readMigrationSql().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private static Set<String> stringSet(JsonNode arrayNode) {
    assertThat(arrayNode.isArray()).as("expected JSON array node but got %s", arrayNode).isTrue();
    List<String> values = new ArrayList<>();
    arrayNode.forEach(value -> values.add(value.asText()));
    return new LinkedHashSet<>(values);
  }

  private static <E extends Enum<E>> Set<String> wireValues(
      E[] values,
      Function<E, String> wireValueExtractor) {
    return Arrays.stream(values)
        .map(wireValueExtractor)
        .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
  }
}
