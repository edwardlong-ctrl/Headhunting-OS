package com.recruitingtransactionos.coreapi.truthlayer.port;

import java.util.List;

public interface CanonicalWriteAttemptReadPort {

  List<CanonicalWriteAttemptRecord> search(CanonicalWriteAttemptQuery query);
}
