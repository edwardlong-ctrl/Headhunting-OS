package com.recruitingtransactionos.coreapi.commission.persistence;

import com.recruitingtransactionos.coreapi.commission.Commission;
import com.recruitingtransactionos.coreapi.commission.CommissionId;
import com.recruitingtransactionos.coreapi.commission.CommissionStatus;
import com.recruitingtransactionos.coreapi.commission.CommissionType;
import com.recruitingtransactionos.coreapi.commission.port.CommissionPersistencePort;
import com.recruitingtransactionos.coreapi.placement.PlacementId;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

public final class JdbcCommissionPersistencePort implements CommissionPersistencePort {

  private static final String INSERT_SQL = """
      INSERT INTO recruiting.commission (
        commission_id, organization_id, placement_id, consultant_id,
        status, commission_type, amount, currency, split_percentage,
        calculation_details, paid_at, withheld_reason, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?::jsonb)
      """;

  private static final String FIND_BY_ID_SQL = """
      SELECT commission_id, organization_id, placement_id, consultant_id,
        status, commission_type, amount, currency, split_percentage,
        calculation_details::text AS calculation_details,
        paid_at, withheld_reason, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.commission
      WHERE organization_id = ? AND commission_id = ?
      """;

  private static final String FIND_BY_PLACEMENT_SQL = """
      SELECT commission_id, organization_id, placement_id, consultant_id,
        status, commission_type, amount, currency, split_percentage,
        calculation_details::text AS calculation_details,
        paid_at, withheld_reason, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.commission
      WHERE organization_id = ? AND placement_id = ?
      ORDER BY created_at
      """;

  private static final String FIND_BY_CONSULTANT_SQL = """
      SELECT commission_id, organization_id, placement_id, consultant_id,
        status, commission_type, amount, currency, split_percentage,
        calculation_details::text AS calculation_details,
        paid_at, withheld_reason, metadata::text AS metadata,
        created_at, updated_at, version
      FROM recruiting.commission
      WHERE organization_id = ? AND consultant_id = ?
      ORDER BY created_at DESC
      """;

  private final DataSource dataSource;

  public JdbcCommissionPersistencePort(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
  }

  @Override
  public Commission create(Commission commission) {
    Objects.requireNonNull(commission, "commission must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
      statement.setObject(1, commission.commissionId().value());
      statement.setObject(2, commission.organizationId());
      statement.setObject(3, commission.placementId().value());
      statement.setObject(4, commission.consultantId());
      statement.setString(5, commission.status().wireValue());
      statement.setString(6, commission.commissionType().wireValue());
      if (commission.amount() != null) {
        statement.setBigDecimal(7, commission.amount());
      } else {
        statement.setNull(7, Types.NUMERIC);
      }
      statement.setString(8, commission.currency());
      if (commission.splitPercentage() != null) {
        statement.setBigDecimal(9, commission.splitPercentage());
      } else {
        statement.setNull(9, Types.NUMERIC);
      }
      statement.setString(10, commission.calculationDetails());
      statement.setTimestamp(11,
          commission.paidAt() != null ? Timestamp.from(commission.paidAt()) : null);
      statement.setString(12, commission.withheldReason());
      statement.setString(13, commission.metadata());
      statement.executeUpdate();
      return findByIdAndOrganizationId(commission.organizationId(), commission.commissionId())
          .orElseThrow(() -> new IllegalStateException("commission not readable after create"));
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to create commission", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public Optional<Commission> findByIdAndOrganizationId(
      UUID organizationId, CommissionId commissionId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(commissionId, "commissionId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, commissionId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(toCommission(resultSet));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find commission by id", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Commission> findByPlacementIdAndOrganizationId(
      UUID organizationId, PlacementId placementId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(placementId, "placementId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_PLACEMENT_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, placementId.value());
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Commission> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCommission(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find commissions by placement", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  @Override
  public List<Commission> findByConsultantIdAndOrganizationId(
      UUID organizationId, UUID consultantId) {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(consultantId, "consultantId must not be null");
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(FIND_BY_CONSULTANT_SQL)) {
      statement.setObject(1, organizationId);
      statement.setObject(2, consultantId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<Commission> results = new ArrayList<>();
        while (resultSet.next()) {
          results.add(toCommission(resultSet));
        }
        return List.copyOf(results);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to find commissions by consultant", exception);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private static Commission toCommission(ResultSet rs) throws SQLException {
    OffsetDateTime paidAtOdt = rs.getObject("paid_at", OffsetDateTime.class);
    BigDecimal amount = rs.getBigDecimal("amount");
    BigDecimal splitPct = rs.getBigDecimal("split_percentage");
    return Commission.builder()
        .commissionId(new CommissionId(rs.getObject("commission_id", UUID.class)))
        .organizationId(rs.getObject("organization_id", UUID.class))
        .placementId(new PlacementId(rs.getObject("placement_id", UUID.class)))
        .consultantId(rs.getObject("consultant_id", UUID.class))
        .status(CommissionStatus.fromWireValue(rs.getString("status")))
        .commissionType(CommissionType.fromWireValue(rs.getString("commission_type")))
        .amount(amount)
        .currency(rs.getString("currency"))
        .splitPercentage(splitPct)
        .calculationDetails(rs.getString("calculation_details"))
        .paidAt(paidAtOdt != null ? paidAtOdt.toInstant() : null)
        .withheldReason(rs.getString("withheld_reason"))
        .metadata(rs.getString("metadata"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class).toInstant())
        .updatedAt(rs.getObject("updated_at", OffsetDateTime.class).toInstant())
        .version(rs.getInt("version"))
        .build();
  }
}
