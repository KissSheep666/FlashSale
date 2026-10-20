# JMeter 对 FlashSale 秒杀系统压测 — 完整指南

> 从零开始，一步一步对秒杀接口做压力测试。

---

## 一、前置要求

| 环境     | 要求                                     |
| -------- | ---------------------------------------- |
| Java     | JDK 17+（和 JMeter 运行需要）            |
| JMeter   | 5.6.3+（[下载](https://jmeter.apache.org/download_jmeter.cgi)） |
| 后端服务 | FlashSaleBackend 已启动（`mvn spring-boot:run`） |
| MySQL    | flashsale 库已建表并插入测试数据         |
| Redis    | localhost:6379 可连接                    |
| RabbitMQ | localhost:5672 可连接                    |

---

## 二、安装 JMeter

### Windows 方式一：直接下载解压

```bash
# 1. 下载 Apache JMeter 5.6.3
#    https://jmeter.apache.org/download_jmeter.cgi
#    选 Binaries → apache-jmeter-5.6.3.zip

# 2. 解压到 D:\jmeter\

# 3. 启动 GUI
D:\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat
```

### Windows 方式二：winget

```bash
winget install Apache.JMeter
```

### 验证安装

```bash
jmeter --version
# 输出类似: Apache JMeter 5.6.3
```

---

## 三、压测目标 & 场景设计

### API 清单

| 方法 | 路径                            | 说明         | 是否需要 Token |
| ---- | ------------------------------- | ------------ | -------------- |
| POST | `/api/user/login`               | 登录拿 Token | 否             |
| POST | `/api/seckill/{goodsId}`        | 执行秒杀     | 是             |
| GET  | `/api/order/result/{goodsId}`   | 轮询秒杀结果 | 是             |

### 压测场景

```
场景：200 并发用户在 60 秒内持续发起秒杀请求

Step 1 — 登录预热（setUp 线程组）
  100 用户先登录，获取 Token 并缓存到 JMeter 属性中

Step 2 — 秒杀主压测（主线程组）
  200 线程，10 秒预热，持续 60 秒
  每个线程循环执行：
    ① POST /api/seckill/1     → 秒杀商品ID=1
    ② 等 200ms
    ③ GET /api/order/result/1  → 轮询结果
```

### 预期行为和验证点

| 指标                  | 预期                                              |
| --------------------- | ------------------------------------------------- |
| 限流拦截器            | 同一用户 5 秒内超过 5 次 → 返回 code=500206       |
| 库存扣减正确性        | Redis DECR + DB stock_count 总和为 100（初始值）  |
| 不超卖                | 下单数 ≤ stock_count（100）                       |
| 防重                  | 同一用户同一商品只能下单 1 次                      |
| TPS                   | 目标 > 500 req/s                                  |
| 平均响应时间          | < 50ms（秒杀接口本身只做 Redis 操作）             |

---

## 四、使用已生成的 JMX 脚本（推荐）

已提供两个文件，开箱即用：

```
jmeter/
├── flashsale-load-test.jmx   # JMeter 测试计划
└── users.csv                  # 测试用户（手机号+密码）
```

### 4.1 直接打开

```bash
# 方式一：GUI 打开
jmeter -t jmeter/flashsale-load-test.jmx

# 方式二：从 JMeter GUI → File → Open → 选择这个 .jmx 文件
```

### 4.2 修改配置（按需）

在 JMeter GUI 中点击根节点 **"FlashSale 秒杀压测计划"**，修改：

| 变量名      | 默认值          | 说明             |
| ----------- | --------------- | ---------------- |
| `BASE_URL`  | `localhost:8080` | 后端地址         |
| `GOODS_ID`  | `1`              | 秒杀商品 ID      |

### 4.3 调整压测强度

在 **"02-秒杀请求（主压测）"** 线程组中可调：

| 参数           | 默认值 | 作用                    |
| -------------- | ------ | ----------------------- |
| 线程数          | 200    | 并发用户数              |
| Ramp-Up 时间    | 10 秒  | 预热时间（逐步加到满额）|
| 持续时间        | 60 秒  | 压测总时长              |

### 4.4 运行

```bash
# 命令行运行（不弹 GUI，适合服务器）
jmeter -n -t jmeter/flashsale-load-test.jmx -l result.jtl -e -o report/

# 参数说明：
#   -n           non-GUI 模式
#   -t           测试计划文件
#   -l           原始结果输出文件 (.jtl)
#   -e -o report/  生成 HTML 报告到 report/ 目录
```

---

## 五、手动创建测试计划（从零开始）

如果你更想理解每一步，按下面流程在 JMeter GUI 中手建。

### 5.1 创建 Test Plan

1. 打开 JMeter → 自动创建 **Test Plan**
2. 右键 Test Plan → Add → Config Element → **User Defined Variables**
3. 添加变量：
   ```
   BASE_URL = localhost:8080
   GOODS_ID = 1
   ```

### 5.2 HTTP 请求默认值

右键 Test Plan → Add → Config Element → **HTTP Request Defaults**：
- Server Name: `${BASE_URL}`
- Port: 留空

### 5.3 线程组 1：登录获取 Token（setUp）

右键 Test Plan → Add → Threads → **setUp Thread Group**：
- Number of Threads: `100`
- Ramp-up: `5`

在此线程组下添加：

**CSV Data Set Config**（Add → Config Element）：
- Filename: `users.csv`
- Variable Names: `phone,password`

**HTTP Request**（Add → Sampler）：
- Method: `POST`
- Path: `/api/user/login`
- Body Data: `{"phone":"${phone}","password":"${password}"}`

**HTTP Header Manager**（HTTP Request 右键 → Add → Config Element）：
- Name: `Content-Type`
- Value: `application/json`

**JSON Extractor**（HTTP Request 右键 → Add → Post Processors）：
- Names of created variables: `token`
- JSON Path expressions: `$.data`

**BeanShell PostProcessor**（把 token 存为全局属性）：
```java
String t = vars.get("token");
if (t != null && !t.isEmpty()) {
    props.put("token_" + (ctx.getThreadNum() % 100), t);
}
```

### 5.4 线程组 2：秒杀主压测

右键 Test Plan → Add → Threads → **Thread Group**：
- Number of Threads: `200`
- Ramp-up: `10`
- Duration: `60`
- 勾选 Scheduler

在此线程组下添加：

**BeanShell PreProcessor**（取 token）：
```java
int idx = ctx.getThreadNum() % 100;
String t = props.get("token_" + idx);
if (t != null) vars.put("myToken", t);
```

**HTTP Request — 秒杀**：
- Method: `POST`
- Path: `/api/seckill/${GOODS_ID}`
- 添加 HTTP Header Manager：
  - Name: `Authorization`
  - Value: `Bearer ${myToken}`

**HTTP Request — 轮询结果**：
- Method: `GET`
- Path: `/api/order/result/${GOODS_ID}`
- 同样添加 Authorization Header

**Constant Timer**（Add → Timer）：
- Delay: `200` ms

### 5.5 添加监听器

右键 Test Plan / 线程组 → Add → Listener：
1. **View Results Tree** — 查看每个请求的详情（调试用，正式压测时禁用）
2. **Summary Report** — 实时 TPS / 平均响应时间
3. **Aggregate Report** — 聚合统计（中位数、90%/95%/99% 线）

---

## 六、运行压测

### GUI 模式（调试）

```bash
# 启动 JMeter 并打开测试计划
jmeter -t jmeter/flashsale-load-test.jmx

# 在 GUI 中点击绿色 ▶ 运行按钮
# 注意：GUI 模式本身消耗资源，200+ 并发时建议用 CLI 模式
```

### CLI 模式（正式压测）

```bash
# 1. 确保没有上次的残留结果
rm -f result.jtl
rm -rf report/

# 2. 运行压测
jmeter -n \
  -t jmeter/flashsale-load-test.jmx \
  -l result.jtl \
  -e -o report/

# 3. 压测完成后打开 HTML 报告
start report/index.html     # Windows
open report/index.html      # Mac
```

### 输出示例

```
Summary +     26 in 00:00:01 =   21.3/s Avg:    35 Min:     8 Max:   512 Err:     0 (0.00%)
Summary +    521 in 00:00:10 =   52.1/s Avg:    28 Min:     5 Max:   321 Err:     0 (0.00%)
Summary =   5104 in 00:01:00 =   85.1/s Avg:    31 Min:     4 Max:   823 Err:     3 (0.06%)
```

---

## 七、常用压测场景

### 场景 A：极限并发（寻找瓶颈）

| 参数       | 值    |
| ---------- | ----- |
| 线程数     | 500   |
| Ramp-up    | 10s   |
| 持续时间   | 120s  |

观察：TPS 是否随线程增加而提升？何时出现拐点？

### 场景 B：突发流量（秒杀开启瞬间）

| 参数       | 值   |
| ---------- | ---- |
| 线程数     | 1000 |
| Ramp-up    | 1s   |
| 循环次数   | 1    |

观察：1 秒内 1000 人同时抢，系统是否崩溃？

### 场景 C：持续压力（稳定性测试）

| 参数       | 值    |
| ---------- | ----- |
| 线程数     | 100   |
| Ramp-up    | 30s   |
| 持续时间   | 600s  |

观察：10 分钟持续压测，内存/CPU 是否稳定？有无内存泄漏？

---

## 八、结果分析 & 优化

### 关键指标

| 指标       | 含义                                      | 好    | 坏     |
| ---------- | ----------------------------------------- | ----- | ------ |
| **TPS**    | 每秒处理请求数                            | >500  | <100   |
| **Avg**    | 平均响应时间 (ms)                         | <50   | >500   |
| **99% Line** | 99% 的请求在这个时间内完成               | <100  | >1000  |
| **Error%** | 错误率（排除业务拒绝如"已售罄"）          | <0.1% | >1%    |
| **Std Dev**| 标准差（越大表示波动越大，越不稳定）      | <100  | >500   |

### 常见问题排查

| 问题                      | 可能原因                        | 解决方案                         |
| ------------------------- | ------------------------------- | -------------------------------- |
| TPS 上不去，CPU 低         | 数据库连接池耗尽                | 增大连接池 `max-active`          |
| 响应时间抖动很大           | GC 暂停                         | 调 JVM 参数 `-Xmx -Xms`          |
| 大量连接超时              | Redis/RabbitMQ 连接不够         | 增大 Lettuce/RabbitMQ 连接池     |
| 错误率飙升                | 限流触发 / 库存不足             | 检查业务 code，区分正常拒绝       |
| "Connection refused"      | 端口耗尽 / TIME_WAIT            | 调整系统 TCP 参数 / 加连接池       |

### 调优顺序

```
1. 先跑一遍，记录基线 TPS 和 Avg RT
2. 查看 View Results Tree，确认 Token 传递正确
3. 观察 Redis：redis-cli MONITOR 看 DECR 是否高频执行
4. 观察 RabbitMQ 管理界面：队列是否堆积
5. 增加线程 → 找到 TPS 瓶颈拐点 → 针对性优化
```

---

## 九、补充：模拟更多用户

当前 `users.csv` 只有 2 个测试用户，增加更多模拟用户：

```csv
phone,password
13800000001,123456
13800000002,123456
13800000003,123456
13800000004,123456
...（手动生成或用脚本批量造）
```

或者直接写个脚本往 `user` 表批量插入：

```sql
-- 批量造 N 个用户，密码都是 123456, salt=1a2b3c4d
INSERT INTO user (phone, password, salt, nickname)
SELECT
    CONCAT('138', LPAD(n, 8, '0')),
    'b7797c6b0187825e79db224b88c12831',
    '1a2b3c4d',
    CONCAT('压测用户', n)
FROM (
    SELECT @row := @row + 1 AS n
    FROM information_schema.columns, (SELECT @row := 2) r
    LIMIT 500
) t;
```

---

## 十、快速命令速查

```bash
# GUI 打开
jmeter -t jmeter/flashsale-load-test.jmx

# CLI 运行压测
jmeter -n -t jmeter/flashsale-load-test.jmx -l result.jtl -e -o report/

# 只生成 HTML 报告（基于已有 .jtl）
jmeter -g result.jtl -o report/

# 分布式压测（Master 节点）
jmeter -n -t flashsale-load-test.jmx -R slave1,slave2 -l result.jtl

# 调整 JVM 堆内存（编辑 jmeter.bat / jmeter.sh 中的 HEAP 参数）
# 默认 -Xms1g -Xmx1g，大量并发时改成 -Xms4g -Xmx4g
```
