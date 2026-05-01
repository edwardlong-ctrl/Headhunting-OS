package com.recruitingtransactionos.coreapi.aitaskrunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.recruitingtransactionos.coreapi.CoreApiApplication;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorInput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.authenticity.AuthenticityRiskAssessorTaskService;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserInput;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserResult;
import com.recruitingtransactionos.coreapi.aitaskrunner.tasks.candidateprofile.CandidateProfileParserTaskService;
import com.recruitingtransactionos.coreapi.truthlayer.port.AITaskRunStatus;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRef;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.EntityRef;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = CoreApiApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "runLiveAiIT", matches = "true")
@Testcontainers
class AITaskRunnerDeepSeekIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
  private static final Path DOCUMENT_STORAGE_ROOT =
      Path.of("target", "deepseek-it-document-storage").toAbsolutePath();
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000220001");
  private static final UUID ACTOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000220002");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  @Autowired
  private CandidateProfileParserTaskService candidateProfileParserTaskService;

  @Autowired
  private AuthenticityRiskAssessorTaskService authenticityRiskAssessorTaskService;

  @Autowired
  private DataSource dataSource;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
    Files.createDirectories(DOCUMENT_STORAGE_ROOT);
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
    registry.add("rto.document-storage.root-dir", () -> DOCUMENT_STORAGE_ROOT.toString());
  }

  @Test
  void executesCandidateProfileParserAgainstDeepSeek() throws SQLException {
    assumeTrue(hasEnv("RTO_AI_DEEPSEEK_API_KEY"), "DeepSeek API key is required for this test");
    seedOrganizationAndUser();

    CandidateProfileParserResult result = candidateProfileParserTaskService.execute(
        ORGANIZATION_ID,
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        new EntityRef("CANDIDATE", UUID.fromString("00000000-0000-0000-0000-000000220003")),
        List.of(UUID.fromString("00000000-0000-0000-0000-000000220004")),
        new CandidateProfileParserInput(
            "Backend engineer profile draft",
            """
            Alex Chen is a backend engineer with 6 years of experience building Java and PostgreSQL services.
            Led API design for recruiting workflow automation, improved system reliability, and collaborated with product teams.
            Core skills include Java, Spring Boot, PostgreSQL, REST APIs, and distributed systems troubleshooting.
            """,
            "",
            "",
            "Focus on architecture ownership, workflow auditability, and database-heavy backend work."),
        null,
        null);

    assertThat(result.execution().runRecord().status()).isEqualTo(AITaskRunStatus.SUCCEEDED);
    assertThat(result.execution().runRecord().model().provider()).isEqualTo("deepseek");
    assertThat(result.execution().runRecord().traceRef()).isNotBlank();
    assertThat(result.execution().runRecord().outputPayloadJson()).contains("headline");
    assertThat(result.output().headline()).isNotBlank();
    assertThat(result.output().summary()).isNotBlank();
    assertThat(result.output().primarySkills()).isNotEmpty();
  }

  @Test
  void executesAuthenticityRiskAssessorAgainstDeepSeek() throws SQLException {
    assumeTrue(hasEnv("RTO_AI_DEEPSEEK_API_KEY"), "DeepSeek API key is required for this test");
    seedOrganizationAndUser();

    AuthenticityRiskAssessorResult result = authenticityRiskAssessorTaskService.execute(
        ORGANIZATION_ID,
        new ActorRef(ACTOR_ID, ActorRole.CONSULTANT),
        new EntityRef("CANDIDATE", UUID.fromString("00000000-0000-0000-0000-000000220005")),
        List.of(UUID.fromString("00000000-0000-0000-0000-000000220006")),
        new AuthenticityRiskAssessorInput(
            """
            Senior platform engineer who claims to have transformed every team into a 10x delivery engine.
            States ownership of distributed systems, security, SRE, hiring, GTM, and product strategy with little concrete evidence.
            Mentions multiple leadership wins but provides no dates, employers, metrics baselines, or external references.
            """,
            "",
            "",
            """
            Language feels marketing-heavy in parts. Check for vague claims, missing grounding, and evidence gaps.
            """),
        null,
        null);

    assertThat(result.execution().runRecord().status()).isEqualTo(AITaskRunStatus.SUCCEEDED);
    assertThat(result.execution().runRecord().model().provider()).isEqualTo("deepseek");
    assertThat(result.execution().runRecord().traceRef()).isNotBlank();
    assertThat(result.execution().runRecord().outputPayloadJson()).contains("authenticityRisk");
    assertThat(result.output().authenticityRisk()).isIn("low", "medium", "high");
    assertThat(result.output().specificityScore()).isBetween(0, 100);
    assertThat(result.output().flags()).isNotNull();
  }

  private static boolean hasEnv(String name) {
    String value = System.getenv(name);
    return value != null && !value.isBlank();
  }

  private void seedOrganizationAndUser() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement organization = connection.prepareStatement("""
            INSERT INTO identity.organization (
              organization_id,
              legal_name,
              display_name,
              status,
              default_timezone
            ) VALUES (?, ?, ?, 'active', 'UTC')
            ON CONFLICT (organization_id) DO NOTHING
            """);
        PreparedStatement user = connection.prepareStatement("""
            INSERT INTO identity.user_account (
              user_account_id,
              organization_id,
              email,
              display_name,
              status
            ) VALUES (?, ?, ?, ?, 'active')
            ON CONFLICT (user_account_id) DO NOTHING
            """)) {
      organization.setObject(1, ORGANIZATION_ID);
      organization.setString(2, "AITask Runner DeepSeek Test Org");
      organization.setString(3, "AITask Runner Test Org");
      organization.executeUpdate();

      user.setObject(1, ACTOR_ID);
      user.setObject(2, ORGANIZATION_ID);
      user.setString(3, "aitask-runner-test@example.com");
      user.setString(4, "AITask Runner Test User");
      user.executeUpdate();
    }
  }
}
