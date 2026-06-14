<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)">
    <img alt="AdaWing" src="https://img.shields.io/badge/AdaWing-v2.0-6366f1?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cGF0aCBkPSJNMTIgMkwyIDIybDEwLTQgMTAgNHoiIGZpbGw9IndoaXRlIi8+PC9zdmc+" />
  </picture>
</p>

<p align="center">
  <em>一片属于自己的天空 · A sky of one's own</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-3b82f6?style=flat&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.0-6db33f?style=flat&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Vue-3.2-4fc08d?style=flat&logo=vuedotjs&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479a1?style=flat&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/MCP-Streamable%20HTTP-6366f1?style=flat" />
  <img src="https://img.shields.io/badge/license-MIT-green?style=flat" />
</p>

---

## ✦ AdaWing 是什么

> 从项目立项的那一刻起，我就清楚地知道，我要建的不是一个网站，而是一片属于自己的天空。
>
> **Ada**，是我的名字。**wing**，是翅膀，是自由。**Adawing**，就是属于我的自由。

AdaWing 是一个**深具个人色彩但在工程上毫不妥协的博客平台**，由单人全栈从零构建至生产环境。它不仅是一个内容管理系统——更是一个**约束工程化的人机协作环境**：人类和 AI Agent 在同一套审核流水线下共同创作内容。

它特别在哪里：

- **你来写。** 全功能 Markdown 编辑器（Vditor），标签管理（Levenshtein 去重），阅读量统计，访客端/管理端双布局设计。
- **AI Agent 也能写。** AdaWing 内置一个 [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 服务器，Claude Code / OpenClaw / Codex 等编程 Agent 可以直接向审核队列推送文章草稿。服务器动态下发内容规则——Agent 必须像人类一样通过编辑审核才能正式发布。
- **一切经由审核。** 策略模式驱动的统一审核中心，文章和留言走审批流。AI 生成内容永不自动发布。
- **资源是一等公民。** 上传文件按池分类（`AVATAR` / `ARTICLE` / `EMOJI`），引用计数追踪，孤儿资源定时垃圾回收。

> 📖 [English version README.md](./README.md)

---

## ✦ 架构

```
┌─────────────────────────────────────────────────────────┐
│                     blog-boot                            │
│            （入口 · CORS · 事件总线）                      │
├──────────┬──────────┬──────────┬──────────┬─────────────┤
│ blog-zoom│ blog-    │ blog-    │ agent-   │  adawing-ui │
│ 内容域   │ system  │ resource │ mcp      │  Vue 3 SPA  │
│          │ 系统域   │ 资源域   │ 服务器    │             │
├──────────┴──────────┴──────────┴──────────┴─────────────┤
│                    blog-common                           │
│        （Result · PageResult · JwtUtil · Exceptions）     │
└─────────────────────────────────────────────────────────┘
```

**自底向上的层叠构建策略。** 模块依赖严格单向——`blog-common` → 共享域 → boot 装配。**禁止跨模块注入 Mapper**，模块间通过 Spring Event 通信。

```
blog-boot ──┬── blog-common           （compile scope，无 Spring Boot Starter）
            ├── blog-resource          resource-core · oss · cleaner
            ├── blog-zoom              zoom-shared · tag · article · note · message
            ├── blog-system            system-auth · config · profile · review
            └── agent-mcp              MCP 服务器（仅依赖 blog-common 防腐层接口）
```

---

## ✦ 功能

### 📝 文章管理
- **状态机：** `DRAFT` → `PENDING_REVIEW` → `PUBLISHED`（被拒回退至 `DRAFT`）
- **来源追踪：** 原创 vs. AI Agent 生成，记录来源 Agent 名称
- **封面图：** OSS 资源引用 + 引用计数
- **阅读量：** Caffeine 内存累加，每 5 分钟批量刷入 MySQL
- **置顶与隐藏：** 可独立控制

### 🏷 标签系统
- 仅管理端可创建，前端禁止 `allow-create`
- 新建标签时 Levenshtein 编辑距离去重
- 支持标签合并及文章重分配
- 按关联文章数排序

### 📋 统一审核中心
- 策略模式分发：`ArticleReviewStrategy` / `MessageReviewStrategy`
- 原创文章免审直发；Agent 生成内容 + 访客留言 **必须通过审核**
- 审核结果通过 Spring Event 通知内容模块，零直接跨模块调用

### 💬 留言板
- 昵称 + 邮箱 + 随机头像 + 内容
- XSS 过滤（`script`、`iframe`、`javascript:`、`onXXX=`）
- 管理员回复通过 SMTP 发送 HTML 邮件通知访客
- 点赞限流（Caffeine 2 秒锁）

### 🗂 资源管理
- 池化分类：`AVATAR` · `ARTICLE` · `EMOJI` · `MISC`
- 通过 `resource_reference` 关联表实现引用计数
- 每日凌晨 3:00 定时清理 7 天前标记的孤儿资源
- 阿里云 OSS 后端 + 代理下载端点（解决跨域和 ACL 问题）

### 🔐 认证
- 管理端：JWT（BCrypt 密码，7 天有效期）
- MCP：API Key（SHA-256 哈希，创建时仅展示一次明文）

### 📬 邮件通知
- Jakarta Mail + SMTP 发送 HTML 邮件（QQ 邮箱）
- 留言审核通过/拒绝时触发

### 📊 数据看板
- 文章 / 留言 / 动态 / 待审核 数量一目了然
- `GET /api/v2/system/config/dashboard`

### ✨ 前端
- **东方美学设计** — 无重度 UI 框架，手写 CSS
- **暗色模式** 自动跟随 `prefers-color-scheme`
- **GSAP 滚动动画** — 错位渐现、打字机 Hero 文字
- **阅读进度条** + **悬浮导航**
- **双布局完全隔离** — 访客端和管理端零样式渗透

---

## ✦ MCP Server（Agent 集成）

AdaWing 同时也是 **AI 编程 Agent 的内容目的地**。MCP 服务器监听 `POST /mcp`，采用 JSON-RPC over Streamable-HTTP 协议。

### 为什么重要

大多数个人博客是人写人看的封闭系统。AdaWing 将 AI Agent 视为**内容贡献者**——它们可以自主调研、起草、提交文章，但最终的编辑决定权始终属于人类。

### 提供的工具

| 工具 | 用途 |
|------|------|
| `get_content_rules` | 动态获取内容规范（标题长度、标签约束、风格偏好） |
| `create_article_draft` | 提交待审核草稿（标题 + Markdown 正文 + 标签） |
| `search_articles` | 按关键词搜索已有文章（避免重复提交） |
| `get_article` | 按 ID 获取文章全文 |

### 支持的 AI 客户端

| 客户端 | 协议 | 接入方式 |
|--------|------|----------|
| **Claude Code** | Streamable-HTTP | `claude mcp add adawing https://<host>/mcp` |
| **OpenClaw** | Streamable-HTTP | OpenClaw MCP 注册中心注册 |
| **Codex** | Streamable-HTTP | Codex MCP 服务器列表添加 |

三者共用同一套 `X-MCP-Key` Header 认证，暴露完全相同的 4 个 Tool。

### Claude Code 配置示例

```json
{
  "mcpServers": {
    "adawing": {
      "type": "http",
      "url": "https://adawing.example.com/mcp",
      "headers": {
        "X-MCP-Key": "${ADAWING_MCP_KEY}"
      }
    }
  }
}
```

更多模板见 `docs/examples/mcp-configs/`（Claude Code / OpenClaw / Codex）。

---

## ✦ 快速开始

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| pnpm | 8+ |
| MySQL | 8.0+（生产环境） |

### 1. 建库

```sql
CREATE DATABASE IF NOT EXISTS adawing
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 后端启动

```bash
cd Blog

# 开发模式（H2 内存库，无需 MySQL）
mvn clean install -DskipTests
java -jar blog-boot/target/blog-boot-1.0-SNAPSHOT.jar

# 或连接本地 MySQL
cp blog-boot/src/main/resources/application-local.yaml application-prod.yaml
# 修改数据库连接信息后：
java -jar blog-boot/target/blog-boot-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

默认端口：**8080**
表结构通过 `spring.sql.init` → `db/schema.sql` 自动初始化。

### 3. 前端启动

```bash
cd adawing-ui

pnpm install
pnpm dev
```

Vite 自动代理 `/api/v2` → `http://localhost:8080`。

### 4. 访问

| 入口 | URL |
|------|-----|
| **博客首页** | `http://localhost:5173/` |
| **管理端登录** | `http://localhost:5173/v2/ren/admin/login` |
| **管理后台** | `http://localhost:5173/v2/ren/admin` |
| **MCP 端点** | `http://localhost:8080/mcp`（POST） |

---

## ✦ 项目地图

```
adawing/
├── Blog/                          # Maven 多模块后端
│   ├── blog-common/               #   共享 DTO、工具类、异常
│   ├── blog-resource/             #   resource-core · oss · cleaner
│   ├── blog-zoom/                 #   tag · article · note · message
│   ├── blog-system/               #   auth · config · profile · review
│   └── blog-boot/                 #   启动入口 · Controller · 配置
├── agent-mcp/                     # 独立 MCP 服务器
├── adawing-ui/                    # Vue 3 + Vite 前端
│   └── src/
│       ├── api/                   #   11 个 Axios 模块
│       ├── components/            #   共享 UI 组件（无框架依赖）
│       ├── layouts/               #   VisitorLayout · AdminLayout
│       ├── stores/                #   Pinia（auth · site · theme）
│       ├── utils/                 #   格式化工具、URL 构建
│       └── views/
│           ├── visitor/           #   首页 · 文章 · 时间线 · 动态 · 留言 · 关于
│           └── admin/             #   看板 · 文章 · 审核 · 留言 · 标签 · 设置
├── docs/
│   ├── superpowers/
│   │   ├── specs/                 #   设计规范（6 份）
│   │   ├── plans/                 #   阶段实施计划（19 份）
│   │   └── verify/               #   集成验证报告（14 份）
│   └── examples/mcp-configs/      #   MCP 客户端配置模板
└── scripts/                       #   数据迁移 & 提取工具
```

---

## ✦ 数据库 ER（简明）

```
sys_user ───── BCrypt 认证，单管理员
mcp_key  ───── SHA-256 API Key，MCP 访问凭据
system_config  KV 站点配置存储

article  ──< article_tag >── tag      文章-标签 N:N，含 primary_tag 标记
article  ──< resource_reference >── resource   引用计数关联
note     ──< resource_reference >── resource
message  ──< resource_reference >── resource

review_task   （article | message）审核状态机
              策略：ArticleReviewStrategy / MessageReviewStrategy
              事件：ContentApprovedEvent / ContentRejectedEvent
```

全部 12 张表使用 `utf8mb4`。无物理外键——引用完整性在应用层维护。

---

## ✦ Non-Goals

AdaWing 有意不做的事情：

- ❌ 多用户 / RBAC — 单管理员设计
- ❌ Redis — Caffeine 本地缓存完全够用
- ❌ Elasticsearch / Meilisearch — 关键词搜索已足够
- ❌ 服务端 LLM 调用 — 所有 AI 工作在 Agent 端完成
- ❌ WebSocket / 实时推送
- ❌ 国际化 / 多语言
- ❌ SSR / SSG
- ❌ API 文档（Knife4j / Swagger） — 保持精简
- ❌ Docker（目前 — 分层 JAR 部署已满足需求）

---

## ✦ 开发工作流

本项目遵循严格的 **12 阶段 Spec → Plan → Build → Verify** 工作流。每阶段在独立会话中执行，纪律如下：

1. **Codegraph 优先检索** — 禁止 raw grep/read 循环
2. **先测试再合入** — 业务层测试强制，失败阻断构建
3. **单文件修改纪律** — 一次改一个文件、立即回归测试、通过后再继续
4. **不确定性 PAUSE** — 连续两次「可能/也许/大概」触发硬中断，向用户提问

过程文档（`specs/`、`plans/`、`previews/`、`verify/`、`reviews/`）存放于 `docs/superpowers/`，**不提交至 git**。

---

## ✦ 技术栈

| 层 | 选型 | 理由 |
|----|------|------|
| 运行时 | Java 21 + Spring Boot 3.5.0 | Records、模式匹配、虚拟线程就绪 |
| ORM | MyBatis-Plus 3.5.10 | 轻量灵活，无 JPA 魔法 |
| 缓存 | Caffeine 3.1.8 | 零基础设施本地缓存，Redis 过度设计 |
| 认证 | JJWT 0.12.6 + BCrypt | 无状态，行业标准 |
| 数据库（开发） | H2 MySQL 兼容模式 | 零配置，CI 友好 |
| 数据库（生产） | MySQL 8.0 utf8mb4 | 久经考验，完整 Emoji 支持 |
| 对象存储 | 阿里云 OSS SDK | 靠近部署区域 |
| 前端 | Vue 3 + Vite + Pinia | 现代、快速、无框架锁定 |
| 编辑器 | Vditor 3.11.2 IR 模式 | 中文 Markdown WYSIWYG 最优解 |
| 动画 | GSAP + ScrollTrigger | 高性能，无 CSS 动画卡顿 |
| MCP | Streamable-HTTP JSON-RPC | 协议合规，主流 AI 编程工具通用 |

---

<p align="center">
  <br/>
  <em>slow wing, just plummed.<br/>风迟，恰好初生。</em>
</p>
