package com.recruitingtransactionos.coreapi.recruiting;

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
import com.recruitingtransactionos.coreapi.company.service.CompanyService;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.persistence.JdbcJobScorecardPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobRequirementPersistencePort;
import com.recruitingtransactionos.coreapi.job.port.JobScorecardPersistencePort;
import com.recruitingtransactionos.coreapi.job.service.JobService;
import com.recruitingtransactionos.coreapi.shortlist.persistence.JdbcShortlistCandidateCardPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.persistence.JdbcShortlistPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistCandidateCardPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.port.ShortlistPersistencePort;
import com.recruitingtransactionos.coreapi.shortlist.service.ShortlistService;
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
  @ConditionalOnMissingBean(CandidatePersistencePort.class)
  CandidatePersistencePort candidatePersistencePort(DataSource dataSource) {
    return new JdbcCandidatePersistencePort(dataSource);
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
}
