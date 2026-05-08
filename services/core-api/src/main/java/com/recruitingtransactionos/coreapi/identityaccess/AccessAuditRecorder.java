package com.recruitingtransactionos.coreapi.identityaccess;

import java.util.Objects;

public interface AccessAuditRecorder {

  void record(AccessAuditEvent event);

  static AccessAuditRecorder noop() {
    return event -> Objects.requireNonNull(event, "event must not be null");
  }
}
