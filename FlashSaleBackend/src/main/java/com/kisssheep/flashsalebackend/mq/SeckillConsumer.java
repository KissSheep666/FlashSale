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
 * 秒杀消息的消费者 - 接收秒杀消息后执行数据库写入等耗时操作
 * 流程：查商品 → DB扣库存 → 创建订单 → 创建秒杀订单 → 写入Redis结果 → 手动ACK
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
     * manual Ack 模式：消费成功手动确认，失败重新入队
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE, ackMode = "MANUAL")
    public void handleSeckillMessage(SeckillMessage message, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();
        log.info("消费者收到秒杀消息: userId={}, goodsId={}", userId, goodsId);

        try {
            // 1. 查询商品信息
            Goods goods = goodsMapper.selectById(goodsId);
            if (goods == null) {
                log.error("商品不存在: goodsId={}", goodsId);
                // 非法消息，直接 ACK 丢弃（不回队）
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. DB 原子扣减库存（带乐观锁条件 WHERE stock_count > 0）
            int rows = goodsMapper.decrStock(goodsId);
            if (rows <= 0) {
                log.warn("库存扣减失败（已售罄）: goodsId={}", goodsId);
                // 扣减失败说明库存确实没了，ACK 丢弃
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. 创建 order_info 记录
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setUserId(userId);
            orderInfo.setGoodsId(goodsId);
            orderInfo.setGoodsName(goods.getName());
            orderInfo.setGoodsCount(1);
            orderInfo.setGoodsPrice(goods.getSeckillPrice());
            orderInfo.setStatus(0); // 0=新建
            orderInfo.setCreateTime(LocalDateTime.now());
            orderInfoMapper.insert(orderInfo);

            // 4. 创建 seckill_order 防重记录
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setGoodsId(goodsId);
            seckillOrder.setOrderId(orderInfo.getId());
            seckillOrder.setCreateTime(LocalDateTime.now());
            seckillOrderMapper.insert(seckillOrder);

            // 5. 将订单 ID 写入 Redis，供前端轮询查询
            // 同时用 Redis 标记完成，更新防重锁的值
            String resultKey = RedisTypeString.RESULT_PREFIX.getValue() + goodsId + ":" + userId;
            redisTemplate.opsForValue().set(resultKey, String.valueOf(orderInfo.getId()), 30, TimeUnit.MINUTES);

            // 同时更新防重锁的值为订单 ID
            String lockKey = RedisTypeString.USER_ORDER_PREFIX.getValue() + goodsId + ":" + userId;
            redisTemplate.opsForValue().set(lockKey, String.valueOf(orderInfo.getId()), 30, TimeUnit.MINUTES);

            log.info("秒杀订单创建成功: orderId={}, userId={}, goodsId={}", orderInfo.getId(), userId, goodsId);

            // 6. 手动 ACK 确认
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("消费秒杀消息异常: userId={}, goodsId={}", userId, goodsId, e);
            try {
                // 消费失败，拒绝并重新入队（让其他消费者重试）
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("消息Nack失败", ex);
            }
        }
    }
}
