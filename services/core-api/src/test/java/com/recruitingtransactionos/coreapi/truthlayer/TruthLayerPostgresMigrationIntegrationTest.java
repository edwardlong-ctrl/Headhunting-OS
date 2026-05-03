package com.recruitingtransactionos.coreapi.truthlayer;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class TruthLayerPostgresMigrationIntegrationTest {

  private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(POSTGRES_IMAGE);

  private static final List<String> REQUIRED_SCHEMAS =
      List.of("identity", "recruiting", "governance", "workflow", "audit", "intake");

  private static final Map<String, List<String>> REQUIRED_TABLES = Map.of(
      "identity", List.of("organization", "user_account", "role_assignment"),
      "recruiting", List.of("candidate", "candidate_profile", "source_item", "information_packet"),
      "governance", List.of(
          "claim_ledger_item",
          "review_event",
          "canonical_write_attempt",
          "ai_task_definition",
          "ai_task_run"),
      "workflow", List.of("workflow_event"),
      "audit", List.of("audit_log"),
      "intake", List.of(
          "source_item",
          "information_packet",
          "information_packet_source_item",
          "extraction_run"));

  private static final List<IndexRef> CRITICAL_INDEXES = List.of(
      new IndexRef("identity", "organization", "organization_active_legal_name_uidx"),
      new IndexRef("identity", "user_account", "user_account_org_email_uidx"),
      new IndexRef("identity", "role_assignment", "role_assignment_active_scope_uidx"),
      new IndexRef("recruiting", "candidate_profile", "candidate_profile_version_uidx"),
      new IndexRef("governance", "ai_task_definition", "ai_task_definition_key_version_uidx"),
      new IndexRef("intake", "source_item", "intake_source_item_org_status_idx"),
      new IndexRef("intake", "information_packet",
          "intake_information_packet_org_type_status_idx"),
      new IndexRef("intake", "information_packet_source_item",
          "intake_packet_source_item_source_idx"),
      new IndexRef("intake", "extraction_run", "intake_extraction_run_org_packet_created_idx"),
      new IndexRef("governance", "claim_ledger_item", "claim_ledger_org_source_span_idx"),
      new IndexRef("governance", "canonical_write_attempt", "cwa_org_decision_occurred_idx"),
      new IndexRef("governance", "canonical_write_attempt", "cwa_org_entity_idx"),
      new IndexRef("governance", "canonical_write_attempt", "cwa_org_idempotency_uidx"),
      new IndexRef("workflow", "workflow_event", "workflow_event_org_idempotency_uidx"),
      new IndexRef("workflow", "workflow_event", "workflow_event_org_correlation_idx"),
      new IndexRef("workflow", "workflow_event", "workflow_event_org_causation_idx"));

  private static final List<ConstraintRef> CRITICAL_FOREIGN_KEYS = List.of(
      new ConstraintRef("recruiting", "candidate_current_profile_fk"),
      new ConstraintRef("governance", "claim_ledger_item_review_event_fk"),
      new ConstraintRef("intake", "intake_packet_source_item_packet_fk"),
      new ConstraintRef("intake", "intake_packet_source_item_source_fk"),
      new ConstraintRef("intake", "intake_extraction_run_packet_fk"));

  @Test
  void flywayRunsTruthLayerMigrationsAgainstRealPostgres() throws SQLException {
    MigrateResult result = Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();

    assertThat(result.migrationsExecuted).isEqualTo(25);

    try (Connection connection = DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
      assertThat(appliedMigrationVersions(connection))
          .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
            "18", "19", "20", "21", "22", "23", "24", "25");

      for (String schema : REQUIRED_SCHEMAS) {
        assertThat(schemaExists(connection, schema))
            .as("schema %s should exist after V1-V7 migrations", schema)
            .isTrue();
      }

      for (Map.Entry<String, List<String>> entry : REQUIRED_TABLES.entrySet()) {
        String schema = entry.getKey();
        for (String table : entry.getValue()) {
          assertThat(tableExists(connection, schema, table))
              .as("table %s.%s should exist after V1-V7 migrations", schema, table)
              .isTrue();
        }
      }

      for (IndexRef index : CRITICAL_INDEXES) {
        assertThat(indexExists(connection, index))
            .as("index %s.%s on %s should exist", index.schema(), index.indexName(), index.table())
            .isTrue();
      }

      assertThat(columnExists(connection, "workflow", "workflow_event", "idempotency_key"))
          .isTrue();
      assertThat(columnExists(connection, "workflow", "workflow_event", "correlation_id"))
          .isTrue();
      assertThat(columnExists(connection, "workflow", "workflow_event", "causation_id"))
          .isTrue();

      for (ConstraintRef constraint : CRITICAL_FOREIGN_KEYS) {
        assertThat(foreignKeyExists(connection, constraint))
            .as("foreign key %s.%s should exist", constraint.schema(), constraint.name())
            .isTrue();
      }
    }
  }

  private static List<String> appliedMigrationVersions(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank");
        ResultSet resultSet = statement.executeQuery()) {
      List<String> versions = new java.util.ArrayList<>();
      while (resultSet.next()) {
        versions.add(resultSet.getString("version"));
      }
      return versions;
    }
  }

  private static boolean schemaExists(Connection connection, String schema) throws SQLException {
    return exists(connection,
        "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)",
        schema);
  }

  private static boolean tableExists(Connection connection, String schema, String table)
      throws SQLException {
    return exists(connection,
        "SELECT EXISTS ("
            + "SELECT 1 FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_name = ? AND table_type = 'BASE TABLE')",
        schema,
        table);
  }

  private static boolean indexExists(Connection connection, IndexRef index) throws SQLException {
    return exists(connection,
        "SELECT EXISTS ("
            + "SELECT 1 FROM pg_indexes "
            + "WHERE schemaname = ? AND tablename = ? AND indexname = ?)",
        index.schema(),
        index.table(),
        index.indexName());
  }

  private static boolean columnExists(
      Connection connection,
      String schema,
      String table,
      String column) throws SQLException {
    return exists(connection,
        "SELECT EXISTS ("
            + "SELECT 1 FROM information_schema.columns "
            + "WHERE table_schema = ? AND table_name = ? AND column_name = ?)",
        schema,
        table,
        column);
  }

  private static boolean foreignKeyExists(Connection connection, ConstraintRef constraint)
      throws SQLException {
    return exists(connection,
        "SELECT EXISTS ("
            + "SELECT 1 FROM pg_constraint c "
            + "JOIN pg_namespace n ON n.oid = c.connamespace "
            + "WHERE n.nspname = ? AND c.conname = ? AND c.contype = 'f')",
        constraint.schema(),
        constraint.name());
  }

  private static boolean exists(Connection connection, String sql, String... parameters)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setString(i + 1, parameters[i]);
      }

      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private record IndexRef(String schema, String table, String indexName) {
  }

  private record ConstraintRef(String schema, String name) {
  }
}
