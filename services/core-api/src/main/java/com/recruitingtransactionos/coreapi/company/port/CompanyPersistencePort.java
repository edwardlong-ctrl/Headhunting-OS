package com.recruitingtransactionos.coreapi.company.port;

import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyPersistencePort {

  Company create(Company company);

  Optional<Company> findByIdAndOrganizationId(UUID organizationId, CompanyId companyId);

  List<Company> findByOrganizationIdAndStatus(UUID organizationId, CompanyStatus status);

  List<Company> findAllByOrganizationId(UUID organizationId);
}
