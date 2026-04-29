package com.recruitingtransactionos.coreapi.placement.port;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlacementPersistencePort {

  Placement create(Placement placement);

  Optional<Placement> findByIdAndOrganizationId(UUID organizationId, PlacementId placementId);

  List<Placement> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId);

  List<Placement> findByCandidateIdAndOrganizationId(
      UUID organizationId, CandidateId candidateId);
}
