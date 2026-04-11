# Playwright MCP E2E 自动化测试设计方案

> **文档版本**: V1.0
> **创建日期**: 2026-04-11
> **维护人**: AI Assistant

---

## 1. 概述

### 1.1 项目背景

根据 Playwright MCP 教程，为报表数据处理平台实现浏览器级端到端自动化测试。通过 TRAE IDE 的 Playwright MCP 工具，由 AI 驱动动态执行测试步骤，验证完整的业务流程。

### 1.2 测试目标

实现完整业务流程的自动化测试：
- 登录系统 → FTP配置管理 → 创建测试Excel文件 → 报表配置管理 → 任务触发执行 → 数据验证 → 日志查看

### 1.3 测试模式

**Playwright MCP + AI 驱动模式**
- 不编写固定脚本
- 通过 TRAE IDE 的 Playwright MCP 工具
- AI 根据页面状态动态决定执行步骤

---

## 2. 前置条件

| 步骤 | 操作 | 验证方式 |
|-----|------|---------|
| 2.1 | 后端服务运行 (localhost:8082) | curl http://localhost:8082/api/health |
| 2.2 | 前端服务运行 (localhost:8080) | 浏览器访问 |
| 2.3 | 内置FTP已配置并启用 | FTP目录可访问 |

---

## 3. AI 驱动测试流程

### 3.1 阶段1: 登录系统

**操作步骤**:
1. `playwright_navigate("http://localhost:8080/#/login")`
2. 等待页面加载
3. `playwright_fill(username输入框, "admin")`
4. `playwright_fill(password输入框, "xxx")`
5. `playwright_click(登录按钮)`
6. `playwright_expect_response("**/api/auth/login**")`

**验证点**:
- URL 跳转到 /ftp
- 页面显示 FTP 配置管理界面

---

### 3.2 阶段2: FTP配置管理

**操作步骤**:
1. `playwright_click(新增配置按钮)`
2. `playwright_fill(配置名称, "E2E测试FTP")`
3. `playwright_fill(主机地址, "localhost")`
4. `playwright_fill(端口, "2121")`
5. `playwright_fill(用户名, "test")`
6. `playwright_fill(密码, "test123")`
7. `playwright_fill(扫描路径, "/upload")`
8. `playwright_fill(文件匹配, "*.xlsx")`
9. `playwright_click(保存按钮)`
10. `playwright_expect_response("**/api/ftp/config**")`

**验证点**:
- 配置出现在 FTP 配置列表中
- 列表显示配置名称 "E2E测试FTP"

---

### 3.3 阶段3: 创建测试Excel文件

**操作步骤**:
1. 调用 Python 脚本生成测试Excel文件
2. 将文件写入 FTP 上传目录

**文件规格**:
- 文件名: `test_flow_e2e_YYYYMMDD.xlsx`
- 内容: 符合报表配置的字段映射
- 行数: 10行测试数据

**FTP目录**:
- 内置FTP: `/data/ftp-root/upload/`
- 外部FTP: 配置的扫描路径

**验证点**:
- 文件已写入 FTP 目录
- 文件名匹配报表配置的文件匹配规则

---

### 3.4 阶段4: 报表配置管理

**操作步骤**:
1. `playwright_click(报表列表菜单)`
2. `playwright_click(新增报表配置)`
3. `playwright_fill(报表编码, "TEST_FLOW_E2E")`
4. `playwright_fill(报表名称, "E2E测试报表")`
5. `playwright_fill(输出表名, "t_e2e_flow_test")`
6. `playwright_select(选择FTP配置, "E2E测试FTP")`
7. `playwright_fill(文件匹配, "test_flow_e2e*.xlsx")`
8. 设置字段映射:
   - A列 → col_a (STRING)
   - B列 → col_b (INTEGER)
   - C列 → col_c (DECIMAL)
9. `playwright_click(保存配置)`
10. `playwright_expect_response("**/api/report/config**")`

**验证点**:
- 配置创建成功
- 配置出现在报表列表中

---

### 3.5 阶段5: 触发任务执行

**方式A - 手动触发**:

**操作步骤**:
1. `playwright_click(任务监控菜单)`
2. `playwright_click(手动触发按钮)`
3. `playwright_select(选择报表配置, "TEST_FLOW_E2E")`
4. `playwright_fill(文件名, "test_flow_e2e_YYYYMMDD.xlsx")`
5. `playwright_click(执行按钮)`
6. `playwright_expect_response("**/api/task/trigger**")`

**方式B - 自动扫描**:

**操作步骤**:
1. 等待定时任务扫描或手动刷新页面
2. `playwright_click(刷新按钮)`
3. 观察任务列表变化

**验证点**:
- 任务状态变为 "SUCCESS"
- 显示处理行数 > 0

---

### 3.6 阶段6: 数据验证

**操作步骤**:
1. `playwright_click(数据管理菜单)`
2. `playwright_select(选择表, "t_e2e_flow_test")`
3. `playwright_click(查询按钮)`
4. `playwright_expect_response("**/api/data/query**")`

**验证点**:
- 数据条数 > 0
- 数据内容正确
- `pt_dt` 字段已填充当前日期

---

### 3.7 阶段7: 日志验证

**操作步骤**:
1. `playwright_click(日志菜单)`
2. `playwright_click(任务日志Tab)`
3. 查找刚才执行的任务记录

**验证点**:
- 日志显示 "处理成功" 或 "SUCCESS"
- 记录数、耗时等信息正确

---

## 4. 测试数据生成模块

### 4.1 Excel文件生成脚本

```python
# e2e-tests/generate_test_data.py
import openpyxl
from datetime import datetime
import os

def create_e2e_excel(filename, rows=10):
    """生成E2E测试用Excel文件"""
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Sheet1"

    # 表头 (根据字段映射)
    headers = ["col_a", "col_b", "col_c"]
    ws.append(headers)

    # 数据行
    for i in range(rows):
        ws.append([
            f"测试数据_{i+1}",
            i + 1,
            round((i + 1) * 1.5, 2)
        ])

    # 确保目录存在
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    wb.save(filename)
    print(f"测试文件已生成: {filename}")

if __name__ == "__main__":
    date_str = datetime.now().strftime("%Y%m%d")
    filename = f"/data/ftp-root/upload/test_flow_e2e_{date_str}.xlsx"
    create_e2e_excel(filename, rows=10)
```

### 4.2 文件存放位置

| 文件 | 路径 |
|-----|------|
| 生成脚本 | `e2e-tests/generate_test_data.py` |
| 测试Excel | `/data/ftp-root/upload/test_flow_e2e_YYYYMMDD.xlsx` |

---

## 5. AI 驱动执行要点

### 5.1 动态决策

- AI 根据页面反馈决定下一步操作
- 遇到错误提示时，智能处理（如重试、跳过、截图）
- 根据实际页面元素定位策略

### 5.2 智能等待

- 使用 `playwright_expect_response` 等待异步操作完成
- 页面元素加载等待策略
- API 响应超时处理

### 5.3 证据留存

- 每个阶段完成后截图
- 捕获 `playwright_console_logs` 用于调试
- 保存操作序列日志

---

## 6. 验证矩阵

| 阶段 | 验证点 | 成功标准 |
|-----|-------|---------|
| TC-01 登录 | 页面跳转 | URL 包含 /ftp |
| TC-02 FTP配置 | 配置出现 | 配置名称在列表中 |
| TC-03 Excel文件 | 文件生成 | 文件存在于FTP目录 |
| TC-04 报表配置 | 配置创建 | 配置保存成功 |
| TC-05 任务执行 | 任务状态 | 状态为 SUCCESS |
| TC-06 数据验证 | 数据入库 | 数据条数 > 0 |
| TC-07 日志验证 | 日志记录 | 包含成功信息 |

---

## 7. 测试输出

| 输出物 | 说明 |
|-------|------|
| 截图 | 每个阶段的页面截图，存放在 `e2e-tests/screenshots/` |
| 操作日志 | AI 执行的操作序列记录 |
| 测试报告 | 汇总各阶段执行结果 |

---

## 8. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-04-11 | V1.0 | 初始创建 Playwright MCP E2E 测试设计方案 | AI Assistant |
