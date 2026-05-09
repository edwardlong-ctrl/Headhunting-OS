package com.recruitingtransactionos.coreapi.recruiting;

import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityAwareMatchRequestFactory;
import com.recruitingtransactionos.coreapi.candidatedocument.persistence.JdbcCandidateDocumentPersistencePort;
import com.recruitingtransactionos.coreapi.candidatedocument.port.CandidateDocumentPersistencePort;
import com.recruitingtransactionos.coreapi.candidatedocument.service.CandidateDocumentService;
import com.recruitingtransactionos.coreapi.candidate.persistence.JdbcCandidatePersistencePort;
import com.recruitingtransactionos.coreapi.candidate.port.CandidatePersistencePort;
import com.recruitingtransactionos.coreapi.candidate.service.CandidateService;
import com.recruitingtransactionos.coreapi.candidateprofile.persistence.JdbcCandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.port.CandidateProfilePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.service.CandidateProfileService;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyContactPersistencePort;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyPersistencePort;
import com.recruitingtransactionos.coreapi.company.persistence.JdbcCompanyPreferencePersistencePort;
import com.recruitingtransactionos.coreapi.company.port.CompanyContactPersistencePort;
import com.recruitingtransactionos.coreapi.company.port.CompanyPersistencePort;
import com.recruitingtransactionos.coreapi.company.port.CompanyPreferencePersistencePort;
import com.recruitingtransactionos.coreapi.company.service.CompanyIntakeApplicationService;
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.consentdisclosure.persistence.JdbcClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ClientUnlockRequestPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.interaction.persistence.JdbcCandidateCompanyInteractionPersistencePort;
import com.recruitingtransactionos.coreapi.interaction.port.CandidateCompanyInteractionPersistencePort;
import com.recruitingtransactionos.coreapi.interaction.service.CandidateCompanyInteractionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.persistence.JdbcInterviewFeedbackPersistencePort;
import com.recruitingtransactionos.coreapi.interviewfeedback.persistence.JdbcInterviewFeedbackSuggestionPersistencePort;
import com.recruitingtransactionos.coreapi.interviewfeedback.persistence.JdbcMatchCalibrationSignalPort;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.InterviewFeedbackPersistencePort;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.InterviewFeedbackSuggestionPersistencePort;
import com.recruitingtransactionos.coreapi.interviewfeedback.port.MatchCalibrationSignalPort;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackOutcomeLoopService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackReviewService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.InterviewFeedbackSuggestionService;
import com.recruitingtransactionos.coreapi.interviewfeedback.service.MatchCalibrationSignalService;
import com.recruitingtransactionos.coreapi.consultantmatching.persistence.JdbcMatchReportPersistencePort;
import com.recruitingtransactionos.coreapi.consultantmatching.port.MatchReportPersistencePort;
import com.recruitingtransactionos.coreapi.commission.persistence.JdbcCommissionPersistencePort;
import com.recruitingtransactionos.coreapi.commission.port.CommissionPersistencePort;
import com.recruitingtransactionos.coreapi.commission.service.CommissionService;
import com.recruitingtransactionos.coreapi.commission.service.CommissionWorkflowService;
import com.recruitingtransactionos.coreapi.industrypack.persistence.JdbcIndustryPackReadPort;
import com.recruitingtransactionos.coreapi.industrypack.port.IndustryPackReadPort;
import com.recruitingtransactionos.coreapi.industrypack.service.IndustryPackService;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobScorecardPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobScorecardPersistencePort;
import com.recruitingtransactionos.coreapi.job.service.JobActivationGateService;
import com.recruitingtransactionos.coreapi.job.service.JobIntakeApplicationService;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.interviewfeedback.InterviewFeedbackStructurerTaskService;
import com.recruitingtransactionos.coreapi.matching.MatchReportGenerationService;
import com.recruitingtransactionos.coreapi.notification.NotificationService;
import com.recruitingtransactionos.coreapi.privacyredaction.RedactionAuditService;
import com.recruitingtransactionos.coreapi.placement.persistence.JdbcPlacementPersistencePort;
import com.recruitingtransactionos.coreapi.placement.port.PlacementPersistencePort;
import com.recruitingtransactionos.coreapi.placement.service.PlacementService;
import com.recruitingtransactionos.coreapi.placement.service.PlacementWorkflowService;
import com.recruitingtransactionos.coreapi.shortlist.persistence.JdbcShortlistCandidateCardPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.persistence.JdbcShortlistPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistCandidateCardPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistBuilderService;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowTransitionAuditService;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RecruitingDomainConfiguration {

  @Bean
  @ConditionalOnMissingBean(CompanyPersistencePort.class)
  CompanyPersistencePort companyPersistencePort(DataSource dataSource) {
    return new JdbcCompanyPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CompanyContactPersistencePort.class)
  CompanyContactPersistencePort companyContactPersistencePort(DataSource dataSource) {
    return new JdbcCompanyContactPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CompanyPreferencePersistencePort.class)
  CompanyPreferencePersistencePort companyPreferencePersistencePort(DataSource dataSource) {
    return new JdbcCompanyPreferencePersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(ClientUnlockRequestPort.class)
  ClientUnlockRequestPort clientUnlockRequestPort(DataSource dataSource) {
    return new JdbcClientUnlockRequestPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CompanyService.class)
  CompanyService companyService(
      CompanyPersistencePort companyPort,
      CompanyContactPersistencePort contactPort,
      CompanyPreferencePersistencePort preferencePort) {
    return new CompanyService(companyPort, contactPort, preferencePort);
  }

  @Bean
  @ConditionalOnMissingBean(JobPersistencePort.class)
  JobPersistencePort jobPersistencePort(DataSource dataSource) {
    return new JdbcJobPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(JobRequirementPersistencePort.class)
  JobRequirementPersistencePort jobRequirementPersistencePort(DataSource dataSource) {
    return new JdbcJobRequirementPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(JobScorecardPersistencePort.class)
  JobScorecardPersistencePort jobScorecardPersistencePort(DataSource dataSource) {
    return new JdbcJobScorecardPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(JobService.class)
  JobService jobService(
      JobPersistencePort jobPort,
      JobRequirementPersistencePort requirementPort,
      JobScorecardPersistencePort scorecardPort) {
    return new JobService(jobPort, requirementPort, scorecardPort);
  }

  @Bean
  @ConditionalOnMissingBean(JobActivationGateService.class)
  JobActivationGateService jobActivationGateService() {
    return new JobActivationGateService();
  }

  @Bean
  @ConditionalOnMissingBean(CompanyIntakeApplicationService.class)
  CompanyIntakeApplicationService companyIntakeApplicationService(CompanyService companyService) {
    return new CompanyIntakeApplicationService(companyService);
  }

  @Bean
  @ConditionalOnMissingBean(JobIntakeApplicationService.class)
  JobIntakeApplicationService jobIntakeApplicationService(
      JobService jobService,
      CompanyService companyService,
      JobActivationGateService jobActivationGateService,
      WorkflowTransitionAuditService workflowTransitionAuditService,
      com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowEventService workflowEventService,
      NotificationService notificationService) {
    return new JobIntakeApplicationService(
        jobService,
        companyService,
        jobActivationGateService,
        workflowTransitionAuditService,
        workflowEventService,
        notificationService);
  }

  @Bean
  @ConditionalOnMissingBean(ShortlistPersistencePort.class)
  ShortlistPersistencePort shortlistPersistencePort(DataSource dataSource) {
    return new JdbcShortlistPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(ShortlistCandidateCardPersistencePort.class)
  ShortlistCandidateCardPersistencePort shortlistCandidateCardPersistencePort(
      DataSource dataSource) {
    return new JdbcShortlistCandidateCardPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(ShortlistService.class)
  ShortlistService shortlistService(
      ShortlistPersistencePort shortlistPort,
      ShortlistCandidateCardPersistencePort cardPort) {
    return new ShortlistService(shortlistPort, cardPort);
  }

  @Bean
  @ConditionalOnMissingBean(ShortlistBuilderService.class)
  ShortlistBuilderService shortlistBuilderService(
      ShortlistService shortlistService,
      CandidateService candidateService,
      CandidateProfileService candidateProfileService,
      MatchReportPersistencePort matchReportPersistencePort,
      ConsentRecordPort consentRecordPort,
      JobService jobService,
      WorkflowTransitionAuditService workflowTransitionAuditService,
      RedactionAuditService redactionAuditService) {
    return new ShortlistBuilderService(
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
  }

  @Bean
  @ConditionalOnMissingBean(CandidatePersistencePort.class)
  CandidatePersistencePort candidatePersistencePort(DataSource dataSource) {
    return new JdbcCandidatePersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CandidateCompanyInteractionPersistencePort.class)
  CandidateCompanyInteractionPersistencePort candidateCompanyInteractionPersistencePort(
      DataSource dataSource) {
    return new JdbcCandidateCompanyInteractionPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CandidateCompanyInteractionService.class)
  CandidateCompanyInteractionService candidateCompanyInteractionService(
      CandidateCompanyInteractionPersistencePort interactionPersistencePort) {
    return new CandidateCompanyInteractionService(interactionPersistencePort);
  }

  @Bean
  @ConditionalOnMissingBean(InterviewFeedbackPersistencePort.class)
  InterviewFeedbackPersistencePort interviewFeedbackPersistencePort(DataSource dataSource) {
    return new JdbcInterviewFeedbackPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(InterviewFeedbackService.class)
  InterviewFeedbackService interviewFeedbackService(
      InterviewFeedbackPersistencePort interviewFeedbackPersistencePort) {
    return new InterviewFeedbackService(interviewFeedbackPersistencePort);
  }

  @Bean
  @ConditionalOnMissingBean(InterviewFeedbackSuggestionPersistencePort.class)
  InterviewFeedbackSuggestionPersistencePort interviewFeedbackSuggestionPersistencePort(
      DataSource dataSource) {
    return new JdbcInterviewFeedbackSuggestionPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(InterviewFeedbackSuggestionService.class)
  InterviewFeedbackSuggestionService interviewFeedbackSuggestionService(
      InterviewFeedbackSuggestionPersistencePort interviewFeedbackSuggestionPersistencePort) {
    return new InterviewFeedbackSuggestionService(interviewFeedbackSuggestionPersistencePort);
  }

  @Bean
  @ConditionalOnMissingBean(MatchCalibrationSignalPort.class)
  MatchCalibrationSignalPort matchCalibrationSignalPort(DataSource dataSource) {
    return new JdbcMatchCalibrationSignalPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(MatchCalibrationSignalService.class)
  MatchCalibrationSignalService matchCalibrationSignalService(
      MatchCalibrationSignalPort matchCalibrationSignalPort) {
    return new MatchCalibrationSignalService(matchCalibrationSignalPort);
  }

  @Bean
  @ConditionalOnMissingBean(InterviewFeedbackOutcomeLoopService.class)
  InterviewFeedbackOutcomeLoopService interviewFeedbackOutcomeLoopService(
      InterviewFeedbackService interviewFeedbackService,
      InterviewFeedbackSuggestionService interviewFeedbackSuggestionService,
      MatchCalibrationSignalService matchCalibrationSignalService,
      InterviewFeedbackStructurerTaskService interviewFeedbackStructurerTaskService) {
    return new InterviewFeedbackOutcomeLoopService(
        interviewFeedbackService,
        interviewFeedbackSuggestionService,
        matchCalibrationSignalService,
        interviewFeedbackStructurerTaskService);
  }

  @Bean
  @ConditionalOnMissingBean(InterviewFeedbackReviewService.class)
  InterviewFeedbackReviewService interviewFeedbackReviewService(
      InterviewFeedbackSuggestionService interviewFeedbackSuggestionService,
      CandidateCompanyInteractionService candidateCompanyInteractionService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    return new InterviewFeedbackReviewService(
        interviewFeedbackSuggestionService,
        candidateCompanyInteractionService,
        workflowTransitionAuditService);
  }

  @Bean
  @ConditionalOnMissingBean(CandidateService.class)
  CandidateService candidateService(CandidatePersistencePort candidatePort) {
    return new CandidateService(candidatePort);
  }

  @Bean
  @ConditionalOnMissingBean(CandidateProfilePersistencePort.class)
  CandidateProfilePersistencePort candidateProfilePersistencePort(DataSource dataSource) {
    return new JdbcCandidateProfilePersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CandidateProfileService.class)
  CandidateProfileService candidateProfileService(
      CandidateProfilePersistencePort candidateProfilePersistencePort) {
    return new CandidateProfileService(candidateProfilePersistencePort);
  }

  @Bean
  @ConditionalOnMissingBean(CandidateDocumentPersistencePort.class)
  CandidateDocumentPersistencePort candidateDocumentPersistencePort(DataSource dataSource) {
    return new JdbcCandidateDocumentPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CandidateDocumentService.class)
  CandidateDocumentService candidateDocumentService(
      CandidateDocumentPersistencePort candidateDocumentPersistencePort) {
    return new CandidateDocumentService(candidateDocumentPersistencePort);
  }

  @Bean
  @ConditionalOnMissingBean(MatchReportPersistencePort.class)
  MatchReportPersistencePort matchReportPersistencePort(DataSource dataSource) {
    return new JdbcMatchReportPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(IndustryPackReadPort.class)
  IndustryPackReadPort industryPackReadPort(DataSource dataSource) {
    return new JdbcIndustryPackReadPort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(IndustryPackService.class)
  IndustryPackService industryPackService(IndustryPackReadPort industryPackReadPort) {
    return new IndustryPackService(industryPackReadPort);
  }

  @Bean
  @ConditionalOnMissingBean(PlacementPersistencePort.class)
  PlacementPersistencePort placementPersistencePort(DataSource dataSource) {
    return new JdbcPlacementPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(PlacementService.class)
  PlacementService placementService(PlacementPersistencePort placementPersistencePort) {
    return new PlacementService(placementPersistencePort);
  }

  @Bean
  @ConditionalOnMissingBean(CommissionPersistencePort.class)
  CommissionPersistencePort commissionPersistencePort(DataSource dataSource) {
    return new JdbcCommissionPersistencePort(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(CommissionService.class)
  CommissionService commissionService(CommissionPersistencePort commissionPersistencePort) {
    return new CommissionService(commissionPersistencePort);
  }

  @Bean
  @ConditionalOnMissingBean(CommissionWorkflowService.class)
  CommissionWorkflowService commissionWorkflowService(
      CommissionService commissionService,
      PlacementService placementService,
      JobService jobService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    return new CommissionWorkflowService(
        commissionService,
        placementService,
        jobService,
        workflowTransitionAuditService);
  }

  @Bean
  @ConditionalOnMissingBean(PlacementWorkflowService.class)
  PlacementWorkflowService placementWorkflowService(
      PlacementService placementService,
      JobService jobService,
      CandidateService candidateService,
      CompanyService companyService,
      CommissionWorkflowService commissionWorkflowService,
      WorkflowTransitionAuditService workflowTransitionAuditService) {
    return new PlacementWorkflowService(
        placementService,
        jobService,
        candidateService,
        companyService,
        commissionWorkflowService,
        workflowTransitionAuditService);
  }

  @Bean
  @ConditionalOnMissingBean(MatchReportGenerationService.class)
  MatchReportGenerationService matchReportGenerationService() {
    return new MatchReportGenerationService();
  }

  @Bean
  @ConditionalOnMissingBean(AuthenticityAwareMatchRequestFactory.class)
  AuthenticityAwareMatchRequestFactory authenticityAwareMatchRequestFactory() {
    return new AuthenticityAwareMatchRequestFactory();
  }
}
