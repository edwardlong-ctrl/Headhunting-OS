package com.recruitingtransactionos.coreapi.company.port;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import java.util.List;
import java.util.UUID;

public interface CompanyPreferencePersistencePort {

  CompanyPreference upsert(CompanyPreference preference);

  List<CompanyPreference> findByCompanyIdAndOrganizationId(
      UUID organizationId, CompanyId companyId);
}
