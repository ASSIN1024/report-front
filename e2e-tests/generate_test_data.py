#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
E2E测试数据生成脚本
用于生成符合报表配置字段映射的Excel测试文件
"""

import openpyxl
from datetime import datetime
import os
import sys

def create_e2e_excel(filename, rows=10, headers=None):
    """
    生成E2E测试用Excel文件

    Args:
        filename: 文件完整路径
        rows: 数据行数
        headers: 表头列表，默认 ["col_a", "col_b", "col_c"]
    """
    if headers is None:
        headers = ["col_a", "col_b", "col_c"]

    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Sheet1"

    # 表头
    ws.append(headers)

    # 数据行
    for i in range(rows):
        row_data = []
        for j, header in enumerate(headers):
            if header.endswith('_a') or header.endswith('_str'):
                row_data.append(f"测试文本_{i+1}")
            elif header.endswith('_b') or header.endswith('_int'):
                row_data.append(i + 1)
            elif header.endswith('_c') or header.endswith('_decimal') or header.endswith('_num'):
                row_data.append(round((i + 1) * 1.5, 2))
            else:
                row_data.append(f"数据_{i+1}_{j}")
        ws.append(row_data)

    # 确保目录存在
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    wb.save(filename)
    print(f"测试文件已生成: {filename}")
    return filename

def main():
    date_str = datetime.now().strftime("%Y%m%d")
    time_str = datetime.now().strftime("%H%M%S")

    # FTP上传目录
    ftp_upload_dir = "/home/nova/projects/report-front/data/ftp-root/upload"

    # 测试文件名
    filename = os.path.join(ftp_upload_dir, f"test_flow_e2e_{date_str}_{time_str}.xlsx")

    # 生成文件
    create_e2e_excel(filename, rows=10)

    print(f"\n文件信息:")
    print(f"  路径: {filename}")
    print(f"  大小: {os.path.getsize(filename)} bytes")
    print(f"  时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

if __name__ == "__main__":
    main()
