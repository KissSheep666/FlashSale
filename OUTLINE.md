# FlashSale 项目大纲

> 高并发秒杀系统 · 轻量版 · 1-2 天周期

---

## 一、项目定位

| 维度   | 决策                       |
| ---- | ------------------------ |
| 目标   | 学习演示 —— 跑通核心链路，代码简洁，聚焦秒杀 |
| 周期   | 1-2 天                    |
| 仓库结构 | 前后端分仓库，独立项目              |

```
FlashSale/
├── FlashSaleBackend/          # Spring Boot 后端（已创建）
├── FlashSaleFrontend/         # Vue 3 前端（待创建）
├── docker-compose.yml         # RabbitMQ（Docker）
├── jmeter/                    # JMeter 压测脚本
└── OUTLINE.md                 # 本文档
```

---

## 二、技术栈

| 层    | 技术                                  | 版本      |
| ---- | ----------------------------------- | ------- |
| 框架   | Spring Boot                         | 4.1.0   |
| JDK  | Java                                | 21      |
| 持久层  | MyBatis-Plus                       | 3.5.12  |
| 数据库  | MySQL                               | 8.0（本地） |
| 缓存   | Redis + Jedis/Lettuce               | —（本地）   |
| 消息队列 | RabbitMQ + Spring AMQP              | Docker  |
| 前端   | Vue 3 + Vue Router 4 + Axios + Vite | 最新      |
| 压测   | JMeter                              | —       |
| 工具   | Lombok, Hibernate Validator         | —       |
| 并发   | Spring @Async, ThreadPoolTaskExecutor, CompletableFuture | —       |

---

## 三、数据库设计（3 张表）

### goods — 商品表（含秒杀字段）

```sql
CREATE TABLE goods (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL COMMENT '商品名称',
    title           VARCHAR(500)    DEFAULT '' COMMENT '商品描述',
    image           VARCHAR(500)    DEFAULT '' COMMENT '商品图片',
    price           DECIMAL(10,2)   NOT NULL COMMENT '原价',
    seckill_price   DECIMAL(10,2)   NOT NULL COMMENT '秒杀价',
    stock_count     INT             NOT NULL COMMENT '秒杀库存',
    start_time      DATETIME        NOT NULL COMMENT '秒杀开始时间',
    end_time        DATETIME        NOT NULL COMMENT '秒杀结束时间',
    version         INT             DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### seckill_order — 秒杀订单（防重）

```sql
CREATE TABLE seckill_order (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    goods_id    BIGINT NOT NULL,
    order_id    BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_goods (user_id, goods_id)  -- 防重复购买
);
```

### order_info — 订单详情

```sql
CREATE TABLE order_info (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    goods_id    BIGINT NOT NULL,
    goods_name  VARCHAR(100)    NOT NULL,
    goods_count INT             DEFAULT 1,
    goods_price DECIMAL(10,2)   NOT NULL COMMENT '成交价',
    status      TINYINT         DEFAULT 0 COMMENT '0=新建 1=已支付 2=已发货 3=已完成',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
    pay_time    DATETIME        DEFAULT NULL
);
```

---

## 四、核心秒杀流程

```
用户（已登录，带 Token）
    │
    ▼
POST /api/seckill/{goodsId}
    │
    ├─[1] Redis 计数器限流           ← 拦截器：N秒内单用户最多M次
    │
    ├─[2] 卖光标记检查               ← Redis Key: seckill:soldout:{goodsId}
    │     标记存在 → 直接返回 "已售罄"
    │
    ├─[3] Redis DECR 原子扣库存     ← 返回新库存值
    │     库存 < 0 → INCR 回滚并打卖光标记 → 返回 "库存不足"
    │
    ├─[4] 防重检查                   ← Redis SETNX: seckill:user:{userId}:{goodsId}
    │     SETNX 失败 → 返回 "请勿重复购买"
    │
    ├─[5] 发送 RabbitMQ 消息        ← seckill.queue: {userId, goodsId}
    │
    └─[6] 立即响应 "排队中"          ← HTTP 200 {code:0, msg:"排队中"}
    │
    ▼
前端轮询 GET /api/order/result/{goodsId}（每 500ms）
    │
    ▼
RabbitMQ 消费者
    ├─ DB 查库存：UPDATE goods SET stock_count = stock_count - 1
    │                                      WHERE id = ? AND stock_count > 0
    ├─ 插入 order_info
    ├─ 插入 seckill_order（唯一索引兜底幂等）
    └─ Redis 写入订单结果: seckill:result:{userId}:{goodsId} = orderId
```

---

## 五、后端分包结构（按层）

```
com.kisssheep.flashsalebackend
├── FlashSaleBackendApplication.java
├── controller/
│   ├── AuthController.java         # POST /api/user/login
│   ├── GoodsController.java        # GET /api/goods (商品列表，带缓存)
│   └── SeckillController.java      # POST /api/seckill/{goodsId}, GET /api/order/result/{goodsId}
├── service/
│   ├── UserService.java
│   ├── GoodsService.java
│   └── SeckillService.java         # Redis 扣库存 + MQ 发送
├── mapper/  (或 repository/)       # MyBatis-Plus Mapper 接口
├── entity/
│   ├── Goods.java
│   ├── SeckillOrder.java
│   ├── OrderInfo.java
│   └── User.java
├── config/
│   ├── RedisConfig.java
│   ├── RabbitMQConfig.java
│   └── WebConfig.java              # 拦截器注册
├── mq/
│   ├── SeckillProducer.java        # MQ 发送者
│   └── SeckillConsumer.java        # MQ 消费者（@RabbitListener）
├── interceptor/
│   ├── LoginInterceptor.java       # Token 校验
│   └── RateLimitInterceptor.java   # Redis 限流
├── common/
│   ├── Result.java                 # 统一响应 {code, msg, data}
│   ├── CodeMsg.java               # 错误码枚举
│   └── GlobalExceptionHandler.java # @ControllerAdvice
└── dto/
    ├── LoginDTO.java
    └── SeckillResultDTO.java
```

---

## 六、前端设计（独立 Vue 3 项目）

### 页面 & 路由

| 路由                  | 页面              | 功能                        |
| ------------------- | --------------- | ------------------------- |
| `/` → 重定向到 `/goods` | —               | 自动跳转                      |
| `/login`            | LoginView.vue   | 登录表单，Token 存 localStorage |
| `/goods`            | GoodsList.vue   | 商品列表 + 秒杀按钮 + 倒计时         |
| `/order/:goodsId`   | OrderResult.vue | 轮询展示秒杀结果                  |

### 组件

| 组件            | 用途          |
| ------------- | ----------- |
| NavBar.vue    | 顶部导航 + 用户信息 |
| CountDown.vue | 秒杀倒计时       |

### API 模块

```
src/api/
├── request.js          # Axios 实例 + 拦截器（自动带 Token）
├── auth.js             # POST /api/user/login
├── goods.js            # GET /api/goods
└── seckill.js          # POST /api/seckill/{goodsId}, GET /api/order/result/{goodsId}
```

---

## 七、环境 & 部署

### docker-compose.yml（只容器化 RabbitMQ）

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  rabbitmq_data:
```

MySQL 和 Redis 使用本地已安装的服务。

### 依赖需要用到的 pom.xml 补充

```xml
<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<!-- Spring Session Redis（Token 存储用） -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

---

## 八、优化项（全选）

| 项      | 方案                             | 效果                 |
| ------ | ------------------------------ | ------------------ |
| ✅ 启动预热 | `@PostConstruct` 加载商品库存到 Redis | 避免首请求冷启动           |
| ✅ 页面缓存 | 商品列表 Redis 缓存，TTL 60s          | 减少 DB 查询           |
| ✅ 卖光标记 | Redis `soldout:{goodsId}` 布尔标记 | 库存=0 后直接拒绝，不调 DECR |
| ✅ 异步下单 | MQ 消费者并发消费                              | 提高下单吞吐量             |
| ✅ 线程池   | `ThreadPoolTaskExecutor` 统一管理线程          | 避免线程泄漏，资源可控         |
| ✅ 并行预热 | `CompletableFuture` 并行加载商品库存到 Redis     | 启动更快                 |

---

## 九、API 清单

| 方法   | 路径                            | 认证    | 说明          |
| ---- | ----------------------------- | ----- | ----------- |
| POST | `/api/user/login`             | 无     | 登录，返回 Token |
| GET  | `/api/goods`                  | 无     | 商品列表（含秒杀状态） |
| POST | `/api/seckill/{goodsId}`      | Token | 执行秒杀        |
| GET  | `/api/order/result/{goodsId}` | Token | 轮询秒杀结果      |

---

## 十、开发顺序

```
Day 1 上午    环境搭建 + 建表 + 配置中间件 + docker-compose
Day 1 下午    后端：Entity → Repository → Service → Controller
              → 公共模块(Result/异常/限流) → MQ配置
Day 2 上午    前端：Vue项目创建 → 路由 → 3个页面 → API封装
Day 2 下午    JMeter 压测 → 调优 → 写 README
```







实际开发

- 引入依赖 缺失的依赖在pom文件中完成引入
