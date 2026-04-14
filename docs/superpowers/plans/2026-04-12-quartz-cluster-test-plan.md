# Quartz JDBC 集群模式测试验证计划

> **版本**: V1.0
> **创建日期**: 2026-04-12
> **测试目标**: 验证 Quartz JDBC 集群模式在多实例场景下能有效防止任务重复执行

---

## 测试环境准备

### 1. 数据库初始化

```bash
# MySQL 开发环境
mysql -uroot -proot123 report_db < report-backend/src/main/resources/quartz_tables_mysql.sql

# 验证表创建成功
mysql -uroot -proot123 report_db -e "SHOW TABLES LIKE 'QRTZ_%';"
```

### 2. 检查现有 Quartz 表

```sql
-- 应返回 11 张表
SELECT COUNT(*) FROM information_schema.tables 
WHERE table_schema = 'report_db' AND table_name LIKE 'QRTZ_%';
```

---

## 测试用例

### 测试用例 1: 单实例启动验证

**目的**: 验证单实例能正常启动并注册到集群

**步骤**:
1. 启动单个后端实例
   ```bash
   cd report-backend && mvn spring-boot:run
   ```

2. 检查启动日志，确认以下内容：
   ```
   Scheduler class: 'org.quartz.core.QuartzScheduler'
   JobStore class: 'org.quartz.impl.jdbcjobstore.JobStoreTX'
   Using cluster configuration
   ```

3. 查询实例注册状态
   ```sql
   SELECT instance_name, last_checkin_time, checkin_interval 
   FROM QRTZ_SCHEDULER_STATE;
   ```

**预期结果**:
- 启动日志显示 `isClustered: true`
- `QRTZ_SCHEDULER_STATE` 表有一条实例记录
- 实例名称格式为 `ReportScheduler<随机ID>`

---

### 测试用例 2: 多实例集群协调验证

**目的**: 验证多实例不会同时执行同一任务

**步骤**:
1. 启动实例 A（端口 8082）
   ```bash
   cd report-backend && mvn spring-boot:run -Dserver.port=8082
   ```

2. 启动实例 B（端口 8083）
   ```bash
   cd report-backend && mvn spring-boot:run -Dserver.port=8083
   ```

3. 查询集群状态
   ```sql
   SELECT instance_name, last_checkin_time, checkin_interval 
   FROM QRTZ_SCHEDULER_STATE 
   ORDER BY last_checkin_time DESC;
   ```

4. 观察 TriggerJob 执行日志，确认只有一个实例执行

**预期结果**:
- `QRTZ_SCHEDULER_STATE` 表有两条实例记录
- TriggerJob 日志只在一个实例中出现
- `QRTZ_FIRED_TRIGGERS` 表记录显示只有一个实例触发任务

---

### 测试用例 3: 故障转移验证

**目的**: 验证实例宕机后任务能被其他实例接管

**步骤**:
1. 启动两个实例 A 和 B

2. 确认任务正在实例 A 执行

3. 强制停止实例 A
   ```bash
   kill -9 <PID_A>
   ```

4. 等待 20-30 秒（集群心跳检测周期）

5. 观察实例 B 日志，确认任务被接管

6. 查询集群状态
   ```sql
   SELECT * FROM QRTZ_SCHEDULER_STATE;
   SELECT * FROM QRTZ_FIRED_TRIGGERS ORDER BY fired_time DESC LIMIT 10;
   ```

**预期结果**:
- 实例 A 从 `QRTZ_SCHEDULER_STATE` 中移除
- 实例 B 开始执行任务
- 任务无遗漏执行

---

### 测试用例 4: FTP 文件处理去重验证

**目的**: 验证文件处理不会重复

**步骤**:
1. 准备测试文件 `test_report.xlsx` 放入 FTP 目录

2. 启动两个实例

3. 等待 FTP 扫描任务执行

4. 检查处理记录
   ```sql
   SELECT * FROM processed_file WHERE file_name = 'test_report.xlsx';
   SELECT * FROM task_execution WHERE file_name = 'test_report.xlsx';
   ```

**预期结果**:
- `processed_file` 表只有一条记录
- `task_execution` 表只有一条成功记录
- 两个实例日志中只有一个显示处理该文件

---

### 测试用例 5: 触发器状态一致性验证

**目的**: 验证触发器状态在多实例间一致

**步骤**:
1. 配置一个触发器监听某张表

2. 启动两个实例

3. 插入测试数据
   ```sql
   INSERT INTO <source_table> VALUES (...);
   ```

4. 检查触发器执行记录
   ```sql
   SELECT * FROM trigger_execution_log 
   WHERE trigger_code = '<trigger_code>' 
   ORDER BY execution_time DESC;
   
   SELECT * FROM trigger_state_record 
   WHERE trigger_code = '<trigger_code>';
   ```

**预期结果**:
- `trigger_execution_log` 只有一条 TRIGGERED 记录
- `trigger_state_record.triggered` 状态正确更新

---

### 测试用例 6: 并发触发压力测试

**目的**: 验证高并发下无重复执行

**步骤**:
1. 创建多个触发器配置

2. 启动两个实例

3. 批量插入数据触发多个触发器

4. 统计执行结果
   ```sql
   SELECT trigger_code, COUNT(*) as exec_count 
   FROM trigger_execution_log 
   WHERE trigger_status = 'TRIGGERED'
   GROUP BY trigger_code;
   ```

**预期结果**:
- 每个触发器只执行一次
- 无重复执行记录

---

## 验证检查清单

| 检查项 | 验证方法 | 状态 |
|--------|----------|------|
| Quartz 表创建成功 | `SHOW TABLES LIKE 'QRTZ_%'` | ☐ |
| 实例注册到集群 | 查询 `QRTZ_SCHEDULER_STATE` | ☐ |
| 集群心跳正常 | `last_checkin_time` 持续更新 | ☐ |
| 任务不重复执行 | 日志 + `QRTZ_FIRED_TRIGGERS` | ☐ |
| 故障转移正常 | 停止实例后任务继续 | ☐ |
| 文件处理去重 | `processed_file` 唯一记录 | ☐ |
| 触发器状态一致 | `trigger_state_record` 正确更新 | ☐ |

---

## 测试命令速查

```bash
# 查看集群实例状态
mysql -uroot -proot123 report_db -e "
SELECT instance_name, 
       FROM_UNIXTIME(last_checkin_time/1000) as last_checkin,
       checkin_interval/1000 as interval_sec
FROM QRTZ_SCHEDULER_STATE;
"

# 查看最近执行的任务
mysql -uroot -proot123 report_db -e "
SELECT trigger_name, instance_name, 
       FROM_UNIXTIME(fired_time/1000) as fired_at,
       state
FROM QRTZ_FIRED_TRIGGERS 
ORDER BY fired_time DESC LIMIT 10;
"

# 查看当前锁状态
mysql -uroot -proot123 report_db -e "
SELECT * FROM QRTZ_LOCKS;
"

# 清理测试数据（测试后执行）
mysql -uroot -proot123 report_db -e "
DELETE FROM QRTZ_FIRED_TRIGGERS;
DELETE FROM QRTZ_SCHEDULER_STATE;
"
```

---

## 测试结果记录

| 测试用例 | 执行时间 | 结果 | 备注 |
|----------|----------|------|------|
| 单实例启动验证 | | ☐ | |
| 多实例集群协调 | | ☐ | |
| 故障转移验证 | | ☐ | |
| 文件处理去重 | | ☐ | |
| 触发器状态一致性 | | ☐ | |
| 并发触发压力测试 | | ☐ | |

---

## 问题记录

| 问题描述 | 严重程度 | 解决方案 | 状态 |
|----------|----------|----------|------|
| | | | |
