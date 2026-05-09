package com.recruitingtransactionos.coreapi.aitaskrunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

public final class DeterministicPilotAITaskProvider implements AITaskProvider {

  private final ObjectMapper objectMapper;

  public DeterministicPilotAITaskProvider(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public String providerKey() {
    return "deterministic";
  }

  @Override
  public AITaskProviderResponse execute(AITaskProviderRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    JsonNode output = switch (request.taskKey()) {
      case "candidate-profile-parser" -> candidateProfileParserOutput(request.inputPayload());
      case "authenticity-risk-assessor" -> authenticityRiskOutput();
      case "interview-feedback-structurer" -> interviewFeedbackOutput(request.inputPayload());
      default -> throw new AITaskProviderException(
          "deterministic_provider_unsupported_task",
          "deterministic_provider_unsupported_task");
    };
    return new AITaskProviderResponse(
        output,
        "[]",
        BigDecimal.ZERO,
        "deterministic-pilot-local",
        Duration.ZERO);
  }

  private ObjectNode candidateProfileParserOutput(JsonNode input) {
    String resumeText = input.path("resumeText").asText("");
    ObjectNode output = objectMapper.createObjectNode();
    output.put("headline", "ASIC verification engineer with UVM and PCIe evidence");
    output.put(
        "summary",
        "Candidate evidence mentions ASIC verification, SystemVerilog, UVM, PCIe, low-power verification, and Shanghai.");
    output.set("primarySkills", array("SystemVerilog", "UVM", "PCIe", "low-power verification"));
    output.set("projects", array("PCIe validation", "low-power verification"));
    output.set("timelineHighlights", array("Shanghai semiconductor verification track"));
    ArrayNode claims = output.putArray("claimCandidates");
    claims.add(claim(
        "headline",
        "ASIC verification",
        "Role family appears in the uploaded candidate material.",
        evidenceQuote(resumeText, "ASIC verification")));
    claims.add(claim(
        "primary_skills",
        "SystemVerilog | UVM | PCIe | low-power verification",
        "Skill terms appear explicitly in the uploaded candidate material.",
        evidenceQuote(resumeText, "SystemVerilog, UVM, PCIe, low-power verification")));
    claims.add(claim(
        "summary",
        "Shanghai semiconductor verification candidate",
        "Location appears explicitly in the uploaded candidate material.",
        evidenceQuote(resumeText, "Shanghai")));
    return output;
  }

  private ObjectNode authenticityRiskOutput() {
    ObjectNode output = objectMapper.createObjectNode();
    output.put("authenticityRisk", "low");
    output.put("specificityScore", 82);
    output.put("independentEvidenceGap", false);
    output.set("flags", array("deterministic_pilot_evidence_present"));
    return output;
  }

  private ObjectNode interviewFeedbackOutput(JsonNode input) {
    String feedbackText = interviewFeedbackText(input).toLowerCase(Locale.ROOT);
    boolean compensationConcern = feedbackText.contains("compensation");
    ObjectNode output = objectMapper.createObjectNode();
    output.put(
        "structuredSummary",
        compensationConcern
            ? "Client feedback is positive on technical fit and asks for compensation review."
            : "Client feedback is positive and should be reviewed by the consultant.");
    output.put("outcomeLabel", compensationConcern ? "compensation_mismatch" : "strong_fit");
    output.put("rejectReasonTaxonomy", compensationConcern ? "compensation_mismatch" : null);
    output.put("confidence", "medium");
    ArrayNode suggestions = output.putArray("suggestions");
    ObjectNode suggestion = suggestions.addObject();
    suggestion.put("scope", "interaction");
    suggestion.put("suggestionType", "outcome_label");
    suggestion.put("title", "Review interview feedback outcome");
    suggestion.put(
        "rationale",
        compensationConcern
            ? "Compensation expectation was called out in client feedback."
            : "Client feedback indicates a strong fit.");
    suggestion.put("outcomeLabel", compensationConcern ? "compensation_mismatch" : "strong_fit");
    suggestion.put("rejectReasonTaxonomy", compensationConcern ? "compensation_mismatch" : null);
    ObjectNode payload = suggestion.putObject("payload");
    payload.put("source", "deterministic_pilot_e2e");
    payload.put("needsConsultantReview", true);
    output.set("evidence", array("client_feedback_text"));
    ObjectNode calibration = output.putObject("calibrationSignal");
    calibration.put("source", "deterministic_pilot_e2e");
    calibration.put("compensationConcern", compensationConcern);
    return output;
  }

  private static String interviewFeedbackText(JsonNode input) {
    StringBuilder text = new StringBuilder();
    appendText(text, input.path("feedbackText").asText(null));
    appendText(text, input.path("strengths").asText(null));
    appendText(text, input.path("concerns").asText(null));
    appendText(text, input.path("notes").asText(null));
    JsonNode evidence = input.path("sourceEvidence");
    if (evidence.isArray()) {
      for (JsonNode item : evidence) {
        appendText(text, item.asText(null));
      }
    }
    return text.toString();
  }

  private static void appendText(StringBuilder target, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!target.isEmpty()) {
      target.append('\n');
    }
    target.append(value);
  }

  private ObjectNode claim(String fieldName, String fieldValue, String rationale, String evidenceQuote) {
    ObjectNode claim = objectMapper.createObjectNode();
    claim.put("fieldName", fieldName);
    claim.put("fieldValue", fieldValue);
    claim.put("rationale", rationale);
    claim.put("evidenceQuote", evidenceQuote);
    return claim;
  }

  private ArrayNode array(String... values) {
    ArrayNode array = objectMapper.createArrayNode();
    for (String value : values) {
      array.add(value);
    }
    return array;
  }

  private static String evidenceQuote(String haystack, String preferred) {
    if (haystack.toLowerCase(Locale.ROOT).contains(preferred.toLowerCase(Locale.ROOT))) {
      return preferred;
    }
    return haystack.isBlank() ? preferred : haystack.lines().findFirst().orElse(preferred);
  }
}
