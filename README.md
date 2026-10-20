# FlashSale · 高并发秒杀系统

一个用于学习高并发场景的秒杀系统实践项目：**Redis 预扣库存扛住瞬时流量，RabbitMQ 异步落库削峰，Sa-Token 做登录鉴权，JMeter 做压测验证**。

后端已完整实现，前端基于 Vue 3 + Vite。仓库里同时附带了从零讲解每个模块原理的中文教程（[GUIDE.md](GUIDE.md)）。

---

## 功能特性

- **库存预热**：应用启动时把 DB 库存加载进 Redis，秒杀请求不直接打数据库
- **Redis 原子扣减**：`DECR` 扣库存，库存不足自动回滚并打上售罄标记，后续请求快速失败
- **一人一单**：`SETNX` 防重锁，同一用户对同一商品只能秒杀一次
- **异步下单**：秒杀请求只做校验，实际落库交给 RabbitMQ 消费者，削峰填谷
- **结果轮询**：前端按商品轮询结果接口，消费者完成下单后写入 Redis 供查询
- **登录鉴权**：Sa-Token 生成 Token 存 Redis，拦截器统一校验
- **接口限流**：秒杀接口独立限流拦截器
- **压测配套**：JMeter 脚本 + 一键压测脚本 + 完整压测报告

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 语言 / 运行时 | Java 21 |
| 后端框架 | Spring Boot 4.1.0 |
| ORM | MyBatis-Plus 3.5.12 |
| 数据库 | MySQL 8 |
| 缓存 | Redis（Lettuce 连接池） |
| 消息队列 | RabbitMQ（手动 ACK） |
| 认证 | Sa-Token 1.44.0 |
| 前端 | Vue 3.5 + Vue Router + Axios + Vite |
| 压测 | Apache JMeter 5.6.3 |

## 秒杀流程

```
客户端
  │  POST /api/seckill/{goodsId}
  ▼
SeckillServiceImpl
  ├─ 1. 商品是否存在 / 秒杀时间是否在区间内
  ├─ 2. Redis 售罄标记 seckill:soldout:{goodsId} 快速失败
  ├─ 3. Redis DECR 原子扣库存（< 0 则回滚 + 打售罄标记）
  ├─ 4. SETNX 防重锁（同一用户同一商品只能一次）
  └─ 5. 投递消息到 RabbitMQ，立即返回「正在排队」
                │
                ▼
        SeckillConsumer（异步）
          ├─ DB 原子扣库存：UPDATE ... SET stock_count = stock_count - 1 WHERE stock_count > 0
          ├─ 写入 order_info（订单）
          ├─ 写入 seckill_order（防重记录）
          ├─ 订单 ID 写入 Redis，供轮询查询
          └─ 手动 ACK

客户端轮询 GET /api/order/result/{goodsId}
  └─ -1 = 排队中，> 0 = 下单成功返回订单 ID
```

## 目录结构

```
FlashSale/
├── FlashSaleBackend/                 # Spring Boot 后端
│   └── src/main/
│       ├── java/com/kisssheep/flashsalebackend/
│       │   ├── common/               # 统一响应 Result / 错误码 / 全局异常
│       │   ├── config/               # Redis / RabbitMQ / Web / 线程池
│       │   ├── controller/           # Auth / Goods / Seckill
│       │   ├── dto/ entity/ mapper/  # 数据层
│       │   ├── interceptor/          # 登录校验 + 限流
│       │   ├── mq/                   # 生产者 / 消费者 / 消息体
│       │   └── service/              # 业务逻辑
│       └── resources/
│           ├── application.yml       # 配置（密码走环境变量）
│           └── schema.sql            # 建表 + 测试数据
├── FlashSaleFrontend/                # Vue 3 + Vite 前端
├── jmeter/                           # 压测脚本 / 数据准备 / 报告
├── GUIDE.md                          # 完整开发教程（推荐从这读起）
├── OUTLINE.md                        # 项目设计大纲
├── LEARNING.md                       # 学习笔记
└── docker-compose.yml                # RabbitMQ 容器
```

## 快速开始

### 环境要求

- JDK 21、Maven（或用 `mvnw`）
- MySQL 8+、Redis、RabbitMQ
- Node.js 18+（只跑前端时需要）

### 1. 克隆项目

```bash
git clone https://github.com/KissSheep666/FlashSale.git
cd FlashSale
```

### 2. 初始化数据库

```bash
mysql -uroot -p < FlashSaleBackend/src/main/resources/schema.sql
```

会创建 `user`、`goods`、`order_info`、`seckill_order` 四张表，并插入 3 个商品和 2 个测试用户。

### 3. 配置数据库密码（不写进代码）

后端从环境变量读取 MySQL 密码：

```powershell
# PowerShell
$env:MYSQL_PASSWORD = "你的 MySQL 密码"
```

```bash
# bash
export MYSQL_PASSWORD="你的 MySQL 密码"
```

IDE 运行时，在 Run Configuration → Environment variables 里添加 `MYSQL_PASSWORD` 即可。

可选环境变量：`MYSQL_USERNAME`（默认 `root`）、`RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD`（默认 `guest`）。

### 4. 启动依赖服务

```bash
# Redis（本地已安装）
redis-server

# RabbitMQ：本地安装
D:\rabbitmq\rabbitmq_server-4.3.2\sbin\rabbitmq-server.bat

# 或者用 Docker 起 RabbitMQ
docker compose up -d
```

### 5. 启动后端

```bash
cd FlashSaleBackend
./mvnw spring-boot:run
```

服务地址：`http://localhost:8080`

### 6. 启动前端

```bash
cd FlashSaleFrontend
npm install
npm run dev
```

## API 接口

| 方法 | 路径 | 需要 Token | 说明 |
| --- | --- | --- | --- |
| POST | `/api/user/login` | 否 | Body：`{ "phone": "...", "password": "..." }`，返回 Token |
| GET | `/api/goods` | 否 | 商品列表 |
| GET | `/api/goods/{goodsId}` | 否 | 商品详情 |
| POST | `/api/seckill/{goodsId}` | 是 | 执行秒杀，返回「正在排队」 |
| GET | `/api/order/result/{goodsId}` | 是 | 轮询结果：`-1` 排队中，`>0` 订单 ID |

需要鉴权的接口在请求头携带：

```
Authorization: Bearer <token>
```

测试账号：`13800000001` / `123456`

## 压测

```bash
# 一键压测（Windows PowerShell，需先设置 MYSQL_PASSWORD）
jmeter\run-test.ps1

# 或直接用 JMeter CLI
jmeter -n -t jmeter/flashsale-load-test.jmx -l jmeter/results/result.jtl -e -o jmeter/results/report
```

压测脚本会预先写入 500 个测试用户，模拟并发抢购，并校验库存一致性（Redis 剩余 + DB 订单数 = 初始库存）。

历史压测报告见 [`jmeter/results/report/index.html`](jmeter/results/report/index.html)，操作细节见 [`jmeter/压测操作指南.md`](jmeter/压测操作指南.md)。

## 文档

| 文档 | 内容 |
| --- | --- |
| [GUIDE.md](GUIDE.md) | 从环境搭建到每个模块实现的完整教程，代码与项目一一对应 |
| [OUTLINE.md](OUTLINE.md) | 项目定位、表设计、秒杀流程、分包结构、优化项 |
| [LEARNING.md](LEARNING.md) | 学习过程中的知识点整理 |
| [jmeter/压测操作指南.md](jmeter/压测操作指南.md) | 压测环境准备、执行步骤、结果解读 |

## 已知限制

- 库存扣减使用 `DECR` + 失败回滚，极端并发下回滚阶段可能出现短暂不一致，生产环境建议改用 Lua 脚本保证原子性
- MQ 消费失败目前只打日志，未接入重试队列 / 死信队列
- 防重锁依赖 Redis 单点，未考虑 Redis 故障降级
- 压测报告基于本机单机环境，不代表生产集群表现
