package com.recruitingtransactionos.coreapi.commission.port;

import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommissionPersistencePort {

  Commission create(Commission commission);

  Commission update(Commission commission);

  Optional<Commission> findByIdAndOrganizationId(
      UUID organizationId, CommissionId commissionId);

  List<Commission> findAllByOrganizationId(UUID organizationId);

  List<Commission> findByPlacementIdAndOrganizationId(
      UUID organizationId, PlacementId placementId);

  List<Commission> findByConsultantIdAndOrganizationId(
      UUID organizationId, UUID consultantId);
}
