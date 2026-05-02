package com.recruitingtransactionos.coreapi.apiboundary.consultant.mapper;

import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanyDetailResponse;
import com.recruitingtransactionos.coreapi.apiboundary.ConsultantCompanySummaryResponse;
import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyContact;
import java.util.List;
import java.util.Objects;

public final class ConsultantCompanyResponseMapper {

  private ConsultantCompanyResponseMapper() {}

  public static ConsultantCompanySummaryResponse toSummary(
      Company company, int contactCount, int jobCount) {
    Objects.requireNonNull(company, "company must not be null");
    return new ConsultantCompanySummaryResponse(
        company.companyId().value().toString(),
        company.name(),
        company.status().wireValue(),
        contactCount,
        jobCount,
        company.createdAt().toString());
  }

  public static ConsultantCompanyDetailResponse toDetail(
      Company company, List<CompanyContact> contacts, int jobCount) {
    Objects.requireNonNull(company, "company must not be null");
    List<ConsultantCompanyDetailResponse.Contact> contactDtos =
        Objects.requireNonNull(contacts, "contacts must not be null").stream()
            .map(ConsultantCompanyResponseMapper::toContactDto)
            .toList();
    return new ConsultantCompanyDetailResponse(
        company.companyId().value().toString(),
        company.version(),
        company.name(),
        company.displayName(),
        company.industry(),
        company.website(),
        company.headquartersLocation(),
        company.sizeBand(),
        company.status().wireValue(),
        company.paymentReliability(),
        company.ownerConsultantId() != null
            ? company.ownerConsultantId().toString() : null,
        company.createdAt().toString(),
        company.updatedAt().toString(),
        contactDtos,
        jobCount);
  }

  private static ConsultantCompanyDetailResponse.Contact toContactDto(CompanyContact contact) {
    return new ConsultantCompanyDetailResponse.Contact(
        contact.companyContactId().value().toString(),
        contact.name(),
        contact.title(),
        contact.email(),
        contact.phone(),
        contact.roleType(),
        contact.isPrimary(),
        contact.status() != null ? contact.status() : "active");
  }
}
