package com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCandidateDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCandidateSummaryResponse;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldConflictValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ConsultantCandidateResponseMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private ConsultantCandidateResponseMapper() {}

  public static ConsultantCandidateSummaryResponse toSummary(Candidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    return new ConsultantCandidateSummaryResponse(
        candidate.candidateId().value().toString(),
        candidate.status().wireValue(),
        candidate.privacyStatus(),
        candidate.currentProfileId() != null ? candidate.currentProfileId().value().toString() : null,
        candidate.ownerConsultantId() != null ? candidate.ownerConsultantId().toString() : null,
        candidate.lastActivityAt() != null ? candidate.lastActivityAt().toString() : null,
        candidate.createdAt().toString());
  }

  public static ConsultantCandidateDetailResponse toDetail(
      Candidate candidate,
      CandidateProfile candidateProfile) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    List<CandidateProfileField> fields = candidateProfile != null ? candidateProfile.fields() : List.of();
    return new ConsultantCandidateDetailResponse(
        candidate.candidateId().value().toString(),
        candidate.status().wireValue(),
        candidate.privacyStatus(),
        candidate.currentProfileId() != null ? candidate.currentProfileId().value().toString() : null,
        candidateProfile != null ? String.valueOf(candidateProfile.profileVersion().value()) : null,
        candidate.ownerConsultantId() != null ? candidate.ownerConsultantId().toString() : null,
        candidate.lastActivityAt() != null ? candidate.lastActivityAt().toString() : null,
        candidate.doNotContactReason(),
        candidate.mergedIntoCandidateId() != null
            ? candidate.mergedIntoCandidateId().value().toString() : null,
        candidate.defaultIndustryPackId() != null ? candidate.defaultIndustryPackId().toString() : null,
        candidate.createdAt().toString(),
        candidate.updatedAt().toString(),
        toOverview(fields),
        toEvidence(fields),
        toConflicts(fields),
        toStaleInfo(fields),
        toFollowUps(fields),
        toHistory(candidate, candidateProfile, fields));
  }

  private static List<ConsultantCandidateDetailResponse.OverviewItem> toOverview(
      List<CandidateProfileField> fields) {
    return fields.stream()
        .sorted(Comparator.comparing(field -> field.fieldPath().value()))
        .map(field -> new ConsultantCandidateDetailResponse.OverviewItem(
            field.fieldPath().value(),
            toLabel(field.fieldPath().value()),
            renderJsonValue(field.value().jsonValue()),
            field.fieldStatus().wireValue(),
            field.lastReviewedAt() != null ? field.lastReviewedAt().toString() : null,
            field.notes()))
        .toList();
  }

  private static List<ConsultantCandidateDetailResponse.EvidenceItem> toEvidence(
      List<CandidateProfileField> fields) {
    List<ConsultantCandidateDetailResponse.EvidenceItem> items = new ArrayList<>();
    for (CandidateProfileField field : fields) {
      field.lineage().sourceReferences().forEach(reference -> items.add(
          new ConsultantCandidateDetailResponse.EvidenceItem(
              field.fieldPath().value(),
              reference.sourceType().wireValue(),
              reference.sourceId(),
              reference.sourceTrust(),
              field.lineage().provenanceLabel(),
              reference.createdAt().toString())));
    }
    return items;
  }

  private static List<ConsultantCandidateDetailResponse.ConflictItem> toConflicts(
      List<CandidateProfileField> fields) {
    return fields.stream()
        .filter(field -> field.conflict() != null)
        .map(field -> new ConsultantCandidateDetailResponse.ConflictItem(
            field.fieldPath().value(),
            field.conflict().severity().name().toLowerCase(),
            field.conflict().resolutionStatus().name().toLowerCase(),
            field.conflict().conflictingValues().stream()
                .map(ConsultantCandidateResponseMapper::renderConflictValue)
                .toList(),
            field.conflict().detectedAt().toString(),
            field.conflict().notes()))
        .toList();
  }

  private static List<ConsultantCandidateDetailResponse.StaleInfoItem> toStaleInfo(
      List<CandidateProfileField> fields) {
    return fields.stream()
        .filter(field -> field.staleness() != null && field.staleness().stale())
        .map(field -> new ConsultantCandidateDetailResponse.StaleInfoItem(
            field.fieldPath().value(),
            field.staleness().staleReason(),
            field.staleness().reviewBy() != null ? field.staleness().reviewBy().toString() : null,
            field.staleness().lastConfirmedAt() != null
                ? field.staleness().lastConfirmedAt().toString()
                : null,
            field.staleness().detectedAt().toString()))
        .toList();
  }

  private static List<ConsultantCandidateDetailResponse.FollowUpItem> toFollowUps(
      List<CandidateProfileField> fields) {
    List<ConsultantCandidateDetailResponse.FollowUpItem> items = new ArrayList<>();
    for (CandidateProfileField field : fields) {
      if (field.fieldStatus() == CandidateProfileFieldStatus.NEEDS_CONFIRMATION) {
        items.add(new ConsultantCandidateDetailResponse.FollowUpItem(
            field.fieldPath().value(),
            "needs_confirmation",
            "Field is waiting for consultant or candidate confirmation.",
            "Review the evidence and confirm the current value."));
      }
      if (field.fieldStatus() == CandidateProfileFieldStatus.CONFLICTING && field.conflict() != null) {
        items.add(new ConsultantCandidateDetailResponse.FollowUpItem(
            field.fieldPath().value(),
            "conflict_resolution",
            "Multiple source-backed values still conflict.",
            "Resolve the conflict before treating this field as current."));
      }
      if (field.staleness() != null && field.staleness().stale()) {
        items.add(new ConsultantCandidateDetailResponse.FollowUpItem(
            field.fieldPath().value(),
            "stale_refresh",
            field.staleness().staleReason(),
            "Refresh this field with a newer source or direct confirmation."));
      }
    }
    return items;
  }

  private static List<ConsultantCandidateDetailResponse.HistoryItem> toHistory(
      Candidate candidate,
      CandidateProfile candidateProfile,
      List<CandidateProfileField> fields) {
    List<ConsultantCandidateDetailResponse.HistoryItem> items = new ArrayList<>();
    items.add(new ConsultantCandidateDetailResponse.HistoryItem(
        "candidate_created",
        null,
        "Candidate record was created.",
        candidate.createdAt().toString()));
    items.add(new ConsultantCandidateDetailResponse.HistoryItem(
        "candidate_updated",
        null,
        "Candidate record was last updated.",
        candidate.updatedAt().toString()));
    if (candidateProfile != null) {
      items.add(new ConsultantCandidateDetailResponse.HistoryItem(
          "profile_created",
          null,
          "Current profile version was created.",
          candidateProfile.createdAt().toString()));
      items.add(new ConsultantCandidateDetailResponse.HistoryItem(
          "profile_updated",
          null,
          "Current profile version was last updated.",
          candidateProfile.updatedAt().toString()));
    }
    fields.stream()
        .filter(field -> field.lastReviewedAt() != null)
        .sorted(Comparator.comparing(CandidateProfileField::lastReviewedAt).reversed())
        .forEach(field -> items.add(new ConsultantCandidateDetailResponse.HistoryItem(
            "field_reviewed",
            field.fieldPath().value(),
            "Field was reviewed in the governed profile.",
            field.lastReviewedAt().toString())));
    return items.stream()
        .sorted(Comparator.comparing(
            (ConsultantCandidateDetailResponse.HistoryItem item) -> Instant.parse(item.occurredAt()))
            .reversed())
        .toList();
  }

  private static String renderConflictValue(CandidateProfileFieldConflictValue value) {
    return renderJsonValue(value.value().jsonValue());
  }

  private static String renderJsonValue(String jsonValue) {
    try {
      JsonNode node = OBJECT_MAPPER.readTree(jsonValue);
      if (node == null || node.isNull()) {
        return null;
      }
      if (node.isTextual()) {
        return node.asText();
      }
      if (node.isNumber() || node.isBoolean()) {
        return node.asText();
      }
      if (node.isArray()) {
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.isValueNode() ? item.asText() : item.toString()));
        return String.join(", ", values);
      }
      return node.toString();
    } catch (JsonProcessingException exception) {
      return jsonValue;
    }
  }

  private static String toLabel(String fieldPath) {
    String[] segments = fieldPath.split("\\.");
    String last = segments[segments.length - 1];
    String[] words = last.split("_");
    StringBuilder builder = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      if (!builder.isEmpty()) {
        builder.append(' ');
      }
      builder.append(Character.toUpperCase(word.charAt(0)));
      if (word.length() > 1) {
        builder.append(word.substring(1));
      }
    }
    return builder.toString();
  }
}
