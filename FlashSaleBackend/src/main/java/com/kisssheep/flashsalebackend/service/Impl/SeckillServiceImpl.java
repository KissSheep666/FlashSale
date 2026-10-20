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
 * 在这个Service方法中 不进行具体的sql库存写入等耗时操作 只进行判断 库够不够?之类的处理
 * 具体的耗时操作交给生产者 经由生产者发送给消费者
 */
@Slf4j
@Service
@AllArgsConstructor
public class SeckillServiceImpl implements SeckillService {
    private final GoodsMapper goodsMapper;
    private final SeckillProducer seckillProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 启动预热 将 DB库里的数据加载到Redis
     * @PostConstruct 在 Bean 初始化完成后自动执行，确保数据预热
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
     * 执行秒杀业务
     * 传入用户id 货物id -> 进行库存校验 -> 保证幂等性(确保已经秒杀过了) -> 传递给MQ进行具体扣减库存的操作(耗时操作)
     */
    @Override
    public String seckill(Long userId, Long goodsId) {
        // 1. 校验商品是否存在
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new GlobalException(CodeMsg.PARAM_ERROR);
        }

        // 2. 校验秒杀时间
        LocalDateTime now = LocalDateTime.now();
        // startTime 在当前时间之后 → 秒杀尚未开始
        if (goods.getStartTime().isAfter(now)) {
            throw new GlobalException(CodeMsg.SECKILL_NOT_STARTED);
        }
        // endTime 在当前时间之前 → 秒杀已经结束
        if (goods.getEndTime().isBefore(now)) {
            throw new GlobalException(CodeMsg.SECKILL_ENDED);
        }

        // 3. 校验是否已售罄（通过 Redis 售罄标记）
        String soldOutKey = RedisTypeString.SOLD_OUT_PREFIX.getValue() + goodsId;
        String soldOutFlag = (String) redisTemplate.opsForValue().get(soldOutKey);
        if ("1".equals(soldOutFlag)) {
            throw new GlobalException(CodeMsg.SECKILL_SOLD_OUT);
        }

        // 4. Redis 原子扣减库存
        String stockKey = RedisTypeString.STOCK_PREFIX.getValue() + goodsId;
        Long stockCount = redisTemplate.opsForValue().decrement(stockKey);
        if (stockCount != null && stockCount < 0) {
            // 库存不足，回滚 INCR 并打上售罄标记
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.opsForValue().set(soldOutKey, "1");
            throw new GlobalException(CodeMsg.STOCK_EMPTY);
        }

        // 5. 防重检查：同一用户同一商品只能秒杀一次
        String lockKey = RedisTypeString.USER_ORDER_PREFIX.getValue() + goodsId + ":" + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1");
        if (Boolean.FALSE.equals(locked)) {
            // 重复购买，回滚库存
            redisTemplate.opsForValue().increment(stockKey);
            throw new GlobalException(CodeMsg.SECKILL_REPEATED);
        }

        // 6. 发送消息给 MQ 生产者，异步写入数据库
        try {
            SeckillMessage seckillMessage = new SeckillMessage(userId, goodsId);
            seckillProducer.send(seckillMessage);
        } catch (Exception e) {
            // 投递失败：回滚库存 + 删除防重标记
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.delete(lockKey);
            log.error("seckill message send failed", e);
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }

        return "购买成功!正在排队...";
    }

    /**
     * 轮询秒杀结果 前端定时的执行此方法 来确定全流程是否完成(数据库中的操作是否完成)
     */
    @Override
    public SeckillResultDTO queryResult(Long userId, Long goodsId) {
        // 消费者写入的订单结果 Key: seckill:result:{goodsId}:{userId}
        String resultKey = RedisTypeString.RESULT_PREFIX.getValue() + goodsId + ":" + userId;
        Object result = redisTemplate.opsForValue().get(resultKey);
        if (result != null && StringUtils.hasText(result.toString())) {
            return new SeckillResultDTO(Long.valueOf(result.toString()));
        }
        return new SeckillResultDTO(-1L);
    }
}
