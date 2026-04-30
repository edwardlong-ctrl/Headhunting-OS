# Task 19B 执行计划

## 摘要
本计划旨在完成 Task 19B：将所有的产品侧 Controller（`ConsultantCompanyController`、`ConsultantJobController`、`ConsultantShortlistController`、`ConsultantDocumentController`、`ClientSafeCandidateCardController`）及访问上下文适配器（`ClientSafeCandidateCardApiAccessContextAdapter`），从依赖临时 Header（`X-RTO-Actor-Role` 和 `X-RTO-Organization-Id`）的鉴权方式，全面迁移至基于 JWT 和 `SecurityContext` 的生产级 Spring Security 鉴权机制。

## 当前状态分析
- Task 19A 已引入基础的 Spring Security 配置、JWT 解析机制，并提供了基于 `identity.user_account` 与 `identity.session` 的真实登录流。
- 目前 `SecurityConfig` 开放了所有的非 `/api/auth/**` 接口（`permitAll`），业务 Controller 仍然在方法签名中通过 `@RequestHeader` 读取临时的角色和组织 ID。
- 测试套件中大量依赖 `.header("X-RTO-Actor-Role", ...)` 等方式模拟用户身份。

## 具体修改内容

### 1. 引入测试依赖
- **文件**：`services/core-api/pom.xml`
- **操作**：添加 `spring-security-test` 依赖（`scope=test`），以便在 `@WebMvcTest` 中使用 `SecurityMockMvcRequestPostProcessors.authentication()` 注入 `RtoAuthenticationToken`。

### 2. 完善安全拦截与全局 401 处理
- **文件**：`services/core-api/src/main/java/com/recruitingtransactionos/coreapi/identityauth/SecurityConfig.java`
- **操作**：
  - 更新 `authorizeHttpRequests` 规则，仅开放 `/api/auth/**` 和可能的 `/health`（如果有），要求其余 `/api/**` 必须 `authenticated()`。
  - 增加 `.exceptionHandling()` 并注册自定义的 `AuthenticationEntryPoint`，当无有效 JWT 访问受保护接口时，返回标准化的 `ApiResponseEnvelope` 格式的 401 响应（`errorCode: authentication_failed`, `safeReason: authentication_required`）。

### 3. Controller 迁移至 `@AuthenticationPrincipal`
- **涉及文件**：
  - `ConsultantCompanyController.java`
  - `ConsultantJobController.java`
  - `ConsultantShortlistController.java`
  - `ConsultantDocumentController.java`
  - `ClientSafeCandidateCardController.java`
- **操作**：
  - 移除 `ACTOR_ROLE_HEADER` 和 `ORGANIZATION_ID_HEADER` 常量及对应 `@RequestHeader` 参数。
  - 注入 `@AuthenticationPrincipal RtoAuthenticatedPrincipal principal`。
  - 修改 `requireConsultantRole()` 的签名与实现，改为接收并判断 `PortalRole` 枚举。
  - 直接从 `principal.organizationId()` 获取组织 ID，移除原有的 `parseOrganizationId` 辅助方法。

### 4. 上下文适配器迁移
- **文件**：`ClientSafeCandidateCardApiAccessContextAdapter.java`
- **操作**：
  - 移除已废弃的身份 Header 常量。
  - 将 `fromHeaders` 签名变更为 `fromPrincipal(RtoAuthenticatedPrincipal principal, String fieldClassificationHeader, String identityDisclosureRequestedHeader)`。
  - 将 `queryScopeFromHeaders` 变更为 `queryScopeFromPrincipal(RtoAuthenticatedPrincipal principal)`。

### 5. 测试套件适配与回归修复
- **涉及文件**：
  - `ConsultantControllerLeakageTest.java`
  - `ClientSafeCandidateCardControllerTest.java`
  - `ApiBoundaryRegressionClosureTest.java`
- **操作**：
  - 移除所有对 `X-RTO-Actor-Role` 和 `X-RTO-Organization-Id` Header 的手动注入。
  - 引入 `org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication`。
  - 使用构造合法的 `RtoAuthenticationToken` 模拟有效的鉴权上下文。
  - 针对原有的“缺少角色 Header 返回 403”的测试，调整为验证“未携带/携带无效 Token 时，Spring Security 拦截并返回 401”。
  - 针对“携带错误角色（如 client）访问 consultant 接口”的测试，保持验证 Controller 层抛出领域 `AccessDeniedException` 并返回 403。

## 假设与决策
- **非身份 Header 保留**：`X-RTO-Field-Classification` 与 `X-RTO-Identity-Disclosure-Requested` 不属于认证范畴（而是上下文修饰符），在本次重构中将作为普通 Header 予以保留。
- **平滑过渡**：Task 19A 已完成 JWT 发放，Task 19B 将完成 Controller 的强制接管，因此一旦上线，所有的业务请求必须携带合法的 Bearer Token。

## 验证步骤
1. 执行 `mvn -f services/core-api/pom.xml test`。
2. 确保之前在 Task 19A 完成后的 664 个测试用例，在修改鉴权上下文后仍保持通过且无 Failure。
3. 确保更新 roadmap 状态文档（`implementation-status.md` 等），将 Task 19B 标记为完成。
