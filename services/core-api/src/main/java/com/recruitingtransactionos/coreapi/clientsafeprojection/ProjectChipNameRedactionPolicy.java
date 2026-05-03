package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task 30 chip / product / project code-name redaction.
 *
 * <p>The pipeline calls this policy with a candidate-supplied free-text value
 * and the snapshot's declared list of exact project / product / chip names.
 * It returns the input with every match replaced by the literal token
 * {@code [REDACTED_CHIP_CODE_NAME]} (or the corresponding generic token), and
 * an explanation of which patterns matched.
 *
 * <p>Three sources of redaction are combined:
 * <ol>
 *   <li>The snapshot's declared exact project/product/chip names. These are
 *       always redacted first because the consultant marked them as
 *       sensitive.</li>
 *   <li>A small list of well-known public chip code-name families
 *       (e.g. Apple A-series, Apple M-series, NVIDIA H100/B200,
 *       Qualcomm Snapdragon, Google Tensor, Huawei Ascend/Kirin). These are
 *       commonly used in resumes and immediately re-identifying when paired
 *       with a top-tier company name and an exact year.</li>
 *   <li>A generic regex pattern for typical chip code-name shapes such as
 *       {@code "Orion-X7"}, {@code "Atlas 9000"}, or {@code "Falcon-Q1"} —
 *       single capitalized word, optional hyphen/space, alphanumeric suffix
 *       — when the surrounding context already hints at chip work
 *       (verification, tape-out, NPU, DSP, etc.).</li>
 * </ol>
 *
 * <p>This is intentionally a conservative redactor. False positives produce a
 * generalized output that is still useful to the client; false negatives
 * leak identity. v1 prefers the false positive.
 */
public final class ProjectChipNameRedactionPolicy {

  /** Replacement token for known chip code names. */
  public static final String REDACTED_CHIP_CODE_NAME_TOKEN =
      "[redacted-chip-code-name]";

  private static final List<Pattern> KNOWN_CHIP_FAMILY_PATTERNS = List.of(
      // Apple A-series, M-series, S-series, R-series, T-series chips.
      Pattern.compile("\\b[Aa]\\d{1,3}(?:\\s*[A-Za-z]{1,3})?\\b"),
      Pattern.compile("\\b[Mm]\\d{1,2}(?:\\s*(?:Pro|Max|Ultra))?\\b"),
      Pattern.compile("\\b[Ss]\\d{1,2}\\b"),
      Pattern.compile("\\b[Rr]\\d{1,2}\\b"),
      Pattern.compile("\\b[Tt]\\d{1,2}\\b"),
      // NVIDIA datacenter and gaming families.
      Pattern.compile("\\b[Hh]100\\b"),
      Pattern.compile("\\b[Hh]200\\b"),
      Pattern.compile("\\b[Aa]100\\b"),
      Pattern.compile("\\b[Bb]100\\b"),
      Pattern.compile("\\b[Bb]200\\b"),
      Pattern.compile("\\b[Gg][Bb]200\\b"),
      Pattern.compile("\\bRTX\\s*\\d{3,5}\\b"),
      Pattern.compile("\\bGTX\\s*\\d{3,5}\\b"),
      // Qualcomm Snapdragon naming.
      Pattern.compile("\\bSnapdragon\\s+(?:[A-Za-z]\\s*)?\\d{1,4}(?:\\s*[A-Za-z0-9]+)?\\b",
          Pattern.CASE_INSENSITIVE),
      // Google Tensor / Pixel chip naming.
      Pattern.compile("\\bTensor\\s+G\\d+\\b", Pattern.CASE_INSENSITIVE),
      // Huawei Kirin / Ascend.
      Pattern.compile("\\bKirin\\s*\\d{2,4}[A-Za-z]?\\b", Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bAscend\\s*\\d{2,4}[A-Za-z]?\\b", Pattern.CASE_INSENSITIVE),
      // Intel Xeon / Core / Atom branded SKUs.
      Pattern.compile(
          "\\b(?:Xeon|Core\\s+i[3579]|Atom)\\s*[A-Za-z0-9-]+\\b",
          Pattern.CASE_INSENSITIVE),
      // AMD EPYC / Ryzen / MI families.
      Pattern.compile("\\b(?:EPYC|Ryzen)\\s*\\d{3,5}[A-Za-z]?\\b",
          Pattern.CASE_INSENSITIVE),
      Pattern.compile("\\bMI\\d{2,4}[A-Za-z]?\\b"),
      // MediaTek Dimensity / Helio.
      Pattern.compile("\\b(?:Dimensity|Helio)\\s*\\d{2,4}[A-Za-z]?\\b",
          Pattern.CASE_INSENSITIVE));

  /**
   * Catch-all generic chip code-name shape: capitalized stem + optional
   * separator + alphanumeric suffix, e.g. "Orion-X7", "Atlas 9000",
   * "Falcon Q1". Only triggered when the surrounding text already hints at
   * chip work.
   */
  private static final Pattern GENERIC_CHIP_CODE_NAME_PATTERN = Pattern.compile(
      "\\b[A-Z][A-Za-z]{2,}(?:[-\\s][A-Z]?\\d+[A-Za-z]?)\\b");

  /** Tokens whose presence signals "this resume is talking about chips". */
  private static final List<String> CHIP_CONTEXT_TOKENS = List.of(
      "chip", "asic", "soc", "fpga", "npu", "tpu", "gpu", "dsp", "cpu",
      "tape-out", "tapeout", "tape out", "verification", "rtl", "synthesis",
      "physical design", "place and route", "p&r", "uvm", "systemverilog",
      "lithography", "yield", "wafer", "foundry", "transistor", "bitline",
      "instruction set", "isa", "verilog", "back end of line", "beol");

  private ProjectChipNameRedactionPolicy() {
  }

  /**
   * Result of applying the policy to a single text value.
   */
  public record Redaction(
      String redactedText,
      Set<String> redactedExactNames,
      boolean knownChipFamilyMatched,
      boolean genericChipShapeMatched) {

    public Redaction {
      Objects.requireNonNull(redactedText, "redactedText must not be null");
      redactedExactNames = Set.copyOf(redactedExactNames);
    }

    public boolean anyRedaction() {
      return !redactedExactNames.isEmpty()
          || knownChipFamilyMatched
          || genericChipShapeMatched;
    }
  }

  public static Redaction redact(String text, List<String> declaredExactNames) {
    Objects.requireNonNull(declaredExactNames, "declaredExactNames must not be null");
    if (text == null || text.isBlank()) {
      return new Redaction(text == null ? "" : text, Set.of(), false, false);
    }
    String working = text;
    Set<String> redactedNames = new LinkedHashSet<>();

    for (String declared : declaredExactNames) {
      if (declared == null || declared.isBlank()) {
        continue;
      }
      String pattern = Pattern.quote(declared.strip());
      Pattern compiled = Pattern.compile("(?i)" + pattern);
      Matcher matcher = compiled.matcher(working);
      if (matcher.find()) {
        working = matcher.replaceAll(REDACTED_CHIP_CODE_NAME_TOKEN);
        redactedNames.add(declared.strip());
      }
    }

    boolean knownMatched = false;
    for (Pattern pattern : KNOWN_CHIP_FAMILY_PATTERNS) {
      Matcher matcher = pattern.matcher(working);
      if (matcher.find()) {
        working = matcher.replaceAll(REDACTED_CHIP_CODE_NAME_TOKEN);
        knownMatched = true;
      }
    }

    boolean genericMatched = false;
    if (mentionsChipContext(working) || mentionsChipContext(text)) {
      Matcher matcher = GENERIC_CHIP_CODE_NAME_PATTERN.matcher(working);
      if (matcher.find()) {
        working = matcher.replaceAll(REDACTED_CHIP_CODE_NAME_TOKEN);
        genericMatched = true;
      }
    }

    return new Redaction(working, redactedNames, knownMatched, genericMatched);
  }

  /**
   * Returns true when the text clearly references chip / silicon work and
   * therefore should be subjected to the generic chip-code-name redactor in
   * addition to the curated patterns.
   */
  public static boolean mentionsChipContext(String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    String normalized = text.toLowerCase(Locale.ROOT);
    for (String token : CHIP_CONTEXT_TOKENS) {
      if (normalized.contains(token)) {
        return true;
      }
    }
    return false;
  }
}
