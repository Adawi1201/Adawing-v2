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

## ✦ What is AdaWing?

> From the moment this project was conceived, I knew I wasn't building a website — I was carving out a sky of my own.
>
> **Ada** is my name. **Wing** is freedom. **AdaWing** is the freedom that belongs to me.

AdaWing is a **deeply personal yet technically ambitious blogging platform**, built from zero to production as a single-developer full-stack project. It's not just a CMS — it's a **constraint-engineered environment** where humans and AI agents collaborate on content creation under a unified moderation pipeline.

What makes it different:

- **You write.** A full-featured Markdown editor with Vditor, tag management with Levenshtein dedup, view-count statistics, and a dual layout (visitor-facing vs. admin) design system.
- **AI agents write too.** AdaWing exposes a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that lets coding agents like Claude Code / OpenClaw / Codex publish draft articles directly into the review queue. The server defines content rules dynamically — agents must earn publication the same way humans do: through editorial review.
- **Everything passes through review.** A strategy-pattern review center routes articles and messages through approval workflows. AI-generated content is never auto-published.
- **Resources are first-class citizens.** Uploads are pooled (`AVATAR` / `ARTICLE` / `EMOJI`), tracked by reference count, and orphaned files are garbage-collected on schedule.

> 📖 [中文版 README-zh.md](./README-zh.md)

---

## ✦ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     blog-boot                           │
│            (entry point · CORS · events)                │
├──────────┬──────────┬──────────┬──────────┬─────────────┤
│ blog-zoom│ blog-    │ blog-    │ agent-   │  adawing-ui │
│ content  │ system   │ resource │ mcp      │  Vue 3 SPA  │
│ domain   │ domain   │ domain   │ server   │             │
├──────────┴──────────┴──────────┴──────────┴─────────────┤
│                    blog-common                          │
│        (Result · PageResult · JwtUtil · Exceptions)     │
└─────────────────────────────────────────────────────────┘
```

**Layered bottom-up construction.** Module dependency flows strictly downward — `blog-common` → shared domains → boot assembly. Cross-module Mapper injection is **forbidden**; communication happens through Spring Events.

```
blog-boot ──┬── blog-common           (compile scope, no Spring Boot starter)
            ├── blog-resource          resource-core · oss · cleaner
            ├── blog-zoom              zoom-shared · tag · article · note · message
            ├── blog-system            system-auth · config · profile · review
            └── agent-mcp              MCP server (only depends on blog-common anti-corrosion layer)
```

---

## ✦ Features

### 📝 Article Management
- **Status machine:** `DRAFT` → `PENDING_REVIEW` → `PUBLISHED` (or back to `DRAFT` on rejection)
- **Source tracking:** original vs. AI-agent-generated, with agent attribution
- **Covers:** OSS-backed resource references with reference counting
- **View counts:** Caffeine-accumulated, batch-flushed to MySQL every 5 minutes
- **Pinning & hiding:** top-sticky and visibility toggles

### 🏷 Tag System
- Admin-only creation; frontend forbids `allow-create`
- Levenshtein-distance deduplication on new tag creation
- Tag merge support with full reassignment
- Sorted by article count

### 📋 Unified Review Center
- Strategy-pattern dispatch: `ArticleReviewStrategy` / `MessageReviewStrategy`
- Original articles bypass review; agent-generated content + visitor messages **must** clear review
- Review results fire Spring Events — zero direct cross-module calls

### 💬 Message Board
- Nickname + email + random avatar + content
- XSS filtering (`script`, `iframe`, `javascript:`, `onXXX=`)
- Admin reply with SMTP HTML email notification (QQ SMTP)
- Like throttle (Caffeine 2-second lock)

### 🗂 Resource Management
- Pool-based classification: `AVATAR` · `ARTICLE` · `EMOJI` · `MISC`
- Reference counting via `resource_reference` junction table
- Scheduled orphan cleanup (daily 3:00 AM, 7-day grace period)
- Alibaba Cloud OSS backend with proxy download endpoint (solves CORS + ACL issues)

### 🔐 Auth
- Admin: JWT (BCrypt-password, 7-day token)
- MCP: API Key (SHA-256 hash, plaintext shown only once at creation)

### 📬 Email Notifications
- HTML email via Jakarta Mail + SMTP
- Triggered on message approval/rejection

### 📊 Admin Dashboard
- Data-at-a-glance: article / message / note / review counts
- `GET /api/v2/system/config/dashboard`

### ✨ Frontend
- **Oriental-aesthetic design** — no heavy UI framework, hand-tuned CSS
- **Dark mode** with `prefers-color-scheme` auto-detection
- **GSAP scroll animations** — staggered reveal, typewriter hero text
- **Reading progress** bar + **floating nav** breadcrumb
- **Dual layout isolation** — visitor layout and admin layout share zero styles

---

## ✦ The MCP Server (Agent Integration)

AdaWing is also a **content destination for AI coding agents**. The MCP server listens on `POST /mcp` and speaks JSON-RPC (Streamable-HTTP transport).

### Why this matters

Most personal blogs are human-write-only silos. AdaWing treats AI agents as **content contributors** — they research, draft, and submit articles programmatically, but the editorial decision always remains human.

### Tools exposed

| Tool | Purpose |
|------|---------|
| `get_content_rules` | Fetch dynamic content guidelines (title length, tag constraints, style preferences) |
| `create_article_draft` | Submit a draft for review with title + markdown body + tags |
| `search_articles` | Keyword-search the existing corpus (prevent duplicate submissions) |
| `get_article` | Retrieve full article content by ID |

### Supported AI Clients

| Client | Protocol | Setup |
|--------|----------|-------|
| **Claude Code** | Streamable-HTTP | `claude mcp add adawing https://<host>/mcp` |
| **OpenClaw** | Streamable-HTTP | Register via OpenClaw MCP registry |
| **Codex** | Streamable-HTTP | Add to Codex MCP server list |

All three use the same `X-MCP-Key` header authentication and expose the same 4 tools.

### Connect it to Claude Code

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

See `docs/examples/mcp-configs/` for Claude Code / OpenClaw / Codex connection templates.

---

## ✦ Quick Start

### Prerequisites

| Dependency | Version |
|------------|---------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| pnpm | 8+ |
| MySQL | 8.0+ (production) |

### 1. Database

```sql
CREATE DATABASE IF NOT EXISTS adawing
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. Backend

```bash
cd Blog

# Dev profile (H2 in-memory, no MySQL needed)
mvn clean install -DskipTests
java -jar blog-boot/target/blog-boot-1.0-SNAPSHOT.jar

# Or with local MySQL
cp blog-boot/src/main/resources/application-local.yaml application-prod.yaml
# edit datasource creds, then:
java -jar blog-boot/target/blog-boot-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Default port: **8080**
Schema auto-initializes via `spring.sql.init` → `db/schema.sql`.

### 3. Frontend

```bash
cd adawing-ui

pnpm install
pnpm dev
```

Vite proxies `/api/v2` → `http://localhost:8080` automatically.

### 4. Access

| Entry | URL |
|-------|-----|
| **Blog Home** | `http://localhost:5173/` |
| **Admin Login** | `http://localhost:5173/v2/ren/admin/login` |
| **Admin Dashboard** | `http://localhost:5173/v2/ren/admin` |
| **MCP Endpoint** | `http://localhost:8080/mcp` (POST) |

---

## ✦ Project Map

```
adawing/
├── Blog/                          # Maven multi-module backend
│   ├── blog-common/               #   shared DTOs, utils, exceptions
│   ├── blog-resource/             #   resource-core · oss · cleaner
│   ├── blog-zoom/                 #   tag · article · note · message
│   ├── blog-system/               #   auth · config · profile · review
│   └── blog-boot/                 #   main class · controllers · config
├── agent-mcp/                     # Standalone MCP server
├── adawing-ui/                    # Vue 3 + Vite SPA
│   └── src/
│       ├── api/                   #   11 Axios modules
│       ├── components/            #   shared UI (no framework)
│       ├── layouts/               #   VisitorLayout · AdminLayout
│       ├── stores/                #   Pinia (auth · site · theme)
│       ├── utils/                 #   formatters, URL builders
│       └── views/
│           ├── visitor/           #   Home · Article · Chronicle · Notes · Messages · About
│           └── admin/             #   Dashboard · Articles · Review · Messages · Tags · Settings
├── docs/
│   ├── superpowers/
│   │   ├── specs/                 #   design specifications (6 reports)
│   │   ├── plans/                 #   phase implementation plans (19 reports)
│   │   └── verify/               #   integration verification reports (14 reports)
│   └── examples/mcp-configs/      #   JSON config templates for MCP clients
└── scripts/                       #   data migration & extraction helpers
```

---

## ✦ Database ER (condensed)

```
sys_user ───── BCrypt auth, single-admin
mcp_key  ───── SHA-256 API keys for MCP access
system_config  KV store for site metadata

article  ──< article_tag >── tag      N:N with primary_tag flag
article  ──< resource_reference >── resource   reference counting
note     ──< resource_reference >── resource
message  ──< resource_reference >── resource

review_task   (article | note) review state machine
              strategies: ArticleReviewStrategy / MessageReviewStrategy
              events: ContentApprovedEvent / ContentRejectedEvent
```

All 12 tables use `utf8mb4` across the board. No physical foreign keys — referential integrity is maintained at the application layer.

---

## ✦ Non-Goals

Things AdaWing deliberately does **not** do:

- ❌ Multi-user / RBAC — single-admin by design
- ❌ Redis — replaced by Caffeine local cache
- ❌ Elasticsearch / Meilisearch — keyword search is sufficient
- ❌ Server-side LLM calls — all AI work happens on the agent side
- ❌ WebSocket / real-time push
- ❌ i18n / multi-language
- ❌ SSR / SSG
- ❌ API docs (Knife4j / Swagger) — kept minimal
- ❌ Docker (for now — layered JAR is enough)

---

## ✦ Development Workflow

This project follows a strict **12-phase spec→plan→build→verify** workflow. Each phase runs in an isolated session with:

1. **Codegraph-first code retrieval** — no raw grep/read loops
2. **Test-before-merge discipline** — business layer tests are mandatory; failures block the build
3. **Single-file edit discipline** — fix one file at a time, re-test, then proceed
4. **PAUSE on uncertainty** — two or more "maybe / perhaps / assume" signals triggers a hard stop and user consultation

Process documents (`specs/`, `plans/`, `previews/`, `verify/`, `reviews/`) are kept in `docs/superpowers/` and are **not committed to git**.

---

## ✦ Tech Stack Summary

| Layer | Choice | Why |
|-------|--------|-----|
| Runtime | Java 21 + Spring Boot 3.5.0 | Records, pattern matching, virtual threads ready |
| ORM | MyBatis-Plus 3.5.10 | Lightweight, flexible SQL, no JPA magic |
| Cache | Caffeine 3.1.8 | Zero-infra local cache; Redis would be overkill |
| Auth | JJWT 0.12.6 + BCrypt | Stateless, industry-standard |
| DB (dev) | H2 in MySQL mode | Zero-config, CI-friendly |
| DB (prod) | MySQL 8.0 utf8mb4 | Battle-tested, full emoji support |
| OSS | Alibaba Cloud OSS SDK | Proximity to deployment region |
| Frontend | Vue 3 + Vite + Pinia | Modern, fast, no framework lock-in |
| Editor | Vditor 3.11.2 IR mode | Best-in-class Chinese Markdown WYSIWYG |
| Animation | GSAP + ScrollTrigger | Performant, no CSS animation jank |
| MCP | Streamable-HTTP JSON-RPC | Spec-compliant, works with all major AI coding tools |

---

<p align="center">
  <br/>
  <em>slow wing, just plummed.<br/>风迟，恰好初生。</em>
</p>
