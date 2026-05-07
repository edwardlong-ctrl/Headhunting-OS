# Task 37 实施计划：Owner and Admin Governance v1

## Summary

继续工作以当前 `HEAD` 为准，而不是继续做 `Task 36`。仓库当前 `git log` 已显示 `66d59e9 Complete Task 36 placement and commission v1`，但路线图与状态文档仍停留在“下一步是 Task 36”。因此本轮计划确定承接 **Task 37: Owner and Admin Governance v1**，并把滞后的 roadmap / status 文档同步纳入同一轮交付。

本轮目标不是再造一个脱离产品主链的“后台系统”，而是在现有五端门户内补齐：

- Owner 真实治理工作台：`dashboard / pipeline / consultants / clients / revenue / risk / data-quality / ai-quality / audit`
- Admin 真实治理工作台：`review-quality / claim-ledger / ontology-governance / privacy-redaction / model-routing / eval-feedback`，并扩到与规格一致的 `ai-task-registry / schema / workflow-rules / permissions / audit-log / security`
- 几乎全可配的治理写入面：用户已明确要求 `Task 37` 不是只读看板，Admin 侧应支持广泛的治理配置写入；但所有写入都必须停留在治理配置层，不得绕过 domain service 改业务事实
- 交付后同步更新 `docs/roadmap/current-engineering-snapshot.md`、`docs/roadmap/implementation-status.md`、`docs/roadmap/known-gaps.md` 等文档，消除当前 Task 36 / Task 37 漂移

## Current State Analysis

### 1. 当前真正的“继续工作”目标已经从 Task 36 切到 Task 37

已确认事实：

- `git log --oneline --decorate -n 8` 显示当前 `HEAD` / `main` 为 `66d59e9 Complete Task 36 placement and commission v1`
- `docs/roadmap/current-engineering-snapshot.md` 仍写着 `next recommended task: Task 36`
- `docs/roadmap/implementation-status.md` 也只记录到 `Task 35`

结论：

- 本轮不能再按旧文档假设“Task 36 未开始”
- 需要一边推进 Task 37，一边修正文档基线漂移

### 2. Owner Portal 已经是真实产品面，但只覆盖 Task 36 的 placements / commission / revenue 子集

已确认文件：

- `apps/web/src/features/owner-portal/OwnerPortal.tsx`
- `apps/web/src/api/ownerPlacements.ts`
- `apps/web/src/api/ownerRevenue.ts`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerPlacementController.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerPlacementQueryService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueController.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueQueryService.java`

已确认能力：

- Owner 已有真实登录、真实 API 和真实前端页面
- 现有 Owner 导航只有 `placements / commission / revenue`
- 现有 Owner 后端能力只聚焦 placement / commission / revenue source data

当前缺口：

- 没有 `dashboard / pipeline / consultants / clients / risk / data-quality / ai-quality / audit`
- 没有对 review quality、AI override、ontology stale、re-identification incidents 的组织级聚合视图

### 3. Admin 端仍是静态壳，不存在真实前后端工作台

已确认文件：

- `apps/web/src/App.tsx`
- `apps/web/src/features/` 下不存在 `admin-portal/`

已确认事实：

- `App.tsx` 当前仍把 `/admin/*` 路由到 `StaticPortal portalKey="admin"`
- 前端没有 Admin 专用 session 管理、API client、页面组件
- 后端 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/` 下没有 `admin/` 包

结论：

- Task 37 必须从零补齐 Admin Portal 的真实登录态、路由、API 边界和页面

### 4. 仓库已经有不少治理数据源，但缺少产品化的查询与配置边界

#### 4.1 已有可复用读模型 / 持久化基础

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/WorkflowAuditQueryService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/port/WorkflowAuditReadPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/persistence/JdbcWorkflowAuditReadPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/port/CanonicalWriteAttemptReadPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/persistence/JdbcCanonicalWriteAttemptReadPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/privacyredaction/port/ReidentificationRiskAssessmentPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/privacyredaction/persistence/JdbcReidentificationRiskAssessmentPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/industrypack/service/IndustryPackService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/industrypack/persistence/JdbcIndustryPackReadPort.java`
- `packages/contracts/schemas/*.json`

#### 4.2 已有但尚未产品化的治理事实源

- `governance.review_event` 表已经有 `reviewer_user_id`、`risk_tier`、`bulk_flag`、`duration_ms`、`sample_audit_status`、`review_velocity_bucket`、`status`
  - 来源：`services/core-api/src/main/resources/db/migration/V2__create_truth_layer_core_tables.sql`
- `governance.ai_task_run` 已有运行审计，但 `AITaskRunService` 目前只有 `findById(...)`
  - 文件：`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/AITaskRunService.java`
- `governance.ai_task_definition` 已由 `JdbcAITaskDefinitionCatalog` 持久化 `model_routing_policy`
  - 文件：`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/JdbcAITaskDefinitionCatalog.java`

#### 4.3 当前缺口

- 没有 ReviewEvent 的搜索读口，无法直接做 `review-quality`、`sample audit queue`
- 没有 AITaskRun / AITaskDefinition 的列表查询与聚合，无法做 `ai-quality`、`ai-task-registry`、`model-routing`
- 没有 ClaimLedger 搜索读口，无法做 `/admin/claim-ledger`
- 没有把 `packages/contracts/schemas/*.json` 暴露为产品 API 的 schema 浏览面
- 没有统一的 Owner/Admin 治理查询服务把 workflow / review / AI / privacy / ontology 聚合成产品 DTO

### 5. 权限与资源词汇已经预埋了一部分，但不足以支撑 Task 37

已确认文件：

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/ResourceType.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`

已确认事实：

- `ResourceType` 已存在 `ADMIN_GOVERNANCE`
- `FieldAccessPolicy` 对 `OWNER` 只放行 `PLACEMENT / COMMISSION / REVENUE_REPORT` 的 `READ`
- `FieldAccessPolicy` 没有 `ADMIN` 的 allow rule

结论：

- Task 37 需要为 Owner 增加更多只读治理资源
- Task 37 需要为 Admin 增加治理配置读写权限，但必须显式限制在治理配置层，不能把 Admin 变成业务事实写入旁路

### 6. 规格对 Task 37 的页面边界已经给出清晰目标

已确认规格来源：

- `docs/roadmap/productization-roadmap.zh-CN.md`
- `docs/specs/v2.1/product-spec-v2.1.md`
- `docs/specs/v2.0/product-spec-v2.0.md`

已确认的 Task 37 / Owner / Admin 页面：

- Owner：`/owner/dashboard`、`/owner/pipeline`、`/owner/consultants`、`/owner/clients`、`/owner/revenue`、`/owner/placements`、`/owner/commission`、`/owner/risk`、`/owner/data-quality`、`/owner/ai-quality`、`/owner/audit`
- Admin：`/admin/review-quality`、`/admin/claim-ledger`、`/admin/ontology-governance`、`/admin/privacy-redaction`、`/admin/model-routing`、`/admin/eval-feedback`
- 规格页还要求 Admin 主导航包含：`/admin/ai-policy`、`/admin/ai-task-registry`、`/admin/industry-packs`、`/admin/schema`、`/admin/workflow-rules`、`/admin/permissions`、`/admin/audit-log`、`/admin/security`

结论：

- Task 37 不应只做少量 KPI 卡片
- 必须把 Owner 和 Admin 两端都做成真实工作台，并保持 `v2.0/v2.1` 的五端门户结构

## Proposed Changes

### 任务 1：建立 Task 37 的治理查询与配置基础层

**目标**

先补齐 Owner/Admin 都会复用的治理数据访问层与配置基础层，避免前端直接拼多张表，也避免在 controller 中散落 SQL 或硬编码规则读取。

**涉及文件**

- 扩展现有 truth/privacy/AI 基础设施：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/TruthLayerConfiguration.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/AITaskRunService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/AITaskModelRouter.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/AITaskRunnerProperties.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/privacyredaction/port/ReidentificationRiskAssessmentPort.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/industrypack/service/IndustryPackService.java`
- 新增治理查询 / 配置目录：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governancequery/`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governanceconfig/`
  - `services/core-api/src/main/resources/db/migration/V31__add_governance_query_and_config_tables.sql`

**影响范围**

- 为以下实体补齐 search/list/readback 读口与 query service：
  - `ReviewEvent`
  - `ClaimLedgerItem`
  - `AITaskRun`
  - `AITaskDefinition`
  - `ReidentificationRiskAssessment`
  - `CanonicalWriteAttempt`
- 新增一套持久化治理配置表，承载 Admin 的“几乎全可配”要求：
  - model routing overrides
  - privacy redaction policy config
  - workflow rule overlays
  - permission policy overlays
  - eval feedback / ontology review 标注
- 保持 fail-closed：
  - 没有有效治理配置时，继续回退到当前代码 / properties 默认值
  - 配置加载失败时不得放宽业务事实边界

**验证方式**

- JDBC / Testcontainers 覆盖新增治理查询端口和配置端口
- 单测覆盖无配置回退默认值、非法配置拒绝、跨组织读取/写入拒绝

### 任务 2：把 runtime 治理配置接入真实执行边界，而不是只做“可保存但不生效”的后台

**目标**

让 Admin 写入的治理配置真正影响运行时行为，但影响范围必须局限于 AI 路由、红线策略、workflow rule overlay、permission overlay 等治理层，而不是业务事实数据。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/AITaskModelRouter.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/JdbcAITaskDefinitionCatalog.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/privacyredaction/RedactionAuditService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`
- 新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governanceconfig/service/`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governanceconfig/persistence/`

**影响范围**

- 模型路由：DB-configured route override 优先，`AITaskRunnerProperties` 作为 boot-time fallback
- 红线策略：Admin 可编辑 re-identification risk 阈值、generalization template / block rule 的治理配置层
- Workflow rules：以 overlay 方式补充或收紧 required checks / blocked transitions，默认规则仍保底
- Permissions：以 overlay 方式配置治理资源读写矩阵，但不得放开业务事实旁路
- 所有 Admin 写入都必须附带治理审计，不可 silent write

**验证方式**

- 单测覆盖 overlay precedence、非法配置 fail-closed、业务事实写入仍被拒绝
- 集成测试覆盖配置变更后查询面 / 执行面能看到一致效果

### 任务 3：新增 Owner Governance API 层和组织级聚合查询

**目标**

把当前 Owner 只读 placement/revenue 面扩展成完整的经营与治理视图，满足 Task 37 的 Owner 规格。

**涉及文件**

- 扩展现有 owner 包：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerPlacementController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerPlacementQueryService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueQueryService.java`
- 新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerDashboardController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRiskController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerAuditController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerGovernanceQueryService.java`
  - owner 相关 response DTO 到 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/`

**影响范围**

- 新增 Owner 真实接口：
  - `/api/owner/dashboard`
  - `/api/owner/pipeline`
  - `/api/owner/consultants`
  - `/api/owner/clients`
  - `/api/owner/risk`
  - `/api/owner/data-quality`
  - `/api/owner/ai-quality`
  - `/api/owner/audit`
- 聚合来源包括：
  - placements / commission / shortlist / jobs / client feedback
  - workflow audit
  - review events
  - canonical write attempts
  - AI task runs
  - redaction assessments
  - ontology stale metadata
- 明确交付 acceptance：
  - Owner 能看到 consultant `bulk approve ratio`
  - Owner 能进入 sample audit records

**验证方式**

- service 单测覆盖 KPI 聚合、未知数据处理、样本审计筛选逻辑
- WebMvc 覆盖 OWNER 成功路径、错角色、错组织、只读边界

### 任务 4：新增 Admin Governance API 层，覆盖 Task 37 的查询与配置写入面

**目标**

把 Admin 从静态入口升级成真实治理后台，既有查询面，也有受控配置写入面。

**涉及文件**

- 新增目录：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/admin/`
- 重点新增控制器 / 服务：
  - `AdminReviewQualityController.java`
  - `AdminClaimLedgerController.java`
  - `AdminOntologyGovernanceController.java`
  - `AdminPrivacyRedactionController.java`
  - `AdminModelRoutingController.java`
  - `AdminEvalFeedbackController.java`
  - `AdminTaskRegistryController.java`
  - `AdminSchemaController.java`
  - `AdminWorkflowRulesController.java`
  - `AdminPermissionsController.java`
  - `AdminAuditController.java`
  - `AdminGovernanceQueryService.java`
  - `AdminGovernanceCommandService.java`
- 扩展 API 边界：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ApiBoundaryContractRules.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ApiSafeResponseBody.java`

**影响范围**

- Admin 查询面：
  - AI task registry / failure logs / schema refs
  - claim ledger search
  - review quality + sample audit queue
  - ontology / industry pack status
  - privacy redaction assessments
  - workflow rule / permission matrix / audit log / schema catalog
- Admin 写入面：
  - model routing config
  - privacy redaction policy config
  - workflow rule overlays
  - permission overlays
  - ontology / eval feedback 标注与治理状态
- 所有 Admin 写入都通过治理 command service，且产生日志 / workflow / governance audit，不允许 controller 直接写表

**验证方式**

- WebMvc 覆盖读写成功路径、参数校验、错角色、错组织、非法配置
- 集成测试覆盖写入后读回一致，以及运行时读取 overlay 生效

### 任务 5：补齐 Owner/Admin 的资源权限模型与安全边界

**目标**

在现有 `FieldAccessPolicy` / `ResourceType` 上，把 Owner 的治理只读边界和 Admin 的治理配置边界做成明确规则，而不是各 controller 自行判断。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/ResourceType.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/PermissionEvaluator.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/PermissionEnforcer.java`
- 相关 controller / query / command tests

**影响范围**

- Owner：
  - 扩大到 Task 37 所需治理只读资源
  - 仍保持 read-only
- Admin：
  - 允许 `ADMIN_GOVERNANCE` 的受控读写
  - 不允许对 `Candidate / CandidateProfile / Job / Company / Placement / Commission` 直接改事实
- SYSTEM / AI roles：
  - 不因新增 admin workbench 而获得额外事实写入旁路

**验证方式**

- 权限单测覆盖 OWNER / ADMIN / CONSULTANT / SYSTEM / AI 多角色矩阵
- controller regression / leakage tests 证明新增 surface 没有越权或内部实体泄漏

### 任务 6：扩展 OwnerPortal 前端为完整治理工作台

**目标**

在已有真实 Owner Portal 的基础上补齐 Task 37 的页面与导航，而不是另起一套 owner UI。

**涉及文件**

- 扩展：
  - `apps/web/src/features/owner-portal/OwnerPortal.tsx`
  - `apps/web/src/features/owner-portal/OwnerPortal.test.tsx`
  - `apps/web/src/App.tsx`
- 新增 API client：
  - `apps/web/src/api/ownerDashboard.ts`
  - `apps/web/src/api/ownerGovernance.ts`
  - `apps/web/src/api/ownerAudit.ts`

**影响范围**

- 新增真实 Owner 路由：
  - `/owner/dashboard`
  - `/owner/pipeline`
  - `/owner/consultants`
  - `/owner/clients`
  - `/owner/risk`
  - `/owner/data-quality`
  - `/owner/ai-quality`
  - `/owner/audit`
- 保留现有 `/owner/placements`、`/owner/commission`、`/owner/revenue`
- 页面全部基于真实后端 DTO，不允许前端假数据 KPI 卡片

**验证方式**

- 前端 `typecheck` / `build`
- portal tests 覆盖导航、真实指标文案、sample audit drill-down、空态 / fail-closed 行为

### 任务 7：新增真实 AdminPortal 与会话边界

**目标**

把 `/admin/*` 从静态壳升级为可登录、可查询、可配置的真实治理工作台。

**涉及文件**

- 新增：
  - `apps/web/src/features/admin-portal/AdminPortal.tsx`
  - `apps/web/src/features/admin-portal/adminSession.ts`
  - `apps/web/src/features/admin-portal/AdminPortal.test.tsx`
  - `apps/web/src/api/adminGovernance.ts`
  - `apps/web/src/api/adminConfig.ts`
- 扩展：
  - `apps/web/src/App.tsx`
  - `apps/web/src/auth/accessTokenStorage.ts`

**影响范围**

- `/admin/*` 改为真实 portal route
- Admin 页面至少覆盖：
  - `review-quality`
  - `claim-ledger`
  - `ontology-governance`
  - `privacy-redaction`
  - `model-routing`
  - `eval-feedback`
  - `ai-task-registry`
  - `schema`
  - `workflow-rules`
  - `permissions`
  - `audit-log`
  - `security`
- 页面支持：
  - 查询治理事实
  - 提交受控配置变更
  - 查看配置生效状态与回退说明

**验证方式**

- 前端 tests 覆盖登录、导航、只读视图、配置保存、错误回显、fail-closed 状态
- smoke 验证 `/admin/*` 不再落到 `StaticPortal`

### 任务 8：为 Task 37 新增聚合指标、sample audit queue 与 drill-down 逻辑

**目标**

确保 Task 37 的验收点不是停留在“能看到表”，而是能真正识别 review fatigue / AI quality / privacy risk，并 drill down 到可审计样本。

**涉及文件**

- 新增或扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governancequery/`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/admin/`
  - `apps/web/src/features/owner-portal/OwnerPortal.tsx`
  - `apps/web/src/features/admin-portal/AdminPortal.tsx`

**影响范围**

- Review quality：
  - review velocity
  - bulk approve ratio
  - sampled / failed audit counts
  - false confirmation proxies
- AI quality：
  - task failure
  - schema validity
  - override rate
  - hallucination / authenticity / redaction risk indicators
- Risk / data quality：
  - consent / disclosure blockers
  - duplicate / conflict / stale / missing-field counts
  - ontology stale / drift warnings
  - re-identification incidents
- Drill-down：
  - 从 Owner KPI 进入 sample audit queue
  - 从 Admin review-quality 进入具体 review / claim / workflow 样本

**验证方式**

- 单测覆盖指标聚合口径、未知值处理、sample queue 选样规则
- 手工 smoke 验证从 Owner 看 bulk approve ratio -> drill down 到 audit records 的完整路径

### 任务 9：完成文档同步与交付回归

**目标**

把实际代码状态、当前主线推荐任务和 Known Gaps 与 Task 37 结果同步，避免继续出现“代码已经完成，roadmap 仍停在上一任务”的漂移。

**涉及文件**

- `docs/roadmap/current-engineering-snapshot.md`
- `docs/roadmap/implementation-status.md`
- `docs/roadmap/known-gaps.md`
- `docs/roadmap/productization-roadmap.md`
- `docs/roadmap/productization-roadmap.zh-CN.md`
- 需要时补充 `docs/roadmap/v2.1-capability-split.md`

**影响范围**

- 明确 Task 36 已完成
- 记录 Task 37 新增的 Owner/Admin 治理面、配置写入边界和剩余缺口
- 更新 next recommended task 到 Task 38 或新的主线节点

**验证方式**

- 文档与当前代码 / 提交状态一致
- 交付说明可直接供下一轮任务引用

## Assumptions & Decisions

- 决策：本次“继续工作”确定承接 `Task 37`，不是重复规划 `Task 36`
- 决策：Task 37 包含 **Owner + Admin** 双端，不做单端拆分计划
- 决策：按用户要求，Admin 治理面采用“几乎全可配”方向，而不是只读后台
- 决策：即便支持广泛治理写入，也只允许修改治理配置；业务事实仍由既有 domain service 持有
- 决策：所有治理配置写入都必须有审计记录，并且在配置缺失或非法时 fail-closed 回退默认值
- 决策：继续保留 `v2.0` / `v2.1` 的五端门户结构；Owner、Admin 都在原端口内扩展，不另起系统
- 决策：把当前 roadmap / status 文档漂移视为本轮范围内必须修复的问题

## Verification Steps

1. `git diff --check`
2. 前端：
   - `npm --workspace @rto/web run typecheck`
   - `npm --workspace @rto/web run build`
   - 针对 `OwnerPortal`、`AdminPortal`、新 API client 运行相关 `vitest`
3. `docker info`
4. 定向后端测试：
   - governance query / config JDBC + Testcontainers tests
   - owner dashboard / risk / ai-quality / audit WebMvc tests
   - admin governance query / config WebMvc tests
   - permission / leakage regression tests
   - overlay 生效与 fail-closed tests
5. `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`
6. 手工 smoke checklist：
   - `/owner/dashboard`、`/owner/risk`、`/owner/ai-quality`、`/owner/audit` 都返回真实数据
   - Owner 能看到某 consultant 的 `bulk approve ratio` 并 drill down 到 sample audit records
   - `/admin/*` 不再是静态壳，Admin 能登录并进入真实治理页面
   - Admin 修改 model routing / privacy redaction / workflow rule / permission overlay 后，读回结果与运行时表现一致
   - 非法治理配置被拒绝且不会放宽业务事实边界
   - Owner 仍不能写业务事实；Admin 仍不能直接改 candidate / job / placement / commission 真值
   - `current-engineering-snapshot`、`implementation-status`、`known-gaps` 已同步到 Task 37 基线
