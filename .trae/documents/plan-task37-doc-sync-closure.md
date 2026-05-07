# Task 37 收尾计划：路线图文档同步与状态闭环

## Summary

当前仓库的真实代码基线已经进入 `Task 37: Owner and Admin Governance v1` 的实现收尾阶段，而路线图文档仍停留在“Task 36 是下一步”的旧状态。本轮“继续工作”的目标应聚焦于 **文档闭环**，而不是重新开启一轮新的功能开发：

- 把 `docs/roadmap/current-engineering-snapshot.md` 从 Task 35 / Task 36 待做状态更新到当前 `HEAD` `66d59e9`
- 把 `docs/roadmap/implementation-status.md` 补齐 Task 36 与 Task 37 的已实现事实
- 把 `docs/roadmap/known-gaps.md` 从“Owner/Admin 治理面未做”更新为“Task 37 已完成，但仍有后续深化项”
- 仅在发现 `docs/roadmap/productization-roadmap.zh-CN.md` 存在与当前代码直接冲突的表述时，做最小必要同步；否则不扩大改动面

本轮计划默认 **不新增后端/前端功能代码**，也 **不重跑重型验证**。它只做文档状态同步、残余缺口澄清和下一任务入口修正。

## Current State Analysis

### 1. 当前真实代码基线已经超出路线图文档

已确认事实：

- `git rev-parse --short HEAD` 返回 `66d59e9`
- 当前工作区已包含 Task 37 相关实现文件和修改，包括：
  - `apps/web/src/features/admin-portal/AdminPortal.tsx`
  - `apps/web/src/api/governance.ts`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/admin/AdminGovernanceController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/apiboundary/owner/OwnerGovernanceController.java`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governanceconfig/`
  - `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/governancequery/`
  - `services/core-api/src/main/resources/db/migration/V31__add_governance_config_entries.sql`
- `apps/web/src/features/owner-portal/OwnerPortal.tsx` 已扩展到 `dashboard / pipeline / consultants / clients / risk / data-quality / ai-quality / audit`
- `apps/web/src/features/admin-portal/AdminPortal.tsx` 已存在真实 Admin Portal 导航与配置保存 UI

结论：

- 当前实现已经不是“Task 36 待开始”的仓库状态
- 继续工作时，文档必须先追上代码事实

### 2. 当前路线图文档存在明确漂移

已确认事实：

- `docs/roadmap/current-engineering-snapshot.md` 仍写着：
  - `next recommended task: Task 36 (Placement and Commission v1)...`
  - `Proceed to Task 36 Placement and Commission v1`
- `docs/roadmap/implementation-status.md` 没有 Task 36 / Task 37 的完成记录
- `docs/roadmap/known-gaps.md` 仍保留：
  - `Owner/Admin workflow analytics and broader cross-portal workflow surfaces remain future work`
  - Admin tooling 仍被描述为不存在或仅停留在更早阶段的表述

结论：

- 三份路线图文档必须一起更新，否则下一轮任务会继续建立在错误基线上

### 3. 当前最适合收尾的是“状态同步”，不是补做新的产品面

已确认事实：

- 当前代码面已经具备 Task 37 主体形态：Owner 治理只读面、Admin 治理查询面、Admin 配置写入面、治理配置持久化与统一 DTO
- 当前明显缺口之一是 `apps/web/src/features/admin-portal/AdminPortal.test.tsx` 不存在
- 但最近工作上下文已经收敛到“代码与测试跑通后，最后同步路线图文档”

结论：

- 这轮继续工作应聚焦：
  - 同步路线图文档
  - 诚实记录 Task 37 后仍未闭合的残余缺口
- 这轮不应重新打开新的代码开发范围，例如新增 AdminPortal 前端测试或继续深化 runtime overlay

## Proposed Changes

### 任务 1：更新 `current-engineering-snapshot.md` 为 Task 37 后基线

**文件**

- `docs/roadmap/current-engineering-snapshot.md`

**改动内容**

- 把 `Current Main Baseline` 从旧的 Task 34 / Task 35 基线更新到当前 `HEAD` `66d59e9`
- 在 `latest product baseline merges on main` 中补入：
  - Task 36 `Placement and Commission v1`
  - Task 37 `Owner and Admin Governance v1`
- 更新 `latest documented validation snapshot`，只写入当前已经确认通过的验证事实，不捏造新的命令结果
- 更新 `merge status`，明确当前工程基线已包含 Task 36 与 Task 37
- 把 `next recommended task` 从 Task 36 改成下一个真实入口：
  - 默认写为 `Task 38 Pilot Seed Data and Import Tools`
  - 若文档结构更适合写“Task 37 后的剩余深化项”，则说明 Task 38 之前仍有 runtime overlay 深化和 AdminPortal 测试缺口，但不把它们伪装成未完成的 Task 37 主体

**为什么这样改**

- 这是下一轮任务的主要入口文档，必须先修正推荐任务和基线描述

### 任务 2：在 `implementation-status.md` 中补齐 Task 36 / Task 37 已实现事实

**文件**

- `docs/roadmap/implementation-status.md`

**改动内容**

- 在 `Current Git Main Milestones` 或等价位置补上 Task 36 与 Task 37 的状态描述
- 为 Task 36 增加一条完成记录，范围至少覆盖：
  - placement / commission 产品面闭环
  - 相关后端、API、前端与验证结果
- 为 Task 37 增加一条完成记录，范围至少覆盖：
  - Owner Portal 扩展为治理工作台
  - `/api/owner/*` 治理读取端点
  - Admin Portal 从静态壳切换为真实 portal
  - `/api/admin/*` 查询与配置写入端点
  - `governance.config_entry` 与 `V31`
  - `GovernanceReadService` / `GovernanceSectionResponse`
  - `FieldAccessPolicy` / `ApiSafeResponseBody` 的治理边界更新
- 在 `Current Test State` 中补入最近一次已知成功验证快照：
  - 前端 build 成功
  - OwnerPortal 前端测试成功
  - 后端全量 `mvn test` 成功
- 若原文中有“下一步是 Task 36”或与 Task 37 冲突的段落，统一改成 Task 37 已完成后的口径

**为什么这样改**

- `implementation-status.md` 是最细的完成记录文档，必须能独立说明 Task 36/37 已经交付了什么

### 任务 3：重写 `known-gaps.md` 中与 Task 37 直接冲突的旧缺口

**文件**

- `docs/roadmap/known-gaps.md`

**改动内容**

- 删除或改写“Owner/Admin workflow analytics and broader cross-portal workflow surfaces remain future work”这类已经被 Task 37 主体覆盖的旧说法
- 把 Task 37 之后真实仍存在的残余缺口改写为更精确的表述，例如：
  - 治理配置 overlay 已建立，但更深的运行时生效面仍有后续深化空间
  - AdminPortal 当前缺少独立前端测试文件
  - Admin 配置写入仍限制在治理配置层，不提供业务事实直写
  - 更细粒度的治理 drill-down / runtime calibration 仍是后续增强项
- 保留仍然真实存在的长期缺口，不因 Task 37 完成而误删，例如：
  - 多组织身份能力
  - SSO/OIDC / MFA / reset password
  - 更完整的 AI productization
  - 更广的 workflow automation / SLA / observability 深化

**为什么这样改**

- `known-gaps.md` 应该表达“还没完成的真实后续项”，不能继续把已经做完的 Task 37 主体写成完全缺失

### 任务 4：仅在存在直接冲突时最小同步 `productization-roadmap.zh-CN.md`

**文件**

- `docs/roadmap/productization-roadmap.zh-CN.md`

**改动内容**

- 只检查 Task 36 / Task 37 是否被描述成未来待做且与当前基线直接冲突
- 如果只是长期路线图分阶段列表，则不修改
- 如果存在“当前下一步仍是 Task 36”或“Task 37 尚未开始”的叙述，则做最小必要修正

**为什么这样改**

- 这份文档是长期路线图，不应为了收尾同步而做大规模重写

### 任务 5：统一下一步入口与残余风险说明

**文件**

- `docs/roadmap/current-engineering-snapshot.md`
- `docs/roadmap/implementation-status.md`
- `docs/roadmap/known-gaps.md`

**改动内容**

- 三份文档统一以下口径：
  - Task 36 已完成
  - Task 37 主体已完成
  - 当前继续工作的残余项以“文档同步后再进入 Task 38 或单独收尾增强”为准
- 显式记录以下残余风险，避免下一轮误判为“全部彻底闭合”：
  - `AdminPortal.test.tsx` 缺失
  - 某些治理 overlay 的运行时生效深度仍可继续增强
  - 文档记录的是最近一次已知成功验证，不代表本轮文档编辑后又重跑了所有重型命令

**为什么这样改**

- 避免三份文档之间再次出现“完成状态不同步”和“下一步任务不同步”

## Assumptions & Decisions

- 决策：本轮“继续工作”按最近上下文收敛为 **Task 37 文档收尾**，不是重新开启 Task 37 新功能开发
- 决策：本轮默认只编辑路线图文档，不改 Task 37 业务代码
- 决策：`AdminPortal.test.tsx` 缺失被视为 **Task 37 后续增强缺口**，本轮只在文档中诚实记录，不把范围扩成新的代码实现
- 决策：`productization-roadmap.zh-CN.md` 只做冲突修正，不做全面改写
- 决策：下一推荐任务默认指向 `Task 38 Pilot Seed Data and Import Tools`，除非实际文档结构更适合写成“Task 37 后 residual closure”
- 决策：验证记录只复用当前已知成功结果，不在文档里伪造新的测试/构建执行结论

## Verification Steps

1. 交叉检查以下文件中的 Task 36 / Task 37 表述是否一致：
   - `docs/roadmap/current-engineering-snapshot.md`
   - `docs/roadmap/implementation-status.md`
   - `docs/roadmap/known-gaps.md`
2. 确认 `current-engineering-snapshot.md` 不再出现“next recommended task: Task 36”
3. 确认 `implementation-status.md` 已包含 Task 36 与 Task 37 的完成记录
4. 确认 `known-gaps.md` 不再把 Owner/Admin 治理面整体描述成“尚未实现”
5. 若修改了 `productization-roadmap.zh-CN.md`，确认其中 Task 36 / Task 37 的表述不与前三份文档冲突
6. 用文本检索复核以下关键词：
   - `Task 36`
   - `Task 37`
   - `next recommended task`
   - `Owner`
   - `Admin`
