package com.recruitingtransactionos.coreapi.apiboundary.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.service.CompanyIntakeApplicationService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.identityaccess.AccessAction;
import com.recruitingtransactionos.coreapi.identityaccess.AccessDecision;
import com.recruitingtransactionos.coreapi.identityaccess.AccessRequest;
import com.recruitingtransactionos.coreapi.identityaccess.FieldClassification;
import com.recruitingtransactionos.coreapi.identityaccess.PermissionEnforcer;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import com.recruitingtransactionos.coreapi.identityaccess.RelationshipScope;
import com.recruitingtransactionos.coreapi.identityaccess.ResourceType;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackOutcomeLoopService;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCard;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistCandidateCardStatus;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistStatus;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ClientApiCommandServiceTransactionalContractTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-00000032d001");
  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-00000032d002");
  private static final UUID CONSULTANT_ID = UUID.fromString("00000000-0000-0000-0000-00000032d003");
  private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("00000000-0000-0000-0000-00000032d004"));
  private static final JobId JOB_ID = new JobId(UUID.fromString("00000000-0000-0000-0000-00000032d005"));
  private static final ShortlistId SHORTLIST_ID = new ShortlistId(UUID.fromString("00000000-0000-0000-0000-00000032d006"));
  private static final ShortlistCandidateCardId CARD_ID = new ShortlistCandidateCardId(UUID.fromString("00000000-0000-0000-0000-00000032d007"));
  private static final UUID ANONYMOUS_CARD_ID = UUID.fromString("00000000-0000-0000-0000-00000032d008");
  private static final CandidateId CANDIDATE_ID = new CandidateId(UUID.fromString("00000000-0000-0000-0000-00000032d009"));
  private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-00000032d00a");
  private static final Instant CREATED_AT = Instant.parse("2026-05-04T02:10:00Z");

  @Test
  void clientApiCommandService_isCreatedAsTransactionalProxy() {
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(TransactionalTestConfig.class)) {
      ClientApiCommandService service = context.getBean(ClientApiCommandService.class);

      assertThat(AopUtils.isAopProxy(service)).isTrue();
      assertThat(AopUtils.isCglibProxy(service)).isTrue();
    }
  }

  @Test
  void clientApiCommandService_rollsBackTransactionWhenSelectCandidateFailsMidFlight() {
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(TransactionalTestConfig.class)) {
      ClientApiCommandService service = context.getBean(ClientApiCommandService.class);
      RecordingTransactionManager transactionManager = context.getBean(RecordingTransactionManager.class);
      JobService jobService = context.getBean(JobService.class);
      ShortlistService shortlistService = context.getBean(ShortlistService.class);
      PermissionEnforcer permissionEnforcer = context.getBean(PermissionEnforcer.class);

      when(permissionEnforcer.requireAllowed(any()))
          .thenReturn(new AccessDecision(true, "allowed_for_test", "Allowed for transactional contract test."));

      Job visibleJob = visibleJob();
      Shortlist sent = shortlist(ShortlistStatus.SENT_TO_CLIENT, CREATED_AT.plusSeconds(60), null, 1);
      Shortlist viewed = shortlist(ShortlistStatus.CLIENT_VIEWED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 2);
      Shortlist selected = shortlist(ShortlistStatus.CANDIDATE_SELECTED, CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120), 3);
      ShortlistCandidateCard includedCard = shortlistCard(ShortlistCandidateCardStatus.INCLUDED);

      when(jobService.findAllJobsByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(visibleJob));
      when(shortlistService.findShortlistByIdAndOrganizationId(ORGANIZATION_ID, SHORTLIST_ID))
          .thenReturn(Optional.of(sent));
      when(shortlistService.findCardByIdAndOrganizationId(ORGANIZATION_ID, CARD_ID))
          .thenReturn(Optional.of(includedCard));
      when(shortlistService.updateShortlist(any())).thenReturn(viewed, selected);
      when(shortlistService.updateCandidateCard(any())).thenAnswer(invocation -> {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        throw new IllegalStateException("simulated_card_update_failure");
      });

      AccessRequest accessRequest = new AccessRequest(
          PortalRole.CLIENT,
          ResourceType.SHORTLIST,
          AccessAction.UPDATE,
          FieldClassification.CLIENT_SAFE,
          Set.of(RelationshipScope.SAME_ORGANIZATION, RelationshipScope.SELF),
          false);

      assertThatThrownBy(() -> service.selectShortlistCandidate(
          accessRequest,
          ORGANIZATION_ID,
          ACTOR_ID,
          SHORTLIST_ID,
          CARD_ID))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("simulated_card_update_failure");

      assertThat(transactionManager.beginCount()).isEqualTo(1);
      assertThat(transactionManager.commitCount()).isZero();
      assertThat(transactionManager.rollbackCount()).isEqualTo(1);
    }
  }

  private static Job visibleJob() {
    return Job.builder()
        .jobId(JOB_ID)
        .organizationId(ORGANIZATION_ID)
        .companyId(COMPANY_ID)
        .title("Principal DV Engineer")
        .status(JobStatus.ACTIVATED)
        .ownerConsultantId(CONSULTANT_ID)
        .metadata("{\"clientActorId\":\"" + ACTOR_ID + "\"}")
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT)
        .version(1)
        .build();
  }

  private static Shortlist shortlist(ShortlistStatus status, Instant sentAt, Instant clientViewedAt, int version) {
    return Shortlist.builder()
        .shortlistId(SHORTLIST_ID)
        .organizationId(ORGANIZATION_ID)
        .jobId(JOB_ID)
        .title("DV shortlist")
        .status(status)
        .sentAt(sentAt)
        .clientViewedAt(clientViewedAt)
        .ownerConsultantId(CONSULTANT_ID)
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT.plusSeconds(version))
        .version(version)
        .build();
  }

  private static ShortlistCandidateCard shortlistCard(ShortlistCandidateCardStatus status) {
    return ShortlistCandidateCard.builder()
        .shortlistCandidateCardId(CARD_ID)
        .organizationId(ORGANIZATION_ID)
        .shortlistId(SHORTLIST_ID)
        .anonymousCandidateCardId(ANONYMOUS_CARD_ID)
        .candidateId(CANDIDATE_ID)
        .candidateProfileId(PROFILE_ID)
        .sortOrder(1)
        .status(status)
        .matchReportId(UUID.fromString("00000000-0000-0000-0000-00000032d00b"))
        .clientNotes("internal note")
        .metadata("{}")
        .createdAt(CREATED_AT)
        .updatedAt(CREATED_AT)
        .version(1)
        .build();
  }

  @Configuration
  @EnableTransactionManagement(proxyTargetClass = true)
  static class TransactionalTestConfig {

    @Bean
    RecordingTransactionManager transactionManager() {
      return new RecordingTransactionManager();
    }

    @Bean
    CompanyIntakeApplicationService companyIntakeApplicationService() {
      return mock(CompanyIntakeApplicationService.class);
    }

    @Bean
    JobIntakeApplicationService jobIntakeApplicationService() {
      return mock(JobIntakeApplicationService.class);
    }

    @Bean
    CompanyService companyService() {
      return mock(CompanyService.class);
    }

    @Bean
    JobService jobService() {
      return mock(JobService.class);
    }

    @Bean
    ShortlistService shortlistService() {
      return mock(ShortlistService.class);
    }

    @Bean
    ClientUnlockRequestPort clientUnlockRequestPort() {
      return mock(ClientUnlockRequestPort.class);
    }

    @Bean
    CandidateCompanyInteractionService interactionService() {
      return mock(CandidateCompanyInteractionService.class);
    }

    @Bean
    InterviewFeedbackService interviewFeedbackService() {
      return mock(InterviewFeedbackService.class);
    }

    @Bean
    InterviewFeedbackOutcomeLoopService interviewFeedbackOutcomeLoopService() {
      return mock(InterviewFeedbackOutcomeLoopService.class);
    }

    @Bean
    WorkflowTransitionAuditService workflowTransitionAuditService() {
      return mock(WorkflowTransitionAuditService.class);
    }

    @Bean
    PermissionEnforcer permissionEnforcer() {
      return mock(PermissionEnforcer.class);
    }

    @Bean
    com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService unlockWorkflowService() {
      return mock(com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService.class);
    }

    @Bean
    ClientApiCommandService clientApiCommandService(
        CompanyIntakeApplicationService companyIntakeApplicationService,
        JobIntakeApplicationService jobIntakeApplicationService,
        CompanyService companyService,
        JobService jobService,
        ShortlistService shortlistService,
        ClientUnlockRequestPort clientUnlockRequestPort,
        com.recruitingtransactionos.coreapi.consentdisclosure.UnlockWorkflowService unlockWorkflowService,
        CandidateCompanyInteractionService interactionService,
        InterviewFeedbackService interviewFeedbackService,
        InterviewFeedbackOutcomeLoopService interviewFeedbackOutcomeLoopService,
        WorkflowTransitionAuditService workflowTransitionAuditService,
        PermissionEnforcer permissionEnforcer) {
      return new ClientApiCommandService(
          companyIntakeApplicationService,
          jobIntakeApplicationService,
          companyService,
          jobService,
          shortlistService,
          clientUnlockRequestPort,
          unlockWorkflowService,
          interactionService,
          interviewFeedbackService,
          interviewFeedbackOutcomeLoopService,
          workflowTransitionAuditService,
          permissionEnforcer);
    }
  }

  static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

    private final AtomicInteger beginCount = new AtomicInteger();
    private final AtomicInteger commitCount = new AtomicInteger();
    private final AtomicInteger rollbackCount = new AtomicInteger();

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
      beginCount.incrementAndGet();
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
      commitCount.incrementAndGet();
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
      rollbackCount.incrementAndGet();
    }

    int beginCount() {
      return beginCount.get();
    }

    int commitCount() {
      return commitCount.get();
    }

    int rollbackCount() {
      return rollbackCount.get();
    }
  }
}
