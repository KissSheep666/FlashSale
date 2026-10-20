package com.kisssheep.flashsalebackend.mq;

import com.kisssheep.flashsalebackend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀消息生产者
 * Rabbit 是类似于RedisTemplate封装的发送工具
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class SeckillProducer {
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀消息到MQ
     * 这个方法在SeckillService 秒杀成功后调用
     * 如果发送失败 调用方法 回滚 撤销对Redis的影响
     */
    public void send(SeckillMessage message){
        //指定交换机 以及相应的 路由键 消息体
        rabbitTemplate.convertAndSend(RabbitMQConfig.SECKILL_EXCHANGE, RabbitMQConfig.SECKILL_ROUTING_KEY, message);
        log.info("Send seckill message: {}", message);
    }
}


