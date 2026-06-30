# CSUFT OJ operations guide

This deployment is designed for one application instance. The judge queue and rate limiter
are process-local, so do not scale the backend horizontally until they are moved to a shared
message broker and Redis.

## Host preparation

1. Install Docker Engine and the Compose plugin on a Linux host.
2. Copy `.env.example` to `.env` and replace every secret.
3. Set `OJ_DATA_DIR` to an absolute host path, then create writable directories:

```bash
mkdir -p /srv/csuft-oj/{testcases,judge-temp,logs,backups}
chown -R 10001:10001 /srv/csuft-oj/{testcases,judge-temp,logs}
stat -c '%g' /var/run/docker.sock
```

Set `DOCKER_GID` to the group ID printed by the last command. Never expose the Docker API
over TCP. The socket grants powerful host access, so run this service on a dedicated judge host.

Judge runtime images are declared as Compose bootstrap services and are pulled before the
backend starts. They can also be pre-pulled manually:

```bash
docker pull gcc:14
docker pull eclipse-temurin:17-jdk
docker pull python:3.12
docker pull golang:1.23
```

Compose binds the web service to `127.0.0.1:8080` by default. Put Caddy, Nginx, or a cloud
load balancer in front of it, terminate HTTPS there, and forward `X-Forwarded-Proto: https`.
HTTPS is mandatory because production refresh cookies are always marked `Secure`.

Public registration requires email verification. Before opening registration in production,
set SMTP values in `.env`. For QQ Mail, use the full mailbox as the username and the SMTP
authorization code as the password, not the normal login password:

```bash
MAIL_ENABLED=true
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-qq-number@qq.com
MAIL_PASSWORD=your-smtp-authorization-code
MAIL_FROM=your-qq-number@qq.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_REGISTER_CODE_TTL_SECONDS=600
```

Registration is rate-limited per client IP. The production template allows 50 registrations
per hour by default:

```bash
RATE_LIMIT_REGISTER_ATTEMPTS=50
RATE_LIMIT_REGISTER_WINDOW_SECONDS=3600
RATE_LIMIT_REGISTER_CODE_ATTEMPTS=10
RATE_LIMIT_REGISTER_CODE_WINDOW_SECONDS=300
```

If you use Nginx or another reverse proxy, forward the real client IP so independent users
do not share one backend limit bucket:

```nginx
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-Proto $scheme;
```

## Deploy and verify

```bash
docker compose build
docker compose up -d
docker compose ps
docker compose exec backend curl -fsS http://127.0.0.1:8081/actuator/health/readiness
docker compose exec backend curl -fsS http://127.0.0.1:8081/actuator/prometheus | head
```

The management port is not published by Compose. Scrape it only from the internal Docker
network or through a protected monitoring agent. Every API response includes `X-Request-ID`;
use that value to correlate user reports with backend logs.

## Database backup and restore

Back up before every release that contains a Flyway migration:

```bash
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers csuft_oj \
  > /srv/csuft-oj/backups/csuft_oj-$(date +%F-%H%M%S).sql
```

Restore into an empty database during a maintenance window:

```bash
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" csuft_oj \
  < /srv/csuft-oj/backups/backup-file.sql
```

## Release and rollback

1. Create and verify a database backup.
2. Run `docker compose build` and `docker compose up -d`.
3. Check readiness, login, one normal submission, and the admin monitor.
4. For an application-only rollback, deploy the previous image. Do not delete or edit Flyway
   history. Database rollback requires restoring the pre-release backup.

Useful diagnostics:

```bash
docker compose logs --since=30m backend
docker compose exec backend grep 'requestId=' /var/log/csuft-oj/application.log | tail -100
docker compose exec mysql mysqladmin status -uroot -p"$MYSQL_ROOT_PASSWORD"
```
