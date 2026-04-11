#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
E2E 完整流程测试脚本 v3
报表数据处理平台 - 端到端自动化测试
"""

import sys
import os
from datetime import datetime
from playwright.sync_api import sync_playwright

sys.path.insert(0, '/home/nova/projects/report-front/e2e-tests')

SCREENSHOT_DIR = '/home/nova/projects/report-front/e2e-tests/screenshots'
REPORT_DIR = '/home/nova/projects/report-front/e2e-tests/reports'
os.makedirs(SCREENSHOT_DIR, exist_ok=True)
os.makedirs(REPORT_DIR, exist_ok=True)

class E2ETestRunner:
    def __init__(self):
        self.results = []
        self.browser = None
        self.page = None
        self.base_url = "http://localhost:8083"
        self.test_start_time = datetime.now()
        self.ftp_config_name = f"E2E_FTP_{datetime.now().strftime('%H%M%S')}"
        self.report_code = f"TEST_E2E_{datetime.now().strftime('%H%M')}"
        self.report_config_name = f"E2E测试报表"
        self.output_table = f"t_e2e_test_{datetime.now().strftime('%H%M%S')}"
        self.test_file_name = None

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

    def navigate_to(self, path):
        """导航到指定页面"""
        self.page.goto(f"{self.base_url}/#/{path}")
        self.page.wait_for_load_state("networkidle")
        self.page.wait_for_timeout(500)

    def run(self):
        self.log("=" * 60)
        self.log("E2E 完整流程测试开始 (v3)")
        self.log("=" * 60)

        with sync_playwright() as p:
            self.browser = p.chromium.launch(headless=True)
            self.page = self.browser.new_page(viewport={'width': 1920, 'height': 1080})

            try:
                # 阶段1: 登录
                self.test_login()

                # 阶段2: FTP配置管理
                self.test_ftp_config()

                # 阶段3: 生成测试文件
                self.generate_test_file()

                # 阶段4: 报表配置管理
                self.test_report_config()

                # 阶段5: 立即扫描触发
                self.test_scan_trigger()

                # 阶段6: 数据验证
                self.test_data_validation()

                # 阶段7: 日志验证
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
            self.navigate_to("login")
            self.save_screenshot("01_login_page")

            self.page.fill('input[placeholder="请输入用户名"]', 'admin')
            self.page.fill('input[placeholder="请输入密码"]', 'admin123')
            self.save_screenshot("02_login_filled")

            self.page.click('button:has-text("登 录")')
            self.page.wait_for_url("**/ftp", timeout=10000)
            self.save_screenshot("03_login_success")

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

            # 点击新增配置
            self.page.click('button:has-text("新增配置")')
            self.page.wait_for_selector('.el-dialog__body', state='visible', timeout=5000)
            self.page.wait_for_timeout(500)
            self.save_screenshot("05_ftp_dialog")

            # 填写表单
            dialog = self.page.locator('.el-dialog')
            dialog.locator('input[placeholder="请输入配置名称"]').click()
            dialog.locator('input[placeholder="请输入配置名称"]').fill(self.ftp_config_name)

            dialog.locator('input[placeholder="请输入FTP服务器地址"]').click()
            dialog.locator('input[placeholder="请输入FTP服务器地址"]').fill('localhost')

            dialog.locator('input[placeholder="请输入用户名"]').click()
            dialog.locator('input[placeholder="请输入用户名"]').fill('test')

            dialog.locator('input[placeholder="请输入密码"]').click()
            dialog.locator('input[placeholder="请输入密码"]').fill('test123')

            dialog.locator('input[placeholder="请输入扫描路径"]').click()
            dialog.locator('input[placeholder="请输入扫描路径"]').fill('/upload')

            dialog.locator('input[placeholder="如: *.xlsx"]').click()
            dialog.locator('input[placeholder="如: *.xlsx"]').fill('test_flow_e2e*.xlsx')

            self.save_screenshot("06_ftp_form_filled")

            # 保存
            dialog.locator('button:has-text("确定")').click()
            self.page.wait_for_timeout(2000)
            self.save_screenshot("07_ftp_save_result")

            # 验证
            table_text = self.page.locator('.el-table').inner_text()
            if self.ftp_config_name in table_text:
                self.record_result("FTP配置", "PASS", f"配置 '{self.ftp_config_name}' 创建成功")
                self.log(f"✅ FTP配置 '{self.ftp_config_name}' 创建成功")
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

    def test_report_config(self):
        self.log("-" * 40)
        self.log("阶段4: 报表配置管理")
        self.log("-" * 40)

        try:
            # 导航到报表列表
            self.navigate_to("report")
            self.save_screenshot("08_report_page")

            # 点击新增报表
            self.page.click('button:has-text("新增报表")')
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("09_report_config_page")

            # 等待页面加载
            self.page.wait_for_selector('input[placeholder="如: SALES_REPORT"]', timeout=10000)
            self.save_screenshot("10_report_form")

            # 填写报表编码 - 使用 label 定位
            self.page.locator('input[placeholder="如: SALES_REPORT"]').fill(self.report_code)
            self.log(f"✅ 已填写报表编码: {self.report_code}")

            # 填写报表名称
            self.page.locator('input[placeholder="如: 销售报表"]').fill(self.report_config_name)
            self.log("✅ 已填写报表名称")

            # 选择FTP配置 - 找关联FTP那行的select
            # 先找到包含"关联FTP"文本的el-form-item，然后找其下的el-select
            ftp_select = self.page.locator('label:has-text("关联FTP")').locator('..').locator('.el-select')
            ftp_select.click()
            self.page.wait_for_timeout(500)
            # 选择包含ftp_config_name的选项
            self.page.locator(f'.el-select-dropdown__item:has-text("{self.ftp_config_name}")').click()
            self.log("✅ 已选择FTP配置")

            # 填写文件匹配
            self.page.locator('input[placeholder="如: sales_*.xlsx"]').fill('test_flow_e2e*.xlsx')
            self.log("✅ 已填写文件匹配")

            self.save_screenshot("11_report_form_filled")

            # 添加列映射 - 找列映射配置区域
            self.page.click('button:has-text("+ 添加映射")')
            self.page.wait_for_timeout(300)

            # 填写第一个映射
            mapping_rows = self.page.locator('.el-table').nth(1).locator('tbody tr')
            first_row = mapping_rows.first

            first_row.locator('input[placeholder="如: A"]').fill('A')
            first_row.locator('input[placeholder="如: order_id"]').fill('col_a')
            first_row.locator('.el-select').click()
            self.page.wait_for_timeout(300)
            self.page.locator('.el-select-dropdown__item:has-text("字符串")').click()

            # 添加第二个映射
            self.page.click('button:has-text("+ 添加映射")')
            self.page.wait_for_timeout(300)
            rows = self.page.locator('.el-table').nth(1).locator('tbody tr')
            second_row = rows.nth(1)

            second_row.locator('input[placeholder="如: A"]').fill('B')
            second_row.locator('input[placeholder="如: order_id"]').fill('col_b')
            second_row.locator('.el-select').click()
            self.page.wait_for_timeout(300)
            self.page.locator('.el-select-dropdown__item:has-text("整数")').click()

            # 添加第三个映射
            self.page.click('button:has-text("+ 添加映射")')
            self.page.wait_for_timeout(300)
            rows = self.page.locator('.el-table').nth(1).locator('tbody tr')
            third_row = rows.nth(2)

            third_row.locator('input[placeholder="如: A"]').fill('C')
            third_row.locator('input[placeholder="如: order_id"]').fill('col_c')
            third_row.locator('.el-select').click()
            self.page.wait_for_timeout(300)
            self.page.locator('.el-select-dropdown__item:has-text("小数")').click()

            self.save_screenshot("12_report_mappings")

            # 点击保存
            self.page.click('button:has-text("保存")')
            self.page.wait_for_timeout(2000)
            self.save_screenshot("13_report_save")

            # 验证是否返回列表
            self.page.wait_for_url("**/report", timeout=5000)
            self.save_screenshot("14_report_list")

            # 检查报表是否在列表中
            table_text = self.page.locator('.el-table').inner_text()
            if self.report_code in table_text:
                self.record_result("报表配置", "PASS", f"报表配置 '{self.report_code}' 创建成功")
                self.log(f"✅ 报表配置 '{self.report_code}' 创建成功")
            else:
                self.record_result("报表配置", "FAIL", "报表配置未出现在列表中")
                self.log("❌ 报表配置创建失败", "ERROR")

        except Exception as e:
            self.save_screenshot("04_report_error")
            self.record_result("报表配置", "FAIL", f"报表配置异常: {str(e)}")
            self.log(f"❌ 报表配置异常: {str(e)}", "ERROR")
            import traceback
            traceback.print_exc()

    def test_scan_trigger(self):
        self.log("-" * 40)
        self.log("阶段5: 立即扫描触发")
        self.log("-" * 40)

        try:
            # 等待页面加载
            self.page.wait_for_selector('.el-table')
            self.save_screenshot("15_task_scan")

            # 检查是否有"立即扫描"按钮
            scan_buttons = self.page.locator('button:has-text("立即扫描")')
            count = scan_buttons.count()

            if count > 0:
                scan_buttons.first.click()
                self.page.wait_for_timeout(2000)
                self.save_screenshot("16_scan_result")

                # 检查确认对话框
                confirm_visible = self.page.is_visible('.el-message-box')
                if confirm_visible:
                    self.page.click('.el-message-box__wrapper button:has-text("确认扫描")')
                    self.page.wait_for_timeout(3000)
                    self.save_screenshot("17_scan_confirmed")

                self.record_result("扫描触发", "PASS", "扫描任务已触发")
                self.log("✅ 扫描任务已触发")
            else:
                self.record_result("扫描触发", "FAIL", "立即扫描按钮不可见")
                self.log("❌ 立即扫描按钮不可见", "ERROR")

        except Exception as e:
            self.save_screenshot("05_scan_error")
            self.record_result("扫描触发", "FAIL", f"扫描触发异常: {str(e)}")
            self.log(f"❌ 扫描触发异常: {str(e)}", "ERROR")

    def test_data_validation(self):
        self.log("-" * 40)
        self.log("阶段6: 数据验证")
        self.log("-" * 40)

        try:
            # 导航到数据管理页面
            self.navigate_to("data-center")
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("18_data_page")

            # 等待表列表加载
            self.page.wait_for_selector('.el-select', timeout=10000)
            self.page.wait_for_timeout(1000)

            # 选择数据表 - 使用第一个select（目标表）
            selects = self.page.locator('.el-select')
            selects.first.click()
            self.page.wait_for_timeout(1000)

            # 查找包含 t_e2e 的表
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

            # 点击查询
            self.page.click('button:has-text("查询")')
            self.page.wait_for_timeout(2000)
            self.save_screenshot("19_data_result")

            # 检查数据
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
            # 导航到日志页面
            self.navigate_to("log")
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("20_log_page")

            # 检查是否有日志记录
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
