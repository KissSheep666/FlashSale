# FlashSale 开发指南

> 高并发秒杀系统 · 后端已全部实现（Spring Boot 4.1 + Java 21）
> 本文档**代码全部来自真实项目**（`FlashSaleBackend/`），可对照阅读；每一步都带注释讲原理，按顺序走一遍就能吃透整个系统。遇到问题叫我。

---

## 当前状态

```
✅ 后端已完成
   ├── ✅ 启动类 + pom.xml（Spring Boot 4.1.0 / Java 21 / MyBatis-Plus 3.5.12 / Sa-Token 1.44.0）
   ├── ✅ application.yml（MySQL:3306 / Redis:6379 / RabbitMQ:5672 + Sa-Token 配置）
   ├── ✅ schema.sql（4 张表 + 测试数据）
   ├── ✅ Entity ×4（User / Goods / OrderInfo / SeckillOrder）
   ├── ✅ DTO ×3（LoginDto / GoodsDTO / SeckillResultDTO）
   ├── ✅ Common ×5（Result / CodeMsg / GlobalException / GlobalExceptionHandler / RedisTypeString）
   ├── ✅ Config ×4（Redis / RabbitMQ / Web / ThreadPool）
   ├── ✅ 拦截器 ×2（LoginInterceptor + RateLimitInterceptor）
   ├── ✅ Service 接口 ×3 + Impl 实现 ×3
   ├── ✅ MQ（SeckillMessage + SeckillProducer + SeckillConsumer）
   ├── ✅ Controller ×3（Auth / Goods / Seckill）
   └── ✅ JMeter 压测脚本（jmeter/）

⏳ 待做
   ├── 前端 Vue 3 项目（FlashSaleFrontend 待创建）
   ├── 接口联调 + 压测验证（jmeter 脚本已备好）
   └── 潜在问题修复（见文末"已知风险"）
```

---

## Git：从 GitHub 拉取最新版本

> 项目托管在 GitHub：`https://github.com/KissSheep666/FlashSale.git`
> 本地仓库已配置好远程（`git remote -v` 查看），当前分支 `main`，已跟踪 `origin/main`。

### 核心原理（先理解再敲命令）

1. **本地和远程是两个独立的 Git 仓库**，平时不通信，只有执行 `fetch` / `pull` / `push` 才交换数据，Git 没有「自动同步」。
2. **一次提交 = 一张项目快照 + 指向父提交的指针**，历史是一条由快照串成的链。
3. **分支只是一个贴在某个提交上的标签**：
   - `main`：本地自己的分支指针
   - `origin/main`：远程分支的「本地镜像」，记录上次 fetch 时远程的样子
4. **拉取 = 下载 + 合并**：
   - `git fetch`：把远程新提交下载到本地 `.git/`，只更新 `origin/main` 指针，**不碰工作区**（永远安全）
   - `git merge`：把 `origin/main` 合进当前分支
   - `git pull` = `fetch` + `merge`，一步到位

### 日常拉取（三选一）

```bash
# 方式一：一条命令搞定（最常用）
git pull

# 方式二：先看后合（学习阶段推荐）
git fetch origin
git status            # 先看本地落后 / 领先几条
git merge origin/main # 确认没问题再合

# 方式三：显式指定远程和分支
git pull origin main
```

### 合并的两种结果

| 情况 | Git 的行为 | 结果 |
| --- | --- | --- |
| 本地没新提交（Fast-forward 快进） | 直接挪 `main` 指针 | 历史一条直线，无 merge 提交 |
| 本地和远程都改了（分叉） | 找共同祖先 → 三方合并 | 产生一个 merge 提交 |

`git pull --rebase`：把本地提交「重新放到」远程最新提交之后，历史更干净（一条直线），但会改写提交历史，只用于还没推给别人的本地提交。

### 本地有未提交改动时

```bash
git stash        # 把改动暂存起来，工作区恢复干净
git pull         # 拉取最新
git stash pop    # 取回你的改动
```

### 完全放弃本地、强制同步成远程（危险！）

> ⚠️ 会**永久丢失**本地所有未提交的改动，慎用。

```bash
git fetch origin
git reset --hard origin/main
```

### 验证同步成功

```bash
git status           # 显示 up to date / nothing to commit 即同步完成
git log --oneline -5 # 看最新几条提交
```

### 拉取时出现冲突怎么办

只有**同一文件的同一处**两边都改了才会冲突。Git 会在文件里标出：

```text
<<<<<<< HEAD
本地版本
=======
远程版本
>>>>>>> origin/main
```

手动改成想要的内容后：

```bash
git add <冲突文件>
git commit -m "解决合并冲突"
```

---

## 环境速查

| 组件       | 地址                     | 账号/密码         |
| -------- | ---------------------- | ------------- |
| MySQL    | localhost:3306         | root / 环境变量 MYSQL_PASSWORD |
| Redis    | localhost:6379         | 无密码           |
| RabbitMQ | localhost:5672         | guest / guest |
| RabbitMQ 管理界面 | http://localhost:15672 | guest / guest |

> **MySQL 密码不写进仓库。** 启动后端前先设置环境变量：
>
> ```powershell
> $env:MYSQL_PASSWORD = "你的 MySQL 密码"
> ```
>
> 或者在 IDE 的运行配置 → Environment variables 里加一条 `MYSQL_PASSWORD`。
> `application.yml` 里读的是 `${MYSQL_PASSWORD}`，没设置会导致启动失败。

### 启动 RabbitMQ（Windows 本地版）

```bash
D:\rabbitmq\rabbitmq_server-4.3.2\sbin\rabbitmq-server.bat
```

也可以用仓库里的 `docker-compose.yml` 起一个容器版。

### 建表

用 Navicat/DataGrip 连接 MySQL，执行 `src/main/resources/schema.sql`（已含 4 张表 + 3 个商品 + 2 个测试用户）。

### 启动后端

```bash
cd FlashSaleBackend
mvn spring-boot:run
```

依赖下载超时可走代理：`mvn spring-boot:run -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890`（见 CLAUDE.md）。

---

## 技术栈（真实 pom.xml）

| 依赖 | 说明 |
| --- | --- |
| Spring Boot `4.1.0` + Java `21` | `spring-boot-starter-webmvc`（Boot 4 已拆出 webmvc 模块） |
| MyBatis-Plus `3.5.12` | `mybatis-plus-spring-boot3-starter`，Mapper 继承 `BaseMapper` 免写 CRUD |
| Sa-Token `1.44.0` | Token 登录认证，Session 存 Redis，`Authorization` 头传 Token |
| spring-session-data-redis | Session 持久化到 Redis |
| spring-boot-starter-data-redis | RedisTemplate / StringRedisTemplate |
| spring-boot-starter-amqp | RabbitMQ（Spring AMQP），手动 ACK |
| spring-boot-starter-validation | `@Valid` + `@NotBlank` 参数校验 |
| Lombok | `@Data` / `@RequiredArgsConstructor` / `@Slf4j` |
| spring-boot-devtools | 热重载（runtime） |

---

## 包结构（真实项目）

```
com.kisssheep.flashsalebackend
├── FlashSaleBackendApplication.java   # 启动类 @MapperScan
├── common/         # Result / CodeMsg / GlobalException / GlobalExceptionHandler / RedisTypeString
├── config/         # RedisConfig / RabbitMQConfig / WebConfig / ThreadPoolConfig
├── controller/     # AuthController / GoodsController / SeckillController
├── dto/            # LoginDto / GoodsDTO / SeckillResultDTO
├── entity/         # User / Goods / OrderInfo / SeckillOrder
├── interceptor/    # LoginInterceptor / RateLimitInterceptor
├── mapper/         # UserMapper / GoodsMapper / OrderInfoMapper / SeckillOrderMapper
├── mq/             # SeckillMessage / SeckillProducer / SeckillConsumer
└── service/
    ├── UserService / GoodsService / SeckillService   ← 接口
    └── Impl/                                           ← 实现类
        ├── UserServiceImpl
        ├── GoodsServiceImpl
        └── SeckillServiceImpl   # 核心秒杀逻辑
```

> **规范点**：Service 采用「接口 + Impl」模式。Controller 面向接口编程，换实现不换调用方；这也是真实项目里的常见分层。

---

## 第一步：启动类

```java
package com.kisssheep.flashsalebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FlashSale 秒杀系统启动类
 *
 * @MapperScan 告诉 MyBatis-Plus 去哪儿找 Mapper 接口
 * 路径和 mapper 包名对应，写错会报 "找不到 Mapper Bean"
 */
@SpringBootApplication
@MapperScan("com.kisssheep.flashsalebackend.mapper")
public class FlashSaleBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashSaleBackendApplication.class, args);
    }
}
```

---

## 第二步：Entity 实体类（对照 schema.sql）

> MyBatis-Plus 默认开启驼峰映射（yml 里 `map-underscore-to-camel-case: true`），
> 字段 `createTime` 自动对应列 `create_time`，所以 `@TableField` 能不写就不写。

### 2.1 User.java

```java
package com.kisssheep.flashsalebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户表 → user
 *
 * 密码存储规则：MD5(salt + 原始密码)，每个用户的 salt 随机生成
 * 验证时：MD5(查出来的 salt + 用户输入的密码) == 数据库 password
 */
@TableName("user")          // 指定映射的数据库表名
@Data                       // Lombok: getter/setter/toString/equals/hashCode
@AllArgsConstructor         // 全参构造（方便测试）
@NoArgsConstructor          // 无参构造（MyBatis-Plus 需要）
public class User {

    @TableId(type = IdType.AUTO)    // 主键自增，走 DB 的 AUTO_INCREMENT
    private Long id;

    private String phone;           // 手机号，唯一索引在 DB 层保证
    private String password;        // MD5(salt + 原始密码)
    private String salt;            // 密码盐值，注册时随机生成
    private String nickname;        // 用户昵称

    private LocalDateTime createTime;   // 驼峰自动映射 create_time
    private LocalDateTime updateTime;   // 驼峰自动映射 update_time
}
```

### 2.2 Goods.java

```java
package com.kisssheep.flashsalebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品表（含秒杀字段）→ goods
 *
 * 金额统一用 BigDecimal，千万不能用 double（0.1 + 0.2 != 0.3 在二进制浮点里是经典 bug）
 */
@TableName("goods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;            // 商品名称
    private String title;           // 商品描述
    private String image;           // 商品图片 URL

    /** 原价 */
    private BigDecimal price;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存 — 用 Integer 而非 int
     * Integer 可为 null，方便判断"是否加载过"
     */
    private Integer stockCount;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /**
     * 乐观锁版本号（预留字段，本项目的秒杀流程不使用）
     *
     * 乐观锁原理：UPDATE goods SET stock=?, version=version+1 WHERE id=? AND version=旧值
     * 本项目中 Redis 已用 DECR 原子扣库存，到 DB 时几乎不会冲突，所以暂未启用
     */
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 2.3 OrderInfo.java

```java
package com.kisssheep.flashsalebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情表 → order_info
 *
 * status 订单状态流转：
 *   0 = 新建（秒杀成功默认为 0）
 *   1 = 已支付
 *   2 = 已发货
 *   3 = 已完成
 *
 * 本项目只用到 status=0，后续支付流程是扩展方向
 */
@TableName("order_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long goodsId;
    private String goodsName;       // 下单时冗余存商品名（商品改名不影响历史订单）
    private Integer goodsCount;     // 购买数量，秒杀场景固定为 1
    private BigDecimal goodsPrice;  // 成交价（秒杀价）

    /** 0=新建, 1=已支付, 2=已发货, 3=已完成 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime payTime;  // 支付时间，新建订单时可为空
}
```

### 2.4 SeckillOrder.java

```java
package com.kisssheep.flashsalebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 秒杀订单表（防重复购买）→ seckill_order
 *
 * 唯一索引 uk_user_goods (user_id, goods_id) 在 schema.sql 已声明
 * 同一用户同一商品重复插入 → DuplicateKeyException → 最后一道防线
 * 正常情况 Redis SETNX 已拦住重复请求
 */
@TableName("seckill_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;        // 哪个用户
    private Long goodsId;       // 秒杀了哪个商品
    private Long orderId;       // 对应的订单 ID（关联 order_info 表）

    private LocalDateTime createTime;
}
```

---

## 第三步：DTO

### 3.1 LoginDto.java

> 注意真实文件名是 **LoginDto**（不是 LoginDTO），保持和项目一致。

```java
package com.kisssheep.flashsalebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求体
 *
 * @NotBlank 校验在 Controller 方法参数 @Valid 上触发，校验失败 Spring 自动返回 400
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    @NotBlank(message = "手机号不能为空")   // jakarta.validation 校验，不能是 null 或 ""
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

### 3.2 GoodsDTO.java

> 真实实现用**静态工厂方法 `GoodsDTO.from(Goods)`** 做实体 → DTO 转换，并在转换时顺带算好秒杀状态（没有用 @Builder）。

```java
package com.kisssheep.flashsalebackend.dto;

import com.kisssheep.flashsalebackend.entity.Goods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表 DTO —— 额外包含前端需要的秒杀状态
 * seckillStatus: 0=未开始, 1=进行中, 2=已结束
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsDTO {

    private Long id;
    private String name;
    private String title;
    private String image;
    private BigDecimal price;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 0=未开始, 1=进行中, 2=已结束 */
    private Integer seckillStatus;

    /**
     * 从 Goods 实体构造 DTO，自动计算秒杀状态
     */
    public static GoodsDTO from(Goods goods) {
        GoodsDTO dto = new GoodsDTO();
        dto.setId(goods.getId());
        dto.setName(goods.getName());
        dto.setTitle(goods.getTitle());
        dto.setImage(goods.getImage());
        dto.setPrice(goods.getPrice());
        dto.setSeckillPrice(goods.getSeckillPrice());
        dto.setStockCount(goods.getStockCount());
        dto.setStartTime(goods.getStartTime());
        dto.setEndTime(goods.getEndTime());

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) {
            dto.setSeckillStatus(0); // 未开始
        } else if (now.isAfter(goods.getEndTime())) {
            dto.setSeckillStatus(2); // 已结束
        } else {
            dto.setSeckillStatus(1); // 进行中
        }
        return dto;
    }
}
```

> **注意**：`from()` 里判断「进行中」只看了时间、没看 `stockCount`。所以库存为 0 的商品仍会显示「进行中」，前端抢购时会收到「库存不足」。想更准确可自行加上 `|| goods.getStockCount() <= 0`。

### 3.3 SeckillResultDTO.java

```java
package com.kisssheep.flashsalebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀结果响应
 *
 * orderId = -1   → 还在排队，消费者还没处理完，前端继续轮询
 * orderId > 0    → 秒杀成功，返回的是 order_info 表的主键
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillResultDTO {
    private Long orderId;
}
```

---

## 第四步：Mapper 接口

> 继承 `BaseMapper<Entity>` 后自带 insert / deleteById / updateById / selectById / selectList 等方法。
> 每个 Mapper 都标 `@Mapper`，启动类 `@MapperScan` 也会扫包，双保险。

### 4.1 UserMapper.java

```java
package com.kisssheep.flashsalebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kisssheep.flashsalebackend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

### 4.2 GoodsMapper.java

```java
package com.kisssheep.flashsalebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kisssheep.flashsalebackend.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 扣减库存（DB 层面原子操作）
     * 只有当 stock_count > 0 时才扣减成功
     *
     * @return 影响行数（0 = 扣减失败，库存不足）
     *
     * MyBatis-Plus 自带的 updateById 做不了 "WHERE stock_count > 0" 这种条件更新，必须手写 SQL
     */
    @Update("UPDATE goods SET stock_count = stock_count - 1 WHERE id = #{goodsId} AND stock_count > 0")
    int decrStock(@Param("goodsId") Long goodsId);
}
```

### 4.3 OrderInfoMapper.java

```java
package com.kisssheep.flashsalebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kisssheep.flashsalebackend.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}
```

### 4.4 SeckillOrderMapper.java

```java
package com.kisssheep.flashsalebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kisssheep.flashsalebackend.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
}
```

---

## 第五步：Common 公共模块

### 5.1 CodeMsg.java（错误码枚举）

```java
package com.kisssheep.flashsalebackend.common;

import lombok.Getter;

/**
 * 统一错误码枚举，确保开发的规范性
 *
 * 设计思路：
 *   code = 0       → 成功
 *   code = 500     → 服务端异常
 *   code = 500xxx  → 业务异常（按模块分段：5001xx=通用, 5002xx=秒杀, 5003xx=用户…）
 *
 * 使用：throw new GlobalException(CodeMsg.STOCK_EMPTY);
 */
@Getter
public enum CodeMsg {

    // ========== 通用 ==========
    SUCCESS(0, "success"),
    SERVER_ERROR(500, "服务器异常"),
    PARAM_ERROR(500101, "参数校验失败"),
    UNAUTHORIZED(500102, "未登录或Token无效"),

    // ========== 秒杀业务 ==========
    STOCK_EMPTY(500201, "库存不足"),
    SECKILL_REPEATED(500202, "请勿重复购买"),
    SECKILL_SOLD_OUT(500203, "已售罄"),
    SECKILL_NOT_STARTED(500204, "秒杀未开始"),
    SECKILL_ENDED(500205, "秒杀已结束"),
    RATE_LIMIT(500206, "操作过于频繁，请稍后再试"),

    // ========== 用户 ==========
    USER_NOT_FOUND(500301, "用户不存在"),
    PASSWORD_ERROR(500302, "密码错误");

    private int code;
    private String msg;

    CodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
```

### 5.2 Result.java（统一响应体）

```java
package com.kisssheep.flashsalebackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一响应体，所有接口返回都套这层壳
 *
 * 格式：{ "code": 0, "msg": "success", "data": {...} }
 *
 * 用法：
 *   成功 → Result.success(data)
 *   失败 → Result.error(CodeMsg.STOCK_EMPTY)
 */
@Data
@AllArgsConstructor
public class Result<T> {

    private int code;       // 0=成功，其他=失败
    private String msg;     // 提示信息
    private T data;         // 业务数据，泛型

    /** 成功响应，data 是业务数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(CodeMsg.SUCCESS.getCode(), CodeMsg.SUCCESS.getMsg(), data);
    }

    /** 失败响应，传入错误码枚举，自动解析出 code + msg */
    public static <T> Result<T> error(CodeMsg codeMsg) {
        return new Result<>(codeMsg.getCode(), codeMsg.getMsg(), null);
    }
}
```

### 5.3 GlobalException.java

```java
package com.kisssheep.flashsalebackend.common;

/**
 * 全局业务异常
 *
 * 比默认的 RuntimeException 多携带一个 CodeMsg（错误码 + 提示）
 * 在 Service 里遇到业务错误直接 throw，由 GlobalExceptionHandler 统一拦截
 */
public class GlobalException extends RuntimeException {

    private final CodeMsg codeMsg;

    public GlobalException(CodeMsg codeMsg) {
        super(codeMsg.getMsg());   // 把提示信息作为异常消息
        this.codeMsg = codeMsg;
    }

    public CodeMsg getCodeMsg() {
        return this.codeMsg;
    }
}
```

### 5.4 GlobalExceptionHandler.java

```java
package com.kisssheep.flashsalebackend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理器
 *
 * @ControllerAdvice = AOP 切面，拦截所有 Controller 抛出的异常
 * @ResponseBody     = 返回 JSON
 * 这样业务代码不用到处 try-catch 手动返回 Result.error()，统一由这里处理并写日志
 */
@ControllerAdvice
@Slf4j
@ResponseBody
public class GlobalExceptionHandler {

    /** 业务异常 — 返回对应的错误码和提示 */
    @ExceptionHandler(GlobalException.class)
    public Result<Void> handleGlobalException(GlobalException e) {
        CodeMsg codeMsg = e.getCodeMsg();
        log.warn("业务异常: code = {} , msg = {}", codeMsg.getCode(), codeMsg.getMsg());
        return Result.error(codeMsg);
    }

    /** 兜底 — 未知异常统一返回"服务器异常"，不把堆栈暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常:", e);   // 详细堆栈只在服务器日志里看
        return Result.error(CodeMsg.SERVER_ERROR);
    }
}
```

### 5.5 RedisTypeString.java（Redis Key 前缀枚举）

> 这是真实项目里用来**统一管理 Redis Key 命名**的枚举，避免各处硬编码字符串。
> 秒杀相关 Redis Key 全是 `seckill:xxx:` 前缀，从这里取值。

```java
package com.kisssheep.flashsalebackend.common;

import lombok.Getter;

/**
 * Redis Key 前缀枚举
 * 规范化 Redis Key 命名，避免硬编码
 */
@Getter
public enum RedisTypeString {
    STOCK_PREFIX("seckill:stock:"),      // 库存：seckill:stock:{goodsId}
    SOLD_OUT_PREFIX("seckill:soldout:"), // 售罄标记：seckill:soldout:{goodsId}
    USER_ORDER_PREFIX("seckill:order:"), // 防重锁：seckill:order:{goodsId}:{userId}
    RESULT_PREFIX("seckill:result:");    // 秒杀结果：seckill:result:{goodsId}:{userId}

    private final String value;

    RedisTypeString(String value) {
        this.value = value;
    }
}
```

> **⚠️ 注意 Key 顺序**：真实项目里防重锁和结果是 **`{goodsId}:{userId}`（商品在前，用户在后）**。
> 和 GUIDE 早期版本写的 `{userId}:{goodsId}` 不一样，阅读 `SeckillServiceImpl` / `SeckillConsumer` 时以这里的顺序为准。

---

## 第六步：Config 配置类

### 6.1 RedisConfig.java

```java
package com.kisssheep.flashsalebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 使用 String 序列化 — redis-cli 里可读
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 使用 JSON 序列化（支持对象存储）
        // Spring Boot 4 用的是 Jackson 3，ObjectMapper 在 tools.jackson.databind 包下
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(new ObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
```

> **秒杀场景的实际分工**：
> - 纯字符串的 Key-Value（库存预热、限流计数器）用 `StringRedisTemplate`
> - 需要存对象 / 通用操作的用上面这个 `RedisTemplate<String, Object>`（JSON 序列化）

### 6.2 RabbitMQConfig.java

```java
package com.kisssheep.flashsalebackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 - 声明交换机 + 队列 + 绑定关系
 *
 * 模式：Direct Exchange（直连交换机）
 *   生产者 → Exchange → [通过 RoutingKey 精确匹配] → Queue → 消费者
 */
@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUEUE = "seckill.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill.routingkey";

    /** 直连交换机 */
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE);
    }

    /** 持久化队列，服务器重启不丢消息 */
    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE).build();
    }

    /**
     * 将交换机与队列绑定，通过 routing key 关联
     * 注意：Binding 方法通过参数注入上面两个 Bean（Spring 会自动装配），更符合 Boot 4 的写法
     */
    @Bean
    public Binding seckillQueueBinding(DirectExchange seckillExchange, Queue seckillQueue) {
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with(SECKILL_ROUTING_KEY);
    }
}
```

### 6.3 WebConfig.java（注册拦截器）

```java
package com.kisssheep.flashsalebackend.config;

import com.kisssheep.flashsalebackend.interceptor.LoginInterceptor;
import com.kisssheep.flashsalebackend.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置 - 注册拦截器，并指定各自拦截 / 放行的路径
 */
@Configuration
@RequiredArgsConstructor        // Lombok: 生成带 final 字段的构造函数，Spring 自动注入
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器：需要 Token 的接口
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/seckill/**", "/api/order/**")  // 拦截这些
                .excludePathPatterns("/api/user/login", "/api/goods"); // 排除这些

        // 限流拦截器：只拦截秒杀接口（登录和商品列表不限流）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/seckill/**");
    }
}
```

### 6.4 ThreadPoolConfig.java

```java
package com.kisssheep.flashsalebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * 实际开发中永远不会 new Thread() 或 Executors.newFixedThreadPool()
 *   1. new Thread() 每次创建/销毁开销大，线程数不可控
 *   2. Executors.newFixedThreadPool() 用无界队列，任务堆积会 OOM
 *   3. ThreadPoolTaskExecutor 有界队列 + 拒绝策略，安全可控
 *
 * 使用：方法上标 @Async("seckillExecutor") 即可异步执行
 */
@Configuration
@EnableAsync    // 开启 Spring @Async 注解支持
public class ThreadPoolConfig {

    @Bean("seckillExecutor")    // Bean 名称，@Async 里用它指定用哪个线程池
    public ThreadPoolTaskExecutor seckillExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);            // 常驻线程
        executor.setMaxPoolSize(10);            // 峰值上限
        executor.setQueueCapacity(100);         // 缓冲队列：满了才会从 core 扩到 max
        executor.setKeepAliveSeconds(60);       // 扩容出来的空闲线程多久回收
        executor.setThreadNamePrefix("seckillExecutor-"); // 线程名前缀，排查问题时一眼认出
        // 拒绝策略：队列+线程全满 → 由调用线程自己执行（天然限流，不丢任务也不崩）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}
```

---

## 第七步：拦截器

### 7.1 LoginInterceptor.java

```java
package com.kisssheep.flashsalebackend.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.kisssheep.flashsalebackend.common.CodeMsg;
import com.kisssheep.flashsalebackend.common.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器 - 校验 Token 并维护当前用户 ID 到 ThreadLocal
 *
 * StpUtil.getLoginIdByToken(token) 做了三件事：
 *   1. 从 Request Header 读取 Token（yml 配的 token-name: Authorization）
 *   2. 去 Redis 查这个 Token 对应的 Session
 *   3. 返回 Session 里绑定的 userId，Token 不存在或过期返回 null
 *
 * ThreadLocal：线程级别的"全局变量"，每个请求一个线程，Controller 里直接取，
 * 不用层层传参；请求结束必须 remove()，否则线程回池后复用会拿到旧数据（内存泄漏/安全漏洞）
 */
@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<Long> USER_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Header: Authorization: Bearer <token>
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new GlobalException(CodeMsg.UNAUTHORIZED);
        }

        // 截掉 "Bearer " 前缀（substring(7)）拿到纯 Token
        String token = authHeader.substring(7);
        log.debug("token: {}", token);

        // Sa-Token 返回的 loginId 是 Object/String 类型，需要安全转成 Long
        Object loginIdObj = StpUtil.getLoginIdByToken(token);
        if (loginIdObj == null) {
            throw new GlobalException(CodeMsg.UNAUTHORIZED);
        }

        Long userId = Long.valueOf(loginIdObj.toString());
        USER_THREAD_LOCAL.set(userId);
        return true;
    }

    /**
     * 请求结束后清理 ThreadLocal，防止内存泄漏
     * 不管业务是否抛异常，afterCompletion 都会执行
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        USER_THREAD_LOCAL.remove();
    }

    /** 任何时候需要获取当前登录用户，调这个静态方法 */
    public static Long getCurrentUserId() {
        return USER_THREAD_LOCAL.get();
    }
}
```

> **另外两种拿 userId 的方式（不需要 ThreadLocal）**：
>
> ```java
> // 方式B：Controller 里直接用 Sa-Token API
> Long userId = StpUtil.getLoginIdAsLong();
>
> // 方式C：Sa-Token 自带拦截器，完全不用自己写
> registry.addInterceptor(new SaInterceptor(handle -> {
>     StpUtil.checkLogin();  // 未登录自动抛异常
> })).addPathPatterns("/api/seckill/**");
> ```
>
> 真实项目选方式A（ThreadLocal），比方式B 少一次 Redis 查询，推荐。

### 7.2 RateLimitInterceptor.java

```java
package com.kisssheep.flashsalebackend.interceptor;

import com.kisssheep.flashsalebackend.common.CodeMsg;
import com.kisssheep.flashsalebackend.common.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器 - 滑动窗口计数器（简易版）
 *
 * 算法：Redis INCR + EXPIRE
 *   每个用户一个计数器 Key = "ratelimit:seckill:{userId}"
 *   首次访问 INCR → 1，设置 5 秒过期
 *   5 秒内每访问一次 INCR +1，超过 5 次 → 拒绝
 *   窗口到期后 Key 自己过期消失，下次访问重新计数
 */
@Component
@RequiredArgsConstructor        // Lombok: 生成含所有 final 字段的构造器
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int MAX_COUNT = 5;                    // 窗口内最大请求数
    private static final int WINDOW_SIZE = 5;                  // 窗口大小（秒）
    private static final String REDIS_KEY_PREFIX = "ratelimit:seckill:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long currentUserId = LoginInterceptor.getCurrentUserId();
        if (currentUserId == null) {
            log.info("未登录用户请求");
            return true;    // 理论上不会走到这里（LoginInterceptor 在前面）
        }

        String redisKey = REDIS_KEY_PREFIX + currentUserId;
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);   // INCR 原子自增

        // 首次访问（count==1）时设置过期时间，5 秒后 Key 自动删除，计数器重置
        if (count != null && count == 1) {
            stringRedisTemplate.expire(redisKey, WINDOW_SIZE, TimeUnit.SECONDS);
        }

        // 超过最大次数 → 拒绝
        if (count != null && count > MAX_COUNT) {
            log.warn("用户{}请求次数超过限制", currentUserId);
            throw new GlobalException(CodeMsg.RATE_LIMIT);
        }

        return true;
    }
}
```

> **注意**：INCR 和 EXPIRE 是两个命令，先 INCR 后 EXPIRE 之间如果程序崩溃，Key 会永久存在。
> 生产环境用 Lua 脚本保证原子性。真实项目为演示从简。

---

## 第八步：Service 接口

> 先定义接口，再在 `service/Impl/` 下写实现。Controller 只依赖接口。

### 8.1 UserService.java

```java
package com.kisssheep.flashsalebackend.service;

import com.kisssheep.flashsalebackend.dto.LoginDto;

public interface UserService {
    String login(LoginDto loginDto);
}
```

### 8.2 GoodsService.java

```java
package com.kisssheep.flashsalebackend.service;

import com.kisssheep.flashsalebackend.dto.GoodsDTO;

import java.util.List;

public interface GoodsService {

    /** 获取所有商品列表（含秒杀状态） */
    List<GoodsDTO> listGoods();

    /** 获取单个商品详情 */
    GoodsDTO getGoodsById(Long goodsId);
}
```

### 8.3 SeckillService.java

```java
package com.kisssheep.flashsalebackend.service;

import com.kisssheep.flashsalebackend.dto.SeckillResultDTO;

public interface SeckillService {

    /** 初始化：将数据库中的库存加载到 Redis */
    void initStock();

    /** 执行秒杀：校验 + 扣减 + 防重，耗时写入交给 MQ */
    String seckill(Long userId, Long goodsId);

    /** 前端定时轮询，获取真正的下单结果 */
    SeckillResultDTO queryResult(Long userId, Long goodsId);
}
```

---

## 第九步：Service 实现（核心）

### 9.1 UserServiceImpl.java

```java
package com.kisssheep.flashsalebackend.service.Impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kisssheep.flashsalebackend.common.CodeMsg;
import com.kisssheep.flashsalebackend.common.GlobalException;
import com.kisssheep.flashsalebackend.dto.LoginDto;
import com.kisssheep.flashsalebackend.entity.User;
import com.kisssheep.flashsalebackend.mapper.UserMapper;
import com.kisssheep.flashsalebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 用户服务实现 - 登录
 *
 * 密码验证：MD5(salt + 原始密码)
 * Token 方案：Sa-Token 自动生成 Token + 存 Redis（yml 配的 timeout: 1800 = 30 分钟）
 *
 * StpUtil.login(userId) 自动完成：生成随机 Token → Redis 建 Session → 绑定 userId
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public String login(LoginDto loginDto) {
        // 1. 按手机号查用户（LambdaQueryWrapper 拼条件，字段名和方法引用绑定，编译期安全）
        LambdaQueryWrapper<User> eq = new LambdaQueryWrapper<User>().eq(User::getPhone, loginDto.getPhone());
        User user = userMapper.selectOne(eq);
        if (user == null) {
            throw new GlobalException(CodeMsg.USER_NOT_FOUND);
        }

        // 2. 验密：MD5(数据库salt + 用户输入的密码)，和数据库 password 比对
        String s = user.getSalt() + loginDto.getPassword();
        String inputHash = DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
        if (!inputHash.equals(user.getPassword())) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }

        // 3. Sa-Token 登录：自动生成 Token + 存 Redis，不用自己写 UUID 和 SET
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        log.info("用户登录成功,id:{},phone:{}", user.getId(), user.getPhone());
        return token;
    }
}
```

### 9.2 GoodsServiceImpl.java

```java
package com.kisssheep.flashsalebackend.service.Impl;

import com.kisssheep.flashsalebackend.common.CodeMsg;
import com.kisssheep.flashsalebackend.common.GlobalException;
import com.kisssheep.flashsalebackend.dto.GoodsDTO;
import com.kisssheep.flashsalebackend.entity.Goods;
import com.kisssheep.flashsalebackend.mapper.GoodsMapper;
import com.kisssheep.flashsalebackend.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    private final GoodsMapper goodsMapper;

    @Override
    public List<GoodsDTO> listGoods() {
        // selectList(null) = 查全部，相当于 SELECT * FROM goods
        // 每个实体用 GoodsDTO.from() 转换，顺带算好秒杀状态
        List<Goods> goodsList = goodsMapper.selectList(null);
        return goodsList.stream()
                .map(GoodsDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public GoodsDTO getGoodsById(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new GlobalException(CodeMsg.PARAM_ERROR);
        }
        return GoodsDTO.from(goods);
    }
}
```

### 9.3 SeckillServiceImpl.java（核心！）

```java
package com.kisssheep.flashsalebackend.service.Impl;

import com.kisssheep.flashsalebackend.common.CodeMsg;
import com.kisssheep.flashsalebackend.common.GlobalException;
import com.kisssheep.flashsalebackend.common.RedisTypeString;
import com.kisssheep.flashsalebackend.dto.SeckillResultDTO;
import com.kisssheep.flashsalebackend.entity.Goods;
import com.kisssheep.flashsalebackend.mapper.GoodsMapper;
import com.kisssheep.flashsalebackend.mq.SeckillMessage;
import com.kisssheep.flashsalebackend.mq.SeckillProducer;
import com.kisssheep.flashsalebackend.service.SeckillService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀核心服务
 *
 * 主链路只做"判断类"的轻操作（库够不够、重复没重复），
 * 真正耗时的 DB 写入交给生产者 → MQ → 消费者 异步完成，接口秒回。
 *
 * 整体流程：
 *   ┌────────────────────────────────────────────────┐
 *   │ ① 校验商品存在 + 秒杀时间                         │
 *   │ ② 售罄标记检查  → 已售罄则秒拒                    │
 *   │ ③ Redis DECR 原子扣库存 → stock<0 回滚+打售罄标记  │
 *   │ ④ SETNX 防重  → 重复则回滚库存                    │
 *   │ ⑤ 发 MQ 消息  → 投递失败回滚全部                  │
 *   │ ⑥ 返回"购买成功!正在排队..."                     │
 *   └────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@AllArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final GoodsMapper goodsMapper;
    private final SeckillProducer seckillProducer;
    private final StringRedisTemplate stringRedisTemplate;   // 纯字符串操作（库存预热）
    private final RedisTemplate<String, Object> redisTemplate; // 通用操作（JSON 序列化）

    /**
     * 启动预热 - 把 DB 库存加载到 Redis
     * @PostConstruct 在 Bean 初始化完成后自动执行
     */
    @Override
    @PostConstruct
    public void initStock() {
        List<Goods> goodsList = goodsMapper.selectList(null);
        for (Goods goods : goodsList) {
            log.info("库存预热: goodsId={}, stock={}", goods.getId(), goods.getStockCount());
            stringRedisTemplate.opsForValue().set(
                    RedisTypeString.STOCK_PREFIX.getValue() + goods.getId(),
                    String.valueOf(goods.getStockCount()));
        }
    }

    /**
     * 执行秒杀
     * 校验 → 扣库存 → 防重 → 发 MQ，异步写库
     */
    @Override
    public String seckill(Long userId, Long goodsId) {
        // ====== 1. 校验商品是否存在 ======
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new GlobalException(CodeMsg.PARAM_ERROR);
        }

        // ====== 2. 校验秒杀时间 ======
        LocalDateTime now = LocalDateTime.now();
        if (goods.getStartTime().isAfter(now)) {   // 还没到开始时间
            throw new GlobalException(CodeMsg.SECKILL_NOT_STARTED);
        }
        if (goods.getEndTime().isBefore(now)) {    // 已经过了结束时间
            throw new GlobalException(CodeMsg.SECKILL_ENDED);
        }

        // ====== 3. 售罄标记检查 ======
        String soldOutKey = RedisTypeString.SOLD_OUT_PREFIX.getValue() + goodsId;
        String soldOutFlag = (String) redisTemplate.opsForValue().get(soldOutKey);
        if ("1".equals(soldOutFlag)) {
            throw new GlobalException(CodeMsg.SECKILL_SOLD_OUT);
        }

        // ====== 4. Redis 原子扣库存 ======
        // DECR 是 Redis 单线程执行的原子操作，10000 个并发请求排队执行，不会多扣
        String stockKey = RedisTypeString.STOCK_PREFIX.getValue() + goodsId;
        Long stockCount = redisTemplate.opsForValue().decrement(stockKey);
        if (stockCount != null && stockCount < 0) {
            // 库存不足 → 回滚 INCR + 打售罄标记
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.opsForValue().set(soldOutKey, "1");
            throw new GlobalException(CodeMsg.STOCK_EMPTY);
        }

        // ====== 5. 防重检查：同一用户同一商品只能秒杀一次 ======
        // SETNX = SET if Not eXists，原子操作；Key = seckill:order:{goodsId}:{userId}
        String lockKey = RedisTypeString.USER_ORDER_PREFIX.getValue() + goodsId + ":" + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1");
        if (Boolean.FALSE.equals(locked)) {
            // 重复购买 → 回滚刚才扣掉的库存
            redisTemplate.opsForValue().increment(stockKey);
            throw new GlobalException(CodeMsg.SECKILL_REPEATED);
        }

        // ====== 6. 发送 MQ 消息，异步写库 ======
        try {
            SeckillMessage seckillMessage = new SeckillMessage(userId, goodsId);
            seckillProducer.send(seckillMessage);
        } catch (Exception e) {
            // 投递失败 → 全部回滚（库存 + 防重标记）
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.delete(lockKey);
            log.error("seckill message send failed", e);
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }

        return "购买成功!正在排队...";
    }

    /**
     * 轮询秒杀结果
     * 消费者写入的订单结果 Key = seckill:result:{goodsId}:{userId}
     */
    @Override
    public SeckillResultDTO queryResult(Long userId, Long goodsId) {
        String resultKey = RedisTypeString.RESULT_PREFIX.getValue() + goodsId + ":" + userId;
        Object result = redisTemplate.opsForValue().get(resultKey);
        if (result != null && StringUtils.hasText(result.toString())) {
            return new SeckillResultDTO(Long.valueOf(result.toString()));
        }
        return new SeckillResultDTO(-1L);   // -1 = 还在排队
    }
}
```

> **为什么主链路不开多线程？**
> Redis 单线程本身就够快（万级 QPS）。多线程反而让库存回滚逻辑变复杂 —— 两个线程同时失败回滚时，谁先 INCR 谁后 INCR？最终库存可能不对。
> 所以秒杀主链路保持同步，把耗时的 DB 写入交给 MQ 异步。

---

## 第十步：MQ 消息体 + 生产者

### 10.1 SeckillMessage.java

```java
package com.kisssheep.flashsalebackend.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀消息体
 *
 * 必须 implements Serializable — RabbitMQ 默认用 JDK 序列化把消息转成字节流
 * serialVersionUID 固定写死，避免类结构变化导致反序列化失败
 *
 * 消息体设计原则：只传最少必要字段
 *   userId + goodsId 就够了，消费者拿着 goodsId 自己去 DB 查最新价格和商品名
 *   不要把价格写死在消息里 — 如果秒杀过程中运营改了价，消费者还按旧价下单就出 bug 了
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long goodsId;
}
```

### 10.2 SeckillProducer.java

```java
package com.kisssheep.flashsalebackend.mq;

import com.kisssheep.flashsalebackend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀消息生产者
 * RabbitTemplate 是 Spring AMQP 封装的发送工具，类似 RedisTemplate
 * convertAndSend = 序列化消息对象 + 发送，一行搞定
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class SeckillProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀消息到 MQ
     * 在 SeckillService 秒杀成功后调用；如果发送失败，调用方需要回滚 Redis 库存
     */
    public void send(SeckillMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,      // 交换机
                RabbitMQConfig.SECKILL_ROUTING_KEY,   // 路由键
                message);                              // 消息体（自动序列化）
        log.info("Send seckill message: {}", message);
    }
}
```

---

## 第十一步：MQ 消费者

```java
package com.kisssheep.flashsalebackend.mq;

import com.kisssheep.flashsalebackend.common.RedisTypeString;
import com.kisssheep.flashsalebackend.config.RabbitMQConfig;
import com.kisssheep.flashsalebackend.entity.Goods;
import com.kisssheep.flashsalebackend.entity.OrderInfo;
import com.kisssheep.flashsalebackend.entity.SeckillOrder;
import com.kisssheep.flashsalebackend.mapper.GoodsMapper;
import com.kisssheep.flashsalebackend.mapper.OrderInfoMapper;
import com.kisssheep.flashsalebackend.mapper.SeckillOrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀消息消费者 - 接收消息后执行数据库写入等耗时操作
 * 流程：查商品 → DB扣库存 → 创建订单 → 创建秒杀订单 → 写Redis结果 → 手动ACK
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillConsumer {

    private final GoodsMapper goodsMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 监听秒杀队列，执行异步下单
     * ackMode = "MANUAL"：消费成功手动确认，失败重新入队
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE, ackMode = "MANUAL")
    public void handleSeckillMessage(SeckillMessage message, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();
        log.info("消费者收到秒杀消息: userId={}, goodsId={}", userId, goodsId);

        try {
            // ====== 1. 查询商品信息 ======
            // 不能用消息里的价格 — 消息发出到消费可能有延迟，这期间价格可能被改了
            Goods goods = goodsMapper.selectById(goodsId);
            if (goods == null) {
                log.error("商品不存在: goodsId={}", goodsId);
                channel.basicAck(deliveryTag, false);   // 非法消息，ACK 丢弃（不回队）
                return;
            }

            // ====== 2. DB 原子扣减库存（最后防线） ======
            // 正常情况 Redis 已扣过，这里基本不会失败；极端情况（Redis/DB 间回滚失败）这里兜底
            int rows = goodsMapper.decrStock(goodsId);
            if (rows <= 0) {
                log.warn("库存扣减失败（已售罄）: goodsId={}", goodsId);
                channel.basicAck(deliveryTag, false);   // 库存确实没了，ACK 丢弃
                return;
            }

            // ====== 3. 创建 order_info 记录 ======
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setUserId(userId);
            orderInfo.setGoodsId(goodsId);
            orderInfo.setGoodsName(goods.getName());          // 冗余存商品名
            orderInfo.setGoodsCount(1);                       // 秒杀限购 1 件
            orderInfo.setGoodsPrice(goods.getSeckillPrice()); // 以 DB 当前秒杀价为准
            orderInfo.setStatus(0);                           // 0 = 新建
            orderInfo.setCreateTime(LocalDateTime.now());
            orderInfoMapper.insert(orderInfo);

            // ====== 4. 创建 seckill_order 防重记录 ======
            // 唯一索引 (user_id, goods_id) 兜底防重；重复插入会抛 DuplicateKeyException
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setGoodsId(goodsId);
            seckillOrder.setOrderId(orderInfo.getId());
            seckillOrder.setCreateTime(LocalDateTime.now());
            seckillOrderMapper.insert(seckillOrder);

            // ====== 5. 写 Redis 结果，前端轮询立刻能看到 ======
            // Key = seckill:result:{goodsId}:{userId}，带 30 分钟 TTL
            String resultKey = RedisTypeString.RESULT_PREFIX.getValue() + goodsId + ":" + userId;
            redisTemplate.opsForValue().set(resultKey, String.valueOf(orderInfo.getId()), 30, TimeUnit.MINUTES);

            // 同时更新防重锁的值 = 订单 ID（也是 30 分钟 TTL）
            String lockKey = RedisTypeString.USER_ORDER_PREFIX.getValue() + goodsId + ":" + userId;
            redisTemplate.opsForValue().set(lockKey, String.valueOf(orderInfo.getId()), 30, TimeUnit.MINUTES);

            log.info("秒杀订单创建成功: orderId={}, userId={}, goodsId={}", orderInfo.getId(), userId, goodsId);

            // ====== 6. 手动 ACK ======
            channel.basicAck(deliveryTag, false);   // 第二个参数 = false：只确认当前这条

        } catch (Exception e) {
            log.error("消费秒杀消息异常: userId={}, goodsId={}", userId, goodsId, e);
            try {
                // basicNack(deliveryTag, false, true)：拒绝并重新入队，让其他消费者重试
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("消息Nack失败", ex);
            }
        }
    }
}
```

> **ACK 模式（yml 里 listener.simple.acknowledge-mode: manual）**：
> 业务处理成功才 `basicAck`，MQ 不会丢消息；失败 `basicNack(..., requeue=true)` 重新入队重试。
> 好处是可靠，坏处是重试可能无限循环（见文末「已知风险」）。

---

## 第十二步：Controller

### 12.1 AuthController.java

```java
package com.kisssheep.flashsalebackend.controller;

import com.kisssheep.flashsalebackend.common.Result;
import com.kisssheep.flashsalebackend.dto.LoginDto;
import com.kisssheep.flashsalebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 登录
     * POST /api/user/login
     * Body: { "phone": "13800000001", "password": "123456" }
     * 返回: { "code": 0, "msg": "success", "data": "token字符串" }
     *
     * @Valid 触发 LoginDto 里的 @NotBlank 校验，校验失败 Spring 自动返回 400
     */
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDto loginDto) {
        String token = userService.login(loginDto);
        return Result.success(token);
    }
}
```

### 12.2 GoodsController.java

```java
package com.kisssheep.flashsalebackend.controller;

import com.kisssheep.flashsalebackend.common.Result;
import com.kisssheep.flashsalebackend.dto.GoodsDTO;
import com.kisssheep.flashsalebackend.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    /** GET /api/goods — 所有商品 + 秒杀状态 */
    @GetMapping
    public Result<List<GoodsDTO>> listGoods() {
        return Result.success(goodsService.listGoods());
    }

    /** GET /api/goods/{goodsId} — 单个商品详情 */
    @GetMapping("/{goodsId}")
    public Result<GoodsDTO> getGoods(@PathVariable Long goodsId) {
        return Result.success(goodsService.getGoodsById(goodsId));
    }
}
```

### 12.3 SeckillController.java

```java
package com.kisssheep.flashsalebackend.controller;

import com.kisssheep.flashsalebackend.common.Result;
import com.kisssheep.flashsalebackend.dto.SeckillResultDTO;
import com.kisssheep.flashsalebackend.interceptor.LoginInterceptor;
import com.kisssheep.flashsalebackend.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 执行秒杀
     * POST /api/seckill/{goodsId}
     * Header: Authorization: Bearer <token>
     * 返回 "购买成功!正在排队..." — 不表示秒杀成功，只表示请求已受理
     */
    @PostMapping("/seckill/{goodsId}")
    public Result<String> seckill(@PathVariable Long goodsId) {
        Long userId = LoginInterceptor.getCurrentUserId();   // ThreadLocal 拿当前用户
        String msg = seckillService.seckill(userId, goodsId);
        return Result.success(msg);
    }

    /**
     * 轮询秒杀结果
     * GET /api/order/result/{goodsId}
     * Header: Authorization: Bearer <token>
     * 前端每 500ms 轮询一次；orderId=-1 还在排队，>0 秒杀成功
     */
    @GetMapping("/order/result/{goodsId}")
    public Result<SeckillResultDTO> queryResult(@PathVariable Long goodsId) {
        Long userId = LoginInterceptor.getCurrentUserId();
        SeckillResultDTO result = seckillService.queryResult(userId, goodsId);
        return Result.success(result);
    }
}
```

---

## 第十三步：前端 Vue 项目（待创建）

> 按 OUTLINE.md，前端是独立仓库 `FlashSaleFrontend`，目前还没建。

```bash
cd D:\CodeSpace\FlashSale
npm create vite@latest FlashSaleFrontend -- --template vue
cd FlashSaleFrontend
npm install vue-router axios
```

### Axios 封装（`src/api/request.js`）

```js
import axios from 'axios'

// 创建 Axios 实例，后端地址
const instance = axios.create({ baseURL: 'http://localhost:8080' })

// 请求拦截器：每次请求自动带 Token
instance.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：统一解包 Result<T>，只返回 data 字段
instance.interceptors.response.use(res => {
  const body = res.data
  if (body.code !== 0) {
    alert(body.msg)
    return Promise.reject(new Error(body.msg))
  }
  return body.data  // 直接返回业务数据，调用方不用写 .data.data
})

export default instance
```

### 页面路由

| 路由                | 组件              | 核心逻辑                                                  |
| ----------------- | --------------- | ----------------------------------------------------- |
| `/login`          | LoginView.vue   | 表单 → 调 login API → Token 存 localStorage → 跳 /goods    |
| `/goods`          | GoodsList.vue   | 调 /api/goods → 渲染卡片 → 秒杀按钮 + CountDown 倒计时            |
| `/order/:goodsId` | OrderResult.vue | 调 /api/seckill/:id → 每 500ms 轮询 /api/order/result/:id |

---

## 第十四步：JMeter 压测

> 真实压测脚本已在 `jmeter/` 目录下，直接用即可：

```
jmeter/
├── flashsale-load-test.jmx    # JMeter 测试计划（5000 线程 × 10 循环）
├── users.csv                  # 压测用户数据
├── prepare.sql                # 造数 SQL
├── run-test.bat / run-test.ps1 / simulate.ps1   # 一键运行脚本
├── 压测操作指南.md              # 详细操作步骤
└── results/                   # 压测结果输出
```

### 压测要点

1. 线程组：5000 线程，循环 10 次
2. HTTP 请求：`POST /api/seckill/1`，Header 带不同 userId Token
3. 监听器：汇总报告 + 聚合图表

### 验证标准

- 订单数 ≤ 初始库存（**不超卖**）
- QPS 1500+
- 错误率可控

---

## API 清单

| 方法   | 路径                            | 认证    | 说明                                 |
| ---- | ----------------------------- | ----- | ---------------------------------- |
| POST | `/api/user/login`             | 无     | Body: `{phone, password}`，返回 Token |
| GET  | `/api/goods`                  | 无     | 商品列表，每个商品带 seckillStatus           |
| GET  | `/api/goods/{goodsId}`        | 无     | 单个商品详情                             |
| POST | `/api/seckill/{goodsId}`      | Token | 执行秒杀，返回"购买成功!正在排队..."             |
| GET  | `/api/order/result/{goodsId}` | Token | 轮询，orderId=-1=排队中，>0=成功            |

---

## 关键提示

1. **Redis Key 统一走 `RedisTypeString` 枚举**，别硬编码字符串；Key 顺序是 `{goodsId}:{userId}`（商品在前）。
2. **秒杀主链路用 `RedisTemplate`，库存预热/限流用 `StringRedisTemplate`**，分工明确。
3. **库存回滚不能漏**：DECR 之后的任何失败都必须 `INCR` 补回去，否则库存越扣越少。
4. **防重靠 SETNX**：`setIfAbsent` 返回 false = 重复购买，回滚库存再抛异常。
5. **DB 原子扣库存是最后防线**：`WHERE stock_count > 0` 条件更新，返回 0 说明卖完了。
6. **消息体只传 userId + goodsId**：价格去 DB 取最新值，不要写死在消息里。
7. **价格用 BigDecimal**：永远不要用 double 存钱。
8. **秒杀主链路不要加多线程**：保持同步，耗时写库交给 MQ 异步。
9. **`@TableField` 能不写就不写**：yml 已开驼峰自动映射（`create_time` → `createTime`）。
10. **ThreadLocal 用完必须 remove**：`afterCompletion` 里清理，防线程池复用读到旧 userId。

---

## 已知风险（真实代码里的坑，建议后续修）

| # | 问题 | 位置 | 建议 |
| --- | --- | --- | --- |
| 1 | **消费者没有 `@Transactional`**：扣库存、建订单、写秒杀订单三步不是原子操作，中途异常会出现「扣了库存但没订单」 | `SeckillConsumer` | 方法上标 `@Transactional`，三步一起成功或一起回滚 |
| 2 | **重复消费会死循环重试**：seckill_order 唯一索引冲突抛 `DuplicateKeyException` → 被 catch 接住 → `basicNack(requeue=true)` → 重新入队 → 再次冲突 → 无限重试 | `SeckillConsumer` | 单独 `catch (DuplicateKeyException)`，直接 `basicAck` 丢弃；不要把这类"业务已确定"的异常 requeue |
| 3 | **防重锁 30 分钟过期后可能重复购买**：消费者把防重 lockKey 覆盖成订单 ID 并设 30 分钟 TTL，过期后同一用户可再次 SETNX 成功 | `SeckillConsumer` | 防重标记不要设 TTL，或用 DB 唯一索引兜底（已有，但要避免问题 2 的死循环） |
| 4 | **`initStock` 无条件覆盖 Redis 库存**：每次启动都从 DB 重置库存，运行中改的库存会被冲掉，且不会清理旧的售罄标记 | `SeckillServiceImpl` | 启动只初始化不存在的 Key，并清掉 `soldout` 标记 |
| 5 | **Redis 结果 key 带 30 分钟 TTL**：用户 30 分钟后再轮询会拿到 -1（其实订单已建好） | `SeckillConsumer` | 结果 key 不用设 TTL，或设更长 |

> 这些是故意留下的真实问题，正好是学习「秒杀系统幂等、事务、可靠性」的好教材 —— 修完它们，你就把这套系统吃透了。
