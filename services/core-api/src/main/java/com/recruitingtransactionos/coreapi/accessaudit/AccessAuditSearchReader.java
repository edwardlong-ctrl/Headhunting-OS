package com.recruitingtransactionos.coreapi.accessaudit;

import java.util.List;

public interface AccessAuditSearchReader {

  List<AccessAuditRecord> search(AccessAuditSearchQuery query);
}
