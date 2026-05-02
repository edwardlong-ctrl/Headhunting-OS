package com.recruitingtransactionos.coreapi.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanyIntakeApplicationServiceTest {

  private final CompanyService companyService = mock(CompanyService.class);
  private final CompanyIntakeApplicationService service =
      new CompanyIntakeApplicationService(companyService);

  @Test
  void metadataContainsActorRequiresExactClientActorIdField() {
    UUID ownerActorId = UUID.randomUUID();
    UUID otherActorId = UUID.randomUUID();
    String metadata = """
        {
          "clientActorId":"%s",
          "notes":"mentioned actor %s in free text"
        }
        """.formatted(ownerActorId, otherActorId);

    assertThat(CompanyIntakeApplicationService.metadataContainsActor(metadata, ownerActorId)).isTrue();
    assertThat(CompanyIntakeApplicationService.metadataContainsActor(metadata, otherActorId)).isFalse();
  }

  @Test
  void upsertClientProfileRejectsExistingCompanyOwnedByAnotherActor() {
    UUID organizationId = UUID.randomUUID();
    UUID ownerActorId = UUID.randomUUID();
    UUID otherActorId = UUID.randomUUID();
    CompanyId companyId = new CompanyId(UUID.randomUUID());
    Company existing = Company.builder()
        .companyId(companyId)
        .organizationId(organizationId)
        .name("Existing Company")
        .status(CompanyStatus.ACTIVE)
        .metadata("{\"clientActorId\":\"%s\"}".formatted(ownerActorId))
        .createdAt(Instant.parse("2026-05-01T00:00:00Z"))
        .updatedAt(Instant.parse("2026-05-01T00:00:00Z"))
        .version(1)
        .build();
    when(companyService.findCompanyByIdAndOrganizationId(organizationId, companyId))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.upsertClientProfile(
        organizationId,
        otherActorId,
        Optional.of(companyId),
        "Updated Company",
        null,
        null,
        null,
        null,
        null,
        null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("client_company_profile_not_owned_by_actor");

    verify(companyService, never()).updateCompany(org.mockito.ArgumentMatchers.any());
  }
}
