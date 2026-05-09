package com.recruitingtransactionos.coreapi.consultantmatching;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse.DimensionScore;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse.EvidenceCoverageSummary;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantMatchReportResponse.ProvenanceSummaryResponse;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityAwareMatchRequestFactory;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityAwareMatchRequestSeed;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorInput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorOutput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorTaskService;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentStatus;
import com.recruitingtransactionos.coreapi.candidatedocument.service.CandidateDocumentService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.consultantmatching.port.MatchReportPersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceHit;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceRetrievalResult;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPack;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackId;
import com.recruitingtransactionos.coreapi.industrypack.IndustryPackKey;
import com.recruitingtransactionos.coreapi.industrypack.IndustryRoleFamilyTemplate;
import com.recruitingtransactionos.coreapi.industrypack.OntologyVersion;
import com.recruitingtransactionos.coreapi.industrypack.port.IndustryPackReadPort;
import com.recruitingtransactionos.coreapi.industrypack.service.IndustryPackMatchProfile;
import com.recruitingtransactionos.coreapi.industrypack.service.IndustryPackService;
import com.recruitingtransactionos.coreapi.industrypack.service.ResolvedIndustryPack;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.matching.AuthenticityRiskLevel;
import com.recruitingtransactionos.coreapi.matching.EvidenceAssertionStrength;
import com.recruitingtransactionos.coreapi.matching.EvidenceCoverageInput;
import com.recruitingtransactionos.coreapi.matching.IndustryPackMaturity;
import com.recruitingtransactionos.coreapi.matching.MatchDimension;
import com.recruitingtransactionos.coreapi.matching.MatchEvidenceSignal;
import com.recruitingtransactionos.coreapi.matching.MatchJobRef;
import com.recruitingtransactionos.coreapi.matching.MatchReport;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationRequest;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationResult;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationService;
import com.recruitingtransactionos.coreapi.matching.MatchReportId;
import com.recruitingtransactionos.coreapi.matching.MatchScore;
import com.recruitingtransactionos.coreapi.matching.MatchSubjectRef;
import com.recruitingtransactionos.coreapi.matching.ProvenanceCategory;
import com.recruitingtransactionos.coreapi.matching.ReidentificationRiskSignal;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class ConsultantMatchingSurfaceService {

  private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final ActorRef CONSULTANT_MATCHING_ACTOR =
      new ActorRef(UUID.fromString("00000000-0000-0000-0000-000000000026"), ActorRole.CONSULTANT);

  private final JobService jobService;
  private final CandidateService candidateService;
  private final CandidateProfileService candidateProfileService;
  private final ShortlistService shortlistService;
  private final MatchReportGenerationService generationService;
  private final MatchReportPersistencePort matchReportPersistencePort;
  private final AuthenticityAwareMatchRequestFactory authenticityAwareMatchRequestFactory;
  private final CandidateDocumentService candidateDocumentService;
  private final DocumentParsingService documentParsingService;
  private final AuthenticityRiskAssessorTaskService authenticityRiskAssessorTaskService;
  private final IndustryPackService industryPackService;
  private final PermissionEnforcer permissionEnforcer;

  @Autowired
  public ConsultantMatchingSurfaceService(
      JobService jobService,
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      ShortlistService shortlistService,
      MatchReportGenerationService generationService,
      MatchReportPersistencePort matchReportPersistencePort,
      AuthenticityAwareMatchRequestFactory authenticityAwareMatchRequestFactory,
      CandidateDocumentService candidateDocumentService,
      DocumentParsingService documentParsingService,
      AuthenticityRiskAssessorTaskService authenticityRiskAssessorTaskService,
      IndustryPackService industryPackService) {
    this(
        jobService,
        candidateService,
        candidateProfileService,
        shortlistService,
        generationService,
        matchReportPersistencePort,
        authenticityAwareMatchRequestFactory,
        candidateDocumentService,
        documentParsingService,
        authenticityRiskAssessorTaskService,
        industryPackService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  ConsultantMatchingSurfaceService(
      JobService jobService,
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      ShortlistService shortlistService,
      MatchReportGenerationService generationService,
      MatchReportPersistencePort matchReportPersistencePort,
      AuthenticityAwareMatchRequestFactory authenticityAwareMatchRequestFactory,
      CandidateDocumentService candidateDocumentService,
      DocumentParsingService documentParsingService,
      AuthenticityRiskAssessorTaskService authenticityRiskAssessorTaskService,
      PermissionEnforcer permissionEnforcer) {
    this(
        jobService,
        candidateService,
        candidateProfileService,
        shortlistService,
        generationService,
        matchReportPersistencePort,
        authenticityAwareMatchRequestFactory,
        candidateDocumentService,
        documentParsingService,
        authenticityRiskAssessorTaskService,
        defaultIndustryPackService(),
        permissionEnforcer);
  }

  ConsultantMatchingSurfaceService(
      JobService jobService,
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      ShortlistService shortlistService,
      MatchReportGenerationService generationService,
      MatchReportPersistencePort matchReportPersistencePort,
      AuthenticityAwareMatchRequestFactory authenticityAwareMatchRequestFactory,
      CandidateDocumentService candidateDocumentService,
      DocumentParsingService documentParsingService,
      AuthenticityRiskAssessorTaskService authenticityRiskAssessorTaskService,
      IndustryPackService industryPackService,
      PermissionEnforcer permissionEnforcer) {
    this.jobService = Objects.requireNonNull(jobService, "jobService must not be null");
    this.candidateService = Objects.requireNonNull(candidateService, "candidateService must not be null");
    this.candidateProfileService =
        Objects.requireNonNull(candidateProfileService, "candidateProfileService must not be null");
    this.shortlistService = Objects.requireNonNull(shortlistService, "shortlistService must not be null");
    this.generationService = Objects.requireNonNull(generationService, "generationService must not be null");
    this.matchReportPersistencePort =
        Objects.requireNonNull(matchReportPersistencePort, "matchReportPersistencePort must not be null");
    this.authenticityAwareMatchRequestFactory = Objects.requireNonNull(
        authenticityAwareMatchRequestFactory,
        "authenticityAwareMatchRequestFactory must not be null");
    this.candidateDocumentService =
        Objects.requireNonNull(candidateDocumentService, "candidateDocumentService must not be null");
    this.documentParsingService =
        Objects.requireNonNull(documentParsingService, "documentParsingService must not be null");
    this.authenticityRiskAssessorTaskService = Objects.requireNonNull(
        authenticityRiskAssessorTaskService,
        "authenticityRiskAssessorTaskService must not be null");
    this.industryPackService = Objects.requireNonNull(industryPackService, "industryPackService must not be null");
    this.permissionEnforcer = Objects.requireNonNull(permissionEnforcer, "permissionEnforcer must not be null");
  }

  public ConsultantMatchReportResponse generateMatchReport(
      AccessRequest accessRequest,
      UUID organizationId,
      JobId jobId,
      ConsultantMatchSelection selection) {
    Objects.requireNonNull(accessRequest, "accessRequest must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    Objects.requireNonNull(selection, "selection must not be null");
    requireMatchReportAccess(accessRequest, AccessAction.CREATE);

    Job job = jobService.findJobByIdAndOrganizationId(organizationId, jobId)
        .orElseThrow(() -> new IllegalArgumentException("jobId not found"));
    List<JobRequirement> requirements = jobService.findRequirementsByJobIdAndOrganizationId(organizationId, jobId);
    Optional<JobScorecard> scorecard =
        jobService.findActiveScorecardByJobIdAndOrganizationId(organizationId, jobId);
    SubjectContext subjectContext = resolveSubjectContext(organizationId, jobId, selection);
    AssemblyContext assembly =
        assemble(organizationId, job, requirements, scorecard.orElse(null), subjectContext);

    MatchReportGenerationResult generated = generationService.generate(assembly.request());
    List<String> explanations =
        buildExplanations(job, requirements, subjectContext, assembly, generated.matchReport());
    List<String> interviewQuestions =
        buildInterviewQuestions(requirements, subjectContext, assembly, generated.matchReport());
    StoredMatchReport stored = matchReportPersistencePort.create(
        new StoredMatchReport(
            organizationId,
            generated.matchReport(),
            subjectContext.subjectType(),
            subjectContext.candidateId().value(),
            subjectContext.shortlistCandidateCardId(),
            assembly.reidentificationRiskSignal(),
            assembly.resolvedIndustryPack().industryPack().packKey().value(),
            assembly.resolvedIndustryPack().industryPack().maturity(),
            assembly.resolvedIndustryPack().ontologyStale(),
            assembly.resolvedIndustryPack().selectionReason(),
            assembly.matchProfile().antiPatternWarnings(),
            explanations,
            interviewQuestions));
    return toResponse(stored);
  }

  public List<ConsultantMatchReportResponse> listMatchReports(
      AccessRequest accessRequest,
      UUID organizationId,
      JobId jobId) {
    Objects.requireNonNull(accessRequest, "accessRequest must not be null");
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    requireMatchReportAccess(accessRequest, AccessAction.READ);
    return matchReportPersistencePort.findByJobIdAndOrganizationId(organizationId, jobId).stream()
        .sorted(Comparator.comparing((StoredMatchReport report) -> report.matchReport().generatedAt()).reversed())
        .map(this::toResponse)
        .toList();
  }

  private void requireMatchReportAccess(AccessRequest accessRequest, AccessAction action) {
    permissionEnforcer.requireAllowed(new AccessRequest(
        accessRequest.actorRole(),
        ResourceType.MATCH_REPORT,
        action,
        FieldClassification.CONSULTANT_PRIVATE,
        accessRequest.relationshipScopes(),
        accessRequest.identityDisclosureRequested()));
  }

  private SubjectContext resolveSubjectContext(
      UUID organizationId,
      JobId jobId,
      ConsultantMatchSelection selection) {
    if (selection.candidateId() != null) {
      CandidateId candidateId = new CandidateId(UUID.fromString(selection.candidateId()));
      Candidate candidate = candidateService.findCandidateByIdAndOrganizationId(organizationId, candidateId)
          .orElseThrow(() -> new IllegalArgumentException("candidateId not found"));
      CandidateProfile profile = candidateProfileService
          .findCandidateProfileByCandidateIdAndOrganizationId(organizationId, candidateId)
          .orElseThrow(() -> new IllegalArgumentException("candidate profile not found"));
      return new SubjectContext("candidate", candidate, profile, null);
    }
    if (selection.shortlistCandidateCardId() != null) {
      UUID shortlistCardId = UUID.fromString(selection.shortlistCandidateCardId());
      for (Shortlist shortlist : shortlistService.findShortlistsByJobIdAndOrganizationId(organizationId, jobId)) {
        Optional<ShortlistCandidateCard> card = shortlistService
            .findCardsByShortlistIdAndOrganizationId(organizationId, shortlist.shortlistId()).stream()
            .filter(candidateCard -> candidateCard.shortlistCandidateCardId().value().equals(shortlistCardId))
            .findFirst();
        if (card.isPresent()) {
          Candidate candidate = candidateService
              .findCandidateByIdAndOrganizationId(organizationId, card.get().candidateId())
              .orElseThrow(() -> new IllegalArgumentException("shortlist candidate not found"));
          CandidateProfile profile = candidateProfileService
              .findCandidateProfileByIdAndOrganizationId(
                  organizationId,
                  new CandidateProfileId(card.get().candidateProfileId()))
              .orElseThrow(() -> new IllegalArgumentException("shortlist candidate profile not found"));
          return new SubjectContext("shortlist_card", candidate, profile, shortlistCardId);
        }
      }
      throw new IllegalArgumentException("shortlistCandidateCardId not found for job");
    }
    throw new IllegalArgumentException("candidate selection is required");
  }

  private AssemblyContext assemble(
      UUID organizationId,
      Job job,
      List<JobRequirement> requirements,
      JobScorecard scorecard,
      SubjectContext subjectContext) {
    List<CandidateProfileField> fields = subjectContext.candidateProfile().fields();
    ResolvedIndustryPack resolvedIndustryPack =
        industryPackService.resolveForJobAndCandidate(job, subjectContext.candidate(), Instant.now());
    Set<MatchDimension> requiredDimensions =
        determineRequiredDimensions(requirements, scorecard, resolvedIndustryPack);
    DocumentEvidenceSummary documentEvidence =
        collectDocumentEvidence(
            organizationId,
            subjectContext.candidateId(),
            job,
            requirements,
            scorecard,
            requiredDimensions,
            resolvedIndustryPack);
    Map<MatchDimension, List<MatchEvidenceSignal>> signalsByDimension = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      signalsByDimension.put(dimension, new ArrayList<>());
    }

    boolean projectEvidencePresent = documentEvidence.projectEvidencePresent();
    boolean skillsPresent = documentEvidence.skillsPresent();
    boolean hasIntentField = documentEvidence.intentSignalPresent();
    boolean highConfidenceIntent = documentEvidence.strongIntentSignalPresent();
    int conflictCount = 0;
    int staleCount = 0;
    int aiDerivedCount = 0;
    int trustedCount = 0;

    for (Map.Entry<MatchDimension, List<MatchEvidenceSignal>> entry : documentEvidence.signalsByDimension().entrySet()) {
      signalsByDimension.get(entry.getKey()).addAll(entry.getValue());
    }
    for (CandidateProfileField field : fields) {
      String fieldPath = field.fieldPath().value().toLowerCase(Locale.ROOT);
      CandidateProfileFieldStatus status = field.fieldStatus();
      boolean intentField = fieldPath.contains("intent") || fieldPath.contains("motivation");
      projectEvidencePresent |= fieldPath.contains("experience") || fieldPath.contains("project");
      skillsPresent |= fieldPath.contains("skill");
      hasIntentField |= intentField;
      highConfidenceIntent |= intentField && isTrusted(status);
      if (status == CandidateProfileFieldStatus.CONFLICTING) {
        conflictCount++;
      }
      if (status == CandidateProfileFieldStatus.STALE || status == CandidateProfileFieldStatus.NEEDS_CONFIRMATION) {
        staleCount++;
      }
      if (status == CandidateProfileFieldStatus.AI_EXTRACTED || status == CandidateProfileFieldStatus.SYSTEM_INFERENCE) {
        aiDerivedCount++;
      }
      if (isTrusted(status)) {
        trustedCount++;
      }
      for (MatchDimension dimension : inferDimensions(fieldPath)) {
        signalsByDimension.get(dimension).add(new MatchEvidenceSignal(
            dimension,
            toProvenance(status),
            assertionStrengthForStatus(status),
            isIndependent(status)));
      }
      signalsByDimension.get(MatchDimension.EVIDENCE_STRENGTH).add(new MatchEvidenceSignal(
          MatchDimension.EVIDENCE_STRENGTH,
          toProvenance(status),
          assertionStrengthForStatus(status),
          isIndependent(status)));
    }

    EvidenceCoverageInput coverageInput = new EvidenceCoverageInput(
        requiredDimensions,
        requiredDimensions.stream()
            .flatMap(dimension -> signalsByDimension.get(dimension).stream())
            .toList());
    Map<MatchDimension, MatchScore> requestedScores = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      requestedScores.put(dimension, scoreDimension(signalsByDimension.get(dimension)));
    }
    IndustryPackMatchProfile matchProfile = industryPackService.evaluateMatchProfile(
        resolvedIndustryPack,
        job.roleFamily(),
        List.of(
            documentEvidence.resumeText(),
            documentEvidence.linkedInText(),
            documentEvidence.portfolioText(),
            documentEvidence.interviewNotesText()),
        fields.stream().map(field -> field.value().jsonValue()).toList());
    if (matchProfile.antiPatternTriggered()) {
      requestedScores.computeIfPresent(
          MatchDimension.TECHNICAL_FIT,
          (dimension, score) -> MatchScore.of(Math.min(score.value(), 2)));
      requestedScores.computeIfPresent(
          MatchDimension.INDUSTRY_FIT,
          (dimension, score) -> MatchScore.of(Math.min(score.value(), 2)));
    }
    MatchScore overallScore = MatchScore.of(Math.max(1, Math.min(5, Math.round((float) requestedScores.values().stream()
        .mapToInt(MatchScore::value)
        .average()
        .orElse(2.0d)))));

    AuthenticityRiskAssessorOutput authenticityAssessment = assessAuthenticity(
        organizationId,
        subjectContext,
        documentEvidence,
        conflictCount,
        staleCount,
        aiDerivedCount,
        trustedCount,
        projectEvidencePresent);
    ReidentificationRiskSignal reidentificationRiskSignal =
        inferReidentificationRiskSignal(subjectContext, documentEvidence.documents());
    MatchSubjectRef subjectRef = subjectContext.shortlistCandidateCardId() == null
        ? MatchSubjectRef.of("match_subject_candidate_" + subjectContext.candidateId().value().toString().replace("-", ""))
        : MatchSubjectRef.of("match_subject_card_" + subjectContext.shortlistCandidateCardId().toString().replace("-", ""));
    MatchReportGenerationRequest request = authenticityAwareMatchRequestFactory.create(
        new AuthenticityAwareMatchRequestSeed(
            new MatchReportId("match_report_" + UUID.randomUUID().toString().replace("-", "")),
            MatchJobRef.of("job_ref_" + job.jobId().value().toString().replace("-", "")),
            subjectRef,
            overallScore,
            requestedScores,
            coverageInput,
            resolvedIndustryPack.industryPack().maturity(),
            skillsPresent && !projectEvidencePresent,
            projectEvidencePresent,
            highConfidenceIntent ? EvidenceAssertionStrength.EXPLICIT : hasIntentField
                ? EvidenceAssertionStrength.IMPLIED
                : EvidenceAssertionStrength.WEAK_SIGNAL,
            resolvedIndustryPack.ontologyStale(),
            resolvedIndustryPack.ontologyStale(),
            reidentificationRiskSignal,
            resolvedIndustryPack.ontologyVersion().versionKey(),
            resolvedIndustryPack.industryPack().packKey().value() + ":" + resolvedIndustryPack.ontologyVersion().versionKey(),
            Instant.now()),
        authenticityAssessment);
    return new AssemblyContext(
        request,
        reidentificationRiskSignal,
        documentEvidence,
        authenticityAssessment,
        resolvedIndustryPack,
        matchProfile);
  }

  private static Set<MatchDimension> determineRequiredDimensions(
      List<JobRequirement> requirements,
      JobScorecard scorecard,
      ResolvedIndustryPack resolvedIndustryPack) {
    Set<MatchDimension> required = EnumSet.noneOf(MatchDimension.class);
    if (requirements != null) {
      for (JobRequirement requirement : requirements) {
        String text = (requirement.requirementType() + " " + requirement.label() + " " + requirement.detail()).toLowerCase();
        required.addAll(inferDimensions(text));
      }
    }
    if (scorecard != null) {
      required.addAll(inferDimensions(scorecard.dimensions().toLowerCase()));
      required.addAll(inferDimensions(scorecard.scoringGuidance() == null ? "" : scorecard.scoringGuidance().toLowerCase()));
    }
    if (resolvedIndustryPack != null && resolvedIndustryPack.roleFamilyTemplate() != null) {
      required.addAll(inferDimensions(resolvedIndustryPack.roleFamilyTemplate().scorecardDimensions().toLowerCase(Locale.ROOT)));
      required.addAll(inferDimensions(resolvedIndustryPack.roleFamilyTemplate().scoringGuidance().toLowerCase(Locale.ROOT)));
    }
    if (required.isEmpty()) {
      required.addAll(EnumSet.of(
          MatchDimension.TECHNICAL_FIT,
          MatchDimension.INDUSTRY_FIT,
          MatchDimension.SENIORITY_FIT,
          MatchDimension.EVIDENCE_STRENGTH));
    }
    return EnumSet.copyOf(required);
  }

  private static Set<MatchDimension> inferDimensions(String text) {
    Set<MatchDimension> dimensions = EnumSet.noneOf(MatchDimension.class);
    if (text.contains("skill") || text.contains("stack") || text.contains("tech")) {
      dimensions.add(MatchDimension.TECHNICAL_FIT);
    }
    if (text.contains("industry") || text.contains("domain") || text.contains("market")) {
      dimensions.add(MatchDimension.INDUSTRY_FIT);
    }
    if (text.contains("senior") || text.contains("lead") || text.contains("experience") || text.contains("level")) {
      dimensions.add(MatchDimension.SENIORITY_FIT);
    }
    if (text.contains("salary") || text.contains("compensation") || text.contains("rate")) {
      dimensions.add(MatchDimension.SALARY_FIT);
    }
    if (text.contains("location") || text.contains("onsite") || text.contains("remote")) {
      dimensions.add(MatchDimension.LOCATION_FIT);
    }
    if (text.contains("motivation") || text.contains("intent") || text.contains("interest")) {
      dimensions.add(MatchDimension.MOTIVATION_FIT);
    }
    if (text.contains("available") || text.contains("notice") || text.contains("start")) {
      dimensions.add(MatchDimension.AVAILABILITY_FIT);
    }
    if (text.contains("manager") || text.contains("culture") || text.contains("team")) {
      dimensions.add(MatchDimension.CULTURE_OR_MANAGER_FIT);
    }
    if (dimensions.isEmpty()) {
      dimensions.add(MatchDimension.EVIDENCE_STRENGTH);
    }
    return dimensions;
  }

  private static MatchScore scoreDimension(List<MatchEvidenceSignal> signals) {
    if (signals == null || signals.isEmpty()) {
      return MatchScore.of(2);
    }
    long trustedSignals = signals.stream()
        .filter(signal -> signal.assertionStrength() == EvidenceAssertionStrength.EXPLICIT)
        .count();
    long mediumSignals = signals.stream()
        .filter(signal -> signal.assertionStrength() == EvidenceAssertionStrength.IMPLIED)
        .count();
    boolean weakOrRisky = signals.stream().anyMatch(signal ->
        signal.provenanceCategory() == ProvenanceCategory.SYSTEM_INFERENCE
            || signal.provenanceCategory() == ProvenanceCategory.AI_EXTRACTED
            || signal.provenanceCategory() == ProvenanceCategory.WEAK_SIGNAL);
    if (trustedSignals >= 2) {
      return MatchScore.of(5);
    }
    if (trustedSignals >= 1 || mediumSignals >= 2) {
      return MatchScore.of(4);
    }
    if (weakOrRisky) {
      return MatchScore.of(3);
    }
    return MatchScore.of(3);
  }

  private static boolean isTrusted(CandidateProfileFieldStatus status) {
    return status == CandidateProfileFieldStatus.EXTERNAL_VERIFIED
        || status == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED
        || status == CandidateProfileFieldStatus.CONSULTANT_ATTESTED
        || status == CandidateProfileFieldStatus.HUMAN_ACKNOWLEDGED
        || status == CandidateProfileFieldStatus.LIKELY_CURRENT;
  }

  private static ProvenanceCategory toProvenance(CandidateProfileFieldStatus status) {
    return switch (status) {
      case EXTERNAL_VERIFIED -> ProvenanceCategory.EXTERNAL_VERIFIED;
      case CANDIDATE_CONFIRMED -> ProvenanceCategory.CANDIDATE_CONFIRMED;
      case CONSULTANT_ATTESTED -> ProvenanceCategory.CONSULTANT_ATTESTED;
      case HUMAN_ACKNOWLEDGED -> ProvenanceCategory.HUMAN_ACKNOWLEDGED;
      case AI_EXTRACTED -> ProvenanceCategory.AI_EXTRACTED;
      case SYSTEM_INFERENCE -> ProvenanceCategory.SYSTEM_INFERENCE;
      default -> ProvenanceCategory.WEAK_SIGNAL;
    };
  }

  private static EvidenceAssertionStrength assertionStrengthForStatus(
      CandidateProfileFieldStatus status) {
    return switch (status) {
      case EXTERNAL_VERIFIED, CANDIDATE_CONFIRMED, CONSULTANT_ATTESTED -> EvidenceAssertionStrength.EXPLICIT;
      case HUMAN_ACKNOWLEDGED, LIKELY_CURRENT, AI_EXTRACTED -> EvidenceAssertionStrength.IMPLIED;
      default -> EvidenceAssertionStrength.WEAK_SIGNAL;
    };
  }

  private static boolean isIndependent(CandidateProfileFieldStatus status) {
    return isTrusted(status);
  }

  private AuthenticityRiskAssessorOutput assessAuthenticity(
      UUID organizationId,
      SubjectContext subjectContext,
      DocumentEvidenceSummary documentEvidence,
      int conflictCount,
      int staleCount,
      int aiDerivedCount,
      int trustedCount,
      boolean projectEvidencePresent) {
    if (!documentEvidence.authenticityInputAvailable()) {
      return fallbackAuthenticityAssessment(
          conflictCount,
          staleCount,
          aiDerivedCount,
          trustedCount,
          projectEvidencePresent);
    }
    AuthenticityRiskAssessorResult result = authenticityRiskAssessorTaskService.execute(
        organizationId,
        CONSULTANT_MATCHING_ACTOR,
        new EntityRef("candidate", subjectContext.candidateId().value()),
        documentEvidence.sourceReferenceIds(),
        new AuthenticityRiskAssessorInput(
            documentEvidence.resumeText(),
            documentEvidence.linkedInText(),
            documentEvidence.portfolioText(),
            documentEvidence.interviewNotesText()),
        new WorkflowCorrelationId(UUID.randomUUID()),
        new WorkflowCausationId(UUID.randomUUID()));
    return result.output();
  }

  private static AuthenticityRiskAssessorOutput fallbackAuthenticityAssessment(
      int conflictCount,
      int staleCount,
      int aiDerivedCount,
      int trustedCount,
      boolean projectEvidencePresent) {
    AuthenticityRiskLevel authenticityRiskLevel;
    if (conflictCount >= 2 || staleCount >= 3) {
      authenticityRiskLevel = AuthenticityRiskLevel.HIGH;
    } else if (conflictCount >= 1 || staleCount >= 1 || aiDerivedCount > trustedCount) {
      authenticityRiskLevel = AuthenticityRiskLevel.MEDIUM;
    } else {
      authenticityRiskLevel = AuthenticityRiskLevel.LOW;
    }
    return new AuthenticityRiskAssessorOutput(
        authenticityRiskLevel.name().toLowerCase(Locale.ROOT),
        Math.max(0, Math.min(100, trustedCount * 10)),
        !projectEvidencePresent,
        authenticityRiskLevel == AuthenticityRiskLevel.HIGH
            ? List.of("conflicting_evidence", "stale_evidence")
            : authenticityRiskLevel == AuthenticityRiskLevel.MEDIUM
                ? List.of("mixed_confidence")
                : List.of("trusted_profile_evidence"));
  }

  private DocumentEvidenceSummary collectDocumentEvidence(
      UUID organizationId,
      CandidateId candidateId,
      Job job,
      List<JobRequirement> requirements,
      JobScorecard scorecard,
      Set<MatchDimension> requiredDimensions,
      ResolvedIndustryPack resolvedIndustryPack) {
    EnumMap<MatchDimension, List<MatchEvidenceSignal>> signalsByDimension = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      signalsByDimension.put(dimension, new ArrayList<>());
    }
    List<CandidateDocument> documents = candidateDocumentService
        .findDocumentsByCandidateIdAndOrganizationId(organizationId, candidateId)
        .stream()
        .filter(document -> document.status() == CandidateDocumentStatus.ACTIVE)
        .toList();
    List<String> evidenceQueries =
        buildEvidenceQueries(job, requirements, scorecard, requiredDimensions, resolvedIndustryPack);
    List<String> documentTitles = new ArrayList<>();
    List<UUID> sourceReferenceIds = new ArrayList<>();
    List<String> resumeTexts = new ArrayList<>();
    List<String> linkedInTexts = new ArrayList<>();
    List<String> portfolioTexts = new ArrayList<>();
    List<String> interviewNotesTexts = new ArrayList<>();
    int evidenceHitCount = 0;
    boolean projectEvidencePresent = false;
    boolean skillsPresent = false;
    boolean intentSignalPresent = false;
    boolean strongIntentSignalPresent = false;

    for (CandidateDocument document : documents) {
      if (document.sourceItemId() == null) {
        continue;
      }
      Optional<ParsedDocument> parsedDocument =
          documentParsingService.findLatestParsedDocument(organizationId, new SourceItemId(document.sourceItemId()));
      if (parsedDocument.isEmpty()
          || parsedDocument.orElseThrow().processingStatus() != DocumentProcessingStatus.SUCCEEDED) {
        continue;
      }
      documentTitles.add(document.title() == null || document.title().isBlank()
          ? document.documentType()
          : document.title().strip());
      sourceReferenceIds.add(document.sourceItemId());
      List<DocumentEvidenceHit> hits = retrieveEvidenceForQueries(
          organizationId,
          new SourceItemId(document.sourceItemId()),
          evidenceQueries);
      if (hits.isEmpty()) {
        continue;
      }
      evidenceHitCount += hits.size();
      for (DocumentEvidenceHit hit : hits) {
        String excerpt = hit.excerpt().toLowerCase(Locale.ROOT);
        if (excerpt.contains("experience") || excerpt.contains("project")) {
          projectEvidencePresent = true;
        }
        if (excerpt.contains("skill") || excerpt.contains("java") || excerpt.contains("python")) {
          skillsPresent = true;
        }
        if (excerpt.contains("motivation") || excerpt.contains("interest") || excerpt.contains("intent")) {
          intentSignalPresent = true;
          strongIntentSignalPresent |= excerpt.contains("motivated") || excerpt.contains("interested");
        }
        Set<MatchDimension> matchedDimensions = EnumSet.copyOf(inferDimensions(excerpt));
        matchedDimensions.add(MatchDimension.EVIDENCE_STRENGTH);
        for (MatchDimension dimension : matchedDimensions) {
          signalsByDimension.get(dimension).add(new MatchEvidenceSignal(
              dimension,
              ProvenanceCategory.AI_EXTRACTED,
              EvidenceAssertionStrength.EXPLICIT,
              false));
        }
      }
      String text = joinEvidenceHits(hits);
      String normalizedDocumentType = normalizeDocumentHint(document.documentType(), document.title());
      if (normalizedDocumentType.contains("linkedin")) {
        linkedInTexts.add(text);
      } else if (normalizedDocumentType.contains("portfolio")) {
        portfolioTexts.add(text);
      } else if (normalizedDocumentType.contains("interview")) {
        interviewNotesTexts.add(text);
      } else {
        resumeTexts.add(text);
      }
    }
    boolean requiredDimensionEvidencePresent = requiredDimensions.stream()
        .anyMatch(dimension -> !signalsByDimension.get(dimension).isEmpty());
    return new DocumentEvidenceSummary(
        documents,
        documentTitles,
        Map.copyOf(signalsByDimension),
        List.copyOf(sourceReferenceIds),
        joinTexts(resumeTexts),
        joinTexts(linkedInTexts),
        joinTexts(portfolioTexts),
        joinTexts(interviewNotesTexts),
        evidenceHitCount,
        requiredDimensionEvidencePresent,
        projectEvidencePresent,
        skillsPresent,
        intentSignalPresent,
        strongIntentSignalPresent);
  }

  private List<DocumentEvidenceHit> retrieveEvidenceForQueries(
      UUID organizationId,
      SourceItemId sourceItemId,
      List<String> evidenceQueries) {
    LinkedHashMap<UUID, DocumentEvidenceHit> uniqueHits = new LinkedHashMap<>();
    for (String query : evidenceQueries) {
      if (query == null || query.isBlank()) {
        continue;
      }
      DocumentEvidenceRetrievalResult result =
          documentParsingService.retrieveEvidence(organizationId, sourceItemId, query, 3);
      for (DocumentEvidenceHit hit : result.hits()) {
        uniqueHits.merge(
            hit.parsedDocumentChunkId(),
            hit,
            (left, right) -> left.score() >= right.score() ? left : right);
      }
    }
    return uniqueHits.values().stream()
        .sorted(Comparator.comparing(DocumentEvidenceHit::score).reversed()
            .thenComparing(DocumentEvidenceHit::chunkIndex))
        .limit(5)
        .toList();
  }

  private static List<String> buildEvidenceQueries(
      Job job,
      List<JobRequirement> requirements,
      JobScorecard scorecard,
      Set<MatchDimension> requiredDimensions,
      ResolvedIndustryPack resolvedIndustryPack) {
    LinkedHashSet<String> queries = new LinkedHashSet<>();
    addQueryParts(queries, job.title());
    addQueryParts(queries, job.roleFamily());
    addQueryParts(queries, job.description());
    if (requirements != null) {
      for (JobRequirement requirement : requirements) {
        addQueryParts(queries, requirement.label());
        addQueryParts(queries, requirement.requirementType());
        addQueryParts(queries, requirement.detail());
      }
    }
    if (scorecard != null) {
      addQueryParts(queries, scorecard.dimensions());
      addQueryParts(queries, scorecard.scoringGuidance());
    }
    if (resolvedIndustryPack != null && resolvedIndustryPack.roleFamilyTemplate() != null) {
      addQueryParts(queries, resolvedIndustryPack.roleFamilyTemplate().displayName());
      addQueryParts(queries, resolvedIndustryPack.roleFamilyTemplate().scorecardDimensions());
      addQueryParts(queries, resolvedIndustryPack.roleFamilyTemplate().scoringGuidance());
      resolvedIndustryPack.roleFamilyTemplate().requiredSkillKeys().forEach(skill -> addQueryParts(queries, skill.replace('_', ' ')));
      resolvedIndustryPack.roleFamilyTemplate().evidenceExamples().forEach(example -> addQueryParts(queries, example));
    }
    for (MatchDimension dimension : requiredDimensions) {
      addQueryParts(queries, dimension.name().replace("_", " "));
    }
    return queries.stream()
        .filter(query -> query.length() >= 3)
        .limit(12)
        .toList();
  }

  private static void addQueryParts(Collection<String> queries, String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    String normalized = text.strip().replaceAll("\\s+", " ");
    queries.add(normalized);
    for (String part : normalized.split("[,;/\\n]+")) {
      String candidate = part.strip();
      if (candidate.length() >= 3 && candidate.length() <= 80) {
        queries.add(candidate);
      }
    }
  }

  private static String joinEvidenceHits(List<DocumentEvidenceHit> hits) {
    return hits.stream()
        .map(DocumentEvidenceHit::excerpt)
        .filter(Objects::nonNull)
        .map(String::strip)
        .filter(value -> !value.isBlank())
        .collect(java.util.stream.Collectors.joining("\n\n"));
  }

  private static String joinTexts(List<String> texts) {
    return texts.stream()
        .filter(Objects::nonNull)
        .map(String::strip)
        .filter(value -> !value.isBlank())
        .collect(java.util.stream.Collectors.joining("\n\n"));
  }

  private static String normalizeDocumentHint(String documentType, String title) {
    return ((documentType == null ? "" : documentType) + " " + (title == null ? "" : title))
        .toLowerCase(Locale.ROOT);
  }

  private static ReidentificationRiskSignal inferReidentificationRiskSignal(
      SubjectContext subjectContext,
      List<CandidateDocument> documents) {
    boolean hasIdentityDocument = documents.stream()
        .map(document -> normalizeDocumentHint(document.documentType(), document.title()))
        .anyMatch(text ->
            text.contains("passport")
                || text.contains("identity")
                || text.contains("driver")
                || text.contains("license"));
    if (hasIdentityDocument) {
      return ReidentificationRiskSignal.HIGH;
    }
    if (subjectContext.shortlistCandidateCardId() == null && !documents.isEmpty()) {
      return ReidentificationRiskSignal.MEDIUM;
    }
    return ReidentificationRiskSignal.LOW;
  }

  private static List<String> buildExplanations(
      Job job,
      List<JobRequirement> requirements,
      SubjectContext subjectContext,
      AssemblyContext assembly,
      MatchReport report) {
    List<String> explanations = new ArrayList<>();
    explanations.add("Match score is generated from backend-owned job requirements, scorecard signals, and candidate profile evidence.");
    if (assembly.documentEvidence().requiredDimensionEvidencePresent()) {
      explanations.add("Parsed candidate documents contributed "
          + assembly.documentEvidence().evidenceHitCount()
          + " document evidence hits across the required dimensions.");
    } else if (!assembly.documentEvidence().documents().isEmpty()) {
      explanations.add("Candidate documents are present, but parsed document evidence did not strongly map to the required match dimensions.");
    }
    explanations.add("Evidence coverage is " + report.evidenceCoverage().coverageLevel().name()
        + " with " + report.evidenceCoverage().independentHighTrustEvidenceCount()
        + " high-trust signals across required dimensions.");
    explanations.add("Strongest provenance is " + report.provenanceSummary().strongestProvenanceCategory().name()
        + " and confidence is " + report.scoreConfidence().name() + ".");
    explanations.add("Authenticity risk is based on "
        + (assembly.documentEvidence().authenticityInputAvailable()
            ? "parsed candidate document content reviewed by the authenticity task."
            : "profile-derived fallback heuristics because no parsed document text was available."));
    if (!assembly.authenticityAssessment().flags().isEmpty()) {
      explanations.add("Authenticity flags observed: " + String.join(", ", assembly.authenticityAssessment().flags()) + ".");
    }
    explanations.add("Industry pack '" + assembly.resolvedIndustryPack().industryPack().displayName()
        + "' (" + assembly.resolvedIndustryPack().industryPack().packKey().value()
        + ") used ontology version " + assembly.resolvedIndustryPack().ontologyVersion().versionKey()
        + " via " + assembly.resolvedIndustryPack().selectionReason() + ".");
    if (assembly.resolvedIndustryPack().roleFamilyTemplate() != null) {
      explanations.add("Role-family guidance applied: "
          + assembly.resolvedIndustryPack().roleFamilyTemplate().displayName()
          + " with " + assembly.resolvedIndustryPack().roleFamilyTemplate().requiredSkillKeys().size()
          + " required skill concepts.");
    }
    if (!assembly.matchProfile().antiPatternWarnings().isEmpty()) {
      explanations.addAll(assembly.matchProfile().antiPatternWarnings());
    }
    if (report.scoreCapDecision().capApplied()) {
      explanations.add("Score cap applied because " + report.scoreCapDecision().safeExplanation() + ".");
    }
    if (!requirements.isEmpty()) {
      explanations.add("Top job requirement considered: " + requirements.get(0).label() + " for job " + job.title() + ".");
    }
    if (subjectContext.shortlistCandidateCardId() != null) {
      explanations.add("This report is linked to shortlist card " + subjectContext.shortlistCandidateCardId() + " for consultant-only follow-up.");
    }
    return List.copyOf(explanations);
  }

  private List<String> buildInterviewQuestions(
      List<JobRequirement> requirements,
      SubjectContext subjectContext,
      AssemblyContext assembly,
      MatchReport report) {
    List<String> questions = new ArrayList<>();
    if (report.evidenceCoverage().coverageLevel().name().equals("LOW")
        || report.evidenceCoverage().coverageLevel().name().equals("NONE")) {
      questions.add("Which recent projects best demonstrate fit against the highest-priority requirements?");
    }
    if (report.provenanceSummary().authenticityRisk() == AuthenticityRiskLevel.HIGH) {
      questions.add("Which claims need direct confirmation or external verification before shortlist progression?");
    }
    if (requirements.stream().anyMatch(requirement -> requirement.requirementType().toLowerCase().contains("salary"))) {
      questions.add("What compensation range and notice period would make this role actionable?");
    }
    if (assembly.authenticityAssessment().independentEvidenceGap()) {
      questions.add("Which claims can be backed by independent evidence beyond self-authored candidate documents?");
    }
    questions.addAll(industryPackService.buildInterviewQuestions(assembly.resolvedIndustryPack()));
    questions.add("Which evidence-backed examples best support the top-scoring dimensions in this report?");
    if (subjectContext.shortlistCandidateCardId() != null) {
      questions.add("Which client-safe talking points can be derived later without exposing candidate identity?");
    }
    return List.copyOf(questions);
  }

  private ConsultantMatchReportResponse toResponse(StoredMatchReport stored) {
    MatchReport report = stored.matchReport();
    return new ConsultantMatchReportResponse(
        report.matchReportId().value(),
        stored.subjectType(),
        report.candidateCardRef().value(),
        report.scoreCapDecision().cappedScore().value(),
        report.scoreCapDecision().capApplied(),
        report.scoreCapDecision().reasonCode().name(),
        report.scoreCapDecision().safeExplanation(),
        report.scoreConfidence().name(),
        report.provenanceSummary().authenticityRisk().name(),
        stored.reidentificationRiskSignal().name(),
        stored.industryPackKey(),
        stored.industryPackMaturity() != null ? stored.industryPackMaturity().wireValue() : null,
        stored.ontologyStale(),
        stored.selectionReason(),
        report.ontologyVersion(),
        report.industryPackVersion(),
        ISO_INSTANT.format(report.generatedAt().atOffset(ZoneOffset.UTC)),
        report.dimensionScores().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new DimensionScore(entry.getKey().name(), entry.getValue().value()))
            .toList(),
        new EvidenceCoverageSummary(
            report.evidenceCoverage().coverageRatio(),
            report.evidenceCoverage().coverageLevel().name(),
            report.evidenceCoverage().independentEvidenceCount(),
            report.evidenceCoverage().independentHighTrustEvidenceCount()),
        new ProvenanceSummaryResponse(
            report.provenanceSummary().strongestProvenanceCategory().name(),
            report.provenanceSummary().strongestSourceStrength().name(),
            report.provenanceSummary().provenanceWeight().value(),
            report.provenanceSummary().assertionStrength().name()),
        stored.antiPatternWarnings(),
        stored.explanations(),
        stored.interviewQuestions());
  }

  public record ConsultantMatchSelection(
      String candidateId,
      String shortlistCandidateCardId) {
  }

  private record SubjectContext(
      String subjectType,
      Candidate candidate,
      CandidateProfile candidateProfile,
      UUID shortlistCandidateCardId) {
    CandidateId candidateId() {
      return candidate.candidateId();
    }
  }

  private record AssemblyContext(
      MatchReportGenerationRequest request,
      ReidentificationRiskSignal reidentificationRiskSignal,
      DocumentEvidenceSummary documentEvidence,
      AuthenticityRiskAssessorOutput authenticityAssessment,
      ResolvedIndustryPack resolvedIndustryPack,
      IndustryPackMatchProfile matchProfile) {
  }

  private record DocumentEvidenceSummary(
      List<CandidateDocument> documents,
      List<String> documentTitles,
      Map<MatchDimension, List<MatchEvidenceSignal>> signalsByDimension,
      List<UUID> sourceReferenceIds,
      String resumeText,
      String linkedInText,
      String portfolioText,
      String interviewNotesText,
      int evidenceHitCount,
      boolean requiredDimensionEvidencePresent,
      boolean projectEvidencePresent,
      boolean skillsPresent,
      boolean intentSignalPresent,
      boolean strongIntentSignalPresent) {

    boolean authenticityInputAvailable() {
      return !(resumeText == null || resumeText.isBlank())
          || !(linkedInText == null || linkedInText.isBlank())
          || !(portfolioText == null || portfolioText.isBlank())
          || !(interviewNotesText == null || interviewNotesText.isBlank());
    }
  }

  private static IndustryPackService defaultIndustryPackService() {
    IndustryPack generalPack = new IndustryPack(
        new IndustryPackId(UUID.fromString("00000000-0000-0000-0000-000000280001")),
        new IndustryPackKey("general"),
        "General",
        IndustryPackMaturity.COLD,
        true);
    OntologyVersion ontologyVersion = new OntologyVersion(
        UUID.fromString("00000000-0000-0000-0000-000000280011"),
        generalPack.industryPackId(),
        "ontology-general-v1",
        "fallback",
        "system",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2027-01-01T00:00:00Z"),
        null);
    return new IndustryPackService(new IndustryPackReadPort() {
      @Override
      public Optional<IndustryPack> findById(IndustryPackId industryPackId) {
        return generalPack.industryPackId().equals(industryPackId) ? Optional.of(generalPack) : Optional.empty();
      }

      @Override
      public Optional<IndustryPack> findByKey(IndustryPackKey packKey) {
        return generalPack.packKey().equals(packKey) ? Optional.of(generalPack) : Optional.empty();
      }

      @Override
      public Optional<OntologyVersion> findActiveOntologyVersion(IndustryPackId industryPackId, Instant asOf) {
        return generalPack.industryPackId().equals(industryPackId) ? Optional.of(ontologyVersion) : Optional.empty();
      }

      @Override
      public Optional<IndustryRoleFamilyTemplate> findRoleFamilyTemplate(
          IndustryPackId industryPackId,
          UUID ontologyVersionId,
          String roleFamily) {
        return Optional.empty();
      }
    });
  }
}
