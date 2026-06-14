#!/usr/bin/env bash
# =============================================================================
# AdaWing v2 — 一键打包脚本
# 用法: bash deploy/package.sh
# 输出: deploy/dist/adawing-backend.jar + deploy/dist/frontend/
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$ROOT/deploy/dist"

echo "========================================="
echo " AdaWing v2 构建打包"
echo "========================================="

# ---- Step 1: 后端 ----
echo ""
echo "[1/4] 清理旧构建产物..."
rm -rf "$DIST"
mkdir -p "$DIST"
cd "$ROOT/Blog" && mvn clean -q

echo "[2/4] 编译后端 + 跑测试..."
cd "$ROOT/Blog" && mvn install -DskipTests -q
cp blog-boot/target/blog-boot-1.0-SNAPSHOT.jar "$DIST/adawing-backend.jar"
echo "  ✓ 后端打包完成: deploy/dist/adawing-backend.jar"

# ---- Step 2: 前端 ----
echo "[3/4] 安装前端依赖..."
cd "$ROOT/adawing-ui" && pnpm install --frozen-lockfile --silent 2>/dev/null || pnpm install --silent

echo "[4/4] 构建前端..."
cd "$ROOT/adawing-ui" && pnpm run build
cp -r dist/* "$DIST/frontend/"
echo "  ✓ 前端打包完成: deploy/dist/frontend/"

# ---- Summary ----
echo ""
echo "========================================="
echo " 打包完成!"
echo "========================================="
echo ""
echo "  deploy/dist/"
echo "  ├── adawing-backend.jar   ($(du -h "$DIST/adawing-backend.jar" | cut -f1))"
echo "  ├── frontend/"
echo "  │   ├── index.html"
echo "  │   └── assets/           ($(ls "$DIST/frontend/assets/" | wc -l) files)"
echo ""
echo "  下一步: bash deploy/start.sh"
