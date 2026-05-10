package com.recruitingtransactionos.coreapi.securitycompliance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityComplianceBaselineDocumentationTest {

  private static final Path TASK_52_DOC =
      Path.of("../../docs/security/task-52-production-security-compliance-baseline.md");
  private static final Path SECURITY_README = Path.of("../../docs/security/README.md");
  private static final Path PACKAGE_JSON = Path.of("../../package.json");

  @Test
  void task52BaselineTracksRequiredControlsIssueClosureAndRegressionSuite() throws IOException {
    String document = Files.readString(TASK_52_DOC);

    assertThat(document)
        .contains("# Task 52 Production Security Compliance Baseline")
        .contains("## Threat Model")
        .contains("## Access Review Process")
        .contains("## Privacy and Data Retention Runbook")
        .contains("## Key and Secret Rotation Runbook")
        .contains("## Dependency Vulnerability Remediation Workflow")
        .contains("## Pen-Test Issue Remediation Tracking")
        .contains("## Security Regression Suite")
        .contains("## Baseline Issue Register")
        .contains("Status: CLOSED")
        .contains("Status: RISK_ACCEPTED")
        .contains("No 100% production-security claim is allowed");

    assertThat(document)
        .contains("SensitiveEndpointRateLimitFilterTest")
        .contains("JwtAuthenticationFilterTest")
        .contains("FivePortalBoundaryRegressionTest")
        .contains("PermissionEnforcerTest")
        .contains("JdbcAccessAuditRecorderPostgresIntegrationTest")
        .contains("npm audit --omit=dev")
        .contains("security:core-api:dependency-check");
  }

  @Test
  void securityReadmeAndPackageScriptsExposeTask52BaselineAndDependencyScan() throws IOException {
    assertThat(Files.readString(SECURITY_README))
        .contains("task-52-production-security-compliance-baseline.md");
    assertThat(Files.readString(PACKAGE_JSON))
        .contains("\"security:core-api:dependency-check\"");
  }

  @Test
  void issueRegisterRowsHaveClosureOrExplicitRiskAcceptanceEvidence() throws IOException {
    String document = Files.readString(TASK_52_DOC);
    List<String> issueRows = document.lines()
        .filter(line -> line.startsWith("| T52-SEC-"))
        .toList();

    assertThat(issueRows)
        .extracting(SecurityComplianceBaselineDocumentationTest::cellValueCount)
        .containsOnly(8);
    assertThat(issueRows).allSatisfy(row -> {
      List<String> cells = cells(row);
      assertThat(cells.get(4)).as("owner for " + cells.get(0)).isNotBlank();
      assertThat(cells.get(5)).as("status for " + cells.get(0))
          .satisfiesAnyOf(
              status -> assertThat(status).isEqualTo("Status: CLOSED"),
              status -> assertThat(status).isEqualTo("Status: RISK_ACCEPTED"));
      assertThat(cells.get(6)).as("evidence for " + cells.get(0)).isNotBlank();
      assertThat(cells.get(7)).as("review date for " + cells.get(0))
          .matches("\\d{4}-\\d{2}-\\d{2}");
    });

    assertThat(document)
        .contains("| T52-SEC-008 | Baseline review | Medium | AI prompt/model-output retention window");
  }

  @Test
  void documentedSecurityScanCommandsRemainRtkWrapped() throws IOException {
    String document = Files.readString(TASK_52_DOC);

    assertThat(document)
        .contains("rtk npm audit --omit=dev")
        .contains("rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check");
    assertThat(document.lines()
        .filter(line -> line.contains("npm run security:core-api:dependency-check"))
        .toList())
        .allMatch(line -> line.contains(
            "rtk env DEPENDENCY_CHECK_PREWARMED_CACHE=1 npm run security:core-api:dependency-check"));
  }

  private static int cellValueCount(String markdownRow) {
    return cells(markdownRow).size();
  }

  private static List<String> cells(String markdownRow) {
    return Arrays.stream(markdownRow.split("\\|", -1))
        .skip(1)
        .limit(8)
        .map(String::trim)
        .toList();
  }
}
