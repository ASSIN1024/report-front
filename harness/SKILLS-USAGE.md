# Harness Skills 使用指南

> 本文档介绍如何在 Trae 中使用已安装的 Harness Skills

---

## 📦 安装状态

| 项目 | 状态 |
|------|------|
| **位置** | `.trae/skills/` |
| **Skills 数量** | 33+ |
| **中文支持** | ✅ 已添加 |

---

## 🚀 快速开始

### 使用 Skill Tool

在 Trae 中，通过 **Skill tool** 调用 skills：

```
/harness-setup    # 项目初始化
/harness-plan     # 任务规划
/harness-work     # 执行任务
/harness-review   # 代码审查
/harness-sync     # 状态同步
/harness-release  # 发布管理
```

### 自然语言触发

Trae 会根据描述中的关键词自动加载对应的 skill：

- 说"帮我制定计划" → 自动加载 `harness-plan`
- 说"帮我实现这个功能" → 自动加载 `harness-work`
- 说"代码审查" → 自动加载 `harness-review`

---

## 📋 Skills 清单

### 核心 Harness Skills

| Skill | 功能 | 触发词 |
|-------|------|--------|
| **harness-setup** | 项目初始化、工具配置 | setup、init、新项目 |
| **harness-plan** | 任务规划、进度管理 | 制定计划、添加任务 |
| **harness-work** | 执行计划中的任务 | 实现、执行、完成 |
| **harness-review** | 多角度代码审查 | review、代码审查 |
| **harness-sync** | Plans.md 与实现同步 | sync-status、检查进度 |
| **harness-loop** | 长时间任务循环 | loop、autonomous |
| **harness-release** | 发布自动化 | release、publish |

### 辅助 Skills

| Skill | 功能 | 触发词 |
|-------|------|--------|
| **memory** | 记忆管理、SSOT | memory、decisions |
| **session** | 会话管理 | /session |
| **breezing** | 团队执行模式 | breezing、团队执行 |
| **ci** | CI 问题处理 | CI 失败、构建错误 |
| **crud** | CRUD 代码生成 | CRUD、实体生成 |
| **ui** | UI 组件生成 | 组件、UI、表单 |

---

## 📖 详细使用说明

### 1. harness-setup - 项目初始化

```
/harness-setup [init|ci|harness-mem|mirrors|agents]
```

**功能**：
- 项目初始化
- 工具配置（ESLint、Prettier 等）
- Agent 设置
- 记忆设置
- 技能同步

**示例**：
```
/harness-setup init
/harness-setup ci
/harness-setup harness-mem
```

### 2. harness-plan - 任务规划

```
/harness-plan [create|add|update|sync]
```

**功能**：
- 创建实施计划
- 添加新任务
- 更新任务状态
- 同步进度

**示例**：
```
/harness-plan create
/harness-plan add
/harness-plan sync
```

### 3. harness-work - 任务执行

```
/harness-work [all|task-number] [--parallel N] [--breezing]
```

**功能**：
- 执行单个或多个任务
- 并行任务执行
- 团队执行模式

**示例**：
```
/harness-work all
/harness-work 1-5
/harness-work all --parallel 3
```

### 4. harness-review - 代码审查

```
/harness-review [code|plan|scope] [--security] [--dual]
```

**功能**：
- 代码审查
- 计划审查
- 安全检查
- 范围分析

**示例**：
```
/harness-review code
/harness-review code --security
/harness-review plan
```

### 5. harness-sync - 状态同步

```
/harness-sync [--snapshot|--no-retro]
```

**功能**：
- 同步 Plans.md 与实现状态
- 检测漂移
- 更新标记
- 保存快照

**示例**：
```
/harness-sync
/harness-sync --snapshot
```

### 6. harness-release - 发布管理

```
/harness-release [patch|minor|major|--dry-run]
```

**功能**：
- 版本检测
- CHANGELOG 更新
- Git 标签
- GitHub Release

**示例**：
```
/harness-release patch
/harness-release --dry-run
```

---

## 🔧 常见工作流

### 完整项目开发流程

```
1. 初始化项目
   /harness-setup init

2. 制定计划
   /harness-plan create

3. 执行任务
   /harness-work all

4. 代码审查
   /harness-review code --security

5. 同步状态
   /harness-sync --snapshot

6. 发布
   /harness-release minor
```

### 快速 Bug 修复流程

```
1. 分析问题
   /ci analyze

2. 制定修复计划
   /harness-plan add

3. 执行修复
   /harness-work 1

4. 审查
   /harness-review code

5. 同步
   /harness-sync
```

---

## 📁 相关文件

- **AGENTS.md** - AI Agent 知识地图
- **harness/** - Harness 工程目录
  - **tasks.json** - 结构化任务清单
  - **progress-notes.md** - 进度记录
  - **session-history/** - 会话历史归档
- **docs/superpowers/** - 技术规范和实施计划

---

## ⚠️ 注意事项

1. **不要混用**：每个 skill 有明确的职责，不要用于其他场景
2. **按顺序执行**：建议按照 setup → plan → work → review → release 的顺序
3. **状态同步**：每次变更后记得使用 `harness-sync` 同步状态
4. **中文支持**：所有 skills 已添加中文描述，触发词也支持中文

---

## ❓ 常见问题

**Q: 为什么我的 skill 没有被识别？**
A: 确保在 `.trae/skills/` 目录下有对应 skill 名称的文件夹，且包含 `SKILL.md` 文件。

**Q: 如何查看所有可用的 skills？**
A: 查看 `.trae/skills/` 目录，或使用 `/memory` 查询。

**Q: skill 不工作怎么办？**
A: 检查 SKILL.md 的 YAML 格式是否正确，特别是 frontmatter 部分。

---

**最后更新**: 2026-04-27
**版本**: v1.0
