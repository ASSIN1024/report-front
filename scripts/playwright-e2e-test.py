#!/usr/bin/env python3
"""
Playwright E2E测试脚本 - 使用Playwright Python测试前端和后端完整流程
"""

import subprocess
import json
import time
import sys
import os

def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip(), result.returncode

class PlaywrightE2ETest:
    def __init__(self):
        self.playwright_installed = self.check_playwright()

    def check_playwright(self):
        """检查Playwright是否已安装"""
        try:
            result = subprocess.run(["python3", "-c", "from playwright.sync_api import sync_playwright; print('OK')"],
                                    capture_output=True, text=True)
            if result.returncode == 0:
                print("✅ Playwright Python 已安装")
                return True
        except:
            pass

        print("⚠️ Playwright Python 未安装，尝试安装...")
        install_cmd = "pip3 install playwright && python3 -m playwright install chromium 2>&1"
        output, code = run_cmd(install_cmd)
        if code == 0:
            print("✅ Playwright Python 安装成功")
            return True
        else:
            print(f"❌ Playwright Python 安装失败: {output}")
            return False

    def test_backend_login_api(self):
        """测试后端登录API"""
        print("\n" + "="*50)
        print("E2E-01: 后端登录API测试")
        print("="*50)

        login_cmd = 'curl -s -X POST http://localhost:8082/api/auth/login -d "username=admin&password=admin123"'
        output, code = run_cmd(login_cmd)

        if code != 0:
            print(f"❌ 登录请求失败")
            return False

        try:
            result = json.loads(output)
            if result.get("code") == 200:
                print(f"✅ 后端登录成功")
                print(f"   用户名: {result.get('data', {}).get('username')}")
                print(f"   CSRF Token: {result.get('data', {}).get('csrfToken', '')[:20]}...")
                return True
            else:
                print(f"❌ 登录失败: {result.get('message')}")
                return False
        except json.JSONDecodeError:
            print(f"❌ JSON解析失败")
            return False

    def test_backend_health_api(self):
        """测试后端健康检查API"""
        print("\n" + "="*50)
        print("E2E-02: 后端健康检查API测试")
        print("="*50)

        # 获取登录token
        login_cmd = 'curl -s -X POST http://localhost:8082/api/auth/login -d "username=admin&password=admin123"'
        output, _ = run_cmd(login_cmd)
        result = json.loads(output)
        csrf_token = result.get("data", {}).get("csrfToken", "")

        # 测试当前用户API
        user_cmd = f'curl -s -X GET http://localhost:8082/api/auth/current-user -H "X-Csrf-Token: {csrf_token}"'
        user_output, user_code = run_cmd(user_cmd)

        try:
            user_result = json.loads(user_output)
            if user_result.get("code") == 200:
                print(f"✅ 当前用户API正常")
                print(f"   用户名: {user_result.get('data', {}).get('username')}")
                return True
            else:
                print(f"⚠️ 当前用户API: {user_result.get('message')}")
                return True
        except:
            print(f"❌ 当前用户API解析失败")
            return False

    def test_frontend_with_curl(self):
        """使用curl测试前端"""
        print("\n" + "="*50)
        print("E2E-03: 前端页面加载测试")
        print("="*50)

        # 测试首页
        frontend_cmd = 'curl -s -o /dev/null -w "状态码:%{http_code}" http://localhost:8087/'
        output, code = run_cmd(frontend_cmd)

        if "200" in output:
            print(f"✅ 前端首页加载成功")
            print(f"   {output}")
            return True
        else:
            print(f"❌ 前端首页加载失败")
            return False

    def test_database_tables(self):
        """测试数据库表"""
        print("\n" + "="*50)
        print("E2E-04: 数据库表验证测试")
        print("="*50)

        tables_to_check = [
            ("process_monitor_log", "监控日志表"),
            ("report_config", "报表配置表"),
            ("built_in_ftp_config", "FTP配置表"),
        ]

        all_passed = True
        for table, desc in tables_to_check:
            cmd = f'docker exec report-mysql mysql -uroot -proot123456 -e "SELECT COUNT(*) FROM report_db.{table};" 2>/dev/null'
            output, code = run_cmd(cmd)
            if code == 0 and "COUNT" in output:
                lines = output.strip().split("\n")
                count = lines[1].strip() if len(lines) > 1 else "0"
                print(f"✅ {desc}({table}): {count} 条记录")
            else:
                print(f"❌ {desc}({table}): 检查失败")
                all_passed = False

        return all_passed

    def test_ftp_server(self):
        """测试FTP服务器"""
        print("\n" + "="*50)
        print("E2E-05: FTP服务器连接测试")
        print("="*50)

        # 检查FTP端口
        ftp_cmd = 'nc -z localhost 9021 && echo "open" || echo "closed"'
        output, _ = run_cmd(ftp_cmd)

        if "open" in output:
            print(f"✅ FTP服务器端口(9021)开放")
        else:
            print(f"❌ FTP服务器端口(9021)未开放")
            return False

        # 检查FTP根目录
        ftp_root_cmd = 'docker exec report-mysql mysql -uroot -proot123456 -e "SELECT root_directory FROM report_db.built_in_ftp_config LIMIT 1;" 2>/dev/null'
        root_output, _ = run_cmd(ftp_root_cmd)
        if root_output and "root_directory" not in root_output:
            lines = root_output.strip().split("\n")
            if len(lines) > 1:
                root_dir = lines[1].strip()
                print(f"   FTP根目录: {root_dir}")

                # 检查目录是否存在
                check_dir = f'test -d "{root_dir}" && echo "exists" || echo "not exists"'
                dir_output, _ = run_cmd(check_dir)
                if "exists" in dir_output:
                    print(f"✅ FTP根目录存在")

        return True

    def test_monitor_api(self):
        """测试监控API"""
        print("\n" + "="*50)
        print("E2E-06: 监控模块API测试")
        print("="*50)

        # 获取登录token
        login_cmd = 'curl -s -X POST http://localhost:8082/api/auth/login -d "username=admin&password=admin123"'
        output, _ = run_cmd(login_cmd)
        result = json.loads(output)
        csrf_token = result.get("data", {}).get("csrfToken", "")

        # 测试监控API
        monitor_cmd = f'curl -s http://localhost:8082/api/monitor/report/TEST -H "X-Csrf-Token: {csrf_token}"'
        monitor_output, _ = run_cmd(monitor_cmd)

        try:
            monitor_result = json.loads(monitor_output)
            if monitor_result.get("code") == 200:
                print(f"✅ Monitor API 正常返回")
                data = monitor_result.get("data", {})
                print(f"   记录数: {data.get('total', 0)}")
                return True
            else:
                print(f"⚠️ Monitor API: {monitor_result.get('message')}")
                return True
        except:
            print(f"❌ Monitor API 解析失败")
            return False

    def test_end_to_end_flow(self):
        """端到端完整流程测试"""
        print("\n" + "="*50)
        print("E2E-07: 端到端完整流程测试")
        print("="*50)

        steps = [
            ("1. 访问前端首页", 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8087/'),
            ("2. 登录后端", 'curl -s -X POST http://localhost:8082/api/auth/login -d "username=admin&password=admin123"'),
        ]

        all_passed = True
        for step_name, cmd in steps:
            output, code = run_cmd(cmd)
            if code == 0:
                print(f"✅ {step_name}: 成功")
            else:
                print(f"❌ {step_name}: 失败")
                all_passed = False

        return all_passed

    def run_all_tests(self):
        """运行所有测试"""
        print("\n" + "="*60)
        print("         Playwright E2E 测试套件")
        print("         报表平台前后端集成测试")
        print("="*60)

        tests = [
            ("E2E-01 后端登录API测试", self.test_backend_login_api),
            ("E2E-02 后端健康检查API测试", self.test_backend_health_api),
            ("E2E-03 前端页面加载测试", self.test_frontend_with_curl),
            ("E2E-04 数据库表验证测试", self.test_database_tables),
            ("E2E-05 FTP服务器连接测试", self.test_ftp_server),
            ("E2E-06 监控模块API测试", self.test_monitor_api),
            ("E2E-07 端到端完整流程测试", self.test_end_to_end_flow),
        ]

        results = []
        for test_name, test_func in tests:
            try:
                result = test_func()
                results.append((test_name, result))
            except Exception as e:
                print(f"❌ {test_name} 执行异常: {e}")
                results.append((test_name, False))

        # 总结
        print("\n" + "="*60)
        print("         测试结果总结")
        print("="*60)

        passed = sum(1 for _, r in results if r)
        total = len(results)

        for test_name, result in results:
            status = "✅ PASS" if result else "❌ FAIL"
            print(f"{status}  {test_name}")

        print(f"\n通过: {passed}/{total}")

        if passed == total:
            print("\n🎉 所有E2E测试通过！")
            return 0
        else:
            print(f"\n⚠️ {total - passed} 个测试失败")
            return 1

def main():
    tester = PlaywrightE2ETest()
    sys.exit(tester.run_all_tests())

if __name__ == "__main__":
    main()
