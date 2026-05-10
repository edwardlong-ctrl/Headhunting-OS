package com.recruitingtransactionos.coreapi.importmigration;

import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.DocumentImportDraft;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.GovernedImportLineage;
import com.recruitingtransactionos.coreapi.importmigration.ImportMigrationModels.ImportDraftRecord;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import java.util.UUID;

public interface GovernedImportGateway {

  GovernedImportLineage createDraft(
      UUID batchId,
      UUID organizationId,
      UUID actorId,
      ActorRole actorRole,
      ImportDraftRecord draft);

  GovernedImportLineage createDocument(
      UUID batchId,
      UUID organizationId,
      UUID actorId,
      ActorRole actorRole,
      DocumentImportDraft document);

  void rollback(UUID organizationId, UUID batchId, GovernedImportLineage lineage);
}
