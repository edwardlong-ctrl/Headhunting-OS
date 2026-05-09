package com.recruitingtransactionos.coreapi.governancequery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRouter;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerConfiguration;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskRunnerProperties;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceSectionResponse;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigPort;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigRecord;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class GovernanceReadServiceTest {

  private static final UUID ORGANIZATION_ID = UUID.fromString("00000000-0000-0000-0000-000000440001");

  @Test
  void aiTaskRegistryListsProductionDefinitionsEvenBeforeAnyRunHistoryExists() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet runCount = countResultSet(0);
    ResultSet failureCount = countResultSet(0);
    ResultSet runStats = emptyResultSet();
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery())
        .thenReturn(runCount)
        .thenReturn(failureCount)
        .thenReturn(runStats);

    AITaskRunnerProperties properties = new AITaskRunnerProperties();
    AITaskRunnerProperties.Route defaultRoute = new AITaskRunnerProperties.Route();
    defaultRoute.setProvider("deepseek");
    defaultRoute.setModel("deepseek-v4-pro");
    properties.getRoutes().put("default", defaultRoute);

    GovernanceReadService service = new GovernanceReadService(
        dataSource,
        new GovernanceConfigService(new EmptyGovernanceConfigPort(), new ObjectMapper()),
        new ObjectMapper(),
        new AITaskRunnerConfiguration().aiTaskDefinitionRegistry(),
        new AITaskModelRouter(properties));

    GovernanceSectionResponse response = service.loadAdminSection(ORGANIZATION_ID, "ai-task-registry");

    assertThat(response.metrics())
        .anySatisfy(metric -> {
          assertThat(metric.key()).isEqualTo("taskDefinitions");
          assertThat(metric.value()).isEqualTo("28");
        });
    assertThat(response.items()).hasSize(28);
    assertThat(response.items().getFirst().primaryText()).isEqualTo("Source Classifier");
    assertThat(response.items().getFirst().secondaryText())
        .contains("Task 0.1", "source-classifier.v1", "deepseek/deepseek-v4-pro");
    assertThat(response.items().getFirst().detail())
        .contains(
            "schema:/ai/schemas/source-classifier-input.schema.json",
            "prompt:prompt.source-classifier.v1",
            "eval:/ai/evals/source-classifier-eval-cases.json",
            "evalResult:registered",
            "runs:0",
            "latencyMs:n/a",
            "replayHistory:0");
  }

  @Test
  void workflowRulesExposeTask45AutomationCoverage() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet overlayCount = countResultSet(2);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(overlayCount);

    GovernanceReadService service = new GovernanceReadService(
        dataSource,
        new GovernanceConfigService(new EmptyGovernanceConfigPort(), new ObjectMapper()),
        new ObjectMapper(),
        new AITaskRunnerConfiguration().aiTaskDefinitionRegistry(),
        new AITaskModelRouter(new AITaskRunnerProperties()));

    GovernanceSectionResponse response = service.loadAdminSection(ORGANIZATION_ID, "workflow-rules");

    assertThat(response.description())
        .contains("SLA", "reminder", "escalation", "next-best-action");
    assertThat(response.metrics())
        .anySatisfy(metric -> {
          assertThat(metric.key()).isEqualTo("slaRules");
          assertThat(metric.value()).isEqualTo("7");
        });
    assertThat(response.items())
        .anySatisfy(item -> {
          assertThat(item.primaryText()).isEqualTo("Consent SLA");
          assertThat(item.detail())
              .contains("due:PT48H", "reminder:PT24H", "escalation:PT72H");
        })
        .noneSatisfy(item -> assertThat(item.detail()).contains("deferred"));
  }

  private static ResultSet countResultSet(long value) throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getLong(1)).thenReturn(value);
    return resultSet;
  }

  private static ResultSet emptyResultSet() throws Exception {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(false);
    return resultSet;
  }

  private static final class EmptyGovernanceConfigPort implements GovernanceConfigPort {
    @Override
    public Optional<GovernanceConfigRecord> findByTypeAndKey(
        UUID organizationId,
        String configType,
        String configKey) {
      return Optional.empty();
    }

    @Override
    public List<GovernanceConfigRecord> listByType(UUID organizationId, String configType) {
      return List.of();
    }

    @Override
    public GovernanceConfigRecord upsert(
        UUID organizationId,
        String configType,
        String configKey,
        String payloadJson,
        boolean enabled,
        UUID actorUserId,
        PortalRole actorRole) {
      throw new UnsupportedOperationException("not needed");
    }
  }
}
