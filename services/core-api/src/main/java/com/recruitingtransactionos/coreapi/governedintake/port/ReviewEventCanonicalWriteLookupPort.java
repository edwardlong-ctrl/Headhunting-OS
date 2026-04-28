package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.port.ReviewEventId;
import java.util.Optional;
import java.util.UUID;

public interface ReviewEventCanonicalWriteLookupPort {

  Optional<ReviewEventForCanonicalWrite> findByIdAndOrganizationId(
      UUID organizationId,
      ReviewEventId reviewEventId);
}
