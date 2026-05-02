package com.recruitingtransactionos.coreapi.company.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.governedintake.service.IntakeReviewQueryService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CompanyIntakeApplicationService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final CompanyService companyService;

  public CompanyIntakeApplicationService(CompanyService companyService) {
    this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
  }

  public Company upsertClientProfile(
      UUID organizationId,
      UUID actorId,
      Optional<CompanyId> existingCompanyId,
      String name,
      String displayName,
      String industry,
      String website,
      String headquartersLocation,
      String sizeBand,
      String paymentReliability) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Instant now = Instant.now();

    if (existingCompanyId.isPresent()) {
      Company existing = companyService.findCompanyByIdAndOrganizationId(organizationId, existingCompanyId.get())
          .orElseThrow(() -> new IllegalArgumentException("company_not_found_in_organization"));
      if (!metadataContainsActor(existing.metadata(), actorId)) {
        throw new IllegalArgumentException("client_company_profile_not_owned_by_actor");
      }
      Company updated = Company.builder()
          .companyId(existing.companyId())
          .organizationId(organizationId)
          .name(name)
          .displayName(displayName)
          .industry(industry)
          .website(website)
          .headquartersLocation(headquartersLocation)
          .sizeBand(sizeBand)
          .status(existing.status())
          .paymentReliability(paymentReliability)
          .ownerConsultantId(existing.ownerConsultantId())
          .metadata(mergeMetadata(existing.metadata(), Map.of(
              "clientProfileSubmitted", true,
              "clientActorId", actorId.toString(),
              "clientProfileUpdatedAt", now.toString())))
          .createdAt(existing.createdAt())
          .updatedAt(now)
          .version(existing.version())
          .build();
      return companyService.updateCompany(updated);
    }

    return companyService.createCompany(Company.builder()
        .companyId(new CompanyId(UUID.randomUUID()))
        .organizationId(organizationId)
        .name(name)
        .displayName(displayName)
        .industry(industry)
        .website(website)
        .headquartersLocation(headquartersLocation)
        .sizeBand(sizeBand)
        .status(CompanyStatus.NEW)
        .paymentReliability(paymentReliability)
        .metadata(mergeMetadata(null, Map.of(
            "clientProfileSubmitted", true,
            "clientActorId", actorId.toString(),
            "clientProfileCreatedAt", now.toString(),
            "profileSource", "client_portal")))
        .createdAt(now)
        .updatedAt(now)
        .version(1)
        .build());
  }

  public static boolean metadataContainsActor(String metadataJson, UUID actorId) {
    if (actorId == null) {
      return false;
    }
    return clientActorIdFromMetadata(metadataJson)
        .map(actorId::equals)
        .orElse(false);
  }

  public Company applyReviewedFacts(
      UUID organizationId,
      UUID actorId,
      Optional<CompanyId> requestedCompanyId,
      List<IntakeReviewQueryService.ReviewedCleanFact> facts,
      String reason) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(actorId, "actorId must not be null");
    Objects.requireNonNull(facts, "facts must not be null");
    Instant now = Instant.now();

    Company base = requestedCompanyId.flatMap(id -> companyService.findCompanyByIdAndOrganizationId(organizationId, id))
        .orElseGet(() -> Company.builder()
            .companyId(new CompanyId(UUID.randomUUID()))
            .organizationId(organizationId)
            .name("Pending company profile")
            .status(CompanyStatus.NEW)
            .metadata("{}")
            .createdAt(now)
            .updatedAt(now)
            .version(1)
            .build());

    String name = firstApprovedValue(facts, "name", "company.name")
        .orElse(base.name());
    String displayName = firstApprovedValue(facts, "displayName", "company.display_name")
        .orElse(base.displayName());
    String industry = firstApprovedValue(facts, "industry", "company.industry")
        .orElse(base.industry());
    String website = firstApprovedValue(facts, "website", "company.website")
        .orElse(base.website());
    String headquartersLocation = firstApprovedValue(
        facts,
        "headquartersLocation",
        "company.headquarters_location").orElse(base.headquartersLocation());
    String sizeBand = firstApprovedValue(facts, "sizeBand", "company.size_band")
        .orElse(base.sizeBand());

    Company next = Company.builder()
        .companyId(base.companyId())
        .organizationId(organizationId)
        .name(name)
        .displayName(displayName)
        .industry(industry)
        .website(website)
        .headquartersLocation(headquartersLocation)
        .sizeBand(sizeBand)
        .status(base.status())
        .paymentReliability(base.paymentReliability())
        .ownerConsultantId(base.ownerConsultantId())
        .metadata(mergeMetadata(base.metadata(), Map.of(
            "intakePublishedAt", now.toString(),
            "intakePublishedByActorId", actorId.toString(),
            "intakePublishReason", safeReason(reason))))
        .createdAt(base.createdAt())
        .updatedAt(now)
        .version(base.version())
        .build();

    return requestedCompanyId.isPresent() || companyExists(base)
        ? companyService.updateCompany(next)
        : companyService.createCompany(next);
  }

  private static boolean companyExists(Company company) {
    return company.version() > 1 || company.ownerConsultantId() != null || company.displayName() != null;
  }

  private static Optional<String> firstApprovedValue(
      List<IntakeReviewQueryService.ReviewedCleanFact> facts,
      String... acceptedFieldPaths) {
    for (IntakeReviewQueryService.ReviewedCleanFact fact : facts) {
      if (fact.latestReview() == null || fact.claimId() == null) {
        continue;
      }
      if (!"APPROVED".equals(fact.latestReview().decision().name())) {
        continue;
      }
      for (String acceptedFieldPath : acceptedFieldPaths) {
        if (acceptedFieldPath.equalsIgnoreCase(fact.candidate().targetFieldPath())) {
          return Optional.ofNullable(fact.candidate().proposedValue());
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<UUID> clientActorIdFromMetadata(String metadataJson) {
    if (metadataJson == null || metadataJson.isBlank()) {
      return Optional.empty();
    }
    try {
      Map<String, Object> metadata = OBJECT_MAPPER.readValue(metadataJson, MAP_TYPE);
      Object value = metadata.get("clientActorId");
      if (!(value instanceof String text) || text.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(UUID.fromString(text.strip()));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private static String mergeMetadata(String existingJson, Map<String, Object> patch) {
    try {
      Map<String, Object> merged = new LinkedHashMap<>();
      if (existingJson != null && !existingJson.isBlank()) {
        merged.putAll(OBJECT_MAPPER.readValue(existingJson, MAP_TYPE));
      }
      merged.putAll(patch);
      return OBJECT_MAPPER.writeValueAsString(merged);
    } catch (Exception exception) {
      throw new IllegalArgumentException("invalid_company_metadata_json", exception);
    }
  }

  private static String safeReason(String reason) {
    return reason == null || reason.isBlank() ? "company intake publish" : reason.strip();
  }
}
