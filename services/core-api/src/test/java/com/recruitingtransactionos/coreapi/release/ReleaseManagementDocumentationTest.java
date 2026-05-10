package com.recruitingtransactionos.coreapi.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReleaseManagementDocumentationTest {

  private static final Path REPO_ROOT = findRepoRoot();

  @Test
  void releaseScriptsPackageScriptsAndCiExposeEveryRequiredTask58Gate() throws IOException {
    List<String> releaseScripts = List.of(
        "scripts/release/release-gate.sh",
        "scripts/release/validate-migrations.sh",
        "scripts/release/run-pilot-e2e.sh",
        "scripts/release/ai-eval-regression.sh",
        "scripts/release/privacy-security-regression.sh");

    for (String script : releaseScripts) {
      assertThat(REPO_ROOT.resolve(script)).as(script).exists();
      assertThat(Files.readString(REPO_ROOT.resolve(script))).as(script)
          .contains("set -euo pipefail");
    }

    String packageJson = Files.readString(REPO_ROOT.resolve("package.json"));
    assertThat(packageJson)
        .contains("\"release:gate\"")
        .contains("\"release:migrations\"")
        .contains("\"release:e2e:pilot\"")
        .contains("\"release:privacy-security\"")
        .contains("\"release:ai-eval\"");

    String releaseGate = Files.readString(REPO_ROOT.resolve("scripts/release/release-gate.sh"));
    assertThat(releaseGate)
        .contains("npm run test:core-api")
        .contains("npm --workspace @rto/web run test")
        .contains("npm run typecheck:web")
        .contains("npm run build:web")
        .contains("npm run release:migrations")
        .contains("npm run release:privacy-security")
        .contains("npm run release:ai-eval")
        .contains("npm run release:e2e:pilot")
        .contains("RELEASE_READY")
        .contains("RTO_RELEASE_SKIP_BROWSER_E2E")
        .contains("RTO_RELEASE_E2E_EVIDENCE")
        .contains("-r \"${RTO_RELEASE_E2E_EVIDENCE}\"")
        .contains("signed risk acceptance")
        .contains("release:e2e:pilot")
        .doesNotContain("Task 60");

    String workflow = Files.readString(REPO_ROOT.resolve(".github/workflows/release-regression.yml"));
    assertThat(workflow)
        .contains("release-regression")
        .contains("npm run test:core-api")
        .contains("npm --workspace @rto/web run test")
        .contains("npm run typecheck:web")
        .contains("npm run build:web")
        .contains("npm run release:migrations")
        .contains("npm run release:privacy-security")
        .contains("npm run release:ai-eval")
        .contains("workflow_dispatch")
        .contains("run_pilot_e2e")
        .doesNotContain("NVD_API_KEY")
        .doesNotContain("production");
  }

  @Test
  void releaseChecklistAndGateDocsRequireEvidenceForEveryBlockingGate() throws IOException {
    String checklist = Files.readString(REPO_ROOT.resolve("docs/release/release-checklist.md"));
    String gates = Files.readString(REPO_ROOT.resolve("docs/release/release-gates.md"));
    String evidence = Files.readString(
        REPO_ROOT.resolve("docs/roadmap/task-58-release-management-regression-suite.md"));

    for (String gate : List.of(
        "Backend regression",
        "Frontend regression",
        "Migration validation",
        "Browser E2E",
        "Privacy/security negative",
        "AI eval regression")) {
      assertThat(checklist).contains(gate);
      assertThat(gates).contains(gate);
      assertThat(evidence).contains(gate);
    }

    assertThat(checklist)
        .contains("Owner")
        .contains("Command")
        .contains("Expected evidence")
        .contains("Blocker condition")
        .contains("Waiver / risk acceptance")
        .contains("signed risk acceptance")
        .contains("A release cannot be called ready");

    assertThat(gates)
        .contains("Backend owns truth")
        .contains("PostgreSQL is the target source of truth")
        .contains("AI outputs claims, not facts")
        .contains("Every key state transition must create WorkflowEvent")
        .contains("Client must never read raw Candidate objects before unlock/disclosure")
        .contains("Task 51 tenant boundaries")
        .contains("Task 58 only creates the release safety system")
        .doesNotContain("public production-ready")
        .doesNotContain("SaaS ready");

    assertThat(evidence)
        .contains("Automated")
        .contains("Manual or environment-dependent")
        .contains("Deferred until Task 60")
        .contains("does not implement Task 60");
  }

  private static Path findRepoRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.exists(current.resolve("package.json"))
          && Files.exists(current.resolve("services/core-api/pom.xml"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Could not locate repository root");
  }
}
