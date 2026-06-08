# RC1 Pilot Readiness Plan - 综合审计与改进方案

## 审计结论

`docs/release/RC1-pilot-readiness-plan.md` 的方向是正确的：它没有把 RC1 当成功能清单，而是当成证据门禁；它也已经把 `ready-for-controlled-pilot` 和 pre-placement / release-regression / public SaaS launch 区分开。

但这份计划还不够“可交给另一个工程师无歧义执行”。主要问题不是缺少宏观意识，而是若干证据契约还不够硬，尤其是商业闭环、隐私/披露、MatchReport v2.1 治理、owner/admin 审计链、运维门禁和 spec traceability。当前审核未运行产品 gate；所有 runtime readiness 仍是 `not verified`。

当前本机还有一个现实阻断：Docker daemon 不可达。QA/Ops 组件报告记录 `rtk docker version` / `rtk docker info` 均无法连接 Docker daemon，因此 Testcontainers、PostgreSQL migration apply、isolated pilot data、browser E2E、`release:gate` 当前都不能产出通过证据。这是 `blocked-environment`，不是产品回归证据。

## 审计材料

组件报告已保存：

- `docs/audits/rc1-pilot-readiness-2026-06-08/01-plan-structure.md`
- `docs/audits/rc1-pilot-readiness-2026-06-08/02-spec-coverage.md`
- `docs/audits/rc1-pilot-readiness-2026-06-08/03-backend-data-governance.md`
- `docs/audits/rc1-pilot-readiness-2026-06-08/04-frontend-ux-workflows.md`
- `docs/audits/rc1-pilot-readiness-2026-06-08/05-qa-release-ops.md`
- `docs/audits/rc1-pilot-readiness-2026-06-08/06-privacy-security-risk.md`

`06` 是主线程补缺报告：隐私/安全子代理多次断流或超时，最终用 bounded search 做了最小证据补全；没有运行测试，也不是完整安全审计。

## P0 Findings

### P0-1 当前环境不能支撑 RC1 L1 证据

Observed:

- QA/Ops 报告记录 Docker client 存在，但 Docker daemon socket 不可达。
- RC1 计划自己规定 Docker/Testcontainers/local PostgreSQL/ports/browser startup blocker 应归类为 `not-ready-blocked-environment`，并在 Testcontainers 不可用时停止相关任务。

Inference:

当前不能证明 migration apply、pilot data rebuild/validate、Postgres-backed integration、Playwright pilot E2E、`release:gate`。这不是产品失败，但会阻断 RC1 执行。

Recommended action:

先修 Docker，再跑：

```sh
rtk docker version
rtk docker info
rtk npm run release:migrations
```

在 Docker server 可达前，不要尝试声明 Testcontainers、migration apply、E2E 或 `release:gate` 通过。

### P0-2 当前 browser gate 不能证明商业闭环

Observed:

- RC1 hard pass 要求 placement/commission 有 runtime/API plus persistence/audit evidence，owner/admin audit trace 也要 current runtime/API plus persistence/WorkflowEvent/audit evidence。
- 前端审计确认当前 pilot browser suite 只覆盖 S01-S08，到 interview feedback / consultant follow-up review；没有 placement、commission、owner revenue/accounting、admin/owner audit trace 的 browser proof。
- RC1 计划的 `RC1-08A` 已经承认 Task 42 browser flows 停在 pre-placement。

Inference:

如果不增加 S09-S12 或等价 manual/API/SQL evidence recipe，前端自动化最多支持 “automated pre-placement workflow”，不能支持 `ready-for-controlled-pilot`。

Recommended action:

新增 `tests/e2e/pilot-commercial-closure.spec.ts` 或把 S09-S12 接到现有 flow：

1. Consultant records placement.
2. Consultant advances placement / invoice / paid or explicitly records supported pilot state.
3. Consultant creates/verifies commission.
4. Owner verifies placement/commission/revenue/accounting handoff.
5. Owner/Admin or consultant workflow filters the same transaction IDs and shows audit trace.

如果先不做自动化，则 RC1-09 必须给出 API calls + SQL/audit queries 的手动证据脚本。

### P0-3 `RC1-09` 证据措辞会低估非商业 P0 步骤

Observed:

- 全局规则说 canonical write、consent、unlock/disclosure、placement、commission、governance/audit 都不能只靠 screenshot。
- 但 RC1-09 Step 3 又说每个 manual step 至少记录 screenshot / API response / log / DB row / WorkflowEvent / E2E 中任一项；后面的强约束只再次点名 placement、commission、audit trace。

Inference:

执行者可能给 canonical write、candidate consent、unlock/disclosure 只贴截图或 API summary，然后误把这些 backend-owned truth / privacy states 当成已验证。

Recommended action:

改 RC1-09：canonical write、consent、unlock/disclosure、placement、commission、governance/audit 全部必须绑定同一 transaction ledger ID，并提供 runtime/API plus DB row 或 WorkflowEvent/audit evidence。截图只能作为辅助。

## P1 Findings

### P1-1 `RC1-08A passed-or-manual-gap-recorded -> RC1-08` 分支过于模糊

Observed:

- Dependency graph 允许 `RC1-08A passed-or-manual-gap-recorded -> RC1-08`。
- Final signoff 很严格，但中间状态容易被理解成“记录 gap 后继续执行就行”。

Recommended action:

替换成明确状态：

```text
RC1-07 passed -> RC1-08A
RC1-08A passed -> RC1-08
RC1-08A runtime-gap-recorded -> RC1-09-commercial-closure-runtime
RC1-08A partial/not-verified -> stop, or final decision cannot exceed not-ready-insufficient-evidence
RC1-09-commercial-closure-runtime closed -> RC1-08
```

`manual-gap-recorded` 必须定义为 non-pass state，包含 missing runtime action、missing persistence/audit proof、owner、next command/manual step、allowed final decision。

### P1-2 缺少 spec-to-RC1 traceability

Observed:

- v2.1 验收覆盖 Claim Ledger、anti-false-confirmation、re-identification risk、MatchScore governance、Living Ontology、audit replay/export、五端 routes 等。
- RC1 plan 有 capability map，但没有把每条 v2.1 acceptance category 映射到 task、evidence row、minimum evidence level、final decision criterion。

Recommended action:

在 `Source Of Truth` 后新增 `Spec-To-RC1 Traceability` 表：

| v2.1 Requirement | RC1 relevance | Task/gate | Minimum evidence | Final evidence row | Out-of-scope note |
| --- | --- | --- | --- | --- | --- |

RC1-10/RC1-11 必须 reconcile 每一行，不能只写 narrative self-review。

### P1-3 MatchReport v2.1 governance 过弱

Observed:

- v2.1 要求 `score_confidence`、`evidence_coverage`、`provenance_weight`、`authenticity_risk`、score caps、ontology version/stale warning。
- RC1 golden path 有 MatchReport，但 P0 Proof Matrix 没有这些治理字段。

Recommended action:

新增 `MatchReport v2.1 governance` 证据行。至少要求：

- evidence coverage；
- provenance weighting；
- score confidence；
- authenticity risk；
- score-cap outcome；
- industry pack / ontology version；
- stale ontology warning 或明确 out-of-RC1 risk acceptance。

### P1-4 隐私/安全 gate 主要是 backend selected tests，缺 browser privacy negative proof

Observed:

- `scripts/release/privacy-security-regression.sh` 是 Maven selected-test runner。
- 代码有 client-safe projection、redaction audit、consent/disclosure policy、tenant/auth/rate-limit、pilot-data validator 等基础面，但 RC1 plan 没有单独要求 browser/API privacy negative artifact。

Recommended action:

新增 `RC1-06B Browser Privacy Negative Proof`：

- pre-unlock client page/API 不含 name、email、phone、LinkedIn、exact employer/project/product/chip、raw source text、consultant notes、candidate/profile UUID、`WorkflowEvent`、`ClaimLedger`；
- raw candidate/profile UUID route fail closed；
- cross-org anonymous-card ref fail closed；
- post-disclosure identity access 必须绑定 consent、unlock、consultant approval、audit evidence。

### P1-5 consent / unlock / disclosure ledger 列不够交易级

Observed:

- v2.1 要求 consent 记录 profile version / consent text version，unlock 生成 DisclosureRecord。
- RC1 ledger 记录 consent/unlock/disclosure IDs，但不强制 profile version、consent text version、human approver、reason、disclosure level、WorkflowEvent ID。

Recommended action:

把 consent/unlock/disclosure 证据改成 mini table：

| Step | Record ID | Profile version | Consent text version | Disclosure level | Human approver | Reason | WorkflowEvent ID | Artifact |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

### P1-6 route preservation 应接入现有 contract test，并补 render-level smoke

Observed:

- 当前已有 `apps/web/src/portalRouteContract.test.ts`，但 RC1-04 没有点名它。
- 该测试主要检查 route string preservation，不渲染页面。

Recommended action:

RC1-04 必须记录 `portalRouteContract.test.ts` 是否在 web test 中运行。再补一组 render-level route smoke：五端关键 spec route 能渲染 expected heading、不会 fallback redirect，并显示明确 empty/error/next action。

### P1-7 Owner 和 Admin 最终 proof 被合并

Observed:

- RC1 把 final oversight 写成 `Admin/owner audit trace`。
- v2.1 中 Owner 是 revenue / placement / commission / risk / audit，Admin 是 policy / task registry / workflow / permissions / audit / security。

Recommended action:

拆成两条 final proof：

- Owner commercial/accounting proof: placement、commission、revenue、accounting handoff。
- Admin governance/audit/replay proof: workflow events、AI task runs、unlock/disclosure、score generation replay/export。

### P1-8 ops readiness 不是 final hard-pass first-class gate

Observed:

- RC1 final criteria 覆盖 regression/golden path，但没有把 deployment config validation、secrets/provider status、observability incident dry run、backup/restore、rollback、first-week monitoring、dependency/security scan 放成硬 gate。
- repo 中已有相关 runbooks / validators，但 RC1 没 stitch 成当前证据行。

Recommended action:

新增 `Operations Readiness` gate group：

- deployment config validator；
- secrets presence check，只记录变量名和 present/missing，不记录值；
- external provider posture: deterministic-only / live-configured / manual-channel-approved / out-of-scope；
- observability incident dry run；
- backup/restore or explicit not-current classification；
- rollback target；
- first-week monitoring owner/cadence；
- `npm audit --omit=dev` 和 dependency-check 的结果或有界 risk acceptance。

## P2 Findings

- RC1 evidence artifacts 尚不存在：`RC1-pilot-readiness-evidence.md`、`RC1-capability-map.md`、`rc1-artifacts/` 还没创建；所以当前只能说“计划已准备”，不能说“RC1 已执行”。
- Per-gate evidence schema 不统一：建议所有 gate 都记录 owner、command、run at、exit code、status、evidence level、artifact path、blocker、rollback/cleanup、next action。
- Readiness glossary 值得加：local acceptance、RC1 controlled pilot readiness、production go-live、commercial/acquisition readiness 分开定义。
- Consultant placement / commission UI 依赖手动复制 UUID，pilot operator 容易断上下文；应加从 feedback/disclosure/workflow 到 placement 的 prefilled CTA，以及 placement 到 commission 的 CTA。
- Synthetic data validator 有价值，但不能证明无真实 PII；RC1-03A 应加 `synthetic-data-attestation` artifact。

## 改进方案

### Phase 0 - 先别跑 RC1，先把计划补硬

修改 `docs/release/RC1-pilot-readiness-plan.md`，只做文档层硬化：

1. 改 dependency graph，移除 `passed-or-manual-gap-recorded` 模糊状态。
2. 新增 Gate Metadata Matrix：task、owner、approver、deliverable、rollback/cleanup、waiver allowed、waiver authority、exit gate、next task。
3. 标准化 Gate Evidence schema：owner、command、run at、exit code、status、evidence level、artifact path、blocker、rollback/cleanup、next action。
4. 新增 Spec-To-RC1 Traceability。
5. 扩展 P0 Proof Matrix：MatchReport governance、anti-false-confirmation、re-identification risk、role/route guard、ops readiness。
6. 改 RC1-09 evidence rules，给 canonical write / consent / unlock-disclosure / placement / commission / audit trace 指定最低证据。

### Phase 1 - 修当前环境 blocker

1. Start/repair Docker Desktop。
2. 跑 `rtk docker version`、`rtk docker info`。
3. 只在 Docker server 可达后跑 `rtk npm run release:migrations`。
4. 通过后再执行 RC1-00/RC1-01/RC1-02，创建 evidence log、capability map、artifact directory、dirty-worktree note。

### Phase 2 - 补 RC1 gate 覆盖

1. 前端：新增 S09-S12 commercial closure Playwright，或新增手动 API/SQL evidence script。
2. 隐私：新增 `RC1-06B Browser Privacy Negative Proof`。
3. 路由：把 `portalRouteContract.test.ts` 纳入 RC1-04，并新增 render-level route smoke。
4. 后端：给 placement/commission/owner revenue/admin observability 加 correlation query recipe。
5. Ops：新增 Operations Readiness gate group 和 dependency/security scan row。

### Phase 3 - 补 operator readiness

1. Consultant feedback/disclosure/workflow 到 placement 的 contextual handoff。
2. Placement 到 commission 的 prefilled handoff。
3. Owner/admin transaction-scoped audit lookup。
4. 五端 empty/error/loading state 统一为 title + safe reason + next action + recovery route。

## 推荐的下一步

最优顺序是先做一个小的 doc-only patch，把 RC1 plan 改到“执行无歧义”；然后修 Docker；然后再开始 RC1-00 到 RC1-02。不要直接从当前计划跑完整 RC1，因为最可能浪费时间的点不是命令缺失，而是中途的 partial / gap / screenshot evidence 会污染最终 readiness 判断。

## Not Verified

- 没有运行 `test:core-api`、`build:core-api`、web test、typecheck、build、`release:migrations`、`release:privacy-security`、`release:ai-eval`、`release:e2e:pilot`、`release:gate`。
- 没有收集当前 API response、DB row、WorkflowEvent、browser screenshot、manual walkthrough evidence。
- 没有验证 GitHub Actions 当前状态。
- 没有改动 `docs/release/RC1-pilot-readiness-plan.md` 本体。
