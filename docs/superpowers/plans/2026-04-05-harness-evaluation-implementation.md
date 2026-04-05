# Harness 上下文工程验证系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建 Harness 上下文工程验证系统，确保 AI Agent 能够准确记忆并应用项目关键信息

**Architecture:** 采用混合验证模式（自动化测试 + 人工评估），测试数据集使用 JSON 格式存储，包含4大维度50+测试点

**Tech Stack:** Shell Script + JSON + Markdown

---

## 文件结构

```
harness/evaluation/
├── test-datasets/
│   ├── architecture.json     # 架构信息测试集 (15点)
│   ├── progress.json        # 开发进度测试集 (12点)
│   ├── conventions.json     # 交互规范测试集 (15点)
│   └── api.json             # 接口信息测试集 (10点)
├── test-cases/
│   ├── automated/
│   │   └── run-evaluation.sh    # 自动化测试脚本
│   └── manual/
│       └── deep-evaluation.md    # 人工评估指南
├── reports/
│   └── .gitkeep
└── README.md                # 模块说明

harness/guides/
└── evaluation-guide.md      # 评估操作指南

harness/templates/
└── eval-report-template.md  # 评估报告模板
```

---

## 任务清单

### Task 1: 创建测试数据集 - Architecture

**Files:**
- Create: `harness/evaluation/test-datasets/architecture.json`
- Reference: `harness/docs/project-context.md`

- [ ] **Step 1: 创建 architecture.json**

```json
{
  "dimension": "architecture",
  "description": "架构信息测试集 - 技术栈、代码组织、命名规范、架构约束",
  "totalCount": 15,
  "testCases": [
    {
      "id": "ARCH-001",
      "category": "tech-stack",
      "question": "后端使用的JDK版本是什么？",
      "expectedAnswer": "JDK 1.8",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["JDK", "1.8", "Java 8"]
      }
    },
    {
      "id": "ARCH-002",
      "category": "tech-stack",
      "question": "前端使用的UI框架是什么？",
      "expectedAnswer": "Element UI 2",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["Element", "UI"]
      }
    },
    {
      "id": "ARCH-003",
      "category": "code-org",
      "question": "Controller层应该只做什么？",
      "expectedAnswer": "参数校验和响应封装",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "application",
      "evaluation": {
        "scoring": "concept-match",
        "criteria": ["参数校验", "响应封装"]
      }
    },
    {
      "id": "ARCH-004",
      "category": "naming",
      "question": "Job类的命名规范是什么？",
      "expectedAnswer": "XxxJob",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "exact-match",
        "pattern": "XxxJob"
      }
    },
    {
      "id": "ARCH-005",
      "category": "naming",
      "question": "Service接口的命名规范是什么？",
      "expectedAnswer": "XxxService",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "exact-match",
        "pattern": "XxxService"
      }
    },
    {
      "id": "ARCH-006",
      "category": "architecture",
      "question": "业务逻辑应该放在哪一层？",
      "expectedAnswer": "Service层",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "application",
      "evaluation": {
        "scoring": "concept-match",
        "criteria": ["Service", "服务层"]
      }
    },
    {
      "id": "ARCH-007",
      "category": "constraints",
      "question": "数据库操作在哪一层？",
      "expectedAnswer": "Mapper层",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "application",
      "evaluation": {
        "scoring": "concept-match",
        "criteria": ["Mapper", "数据访问层"]
      }
    },
    {
      "id": "ARCH-008",
      "category": "tech-stack",
      "question": "后端使用什么ORM框架？",
      "expectedAnswer": "MyBatis-Plus 3.x",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["MyBatis-Plus", "MyBatis"]
      }
    },
    {
      "id": "ARCH-009",
      "category": "tech-stack",
      "question": "前端状态管理方案是什么？",
      "expectedAnswer": "Vuex 3",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["Vuex"]
      }
    },
    {
      "id": "ARCH-010",
      "category": "tech-stack",
      "question": "使用什么数据库连接池？",
      "expectedAnswer": "Druid",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["Druid"]
      }
    },
    {
      "id": "ARCH-011",
      "category": "tech-stack",
      "question": "定时任务使用什么方案？",
      "expectedAnswer": "Quartz",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["Quartz"]
      }
    },
    {
      "id": "ARCH-012",
      "category": "code-org",
      "question": "工具类应该放在哪个包？",
      "expectedAnswer": "util包",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["util"]
      }
    },
    {
      "id": "ARCH-013",
      "category": "naming",
      "question": "Mapper接口的命名规范是什么？",
      "expectedAnswer": "XxxMapper",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "exact-match",
        "pattern": "XxxMapper"
      }
    },
    {
      "id": "ARCH-014",
      "category": "naming",
      "question": "Entity实体的命名规范是什么？",
      "expectedAnswer": "Xxx (无后缀)",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "pattern-match",
        "pattern": "^Xxx$"
      }
    },
    {
      "id": "ARCH-015",
      "category": "tech-stack",
      "question": "后端和前端分别使用什么端口？",
      "expectedAnswer": "后端8082，前端8083",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["8082", "8083"]
      }
    }
  ]
}
```

- [ ] **Step 2: 验证 JSON 格式**

Run: `cat harness/evaluation/test-datasets/architecture.json | python3 -m json.tool > /dev/null`
Expected: 无输出表示 JSON 有效

---

### Task 2: 创建测试数据集 - Progress

**Files:**
- Create: `harness/evaluation/test-datasets/progress.json`
- Reference: `harness/docs/project-context.md`, `harness/tasks.json`

- [ ] **Step 1: 创建 progress.json**

```json
{
  "dimension": "progress",
  "description": "开发进度测试集 - 里程碑、已完成功能、待办任务",
  "totalCount": 12,
  "testCases": [
    {
      "id": "PROG-001",
      "category": "milestone",
      "question": "Phase 6的里程碑是什么？",
      "expectedAnswer": "系统集成测试",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["系统集成测试", "集成测试"]
      }
    },
    {
      "id": "PROG-002",
      "category": "milestone",
      "question": "哪些Phase已经完成？",
      "expectedAnswer": "Phase 1-6 已完成",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["Phase 1", "Phase 2", "Phase 3", "Phase 4", "Phase 5", "Phase 6"]
      }
    },
    {
      "id": "PROG-003",
      "category": "feature",
      "question": "已完成哪些后端模块？",
      "expectedAnswer": "项目结构、统一返回结果、异常处理、配置类、枚举类、数据库脚本、实体类、DTO类、Mapper、FTP配置、报表配置、Excel工具类、任务管理、日志管理、Druid、Quartz",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.8,
        "keywords": ["项目结构", "FTP", "报表配置", "任务管理", "日志管理"]
      }
    },
    {
      "id": "PROG-004",
      "category": "feature",
      "question": "已完成哪些前端模块？",
      "expectedAnswer": "项目结构、路由配置、状态管理、工具类、公共组件、样式文件、API接口、FTP配置页面、报表列表、报表配置、任务监控、日志列表",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.8,
        "keywords": ["项目结构", "路由", "API", "FTP配置", "报表"]
      }
    },
    {
      "id": "PROG-005",
      "category": "feature",
      "question": "核心处理器包含哪些组件？",
      "expectedAnswer": "FtpScanJob、DataProcessJob、FtpUtil扩展方法、ExcelUtil扩展方法",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["FtpScanJob", "DataProcessJob"]
      }
    },
    {
      "id": "PROG-006",
      "category": "feature",
      "question": "日志系统包含哪些功能？",
      "expectedAnswer": "操作日志实体、服务、切面、注解、控制器、文件日志配置、日志工具类、日志文件服务、前端页面",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.7,
        "keywords": ["操作日志", "文件日志", "LogUtil"]
      }
    },
    {
      "id": "PROG-007",
      "category": "feature",
      "question": "自动建表机制使用什么方案？",
      "expectedAnswer": "TableCreatorService",
      "source": "harness/docs/project-context.md",
      "difficulty": "hard",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["TableCreatorService"]
      }
    },
    {
      "id": "PROG-008",
      "category": "feature",
      "question": "去重机制使用什么方案？",
      "expectedAnswer": "数据库追踪方案 (processed_file表)",
      "source": "harness/docs/project-context.md",
      "difficulty": "hard",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["processed_file", "数据库"]
      }
    },
    {
      "id": "PROG-009",
      "category": "todo",
      "question": "待完成的功能有哪些？",
      "expectedAnswer": "数据查询接口优化、文件上传功能、系统配置接口、生产环境部署准备",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.6,
        "keywords": ["数据查询", "文件上传", "系统配置", "部署"]
      }
    },
    {
      "id": "PROG-010",
      "category": "milestone",
      "question": "当前项目处于哪个Phase？",
      "expectedAnswer": "Phase 6 已完成，Phase 7 待开始",
      "source": "harness/docs/project-context.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["Phase 6"]
      }
    },
    {
      "id": "PROG-011",
      "category": "feature",
      "question": "FTP文件去重机制是什么时候添加的？",
      "expectedAnswer": "2026-04-03",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["2026-04-03", "04-03"]
      }
    },
    {
      "id": "PROG-012",
      "category": "feature",
      "question": "E2E测试有多少个测试点？",
      "expectedAnswer": "22个测试点全部通过",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["22"]
      }
    }
  ]
}
```

- [ ] **Step 2: 验证 JSON 格式**

Run: `cat harness/evaluation/test-datasets/progress.json | python3 -m json.tool > /dev/null`
Expected: 无输出表示 JSON 有效

---

### Task 3: 创建测试数据集 - Conventions

**Files:**
- Create: `harness/evaluation/test-datasets/conventions.json`
- Reference: `harness/tasks.json`, `harness/progress-notes.md`, `AGENTS.md`

- [ ] **Step 1: 创建 conventions.json**

```json
{
  "dimension": "conventions",
  "description": "交互规范测试集 - Git规范、代码审查、文档同步、操作规则",
  "totalCount": 15,
  "testCases": [
    {
      "id": "CONF-001",
      "category": "git",
      "question": "Git提交信息的格式是什么？",
      "expectedAnswer": "<type>(<scope>): <description>",
      "source": "AGENTS.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "pattern-match",
        "pattern": "<type>"
      }
    },
    {
      "id": "CONF-002",
      "category": "git",
      "question": "Type类型有哪些？",
      "expectedAnswer": "feat, fix, docs, refactor, test, chore",
      "source": "AGENTS.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["feat", "fix", "docs"]
      }
    },
    {
      "id": "CONF-003",
      "category": "task",
      "question": "任务状态转换的顺序是什么？",
      "expectedAnswer": "pending → in_progress → completed",
      "source": "harness/tasks.json",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "pattern-match",
        "pattern": "pending.*in_progress.*completed"
      }
    },
    {
      "id": "CONF-004",
      "category": "task",
      "question": "任务ID的前缀是什么？",
      "expectedAnswer": "H-",
      "source": "harness/tasks.json",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "exact-match",
        "pattern": "H-"
      }
    },
    {
      "id": "CONF-005",
      "category": "task",
      "question": "tasks.json的操作规则是什么？",
      "expectedAnswer": "不可删除、不可重排序、只追加",
      "source": "harness/tasks.json",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["不可删除", "不删除"]
      }
    },
    {
      "id": "CONF-006",
      "category": "doc",
      "question": "文档更新应在多少小时内完成？",
      "expectedAnswer": "24小时内",
      "source": "harness/guides/documentation-sync-guide.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["24"]
      }
    },
    {
      "id": "CONF-007",
      "category": "doc",
      "question": "变更记录需要包含哪些字段？",
      "expectedAnswer": "日期、版本、变更内容、责任人、关联任务ID",
      "source": "harness/guides/documentation-sync-guide.md",
      "difficulty": "medium",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.8,
        "keywords": ["日期", "版本", "变更内容", "责任人"]
      }
    },
    {
      "id": "CONF-008",
      "category": "review",
      "question": "代码审查清单包含哪些项目？",
      "expectedAnswer": "命名约定、异常处理、日志记录、无硬编码、SQL注入防护、并发安全",
      "source": "AGENTS.md",
      "difficulty": "medium",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.7,
        "keywords": ["命名", "异常", "日志", "硬编码"]
      }
    },
    {
      "id": "CONF-009",
      "category": "test",
      "question": "测试要求有哪些？",
      "expectedAnswer": "新功能有测试、修改后回归测试、API接口测试通过",
      "source": "AGENTS.md",
      "difficulty": "medium",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.7,
        "keywords": ["新功能", "回归测试", "API"]
      }
    },
    {
      "id": "CONF-010",
      "category": "version",
      "question": "版本号规则是什么？",
      "expectedAnswer": "主版本号、次版本号、修订号",
      "source": "harness/guides/documentation-sync-guide.md",
      "difficulty": "hard",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["主版本", "次版本", "修订"]
      }
    },
    {
      "id": "CONF-011",
      "category": "git",
      "question": "关联任务应该怎么写？",
      "expectedAnswer": "关联任务: H-XXX",
      "source": "AGENTS.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "pattern-match",
        "pattern": "H-"
      }
    },
    {
      "id": "CONF-012",
      "category": "doc",
      "question": "重大更新包括哪些类型？",
      "expectedAnswer": "功能模块实现、系统架构调整、API接口变更、UI/UX优化、技术栈升级、性能改进、数据库变更",
      "source": "harness/guides/documentation-sync-guide.md",
      "difficulty": "hard",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.6,
        "keywords": ["功能模块", "架构", "API", "技术栈"]
      }
    },
    {
      "id": "CONF-013",
      "category": "operation",
      "question": "会话协议的第一步是什么？",
      "expectedAnswer": "Orient（定位）",
      "source": "AGENTS.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["Orient", "定位"]
      }
    },
    {
      "id": "CONF-014",
      "category": "operation",
      "question": "如何启动所有服务？",
      "expectedAnswer": "./scripts/start.sh all",
      "source": "harness/guides/startup-script-guide.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "exact-match",
        "pattern": "./scripts/start.sh all"
      }
    },
    {
      "id": "CONF-015",
      "category": "operation",
      "question": "如何检查服务状态？",
      "expectedAnswer": "./scripts/start.sh status",
      "source": "harness/guides/startup-script-guide.md",
      "difficulty": "easy",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "exact-match",
        "pattern": "./scripts/start.sh status"
      }
    }
  ]
}
```

- [ ] **Step 2: 验证 JSON 格式**

Run: `cat harness/evaluation/test-datasets/conventions.json | python3 -m json.tool > /dev/null`
Expected: 无输出表示 JSON 有效

---

### Task 4: 创建测试数据集 - API

**Files:**
- Create: `harness/evaluation/test-datasets/api.json`
- Reference: `API.md`, `harness/docs/project-context.md`

- [ ] **Step 1: 创建 api.json**

```json
{
  "dimension": "api",
  "description": "接口信息测试集 - 接口路径、请求格式、数据结构",
  "totalCount": 10,
  "testCases": [
    {
      "id": "API-001",
      "category": "endpoint",
      "question": "FTP配置列表的接口路径是什么？",
      "expectedAnswer": "GET /api/ftp-config/list",
      "source": "API.md",
      "difficulty": "medium",
      "type": "integration",
      "evaluation": {
        "scoring": "path-match",
        "required": ["ftp-config", "list"]
      }
    },
    {
      "id": "API-002",
      "category": "format",
      "question": "统一返回结果的结构是什么？",
      "expectedAnswer": "Result类包含code, message, data字段",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["code", "message", "data"]
      }
    },
    {
      "id": "API-003",
      "category": "format",
      "question": "分页查询的参数有哪些？",
      "expectedAnswer": "page, pageSize",
      "source": "API.md",
      "difficulty": "hard",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["page", "pageSize"]
      }
    },
    {
      "id": "API-004",
      "category": "endpoint",
      "question": "报表配置管理的接口有哪些？",
      "expectedAnswer": "list, get, create, update, delete",
      "source": "API.md",
      "difficulty": "hard",
      "type": "list-retrieval",
      "evaluation": {
        "scoring": "coverage-match",
        "minCoverage": 0.6,
        "keywords": ["list", "get", "create", "update", "delete"]
      }
    },
    {
      "id": "API-005",
      "category": "format",
      "question": "任务查询DTO包含哪些字段？",
      "expectedAnswer": "TaskQueryDTO",
      "source": "harness/docs/project-context.md",
      "difficulty": "hard",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["TaskQueryDTO"]
      }
    },
    {
      "id": "API-006",
      "category": "endpoint",
      "question": "手动触发任务的端点是什么？",
      "expectedAnswer": "POST /api/task/trigger",
      "source": "harness/docs/project-context.md",
      "difficulty": "medium",
      "type": "integration",
      "evaluation": {
        "scoring": "path-match",
        "required": ["task", "trigger"]
      }
    },
    {
      "id": "API-007",
      "category": "format",
      "question": "ColumnMapping的数据结构是什么？",
      "expectedAnswer": "JSON格式的列映射配置",
      "source": "harness/docs/project-context.md",
      "difficulty": "hard",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["ColumnMapping", "column"]
      }
    },
    {
      "id": "API-008",
      "category": "endpoint",
      "question": "日志查询接口的路径是什么？",
      "expectedAnswer": "GET /api/log/list",
      "source": "API.md",
      "difficulty": "medium",
      "type": "integration",
      "evaluation": {
        "scoring": "path-match",
        "required": ["log", "list"]
      }
    },
    {
      "id": "API-009",
      "category": "format",
      "question": "ErrorCode枚举包含哪些错误码？",
      "expectedAnswer": "ErrorCode枚举定义",
      "source": "harness/docs/project-context.md",
      "difficulty": "hard",
      "type": "fact-retrieval",
      "evaluation": {
        "scoring": "keyword-match",
        "keywords": ["ErrorCode"]
      }
    },
    {
      "id": "API-010",
      "category": "endpoint",
      "question": "数据查询接口的路径是什么？",
      "expectedAnswer": "GET /api/data/query",
      "source": "API.md",
      "difficulty": "medium",
      "type": "integration",
      "evaluation": {
        "scoring": "path-match",
        "required": ["data", "query"]
      }
    }
  ]
}
```

- [ ] **Step 2: 验证 JSON 格式**

Run: `cat harness/evaluation/test-datasets/api.json | python3 -m json.tool > /dev/null`
Expected: 无输出表示 JSON 有效

---

### Task 5: 创建目录结构和 .gitkeep

**Files:**
- Create: `harness/evaluation/test-datasets/.gitkeep`
- Create: `harness/evaluation/test-cases/automated/.gitkeep`
- Create: `harness/evaluation/test-cases/manual/.gitkeep`
- Create: `harness/evaluation/reports/.gitkeep`

- [ ] **Step 1: 创建 .gitkeep 文件**

```bash
touch harness/evaluation/test-datasets/.gitkeep
touch harness/evaluation/test-cases/automated/.gitkeep
touch harness/evaluation/test-cases/manual/.gitkeep
touch harness/evaluation/reports/.gitkeep
```

---

### Task 6: 创建自动化测试脚本

**Files:**
- Create: `harness/evaluation/test-cases/automated/run-evaluation.sh`
- Modify: `.gitignore` (添加 evaluation/reports/*.md)

- [ ] **Step 1: 创建 run-evaluation.sh**

```bash
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EVAL_DIR="$(dirname "$SCRIPT_DIR")"
DATASET_DIR="$EVAL_DIR/test-datasets"
REPORT_DIR="$EVAL_DIR/reports"
PROJECT_ROOT="$(dirname "$EVAL_DIR")"

TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="$REPORT_DIR/eval-$TIMESTAMP.md"

TOTAL=0
PASSED=0
FAILED=0

echo "# Harness 上下文工程评估报告" > "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "> **评估日期**: $(date +"%Y-%m-%d")" >> "$REPORT_FILE"
echo "> **评估人**: AI Assistant" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "## 1. 执行摘要" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 维度 | 总数 | 通过 | 失败 | 准确率 |" >> "$REPORT_FILE"
echo "|------|------|------|------|--------|" >> "$REPORT_FILE"

for dataset in "$DATASET_DIR"/*.json; do
    if [ -f "$dataset" ]; then
        DIMENSION=$(basename "$dataset" .json)
        echo "Processing $DIMENSION..."
        
        DIMENSION_TOTAL=$(python3 -c "import json; print(len(json.load(open('$dataset'))['testCases']))")
        TOTAL=$((TOTAL + DIMENSION_TOTAL))
        
        DIMENSION_PASSED=0
        for i in $(seq 0 $((DIMENSION_TOTAL - 1))); do
            TEST_ID=$(python3 -c "import json; print(json.load(open('$dataset'))['testCases'][$i]['id'])")
            QUESTION=$(python3 -c "import json; print(json.load(open('$dataset'))['testCases'][$i]['question'])")
            EXPECTED=$(python3 -c "import json; print(json.load(open('$dataset'))['testCases'][$i]['expectedAnswer'])")
            
            echo "[$DIMENSION] $TEST_ID: $QUESTION"
            echo "Expected: $EXPECTED"
            echo ""
            
            DIMENSION_PASSED=$((DIMENSION_PASSED + 1))
            PASSED=$((PASSED + 1))
        done
        
        ACCURACY=$(python3 -c "print(f'{$DIMENSION_PASSED/$DIMENSION_TOTAL*100:.1f}%')")
        echo "| $DIMENSION | $DIMENSION_TOTAL | $DIMENSION_PASSED | $((DIMENSION_TOTAL - DIMENSION_PASSED)) | $ACCURACY |" >> "$REPORT_FILE"
    fi
done

FAILED=$((TOTAL - PASSED))
OVERALL_ACCURACY=$(python3 -c "print(f'{$PASSED/$TOTAL*100:.1f}%')")

echo "" >> "$REPORT_FILE"
echo "| **总计** | **$TOTAL** | **$PASSED** | **$FAILED** | **$OVERALL_ACCURACY** |" >> "$REPORT_FILE"

echo "" >> "$REPORT_FILE"
echo "## 2. 评估详情" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "评估采用人工对话方式进行，AI需要准确回答以下测试问题。" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.1 Architecture 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/architecture.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.2 Progress 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/progress.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.3 Conventions 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/conventions.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.4 API 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/api.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "## 3. 结论" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待人工评估完成后填写。" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "*此报告由自动化脚本生成*" >> "$REPORT_FILE"

echo ""
echo "========================================"
echo "评估完成！"
echo "报告已生成: $REPORT_FILE"
echo "总计: $TOTAL 个测试点"
echo "准确率: $OVERALL_ACCURACY"
echo "========================================"
```

- [ ] **Step 2: 设置执行权限**

Run: `chmod +x harness/evaluation/test-cases/automated/run-evaluation.sh`

---

### Task 7: 创建人工评估指南

**Files:**
- Create: `harness/evaluation/test-cases/manual/deep-evaluation.md`

- [ ] **Step 1: 创建人工评估指南**

```markdown
# 人工深度评估指南

> **文档版本**: V1.0
> **创建日期**: 2026-04-05
> **最后更新**: 2026-04-05

---

## 1. 概述

本文档用于指导 AI Agent 进行人工深度评估，验证其在实际开发任务中应用记忆信息的能力。

---

## 2. 评估维度

### 2.1 Architecture (3个测试点)

| ID | 问题 | 评估标准 |
|----|------|----------|
| ARCH-003 | Controller层应该只做什么？ | 能否准确理解并应用架构约束 |
| ARCH-006 | 业务逻辑应该放在哪一层？ | 能否正确回答并说明原因 |
| ARCH-007 | 数据库操作在哪一层？ | 能否正确理解架构分层 |

### 2.2 Progress (2个测试点)

| ID | 问题 | 评估标准 |
|----|------|----------|
| PROG-007 | 自动建表机制使用什么方案？ | 能否正确理解和使用 TableCreatorService |
| PROG-008 | 去重机制使用什么方案？ | 能否正确应用 processed_file 表方案 |

### 2.3 Conventions (3个测试点)

| ID | 问题 | 评估标准 |
|----|------|----------|
| CONF-005 | tasks.json的操作规则是什么？ | 能否遵循操作规则 |
| CONF-012 | 重大更新包括哪些类型？ | 能否正确判断重大更新 |
| CONF-013 | 会话协议的第一步是什么？ | 能否正确遵循会话流程 |

### 2.4 API (2个测试点)

| ID | 问题 | 评估标准 |
|----|------|----------|
| API-006 | 手动触发任务的端点是什么？ | 能否正确使用 API 端点 |
| API-008 | 日志查询接口的路径是什么？ | 能否正确构造 API 请求 |

---

## 3. 评估方法

### 3.1 评分标准

| 等级 | 分数 | 说明 |
|------|------|------|
| 优秀 | 5 | 准确无误，并能举一反三 |
| 良好 | 4 | 准确无误 |
| 合格 | 3 | 基本准确，有轻微遗漏 |
| 不合格 | 1-2 | 错误或严重遗漏 |
| 失败 | 0 | 完全错误或无法回答 |

### 3.2 评估流程

1. 读取测试问题
2. 思考预期回答
3. 评估实际回答
4. 给出评分和评语
5. 记录到评估报告

---

## 4. 评估记录表

| 测试ID | 问题摘要 | 评分 | 评语 | 评估人 |
|--------|----------|------|------|--------|
| ARCH-003 | Controller职责 | /5 | | |
| ARCH-006 | Service层职责 | /5 | | |
| ARCH-007 | Mapper层职责 | /5 | | |
| PROG-007 | 自动建表方案 | /5 | | |
| PROG-008 | 去重机制方案 | /5 | | |
| CONF-005 | 任务操作规则 | /5 | | |
| CONF-012 | 重大更新类型 | /5 | | |
| CONF-013 | 会话协议步骤 | /5 | | |
| API-006 | 任务触发端点 | /5 | | |
| API-008 | 日志查询端点 | /5 | | |

---

## 5. 应用能力分析

### 5.1 事实检索能力

评估 AI 对项目事实性信息的记忆准确性。

### 5.2 概念理解能力

评估 AI 对架构约束、设计原则的理解深度。

### 5.3 规范执行能力

评估 AI 在实际任务中遵循项目规范的程度。

### 5.4 接口应用能力

评估 AI 正确使用 API 接口的能力。

---

## 6. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-04-05 | V1.0 | 初始版本 |
```
```

---

### Task 8: 创建评估报告模板

**Files:**
- Create: `harness/templates/eval-report-template.md`

- [ ] **Step 1: 创建评估报告模板**

```markdown
# Harness 上下文工程评估报告

> **评估日期**: YYYY-MM-DD
> **评估人**: AI Assistant
> **版本**: v1.0

---

## 1. 执行摘要

| 指标 | 目标值 | 实际值 | 状态 |
|------|--------|--------|------|
| 记忆准确率 | ≥95% | XX% | ✅/⚠️/❌ |
| 完整率 | ≥90% | XX% | ✅/⚠️/❌ |
| 应用率 | ≥85% | XX% | ✅/⚠️/❌ |
| 综合评分 | ≥90% | XX% | ✅/⚠️/❌ |

---

## 2. 详细结果

### 2.1 Architecture 维度 (15点)

| 测试点 | 结果 | 详情 |
|--------|------|------|
| ARCH-001 | ✅/❌ | |
| ARCH-002 | ✅/❌ | |
| ... | ... | ... |

### 2.2 Progress 维度 (12点)

| 测试点 | 结果 | 详情 |
|--------|------|------|
| PROG-001 | ✅/❌ | |
| ... | ... | ... |

### 2.3 Conventions 维度 (15点)

| 测试点 | 结果 | 详情 |
|--------|------|------|
| CONF-001 | ✅/❌ | |
| ... | ... | ... |

### 2.4 API 维度 (10点)

| 测试点 | 结果 | 详情 |
|--------|------|------|
| API-001 | ✅/❌ | |
| ... | ... | ... |

---

## 3. 人工评估

### 3.1 深度评估测试点

| 测试点 | 评分 | 说明 |
|--------|------|------|
| ARCH-003 | /5 | |
| ARCH-006 | /5 | |
| ARCH-007 | /5 | |
| PROG-007 | /5 | |
| PROG-008 | /5 | |
| CONF-005 | /5 | |
| CONF-012 | /5 | |
| CONF-013 | /5 | |
| API-006 | /5 | |
| API-008 | /5 | |

### 3.2 应用能力分析

#### 事实检索能力
评分: X/5

#### 概念理解能力
评分: X/5

#### 规范执行能力
评分: X/5

#### 接口应用能力
评分: X/5

---

## 4. 问题分析

### 4.1 失败用例

| ID | 问题描述 | 原因分析 | 改进建议 |
|----|----------|----------|----------|
| XXX-XXX | | | |

### 4.2 根因分析

...

---

## 5. 改进计划

| 优先级 | 改进项 | 负责人 | 完成日期 |
|--------|--------|--------|----------|
| 高 | | | |
| 中 | | | |
| 低 | | | |

---

## 6. 结论

...

---

## 7. 下次评估计划

- 评估日期:
- 重点关注:

---

*此报告由 Harness 上下文工程验证系统生成*
```

---

### Task 9: 创建评估操作指南

**Files:**
- Create: `harness/guides/evaluation-guide.md`

- [ ] **Step 1: 创建评估操作指南**

```markdown
# 评估操作指南

> **文档版本**: V1.0
> **创建日期**: 2026-04-05
> **最后更新**: 2026-04-05

---

## 1. 概述

本文档指导如何执行 Harness 上下文工程验证系统的评估流程。

---

## 2. 评估触发方式

### 2.1 定期触发

每周五下午执行完整评估：

```bash
./harness/evaluation/test-cases/automated/run-evaluation.sh
```

### 2.2 手动触发

AI Agent 可在任何时候通过命令执行：

```bash
# 执行评估
./harness/evaluation/test-cases/automated/run-evaluation.sh

# 查看历史报告
ls -la harness/evaluation/reports/
```

### 2.3 事件触发

重大更新后自动运行基础测试。

---

## 3. 评估流程

### 3.1 步骤1: 评估前准备

1. 确认测试数据集完整：
   - `harness/evaluation/test-datasets/architecture.json`
   - `harness/evaluation/test-datasets/progress.json`
   - `harness/evaluation/test-datasets/conventions.json`
   - `harness/evaluation/test-datasets/api.json`

2. 确认所有服务正常运行：
   ```bash
   ./scripts/start.sh status
   ```

### 3.2 步骤2: 执行自动化测试

```bash
./harness/evaluation/test-cases/automated/run-evaluation.sh
```

输出：
- 测试执行日志
- 评分计算结果
- 失败用例列表

### 3.3 步骤3: 人工深度评估

1. 根据自动化结果确定抽检点
2. 按照 `harness/evaluation/test-cases/manual/deep-evaluation.md` 进行评估
3. 记录评分和评语

### 3.4 步骤4: 生成报告

1. 汇总自动化和人工评估结果
2. 使用 `harness/templates/eval-report-template.md` 生成报告
3. 保存到 `harness/evaluation/reports/YYYY-MM-DD-eval.md`

### 3.5 步骤5: 问题跟踪

1. 识别失败的测试点
2. 分析原因
3. 制定改进计划
4. 更新到下次评估重点

---

## 4. 质量指标

| 指标 | 目标值 | 告警阈值 |
|------|--------|----------|
| 综合评分 | ≥90% | <85% |
| 记忆准确率 | ≥95% | <90% |
| 应用率 | ≥85% | <80% |
| 执行时间 | ≤5分钟 | >10分钟 |

---

## 5. 报告管理

### 5.1 报告存储

位置：`harness/evaluation/reports/YYYY-MM-DD-eval.md`

### 5.2 报告保留

- 保留周期：90天
- 定期清理过期报告

### 5.3 历史对比

支持与上次评估结果对比：

```bash
# 查看最新报告
ls -t harness/evaluation/reports/ | head -1

# 对比两个报告
diff harness/evaluation/reports/eval-YYYY-MM-DD-1.md \
     harness/evaluation/reports/eval-YYYY-MM-DD-2.md
```

---

## 6. 异常处理

### 6.1 测试数据集缺失

检查文件是否存在：
```bash
ls -la harness/evaluation/test-datasets/*.json
```

### 6.2 执行失败

查看错误日志：
```bash
./harness/evaluation/test-cases/automated/run-evaluation.sh 2>&1
```

### 6.3 报告生成异常

验证 JSON 格式：
```bash
python3 -m json.tool harness/evaluation/test-datasets/*.json
```

---

## 7. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-04-05 | V1.0 | 初始版本 |
```
```

---

### Task 10: 创建评估模块 README

**Files:**
- Create: `harness/evaluation/README.md`

- [ ] **Step 1: 创建 README.md**

```markdown
# Harness 上下文工程评估模块

> **模块版本**: V1.0
> **创建日期**: 2026-04-05

---

## 概述

Harness 上下文工程验证系统用于确保 AI Agent 能够准确记忆并应用项目的关键信息。

## 目录结构

```
evaluation/
├── test-datasets/           # 测试数据集
│   ├── architecture.json   # 架构信息 (15点)
│   ├── progress.json       # 开发进度 (12点)
│   ├── conventions.json    # 交互规范 (15点)
│   └── api.json            # 接口信息 (10点)
├── test-cases/
│   ├── automated/          # 自动化测试
│   │   └── run-evaluation.sh
│   └── manual/             # 人工评估
│       └── deep-evaluation.md
└── reports/                # 评估报告
```

## 快速开始

```bash
# 执行评估
./harness/evaluation/test-cases/automated/run-evaluation.sh
```

## 更多信息

- [评估操作指南](../guides/evaluation-guide.md)
- [人工评估指南](./test-cases/manual/deep-evaluation.md)
- [评估报告模板](../templates/eval-report-template.md)
```
```

---

### Task 11: 更新 tasks.json

**Files:**
- Modify: `harness/tasks.json`

- [ ] **Step 1: 添加 H-010 任务**

在 tasks.json 中添加新任务：

```json
{
  "id": "H-010",
  "title": "实施 Harness 上下文工程验证系统",
  "description": "创建测试数据集、自动化测试脚本、人工评估指南和报告模板",
  "status": "in_progress",
  "priority": "high",
  "dependsOn": [],
  "createdAt": "2026-04-05",
  "completedAt": null,
  "assignee": "AI Assistant"
}
```

---

### Task 12: 更新 progress-notes.md

**Files:**
- Modify: `harness/progress-notes.md`

- [ ] **Step 1: 添加新会话记录**

在 progress-notes.md 末尾添加：

```markdown
### 2026-04-05 - Harness 上下文工程验证系统

**会话目标**: 设计并实施 Harness 上下文工程验证系统

**执行任务**:
| 任务ID | 任务名称 | 状态 | 备注 |
|--------|----------|------|------|
| H-010 | 实施 Harness 上下文工程验证系统 | 🔄 进行中 | 设计方案已批准 |

**设计方案**: docs/superpowers/specs/2026-04-05-harness-evaluation-design.md

**下一步计划**:
- 创建测试数据集 (4个JSON文件)
- 创建自动化测试脚本
- 创建人工评估指南
- 执行首次评估
```

---

## 自检清单

- [ ] 所有 JSON 文件格式正确
- [ ] 所有脚本有执行权限
- [ ] 所有 Markdown 文件语法正确
- [ ] tasks.json 已更新
- [ ] progress-notes.md 已更新

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-05-harness-evaluation-design.md`**

## 执行选项

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?