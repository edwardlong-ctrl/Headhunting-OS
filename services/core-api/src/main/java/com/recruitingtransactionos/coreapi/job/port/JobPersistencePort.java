package com.recruitingtransactionos.coreapi.job.port;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.job.Job;
import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.job.JobStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPersistencePort {

  Job create(Job job);

  Optional<Job> findByIdAndOrganizationId(UUID organizationId, JobId jobId);

  List<Job> findByOrganizationIdAndStatus(UUID organizationId, JobStatus status);

  List<Job> findByCompanyIdAndOrganizationId(UUID organizationId, CompanyId companyId);
}
