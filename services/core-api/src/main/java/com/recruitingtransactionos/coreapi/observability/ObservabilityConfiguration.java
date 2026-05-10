package com.recruitingtransactionos.coreapi.observability;

import com.recruitingtransactionos.coreapi.consentdisclosure.port.ConsentRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.DisclosureRecordPort;
import com.recruitingtransactionos.coreapi.consentdisclosure.port.UnlockDecisionPort;
import com.recruitingtransactionos.coreapi.accessaudit.AccessAuditSearchReader;
import com.recruitingtransactionos.coreapi.truthlayer.port.ActorRole;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowAuditQuery;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCausationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowCorrelationId;
import com.recruitingtransactionos.coreapi.truthlayer.port.WorkflowEventId;
import com.recruitingtransactionos.coreapi.truthlayer.service.WorkflowAuditQueryService;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ObservabilityConfiguration {

  @Bean
  @ConditionalOnMissingBean(RequestCorrelationFilter.class)
  RequestCorrelationFilter requestCorrelationFilter() {
    return new RequestCorrelationFilter();
  }

  @Bean
  @ConditionalOnMissingBean(ObservabilityReviewEventReader.class)
  ObservabilityReviewEventReader observabilityReviewEventReader(DataSource dataSource) {
    return new JdbcObservabilityReviewEventReader(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(ObservabilityAITaskRunReader.class)
  ObservabilityAITaskRunReader observabilityAITaskRunReader(DataSource dataSource) {
    return new JdbcObservabilityAITaskRunReader(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(ObservabilityWorkflowEventReader.class)
  ObservabilityWorkflowEventReader observabilityWorkflowEventReader(
      WorkflowAuditQueryService workflowAuditQueryService) {
    return query -> workflowAuditQueryService.search(toWorkflowAuditQuery(query));
  }

  @Bean
  @ConditionalOnMissingBean(ObservabilityDisclosureRecordReader.class)
  ObservabilityDisclosureRecordReader observabilityDisclosureRecordReader(
      DisclosureRecordPort disclosureRecordPort) {
    return disclosureRecordPort::findByRefAndOrganizationId;
  }

  @Bean
  @ConditionalOnMissingBean(ObservabilityConsentRecordReader.class)
  ObservabilityConsentRecordReader observabilityConsentRecordReader(ConsentRecordPort consentRecordPort) {
    return consentRecordPort::findByRefAndOrganizationId;
  }

  @Bean
  @ConditionalOnMissingBean(ObservabilityUnlockDecisionReader.class)
  ObservabilityUnlockDecisionReader observabilityUnlockDecisionReader(UnlockDecisionPort unlockDecisionPort) {
    return unlockDecisionPort::findByRefAndOrganizationId;
  }

  @Bean
  @ConditionalOnMissingBean(ObservabilityReadService.class)
  ObservabilityReadService observabilityReadService(
      ObservabilityWorkflowEventReader workflowEventReader,
      ObservabilityReviewEventReader reviewEventReader,
      ObservabilityAITaskRunReader aiTaskRunReader,
      ObservabilityDisclosureRecordReader disclosureRecordReader,
      ObservabilityConsentRecordReader consentRecordReader,
      ObservabilityUnlockDecisionReader unlockDecisionReader,
      AccessAuditSearchReader accessAuditSearchReader) {
    return new ObservabilityReadService(
        workflowEventReader,
        reviewEventReader,
        aiTaskRunReader,
        disclosureRecordReader,
        consentRecordReader,
        unlockDecisionReader,
        accessAuditSearchReader);
  }

  private static WorkflowAuditQuery toWorkflowAuditQuery(ObservabilityWorkflowEventQuery query) {
    WorkflowAuditQuery.Builder builder = WorkflowAuditQuery.builder(query.organizationId());
    if (query.workflowEventId() != null) {
      builder.workflowEventId(new WorkflowEventId(query.workflowEventId()));
    }
    builder.entityType(query.entityType())
        .entityId(query.entityId())
        .actionCode(query.actionCode())
        .actorType(actorRole(query.actorType()))
        .actorId(query.actorId())
        .correlationId(correlationId(query.correlationId()))
        .causationId(causationId(query.causationId()))
        .occurredFrom(query.occurredFrom())
        .occurredTo(query.occurredTo())
        .limit(query.limit())
        .offset(query.offset());
    return builder.build();
  }

  private static ActorRole actorRole(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return ActorRole.fromWireValue(value.strip().toLowerCase(java.util.Locale.ROOT));
  }

  private static WorkflowCorrelationId correlationId(UUID value) {
    return value == null ? null : new WorkflowCorrelationId(value);
  }

  private static WorkflowCausationId causationId(UUID value) {
    return value == null ? null : new WorkflowCausationId(value);
  }
}
