# Harness 上下文工程验证系统设计方案

> **文档版本**: V1.0
> **创建日期**: 2026-04-05
> **最后更新**: 2026-04-05
> **维护人**: AI Assistant

---

## 1. 概述

### 1.1 背景

随着 Harness Engineering 体系在项目中的深入应用，需要一套系统化的验证机制，确保 AI Agent 能够准确记忆并应用项目的关键信息。本方案旨在构建一个全面的测试系统，验证上下文工程的有效性。

### 1.2 目标

- 验证 AI 对项目核心信息的记忆准确性
- 评估 AI 在实际开发任务中的应用能力
- 建立持续验证机制，确保长期稳定运行
- 制定标准化操作流程和质量监控指标

### 1.3 设计原则

- **均衡发展**: 准确性、完整性、实用性并重
- **混合验证**: 自动化测试 + 人工深度评估结合
- **可量化**: 所有指标均可测量和追踪
- **可持续**: 建立长期运行和维护机制

---

## 2. 整体架构

### 2.1 目录结构

```
harness/
├── evaluation/                    # 评估模块
│   ├── test-datasets/             # 测试数据集
│   │   ├── architecture.json     # 架构信息测试集
│   │   ├── progress.json         # 开发进度测试集
│   │   ├── conventions.json      # 交互规范测试集
│   │   └── api.json              # 接口信息测试集
│   ├── test-cases/               # 测试用例
│   │   ├── automated/            # 自动化测试用例
│   │   │   └── run-evaluation.sh # 自动执行脚本
│   │   └── manual/              # 人工评估用例
│   │       └── deep-evaluation.md
│   └── reports/                  # 测试报告
│       └── YYYY-MM-DD-eval.md
└── guides/
    └── evaluation-guide.md       # 评估操作指南
```

### 2.2 核心组件

| 组件 | 说明 |
|------|------|
| 测试数据集 | 结构化 JSON 文件，包含4大维度50+测试点 |
| 自动化测试器 | Shell脚本执行测试用例，自动评分 |
| 人工评估指南 | 用于深度评估AI应用能力 |
| 报告生成器 | 自动生成评估报告 |

---

## 3. 测试数据集设计

### 3.1 数据集结构

```json
{
  "id": "ARCH-001",
  "dimension": "architecture",
  "category": "tech-stack",
  "question": "后端使用的JDK版本是什么？",
  "expectedAnswer": "JDK 1.8",
  "source": "harness/docs/project-context.md",
  "difficulty": "easy",
  "type": "fact-retrieval",
  "evaluation": {
    "scoring": "exact-match",
    "keywords": ["JDK", "1.8", "Java 8"]
  }
}
```

### 3.2 四大维度

| 维度 | 测试点数量 | 内容范围 |
|------|-----------|----------|
| architecture | 15 | 技术栈、代码组织、命名规范、架构约束 |
| progress | 12 | 里程碑、已完成功能、待办任务 |
| conventions | 15 | Git规范、代码审查、文档同步、操作规则 |
| api | 10 | 接口路径、请求格式、数据结构 |

### 3.3 测试点分布

#### Architecture (15点)

| ID | 类别 | 问题示例 | 难度 |
|----|------|----------|------|
| ARCH-001 | tech-stack | 后端使用的JDK版本是什么？ | easy |
| ARCH-002 | tech-stack | 前端使用的UI框架是什么？ | easy |
| ARCH-003 | code-org | Controller层应该只做什么？ | medium |
| ARCH-004 | naming | Job类的命名规范是什么？ | easy |
| ARCH-005 | naming | Service接口的命名规范是什么？ | medium |
| ARCH-006 | architecture | 业务逻辑应该放在哪一层？ | medium |
| ARCH-007 | constraints | 数据库操作在哪一层？ | medium |
| ARCH-008 | tech-stack | 后端使用什么ORM框架？ | easy |
| ARCH-009 | tech-stack | 前端状态管理方案是什么？ | easy |
| ARCH-010 | tech-stack | 使用什么数据库连接池？ | medium |
| ARCH-011 | tech-stack | 定时任务使用什么方案？ | medium |
| ARCH-012 | code-org | 工具类应该放在哪个包？ | easy |
| ARCH-013 | naming | Mapper接口的命名规范是什么？ | easy |
| ARCH-014 | naming | Entity实体的命名规范是什么？ | easy |
| ARCH-015 | tech-stack | 前后端分别使用什么端口？ | easy |

#### Progress (12点)

| ID | 类别 | 问题示例 | 难度 |
|----|------|----------|------|
| PROG-001 | milestone | Phase 6的里程碑是什么？ | easy |
| PROG-002 | milestone | 哪些Phase已经完成？ | medium |
| PROG-003 | feature | 已完成哪些后端模块？ | medium |
| PROG-004 | feature | 已完成哪些前端模块？ | medium |
| PROG-005 | feature | 核心处理器包含哪些组件？ | medium |
| PROG-006 | feature | 日志系统包含哪些功能？ | medium |
| PROG-007 | feature | 自动建表机制是什么？ | hard |
| PROG-008 | feature | 去重机制使用什么方案？ | hard |
| PROG-009 | todo | 待完成的功能有哪些？ | medium |
| PROG-010 | milestone | 当前项目处于哪个Phase？ | easy |
| PROG-011 | feature | FTP文件去重机制是什么时候添加的？ | medium |
| PROG-012 | feature | E2E测试有多少个测试点？ | medium |

#### Conventions (15点)

| ID | 类别 | 问题示例 | 难度 |
|----|------|----------|------|
| CONF-001 | git | Git提交信息的格式是什么？ | easy |
| CONF-002 | git | Type类型有哪些？ | medium |
| CONF-003 | task | 任务状态转换的顺序是什么？ | easy |
| CONF-004 | task | 任务ID的前缀是什么？ | easy |
| CONF-005 | task | tasks.json的操作规则是什么？ | medium |
| CONF-006 | doc | 文档更新应在多少小时内完成？ | medium |
| CONF-007 | doc | 变更记录需要包含哪些字段？ | medium |
| CONF-008 | review | 代码审查清单包含哪些项目？ | medium |
| CONF-009 | test | 测试要求有哪些？ | medium |
| CONF-010 | version | 版本号规则是什么？ | hard |
| CONF-011 | git | 关联任务应该怎么写？ | easy |
| CONF-012 | doc | 重大更新包括哪些类型？ | hard |
| CONF-013 | operation | 会话协议的第一步是什么？ | medium |
| CONF-014 | operation | 如何启动所有服务？ | easy |
| CONF-015 | operation | 如何检查服务状态？ | easy |

#### API (10点)

| ID | 类别 | 问题示例 | 难度 |
|----|------|----------|------|
| API-001 | endpoint | FTP配置列表的接口路径是什么？ | medium |
| API-002 | format | 统一返回结果的结构是什么？ | medium |
| API-003 | format | Result类包含哪些字段？ | medium |
| API-004 | endpoint | 报表配置管理的接口有哪些？ | hard |
| API-005 | format | 分页查询的参数有哪些？ | hard |
| API-006 | format | 任务查询DTO包含哪些字段？ | hard |
| API-007 | endpoint | 手动触发任务的端点是什么？ | medium |
| API-008 | format | ColumnMapping的数据结构是什么？ | hard |
| API-009 | endpoint | 日志查询接口的路径是什么？ | medium |
| API-010 | format | ErrorCode枚举包含哪些错误码？ | hard |

---

## 4. 验证流程

### 4.1 流程图

```
┌─────────────────────────────────────────────────────────┐
│                    验证流程                               │
├─────────────────────────────────────────────────────────┤
│  1. 初始化: 加载测试数据集 (JSON)                         │
│           ↓                                              │
│  2. 自动化测试: 执行50+测试用例                           │
│           ↓                                              │
│  3. 评分计算: 准确率/完整率/应用率                        │
│           ↓                                              │
│  4. 人工抽检: 深度评估应用能力 (每类2-3个)                 │
│           ↓                                              │
│  5. 报告生成: 输出评估报告                               │
└─────────────────────────────────────────────────────────┘
```

### 4.2 自动化测试执行

```bash
# 执行评估
./harness/evaluation/test-cases/automated/run-evaluation.sh

# 输出结果
- 测试执行日志
- 评分计算结果
- 失败用例列表
```

### 4.3 人工抽检清单

每类维度抽取2-3个测试点进行深度评估：

| 维度 | 抽检数量 | 评估重点 |
|------|---------|----------|
| architecture | 3 | 能否准确理解和应用架构约束 |
| progress | 2 | 能否正确把握项目进度 |
| conventions | 3 | 能否遵循交互规范 |
| api | 2 | 能否正确使用接口信息 |

---

## 5. 质量指标体系

### 5.1 核心指标

| 指标 | 目标值 | 计算方式 | 权重 |
|------|--------|----------|------|
| **记忆准确率** | ≥95% | 事实性问题正确数 / 总数 | 0.4 |
| **完整率** | ≥90% | 完整回答数 / 总问题数 | 0.3 |
| **应用率** | ≥85% | 正确应用的测试点数 / 需要应用的总数 | 0.3 |
| **综合评分** | ≥90% | (准确率×0.4 + 完整率×0.3 + 应用率×0.3) | - |

### 5.2 辅助指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 执行时间 | ≤5分钟 | 自动化测试应在5分钟内完成 |
| 测试覆盖率 | 100% | 所有测试点都应被覆盖 |
| 报告完整率 | 100% | 报告包含所有必需章节 |

### 5.3 告警阈值

| 指标 | 告警阈值 | 处理方式 |
|------|----------|----------|
| 综合评分 | <85% | 触发人工复审 |
| 记忆准确率 | <90% | 需要更新测试数据集 |
| 应用率 | <80% | 需要改进上下文管理 |

---

## 6. 持续验证机制

### 6.1 触发方式

| 方式 | 频率 | 说明 |
|------|------|------|
| 定期触发 | 每周五 | 自动执行完整评估 |
| 手动触发 | 按需 | AI Agent 通过命令执行 |
| 事件触发 | 重大更新后 | 自动运行基础测试 |

### 6.2 监控告警

```bash
# 监控命令
./scripts/start.sh status

# 评估执行
./harness/evaluation/test-cases/automated/run-evaluation.sh
```

### 6.3 报告归档

- 报告存储位置: `harness/evaluation/reports/YYYY-MM-DD-eval.md`
- 报告保留周期: 90天
- 历史对比: 支持与上次评估结果对比

---

## 7. 操作流程

### 7.1 标准化操作流程

#### 步骤1: 评估前准备
1. 确认测试数据集完整
2. 确认所有服务正常运行
3. 记录基准版本

#### 步骤2: 执行自动化测试
1. 执行 `run-evaluation.sh`
2. 收集测试结果
3. 计算各项指标

#### 步骤3: 人工深度评估
1. 根据自动化结果抽取测试点
2. 进行深度对话测试
3. 记录应用能力评估

#### 步骤4: 生成报告
1. 汇总自动化和人工评估结果
2. 生成评估报告
3. 提交到 reports 目录

#### 步骤5: 问题跟踪
1. 识别失败的测试点
2. 分析原因
3. 制定改进计划

### 7.2 质量检查清单

- [ ] 测试数据集完整无遗漏
- [ ] 自动化测试执行成功
- [ ] 所有指标达到目标值
- [ ] 报告格式符合模板
- [ ] 失败用例已记录原因
- [ ] 后续改进计划已制定

---

## 8. 测试用例示例

### 8.1 fact-retrieval 类型

```json
{
  "id": "ARCH-001",
  "dimension": "architecture",
  "category": "tech-stack",
  "question": "后端使用的JDK版本是什么？",
  "expectedAnswer": "JDK 1.8",
  "source": "harness/docs/project-context.md",
  "difficulty": "easy",
  "type": "fact-retrieval",
  "evaluation": {
    "scoring": "exact-match",
    "keywords": ["JDK", "1.8", "Java 8"]
  }
}
```

### 8.2 application 类型

```json
{
  "id": "ARCH-006",
  "dimension": "architecture",
  "category": "code-org",
  "question": "在开发新功能时，业务逻辑应该放在哪一层？",
  "expectedAnswer": "Service层",
  "source": "harness/docs/project-context.md",
  "difficulty": "medium",
  "type": "application",
  "evaluation": {
    "scoring": "concept-match",
    "criteria": ["理解架构约束", "正确回答层级", "能说明原因"]
  }
}
```

### 8.3 integration 类型

```json
{
  "id": "API-001",
  "dimension": "api",
  "category": "endpoint",
  "question": "如果要查询FTP配置列表，前端应该调用哪个API接口？",
  "expectedAnswer": "GET /api/ftp-config/list",
  "source": "API.md",
  "difficulty": "medium",
  "type": "integration",
  "evaluation": {
    "scoring": "path-match",
    "required": ["method", "path"]
  }
}
```

---

## 9. 报告模板

```markdown
# Harness 上下文工程评估报告

> **评估日期**: YYYY-MM-DD
> **评估人**: AI Assistant
> **版本**: v1.0

---

## 1. 执行摘要

| 指标 | 目标值 | 实际值 | 状态 |
|------|--------|--------|------|
| 记忆准确率 | ≥95% | XX% | ✅/⚠️/❌ |
| 完整率 | ≥90% | XX% | ✅/⚠️/❌ |
| 应用率 | ≥85% | XX% | ✅/⚠️/❌ |
| 综合评分 | ≥90% | XX% | ✅/⚠️/❌ |

---

## 2. 详细结果

### 2.1 Architecture 维度

| 测试点 | 结果 | 详情 |
|--------|------|------|
| ARCH-001 | ✅ | 正确 |
| ARCH-002 | ❌ | 回答错误 |

...

### 2.2 Progress 维度

...

### 2.3 Conventions 维度

...

### 2.4 API 维度

...

---

## 3. 人工评估

### 3.1 深度评估测试点

| 测试点 | 评估结果 | 说明 |
|--------|----------|------|
| ARCH-006 | ✅ | 正确理解并应用 |

### 3.2 应用能力分析

...

---

## 4. 问题分析

### 4.1 失败用例

| ID | 问题描述 | 原因分析 | 改进建议 |
|----|----------|----------|----------|
| ARCH-XXX | ... | ... | ... |

### 4.2 根因分析

...

---

## 5. 改进计划

| 优先级 | 改进项 | 负责人 | 完成日期 |
|--------|--------|--------|----------|
| 高 | ... | ... | ... |

---

## 6. 结论

...
```

---

## 10. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-04-05 | V1.0 | 初始版本创建 | AI Assistant |
