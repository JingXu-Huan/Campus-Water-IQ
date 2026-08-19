# 本地 Nacos（Docker Desktop）

项目原先从已过期的远端 Nacos 读取配置和服务注册信息。本目录提供一个可持久化的 Nacos 2.3.2 单机实例，以及项目当前所需的本地开发配置种子。

## 启动

在项目根目录执行 PowerShell：

```powershell
pwsh -File .\docker\nacos\start-local.ps1
```

The Nacos compose file starts the complete `campus-water-middleware` group: Nacos, RocketMQ, MySQL, Redis, Redisson, InfluxDB, and Canal.

脚本会完成以下动作：

1. 校验并启动 `campus-water-nacos` 容器；
2. 等待 Nacos readiness 接口返回 200；
3. 将 `config` 目录中的 YAML 幂等写入 `DEFAULT_GROUP`；
4. 重新读取每份配置，确认写入成功。

Nacos 控制台地址：`http://127.0.0.1:8848/nacos`。本地容器关闭了 Nacos 鉴权，应用默认不发送用户名和密码；连接开启鉴权的其他实例时，再通过 `NACOS_USERNAME`、`NACOS_PASSWORD` 覆盖。

数据和日志分别保存在 Docker Named Volume `campus_water_nacos_data`、`campus_water_nacos_logs` 中。删除容器不会删除这些卷。

## 应用启动

应用配置默认使用：

```text
NACOS_SERVER_ADDR=127.0.0.1:8848
NACOS_NAMESPACE=<空字符串，Nacos public namespace>
```

因此在 IntelliJ IDEA 中直接启动即可。若连接其他 Nacos 实例，可在运行配置中覆盖 `NACOS_SERVER_ADDR` 和 `NACOS_NAMESPACE`，无需再改各模块的 `bootstrap.yml`。

本地配置种子面向容器网络使用服务名：MySQL `mysql:3306`、Redis `redis:6379`、Redisson `redission:6379`、InfluxDB `http://influxdb2:8086`、Canal `canal-server:11111`、RocketMQ NameServer `campus-water-rmq-namesrv:9876`。如果在 IntelliJ IDEA 中直接启动 Java 服务，仍可在运行配置中覆盖为 `127.0.0.1` 对应端口；原项目的初始化说明位于 `common/src/init/环境搭建指南.md`。

## 常用命令

```powershell
# 查看状态
docker compose -f .\docker-compose.nacos.yml ps

# 查看日志
docker compose -f .\docker-compose.nacos.yml logs -f nacos

# 只重新写入配置，不重启 Nacos
pwsh -File .\docker\nacos\seed-nacos.ps1

# 停止容器（保留 Named Volumes）
docker compose -f .\docker-compose.nacos.yml down
```

不要使用 `down -v`，除非确认要删除本地 Nacos 的持久化数据。
