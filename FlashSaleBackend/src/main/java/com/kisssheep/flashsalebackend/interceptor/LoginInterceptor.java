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
 */
@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<Long> USER_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new GlobalException(CodeMsg.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        log.debug("token: {}", token);

        // Sa-Token 返回的 loginId 是 String 类型，需要安全转换
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
     * 线程回池后如果不清理，下次复用会拿到旧数据
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        USER_THREAD_LOCAL.remove();
    }

    /** 获取当前请求的用户 ID */
    public static Long getCurrentUserId() {
        return USER_THREAD_LOCAL.get();
    }
}
