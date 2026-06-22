#!/bin/bash
set -e

# ============================================================
# AdaWing v2 — 服务器一键部署
# 支持：Ubuntu 22.04+ / Debian 12+ / CentOS 9+ / Rocky 9+
# 用法：sudo bash setup.sh
# ============================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

APP_USER="adawing"
APP_HOME="/opt/adawing"
LOG_DIR="/var/log/adawing"
DB_NAME="adawing"
DB_USER="adawing"
DB_PASS="adawing_$(openssl rand -hex 8)"
DB_REMOTE_USER="adawing_remote"
DB_REMOTE_PASS="adawing_remote_$(openssl rand -hex 8)"
JWT_SECRET="$(openssl rand -base64 48)"

DOMAIN="${DOMAIN:-}"
ADMIN_EMAIL="${ADMIN_EMAIL:-}"

function log_info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
function log_ok()    { echo -e "${GREEN}[ OK ]${NC} $1"; }
function log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
function log_err()   { echo -e "${RED}[ERR]${NC} $1"; }

# ============================================================
# 0. OS detection
# ============================================================
function detect_os() {
  if [[ -f /etc/os-release ]]; then
    . /etc/os-release
    OS=$ID
    VER=$VERSION_ID
  else
    log_err "Cannot detect OS"
    exit 1
  fi
  log_info "OS: $OS $VER"
}

# ============================================================
# 1. Install dependencies
# ============================================================
function install_jdk() {
  log_info "Installing JDK 21..."
  if java -version 2>&1 | grep -q '21\.'; then
    log_ok "JDK 21 already installed ($(java -version 2>&1 | head -1))"
    return
  fi
  case "$OS" in
    ubuntu|debian)
      apt-get update -qq
      if apt-get install -y -qq openjdk-21-jre-headless 2>/dev/null; then
        log_ok "JDK 21 installed (apt)"
        return
      fi
      # Fallback: Eclipse Temurin (Adoptium)
      log_warn "openjdk-21 not in repo, trying Temurin..."
      apt-get install -y -qq wget apt-transport-https gnupg 2>/dev/null || true
      wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor > /usr/share/keyrings/adoptium.gpg 2>/dev/null
      echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/VERSION_CODENAME/{print $2}' /etc/os-release) main" > /etc/apt/sources.list.d/adoptium.list
      apt-get update -qq
      apt-get install -y -qq temurin-21-jre || { log_err "JDK 21 install failed"; exit 1; }
      ;;
    centos|rocky|almalinux|rhel)
      dnf install -y -q java-21-openjdk-headless 2>/dev/null || yum install -y -q java-21-openjdk-headless
      ;;
    *) log_err "Unsupported OS: $OS"; exit 1 ;;
  esac
  log_ok "JDK 21 installed: $(java -version 2>&1 | head -1)"
}

function install_mysql() {
  log_info "Installing MySQL 8..."
  if mysql --version 2>/dev/null | grep -q '8\.'; then
    log_ok "MySQL 8 already installed"
    return
  fi
  case "$OS" in
    ubuntu|debian)
      apt-get update -qq
      if apt-get install -y -qq mysql-server-8.0 2>/dev/null; then
        log_ok "MySQL 8 installed"
        return
      fi
      # Fallback: generic mysql-server (Ubuntu 22.04 default is 8.0 anyway)
      apt-get install -y -qq mysql-server
      ;;
    centos|rocky|almalinux|rhel)
      dnf install -y -q mysql-server 2>/dev/null || yum install -y -q mysql-server
      systemctl enable mysqld 2>/dev/null || true
      systemctl start mysqld 2>/dev/null || true
      ;;
  esac
  log_ok "MySQL installed"
}

function install_nginx() {
  log_info "Installing Nginx..."
  if command -v nginx &>/dev/null; then
    log_ok "Nginx already installed"
    return
  fi
  case "$OS" in
    ubuntu|debian) apt-get install -y -qq nginx ;;
    centos|rocky|almalinux|rhel) dnf install -y -q nginx 2>/dev/null || yum install -y -q nginx ;;
  esac
  log_ok "Nginx installed"
}

function install_certbot() {
  log_info "Installing Certbot + nginx plugin..."
  if command -v certbot &>/dev/null; then
    log_ok "Certbot already installed"
    return
  fi
  case "$OS" in
    ubuntu|debian) apt-get install -y -qq certbot python3-certbot-nginx ;;
    centos|rocky|almalinux|rhel) dnf install -y -q certbot python3-certbot-nginx 2>/dev/null || yum install -y -q certbot python3-certbot-nginx ;;
  esac
  log_ok "Certbot installed"
}

# ============================================================
# 2. MySQL init
# ============================================================
function setup_mysql() {
  log_info "Setting up MySQL..."

  case "$OS" in
    ubuntu|debian)
      systemctl start mysql 2>/dev/null || service mysql start 2>/dev/null || true
      ;;
  esac

  for i in $(seq 1 30); do
    if mysqladmin ping &>/dev/null; then break; fi
    sleep 1
  done

  # Create database + local user
  mysql -u root <<SQL 2>/dev/null || sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

  log_ok "Database: $DB_NAME"
  log_ok "Local user: $DB_USER"

  # Remote maintenance user (Navicat)
  mysql -u root <<SQL 2>/dev/null || sudo mysql <<SQL
CREATE USER IF NOT EXISTS '${DB_REMOTE_USER}'@'%' IDENTIFIED BY '${DB_REMOTE_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_REMOTE_USER}'@'%';
FLUSH PRIVILEGES;
SQL
  log_ok "Remote user: $DB_REMOTE_USER (for Navicat)"

  # Enable remote access
  local mysql_cnf=""
  for f in /etc/mysql/mysql.conf.d/mysqld.cnf /etc/my.cnf /etc/my.cnf.d/mysql-server.cnf; do
    if [[ -f "$f" ]]; then mysql_cnf="$f"; break; fi
  done
  if [[ -n "$mysql_cnf" ]]; then
    if grep -q "^bind-address" "$mysql_cnf"; then
      sed -i "s/^bind-address.*/bind-address = 0.0.0.0/" "$mysql_cnf"
      systemctl restart mysql 2>/dev/null || service mysql restart 2>/dev/null || true
      log_ok "MySQL remote access enabled (bind-address = 0.0.0.0)"
    fi
  fi

  # Run schema.sql
  local schema=""
  for f in "$SCRIPT_DIR/schema.sql" "$SCRIPT_DIR/../Blog/blog-boot/src/main/resources/db/schema.sql"; do
    if [[ -f "$f" ]]; then schema="$f"; break; fi
  done
  if [[ -n "$schema" ]]; then
    mysql -u root "$DB_NAME" < "$schema" 2>/dev/null || sudo mysql "$DB_NAME" < "$schema"
    local cnt; cnt=$(mysql -u root -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';" 2>/dev/null)
    log_ok "Schema imported (${cnt:-?} tables)"
  else
    log_warn "schema.sql not found, skipping table creation"
  fi
}

# ============================================================
# 3. Deploy files
# ============================================================
function create_dirs() {
  log_info "Creating directories..."
  mkdir -p "$APP_HOME" "$APP_HOME/frontend" "$LOG_DIR"
  id -u "$APP_USER" &>/dev/null || useradd -r -s /bin/false "$APP_USER"
  log_ok "Directories ready"
}

function deploy_files() {
  log_info "Deploying files..."

  local src="$SCRIPT_DIR"

  # Backend jar
  if [[ -f "$src/adawing-backend.jar" ]]; then
    cp "$src/adawing-backend.jar" "$APP_HOME/adawing-backend.jar"
    log_ok "adawing-backend.jar"
  else
    log_err "adawing-backend.jar not found in $src"
    echo "  请先运行 build.bat 打包"
    exit 1
  fi

  # Frontend
  if [[ -d "$src/frontend" ]]; then
    rm -rf "$APP_HOME/frontend"/*
    cp -r "$src/frontend"/* "$APP_HOME/frontend/"
    log_ok "frontend/ ($(ls "$APP_HOME/frontend/assets/" 2>/dev/null | wc -l) files)"
  else
    log_warn "frontend/ not found, skipping"
  fi

  # run.sh
  if [[ -f "$src/run.sh" ]]; then
    cp "$src/run.sh" "$APP_HOME/run.sh"
    chmod +x "$APP_HOME/run.sh"
  fi

  # Application config — copy template then inject real values
  local cfg="$APP_HOME/application-prod.yaml"
  if [[ -f "$src/application-prod.yaml" ]]; then
    cp "$src/application-prod.yaml" "$cfg"
  else
    log_err "application-prod.yaml not found"
    exit 1
  fi

  # Auto-fill placeholders
  sed -i "s|your-db-password-here|$DB_PASS|"           "$cfg"
  sed -i "s|your-email@qq.com|${MAIL_USER:-your-email@qq.com}|"  "$cfg"
  sed -i "s|your-smtp-auth-code|${MAIL_PASS:-your-smtp-auth-code}|" "$cfg"
  sed -i "s|your-access-key|${OSS_AK:-your-access-key}|"            "$cfg"
  sed -i "s|your-secret-key|${OSS_SK:-your-secret-key}|"            "$cfg"
  sed -i "s|your-bucket-name|${OSS_BUCKET:-your-bucket-name}|"      "$cfg"
  sed -i "s|change-me-to-a-random-string|$JWT_SECRET|"  "$cfg"
  if [[ -n "$DOMAIN" ]]; then
    sed -i "s|https://wing.adabyte.top|https://$DOMAIN|" "$cfg"
  fi

  # If env vars not set, leave placeholders — user edits manually later
  # But warn about unmapped ones
  if grep -q "your-" "$cfg" 2>/dev/null; then
    log_warn "Some placeholders still in $cfg — edit manually:"
    grep -n "your-" "$cfg" | while read line; do echo "      $line"; done
  fi

  log_ok "application-prod.yaml (auto-filled)"

  chown -R "$APP_USER:$APP_USER" "$APP_HOME" "$LOG_DIR"
}

# ============================================================
# 4. Nginx
# ============================================================
function setup_nginx() {
  log_info "Configuring Nginx..."

  # Copy template
  if [[ -f "$SCRIPT_DIR/nginx.conf" ]]; then
    cp "$SCRIPT_DIR/nginx.conf" /etc/nginx/sites-available/adawing
  else
    log_err "nginx.conf not found"
    exit 1
  fi

  sed -i "s|/opt/adawing/frontend|$APP_HOME/frontend|g" /etc/nginx/sites-available/adawing

  if [[ -n "$DOMAIN" ]]; then
    sed -i "s|server_name _;|server_name $DOMAIN;|g" /etc/nginx/sites-available/adawing
  fi

  rm -f /etc/nginx/sites-enabled/default
  ln -sf /etc/nginx/sites-available/adawing /etc/nginx/sites-enabled/adawing

  if nginx -t 2>&1; then
    systemctl reload nginx 2>/dev/null || service nginx reload
    log_ok "Nginx configured (HTTP)"
  else
    log_err "nginx -t failed"
    exit 1
  fi
}

# ============================================================
# 5. SSL (optional, needs DOMAIN set)
# ============================================================
function setup_ssl() {
  if [[ -z "$DOMAIN" ]]; then
    log_warn "No DOMAIN set — skipping SSL. Using HTTP only."
    echo "  To add HTTPS later:"
    echo "    sudo certbot --nginx -d your-domain.com"
    return
  fi

  log_info "Requesting SSL cert for $DOMAIN..."

  if [[ -d "/etc/letsencrypt/live/$DOMAIN" ]]; then
    log_ok "SSL cert already exists, skipping"
    return
  fi

  if [[ -z "$ADMIN_EMAIL" ]]; then
    log_err "ADMIN_EMAIL required. Run: export ADMIN_EMAIL=you@example.com"
    exit 1
  fi

  certbot --nginx \
    -d "$DOMAIN" \
    --non-interactive --agree-tos -m "$ADMIN_EMAIL" \
    --redirect || {
    log_err "SSL request failed — is DNS pointing to this server?"
    echo "  Manual retry: sudo certbot --nginx -d $DOMAIN"
    exit 1
  }

  log_ok "HTTPS enabled for $DOMAIN"

  # Auto-renew cron
  if [[ ! -f /etc/cron.d/certbot-renew ]]; then
    cat > /etc/cron.d/certbot-renew <<'CRON'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
0 3 * * * root certbot renew --quiet --deploy-hook "systemctl reload nginx"
CRON
    chmod 644 /etc/cron.d/certbot-renew
    log_ok "Certbot auto-renewal configured"
  fi
}

# ============================================================
# 6. Systemd service
# ============================================================
function setup_systemd() {
  log_info "Setting up systemd service..."

  cat > /etc/systemd/system/adawing.service <<UNIT
[Unit]
Description=AdaWing v2
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=$APP_USER
Group=$APP_USER
WorkingDirectory=$APP_HOME
EnvironmentFile=-$APP_HOME/.env
ExecStart=/usr/bin/java -jar $APP_HOME/adawing-backend.jar --spring.config.name=application-prod --spring.config.location=/opt/adawing/
ExecStop=/bin/kill -15 \$MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=append:$LOG_DIR/app.log
StandardError=append:$LOG_DIR/app.log

[Install]
WantedBy=multi-user.target
UNIT

  systemctl daemon-reload
  systemctl enable adawing
  log_ok "Systemd service: adawing.service"
}

# ============================================================
# 7. Done — print summary
# ============================================================
function show_summary() {
  # Save credentials
  cat > "$APP_HOME/.db_credentials" <<EOF
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASS=$DB_PASS
DB_REMOTE_USER=$DB_REMOTE_USER
DB_REMOTE_PASS=$DB_REMOTE_PASS
EOF
  chmod 600 "$APP_HOME/.db_credentials"
  chown "$APP_USER:$APP_USER" "$APP_HOME/.db_credentials"

  cat <<EOF

============================================
  AdaWing v2 — Deployment Complete
============================================

  App dir:    $APP_HOME
  Config:     $APP_HOME/application-prod.yaml
  DB creds:   $APP_HOME/.db_credentials

  ── Database ──
  Name:       $DB_NAME
  Local user: $DB_USER / $DB_PASS

  ── Navicat Remote ──
  Host:       <server public IP>
  Port:       3306
  User:       $DB_REMOTE_USER
  Pass:       $DB_REMOTE_PASS

  ── Admin Login ──
  URL:        http://<server IP>/yusal/admin/login
  User:       admin
  Pass:       admin123

  ── Next Steps ──
  1. Edit config:  vim $APP_HOME/application-prod.yaml
     (OSS keys, email auth code — database is auto-filled)
  2. Allow firewall ports:
     sudo ufw allow 80/tcp
     sudo ufw allow 443/tcp
     sudo ufw allow 3306/tcp
     sudo ufw enable
  3. Cloud security group: allow 80, 443, 3306
  4. Start:         sudo systemctl start adawing nginx
  5. Check status:  bash $APP_HOME/run.sh status
  6. View logs:     bash $APP_HOME/run.sh logs

EOF

  if [[ -n "$DOMAIN" ]]; then
    echo "  Blog:       https://$DOMAIN"
  else
    echo "  Blog:       http://<server IP>"
  fi
  echo ""
}

# ============================================================
# Main
# ============================================================
if [[ "$(id -u)" -ne 0 ]]; then
  echo -e "${RED}Run with sudo: sudo bash setup.sh${NC}"
  exit 1
fi

echo ""
echo "============================================"
echo " AdaWing v2 — Server Setup"
echo "============================================"
echo ""

detect_os
install_jdk
install_mysql
install_nginx
install_certbot
setup_mysql
create_dirs
deploy_files
setup_nginx
setup_ssl
setup_systemd
show_summary

log_ok "Setup complete"
