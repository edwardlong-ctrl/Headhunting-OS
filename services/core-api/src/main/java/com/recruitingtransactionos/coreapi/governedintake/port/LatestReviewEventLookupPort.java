package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimId;
import java.util.Optional;
import java.util.UUID;

public interface LatestReviewEventLookupPort {

  Optional<ReviewEventForCanonicalWrite> findLatestByClaimIdAndOrganizationId(
      UUID organizationId,
      ClaimId claimId);
}
