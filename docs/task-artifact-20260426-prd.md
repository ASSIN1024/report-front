# Task Artifact: FTP 报表数据转换中间件 — 需求发现与 PRD 输出

**Date**: 2026-04-26  
**PM**: Alex  
**Requestor**: Adam

---

## Objective
Adam 提出内部系统想法：一个位于上游 RPA 和下游 RPA 之间的数据转换中间件。通过结构化追问，将模糊想法转化为完整 PRD。

## Key Discovery Process（12 轮追问）

| 轮次 | 追问点 | 关键结论 |
|------|--------|----------|
| Q1 | 用户与痛点 | 用户为开发人员；上游 RPA 爬数据→FTP，下游 RPA 从 FTP 取→入库，缺中间转换环节 |
| Q2 | 规则维护方式 | 需管理界面（非手工配置文件）；多数报表为中国风非标表格 |
| Q3 | 报表格式数量 | N 张表 = N 种格式，列数 10~100+，无模板复用 |
| Q4 | 量级 | 日均几十个文件，单文件最大 50MB，峰值低 |
| Q5 | FTP 路由策略 | 目录分流 + 文件名模式匹配 + 文件名日期自动提取为 pt_dt |
| Q6 | 异常值处理 | 列级配置异常值转换（如 "-" → "0"），防止下游类型报错 |
| Q7 | 配置表信息 | 每报表配一次固定不变（库名、表名、字段映射 JSON、分区信息） |
| Q8 | 字段映射模式 | 双模式：按列名（位置无关）+ 按列序号（兜底）；需支持重复列名和起始行列偏移 |
| Q9 | 打包策略 | 单批次多文件合并打包，因为下游 WebUI 上传慢 |
| Q10 | 源文件处理 | 处理后归档，避免重复扫描和目录混乱 |
| Q11 | 技术栈 | Vue 3 + SpringBoot，内部 Web 应用，告警在网页端 |

## PRD Output
完整 PRD 已生成至 `prd-data-pipeline-middleware.md`，包含：

1. Problem Statement（背景+4 大痛点）
2. Goals & Success Metrics（5 项量化指标）
3. Non-Goals（V1 不做的事 × 5）
4. Core User Stories（6 个故事 + 验收标准）
5. Solution Overview（核心流程+两大子系统+关键设计决策）
6. Technical Considerations（技术栈+依赖+风险+Open Questions）
7. Launch Plan（4 Phase 发布策略）
8. Appendix（配置 Excel 字段规格+界面功能清单+术语表）

## Next Steps
- Adam Review PRD
- 对齐下游 RPA 团队确认压缩包格式
- Sprint 0 Spike：POI/EasyExcel 对中国风表格解析能力验证
