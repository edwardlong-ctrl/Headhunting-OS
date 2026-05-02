# Task 26 实施计划：Workflow Engine v1

## Summary

本轮按路线图顺序承接 `Task 26`，不并行展开 `Task 27`。目标是在现有 `WorkflowEvent` 审计基础上，补齐 **服务层合法状态迁移、阻断原因、可消费 timeline/blocker API，以及 Consultant 端最小可操作 workflow UI**，把当前“只会记录事件”升级成“会判定是否合法、会阻断非法迁移、会把状态机结果返回给产品层”的第一版 Workflow Engine。

本次范围以 **真实可落地的 v1** 为准：

- 覆盖 `Job`、`Candidate`、`Shortlist`、`Consent`、`Disclosure` 的合法迁移与 blocker 返回。
- 为 `Placement / Commission` 交付 **后端状态机基线与 read-model 支撑**，但不提前伪造完整前端运营页面。
- 保持 `Consultant` 为统一门户；不新增第二套 workflow 产品面。
- 继续坚持 `WorkflowEvent` 是强制审计边界，任何关键状态变化都不能直接写状态而不留下 event。

## 当前状态分析

### 1. 已有 workflow 基础件已经存在，但仍停留在“审计优先”

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/WorkflowTransitionAuditService.java`
  - 已经会校验 action vocabulary、要求 `beforeState/afterState`，并通过 `WorkflowTransitionLegalityPolicy` 拦截非法组合。
  - 已经会对比 `WorkflowEntityStatePort` 读取到的当前状态，阻止 `beforeState` 与真实实体状态不一致的请求。
  - 但它本身只是 audit append boundary，不负责“先做 transition decision，再驱动 domain update，再输出 blocker/next action”。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
  - 已经内置多类 action 的允许前后状态。
  - 但目前仍是“静态合法性表”，没有 blocker 对象、没有 transition decision、没有可直接供 API/UI 使用的 next-step 结果。

### 2. 现有状态词汇已经基本具备，但部分 wire value 还未完全对齐

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/JobStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/candidate/CandidateStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/shortlist/ShortlistStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/DisclosureStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/PlacementStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/CommissionStatus.java`

已确认的关键不一致点：

- `WorkflowTransitionLegalityPolicy` 里候选人状态仍使用 `parsed`、`outreach_started`、`interest_recorded`，而 `CandidateStatus` 的真实 wire value 是 `profile_parsed`、`outreach`、`interested`。
- 这意味着 Task 26 必须先做 **状态词汇对齐**，否则后续状态机会在合法性校验与真实实体状态之间持续漂移。

### 3. 现有实体服务大多还是 CRUD，只有 Job activation 已部分进入 workflow

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobIntakeApplicationService.java`
  - 已经在 `activateJob()` 中通过 `WorkflowTransitionAuditService` 记录 `JOB_CONTRACT_PENDING` 与 `JOB_ACTIVATED`。
  - 这是当前最接近 Task 26 目标的实现样板。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/candidate/service/CandidateService.java`
  - 目前只有 create/find/list，没有统一的 workflow transition 方法。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/shortlist/service/ShortlistService.java`
  - 目前只有 create/update/find/list/add card，没有 ready/send/view/close 一类状态迁移封装。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureService.java`
  - 已经具备 unlock/disclosure 的保护策略和事件 append，但还没有抽象成通用的 consent/disclosure workflow surface。

### 4. Workflow read model 和 Consultant UI 已存在，但还是只读、信息不完整

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowController.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowSurfaceService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowEventResponse.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowTimelineResponse.java`
- `apps/web/src/api/consultantWorkflow.ts`
- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`

已确认缺口：

- `ConsultantWorkflowSurfaceService.toResponse()` 目前把 `beforeStatus` / `afterStatus` 直接写成 `null`，前端只能看到 action code，看不到真实状态迁移。
- Consultant Portal 的 workflow 区域目前仍偏“事件查看器”，没有合法 next action、blocker reason、transition attempt 的最小交互闭环。

### 5. WorkflowEntityStatePort 已具备真实状态读取，但覆盖面还不完整

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/persistence/JdbcWorkflowEntityStatePort.java`
  - 已能读取 `job`、`shortlist`、`candidate`、`consent_record`、`disclosure_record` 的当前状态。
  - 尚未覆盖 `placement` 与 `commission`，因此路线图要求的 placement/commission state machine baseline 还没有真实 state snapshot 支撑。

## 前置约束

- `v2.1` 仍是产品 source of truth，`v2.0` 仍是 UI/portal baseline。
- 任何关键状态变化都必须继续写 `WorkflowEvent`，不能只改实体状态。
- 不能引入复杂 BPMN/流程编排引擎；Task 26 只做清晰、可测试、服务层拥有真值的状态机 v1。
- 不能让前端决定 transition legality；前端只能展示后端返回的 allowed/blocker/next action 结果。
- 不能把 `Consent / Disclosure` 的现有保护链条简化成“改个 status 字段”；unlock/disclosure 仍必须先过原有 policy/service gate。
- 本轮不把 `Task 27` 的 match persistence、证据评分、provenance weighting 偷带进来。

## Proposed Changes

### 任务 1：统一 workflow 状态词汇，并修正 legality policy 与真实实体状态的映射

**目标**

先消除状态机 vocabulary 漂移，确保合法性校验使用的状态值与各领域实体真实 wire value 一致。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/candidate/CandidateStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/DisclosureStatus.java`
- 如需最小补充说明，也包括 `JobStatus.java`、`ShortlistStatus.java`、`PlacementStatus.java`、`CommissionStatus.java`

**影响范围**

- 把 policy 中使用的候选人状态改为与 `CandidateStatus.wireValue()` 对齐。
- 校对 consent / disclosure / placement / commission 的 after-state 取值，避免未来 transition validator 与真实表状态脱节。
- 明确允许的别名只存在于 compatibility 层，而不是散落在业务逻辑里。

**验证方式**

- 为 legality policy 增加聚焦单测，覆盖候选人状态词汇对齐。
- 证明 `WorkflowTransitionAuditService` 针对真实实体状态快照不再因为旧词汇误判。

### 任务 2：在现有 audit service 之上增加可复用的 transition decision / blocker 模型

**目标**

把“会抛异常的 legality check”升级成“可被应用服务和 API 层消费的 transition decision”，统一表达 `allowed / blocked / reasons / required action`。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/WorkflowTransitionAuditService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/WorkflowTransitionAuditRequest.java`
- 新增 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionDecision.java`
- 新增 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionBlocker.java`
- 新增一个聚焦 orchestrator / policy facade，建议放在 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/`

**影响范围**

- 保留现有 `record()` 的审计职责，但增加一个先判定再执行的统一入口。
- 把非法 before/after、真实实体状态不匹配、缺少前置条件等都转换成稳定 blocker code，而不是让 controller/UI 只能接裸 `IllegalArgumentException`。
- 为 Job / Candidate / Shortlist / Consent / Disclosure 共用同一套 transition decision 输出形状。

**验证方式**

- 单测覆盖：合法迁移返回 allowed，非法迁移返回 blocker code，真实状态漂移返回 fail-closed blocker。
- 证明 decision 层不直接做实体 mutation，仍由领域服务掌管状态变更。

### 任务 3：扩展 `WorkflowEntityStatePort` 和 read model，使 Placement / Commission 也进入状态机基线

**目标**

让路线图要求的 `Placement / Commission state machine baseline` 至少在后端状态读取、合法性校验和 timeline 可见性层面成立。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/port/WorkflowEntityStatePort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/persistence/JdbcWorkflowEntityStatePort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/placement/PlacementStatus.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/commission/CommissionStatus.java`
- 相关 read-model / integration test 文件

**影响范围**

- 为 `recruiting.placement` 与 `recruiting.commission` 增加当前状态读取。
- 让 workflow legality policy 能对 placement/commission 的 before/after status 做基础校验。
- 先不新增完整 Owner/Finance 前端页面，但保证这些实体进入统一 workflow 基础设施。

**验证方式**

- 新增 PostgreSQL/Testcontainers 集成测试，证明 `WorkflowEntityStatePort` 能按组织读取 placement/commission 当前状态。
- 证明无状态快照时仍 fail-closed，而不是默认放行。

### 任务 4：把 Job / Shortlist / Candidate 的关键状态迁移收敛成服务层 workflow application path

**目标**

不再让状态迁移散落在各 controller/UI 或单个 use case 里，而是为最先会被 Consultant Portal 触发的三个实体建立统一的 transition application path。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobIntakeApplicationService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/shortlist/service/ShortlistService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/candidate/service/CandidateService.java`
- 可能新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/shortlist/service/ShortlistWorkflowService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/candidate/service/CandidateWorkflowService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/recruiting/RecruitingDomainConfiguration.java`

**影响范围**

- `Job`：延续并统一 `contract_pending -> activated` 一类迁移方式，补齐 pause / cancel / shortlist progression 的服务层入口。
- `Shortlist`：补齐 `draft -> ready_for_review -> sent_to_client -> client_viewed -> closed` 的服务层迁移与 blocker。
- `Candidate`：补齐 `profile_parsed -> consultant_review -> available -> matched_to_job -> outreach -> interested -> consent_pending -> consent_confirmed -> client_review -> identity_disclosed` 的最小受控迁移。
- 所有迁移都必须：
  - 先做 decision / blocker 判定
  - 再做实体状态更新
  - 再 append `WorkflowEvent`

**验证方式**

- 增加 Job / Shortlist / Candidate 聚焦服务测试。
- 证明非法跳转被 block 且不写实体状态。
- 证明合法跳转既更新实体状态，也写入 `WorkflowEvent`。

### 任务 5：把 Consent / Disclosure 现有保护链条接入统一 workflow 语义，并保留原有 gate

**目标**

让 `Consent` 与 `Disclosure` 不再只是“保护策略 + 持久化”，而是纳入统一状态机结果与 blocker 表达，同时继续坚持原有 unlock/disclosure 审批边界。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureProtectionPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/JdbcConsentDisclosurePrerequisiteEvaluator.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- 相关 consent/disclosure 测试文件

**影响范围**

- 把 `job activation gate required`、`fee agreement gate required`、`prior contact review required`、`prior application review required`、`privacy risk gate required` 统一映射到 workflow blocker 结果。
- 让 disclosure 的批准、披露、fee protection activation 既遵守原 policy，又能反映到统一状态机语言中。
- 保持 `L3` 与 `L4` 的边界，不把 richer detail 误当 identity disclosure。

**验证方式**

- 聚焦测试验证 disclosure 不能绕过 consent 和 consultant approval。
- 证明 blocker response 能稳定返回，而不是靠异常字符串供前端解析。

### 任务 6：扩展 Consultant workflow API，使 timeline 和 transition/blocker 成为页面可消费能力

**目标**

把现有只读 workflow timeline 升级为更完整的产品 API：既能返回真实状态迁移细节，也能返回某实体的 legal next actions / blockers。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowController.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowSurfaceService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowEventResponse.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowTimelineResponse.java`
- 建议新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowTransitionOptionResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowBlockerResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowEntityStateResponse.java`

**影响范围**

- `timeline` 返回真实 `beforeStatus` / `afterStatus`，不再填 `null`。
- 新增针对 job / candidate / shortlist / consent / disclosure 的 transition preview 或 transition options 查询接口。
- 若本轮需要最小执行入口，则仅开放 Consultant 能安全执行的 transition command，不提前开放 client/candidate 端本应参与的动作。

**验证方式**

- WebMvc 测试覆盖 allow/deny/invalid-id/blocked transition 等路径。
- 证明 API 只返回 DTO，不泄露内部实体或未净化错误文本。

### 任务 7：更新 Consultant Portal 的 workflow / job / shortlist 视图，展示真实状态、blocker 和下一步动作

**目标**

让现有 Consultant Portal 从“事件浏览”提升为“可理解、可推进、会提示为什么被卡住”的 workflow 工作区。

**涉及文件**

- `apps/web/src/api/consultantWorkflow.ts`
- `apps/web/src/api/consultantJobs.ts`
- `apps/web/src/api/consultantShortlists.ts`
- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`
- `apps/web/src/features/consultant-portal/consultantPortalUtils.ts`

**影响范围**

- 在 workflow timeline 中显示真实 `before -> after` 状态。
- 在 job / shortlist / workflow 页面中展示：
  - 当前状态
  - legal next actions
  - blocker reasons
  - 最近 workflow events
- 延续 Task 24 的统一 Consultant portal，不新增第二套 workflow 页面体系。
- 对 `Consent / Disclosure / Placement / Commission` 暂无完整前端操作面的部分，只做只读状态展示或 explain-only 提示，不伪造已可操作页面。

**验证方式**

- `npm --workspace @rto/web run typecheck`
- `npm --workspace @rto/web run build`
- 手动 smoke 验证：workflow 页面能看见状态迁移、blocker 和 next actions，而不是只有 action code 列表。

### 任务 8：补齐 workflow regression，覆盖 legality、blocker、read-model 和现有任务回归

**目标**

让 Task 26 成为稳定基础层，避免后续 Task 27/29/33 在 workflow 之上继续重复修地基。

**涉及文件**

- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/truthlayer/WorkflowTransitionAuditServiceTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/truthlayer/WorkflowTransitionAuditPostgresIntegrationTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/job/service/JobIntakeApplicationServiceTest.java`
- 新增或扩展：
  - `candidate/service/*Workflow*Test.java`
  - `shortlist/service/*Workflow*Test.java`
  - `consentdisclosure/*Workflow*Test.java`
  - workflow controller WebMvc 测试

**影响范围**

- 覆盖状态词汇对齐。
- 覆盖非法迁移 blocker。
- 覆盖真实实体状态与 beforeState 不一致时的 fail-closed。
- 覆盖 timeline DTO 返回 before/after status。
- 覆盖现有 `Task 25` 的 job activation path 不回归。

**验证方式**

- 定向 workflow / consent / job activation 测试通过。
- 全量 backend 测试通过后，Task 26 才算闭环。

## Assumptions & Decisions

- 决策：`/plan 按照计划继续工作` 默认承接路线图顺序中的 **Task 26**，而不是同时启动 Task 27。
- 决策：Task 26 的核心是 **服务层合法状态机 + blocker + timeline/read-model productization**，不是引入 BPMN、工作流引擎平台或脚本编排器。
- 决策：`Placement / Commission` 本轮只交付后端状态机 baseline 与 read-model 支撑，不提前伪造完整产品页面；对应前端深度仍属于 Task 36。
- 决策：`Consent / Disclosure` 继续复用现有 `ConsentDisclosureService` 保护链，而不是旁路做“纯 workflow 引擎化”重写。
- 决策：Consultant Portal 只消费后端返回的 `allowed / blocked / reasons / next actions`，不在前端硬编码 transition legality。
- 约束：不得削弱 `WorkflowEvent` 审计、`CanonicalWriteGate`、`Consent / Disclosure` 保护链、以及 `Client before unlock/disclosure cannot read raw Candidate` 的既有规则。

## Verification Steps

1. `git diff --check`
2. `npm --workspace @rto/web run typecheck`
3. `npm --workspace @rto/web run build`
4. `docker info`
5. 定向 backend 测试：
   - `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml -Dtest=WorkflowTransitionAuditServiceTest,WorkflowTransitionAuditPostgresIntegrationTest,JobIntakeApplicationServiceTest test`
6. 按新增范围补充的候选人 / shortlist / consent-disclosure / workflow controller 聚焦测试通过
7. `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`
8. 手动 smoke checklist：
   - Consultant workflow timeline 返回真实 `beforeStatus` / `afterStatus`
   - job activation 仍可走通，且 blocker 信息稳定
   - 非法 workflow transition 会被 block，且 UI 能看到原因
   - disclosure 相关 transition 不会绕过 consent 和 consultant approval
   - placement / commission 至少能被 workflow state read-model 正确读取
