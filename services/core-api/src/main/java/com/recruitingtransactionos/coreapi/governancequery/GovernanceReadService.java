package com.recruitingtransactionos.coreapi.governancequery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskDefinition;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskDefinitionRegistry;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRoute;
import com.recruitingtransactionos.coreapi.aitaskrunner.AITaskModelRouter;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceItemResponse;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceMetricResponse;
import com.recruitingtransactionos.coreapi.apiboundary.GovernanceSectionResponse;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigRecord;
import com.recruitingtransactionos.coreapi.governanceconfig.GovernanceConfigService;
import com.recruitingtransactionos.coreapi.workflowautomation.WorkflowAutomationPolicy;
import com.recruitingtransactionos.coreapi.workflowautomation.WorkflowAutomationRule;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public final class GovernanceReadService {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final DataSource dataSource;
  private final GovernanceConfigService governanceConfigService;
  private final ObjectMapper objectMapper;
  private final AITaskDefinitionRegistry aiTaskDefinitionRegistry;
  private final AITaskModelRouter aiTaskModelRouter;

  public GovernanceReadService(
      DataSource dataSource,
      GovernanceConfigService governanceConfigService,
      ObjectMapper objectMapper,
      AITaskDefinitionRegistry aiTaskDefinitionRegistry,
      AITaskModelRouter aiTaskModelRouter) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    this.governanceConfigService = Objects.requireNonNull(
        governanceConfigService,
        "governanceConfigService must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.aiTaskDefinitionRegistry = Objects.requireNonNull(
        aiTaskDefinitionRegistry,
        "aiTaskDefinitionRegistry must not be null");
    this.aiTaskModelRouter = Objects.requireNonNull(aiTaskModelRouter, "aiTaskModelRouter must not be null");
  }

  public GovernanceSectionResponse loadOwnerSection(UUID organizationId, String sectionKey) {
    return switch (normalize(sectionKey)) {
      case "dashboard" -> ownerDashboard(organizationId);
      case "pipeline" -> ownerPipeline(organizationId);
      case "consultants" -> groupedCountSection(
          organizationId,
          "consultants",
          "Consultant Performance",
          "Live consultant leaderboard based on commission activity.",
          "SELECT consultant_id::text AS key, COUNT(*) AS total FROM recruiting.commission WHERE organization_id = ? GROUP BY consultant_id ORDER BY total DESC LIMIT 10",
          "consultant");
      case "clients" -> groupedCountSection(
          organizationId,
          "clients",
          "Client Exposure",
          "Top client companies by placement volume.",
          "SELECT company_id::text AS key, COUNT(*) AS total FROM recruiting.placement WHERE organization_id = ? GROUP BY company_id ORDER BY total DESC LIMIT 10",
          "company");
      case "risk" -> ownerRisk(organizationId);
      case "data-quality" -> ownerDataQuality(organizationId);
      case "ai-quality" -> ownerAiQuality(organizationId);
      case "audit" -> auditSection(organizationId, "audit", "Owner Audit Search", false, "owner/audit");
      default -> throw new IllegalArgumentException("unknown_owner_governance_section");
    };
  }

  public GovernanceSectionResponse loadAdminSection(UUID organizationId, String sectionKey) {
    String normalized = normalize(sectionKey);
    return switch (normalized) {
      case "review-quality" -> reviewQuality(organizationId);
      case "claim-ledger" -> claimLedger(organizationId);
      case "ontology-governance" -> configBackedSection(
          organizationId,
          normalized,
          "Ontology Governance",
          "Industry packs, ontology freshness, and admin annotations.",
          industryPackMetrics(organizationId),
          groupedCountItems(
              organizationId,
              "SELECT pack_key AS key, COUNT(*) AS total FROM recruiting.industry_pack WHERE ?::uuid IS NOT NULL GROUP BY pack_key ORDER BY key ASC LIMIT 10",
              "pack"),
          false);
      case "privacy-redaction" -> privacyRedaction(organizationId);
      case "model-routing" -> modelRouting(organizationId);
      case "eval-feedback" -> configBackedSection(
          organizationId,
          normalized,
          "Eval Feedback",
          "Feedback queue and adjudication notes for AI evaluations.",
          List.of(metric("entries", "Config Entries", String.valueOf(governanceConfigService.list(organizationId, normalized).size()), "info", "Stored governance annotations")),
          List.of(),
          false);
      case "ai-policy" -> configBackedSection(
          organizationId,
          normalized,
          "AI Policy",
          "Runtime policy toggles and operational guidance for AI surfaces.",
          aiPolicyMetrics(organizationId),
          List.of(),
          false);
      case "ai-task-registry" -> aiTaskRegistry(organizationId);
      case "industry-packs" -> configBackedSection(
          organizationId,
          normalized,
          "Industry Packs",
          "Pack inventory and governance metadata.",
          industryPackMetrics(organizationId),
          groupedCountItems(
              organizationId,
              "SELECT pack_key AS key, COUNT(*) AS total FROM recruiting.industry_pack WHERE ?::uuid IS NOT NULL GROUP BY pack_key ORDER BY key ASC LIMIT 10",
              "pack"),
          false);
      case "schema" -> schemaCatalog(organizationId);
      case "workflow-rules" -> workflowRules(organizationId);
      case "permissions" -> permissions(organizationId);
      case "audit-log" -> auditSection(organizationId, normalized, "Security Audit Log", false, "admin/audit-log");
      case "integrations" -> integrations(organizationId);
      case "security" -> security(organizationId);
      default -> throw new IllegalArgumentException("unknown_admin_governance_section");
    };
  }

  private GovernanceSectionResponse ownerDashboard(UUID organizationId) {
    List<GovernanceMetricResponse> metrics = new ArrayList<>();
    metrics.add(metric("placements", "Placements", count(organizationId, "SELECT COUNT(*) FROM recruiting.placement WHERE organization_id = ?"), "info", "Current placement records"));
    metrics.add(metric("expectedFee", "Expected Fee", sumCurrency(organizationId, "SELECT COALESCE(SUM((offer_details ->> 'expectedFeeAmount')::numeric), 0) FROM recruiting.placement WHERE organization_id = ?"), "success", "Offer details expected fee total"));
    metrics.add(metric("paidCommissions", "Paid Commissions", count(organizationId, "SELECT COUNT(*) FROM recruiting.commission WHERE organization_id = ? AND status = 'paid'"), "success", "Paid commission records"));
    metrics.add(metric("workflowEvents", "7d Workflow Events", count(organizationId, "SELECT COUNT(*) FROM workflow.workflow_event WHERE organization_id = ? AND occurred_at >= now() - interval '7 days'"), "info", "Recent state transitions"));
    return new GovernanceSectionResponse(
        "dashboard",
        "Owner Dashboard",
        "Organization-level operating summary across placements, fees, and workflow activity.",
        metrics,
        groupedCountItems(
            organizationId,
            "SELECT status AS key, COUNT(*) AS total FROM recruiting.placement WHERE organization_id = ? GROUP BY status ORDER BY total DESC LIMIT 10",
            "status"),
        List.of(),
        false,
        "{}",
        "");
  }

  private GovernanceSectionResponse ownerPipeline(UUID organizationId) {
    return new GovernanceSectionResponse(
        "pipeline",
        "Pipeline and Revenue",
        "Placement, guarantee, and commission pipeline view for owners.",
        List.of(
            metric("pendingCommission", "Pending Commission", count(organizationId, "SELECT COUNT(*) FROM recruiting.commission WHERE organization_id = ? AND status = 'pending'"), "warning", "Commission awaiting payment"),
            metric("invoiceReady", "Invoice Ready", count(organizationId, "SELECT COUNT(*) FROM recruiting.placement WHERE organization_id = ? AND status = 'invoice_ready'"), "info", "Placement ready for invoicing"),
            metric("guaranteeActive", "Active Guarantee", count(organizationId, "SELECT COUNT(*) FROM recruiting.placement WHERE organization_id = ? AND status = 'guarantee_active'"), "info", "Guarantee monitoring window"),
            metric("replacementRequired", "Replacement Required", count(organizationId, "SELECT COUNT(*) FROM recruiting.placement WHERE organization_id = ? AND status = 'replacement_required'"), "danger", "Placements requiring recovery")),
        groupedCountItems(
            organizationId,
            "SELECT status AS key, COUNT(*) AS total FROM recruiting.commission WHERE organization_id = ? GROUP BY status ORDER BY total DESC LIMIT 10",
            "commission"),
        List.of(),
        false,
        "{}",
        "");
  }

  private GovernanceSectionResponse ownerRisk(UUID organizationId) {
    return new GovernanceSectionResponse(
        "risk",
        "Risk Dashboard",
        "Review fatigue, audit pressure, and privacy risk indicators.",
        List.of(
            metric("bulkApprove", "Bulk Approve Reviews", count(organizationId, "SELECT COUNT(*) FROM governance.review_event WHERE organization_id = ? AND bulk_flag = true"), "warning", "Bulk-reviewed decisions"),
            metric("sampledAudit", "Sample Audit Queue", count(organizationId, "SELECT COUNT(*) FROM governance.review_event WHERE organization_id = ? AND status IN ('sampled_for_audit','failed_audit')"), "danger", "Audits that need attention"),
            metric("reidIncidents", "Re-identification Incidents", count(organizationId, "SELECT COUNT(*) FROM privacy.reidentification_risk_assessment WHERE organization_id = ? AND decision <> 'allow'"), "danger", "Privacy gating blocks"),
            metric("canonicalRejects", "Rejected Writes", count(organizationId, "SELECT COUNT(*) FROM governance.canonical_write_attempt WHERE organization_id = ? AND decision <> 'allow'"), "warning", "Canonical write denials")),
        recentReviewItems(organizationId),
        List.of("Owner remains read-only on all governance surfaces."),
        false,
        "{}",
        "");
  }

  private GovernanceSectionResponse ownerDataQuality(UUID organizationId) {
    return new GovernanceSectionResponse(
        "data-quality",
        "Data Quality",
        "Canonical write quality, stale facts, and missing financial metadata.",
        List.of(
            metric("unknownExpectedFee", "Unknown Expected Fee", count(organizationId, "SELECT COUNT(*) FROM recruiting.placement WHERE organization_id = ? AND (offer_details ->> 'expectedFeeAmount') IS NULL"), "warning", "Placements missing expected fee"),
            metric("failedAudit", "Failed Audit", count(organizationId, "SELECT COUNT(*) FROM governance.review_event WHERE organization_id = ? AND status = 'failed_audit'"), "danger", "Failed review samples"),
            metric("staleIndustryPacks", "Industry Pack Rows", count(organizationId, "SELECT COUNT(*) FROM recruiting.industry_pack WHERE ?::uuid IS NOT NULL"), "info", "Pack inventory for ontology drift review"),
            metric("claimItems", "Claim Ledger Items", count(organizationId, "SELECT COUNT(*) FROM governance.claim_ledger_item WHERE organization_id = ?"), "info", "Claim-ledger evidence volume")),
        claimLedgerItems(organizationId),
        List.of(),
        false,
        "{}",
        "");
  }

  private GovernanceSectionResponse ownerAiQuality(UUID organizationId) {
    return new GovernanceSectionResponse(
        "ai-quality",
        "AI Quality",
        "Task health, failure rate, human review pressure, and active routing coverage.",
        List.of(
            metric("taskRuns", "AI Task Runs", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ?"), "info", "Recorded AI task executions"),
            metric("failedRuns", "Failed Runs", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ? AND status = 'failed'"), "danger", "Runs with failed status"),
            metric("humanReview", "Human Review Required", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ? AND human_review_status = 'required'"), "warning", "Runs gated for human review"),
            metric("routes", "Configured Routes", String.valueOf(governanceConfigService.list(organizationId, "model-routing").size()), "info", "Saved model routing overlays")),
        aiTaskItems(organizationId),
        List.of(),
        false,
        "{}",
        "");
  }

  private GovernanceSectionResponse reviewQuality(UUID organizationId) {
    String totalReviews = count(organizationId, "SELECT COUNT(*) FROM governance.review_event WHERE organization_id = ?");
    String bulkReviews = count(organizationId, "SELECT COUNT(*) FROM governance.review_event WHERE organization_id = ? AND bulk_flag = true");
    return configBackedSection(
        organizationId,
        "review-quality",
        "Review Quality",
        "Review velocity, bulk approve pressure, and sample audit queue.",
        List.of(
            metric("totalReviews", "Total Reviews", totalReviews, "info", "All review events"),
            metric("bulkReviews", "Bulk Approve Reviews", bulkReviews, "warning", "High-risk bulk decisions"),
            metric("sampleQueue", "Sample Audit Queue", count(organizationId, "SELECT COUNT(*) FROM governance.review_event WHERE organization_id = ? AND status IN ('sampled_for_audit','failed_audit')"), "danger", "Items queued for audit"),
            metric("bulkRatio", "Bulk Approve Ratio", ratio(organizationId, "SELECT COUNT(*) FILTER (WHERE bulk_flag) AS numerator, COUNT(*) AS denominator FROM governance.review_event WHERE organization_id = ?"), "warning", "Bulk approvals over all reviews")),
        recentReviewItems(organizationId),
        false);
  }

  private GovernanceSectionResponse claimLedger(UUID organizationId) {
    return configBackedSection(
        organizationId,
        "claim-ledger",
        "Claim Ledger",
        "Recent claim evidence, verification status, and write-back relevance.",
        List.of(
            metric("claims", "Claims", count(organizationId, "SELECT COUNT(*) FROM governance.claim_ledger_item WHERE organization_id = ?"), "info", "Claim ledger rows"),
            metric("verified", "Verified Claims", count(organizationId, "SELECT COUNT(*) FROM governance.claim_ledger_item WHERE organization_id = ? AND verification_status IN ('candidate_confirmed','external_verified')"), "success", "Verified claims"),
            metric("candidateVisible", "Client-safe Claims", count(organizationId, "SELECT COUNT(*) FROM governance.claim_ledger_item WHERE organization_id = ? AND client_shareability <> 'internal_only'"), "info", "Claims safe beyond internal-only"),
            metric("writeAllowed", "Write Allowed", count(organizationId, "SELECT COUNT(*) FROM governance.claim_ledger_item WHERE organization_id = ? AND canonical_write_allowed = true"), "success", "Claims eligible for write-back")),
        claimLedgerItems(organizationId),
        false);
  }

  private GovernanceSectionResponse privacyRedaction(UUID organizationId) {
    return configBackedSection(
        organizationId,
        "privacy-redaction",
        "Privacy Redaction",
        "Stored policy overlays plus recent re-identification assessments.",
        List.of(
            metric("assessments", "Assessments", count(organizationId, "SELECT COUNT(*) FROM privacy.reidentification_risk_assessment WHERE organization_id = ?"), "info", "Recorded privacy assessments"),
            metric("blocked", "Blocked Decisions", count(organizationId, "SELECT COUNT(*) FROM privacy.reidentification_risk_assessment WHERE organization_id = ? AND decision <> 'allow'"), "danger", "Assessments that blocked disclosure"),
            metric("highRisk", "High Risk", count(organizationId, "SELECT COUNT(*) FROM privacy.reidentification_risk_assessment WHERE organization_id = ? AND risk_level IN ('high','critical')"), "danger", "High/critical re-identification risk"),
            metric("policies", "Stored Policies", String.valueOf(governanceConfigService.list(organizationId, "privacy-redaction").size()), "info", "Saved redaction policy overlays")),
        privacyItems(organizationId),
        false);
  }

  private GovernanceSectionResponse modelRouting(UUID organizationId) {
    return configBackedSection(
        organizationId,
        "model-routing",
        "Model Routing",
        "Task registry and persisted model route overrides.",
        List.of(
            metric("definitions", "Task Definitions", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_definition WHERE organization_id = ?"), "info", "Persisted AI task definitions"),
            metric("activeDefinitions", "Active Definitions", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_definition WHERE organization_id = ? AND status = 'active'"), "success", "Active AI task definitions"),
            metric("routeOverrides", "Route Overrides", String.valueOf(governanceConfigService.list(organizationId, "model-routing").size()), "info", "Stored route overlays"),
            metric("failedRuns", "Failed Runs", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ? AND status = 'failed'"), "danger", "Failed executions to triage")),
        aiTaskItems(organizationId),
        true);
  }

  private GovernanceSectionResponse aiTaskRegistry(UUID organizationId) {
    int definitionCount = aiTaskDefinitionRegistry.definitions().size();
    return configBackedSection(
        organizationId,
        "ai-task-registry",
        "AI Task Registry",
        "Production AI task definitions, schema/prompt/eval governance, and run history coverage.",
        List.of(
            metric("taskDefinitions", "Definitions", String.valueOf(definitionCount), "info", "v2.1 production task definitions"),
            metric("activeDefinitions", "Active", String.valueOf(definitionCount), "success", "Registry definitions available for governance"),
            metric("runVolume", "Run Volume", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ?"), "info", "Recorded AI runs"),
            metric("errorVolume", "Failures", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ? AND status = 'failed'"), "danger", "Failed AI runs")),
        aiTaskDefinitionItems(organizationId),
        false);
  }

  private GovernanceSectionResponse schemaCatalog(UUID organizationId) {
    List<GovernanceItemResponse> items = List.of(
        item("claim-ledger.schema.json", "packages/contracts/schemas", "available", "Contract schema asset", "admin/schema"),
        item("candidate-profile.schema.json", "packages/contracts/schemas", "available", "Contract schema asset", "admin/schema"),
        item("match-report.schema.json", "packages/contracts/schemas", "available", "Contract schema asset", "admin/schema"));
    return configBackedSection(
        organizationId,
        "schema",
        "Schema Catalog",
        "Known contract schemas exposed to governance users.",
        List.of(metric("schemaCount", "Known Schemas", String.valueOf(items.size()), "info", "Curated contract schema list")),
        items,
        false);
  }

  private GovernanceSectionResponse workflowRules(UUID organizationId) {
    WorkflowAutomationPolicy automationPolicy = WorkflowAutomationPolicy.standard();
    List<WorkflowAutomationRule> rules = automationPolicy.rules();
    return configBackedSection(
        organizationId,
        "workflow-rules",
        "Workflow Rules",
        "Built-in transition rules plus Task 45 SLA, reminder, escalation, and next-best-action automation coverage.",
        List.of(
            metric("overlays", "Stored Overlays", String.valueOf(governanceConfigService.list(organizationId, "workflow-rules").size()), "info", "Workflow rule overrides"),
            metric("slaRules", "SLA Rules", String.valueOf(rules.size()), "success", "Task 45 workflow automation rules"),
            metric("reminderRules", "Reminder Rules", String.valueOf(rules.size()), "success", "Rules with due-date reminders")),
        rules.stream()
            .map(rule -> item(
                rule.label(),
                rule.workflowFamily() + " owner:" + rule.ownerRole().wireValue(),
                "active",
                "action:" + rule.actionCode()
                    + " due:" + rule.dueAfter()
                    + " reminder:" + rule.reminderAfter()
                    + " escalation:" + rule.escalationAfter()
                    + " nextBestAction:" + rule.nextBestAction(),
                "admin/workflow-rules"))
            .toList(),
        false);
  }

  private GovernanceSectionResponse permissions(UUID organizationId) {
    return configBackedSection(
        organizationId,
        "permissions",
        "Permissions",
        "Governance-owned permission overlays for admin-controlled surfaces.",
        List.of(metric("overlays", "Stored Overlays", String.valueOf(governanceConfigService.list(organizationId, "permissions").size()), "info", "Permission overlay records")),
        List.of(item("admin_governance", "same organization", "read-only", "Runtime permission overlays are not active; built-in policy remains authoritative.", "admin/permissions")),
        false);
  }

  private GovernanceSectionResponse integrations(UUID organizationId) {
    return configBackedSection(
        organizationId,
        "integrations",
        "Integrations",
        "External channel readiness for email, SMS, calendar, ATS, HRIS, speech-to-text, and OCR.",
        List.of(
            metric("auditRows", "Integration Audit Rows", count(organizationId, "SELECT COUNT(*) FROM audit.audit_log WHERE organization_id = ? AND target_entity_type IN ('integration','notification','document')"), "info", "Recent integration-adjacent audit records"),
            metric("documentSources", "Document Sources", count(organizationId, "SELECT COUNT(*) FROM intake.source_item WHERE organization_id = ?"), "info", "Uploaded source items available to document intelligence"),
            metric("notifications", "Notifications", count(organizationId, "SELECT COUNT(*) FROM operations.notification WHERE organization_id = ?"), "info", "Portal notifications generated by workflow surfaces"),
            metric("aiTaskRuns", "AI Task Runs", count(organizationId, "SELECT COUNT(*) FROM governance.ai_task_run WHERE organization_id = ?"), "info", "AI and tool execution records")),
        List.of(
            item("email", "notification channel", "read-only", "Runtime provider wiring is deferred to the production integration task; portal notifications remain backend-owned.", "admin/integrations"),
            item("calendar", "interview coordination", "read-only", "Calendar provider setup remains future production integration scope.", "admin/integrations"),
            item("ATS / HRIS", "external system sync", "read-only", "No external ATS or HRIS is configured as a source of truth.", "admin/integrations"),
            item("OCR / speech-to-text", "document intelligence", "read-only", "Document source items and AI task runs expose current ingestion activity.", "admin/integrations")),
        false);
  }

  private GovernanceSectionResponse security(UUID organizationId) {
    return configBackedSection(
        organizationId,
        "security",
        "Security",
        "Recent audit log activity and governance configuration pressure.",
        List.of(
            metric("auditRows", "Audit Log Rows", count(organizationId, "SELECT COUNT(*) FROM audit.audit_log WHERE organization_id = ?"), "info", "Security and governance log rows"),
            metric("deniedWrites", "Denied Write Attempts", count(organizationId, "SELECT COUNT(*) FROM governance.canonical_write_attempt WHERE organization_id = ? AND decision <> 'allow'"), "warning", "Canonical write denials"),
            metric("adminConfigs", "Admin Config Entries", count(organizationId, "SELECT COUNT(*) FROM governance.config_entry WHERE organization_id = ?"), "info", "Persisted governance config entries"),
            metric("workflowEvents", "Workflow Events", count(organizationId, "SELECT COUNT(*) FROM workflow.workflow_event WHERE organization_id = ?"), "info", "Total workflow history")),
        auditItems(organizationId),
        false);
  }

  private GovernanceSectionResponse auditSection(
      UUID organizationId,
      String sectionKey,
      String title,
      boolean editable,
      String routePrefix) {
    return new GovernanceSectionResponse(
        sectionKey,
        title,
        "Recent workflow and security audit events.",
        List.of(
            metric("workflowEvents", "Workflow Events", count(organizationId, "SELECT COUNT(*) FROM workflow.workflow_event WHERE organization_id = ?"), "info", "All workflow events"),
            metric("auditLogs", "Audit Logs", count(organizationId, "SELECT COUNT(*) FROM audit.audit_log WHERE organization_id = ?"), "info", "Security audit rows")),
        auditItems(organizationId),
        List.of(),
        editable,
        editable ? configJson(organizationId, sectionKey) : "{}",
        configUpdatedAt(organizationId, sectionKey));
  }

  private GovernanceSectionResponse configBackedSection(
      UUID organizationId,
      String sectionKey,
      String title,
      String description,
      List<GovernanceMetricResponse> metrics,
      List<GovernanceItemResponse> items,
      boolean editable) {
    return new GovernanceSectionResponse(
        sectionKey,
        title,
        description,
        metrics,
        items,
        List.of(),
        editable,
        editable ? configJson(organizationId, sectionKey) : "{}",
        configUpdatedAt(organizationId, sectionKey));
  }

  private List<GovernanceMetricResponse> industryPackMetrics(UUID organizationId) {
    return List.of(metric("packCount", "Industry Packs", count(organizationId, "SELECT COUNT(*) FROM recruiting.industry_pack WHERE ?::uuid IS NOT NULL"), "info", "Stored pack rows"));
  }

  private List<GovernanceMetricResponse> aiPolicyMetrics(UUID organizationId) {
    return List.of(metric("policyEntries", "Policy Entries", String.valueOf(governanceConfigService.list(organizationId, "ai-policy").size()), "info", "Stored AI policy configs"));
  }

  private List<GovernanceItemResponse> privacyItems(UUID organizationId) {
    return limitedItems(
        organizationId,
        "SELECT candidate_card_id, decision, risk_level, explanation FROM privacy.reidentification_risk_assessment WHERE organization_id = ? ORDER BY recorded_at DESC LIMIT 10",
        resultSet -> item(
            resultSet.getString(1),
            resultSet.getString(2),
            resultSet.getString(3),
            resultSet.getString(4),
            "admin/privacy-redaction"));
  }

  private List<GovernanceItemResponse> claimLedgerItems(UUID organizationId) {
    return limitedItems(
        organizationId,
        "SELECT COALESCE(target_field_path, '<unknown>'), verification_status::text, client_shareability::text, COALESCE(entity_type, '<unknown>') FROM governance.claim_ledger_item WHERE organization_id = ? ORDER BY created_at DESC LIMIT 10",
        resultSet -> item(
            resultSet.getString(1),
            resultSet.getString(4),
            resultSet.getString(2),
            resultSet.getString(3),
            "admin/claim-ledger"));
  }

  private List<GovernanceItemResponse> recentReviewItems(UUID organizationId) {
    return limitedItems(
        organizationId,
        "SELECT field_path, status, review_velocity_bucket, COALESCE(reason, '') FROM governance.review_event WHERE organization_id = ? ORDER BY created_at DESC LIMIT 10",
        resultSet -> item(
            resultSet.getString(1),
            resultSet.getString(3),
            resultSet.getString(2),
            resultSet.getString(4),
            "admin/review-quality"));
  }

  private List<GovernanceItemResponse> aiTaskItems(UUID organizationId) {
    return limitedItems(
        organizationId,
        "SELECT definition.task_key, run.status, COALESCE(run.error_code, ''), COALESCE(run.model_provider || '/' || run.model_name, '') FROM governance.ai_task_run run JOIN governance.ai_task_definition definition ON definition.ai_task_definition_id = run.ai_task_definition_id WHERE run.organization_id = ? ORDER BY run.created_at DESC LIMIT 10",
        resultSet -> item(
            resultSet.getString(1),
            resultSet.getString(4),
            resultSet.getString(2),
            resultSet.getString(3),
            "admin/ai-task-registry"));
  }

  private List<GovernanceItemResponse> aiTaskDefinitionItems(UUID organizationId) {
    Map<String, AITaskRunStats> runStats = aiTaskRunStats(organizationId);
    List<GovernanceItemResponse> items = new ArrayList<>();
    for (AITaskDefinition definition : aiTaskDefinitionRegistry.definitions()) {
      AITaskModelRoute route = aiTaskModelRouter.routeFor(organizationId, definition.taskKey());
      AITaskRunStats stats = runStats.getOrDefault(definition.taskKey(), AITaskRunStats.empty());
      items.add(item(
          definition.displayName(),
          "Task " + definition.registryTaskId()
              + " | " + definition.taskVersion()
              + " | " + route.providerKey() + "/" + route.modelName(),
          "active",
          "schema:" + definition.inputSchemaResourcePath()
              + " -> " + definition.outputSchemaResourcePath()
              + " | prompt:" + definition.promptVersion()
              + " | review:" + definition.humanReviewStatus().wireValue()
              + " | writeBack:" + definition.writeBackTarget().wireValue()
              + " | eval:" + definition.evalSuiteResourcePath()
              + " | evalResult:registered"
              + " | runs:" + stats.runCount()
              + " failures:" + stats.failureCount()
              + " cost:" + stats.costUnits()
              + " latencyMs:" + stats.latencyMs()
              + " replayHistory:" + stats.replayCount(),
          "admin/ai-task-registry"));
    }
    return List.copyOf(items);
  }

  private Map<String, AITaskRunStats> aiTaskRunStats(UUID organizationId) {
    String sql = """
        SELECT
          definition.task_key,
          COUNT(run.ai_task_run_id) AS run_count,
          COUNT(run.ai_task_run_id) FILTER (WHERE run.status = 'failed') AS failure_count,
          COALESCE(SUM(run.cost_units), 0) AS cost_units,
          ROUND(AVG(EXTRACT(EPOCH FROM (run.completed_at - run.started_at)) * 1000))::bigint AS latency_ms,
          COUNT(run.ai_task_run_id) FILTER (WHERE run.replayed_from_ai_task_run_id IS NOT NULL) AS replay_count
        FROM governance.ai_task_definition definition
        LEFT JOIN governance.ai_task_run run
          ON run.organization_id = definition.organization_id
          AND run.ai_task_definition_id = definition.ai_task_definition_id
        WHERE definition.organization_id = ?
        GROUP BY definition.task_key
        """;
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        Map<String, AITaskRunStats> stats = new LinkedHashMap<>();
        while (resultSet.next()) {
          stats.put(resultSet.getString("task_key"), new AITaskRunStats(
              resultSet.getLong("run_count"),
              resultSet.getLong("failure_count"),
              resultSet.getBigDecimal("cost_units") == null
                  ? "0"
                  : resultSet.getBigDecimal("cost_units").toPlainString(),
              resultSet.getObject("latency_ms") == null ? "n/a" : String.valueOf(resultSet.getLong("latency_ms")),
              resultSet.getLong("replay_count")));
        }
        return Map.copyOf(stats);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load AI task run stats", exception);
    }
  }

  private List<GovernanceItemResponse> auditItems(UUID organizationId) {
    return limitedItems(
        organizationId,
        "SELECT action, target_entity_type, result, COALESCE(reason, '') FROM audit.audit_log WHERE organization_id = ? ORDER BY occurred_at DESC LIMIT 10",
        resultSet -> item(
            resultSet.getString(1),
            resultSet.getString(2),
            resultSet.getString(3),
            resultSet.getString(4),
            "admin/audit-log"));
  }

  private GovernanceSectionResponse groupedCountSection(
      UUID organizationId,
      String sectionKey,
      String title,
      String description,
      String sql,
      String noun) {
    List<GovernanceItemResponse> items = groupedCountItems(organizationId, sql, noun);
    return new GovernanceSectionResponse(
        sectionKey,
        title,
        description,
        List.of(metric("topCount", "Visible Rows", String.valueOf(items.size()), "info", "Grouped rows")),
        items,
        List.of(),
        false,
        "{}",
        "");
  }

  private List<GovernanceItemResponse> groupedCountItems(UUID organizationId, String sql, String noun) {
    return limitedItems(
        organizationId,
        sql,
        resultSet -> item(
            resultSet.getString(1),
            noun,
            "visible",
            String.valueOf(resultSet.getLong(2)),
            noun));
  }

  private List<GovernanceItemResponse> limitedItems(
      UUID organizationId,
      String sql,
      ResultMapper<GovernanceItemResponse> mapper) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<GovernanceItemResponse> items = new ArrayList<>();
        while (resultSet.next()) {
          items.add(mapper.map(resultSet));
        }
        return List.copyOf(items);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to load governance items", exception);
    }
  }

  private GovernanceMetricResponse metric(String key, String label, String value, String severity, String helperText) {
    return new GovernanceMetricResponse(key, label, value, severity, helperText);
  }

  private GovernanceItemResponse item(String primary, String secondary, String status, String detail, String route) {
    return new GovernanceItemResponse(primary, secondary, status, detail, route);
  }

  private String count(UUID organizationId, String sql) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return String.valueOf(resultSet.getLong(1));
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to count governance rows", exception);
    }
  }

  private String sumCurrency(UUID organizationId, String sql) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        BigDecimal value = resultSet.getBigDecimal(1);
        if (value == null) {
          return "0";
        }
        return value.toPlainString();
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to sum governance value", exception);
    }
  }

  private String ratio(UUID organizationId, String sql) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, organizationId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        long numerator = resultSet.getLong("numerator");
        long denominator = resultSet.getLong("denominator");
        if (denominator == 0) {
          return "0.0%";
        }
        double ratio = (numerator * 100.0d) / denominator;
        return String.format(Locale.ROOT, "%.1f%%", ratio);
      }
    } catch (SQLException exception) {
      throw new IllegalStateException("Failed to calculate governance ratio", exception);
    }
  }

  private String configJson(UUID organizationId, String sectionKey) {
    return governanceConfigService.find(organizationId, sectionKey, "default")
        .map(GovernanceConfigRecord::payloadJson)
        .orElse("{}");
  }

  private String configUpdatedAt(UUID organizationId, String sectionKey) {
    Optional<GovernanceConfigRecord> record = governanceConfigService.find(organizationId, sectionKey, "default");
    return record.map(value -> TIMESTAMP_FORMATTER.format(OffsetDateTime.ofInstant(value.updatedAt(), OffsetDateTime.now().getOffset()))).orElse("");
  }

  private static String normalize(String sectionKey) {
    if (sectionKey == null || sectionKey.isBlank()) {
      throw new IllegalArgumentException("sectionKey must not be blank");
    }
    return sectionKey.strip().toLowerCase(Locale.ROOT);
  }

  @FunctionalInterface
  private interface ResultMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
  }

  private record AITaskRunStats(
      long runCount,
      long failureCount,
      String costUnits,
      String latencyMs,
      long replayCount) {

    private static AITaskRunStats empty() {
      return new AITaskRunStats(0, 0, "0", "n/a", 0);
    }
  }
}
