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
 * 使用滑动窗口策略进行限流处理 每N秒内最多可以处理M次请求
 * INCR - 原子自增 EXPIRE - 设置过期时间
 * 基于在Redis中的计数器进行实现
 * 每次访问InCr +1 设置N秒过期时间
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int MAX_COUNT = 5;//窗口最大请求数
    private static final int WINDOW_SIZE = 5;//窗口时间窗口 s
    private static final String REDIS_KEY_PREFIX = "ratelimit:seckill:";

    /**
     * 拦截请求后 进行相应的处理 - 判断是否已经达到了最大的请求数
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler chosen handler to execute, for type and/or instance evaluation
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long currentUserId = LoginInterceptor.getCurrentUserId();
        if (currentUserId == null) {
            log.info("未登录用户请求");
            return true;
        }

        String redisKey = REDIS_KEY_PREFIX + currentUserId;
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);

        //进行初始化处理
        if (count != null && count == 1){
            //设置过期时间五秒，在五秒内可以访问
            stringRedisTemplate.expire(redisKey, WINDOW_SIZE, TimeUnit.SECONDS); // 设置过期时间
        }

        //如果超过最大访问次数
        if (count != null && count > MAX_COUNT){
            log.warn("用户{}请求次数超过限制", currentUserId);
            throw new GlobalException(CodeMsg.RATE_LIMIT);
        }

        return true;
    }

}
