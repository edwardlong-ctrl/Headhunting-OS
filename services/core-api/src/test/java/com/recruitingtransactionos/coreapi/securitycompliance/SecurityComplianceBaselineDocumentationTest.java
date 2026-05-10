package com.recruitingtransactionos.coreapi.securitycompliance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
