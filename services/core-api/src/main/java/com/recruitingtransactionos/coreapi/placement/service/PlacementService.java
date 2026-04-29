package com.recruitingtransactionos.coreapi.placement.service;

import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.placement.Placement;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import com.recruitingtransactionos.coreapi.placement.port.PlacementPersistencePort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlacementService {

  private final PlacementPersistencePort placementPort;

  public PlacementService(PlacementPersistencePort placementPort) {
    this.placementPort = Objects.requireNonNull(placementPort, "placementPort must not be null");
  }

  public Placement createPlacement(Placement placement) {
    Objects.requireNonNull(placement, "placement must not be null");
    return placementPort.create(placement);
  }

  public Optional<Placement> findPlacementByIdAndOrganizationId(
      UUID organizationId, PlacementId placementId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(placementId, "placementId must not be null");
    return placementPort.findByIdAndOrganizationId(organizationId, placementId);
  }

  public List<Placement> findPlacementsByJobIdAndOrganizationId(
      UUID organizationId, JobId jobId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(jobId, "jobId must not be null");
    return placementPort.findByJobIdAndOrganizationId(organizationId, jobId);
  }

  public List<Placement> findPlacementsByCandidateIdAndOrganizationId(
      UUID organizationId, CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    return placementPort.findByCandidateIdAndOrganizationId(organizationId, candidateId);
  }
}
