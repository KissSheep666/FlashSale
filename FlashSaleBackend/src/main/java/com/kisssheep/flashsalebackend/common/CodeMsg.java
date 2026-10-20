package com.kisssheep.flashsalebackend.common;

import lombok.Getter;

/**
 * 使用枚举类 统一错误码 确保开发的规范性
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
