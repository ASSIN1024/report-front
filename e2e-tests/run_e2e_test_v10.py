#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
E2E 完整流程测试脚本 v10
通过 API 创建报表配置（绕过 Element UI 表格的限制）
"""

import sys
import os
import json
import requests
from datetime import datetime
from playwright.sync_api import sync_playwright

sys.path.insert(0, '/home/nova/projects/report-front/e2e-tests')

SCREENSHOT_DIR = '/home/nova/projects/report-front/e2e-tests/screenshots'
REPORT_DIR = '/home/nova/projects/report-front/e2e-tests/reports'
API_BASE = "http://localhost:8082/api"
os.makedirs(SCREENSHOT_DIR, exist_ok=True)
os.makedirs(REPORT_DIR, exist_ok=True)

class E2ETestRunner:
    def __init__(self):
        self.results = []
        self.browser = None
        self.page = None
        self.base_url = "http://localhost:8083"
        self.test_start_time = datetime.now()
        self.ftp_config_id = None
        self.report_config_id = None
        self.ftp_config_name = f"E2E_FTP_{datetime.now().strftime('%H%M%S')}"
        self.report_code = f"TEST_E2E_{datetime.now().strftime('%H%M')}"
        self.report_config_name = f"E2E测试报表"
        self.output_table = f"t_e2e_test_{datetime.now().strftime('%H%M%S')}"
        self.test_file_name = None
        self.session = requests.Session()
        self.session.headers.update({'Content-Type': 'application/json'})

    def log(self, message, level="INFO"):
        timestamp = datetime.now().strftime("%H:%M:%S")
        print(f"[{timestamp}] [{level}] {message}")

    def save_screenshot(self, name):
        filename = f"{SCREENSHOT_DIR}/{name}_{datetime.now().strftime('%H%M%S')}.png"
        self.page.screenshot(path=filename)
        self.log(f"📸 截图保存: {name}")
        return filename

    def record_result(self, stage, status, message, screenshot=None):
        self.results.append({
            "stage": stage,
            "status": status,
            "message": message,
            "screenshot": screenshot,
            "timestamp": datetime.now().strftime("%H:%M:%S")
        })

    def api_post(self, path, data):
        """发送 POST 请求到 API"""
        url = f"{API_BASE}{path}"
        try:
            # 获取并设置CSRF token
            csrf_token = self.get_csrf_token()
            if csrf_token:
                self.session.headers.update({'X-CSRF-Token': csrf_token})
            response = self.session.post(url, json=data, timeout=10)
            return response.status_code, response.json() if response.text else {}
        except Exception as e:
            self.log(f"API POST 请求失败: {str(e)}", "ERROR")
            return 500, {"message": str(e)}

    def api_get(self, path):
        """发送 GET 请求到 API"""
        url = f"{API_BASE}{path}"
        try:
            # 获取并设置CSRF token
            csrf_token = self.get_csrf_token()
            if csrf_token:
                self.session.headers.update({'X-CSRF-Token': csrf_token})
            response = self.session.get(url, timeout=10)
            return response.status_code, response.json() if response.text else {}
        except Exception as e:
            self.log(f"API GET 请求失败: {str(e)}", "ERROR")
            return 500, {"message": str(e)}

    def get_csrf_token(self):
        """通过API获取CSRF token (使用直接请求避免递归)"""
        try:
            url = f"{API_BASE}/auth/csrf-token"
            response = self.session.get(url, timeout=10)
            code = response.status_code
            data = response.json() if response.text else {}
            if code == 200 and data.get("code") in [0, 200]:
                return data.get("data")
            self.log(f"获取CSRF Token失败: code={code}, data={data}", "ERROR")
            return None
        except Exception as e:
            self.log(f"获取CSRF Token失败: {str(e)}", "ERROR")
            return None

    def navigate_to(self, path):
        self.page.goto(f"{self.base_url}/#/{path}")
        self.page.wait_for_load_state("networkidle")
        self.page.wait_for_timeout(500)

    def run(self):
        self.log("=" * 60)
        self.log("E2E 完整流程测试开始 (v10)")
        self.log("=" * 60)

        with sync_playwright() as p:
            self.browser = p.chromium.launch(headless=True)
            self.page = self.browser.new_page(viewport={'width': 1920, 'height': 1080})

            try:
                self.test_login()
                self.test_ftp_config()
                self.generate_test_file()
                self.test_report_config_via_api()
                self.test_scan_trigger()
                self.test_data_validation()
                self.test_log_validation()

            except Exception as e:
                self.log(f"测试异常: {str(e)}", "ERROR")
                import traceback
                traceback.print_exc()
                self.save_screenshot("error_final")
            finally:
                self.browser.close()

        self.generate_report()
        self.log("=" * 60)
        self.log("E2E 测试完成")
        self.log("=" * 60)

    def test_login(self):
        self.log("-" * 40)
        self.log("阶段1: 登录系统")
        self.log("-" * 40)

        try:
            # UI 登录
            self.navigate_to("login")
            self.save_screenshot("01_login_page")

            self.page.fill('input[placeholder="请输入用户名"]', 'admin')
            self.page.fill('input[placeholder="请输入密码"]', 'admin123')
            self.save_screenshot("02_login_filled")

            self.page.click('button:has-text("登 录")')
            self.page.wait_for_url("**/ftp", timeout=10000)
            self.save_screenshot("03_login_success")

            # 获取 cookie 用于 API 请求
            cookies = self.browser.contexts[0].cookies()
            for cookie in cookies:
                self.session.cookies.set(cookie['name'], cookie['value'])

            self.record_result("登录", "PASS", "登录成功，跳转到FTP配置页面")
            self.log("✅ 登录成功")

        except Exception as e:
            self.save_screenshot("01_login_error")
            self.record_result("登录", "FAIL", f"登录失败: {str(e)}")
            self.log(f"❌ 登录失败: {str(e)}", "ERROR")

    def test_ftp_config(self):
        self.log("-" * 40)
        self.log("阶段2: FTP配置管理")
        self.log("-" * 40)

        try:
            self.save_screenshot("04_ftp_page")

            self.page.click('button:has-text("新增配置")')
            self.page.wait_for_selector('.el-dialog__body', state='visible', timeout=5000)
            self.page.wait_for_timeout(500)
            self.save_screenshot("05_ftp_dialog")

            dialog = self.page.locator('.el-dialog')
            dialog.locator('input[placeholder="请输入配置名称"]').fill(self.ftp_config_name)
            dialog.locator('input[placeholder="请输入FTP服务器地址"]').fill('localhost')
            dialog.locator('.el-input-number input').first.fill('9021')
            dialog.locator('input[placeholder="请输入用户名"]').fill('rpa_user')
            dialog.locator('input[placeholder="请输入密码"]').fill('rpa_password')
            dialog.locator('input[placeholder="请输入扫描路径"]').fill('/upload')
            dialog.locator('input[placeholder="如: *.xlsx"]').fill('test_flow_e2e*.xlsx')

            self.save_screenshot("06_ftp_form_filled")

            dialog.locator('button:has-text("确定")').click()
            self.page.wait_for_timeout(2000)
            self.save_screenshot("07_ftp_save_result")

            # 获取 FTP 配置 ID
            code, data = self.api_get("/ftp/config/page?pageNum=1&pageSize=100")
            self.log(f"FTP Config API Response: code={code}, data={data}")
            if code == 200 and data.get("code") in [0, 200]:
                configs = data.get("data", {}).get("records", [])
                for cfg in configs:
                    if cfg.get("configName") == self.ftp_config_name:
                        self.ftp_config_id = cfg.get("id")
                        self.log(f"✅ 找到 FTP 配置 ID: {self.ftp_config_id}")
                        break

            table_text = self.page.locator('.el-table').inner_text()
            if self.ftp_config_name in table_text:
                self.record_result("FTP配置", "PASS", f"配置 '{self.ftp_config_name}' 创建成功, ID: {self.ftp_config_id}")
                self.log(f"✅ FTP配置 '{self.ftp_config_name}' 创建成功, ID: {self.ftp_config_id}")
            else:
                self.record_result("FTP配置", "FAIL", "配置未出现在列表中")
                self.log("❌ FTP配置创建失败", "ERROR")

        except Exception as e:
            self.save_screenshot("02_ftp_error")
            self.record_result("FTP配置", "FAIL", f"FTP配置异常: {str(e)}")
            self.log(f"❌ FTP配置异常: {str(e)}", "ERROR")

    def generate_test_file(self):
        self.log("-" * 40)
        self.log("阶段3: 生成测试Excel文件")
        self.log("-" * 40)

        try:
            from generate_test_data import create_e2e_excel

            date_str = datetime.now().strftime("%Y%m%d")
            time_str = datetime.now().strftime("%H%M%S")
            self.test_file_name = f"test_flow_e2e_{date_str}_{time_str}.xlsx"
            filepath = f"/home/nova/projects/report-front/data/ftp-root/upload/{self.test_file_name}"

            create_e2e_excel(filepath, rows=10)
            self.record_result("文件生成", "PASS", f"测试文件已生成: {self.test_file_name}")
            self.log(f"✅ 测试文件已生成: {self.test_file_name}")

        except Exception as e:
            self.record_result("文件生成", "FAIL", f"文件生成异常: {str(e)}")
            self.log(f"❌ 文件生成异常: {str(e)}", "ERROR")

    def test_report_config_via_api(self):
        self.log("-" * 40)
        self.log("阶段4: 报表配置管理 (通过API)")
        self.log("-" * 40)

        try:
            if not self.ftp_config_id:
                self.record_result("报表配置", "FAIL", "FTP配置ID未找到")
                self.log("❌ FTP配置ID未找到", "ERROR")
                return

            # 通过 API 创建报表配置
            report_data = {
                "reportCode": self.report_code,
                "reportName": self.report_config_name,
                "ftpConfigId": self.ftp_config_id,
                "filePattern": "test_flow_e2e*.xlsx",
                "sheetIndex": 0,
                "headerRow": 0,
                "dataStartRow": 1,
                "columnMappings": [
                    {"excelColumn": "A", "fieldName": "col_a", "fieldType": "STRING", "dateFormat": "", "scale": None, "cleanRules": []},
                    {"excelColumn": "B", "fieldName": "col_b", "fieldType": "INTEGER", "dateFormat": "", "scale": 0, "cleanRules": []},
                    {"excelColumn": "C", "fieldName": "col_c", "fieldType": "DECIMAL", "dateFormat": "", "scale": 2, "cleanRules": []}
                ],
                "outputTable": self.output_table,
                "outputMode": "APPEND",
                "status": 1
            }

            code, response = self.api_post("/report/config", report_data)

            if code == 200 and response.get("code") == 200:
                self.record_result("报表配置", "PASS", f"报表配置 '{self.report_code}' 通过API创建成功")
                self.log(f"✅ 报表配置 '{self.report_code}' 通过API创建成功")

                self.log(f"尝试获取报表配置ID...")
                code, data = self.api_get("/report/config/page?pageNum=1&pageSize=100")
                self.log(f"报表配置列表API响应: code={code}, data类型={type(data)}")
                if code == 200 and data.get("code") in [0, 200]:
                    configs = data.get("data", {}).get("records", [])
                    self.log(f"找到 {len(configs)} 个报表配置")
                    for cfg in configs:
                        self.log(f"检查配置: {cfg.get('reportCode')} vs {self.report_code}")
                        if cfg.get("reportCode") == self.report_code:
                            self.report_config_id = cfg.get("id")
                            self.log(f"✅ 获取报表配置ID: {self.report_config_id}")
                            break
                    if not self.report_config_id:
                        self.log(f"⚠️ 未找到匹配的报表配置")
                else:
                    self.log(f"⚠️ 获取报表配置列表失败: code={code}, data={data}")
            else:
                self.record_result("报表配置", "FAIL", f"API返回错误: {response.get('message', 'Unknown error')}")
                self.log(f"❌ 报表配置API失败: {response.get('message', 'Unknown error')}", "ERROR")

            # 刷新页面验证
            self.navigate_to("report")
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("08_report_page")

            table_text = self.page.locator('.el-table').inner_text()
            if self.report_code in table_text:
                self.log("✅ 报表配置已在列表中显示")
            else:
                self.log("⚠️ 报表配置未在UI列表中显示（可能需要刷新）")

        except Exception as e:
            self.record_result("报表配置", "FAIL", f"报表配置异常: {str(e)}")
            self.log(f"❌ 报表配置异常: {str(e)}", "ERROR")

    def test_scan_trigger(self):
        self.log("-" * 40)
        self.log("阶段5: 立即扫描触发")
        self.log("-" * 40)

        try:
            self.navigate_to("report")
            self.page.wait_for_load_state("networkidle")
            self.page.wait_for_timeout(1000)
            self.save_screenshot("15_task_scan")

            if not self.report_config_id:
                self.log(f"⚠️ report_config_id未设置，尝试从API获取")
                code, data = self.api_get("/report/config/page?pageNum=1&pageSize=100")
                self.log(f"报表配置列表响应: code={code}, data={data}")
                if code == 200 and data.get("code") in [0, 200]:
                    configs = data.get("data", {}).get("records", [])
                    self.log(f"找到 {len(configs)} 个报表配置")
                    for cfg in configs:
                        self.log(f"检查配置: {cfg.get('reportCode')} vs {self.report_code}")
                        if cfg.get("reportCode") == self.report_code:
                            self.report_config_id = cfg.get("id")
                            self.log(f"✅ 获取报表配置ID: {self.report_config_id}")
                            break

            if self.report_config_id:
                self.log(f"✅ 通过API触发扫描: report_id={self.report_config_id}")
                code, response = self.api_post(f"/report/config/{self.report_config_id}/scan", {})
                self.log(f"API扫描响应: code={code}, response={response}")
                result = (code == 200 and response.get("code") == 200)
                self.log(f"扫描结果判断: code={code}, response_code={response.get('code')}, result={result}")
            else:
                self.log(f"❌ 无法获取report_config_id")
                result = False

            if result:
                self.page.wait_for_timeout(3000)
                self.save_screenshot("16_scan_result")
                self.record_result("扫描触发", "PASS", "扫描任务已触发")
                self.log("✅ 扫描任务已触发")
            else:
                self.record_result("扫描触发", "FAIL", "扫描触发失败")
                self.log("❌ 扫描触发失败", "ERROR")

        except Exception as e:
            self.save_screenshot("05_scan_error")
            self.record_result("扫描触发", "FAIL", f"扫描触发异常: {str(e)}")
            self.log(f"❌ 扫描触发异常: {str(e)}", "ERROR")

    def test_data_validation(self):
        self.log("-" * 40)
        self.log("阶段6: 数据验证")
        self.log("-" * 40)

        try:
            self.navigate_to("data-center")
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("18_data_page")

            self.page.wait_for_selector('.el-select', timeout=10000)
            self.page.wait_for_timeout(1000)

            self.page.locator('.el-select').first.click()
            self.page.wait_for_timeout(1000)

            options = self.page.locator('.el-select-dropdown__item').all()
            table_found = False
            for option in options:
                text = option.inner_text()
                if 't_e2e' in text.lower():
                    option.click()
                    table_found = True
                    self.log(f"✅ 已选择数据表: {text}")
                    break

            if not table_found:
                self.record_result("数据验证", "FAIL", "未找到E2E测试数据表")
                self.log("❌ 未找到E2E测试数据表", "ERROR")
                return

            self.page.click('button:has-text("查询")')
            self.page.wait_for_timeout(2000)
            self.save_screenshot("19_data_result")

            table = self.page.locator('.el-table')
            if table.is_visible():
                table_text = table.inner_text()
                if len(table_text) > 50:
                    self.record_result("数据验证", "PASS", "数据查询成功，表中有数据")
                    self.log("✅ 数据验证成功")
                else:
                    self.record_result("数据验证", "FAIL", "数据表中没有数据")
                    self.log("❌ 数据表中没有数据", "ERROR")
            else:
                self.record_result("数据验证", "FAIL", "数据表未显示")
                self.log("❌ 数据表未显示", "ERROR")

        except Exception as e:
            self.save_screenshot("06_data_error")
            self.record_result("数据验证", "FAIL", f"数据验证异常: {str(e)}")
            self.log(f"❌ 数据验证异常: {str(e)}", "ERROR")

    def test_log_validation(self):
        self.log("-" * 40)
        self.log("阶段7: 日志验证")
        self.log("-" * 40)

        try:
            self.navigate_to("log")
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("20_log_page")

            table = self.page.locator('.el-table')
            if table.is_visible():
                table_text = table.inner_text()
                if len(table_text) > 50:
                    self.record_result("日志验证", "PASS", "日志记录存在")
                    self.log("✅ 日志验证成功")
                else:
                    self.record_result("日志验证", "FAIL", "日志表为空")
                    self.log("❌ 日志表为空", "ERROR")
            else:
                self.record_result("日志验证", "FAIL", "日志表未显示")
                self.log("❌ 日志表未显示", "ERROR")

        except Exception as e:
            self.save_screenshot("07_log_error")
            self.record_result("日志验证", "FAIL", f"日志验证异常: {str(e)}")
            self.log(f"❌ 日志验证异常: {str(e)}", "ERROR")

    def generate_report(self):
        self.log("生成测试报告...")
        test_end_time = datetime.now()
        duration = test_end_time - self.test_start_time

        passed = sum(1 for r in self.results if r['status'] == 'PASS')
        failed = sum(1 for r in self.results if r['status'] == 'FAIL')

        report = f"""# E2E 端到端测试报告

## 测试概述

| 项目 | 内容 |
|-----|------|
| 测试日期 | {self.test_start_time.strftime('%Y-%m-%d %H:%M:%S')} |
| 测试耗时 | {duration.total_seconds():.2f} 秒 |
| 测试环境 | 后端: localhost:8082, 前端: localhost:8083 |
| 测试人员 | AI Assistant |

## 测试统计

| 指标 | 数值 |
|-----|------|
| 总测试阶段 | {len(self.results)} |
| 通过 | {passed} |
| 失败 | {failed} |
| 通过率 | {passed * 100 / len(self.results):.1f}% |

## 测试结果详情

| 阶段 | 状态 | 消息 | 时间 |
|-----|------|------|------|
"""

        for r in self.results:
            report += f"| {r['stage']} | {r['status']} | {r['message']} | {r['timestamp']} |\n"

        report += f"""
## 测试配置信息

| 配置项 | 值 |
|-------|-----|
| FTP配置名称 | {self.ftp_config_name} |
| FTP配置ID | {self.ftp_config_id} |
| 报表编码 | {self.report_code} |
| 报表配置名称 | {self.report_config_name} |
| 输出表名 | {self.output_table} |
| 测试文件名 | {self.test_file_name} |

## 发现的缺陷

"""

        for r in self.results:
            if r['status'] == 'FAIL':
                msg = r['message'].replace('\n', '<br>')
                report += f"- **{r['stage']}**: {msg}\n"

        report += f"""
## 技术说明

### Element UI 表格双向绑定问题

在测试过程中发现，Playwright 与 Element UI 的表格组件存在兼容性问题：

- **问题描述**: Element UI 的 `el-table` 内部单元格使用 Scoped Slots 实现，输入框的 Vue 双向绑定无法通过 Playwright 的 `fill()` 或 `evaluate()` 方法触发
- **影响**: 无法通过 UI 填写报表配置的列映射表格
- **解决方案**: 报表配置通过后端 API 直接创建，绕过 UI 层面的限制

### 相关技术栈
- Frontend: Vue 2.6 + Element UI 2
- Backend: Spring Boot 2.1.2
- Browser Automation: Playwright 1.50.0

## 截图清单

```
{SCREENSHOT_DIR}/
```

---

**报告生成时间**: {test_end_time.strftime('%Y-%m-%d %H:%M:%S')}
"""

        report_file = f"{REPORT_DIR}/e2e-test-report-{datetime.now().strftime('%Y%m%d_%H%M%S')}.md"
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)

        self.log(f"📄 测试报告已保存: {report_file}")
        print("\n" + report)


if __name__ == "__main__":
    runner = E2ETestRunner()
    runner.run()
