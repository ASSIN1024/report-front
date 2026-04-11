# Playwright MCP E2E 测试执行指南

## 前置条件检查

### 1. 服务状态

```bash
# 检查后端服务 (应在 8082 端口运行)
curl -s http://localhost:8082/api/health

# 检查前端服务 (应在 8083 端口运行)
curl -s http://localhost:8083 | head -5

# 检查FTP目录
ls -la /home/nova/projects/report-front/data/ftp-root/upload/
```

### 2. 启动服务（如未运行）

```bash
cd /home/nova/projects/report-front
./scripts/start.sh all
# 或只启动前端
./scripts/start.sh frontend
```

---

## Playwright MCP AI 驱动测试流程

### 阶段1: 登录系统

```
操作:
1. playwright_navigate("http://localhost:8083/#/login")
2. 等待页面加载完成 (networkidle)
3. playwright_fill('input[placeholder="请输入用户名"]', "admin")
4. playwright_fill('input[placeholder="请输入密码"]', "admin123")
5. playwright_click('button:has-text("登 录")')
6. playwright_expect_response("**/api/auth/login**")

验证: URL 跳转到 /ftp
```

### 阶段2: FTP配置管理

**重要**: Element UI 对话框需要使用 `locator()` 精确操作

```
操作:
1. 点击"新增配置"按钮
2. 等待对话框出现: .el-dialog__body
3. 在对话框内填写表单:
   dialog.locator('input[placeholder="请输入配置名称"]').click()
   dialog.locator('input[placeholder="请输入配置名称"]').fill("E2E测试FTP")
   dialog.locator('input[placeholder="请输入FTP服务器地址"]').click()
   dialog.locator('input[placeholder="请输入FTP服务器地址"]').fill("localhost")
   dialog.locator('input[placeholder="请输入用户名"]').click()
   dialog.locator('input[placeholder="请输入用户名"]').fill("test")
   dialog.locator('input[placeholder="请输入密码"]').click()
   dialog.locator('input[placeholder="请输入密码"]').fill("test123")
   dialog.locator('input[placeholder="请输入扫描路径"]').click()
   dialog.locator('input[placeholder="请输入扫描路径"]').fill("/upload")
   dialog.locator('input[placeholder="如: *.xlsx"]').click()
   dialog.locator('input[placeholder="如: *.xlsx"]').fill("test_flow_e2e*.xlsx")
4. 点击确定: dialog.locator('button:has-text("确定")').click()
5. 等待对话框关闭

验证: 配置出现在FTP配置列表中，表格包含" E2E测试FTP"
```

### 阶段3: 报表配置管理

```
操作:
1. 点击"报表列表"菜单
2. 点击"新增报表配置"按钮
3. 填写表单:
   - 报表编码: "TEST_FLOW_E2E"
   - 报表名称: "E2E测试报表"
   - 输出表名: "t_e2e_flow_test"
   - FTP配置: 选择刚才创建的 "E2E测试FTP"
   - 文件匹配: "test_flow_e2e*.xlsx"
4. 设置字段映射:
   - A列 → col_a (STRING)
   - B列 → col_b (INTEGER)
   - C列 → col_c (DECIMAL)
5. 点击"保存配置"

验证: 配置创建成功
```

### 阶段4: 生成测试文件

```bash
# 已预生成测试文件
ls -la /home/nova/projects/report-front/data/ftp-root/upload/test_flow_e2e*.xlsx

# 如需重新生成
python3 /home/nova/projects/report-front/e2e-tests/generate_test_data.py
```

### 阶段5: 触发任务执行

```
方式A - 手动触发:
1. 点击"任务监控"菜单
2. 点击"手动触发"按钮
3. 选择报表配置: "TEST_FLOW_E2E"
4. 输入文件名: test_flow_e2e_YYYYMMDD_HHMMSS.xlsx
5. 点击"执行"按钮
6. playwright_expect_response("**/api/task/trigger**")

方式B - 等待自动扫描:
1. 内置FTP扫描任务会每分钟执行
2. 扫描到文件后自动处理

验证: 任务状态变为 SUCCESS
```

### 阶段6: 数据验证

```
操作:
1. 点击"数据管理"菜单
2. 选择数据表: "t_e2e_flow_test"
3. 点击"查询"按钮

验证:
- 数据条数 > 0
- 数据内容正确
- pt_dt 字段已填充
```

### 阶段7: 日志验证

```
操作:
1. 点击"日志"菜单
2. 选择"任务日志" Tab
3. 查找刚才执行的任务记录

验证:
- 日志显示处理成功
- 记录数、耗时等信息正确
```

---

## 测试文件

- **生成脚本**: `e2e-tests/generate_test_data.py`
- **测试Excel**: `/home/nova/projects/report-front/data/ftp-root/upload/test_flow_e2e_*.xlsx`
- **截图目录**: `e2e-tests/screenshots/`
- **报告目录**: `e2e-tests/reports/`

---

## 注意事项

1. **登录凭证**: 请使用实际的 admin 用户密码
2. **FTP端口**: 内置FTP默认端口为 2121
3. **文件名**: 测试文件名为 `test_flow_e2e_YYYYMMDD_HHMMSS.xlsx`
4. **等待时间**: 自动扫描间隔为 60 秒（可配置）
