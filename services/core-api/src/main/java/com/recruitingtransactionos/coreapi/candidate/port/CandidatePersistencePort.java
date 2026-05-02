package com.recruitingtransactionos.coreapi.candidate.port;

import com.recruitingtransactionos.coreapi.candidate.Candidate;
import com.recruitingtransactionos.coreapi.candidate.CandidateStatus;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidatePersistencePort {

  Candidate create(Candidate candidate);

  Optional<Candidate> findByIdAndOrganizationId(UUID organizationId, CandidateId candidateId);

  List<Candidate> findByOrganizationIdAndStatus(UUID organizationId, CandidateStatus status);

  List<Candidate> findAllByOrganizationId(UUID organizationId);
}
