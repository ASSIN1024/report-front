# 消费检测与智能打包实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现消费检测机制（检测for-upload目录是否为空）和智能打包机制（单包不超过200MB，分批打包）

**Architecture:** 修改现有 `ConsumptionWatcher`、`PackingService`、`PackingManager` 组件，增加文件存在检测和大小预估逻辑

**Tech Stack:** Java Spring Boot, MyBatis-Plus, Quartz

---

## 文件改动概览

| 文件 | 改动类型 | 职责 |
|------|---------|------|
| `ConsumptionWatcherImpl.java` | 修改 | 实现 `isBeingConsumed()` 检测 for-upload 目录是否为空 |
| `PackingServiceImpl.java` | 修改 | 实现 `isBeingConsumed()` 检查固定文件名是否存在 |
| `PackingManagerImpl.java` | 修改 | 增加预估大小和分批打包逻辑 |

---

## Task 1: 实现 ConsumptionWatcherImpl.isBeingConsumed()

**Files:**
- Modify: `report-backend/src/main/java/com/report/packing/service/impl/ConsumptionWatcherImpl.java`

- [ ] **Step 1: 添加 PackingService 依赖**

在 `ConsumptionWatcherImpl.java` 中添加 `PackingService` 依赖注入

```java
@Autowired
private PackingService packingService;
```

- [ ] **Step 2: 实现 isBeingConsumed() 方法**

修改 `isBeingConsumed()` 方法，委托给 `packingService.isBeingConsumed()`

```java
@Override
public boolean isConsumed() {
    if (!running.get()) {
        return false;
    }
    return !packingService.isBeingConsumed();
}
```

- [ ] **Step 3: 编译验证**

Run: `cd report-backend && mvn compile -q`
Expected: 编译成功

- [ ] **Step 4: 提交**

```bash
git add report-backend/src/main/java/com/report/packing/service/impl/ConsumptionWatcherImpl.java
git commit -m "feat(packing): 实现isBeingConsumed检测for-upload目录是否为空"
```

---

## Task 2: 实现 PackingServiceImpl.isBeingConsumed()

**Files:**
- Modify: `report-backend/src/main/java/com/report/packing/service/impl/PackingServiceImpl.java:137-151`

- [ ] **Step 1: 替换 isBeingConsumed() 方法体**

当前实现始终返回 `false`，需要替换为检测 `for-upload` 目录中是否存在 `outputs.zip`

```java
@Override
public boolean isBeingConsumed() {
    String uploadDir = getUploadDir();
    String fixedFilename = getFixedFilename();
    try {
        File uploadDirFile = new File(uploadDir);
        File targetFile = new File(uploadDirFile, fixedFilename);
        return targetFile.exists();
    } catch (Exception e) {
        log.error("Failed to check FTP file existence", e);
        return true;  // 保守策略：异常时认为正在被消费
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd report-backend && mvn compile -q`
Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/packing/service/impl/PackingServiceImpl.java
git commit -m "feat(packing): 实现isBeingConsumed检测outputs.zip是否存在"
```

---

## Task 3: 实现智能打包 - 预估大小和分批逻辑

**Files:**
- Modify: `report-backend/src/main/java/com/report/packing/manager/impl/PackingManagerImpl.java`

- [ ] **Step 1: 添加 getMaxPackageSize() 方法调用**

在 `executePacking()` 方法开头添加最大包大小配置获取

```java
Long maxPackageSize = configService.getIntValue("max_package_size", 209715200L); // 200MB
```

- [ ] **Step 2: 重构打包逻辑 - 分批打包**

将原有的简单打包逻辑改为：
1. 收集所有待打包文件
2. 计算预估大小
3. 如果超过阈值，分批打包

替换原有的打包循环逻辑：

```java
@Override
public void executePacking() {
    log.info("PackingManager executing...");

    // 检查消费状态
    if (!packingService.canUpload()) {
        log.info("Previous package is being consumed, waiting...");
        boolean consumed = checkAndWaitForConsumption();
        if (!consumed) {
            log.warn("Consumption timeout, will retry next cycle");
            return;
        }
    }

    // 获取待打包文件
    List<ProcessedFile> pendingFiles = processedFileMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProcessedFile>()
            .eq("status", "PROCESSED")
            .isNull("batch_no")
            .orderByAsc("create_time")  // 按创建时间顺序打包
    );

    if (pendingFiles.isEmpty()) {
        log.info("No pending files to pack");
        return;
    }

    // 计算预估大小
    long estimatedSize = pendingFiles.stream()
        .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0)
        .sum();

    Long maxPackageSize = configService.getIntValue("max_package_size", 209715200L);

    if (estimatedSize > maxPackageSize) {
        // 超过阈值，分批打包
        List<Long> fileIds = new java.util.ArrayList<>();
        long currentSize = 0;

        for (ProcessedFile file : pendingFiles) {
            long fileSize = file.getFileSize() != null ? file.getFileSize() : 0;
            if (currentSize + fileSize > maxPackageSize && !fileIds.isEmpty()) {
                // 达到阈值，打包当前批次
                packingService.pack(fileIds);
                log.info("分批打包完成，已打包 {} 个文件，大小 {} bytes，剩余文件下一轮处理",
                    fileIds.size(), currentSize);
                return;  // 本轮结束，等待消费后下一轮继续
            }
            fileIds.add(file.getId());
            currentSize += fileSize;
        }

        // 打包剩余文件（未超过阈值）
        if (!fileIds.isEmpty()) {
            packingService.pack(fileIds);
        }
    } else {
        // 未超过阈值，打包全部
        List<Long> fileIds = pendingFiles.stream()
            .map(ProcessedFile::getId)
            .collect(java.util.stream.Collectors.toList());
        packingService.pack(fileIds);
    }

    log.info("PackingManager execution completed");
}
```

- [ ] **Step 3: 编译验证**

Run: `cd report-backend && mvn compile -q`
Expected: 编译成功

- [ ] **Step 4: 提交**

```bash
git add report-backend/src/main/java/com/report/packing/manager/impl/PackingManagerImpl.java
git commit -m "feat(packing): 实现智能打包 - 单包不超过200MB,超过时分批打包"
```

---

## Task 4: 添加配置项（可选）

**Files:**
- 无代码改动，仅文档说明

如需在 `packing_config` 表中添加配置项（可选，当前代码使用默认值）：

```sql
INSERT INTO packing_config (config_key, config_value, description, deleted)
VALUES ('max_package_size', '209715200', '单包大小上限(字节)', 0);
```

---

## Task 5: 端到端测试

**Files:**
- 无代码改动

- [ ] **Step 1: 重启后端服务**

```bash
ps aux | grep spring-boot:run | grep -v grep | awk '{print $2}' | xargs kill
cd report-backend && mvn spring-boot:run -Dmaven.test.skip=true &
```

- [ ] **Step 2: 模拟消费流程**

```bash
# 1. 确认 for-upload 目录为空
ls -la /data/ftp-root/for-upload/

# 2. 等待打包任务执行
sleep 90

# 3. 检查 outputs.zip 是否生成
ls -la /data/ftp-root/for-upload/

# 4. 模拟消费（移动文件到 done）
mkdir -p /data/ftp-root/done
mv /data/ftp-root/for-upload/outputs.zip /data/ftp-root/done/outputs_$(date +%Y%m%d_%H%M%S)_done.zip

# 5. 验证 for-upload 变空后，下一轮能正常打包
sleep 90
ls -la /data/ftp-root/for-upload/
```

- [ ] **Step 6: 验证完成**

Expected:
- 首次打包后 for-upload 有 outputs.zip
- 消费后 for-upload 为空
- 下一轮能正常生成新的 outputs.zip

---

## 验证清单

- [ ] `ConsumptionWatcherImpl.isBeingConsumed()` 返回正确值
- [ ] `PackingServiceImpl.isBeingConsumed()` 检测 outputs.zip 存在性
- [ ] `PackingManagerImpl` 预估打包大小并分批处理
- [ ] 端到端消费流程正常工作

---

## Task 6: 分区日期提取 - 有效性校验

**Files:**
- Modify: `report-backend/src/main/java/com/report/service/impl/ProcessedFileServiceImpl.java`

**问题背景:**
文件名 `scan_12026052_test120260520.xlsx` 包含两个8位数字：
- `12026052` → 1202年60月52日 ❌ 无效月份和日期
- `20260520` → 2026年05月20日 ✅ 有效

**实现方案:**

从文件名提取所有8位数字候选，从后向前校验有效性（月份1-12，日期1-31），返回第一个有效日期。

- [ ] **Step 1: 实现有效性校验逻辑**

修改 `extractDateFromFileName()` 方法：

```java
private String extractDateFromFileName(String fileName) {
    if (fileName == null || fileName.trim().isEmpty()) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{8})");
    java.util.regex.Matcher matcher = pattern.matcher(fileName);
    java.util.ArrayList<String> candidates = new java.util.ArrayList<>();
    while (matcher.find()) {
        candidates.add(matcher.group(1));
    }
    // 从后向前校验有效性
    for (int i = candidates.size() - 1; i >= 0; i--) {
        String dateStr = candidates.get(i);
        int month = Integer.parseInt(dateStr.substring(4, 6));
        int day = Integer.parseInt(dateStr.substring(6, 8));
        if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
            return dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
        }
    }
    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
}
```

- [ ] **Step 2: 编译验证**

Run: `cd report-backend && mvn compile -q`
Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/service/impl/ProcessedFileServiceImpl.java
git commit -m "fix(packing): 添加分区日期有效性校验"
```

- [ ] **Step 4: 更新设计文档**

在 `docs/superpowers/specs/2026-05-06-consumption-detection-design.md` 中添加第11章「分区日期提取规则」
