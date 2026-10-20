package com.kisssheep.flashsalebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
//在别的类中使用@Async注解，就可以实现异步方法调用 具体调用哪个方法池 可以通过下面的线程池@Bean注解进行声明
@EnableAsync
public class ThreadPoolConfig {

    @Bean("seckillExecutor")//另一个@Async中可以通过Bean的名称进行指定
    public ThreadPoolTaskExecutor seckillExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //常驻线程
        executor.setCorePoolSize(5);
        //峰值线程
        executor.setMaxPoolSize(10);
        //缓冲队列,缓冲队列满了之后才会占用最大线程
        executor.setQueueCapacity(100);
        //设置扩容出来线程的最大保存时间
        executor.setKeepAliveSeconds(60);
        //设置线程名称的前缀 方便后期错误的排查
        executor.setThreadNamePrefix("seckillExecutor-");
        // 拒绝策略：调用者线程执行，防止任务丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //初始化
        executor.initialize();
        return executor;
    }

}
