#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
E2E 完整流程测试脚本
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
        self.ftp_config_name = f"E2E测试FTP_{datetime.now().strftime('%H%M%S')}"
        self.report_config_name = f"E2E测试报表_{datetime.now().strftime('%H%M%S')}"
        self.output_table = f"t_e2e_flow_test_{datetime.now().strftime('%H%M%S')}"
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

    def run(self):
        self.log("=" * 60)
        self.log("E2E 完整流程测试开始")
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

                # 阶段5: 任务触发
                self.test_task_trigger()

                # 阶段6: 数据验证
                self.test_data_validation()

                # 阶段7: 日志验证
                self.test_log_validation()

            except Exception as e:
                self.log(f"测试异常: {str(e)}", "ERROR")
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
            self.page.goto(f"{self.base_url}/#/login")
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("01_login_page")

            self.page.fill('input[placeholder="请输入用户名"]', 'admin')
            self.page.fill('input[placeholder="请输入密码"]', 'admin123')
            self.save_screenshot("02_login_filled")

            self.page.click('button:has-text("登 录")')
            self.page.wait_for_url("**/ftp", timeout=10000)
            self.save_screenshot("03_login_success")

            self.page.wait_for_selector('.page-title:has-text("FTP配置管理")', timeout=10000)
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
            # 导航到报表配置页面
            self.page.click('text=报表')
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("08_report_page")

            # 点击新增报表配置
            self.page.click('button:has-text("新增报表")')
            self.page.wait_for_selector('.el-dialog__body', state='visible', timeout=5000)
            self.page.wait_for_timeout(500)
            self.save_screenshot("09_report_dialog")

            # 填写报表配置表单
            dialog = self.page.locator('.el-dialog')

            dialog.locator('input[placeholder="请输入报表编码"]').click()
            dialog.locator('input[placeholder="请输入报表编码"]').fill('TEST_FLOW_E2E')

            dialog.locator('input[placeholder="请输入报表名称"]').click()
            dialog.locator('input[placeholder="请输入报表名称"]').fill(self.report_config_name)

            dialog.locator('input[placeholder="请输入目标表名"]').click()
            dialog.locator('input[placeholder="请输入目标表名"]').fill(self.output_table)

            self.save_screenshot("10_report_form_filled")

            # 点击保存
            dialog.locator('button:has-text("确定")').click()
            self.page.wait_for_timeout(2000)
            self.save_screenshot("11_report_save_result")

            # 验证
            table_text = self.page.locator('.el-table').inner_text()
            if 'TEST_FLOW_E2E' in table_text:
                self.record_result("报表配置", "PASS", "报表配置创建成功")
                self.log("✅ 报表配置创建成功")
            else:
                self.record_result("报表配置", "FAIL", "报表配置未出现在列表中")
                self.log("❌ 报表配置创建失败", "ERROR")

        except Exception as e:
            self.save_screenshot("04_report_error")
            self.record_result("报表配置", "FAIL", f"报表配置异常: {str(e)}")
            self.log(f"❌ 报表配置异常: {str(e)}", "ERROR")

    def test_task_trigger(self):
        self.log("-" * 40)
        self.log("阶段5: 任务触发执行")
        self.log("-" * 40)

        try:
            # 导航到任务监控页面
            self.page.click('text=任务监控')
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("12_task_page")

            # 点击手动触发按钮
            self.page.click('button:has-text("手动触发")')
            self.page.wait_for_selector('.el-dialog__body', state='visible', timeout=5000)
            self.page.wait_for_timeout(500)
            self.save_screenshot("13_task_dialog")

            # 选择报表配置
            dialog = self.page.locator('.el-dialog')
            dialog.locator('.el-select').click()
            self.page.wait_for_timeout(500)
            self.page.locator('.el-select-dropdown__item:has-text("TEST_FLOW_E2E")').click()

            # 填写文件名
            dialog.locator('input[placeholder="请输入待处理文件名"]').click()
            dialog.locator('input[placeholder="请输入待处理文件名"]').fill(self.test_file_name)

            self.save_screenshot("14_task_form_filled")

            # 点击执行
            dialog.locator('button:has-text("执行")').click()
            self.page.wait_for_timeout(3000)
            self.save_screenshot("15_task_result")

            # 检查任务状态
            success_visible = self.page.is_visible('.el-message--success')
            if success_visible:
                self.record_result("任务触发", "PASS", "任务触发成功")
                self.log("✅ 任务触发成功")
            else:
                self.record_result("任务触发", "FAIL", "任务触发后未显示成功消息")
                self.log("⚠️ 任务触发结果未知")

        except Exception as e:
            self.save_screenshot("05_task_error")
            self.record_result("任务触发", "FAIL", f"任务触发异常: {str(e)}")
            self.log(f"❌ 任务触发异常: {str(e)}", "ERROR")

    def test_data_validation(self):
        self.log("-" * 40)
        self.log("阶段6: 数据验证")
        self.log("-" * 40)

        try:
            # 导航到数据管理页面
            self.page.click('text=数据管理')
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("16_data_page")

            # 选择数据表
            self.page.locator('.el-select').click()
            self.page.wait_for_timeout(500)

            # 查找并选择刚才创建的表
            options = self.page.locator('.el-select-dropdown__item').all()
            table_found = False
            for option in options:
                text = option.inner_text()
                if self.output_table in text:
                    option.click()
                    table_found = True
                    break

            if not table_found:
                self.record_result("数据验证", "FAIL", f"未找到数据表: {self.output_table}")
                self.log(f"❌ 未找到数据表: {self.output_table}", "ERROR")
                return

            # 点击查询
            self.page.click('button:has-text("查询")')
            self.page.wait_for_timeout(2000)
            self.save_screenshot("17_data_result")

            # 检查数据
            table = self.page.locator('.el-table')
            if table.is_visible():
                table_text = table.inner_text()
                if '测试数据' in table_text or len(table_text) > 50:
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
            self.page.click('text=日志')
            self.page.wait_for_load_state("networkidle")
            self.save_screenshot("18_log_page")

            # 选择任务日志Tab
            self.page.click('.el-tabs__item:has-text("任务日志")')
            self.page.wait_for_timeout(1000)
            self.save_screenshot("19_log_task")

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

| 阶段 | 状态 | 消息 | 截图 |
|-----|------|------|------|
"""

        for r in self.results:
            screenshot_name = os.path.basename(r['screenshot']) if r['screenshot'] else '无'
            report += f"| {r['stage']} | {r['status']} | {r['message']} | {screenshot_name} |\n"

        report += f"""
## 测试配置信息

| 配置项 | 值 |
|-------|-----|
| FTP配置名称 | {self.ftp_config_name} |
| 报表配置名称 | {self.report_config_name} |
| 输出表名 | {self.output_table} |
| 测试文件名 | {self.test_file_name} |

## 截图清单

```
{SCREENSHOT_DIR}/
"""

        for r in self.results:
            if r['screenshot']:
                report += f"- {os.path.basename(r['screenshot'])}\n"

        report += f"""
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
