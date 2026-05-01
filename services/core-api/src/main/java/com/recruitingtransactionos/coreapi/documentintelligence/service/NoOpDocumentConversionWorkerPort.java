package com.recruitingtransactionos.coreapi.documentintelligence.service;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;

public final class NoOpDocumentConversionWorkerPort implements DocumentConversionWorkerPort {

  @Override
  public DocumentProcessingStatus requestProcessing(SourceItem sourceItem) {
    return DocumentProcessingStatus.PENDING_EXTERNAL_PROCESSING;
  }
}
