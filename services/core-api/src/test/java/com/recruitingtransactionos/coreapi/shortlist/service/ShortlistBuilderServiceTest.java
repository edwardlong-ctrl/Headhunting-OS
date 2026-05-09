package com.recruitingtransactionos.coreapi.shortlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consultantmatching.StoredMatchReport;
import com.recruitingtransactionos.coreapi.consultantmatching.port.MatchReportPersistencePort;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.matching.AuthenticityRiskLevel;
import com.recruitingtransactionos.coreapi.matching.EvidenceAssertionStrength;
import com.recruitingtransactionos.coreapi.matching.EvidenceCoverage;
import com.recruitingtransactionos.coreapi.matching.EvidenceCoverageLevel;
import com.recruitingtransactionos.coreapi.matching.MatchDimension;
import com.recruitingtransactionos.coreapi.matching.MatchJobRef;
import com.recruitingtransactionos.coreapi.matching.MatchReport;
import com.recruitingtransactionos.coreapi.matching.MatchReportId;
import com.recruitingtransactionos.coreapi.matching.MatchScore;
import com.recruitingtransactionos.coreapi.matching.MatchSubjectRef;
import com.recruitingtransactionos.coreapi.matching.ProvenanceCategory;
import com.recruitingtransactionos.coreapi.matching.ProvenanceSourceStrength;
import com.recruitingtransactionos.coreapi.matching.ProvenanceSummary;
import com.recruitingtransactionos.coreapi.matching.ProvenanceWeight;
import com.recruitingtransactionos.coreapi.matching.ReidentificationRiskSignal;
import com.recruitingtransactionos.coreapi.matching.ScoreCapDecision;
import com.recruitingtransactionos.coreapi.matching.ScoreCapReason;
import com.recruitingtransactionos.coreapi.matching.ScoreConfidence;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessment;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskDecision;
import com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskLevel;
import com.recruitingtransactionos.coreapi.privacyredaction.PersistedReidentificationRiskAssessment;
import com.recruitingtransactionos.coreapi.privacyredaction.RedactionAuditService;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.truthlayer.WorkflowActionCode;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditRequest;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShortlistBuilderServiceTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000222");
  private static final Instant NOW = Instant.parse("2026-05-03T08:00:00Z");

  @Mock private ShortlistService shortlistService;
  @Mock private CandidateService candidateService;
  @Mock private CandidateProfileService candidateProfileService;
  @Mock private MatchReportPersistencePort matchReportPersistencePort;
  @Mock private ConsentRecordPort consentRecordPort;
  @Mock private JobService jobService;
  @Mock private WorkflowTransitionAuditService workflowTransitionAuditService;
  @Mock private RedactionAuditService redactionAuditService;

  @Test
  void sendToClientFailsClosedWhenPreSendChecksDoNotPass() {
    Shortlist shortlist = shortlist(ShortlistStatus.DRAFT, 1);
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    ShortlistCandidateCard[] persistedCard = new ShortlistCandidateCard[1];
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenAnswer(invocation -> persistedCard[0] == null ? List.of() : List.of(persistedCard[0]));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    assertThatThrownBy(() -> service.sendToClient(ORG_ID, ACTOR_ID, shortlist.shortlistId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_send_blocked");
    verify(shortlistService, never()).updateShortlist(any());
  }

  @Test
  void addCandidateCardCreatesIncludedCardAndMovesJobIntoShortlistInProgress() {
    Shortlist shortlist = shortlist(ShortlistStatus.DRAFT, 1);
    CandidateId candidateId = new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000000333"));
    CandidateProfileId profileId =
        new CandidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000444"));
    Candidate candidate = Candidate.builder()
        .candidateId(candidateId)
        .organizationId(ORG_ID)
        .status(CandidateStatus.AVAILABLE)
        .currentProfileId(profileId)
        .privacyStatus("internal_only")
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    CandidateProfile profile = CandidateProfile.builder()
        .candidateProfileId(profileId)
        .organizationId(ORG_ID)
        .candidateId(candidateId)
        .profileVersion(new CandidateProfileVersion(1))
        .fields(List.of())
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
    Job job = Job.builder()
        .jobId(shortlist.jobId())
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000555")))
        .title("Platform engineer")
        .status(JobStatus.ACTIVATED)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();

    ShortlistCandidateCard[] persistedCard = new ShortlistCandidateCard[1];
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenAnswer(invocation -> persistedCard[0] == null ? List.of() : List.of(persistedCard[0]));
    when(candidateService.findCandidateByIdAndOrganizationId(ORG_ID, candidateId))
        .thenReturn(Optional.of(candidate));
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(ORG_ID, candidateId))
        .thenReturn(Optional.of(profile));
    when(matchReportPersistencePort.findLatestByCandidateIdAndJobId(ORG_ID, shortlist.jobId(), candidateId.value()))
        .thenReturn(Optional.empty());
    when(shortlistService.addCandidateCard(any())).thenAnswer(invocation -> {
      persistedCard[0] = invocation.getArgument(0);
      return persistedCard[0];
    });
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, shortlist.jobId()))
        .thenReturn(Optional.of(job));
    when(jobService.updateJob(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    ShortlistBuilderState state =
        service.addCandidateCard(ORG_ID, ACTOR_ID, shortlist.shortlistId(), candidateId, null, "Client-safe note");

    assertThat(state.cards()).hasSize(1);
    ShortlistCandidateCard card = state.cards().get(0);
    assertThat(card.status().wireValue()).isEqualTo("included");
    assertThat(card.clientNotes()).isEqualTo("Client-safe note");
    assertThat(card.metadata()).doesNotContain("candidate profile");
    assertThat(state.deliveryPreview().clientSafeSummary()).doesNotContain("candidate profile");
    assertThat(state.preSendChecks())
        .anyMatch(check -> check.code().equals("has_included_cards") && check.passed());
    assertThat(state.preSendChecks())
        .anyMatch(check -> check.code().equals("status_ready_for_review") && !check.passed());

    ArgumentCaptor<Job> updatedJobCaptor = ArgumentCaptor.forClass(Job.class);
    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(jobService).updateJob(updatedJobCaptor.capture());
    assertThat(updatedJobCaptor.getValue().status()).isEqualTo(JobStatus.SHORTLIST_IN_PROGRESS);
    verify(workflowTransitionAuditService, org.mockito.Mockito.atLeast(2)).record(auditCaptor.capture());
    assertThat(auditCaptor.getAllValues())
        .anyMatch(request -> request.actionCode().equals("CANDIDATE_SHORTLISTED"));
  }

  @Test
  void sendToClientFailsWhenIncludedCardHasHighReidentificationRisk() {
    Shortlist shortlist = shortlist(ShortlistStatus.READY_FOR_REVIEW, 2);
    ShortlistCandidateCard card = shortlistCard(
        shortlist,
        UUID.fromString("00000000-0000-0000-0000-000000000333"),
        UUID.fromString("00000000-0000-0000-0000-000000000444"),
        "high");
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(card));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    assertThatThrownBy(() -> service.sendToClient(ORG_ID, ACTOR_ID, shortlist.shortlistId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("shortlist_send_blocked");

    ShortlistBuilderState state = service.getBuilderState(ORG_ID, shortlist.shortlistId());
    assertThat(state.preSendChecks())
        .anyMatch(check -> check.code().equals("reidentification_risk_within_gate") && !check.passed());
  }

  @Test
  void sendToClientAllowsAnonymousClientSafeShortlistBeforeUnlockConsent() {
    Shortlist shortlist = shortlist(ShortlistStatus.READY_FOR_REVIEW, 2);
    ShortlistCandidateCard card = shortlistCard(
        shortlist,
        UUID.fromString("00000000-0000-0000-0000-000000000333"),
        UUID.fromString("00000000-0000-0000-0000-000000000444"),
        "low");
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(card));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    when(shortlistService.updateShortlist(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, shortlist.jobId()))
        .thenReturn(Optional.of(job(JobStatus.SHORTLIST_IN_PROGRESS)));
    when(jobService.updateJob(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ShortlistBuilderState state = service.sendToClient(ORG_ID, ACTOR_ID, shortlist.shortlistId());
    assertThat(state.shortlist().status()).isEqualTo(ShortlistStatus.SENT_TO_CLIENT);
    assertThat(state.preSendChecks())
        .noneMatch(check -> check.code().equals("client_safe_consent_confirmed"));
  }

  @Test
  void emptyShortlistChecksFailClosedInsteadOfPassingVacuously() {
    Shortlist shortlist = shortlist(ShortlistStatus.DRAFT, 1);
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of());

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    ShortlistBuilderState state = service.getBuilderState(ORG_ID, shortlist.shortlistId());

    assertThat(state.preSendChecks())
        .anyMatch(check -> check.code().equals("has_included_cards") && !check.passed())
        .anyMatch(check -> check.code().equals("anonymous_cards_generated") && !check.passed())
        .anyMatch(check -> check.code().equals("delivery_preview_ready") && !check.passed())
        .anyMatch(check -> check.code().equals("reidentification_risk_within_gate") && !check.passed())
        .noneMatch(check -> check.code().equals("client_safe_consent_confirmed"));
    assertThat(state.deliveryPreview().clientSafeSummary())
        .contains("No client-safe shortlist summary is available");
  }

  @Test
  void deliveryPreviewFailsClosedWhenAnyIncludedCardMetadataIsUnreadable() {
    Shortlist shortlist = shortlist(ShortlistStatus.READY_FOR_REVIEW, 2);
    ShortlistCandidateCard validCard = shortlistCard(
        shortlist,
        UUID.fromString("00000000-0000-0000-0000-000000000333"),
        UUID.fromString("00000000-0000-0000-0000-000000000444"),
        "low");
    ShortlistCandidateCard invalidMetadataCard = ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(
            UUID.fromString("00000000-0000-0000-0000-000000000778")))
        .organizationId(ORG_ID)
        .shortlistId(shortlist.shortlistId())
        .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000779"))
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000000780")))
        .candidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000781"))
        .sortOrder(1)
        .status(ShortlistCandidateCardStatus.INCLUDED)
        .matchReportId(null)
        .clientNotes(null)
        .metadata("{not-valid-json")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(validCard, invalidMetadataCard));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    ShortlistBuilderState state = service.getBuilderState(ORG_ID, shortlist.shortlistId());

    assertThat(state.preSendChecks())
        .anyMatch(check -> check.code().equals("anonymous_cards_generated") && !check.passed())
        .anyMatch(check -> check.code().equals("delivery_preview_ready") && !check.passed());
    assertThat(state.deliveryPreview().clientSafeSummary())
        .contains("No client-safe shortlist summary is available");
  }

  @Test
  void sentShortlistKeepsReviewGateDisplayedAsPassedButCannotSendAgain() {
    Shortlist shortlist = shortlist(ShortlistStatus.SENT_TO_CLIENT, 3);
    ShortlistCandidateCard card = shortlistCard(
        shortlist,
        UUID.fromString("00000000-0000-0000-0000-000000000333"),
        UUID.fromString("00000000-0000-0000-0000-000000000444"),
        "low");
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(card));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    ShortlistBuilderState state = service.getBuilderState(ORG_ID, shortlist.shortlistId());

    assertThat(state.preSendChecks())
        .anyMatch(check -> check.code().equals("status_ready_for_review") && check.passed());
    assertThat(state.canSend()).isFalse();
  }

  @Test
  void updateCandidateCardRecordsAuditWhenRemovingIncludedCard() {
    Shortlist shortlist = shortlist(ShortlistStatus.DRAFT, 2);
    ShortlistCandidateCard existing = shortlistCard(
        shortlist,
        UUID.fromString("00000000-0000-0000-0000-000000000333"),
        UUID.fromString("00000000-0000-0000-0000-000000000444"),
        "low");
    ShortlistCandidateCard removed = ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(existing.shortlistCandidateCardId())
        .organizationId(existing.organizationId())
        .shortlistId(existing.shortlistId())
        .anonymousCandidateCardId(existing.anonymousCandidateCardId())
        .candidateId(existing.candidateId())
        .candidateProfileId(existing.candidateProfileId())
        .sortOrder(existing.sortOrder())
        .status(ShortlistCandidateCardStatus.REMOVED)
        .matchReportId(existing.matchReportId())
        .clientNotes(existing.clientNotes())
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(NOW)
        .version(existing.version() + 1)
        .build();
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardByIdAndOrganizationId(ORG_ID, existing.shortlistCandidateCardId()))
        .thenReturn(Optional.of(existing));
    when(shortlistService.updateCandidateCard(any())).thenReturn(removed);
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(removed));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    ShortlistBuilderState state = service.updateCandidateCard(
        ORG_ID,
        ACTOR_ID,
        shortlist.shortlistId(),
        existing.shortlistCandidateCardId(),
        existing.version(),
        null,
        ShortlistCandidateCardStatus.REMOVED,
        null);

    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService).record(auditCaptor.capture());
    assertThat(auditCaptor.getValue().actionCode())
        .isEqualTo(WorkflowActionCode.SHORTLIST_CARD_REMOVED.wireValue());
    assertThat(auditCaptor.getValue().actorId()).isEqualTo(ACTOR_ID);
    assertThat(state.cards()).singleElement().satisfies(card ->
        assertThat(card.status()).isEqualTo(ShortlistCandidateCardStatus.REMOVED));
  }

  @Test
  void updateCandidateCardRecordsAuditWhenRestoringRemovedCard() {
    Shortlist shortlist = shortlist(ShortlistStatus.READY_FOR_REVIEW, 2);
    ShortlistCandidateCard existing = ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(
            UUID.fromString("00000000-0000-0000-0000-000000000778")))
        .organizationId(ORG_ID)
        .shortlistId(shortlist.shortlistId())
        .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000779"))
        .candidateId(new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000000780")))
        .candidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000781"))
        .sortOrder(0)
        .status(ShortlistCandidateCardStatus.REMOVED)
        .matchReportId(null)
        .clientNotes(null)
        .metadata("{\"anonymousCandidateRef\":\"anon_candidate_1\",\"projectionVersion\":\"task29-shortlist-v1\",\"redactionLevel\":\"l2_client_safe\",\"generalizedHeadline\":\"Consultant-reviewed candidate\",\"generalizedRoleFamily\":\"Confidential role family\",\"generalizedSeniorityBand\":\"Consultant-reviewed shortlist level\",\"generalizedLocationRegion\":\"Location shared after identity unlock\",\"safeSummary\":\"safe summary\",\"safeSkillSummary\":\"safe skill summary\",\"safeEvidenceSummaries\":[\"evidence\"],\"safeMatchNarratives\":[\"narrative\"],\"overallScore\":4,\"confidence\":\"high\",\"reidentificationRiskSignal\":\"low\",\"dimensionScores\":[]}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    ShortlistCandidateCard restored = ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(existing.shortlistCandidateCardId())
        .organizationId(existing.organizationId())
        .shortlistId(existing.shortlistId())
        .anonymousCandidateCardId(existing.anonymousCandidateCardId())
        .candidateId(existing.candidateId())
        .candidateProfileId(existing.candidateProfileId())
        .sortOrder(existing.sortOrder())
        .status(ShortlistCandidateCardStatus.INCLUDED)
        .matchReportId(existing.matchReportId())
        .clientNotes(existing.clientNotes())
        .metadata(existing.metadata())
        .createdAt(existing.createdAt())
        .updatedAt(NOW)
        .version(existing.version() + 1)
        .build();
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardByIdAndOrganizationId(ORG_ID, existing.shortlistCandidateCardId()))
        .thenReturn(Optional.of(existing));
    when(shortlistService.updateCandidateCard(any())).thenReturn(restored);
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(restored));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    ShortlistBuilderState state = service.updateCandidateCard(
        ORG_ID,
        ACTOR_ID,
        shortlist.shortlistId(),
        existing.shortlistCandidateCardId(),
        existing.version(),
        null,
        ShortlistCandidateCardStatus.INCLUDED,
        null);

    ArgumentCaptor<WorkflowTransitionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(WorkflowTransitionAuditRequest.class);
    verify(workflowTransitionAuditService).record(auditCaptor.capture());
    assertThat(auditCaptor.getValue().actionCode())
        .isEqualTo(WorkflowActionCode.SHORTLIST_CARD_RESTORED.wireValue());
    assertThat(auditCaptor.getValue().actorId()).isEqualTo(ACTOR_ID);
    assertThat(state.cards()).singleElement().satisfies(card ->
        assertThat(card.status()).isEqualTo(ShortlistCandidateCardStatus.INCLUDED));
  }

  @Test
  void addCandidateCardBlocksMutationAfterShortlistWasSent() {
    Shortlist shortlist = shortlist(ShortlistStatus.SENT_TO_CLIENT, 2);
    CandidateId candidateId = new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000000333"));
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    assertThatThrownBy(() -> service.addCandidateCard(
        ORG_ID, ACTOR_ID, shortlist.shortlistId(), candidateId, null, "note"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("shortlist_builder_locked_after_send");
    verify(candidateService, never()).findCandidateByIdAndOrganizationId(any(), any());
  }

  @Test
  void updateCandidateCardBlocksMutationAfterShortlistWasSent() {
    Shortlist shortlist = shortlist(ShortlistStatus.SENT_TO_CLIENT, 2);
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    assertThatThrownBy(() -> service.updateCandidateCard(
        ORG_ID,
        ACTOR_ID,
        shortlist.shortlistId(),
        new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000777")),
        1,
        null,
        ShortlistCandidateCardStatus.INCLUDED,
        "note"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("shortlist_builder_locked_after_send");
    verify(shortlistService, never()).findCardByIdAndOrganizationId(any(), any());
  }

  @Test
  void addCandidateCardPersistsLatestMatchReportId() {
    Shortlist shortlist = shortlist(ShortlistStatus.DRAFT, 1);
    CandidateId candidateId = new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000000333"));
    CandidateProfileId profileId =
        new CandidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000444"));
    Candidate candidate = Candidate.builder()
        .candidateId(candidateId)
        .organizationId(ORG_ID)
        .status(CandidateStatus.AVAILABLE)
        .currentProfileId(profileId)
        .privacyStatus("internal_only")
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    CandidateProfile profile = CandidateProfile.builder()
        .candidateProfileId(profileId)
        .organizationId(ORG_ID)
        .candidateId(candidateId)
        .profileVersion(new CandidateProfileVersion(1))
        .fields(List.of())
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
    Job job = Job.builder()
        .jobId(shortlist.jobId())
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000555")))
        .title("Platform engineer")
        .status(JobStatus.ACTIVATED)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    StoredMatchReport storedMatchReport = storedMatchReport(shortlist.jobId().value());

    ShortlistCandidateCard[] persistedCard = new ShortlistCandidateCard[1];
    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenAnswer(invocation -> persistedCard[0] == null ? List.of() : List.of(persistedCard[0]));
    when(candidateService.findCandidateByIdAndOrganizationId(ORG_ID, candidateId))
        .thenReturn(Optional.of(candidate));
    when(candidateProfileService.findCandidateProfileByCandidateIdAndOrganizationId(ORG_ID, candidateId))
        .thenReturn(Optional.of(profile));
    when(matchReportPersistencePort.findLatestByCandidateIdAndJobId(ORG_ID, shortlist.jobId(), candidateId.value()))
        .thenReturn(Optional.of(storedMatchReport));
    when(shortlistService.addCandidateCard(any())).thenAnswer(invocation -> {
      persistedCard[0] = invocation.getArgument(0);
      return persistedCard[0];
    });
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, shortlist.jobId()))
        .thenReturn(Optional.of(job));
    when(jobService.updateJob(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService);

    ShortlistBuilderState state =
        service.addCandidateCard(ORG_ID, ACTOR_ID, shortlist.shortlistId(), candidateId, null, null);

    assertThat(state.cards()).hasSize(1);
    assertThat(state.cards().get(0).matchReportId())
        .isEqualTo(UUID.fromString("12345678-1234-1234-1234-1234567890ab"));
  }

  @Test
  void sendToClientReevaluatesUsingCardBoundProfileVersion() {
    Shortlist shortlist = shortlist(ShortlistStatus.READY_FOR_REVIEW, 2);
    ShortlistCandidateCard card = shortlistCard(
        shortlist,
        UUID.fromString("00000000-0000-0000-0000-000000000333"),
        UUID.fromString("00000000-0000-0000-0000-000000000444"),
        "low");
    Candidate candidate = Candidate.builder()
        .candidateId(card.candidateId())
        .organizationId(ORG_ID)
        .status(CandidateStatus.AVAILABLE)
        .currentProfileId(new CandidateProfileId(UUID.fromString("00000000-0000-0000-0000-000000000999")))
        .privacyStatus("internal_only")
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    CandidateProfile boundProfile = CandidateProfile.builder()
        .candidateProfileId(new CandidateProfileId(card.candidateProfileId()))
        .organizationId(ORG_ID)
        .candidateId(card.candidateId())
        .profileVersion(new CandidateProfileVersion(1))
        .fields(List.of())
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
    Job job = Job.builder()
        .jobId(shortlist.jobId())
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000555")))
        .title("Platform engineer")
        .status(JobStatus.SHORTLIST_IN_PROGRESS)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
    WorkflowEventId workflowEventId = new WorkflowEventId(
        UUID.fromString("00000000-0000-0000-0000-000000000888"));

    when(shortlistService.findShortlistByIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(Optional.of(shortlist));
    when(shortlistService.findCardsByShortlistIdAndOrganizationId(ORG_ID, shortlist.shortlistId()))
        .thenReturn(List.of(card));
    when(shortlistService.updateShortlist(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(candidateService.findCandidateByIdAndOrganizationId(ORG_ID, card.candidateId()))
        .thenReturn(Optional.of(candidate));
    when(candidateProfileService.findCandidateProfileByIdAndOrganizationId(
        ORG_ID,
        new CandidateProfileId(card.candidateProfileId())))
            .thenReturn(Optional.of(boundProfile));
    when(matchReportPersistencePort.findLatestByCandidateIdAndJobId(
        ORG_ID,
        shortlist.jobId(),
        card.candidateId().value()))
            .thenReturn(Optional.empty());
    when(jobService.findJobByIdAndOrganizationId(ORG_ID, shortlist.jobId()))
        .thenReturn(Optional.of(job));
    when(jobService.updateJob(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(redactionAuditService.evaluate(any())).thenAnswer(invocation -> {
      RedactionAuditService.RedactionAuditRequest request = invocation.getArgument(0);
      ReidentificationRiskAssessment assessment = new ReidentificationRiskAssessment(
          request.snapshot().cardId(),
          request.snapshot().redactionLevel(),
          ReidentificationRiskLevel.LOW,
          java.util.Set.of(),
          ReidentificationRiskDecision.ALLOW,
          0.0,
          "safe anonymous shortlist projection");
      PersistedReidentificationRiskAssessment persisted =
          new PersistedReidentificationRiskAssessment(
              request.reidentificationRiskAssessmentRef(),
              request.organizationId(),
              request.candidateRef(),
              request.jobRef(),
              assessment,
              Optional.of(workflowEventId),
              NOW);
      return new RedactionAuditService.RedactionAuditResult(
          persisted,
          workflowEventId,
          request.snapshot(),
          false);
    });

    ShortlistBuilderService service = new ShortlistBuilderService(
        shortlistService,
        candidateService,
        candidateProfileService,
        matchReportPersistencePort,
        consentRecordPort,
        jobService,
        workflowTransitionAuditService,
        new com.recruitingtransactionos.coreapi.clientsafeprojection.ClientSafeCandidateProjectionService(),
        new com.recruitingtransactionos.coreapi.clientsafeprojection.ReidentificationRiskAssessmentService(),
        redactionAuditService);

    service.sendToClient(ORG_ID, ACTOR_ID, shortlist.shortlistId());

    ArgumentCaptor<RedactionAuditService.RedactionAuditRequest> auditCaptor =
        ArgumentCaptor.forClass(RedactionAuditService.RedactionAuditRequest.class);
    verify(redactionAuditService).evaluate(auditCaptor.capture());
    assertThat(auditCaptor.getValue().snapshot().rawCandidateProfileId())
        .isEqualTo(card.candidateProfileId().toString());
    verify(candidateProfileService).findCandidateProfileByIdAndOrganizationId(
        ORG_ID,
        new CandidateProfileId(card.candidateProfileId()));
    verify(candidateProfileService, never()).findCandidateProfileByCandidateIdAndOrganizationId(
        ORG_ID,
        card.candidateId());
  }

  private static Shortlist shortlist(ShortlistStatus status, int version) {
    return Shortlist.builder()
        .shortlistId(new ShortlistId(UUID.fromString("00000000-0000-0000-0000-000000000101")))
        .organizationId(ORG_ID)
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-000000000102")))
        .title("Task 29 shortlist")
        .status(status)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(version)
        .build();
  }

  private static ShortlistCandidateCard shortlistCard(
      Shortlist shortlist,
      UUID candidateId,
      UUID candidateProfileId,
      String reidentificationRiskSignal) {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(new ShortlistCandidateCardId(
            UUID.fromString("00000000-0000-0000-0000-000000000666")))
        .organizationId(ORG_ID)
        .shortlistId(shortlist.shortlistId())
        .anonymousCandidateCardId(UUID.fromString("00000000-0000-0000-0000-000000000667"))
        .candidateId(new CandidateId(candidateId))
        .candidateProfileId(candidateProfileId)
        .sortOrder(0)
        .status(ShortlistCandidateCardStatus.INCLUDED)
        .matchReportId(null)
        .clientNotes("note")
        .metadata("{\"anonymousCandidateRef\":\"anon_candidate_1\",\"projectionVersion\":\"task29-shortlist-v1\","
            + "\"redactionLevel\":\"l2_client_safe\",\"generalizedHeadline\":\"Consultant-reviewed candidate\","
            + "\"generalizedRoleFamily\":\"Confidential role family\",\"generalizedSeniorityBand\":\"Consultant-reviewed shortlist level\","
            + "\"generalizedLocationRegion\":\"Location shared after identity unlock\",\"safeSummary\":\"safe summary\","
            + "\"safeSkillSummary\":\"safe skill summary\",\"safeEvidenceSummaries\":[\"evidence\"],"
            + "\"safeMatchNarratives\":[\"narrative\"],\"overallScore\":4,\"confidence\":\"high\","
            + "\"reidentificationRiskSignal\":\"" + reidentificationRiskSignal + "\",\"dimensionScores\":[]}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
  }

  private static Job job(JobStatus status) {
    return Job.builder()
        .jobId(new JobId(UUID.fromString("00000000-0000-0000-0000-000000000102")))
        .organizationId(ORG_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000555")))
        .title("Platform engineer")
        .status(status)
        .metadata("{}")
        .createdAt(NOW)
        .updatedAt(NOW)
        .version(1)
        .build();
  }

  private static StoredMatchReport storedMatchReport(UUID jobId) {
    Map<MatchDimension, MatchScore> dimensionScores = new EnumMap<>(MatchDimension.class);
    for (MatchDimension dimension : MatchDimension.values()) {
      dimensionScores.put(dimension, MatchScore.of(4));
    }
    MatchReport matchReport = new MatchReport(
        MatchReportId.of("match_report_123456781234123412341234567890ab"),
        MatchJobRef.of("job_ref_" + jobId.toString().replace("-", "")),
        MatchSubjectRef.of("match_subject_abcdefabcdefabcdefabcdefabcdefab"),
        MatchScore.of(4),
        dimensionScores,
        ScoreConfidence.HIGH,
        new EvidenceCoverage(0.9, EvidenceCoverageLevel.HIGH, 3, 2),
        new ProvenanceSummary(
            ProvenanceCategory.EXTERNAL_VERIFIED,
            ProvenanceSourceStrength.HIGH_TRUST,
            ProvenanceWeight.of(0.9),
            EvidenceAssertionStrength.EXPLICIT,
            AuthenticityRiskLevel.LOW),
        new ScoreCapDecision(
            MatchScore.of(4),
            MatchScore.of(4),
            false,
            ScoreCapReason.NONE,
            "No score cap was applied by the current metadata policy.",
            false,
            false,
            false),
        "ontology-v1",
        "industry-v1",
        NOW);
    return new StoredMatchReport(
        ORG_ID,
        matchReport,
        "candidate",
        UUID.fromString("00000000-0000-0000-0000-000000000333"),
        null,
        ReidentificationRiskSignal.LOW,
        List.of("evidence"),
        List.of("question"));
  }
}
