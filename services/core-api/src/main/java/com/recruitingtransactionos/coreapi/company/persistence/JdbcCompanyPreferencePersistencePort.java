package com.recruitingtransactionos.coreapi.company.persistence;

import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyPreference;
import com.recruitingtransactionos.coreapi.company.CompanyPreferenceId;
import com.recruitingtransactionos.coreapi.company.port.CompanyPreferencePersistencePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCompanyPreferencePersistencePort implements CompanyPreferencePersistencePort {

  private static final String UPSERT_SQL = """
      INSERT INTO recruiting.company_preference (
        company_preference_id, organization_id, company_id,
        preference_key, preference_value, notes
      ) VALUES (?, ?, ?, ?, ?::jsonb, ?)
      ON CONFLICT (organization_id, company_id, preference_key)
      DO UPDATE SET
        preference_value = EXCLUDED.preference_value,
        notes = EXCLUDED.notes,
        updated_at = now(),
        version = recruiting.company_preference.version + 1
      """;

  private static final String FIND_BY_COMPANY_SQL = """
      SELECT company_preference_id, organization_id, company_id,
        preference_key, preference_value::text AS preference_value, notes,
        created_at, updated_at, version
      FROM recruiting.company_preference
      WHERE organization_id = ? AND company_id = ?
      ORDER BY preference_key
      """;

  private final DataSource dataSource;

  public JdbcCompanyPreferencePersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public CompanyPreference upsert(CompanyPreference preference) {
    Objects.requireNonNull(preference, "preference must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
      statement.setObject(1, preference.companyPreferenceId().value());
      statement.setObject(2, preference.organizationId());
      statement.setObject(3, preference.companyId().value());
      statement.setString(4, preference.preferenceKey());
      statement.setString(5, preference.preferenceValue());
      statement.setString(6, preference.notes());
      statement.executeUpdate();
      return preference;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to upsert company preference", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<CompanyPreference> findByCompanyIdAndOrganizationId(
      UUID organizationId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, companyId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<CompanyPreference> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCompanyPreference(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find company preferences", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static CompanyPreference toCompanyPreference(ResultSet rs) throws SQLException {
    return CompanyPreference.builder()
        .companyPreferenceId(new CompanyPreferenceId(
            rs.getObject("company_preference_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .companyId(new CompanyId(rs.getObject("company_id", UUID.class)))
        .preferenceKey(rs.getString("preference_key"))
        .preferenceValue(rs.getString("preference_value"))
        .notes(rs.getString("notes"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
