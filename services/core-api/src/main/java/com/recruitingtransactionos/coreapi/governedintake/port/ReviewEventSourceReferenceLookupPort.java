package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.util.Optional;
import java.util.UUID;

public interface ReviewEventSourceReferenceLookupPort {

  Optional<ReviewEventSourceReference> findBySourceSpanReference(
      UUID organizationId,
      SourceSpanRef sourceSpanReference);
}
