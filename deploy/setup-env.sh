#!/usr/bin/env bash
# =============================================================================
# AdaWing v2 — 环境检查 + 配置初始化
# 用法: bash deploy/setup-env.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PASS=1

echo "========================================="
echo " AdaWing v2 环境检查"
echo "========================================="

# ---- JDK ----
echo ""
echo "[1/6] 检查 JDK..."
if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1 | grep -oP '"[0-9]+' | tr -d '"')
    if [ "$JAVA_VER" -ge 21 ] 2>/dev/null; then
        echo "  ✓ Java $JAVA_VER  ($(which java))"
    else
        echo "  ✗ Java $JAVA_VER  (需要 21+)，请安装 JDK 21"
        PASS=0
    fi
else
    echo "  ✗ 未找到 java，请安装 JDK 21+"
    PASS=0
fi

# ---- Maven ----
echo "[2/6] 检查 Maven..."
if command -v mvn &>/dev/null; then
    MVN_VER=$(mvn -version 2>&1 | head -1 | grep -oP '3\.\d+' | head -1)
    if [ -n "$MVN_VER" ]; then
        echo "  ✓ Maven $MVN_VER  ($(which mvn))"
    else
        echo "  ✗ 无法识别 Maven 版本 (需要 3.9+)"
        PASS=0
    fi
else
    echo "  ✗ 未找到 mvn，请安装 Maven 3.9+"
    PASS=0
fi

# ---- Node ----
echo "[3/6] 检查 Node.js..."
if command -v node &>/dev/null; then
    NODE_VER=$(node -v | grep -oP '\d+' | head -1)
    if [ "$NODE_VER" -ge 18 ] 2>/dev/null; then
        echo "  ✓ Node $(node -v)  ($(which node))"
    else
        echo "  ✗ Node $(node -v) (需要 18+)，请升级"
        PASS=0
    fi
else
    echo "  ✗ 未找到 node，请安装 Node.js 18+"
    PASS=0
fi

# ---- pnpm ----
echo "[4/6] 检查 pnpm..."
if command -v pnpm &>/dev/null; then
    echo "  ✓ pnpm $(pnpm -v)  ($(which pnpm))"
else
    echo "  ✗ 未找到 pnpm，运行: npm install -g pnpm"
    PASS=0
fi

# ---- MySQL ----
echo "[5/6] 检查 MySQL..."
if command -v mysql &>/dev/null; then
    MYSQL_VER=$(mysql --version 2>&1 | grep -oP '8\.\d+')
    if [ -n "$MYSQL_VER" ]; then
        echo "  ✓ MySQL $MYSQL_VER  ($(which mysql))"
    else
        echo "  ! MySQL 已安装但不是 8.x 版本，建议升级"
    fi
else
    echo "  ! 未找到 mysql 客户端 (不强制，但 init-db.sh 需要)"
fi

# ---- .env ----
echo "[6/6] 初始化 .env..."
if [ -f "$ROOT/deploy/.env" ]; then
    echo "  ✓ deploy/.env 已存在"
else
    cp "$ROOT/deploy/.env.example" "$ROOT/deploy/.env"
    echo "  ✓ 已从 .env.example 复制，请编辑 deploy/.env 填入真实配置!"
fi

# ---- Result ----
echo ""
if [ "$PASS" -eq 1 ]; then
    echo "========================================="
    echo " 环境检查通过 ✓"
    echo "========================================="
    echo ""
    echo " 下一步:"
    echo "   1. 编辑 deploy/.env 填入数据库密码等真实配置"
    echo "   2. bash deploy/package.sh     # 构建"
    echo "   3. bash deploy/init-db.sh     # 初始化数据库"
    echo "   4. bash deploy/start.sh       # 启动服务"
else
    echo "========================================="
    echo " 环境检查未通过 ✗，请安装缺失的依赖"
    echo "========================================="
    exit 1
fi
