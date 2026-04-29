package com.recruitingtransactionos.coreapi.job.port;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobScorecard;
import java.util.Optional;
import java.util.UUID;

public interface JobScorecardPersistencePort {

  JobScorecard create(JobScorecard scorecard);

  Optional<JobScorecard> findActiveByJobIdAndOrganizationId(UUID organizationId, JobId jobId);
}
