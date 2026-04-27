package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import java.util.Optional;
import java.util.UUID;

public interface ClaimLedgerItemReviewLookupPort {

  Optional<ClaimLedgerItemForReview> findByIdAndOrganizationId(
      UUID organizationId,
      ClaimId claimLedgerItemId);
}
