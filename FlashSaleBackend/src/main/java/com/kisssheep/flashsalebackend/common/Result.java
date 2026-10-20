package com.kisssheep.flashsalebackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一响应体 统一向前端返回数据
 */
@Data
@AllArgsConstructor
public class Result<T> {
    //错误码
    private int code;

    //错误信息
    private String msg;

    //实际相应数据
    private T data;

    //成功时候
    public static <T> Result<T> success(T data) {
        return new Result<>(CodeMsg.SUCCESS.getCode(), CodeMsg.SUCCESS.getMsg(), data);
    }

    //失败情况 - 上层传递枚举类 codeMsg 下层自动完成解析 返回 code msg
    public static <T> Result<T> error(CodeMsg codeMsg) {
        return new Result<>(codeMsg.getCode(), codeMsg.getMsg(), null);
    }


}
