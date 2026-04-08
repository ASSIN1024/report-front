# AGENTS.md - AI Agent 知识地图

> **文档版本**: V1.4
> **创建日期**: 2026-04-04
> **最后更新**: 2026-04-08
> **维护人**: AI Assistant

---

## 项目概述

| 项目 | 内容 |
|------|------|
| **项目名称** | 报表数据处理平台 |
| **项目状态** | 生产就绪，Phase 6 系统集成测试已完成 |
| **技术栈** | Spring Boot 2.1.2 + Vue 2.6 + MySQL |
| ** Harness成熟度** | Phase 1 基础建设进行中 |

**快速链接**:
- 任务清单: [harness/tasks.json](file:///home/nova/projects/report-front/harness/tasks.json)
- 进度记录: [harness/progress-notes.md](file:///home/nova/projects/report-front/harness/progress-notes.md)
- 启动指南: [harness/guides/startup-script-guide.md](file:///home/nova/projects/report-front/harness/guides/startup-script-guide.md)
- 技术规范: [docs/superpowers/specs/](file:///home/nova/projects/report-front/docs/superpowers/specs/)
- 实施计划: [docs/superpowers/plans/](file:///home/nova/projects/report-front/docs/superpowers/plans/)

---

## 目录结构

```
report-front/
├── harness/                    # Harness Engineering 状态管理
│   ├── docs/                  # 项目文档
│   │   └── project-context.md # 项目上下文（核心）
│   ├── tasks.json             # 结构化任务清单（单一事实来源）
│   ├── progress-notes.md       # 进度记录
│   ├── session-history/        # 会话历史归档
│   ├── templates/              # 文档模板
│   └── guides/                 # 操作指南
│       ├── startup-script-guide.md    # 启动脚本指南
│       ├── quick-start.md            # 快速开始
│       └── documentation-sync-guide.md # 文档同步管理
├── scripts/
│   └── start.sh               # 前后端启动脚本
├── docs/
│   ├── superpowers/           # 主要文档区
│   │   ├── plans/             # 实施计划
│   │   └── specs/             # 技术规范
│   └── changes/               # 变更记录
├── report-backend/             # 后端代码 (Spring Boot)
│   └── src/main/java/com/report/
│       ├── controller/        # REST API 控制器
│       ├── service/           # 业务服务层
│       ├── job/               # Quartz 定时任务
│       ├── pipeline/          # 数据处理流水线
│       │   ├── step/         # Step 实现
│       │   ├── example/       # Pipeline 示例
│       │   └── util/          # 工具类
│       ├── trigger/           # 触发器
│       ├── entity/            # 数据实体
│       └── util/              # 工具类
├── src/                       # 前端代码 (Vue 2)
│   └── views/                 # 页面组件
└── test-files/                # 测试数据
```

---

## 技术栈规范

### 后端
- **框架**: Spring Boot 2.1.2
- **ORM**: MyBatis-Plus 3.x
- **数据库**: MySQL 5.7+
- **连接池**: Druid
- **定时任务**: Quartz
- **JDK版本**: 1.8

### 前端
- **框架**: Vue 2.6
- **路由**: Vue Router 3
- **状态管理**: Vuex 3
- **UI库**: Element UI 2
- **HTTP客户端**: Axios 0.21

---

## 会话协议

每次会话开始时，AI Agent 应遵循以下流程：

### 1. Orient（定位）
1. 读取 [harness/progress-notes.md](file:///home/nova/projects/report-front/harness/progress-notes.md) 了解当前进度
2. 读取 [harness/tasks.json](file:///home/nova/projects/report-front/harness/tasks.json) 获取任务清单
3. 查看最近 git history 了解最近变更

### 2. Setup（准备）
1. 确保开发环境正常
2. 使用启动脚本检查服务状态：`./scripts/start.sh status`
3. 如需启动服务：`./scripts/start.sh all`

### 3. Select Task（选择任务）
1. 选择最高优先级的 `pending` 任务
2. 更新任务状态为 `in_progress`
3. 记录到 progress-notes.md

### 4. Implement（实施）
1. 专注完成单个任务
2. 遵循架构约束
3. 保持代码风格一致

### 5. Verify（验证）
1. 运行相关测试
2. 验证功能正常
3. 检查代码规范

### 6. Record（记录）

**自动文档同步（每次变更必须执行）**:
1. 更新 `harness/tasks.json` 任务状态
2. 更新 `harness/progress-notes.md` 会话记录
3. 更新 `API.md` 接口文档（如有新增或修改API）
4. 更新 `docs/superpowers/specs/` 或 `docs/superpowers/plans/` 设计/计划文档
5. 提交 Git

**自动同步检查清单**:
- [ ] tasks.json 任务状态已更新
- [ ] progress-notes.md 会话记录已追加
- [ ] API.md 接口文档已同步（如有变更）
- [ ] 设计/计划文档已更新（如有新文档）
- [ ] Git 已提交

### 7. Sync（同步）

**文档同步自动化要求**:
每次代码变更后，必须通过 `scripts/sync-docs.sh` 脚本自动同步文档，或手动完成以下同步：

1. **API接口变更** → 同步到 `API.md`
2. **新增页面组件** → 记录到对应模块文档
3. **数据库Schema变更** → 更新 `schema.sql`
4. **新增任务** → 更新 `harness/tasks.json`

**脚本位置**: `scripts/sync-docs.sh`（如存在）

---

## 任务管理规范

### tasks.json 操作规则
- **不可删除**: 任务只改变状态，不删除
- **不可重排序**: 保持任务创建顺序
- **只追加**: 新任务添加到数组末尾
- **状态转换**: `pending` → `in_progress` → `completed`

### 任务状态
| 状态 | 说明 |
|------|------|
| `pending` | 待开始 |
| `in_progress` | 进行中 |
| `completed` | 已完成 |
| `blocked` | 被阻塞 |

---

## 架构约束

### 代码组织
- Controller层只做参数校验和响应封装
- 业务逻辑在 Service 层
- 数据库操作在 Mapper 层
- 工具类放在 util 包

### 命名约定
| 类型 | 规范 | 示例 |
|------|------|------|
| Controller | XxxController | FtpConfigController |
| Service | XxxService / XxxServiceImpl | FtpConfigServiceImpl |
| Mapper | XxxMapper | FtpConfigMapper |
| Entity | Xxx | FtpConfig |
| Job | XxxJob | FtpScanJob |

### Git提交规范
```
<type>(<scope>): <description>

[可选的详细说明]

关联任务: H-XXX
```

**Type类型**: feat, fix, docs, refactor, test, chore

---

## 验证标准

### 代码审查清单
- [ ] 代码符合命名约定
- [ ] 异常处理完善
- [ ] 日志记录适当
- [ ] 无硬编码配置
- [ ] SQL注入防护
- [ ] 并发安全（如适用）

### 测试要求
- [ ] 新功能有测试
- [ ] 修改后回归测试
- [ ] API接口测试通过

---

## 紧急情况参考

| 情况 | 参考文档 |
|------|----------|
| 服务启动/停止 | [启动脚本指南](file:///home/nova/projects/report-front/harness/guides/startup-script-guide.md) |
| 项目上下文 | [project-context.md](file:///home/nova/projects/report-front/harness/docs/project-context.md) |
| 快速开始 | [quick-start.md](file:///home/nova/projects/report-front/harness/guides/quick-start.md) |
| API问题 | API.md |
| 数据库问题 | schema.sql |
| 部署问题 | docs/superpowers/plans/ |

---

## 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-04-04 | V1.0 | 初始创建AGENTS.md知识地图 | AI Assistant |
| 2026-04-05 | V1.1 | [优化] 更新harness目录结构，移除CLAUDE.md引用 | AI Assistant |
| 2026-04-06 | V1.2 | [修复] 内置FTP MyBatis映射问题，新增配置驱动自动启动功能 | AI Assistant |
