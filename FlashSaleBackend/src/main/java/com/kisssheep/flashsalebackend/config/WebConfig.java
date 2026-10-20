package com.kisssheep.flashsalebackend.config;

import com.kisssheep.flashsalebackend.interceptor.LoginInterceptor;
import com.kisssheep.flashsalebackend.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置 - 注册拦截器 拦截器负责拦截,校验 配置类里负责注册拦截器 指定拦截的路径
 * 给两个拦截器指定相应的路径
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer{

    private final LoginInterceptor loginInterceptor;

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器,需要token校验的路径
        registry.addInterceptor(loginInterceptor)
                //参与秒杀和查看订单的路径
                .addPathPatterns("/api/seckill/**", "/api/order/**")  // 拦截这些
                //登录的路径和查看商品的路径
                .excludePathPatterns("/api/user/login", "/api/goods"); // 排除这些
        //限流拦截器 只拦截秒杀接口即可
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/seckill/**");
    }
}
