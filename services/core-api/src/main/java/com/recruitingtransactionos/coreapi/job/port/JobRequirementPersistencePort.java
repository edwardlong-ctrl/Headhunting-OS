package com.recruitingtransactionos.coreapi.job.port;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobRequirement;
import java.util.List;
import java.util.UUID;

public interface JobRequirementPersistencePort {

  JobRequirement create(JobRequirement requirement);

  List<JobRequirement> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId);
}
