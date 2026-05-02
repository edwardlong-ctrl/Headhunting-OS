# Task 24 首刀计划：Consultant Intake Review 前端最小闭环

## 摘要

基于当前仓库状态，`Task 23` 已完成 **backend/API scope**，而下一推荐任务应进入 `Task 24: Consultant Portal v1`。本计划不试图一次完成整个 Consultant Portal，而是先交付一个与现有后端能力严格对齐的最小前端切片：

- 保持 `Consultant` 为同一个统一门户，不拆第二个顾问端。
- 在 `apps/web` 内新增顾问端 intake review 工作流：
  - `/consultant/intake`
  - `/consultant/intake/review/:packetId`
- 支持最小闭环：手动输入 `packetId` -> 触发 extract -> 查看 review 数据 -> 对单条 clean fact 做 decision -> 发起 publish。
- 明确保留当前后端边界：不新增后端接口，不绕过 CanonicalWriteGate，不把 AI 输出当作事实，不实现公司/岗位 publish UI，不补整个 Talent Pool/Workflow/Matching 运营面。

这意味着：**从任务编号上看，应该进入 Task 24；但实现内容会选 Task 24 中最贴近当前后端完成度的第一刀，即 intake review/operator slice。**

## 当前状态分析

### 1. 产品与 UI 约束

- `docs/specs/CURRENT_SPEC.md` 明确：`v2.1` 是当前产品真相，`v2.0` 是 UI/门户基线。
- `docs/specs/v2.1/product-spec-v2.1.md` 与 `docs/specs/v2.0/product-spec-v2.0.md` 都明确：
  - `Consultant` 必须是 **一个统一门户**。
  - `/consultant/intake` 和 `/consultant/intake/review/:packetId` 是既定信息架构的一部分。
  - review 页必须体现 `Clean Facts / Source Highlight / field-level approval / bulk approve` 等治理语义，而不是只给一个黑盒“确认”按钮。
- v2.1 额外要求 review 页显式呈现治理信息：claim strength、bulk approve 限制、source span、write-back gate、client-shareability 等；在本轮最小切片里，至少要先把后端已提供的风险、冲突、review 状态、source highlight 展出来。

### 2. 后端已完成能力

- `ConsultantIntakeController` 已提供 4 个可直接消费的受保护接口：
  - `POST /api/consultant/intake/packets/{informationPacketId}/extract`
  - `GET /api/consultant/intake/packets/{informationPacketId}/review`
  - `POST /api/consultant/intake/claims/{claimLedgerItemId}/decisions`
  - `POST /api/consultant/intake/packets/{informationPacketId}/publish`
- `ConsultantIntakeReviewResponse` 已返回 review 页面所需的核心数据：
  - `cleanFactCount`
  - `cleanFacts[]`
  - 每条 fact 含 `claimId`、`claimFieldName`、`targetFieldPath`、`proposedValue`、`suggestedVerificationStatus`、`suggestedRiskTier`、`latestReviewDecision`、`conflictsWithCanonical`、`rationale`
  - `sourceHighlight` 含 `safeSnippet`、`locator`、页码与 offset
- decision 的可用值来自 `ReviewDecision`：
  - `approved`
  - `rejected`
  - `escalated`
  - `needs_confirmation`
- risk tier 必须使用 `RiskTier` 的现有 wire value：
  - `T0_AUTOMATED_CLEANUP`
  - `T1_LOW_RISK`
  - `T2_MEDIUM_RISK`
  - `T3_HIGH_RISK`
  - `T4_TRANSACTION_LEGAL_BLOCKING`
- publish 当前是**收敛实现**：
  - 候选人 publish 需要已有 `candidateId`
  - 公司/岗位 publish 仍是受限能力，不能在前端假装已经完整可用

### 3. 前端当前真实情况

- `apps/web/src/App.tsx` 现在只有：
  - 五端静态路由壳
  - 一个真实 client-safe candidate card 流程
- `consultant` 端目前仍是静态面板，没有真实 workflow 页面。
- `apps/web/src/api` 目前只有 `clientSafeCandidateCards.ts`，没有顾问 intake API 客户端。
- 当前没有 packet 列表 API，也没有前端 queue 页面，因此本轮最小闭环不能依赖“从列表进入 review”，只能从**手动输入 packetId** 或 review route 直接进入。
- `apps/web/src/auth/accessTokenStorage.ts` 现有本地 token helper 已可复用；本轮先复用，不额外引入完整登录 UI。

## Proposed Changes

### 任务 1：新增 Consultant Intake API 客户端层

**目标**

把 intake 的 extract/review/decision/publish 请求从页面组件中抽离成独立 API helper，统一处理 bearer token、envelope 解包、错误态映射和后端 wire value。

**涉及文件**

- `apps/web/src/api/consultantIntake.ts`（新增）
- `apps/web/src/auth/accessTokenStorage.ts`（仅在确有必要时调整；默认优先复用）

**影响范围**

- 定义前端类型：
  - `ConsultantIntakeRunResponse`
  - `ConsultantIntakeReviewResponse`
  - `ConsultantCleanFact`
  - `ConsultantSourceHighlight`
  - decision request / publish request payload
- 封装以下函数：
  - `extractConsultantIntake(packetId)`
  - `fetchConsultantIntakeReview(packetId)`
  - `submitConsultantIntakeDecision(claimId, payload)`
  - `publishConsultantIntake(packetId, payload)`
- 统一把 HTTP 状态映射为页面可消费的状态：
  - `unauthenticated`
  - `denied`
  - `invalid_request`
  - `unavailable`
  - `failed`

**验证方式**

- `npm --prefix apps/web run typecheck`
- API helper 类型与后端 DTO 字段一一对应，且不引入假字段
- 页面层不再手写 `fetch()` 和散落的状态码分支

### 任务 2：把 Consultant 从静态壳升级成真实 route-aware 工作区

**目标**

在不破坏统一 Consultant 门户的前提下，把 `App.tsx` 的顾问端从静态占位升级为可承载 intake 页面与 review 页的真实路由。

**涉及文件**

- `apps/web/src/App.tsx`
- `apps/web/src/features/consultant-intake/ConsultantPortalHome.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantPortalLayout.tsx`（新增，可选；如不拆则逻辑保留在页面文件中）

**影响范围**

- 保留现有 portal nav，不新增第二个 consultant portal。
- 为 consultant 增加子路由：
  - `/consultant`
  - `/consultant/intake`
  - `/consultant/intake/review/:packetId`
- 其他 consultant 模块仍保留“未实现”的静态入口，但 intake 成为第一个真实可操作模块。

**验证方式**

- consultant 根路由可访问
- `/consultant/intake` 与 `/consultant/intake/review/:packetId` 能正常渲染
- 其他 portal 路由不回归，现有 client-safe flow 不受影响

### 任务 3：实现 `/consultant/intake` 最小入口页

**目标**

提供一个符合当前后端现实能力的 AI Intake Center 首屏，而不是伪造上传队列或完整工作台。

**涉及文件**

- `apps/web/src/features/consultant-intake/ConsultantIntakeHome.tsx`（新增）
- `apps/web/src/App.tsx`
- `apps/web/src/styles.css`

**影响范围**

- 页面包含：
  - access token 输入与本地持久化
  - `packetId` 输入
  - 跳转 review 页按钮
  - “Run extract” 按钮
  - extract 返回的 `status`、`cleanFactCount`、`aiTaskRunIds`
- 因为当前没有 packet list API，本页明确把 `packetId` 手工输入视为**最小操作入口**，并在 UI 上说明这是当前 slice 的限制，而非完整 intake queue。
- 本轮**不接入文档上传 UI**，因为你已选择“最小评审闭环”，而 upload center 更适合作为后续 Task 24 扩展。

**验证方式**

- 输入合法 `packetId` 后可触发 extract
- 成功后可导航到 review 页
- 无 token / 401 / 403 / 400 时页面显示安全失败态，不泄露后端内部错误

### 任务 4：实现 `/consultant/intake/review/:packetId` review 页面

**目标**

把后端的 `ConsultantIntakeReviewResponse` 渲染成真正可操作的顾问评审页面，显式展示 Clean Facts 与 Source Highlight。

**涉及文件**

- `apps/web/src/features/consultant-intake/ConsultantIntakeReviewPage.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantIntakeFactCard.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantSourceHighlightPanel.tsx`（新增，可选；如不拆则合并进 fact card）
- `apps/web/src/styles.css`

**影响范围**

- 页面首屏展示：
  - packetId
  - extractionRunId
  - intendedEntityType
  - cleanFactCount
- 每条 clean fact 卡片展示：
  - `claimFieldName`
  - `targetFieldPath`
  - `proposedValue`
  - `suggestedVerificationStatus`
  - `suggestedRiskTier`
  - `entityResolutionStatus`
  - `latestReviewDecision`
  - `conflictsWithCanonical`
  - `rationale`
  - `sourceHighlight.safeSnippet`
  - `sourceHighlight.locator`
- UI 采用“双视角但单页面”的最小实现：
  - 默认以 Clean Facts 列表阅读
  - 每条 fact 内联展示 Source Highlight
  - 不额外做复杂 tabs/分屏编辑器，避免首刀过重
- 对于无 `claimId` 的 fact，decision 控件要 fail-closed 禁用，因为接口是按 claim 维度提交。

**验证方式**

- review 成功返回时，页面字段完整映射
- `claimId` 缺失项不会错误提交 decision
- 404/503/401/403 呈现为安全状态文案，不泄露内部类型名或栈信息

### 任务 5：实现字段级 decision 操作

**目标**

让顾问可以直接在 review 页对单条 clean fact 做审批决策，并与后端现有 decision 语义完全对齐。

**涉及文件**

- `apps/web/src/features/consultant-intake/ConsultantIntakeFactCard.tsx`
- `apps/web/src/api/consultantIntake.ts`
- `apps/web/src/styles.css`

**影响范围**

- 为每条 fact 提供表单控件：
  - `decision` 下拉：`approved` / `rejected` / `escalated` / `needs_confirmation`
  - `riskTier` 下拉：`T0_AUTOMATED_CLEANUP` 到 `T4_TRANSACTION_LEGAL_BLOCKING`
  - `reason` 文本输入
  - `bulkFlag` 布尔开关
- 提交成功后：
  - 局部显示提交状态
  - 重新获取 review 数据，刷新 `latestReviewDecision`
- 本轮不做真正的“批量多选一键审批”，因为后端当前接口仍是逐 claim 决策；`bulkFlag` 只作为现有审计语义输入，不伪造成批量工作台。

**验证方式**

- decision 请求 payload 与 `ConsultantIntakeDecisionRequest` 完全一致
- 提交后页面状态可更新
- 非法输入、网络失败和权限失败均被页面层安全处理

### 任务 6：实现受限 publish 面板，明确候选人路径与阻断边界

**目标**

在 review 页内完成最小 publish 闭环，但严格遵守当前后端收敛范围，不把未完成能力伪装成可用。

**涉及文件**

- `apps/web/src/features/consultant-intake/ConsultantIntakePublishPanel.tsx`（新增）
- `apps/web/src/features/consultant-intake/ConsultantIntakeReviewPage.tsx`
- `apps/web/src/api/consultantIntake.ts`
- `apps/web/src/styles.css`

**影响范围**

- Publish 面板只开放：
  - `candidateId`
  - `reason`
- 对 `companyId` / `jobId` / `jobCompanyId`：
  - 不在最小 UI 中主打暴露
  - 可以保留为隐藏的后续扩展能力，不在本轮作为主流程
  - 页面文案必须明确说明：公司/岗位 publish 仍未进入当前交付范围
- publish 结果展示：
  - `canonicalWriteCount`
  - `canonicalWriteStatuses`
  - `directWrites`
- 若 publish 被阻断，UI 必须把结果当作治理反馈展示，而不是显示“发布成功”。

**验证方式**

- 候选人 publish 请求可从页面发起
- 响应中的 canonical write 状态可回显
- UI 不会错误承诺 company/job publish 已可用

### 任务 7：补齐样式、状态文案与前端回归校验

**目标**

让第一刀 UI 具备生产可读性与 fail-closed 状态表达，同时控制实现规模。

**涉及文件**

- `apps/web/src/styles.css`
- `apps/web/src/App.tsx`
- `apps/web/src/features/consultant-intake/*.tsx`

**影响范围**

- 扩展现有设计语言，新增：
  - operator workspace 布局
  - fact card
  - status badge
  - review action bar
  - source highlight block
  - publish result panel
  - 安全失败态/空态
- 延续当前 `apps/web` 的单文件 CSS 方式，不在这一刀引入新的 styling framework。
- 不额外引入测试框架；优先使用类型检查、构建、手动 smoke 验证和已有后端回归能力。

**验证方式**

- `npm --prefix apps/web run typecheck`
- `npm --prefix apps/web run build`
- 页面在无数据、未认证、被拒绝、review 可用、publish 返回阻断结果时均有稳定展示

## 假设与决策

- 决策：把“继续工作”落在 **Task 24 的第一刀**，而不是继续扩写 Task 23；原因是仓库文档已把 Task 23 记为 backend/API 已完成。
- 决策：本轮范围是 **Consultant intake review 最小闭环**，不做整个 Consultant Portal。
- 决策：由于当前没有 packet list API，本轮 `/consultant/intake` 使用手动 `packetId` 入口，不伪造 intake queue。
- 决策：由于当前后端 publish 只对候选人路径有现实意义，本轮 publish UI 只主打 candidate publish；company/job 仅保留明确的“未完成”边界说明。
- 决策：复用现有 `accessTokenStorage.ts`，不把这一轮扩成完整登录页或多门户 auth UX 重构。
- 决策：不新增后端接口；如执行阶段发现前端最小闭环被现有接口形状阻断，再单独回退到证据驱动调整，但不预设 backend scope 扩张。
- 约束：不得新增任何会绕过 `CanonicalWriteGate`、把 AI claim 当作 canonical fact、或弱化风险分层 review 的前端行为。
- 约束：保持 `Consultant` 单一门户，不新增第二套 consultant route hierarchy。

## 验证步骤

1. `git diff --check`
2. `npm --prefix apps/web run typecheck`
3. `npm --prefix apps/web run build`
4. `docker info`
5. `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`
6. 手动 smoke checklist：
   - consultant 根路由可进入 intake 页
   - 输入 token + `packetId` 后可触发 extract
   - review 页能渲染 clean facts 与 source highlights
   - 单条 fact 可提交 decision 并刷新状态
   - publish 面板能返回 canonical write 结果
   - 401/403/404/503 均呈现安全失败态

## 计划结论

- 进度判断：`Task 23` 可以视为已完成，但完成的是 **backend/API scope**，不是完整用户界面。
- 下一步编号：应进入 `Task 24`。
- 建议执行切片：先实现 `Task 24` 中最接近当前后端完成度的 **Consultant intake review UI 最小闭环**。
