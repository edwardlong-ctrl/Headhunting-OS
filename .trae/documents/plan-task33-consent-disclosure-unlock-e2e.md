# Task 33 实施计划：Consent / Disclosure / Unlock End-to-End

## 范围摘要

本轮按仓库当前推荐顺序承接 `Task 33`，目标是把已经存在的 consent/disclosure/unlock 后端内核，扩展为真实的端到端交易保护闭环：候选人发起并确认 consent、Client 发起 unlock、Consultant 审批、系统生成 `DisclosureRecord` 与完整 `WorkflowEvent` 链路，并在通过 `L4` 门禁后交付受控的 identity-disclosed client read 行为。

本次计划严格遵守以下产品真相与边界：

- `v2.1` 是当前产品 source of truth，`v2.0` 仍是 UI / portal baseline，不可删除、压缩或替换。
- `Consultant` 仍是一个统一门户，不拆成第二套顾问端。
- `Backend owns truth`，unlock / disclosure 的最终真值与审批链只能在后端域服务中成立。
- `AI outputs claims, not facts`，AI 不能自动 unlock，也不能自动 disclose identity。
- Client 在 unlock / disclosure 之前不能读取 raw `Candidate` 或 raw `CandidateProfile`。
- 任何关键状态变化都必须写入 `WorkflowEvent`。

## 当前状态分析

### 1. Consent / disclosure 内核已经存在，但目前仍停留在“后端保护层”

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureService.java`
  - 已能读取 `ConsentRecord`、`UnlockDecision`、`DisclosureRecord`。
  - 已能通过 `ConsentDisclosureProtectionPolicy`、`ConsentDisclosurePrerequisiteEvaluator` 做 fail-closed 判定。
  - 在 `L4` 允许时，已能 append `DISCLOSURE_IDENTITY_DISCLOSED` 与 `CANDIDATE_IDENTITY_DISCLOSED` 的 `WorkflowEvent`，并把 `DisclosureRecord` 转到 `IDENTITY_DISCLOSED`。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureProtectionPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/JdbcConsentDisclosurePrerequisiteEvaluator.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/port/ConsentRecordPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/port/UnlockDecisionPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/port/DisclosureRecordPort.java`

当前缺口：

- 还没有 candidate 侧 consent request / confirmation / shared-fields preview 的真实 API 与 UI。
- 还没有 consultant 侧 unlock approval 工作台。
- 还没有 prior-contact / prior-application / fee-protection 的真实领域对象与审批链。
- 还没有通过 L4 后的 client identity-disclosed 读取面。

### 2. Client 端已经能发起 unlock request，但仍然是 request-only

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientShortlistController.java`
  - 已提供 `POST /api/client/shortlists/{shortlistId}/cards/{cardId}/unlock-requests`。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientApiCommandService.java`
  - 已能创建 `ClientUnlockRequest`，但只记录请求，并未推进 consultant approval 或 disclosure release。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ClientUnlockRequestResponse.java`
  - 当前客户端只拿到请求状态、`unlockDecisionRef`、`approvedDisclosureRecordRef` 等轻量字段。
- `apps/web/src/api/clientShortlists.ts`
- `apps/web/src/features/client-portal/clientPortalShortlistUtils.ts`
- `apps/web/src/features/client-portal/ClientPortal.tsx`

当前缺口：

- 前端的 unlock 状态判断仍基于 `approvedDisclosureRecordRef` 是否存在，并没有真实 consultant approval / disclosure timeline。
- `contact_unlocked` 与 `identity_disclosed` 之间仍有语义空档，当前更多是 UI 占位，而不是完整 release 行为。
- Client 仍然没有看到经过 `L4` 门禁后的真实身份页。

### 3. Consultant workflow 基础已具备，但未落到 unlock 审批闭环

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowSurfaceService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/WorkflowActionCode.java`

当前可确认事实：

- workflow 词汇中已经有 `DISCLOSURE_UNLOCK_REQUESTED`、`DISCLOSURE_CONSULTANT_APPROVED`、`DISCLOSURE_IDENTITY_DISCLOSED`。
- legality policy 已覆盖 consent / disclosure 状态迁移。
- 但 Consultant Portal 目前没有专门面向 unlock approval 的可执行工作区。

### 4. Candidate Portal 仍是静态壳，不足以承接 consent flow

- `apps/web/src/App.tsx`
  - `/candidate/*` 目前仍映射到 `StaticPortal`，只有静态模块清单，没有真实交互。

可推导结论：

- Task 33 必须新增 candidate 端最小真实页面与 API，至少覆盖 consent request、versioned consent、shared-fields preview、candidate confirm / decline。

### 5. Prior-contact / prior-application 目前只有路线图要求，没有现成实现

- 代码中存在 `WorkflowActionCode.PRIOR_CONTACT_CLAIM_CREATED` 与 `WorkflowActionCode.PRIOR_APPLICATION_CLAIM_CREATED`。
- 但在 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/` 下尚未发现对应领域实体、端口、持久化或 API。

这意味着：

- Task 33 需要把 prior-contact / prior-application 从“动作码占位”补成最小真实的后端对象、审批结果与 blocker 来源。
- 本轮应优先做能支撑 unlock gate 的最小实现，而不是提前扩成完整关系情报系统。

## 前置约束

- 不允许让 AI 直接批准 unlock、直接生成最终 disclosure、直接释放身份。
- 不允许 Client 绕过 Consultant approval 获得 `L4_IDENTITY_DISCLOSED`。
- 不允许在 consent 缺失、过期、撤回、profile version 不匹配时生成 `DisclosureRecord`。
- 不允许把 `L3_CONSENTED_DETAIL` 混同为 `L4` 身份披露。
- 不允许新增任何 Client 可读的 raw `Candidate` / raw `CandidateProfile` API。
- 若新增 identity-disclosed 读取面，必须明确它是受 consent / unlock / disclosure / audit 四链约束的 DTO，而不是复用内部实体。

## Proposed Changes

### 1. 补齐 consent / unlock / disclosure 的领域模型与门禁依赖

**目标**

把当前 request-only 与 policy-only 的局部实现收敛成 Task 33 所需的完整后端领域基线：Consent version、shared-fields preview、UnlockDecision、PriorContactClaim、PriorApplicationClaim、fee protection placeholder 及其 blocker 语义。

**涉及文件**

- 复用并扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentRecord.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/UnlockDecision.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/DisclosureRecord.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureProtectionPolicy.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/JdbcConsentDisclosurePrerequisiteEvaluator.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureConfiguration.java`
- 需要新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/PriorContactClaim.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/PriorApplicationClaim.java`
  - 对应 `port/`、`persistence/` 文件
  - 如当前表结构不够，新增对应 Flyway migration

**影响范围**

- 定义可审计的 prior-contact / prior-application 最小对象与 review 状态。
- 为 fee-protection / fee-agreement 增加最小占位判断来源，使 unlock gate 不再只返回抽象 `REQUIRES_REVIEW`。
- 让 consent version 与 shared-fields preview 在后端成为显式数据，而不是前端自行拼装。

**验证方式**

- 单测覆盖 consent version mismatch、consent revoked、prior-contact 未评审、prior-application 未评审、fee gate 缺失等 fail-closed 场景。
- PostgreSQL/Testcontainers 验证新对象组织隔离、读写回放与 prerequisite evaluator 真实集成。

### 2. 把候选人 consent 流程从静态缺失补成真实 Candidate API + Portal

**目标**

交付 candidate 侧最小真实 consent 流程：查看请求、预览 shared fields、确认或拒绝 consent，并把 consent 与 profile version / consent text version 绑定。

**涉及文件**

- 前端：
  - `apps/web/src/App.tsx`
  - 新增 `apps/web/src/features/candidate-portal/`
  - 新增 `apps/web/src/api/candidateConsent.ts`
- 后端：
  - 新增 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/candidate/`
  - 新增 candidate consent request / response DTO
  - 复用 `ConsentRecordPort`、`ConsentDisclosureService` 或补一层 candidate-oriented application service

**影响范围**

- `/candidate/*` 从静态 portal 壳升级为真实 route。
- Candidate 能看到：请求来源、岗位上下文、将被共享的字段预览、consent text version、绑定的 profile version。
- Candidate 的 confirm / decline 会写入 `ConsentRecord` 并追加相应 `WorkflowEvent`。

**验证方式**

- WebMvc 覆盖 candidate 自己可见、他人不可见、版本不匹配拒绝、重复确认幂等等路径。
- 前端 `typecheck` / `build` 通过，并能在受限状态下显示 fail-closed UI。

### 3. 扩展 client unlock request，从“仅提交”升级为带预检和明确 blocker 的申请入口

**目标**

让 Client 发起 unlock 时就拿到明确的 pre-check 结果，而不是单纯创建请求后等待隐式处理。

**涉及文件**

- 复用并扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientShortlistController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientApiCommandService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientApiQueryService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ClientUnlockRequestResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ClientUnlockRequest.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/port/ClientUnlockRequestPort.java`
- 前端：
  - `apps/web/src/api/clientShortlists.ts`
  - `apps/web/src/features/client-portal/clientPortalShortlistUtils.ts`
  - `apps/web/src/features/client-portal/ClientPortal.tsx`

**影响范围**

- 创建 unlock request 前先跑 consent / fee / prior-contact / prior-application / shortlist state / job activation 的预检。
- API 返回稳定的 blocker code / explanation，而不是依赖错误字符串。
- Client Portal 明确展示“可申请 / 已申请 / 待顾问审批 / 审批通过待披露 / 已披露”的状态机。

**验证方式**

- WebMvc 与 service 测试覆盖 blocked / allowed / duplicate request / cross-org / wrong-role。
- 前端 contract 测试或工具函数测试覆盖 status 派生与禁止按钮场景。

### 4. 新增 consultant approval 工作台与受控审批命令

**目标**

把顾问侧从“只看到 workflow”升级为“可以审阅并审批 unlock 请求”，同时保留统一 Consultant Portal。

**涉及文件**

- 前端：
  - `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`
  - `apps/web/src/api/consultantWorkflow.ts`
  - 需要时新增 `apps/web/src/api/consultantUnlocks.ts`
- 后端：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowSurfaceService.java`
  - 需要时新增 consultant unlock approval DTO / command service
  - 复用 `ConsentDisclosureService`

**影响范围**

- 顾问能看到 unlock 申请详情：client request、候选人状态、consent 状态、prior-contact / prior-application 状态、fee gate 状态、re-identification 风险摘要。
- 顾问审批动作会生成 `UnlockDecision`，并追加 `DISCLOSURE_CONSULTANT_APPROVED` 等 `WorkflowEvent`。
- 顾问拒绝或要求补充信息时，状态和 blocker 会落回 client / workflow 面。

**验证方式**

- controller / service 测试覆盖 approve / deny / wrong-role / already-approved / gate-missing。
- Consultant UI smoke 验证能查看并操作 unlock approval，而不引入第二套顾问门户。

### 5. 串起 disclosure 生成与完整 WorkflowEvent 链

**目标**

把 client request -> consultant approved -> disclosure generated -> identity disclosed 串成一条可追踪、可幂等、可回放的后端链路。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/ConsentDisclosureService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/service/WorkflowTransitionAuditService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/truthlayer/WorkflowActionCode.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/consentdisclosure/persistence/*`

**影响范围**

- unlock request、consultant approval、identity disclosed、candidate identity disclosed、fee-protection activation（若通过）都要有稳定 action code 与状态推进。
- 保证 `DisclosureRecord` 的最终落地与 `WorkflowEvent` 事务边界一致，不出现“披露成功但审计丢失”或“审计成功但披露未落地”。
- 保持幂等，避免重复审批导致多条冲突的最终 disclosure 记录。

**验证方式**

- 聚焦集成测试验证事务一致性、重复请求幂等、链路回读。
- 断言没有通过任何旁路直接改 candidate / disclosure 状态。

### 6. 交付 L4 门禁后的 client identity-disclosed 读取面

**目标**

在保持 unlock 前 client-safe 匿名边界不被削弱的前提下，新增通过 `L4` 后才可访问的 client 读取 DTO 与页面。

**涉及文件**

- 后端：
  - 可能新增 `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientDisclosedCandidateController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientApiQueryService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ApiBoundaryContractRules.java`
  - 视需要新增 identity-disclosed response DTO
- 前端：
  - `apps/web/src/api/clientShortlists.ts`
  - `apps/web/src/api/clientSafeCandidateCards.ts`
  - `apps/web/src/features/client-portal/ClientPortal.tsx`

**影响范围**

- unlock / disclosure 前继续只读 client-safe 匿名卡片。
- 只有在 approved `DisclosureRecord` + `WorkflowEvent` 成立后，Client 才能读到受 consent 约束的实名 / 联系方式 / 完整资料字段。
- 读取面必须是新的受控 DTO，不能直接返回 raw `Candidate` / `CandidateProfile`。

**验证方式**

- WebMvc 泄漏测试覆盖 unlock 前拒绝、unlock 后允许、错组织拒绝、错角色拒绝、DTO 无内部实体泄漏。
- 前端 smoke 验证 anonymous detail 与 disclosed detail 的切换行为。

### 7. 对齐 workflow/read-model/UI 语义，消除当前 unlock 状态空档

**目标**

把当前 Client 与 Consultant 页面里关于 `contact_unlocked`、`identity_disclosed`、`approvedDisclosureRecordRef` 的松散判断收敛成统一 workflow 语言。

**涉及文件**

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowSurfaceService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientApiQueryService.java`
- `apps/web/src/features/client-portal/clientPortalShortlistUtils.ts`
- `apps/web/src/features/client-portal/ClientPortal.tsx`
- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`

**影响范围**

- 统一前后端对 unlock / disclosure 的状态名称、阶段说明、blocker 展示与 CTA。
- 避免“顾问已批准但 client 仍看不到原因”“前端误把 approved 当 disclosed”之类的语义漂移。
- 让 workflow timeline 能真实反映 unlock / disclosure 的推进链。

**验证方式**

- DTO / utility / workflow surface 测试覆盖阶段映射正确性。
- 手工 smoke 验证 client 和 consultant 两端显示一致。

### 8. 回归与验收闭环

**目标**

用后端、API、前端三层验证，确保 Task 33 不破坏现有 Task 30 / 32 基线，并补齐路线图要求的关键验收点。

**涉及文件**

- 后端测试：
  - `services/core-api/src/test/java/**/ConsentDisclosure*`
  - `services/core-api/src/test/java/**/Client*Controller*`
  - `services/core-api/src/test/java/**/Consultant*Workflow*`
  - 新增 candidate consent 相关测试
- 前端测试：
  - `apps/web/src/features/client-portal/clientPortalShortlistUtils.contract.test.ts`
  - 视新增 candidate/client API 再补充 contract test

**影响范围**

- 覆盖 consent 缺失 / 过期 / 撤回 / version mismatch 阻断。
- 覆盖 prior-contact / prior-application / fee gate 阻断。
- 覆盖 consultant approval 后 disclosure 生成与 `L4` 读取放行。
- 覆盖 unlock 前无身份泄漏。

**验证方式**

- 后端定向测试先通过，再跑全量 Maven。
- 若涉及前端与 API 变更，同时完成 web `typecheck`、`build` 与关键 contract test。

## Assumptions & Decisions

- 决策：本次“继续工作”按仓库记录的下一推荐任务推进，即 `Task 33`。
- 决策：Task 33 是跨后端、Client Portal、Consultant Portal、Candidate Portal 的端到端任务，不仅是补一个后端 service。
- 决策：prior-contact / prior-application 本轮按“支撑 unlock gate 的最小真实领域对象”交付，不提前做完整历史关系 intelligence 平台。
- 决策：fee protection 本轮先做 placeholder / gate baseline，与 `Job` / `commercialTerms` 和 disclosure prerequisite 关联，但不扩成完整佣金合同系统。
- 决策：identity-disclosed client read 将使用新的受控 DTO / endpoint，而不是开放 raw candidate 实体读取。
- 决策：Candidate Portal 本轮只补 consent 相关最小真实页面，不顺带实现完整 candidate profile / opportunity / timeline 产品面。

## 验证清单

1. `git diff --check`
2. `npm --workspace @rto/web run typecheck`
3. `npm --workspace @rto/web run build`
4. 如新增前端 contract 测试，执行相关 `vitest` 用例
5. `docker info`
6. 定向后端测试：
   - consent / disclosure / unlock service suites
   - client shortlist / unlock controller suites
   - consultant unlock approval / workflow controller suites
   - candidate consent controller / service suites
7. `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`
8. 手工 smoke checklist：
   - Candidate 能看到 consent 请求、shared-fields preview，并能确认或拒绝
   - Client 在 consent 缺失 / 过期 / 撤回 / mismatch 时请求 unlock 被明确阻断
   - Consultant 能查看并审批 unlock 请求，且 blocker 原因可见
   - 审批通过后生成 `UnlockDecision`、`DisclosureRecord` 和完整 `WorkflowEvent` 链
   - unlock 前 client 只能看匿名资料；unlock + disclosure 完成后才可看 identity-disclosed 资料
   - 无 AI 直接 unlock / disclose，且不存在 raw `Candidate` / `CandidateProfile` 泄漏
