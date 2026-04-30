# DeepSeek V4 Pro + Claude Code 适配报告

**日期**: 2026-04-30 | **项目**: AI Headhunting Transaction OS | **模型**: deepseek-v4-pro

---

## 一、当前已完成的配置

| 配置 | 状态 | 作用 |
|------|------|------|
| `CLAUDE.md` | ✅ | AI 项目上下文（86行），每次 Code 模式自动加载 |
| `PROMPT_WRITING_GUIDE.md` | ✅ | 写 task prompt 的 11 条规范（给人看） |
| `codex-task-operating-rules.md` | ✅ | AI 行为约束 24 条（给 AI 看） |
| `.claude/settings.json` | ✅ | 权限白名单 + PreToolUse hook 配置 |
| `.claude/hooks/block-dangerous.sh` | ✅ | 拦截 force push / push main / merge main / reset --hard / rm -rf |

**已具备的能力**: 上下文注入、行为约束、危险操作拦截、常用命令免确认

**缺口**: 无 skill、无 launch.json、无 rules 文件、无 memory 初始化

---

## 二、DeepSeek V4 Pro + Claude Code 兼容性深度分析

### 2.1 强项

| 维度 | 表现 | 说明 |
|------|------|------|
| 纯文本代码任务 | ⭐⭐⭐⭐⭐ | 几乎无缝，工具调用稳定 |
| 1M 上下文 | ⭐⭐⭐⭐⭐ | 比 Claude 200K 大 5 倍，适合大项目 |
| 成本 | ⭐⭐⭐⭐⭐ | 约为 Claude Sonnet 的 1/4~1/5 |
| 工具调用 | ⭐⭐⭐⭐ | 够用但不如 GPT-4o 精准 |
| reasoning_content | ⭐⭐⭐⭐ | 支持 extended thinking，但需回传历史 reasoning |

### 2.2 弱点与缓解策略

| 弱点 | 影响 | 缓解措施 |
|------|------|---------|
| **不支持图片/多模态** | 不能截图让 AI 分析、不能发 UI 设计稿 | 用文字描述替代；需要时临时切回 Anthropic 原生 |
| **工具调用不如 Claude 原生精准** | 可能调用错误工具、参数不对 | PreToolUse hook 拦截危险操作；CLAUDE.md 里写清命令格式 |
| **理解复杂指令不如 Claude** | 可能在复杂 task 中迷路 | Task prompt 必须按 PROMPT_WRITING_GUIDE 三层结构写 |
| **不如 Claude 保守谨慎** | 可能做不该做的事 | Hooks 硬拦截 + 人工 review 每个 diff |
| **reasoning_content 回传要求** | 不传历史 reasoning 会报错 | Claude Code 层面自动处理（使用 Anthropic 兼容端点时） |

### 2.3 Claude App Code 模式 vs CLI

| | Claude App Code 模式 | Claude Code CLI |
|---|---|---|
| `.claude/settings.json` | ✅ 读取 | ✅ 读取 |
| `CLAUDE.md` | ✅ 读取 | ✅ 读取 |
| hooks | ✅ 执行 | ✅ 执行 |
| skills | ✅ 可用 | ✅ 可用 |
| 聊天模式隔离 | ✅ 不受影响 | N/A |

**结论**: Claude App 的 Code 模式本质是内嵌的 Claude Code 引擎。所有配置在两个入口均生效。

### 2.4 已知的硬限制

1. **不支持图片** — 这是最明显的短板，DeepSeek 已灰度推送 Vision，API 端预计两周内开放
2. **旧模型名即将停用** — `deepseek-chat` 和 `deepseek-reasoner` 于 2026-07-24 停用，需确保使用 `deepseek-v4-pro`
3. **超时需要设长** — `API_TIMEOUT_MS` 建议 ≥ 600000ms (10min)，pro 模式推理较慢

---

## 三、可用的 Skills 和相关配置总结

### 3.1 Werkstatt（推荐 ⭐⭐⭐⭐⭐）

**来源**: `github.com/Bollwerkio/werkstatt` | **安装**: `/plugin marketplace add Bollwerkio/werkstatt`

Superpowers 的硬核进化版。为什么比 superpowers 更适合你：

| | Superpowers | Werkstatt |
|---|---|---|
| 模型适配 | Claude 原生优化 | 多平台，更宽容 |
| TDD 严格性 | 有，但可跳过 | 强制 RED-GREEN-REFACTOR，先于实现写代码会被删除 |
| Review 机制 | 单轮 | 两阶段（spec compliance → code quality） |
| Sub-agent 隔离 | 有 | 更强，per-task fresh subagent |
| 维护状态 | v5.0.7 (Mar 2026) | v1.x (Apr 2026)，更活跃 |

**建议采用 2 个 skill**（你已经有的规则不需要重复）:
- `test-driven-development` — 填补你现有规则的空缺
- `requesting-code-review` / `receiving-code-review` — 加强 review

### 3.2 Developer Kit for Claude Code（推荐 ⭐⭐⭐⭐）

**来源**: `github.com/giuseppe-trisciuoglio/developer-kit` | **安装**: `/plugin marketplace add giuseppe-trisciuoglio/developer-kit`

50+ Skills + 20+ Agents + 30+ Commands，专门针对 Java/Spring Boot。直接相关的：

| 组件 | 相关度 | 用途 |
|------|--------|------|
| spring-boot-code-review-expert agent | 🔥🔥🔥🔥🔥 | Java 代码审查 |
| java-security-expert agent | 🔥🔥🔥🔥🔥 | 安全审查（你的项目 security 要求高） |
| JUnit test skills | 🔥🔥🔥🔥 | 测试生成 |
| CRUD generation commands | 🔥🔥🔥 | 快速生成 CRUD（但要按你的 pattern） |

### 3.3 其他值得关注但优先级较低的

| 资源 | 用途 | 优先级 |
|------|------|--------|
| Trail of Bits 安全 skills | 智能合约/supply chain 审计，和你的项目不直接相关 | ⭐⭐ |
| claude-code-zh + china-mcp-servers | Spring Boot DDD + 飞书/钉钉，偏中文生态 | ⭐⭐ |
| DevOps skills (terraform, k8s) | 和你的 infra 暂时无关 | ⭐ |

### 3.4 尚未配置但建议考虑的

| 配置 | 文件位置 | 用途 |
|------|---------|------|
| `.claude/launch.json` | 项目根 | 一键启动 `npm run dev:web`，Code 模式内置浏览器预览前端 |
| `.claude/settings.local.json` | 项目根（gitignored） | 个人偏好覆盖（不提交到团队） |
| `.claude/rules/*.md` | 项目根 | 分模块规则文件，比 CLAUDE.md 更精细化 |
| `~/.claude/CLAUDE.md` | 全局 | 跨项目通用规则 |

---

## 四、Werkstatt vs 现有规则体系的冲突分析

你的 `codex-task-operating-rules.md` 已经定义了完整的 task 流程。Werkstatt 会叠加它自己的流程。两者如何共存？

| 功能模块 | 你已有的规则 | Werkstatt 的做法 | 是否冲突 |
|---------|------------|----------------|---------|
| Worktree 隔离 | ✅ Codex Execution Rules | ✅ using-git-worktrees (自动创建) | ⚠️ 可能重复创建 |
| TDD | ❌ 无强制规则 | ✅ 强制 RED-GREEN-REFACTOR | ✅ 互补 |
| Task 拆分 | ✅ Task Sizing Strategy | ✅ writing-plans (更细，2-5min/task) | ⚠️ 粒度可能冲突 |
| Code Review | ❌ 无系统 review | ✅ 两阶段 review | ✅ 互补 |
| Merge 原则 | ✅ 不允许 AI 自行 merge | ✅ finishing-a-development-branch (给选项) | ✅ 兼容 |
| Final Report | ✅ 五项分离 | ❌ 无 | ✅ 互补 |

**建议**: 不全量安装 Werkstatt。只取 `test-driven-development` 和 `requesting-code-review` 两个 skill。其余的你的规则已经覆盖。

---

## 五、推荐下一步行动（按优先级）

### P0：马上做，0 成本

1. **`.claude/launch.json`** — 配好前端预览
2. **确认 `API_TIMEOUT_MS`** — 确保 DeepSeek 不会因为超时中断

### P1：测试后决定

3. **安装 Werkstatt 的 TDD + Code Review 两个 skill** — 在一个小 task 上先试用
4. **评估 Developer Kit** — 看看 spring-boot-code-review-expert agent 的质量

### P2：按需

5. **`.claude/rules/`** — 当 CLAUDE.md 超过 150 行时，拆分到 rules
6. **`~/.claude/CLAUDE.md`** — 当你有跨项目通用规则时
7. **`.claude/settings.local.json`** — 当你有个人特殊偏好时

### 暂不做

- ❌ 全量装 superpowers（被 Werkstatt 替代）
- ❌ 全量装 Werkstatt（大部分功能你的规则已覆盖）
- ❌ MCP servers（你的项目暂时不需要外部工具连接）
- ❌ Memory 初始化（等积累更多使用数据后再说）

---

## 六、风险提示

1. **图片缺失是最大硬伤** — 如果任务涉及 UI review，DeepSeek 做不到。需要临时切回 Anthropic 原生模型
2. **Skill 和规则叠加会增加 token 消耗** — 每加一个 skill 都会占用上下文。不要贪多
3. **DeepSeek V4 Pro 和 Claude Opus 在复杂推理上仍有差距** — 非常复杂的架构决策建议用 Claude 原生模型 double-check
4. **hooks 依赖 `jq`** — 确保你的环境里有 `jq`，否则 hook 会静默失败

---

## 附录 A：DeepSeek 配置参考

```json
{
  "env": {
    "ANTHROPIC_BASE_URL": "https://api.deepseek.com/anthropic",
    "ANTHROPIC_AUTH_TOKEN": "<your-api-key>",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "deepseek-v4-pro",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "deepseek-v4-pro",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "deepseek-v4-flash",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1",
    "API_TIMEOUT_MS": "600000"
  }
}
```

## 附录 B：Werkstatt 安装命令

```bash
# 在 Claude Code 或 Claude App Code 模式中运行
/plugin marketplace add Bollwerkio/werkstatt
```

## 附录 C：Developer Kit 安装命令

```bash
/plugin marketplace add giuseppe-trisciuoglio/developer-kit
```
