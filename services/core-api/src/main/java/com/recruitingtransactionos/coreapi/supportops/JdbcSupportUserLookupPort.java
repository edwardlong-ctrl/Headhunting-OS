package com.recruitingtransactionos.coreapi.supportops;

import com.recruitingtransactionos.coreapi.identityaccess.PortalRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcSupportUserLookupPort implements SupportUserLookupPort {

  private static final String FIND_USER_SQL = """
      SELECT user_account_id, organization_id, email, display_name, status
      FROM identity.user_account
      WHERE organization_id = ?
        AND user_account_id = ?
      """;

  private static final String FIND_ACTIVE_ROLES_SQL = """
      SELECT role::text
      FROM identity.role_assignment
      WHERE organization_id = ?
        AND user_account_id = ?
        AND status = 'active'
      ORDER BY role::text
      """;

  private final DataSource dataSource;

  public JdbcSupportUserLookupPort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Optional<SupportUserSummary> findUser(SupportUserLookupPortCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      try (PreparedStatement statement = connection.prepareStatement(FIND_USER_SQL)) {
        statement.setObject(1, command.organizationId());
        statement.setObject(2, command.userAccountId());
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            return Optional.empty();
          }
          return Optional.of(new SupportUserSummary(
              resultSet.getObject("user_account_id", UUID.class),
              resultSet.getObject("organization_id", UUID.class),
              resultSet.getString("email"),
              resultSet.getString("display_name"),
              resultSet.getString("status"),
              roles(connection, command)));
        }
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to lookup support user", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static List<PortalRole> roles(
      Connection connection,
      SupportUserLookupPortCommand command) throws SQLException {
    List<PortalRole> roles = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_ROLES_SQL)) {
      statement.setObject(1, command.organizationId());
      statement.setObject(2, command.userAccountId());
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          roles.add(PortalRole.fromWireValue(resultSet.getString("role")));
        }
      }
    }
    return List.copyOf(roles);
  }
}
