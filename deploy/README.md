# AdaWing v2 部署手册

## 脚本速查

| 脚本 | 用途 | 执行时机 |
|------|------|----------|
| `setup-env.sh` | 环境检查 + 生成 .env | 首次部署 |
| `package.sh` | Maven 编译 + 前端打包 → `dist/` | 每次部署 |
| `init-db.sh` | 建库 + 执行 schema + 种子数据 | 首次部署 / 重置 |
| `start.sh` | 启动 / 停止 / 查看状态 | 日常运维 |

---

## 快速开始（5 步上线）

```bash
# 1. 环境检查
bash deploy/setup-env.sh

# 2. 编辑配置（填入你的数据库密码、OSS 密钥等）
vim deploy/.env

# 3. 构建
bash deploy/package.sh

# 4. 初始化数据库
bash deploy/init-db.sh

# 5. 启动
bash deploy/start.sh
```

启动后访问：
- **博客首页** → `http://localhost:80`
- **管理后台** → `http://localhost:80/v2/ren/admin/login`
- **MCP 端点** → `http://localhost:8080/mcp`

---

## 日常运维

```bash
bash deploy/start.sh status    # 查看服务状态
bash deploy/start.sh stop      # 停止所有服务
bash deploy/start.sh restart   # 重启 (stop + start)

tail -f deploy/.logs/backend.log   # 查看后端日志
tail -f deploy/.logs/frontend.log  # 查看前端日志
```

---

## 重置数据库

```bash
bash deploy/init-db.sh --reset
```

这会删除所有表并重新建表 + 插入种子数据。**现有数据会丢失！**

---

## 生产部署（Nginx + systemd）

### 1. 配置 Nginx

```bash
sudo cp deploy/nginx.conf /etc/nginx/sites-available/adawing
sudo ln -s /etc/nginx/sites-available/adawing /etc/nginx/sites-enabled/
# 修改 server_name + SSL 证书路径
sudo nginx -t && sudo nginx -s reload
```

### 2. 安装 systemd 服务（可选）

```ini
# /etc/systemd/system/adawing.service
[Unit]
Description=AdaWing v2 Backend
After=network.target mysql.service

[Service]
Type=simple
User=adawing
WorkingDirectory=/opt/adawing
ExecStart=/usr/bin/java -jar /opt/adawing/adawing-backend.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now adawing
sudo systemctl status adawing
```

### 3. 前端自动部署

```bash
# 每次 package.sh 后，把 dist/frontend/ 复制到 nginx 目录
cp -r deploy/dist/frontend/* /var/www/adawing/
```

---

## 文件结构

```
deploy/
├── .env.example        ← 环境变量模板，不提交 git
├── .env                ← 本地配置（gitignore），从 .env.example 复制
├── setup-env.sh        ← 环境检查
├── package.sh          ← 一键打包
├── init-db.sh          ← 数据库初始化
├── start.sh            ← 启动/停止/状态
├── nginx.conf           ← Nginx 配置模板
├── README.md            ← 本文件
├── dist/                ← 构建产物（gitignore）
│   ├── adawing-backend.jar
│   └── frontend/
├── .logs/               ← 运行日志（gitignore）
└── .pids/               ← 进程 PID（gitignore）
```
