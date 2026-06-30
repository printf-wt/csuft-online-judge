#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/srv/csuft-oj-app}"
DATA_DIR="${DATA_DIR:-/srv/csuft-oj}"
BACKUP_DIR="${BACKUP_DIR:-/srv/csuft-oj-backups}"
OLD_PROJECT_DIR="${OLD_PROJECT_DIR:-}"
CONFIRM_DELETE_OLD_PROJECT="${CONFIRM_DELETE_OLD_PROJECT:-no}"
DELETE_OLD_COMPOSE_VOLUMES="${DELETE_OLD_COMPOSE_VOLUMES:-yes}"
BIND_ADDRESS="${BIND_ADDRESS:-0.0.0.0}"
HTTP_PORT="${HTTP_PORT:-8080}"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "$1 is required"
}

random_secret() {
  openssl rand -base64 36 | tr -d '\n'
}

compose_down_old_project() {
  local old_dir="$1"
  if [[ ! -f "$old_dir/docker-compose.yml" && ! -f "$old_dir/compose.yml" ]]; then
    return
  fi

  echo "Stopping old compose project in $old_dir"
  if [[ "$DELETE_OLD_COMPOSE_VOLUMES" == "yes" ]]; then
    (cd "$old_dir" && docker compose down -v --remove-orphans)
  else
    (cd "$old_dir" && docker compose down --remove-orphans)
  fi
}

require_command docker
require_command openssl
require_command tar
docker compose version >/dev/null 2>&1 || die "docker compose plugin is required"

if [[ "$(id -u)" -ne 0 ]]; then
  die "Run this script as root so it can manage /srv directories and Docker"
fi

mkdir -p "$BACKUP_DIR"

if [[ -n "$OLD_PROJECT_DIR" ]]; then
  [[ "$CONFIRM_DELETE_OLD_PROJECT" == "yes" ]] || die "Set CONFIRM_DELETE_OLD_PROJECT=yes before deleting $OLD_PROJECT_DIR"
  [[ -d "$OLD_PROJECT_DIR" ]] || die "OLD_PROJECT_DIR does not exist: $OLD_PROJECT_DIR"
  [[ "$OLD_PROJECT_DIR" != "/" ]] || die "Refusing to delete /"
  [[ "$OLD_PROJECT_DIR" != "$APP_DIR" ]] || die "OLD_PROJECT_DIR must not equal APP_DIR"

  timestamp="$(date +%F-%H%M%S)"
  backup_file="$BACKUP_DIR/old-project-$timestamp.tar.gz"
  echo "Backing up old project to $backup_file"
  tar -czf "$backup_file" -C "$(dirname "$OLD_PROJECT_DIR")" "$(basename "$OLD_PROJECT_DIR")"
  compose_down_old_project "$OLD_PROJECT_DIR"
  echo "Removing old project directory $OLD_PROJECT_DIR"
  rm -rf --one-file-system "$OLD_PROJECT_DIR"
fi

mkdir -p "$APP_DIR" "$DATA_DIR/testcases" "$DATA_DIR/judge-temp" "$DATA_DIR/logs" "$DATA_DIR/backups"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
release_root="$(cd "$script_dir/.." && pwd)"
if [[ "$release_root" != "$APP_DIR" ]]; then
  require_command rsync
  echo "Syncing release files into $APP_DIR"
  rsync -a --delete \
    --exclude '.env' \
    --exclude '.env.local' \
    --exclude 'node_modules' \
    --exclude 'target' \
    --exclude 'dist' \
    --exclude 'logs' \
    "$release_root/" "$APP_DIR/"
fi

cd "$APP_DIR"

docker_gid="$(stat -c '%g' /var/run/docker.sock)"
chown -R 10001:10001 "$DATA_DIR/testcases" "$DATA_DIR/judge-temp" "$DATA_DIR/logs"

if [[ ! -f .env ]]; then
  echo "Creating production .env"
  cat > .env <<EOF
DB_USERNAME=csuft_oj
DB_PASSWORD=$(random_secret)
MYSQL_ROOT_PASSWORD=$(random_secret)
JWT_SECRET=$(random_secret)$(random_secret)
JWT_REFRESH_COOKIE_SECURE=true
RATE_LIMIT_LOGIN_ATTEMPTS=20
RATE_LIMIT_LOGIN_WINDOW_SECONDS=300
RATE_LIMIT_REGISTER_ATTEMPTS=50
RATE_LIMIT_REGISTER_WINDOW_SECONDS=3600
RATE_LIMIT_REGISTER_CODE_ATTEMPTS=10
RATE_LIMIT_REGISTER_CODE_WINDOW_SECONDS=300
RATE_LIMIT_SUBMISSIONS=30
RATE_LIMIT_SUBMISSION_WINDOW_SECONDS=60
MAIL_ENABLED=false
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_REGISTER_CODE_TTL_SECONDS=600
BIND_ADDRESS=$BIND_ADDRESS
HTTP_PORT=$HTTP_PORT
OJ_DATA_DIR=$DATA_DIR
DOCKER_GID=$docker_gid
JAVA_OPTS=-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError
JUDGE_JAVA_IMAGE=eclipse-temurin:17-jdk
JUDGE_GO_IMAGE=golang:1.23
EOF
else
  echo "Keeping existing $APP_DIR/.env"
fi

echo "Building and starting CSUFT OJ"
docker compose build
docker compose up -d
docker compose ps
docker compose exec backend curl -fsS http://127.0.0.1:8081/actuator/health/readiness
echo
echo "CSUFT OJ deployed. Frontend: http://SERVER_IP:$HTTP_PORT/"
echo "For real production login, put HTTPS reverse proxy in front because JWT_REFRESH_COOKIE_SECURE=true."
