# Task 25 实施计划：Company and Job Intake v1

## Summary

本计划按**整包 Task 25**推进 `Company and Job Intake v1`，范围明确包含：

- 顾问端创建/完善 company
- Client 端提交 company profile
- manual job intake
- AI job intake from JD
- clarification questions
- scorecard generation
- consultant activation gate
- commercial terms placeholder

本次规划同时遵守以下已经确认的产品/实施决策：

- `v2.1` 是当前产品真相，`v2.0` 是 UI/门户基线，不可删除、压缩、替换。
- `Consultant` 仍是一个统一门户，不拆第二个顾问端。
- 本次需要做**前后端双端**：不仅补后端与顾问端，也要落最小真实 `Client` 门户页面和 API。
- 本次需要补齐 `company/job` 的**受控写回路径**，不能停留在“AI 只给建议、不进入实体”的层面。
- `commercial terms` 本次只做**最小占位模型**，不做合同系统、计费引擎或复杂审批流。
- `Task 25` 只实现**岗位 intake 与激活门禁**，不把完整通用状态机引擎偷带进来；更通用的 transition engine 仍属于 `Task 26`。

## Current State Analysis

### 1. 现有顾问端基础已具备，但仍不是完整 Task 25

- `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`
  - 已有 company/job 列表、详情、创建、更新。
  - 已有 job requirement / scorecard 创建能力。
  - 已有 `/consultant/jobs/:jobId/intake` 页面，但当前只是读取 `job` 实体并做前端侧 checklist 展示，不是完整的 AI intake -> clarification -> controlled publish -> activation 闭环。
  - 已有 `/consultant/intake/upload/company` 与 `/consultant/intake/upload/job` 上传入口配置，说明顾问端已能把公司材料和 JD 放进 governed intake 队列。
- `apps/web/src/api/consultantJobs.ts`
  - 已有 `list/fetch/create/update` job API helper。
  - 已有 `createConsultantJobRequirement()` 与 `createConsultantJobScorecard()`。
  - 说明 Task 25 不必从 0 开始搭顾问端 CRUD。

### 2. 现有后端已有 company/job 领域模型，可承接最小 commercial placeholder

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/company/Company.java`
  - 已有 `name/displayName/industry/website/headquartersLocation/sizeBand/status/paymentReliability/metadata`。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/Job.java`
  - 已有 `description/location/seniorityBand/roleFamily/employmentType/compensation/status/commercialTerms/metadata`。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/company/CompanyStatus.java`
  - 当前状态集：`new/active/inactive/archived`。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/JobStatus.java`
  - 当前状态集已覆盖 `draft/submitted/intake_review/needs_more_info/commercial_pending/contract_pending/activated/...`。

结论：

- `commercial terms placeholder` 可优先复用 `job.commercialTerms` 与 `company/job.metadata`，不必强制引入完整新表。
- `activation gate` 所需的状态 vocabulary 已存在，但还缺服务层规则、阻断原因、API 和真实 UI 流程。

### 3. AI intake 基础已存在，但 company/job 受控 publish 明确缺失

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/tasks/companyintake/*`
  - 已有 `CompanyIntakeTaskService / CompanyIntakeInput / CompanyIntakeOutput`。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/tasks/jobintake/*`
  - 已有 `JobIntakeTaskService / JobIntakeInput / JobIntakeOutput`。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/IntakeReviewDecisionService.java`
  - 当前 `publish()` 对 `COMPANY` 和 `JOB` 直接 fail-closed：
    - `company_publish_requires_future_governed_canonical_write_path`
    - `job_publish_requires_future_governed_canonical_write_path`

结论：

- 本次 Task 25 的核心后端缺口之一，是把 `Task 23` 已经存在的 governed intake review 能力扩展到 `company/job`。
- 扩展时必须保持“AI outputs claims, not facts”与“backend owns truth”，即：
  - AI 先产出 clean facts / missing questions / draft。
  - 只有经 review 批准的字段，才能通过受控 publish 写入 `Company` / `Job`。
  - 写入必须是服务层执行，并留下 audit / workflow 记录。

### 4. Client 门户目前几乎为空白

- `apps/web/src/App.tsx`
  - 当前 `ClientPortal()` 只有 client-safe candidate card 入口。
  - 目前没有 `/client/company-profile`、`/client/jobs/new`、`/client/jobs/:jobId/clarification` 等真实页面。
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary`
  - 当前只有 `ClientSafeCandidateCardController`；没有 `client` 子包，也没有真正的 client company/job API。

结论：

- 若本次要兑现“client creates company profile”，必须新增最小 client API 和最小 client portal 页面。
- 该 client 流程应是**受限输入与提交入口**，而不是让 client 直接激活 job 或绕过顾问 review。

### 5. Task 25 与 Task 26 的边界必须显式锁死

- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
  - 已有 job/candidate/shortlist/consent/disclosure 等 transition legality policy。
  - 但当前更多是 policy/audit 基础，不等于完整 workflow engine。

本次边界决策：

- `Task 25` 只补**岗位 intake 相关激活门禁**与必要 workflow append。
- 不在本次引入通用 transition engine、通用 blocker orchestration、SLA engine、placement/commission 状态机运行时。
- 只要涉及 `job activation`、`job more info requested`、`job commercial pending` 这类 intake 直接相关状态，就可以在 Task 25 内做**窄而真实**的服务层规则与 append。

## Assumptions & Decisions

- `Client` 本次只获得最小真实输入能力：
  - company profile 提交
  - JD / job brief 提交
  - clarification 回答
  - 查看自身提交状态
- `Consultant` 仍是唯一可执行以下动作的角色：
  - 审核 AI draft
  - 生成/确认 scorecard
  - 处理 blocker
  - 执行 activation
- `company/job` 的 AI 受控写回不走“通用 canonical profile 引擎”，而是为 `Company` 与 `Job` 实体补一条**受 review、受 audit、受 workflow 保护的 domain publish 路径**。
- `commercial terms placeholder` 采用最小可交付模型：
  - `feeModel`
  - `feeRangeOrRate`
  - `paymentTerms`
  - `exclusivity`
  - `contractStatus`
  - `notes`
  - 优先落在 `job.commercialTerms` 与 `metadata`，仅在实现中证明确实不够时再加 migration。
- 本次不实现：
  - 复杂合同审批
  - 自动激活 job
  - client 直接改写 canonical company/job 真值
  - 通用工作流引擎
  - shortlist/client feedback/unlock 等后续任务内容

## Proposed Changes

### 1. 建立 Task 25 的后端 intake/application 编排层

**目标**

把目前分散的 company/job CRUD、AI intake task、governed intake review、job 状态 vocabulary 整理成 Task 25 专属的 application/service 流程，让“client/consultant 提交 -> AI draft -> review -> clarification -> publish -> activation”有统一入口。

**涉及文件**

- 复用并扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantApiCommandService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/GovernedAiIntakeOrchestrator.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/IntakeReviewDecisionService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/tasks/companyintake/*`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/aitaskrunner/tasks/jobintake/*`
- 新增建议文件：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/company/service/CompanyIntakeApplicationService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobIntakeApplicationService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobActivationGateService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobActivationGateResult.java`

**影响范围**

- 收敛 Task 25 application 规则，避免 controller 或前端直接拼流程。
- 明确哪些输入来自 client、哪些来自 consultant、哪些来自 AI review/publish。
- 为后续 client API、consultant UI、workflow append 提供同一套后端决策入口。

**验证方式**

- application service 层可单测覆盖：
  - client intake 进入 packet
  - consultant review 后 publish
  - activation gate 返回 allow/block + reasons
- 不新增前端绕过服务层的状态切换路径

### 2. 补齐 company/job 的 governed review -> publish 受控写回路径

**目标**

解除当前 `IntakeReviewDecisionService` 对 `COMPANY` 与 `JOB` 的 fail-closed 占位，交付真实的受控 publish 路径，让 review 后的 clean facts 能写入 `Company` / `Job`，但仍然保持 claim/fact 边界和 audit。

**涉及文件**

- 必改：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/IntakeReviewDecisionService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governedintake/service/IntakeCanonicalWriteBridgeService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governedintake/IntakeCanonicalWriteBridgeRequest.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governedintake/IntakeCanonicalWriteBridgeResult.java`
- 复用并扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/company/service/CompanyService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/company/Company.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/Job.java`
- API boundary：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantIntakePublishRequest.java`
  - 相关 response DTO / mapper

**影响范围**

- 为 `COMPANY` 与 `JOB` 定义 review-approved clean fact 到实体字段的稳定映射。
- publish 仅允许：
  - 已批准 review 的 claim
  - 无冲突或已被人工接受的目标字段
  - 通过 job/company 对应 gate 的字段
- 需要把写回结果以治理结果返回给前端，而不是“静默成功”。
- 对不支持的字段必须继续 fail-closed，不允许 AI output 直接全量覆盖 entity。

**验证方式**

- 新增/更新后端测试覆盖：
  - company/job packet review 后可 publish 到真实 entity
  - 未审批 / 冲突 / 字段不支持时 fail-closed
  - publish 不绕过 service 层，不绕过 audit
- 保留 candidate 路径现有行为不回归

### 3. 交付 Job clarification、scorecard 生成与 activation gate

**目标**

把现在顾问端页面上“缺什么字段”的静态 checklist，升级成真正的后端 activation gate 与 clarification 机制，满足“AI 生成 job draft 和 missing questions；顾问审查后才能激活 job”的任务验收。

**涉及文件**

- 后端：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/JobStatus.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantJobController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/JobCreateRequest.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/JobUpdateRequest.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/JobScorecardCreateRequest.java`
  - 新增 `JobActivationRequest / JobClarificationQuestionResponse / JobActivationGateResponse` 等 DTO
- 复用 workflow 基础：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/workflowaudit/WorkflowTransitionLegalityPolicy.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWorkflowSurfaceService.java`

**影响范围**

- 后端统一返回：
  - clarification questions
  - missing / uncertain fields
  - scorecard completeness
  - commercial placeholder completeness
  - activation allowed / blocked reasons
- 顾问执行 activation 时必须：
  - 服务层验证 gate
  - append 对应 `WorkflowEvent`
  - 更新 job 状态到允许的 intake 完成状态
- 本次只覆盖 job intake 相关状态切换，不做完整通用 workflow engine。

**验证方式**

- 测试证明：
  - 缺 scorecard / 缺关键字段 / 缺商业占位时不能激活
  - activation 成功后有对应 workflow audit
  - 非法状态跳转依旧 fail-closed

### 4. 新增 Client API 边界：company profile、job intake、clarification

**目标**

在现有只有 `client-safe candidate card` 的情况下，补出 Task 25 需要的最小 client 端 API，让 Client 能真实创建 company profile 和提交 job intake，但不获得顾问端内部真值控制权。

**涉及文件**

- 新增目录与控制器：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientCompanyController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientJobController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientApiCommandService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/client/ClientApiQueryService.java`
- 新增 request/response DTO：
  - `ClientCompanyProfileCreateRequest.java`
  - `ClientCompanyProfileResponse.java`
  - `ClientJobIntakeCreateRequest.java`
  - `ClientJobClarificationResponse.java`
  - `ClientJobSubmissionStatusResponse.java`
- 可能扩展：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityaccess/FieldAccessPolicy.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/ApiBoundaryContractRules.java`
  - controller allowlist / leakage tests

**影响范围**

- 提供最小 API：
  - `POST /api/client/company-profile`
  - `GET /api/client/company-profile`
  - `POST /api/client/jobs`
  - `GET /api/client/jobs/{jobId}`
  - `POST /api/client/jobs/{jobId}/clarification`
- Client 只能读写自身输入与安全状态，不可直接：
  - 激活 job
  - 查看顾问内部 review 细节
  - 写入 raw candidate/company/job 内部字段

**验证方式**

- 新增 WebMvc / leakage / org-scope 测试：
  - Client 可提交自己的 company/job intake
  - 错角色、跨组织、越权字段 fail-closed
  - API 返回 DTO 不暴露顾问内部字段

### 5. 扩展顾问端与顾问 API：company/job AI intake review、clarification、activation

**目标**

把现有顾问端 company/job CRUD 与 upload/review 页面补成真实 Task 25 工作台，让顾问能消费 client/consultant 提交的 company/job packet，完成 review、clarification、scorecard、commercial placeholder、activation。

**涉及文件**

- 前端：
  - `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`
  - `apps/web/src/api/consultantIntake.ts`
  - `apps/web/src/api/consultantJobs.ts`
  - `apps/web/src/api/consultantCompanies.ts`
  - 必要时新增拆分文件：
    - `apps/web/src/features/consultant-portal/job-intake/*.tsx`
    - `apps/web/src/features/consultant-portal/company-intake/*.tsx`
- 后端：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantIntakeController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantJobController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantCompanyController.java`

**影响范围**

- 顾问端页面需要新增或强化：
  - company/job packet intake queue 过滤
  - company/job review 页的 clean facts / source highlight / decisions / publish
  - clarification questions 面板
  - commercial placeholder 编辑区
  - activation gate 结果与 blocker reasons
- 顾问端 `job detail` 与 `job intake` 页面不再依赖前端自己拼 checklist。
- 顾问端 `company detail` 需要能看到 client 提交来源与顾问补充字段边界。

**验证方式**

- 顾问可真实完成：
  - upload or receive packet
  - extract
  - review decision
  - publish 到 company/job
  - 补齐 clarification / scorecard / commercial placeholder
  - activation
- 页面在 `401/403/404/400/503` 下均安全失败

### 6. 交付最小真实 Client 门户页面与前端 API

**目标**

把当前几乎空白的 Client 门户扩展到 Task 25 所需最小页面，使 “client creates company profile” 和 “AI job intake from JD” 具有真实入口。

**涉及文件**

- 必改：
  - `apps/web/src/App.tsx`
  - `apps/web/src/styles.css`
- 新增建议文件：
  - `apps/web/src/api/clientCompanies.ts`
  - `apps/web/src/api/clientJobs.ts`
  - `apps/web/src/features/client-portal/ClientPortalHome.tsx`
  - `apps/web/src/features/client-portal/ClientCompanyProfilePage.tsx`
  - `apps/web/src/features/client-portal/ClientJobCreatePage.tsx`
  - `apps/web/src/features/client-portal/ClientJobClarificationPage.tsx`
  - `apps/web/src/features/client-portal/ClientSubmissionStatusPage.tsx`

**影响范围**

- 新增最小 client 路由：
  - `/client`
  - `/client/company-profile`
  - `/client/jobs/new`
  - `/client/jobs/:jobId`
  - `/client/jobs/:jobId/clarification`
- 页面能力：
  - company profile 表单
  - JD / job brief 提交
  - clarification 回答
  - 查看提交状态与顾问待处理/需补充提示
- 不在本次加入 shortlist/unlock/feedback 等后续任务页面。

**验证方式**

- Client 使用 access token 可完成最小提交流程
- 未认证 / 越权 / 错组织 fail-closed
- 现有 `/client/candidate-cards/:anonymousCardRef` 不回归

### 7. 最小 commercial placeholder 模型与双端呈现

**目标**

在不引入合同系统的前提下，把 `commercial terms placeholder` 做成真实可编辑、可校验、可作为 activation blocker 的字段组。

**涉及文件**

- 后端：
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/Job.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/job/service/JobService.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/JobCreateRequest.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/JobUpdateRequest.java`
  - 必要时新增 `CommercialTermsPlaceholder` 值对象与 mapper
- 前端：
  - `apps/web/src/api/consultantJobs.ts`
  - `apps/web/src/api/clientJobs.ts`
  - `apps/web/src/features/consultant-portal/ConsultantPortal.tsx`
  - `apps/web/src/features/client-portal/*`

**影响范围**

- 占位字段最小集合进入：
  - consultant job detail/intake 页面
  - client job create / clarification 页面
  - activation gate 检查结果
- 允许结构化保存，但明确文案标注为 placeholder，不代表合同已生效。

**验证方式**

- 缺失必要商业字段时 activation gate 阻断
- 已填写字段可在 client/consultant 双端按各自权限安全显示
- 不出现“系统自动承诺商业条款”的误导文案

### 8. 回归测试、泄漏测试与端到端验证

**目标**

用聚焦测试证明 Task 25 的新增链路成立，并确保没有破坏现有 Task 23/24 能力与权限边界。

**涉及文件**

- 后端测试：
  - `services/core-api/src/test/java/**/Consultant*Controller*`
  - `services/core-api/src/test/java/**/Client*Controller*`
  - `services/core-api/src/test/java/**/IntakeReviewDecisionService*`
  - `services/core-api/src/test/java/**/Job*Service*`
  - `services/core-api/src/test/java/**/Workflow*`
  - 必要的 PostgreSQL/Testcontainers integration tests
- 前端：
  - `apps/web/src/api/*.test.ts`（如现有模式适合）
  - 关键交互可先以 typecheck/build + 手工路径验证为主

**影响范围**

- 覆盖核心验收：
  - company/job AI draft 经 review 后可 controlled publish
  - job clarification / scorecard / commercial placeholder 未满足时不能 activation
  - client 可提交 company profile / JD intake，但不能越权激活
  - workflow/audit append 对关键状态变化可见
  - consultant/client 双端 API 不泄漏内部字段

**验证方式**

- `git diff --check`
- `npm run typecheck:web`
- `npm run build:web`
- `docker info`
- `PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test`
- 如需聚焦回归，可先跑：
  - consultant/client controller suites
  - governed intake publish suites
  - job/company service + workflow legality suites

## Verification Steps

1. Consultant 创建或接收 company/job intake，不需要脱离产品界面使用 Postman。
2. Client 能在真实 Client 门户中提交 company profile 与 job/JD intake。
3. AI 对 company/job 产出 draft、missing questions、scorecard draft 后，顾问必须 review 才能 publish。
4. `COMPANY` 与 `JOB` packet 的 publish 不再报 `future_governed_canonical_write_path`，而是返回真实受控结果。
5. job 在缺少关键字段、clarification、scorecard 或 commercial placeholder 时不能激活。
6. job 激活后具有对应 workflow/audit 记录；非法跳转继续 fail-closed。
7. Client API 只暴露 client-safe / self-owned submission 字段，不泄漏顾问内部 review 数据。
8. 现有 consultant portal、client-safe candidate card、candidate publish 路径不回归。

