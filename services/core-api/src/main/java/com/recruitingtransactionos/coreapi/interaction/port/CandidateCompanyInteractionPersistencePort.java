package com.recruitingtransactionos.coreapi.interaction.port;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateCompanyInteractionPersistencePort {

  CandidateCompanyInteraction create(CandidateCompanyInteraction interaction);

  Optional<CandidateCompanyInteraction> findByIdAndOrganizationId(
      UUID organizationId, CandidateCompanyInteractionId interactionId);

  List<CandidateCompanyInteraction> findByCandidateAndCompany(
      UUID organizationId, CandidateId candidateId, CompanyId companyId);

  List<CandidateCompanyInteraction> findByCandidateId(
      UUID organizationId, CandidateId candidateId);

  List<CandidateCompanyInteraction> findByJobId(UUID organizationId, JobId jobId);
}
