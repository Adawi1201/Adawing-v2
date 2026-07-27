#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_DIR/Blog"
FRONTEND_DIR="$PROJECT_DIR/adawing-ui"
DEPLOY_DIR="$PROJECT_DIR/deploy"
DIST_DIR="$DEPLOY_DIR/adawing-v2"

fail() {
    echo
    echo "========================================="
    echo "  BUILD FAILED"
    echo "========================================="
    echo
    echo "  Check the error messages above."
    exit 1
}

echo
echo "========================================="
echo "  AdaWing v2 - Build & Package"
echo "========================================="
echo "  Project: $PROJECT_DIR"
echo

# ---- Step 1: Check tools ----
echo "[1/4] Checking build tools..."
echo

check_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "  [FAIL] $1 not found - $2"
        fail
    fi
    echo "  [OK] $3"
}

check_tool java "Install JDK 21" "Java"
check_tool mvn "Install Maven 3.9+" "Maven"
check_tool node "Install Node.js 18+" "Node"
check_tool pnpm "Run: npm install -g pnpm" "pnpm"

# ---- Step 2: Backend ----
echo
echo "[2/4] Building backend (Maven)..."

cd "$PROJECT_DIR"
mvn clean package -DskipTests -q || { cd "$PROJECT_DIR"; fail; }

if [ ! -f "$BACKEND_DIR/blog-boot/target/blog-boot-1.0-SNAPSHOT.jar" ]; then
    echo "  [FAIL] jar not found"
    fail
fi
echo "  [OK] Backend built"

# ---- Step 3: Frontend ----
echo
echo "[3/4] Building frontend (Vite)..."

cd "$FRONTEND_DIR"
pnpm install || { cd "$PROJECT_DIR"; fail; }
pnpm run build || { cd "$PROJECT_DIR"; fail; }

if [ ! -f "$FRONTEND_DIR/dist/index.html" ]; then
    echo "  [FAIL] dist not found"
    cd "$PROJECT_DIR"
    fail
fi
echo "  [OK] Frontend built"

# ---- Step 4: Collect files ----
echo
echo "[4/4] Collecting files..."

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/frontend"

cp "$BACKEND_DIR/blog-boot/target/blog-boot-1.0-SNAPSHOT.jar" "$DIST_DIR/adawing-backend.jar" \
    || echo "  [WARN] jar copy failed"
cp -R "$FRONTEND_DIR/dist/." "$DIST_DIR/frontend/" \
    || echo "  [WARN] frontend copy failed"
cp "$BACKEND_DIR/blog-boot/src/main/resources/db/schema.sql" "$DIST_DIR/" 2>/dev/null || true
cp "$DEPLOY_DIR/application-prod.yaml" "$DIST_DIR/" 2>/dev/null || true
cp "$DEPLOY_DIR/setup.sh" "$DIST_DIR/" 2>/dev/null || true
cp "$DEPLOY_DIR/run.sh" "$DIST_DIR/" 2>/dev/null || true
cp "$DEPLOY_DIR/nginx.conf" "$DIST_DIR/" 2>/dev/null || true
chmod +x "$DIST_DIR/setup.sh" "$DIST_DIR/run.sh" 2>/dev/null || true

# Strip macOS metadata files that Finder scatters around
find "$DIST_DIR" -name ".DS_Store" -type f -delete 2>/dev/null || true

echo "  Checking collected files:"
find "$DIST_DIR" -type f | sed "s|$DIST_DIR/|    |"
echo
echo "  [OK] Files collected"

# ---- Done ----
echo
echo "========================================="
echo "  BUILD SUCCESS"
echo "========================================="
echo
echo "  Output: deploy/adawing-v2/"
echo
echo "  Next step:"
echo "    1. COPYFILE_DISABLE=1 tar -czf adawing-v2.tar.gz adawing-v2/"
echo "    2. scp adawing-v2.tar.gz root@YOUR_IP:/opt/"
echo "    3. ssh root@YOUR_IP"
echo "       cd /opt && tar xzf adawing-v2.tar.gz"
echo "       cd adawing-v2 && sudo bash setup.sh"
echo
