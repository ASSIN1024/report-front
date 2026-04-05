# Harness 上下文工程评估模块

> **模块版本**: V1.0
> **创建日期**: 2026-04-05

---

## 概述

Harness 上下文工程验证系统用于确保 AI Agent 能够准确记忆并应用项目的关键信息。

## 目录结构

```
evaluation/
├── test-datasets/           # 测试数据集
│   ├── architecture.json   # 架构信息 (15点)
│   ├── progress.json       # 开发进度 (12点)
│   ├── conventions.json    # 交互规范 (15点)
│   └── api.json            # 接口信息 (10点)
├── test-cases/
│   ├── automated/          # 自动化测试
│   │   └── run-evaluation.sh
│   └── manual/             # 人工评估
│       └── deep-evaluation.md
└── reports/                # 评估报告
```

## 快速开始

```bash
# 执行评估
./harness/evaluation/test-cases/automated/run-evaluation.sh
```

## 更多信息

- [评估操作指南](../guides/evaluation-guide.md)
- [人工评估指南](./test-cases/manual/deep-evaluation.md)
- [评估报告模板](../templates/eval-report-template.md)
