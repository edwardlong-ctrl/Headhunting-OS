package com.recruitingtransactionos.coreapi.documentintelligence.service;

import com.recruitingtransactionos.coreapi.documentintelligence.DocumentProcessingStatus;
import com.recruitingtransactionos.coreapi.governedintake.SourceItem;

public interface DocumentConversionWorkerPort {

  DocumentProcessingStatus requestProcessing(SourceItem sourceItem);
}
