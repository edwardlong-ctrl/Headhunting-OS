package com.recruitingtransactionos.coreapi.candidatedocument.service;

import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentId;
import com.recruitingtransactionos.coreapi.candidatedocument.port.CandidateDocumentPersistencePort;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CandidateDocumentService {

  private final CandidateDocumentPersistencePort documentPort;

  public CandidateDocumentService(CandidateDocumentPersistencePort documentPort) {
    this.documentPort = Objects.requireNonNull(documentPort, "documentPort must not be null");
  }

  public CandidateDocument createDocument(CandidateDocument document) {
    Objects.requireNonNull(document, "document must not be null");
    return documentPort.create(document);
  }

  public Optional<CandidateDocument> findDocumentByIdAndOrganizationId(
      UUID organizationId, CandidateDocumentId documentId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    return documentPort.findByIdAndOrganizationId(organizationId, documentId);
  }

  public List<CandidateDocument> findDocumentsByCandidateIdAndOrganizationId(
      UUID organizationId, CandidateId candidateId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    return documentPort.findByCandidateIdAndOrganizationId(organizationId, candidateId);
  }
}
