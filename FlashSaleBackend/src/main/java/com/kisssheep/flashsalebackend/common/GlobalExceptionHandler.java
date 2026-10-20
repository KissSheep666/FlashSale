package com.kisssheep.flashsalebackend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理器
 * 使用AOP切面 拦截所有Controller 抛出的异常
 * 返回统一的JSON格式
 * 异常直接抛出 系统无法感知和收集错误 使用全局异常处理器
 * 统一的对异常进行处理 执行写入日志等操作
 */
//底层是一个AOP切面编程
@ControllerAdvice
@Slf4j
@ResponseBody
public class GlobalExceptionHandler {
    /**
     * 业务异常
     */
    //标注捕获异常的类型
    @ExceptionHandler(GlobalException.class)
    public Result<Void> handleGlobalException(GlobalException e){
        CodeMsg codeMsg = e.getCodeMsg();
        log.warn("业务异常: code = {} , msg = {}", codeMsg.getCode(), codeMsg.getMsg());
        //不用在类中频繁返回Result 使用 抛出异常代为管理
        return Result.error(codeMsg);
    }

    /**
     * 兜底策略 - 不把具体的业务错误暴露给用户,捕获剩余的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e){
        log.error("系统异常:",e);
        return Result.error(CodeMsg.SERVER_ERROR);
    }
}
