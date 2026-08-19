# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Backend (Java/Maven)
```bash
# Full build (from project root)
mvn clean package -DskipTests

# Build specific module
mvn clean package -DskipTests -pl <module-name> -am

# Run single module (example: IoT-device)
mvn spring-boot:run -pl IoT-device

# Run tests for specific module
mvn test -pl <module-name>
```

### Frontend (React/Vite)
```bash
cd frontend
npm install
npm run dev      # Development server
npm run build    # Production build
npm run lint     # Lint check
```

## Architecture Overview

### Microservices Structure

```
IoT-device (模拟设备接入服务)
    ├── Netty + WebSocket 长连接
    ├── 数字孪生设备模型
    └── 设备数据清洗 → Redis → RocketMQ

IoT-service/
    ├── ingest-group (数据接入层)
    │   └── 消费MQ消息写入InfluxDB时序数据库
    └── service (数据服务层)
        ├── Dubbo RPC (20881端口)
        └── REST API (18016端口)

water-gateway (API网关)
    └── 路由转发、权限校验

auth-service (认证服务)
    ├── 登录/注册策略工厂
    └── JWT token管理

prediction-service (AI预测服务)
    └── 基于LangChain4j调用GLM大模型

warning-service (告警服务)
    └── 告警分级、多渠道通知

repair-service (维修服务)
    └── 工单管理
```

### Key Infrastructure

| Service | Port | Purpose |
|---------|------|---------|
| Nacos | 8848 | 服务注册与发现 |
| InfluxDB | 8086 | 时序数据存储 |
| MySQL | 3306 | 关系型数据 |
| Redis | 6379/36379 | 缓存/分布式锁 |
| Canal | 11111 | 数据同步 |
| RocketMQ | 9876 | 消息队列 |

### Data Flow
1. IoT-device 模拟设备通过 WebSocket 上报数据
2. 数据经 Redis 缓存后发送至 RocketMQ
3. ingest-group 消费消息写入 InfluxDB
4. IoT-service 提供数据查询 API
5. prediction-service 调用 AI 分析异常
6. warning-service 触发告警通知

## Module Responsibilities

- **common**: 公共代码、实体类、常量、API接口定义
- **IoT-device**: 设备接入、数字孪生、边缘校验(AOP)
- **IoT-service/ingest-group**: MQ消费者、数据清洗、时序数据写入
- **IoT-service/service**: 业务服务、Dubbo provider、数据分析API
- **water-gateway**: API网关、路由、鉴权
- **auth-service**: 用户认证、多登录方式(邮箱/微信/QQ/GitHub)
- **prediction-service**: AI用水预测、节水建议生成
- **warning-service**: 告警触发、通知渠道管理
- **repair-service**: 维修工单管理

## Configuration Notes

- 所有微服务使用 Nacos 作为注册中心，默认连接本机 `127.0.0.1:8848`；可通过 `NACOS_SERVER_ADDR` 和 `NACOS_NAMESPACE` 覆盖
- InfluxDB token 需要在 `IoT-service/ingest-group` 和 `IoT-service/service` 两处配置
- Dubbo 协议端口: IoT-service 20881, IoT-device 50052
- Java 版本: **21** (强制要求)

## Important Patterns

- **AOP 注解**: IoT-device 使用 AOP 进行数据校验、耗时统计、Lua脚本初始化
- **策略模式**: auth-service 的登录/注册策略工厂
- **数字孪生**: VirtualDevice 实体实时映射物理设备状态
- **规则引擎+AI双重检测**: 异常检测结合阈值规则和AI模型
