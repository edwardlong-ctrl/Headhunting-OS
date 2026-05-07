package com.recruitingtransactionos.coreapi.pilotdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PilotDatasetLoader {

  private static final String DEFAULT_RESOURCE = "/pilotdata/semiconductor-pilot-v1.json";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String resourcePath;

  private PilotDatasetLoader(String resourcePath) {
    this.resourcePath = PilotDataset.requireNonBlank(resourcePath, "resourcePath");
  }

  public static PilotDatasetLoader defaultLoader() {
    return new PilotDatasetLoader(DEFAULT_RESOURCE);
  }

  public PilotDataset loadDefault() {
    try (InputStream inputStream = PilotDatasetLoader.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IllegalStateException("Pilot dataset resource is missing: " + resourcePath);
      }
      PilotDatasetSpec spec = OBJECT_MAPPER.readValue(inputStream, PilotDatasetSpec.class);
      return toDataset(spec);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to load pilot dataset resource: " + resourcePath, exception);
    }
  }

  private static PilotDataset toDataset(PilotDatasetSpec spec) {
    UUID organizationId = UUID.fromString(spec.organization().organizationId());
    List<PilotDataset.AccountSeed> accounts = spec.accounts().stream()
        .map(account -> new PilotDataset.AccountSeed(
            UUID.fromString(account.userAccountId()),
            account.email(),
            account.displayName(),
            account.role(),
            account.password()))
        .toList();
    List<PilotDataset.CompanySeed> companies = spec.companies().stream()
        .map(company -> new PilotDataset.CompanySeed(
            UUID.fromString(company.companyId()),
            company.name(),
            company.industry(),
            company.headquartersLocation(),
            company.sizeBand(),
            spec.consultantUserAccountId(),
            metadata(spec.version(), company.name())))
        .toList();
    List<PilotDataset.JobSeed> jobs = buildJobs(spec);
    List<PilotDataset.CandidateSeed> candidates = buildCandidates(spec);
    List<PilotDataset.SourceDocumentSeed> sourceDocuments = buildSourceDocuments(spec, jobs, candidates);
    return new PilotDataset(
        spec.version(),
        new PilotDataset.OrganizationSeed(
            organizationId,
            spec.organization().legalName(),
            spec.organization().displayName(),
            spec.organization().defaultTimezone()),
        accounts,
        companies,
        jobs,
        candidates,
        sourceDocuments);
  }

  private static List<PilotDataset.JobSeed> buildJobs(PilotDatasetSpec spec) {
    List<PilotDataset.JobSeed> jobs = new ArrayList<>();
    for (int index = 1; index <= spec.activeJobCount() + spec.underReviewJobCount(); index++) {
      RoleFamilySeed family = spec.roleFamilies().get((index - 1) % spec.roleFamilies().size());
      CompanySpec company = spec.companies().get((index - 1) % spec.companies().size());
      boolean active = index <= spec.activeJobCount();
      String status = active ? "activated" : "intake_review";
      jobs.add(new PilotDataset.JobSeed(
          uuid("pilot-job-" + index),
          UUID.fromString(company.companyId()),
          "Pilot " + family.label() + " Search " + two(index),
          status,
          family.label(),
          active ? "staff-principal" : "senior-staff",
          "{\"region\":\"" + spec.locations().get((index - 1) % spec.locations().size()) + "\"}",
          "{\"range\":\"" + (active ? "USD 210k-280k" : "USD 190k-250k") + "\"}",
          spec.consultantUserAccountId(),
          "job-intake-" + two(index),
          metadata(spec.version(), "job-" + two(index))));
    }
    return List.copyOf(jobs);
  }

  private static List<PilotDataset.CandidateSeed> buildCandidates(PilotDatasetSpec spec) {
    List<PilotDataset.CandidateSeed> candidates = new ArrayList<>();
    for (int index = 1; index <= spec.candidateCount(); index++) {
      RoleFamilySeed family = spec.roleFamilies().get((index - 1) % spec.roleFamilies().size());
      String seniority = spec.seniorityBands().get((index - 1) % spec.seniorityBands().size());
      String location = spec.locations().get((index - 1) % spec.locations().size());
      List<String> skills = List.of(
          family.skills().get((index - 1) % family.skills().size()),
          family.skills().get(index % family.skills().size()),
          "evidence-led validation");
      candidates.add(new PilotDataset.CandidateSeed(
          uuid("pilot-candidate-" + index).toString(),
          uuid("pilot-profile-" + index).toString(),
          "Pilot Talent " + two(index),
          "talent" + two(index) + "@candidate.example.test",
          family.label(),
          seniority,
          location,
          index % 5 == 0 ? "consultant_review" : "available",
          skills,
          "Synthetic semiconductor " + family.label()
              + " profile with generalized project evidence and no real employer identity.",
          "candidate-resume-" + two(index),
          metadata(spec.version(), "candidate-" + two(index))));
    }
    return List.copyOf(candidates);
  }

  private static List<PilotDataset.SourceDocumentSeed> buildSourceDocuments(
      PilotDatasetSpec spec,
      List<PilotDataset.JobSeed> jobs,
      List<PilotDataset.CandidateSeed> candidates) {
    List<PilotDataset.SourceDocumentSeed> sourceDocuments = new ArrayList<>();
    candidates.forEach(candidate -> sourceDocuments.add(new PilotDataset.SourceDocumentSeed(
        uuid("source-" + candidate.sourceDocumentRef()),
        candidate.sourceDocumentRef(),
        "CV",
        candidate.syntheticName() + " synthetic resume",
        candidate.sourceDocumentRef() + ".txt",
        candidate.syntheticName() + "\n"
            + "Role family: " + candidate.roleFamily() + "\n"
            + "Skills: " + String.join(", ", candidate.skills()) + "\n"
            + candidate.summary() + "\n"
            + "All companies and project names are fictional generalized pilot data.",
        metadata(spec.version(), candidate.sourceDocumentRef()))));
    jobs.forEach(job -> sourceDocuments.add(new PilotDataset.SourceDocumentSeed(
        uuid("source-" + job.sourceDocumentRef()),
        job.sourceDocumentRef(),
        "JD",
        job.title() + " synthetic job intake",
        job.sourceDocumentRef() + ".txt",
        job.title() + "\n"
            + "Role family: " + job.roleFamily() + "\n"
            + "Status: " + job.status() + "\n"
            + "This fictional pilot job is for workflow testing only.",
        metadata(spec.version(), job.sourceDocumentRef()))));
    return List.copyOf(sourceDocuments);
  }

  private static UUID uuid(String seed) {
    return UUID.nameUUIDFromBytes(("rto-task38-" + seed).getBytes(StandardCharsets.UTF_8));
  }

  private static String two(int index) {
    return String.format("%02d", index);
  }

  private static String metadata(String version, String ref) {
    return "{\"synthetic\":true,\"pilotDataset\":\"" + version + "\",\"ref\":\"" + ref + "\"}";
  }

  private record PilotDatasetSpec(
      String version,
      OrganizationSpec organization,
      String consultantUserAccountId,
      int candidateCount,
      int activeJobCount,
      int underReviewJobCount,
      List<AccountSpec> accounts,
      List<CompanySpec> companies,
      List<RoleFamilySeed> roleFamilies,
      List<String> seniorityBands,
      List<String> locations) {
  }

  private record OrganizationSpec(
      String organizationId,
      String legalName,
      String displayName,
      String defaultTimezone) {
  }

  private record AccountSpec(
      String userAccountId,
      String email,
      String displayName,
      String role,
      String password) {
  }

  private record CompanySpec(
      String companyId,
      String name,
      String industry,
      String headquartersLocation,
      String sizeBand) {
  }

  private record RoleFamilySeed(String label, List<String> skills) {
  }
}
