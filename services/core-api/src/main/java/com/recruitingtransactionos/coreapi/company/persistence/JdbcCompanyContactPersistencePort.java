package com.recruitingtransactionos.coreapi.company.persistence;

import com.recruitingtransactionos.coreapi.company.CompanyContact;
import com.recruitingtransactionos.coreapi.company.CompanyContactId;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.port.CompanyContactPersistencePort;
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

public final class JdbcCompanyContactPersistencePort implements CompanyContactPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.company_contact (
        company_contact_id, organization_id, company_id, name, title,
        email, phone, role_type, is_primary, status, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_COMPANY_SQL = """
      SELECT company_contact_id, organization_id, company_id, name, title,
        email, phone, role_type, is_primary, status, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.company_contact
      WHERE organization_id = ? AND company_id = ?
      ORDER BY is_primary DESC, name
      """;

  private final DataSource dataSource;

  public JdbcCompanyContactPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public CompanyContact create(CompanyContact contact) {
    Objects.requireNonNull(contact, "contact must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, contact.companyContactId().value());
      statement.setObject(2, contact.organizationId());
      statement.setObject(3, contact.companyId().value());
      statement.setString(4, contact.name());
      statement.setString(5, contact.title());
      statement.setString(6, contact.email());
      statement.setString(7, contact.phone());
      statement.setString(8, contact.roleType());
      statement.setBoolean(9, contact.isPrimary());
      statement.setString(10, contact.status());
      statement.setString(11, contact.metadata());
      statement.executeUpdate();
      return contact;
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create company contact", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<CompanyContact> findByCompanyIdAndOrganizationId(
      UUID organizationId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_COMPANY_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, companyId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<CompanyContact> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCompanyContact(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find company contacts", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static CompanyContact toCompanyContact(ResultSet rs) throws SQLException {
    return CompanyContact.builder()
        .companyContactId(new CompanyContactId(rs.getObject("company_contact_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .companyId(new CompanyId(rs.getObject("company_id", UUID.class)))
        .name(rs.getString("name"))
        .title(rs.getString("title"))
        .email(rs.getString("email"))
        .phone(rs.getString("phone"))
        .roleType(rs.getString("role_type"))
        .isPrimary(rs.getBoolean("is_primary"))
        .status(rs.getString("status"))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
