# 本地微服务容器化运行

## 1. 先启动中间件组

在项目根目录执行：

```powershell
pwsh -File .\docker\nacos\start-local.ps1
```

The startup script starts the complete `campus-water-middleware` group: Nacos, RocketMQ, MySQL, Redis, Redisson, InfluxDB, and Canal. Application containers use the middleware container DNS names through the shared Docker network.

`iot-service` depends on `iot-device` for the Dubbo `BloomFilterService`; Compose declares this startup dependency.

该命令会创建并启动 Docker Desktop 中的 `campus-water-middleware` 组，所有容器加入 `campus-water-network`。

## 2. 构建并启动服务

```powershell
docker compose -f .\docker-compose.services.yml build
docker compose -f .\docker-compose.services.yml up -d
```

停止服务但保留镜像：

```powershell
docker compose -f .\docker-compose.services.yml down
```

## 服务端口

| 服务 | HTTP | Dubbo/三方端口 |
| --- | ---: | ---: |
| water-gateway | 18080 | - |
| auth-service | 18099 | - |
| iot-device | 18015 | 50052 |
| iot-service | 18016 | 20881 |
| prediction-service | 18017 | - |
| repair-service | 20000 | - |
| warning-service | 18018 | 20888 |
| ingest-group | 无 Web 端口 | - |

## 配置覆盖和外部依赖

`docker-compose.services.yml` 默认通过同一 Docker 网络访问 `mysql`、`redis`、`redission`、`influxdb2` 和 `canal-server`；Nacos、RocketMQ 也使用同一网络中的容器 DNS。可在项目根目录创建 `.env` 覆盖连接信息，例如使用宿主机中间件时设置 `MYSQL_HOST=host.docker.internal`。

首次配置可复制 `.env.example` 为 `.env`，再填写本机数据库、InfluxDB、OSS、预测 API 和邮件凭证；`.env` 已加入 `.gitignore`，不会提交到远程仓库。

业务容器属于 `campus-water-services` 组，中间件容器属于 `campus-water-middleware` 组；两组通过外部网络 `campus-water-network` 互通。

`auth-service` 默认使用仅用于本地启动的 OSS 占位凭证；真正执行 OSS 文件上传前，需要在 `.env` 中提供 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`。启动预测服务需要提供 `API_KEY`。`warning-service` 默认关闭 Actuator 邮件健康检查，真正发送邮件前需提供 `SPRING_MAIL_USERNAME`、`SPRING_MAIL_PASSWORD`，并可设置 `MANAGEMENT_HEALTH_MAIL_ENABLED=true`。`repair-service` 默认连接宿主机 Ollama 的 `11434` 端口。

当前 Dockerfile 使用 Java 21 多阶段构建，并在镜像构建时只对目标服务执行 `spring-boot:repackage`；`common` 作为普通依赖库保留。
