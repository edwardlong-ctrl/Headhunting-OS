package com.recruitingtransactionos.coreapi.apiboundary;

import java.util.List;

public record ConsultantCompanyDetailResponse(
    String companyId,
    long version,
    String name,
    String displayName,
    String industry,
    String website,
    String headquartersLocation,
    String sizeBand,
    String status,
    String paymentReliability,
    String ownerConsultantId,
    String createdAt,
    String updatedAt,
    List<Contact> contacts,
    int jobCount) implements ApiSafeResponseBody {

  public ConsultantCompanyDetailResponse {
    companyId = ApiBoundaryContractRules.requireNonBlank(companyId, "companyId");
    if (version < 0) {
      throw new IllegalArgumentException("version must be >= 0");
    }
    name = ApiBoundaryContractRules.requireBusinessVisibleText(name, "name");
    displayName = ApiBoundaryContractRules.sanitizeBusinessVisibleText(displayName, null);
    industry = ApiBoundaryContractRules.sanitizeBusinessVisibleText(industry, null);
    website = ApiBoundaryContractRules.sanitizeBusinessVisibleText(website, null);
    headquartersLocation =
        ApiBoundaryContractRules.sanitizeBusinessVisibleText(headquartersLocation, null);
    sizeBand = ApiBoundaryContractRules.sanitizeBusinessVisibleText(sizeBand, null);
    status = ApiBoundaryContractRules.requireNonBlank(status, "status");
    paymentReliability =
        ApiBoundaryContractRules.sanitizeBusinessVisibleText(paymentReliability, null);
    createdAt = ApiBoundaryContractRules.requireNonBlank(createdAt, "createdAt");
    updatedAt = ApiBoundaryContractRules.requireNonBlank(updatedAt, "updatedAt");
    contacts = ApiBoundaryContractRules.requireNonNullList(contacts, "contacts");
    if (jobCount < 0) {
      throw new IllegalArgumentException("jobCount must be >= 0");
    }
  }

  public record Contact(
      String contactId,
      String name,
      String title,
      String email,
      String phone,
      String roleType,
      boolean isPrimary,
      String status) {

    public Contact {
      contactId = ApiBoundaryContractRules.requireNonBlank(contactId, "contactId");
      name = ApiBoundaryContractRules.requireNonBlank(name, "name");
      title = ApiBoundaryContractRules.sanitizeConsultantVisibleText(title, null);
      email = ApiBoundaryContractRules.sanitizeConsultantVisibleText(email, null);
      phone = ApiBoundaryContractRules.sanitizeConsultantVisibleText(phone, null);
      roleType = ApiBoundaryContractRules.sanitizeConsultantVisibleText(roleType, null);
      status = ApiBoundaryContractRules.sanitizeConsultantVisibleText(status, "active");
    }
  }
}
