# Task 19C 实施计划

## 范围摘要

本次 19C 按收敛版范围推进，目标是在既有 19A/19B 基础上完成三类收尾工作：

1. 强化 JWT 会话撤销能力，使 access token 在 session 被 logout / refresh 轮换后可立即失效。
2. 补齐以 Spring Security principal 为核心的跨组织负例与鉴权回归测试。
3. 更新 roadmap / status / known-gaps 文档，把下一推荐任务推进到 19C 之后的阶段。

本轮**不包含**邮箱验证、密码重置、完整登录限流、SSO/OIDC、多组织切换等额外 auth 产品能力。

## 当前状态分析

- 产品真相约束来自 `docs/specs/CURRENT_SPEC.md`：后端拥有 truth，客户端不能在 unlock/disclosure 前读取 raw Candidate，对关键状态变化要保留审计边界。
- 19A 已落地基础认证设施：
  - [AuthenticationService](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/AuthenticationService.java)
  - [JwtService](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/JwtService.java)
  - [JwtAuthenticationFilter](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/JwtAuthenticationFilter.java)
  - [JdbcIdentityAuthenticationPort](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/persistence/JdbcIdentityAuthenticationPort.java)
  - [V15__add_identity_auth_baseline.sql](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/main/resources/db/migration/V15__add_identity_auth_baseline.sql)
- 19B 已完成 controller 迁移：
  - [SecurityConfig](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/SecurityConfig.java) 已要求 `/api/**` 认证。
  - Consultant / client-safe controller 已改为依赖 `@AuthenticationPrincipal RtoAuthenticatedPrincipal`。
- 当前 auth 仍存在一个已确认硬化缺口：
  - `JwtService` 在 access token 中写入 `sid` claim，但 [JwtAuthenticationFilter](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/JwtAuthenticationFilter.java) 只验签和过期，不查询 `identity.session`。
  - 结果是 logout 或 refresh 轮换后，旧 access token 可能在 TTL 内继续可用，这与本次选择的“强撤销”目标不一致。
- 现有 auth 回归主要覆盖 login / refresh / logout 基础流：
  - [AuthenticationControllerTest](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/auth/AuthenticationControllerTest.java)
  - [IdentityAuthPostgresIntegrationTest](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/test/java/com/recruitingtransactionos/coreapi/identityauth/IdentityAuthPostgresIntegrationTest.java)
- 现有跨组织基线主要在业务层和 DB 组合键层：
  - [ConsultantWriteOrgIsolationIntegrationTest](file:///Users/edwardlong/.trae/worktrees/New%20project/feat-check-progress-next-task-kUAHVF/services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWriteOrgIsolationIntegrationTest.java)
  - 但还缺少“错误 org JWT / revoked session / stale session / role escalation”这类从 auth 入口进入的负例收口。

## 前置约束

- 只扩展 auth/session hardening 和相关测试，不新增业务功能。
- 不改变既有 ABAC 规则语义，只强化 principal 建立前后的认证边界。
- 继续保持 fail-closed：会话不存在、被撤销、过期、角色失效、principal 与 session 不一致时，一律返回安全 401/403，而不是回退成匿名成功。
- 文档更新要与当前实际实现一致，不能继续保留“19B 未完成”或“仍依赖 header 鉴权”的表述。

## Proposed Changes

### 1. 为 access token 增加基于 `sid` 的活跃 session 校验
**目标**
让 access token 在每次认证请求时都校验其 `sid` 对应的 `identity.session` 仍处于 active 状态，实现 logout/refresh 后的即时失效。

**涉及文件**
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/IdentityAuthenticationPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/persistence/JdbcIdentityAuthenticationPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/IdentityAuthSession.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/JwtAuthenticationFilter.java`

**影响范围**
- 新增按 `session_id` 查询 active session 的 port 能力。
- filter 在 parse token 后，使用 principal.sessionId() + 当前时间校验 session 是否存在、未 revoked、未 expired。
- 若 session 已失效，则清理 `SecurityContext` 并返回与现有 invalid token 同级的安全 401。

**验证方式**
- 新增/更新 auth integration test，证明 logout 后旧 access token 立即失效。
- 新增/更新 auth integration test，证明 refresh 轮换后旧 access token 立即失效。
- 保持 `/api/auth/**` 与 `/health` 匿名行为不受影响。

### 2. 收紧 session / principal / user-account 的一致性检查
**目标**
避免 token 虽然验签通过，但其用户状态、组织作用域、角色授权与当前 session/账号状态已不一致时继续放行。

**涉及文件**
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/JwtAuthenticationFilter.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/AuthenticationService.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/IdentityAuthenticationPort.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/persistence/JdbcIdentityAuthenticationPort.java`

**影响范围**
- 在 filter 的 session 校验通过后，可进一步核对：
  - session 的 `organization_id` 与 token `org` 一致
  - session 的 `role` 与 token `role` 一致
  - user_account 仍 active
  - role_assignment 仍 active
- 若任一条件不满足，则 fail-closed。

**验证方式**
- 增加集成测试覆盖：账号被停用、role_assignment 被撤销、token org/role 与 session 不一致时，请求被拒绝。
- 确认正常登录用户访问 consultant/client-safe 端点仍可通过。

### 3. 增加以认证入口为中心的跨组织负例测试
**目标**
把“跨组织隔离”从现有 service/DB 层扩展到 auth + controller 入口，证明错误 org JWT 无法越权读取或写入。

**涉及文件**
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantWriteOrgIsolationIntegrationTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantControllerLeakageTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/ClientSafeCandidateCardControllerTest.java`
- 如有必要，新增一个面向 auth+MockMvc 的 integration test 文件到 `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/identityauth/`

**影响范围**
- 设计覆盖以下负例：
  - org A token 访问 org B 数据返回 fail-closed 结果
  - 被撤销 session 的 token 访问 consultant/client-safe/document 端点返回 401
  - stale / forged principal 情况不能绕过组织边界
  - 角色提升尝试仍被现有 access policy 拦截

**验证方式**
- 使用现有组织隔离 fixture 模式，补一组 auth-aware 负例。
- 断言保持 sanitized 错误体，不泄露内部类名、表名、stack trace。

### 4. 扩展 auth 回归测试到 session 生命周期边界
**目标**
让 19C 的测试覆盖不只停留在 refresh token 生命周期，还包括 access token 强撤销、并发轮换后的旧 token 行为、logout 后行为和异常原因码稳定性。

**涉及文件**
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/identityauth/IdentityAuthPostgresIntegrationTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/auth/AuthenticationControllerTest.java`
- 可能新增 `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/identityauth/JwtAuthenticationFilterIntegrationTest.java`

**影响范围**
- 为 auth 基础设施增加这些回归：
  - login 后 access token 可正常访问受保护端点
  - refresh 后旧 refresh token 失效
  - refresh 后旧 access token 也失效
  - logout 后 refresh token 和 access token 都失效
  - malformed / unknown / revoked session token 返回一致安全响应

**验证方式**
- 以 PostgreSQL/Testcontainers 为主覆盖真实 `identity.session` 生命周期。
- 以 `MockMvc` 验证 controller 层响应码与安全错误体。

### 5. 统一 401/403 语义与安全错误码断言
**目标**
把 19C 新增的 session hardening 行为稳定映射到现有 API 错误语义，避免后续测试和文档继续混淆“未认证”“已认证但无权限”“访问上下文非法”。

**涉及文件**
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/SecurityConfig.java`
- `services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/JwtAuthenticationFilter.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/ApiBoundaryRegressionClosureTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/consultant/ConsultantControllerLeakageTest.java`
- `services/core-api/src/test/java/com/recruitingtransactionos/coreapi/apiboundary/ClientSafeCandidateCardControllerTest.java`

**影响范围**
- 明确约束：
  - token 缺失 / invalid / revoked session -> 401
  - principal 有效但角色/字段分类不允许 -> 403
  - 非法请求体 / 非法 path / 非法 field header -> 400 或 403，保持当前 API 语义
- 让 API boundary regression 测试记录这套最终规则。

**验证方式**
- 定向运行 `ApiBoundaryRegressionClosureTest`、consultant/client-safe controller 测试。
- 确认错误体里的 `errorCode` / `safeReason` 不回归。

### 6. 文档收尾并把下一任务推进到 19C 之后
**目标**
让 roadmap、状态快照与 known gaps 完整反映 19C 已完成，以及剩余 auth 工作只剩更长期能力。

**涉及文件**
- `docs/roadmap/current-engineering-snapshot.md`
- `docs/roadmap/implementation-status.md`
- `docs/roadmap/known-gaps.md`

**影响范围**
- 记录 19C 的真实交付物：
  - 强撤销 session 校验
  - auth-aware cross-org negative coverage
  - broader auth regression coverage
- 从 known gaps 中移除“19C 未完成”的表述，保留仍延期项：
  - SSO/OIDC
  - password reset
  - email verification
  - rate limiting / lockout
  - multi-org membership/session switching

**验证方式**
- grep 文档，确认不存在“Task 19C still required”一类过期表述。
- 文档中的下一推荐任务应推进到 19C 之后。

## Assumptions & Decisions

- 决策：本次采用“强撤销”，即 access token 每次请求都校验 `identity.session` active 状态，而不是继续保持纯无状态 access token。
- 决策：19C 仅做 auth/session hardening 与负例收尾，不把登录限流、邮箱验证、密码重置塞进同一轮。
- 假设：现有 `identity.session` 表结构已足够支持强撤销，无需新增 migration；`session_id` + `revoked_at` + `expires_at` 足以支撑请求期 session 校验。
- 假设：产品 controller 继续从 principal 读取 org/role，不会重新引入 header fallback。
- 约束：不改变既有 Consultant / Client-safe 业务能力，不新增业务端点，不改变现有 access policy vocabulary。

## 验证清单

1. 定向 auth 测试通过：
   - `AuthenticationControllerTest`
   - `IdentityAuthPostgresIntegrationTest`
   - 新增/扩展的 `JwtAuthenticationFilter` 相关 integration test
2. 定向 API 边界测试通过：
   - `ApiBoundaryRegressionClosureTest`
   - `ConsultantControllerLeakageTest`
   - `ClientSafeCandidateCardControllerTest`
3. 定向组织隔离测试通过：
   - `ConsultantWriteOrgIsolationIntegrationTest`
4. `services/core-api` 全量测试通过：
   - `mvn -q -f services/core-api/pom.xml test -DskipITs`
5. 文档校验通过：
   - roadmap/status/known-gaps 中不再保留 “19C 未完成” 的过期描述
   - 下一推荐任务已推进到 19C 之后
