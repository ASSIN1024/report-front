# 评估操作指南

> **文档版本**: V1.0
> **创建日期**: 2026-04-05
> **最后更新**: 2026-04-05

---

## 1. 概述

本文档指导如何执行 Harness 上下文工程验证系统的评估流程。

---

## 2. 评估触发方式

### 2.1 定期触发

每周五下午执行完整评估：

```bash
./harness/evaluation/test-cases/automated/run-evaluation.sh
```

### 2.2 手动触发

AI Agent 可在任何时候通过命令执行：

```bash
# 执行评估
./harness/evaluation/test-cases/automated/run-evaluation.sh

# 查看历史报告
ls -la harness/evaluation/reports/
```

### 2.3 事件触发

重大更新后自动运行基础测试。

---

## 3. 评估流程

### 3.1 步骤1: 评估前准备

1. 确认测试数据集完整：
   - `harness/evaluation/test-datasets/architecture.json`
   - `harness/evaluation/test-datasets/progress.json`
   - `harness/evaluation/test-datasets/conventions.json`
   - `harness/evaluation/test-datasets/api.json`

2. 确认所有服务正常运行：
   ```bash
   ./scripts/start.sh status
   ```

### 3.2 步骤2: 执行自动化测试

```bash
./harness/evaluation/test-cases/automated/run-evaluation.sh
```

输出：
- 测试执行日志
- 评分计算结果
- 失败用例列表

### 3.3 步骤3: 人工深度评估

1. 根据自动化结果确定抽检点
2. 按照 `harness/evaluation/test-cases/manual/deep-evaluation.md` 进行评估
3. 记录评分和评语

### 3.4 步骤4: 生成报告

1. 汇总自动化和人工评估结果
2. 使用 `harness/templates/eval-report-template.md` 生成报告
3. 保存到 `harness/evaluation/reports/YYYY-MM-DD-eval.md`

### 3.5 步骤5: 问题跟踪

1. 识别失败的测试点
2. 分析原因
3. 制定改进计划
4. 更新到下次评估重点

---

## 4. 质量指标

| 指标 | 目标值 | 告警阈值 |
|------|--------|----------|
| 综合评分 | ≥90% | <85% |
| 记忆准确率 | ≥95% | <90% |
| 应用率 | ≥85% | <80% |
| 执行时间 | ≤5分钟 | >10分钟 |

---

## 5. 报告管理

### 5.1 报告存储

位置：`harness/evaluation/reports/YYYY-MM-DD-eval.md`

### 5.2 报告保留

- 保留周期：90天
- 定期清理过期报告

### 5.3 历史对比

支持与上次评估结果对比：

```bash
# 查看最新报告
ls -t harness/evaluation/reports/ | head -1

# 对比两个报告
diff harness/evaluation/reports/eval-YYYY-MM-DD-1.md \
     harness/evaluation/reports/eval-YYYY-MM-DD-2.md
```

---

## 6. 异常处理

### 6.1 测试数据集缺失

检查文件是否存在：
```bash
ls -la harness/evaluation/test-datasets/*.json
```

### 6.2 执行失败

查看错误日志：
```bash
./harness/evaluation/test-cases/automated/run-evaluation.sh 2>&1
```

### 6.3 报告生成异常

验证 JSON 格式：
```bash
python3 -m json.tool harness/evaluation/test-datasets/*.json
```

---

## 7. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-04-05 | V1.0 | 初始版本 |
