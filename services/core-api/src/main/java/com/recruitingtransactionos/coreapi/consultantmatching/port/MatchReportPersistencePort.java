package com.recruitingtransactionos.coreapi.consultantmatching.port;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.consultantmatching.StoredMatchReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchReportPersistencePort {

  StoredMatchReport create(StoredMatchReport storedMatchReport);

  List<StoredMatchReport> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId);

  Optional<StoredMatchReport> findLatestByCandidateIdAndJobId(
      UUID organizationId, JobId jobId, UUID candidateId);

  Optional<StoredMatchReport> findLatestByShortlistCandidateCardIdAndJobId(
      UUID organizationId, JobId jobId, UUID shortlistCandidateCardId);
}
