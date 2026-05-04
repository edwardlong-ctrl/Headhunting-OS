package com.recruitingtransactionos.coreapi.interaction.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteraction;
import com.recruitingtransactionos.coreapi.interaction.CandidateCompanyInteractionId;
import com.recruitingtransactionos.coreapi.interaction.port.CandidateCompanyInteractionPersistencePort;
import com.recruitingtransactionos.coreapi.job.JobId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CandidateCompanyInteractionService {

  private final CandidateCompanyInteractionPersistencePort interactionPort;

  public CandidateCompanyInteractionService(
      CandidateCompanyInteractionPersistencePort interactionPort) {
    this.interactionPort = Objects.requireNonNull(
        interactionPort, "interactionPort must not be null");
  }

  public CandidateCompanyInteraction createInteraction(CandidateCompanyInteraction interaction) {
    Objects.requireNonNull(interaction, "interaction must not be null");
    return interactionPort.create(interaction);
  }

  public CandidateCompanyInteraction updateInteraction(CandidateCompanyInteraction interaction) {
    Objects.requireNonNull(interaction, "interaction must not be null");
    return interactionPort.update(interaction);
  }

  public Optional<CandidateCompanyInteraction> findInteractionByIdAndOrganizationId(
      UUID organizationId, CandidateCompanyInteractionId interactionId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(interactionId, "interactionId must not be null");
    return interactionPort.findByIdAndOrganizationId(organizationId, interactionId);
  }

  public List<CandidateCompanyInteraction> findInteractionsByCandidateAndCompany(
      UUID organizationId, CandidateId candidateId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    return interactionPort.findByCandidateAndCompany(organizationId, candidateId, companyId);
  }

  public List<CandidateCompanyInteraction> findInteractionsByCandidateId(
      UUID organizationId, CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    return interactionPort.findByCandidateId(organizationId, candidateId);
  }

  public List<CandidateCompanyInteraction> findInteractionsByJobId(
      UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    return interactionPort.findByJobId(organizationId, jobId);
  }
}
