# AdaWing MCP Server — 连接配置示例

AdaWing MCP 是 **Streamable-HTTP** 类型的远程 MCP Server，部署在 `http://<host>:<port>/mcp`。

认证方式：请求头 `X-MCP-Key: <your-api-key>`。

API Key 需通过 AdaWing 后台 `POST /api/v2/mcp-keys` 生成（一次一密，SHA-256 存储）。

---

## 本目录文件

| 文件 | 场景 |
|------|------|
| `mcp.json` | **单文件模板** — 直接复制到 `~/.claude/mcp.json` |
| `adawing-mcp.code-workspace.example.json` | **VS Code 多项目** — 贴到 `.vscode/settings.json` 的 `mcpServers` |
| `claude-desktop-config.example.json` | **Claude Desktop** — 贴到应用的 `claude_desktop_config.json` |
| `env.example` | 环境变量 — 填入 `.bashrc` / `.zshrc` / 系统变量 |

---

## 三种使用方式

### 方式 A：全局 mcp.json（推荐 — 所有项目通用）

把 `mcp.json` 放到用户目录 `~/.claude/mcp.json`。Claude Code 启动时自动加载。

```bash
# Linux / macOS
cp mcp.json ~/.claude/mcp.json

# Windows (Git Bash)
cp mcp.json "$HOME/.claude/mcp.json"
```

### 方式 B：项目级 .claude/mcp.json

放到项目根目录 `.claude/mcp.json`。仅当前项目生效。

### 方式 C：环境变量注入

在 `mcp.json` 中用 `${ADIWING_MCP_KEY}` 引用 API Key，Key 本身通过 shell 环境变量注入，避免明文落盘。

---

## 一行快速配置

```bash
# 1. 生成 API Key（一次性）
curl -s -X POST http://localhost:8080/api/v2/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' > /dev/null

curl -s -X POST http://localhost:8080/api/v2/mcp-keys \
  -H "Content-Type: application/json" \
  -d '{"name":"claude-code","description":"Claude Code 本地开发"}' \
  | grep -o '"data":"[^"]*"' | cut -d'"' -f4

# 2. 把上面输出的 key 填入
#    ~/.claude/mcp.json 中的 X-MCP-Key 字段

# 3. 重启 Claude Code
```
