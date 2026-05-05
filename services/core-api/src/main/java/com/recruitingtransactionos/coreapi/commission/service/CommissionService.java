package com.recruitingtransactionos.coreapi.commission.service;

import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.port.CommissionPersistencePort;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CommissionService {

  private final CommissionPersistencePort commissionPort;

  public CommissionService(CommissionPersistencePort commissionPort) {
    this.commissionPort = Objects.requireNonNull(
        commissionPort, "commissionPort must not be null");
  }

  public Commission createCommission(Commission commission) {
    Objects.requireNonNull(commission, "commission must not be null");
    return commissionPort.create(commission);
  }

  public Commission updateCommission(Commission commission) {
    Objects.requireNonNull(commission, "commission must not be null");
    return commissionPort.update(commission);
  }

  public Optional<Commission> findCommissionByIdAndOrganizationId(
      UUID organizationId, CommissionId commissionId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(commissionId, "commissionId must not be null");
    return commissionPort.findByIdAndOrganizationId(organizationId, commissionId);
  }

  public List<Commission> findAllCommissionsByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    return commissionPort.findAllByOrganizationId(organizationId);
  }

  public List<Commission> findCommissionsByPlacementIdAndOrganizationId(
      UUID organizationId, PlacementId placementId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(placementId, "placementId must not be null");
    return commissionPort.findByPlacementIdAndOrganizationId(organizationId, placementId);
  }

  public List<Commission> findCommissionsByConsultantIdAndOrganizationId(
      UUID organizationId, UUID consultantId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(consultantId, "consultantId must not be null");
    return commissionPort.findByConsultantIdAndOrganizationId(organizationId, consultantId);
  }
}
