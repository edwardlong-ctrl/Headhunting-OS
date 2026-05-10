# Post-Task 14 完整后续计划书

## 从 Production Kernel 到产品 100% 实现

这份计划书从 Task 14 之后开始。它的第一原则是诚实：

Task 0-14 完成的是 **Production Kernel**，不是完整产品，也不是可以直接商业 pilot 的系统。

Production Kernel 的价值很大：它已经把 backend-owned truth、Claim Ledger、ReviewEvent、WorkflowEvent、CanonicalWriteGate、CandidateProfile 最小写入、client-safe projection、RBAC/ABAC 内核、AITaskRun 元数据、MatchReport 合同、Consent/Disclosure 后端保护和五端 route shell 建起来了。

但是从 v2.1/v2.0 的完整规格来看，现在还缺真实日常工作流、真实 AI 执行、真实文档解析、完整五端页面、生产身份权限、部署、安全、通知、workflow engine、matching engine、shortlist、candidate/client loop、placement/commission、治理 dashboard 和 E2E 验收。

所以后续路线必须分成两段：

```text
Task 15-42：从 Production Kernel 到 Usable v1 / Controlled Commercial Pilot Ready
Task 43-60：从 Usable v1 到 v2.1/v2.0 当前规格 100% 实现
```

Task 42 不是 100%。Task 42 是“可以小范围给真实顾问、真实企业、真实候选人受控使用”的门槛。

Task 60 才是这份计划中定义的“当前 v2.1/v2.0 规格 100% 实现”。

## 当前真实完成度

| 目标口径 | 当前估算 | 判断 |
| --- | ---: | --- |
| Production Kernel | 85-90% | 可信内核已经基本成型，但还有 blocked canonical attempt ledger、schema cleanup 等内核相邻缺口。 |
| Usable v1 / Controlled Pilot | 25-30% | 安全地基已经有了，但真实用户每天能用的业务闭环还没完成。 |
| v2.1/v2.0 Full Product | 30% +/- 5% | 后端地基较深，所以不是 10% demo；但完整产品还远未完成。 |

这个估算刻意不把“后端合同存在”当成“产品功能完成”。一个功能只有同时具备持久化、服务层门禁、安全 API、权限、UI 工作流、审计和验收证据，才算产品完成。

## 必须长期坚持的硬约束

- v2.1 是当前产品 source of truth。
- v2.0 是 UI / portal baseline，不能删除、压缩或替代。
- Consultant 始终是一个统一端口。
- Backend owns truth。
- PostgreSQL 是目标事实源。
- AI outputs claims, not facts。
- Raw input is not fact。
- Extraction output is not fact。
- ClaimLedgerItem 是 claim，不是 fact。
- ReviewEvent 是 review evidence，不是 fact promotion。
- Risk-tiered human review 必须在 canonical write 之前。
- CanonicalWriteGate 不能绕过。
- 每个关键状态变化必须创建 WorkflowEvent。
- 每个 AI-assisted state transition 必须创建 AITaskRun 和 WorkflowEvent。
- Client 在 unlock/disclosure 之前不能读 raw Candidate / raw CandidateProfile。
- L3 consented detail 不是 L4 identity disclosure。
- Bulk approve 不能产生 candidate_confirmed 或 external_verified。
- AI 不能自动披露身份、自动 unlock、自动拒绝候选人、自动承诺 offer/薪资/商业条款、自动覆盖人工确认事实。

## 对上一版 ChatGPT 建议的判断

我认同它的核心方向：Task 14 之后必须从“继续完善抽象架构”转向“做真实可使用产品”。它对 Usable v1 的定义也基本正确：真实顾问、真实企业、真实候选人能在受控范围内跑完整交易链路。

但那份建议不能照抄，因为它对当前 repo 状态有几个过时判断：

- 它说 transaction boundary 只是 skeleton；现在已经有 Spring/JDBC transaction boundary 和 rollback tests。
- 它说没有 API DTO/client-safe contract；现在已有 API-safe envelope/DTO 和一个 client-safe candidate-card endpoint。
- 它说没有 CandidateProfile persistence；现在已有最小 CandidateProfile persistence 和一个 gated field write path。
- 它说 RBAC/ABAC 完全没有；现在已有 backend policy/evaluator/enforcer kernel，但还不是 production auth/session。
- 它说 AITaskRun 只是 skeleton；现在已有 metadata persistence 和 governance policy，但还没有 real execution。
- 它说 matching/privacy/consent 基本不存在；现在已有 backend kernel 和 placeholder，但还不是完整产品工作流。

所以正确路线是：保留它的产品化方向，但把 Task 15 之后按当前真实基线重排。

## 阶段总览

| 阶段 | Tasks | 目标 | 完成后状态 |
| --- | --- | --- | --- |
| Phase 0 | 0-14 | Production Kernel | 已完成当前 kernel scope |
| Phase A | 15 | Product Readiness Bridge | kernel/product/pilot/100% 边界清楚 |
| Phase B | 16-20 | Operational Core | 真实业务对象、API、auth、document storage 基础 |
| Phase C | 21-24 | Real AI Intake | 真实 AI task、文档证据、claim review、顾问端 intake |
| Phase D | 25-34 | Recruiting Transaction Core | 岗位、workflow、matching、shortlist、redaction、candidate/client loop、unlock |
| Phase E | 35-42 | Pilot Operations | feedback、placement、governance、seed、deployment、observability、security、E2E pilot gate |
| Phase F | 43-60 | Full Product Completion | v2.1/v2.0 当前规格 100% 实现 |

## P0 / P1 / P2 优先级

P0：没有它不能进入 controlled pilot。

- Task 15 Product Readiness Bridge
- Task 16 Real Product Data Model Completion
- Task 17 Canonical Write Audit and Blocked Attempt Ledger
- Task 18 Product API Layer v1
- Task 19 Identity/Auth/RBAC/ABAC Production v1
- Task 20 Document Storage and SourceItem v1
- Task 21 Real AI Task Runner v1
- Task 23 Governed AI Intake End-to-End
- Task 24 Consultant Portal v1
- Task 25 Company and Job Intake v1
- Task 26 Workflow Engine v1
- Task 27 Matching and Evidence v1
- Task 29 Shortlist Builder v1
- Task 30 Privacy Redaction and Re-identification v1
- Task 31 Candidate Portal v1
- Task 32 Client Portal v1
- Task 33 Consent/Disclosure/Unlock End-to-End
- Task 39 Deployment v1
- Task 41 Security and Privacy Hardening v1
- Task 42 Pilot E2E Acceptance Gate

P1：没有它可以内部跑，但 pilot 体验、可信度或治理会明显不足。

- Task 22 Document Intelligence and Evidence Retrieval v1
- Task 28 Semiconductor Industry Pack v1
- Task 34 Notification and Follow-up System v1
- Task 35 Interview Feedback and Outcome Loop v1
- Task 36 Placement and Commission v1
- Task 37 Owner and Admin Governance v1
- Task 38 Pilot Seed Data and Import Tools
- Task 40 Observability, Audit, and Replay v1

P2：可以在 Usable v1 后继续增强，但必须在 100% 完成前补齐。

- Task 43-60。

## 最短可用路线

如果目标是最快进入受控真实 pilot，不要平均推进所有模块。建议最短路线是：

```text
15 -> 16 -> 17 -> 18 -> 19 -> 20
-> 21 -> 22 -> 23 -> 24
-> 25 -> 26 -> 27 -> 28 -> 29 -> 30 -> 31 -> 32 -> 33 -> 34
-> 38 -> 39 -> 41 -> 42
```

Task 35、36、37、40 可以根据 pilot 需要提前插入。如果 pilot 直接涉及 placement、commission、Owner 治理或生产事故响应，它们就不能后置。

# Phase A：Product Readiness Bridge

## Task 15：Product Readiness Bridge

优先级：P0

目标：把 Task 0-14 的 kernel 变成产品化执行基线，明确哪些已完成、哪些只是 kernel、哪些必须进入真实产品能力。

必须交付：

- `docs/roadmap/productization-roadmap.md`
- `docs/roadmap/productization-roadmap.zh-CN.md`
- `docs/roadmap/pilot-readiness-checklist.md`
- `docs/roadmap/product-scope-after-kernel.md`
- v2.1 能力的 complete / partial / missing / forbidden-to-fake 分类。
- Usable v1 E2E 验收场景。

禁止范围：

- 不写业务代码。
- 不改 v2.1/v2.0 产品规格书。
- 不把 Task 14 说成完整产品完成。

完成标准：

- 文档明确 Task 14 是 Production Kernel，不是 full product。
- 文档明确 Task 42 是 Usable v1 gate。
- 文档明确 Task 60 是当前规格 100% gate。
- 文档保留 v2.1 source of truth、v2.0 UI baseline、统一 Consultant portal、AI claims not facts、Backend owns truth、Client before unlock/disclosure cannot read raw Candidate。

# Phase B：Operational Core

## Task 16：Real Product Data Model Completion

优先级：P0

目标：从 kernel 表和 CandidateProfile 最小写入，扩展到真实业务对象。

当前基线：

- `recruiting.candidate` 和 `recruiting.candidate_profile` 已存在。
- CandidateProfile 最小 field write 已存在。
- Company / Job / Shortlist / Placement 等还不是完整产品对象。

必须交付：

- Candidate aggregate。
- CandidateProfile 完整字段族、profile version、status、lineage、stale、conflict。
- CandidateDocument metadata。
- Company / CompanyContact / CompanyPreference。
- Job / JobRequirement / JobScorecard。
- CandidateCompanyInteraction。
- Shortlist / ShortlistCandidateCard。
- InterviewFeedback / Placement / Commission 基础表。
- canonical field 到 ClaimLedgerItem、ReviewEvent、SourceItem、InformationPacket、AITaskRun、WorkflowEvent、source span 的 lineage。

禁止范围：

- 不接真实 AI。
- 不做 UI。
- 不绕过 CanonicalWriteGate。
- 不让 Client 读 raw Candidate。

验收标准：

- 核心对象可以组织隔离地持久化和读回。
- CandidateProfile 每个核心字段有 verification status、source lineage、profile version。
- canonical write 仍必须通过 domain service 和 CanonicalWriteGate。

## Task 17：Canonical Write Audit and Blocked Attempt Ledger

优先级：P0

目标：补齐当前内核缺口：gate-blocked canonical attempts 也要可审计，而不是只有 allowed writes 有 WorkflowEvent。

必须交付：

- CanonicalWriteAttempt 或等价 audit ledger。
- allow / block / require_review 都要记录。
- blocked attempt 不写事实，但可查、可统计、可回放。
- canonical write、WorkflowEvent、attempt audit 的事务一致性。
- 重复请求 idempotency。

禁止范围：

- 不削弱 CanonicalWriteGate。
- 不让 AI 直接写 canonical。
- 不在 controller 里手写事务绕过服务层。

验收标准：

- canonical write 成功但 WorkflowEvent 失败时整笔 rollback。
- WorkflowEvent 成功但 canonical write 失败时整笔 rollback。
- gate block 时无事实变更，但 blocked attempt 可审计。

## Task 18：Product API Layer v1

优先级：P0

目标：从一个 narrow client-safe card endpoint 扩展到五端真实 API。

必须交付：

- `/api/consultant/*`
- `/api/client/*`
- `/api/candidate/*`
- `/api/owner/*`
- `/api/admin/*`
- DTO mapping，不暴露 internal entity。
- pagination / filtering / sorting / search baseline。
- error / validation contract。
- API leakage tests。

禁止范围：

- 不用 entity 直接当 response。
- 不做 raw Candidate/Profile client endpoint。
- 不用前端隐藏替代后端权限。

验收标准：

- Client shortlist/candidate detail 只能拿到 client-safe DTO。
- unsafe path ref、raw UUID candidate ref、missing auth context 都 fail closed。

## Task 19：Identity / Auth / RBAC / ABAC Production v1

优先级：P0

目标：把 backend access kernel 变成 production identity/access control。

必须交付：

- User / Organization / Membership / RoleAssignment / Session。
- Spring Security 或等价 backend-owned auth layer。
- Consultant agency org membership。
- Client company org membership。
- Candidate self-owned identity。
- Admin/System role separation。
- field-level ABAC 接入 API/service。
- cross-org / ID enumeration negative tests。
- 移除 production path 的临时 header-based auth context。

禁止范围：

- 不做复杂 SSO。
- 不做 billing/multi-tenant commercial scope。
- 不允许前端隐藏替代后端权限。

验收标准：

- Client 即使知道 candidate/profile id，也不能读 raw Candidate/Profile。
- Candidate 只能读 self-scoped 数据。
- Admin/System 不能靠角色绕过 canonical/disclosure gate。

## Task 20：Document Storage and SourceItem v1

优先级：P0

目标：让系统能真实接收 CV、JD、notes、截图、反馈文件，同时保持 raw input is not fact。

必须交付：

- Candidate CV upload。
- Job JD upload。
- Consultant notes upload。
- Client feedback upload。
- Object storage abstraction。
- File hash / duplicate detection。
- MIME/type/size validation。
- malware scan placeholder 或 integration boundary。
- document text extraction boundary。
- source span addressing。
- access-controlled download/read。

禁止范围：

- 不把大文件塞进 canonical fact table。
- 不让 upload 直接生成 confirmed fact。
- 不让 AI 读无权限文件。

验收标准：

- 上传 CV 后只创建 SourceItem/InformationPacket。
- confirmed CandidateProfile 必须经过 extraction、claim、review、canonical gate。

# Phase C：Real AI Intake

## Task 21：Real AI Task Runner v1

优先级：P0

目标：把 AITaskRun metadata governance 变成真实可执行、可回放、可审计的 AI task runner。

必须交付：

- AI provider abstraction。
- ModelRouter。
- Prompt registry。
- input/output schema validation。
- queued/running/succeeded/failed/cancelled lifecycle。
- retry/failure state。
- cost/latency/tool-call logging。
- replay support。
- deterministic test provider。
- write-back target enforcement。

禁止范围：

- AI response 不直接写 canonical。
- prompt 不写死在 controller。
- model vendor 不是事实源。
- AI 不能 self-approve。

验收标准：

- Candidate Profile Parser 完整记录 input、output、model、prompt version、schema version、failure reason、cost、latency、source refs、write-back target、human review status。

## Task 22：Document Intelligence and Evidence Retrieval v1

优先级：P1；证据密集 pilot 中升为 P0

目标：接入真实文档解析、chunk、source highlight、evidence retrieval。

必须交付：

- document parsing service interface。
- parsed document chunk model。
- source span mapping。
- OCR/STT/file-conversion worker boundary。
- evidence retrieval API。
- RAGFlow adapter 或 internal adapter。
- parsed document audit metadata。
- evidence retrieval permission checks。

禁止范围：

- RAG 不能成为事实源。
- parsed chunk 不能直接写 canonical fact。
- 不做泛聊天主入口。

验收标准：

- “候选人做过 UVM coverage closure”必须能指回 CV/note source span。

## Task 23：Governed AI Intake End-to-End

优先级：P0

目标：打通真实 AI intake：upload/source -> AI extraction -> Claim Ledger -> review -> canonical write。

必须交付：

- Candidate intake flow。
- Company intake flow。
- Job intake flow。
- Claim Ledger Builder task integration。
- Conflict Detector integration。
- Entity Resolver integration。
- Canonical Record Builder integration。
- review API。
- approve / reject / needs-follow-up。
- Clean Facts mode 和 Source Highlight mode。

禁止范围：

- 不自动创建正式 candidate/company/job。
- 不覆盖 human-confirmed facts。
- 不把 weak-signal intent 写成 confirmed intent。

验收标准：

- 顾问上传 CV + 微信 note + call note 后，系统生成 claims；只有通过 risk-tiered review 的字段才进入 canonical profile。

## Task 24：Consultant Portal v1

优先级：P0

目标：让顾问端成为真实主战场。

必须交付：

- dashboard。
- AI Intake center。
- intake review，支持 Clean Facts / Source Highlight。
- Talent Pool list/detail。
- Company list/detail。
- Job list/detail。
- Matching review。
- Shortlist builder entry。
- Follow-up queue。
- Workflow timeline。
- Risk/blocked actions panel。
- Audit drawer。

禁止范围：

- 不做两个 Consultant portal。
- 不做静态 mock workflow。
- 不绕过 backend-approved API。

验收标准：

- 顾问不用 Postman，只靠 UI 完成 candidate intake review 并发布到 Talent Pool。

# Phase D：Recruiting Transaction Core

## Task 25：Company and Job Intake v1

优先级：P0

目标：让企业需求真实进入系统，并形成可匹配的 JobScorecard。

必须交付：

- consultant creates company。
- client creates company profile。
- manual job intake。
- AI job intake from JD。
- job clarification questions。
- scorecard generation。
- consultant activation gate。
- commercial terms placeholder。

禁止范围：

- 不自动激活岗位。
- 不自动承诺商业条款。
- 不把 JD 原文当最终 scorecard。

验收标准：

- 企业上传 JD 后，AI 生成 job draft 和 missing questions；顾问审查后才能激活 job。

## Task 26：Workflow Engine v1

优先级：P0

目标：从 WorkflowEvent append/audit 升级为真实状态机。

当前状态：

- 主线已在 `c63d79a` 实现 transition legality validation、blocker 模型、consultant workflow timeline 增强和 entity-state preview。
- SLA due date placeholder 与自动化规则在 Task 26 时仍然延后；Task 45
  现在已补上第一版 deterministic backend-owned SLA baseline。

必须交付：

- Job state machine。
- Candidate state machine。
- Shortlist state machine。
- Consent state machine。
- Disclosure state machine。
- Placement/Commission state machine baseline。
- transition validator。
- blocker reasons。
- SLA due date placeholder。
- workflow timeline API。

禁止范围：

- 不做复杂 BPMN。
- 不允许直接 update state 不写 WorkflowEvent。
- 不让前端决定 transition legality。

当前实现快照：

- Job/Candidate/Shortlist/Consent/Disclosure 的 legality preview 已返回稳定 target status 与 blocker reasons。
- consultant workflow timeline 已暴露真实 `beforeStatus` / `afterStatus`。
- consultant workflow `entity-state` 已暴露 `currentStatus`、legal next actions 与 blockers。
- disclosure preview 已复用真实 prerequisite gate，而不是只依赖静态状态机输出。
- Placement/Commission workflow state read-model baseline 已补齐。

验收标准：

- Candidate 不能从 `new` 直接跳到 `identity_disclosed`。
- Disclosure 不能绕过 consent 和 consultant approval。

## Task 27：Matching and Evidence v1

优先级：P0

目标：把 matching contracts 变成持久化、可解释、证据驱动的 matching engine。

必须交付：

- MatchReport persistence。
- dimension score model。
- evidence coverage calculation。
- provenance weighting v1。
- score cap enforcement。
- authenticity risk v1。
- match explanation generator。
- interview question generator。
- negative case tests。

禁止范围：

- 不用简历关键词堆砌给高分。
- cold pack 不给 5。
- 无 evidence 的解释不能给 client 看。

验收标准：

- 只有 CV keyword、没有项目证据的候选人，Technical Fit 不能给 5。

## Task 28：Semiconductor Industry Pack v1

优先级：P1；半导体 pilot 中升为 P0

目标：先把一个行业包做真，不假装 8 个行业都 production-ready。

必须交付：

- DV/Verification、PD、DFT、Analog/Mixed Signal、Firmware/Embedded。
- SkillConcept seed data。
- anti-pattern definitions。
- scorecard templates。
- interview question templates。
- evidence examples。
- cold/seeded maturity rules。
- OntologyVersion persistence。

禁止范围：

- 不做 8 个行业假深度。
- 不把 software QA 当 IC verification。
- 不把 PCB layout 当 physical design。

验收标准：

- software testing 简历匹配 DV 岗位时，系统降分并解释 anti-pattern。

## Task 29：Shortlist Builder v1

优先级：P0

目标：让顾问从 MatchReport 生成 client-safe shortlist。

必须交付：

- Shortlist draft。
- candidate selection。
- anonymous candidate card generation。
- comparison table。
- pre-send checks。
- client-safe summary generation。
- PDF/email/WeChat-safe summary placeholder。
- consultant approval before send。
- audit events。

禁止范围：

- AI 不自动发送 shortlist。
- unlock 前不展示姓名、联系方式、完整 LinkedIn。
- 不展示高反推风险细节。

验收标准：

- 顾问手动确认且 privacy gates 通过后，shortlist 才能从 draft 变成 sent_to_client。

## Task 30：Privacy Redaction and Re-identification v1

优先级：P0

目标：从 placeholder privacy check 升级为真实匿名摘要风险控制。

必须交付：

- ReidentificationRiskAssessment persistence。
- unsafe feature detector。
- company name generalization。
- project/product/chip name generalization。
- rare title/year risk rules。
- L0/L1/L2/L3/L4 summary generation pipeline。
- client-safe summary gate。
- redaction audit event。

禁止范围：

- 不允许手动绕过 high-risk redaction。
- 候选人授权前不展示专利、论文、公开演讲、唯一成就。
- client 看不到 internal-only evidence。

验收标准：

- “头部芯片公司 + unique title + exact year + chip code name”必须被泛化或阻断。

## Task 31：Candidate Portal v1

优先级：P0

目标：让候选人真实参与资料确认、follow-up、机会确认和授权。

必须交付：

- Candidate home。
- resume/profile document upload。
- AI extracted profile review。
- follow-up form。
- opportunity view。
- consent request。
- consent text versioning。
- shared fields preview。
- status timeline。

禁止范围：

- 不做开放职位市场。
- 不让候选人看其他候选人或客户内部数据。
- 不让一次 consent 被无限复用到其他岗位。

验收标准：

- candidate consent 绑定 opportunity、profile version、consent text version、shared fields。

## Task 32：Client Portal v1

优先级：P0

目标：让企业客户真实完成岗位、澄清、shortlist、unlock、feedback。

必须交付：

- Client dashboard。
- manual job creation。
- JD upload job creation。
- clarification answers。
- shortlist review。
- anonymous candidate detail。
- unlock request。
- interview feedback。
- client profile/preferences。

禁止范围：

- 不显示 raw Candidate。
- 不显示联系方式。
- 不显示 consultant internal notes。
- 不允许 client 直接披露候选人身份。

验收标准：

- Client 完成 create job -> clarification -> shortlist review -> unlock request -> feedback。

## Task 33：Consent / Disclosure / Unlock End-to-End

优先级：P0

目标：把后端 consent/disclosure kernel 变成完整交易保护 workflow。

必须交付：

- consent request。
- candidate consent confirmation。
- consent versioning。
- shared-fields preview。
- client unlock request。
- consultant approval。
- PriorContactClaim。
- PriorApplicationClaim。
- fee protection placeholder。
- DisclosureRecord generation。
- unlock/disclosure WorkflowEvent chain。
- L4 gate 后的 identity-disclosed client read behavior。

禁止范围：

- AI 不 unlock。
- AI 不 disclose identity。
- Client 不绕过 consultant approval。
- 没有 consent_confirmed 不生成 L4 identity disclosure。

验收标准：

- Client 请求 unlock 时，如果 consent missing/expired/revoked/mismatched，必须阻断。

## Task 34：Notification and Follow-up System v1

优先级：P1；低接触 pilot 中升为 P0

目标：让系统主动推动流程，而不是靠顾问刷新页面。

当前执行说明：

- 在 Task 34 合并交付中，Task 34 已经与 Task 31/19 后续剩余的 candidate-facing portal auth/session hardening 和 profile-participation closure 一起完成。这种合并执行是刻意的：只有 candidate/client portal session 能安全 refresh/logout 之后，candidate/client reminder 才真正可用；candidate follow-up submission 也必须先变成显式 review task，而不是直接改写 canonical fact；最终 reminder 与 follow-up task 现在都能在 candidate/client/consultant portal 中可见，并带有 workflow audit。

必须交付：

- in-app notification。
- email provider abstraction。
- SMS provider abstraction / placeholder。
- candidate follow-up form delivery。
- client clarification delivery。
- reminder schedule。
- notification audit。
- unsubscribe / preference baseline。

禁止范围：

- 不发送敏感信息。
- AI 不自动发送 shortlist/disclosure。
- candidate/client answer 不直接写 canonical。

验收标准：

- 候选人回答薪资/地点/到岗时间后，系统生成 review task，而不是直接覆盖 profile。

# Phase E：Pilot Operations

## Task 35：Interview Feedback and Outcome Loop v1

优先级：P1

目标：把面试反馈和结果回流到 interaction、job outcome 和 suggested updates。

必须交付：

- InterviewFeedback entity。
- structured feedback form。
- AI feedback structurer。
- profile update suggestions。
- company preference update suggestions。
- reject reason taxonomy。
- outcome label。
- match calibration dataset baseline。

禁止范围：

- AI 不自动拒绝候选人。
- AI 不自动覆盖能力事实。
- 单次客户反馈不永久污染全局 ontology。

验收标准：

- “技术不错但 salary mismatch”更新该次 interaction/job outcome，不把候选人全局标记为不适合类似岗位。

## Task 36：Placement and Commission v1

优先级：P1；交易 OS 可信度中升为 P0

目标：从推荐工具推进到交易操作系统。

必须交付：

- offer tracking。
- placement record。
- start date。
- fee rate。
- invoice status。
- payment status。
- guarantee period。
- replacement required。
- commission record。
- revenue dashboard source data。

禁止范围：

- 不做完整财务系统。
- 不自动确认 offer 或 commission。
- 不替代正式合同/发票系统。

验收标准：

- offer_accepted 后，Owner 可以看 expected fee、invoice status、payment status、guarantee status。
- Owner revenue 中的 expected fee / paid fee 必须保持后端真值：同一 placement 下按全部 commissions 聚合；未知金额要显示 known subtotal 与排除数量；缺少 `amount` 的 commission 不能被标记为 paid。

## Task 37：Owner and Admin Governance v1

优先级：P1

目标：让老板和管理员能看系统质量，而不只是业务数字。

必须交付：

- Owner dashboard。
- pipeline/revenue dashboard。
- risk dashboard。
- AI quality dashboard。
- review quality dashboard。
- audit search。
- model routing config UI。
- ontology governance view。
- redaction policy view。
- sample audit queue。

禁止范围：

- 不做前端假数据 dashboard。
- Admin 不绕过 domain service 修改事实。
- 不隐藏 privacy gate override。

验收标准：

- Owner 能看到某顾问 bulk approve ratio 过高，并进入 sample audit records。

## Task 38：Pilot Seed Data and Import Tools

优先级：P1

目标：构建可重复的 pilot 数据，不用真实敏感个人信息做公开 demo。

必须交付：

- realistic synthetic semiconductor candidates。
- 75 talent-pool seed records。
- 5 active jobs。
- 3 under-review jobs。
- company account seed。
- candidate account seed。
- source document seed。
- reset/import/export CLI。
- data quality validation。
- demo/pilot scenario script。

禁止范围：

- 不用真实个人敏感信息做公开 demo。
- seed data 不绕过正常 workflow。

验收标准：

- 一条命令重建 pilot 数据环境，且数据能走正常 UI/API workflow。

## Task 39：Deployment v1

优先级：P0

目标：从本地开发进入真实可访问 staging / production-like environment。

必须交付：

- production config profiles。
- env var validation。
- managed PostgreSQL deployment。
- object storage config。
- backend deployment。
- frontend deployment。
- HTTPS/domain。
- migration runbook。
- rollback docs。
- backup/restore validation。

禁止范围：

- 不 commit secrets。
- 不手工改生产 DB schema。
- 不跳过 migration validation。

验收标准：

- 空数据库开始，运行 migrations + seed + deployment 后，顾问可登录并完成 intake。

## Task 40：Observability / Audit / Replay v1

优先级：P1；pilot 需要 P0 子集

目标：出问题时能查、能回放、能追责。

`68647b5` 之后状态：provider-neutral backend/API/runbook 子集已经在
`main` 完成，包括安全 request correlation、staging/production 结构化日志
pattern、Admin audit search APIs、AITaskRun trace/replay 可见性、Disclosure
audit export 和 incident runbook。error dashboard、AI cost/latency dashboard
UI、外部 observability vendor、product-wide PII log audit 仍然后续延期。

必须交付：

- structured logs。
- request correlation ID。
- AITaskRun trace。
- WorkflowEvent search。
- ReviewEvent search。
- Disclosure audit export。
- error dashboard。
- AI cost/latency dashboard。
- incident runbook。

禁止范围：

- 普通日志不记录明文敏感数据。
- 不允许不可追踪后台修复。
- 不删除 workflow/consent/disclosure audit records。

验收标准：

- 给定 DisclosureRecord，系统能查 requester、approver、consent version、client-safe card version、AI tasks、workflow events。

## Task 41：Security and Privacy Hardening v1

优先级：P0

目标：真实候选人/客户数据进入系统前，风险可控。

必须交付：

- auth/session hardening。
- password/login policy。
- rate limiting。
- file upload security。
- PII masking in logs。
- access audit。
- export permission。
- data retention baseline。
- vulnerability scan baseline。
- privacy regression tests。

禁止范围：

- 不做假安全说明。
- 不把 raw Candidate export 给 Client。
- 无权限不能下载 source document。

验收标准：

- Client 不能通过 ID 枚举、URL 修改、API、export、logs、error message 得到未授权身份信息。

## Task 42：Pilot E2E Acceptance Gate

优先级：P0

目标：判断系统是否真的进入 Usable v1 / Controlled Commercial Pilot Ready。

当前 gate 状态：Task 42 Usable v1 gate 已达到 `CONTROLLED_PILOT_READY`。
Task 42 acceptance-gate model 和报告已有当前执行证据：Task 38 pilot CLI
链路、5 个 seed portal account 的 Playwright 登录覆盖、S01-S08 business-flow
Playwright 覆盖，以及 Task 39 backup/restore validation。详见
`docs/roadmap/task-42-pilot-e2e-acceptance-gate.md`。这不等于 public
production-ready；Tasks 43-60 仍需继续补齐更完整的运营、安全、支持、托管部署
和产品深度。

必须跑通 8 条流程：

1. 顾问上传 CV + note -> AI claims -> review -> canonical profile。
2. 企业/顾问上传 JD -> AI job draft -> clarification -> consultant activation。
3. MatchReport -> evidence-backed explanation -> score cap 生效。
4. 顾问选择候选人 -> anonymous shortlist -> client-safe preview。
5. 候选人收到 opportunity/consent -> 确认授权。
6. 企业查看 shortlist -> 请求 unlock。
7. 顾问批准 unlock -> DisclosureRecord -> identity disclosed。
8. 企业提交 interview feedback -> outcome label -> suggested updates 进入 review。

禁止范围：

- 不用 seed shortcut 绕过 workflow。
- 不手工改数据库制造通过。
- 不隐藏失败 case。

完成标准：

- 8 条流程全部通过。
- negative privacy / permission / canonical-write / AI-boundary tests 通过。
- 可以称为 Usable v1 / Controlled Commercial Pilot Ready。
- 仍不能称为完整商业 SaaS，也不能称为 v2.1/v2.0 100%。

# Phase F：Full Product 100%

## Task 43：Full Portal Depth and UX Completion

目标：补齐 v2.0/v2.1 五端所有页面组，不再只是 pilot path。

必须交付：

- Owner 完整页面组。
- Consultant 完整页面组。
- Client 完整页面组。
- Candidate 完整页面组。
- Admin 完整页面组。
- responsive layout / accessibility baseline。
- empty/loading/error/permission states。
- 跨页面 workflow continuity。

完成标准：

- v2.1/v2.0 命名的每个 route 都有真实 backend-connected page。100% 时核心 route 不再是 shell。

收口状态：route-depth gate 已完成。Owner、Consultant、Client、Candidate、
Admin 的 v2.0/v2.1 route set 均已由
`portalRouteContract.test.ts` 覆盖；client anonymous candidate review 与
candidate opportunity/consent detail route 已按 spec 参数名对齐；
`/admin/integrations` 已接入 Admin governance read boundary。

## Task 44：Full AI Task Registry Production Coverage

目标：实现 v2.1 AI Task Registry 的生产覆盖。

必须交付：

- Base registry Tasks 0.1-13。
- v2.1 governance registry Tasks 14-23。
- input/output schemas。
- prompt versions。
- eval cases。
- human-review policy。
- write-back target policy。
- replay and regression reports。

完成标准：

- Admin 可查看每个 production AI task 的 version、schema、model route、eval result、cost/latency、replay history。

收口状态：registry coverage 与 Admin inspection gate 已完成。v2.1 的 28 个
production AI task definition 均已有 registry task id、version、prompt
version、classpath prompt/schema/eval artifact、human-review policy、
write-back target policy 与可检查的 governed model route。
`/admin/ai-task-registry` 现在按 definition-first 展示每个 production task
的 version、schema、model route、eval result registration、aggregate
cost/latency、failure count 与 replay history count。此收口不新增 worker queue、广泛 write-back execution，
也不宣称每个 registry-only task 都已经具备完整业务执行器。

## Task 45：Full Workflow Automation and SLA Engine

目标：从合法状态机扩展到业务自动化。

必须交付：

- SLA rules。
- reminder rules。
- blocker escalation。
- AI next-best-action suggestions。
- manual override with reason。
- workflow rule admin view。
- timeline exports。

完成标准：

- consent、clarification、feedback、interview、offer、invoice、guarantee 卡住时，系统能把 due date、owner、blocker 和 audit trail 显示给正确角色。

收口状态：第一版 backend-owned workflow automation 与 SLA baseline 已完成。
`WorkflowAutomationPolicy` 现在为 consent、clarification、feedback、
interview、offer、invoice、guarantee 七类工作流定义 SLA、reminder、
escalation、owner、blocker 与 next-best-action 规则。Consultant workflow
现在基于现有 `WorkflowEvent` read model 暴露 automation queue 与 CSV timeline
export；manual override request 必须带非空 reason。Admin
`/admin/workflow-rules` 现在展示 Task 45 内建 automation coverage，不再是
deferred placeholder。此收口不新增现有 notification baseline 之外的外部
email/SMS dispatch，不新增持久化 AIActionRecommendation 表，也不宣称已有完整
workflow/BPMN runtime。

## Task 46：Full Data Lifecycle, Deduplication, Conflict, Stale, and Merge

目标：数据质量不再靠人工清理。

必须交付：

- candidate/company/job duplicate detection。
- high-confidence duplicate block。
- low-confidence duplicate warning with justification。
- merge proposal and merge audit。
- conflict resolution workflow。
- stale detection engine。
- refresh workflow。
- data retention/deletion policy execution。

完成标准：

- duplicate/merge/conflict/stale 决策可审计，不能静默覆盖 confirmed facts。

收口状态：第一版 backend-owned data lifecycle decision layer 已完成。
`DataLifecycleService` 和公开的 `DataLifecycleModels` contract 现在覆盖
candidate/company/job duplicate detection、high-confidence duplicate block、
带 justification 的 low-confidence warning、merge proposal、confirmed-fact
merge conflict block、conflict-resolution workflow recording、stale-field
refresh request，以及带 confirmed-fact tombstone protection 的
retention/deletion policy decision。Task 46 新增了完整 data lifecycle
workflow action vocabulary，并把 Owner `data-quality` 指标接入现有
`workflow.workflow_event` read model。此收口不新增物理行删除、直接 merge
mutation、fuzzy search index、外部 data-quality queue，也不执行 canonical
field overwrite。

## Task 47：Industry Pack Expansion and Calibration

目标：把所有 v2.1 industry packs 放到诚实 maturity 状态。

必须交付：

- general。
- semiconductor production calibration。
- finance。
- healthcare。
- internet_ai。
- sales。
- executive_search。
- manufacturing。
- 每个 pack 的 gold cases / negative cases / anti-patterns / score caps。
- drift detection and review queue。

完成标准：

- 每个 pack 有 maturity、ontology version、review_by、gold cases、negative cases、anti-patterns、score caps。

Task 47 收口说明：`docs/roadmap/task-47-industry-pack-expansion-and-calibration.md`
记录当前 backend-owned calibration baseline。V32 增加 Task 47 校准元数据并种入
全部 8 个 v2.1 packs；只有 `semiconductor` 标记为 `production`，其他 pack
保持诚实 seeded 状态，并在 Admin review queue 中暴露。

## Task 48：Commercial and Finance Operations Hardening

状态：**已完成**，当前 commercial-finance hardening baseline 位于 `5dfcf71`。

目标：补齐 placement、fee protection、invoice、guarantee、commission。

必须交付：

- fee agreement tracking。
- invoice readiness workflow。
- invoice sent/paid states。
- guarantee active/completed/replacement states。
- commission calculation inputs。
- Owner revenue reporting。
- accounting export process。

完成标准：

- placement-to-paid lifecycle 全链路可审计，同时不假装替代正式财务系统。

关闭说明：当前 placement-to-paid hardening gate 已完成。系统已有 fee
agreement snapshot、invoice readiness gate、invoice sent/paid/guarantee 状态约束、
commission calculation inputs、Owner revenue reporting 和 read-only accounting
export handoff。仍不包含 invoice issuing、payment collection、tax handling、GL
posting、通用导出包或替代正式财务系统。

## Task 49：Integrations v1

状态：**已完成**，当前 audited integration boundary baseline 位于 `d777456`。

目标：连接真实运营渠道。

必须交付：

- email provider。
- SMS provider 或 production placeholder。
- calendar integration。
- OCR/STT service。
- ATS/HRIS import/export baseline。
- WeChat/email-safe summary export。
- webhook/event integration boundary。

完成标准：

- 外部输入先成为 SourceItem/InformationPacket/claims；外发消息全部可审计。

关闭说明：当前 backend integration-boundary gate 已完成。Inbound/outbound
contracts、provider placeholders、governed-intake integration routing、outbound
redaction/disclosure checks、webhook boundaries、PostgreSQL integration audit
persistence 已经存在。这不是激活真实生产 provider、credentials、delivery SLA 或
customer-specific channel configuration。

## Task 50：Governance, Eval, and Ontology Production Console

状态：当前 read-only governance console baseline 已完成。Admin 现在可以看到
eval failures、deterministic negative cases、review quality、model routing
inspection、cost/latency、ontology drift、redaction incidents、AI resume
authenticity risk 等一线治理页面。Owner `ai-quality` 提供更窄的管理摘要。该范围
不包含 live provider activation/switching、ML resume-fraud detector、
ontology editing UI、Task 58 release management，或 Task 60 final acceptance。

目标：把质量治理做成 Owner/Admin 正式产品能力。

必须交付：

- eval dashboard。
- negative case generator。
- review quality signals。
- model routing console。
- cost/latency dashboard。
- ontology drift dashboard。
- redaction incident dashboard。
- AI resume authenticity risk dashboard。

完成标准：

- Admin/Owner 不用查数据库，就能定位 AI task failures、hallucination risks、stale ontology warnings、privacy incidents、low-quality review patterns。

## Task 51：Multi-organization Boundary Hardening

状态：当前 multi-organization boundary-hardening 范围已在 `c14723a`
完成。该范围关闭 organization-scoped identity constraints、已硬化 surface
的 cross-org negative coverage、tenant-aware access-audit search、
tenant-aware owner exports、tenant-aware pilot seed/import preflight，以及带
audit 的 support/admin impersonation policy。它不包含
multi-organization membership/session switching、完整 support tooling、广义
reporting/legal export packages，或真实客户 import/migration workflows。

目标：全产品组织边界 production-grade。

必须交付：

- organization-scoped unique constraints。
- every major API cross-org negative tests。
- tenant-aware audit search。
- tenant-aware exports。
- tenant-aware seed/import tools。
- support/admin impersonation policy with audit。

完成标准：

- 一个 organization 的用户不能推断、搜索、导出或访问另一个 organization 的私有数据。

## Task 52：Production Security Compliance Baseline

状态：**已完成**，当前 security compliance baseline 位于 `afc6942`。

目标：完成 pilot 之外的安全隐私基线。

必须交付：

- threat model。
- access review process。
- privacy/data-retention runbook。
- key/secret rotation。
- dependency vulnerability remediation。
- pen-test issue remediation。
- security regression suite。

完成标准：

- baseline scan/review 发现的问题被关闭或明确 risk-accepted，才能进入 100%。

关闭说明：当前 baseline documentation and regression gate 已完成。Threat model、
access review、privacy/data-retention runbook、key/secret rotation runbook、
dependency / pen-test remediation workflow、issue register 和
`SecurityComplianceBaselineDocumentationTest` 已存在。这不是 SOC 2、ISO 认证、
公开渗透测试证明，也不代表 MFA/SSO、distributed rate limiting、product-wide
field-level access audit 或精确生产 retention window 全部完成。

## Task 53：Disaster Recovery and Business Continuity

状态：**已完成**，当前 provider-neutral local DR/BCP baseline 位于 `add4d5f`。

目标：证明系统能从运营失败中恢复。

必须交付：

- backup schedule。
- restore drill。
- migration rollback drill。
- object storage recovery。
- AI provider outage playbook。
- notification provider outage playbook。
- incident severity levels。

完成标准：

- restore drill 能从近期备份恢复数据库和文档到可工作环境。

关闭说明：当前 local DR/BCP gate 已完成。Backup schedule、restore drill evidence、
migration rollback invariants、document/object recovery、AI provider outage
playbook、notification provider outage playbook 和 incident severity levels
已文档化。仍不代表 managed cloud backup、multi-region failover、external vendor
SLA 或 public production incident communications 已验证。

## Task 54：Performance, Load, and Cost Targets

状态：**已完成**，当前 deterministic performance/load/cost target baseline 位于 `13cc42a`。

目标：定义并达到真实性能和 AI 成本边界。

必须交付：

- API latency targets。
- portal interaction targets。
- AI task latency/cost budgets。
- batch parsing throughput。
- matching throughput。
- load tests。
- cost alerts。

完成标准：

- pilot 规模和预期生产规模 workload 都达到文档化 latency/cost targets。

关闭说明：当前 target and harness gate 已完成。Latency、throughput、AI cost
envelope、backend budget policy、deterministic local performance/load/cost
harness 和 alert classification 已存在。这是 capacity-model evidence，不是
deployed API/browser/provider performance proof；expected-production 的
interview-feedback cost row 仍是接近预算的 `WATCH` 项。

## Task 55：Data Import and Migration from Existing Systems

状态：**已完成**，当前 governed import/migration baseline 位于 `02fbda9`。

目标：让真实团队把历史招聘数据迁移进 governed system。

必须交付：

- CSV import。
- legacy ATS/CRM mapping。
- resume/document batch import。
- import validation report。
- duplicate detection during import。
- rollback/reset behavior。

完成标准：

- 历史数据通过 governed paths 导入 SourceItem/InformationPacket/claims/canonical records，并有 validation/rollback。

关闭说明：当前 backend import/migration-safety gate 已完成。Import planning、
validation/reporting、legacy ATS/CRM mapping contracts、duplicate/import
safeguards、rollback/reset planning、governed-intake import gateway boundaries
已经存在。这不是执行真实客户迁移，也不是 vendor-specific connector 激活或绕过
governed paths 的直接写入。

## Task 56：Support and Operations Tooling

状态：**已完成**，当前 backend support-operations baseline 位于 `68cef32`。

目标：让运营支持不靠手工改数据库。

必须交付：

- user support lookup。
- audit-safe resend/retry actions。
- AI task retry/replay tools。
- failed notification retry。
- data correction request workflow。
- support action audit。

完成标准：

- 常见支持问题能通过 audited tools 处理，而不是直接 DB edits。

关闭说明：当前 backend support-operations gate 已完成。Audited support
lookup/action contracts、failed-notification retry、AI task replay adapter
boundary、support transaction boundary、support user lookup、support action
audit persistence 已经存在。这不是完整 support console UI、external ticketing
integration，也不是绕过 domain services 的后门。

## Task 57：Reporting, Exports, and Legal Audit Packages

状态：**已完成**，当前 backend export package baseline 位于 `cd81acc`。

目标：产品化业务和合规导出。

必须交付：

- Owner reports。
- Consultant activity reports。
- Client-facing shortlist/feedback exports。
- Candidate personal-data export。
- Disclosure audit export。
- Placement/commission export。
- data retention export/delete evidence。

完成标准：

- exports 遵守 role、organization、consent、disclosure、field-level visibility policies。

关闭说明：当前 backend reporting/export/legal-audit package gate 已完成。
Role/scope-safe export payload contracts 和 adapters 已覆盖 Owner reports、
Consultant activity、Client shortlist feedback、Candidate personal-data
export、Disclosure audit、Placement/commission export、Retention evidence。这不是
BI warehouse、external legal hold system、accounting integration，也不是不受限制的
raw data export。

## Task 58：Release Management and Regression Suite

状态：**已完成**，当前 release-safety baseline 已加入 CI、local release
gates、migration validation、backend/frontend regression chain、deterministic
pilot browser E2E wrapper、privacy/security negative regressions、AI eval
artifact/schema regressions，以及 release checklist/gate docs。Task 60 现在使用
这套 release-safety evidence 作为当前规格 final acceptance 的一部分。

目标：让发布变得可重复、可验证。

必须交付：

- CI pipeline。
- migration validation。
- backend regression suite。
- frontend regression suite。
- browser E2E suite。
- privacy/security negative suite。
- AI eval regression suite。
- release checklist。

完成标准：

- release ready 必须通过 tests、migrations、E2E、privacy、eval gates。

## Task 59：Pilot-to-Production Onboarding Playbooks

状态：**已完成**，当前 controlled-pilot onboarding playbook package 位于 `a2173d0`。

目标：把 controlled pilot 经验变成可复制客户 onboarding。

必须交付：

- customer onboarding checklist。
- consultant training flow。
- client training flow。
- candidate consent FAQ。
- admin setup guide。
- data import guide。
- risk review guide。
- go-live checklist。

完成标准：

- 新 pilot 客户无需工程介入即可完成大部分 onboarding，除非涉及已批准的数据导入和集成配置。

关闭说明：当前 controlled-pilot onboarding gate 已完成。Customer onboarding、
consultant training、client training、candidate consent FAQ、admin setup、data
import、risk review 和 go-live playbook 已存在。仍不代表 public SaaS readiness、
real integration completion 或未批准的生产客户数据导入。

## Task 60：Full Product Acceptance Gate

状态：**已完成**，当前 v2.1/v2.0 规格的 Task 60 report 位于
`docs/roadmap/task-60-full-product-acceptance-gate.md`，结果为
`FULL_PRODUCT_100_READY`，并包含通过的 release gate / browser E2E 证据。

目标：判断当前 v2.1/v2.0 产品计划是否 100% 实现。

必须通过：

- v2.0/v2.1 五端页面组都有真实 route、API、state、permission behavior、acceptance evidence。
- v2.1 AI Task Registry 每个 task 都有 schema、prompt version、execution policy、AITaskRun、human review/write-back policy、eval evidence。
- v2.1 每个核心数据对象都有 contract、persistence 或 derived model、service boundary、access policy、audit behavior、tests。
- 每个 workflow state machine 都有 transition legality validation 和 WorkflowEvent audit。
- matching、industry pack、evidence coverage、score cap、provenance、authenticity risk、ontology version、outcome feedback 都已实现。
- consent、disclosure、unlock、prior contact、prior application、fee protection、placement、commission、audit export 都已实现。
- security、privacy、observability、backup/restore、deployment、support、import/export、release gates 全部通过。
- v2.0 UI/portal definitions 保留。
- 核心 v2.1 requirement 没有 fake completed shell。

完成标准：

- 可以称为当前 v2.1/v2.0 规格 Full Product 100%。
- 这不等于未来所有商业 SaaS 功能都完成。billing、marketplace scale、深度 ATS ecosystem、更多 production-calibrated industry packs 可以作为 v2.1/v2.0 之后的新 roadmap。

关闭说明：当前规格的 full-product acceptance gate 已完成。Task 60 修复了
一个 evidence blocker：把 Consultant `/consultant/placements` 加入 route
contract 断言；实际 route 和 backend API 在此前已经存在。剩余事项属于
post-100 roadmap、deployment 或 customer go-live work，不是隐藏的 v2.1/v2.0
blocker。

## 现在不要做什么

- 不要先做 8 个假 production-ready industry packs。
- 不要在 modular monolith 产品完成前拆复杂微服务。
- 不要把 AI chat 做成主入口。
- 不要让 RAG/MCP/LLM vendor/low-code tool 成为事实源。
- 不要优先做装饰性 UI，而忽略真实 workflow、privacy gates、consent、disclosure、audit。
- 不要让 seed data 绕过真实 workflow。
- Task 42 gate 通过前不要声称 pilot-ready。
- 不要把 Task 60 误写成 public SaaS launch、managed infrastructure readiness、
formal certification 或 customer go-live approval。

## 后续每个任务的固定格式

之后执行 Task 16 及以后，每个任务都应该明确：

- Parent task。
- Goal。
- Current baseline。
- Must deliver。
- Forbidden scope。
- Acceptance。
- Validation commands。
- Changed files。
- Boundary confirmation。
- Known gaps remaining。

建议每个任务默认验证：

```sh
git diff --check
npm run typecheck:web
npm run build:web
docker info
PATH=/opt/homebrew/bin:$PATH mvn -f services/core-api/pom.xml test
```

如果任务是 backend-only，可以在任务说明里明确跳过 frontend build；如果涉及 API、web、contracts、DTO、route、auth、portal，必须跑前后端验证。
