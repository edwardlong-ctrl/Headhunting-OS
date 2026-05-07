package com.recruitingtransactionos.coreapi.pilotdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PilotDataCliApplication {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  private PilotDataCliApplication() {
  }

  public static void main(String[] args) throws Exception {
    PilotDataCommand command = args.length == 0
        ? PilotDataCommand.VALIDATE
        : PilotDataCommand.parse(args[0]);
    DataSource dataSource = dataSourceFromEnvironment();
    PilotDataset dataset = PilotDatasetLoader.defaultLoader().loadDefault();
    PilotDataService service = new PilotDataService(
        dataSource,
        new BCryptPasswordEncoder(),
        new PilotDataPrivacyValidator());

    if (command == PilotDataCommand.REBUILD || command == PilotDataCommand.IMPORT) {
      runMigrations(dataSource);
    }

    PilotDataReport report = switch (command) {
      case REBUILD -> service.rebuild(dataset);
      case RESET -> service.reset(
          dataset.organization().organizationId(),
          "true".equalsIgnoreCase(System.getenv("RTO_PILOT_DATA_ALLOW_RESET")));
      case IMPORT -> service.importDataset(dataset);
      case VALIDATE -> service.validate(dataset.organization().organizationId());
      case EXPORT -> service.export(dataset.organization().organizationId());
    };
    System.out.println(OBJECT_MAPPER.writeValueAsString(report));
    if (!report.valid()) {
      System.exit(2);
    }
  }

  private static void runMigrations(DataSource dataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
  }

  private static DataSource dataSourceFromEnvironment() {
    String url = env("RTO_PILOT_DATA_JDBC_URL", env(
        "SPRING_DATASOURCE_URL",
        "jdbc:postgresql://localhost:5432/recruiting_os"));
    String username = env("RTO_PILOT_DATA_DB_USER", env("POSTGRES_USER", "recruiting_os"));
    String password = env(
        "RTO_PILOT_DATA_DB_PASSWORD",
        env("POSTGRES_PASSWORD", "recruiting_os_local_password"));
    return new DriverManagerBackedDataSource(url, username, password);
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }

  private record DriverManagerBackedDataSource(String url, String username, String password)
      implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
      return DriverManager.getConnection(url, username, password);
    }

    @Override
    public Connection getConnection(String requestedUsername, String requestedPassword)
        throws SQLException {
      return DriverManager.getConnection(url, requestedUsername, requestedPassword);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
      return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
    }

    @Override
    public int getLoginTimeout() throws SQLException {
      return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
    }
  }
}
