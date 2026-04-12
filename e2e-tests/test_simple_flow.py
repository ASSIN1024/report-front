#!/usr/bin/env python3
"""
简化版E2E测试 - 验证完整流程
"""
import requests
from playwright.sync_api import sync_playwright

API_BASE = "http://localhost:8082/api"
FRONTEND_BASE = "http://localhost:8083"

def main():
    session = requests.Session()
    
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        # 1. UI登录
        print("1. UI登录...")
        page.goto(f"{FRONTEND_BASE}/#/login")
        page.fill('input[placeholder="请输入用户名"]', 'admin')
        page.fill('input[placeholder="请输入密码"]', 'admin123')
        page.click('button:has-text("登 录")')
        page.wait_for_url("**/ftp", timeout=10000)
        print("   登录成功!")
        
        # 2. 获取浏览器cookie
        cookies = browser.contexts[0].cookies()
        for cookie in cookies:
            session.cookies.set(cookie['name'], cookie['value'])
        print(f"   获取到 {len(cookies)} 个cookie")
        
        browser.close()
    
    # 3. 获取CSRF token
    print("2. 获取CSRF token...")
    resp = session.get(f"{API_BASE}/auth/csrf-token")
    print(f"   CSRF响应: {resp.status_code} - {resp.json()}")
    csrf_token = resp.json().get('data')
    session.headers.update({'X-CSRF-Token': csrf_token})
    
    # 4. 创建FTP配置
    print("3. 创建FTP配置...")
    ftp_data = {
        "configName": "E2E_FTP_SIMPLE",
        "host": "localhost",
        "port": 9021,
        "username": "rpa_user",
        "password": "rpa_password",
        "scanPath": "/upload",
        "filePattern": "test_flow_e2e*.xlsx",
        "status": 1
    }
    resp = session.post(f"{API_BASE}/ftp/config", json=ftp_data)
    print(f"   FTP响应: {resp.status_code} - {resp.json()}")
    
    if resp.status_code != 200 or resp.json().get('code') != 200:
        print("   FTP配置创建失败!")
        return
    
    # 5. 获取FTP配置ID
    print("4. 获取FTP配置ID...")
    resp = session.get(f"{API_BASE}/ftp/config/page?pageNum=1&pageSize=100")
    ftp_id = None
    for cfg in resp.json().get('data', {}).get('records', []):
        if cfg.get('configName') == 'E2E_FTP_SIMPLE':
            ftp_id = cfg.get('id')
            print(f"   FTP ID: {ftp_id}")
            break
    
    # 6. 创建报表配置
    print("5. 创建报表配置...")
    report_data = {
        "reportCode": "E2E_SIMPLE_TEST",
        "reportName": "E2E简化测试报表",
        "ftpConfigId": ftp_id,
        "filePattern": "test_flow_e2e*.xlsx",
        "sheetIndex": 0,
        "headerRow": 0,
        "dataStartRow": 1,
        "columnMappings": [
            {"excelColumn": "A", "fieldName": "col_a", "fieldType": "STRING"},
            {"excelColumn": "B", "fieldName": "col_b", "fieldType": "INTEGER"},
            {"excelColumn": "C", "fieldName": "col_c", "fieldType": "DECIMAL"}
        ],
        "outputTable": "t_e2e_simple_test",
        "outputMode": "APPEND",
        "status": 1
    }
    resp = session.post(f"{API_BASE}/report/config", json=report_data)
    print(f"   报表响应: {resp.status_code} - {resp.json()}")
    
    if resp.status_code != 200 or resp.json().get('code') != 200:
        print("   报表配置创建失败!")
        return
    
    # 7. 获取报表配置ID
    print("6. 获取报表配置ID...")
    resp = session.get(f"{API_BASE}/report/config/page?pageNum=1&pageSize=100")
    report_id = None
    for cfg in resp.json().get('data', {}).get('records', []):
        if cfg.get('reportCode') == 'E2E_SIMPLE_TEST':
            report_id = cfg.get('id')
            print(f"   报表ID: {report_id}")
            break
    
    # 8. 触发扫描
    print("7. 触发扫描...")
    resp = session.post(f"{API_BASE}/report/config/{report_id}/scan")
    print(f"   扫描响应: {resp.status_code} - {resp.json()}")
    task_id = resp.json().get('data')
    
    if resp.status_code != 200 or resp.json().get('code') != 200:
        print("   扫描触发失败!")
        return
    
    # 9. 等待并检查任务状态
    print("8. 检查任务状态...")
    import time
    time.sleep(3)
    resp = session.get(f"{API_BASE}/task/{task_id}")
    print(f"   任务状态: {resp.json()}")
    
    # 10. 检查数据表
    print("9. 检查数据表...")
    resp = session.get(f"{API_BASE}/data-center/tables")
    print(f"   数据表列表: {resp.status_code}")
    
    print("\n" + "=" * 50)
    print("测试完成!")
    print("=" * 50)

if __name__ == "__main__":
    main()
