# Harness Engineering 改造实施计划

&gt; **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为项目建立 Harness Engineering 基础框架，包括 AGENTS.md 知识地图、tasks.json 任务管理和文档体系

**Architecture:** 渐进式改造，保持现有功能不变，只添加新的 Harness 相关文件

**Tech Stack:** 现有技术栈保持不变（Vue 2, Spring Boot, MySQL）

---

## 文件结构映射

| 文件 | 操作 | 说明 |
|------|------|------|
| `AGENTS.md` | 创建 | AI Agent 知识地图 |
| `harness/tasks.json` | 创建 | 结构化任务清单 |
| `harness/progress-notes.md` | 创建 | 进度记录 |
| `harness/session-history/` | 创建 | 会话历史目录 |
| `harness/templates/plan-template.md` | 创建 | 计划文档模板 |
| `harness/templates/spec-template.md` | 创建 | 规范文档模板 |
| `docs/harness/` | 创建 | Harness 补充文档目录 |
| `docs/changes/` | 创建 | 变更记录目录 |

---

## Task 1: 创建 AGENTS.md 知识地图

**Files:**
- Create: `AGENTS.md`

- [ ] **Step 1: 创建 AGENTS.md 文件**

```markdown
# Harness Engineering: AI Agent Knowledge Map

&gt; **文档版本**: V1.0
&gt; **创建日期**: 2026-04-03

---

## 项目概述

**项目名称**: 报表数据处理平台
**项目状态**: 生产就绪，Phase 6 系统集成测试已完成
**改造目标**: 引入 Harness Engineering 模式，使 AI Agent 能稳定参与开发

**快速链接**:
- 设计文档: [docs/superpowers/specs/2026-04-03-harness-engineering-design.md](file:///workspace/docs/superpowers/specs/2026-04-03-harness-engineering-design.md)
- 任务清单: [harness/tasks.json](file:///workspace/harness/tasks.json)
- 进度记录: [harness/progress-notes.md](file:///workspace/harness/progress-notes.md)
- CLAUDE.md: [CLAUDE.md](file:///workspace/CLAUDE.md)

---

## 目录结构导航

### 代码位置
- **前端**: `src/` - Vue 2 + Element UI
- **后端**: `report-backend/` - Spring Boot + MyBatis Plus

### 文档体系
- **主要文档**: `docs/superpowers/`
  - `plans/` - 实施计划
  - `specs/` - 技术规范
- **Harness 补充**: `docs/harness/`
  - `conventions/` - 编码规范
  - `guides/` - 操作指南
  - `reference/` - 参考资料
- **变更记录**: `docs/changes/`

### 状态管理
- **任务清单**: `harness/tasks.json` (JSON 格式，单一事实来源)
- **进度记录**: `harness/progress-notes.md`
- **会话历史**: `harness/session-history/`

---

## 架构约束

### 技术栈规范
- **前端**: Vue 2.6, Vue Router 3, Vuex 3, Element UI 2, Axios 0.21
- **后端**: Java 8, Spring Boot 2.x, MyBatis Plus, Quartz, Druid
- **数据库**: MySQL 5.7+

### 代码组织原则
- **前后端分离**: 前端在 `src/`，后端在 `report-backend/`
- **分层架构**: Controller → Service → Mapper
- **命名约定**: 遵循现有代码风格

### 关键约束
- **不要删除**：现有文件和功能保持不变
- **只添加新内容**：Harness 相关文件是新增的
- **遵循现有模式**：修改现有代码时保持风格一致

---

## 会话协议

### 标准会话流程

1. **Orient（定位）**
   - 读取 `harness/progress-notes.md`
   - 读取 `harness/tasks.json`
   - 查看最近 git history

2. **Setup（准备）**
   - 确保开发环境正常（如果需要）
   - 验证现有功能（如果修改代码）

3. **Select Task（选择任务）**
   - 选择最高优先级的 `pending` 任务
   - 更新任务状态为 `in_progress`

4. **Implement（实施）**
   - 专注完成单个任务
   - 遵循架构约束

5. **Test（测试）**
   - 运行相关测试（如果修改代码）
   - 验证功能正常

6. **Record（记录）**
   - 更新 `harness/progress-notes.md`
   - 更新任务状态为 `completed`
   - Git 提交

---

## 验证标准

### 代码修改时
- 运行相关测试确保通过
- 遵循 ESLint（前端）和代码风格（后端）
- 提交信息符合 Conventional Commits 规范

### 文档更新时
- 保持文档结构一致
- 更新版本号和变更记录
- 确保链接正确

### 任务管理时
- 只修改 tasks.json 中的 status 字段
- 不删除、不重新排序任务
- 新任务追加到数组末尾

---

## 紧急联系人

遇到阻塞时，查阅以下文档：
- 设计文档: [docs/superpowers/specs/2026-04-03-harness-engineering-design.md](file:///workspace/docs/superpowers/specs/2026-04-03-harness-engineering-design.md)
- 项目概述: [CLAUDE.md](file:///workspace/CLAUDE.md)
- 技术栈: [TECH_STACK.md](file:///workspace/TECH_STACK.md)
```

- [ ] **Step 2: 提交 AGENTS.md**

```bash
git add AGENTS.md
git commit -m "docs: add AGENTS.md knowledge map for harness engineering"
```

---

## Task 2: 建立 harness/ 目录结构

**Files:**
- Create: `harness/` 目录
- Create: `harness/session-history/` 目录
- Create: `harness/templates/` 目录
- Create: `docs/harness/` 目录
- Create: `docs/harness/conventions/` 目录
- Create: `docs/harness/guides/` 目录
- Create: `docs/harness/reference/` 目录
- Create: `docs/changes/` 目录

- [ ] **Step 1: 创建所有目录**

```bash
mkdir -p harness/session-history
mkdir -p harness/templates
mkdir -p docs/harness/conventions
mkdir -p docs/harness/guides
mkdir -p docs/harness/reference
mkdir -p docs/changes
```

- [ ] **Step 2: 提交目录结构**

```bash
git add harness/ docs/harness/ docs/changes/
git commit -m "chore: create harness directory structure"
```

---

## Task 3: 创建 tasks.json 初始版本

**Files:**
- Create: `harness/tasks.json`

- [ ] **Step 1: 创建 tasks.json 文件**

```json
{
  "version": "1.0",
  "lastUpdated": "2026-04-03",
  "tasks": [
    {
      "id": "T-001",
      "title": "创建 AGENTS.md 知识地图",
      "description": "创建 AI Agent 的单一事实来源文档，包含项目概述、目录导航、架构约束等",
      "status": "pending",
      "priority": "high",
      "dependsOn": [],
      "createdAt": "2026-04-03",
      "completedAt": null,
      "assignee": "AI Assistant"
    },
    {
      "id": "T-002",
      "title": "建立 harness/ 目录结构",
      "description": "创建 Harness 状态管理所需的目录结构",
      "status": "pending",
      "priority": "high",
      "dependsOn": [],
      "createdAt": "2026-04-03",
      "completedAt": null,
      "assignee": "AI Assistant"
    },
    {
      "id": "T-003",
      "title": "创建 tasks.json 初始版本",
      "description": "创建结构化任务清单，JSON 格式，抗模型腐化",
      "status": "pending",
      "priority": "high",
      "dependsOn": ["T-002"],
      "createdAt": "2026-04-03",
      "completedAt": null,
      "assignee": "AI Assistant"
    },
    {
      "id": "T-004",
      "title": "创建 progress-notes.md",
      "description": "创建进度记录文件，每次会话结束时更新",
      "status": "pending",
      "priority": "high",
      "dependsOn": ["T-002"],
      "createdAt": "2026-04-03",
      "completedAt": null,
      "assignee": "AI Assistant"
    },
    {
      "id": "T-005",
      "title": "添加文档模板",
      "description": "创建计划和规范文档模板，保持一致性",
      "status": "pending",
      "priority": "medium",
      "dependsOn": ["T-002"],
      "createdAt": "2026-04-03",
      "completedAt": null,
      "assignee": "AI Assistant"
    }
  ]
}
```

- [ ] **Step 2: 验证 JSON 格式**

```bash
python3 -m json.tool harness/tasks.json &gt; /dev/null &amp;&amp; echo "JSON 格式正确" || echo "JSON 格式错误"
```

预期: `JSON 格式正确`

- [ ] **Step 3: 提交 tasks.json**

```bash
git add harness/tasks.json
git commit -m "chore: add initial tasks.json for harness engineering"
```

---

## Task 4: 创建 progress-notes.md

**Files:**
- Create: `harness/progress-notes.md`

- [ ] **Step 1: 创建 progress-notes.md 文件**

```markdown
# Progress Notes

&gt; **最后更新**: 2026-04-03

---

## 2026-04-03

### Session 1: Harness Engineering 改造启动

**完成的工作**:
- 完成 Harness Engineering 设计文档
- 创建实施计划
- 开始 Phase 1 核心框架搭建

**下一步**:
- 创建 AGENTS.md
- 建立 harness/ 目录结构
- 创建 tasks.json

**决策记录**:
- 采用 JSON 格式的 tasks.json 作为单一事实来源
- 整合现有 superpowers 文档体系
- 渐进式改造，保持现有功能不变

**遇到的问题**: 无
```

- [ ] **Step 2: 提交 progress-notes.md**

```bash
git add harness/progress-notes.md
git commit -m "chore: add progress-notes.md for session tracking"
```

---

## Task 5: 添加文档模板

**Files:**
- Create: `harness/templates/plan-template.md`
- Create: `harness/templates/spec-template.md`

- [ ] **Step 1: 创建 plan-template.md**

```markdown
# [Feature Name] Implementation Plan

&gt; **创建日期**: YYYY-MM-DD
&gt; **负责人**: [Name]
&gt; **状态**: 进行中

---

## 概述

**目标**: [一句话描述目标]

**背景**: [为什么要做这个]

---

## 任务清单

- [ ] Task 1: [任务描述]
- [ ] Task 2: [任务描述]
- [ ] Task 3: [任务描述]

---

## 验收标准

- [ ] [标准 1]
- [ ] [标准 2]
- [ ] [标准 3]

---

## 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| YYYY-MM-DD | V1.0 | 初始版本 | [Name] |
```

- [ ] **Step 2: 创建 spec-template.md**

```markdown
# [Feature Name] Design Specification

&gt; **文档版本**: V1.0
&gt; **创建日期**: YYYY-MM-DD
&gt; **最后更新**: YYYY-MM-DD
&gt; **文档状态**: 进行中

---

## 1. 项目概述

### 1.1 背景
[为什么需要这个功能]

### 1.2 目标
| 目标 | 描述 |
|------|------|
| [目标1] | [描述] |
| [目标2] | [描述] |

### 1.3 范围
| 范围 | 说明 |
|------|------|
| 包含 | [功能范围] |
| 不包含 | [排除范围] |

---

## 2. 架构设计

### 2.1 整体架构
[架构图或描述]

### 2.2 模块划分
| 模块 | 职责 |
|------|------|
| [模块1] | [职责] |
| [模块2] | [职责] |

---

## 3. 详细设计

### 3.1 数据模型
[实体、字段、关系]

### 3.2 API 设计
[接口列表、请求/响应格式]

### 3.3 界面设计
[页面结构、交互流程]

---

## 4. 实施计划

| 阶段 | 任务 | 优先级 |
|------|------|--------|
| Phase 1 | [任务] | 高 |
| Phase 2 | [任务] | 中 |

---

## 5. 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| [风险1] | [高/中/低] | [措施] |

---

## 6. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| YYYY-MM-DD | V1.0 | 初始版本 | [Name] |
```

- [ ] **Step 3: 提交文档模板**

```bash
git add harness/templates/
git commit -m "chore: add document templates for plans and specs"
```

---

## Task 6: 更新 tasks.json 状态（收尾）

**Files:**
- Modify: `harness/tasks.json`

- [ ] **Step 1: 更新任务状态为 completed**

将 T-001 到 T-005 的 `status` 改为 `"completed"`，`completedAt` 改为 `"2026-04-03"`

- [ ] **Step 2: 验证 JSON 格式**

```bash
python3 -m json.tool harness/tasks.json &gt; /dev/null &amp;&amp; echo "JSON 格式正确" || echo "JSON 格式错误"
```

预期: `JSON 格式正确`

- [ ] **Step 3: 提交更新**

```bash
git add harness/tasks.json
git commit -m "chore: update tasks.json status to completed for Phase 1"
```

- [ ] **Step 4: 更新 progress-notes.md**

在 progress-notes.md 中添加本次会话的完成记录

---

## 自我审查

### 1. Spec 覆盖检查
- ✅ AGENTS.md 知识地图 - Task 1
- ✅ harness/ 目录结构 - Task 2
- ✅ tasks.json 任务清单 - Task 3
- ✅ progress-notes.md 进度记录 - Task 4
- ✅ 文档模板 - Task 5
- ✅ 状态更新 - Task 6

### 2. 占位符扫描
- ✅ 无 TBD/TODO
- ✅ 所有文件路径完整
- ✅ 所有代码块完整
- ✅ 所有命令完整

### 3. 一致性检查
- ✅ 文件名一致
- ✅ 目录结构一致
- ✅ 任务 ID 一致

---

计划完成并保存到 [docs/superpowers/plans/2026-04-03-harness-engineering-implementation.md](file:///workspace/docs/superpowers/plans/2026-04-03-harness-engineering-implementation.md)。

**两个执行选项:**

**1. Subagent-Driven (推荐)** - 我为每个任务调度一个新的子代理，在任务之间进行审查，快速迭代

**2. Inline Execution** - 在本次会话中使用 executing-plans 执行任务，批量执行并设置检查点

**选择哪种方式?**
