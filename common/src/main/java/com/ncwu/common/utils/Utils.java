package com.ncwu.common.utils;


import com.alibaba.nacos.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.*;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */
@Component
@RequiredArgsConstructor
public class Utils {
    public static double keep2(double num) {
        return BigDecimal.valueOf(num)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 得到一个自定义线程池
     *
     * @param name        线程池名称
     * @param coreSize        核心线程池数量
     * @param maxSize     最大线程数量
     * @param seconds     线程空闲生存时间
     * @param tasksLength 最多任务数量
     * @return pool 自定义线程池
     */
    public static ExecutorService getExecutorPools(String name, int coreSize, int maxSize, int seconds, int tasksLength) {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name).build();
        return new ThreadPoolExecutor(
                coreSize,                      // 核心线程
                maxSize,                     // 最大线程
                seconds, TimeUnit.SECONDS,  // 空闲生存时间
                new ArrayBlockingQueue<>(tasksLength), // 有界队列
                namedThreadFactory,     // 自定义名称方便排查
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：队列满了让调用者自己执行
        );
    }

}

