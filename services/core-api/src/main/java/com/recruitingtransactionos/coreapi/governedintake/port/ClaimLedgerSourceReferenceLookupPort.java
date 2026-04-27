package com.recruitingtransactionos.coreapi.governedintake.port;

import com.recruitingtransactionos.coreapi.truthlayer.port.SourceSpanRef;
import java.util.Optional;
import java.util.UUID;

public interface ClaimLedgerSourceReferenceLookupPort {

  Optional<ClaimLedgerSourceReference> findBySourceSpanReference(
      UUID organizationId,
      SourceSpanRef sourceSpanReference);
}
