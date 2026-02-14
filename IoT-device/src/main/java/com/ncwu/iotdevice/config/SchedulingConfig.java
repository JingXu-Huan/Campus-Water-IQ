package com.ncwu.iotdevice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 这是定时任务配置类
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/23
 */
@Configuration
public class SchedulingConfig {

    /**
     * 使用自定义线程池
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("My-Scheduling-Pool");
        scheduler.setPoolSize(5);  // 设置线程池大小
        scheduler.initialize();
        return scheduler;
    }
}