package com.recruitingtransactionos.coreapi.importmigration;

import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportBatchReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportBatchRepository {

  ImportBatchReport save(ImportBatchReport report);

  Optional<ImportBatchReport> find(UUID organizationId, UUID batchId);

  List<ImportBatchReport> savedReports();
}
