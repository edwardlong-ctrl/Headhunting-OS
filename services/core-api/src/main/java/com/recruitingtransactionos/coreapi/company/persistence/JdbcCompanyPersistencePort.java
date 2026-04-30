package com.recruitingtransactionos.coreapi.company.persistence;

import com.recruitingtransactionos.coreapi.company.Company;
import com.recruitingtransactionos.coreapi.company.CompanyId;
import com.recruitingtransactionos.coreapi.company.CompanyStatus;
import com.recruitingtransactionos.coreapi.company.port.CompanyPersistencePort;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCompanyPersistencePort implements CompanyPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.company (
        company_id, organization_id, name, display_name, industry, website,
        headquarters_location, size_band, status, payment_reliability,
        owner_consultant_id, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT company_id, organization_id, name, display_name, industry, website,
        headquarters_location, size_band, status, payment_reliability,
        owner_consultant_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.company
      WHERE organization_id = ? AND company_id = ?
      """;

  private static final String FIND_BY_ORG_AND_STATUS_SQL = """
      SELECT company_id, organization_id, name, display_name, industry, website,
        headquarters_location, size_band, status, payment_reliability,
        owner_consultant_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.company
      WHERE organization_id = ? AND status = ?
      ORDER BY name
      """;

  private static final String FIND_ALL_BY_ORG_SQL = """
      SELECT company_id, organization_id, name, display_name, industry, website,
        headquarters_location, size_band, status, payment_reliability,
        owner_consultant_id, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.company
      WHERE organization_id = ?
      ORDER BY name
      """;

  private final DataSource dataSource;

  public JdbcCompanyPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Company create(Company company) {
    Objects.requireNonNull(company, "company must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, company.companyId().value());
      statement.setObject(2, company.organizationId());
      statement.setString(3, company.name());
      statement.setString(4, company.displayName());
      statement.setString(5, company.industry());
      statement.setString(6, company.website());
      statement.setString(7, company.headquartersLocation());
      statement.setString(8, company.sizeBand());
      statement.setString(9, company.status().wireValue());
      statement.setString(10, company.paymentReliability());
      statement.setObject(11, company.ownerConsultantId());
      statement.setString(12, company.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(company.organizationId(), company.companyId())
          .orElseThrow(() -> new IllegalStateException("company not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create company", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<Company> findByIdAndOrganizationId(UUID organizationId, CompanyId companyId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(companyId, "companyId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, companyId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toCompany(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find company by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Company> findByOrganizationIdAndStatus(UUID organizationId, CompanyStatus status) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ORG_AND_STATUS_SQL)) {
      statement.setObject(1, organizationId);
      statement.setString(2, status.wireValue());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Company> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCompany(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find companies by status", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Company> findAllByOrganizationId(UUID organizationId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_BY_ORG_SQL)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Company> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCompany(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find companies by organization", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static Company toCompany(ResultSet rs) throws SQLException {
    return Company.builder()
        .companyId(new CompanyId(rs.getObject("company_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .name(rs.getString("name"))
        .displayName(rs.getString("display_name"))
        .industry(rs.getString("industry"))
        .website(rs.getString("website"))
        .headquartersLocation(rs.getString("headquarters_location"))
        .sizeBand(rs.getString("size_band"))
        .status(CompanyStatus.fromWireValue(rs.getString("status")))
        .paymentReliability(rs.getString("payment_reliability"))
        .ownerConsultantId(rs.getObject("owner_consultant_id", UUID.class))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
