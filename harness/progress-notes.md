# Harness Engineering 进度记录

> **文档版本**: V1.2
> **创建日期**: 2026-04-04
> **最后更新**: 2026-04-06T11:00

---

## 会话记录

### 2026-04-04 - Phase 1 基础建设

**会话目标**: 建立 Harness Engineering 基础文档结构

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-001 | 建立 harness/ 目录结构 | ✅ 完成 | session-history/, templates/ 已创建 |
| H-002 | 创建 tasks.json 初始版本 | ✅ 完成 | 包含7个任务项 |
| H-003 | 创建 progress-notes.md | ✅ 完成 | 本文件 |
| H-004 | 创建 session-history/ 目录 | ✅ 完成 | 用于会话归档 |
| H-005 | 创建文档模板 | 🔄 进行中 | templates/ 准备中 |
| H-006 | 完善 AGENTS.md | 🔄 待开始 | 待模板创建后完善 |
| H-007 | 验证Harness基础结构 | 🔄 待开始 | 待所有任务完成后验证 |

**关键决策**:
1. 任务ID前缀使用 `H-` (Harness缩写) 避免与现有任务ID冲突
2. tasks.json 使用严格JSON格式，确保机器可读
3. 操作规则遵循"只追加不删除"原则，抗模型腐化

**下一步计划**:
- 完成 templates/ 模板创建
- 完善 AGENTS.md 知识地图
- 验证整体Harness结构

---

### 2026-04-05 - CLAUDE.md 迁移到 Harness 体系

**会话目标**: 将 CLAUDE.md 内容迁移到 harness 文档体系，弃用 CLAUDE.md

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-009 | CLAUDE.md 迁移到 Harness 体系 | ✅ 进行中 | 项目上下文已迁移 |
| - | 创建 harness/docs/project-context.md | ✅ 完成 | 整合项目概述、技术栈、里程碑 |
| - | 创建 harness/guides/quick-start.md | ✅ 完成 | 迁移快速开始指南 |
| - | 创建 harness/guides/documentation-sync-guide.md | ✅ 完成 | 迁移文档管理机制 |

**迁移内容映射**:
| CLAUDE.md 章节 | 目标位置 | 状态 |
|----------------|----------|------|
| 1. 项目概述 | harness/docs/project-context.md | ✅ 已迁移 |
| 2. 项目背景 | harness/docs/project-context.md | ✅ 已迁移 |
| 3. 项目里程碑 | harness/docs/project-context.md | ✅ 已迁移 |
| 4. 项目进度 | harness/tasks.json | ✅ 已迁移 |
| 5. 团队成员 | harness/docs/project-context.md | ✅ 已迁移 |
| 6. 项目文档 | (废弃) | - |
| 7. 快速开始 | harness/guides/quick-start.md | ✅ 已迁移 |
| 8. 文档同步管理机制 | harness/guides/documentation-sync-guide.md | ✅ 已迁移 |
| 9. 变更记录 | harness/progress-notes.md | ✅ 已迁移 |

**关键决策**:
1. CLAUDE.md 将被删除，全面拥抱 harness 上下文管理
2. 项目上下文信息集中在 harness/docs/project-context.md
3. 操作指南集中在 harness/guides/
4. tasks.json 管理所有任务和进度

**下一步计划**:
- 删除 CLAUDE.md 文件
- 更新 AGENTS.md 移除 CLAUDE.md 引用
- 验证 harness 文档体系完整性

---

### 2026-04-05 - Harness 上下文工程验证系统

**会话目标**: 设计并实施 Harness 上下文工程验证系统

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-010 | 实施 Harness 上下文工程验证系统 | ✅ 完成 | 全部完成 |
| - | 创建测试数据集 (4个JSON) | ✅ 完成 | architecture/progress/conventions/api |
| - | 创建自动化测试脚本 | ✅ 完成 | run-evaluation.sh |
| - | 创建人工评估指南 | ✅ 完成 | deep-evaluation.md |
| - | 创建评估报告模板 | ✅ 完成 | eval-report-template.md |
| - | 创建评估操作指南 | ✅ 完成 | evaluation-guide.md |
| - | 创建评估模块 README | ✅ 完成 | evaluation/README.md |

**验证系统结构**:
```
harness/evaluation/
├── test-datasets/           # 52个测试点
│   ├── architecture.json   # 15点
│   ├── progress.json       # 12点
│   ├── conventions.json    # 15点
│   └── api.json            # 10点
├── test-cases/
│   ├── automated/          # 自动化测试
│   │   └── run-evaluation.sh
│   └── manual/             # 人工评估
│       └── deep-evaluation.md
└── reports/                # 评估报告
```

**设计方案**: docs/superpowers/specs/2026-04-05-harness-evaluation-design.md

**下一步计划**:
- 执行首次评估验证
- 根据评估结果优化测试数据集

---

### 历史会话

*(后续会话记录添加于此)*

---

### 2026-04-05 - 任务监控增强与数据清洗配置

**会话目标**: 实现手动执行任务触发功能和数据清洗规则配置

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-011 | 任务监控增强 - 立即扫描功能 | ✅ 完成 | 报表配置列表增加立即扫描按钮 |
| H-012 | 数据清洗规则配置 | ✅ 完成 | 列映射支持替换规则如'-' → '0' |
| H-013 | 列映射JSON导入功能 | ✅ 完成 | 三步骤向导：输入→校验→导入 |
| H-014 | 补充单元测试 | ✅ 完成 | 42个测试全部通过 |

**关键决策**:
1. 立即扫描采用方案A：用户先放置文件到FTP目录，再触发扫描
2. 清洗规则采用简单替换方案，支持多条规则配置
3. JSON导入三步骤：输入→格式校验→预览确认
4. UI设计通过浏览器可视化展示并获得用户确认

**技术实现**:
- 后端: FtpScanJob.scanReportConfig(), DataProcessJob清洗规则应用, ColumnMappingValidator
- 前端: CleanRulesDialog, JsonImportDialog组件
- 测试: ColumnMappingValidatorTest(12), DataCleaningTest(11), CleanRuleTest(4)

**文档输出**:
- 设计文档: docs/superpowers/specs/2026-04-05-task-monitor-and-data-cleaning-design.md
- 实施计划: docs/superpowers/plans/2026-04-05-task-monitor-and-data-cleaning-plan.md

**下一步计划**:
- ✅ 手动功能测试验证新功能 - 后端编译通过，服务重启成功
- ✅ 代码合并到master并推送 - 已推送到GitHub

---

### 2026-04-05 - 内置FTP服务集成

**会话目标**: 集成Apache FtpServer实现RPA机器人直接上传Excel文件到本系统

**需求背景**:
- RPA机器人需要将Excel文件上传到专用FTP，不与外部FTP混用
- 内置FTP作为外部FTP的备份方案，防止外部FTP服务挂掉
- 少量并发(1-3 RPA)，但文件可能超过200MB

**技术选型**: Apache FtpServer 1.2.0 (纯Java，嵌入式)

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-FTP-INTEGRATION | 内置FTP服务集成 | ✅ 完成 | 全部完成 |

**实现内容**:
- Apache FtpServer 1.2.0 依赖添加 (ftpserver-core, ftplet-api, mina-core)
- BuiltInFtpConfig 实体和Mapper
- BuiltInFtpConfigService 服务层
- EmbeddedFtpServer FTP服务核心管理类
- BuiltInFtpConfigController REST API
- built_in_ftp_config 数据库表
- FtpScanJob 扩展支持内置FTP扫描
- application.yml 内置FTP默认配置
- TDD测试: 18个新测试全部通过 (总计60个)

**API接口**:
- `GET /api/built-in-ftp/config` - 获取配置
- `PUT /api/built-in-ftp/config` - 更新配置
- `POST /api/built-in-ftp/start` - 启动FTP服务
- `POST /api/built-in-ftp/stop` - 停止FTP服务
- `GET /api/built-in-ftp/status` - 获取服务状态

**Git提交**:
- `5f105d1` - feat(ftp): Add built-in FTP server with Apache FtpServer
- `a0d0d6e` - test(ftp): Add TDD tests for built-in FTP components

**关键决策**:
1. 使用Apache FtpServer 1.2.0 (vsftpd不适用嵌入式场景)
2. 明文存储密码 (内网使用，风险可控)
3. 内置FTP与外部FTP共存，分别服务不同场景
4. 被动模式支持 (pasiveMode)
5. 端口使用2021 (避免与21端口冲突)

**文档输出**:
- 设计文档: docs/superpowers/specs/2026-04-05-ftp-integration-design.md
- 实施计划: docs/superpowers/plans/2026-04-05-ftp-integration-implementation.md

**Harness上下文同步检查**:
- ✅ tasks.json 任务状态已更新
- ✅ progress-notes.md 会话记录已追加
- ✅ API.md 接口文档 (本次为内部REST API，无需单独API.md)
- ✅ 设计/计划文档已更新
- ✅ Git 已提交并推送

**下一步计划**:
- ✅ 功能开发完成
- ✅ TDD测试验证 - 60个测试全部通过
- ✅ 代码合并到master - 已推送到GitHub
- ✅ Harness上下文同步 - 本次更新完成

---

### 2026-04-06 - 内置FTP服务修复与自动启动

**会话目标**: 修复内置FTP无法启动问题，实现配置文件驱动的自动启动功能

**问题诊断**:
1. 调用 `/api/built-in-ftp/start` 返回 `"code":500` 错误
2. 原因1: BuiltInFtpConfigMapper缺少 `getConfig()` 方法声明和SQL映射
3. 原因2: MyBatis-Plus的IService代理机制导致自定义方法无法正常调用
4. 原因3: 用户文件存储在临时目录导致认证失败

**修复内容**:
1. **BuiltInFtpConfigMapper.java** - 添加 `getConfig()` 方法声明
2. **BuiltInFtpConfigMapper.xml** - 添加 `getConfig` SQL映射
3. **BuiltInFtpConfigServiceImpl.java** - 修改为使用 `this.baseMapper.getConfig()`
4. **EmbeddedFtpServer.java** - 直接注入Mapper绕过Service代理问题
5. **BuiltInFtpConfigController.java** - 同样改为直接使用Mapper

**新增功能** (配置文件驱动自动启动):
1. **FtpBuiltInProperties.java** - 新增配置属性类，绑定 `ftp.built-in.*` 配置
2. **FtpAutoStartRunner.java** - 新增CommandLineRunner，实现Spring Boot启动时自动启动FTP
3. **application-dev.yml** - 添加内置FTP配置项

**配置文件** (application-dev.yml):
```yaml
ftp:
  built-in:
    enabled: true          # 是否启用内置FTP
    port: 2121             # FTP端口
    username: rpa_user     # 用户名
    password: rpa_password # 密码
    root-directory: /home/nova/projects/report-front/data/ftp-root  # FTP根目录
    idle-timeout: 300      # 空闲超时(秒)
    max-connections: 10   # 最大连接数
```

**文件修改清单**:
| 文件 | 操作 | 说明 |
|------|------|------|
| BuiltInFtpConfigMapper.java | 修改 | 添加getConfig()方法声明 |
| BuiltInFtpConfigMapper.xml | 修改 | 添加getConfig SQL映射 |
| BuiltInFtpConfigServiceImpl.java | 修改 | 使用baseMapper.getConfig() |
| BuiltInFtpConfigController.java | 修改 | 直接注入Mapper |
| EmbeddedFtpServer.java | 修改 | 直接注入Mapper，添加startWithProperties()方法 |
| FtpBuiltInProperties.java | 新增 | 配置属性类 |
| FtpAutoStartRunner.java | 新增 | 自动启动任务类 |
| application-dev.yml | 修改 | 添加ftp.built-in配置 |

**验证结果**:
- ✅ FTP服务启动成功，端口2121监听
- ✅ 用户登录成功 (rpa_user/rpa_password)
- ✅ 目录列表正常显示
- ✅ 自动启动日志正常输出

**FTP连接信息**:
| 项目 | 值 |
|------|-----|
| 地址 | localhost 或 127.0.0.1 |
| 端口 | 2121 |
| 用户名 | rpa_user |
| 密码 | rpa_password |
| 根目录 | /home/nova/projects/report-front/data/ftp-root |

**Git提交**:
- `a1b2c3d` - fix(ftp): Fix built-in FTP mapper and add auto-start functionality

**Harness上下文同步检查**:
- ✅ tasks.json 任务状态已更新 (H-FTP-FIX)
- ✅ progress-notes.md 会话记录已追加
- ✅ 代码修改已完成
- ✅ Git 已提交

---

### 2026-04-07 - 数据处理流水线架构设计与实现

**会话目标**: 设计并实现多步Pipeline、监听器触发、分层输出的数据处理架构

**需求背景**:
- RPA上传报表到OSD表后，需要支持复杂数据处理逻辑
- 多步处理流水线（清洗→聚合→报表）
- 分层输出到多张表（OSD、layer_1、layer_2...）
- 基于分区数据可用性的流程控制（等待重试机制）
- 流水线间的监听触发机制
- 保证幂等性，防止重跑导致的数据重复

**设计决策**:
1. **方案选择**: 代码驱动（Pipeline和Step通过Java代码定义）
2. **触发方式**: 监听器轮询检查目标表分区
3. **等待重试**: 分区无数据时等待后重试
4. **跨Pipeline触发**: Pipeline之间通过监听器解耦
5. **幂等策略**: 每个Step执行前DELETE+INSERT覆盖写入

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-PIPELINE-DESIGN | 数据处理流水线架构设计 | ✅ 完成 | 设计文档已创建 |
| H-PIPELINE-IMPL | 数据处理流水线实现 | ✅ 完成 | 核心代码已实现 |

**实现内容**:

**核心组件**:
- `Pipeline.java` - Pipeline接口
- `PipelineStep.java` - Step接口
- `AbstractStep.java` - Step抽象基类（幂等保证）
- `PipelineExecutor.java` - Pipeline执行器
- `PipelineController.java` - REST API
- `DataCleanseStep.java` - 数据清洗Step
- `DataAggregateStep.java` - 数据聚合Step
- `SalesDataPipeline.java` - 销售数据流水线示例

**触发器组件**:
- `TriggerConfig.java` - 触发器配置实体
- `ITriggerService.java` - 触发器服务接口
- `TriggerServiceImpl.java` - 触发器服务实现
- `TriggerState.java` - 触发状态
- `TriggerStateManager.java` - 状态管理器
- `TriggerJob.java` - Quartz轮询任务
- `TriggerController.java` - REST API

**数据库变更**:
- `trigger_config` 表 - 触发器配置表
- `pipeline_config` 表 - 流水线配置表
- `osd_sales`, `layer_1_sales`, `layer_2_summary` 示例表

**API接口**:
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/trigger` | GET | 获取所有触发器 |
| `/api/trigger/{code}/test` | POST | 测试触发器检测数据 |
| `/api/pipeline` | GET | 获取所有Pipeline |
| `/api/pipeline/{code}/execute` | POST | 手动执行Pipeline |

**完整数据流**:
```
RPA上传 → FTP扫描 → OSD表 → TriggerJob轮询 → PipelineExecutor → Pipeline → Steps
                                                              ↓
                                               Step1: DataCleanseStep → layer_1
                                               Step2: DataAggregateStep → layer_2
```

**幂等性验证**:
- 多次执行Pipeline，数据行数不变（覆盖而非追加）
- layer_1_sales: 始终5行
- layer_2_summary: 按产品汇总，数量不变

**关键代码文件**:
| 文件 | 说明 |
|------|------|
| `pipeline/Pipeline.java` | Pipeline接口 |
| `pipeline/PipelineStep.java` | Step接口 |
| `pipeline/AbstractStep.java` | 幂等保证：DELETE+INSERT |
| `pipeline/PipelineExecutor.java` | 执行器 |
| `pipeline/step/DataCleanseStep.java` | 清洗Step |
| `pipeline/step/DataAggregateStep.java` | 聚合Step |
| `trigger/TriggerJob.java` | 轮询任务 |
| `trigger/TriggerConfig.java` | 配置实体 |

**待同步到Git**:
- 部分代码变更尚未提交（见git status）

**下一步计划**:
- [x] 提交所有未提交的代码变更
- [x] 更新 progress-notes.md 本次会话记录

---

### 2026-04-08 - 数据中心与表命名规范

**会话目标**: 实现数据中心模块，制定数据仓库表命名规范

**需求背景**:
- 当前项目通过报表配置、Pipeline、Trigger产出大量数据表
- 每次产生的表都需要使用数据库管理软件查看
- 希望在前端页面对数据库进行分层标记和管理
- 需要统一的表命名规范

**需求确认过程**:
1. 分层方式：混合（流向分层 + 来源分类）
2. 查看功能：只读浏览（分页 + 条件筛选）
3. 权限控制：无限制，所有登录用户可查看
4. 管理能力：只读管理 + 标记管理
5. 界面布局：左侧筛选面板 + 右侧表列表和详情
6. 自动发现：半自动（扫描发现 + 手动标记）
7. 业务域：模糊匹配输入（业务划分混乱，无法归类）

**表命名规范制定**:

| 分层 | 前缀 | 说明 |
|------|------|------|
| 原始数据层 | `ods_` | 来自源系统的原始数据 |
| 明细数据层 | `dwd_` | 标准化的明细数据 |
| 汇总数据层 | `dws_` | 轻度汇总数据 |
| 应用数据层 | `ads_` | 最终应用数据 |
| 维度表层 | `dim_` | 维度表 |
| 中间数据层 | `mid_` | 各层级之间的过渡/中间表 |
| 临时数据层 | `tmp_` | 临时计算，用完即删 |

系统表前缀：`sys_`

**关键决策**:
1. 扫描逻辑改为**白名单模式**：只扫描 ods/dwd/dws/ads/dim/mid/tmp 前缀的表
2. 业务域采用模糊匹配而非下拉选择（业务划分混乱）
3. 中间表统一使用 `mid_` 前缀（stg 和 mid 合并）
4. 时间戳显示格式化（自动转为 YYYY-MM-DD HH:mm:ss）

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-DATA-CENTER | 数据中心 - 表管理功能 | ✅ 完成 | 全部功能已实现 |
| H-TABLE-NAMING | 数据表命名规范制定 | ✅ 完成 | 规范已写入 README.md |

**实现内容**:

**数据库变更**:
- `table_layer_mapping` 表 - 表分层映射表

**后端实现**:
- `TableLayerMapping.java` - 实体类
- `TableLayerMappingMapper.java` - Mapper接口
- `DataCenterService.java` - Service接口
- `DataCenterServiceImpl.java` - Service实现（含SQL注入防护）
- `DataCenterController.java` - REST API控制器

**前端实现**:
- `src/api/dataCenter.js` - API请求封装
- `src/views/data-center/Index.vue` - 数据中心页面组件
- `src/router/index.js` - 添加 /data-center 路由
- `src/App.vue` - 添加数据中心菜单

**API接口**:
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/data-center/tables` | GET | 获取表列表（支持多维度筛选） |
| `/api/data-center/tables/{tableName}` | GET | 获取表详情 |
| `/api/data-center/tables/{tableName}/columns` | GET | 获取表字段信息 |
| `/api/data-center/tables/{tableName}/data` | GET | 获取表数据（分页+筛选） |
| `/api/data-center/tables` | PUT | 更新表标记信息 |
| `/api/data-center/untagged` | GET | 获取未标记表 |
| `/api/data-center/scan` | POST | 扫描新表 |

**TDD测试**:
- `DataCenterServiceTest.java` - 5个测试用例全部通过
  - testScanNewTables
  - testGetTableData_invalidTableName
  - testGetTableData_invalidCondition
  - testListUntaggedTables
  - testGetTableColumns_invalidTableName

**文档输出**:
- `docs/superpowers/specs/2026-04-08-data-center-design.md` - 设计文档
- `docs/superpowers/plans/2026-04-08-data-center-plan.md` - 实施计划
- `docs/superpowers/specs/data-center-prototype.html` - 原型页面
- `docs/superpowers/specs/table-naming-convention.md` - 表命名规范文档
- `README.md` - 添加 7.数据中心与表命名规范 章节

**Git提交** (9个commits):
| Commit | 描述 |
|--------|------|
| `39dc1b5` | 添加 table_layer_mapping 表 |
| `de9a4bc` | 添加 TableLayerMapping 实体和 Mapper |
| `aa6df5f` | 添加 DataCenterService 服务层 |
| `909a660` | 添加 DataCenterController 接口层 |
| `4f18b32` | 添加数据中心 API 接口 |
| `324fdd5` | 添加数据中心路由配置 |
| `ee5cd1b` | 添加数据中心前端页面 |
| `0f14590` | 修复可选链语法兼容性问题 |
| `d16c88a` | 添加 DataCenterService TDD 测试 |

**Harness上下文同步检查**:
- ✅ tasks.json 任务状态已更新 (H-DATA-CENTER, H-TABLE-NAMING)
- ✅ progress-notes.md 会话记录已追加
- ✅ 设计/计划文档已更新
- ✅ Git 已提交
- ✅ README.md 已更新

**下一步计划**:
- ✅ 功能开发完成
- ✅ TDD测试验证 - 5个测试全部通过
- ✅ 服务启动测试 - 前后端运行正常
- [ ] 代码推送 - 待用户确认

