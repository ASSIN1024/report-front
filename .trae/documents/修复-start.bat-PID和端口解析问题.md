# 修复 start.bat 脚本问题

## 问题分析

### 问题1：PID 获取失败
**现象**：`[WARN] Cannot get PID`
**原因**：`wmic process where "name='java.exe'" get processid` 在某些 Windows 版本输出格式不同

### 问题2：端口解析错误
**现象**：`Access URL: http://localhost:com.report.ReportApplication`
**原因**：日志中包含 "started on port" 的行被错误解析

## 修复方案

### 1. 简化 PID 获取逻辑
使用更可靠的方式获取 PID：
```batch
REM 直接获取当前批处理的子进程 PID 太复杂，改用：
REM 方案A：启动后检查 java.exe 进程数是否增加
REM 方案B：使用 %ERRORLEVEL% 判断启动是否成功
```

### 2. 修复端口解析
Spring Boot 日志格式：
```
Started com.report.ReportApplication in X seconds
```
端口信息通常在另一行：`Tomcat started on port 8082`

**修复**：改为读取包含 `port` 和数字的行

## 实施步骤

1. 修改 PID 检测逻辑，使用更简单的进程计数方式
2. 修改端口解析正则，匹配正确的日志格式
3. 测试验证
