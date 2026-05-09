package com.recruitingtransactionos.coreapi.recruiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.recruitingtransactionos.coreapi.candidatedocument.port.CandidateDocumentPersistencePort;
import com.recruitingtransactionos.coreapi.candidatedocument.persistence.JdbcCandidateDocumentPersistencePort;
import com.recruitingtransactionos.coreapi.candidatedocument.service.CandidateDocumentService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class RecruitingDomainConfigurationTest {

  @Test
  void exposesCandidateDocumentRuntimeBeansForCandidatePortalAndMatchingSurfaces() {
    RecruitingDomainConfiguration configuration = new RecruitingDomainConfiguration();
    CandidateDocumentPersistencePort port = configuration.candidateDocumentPersistencePort(
        mock(DataSource.class));
    CandidateDocumentService service = configuration.candidateDocumentService(port);

    assertThat(port).isInstanceOf(JdbcCandidateDocumentPersistencePort.class);
    assertThat(service).isNotNull();
  }
}
