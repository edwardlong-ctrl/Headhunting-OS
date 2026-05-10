package com.recruitingtransactionos.coreapi.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeploymentArtifactsContractTest {

  private static final Path REPO_ROOT = findRepoRoot();

  @Test
  void providerNeutralDeploymentArtifactsExist() {
    assertThat(REPO_ROOT.resolve("services/core-api/Dockerfile")).exists();
    assertThat(REPO_ROOT.resolve("apps/web/Dockerfile")).exists();
    assertThat(REPO_ROOT.resolve("apps/web/nginx.conf.template")).exists();
    assertThat(REPO_ROOT.resolve("infra/docker/compose.production-like.yml")).exists();
    assertThat(REPO_ROOT.resolve("infra/deployment/production-like.env.example")).exists();
    assertThat(REPO_ROOT.resolve("infra/deployment/migration-runbook.md")).exists();
    assertThat(REPO_ROOT.resolve("infra/deployment/rollback-runbook.md")).exists();
    assertThat(REPO_ROOT.resolve("infra/deployment/backup-restore-runbook.md")).exists();
    assertThat(REPO_ROOT.resolve("infra/deployment/task-53-disaster-recovery-business-continuity.md")).exists();
    assertThat(REPO_ROOT.resolve("infra/deployment/staging-smoke-test.md")).exists();
  }

  @Test
  void productionLikeExampleDocumentsRequiredEnvironmentWithoutRealSecrets() throws Exception {
    Path example = REPO_ROOT.resolve("infra/deployment/production-like.env.example");
    String content = Files.readString(example);

    List<String> requiredKeys = List.of(
        "SPRING_PROFILES_ACTIVE",
        "SPRING_DATASOURCE_URL",
        "SPRING_DATASOURCE_USERNAME",
        "SPRING_DATASOURCE_PASSWORD",
        "CORE_API_FLYWAY_ENABLED",
        "RTO_AUTH_JWT_SECRET",
        "RTO_DOCUMENT_STORAGE_ROOT_DIR",
        "RTO_DOCUMENT_STORAGE_VIRUS_SCAN_MODE",
        "RTO_AI_DEEPSEEK_API_KEY",
        "RTO_PUBLIC_BASE_URL",
        "RTO_FRONTEND_ORIGIN",
        "RTO_DEPLOYMENT_DATABASE_MANAGED",
        "RTO_OBJECT_STORAGE_PROVIDER",
        "RTO_OBJECT_STORAGE_BUCKET",
        "RTO_OBJECT_STORAGE_ENDPOINT",
        "RTO_OBJECT_STORAGE_ACCESS_KEY",
        "RTO_OBJECT_STORAGE_SECRET_KEY");

    for (String key : requiredKeys) {
      assertThat(content).contains(key + "=");
    }

    assertThat(content)
        .doesNotContain("recruiting_os_local_password")
        .doesNotContain("minioadmin")
        .doesNotContain("sk-")
        .doesNotContain("deepseek_api_key");
    assertThat(content).contains("CHANGE_ME_");
  }

  @Test
  void productionLikeComposeCreatesLocalMinioBucketBeforeCoreApiStartup() throws Exception {
    String compose = Files.readString(REPO_ROOT.resolve("infra/docker/compose.production-like.yml"));
    String env = Files.readString(REPO_ROOT.resolve("infra/deployment/production-like.env.example"));
    String coreApiDockerfile = Files.readString(REPO_ROOT.resolve("services/core-api/Dockerfile"));

    assertThat(env)
        .contains("RTO_OBJECT_STORAGE_PROVIDER=minio")
        .contains("RTO_OBJECT_STORAGE_ENDPOINT=http://minio:9000")
        .contains("RTO_OBJECT_STORAGE_BUCKET=rto-documents");
    assertThat(compose)
        .contains("minio-init:")
        .contains("mc mb --ignore-existing")
        .contains("condition: service_completed_successfully");
    assertThat(coreApiDockerfile)
        .contains("curl")
        .contains("rm -rf /var/lib/apt/lists/*");
  }

  @Test
  void deploymentRunbooksKeepTask39ProviderNeutralBoundary() throws Exception {
    String migration = Files.readString(REPO_ROOT.resolve("infra/deployment/migration-runbook.md"));
    String rollback = Files.readString(REPO_ROOT.resolve("infra/deployment/rollback-runbook.md"));
    String backup = Files.readString(REPO_ROOT.resolve("infra/deployment/backup-restore-runbook.md"));
    String smoke = Files.readString(REPO_ROOT.resolve("infra/deployment/staging-smoke-test.md"));

    assertThat(migration)
        .contains("empty database")
        .contains("Flyway")
        .contains("npm run pilot:data:import");
    assertThat(rollback)
        .contains("do not hand-edit production schema")
        .contains("previous image");
    assertThat(backup)
        .contains("pg_dump")
        .contains("restore")
        .contains("POSTGRES_BACKUP_URL")
        .contains("RTO_DOCUMENT_STORAGE_BACKUP_PATH")
        .contains("Document storage backup")
        .contains("Document storage restore")
        .doesNotContain("pg_dump \"$SPRING_DATASOURCE_URL\"")
        .doesNotContain("pg_restore --dbname \"$SPRING_DATASOURCE_URL\"");
    assertThat(smoke)
        .contains("consultant@pilot.example.test")
        .contains("intake")
        .contains("deployment baseline exists")
        .contains("not a production-ready claim");
  }

  @Test
  void task53DisasterRecoveryBusinessContinuityArtifactCoversRequiredDrills() throws Exception {
    String task53 = Files.readString(
        REPO_ROOT.resolve("infra/deployment/task-53-disaster-recovery-business-continuity.md"));

    assertThat(task53)
        .contains("# Task 53 Disaster Recovery and Business Continuity")
        .contains("Backup schedule")
        .contains("Restore drill")
        .contains("Migration rollback drill")
        .contains("Object storage recovery")
        .contains("AI provider outage playbook")
        .contains("Notification provider outage playbook")
        .contains("Incident severity levels")
        .contains("Do not fake restore success")
        .contains("Restored database")
        .contains("Restored document")
        .contains("WorkflowEvent")
        .contains("ClaimLedger")
        .contains("AITaskRun")
        .contains("DisclosureRecord")
        .contains("rto-postgres.dump")
        .contains("artifacts/task53-dr-bcp-20260510")
        .contains("rto-documents.tar.gz")
        .contains("artifacts/task42-backup-restore-20260509/evidence.md");
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
