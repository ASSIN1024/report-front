# 消费检测与智能打包机制设计方案

**日期**: 2026-05-06
**状态**: 已批准

## 1. 背景

当前打包系统存在以下问题：
1. `ConsumptionWatcherImpl.isBeingConsumed()` 方法未实现，始终返回 `false`
2. 打包时未考虑文件大小阈值（200MB），可能导致打包文件过大
3. 等待消费超时时直接跳过本次打包，导致文件累积

## 2. 设计目标

1. 实现消费检测机制：检测 `for-upload` 目录是否为空
2. 实现智能打包：单包大小不超过 200MB
3. 解决文件累积问题：分批打包，不阻塞

## 3. 目录结构

```
/data/ftp-root/
├── for-upload/          # 下游RPA取件目录，固定文件名 outputs.zip
├── staging/             # 打包临时目录
├── done/                # 消费归档目录，命名格式: outputs_YYYYMMDD_HHmmss_done.zip
└── upload/              # 源文件上传目录
```

## 4. 消费检测机制

### 4.1 检测方式
- **定时轮询**：由 Quartz Job 触发
- **检测逻辑**：检查 `for-upload` 目录是否为空
- **轮询间隔**：由配置 `polling_interval` 控制（默认30秒）
- **最大等待时间**：由配置 `max_wait_minutes` 控制（默认60分钟）

### 4.2 消费流程
```
下游RPA:
1. 从 for-upload/ 取走 outputs.zip
2. 归档到 done/outputs_YYYYMMDD_HHmmss_done.zip (命名仅用于审计)

系统检测:
1. 下一轮打包前检查 for-upload 是否为空
2. 为空 → 正常打包
3. 不为空 → 等待消费
4. 超时 → 跳过本次，等待下一轮
```

## 5. 智能打包机制

### 5.1 打包阈值
- **单包大小上限**：由配置 `max_package_size` 控制（默认 200MB）

### 5.2 打包流程

```
PackingJob 触发
     │
     ▼
检查 for-upload 状态
     │
     ├─ 为空 ──────────────────────┐
     │                              │
     │                              ▼
     │                    收集待打包文件 (batch_no IS NULL)
     │                              │
     │                              ▼
     │                    预估打包大小
     │                              │
     │              ┌───────────────┴───────────────┐
     │              ▼                               ▼
     │    预估大小 ≤ 200MB               预估大小 > 200MB
     │              │                               │
     │              │                               ▼
     │              │                     打包累计 < 200MB 的文件
     │              │                     (剩余文件下一轮处理)
     │              │                               │
     │              ▼                               ▼
     │        打包所有文件                   本轮打包完成，返回
     │              │
     │              ▼
     └──────► 复制到 for-upload
                      │
                      ▼
                 本轮打包完成
```

### 5.3 分批打包逻辑

```java
// 收集所有待打包文件
List<ProcessedFile> allFiles = getPendingFiles();

// 计算预估大小
long estimatedSize = allFiles.stream()
    .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0)
    .sum();

// 如果超过阈值，分批打包
if (estimatedSize > maxPackageSize) {
    // 先打包一部分（< 200MB）
    packSubset(allFiles, maxPackageSize);
    // 剩余文件保持 batch_no = NULL，下一轮处理
} else {
    // 打包全部
    packAll(allFiles);
}
```

## 6. 改动范围

### 6.1 新增/修改文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `ConsumptionWatcherImpl.java` | 修改 | 实现 `isBeingConsumed()` 检测 for-upload 是否为空 |
| `PackingServiceImpl.java` | 修改 | 实现 `isBeingConsumed()` 检查文件是否存在 |
| `PackingManagerImpl.java` | 修改 | 增加预估大小和分批打包逻辑 |

### 6.2 新增配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `max_package_size` | 209715200 (200MB) | 单包大小上限 |
| `consumption_check_interval` | 30 | 消费检测间隔（秒） |
| `max_wait_minutes` | 60 | 最大等待消费时间（分钟） |

## 7. 消费检测伪代码

```java
public boolean isBeingConsumed() {
    String uploadDir = getUploadDir();
    File dir = new File(uploadDir);
    File[] files = dir.listFiles((d, name) -> name.equals(getFixedFilename()));

    // 如果 outputs.zip 存在，说明还未被消费
    return files != null && files.length > 0;
}
```

## 8. 日志记录

| 事件 | 日志级别 | 说明 |
|------|---------|------|
| 开始等待消费 | INFO | 检测到 for-upload 不为空 |
| 消费完成 | INFO | for-upload 变空，被消费 |
| 等待超时 | WARN | 超过最大等待时间 |
| 分批打包 | INFO | 检测到超过阈值，分批处理 |
| 打包完成 | INFO | 本轮打包完成 |

## 9. 异常处理

| 异常场景 | 处理方式 |
|---------|---------|
| 检测文件时异常 | 返回 true（保守策略，不继续打包） |
| 打包时异常 | 抛出异常，不更新 batch_no |
| 磁盘空间不足 | 抛出异常，日志记录 |

## 10. 后续优化方向（不在本期范围）

1. 消费完成自动归档检测（done 目录）
2. 消费完成事件通知（webhook/消息）
3. 历史归档自动清理策略
4. 打包进度可视化
