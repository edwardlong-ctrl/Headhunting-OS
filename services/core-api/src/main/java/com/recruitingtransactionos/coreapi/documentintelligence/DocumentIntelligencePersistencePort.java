package com.recruitingtransactionos.coreapi.documentintelligence;

import com.recruitingtransactionos.coreapi.governedintake.SourceItemId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentIntelligencePersistencePort {

  ParsedDocument saveParsedDocument(ParsedDocument parsedDocument);

  ParsedDocument saveParsedDocumentWithArtifacts(
      ParsedDocument parsedDocument,
      List<ParsedDocumentChunk> chunks,
      List<ParsedDocumentSpan> spans);

  void replaceChunksAndSpans(
      UUID organizationId,
      UUID parsedDocumentId,
      List<ParsedDocumentChunk> chunks,
      List<ParsedDocumentSpan> spans);

  Optional<ParsedDocument> findLatestParsedDocumentBySourceItem(
      UUID organizationId,
      SourceItemId sourceItemId);

  List<ParsedDocumentChunk> listChunksByParsedDocument(UUID organizationId, UUID parsedDocumentId);

  List<ParsedDocumentSpan> listSpansByParsedDocument(UUID organizationId, UUID parsedDocumentId);
}
