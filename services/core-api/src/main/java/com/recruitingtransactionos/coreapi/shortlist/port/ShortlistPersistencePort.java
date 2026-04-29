package com.recruitingtransactionos.coreapi.shortlist.port;

import com.recruitingtransactionos.coreapi.job.JobId;
import com.recruitingtransactionos.coreapi.shortlist.Shortlist;
import com.recruitingtransactionos.coreapi.shortlist.ShortlistId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShortlistPersistencePort {

  Shortlist create(Shortlist shortlist);

  Optional<Shortlist> findByIdAndOrganizationId(UUID organizationId, ShortlistId shortlistId);

  List<Shortlist> findByJobIdAndOrganizationId(UUID organizationId, JobId jobId);
}
