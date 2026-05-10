package com.recruitingtransactionos.coreapi.reportingexport;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruitingtransactionos.coreapi.apiboundary.ApiBoundaryContractRules;
import com.recruitingtransactionos.coreapi.apiboundary.ApiSafeResponseBody;
import com.recruitingtransactionos.coreapi.apiboundary.ReportingExportResult;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ReportingExportSafeDtoTest {

  @Test
  void exportResultIsWhitelistedApiSafeResponseBody() {
    assertThat(ApiSafeResponseBody.class).isAssignableFrom(ReportingExportResult.class);

    Set<String> fieldNames = Arrays.stream(ReportingExportResult.class.getRecordComponents())
        .map(RecordComponent::getName)
        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

    assertThat(fieldNames)
        .containsExactlyInAnyOrderElementsOf(ApiBoundaryContractRules.reportingExportResultFieldNames());
    fieldNames.forEach(field ->
        assertThat(ApiBoundaryContractRules.isAllowedReportingExportResultField(field)).isTrue());
    assertThat(ApiBoundaryContractRules.isAllowedReportingExportResultField("rawCandidate"))
        .isFalse();
    assertThat(ApiBoundaryContractRules.isAllowedReportingExportResultField("rawPayload"))
        .isFalse();
  }
}
