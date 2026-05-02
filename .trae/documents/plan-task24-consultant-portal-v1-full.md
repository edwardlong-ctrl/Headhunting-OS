# Task 24 实施计划：完整 Consultant Portal v1

## Summary

本计划按“**把缺口一起前拉**”的范围推进 `Task 24: Consultant Portal v1`，目标不是只补一个前端壳，而是把顾问端真正提升为可日常操作的统一门户。

计划范围覆盖：

- `Consultant dashboard`
- `AI Intake Center`
- `/consultant/intake/talent` 上传入口
- `/consultant/intake/review/:packetId` 的 `Clean Facts / Source Highlight`
- `Talent Pool list/detail`
- `Company list/detail`
- `Job list/detail`
- `Matching review`
- `Shortlist builder entry`
- `Workflow timeline`
- `Risk / blocked actions panel`
- `Audit drawer`
- `Follow-up queue` 的 Task 24 现实可交付切片

本计划遵循以下仓库事实与产品约束：

- `v2.1` 是当前产品真相，`v2.0` 是 UI/门户基线，不能删除、压缩、替换。
- `Consultant` 必须是**一个统一门户**，不能拆出第二个顾问端。
- 不能做静态 mock-only workflow。
- 不能绕过后端批准的 API、`CanonicalWriteGate`、review gate、client-safe/redaction 规则。
- 当前已有 `Task 23 backend/API scope`，但没有完整 Consultant 前端工作区；本计划把后端缺口和前端缺口一起补齐。

## Current State Analysis

### 1. 已有前端基线

- `apps/web/src/App.tsx`
  - 当前只有五端静态路由壳。
  - 真实可操作页面仅有 client-safe candidate card 流程。
  - `consultant` 仍是静态模块卡片，并非 workflow 应用。
- `apps/web/src/styles.css`
  - 已形成基础设计语言，可继续扩展单文件样式体系。
- `apps/web/src/auth/accessTokenStorage.ts`
  - 现有 token 持久化 helper 仍以 client portal 命名，需要在 Task 24 中升级为统一 portal 认证存储。

### 2. 已有 Consultant 后端 API 基线

- `services/core-api/.../ConsultantIntakeController.java`
  - 已有：
    - `POST /api/consultant/intake/packets/{informationPacketId}/extract`
    - `GET /api/consultant/intake/packets/{informationPacketId}/review`
    - `POST /api/consultant/intake/claims/{claimLedgerItemId}/decisions`
    - `POST /api/consultant/intake/packets/{informationPacketId}/publish`
- `services/core-api/.../ConsultantDocumentController.java`
  - 已有上传、下载、解析、parsed、evidence 查询接口。
  - 可支撑 `/consultant/intake/talent` 的真实上传起点。
- `services/core-api/.../ConsultantCompanyController.java`
  - 已有公司 list/detail/create/update/contact-create。
- `services/core-api/.../ConsultantJobController.java`
  - 已有岗位 list/detail/create/update/requirement-create/scorecard-create。
- `services/core-api/.../ConsultantShortlistController.java`
  - 已有 shortlist list/detail/create/update。
- `services/core-api/.../ConsultantMatchingController.java`
  - 已有 `POST /api/consultant/jobs/{jobId}/matching/generate`，但当前仍是收敛的生成入口，不是完整 matching review 工作台。

### 3. 缺失或不足的后端能力

- 目前**没有** Consultant dashboard API。
- 目前**没有** Talent Pool / Candidate detail API。
- 目前**没有** Consultant workflow timeline API，虽然已有：
  - `services/core-api/.../truthlayer/service/WorkflowAuditQueryService.java`
  - `services/core-api/.../truthlayer/port/WorkflowAuditReadPort.java`
- 目前**没有** Consultant audit drawer API。
- 目前**没有**可直接支撑完整 `/consultant/follow-ups` 的产品级 API；此能力在路线图上与后续任务存在交叉，但本次已决定把 Task 24 需要的顾问工作台缺口前拉，因此需要在 Task 24 内交付一个**真实数据驱动的最小 follow-up queue 切片**，而不是静态占位。

### 4. 产品/页面约束

- `docs/roadmap/productization-roadmap.md`
  - `Task 24` 的正式目标是把 Consultant 变成 daily operating surface。
  - 明确 must deliver：
    - dashboard
    - intake center
    - intake review page
    - talent pool
    - company list/detail
    - job list/detail
    - matching review
    - shortlist builder entry
    - follow-up queue
    - workflow timeline
    - risk/blocked actions panel
    - audit drawer
- `docs/roadmap/v2.1-capability-split.md`
  - 当前 Consultant portal 相关条目几乎全部是 `Missing`。
- `docs/specs/v2.1/product-spec-v2.1.md` 与 `docs/specs/v2.0/product-spec-v2.0.md`
  - 明确顾问端是统一导航与统一上下文。
  - `/consultant/intake`、`/consultant/intake/review/:packetId` 是既定页面。
  - review 页必须呈现 `Clean Facts / Source Highlight / 风险 / 冲突 / blocked reasons`，而不是把治理逻辑藏起来。

## Proposed Changes

### 1. 统一 Consultant Portal 前端骨架与认证存储

**目标**

把当前 `consultant` 静态壳升级为真正的 route-aware 工作区，并把现有 token 存储从 client-safe 单点 helper 提升为全门户可复用认证状态。

**涉及文件**

- `apps/web/src/App.tsx`
- `apps/web/src/styles.css`
- `apps/web/src/auth/accessTokenStorage.ts`
- `apps/web/src/features/consultant-portal/ConsultantPortalLayout.tsx`（新增）
- `apps/web/src/features/consultant-portal/ConsultantPortalNav.tsx`（新增）
- `apps/web/src/features/consultant-portal/ConsultantPortalHome.tsx`（新增）

**影响范围**

- 统一顾问端子路由：
  - `/consultant`
  - `/consultant/dashboard`
  - `/consultant/intake`
  - `/consultant/intake/talent`
  - `/consultant/intake/review/:packetId`
  - `/consultant/talent`
  - `/consultant/talent/:candidateId`
  - `/consultant/companies`
  - `/consultant/companies/:companyId`
  - `/consultant/jobs`
  - `/consultant/jobs/:jobId`
  - `/consultant/jobs/:jobId/matching`
  - `/consultant/jobs/:jobId/shortlist`
  - `/consultant/follow-ups`
  - `/consultant/workflow`
- 保持五端导航不变，不引入第二套 consultant portal。
- 将 access token 存储改为 portal-neutral，避免 Consultant 与 Client 流程分裂。

**验证方式**

- consultant 入口不再落到静态占位页
- consultant 子路由结构稳定
- 现有 client-safe candidate card 流程不回归
- `npm --prefix apps/web run typecheck`

### 2. 建立 Consultant API 前端客户端层

**目标**

为 Consultant Portal v1 的所有真实页面建立统一的前端 API 客户端，避免各页面散落 `fetch()`、状态码映射和 DTO 解包逻辑。

**涉及文件**

- `apps/web/src/api/consultantIntake.ts`（新增）
- `apps/web/src/api/consultantDocuments.ts`（新增）
- `apps/web/src/api/consultantCompanies.ts`（新增）
- `apps/web/src/api/consultantJobs.ts`（新增）
- `apps/web/src/api/consultantShortlists.ts`（新增）
- `apps/web/src/api/consultantMatching.ts`（新增）
- `apps/web/src/api/consultantDashboard.ts`（新增）
- `apps/web/src/api/consultantCandidates.ts`（新增）
- `apps/web/src/api/consultantWorkflow.ts`（新增）
- `apps/web/src/api/consultantFollowUps.ts`（新增）
- `apps/web/src/api/http.ts`（新增，用于 envelope 解包与错误映射）

**影响范围**

- 统一 Bearer token 注入。
- 统一 API envelope 解析。
- 统一把 `401/403/404/400/503` 映射为页面态。
- 使用与后端 DTO 严格对应的 TypeScript 类型，避免 UI 发明不存在的字段。

**验证方式**

- 所有 Consultant 页面通过 API helper 访问后端
- 页面层不再直接拼装错误对象
- `npm --prefix apps/web run typecheck`

### 3. 交付 Intake Center、上传入口与 Intake Review 工作流

**目标**

完成 Task 24 中最核心的顾问 intake 流水线前端，并把 Task 20/22/23 已有后端能力整合成可日常操作的入口。

**涉及文件**

- `apps/web/src/features/consultant-intake/ConsultantIntakeHome.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantIntakeTalentUploadPage.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantIntakeReviewPage.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantIntakeFactCard.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantIntakePublishPanel.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantSourceHighlightPanel.tsx`（新增）
- `apps/web/src/styles.css`
- 复用后端：
  - `ConsultantDocumentController.java`
  - `ConsultantIntakeController.java`

**影响范围**

- `/consultant/intake`
  - 展示 intake 入口、extract 触发、packet review 导航。
- `/consultant/intake/talent`
  - 基于真实上传接口完成候选材料上传。
  - 使用上传响应中的 `informationPacketId` 进入后续 review。
- `/consultant/intake/review/:packetId`
  - 完整渲染 `Clean Facts` 与 `Source Highlight`
  - 支持逐字段 `approved/rejected/escalated/needs_confirmation`
  - 支持 publish 到 candidate path
  - 明确 company/job publish 的受限边界与阻断原因
- Review 页面必须显式显示：
  - 风险等级
  - 最新 review 状态
  - 冲突标记
  - source snippet / locator
  - blocked reason / canonical write status

**验证方式**

- 顾问无需 Postman 即可完成 upload -> extract -> review -> decide -> publish
- 401/403/404/503 均 fail-closed 展示
- UI 不把 AI 输出当事实，也不伪造 company/job publish 已完成
- `npm --prefix apps/web run typecheck`
- `npm --prefix apps/web run build`

### 4. 前拉 Candidate/Talent Pool 后端 API 与 Talent 页面

**目标**

补齐 Task 24 缺失的 Talent Pool / Candidate detail 能力，使顾问能在完成 intake publish 后直接进入人才视图，而不是停留在 packet 级别。

**涉及文件**

- 后端新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCandidateController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantCandidateSummaryResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantCandidateDetailResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/mapper/ConsultantCandidateResponseMapper.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantApiQueryService.java`（扩展）
  - 复用 `candidate/`、`candidateprofile/`、`candidateprofile/service/`、`metadata/` 现有读能力
- 前端新增：
  - `apps/web/src/features/consultant-talent/ConsultantTalentListPage.tsx`
  - `apps/web/src/features/consultant-talent/ConsultantTalentDetailPage.tsx`
  - `apps/web/src/styles.css`

**影响范围**

- 新增 `/api/consultant/candidates`
- 新增 `/api/consultant/candidates/{candidateId}`
- 候选人 detail 必须体现：
  - CandidateProfile 关键字段
  - 字段状态
  - lineage/conflict/stale 元数据的顾问端安全视图
- 前端 `/consultant/talent` 与 `/consultant/talent/:candidateId` 成为 review publish 后的自然落点。

**验证方式**

- review publish 后能跳转到候选人详情
- 候选人 detail 不暴露 Client raw access 规则之外的新对外边界，因为这是 Consultant 内部端而非 client-safe API
- 现有权限与组织隔离仍 fail-closed
- 新增 controller/API boundary 测试通过

### 5. 完成 Company / Job 工作区与 Job Matching 页面

**目标**

把已有 company/job/matching API 接到真正的顾问端页面，并补足导航上的业务连续性。

**涉及文件**

- 前端新增：
  - `apps/web/src/features/consultant-companies/ConsultantCompanyListPage.tsx`
  - `apps/web/src/features/consultant-companies/ConsultantCompanyDetailPage.tsx`
  - `apps/web/src/features/consultant-jobs/ConsultantJobListPage.tsx`
  - `apps/web/src/features/consultant-jobs/ConsultantJobDetailPage.tsx`
  - `apps/web/src/features/consultant-matching/ConsultantMatchingPage.tsx`
  - `apps/web/src/styles.css`
- 复用后端：
  - `ConsultantCompanyController.java`
  - `ConsultantJobController.java`
  - `ConsultantMatchingController.java`

**影响范围**

- `/consultant/companies`
- `/consultant/companies/:companyId`
- `/consultant/jobs`
- `/consultant/jobs/:jobId`
- `/consultant/jobs/:jobId/matching`
- Matching 页面使用现有生成接口，但 UI 要明确该页展示的是 evidence/risk aware review，不是“自动成交引擎”。
- 保留从 candidate detail / job detail 到 matching 的上下文跳转。

**验证方式**

- 列表页支持现有分页/过滤参数
- detail 页正确映射已有 DTO
- matching 页可调用生成接口并展示 score-cap / confidence / reason code
- 前端 build 与相关后端回归测试通过

### 6. 完成 Shortlist Builder Entry 与顾问端 shortlist 视图

**目标**

把已有 shortlist list/detail/create/update API 落成 Task 24 所需的 shortlist builder entry，而不越权伪造 Client review/send 流程。

**涉及文件**

- 前端新增：
  - `apps/web/src/features/consultant-shortlists/ConsultantShortlistListPage.tsx`
  - `apps/web/src/features/consultant-shortlists/ConsultantShortlistDetailPage.tsx`
  - `apps/web/src/features/consultant-shortlists/ConsultantShortlistBuilderPage.tsx`
  - `apps/web/src/styles.css`
- 复用后端：
  - `ConsultantShortlistController.java`

**影响范围**

- `/consultant/jobs/:jobId/shortlist`
- `/consultant/shortlists`
- `/consultant/shortlists/:shortlistId`
- UI 先完成顾问端构建、查看、更新、预发送检查入口。
- 不提前实现 Task 29 的完整 client delivery/send/preview 行为，但需给出真实 builder 入口与 blocked-state 信息。

**验证方式**

- shortlist create/update 可从 UI 发起
- 详情页能展示 shortlist 与 card 数量等已存在信息
- 不新增任何绕过 consent/disclosure/client-safe gate 的前端行为

### 7. 前拉 Dashboard、Risk / Blocked Actions 后端聚合与首页

**目标**

补齐 Task 24 缺失的 dashboard 与 risk panel，使 Consultant 首页成为真实运营入口，而不是菜单页。

**涉及文件**

- 后端新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantDashboardController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantDashboardResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantBlockedActionResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantDashboardQueryService.java`
  - 复用现有 `ConsultantApiQueryService`、`WorkflowAuditQueryService`、`DocumentParsingService`、`IntakeReviewQueryService` 等读能力
- 前端新增：
  - `apps/web/src/features/consultant-dashboard/ConsultantDashboardPage.tsx`
  - `apps/web/src/features/consultant-dashboard/ConsultantBlockedActionsPanel.tsx`
  - `apps/web/src/styles.css`

**影响范围**

- `/consultant/dashboard`
- Dashboard 聚合来源采用**真实现有数据**：
  - 近期待 review 的 intake packet / clean facts
  - parse 未完成/失败文档
  - blocked publish / blocked governed action
  - 待跟进 shortlist / job / company 计数
- `Risk / blocked actions panel` 以真实 gate 结果和 workflow blockers 为数据源，不做假 KPI。

**验证方式**

- 首页展示的卡片全部来源于真实 API/聚合
- blocked items 可跳转到对应 review/job/shortlist/workflow 页面
- 无数据时有稳定空态，不出现 mock 数字

### 8. 前拉 Workflow Timeline 与 Audit Drawer API，并接入顾问端

**目标**

把已有 backend-internal workflow audit read model 产品化成顾问端可读的 timeline / audit drawer。

**涉及文件**

- 后端新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowTimelineResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantWorkflowEventResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantAuditDrawerResponse.java`
  - 复用：
    - `truthlayer/service/WorkflowAuditQueryService.java`
    - `truthlayer/port/WorkflowAuditReadPort.java`
- 前端新增：
  - `apps/web/src/features/consultant-workflow/ConsultantWorkflowPage.tsx`
  - `apps/web/src/features/consultant-workflow/ConsultantAuditDrawer.tsx`
  - `apps/web/src/styles.css`

**影响范围**

- `/consultant/workflow`
- 候选人/岗位/shortlist/intake 页面侧边 `audit drawer`
- 顾问端 timeline 只展示安全、可解释的 workflow/audit 信息，不暴露不必要的内部实现细节。
- 以 entity/ref、action、risk tier、occurredAt、reason、安全元数据为主。

**验证方式**

- timeline 与 drawer 均来源于真实 workflow audit query
- 组织隔离、访问控制、错误体净化保持不回归
- 前端可从多个业务页打开 audit drawer

### 9. 交付 Task 24 范围内的真实 Follow-up Queue 切片

**目标**

在不伪造未来 Task 34 完整外呼/收件工作流的前提下，交付 Task 24 所需的真实数据驱动 follow-up queue，使 Consultant Portal v1 的导航闭环成立。

**涉及文件**

- 后端新增：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantFollowUpController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ConsultantFollowUpSummaryResponse.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantFollowUpQueryService.java`
  - 聚合来源优先复用：
    - `IntakeReviewQueryService`
    - `WorkflowAuditQueryService`
    - `ConsultantApiQueryService`
    - 已存在 document/intake 状态
- 前端新增：
  - `apps/web/src/features/consultant-followups/ConsultantFollowUpQueuePage.tsx`
  - `apps/web/src/styles.css`

**影响范围**

- `/consultant/follow-ups`
- 本轮 follow-up queue 的“真实任务源”限定为当前系统内已经存在、且需要顾问继续处理的项目，例如：
  - `needs_confirmation` 的 clean facts
  - blocked publish / blocked canonical writes
  - 解析失败/待人工推进的 intake 文档
  - 需要补齐 company/job/shortlist 后续动作的工作项
- 明确不在 Task 24 中伪造未来 candidate/client messaging automation；这类外部 follow-up 自动化继续属于后续任务，但不影响 Task 24 交付一个**真实可用的顾问待办队列**。

**验证方式**

- follow-up queue 每一项都有真实后端来源
- 每一项都能跳转到对应 intake/job/shortlist/workflow 页面
- 页面不展示虚构的消息发送状态或外部回执

## Assumptions & Decisions

- 决策：本次 `/plan` 按**完整 Task 24** 规划，不是只做第一刀 intake review。
- 决策：采用“**把缺口一起前拉**”策略，Task 24 缺失的 backend/API 要一并补齐。
- 决策：`Task 24` 的实现边界是“完整 Consultant Portal v1”，但仍必须对未来任务依赖保持诚实：
  - 不伪造外部 follow-up 自动化
  - 不提前宣称完整 client delivery / consent-disclosure workflow 已完成
  - 不把 AI 输出包装成事实
- 决策：`/consultant/intake/talent` 上传入口纳入本次范围，并以 `ConsultantDocumentController` 为真实起点。
- 决策：`follow-up queue` 在 Task 24 中交付**真实数据驱动的最小顾问待办切片**，而不是静态假队列，也不是后续 Task 34 的全量自动化替身。
- 决策：`workflow timeline` 与 `audit drawer` 在 Task 24 中前拉 productization，把已有 `WorkflowAuditQueryService` 通过顾问端安全 DTO 暴露出来。
- 决策：统一使用现有 `ConsultantApiQueryService` / `ConsultantApiCommandService` 模式继续扩展 consultant API，而不是为每个页面临时散落 controller/service 组合。
- 约束：若某页面需要新增后端接口，必须优先走 API-safe DTO、权限检查、组织隔离、错误体净化路径，不能直接暴露内部实体。
- 约束：任何 candidate/client 可见内容都必须继续遵守 anonymity / disclosure / client-safe 边界；Consultant 内部页也不得绕过 CanonicalWriteGate、ReviewEvent 语义和 WorkflowEvent 审计要求。

## Verification Steps

1. 前端静态校验：
   - `git diff --check`
   - `npm --prefix apps/web run typecheck`
   - `npm --prefix apps/web run build`
2. 后端测试前置：
   - `docker info`
3. 后端回归：
   - `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`
4. 重点行为验证：
   - 顾问可在统一 portal 内完成 upload -> extract -> review -> decide -> publish
   - dashboard 展示真实运营摘要与 blocked actions
   - talent/company/job/shortlist 页面均来自真实 consultant API
   - matching 页可真实调用生成接口
   - workflow timeline 与 audit drawer 来源于真实 workflow audit read model
   - follow-up queue 使用真实系统内待处理项，而不是静态样例
   - 所有 401/403/404/400/503 均为安全、净化后的 UI 状态
5. 文档与状态同步：
   - 更新 `docs/roadmap/current-engineering-snapshot.md`
   - 更新 `docs/roadmap/implementation-status.md`
   - 更新 `docs/roadmap/known-gaps.md`
   - 将 `Task 24` 标记为完成，并把下一推荐任务推进到 `Task 25/26/27` 相关后续
