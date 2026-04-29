package com.recruitingtransactionos.coreapi.company.port;

import com.recruitingtransactionos.coreapi.company.CompanyContact;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import java.util.List;
import java.util.UUID;

public interface CompanyContactPersistencePort {

  CompanyContact create(CompanyContact contact);

  List<CompanyContact> findByCompanyIdAndOrganizationId(UUID organizationId, CompanyId companyId);
}
