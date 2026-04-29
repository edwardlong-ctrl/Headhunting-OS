package com.recruitingtransactionos.coreapi.candidatedocument.port;

import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocument;
import com.recruitingtransactionos.coreapi.candidatedocument.CandidateDocumentId;
import com.recruitingtransactionos.coreapi.candidateprofile.CandidateId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateDocumentPersistencePort {

  CandidateDocument create(CandidateDocument document);

  Optional<CandidateDocument> findByIdAndOrganizationId(
      UUID organizationId, CandidateDocumentId documentId);

  List<CandidateDocument> findByCandidateIdAndOrganizationId(
      UUID organizationId, CandidateId candidateId);
}
