package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.recruitingtransactionos.coreapi.truthlayer.port.ClaimLedgerPort;
import com.recruitingtransactionos.coreapi.truthlayer.service.ClaimLedgerService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class TruthLayerConfigurationTest {

  @Test
  void configurationExposesClaimLedgerBeansForAiTaskWriteBack() {
    TruthLayerConfiguration configuration = new TruthLayerConfiguration();

    ClaimLedgerPort port = configuration.claimLedgerPort(mock(DataSource.class));
    ClaimLedgerService service = configuration.claimLedgerService(port);

    assertThat(port).isNotNull();
    assertThat(service).isNotNull();
  }
}
