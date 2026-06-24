# Windows 本机实验部署

本方案通过 Docker Desktop 运行 MySQL、后端、前端和隔离评测容器。Web 服务仅绑定到
`127.0.0.1`，不会直接开放到局域网或互联网。

## 启动

在 PowerShell 中执行：

    Set-Location 'D:\软件工程课程设计\csuft-oj'
    Copy-Item .env.local.example .env.local
    New-Item -ItemType Directory -Force C:\csuft-oj-data\testcases, C:\csuft-oj-data\judge-temp, C:\csuft-oj-data\logs | Out-Null
    docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml up -d --build
    docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml ps

三个服务都显示 `healthy` 后，访问 http://127.0.0.1:8080 。

首次使用某种语言判题时，Docker 会自动下载对应运行时镜像。也可以提前下载：

    docker pull gcc:14
    docker pull eclipse-temurin:17-jdk
    docker pull python:3.12
    docker pull golang:1.23

## 健康检查

    docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml exec backend curl -fsS http://127.0.0.1:8081/actuator/health/readiness

## 创建管理员

先在网页注册账号，再将 `你的用户名` 替换为实际用户名：

    docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml exec mysql mysql -ucsuft_oj -plocal-csuft-oj-db-password csuft_oj -e "UPDATE tb_user SET role='ADMIN', token_version=token_version+1 WHERE username='你的用户名';"

执行后重新登录，即可访问管理监控和用户管理。

## 日志

    docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml logs -f backend

## 停止或重置

停止并保留数据库：

    docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml down

彻底删除本机实验数据库：

    docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml down -v

`down -v` 会删除 MySQL 数据卷，仅用于测试重置。
