package com.kisssheep.flashsalebackend.common;

import lombok.Getter;

/**
 * Redis Key 前缀枚举
 * 规范化 Redis Key 命名，避免硬编码
 */
@Getter
public enum RedisTypeString {
    STOCK_PREFIX("seckill:stock:"),
    SOLD_OUT_PREFIX("seckill:soldout:"),
    USER_ORDER_PREFIX("seckill:order:"),
    RESULT_PREFIX("seckill:result:");

    private final String value;

    RedisTypeString(String value) {
        this.value = value;
    }
}
