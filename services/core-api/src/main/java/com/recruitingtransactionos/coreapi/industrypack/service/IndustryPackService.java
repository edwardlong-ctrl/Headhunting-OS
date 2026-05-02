package com.recruitingtransactionos.coreapi.industrypack.service;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPack;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackId;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackKey;
import com.recruitingtransactionos.coreapi.industrypack.IndustryRoleFamilyTemplate;
import com.recruitingtransactionos.coreapi.industrypack.OntologyVersion;
import com.recruitingtransactionos.coreapi.industrypack.port.IndustryPackReadPort;
import com.recruitingtransactionos.coreapi.job.Job;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class IndustryPackService {
  private static final IndustryPackKey GENERAL_PACK_KEY = new IndustryPackKey("general");
  private final IndustryPackReadPort readPort;

  public IndustryPackService(IndustryPackReadPort readPort) {
    this.readPort = Objects.requireNonNull(readPort, "readPort must not be null");
  }

  public ResolvedIndustryPack resolveForJobAndCandidate(Job job, Candidate candidate, Instant asOf) {
    Objects.requireNonNull(job, "job must not be null");
    Instant instant = asOf == null ? Instant.now() : asOf;
    IndustryPack industryPack;
    String selectionReason;
    if (job.industryPackId() != null) {
      industryPack = readPort.findById(new IndustryPackId(job.industryPackId()))
          .orElseThrow(() -> new IllegalStateException("Configured job industry pack is missing"));
      selectionReason = "job_industry_pack";
    } else if (candidate != null && candidate.defaultIndustryPackId() != null) {
      industryPack = readPort.findById(new IndustryPackId(candidate.defaultIndustryPackId()))
          .orElseThrow(() -> new IllegalStateException("Configured candidate default industry pack is missing"));
      selectionReason = "candidate_default_industry_pack";
    } else {
      industryPack = readPort.findByKey(GENERAL_PACK_KEY)
          .orElseThrow(() -> new IllegalStateException("General industry pack seed is missing"));
      selectionReason = "fallback_general";
    }
    OntologyVersion ontologyVersion = readPort.findActiveOntologyVersion(industryPack.industryPackId(), instant)
        .orElseThrow(() -> new IllegalStateException("Active ontology version is missing for pack " + industryPack.packKey().value()));
    String roleFamily = normalizeRoleFamily(job.roleFamily());
    Optional<IndustryRoleFamilyTemplate> template = roleFamily == null
        ? Optional.empty()
        : readPort.findRoleFamilyTemplate(industryPack.industryPackId(), ontologyVersion.ontologyVersionId(), roleFamily);
    return new ResolvedIndustryPack(
        industryPack,
        ontologyVersion,
        template.orElse(null),
        selectionReason,
        ontologyVersion.isStale(instant));
  }

  public Optional<IndustryPack> findIndustryPackById(java.util.UUID industryPackId) {
    if (industryPackId == null) {
      return Optional.empty();
    }
    return readPort.findById(new IndustryPackId(industryPackId));
  }

  public Optional<IndustryPack> findIndustryPackByKey(String industryPackKey) {
    if (industryPackKey == null || industryPackKey.isBlank()) {
      return Optional.empty();
    }
    return readPort.findByKey(new IndustryPackKey(industryPackKey));
  }

  public IndustryPackMatchProfile evaluateMatchProfile(
      ResolvedIndustryPack resolvedIndustryPack,
      String roleFamilyText,
      List<String> evidenceTexts,
      List<String> skillTexts) {
    Objects.requireNonNull(resolvedIndustryPack, "resolvedIndustryPack must not be null");
    String normalizedRoleFamily = normalizeRoleFamily(roleFamilyText);
    IndustryRoleFamilyTemplate template = resolvedIndustryPack.roleFamilyTemplate();
    if (template == null || normalizedRoleFamily == null || !template.roleFamily().equals(normalizedRoleFamily)) {
      return new IndustryPackMatchProfile(List.of(), false, false, false);
    }
    String combined = join(evidenceTexts, skillTexts);
    boolean requiredSkillEvidencePresent = template.requiredSkillKeys().stream().anyMatch(key -> containsKeyword(combined, key));
    List<String> warnings = new ArrayList<>();
    if ("semiconductor".equals(resolvedIndustryPack.industryPack().packKey().value())) {
      if ("dv_verification".equals(template.roleFamily())) {
        if (!requiredSkillEvidencePresent && containsAny(combined, List.of("software testing", "software qa", "manual testing", "test automation", "qa engineer"))) {
          warnings.add("Semiconductor DV anti-pattern detected: software testing or QA evidence is not equivalent to IC verification without SystemVerilog/UVM or verification ownership evidence.");
        }
      }
      if ("physical_design".equals(template.roleFamily())) {
        if (!requiredSkillEvidencePresent && containsAny(combined, List.of("pcb layout", "board layout", "cad drafting"))) {
          warnings.add("Semiconductor PD anti-pattern detected: PCB layout or generic CAD experience is not equivalent to chip physical design.");
        }
      }
      if ("dft".equals(template.roleFamily())) {
        if (!requiredSkillEvidencePresent && containsAny(combined, List.of("manufacturing test", "quality inspection", "factory testing"))) {
          warnings.add("Semiconductor DFT anti-pattern detected: manufacturing quality testing is not equivalent to DFT ownership.");
        }
      }
    }
    return new IndustryPackMatchProfile(warnings, !warnings.isEmpty(), requiredSkillEvidencePresent, true);
  }

  public List<String> buildInterviewQuestions(ResolvedIndustryPack resolvedIndustryPack) {
    if (resolvedIndustryPack == null || resolvedIndustryPack.roleFamilyTemplate() == null) {
      return List.of();
    }
    return resolvedIndustryPack.roleFamilyTemplate().interviewQuestionTemplates();
  }

  public List<String> buildEvidenceExamples(ResolvedIndustryPack resolvedIndustryPack) {
    if (resolvedIndustryPack == null || resolvedIndustryPack.roleFamilyTemplate() == null) {
      return List.of();
    }
    return resolvedIndustryPack.roleFamilyTemplate().evidenceExamples();
  }

  public String buildScoringGuidance(ResolvedIndustryPack resolvedIndustryPack) {
    if (resolvedIndustryPack == null || resolvedIndustryPack.roleFamilyTemplate() == null) {
      return null;
    }
    return resolvedIndustryPack.roleFamilyTemplate().scoringGuidance();
  }

  private static String join(List<String> evidenceTexts, List<String> skillTexts) {
    List<String> values = new ArrayList<>();
    if (evidenceTexts != null) {
      values.addAll(evidenceTexts);
    }
    if (skillTexts != null) {
      values.addAll(skillTexts);
    }
    return String.join("\n", values).toLowerCase(Locale.ROOT);
  }

  private static boolean containsAny(String haystack, List<String> needles) {
    return needles.stream().anyMatch(needle -> containsKeyword(haystack, needle));
  }

  private static boolean containsKeyword(String haystack, String needle) {
    if (haystack == null || haystack.isBlank() || needle == null || needle.isBlank()) {
      return false;
    }
    String normalizedHaystack = haystack.toLowerCase(Locale.ROOT);
    String normalizedNeedle = needle.replace('_', ' ').toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    if (normalizedNeedle.isBlank()) {
      return false;
    }
    String tokenPattern = Arrays.stream(normalizedNeedle.split(" "))
        .filter(token -> !token.isBlank())
        .map(Pattern::quote)
        .collect(java.util.stream.Collectors.joining("\\s+"));
    String boundaryAwarePattern = "(?<![a-z0-9])"
        + tokenPattern
        + "(?![a-z0-9])";
    return Pattern.compile(boundaryAwarePattern).matcher(normalizedHaystack).find();
  }

  private static String normalizeRoleFamily(String roleFamily) {
    if (roleFamily == null || roleFamily.isBlank()) {
      return null;
    }
    String normalized = roleFamily.strip().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_').replace('/', '_');
    while (normalized.contains("__")) {
      normalized = normalized.replace("__", "_");
    }
    if (normalized.contains("dv") || normalized.contains("verification")) {
      return "dv_verification";
    }
    if (normalized.contains("physical_design") || normalized.equals("pd")) {
      return "physical_design";
    }
    if (normalized.contains("analog") || normalized.contains("mixed")) {
      return "analog_mixed_signal";
    }
    if (normalized.contains("firmware") || normalized.contains("embedded")) {
      return "firmware_embedded";
    }
    return normalized;
  }
}
