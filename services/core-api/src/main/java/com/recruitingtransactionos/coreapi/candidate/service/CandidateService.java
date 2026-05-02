package com.recruitingtransactionos.coreapi.candidate.service;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidate.port.CandidatePersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CandidateService {

  private final CandidatePersistencePort candidatePort;

  public CandidateService(CandidatePersistencePort candidatePort) {
    this.candidatePort = Objects.requireNonNull(candidatePort, "candidatePort must not be null");
  }

  public Candidate createCandidate(Candidate candidate) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    return candidatePort.create(candidate);
  }

  public Optional<Candidate> findCandidateByIdAndOrganizationId(
      UUID organizationId, CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    return candidatePort.findByIdAndOrganizationId(organizationId, candidateId);
  }

  public List<Candidate> findCandidatesByOrganizationIdAndStatus(
      UUID organizationId, CandidateStatus status) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    return candidatePort.findByOrganizationIdAndStatus(organizationId, status);
  }

  public List<Candidate> findAllCandidatesByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    return candidatePort.findAllByOrganizationId(organizationId);
  }
}
