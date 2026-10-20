# Java 技术栈学习路线

> 按顺序学，标 ✅ 的是和本秒杀项目相关的，算是你当前正在攻克的。

---

## 一、地基

```
Java 基础
├── 语法：变量、运算符、流程控制、数组
├── 面向对象：封装、继承、多态、抽象类、接口、内部类
├── 异常：try-catch-finally、throws、自定义异常
├── 常用类：String、包装类、Math、UUID、日期时间类
├── 集合 ✅
│   ├── ArrayList / LinkedList
│   ├── HashMap（1.7 数组+链表 → 1.8 数组+链表+红黑树）
│   ├── HashSet / TreeSet
│   └── ConcurrentHashMap（分段锁 → CAS+synchronized）
├── 泛型
├── 注解
├── 反射
├── IO / NIO
├── Lambda + Stream ✅（GoodsService 里 listGoods 用了 stream().map().collect()）
└── JDBC
```

---

## 二、数据库

```
MySQL ✅
├── SQL：SELECT / JOIN / GROUP BY / 子查询 / UNION
├── 函数：COUNT / SUM / AVG / IF / CASE WHEN
├── 索引
│   ├── B+Tree 结构、聚簇索引 vs 二级索引
│   ├── 最左前缀原则
│   ├── explain 执行计划
│   ├── 覆盖索引、索引下推
│   └── 索引失效场景（函数、隐式转换、LIKE '%xx'）
├── 事务
│   ├── ACID
│   ├── 隔离级别（读未提交/读已提交/可重复读/串行）
│   ├── MVCC（版本链 + ReadView）
│   └── undo log / redo log / binlog
├── 锁
│   ├── 行锁（Record Lock / Gap Lock / Next-Key Lock）
│   ├── 表锁、意向锁
│   └── 死锁排查
├── SQL 优化
│   ├── 慢查询日志
│   ├── 分页优化（深分页问题）
│   └── 大表 DDL（pt-osc / gh-ost）
└── 设计
    ├── 三大范式 + 反范式
    ├── 分库分表（ShardingSphere）
    └── 读写分离
```

---

## 三、框架

```
Spring Boot ✅
├── IoC 容器
│   ├── Bean 生命周期（实例化→属性注入→初始化→使用→销毁）
│   ├── 作用域（singleton / prototype / request / session）
│   ├── @Component / @Service / @Repository / @Controller
│   └── @Configuration + @Bean
├── DI 依赖注入
│   ├── @Autowired vs @Resource
│   ├── 构造器注入 vs 属性注入（为什么推荐构造器）✅
│   └── 循环依赖（三级缓存）
├── AOP 切面编程
│   ├── JDK 动态代理 vs CGLIB
│   ├── @Aspect / @Before / @After / @Around
│   └── @Transactional 原理（AOP + 动态代理 → 失效场景）
├── Spring MVC
│   ├── DispatcherServlet 请求流程
│   ├── @RestController / @RequestMapping ✅
│   ├── 拦截器 vs 过滤器（HandlerInterceptor vs Filter）
│   ├── 参数校验 @Valid / @Validated ✅
│   └── 统一异常处理 @ControllerAdvice ✅
├── Spring Boot 自动配置
│   ├── @SpringBootApplication 做了什么
│   ├── spring.factories / @EnableAutoConfiguration
│   └── 条件注解 @ConditionalOnClass / @ConditionalOnBean
├── 配置：application.yml / 多环境 profile
├── starter 机制：自己写一个 starter
└── 定时任务：@Scheduled / 分布式定时任务（XXL-JOB）

MyBatis-Plus ✅
├── BaseMapper 自带 CRUD
├── LambdaQueryWrapper 条件构造器
├── 分页插件
├── 自动填充（createTime / updateTime）
├── 逻辑删除
├── @Version 乐观锁
├── 自定义 SQL（注解 / XML）✅
├── 动态表名、多数据源
└── 代码生成器
```

---

## 四、缓存

```
Redis ✅
├── 5 种类型及应用场景
│   ├── String：库存计数、分布式锁、Token 存储 ✅
│   ├── Hash：用户信息、购物车
│   ├── List：消息队列、时间线
│   ├── Set：标签、共同好友、抽奖去重
│   └── ZSet：排行榜、延迟队列
├── 秒杀场景 Key 设计 ✅
│   ├── seckill:stock:{goodsId}        ← DECR 扣库存
│   ├── seckill:soldout:{goodsId}      ← 卖光标记
│   ├── seckill:order:{userId}:{goodsId} ← SETNX 防重
│   └── seckill:result:{userId}:{goodsId} ← 轮询结果
├── 三大问题（面试必问）
│   ├── 缓存穿透：布隆过滤器 / 空值缓存
│   ├── 缓存击穿：互斥锁 / 逻辑过期
│   └── 缓存雪崩：随机 TTL / 多级缓存 / 熔断降级
├── 数据一致性：先删缓存还是先写 DB？延迟双删 / Canal
├── 持久化：RDB（快照）vs AOF（增量日志）、混合持久化
├── 高可用：主从、哨兵、Cluster（16384 个 slot）
├── 内存淘汰策略：LRU / LFU / TTL / allkeys / volatile
├── 分布式锁：Redisson（看门狗续期 + 红锁）
├── Redis 6/7 新特性：多线程 IO、RESP3
├── 客户端：Lettuce（默认）vs Jedis
└── 常见故障：大 Key、热 Key、OOM
```

---

## 五、消息队列

```
RabbitMQ ✅
├── 核心概念：Producer / Exchange / Queue / Consumer / Binding
├── Exchange 类型 ✅
│   ├── Direct：精确匹配 RoutingKey（秒杀项目用这个）
│   ├── Fanout：广播到所有绑定队列
│   ├── Topic：通配符匹配（* 单段 / # 多段）
│   └── Headers：用 Header 匹配
├── 可靠性
│   ├── 生产端：confirm 模式 / return 回调
│   ├── Broker：队列 + 消息持久化（durable）
│   ├── 消费端：手动 ACK（acknowledge-mode: manual）✅
│   └── 死信队列（DLX）：重试 N 次后转入死信，人工处理
├── 重复消费（幂等）✅
│   ├── DB 唯一索引兜底（seckill_order 的 uk_user_goods）
│   └── Redis SETNX 预处理
├── 消息顺序性：单队列单消费者 / 业务 ID 哈希
├── Spring AMQP：RabbitTemplate / @RabbitListener ✅
├── 常用命令：rabbitmqctl、管理界面排查

Kafka（进阶）
├── 核心概念：Broker / Topic / Partition / Consumer Group
├── 高吞吐原因：顺序读写磁盘、零拷贝、批量发送
├── 消息不丢：ACK / ISR / min.insync.replicas
├── 重复消费：幂等生产者 + 消费者幂等
├── 顺序消费：单 Partition 内有序
├── 消费者重平衡（Rebalance）
├── 时间轮、压缩算法
├── Kafka Streams / Kafka Connect
└── 选型：高吞吐 → Kafka / 业务解耦小消息 → RabbitMQ
```

---

## 六、并发编程

```
Java 并发 ✅（部分用到）
├── 线程基础
│   ├── 线程生命周期（NEW/RUNNABLE/BLOCKED/WAITING/TERMINATED）
│   ├── Thread vs Runnable vs Callable
│   └── 线程的 6 种状态
├── synchronized
│   ├── 对象头（Mark Word）
│   ├── 锁升级：无锁→偏向锁→轻量级锁→重量级锁
│   └── 锁粗化 / 锁消除
├── volatile：可见性 + 禁止指令重排（DCL 单例）
├── JMM（Java 内存模型）
│   ├── 主内存 vs 工作内存
│   └── happens-before 原则
├── AQS
│   ├── state + CLH 队列
│   ├── ReentrantLock（公平/非公平、可中断、超时）
│   ├── CountDownLatch / CyclicBarrier / Semaphore
│   └── ReadWriteLock（读写锁：读共享写互斥）
├── 线程池 ✅
│   ├── ThreadPoolExecutor 7 个参数（核心/最大/队列/TTL/工厂/拒绝）
│   ├── 四种拒绝策略：CallerRuns/Abort/Discard/DiscardOldest ✅
│   ├── 四种内置线程池：Fixed/Cached/Single/Scheduled（为什么不用）
│   ├── Spring ThreadPoolTaskExecutor ✅
│   └── 动态线程池（调整参数不重启）
├── CompletableFuture ✅
│   ├── supplyAsync / runAsync
│   ├── thenApply / thenCompose / thenCombine
│   ├── allOf / anyOf
│   ├── 异常处理：exceptionally / handle
│   └── 一定传自定义线程池！
├── ConcurrentHashMap
│   └── 1.7 Segment 分段锁 → 1.8 CAS+synchronized+(数组+链表+红黑树)
├── ThreadLocal ✅
│   ├── Thread → ThreadLocalMap → Entry(弱引用)
│   ├── 内存泄漏原因 + remove() ✅
│   └── 线程池场景（线程复用导致值残留）
├── BlockingQueue：ArrayBlockingQueue / LinkedBlockingQueue
└── ForkJoinPool：工作窃取、parallelStream 陷阱
```

---

## 七、JVM

```
JVM
├── 内存结构（Run-Time Data Areas）
│   ├── 堆（Heap）：年轻代(Eden+S0+S1) + 老年代 + 字符串常量池
│   ├── 方法区/元空间（Method Area / Metaspace）：类信息、常量
│   ├── 虚拟机栈（Stack）：栈帧（局部变量表+操作数栈+返回地址）
│   ├── 本地方法栈
│   └── 程序计数器（PC Register）
├── 对象
│   ├── 创建过程：类加载→分配内存→初始化零值→设置对象头→init
│   ├── 内存分配：指针碰撞 / 空闲列表、TLAB
│   ├── 对象头（Mark Word + Klass Pointer）
│   └── 判断死亡：引用计数、可达性分析（GCRoots）
├── GC
│   ├── 分代理论：弱分代、强分代、跨代引用（卡表）
│   ├── 标记-清除、标记-复制、标记-整理
│   ├── 新生代 GC：Serial / ParNew / Parallel Scavenge
│   ├── 老年代 GC：Serial Old / Parallel Old / CMS
│   ├── 大杀器 G1：Region + SATB + 并发标记 + MixedGC
│   ├── ZGC / Shenandoah：亚毫秒级暂停、染色指针
│   └── GC 日志 + 选型策略
├── 类加载
│   ├── 加载→验证→准备→解析→初始化（加载/连接/初始化）
│   ├── 双亲委派模型 & 破坏（Tomcat/SPI）
│   └── 自定义类加载器
├── 调优
│   ├── 常用参数：-Xms -Xmx -Xss -XX:+UseG1GC
│   ├── jps / jstack（线程栈）/ jmap（内存 dump）/ jstat（GC 统计）
│   ├── jconsole / jvisualvm / Arthas（阿里线上诊断神器）
│   ├── MAT（分析 dump 文件）
│   └── Full GC 频繁怎么排查？
└── 四种引用：强/软/弱/虚
```

---

## 八、分布式

```
分布式基础
├── CAP 定理（一致性/可用性/分区容错 → 最多同时满足两个）
├── BASE 理论（基本可用 + 软状态 + 最终一致性）
├── 分布式 ID
│   ├── UUID（无序、不适合主键）
│   ├── 雪花算法（Snowflake：1b+41b+10b+12b）
│   └── 号段模式（Leaf）
├── 分布式锁
│   ├── Redis：SETNX + Lua 解锁、Redisson 看门狗
│   └── Zookeeper：临时顺序节点 + Watch
├── 分布式事务
│   ├── 2PC / 3PC（XA）
│   ├── TCC（Try-Confirm-Cancel）
│   ├── 本地消息表 + 定时任务
│   ├── MQ 事务消息（RocketMQ）
│   └── Seata（AT / TCC / Saga）
├── 服务调用
│   ├── HTTP + JSON（RestTemplate → OpenFeign）
│   ├── RPC（Dubbo）
│   └── gRPC（Protobuf）
├── 注册中心：Nacos / Eureka / Zookeeper / Consul
├── 配置中心：Nacos / Apollo
├── 网关：Gateway / Zuul / Nginx + Lua
├── 限流
│   ├── 算法：计数器、滑动窗口、漏桶、令牌桶
│   ├── 单机：Guava RateLimiter
│   └── 分布式：Sentinel（QPS + 线程 + 熔断降级）
├── 熔断降级：Sentinel / Hystrix / Resilience4j
└── 链路追踪：SkyWalking / Zipkin
```

---

## 九、运维 + 部署

```
运维
├── Linux
│   ├── 文件操作：ls/cd/mv/rm/find/tail/grep/awk/sed
│   ├── 权限：chmod/chown
│   ├── 系统：top/free/df/ps/netstat/lsof
│   ├── 磁盘：du/iostat
│   └── Shell 脚本：启动/停止/备份
├── Docker
│   ├── 镜像 vs 容器
│   ├── Dockerfile（FROM/RUN/COPY/CMD/ENTRYPOINT）
│   ├── docker-compose（多容器编排）
│   └── 常用命令：build/run/exec/logs/ps/rm
├── K8s（大厂标配）
│   ├── Pod / Service / Deployment / Ingress
│   ├── ConfigMap / Secret
│   ├── PV / PVC
│   └── HPA（水平扩缩容）
├── CI/CD
│   ├── Jenkins Pipeline
│   ├── GitHub Actions
│   └── GitLab CI
├── Nginx：反向代理、负载均衡、静态资源
└── 监控告警
    ├── Prometheus + Grafana
    └── ELK（日志采集）
```

---

## 十、必备工具

```
Git ✅：add/commit/push/pull/branch/merge/rebase/cherry-pick
Maven ✅：依赖管理、多模块、常用插件
IDEA ✅：Debug、快捷键、Live Templates、插件
Postman / Apifox：接口调试
JMeter ✅：压测
Arthas：线上诊断
Charles / Fiddler：抓包
```

---

## 你现在覆盖的

```
✅ Java 基础（大部分）
✅ 集合（HashMap 原理要再深入）
✅ MySQL（基础 CRUD + 索引入门）
✅ Spring Boot（IoC/MVC/AOP 基本会用）
✅ MyBatis-Plus
✅ Redis（5 种类型 + 秒杀场景 Key 设计）
✅ RabbitMQ（DirectExchange + ACK + 幂等）
✅ 线程池（ThreadPoolTaskExecutor）
✅ ThreadLocal
✅ Sa-Token
✅ Maven + Git + IDEA

⏳ 正在学/即将学
   CompletableFuture 并行任务
   MQ 可靠性（confirm + 死信）
   缓存三大问题（穿透/击穿/雪崩）

⬜ 之后
   JVM
   分布式（Spring Cloud）
   Docker / K8s
   Kafka
   HashMap 源码
   AQS 源码
```

---

## 推荐书单

| 阶段 | 书 | 说明 |
|------|-----|------|
| 入门 | 《Java 核心技术 卷一》 | 当字典查，别通读 |
| 进阶 | 《Java 并发编程的艺术》 | 并发入门的教科书 |
| 深入 | 《深入理解 Java 虚拟机》（周志明） | 国内最好的 JVM 书 |
| 网络 | 《Netty 实战》 | Netty 必看 |
| 架构 | 《凤凰架构》（周志明） | 分布式架构演进 |
| MySQL | 《MySQL 实战 45 讲》（极客时间） | 比书好使 |
| Redis | 《Redis 设计与实现》 | 源码级别的理解 |
| 设计 | 《设计模式之禅》 | 比 GoF 好读 |
