# Pipeline与Trigger监控功能设计

> **文档版本**: V1.0
> **创建日期**: 2026-04-07
> **状态**: 待审核

---

## 1. 需求概述

### 1.1 背景
当前项目的Trigger和Pipeline模块工作在后端，缺乏前端监控界面。用户无法直观地：
- 查看Trigger的实时状态（等待数据/已触发/重试中）
- 查看Pipeline的执行历史记录
- 了解系统数据处理流水线的工作状态

### 1.2 优化目标
1. **Trigger轮询间隔优化**：TriggerJob使用数据库配置的`poll_interval_seconds`字段
2. **Trigger实时状态监控**：前端展示Trigger列表及当前状态
3. **Pipeline执行历史**：展示Pipeline执行记录（复用现有task_execution表）

### 1.3 范围界定
- **保持不变**：Trigger和Pipeline的Java代码实现（硬编码方案）
- **新增功能**：Trigger状态API、Trigger执行日志表、前端监控页面

---

## 2. 技术方案

### 2.1 数据库改造

#### 2.1.1 新增表：trigger_execution_log（Trigger执行日志表）

```sql
CREATE TABLE trigger_execution_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code VARCHAR(100) NOT NULL COMMENT '触发器编码',
    trigger_name VARCHAR(200) NOT NULL COMMENT '触发器名称',
    partition_date DATE NOT NULL COMMENT '分区日期',
    data_count INT DEFAULT 0 COMMENT '检测到的数据行数',
    trigger_status VARCHAR(20) NOT NULL COMMENT '触发状态: TRIGGERED-已触发, WAITING-等待中, SKIPPED-已跳过, FAILED-失败',
    pipeline_task_id BIGINT COMMENT '关联的Pipeline任务ID',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '触发时的重试次数',
    execution_time DATETIME NOT NULL COMMENT '执行时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_trigger_code (trigger_code),
    KEY idx_partition_date (partition_date),
    KEY idx_execution_time (execution_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Trigger执行日志表';
```

### 2.2 后端改造

#### 2.2.1 TriggerServiceImpl 新增方法

```java
public interface ITriggerService {
    // 现有方法...

    // 新增：获取Trigger实时状态
    TriggerRealtimeState getRealtimeState(String triggerCode);

    // 新增：记录Trigger执行日志
    void logTriggerExecution(TriggerExecutionLog log);

    // 新增：查询Trigger执行历史
    List<TriggerExecutionLog> getExecutionHistory(String triggerCode, LocalDate partitionDate);
}
```

#### 2.2.2 TriggerState持久化

TriggerStateManager增加持久化方法，将状态同步到数据库：

```java
@Component
public class TriggerStateManager {
    // 内存状态
    private final ConcurrentHashMap<String, TriggerState> states = new ConcurrentHashMap<>();

    // 新增：持久化状态到数据库
    public void persistState(String triggerCode);

    // 新增：从数据库恢复状态
    public void restoreState(String triggerCode);
}
```

#### 2.2.3 TriggerJob动态间隔

修改TriggerJob使用数据库配置的`poll_interval_seconds`：

```java
public class TriggerJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 从trigger_config读取各触发器的间隔配置
        // 使用ScheduleBuilder动态调整下次执行时间
    }
}
```

#### 2.2.4 新增API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/trigger` | GET | 获取所有Trigger配置列表 |
| `/api/trigger/state` | GET | 获取所有Trigger实时状态 |
| `/api/trigger/state/{code}` | GET | 获取单个Trigger实时状态 |
| `/api/trigger/history/{code}` | GET | 获取Trigger执行历史 |
| `/api/trigger/log` | POST | 记录Trigger执行日志（内部调用） |

#### 2.2.5 实体类新增

**TriggerRealtimeState（实时状态DTO）**：
```java
@Data
public class TriggerRealtimeState {
    private String triggerCode;
    private String triggerName;
    private String status;          // WAITING/CHECKING/TRIGGERED/SKIPPED
    private int retryCount;
    private int maxRetries;
    private Date lastCheckTime;
    private Date lastTriggerTime;
    private String pipelineCode;
    private Integer pollIntervalSeconds;
}
```

### 2.3 前端改造

#### 2.3.1 新增页面：TriggerMonitor.vue

**路由**：`/trigger-monitor`

**功能**：
- Trigger配置列表（关联trigger_config表）
- 实时状态展示（轮询获取）
- 状态说明：
  - `WAITING`：等待数据中（retryCount < maxRetries）
  - `CHECKING`：正在检查数据
  - `TRIGGERED`：已触发Pipeline
  - `SKIPPED`：等待超时已跳过

**界面布局**：
```
┌─────────────────────────────────────────────────────┐
│  Trigger监控                                           │
├─────────────────────────────────────────────────────┤
│  刷新按钮 │ 筛选条件（状态/Trigger名称）              │
├─────────────────────────────────────────────────────┤
│  Trigger名称 │ 状态 │ 重试次数 │ 最后检查 │ 操作   │
├─────────────────────────────────────────────────────┤
│  sales_trigger │ WAITING │ 3/60 │ 2026-04-07 10:30 │ 查看历史│
│  inventory_... │ TRIGGERED│ 0/60 │ 2026-04-07 10:29 │ 查看历史│
└─────────────────────────────────────────────────────┘
```

#### 2.3.2 新增页面：TriggerHistory.vue（可选：抽屉/对话框）

**功能**：
- Trigger执行历史时间线
- 展示每次触发的：执行时间、分区日期、数据行数、触发状态、关联的Pipeline任务

#### 2.3.3 Pipeline历史查询

**整合方式**：
- 复用现有任务监控页面（TaskMonitor.vue）
- 增加任务类型筛选：`PIPELINE`
- 或新增独立页面`PipelineHistory.vue`

**展示信息**：
- Pipeline代码和名称
- 分区日期
- 执行状态（SUCCESS/FAILED）
- 执行时长
- 触发来源（手动/Trigger触发）

#### 2.3.4 API接口封装

**新增文件**：`src/api/trigger.js`

```javascript
import request from '@/utils/request'

export function getTriggerList() {
  return request({
    url: '/trigger',
    method: 'get'
  })
}

export function getTriggerStateList() {
  return request({
    url: '/trigger/state',
    method: 'get'
  })
}

export function getTriggerState(code) {
  return request({
    url: `/trigger/state/${code}`,
    method: 'get'
  })
}

export function getTriggerHistory(code, params) {
  return request({
    url: `/trigger/history/${code}`,
    method: 'get',
    params
  })
}
```

---

## 3. 数据流设计

### 3.1 Trigger执行流程（增强版）

```
┌─────────────────────────────────────────────────────────────────┐
│                        TriggerJob.execute()                      │
├─────────────────────────────────────────────────────────────────┤
│  1. 加载所有ENABLED的TriggerConfig                               │
│  2. 遍历每个Trigger：                                            │
│     ├─ getOrCreate TriggerState (内存或数据库恢复)                │
│     ├─ checkDataExists() 检查源表数据                             │
│     ├─ 有数据且未触发：                                           │
│     │   ├─ triggerPipeline() 触发Pipeline执行                     │
│     │   ├─ logTriggerExecution(TRIGGERED) 记录日志                │
│     │   └─ stateManager.reset() 重置状态                          │
│     ├─ 无数据：                                                   │
│     │   ├─ retryCount++                                          │
│     │   ├─ logTriggerExecution(WAITING) 记录日志(可选)            │
│     │   └─ 超时检查：retryCount > maxRetries → SKIPPED            │
│     └─ stateManager.persistState() 持久化状态                     │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 前端轮询机制

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  前端页面        │     │  后端API         │     │  TriggerState   │
│  TriggerMonitor │ ←──→│  /trigger/state  │ ←──→│  Manager        │
└─────────────────┘     └─────────────────┘     └─────────────────┘
       │                                                │
       │  每10秒轮询                                     │
       │                                                │
       ▼                                                ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. getTriggerStateList() → 获取所有Trigger状态                  │
│  2. 渲染状态列表                                                 │
│  3. 状态变化时高亮显示                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 界面设计

### 4.1 TriggerMonitor页面

#### 4.1.1 页面布局

```
┌──────────────────────────────────────────────────────────────────┐
│ Trigger监控                                    [🔄 刷新] [⚙ 设置] │
├──────────────────────────────────────────────────────────────────┤
│ 状态筛选： [全部▼] [WAITING] [TRIGGERED] [SKIPPED]               │
├──────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────┐ │
│ │ Trigger名称      │ 状态        │ 重试/最大  │ 最后检查时间   │ │
│ ├──────────────────────────────────────────────────────────────┤ │
│ │ sales_trigger   │ 🟡 WAITING  │ 12/60    │ 10:30:15      │ │
│ │ inventory_trg   │ 🟢 TRIGGERED│ 0/60     │ 10:29:45      │ │
│ │ order_trigger   │ 🔴 SKIPPED  │ 65/60    │ 10:28:30      │ │
│ └──────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────┤
│ 状态说明：                                                        │
│ 🟡 WAITING - 等待源数据就绪，当前正在重试                           │
│ 🟢 TRIGGERED - 已检测到数据并触发Pipeline执行                      │
│ 🔴 SKIPPED - 等待超时（超过最大重试次数）                         │
└──────────────────────────────────────────────────────────────────┘
```

#### 4.1.2 Trigger历史抽屉

点击某行的"查看历史"按钮，弹出抽屉展示该Trigger的执行历史：

```
┌─────────────────────────────────────────────────────────────┐
│ Trigger执行历史: sales_trigger                    [关闭]     │
├─────────────────────────────────────────────────────────────┤
│ 时间                    │ 状态       │ 数据行数 │ Pipeline任务│
├─────────────────────────────────────────────────────────────┤
│ 2026-04-07 10:30:15   │ TRIGGERED  │ 1520    │ Task #123  │
│ 2026-04-07 10:29:15   │ WAITING    │ 0       │ -          │
│ 2026-04-07 10:28:15   │ WAITING    │ 0       │ -          │
│ ...                    │ ...        │ ...     │ ...        │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Pipeline历史（整合到任务监控）

在现有TaskMonitor页面增加Pipeline筛选：

```
┌──────────────────────────────────────────────────────────────────┐
│ 任务监控                                            [🔄 刷新]    │
├──────────────────────────────────────────────────────────────────┤
│ 任务类型：[全部▼] [FTP_SCAN] [FILE_PROCESS] [PIPELINE] [TRIGGER] │
├──────────────────────────────────────────────────────────────────┤
│ 任务名称              │ 类型       │ 状态   │ 开始时间    │ 操作│
├──────────────────────────────────────────────────────────────────┤
│ 销售数据处理流水线    │ PIPELINE   │ SUCCESS│ 10:30:15   │日志│
│ 内置FTP扫描-销售报表  │ FTP_SCAN   │ SUCCESS│ 10:25:30   │日志│
│ 销售数据处理流水线    │ PIPELINE   │ FAILED │ 09:30:00   │日志│
└──────────────────────────────────────────────────────────────────┘
```

---

## 5. 配置变更

### 5.1 Router新增

```javascript
{
  path: '/trigger-monitor',
  name: 'TriggerMonitor',
  component: () => import('@/views/trigger/TriggerMonitor.vue')
}
```

### 5.2 路由调整（可选）

将Trigger监控入口添加到导航菜单或现有Tab页中。

---

## 6. 实现计划

### 6.1 Phase 1: 后端基础改造
1. 创建trigger_execution_log表（SQL脚本）
2. TriggerExecutionLog实体类
3. ITriggerService新增方法实现
4. TriggerStateManager持久化改造
5. 新增TriggerState API

### 6.2 Phase 2: TriggerJob动态间隔
1. 修改QuartzConfig支持动态Trigger
2. TriggerJob使用数据库配置的间隔
3. 触发时记录执行日志

### 6.3 Phase 3: 前端页面开发
1. 新增trigger.js API封装
2. TriggerMonitor.vue页面
3. TriggerHistory.vue历史抽屉
4. TaskMonitor.vue增加PIPELINE筛选

---

## 7. 风险与注意事项

| 风险 | 说明 | 应对措施 |
|------|------|----------|
| TriggerState内存丢失 | 服务重启后内存状态丢失 | 服务启动时从数据库恢复状态 |
| 频繁轮询性能 | 前端10秒轮询可能造成压力 | 可调整为30秒或使用WebSocket |
| 状态同步一致性 | 内存状态和数据库状态可能不一致 | 使用数据库作为事实来源，内存作为缓存 |
| TriggerJob并发 | 多个TriggerJob实例可能同时执行 | 使用Quartz分布式锁或单节点部署 |

---

## 8. 验收标准

### 8.1 功能验收
- [ ] Trigger实时状态API正常返回各Trigger状态
- [ ] Trigger执行历史正确记录到数据库
- [ ] TriggerJob使用数据库配置的poll_interval_seconds
- [ ] 前端TriggerMonitor页面正常展示Trigger列表和状态
- [ ] 前端可查看Trigger执行历史时间线
- [ ] 任务监控页面可筛选PIPELINE类型的任务

### 8.2 非功能验收
- [ ] 状态轮询响应时间 < 500ms
- [ ] 页面加载时间 < 2s
- [ ] 无明显前端性能问题

---

## 9. 附录

### 9.1 相关文件清单

**后端新增**：
- `TriggerExecutionLog.java` - Trigger执行日志实体
- `TriggerRealtimeState.java` - 实时状态DTO
- `ITriggerService` - 新增3个方法
- `TriggerServiceImpl` - 实现新方法
- `TriggerStateManager` - 增加持久化方法
- `TriggerJob` - 动态间隔改造
- `TriggerController` - 新增API端点
- SQL脚本 - 创建trigger_execution_log表

**前端新增**：
- `src/api/trigger.js` - API封装
- `src/views/trigger/TriggerMonitor.vue` - 监控页面
- `src/views/trigger/TriggerHistory.vue` - 历史抽屉

**前端修改**：
- `src/router/index.js` - 新增路由
- `src/views/task/TaskMonitor.vue` - 增加PIPELINE筛选