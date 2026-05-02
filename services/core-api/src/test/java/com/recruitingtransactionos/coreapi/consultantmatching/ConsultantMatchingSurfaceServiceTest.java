package com.recruitingtransactionos.coreapi.consultantmatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskExecutionResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityAwareMatchRequestFactory;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityAwareMatchRequestSeed;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorOutput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorTaskService;
import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentId;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentStatus;
import com.recruitingtransactionos.coreapi.candidatedocument.service.CandidateDocumentService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileField;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldLineage;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldPath;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldSourceReference;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileFieldValue;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.consultantmatching.ConsultantMatchingSurfaceService.ConsultantMatchSelection;
import com.recruitingtransactionos.coreapi.consultantmatching.port.MatchReportPersistencePort;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceHit;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentEvidenceRetrievalResult;
import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.documentintelligence.ParsedDocument;
import com.recruitingtransactionos.coreapi.documentintelligence.service.DocumentParsingService;
import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDeniedException;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEvaluator;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import com.recruitingtransactionos.coreapi.job.JobRequirementId;
import com.recruitingtransactionos.coreapi.job.JobRequirementImportance;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import com.recruitingtransactionos.coreapi.job.JobScorecardId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationService;
import com.recruitingtransactionos.coreapi.matching.ReidentificationRiskSignal;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultantMatchingSurfaceServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027c001");
  private static final UUID OWNER_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027c002");
  private static final JobId JOB_ID =
      new JobId(UUID.fromString("00000000-0000-0000-0000-00000027c003"));
  private static final CandidateId CANDIDATE_ID =
      new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000027c004"));
  private static final CandidateProfileId PROFILE_ID =
      new CandidateProfileId(UUID.fromString("00000000-0000-0000-0000-00000027c005"));
  private static final UUID SOURCE_ITEM_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027c006");
  private static final UUID ARCHIVED_SOURCE_ITEM_ID =
      UUID.fromString("00000000-0000-0000-0000-00000027c016");
  private static final Instant NOW = Instant.parse("2026-05-03T00:00:00Z");

  @Mock
  private JobService jobService;

  @Mock
  private CandidateService candidateService;

  @Mock
  private CandidateProfileService candidateProfileService;

  @Mock
  private ShortlistService shortlistService;

  @Mock
  private MatchReportPersistencePort matchReportPersistencePort;

  @Mock
  private CandidateDocumentService candidateDocumentService;

  @Mock
  private DocumentParsingService documentParsingService;

  @Mock
  private AuthenticityRiskAssessorTaskService authenticityRiskAssessorTaskService;

  private ConsultantMatchingSurfaceService service;
  private AuthenticityAwareMatchRequestFactory authenticityAwareMatchRequestFactory;

  @BeforeEach
  void setUp() {
    authenticityAwareMatchRequestFactory = org.mockito.Mockito.spy(new AuthenticityAwareMatchRequestFactory());
    service = new ConsultantMatchingSurfaceService(
        jobService,
        candidateService,
        candidateProfileService,
        shortlistService,
        new MatchReportGenerationService(),
        matchReportPersistencePort,
        authenticityAwareMatchRequestFactory,
        candidateDocumentService,
        documentParsingService,
        authenticityRiskAssessorTaskService,
        new PermissionEnforcer(new PermissionEvaluator()));
  }

  @Test
  void generateMatchReportUsesParsedDocumentEvidenceAndPersistsRealRiskSignal() {
    Job job = sampleJob();
    Candidate candidate = sampleCandidate();
    CandidateProfile profile = sampleProfile();
    CandidateDocument resume = sampleResumeDocument();
    CandidateDocument archivedPassport = sampleArchivedPassportDocument();
    ParsedDocument parsedDocument = new ParsedDocument(
        UUID.fromString("00000000-0000-0000-0000-00000027c007"),
        ORGANIZATION_ID,
        new SourceItemId(SOURCE_ITEM_ID),
        DocumentProcessingStatus.SUCCEEDED,
        "txt-parser",
        "v1",
        "text/plain",
        "sha256:test",
        "en",
        false,
        NOW,
        Optional.of(NOW),
        Optional.empty());
    DocumentEvidenceRetrievalResult evidence = new DocumentEvidenceRetrievalResult(
        parsedDocument,
        List.of(
            new DocumentEvidenceHit(
                UUID.fromString("00000000-0000-0000-0000-00000027c008"),
                0,
                null,
                0,
                120,
                2.0d,
                "Senior Java engineer with finance domain project leadership and strong motivation to switch.")));

    when(jobService.findJobByIdAndOrganizationId(ORGANIZATION_ID, JOB_ID)).thenReturn(Optional.of(job));
    when(jobService.findRequirementsByJobIdAndOrganizationId(ORGANIZATION_ID, JOB_ID)).thenReturn(List.of(
        JobRequirement.builder()
            .jobRequirementId(new JobRequirementId(UUID.fromString("00000000-0000-0000-0000-00000027c009")))
            .organizationId(ORGANIZATION_ID)
            .jobId(JOB_ID)
            .requirementType("skill")
            .label("Java")
            .importance(JobRequirementImportance.MUST_HAVE)
            .detail("Strong Java backend experience in finance.")
            .sortOrder(0)
            .createdAt(NOW)
            .updatedAt(NOW)
            .build()));
    when(jobService.findActiveScorecardByJobIdAndOrganizationId(ORGANIZATION_ID, JOB_ID)).thenReturn(Optional.of(
        JobScorecard.builder()
            .jobScorecardId(new JobScorecardId(UUID.fromString("00000000-0000-0000-0000-00000027c00a")))
            .organizationId(ORGANIZATION_ID)
            .jobId(JOB_ID)
            .dimensions("technical fit, industry fit, motivation fit")
            .scoringGuidance("Reward direct project evidence and strong motivation.")
            .status("active")
            .createdAt(NOW)
            .updatedAt(NOW)
            .build()));
    when(candidateService.findCandidateByIdAndOrganizationId(ORGANIZATION_ID, CANDIDATE_ID))
        .thenReturn(Optional.of(candidate));
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(ORGANIZATION_ID, CANDIDATE_ID))
        .thenReturn(Optional.of(profile));
    when(candidateDocumentService.findDocumentsByCandidateIdAndOrganizationId(ORGANIZATION_ID, CANDIDATE_ID))
        .thenReturn(List.of(resume, archivedPassport));
    when(documentParsingService.findLatestParsedDocument(eq(ORGANIZATION_ID), any(SourceItemId.class)))
        .thenReturn(Optional.of(parsedDocument));
    when(documentParsingService.retrieveEvidence(eq(ORGANIZATION_ID), any(SourceItemId.class), anyString(), eq(3)))
        .thenAnswer(invocation -> {
          String query = invocation.getArgument(2, String.class).toLowerCase();
          if (query.contains("java")
              || query.contains("finance")
              || query.contains("motivation")
              || query.contains("technical fit")) {
            return evidence;
          }
          return new DocumentEvidenceRetrievalResult(parsedDocument, List.of());
        });
    when(authenticityRiskAssessorTaskService.execute(
        eq(ORGANIZATION_ID),
        any(),
        any(),
        eq(List.of(SOURCE_ITEM_ID)),
        any(),
        any(),
        any()))
        .thenReturn(new AuthenticityRiskAssessorResult(
            org.mockito.Mockito.mock(AITaskExecutionResult.class),
            new AuthenticityRiskAssessorOutput(
                "high",
                91,
                true,
                List.of("suspicious_timeline"))));
    when(matchReportPersistencePort.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0, StoredMatchReport.class));

    var response = service.generateMatchReport(
        consultantCreateAccessRequest(),
        ORGANIZATION_ID,
        JOB_ID,
        new ConsultantMatchSelection(CANDIDATE_ID.value().toString(), null));

    ArgumentCaptor<StoredMatchReport> storedCaptor = ArgumentCaptor.forClass(StoredMatchReport.class);
    verify(matchReportPersistencePort).create(storedCaptor.capture());
    StoredMatchReport stored = storedCaptor.getValue();

    assertThat(stored.matchReport().jobRef().value())
        .isEqualTo("job_ref_" + JOB_ID.value().toString().replace("-", ""));
    assertThat(stored.reidentificationRiskSignal()).isEqualTo(ReidentificationRiskSignal.MEDIUM);
    assertThat(stored.matchReport().provenanceSummary().authenticityRisk().name()).isEqualTo("HIGH");
    assertThat(stored.explanations())
        .anyMatch(value -> value.contains("Parsed candidate documents contributed"))
        .anyMatch(value -> value.contains("suspicious_timeline"));
    assertThat(response.reidentificationRiskSignal()).isEqualTo("MEDIUM");
    assertThat(response.authenticityRisk()).isEqualTo("HIGH");

    verify(authenticityRiskAssessorTaskService).execute(
        eq(ORGANIZATION_ID),
        any(),
        any(),
        eq(List.of(SOURCE_ITEM_ID)),
        any(),
        any(),
        any());
    assertThat(response.explanations())
        .anyMatch(value -> value.contains("authenticity task"));
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(documentParsingService, org.mockito.Mockito.atLeastOnce()).retrieveEvidence(
        eq(ORGANIZATION_ID),
        any(SourceItemId.class),
        queryCaptor.capture(),
        eq(3));
    assertThat(queryCaptor.getAllValues()).allMatch(value -> value != null && !value.isBlank());
    assertThat(queryCaptor.getAllValues()).anyMatch(value ->
        value.toLowerCase().contains("java") || value.toLowerCase().contains("finance"));
  }

  @Test
  void generateMatchReportFailsClosedWhenServiceAccessIsNotConsultantMatchReportCreate() {
    assertThatThrownBy(() -> service.generateMatchReport(
        clientReadAccessRequest(),
        ORGANIZATION_ID,
        JOB_ID,
        new ConsultantMatchSelection(CANDIDATE_ID.value().toString(), null)))
        .isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(jobService, candidateService, candidateProfileService, matchReportPersistencePort);
  }

  @Test
  void generateMatchReportDoesNotEscalateWeakIntentSignalFromUnrelatedTrustedFields() {
    Job job = sampleJob();
    Candidate candidate = sampleCandidate();
    CandidateProfile profile = CandidateProfile.builder()
        .candidateProfileId(PROFILE_ID)
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .profileVersion(new CandidateProfileVersion(1))
        .fields(List.of(
            profileField(
                CandidateProfileFieldPath.INTENT_MOTIVATION_TOWARD_OPPORTUNITY,
                "Potentially open, but not confirmed.",
                CandidateProfileFieldStatus.AI_EXTRACTED),
            profileField(
                CandidateProfileFieldPath.SKILLS_PRIMARY_SKILLS,
                "Java, Spring Boot",
                CandidateProfileFieldStatus.CONSULTANT_ATTESTED)))
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();

    when(jobService.findJobByIdAndOrganizationId(ORGANIZATION_ID, JOB_ID)).thenReturn(Optional.of(job));
    when(jobService.findRequirementsByJobIdAndOrganizationId(ORGANIZATION_ID, JOB_ID)).thenReturn(List.of(
        JobRequirement.builder()
            .jobRequirementId(new JobRequirementId(UUID.fromString("00000000-0000-0000-0000-00000027c019")))
            .organizationId(ORGANIZATION_ID)
            .jobId(JOB_ID)
            .requirementType("skill")
            .label("Java")
            .importance(JobRequirementImportance.MUST_HAVE)
            .detail("Strong Java backend experience in finance.")
            .sortOrder(0)
            .createdAt(NOW)
            .updatedAt(NOW)
            .build()));
    when(jobService.findActiveScorecardByJobIdAndOrganizationId(ORGANIZATION_ID, JOB_ID))
        .thenReturn(Optional.empty());
    when(candidateService.findCandidateByIdAndOrganizationId(ORGANIZATION_ID, CANDIDATE_ID))
        .thenReturn(Optional.of(candidate));
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(ORGANIZATION_ID, CANDIDATE_ID))
        .thenReturn(Optional.of(profile));
    when(candidateDocumentService.findDocumentsByCandidateIdAndOrganizationId(ORGANIZATION_ID, CANDIDATE_ID))
        .thenReturn(List.of());
    when(matchReportPersistencePort.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0, StoredMatchReport.class));

    service.generateMatchReport(
        consultantCreateAccessRequest(),
        ORGANIZATION_ID,
        JOB_ID,
        new ConsultantMatchSelection(CANDIDATE_ID.value().toString(), null));

    ArgumentCaptor<AuthenticityAwareMatchRequestSeed> seedCaptor =
        ArgumentCaptor.forClass(AuthenticityAwareMatchRequestSeed.class);
    verify(authenticityAwareMatchRequestFactory).create(seedCaptor.capture(), any());
    assertThat(seedCaptor.getValue().candidateIntentSignalStrength())
        .isEqualTo(com.recruitingtransactionos.coreapi.matching.EvidenceAssertionStrength.IMPLIED);
  }

  private static AccessRequest consultantCreateAccessRequest() {
    return new AccessRequest(
        PortalRole.CONSULTANT,
        ResourceType.MATCH_REPORT,
        AccessAction.CREATE,
        FieldClassification.CONSULTANT_PRIVATE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static AccessRequest clientReadAccessRequest() {
    return new AccessRequest(
        PortalRole.CLIENT,
        ResourceType.MATCH_REPORT,
        AccessAction.READ,
        FieldClassification.CONSULTANT_PRIVATE,
        Set.of(RelationshipScope.SAME_ORGANIZATION),
        false);
  }

  private static Candidate sampleCandidate() {
    return Candidate.builder()
        .candidateId(CANDIDATE_ID)
        .organizationId(ORGANIZATION_ID)
        .status(CandidateStatus.AVAILABLE)
        .currentProfileId(PROFILE_ID)
        .privacyStatus("internal_only")
        .ownerConsultantId(OWNER_ID)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static CandidateProfile sampleProfile() {
    return CandidateProfile.builder()
        .candidateProfileId(PROFILE_ID)
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .profileVersion(new CandidateProfileVersion(1))
        .fields(List.of(
            profileField(
                CandidateProfileFieldPath.SKILLS_PRIMARY_SKILLS,
                "Java, Spring Boot",
                CandidateProfileFieldStatus.CONSULTANT_ATTESTED),
            profileField(
                CandidateProfileFieldPath.INTENT_MOTIVATION_TOWARD_OPPORTUNITY,
                "Open to a stronger fintech platform role.",
                CandidateProfileFieldStatus.CANDIDATE_CONFIRMED)))
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static CandidateProfileField profileField(
      CandidateProfileFieldPath path,
      String value,
      CandidateProfileFieldStatus status) {
    CandidateProfileField.Builder builder = CandidateProfileField.builder()
        .fieldPath(path)
        .value(CandidateProfileFieldValue.ofString(value))
        .fieldStatus(status)
        .lineage(new CandidateProfileFieldLineage(
            List.of(CandidateProfileFieldSourceReference.sourceSpan(
                "span:consultant-matching:" + path.value(),
                "resume",
                NOW)),
            "consultant-matching-test",
            NOW));
    if (status == CandidateProfileFieldStatus.CONSULTANT_ATTESTED
        || status == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED) {
      builder.confirmedByActorId(OWNER_ID);
    }
    if (status == CandidateProfileFieldStatus.CANDIDATE_CONFIRMED) {
      builder.confirmedAgainstProfileVersion(new CandidateProfileVersion(1));
    }
    return builder.build();
  }

  private static CandidateDocument sampleResumeDocument() {
    return CandidateDocument.builder()
        .candidateDocumentId(new CandidateDocumentId(UUID.fromString("00000000-0000-0000-0000-00000027c00b")))
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .documentType("resume")
        .title("Candidate Resume")
        .sourceItemId(SOURCE_ITEM_ID)
        .status(CandidateDocumentStatus.ACTIVE)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static CandidateDocument sampleArchivedPassportDocument() {
    return CandidateDocument.builder()
        .candidateDocumentId(new CandidateDocumentId(UUID.fromString("00000000-0000-0000-0000-00000027c017")))
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .documentType("passport")
        .title("Archived Passport")
        .sourceItemId(ARCHIVED_SOURCE_ITEM_ID)
        .status(CandidateDocumentStatus.ARCHIVED)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static Job sampleJob() {
    return Job.builder()
        .jobId(JOB_ID)
        .organizationId(ORGANIZATION_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000027c00c")))
        .title("Senior Java Consultant")
        .description("Finance platform modernization")
        .location("Singapore")
        .status(JobStatus.ACTIVATED)
        .ownerConsultantId(OWNER_ID)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }
}
