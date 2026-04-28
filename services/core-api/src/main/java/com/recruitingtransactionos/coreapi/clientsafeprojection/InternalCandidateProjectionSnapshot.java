package com.recruitingtransactionos.coreapi.clientsafeprojection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record InternalCandidateProjectionSnapshot(
    String rawCandidateId,
    String rawCandidateProfileId,
    String fullName,
    String email,
    String phone,
    String linkedInUrl,
    String exactCurrentEmployer,
    List<String> exactProjectProductOrChipNames,
    String rawSourceText,
    String consultantInternalNotes,
    AnonymousCandidateCardId cardId,
    AnonymousCandidateRef anonymousCandidateRef,
    String projectionVersion,
    RedactionLevel redactionLevel,
    String generalizedHeadline,
    String generalizedRoleFamily,
    String generalizedSeniorityBand,
    String generalizedLocationRegion,
    String safeSummary,
    String safeSkillSummary,
    List<String> safeEvidenceSummaries,
    List<String> safeMatchNarratives,
    Set<String> selectedClientVisibleFieldPaths) {

  public InternalCandidateProjectionSnapshot {
    rawCandidateId = optionalNonBlank(rawCandidateId);
    rawCandidateProfileId = optionalNonBlank(rawCandidateProfileId);
    fullName = optionalNonBlank(fullName);
    email = optionalNonBlank(email);
    phone = optionalNonBlank(phone);
    linkedInUrl = optionalNonBlank(linkedInUrl);
    exactCurrentEmployer = optionalNonBlank(exactCurrentEmployer);
    exactProjectProductOrChipNames = copyOptionalNonBlankList(
        exactProjectProductOrChipNames,
        "exactProjectProductOrChipNames");
    rawSourceText = optionalNonBlank(rawSourceText);
    consultantInternalNotes = optionalNonBlank(consultantInternalNotes);
    Objects.requireNonNull(cardId, "cardId must not be null");
    Objects.requireNonNull(anonymousCandidateRef, "anonymousCandidateRef must not be null");
    projectionVersion =
        ClientSafeProjectionGuards.requireNonBlank(projectionVersion, "projectionVersion");
    Objects.requireNonNull(redactionLevel, "redactionLevel must not be null");
    generalizedHeadline =
        ClientSafeProjectionGuards.requireNonBlank(generalizedHeadline, "generalizedHeadline");
    generalizedRoleFamily =
        ClientSafeProjectionGuards.requireNonBlank(
            generalizedRoleFamily, "generalizedRoleFamily");
    generalizedSeniorityBand =
        ClientSafeProjectionGuards.requireNonBlank(
            generalizedSeniorityBand, "generalizedSeniorityBand");
    generalizedLocationRegion =
        ClientSafeProjectionGuards.requireNonBlank(
            generalizedLocationRegion, "generalizedLocationRegion");
    safeSummary = ClientSafeProjectionGuards.requireNonBlank(safeSummary, "safeSummary");
    safeSkillSummary =
        ClientSafeProjectionGuards.requireNonBlank(safeSkillSummary, "safeSkillSummary");
    safeEvidenceSummaries =
        ClientSafeProjectionGuards.copyNonBlankList(
            safeEvidenceSummaries,
            "safeEvidenceSummaries");
    safeMatchNarratives =
        ClientSafeProjectionGuards.copyNonBlankList(safeMatchNarratives, "safeMatchNarratives");
    selectedClientVisibleFieldPaths = copySelectedFieldPaths(selectedClientVisibleFieldPaths);
  }

  List<String> rawSensitiveValues() {
    List<String> values = new ArrayList<>();
    addIfPresent(values, rawCandidateId);
    addIfPresent(values, rawCandidateProfileId);
    addIfPresent(values, fullName);
    addIfPresent(values, email);
    addIfPresent(values, phone);
    addIfPresent(values, linkedInUrl);
    addIfPresent(values, exactCurrentEmployer);
    exactProjectProductOrChipNames.forEach(value -> addIfPresent(values, value));
    addIfPresent(values, rawSourceText);
    addIfPresent(values, consultantInternalNotes);
    return List.copyOf(values);
  }

  List<String> projectedTextValues() {
    List<String> values = new ArrayList<>();
    values.add(cardId.value());
    values.add(anonymousCandidateRef.value());
    values.add(projectionVersion);
    values.add(redactionLevel.wireValue());
    values.add(generalizedHeadline);
    values.add(generalizedRoleFamily);
    values.add(generalizedSeniorityBand);
    values.add(generalizedLocationRegion);
    values.add(safeSummary);
    values.add(safeSkillSummary);
    values.addAll(safeEvidenceSummaries);
    values.addAll(safeMatchNarratives);
    return List.copyOf(values);
  }

  private static String optionalNonBlank(String value) {
    if (value == null) {
      return null;
    }
    String stripped = value.strip();
    if (stripped.isBlank()) {
      return null;
    }
    return stripped;
  }

  private static List<String> copyOptionalNonBlankList(List<String> values, String name) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .map(value -> ClientSafeProjectionGuards.requireNonBlank(value, name + " entry"))
        .toList();
  }

  private static Set<String> copySelectedFieldPaths(Set<String> selectedFieldPaths) {
    Objects.requireNonNull(
        selectedFieldPaths,
        "selectedClientVisibleFieldPaths must not be null");
    LinkedHashSet<String> copied = new LinkedHashSet<>();
    for (String fieldPath : selectedFieldPaths) {
      copied.add(ClientSafeProjectionGuards.requireNonBlank(
          fieldPath,
          "selectedClientVisibleFieldPaths entry"));
    }
    return Set.copyOf(copied);
  }

  private static void addIfPresent(List<String> values, String value) {
    if (value != null && !value.isBlank()) {
      values.add(value);
    }
  }
}
