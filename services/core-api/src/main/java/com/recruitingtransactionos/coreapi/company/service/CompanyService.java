package com.recruitingtransactionos.coreapi.company.service;

import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyContact;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.port.CompanyContactPersistencePort;
import com.recruitingtransactionos.coreapi.company.port.CompanyPersistencePort;
import com.recruitingtransactionos.coreapi.company.port.CompanyPreferencePersistencePort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CompanyService {

  private final CompanyPersistencePort companyPort;
  private final CompanyContactPersistencePort contactPort;
  private final CompanyPreferencePersistencePort preferencePort;

  public CompanyService(
      CompanyPersistencePort companyPort,
      CompanyContactPersistencePort contactPort,
      CompanyPreferencePersistencePort preferencePort) {
    this.companyPort = Objects.requireNonNull(companyPort, "companyPort must not be null");
    this.contactPort = Objects.requireNonNull(contactPort, "contactPort must not be null");
    this.preferencePort = Objects.requireNonNull(
        preferencePort, "preferencePort must not be null");
  }

  public Company createCompany(Company company) {
    Objects.requireNonNull(company, "company must not be null");
    return companyPort.create(company);
  }

  public Optional<Company> findCompanyByIdAndOrganizationId(
      UUID organizationId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    return companyPort.findByIdAndOrganizationId(organizationId, companyId);
  }

  public List<Company> findCompaniesByOrganizationIdAndStatus(
      UUID organizationId, CompanyStatus status) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    return companyPort.findByOrganizationIdAndStatus(organizationId, status);
  }

  public List<Company> findAllCompaniesByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    return companyPort.findAllByOrganizationId(organizationId);
  }

  public CompanyContact createContact(CompanyContact contact) {
    Objects.requireNonNull(contact, "contact must not be null");
    return contactPort.create(contact);
  }

  public List<CompanyContact> findContactsByCompanyIdAndOrganizationId(
      UUID organizationId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    return contactPort.findByCompanyIdAndOrganizationId(organizationId, companyId);
  }

  public CompanyPreference upsertPreference(CompanyPreference preference) {
    Objects.requireNonNull(preference, "preference must not be null");
    return preferencePort.upsert(preference);
  }

  public List<CompanyPreference> findPreferencesByCompanyIdAndOrganizationId(
      UUID organizationId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    return preferencePort.findByCompanyIdAndOrganizationId(organizationId, companyId);
  }
}
