# AdaWing v2 生产部署指南

> 本地构建 → scp 上传 → 服务器一键 setup → 改配置 → 启动。服务器只需 JDK + MySQL + Nginx，无需 Maven/Node/pnpm。

---

## 一、前置条件

| 资源 | 要求 |
|------|------|
| 服务器 | 1 台，2C2G 及以上，公网 IP |
| 操作系统 | Ubuntu 22.04+ / Debian 12+（推荐），CentOS 9+ / Rocky 9+ |
| 域名 | 已注册，DNS A 记录解析到服务器 IP（可选，HTTP 也能用） |
| 本地 | JDK 21、Maven 3.9+、Node 18+、pnpm 8+ |

---

## 二、本地构建打包

在**本地 Windows 开发机**执行（双击或在命令行运行）：

```batch
deploy\build.bat
```

或在 Git Bash / WSL 中：

```bash
cmd //c deploy/build.bat
```

脚本会依次：
1. 检查本地 JDK / Maven / Node / pnpm 版本
2. `mvn clean package -DskipTests` 构建后端
3. `pnpm install && pnpm run build` 构建前端
4. 收集所有文件到 `deploy/adawing-v2/`

产物内容：

```
deploy/adawing-v2/
├── adawing-backend.jar      后端 Spring Boot 胖 JAR
├── frontend/                前端静态文件（SPA）
├── schema.sql               数据库建表脚本
├── application-prod.yaml    生产配置模板
├── setup.sh                 服务器一键初始化
├── run.sh                   应用启动脚本
└── nginx.conf               Nginx 配置
```

---

## 三、上传至服务器

```bash
# 压缩（在项目根目录执行）
cd deploy
tar -czf adawing-v2.tar.gz adawing-v2/

# 上传
scp adawing-v2.tar.gz root@<服务器IP>:/opt/

# SSH 登录
ssh root@<服务器IP>

# 解压
cd /opt
tar xzf adawing-v2.tar.gz
cd adawing-v2
```

---

## 四、一键部署

```bash
sudo bash setup.sh
```

### setup.sh 执行步骤

| 步骤 | 动作 |
|------|------|
| 1 | 检测操作系统（Ubuntu/Debian/CentOS/Rocky） |
| 2 | 安装 JDK 21（如果未安装） |
| 3 | 安装 MySQL 8 并启动（如果未安装） |
| 4 | 安装 Nginx（如果未安装） |
| 5 | 安装 Certbot（如果未安装，用于 SSL 证书） |
| 6 | 建库 `adawing` + 创建专用数据库用户 + 执行 `schema.sql` 建表 |
| 7 | 创建系统用户 `adawing` + 目录 `/opt/adawing` + 日志目录 |
| 8 | 部署 jar / 前端 / 配置模板 / run.sh 到 `/opt/adawing/` |
| 9 | 申请 SSL 证书（需要设置 DOMAIN + ADMIN_EMAIL 环境变量） |
| 10 | 配置 Nginx，测试并重载 |
| 11 | 创建 Systemd 服务 `adawing.service`，设置开机自启 |

### SSL 证书申请

```bash
# 带域名
export DOMAIN=adawing.example.com
export ADMIN_EMAIL=your@email.com
sudo bash setup.sh
```

### 证书申请失败排查

1. **DNS 解析**：`dig $DOMAIN +short` 应返回服务器公网 IP
2. **80 端口**：`sudo ss -tlnp | grep :80`，如有占用先停止
3. **防火墙/安全组**：云厂商需放行 80、443
4. 修好后重新运行 `sudo bash setup.sh`，已完成的步骤会自动跳过

---

## 五、修改配置

```bash
sudo vim /opt/adawing/application-prod.yaml
```

**必须修改的字段：**

| 字段 | 说明 |
|------|------|
| `spring.datasource.password` | 数据库密码（查看 `/opt/adawing/.db_credentials`） |
| `jwt.secret` | `openssl rand -base64 48` 生成，≥32位 |
| `spring.mail.username` | QQ 邮箱地址 |
| `spring.mail.password` | QQ 邮箱 SMTP 授权码 |
| `oss.access-key` | 阿里云 OSS AccessKey ID |
| `oss.secret-key` | 阿里云 OSS AccessKey Secret |
| `oss.bucket-name` | OSS Bucket 名称 |

修改后改权限：

```bash
sudo chown adawing:adawing /opt/adawing/application-prod.yaml
sudo chmod 600 /opt/adawing/application-prod.yaml
```

---

## 六、启动服务

```bash
sudo systemctl start adawing nginx
```

### 验证

```bash
# Systemd 状态
sudo systemctl status adawing

# 实时日志
sudo tail -f /var/log/adawing/app.log

# 健康检查
curl -s http://localhost:8080/api/v2/system/config/dashboard

# 博客首页
curl -s -H "Host: your-domain.com" https://localhost/ | head
```

### 常见启动失败

| 现象 | 原因 | 解决 |
|------|------|------|
| `status adawing` 反复 failed | `application-prod.yaml` 数据库密码错误 | 检查 `/var/log/adawing/app.log`，修正配置后重启 |
| `Connection refused` on :8080 | 后端未启动 | `sudo systemctl restart adawing` |
| 日志显示 OSS 异常 | OSS 密钥错误 | 暂时留空，后台手动上传图片也可以 |
| Nginx 配置报错 | SSL 证书路径不存在 | 如果没申请证书，暂时去掉 443 的 server block |

---

## 七、防火墙

```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 3306/tcp    # MySQL 远程维护
sudo ufw enable
```

**云服务器还需在控制台安全组放行 80、443、3306。**

---

## 八、访问

| 入口 | URL |
|------|-----|
| **博客首页** | `https://your-domain.com` |
| **管理后台** | `https://your-domain.com/yusal/admin/login` |
| **MCP 端点** | `https://your-domain.com/mcp` |

默认管理员：`admin / admin123`。登录后立即改密码。

---

## 九、Navicat 远程数据库维护

`setup.sh` 自动创建了远程维护用户并开启了 MySQL 远程访问。

### Navicat 连接信息

```
主机:   <服务器公网 IP>
端口:   3306
用户:   adawing_remote
密码:   查看 /opt/adawing/.db_credentials 中的 DB_REMOTE_PASS
数据库: adawing
```

### 排查

| 问题 | 解决 |
|------|------|
| `2003 - Can't connect` | 云服务器安全组未放行 3306 端口 |
| `1045 - Access denied` | 密码不对，`cat /opt/adawing/.db_credentials` 查看 |
| `1130 - Host not allowed` | MySQL 用户创建失败，重新执行 `sudo bash setup.sh` |

---

## 九、迭代更新

不需要重新跑 setup.sh，只需替换 jar + 前端 + 重启。

```bash
# 本地重新构建 + 压缩上传（在项目根目录执行）
deploy\build.bat
cd deploy && tar -czf adawing-v2.tar.gz adawing-v2/
scp adawing-v2.tar.gz root@<服务器IP>:/opt/

# 服务器
cd /opt && tar xzf adawing-v2.tar.gz

# 替换 jar + 前端
sudo cp /opt/adawing-v2/adawing-backend.jar /opt/adawing/
sudo cp -r /opt/adawing-v2/frontend/* /opt/adawing/frontend/
sudo chown -R adawing:adawing /opt/adawing

# 重启
sudo systemctl restart adawing
```

---

## 十、常用运维命令

```bash
# 查看服务状态
sudo systemctl status adawing

# 重启
sudo systemctl restart adawing

# 停止
sudo systemctl stop adawing

# 查看实时日志
sudo tail -f /var/log/adawing/app.log

# 查看最近 100 行
sudo journalctl -u adawing -n 100

# Nginx 配置检查
sudo nginx -t

# 重载 Nginx
sudo systemctl reload nginx

# 查看端口占用
sudo ss -tlnp | grep -E '8080|80|443'

# 查看 JVM 进程
jps -v | grep adawing

# SSL 证书续期测试
sudo certbot renew --dry-run

# 查看证书有效期
sudo certbot certificates
```

---

## 十一、目录结构（服务器）

```
/opt/adawing/
├── adawing-backend.jar        后端正主
├── application-prod.yaml      生产配置（敏感，权限 600）
├── run.sh                     启动脚本
├── .db_credentials            数据库账号密码（权限 600）
├── frontend/                  前端静态文件
├── logs/                      运行日志
└── schema.sql                 建表脚本（备用）

/etc/
├── systemd/system/adawing.service    Systemd 服务定义
└── nginx/sites-available/adawing     Nginx 站点配置

/var/log/adawing/app.log        应用日志（Systemd 标准输出）
```

---

## 十二、回滚

```bash
sudo systemctl stop adawing

# 用之前的 jar 替换
sudo cp /opt/adawing/adawing-backend.jar.bak /opt/adawing/adawing-backend.jar

sudo systemctl start adawing
```
