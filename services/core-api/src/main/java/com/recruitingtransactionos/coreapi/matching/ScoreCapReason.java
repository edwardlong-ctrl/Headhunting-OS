package com.recruitingtransactionos.coreapi.matching;

public enum ScoreCapReason {
  NONE(
      "none",
      "No score cap was applied by the current metadata policy."),
  INSUFFICIENT_INDEPENDENT_HIGH_TRUST_EVIDENCE(
      "insufficient_independent_high_trust_evidence",
      "Independent high-trust evidence is insufficient for a top score."),
  COLD_INDUSTRY_PACK(
      "cold_industry_pack",
      "Cold industry-pack metadata caps this score until calibration improves."),
  KEYWORD_ONLY_WITHOUT_PROJECT_EVIDENCE(
      "keyword_only_without_project_evidence",
      "Keyword-only evidence without project evidence caps this score."),
  WEAK_SIGNAL_INTENT_ONLY(
      "weak_signal_intent_only",
      "Candidate intent is only a weak signal and needs stronger evidence."),
  STALE_ONTOLOGY_OR_INDUSTRY_PACK(
      "stale_ontology_or_industry_pack",
      "Stale ontology or industry-pack metadata caps this score."),
  HIGH_AUTHENTICITY_RISK(
      "high_authenticity_risk",
      "High authenticity risk requires review or stronger evidence before top scoring."),
  HIGH_REIDENTIFICATION_RISK(
      "high_reidentification_risk",
      "High re-identification risk blocks client delivery until privacy review."),
  POLICY_INPUT_REQUIRED(
      "policy_input_required",
      "Score-cap policy input is missing and the decision failed closed.");

  private final String wireValue;
  private final String safeExplanation;

  ScoreCapReason(String wireValue, String safeExplanation) {
    this.wireValue = MatchingGuards.requireSafeReasonCode(wireValue);
    this.safeExplanation = MatchingGuards.requireSafeExplanation(safeExplanation);
  }

  public String wireValue() {
    return wireValue;
  }

  public String safeExplanation() {
    return safeExplanation;
  }
}
