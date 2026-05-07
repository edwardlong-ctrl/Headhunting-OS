package com.recruitingtransactionos.coreapi.pilotdata;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PilotDataset(
    String version,
    OrganizationSeed organization,
    List<AccountSeed> accounts,
    List<CompanySeed> companies,
    List<JobSeed> jobs,
    List<CandidateSeed> candidates,
    List<SourceDocumentSeed> sourceDocuments) {

  public PilotDataset {
    version = requireNonBlank(version, "version");
    Objects.requireNonNull(organization, "organization must not be null");
    accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts must not be null"));
    companies = List.copyOf(Objects.requireNonNull(companies, "companies must not be null"));
    jobs = List.copyOf(Objects.requireNonNull(jobs, "jobs must not be null"));
    candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates must not be null"));
    sourceDocuments = List.copyOf(Objects.requireNonNull(
        sourceDocuments,
        "sourceDocuments must not be null"));
  }

  public PilotDataset withCandidates(List<CandidateSeed> replacementCandidates) {
    return new PilotDataset(
        version,
        organization,
        accounts,
        companies,
        jobs,
        replacementCandidates,
        sourceDocuments);
  }

  public record OrganizationSeed(
      UUID organizationId,
      String legalName,
      String displayName,
      String defaultTimezone) {

    public OrganizationSeed {
      Objects.requireNonNull(organizationId, "organizationId must not be null");
      legalName = requireNonBlank(legalName, "legalName");
      displayName = requireNonBlank(displayName, "displayName");
      defaultTimezone = requireNonBlank(defaultTimezone, "defaultTimezone");
    }
  }

  public record AccountSeed(
      UUID userAccountId,
      String email,
      String displayName,
      String role,
      String password) {

    public AccountSeed {
      Objects.requireNonNull(userAccountId, "userAccountId must not be null");
      email = requireNonBlank(email, "email").toLowerCase();
      displayName = requireNonBlank(displayName, "displayName");
      role = requireNonBlank(role, "role");
      password = requireNonBlank(password, "password");
    }
  }

  public record CompanySeed(
      UUID companyId,
      String name,
      String industry,
      String headquartersLocation,
      String sizeBand,
      String ownerConsultantId,
      String metadata) {

    public CompanySeed {
      Objects.requireNonNull(companyId, "companyId must not be null");
      name = requireNonBlank(name, "name");
      industry = requireNonBlank(industry, "industry");
      headquartersLocation = requireNonBlank(headquartersLocation, "headquartersLocation");
      sizeBand = requireNonBlank(sizeBand, "sizeBand");
      ownerConsultantId = requireNonBlank(ownerConsultantId, "ownerConsultantId");
      metadata = requireJsonObject(metadata, "metadata");
    }
  }

  public record JobSeed(
      UUID jobId,
      UUID companyId,
      String title,
      String status,
      String roleFamily,
      String seniorityBand,
      String location,
      String compensation,
      String ownerConsultantId,
      String sourceDocumentRef,
      String metadata) {

    public JobSeed {
      Objects.requireNonNull(jobId, "jobId must not be null");
      Objects.requireNonNull(companyId, "companyId must not be null");
      title = requireNonBlank(title, "title");
      status = requireNonBlank(status, "status");
      roleFamily = requireNonBlank(roleFamily, "roleFamily");
      seniorityBand = requireNonBlank(seniorityBand, "seniorityBand");
      location = requireJsonObject(location, "location");
      compensation = requireJsonObject(compensation, "compensation");
      ownerConsultantId = requireNonBlank(ownerConsultantId, "ownerConsultantId");
      sourceDocumentRef = requireNonBlank(sourceDocumentRef, "sourceDocumentRef");
      metadata = requireJsonObject(metadata, "metadata");
    }
  }

  public record CandidateSeed(
      String candidateId,
      String profileId,
      String syntheticName,
      String email,
      String roleFamily,
      String seniorityBand,
      String locationRegion,
      String status,
      List<String> skills,
      String summary,
      String sourceDocumentRef,
      String metadata) {

    public CandidateSeed {
      candidateId = requireNonBlank(candidateId, "candidateId");
      profileId = requireNonBlank(profileId, "profileId");
      syntheticName = requireNonBlank(syntheticName, "syntheticName");
      email = requireNonBlank(email, "email").toLowerCase();
      roleFamily = requireNonBlank(roleFamily, "roleFamily");
      seniorityBand = requireNonBlank(seniorityBand, "seniorityBand");
      locationRegion = requireNonBlank(locationRegion, "locationRegion");
      status = requireNonBlank(status, "status");
      skills = List.copyOf(Objects.requireNonNull(skills, "skills must not be null"));
      summary = requireNonBlank(summary, "summary");
      sourceDocumentRef = requireNonBlank(sourceDocumentRef, "sourceDocumentRef");
      metadata = requireJsonObject(metadata, "metadata");
    }
  }

  public record SourceDocumentSeed(
      UUID sourceItemId,
      String documentRef,
      String sourceType,
      String title,
      String filename,
      String body,
      String metadata) {

    public SourceDocumentSeed {
      Objects.requireNonNull(sourceItemId, "sourceItemId must not be null");
      documentRef = requireNonBlank(documentRef, "documentRef");
      sourceType = requireNonBlank(sourceType, "sourceType");
      title = requireNonBlank(title, "title");
      filename = requireNonBlank(filename, "filename");
      body = requireNonBlank(body, "body");
      metadata = requireJsonObject(metadata, "metadata");
    }
  }

  static String requireNonBlank(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    String stripped = value.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return stripped;
  }

  static String requireJsonObject(String value, String fieldName) {
    String stripped = requireNonBlank(value, fieldName);
    if (!stripped.startsWith("{") || !stripped.endsWith("}")) {
      throw new IllegalArgumentException(fieldName + " must be a JSON object");
    }
    return stripped;
  }
}
