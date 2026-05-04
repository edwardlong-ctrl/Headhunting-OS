package com.recruitingtransactionos.coreapi.consentdisclosure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfile;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateProfileVersion;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidateConsentWorkflowServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000331001");
  private static final UUID CANDIDATE_ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000331002");
  private static final CandidateId CANDIDATE_ID =
      new CandidateId(UUID.fromString("00000000-0000-0000-0000-000000331003"));
  private static final String CANDIDATE_REF = CANDIDATE_ID.value().toString();
  private static final UUID PROFILE_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000331004");
  private static final String PROFILE_REF = PROFILE_UUID.toString();
  private static final UUID JOB_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000331005");
  private static final String JOB_REF = JOB_UUID.toString();
  private static final Instant NOW = Instant.parse("2026-05-04T00:00:00Z");

  @Mock private ConsentRecordPort consentRecordPort;
  @Mock private CandidateProfileService candidateProfileService;
  @Mock private JobService jobService;
  @Mock private WorkflowTransitionAuditService workflowTransitionAuditService;

  private CandidateConsentWorkflowService service;

  @BeforeEach
  void setUp() {
    service = new CandidateConsentWorkflowService(
        consentRecordPort,
        candidateProfileService,
        jobService,
        workflowTransitionAuditService);
  }

  @Test
  void latestConsentSnapshotDoesNotAppendViewedByCandidateState() {
    when(consentRecordPort.findLatestByCandidateProfileAndJob(
        eq(ORGANIZATION_ID),
        eq(CANDIDATE_REF),
        eq(PROFILE_REF),
        eq(JOB_REF)))
        .thenReturn(Optional.of(consentRecord("7", ConsentStatus.REQUESTED)));
    when(candidateProfileService.findCandidateProfileByIdAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(new CandidateProfileId(PROFILE_UUID))))
        .thenReturn(Optional.of(candidateProfile(7)));
    when(jobService.findJobByIdAndOrganizationId(eq(ORGANIZATION_ID), eq(new JobId(JOB_UUID))))
        .thenReturn(Optional.of(job()));

    CandidateConsentWorkflowService.CandidateConsentView view = service.latestConsentSnapshot(
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF);

    assertThat(view.consentRecord().status()).isEqualTo(ConsentStatus.REQUESTED);
    verify(consentRecordPort, never()).append(any());
    verify(workflowTransitionAuditService, never()).record(any());
  }

  @Test
  void respondToConsentFailsClosedWhenProfileVersionChangedSinceRequest() {
    when(consentRecordPort.findLatestByCandidateProfileAndJob(
        eq(ORGANIZATION_ID),
        eq(CANDIDATE_REF),
        eq(PROFILE_REF),
        eq(JOB_REF)))
        .thenReturn(Optional.of(consentRecord("7", ConsentStatus.REQUESTED)));
    when(candidateProfileService.findCandidateProfileByIdAndOrganizationId(
        eq(ORGANIZATION_ID),
        eq(new CandidateProfileId(PROFILE_UUID))))
        .thenReturn(Optional.of(candidateProfile(8)));

    assertThatThrownBy(() -> service.respondToConsent(
        ORGANIZATION_ID,
        CANDIDATE_ACTOR_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("consent_profile_version_mismatch");

    verify(consentRecordPort, never()).append(any());
    verify(workflowTransitionAuditService, never()).record(any());
  }

  private static ConsentRecord consentRecord(String profileVersion, ConsentStatus status) {
    return new ConsentRecord(
        "consent-task33-3310",
        ORGANIZATION_ID,
        CANDIDATE_REF,
        PROFILE_REF,
        JOB_REF,
        profileVersion,
        "task33-v1",
        status,
        Set.of(DisclosureLevel.L3_CONSENTED_DETAIL, DisclosureLevel.L4_IDENTITY_DISCLOSED),
        NOW.minusSeconds(300),
        NOW.plusSeconds(60L * 60L * 24L * 30L),
        false);
  }

  private static CandidateProfile candidateProfile(int profileVersion) {
    return CandidateProfile.builder()
        .candidateProfileId(new CandidateProfileId(PROFILE_UUID))
        .organizationId(ORGANIZATION_ID)
        .candidateId(CANDIDATE_ID)
        .profileVersion(new CandidateProfileVersion(profileVersion))
        .fields(List.of())
        .createdAt(NOW.minusSeconds(3600))
        .updatedAt(NOW.minusSeconds(60))
        .build();
  }

  private static Job job() {
    return Job.builder()
        .jobId(new JobId(JOB_UUID))
        .organizationId(ORGANIZATION_ID)
        .companyId(new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000331006")))
        .title("Principal Verification Engineer")
        .status(JobStatus.ACTIVATED)
        .createdAt(NOW.minusSeconds(3600))
        .updatedAt(NOW.minusSeconds(60))
        .build();
  }
}
