import openpyxl
from datetime import datetime, timedelta
import random
import sys

def generate_test_excel(filename, rows):
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "销售数据"
    
    headers = ["订单编号", "产品名称", "客户名称", "销售数量", "销售金额", "销售日期", "区域"]
    ws.append(headers)
    
    products = ["产品A", "产品B", "产品C", "产品D", "产品E"]
    regions = ["华东", "华南", "华北", "华中", "西南", "西北", "东北"]
    
    for i in range(rows):
        order_id = f"ORD{datetime.now().strftime('%Y%m%d')}{str(i+1).zfill(6)}"
        product = random.choice(products)
        customer = f"客户{random.randint(1, 100)}"
        quantity = random.randint(10, 1000)
        amount = quantity * random.uniform(10, 100)
        date = datetime.now() - timedelta(days=random.randint(0, 30))
        region = random.choice(regions)
        
        ws.append([order_id, product, customer, quantity, round(amount, 2), date, region])
    
    wb.save(filename)
    print(f"生成测试文件: {filename}, 行数: {rows}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        rows = int(sys.argv[1])
        filename = f"test-sales-{rows}.xlsx"
        generate_test_excel(filename, rows)
    else:
        generate_test_excel("test-sales-1000.xlsx", 1000)
        generate_test_excel("test-sales-5000.xlsx", 5000)
        generate_test_excel("test-sales-10000.xlsx", 10000)
