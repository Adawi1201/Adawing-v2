#!/usr/bin/env bash
# =============================================================================
# AdaWing v2 — 数据库初始化
# 用法: bash deploy/init-db.sh                 # 新建
#       bash deploy/init-db.sh --reset         # 删除重建 (危险!)
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# 加载 .env
if [ -f "$ROOT/deploy/.env" ]; then
    source "$ROOT/deploy/.env"
else
    echo "错误: 请先运行 bash deploy/setup-env.sh 初始化 .env"
    exit 1
fi

SCHEMA="$ROOT/Blog/blog-boot/src/main/resources/db/schema.sql"
MYSQL_CMD="mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASS}"

RESET=false
if [ "${1:-}" = "--reset" ]; then
    RESET=true
fi

echo "========================================="
echo " AdaWing v2 数据库初始化"
echo "========================================="
echo "  主机: $DB_HOST:$DB_PORT"
echo "  库名: $DB_NAME"
echo "  用户: $DB_USER"

# ---- 检查 schema 文件 ----
if [ ! -f "$SCHEMA" ]; then
    echo "错误: schema.sql 不存在: $SCHEMA"
    exit 1
fi

# ---- 建库 ----
echo ""
echo "[1/3] 创建数据库..."
if $MYSQL_CMD -e "CREATE DATABASE IF NOT EXISTS $DB_NAME DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;" 2>/dev/null; then
    echo "  ✓ 数据库 $DB_NAME 已就绪"
else
    echo "  ✗ 无法连接 MySQL，请检查 deploy/.env 中的 DB_HOST/DB_USER/DB_PASS"
    exit 1
fi

# ---- Reset ----
if $RESET; then
    echo ""
    echo "[!] --reset 模式: 删除所有表..."
    $MYSQL_CMD "$DB_NAME" -e "SET FOREIGN_KEY_CHECKS=0; DROP TABLE IF EXISTS article_tag, article, tag, note, message, resource_reference, resource, review_task, system_config, mcp_key, sys_user; SET FOREIGN_KEY_CHECKS=1;" 2>/dev/null
    echo "  ✓ 旧表已清除"
fi

# ---- 执行 Schema ----
echo ""
echo "[2/3] 执行 schema.sql..."
if $MYSQL_CMD "$DB_NAME" < "$SCHEMA" 2>&1; then
    # count tables
    TABLE_COUNT=$($MYSQL_CMD "$DB_NAME" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';" 2>/dev/null)
    echo "  ✓ 已创建 ${TABLE_COUNT:-?} 张表"
else
    echo "  ✗ Schema 执行失败"
    exit 1
fi

# ---- 种子数据 ----
echo ""
echo "[3/3] 写入种子数据..."

# 管理员 (密码: admin123)
$MYSQL_CMD "$DB_NAME" -e "
INSERT INTO sys_user (id, username, password_hash, nickname, status)
VALUES (1, 'admin', '\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Ada', 1)
ON DUPLICATE KEY UPDATE username='admin';
" 2>/dev/null
echo "  ✓ 管理员: admin / admin123"

# 默认站点配置
$MYSQL_CMD "$DB_NAME" -e "
INSERT INTO system_config (config_key, config_value, description) VALUES
('site_title',       'Adawing', '站点标题'),
('site_subtitle',    '/dev/life/observer', '副标题'),
('seo_keywords',     '技术,博客,AI', 'SEO 关键词'),
('seo_description',  '', 'SEO 描述'),
('footer_copyright', 'AdaByte. All rights reserved.', '页脚版权')
ON DUPLICATE KEY UPDATE config_key=config_key;
" 2>/dev/null
echo "  ✓ 站点配置"

echo ""
echo "========================================="
echo " 数据库初始化完成 ✓"
echo "========================================="
echo ""
echo "  管理员登录: admin / admin123"
echo "  下一步: bash deploy/start.sh"
