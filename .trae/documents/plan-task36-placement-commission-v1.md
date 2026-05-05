# Task 36 实施计划：Placement and Commission v1

## 范围摘要

本轮按当前主线继续推进 `Task 36`，交付 **Placement and Commission 端到端 v1**。目标是把已有的 `placement / commission` 数据模型与 workflow 词汇，升级成真实可操作的产品闭环：Consultant 在统一门户记录 offer、onboard、invoice、payment、guarantee、replacement 与 commission；Owner 在 `/owner` 看到 placement 列表、commission 列表和 revenue source data；所有关键状态推进都通过后端域服务和 `WorkflowEvent` 审计落地。

本次计划严格遵守以下边界：

- `v2.1` 是当前产品 source of truth；`v2.0` UI/portal 定义只可扩展，不可删除、压缩或替换。
- `Backend owns truth`；offer、placement、invoice、payment、guarantee、commission 不能由前端或 AI 直接确认为事实。
- `AI outputs claims, not facts`；AI 不能自动确认 offer、上岗、付款或佣金。
- `Consultant` 仍是一个统一门户；Task 36 只在现有 `ConsultantPortal` 内补真实 placements / commission 模块。
- `Owner` 是新增真实产品面，不复用 raw 内部实体，也不能绕过领域服务写真值。
- 每一个关键 placement / commission 状态变化都必须生成 `WorkflowEvent`。

## 当前状态分析

### 1. placement / commission 数据模型已经存在，但只到基础持久化层

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/Placement.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/PlacementStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/service/PlacementService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/port/PlacementPersistencePort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/persistence/JdbcPlacementPersistencePort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/Commission.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/CommissionStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/service/CommissionService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/port/CommissionPersistencePort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/persistence/JdbcCommissionPersistencePort.java`

已确认事实：

- `V10__create_product_data_model_completion.sql` 已建好 `recruiting.placement` 与 `recruiting.commission` 表。
- `PlacementStatus` 已覆盖 `offer_pending -> offer_accepted -> onboarded -> invoice_ready -> invoice_sent -> paid -> guarantee_active -> guarantee_completed -> replacement_required -> cancelled`。
- `CommissionStatus` 已覆盖 `pending / calculated / paid / withheld`。
- 当前持久化端口只有 `create` 和若干 `find*`，没有 `update`、没有产品 API、没有基于 workflow 的应用服务。

结论：

- Task 36 不需要新建整套 placement/commission 数据模型，但需要补齐 **更新路径、领域应用服务、DTO/API 边界、Owner/Consultant 查询面**。

### 2. workflow 词汇已预埋 placement / commission 事件，但还未接到真实应用路径

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/WorkflowActionCode.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/persistence/JdbcWorkflowEntityStatePort.java`

已确认事实：

- 已存在 `OFFER_ACCEPTED`、`CANDIDATE_ONBOARDED`、`INVOICE_READY`、`INVOICE_SENT`、`PAYMENT_MARKED_PAID`、`GUARANTEE_ACTIVATED`、`GUARANTEE_COMPLETED`、`REPLACEMENT_REQUIRED`、`COMMISSION_PENDING`、`COMMISSION_PAID`、`COMMISSION_WITHHELD`。
- `JdbcWorkflowEntityStatePort` 已能读取 `recruiting.placement` 和 `recruiting.commission` 当前状态。

当前缺口：

- placement / commission 尚未通过独立应用服务驱动这些 action code。
- 尚无 transaction path 保证“实体状态更新 + workflow event”一致提交。
- 尚无针对 offer tracking、invoice/payment、guarantee/replacement 的 blocker 或命令语义。

### 3. Consultant Portal 已有 placements / commission 导航，但仍是空页

- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`
- `apps/web/src/App.tsx`

已确认事实：

- `ConsultantPortal.tsx` 已有 `/consultant/placements` 与 `/consultant/commission` 路由。
- 当前 `PlacementsPage()` 和 `CommissionPage()` 只渲染空状态。

结论：

- Task 36 应优先复用这两个真实路由，而不是再开新页面体系。

### 4. Owner Portal 只有静态壳，且权限策略没有 Owner 放行规则

- `apps/web/src/App.tsx`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/PortalRole.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/ResourceType.java`

已确认事实：

- `/owner/*` 当前仍映射到 `StaticPortal portalKey="owner"`。
- `PortalRole` 已有 `OWNER` 枚举。
- `FieldAccessPolicy` 当前只显式放行 `CLIENT`、`CANDIDATE`、`CONSULTANT`，没有任何 `OWNER` allow rule。
- `ResourceType` 目前也没有 placement/commission/revenue 专用资源类型。

结论：

- Task 36 必须一并规划 **Owner 读权限、Owner API、Owner 前端模块**；否则无法满足验收标准“Owner 能看 expected fee / invoice / payment / guarantee / revenue source data”。

### 5. 现有 dashboard / API 模式可直接复用

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantDashboardController.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantDashboardQueryService.java`
- 现有 `apiboundary/consultant/*Controller.java`
- 现有 `apps/web/src/api/*.ts`

已确认事实：

- 当前产品 API 已形成稳定模式：controller + query/command service + allowlisted response DTO + portal API client + 单页工作区。
- Consultant 与 Client 路线都已经按该模式落地。

结论：

- Task 36 应沿用现有模式，避免引入另一套 API 或前端架构。

## Proposed Changes

### 任务 1：补齐 placement / commission 的领域更新与事务边界

**目标**

把现有 create-only 持久化升级为可支持产品操作的真实领域更新路径，覆盖 offer、onboard、invoice、payment、guarantee、replacement 与 commission 状态变化。

**涉及文件**

- 复用并扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/port/PlacementPersistencePort.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/persistence/JdbcPlacementPersistencePort.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/service/PlacementService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/port/CommissionPersistencePort.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/persistence/JdbcCommissionPersistencePort.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/service/CommissionService.java`
- 新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/service/PlacementWorkflowService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/service/CommissionWorkflowService.java`
  - 如字段不够表达 invoice/payment 明细，新增一份新的 Flyway migration 到 `services/core-api/src/main/resources/db/migration/`

**影响范围**

- 为 `Placement` 和 `Commission` 增加 `update` 能力与 optimistic locking。
- 用显式命令而不是自由字段编辑来驱动状态变化，例如：
  - 记录 offer
  - 标记 offer accepted
  - 标记 onboarded
  - 标记 invoice ready / sent
  - 标记 payment paid
  - 激活 / 完成 guarantee
  - 标记 replacement required
  - 创建 / 更新 commission
- 若现有表字段不足以支撑验收面，统一通过 migration 增量补齐，不把关键业务字段塞进无结构 `metadata` 里伪装完成。

**验证方式**

- 单测覆盖合法状态推进、非法跳转、版本冲突、重复命令幂等或拒绝语义。
- PostgreSQL/Testcontainers 覆盖 placement/commission update 持久化与组织隔离。

### 任务 2：把 placement / commission 状态推进接入真实 WorkflowEvent 链路

**目标**

确保 Task 36 的所有关键交易状态变化都通过后端服务写入 `WorkflowEvent`，而不是只更新业务表。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/WorkflowTransitionAuditService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/WorkflowActionCode.java`
- 新增或扩展 placement/commission workflow 服务对应测试

**影响范围**

- 将 `OFFER_ACCEPTED`、`CANDIDATE_ONBOARDED`、`INVOICE_READY`、`INVOICE_SENT`、`PAYMENT_MARKED_PAID`、`GUARANTEE_ACTIVATED`、`GUARANTEE_COMPLETED`、`REPLACEMENT_REQUIRED`、`COMMISSION_PENDING`、`COMMISSION_PAID`、`COMMISSION_WITHHELD` 接到真实域命令上。
- 明确 placement 与 commission 的 before/after state 组合，保持 legality policy 与实体真实状态一致。
- 保证业务写入与 workflow 审计在同一事务边界内，不出现“状态已变但没有 event”或“event 已写但业务没落地”。

**验证方式**

- workflow service / integration test 断言每个关键 transition 都存在对应 `WorkflowEvent`。
- 非法 transition 必须 fail-closed，且不得写业务状态或 event。

### 任务 3：新增 Consultant placement / commission API 边界

**目标**

在现有 Consultant API 模式下，交付顾问可读可写的 placement / commission 产品接口。

**涉及文件**

- 新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementQueryService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantPlacementCommandService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCommissionController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCommissionQueryService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCommissionCommandService.java`
  - placement / commission 相关 request/response DTO
- 复用并扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ApiBoundaryContractRules.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/ResourceType.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`

**影响范围**

- 顾问可在统一门户下：
  - 列表查看 placements
  - 查看 placement detail 与 timeline
  - 创建 placement 记录
  - 推进 placement 状态
  - 查看 / 创建 / 更新 commission
- 为 consultant 新增 placement/commission 资源权限，仍按 same-organization fail-closed。
- DTO 只暴露产品层所需字段，不返回 raw 内部实体或无约束 JSON。

**验证方式**

- WebMvc 覆盖成功路径、错角色、错组织、非法 payload、版本冲突、非法 transition。
- API 泄漏测试覆盖 response allowlist。

### 任务 4：新增 Owner 读模型、权限与 API

**目标**

交付 Owner 的 placement / commission / revenue source data 读取面，满足 Task 36 验收要求，同时保持 Owner 为只读业务监督者，不直接写事实。

**涉及文件**

- 新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerPlacementController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerPlacementQueryService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerRevenueQueryService.java`
  - owner 相关 response DTO
- 复用并扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/ResourceType.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/*` 中的角色校验模式

**影响范围**

- 新增 Owner 读权限，只放行 placement、commission、revenue summary 这类监督性读操作。
- Owner API 至少返回：
  - placement table
  - expected fee（同一 placement 下按全部 commissions 聚合；若存在未知金额则显示 known subtotal / pending disclosure，而不是静默低报）
  - invoice status
  - payment status（paid fee 只统计已知金额；若历史 paid commission 缺失 amount，必须显式披露 excluded count）
  - guarantee status
  - commission status（保留多 commission 真实状态，不压扁成单条 latest commission 真值）
  - revenue pipeline summary
- 不为 Owner 提供更新 placement/commission 真值的写接口。

**验证方式**

- 权限测试覆盖 OWNER allowed / CONSULTANT denied-on-owner-surface / wrong-org denied。
- WebMvc 测试覆盖只读成功与错误处理。

### 任务 5：交付 Consultant Portal 的真实 placements / commission 页面

**目标**

把现有空白的顾问页升级为真实工作区，让顾问能从 shortlist / interview 之后推进到 placement 和 commission。

**涉及文件**

- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`
- 新增：
  - `apps/web/src/api/consultantPlacements.ts`
  - `apps/web/src/api/consultantCommission.ts`
- 复用并扩展：
  - `apps/web/src/features/consultant-portal/consultantPortalUtils.ts`

**影响范围**

- 将当前 `PlacementsPage()` / `CommissionPage()` 空状态替换为真实列表与详情交互。
- 顾问页面至少支持：
  - 创建 placement
  - 录入 salary / start date / fee rate / guarantee period
  - 推进 offer accepted、onboarded、invoice ready/sent、payment paid、guarantee、replacement
  - 查看并维护 commission 记录
  - 对 `amount == null` 的 commission 禁止 `mark paid`
- 继续保持统一 Consultant Portal，不新增第二套顾问端。

**验证方式**

- 前端 `typecheck` / `build` 通过。
- 关键工具函数或 contract test 覆盖状态展示和 CTA 限制。

### 任务 6：把 `/owner` 从静态壳升级为真实 Owner Portal v1 子集

**目标**

在不提前展开 Task 37 全量治理面的前提下，为 Task 36 补齐 Owner 的 placements / commission / revenue 三个真实页面。

**涉及文件**

- `apps/web/src/App.tsx`
- 新增：
  - `apps/web/src/features/owner-portal/OwnerPortal.tsx`
  - `apps/web/src/api/ownerPlacements.ts`
  - `apps/web/src/api/ownerRevenue.ts`
- 需要时新增 owner portal 辅助工具文件到 `apps/web/src/features/owner-portal/`

**影响范围**

- `/owner/*` 由 `StaticPortal` 改为真实 `OwnerPortal`。
- 第一版页面范围只包含：
  - `/owner/placements`
  - `/owner/commission`
  - `/owner/revenue`
- 页面展示真实后端数据，不使用假 dashboard 卡片冒充完成。
- Task 37 的 AI quality / risk / audit search 等治理面仍保持后续任务范围，不在本轮偷带。

**验证方式**

- 前端 `typecheck` / `build` 通过。
- 手工 smoke 验证 Owner 能看到 placement、commission、revenue source data。

### 任务 7：补齐 placement / commission 的 API 合同、权限与测试闭环

**目标**

确保新资源进入现有安全边界体系，而不是仅靠 controller 手写判定。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/ResourceType.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ApiBoundaryContractRules.java`
- 现有 consultant / owner controller leakage tests

**影响范围**

- 为 placement、commission、revenue summary 定义资源级权限语义。
- 顾问拥有同组织读写 placement/commission 的受限权限。
- Owner 只有同组织读 placement/commission/revenue 的权限。
- 防止将 Owner 误做成管理员或 Consultant 权限旁路。

**验证方式**

- 单测覆盖 `FieldAccessPolicy` 的 OWNER / CONSULTANT 新规则。
- controller leakage tests 断言 API 仍只暴露 allowlisted DTO 字段。

### 任务 8：完成 Task 36 的端到端验证与回归

**目标**

以产品验收结果而不是仅编译通过来验证 Task 36，确保不破坏 Task 26/29/33/35 的既有链路。

**涉及文件**

- 后端测试：
  - `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/productdatamodel/*`
  - `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/truthlayer/*`
  - `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/*`
  - 新增 `apiboundary/owner/*` 测试
  - 新增 placement / commission service / JDBC / integration tests
- 前端测试：
  - `apps/web/src/api/*`
  - 需要时新增 owner / consultant contract tests

**影响范围**

- 覆盖完整链路：
  - Consultant 录入 offer
  - offer accepted
  - onboarded
  - invoice ready / sent
  - payment marked paid
  - guarantee active / completed
  - replacement required
  - commission created / paid / withheld
  - Owner 查看 placement / commission / revenue
- 验证 `WorkflowEvent` 不缺失、角色权限不越权、Owner 只读边界不被削弱。
- 验证 expected fee / paid fee 在多 commission、部分未知金额、历史异常 paid commission 下仍保持 `Backend owns truth`，不会静默低报。

**验证方式**

- 先跑定向 backend suites，再跑全量 Maven。
- 同步完成 web `typecheck`、`build` 与必要的 contract tests。

## Assumptions & Decisions

- 决策：本次“继续工作”确定承接 `Task 36 Placement and Commission v1`，不是切到 Task 37 或补丁式硬化。
- 决策：范围是 **端到端 v1**，而不是后端-only 或顾问侧-only；因此必须包含 Consultant 产品面和 Owner 产品面。
- 决策：Owner 本轮只交付 Task 36 所需的 placements / commission / revenue 子集，不提前展开 Task 37 的全量治理 dashboard。
- 决策：placement / commission 真值继续由后端域服务维护；Owner 保持只读监督。
- 决策：如现有 schema 字段无法满足 `fee rate / invoice status / payment status / revenue source data` 的产品面要求，优先通过新的结构化 migration 补齐，而不是把关键状态藏进 `metadata`。
- 决策：Task 36 不做完整财务系统，不自动确认 offer 或 commission，不替代合同或发票系统。

## Verification Steps

1. `git diff --check`
2. 前端：
   - `npm --workspace @rto/web run typecheck`
   - `npm --workspace @rto/web run build`
   - 如新增 contract test，执行对应 `vitest` 用例
3. `docker info`
4. 定向后端测试：
   - placement / commission service tests
   - placement / commission JDBC / Testcontainers integration tests
   - consultant placement / commission controller tests
   - owner placement / revenue controller tests
   - workflow transition / audit tests（含 placement / commission action）
   - permission / API leakage tests
5. `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`
6. 手工 smoke checklist：
   - Consultant 能在 `/consultant/placements` 创建并推进 placement
   - Consultant 能录入 fee rate、start date、guarantee period，并查看 invoice/payment/guarantee/replacement 状态
   - commission 记录会在受控时点生成或更新，且不会由 AI 自动确认
   - `amount == null` 的 commission 不能被标记为 `paid`
   - 每个关键状态推进都能在 workflow timeline 中看到对应 `WorkflowEvent`
   - Owner 能在 `/owner/placements`、`/owner/commission`、`/owner/revenue` 看到 expected fee、invoice status、payment status、guarantee status 与 revenue source data
   - 当 expected fee 或 paid fee 存在未知金额时，Owner revenue 必须显示 known subtotal 和 excluded/pending disclosure，而不是给出看似完整的总额
   - Owner 无法通过 Owner surface 直接修改 placement 或 commission 真值
