package com.ncwu.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {
    
    private final RedissonClient redissonClient;
    
    /**
     * 尝试获取访问许可
     * @param key 限流键
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key) {
        String limiterKey = "rate_limit:" + key;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(limiterKey);
        log.info("限流器键: {}", limiterKey);
        // 只有在限流器不存在时才设置规则
        if (!rateLimiter.isExists()) {
            // 设置限流规则：每10秒最多1次请求（测试用）
            rateLimiter.setRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);
            log.info("创建新的限流器: {}", limiterKey);
        } else {
            log.info("使用已有限流器: {}", limiterKey);
        }
        // 尝试获取许可
        boolean result = rateLimiter.tryAcquire();
        log.info("限流结果: {} for key: {}", result, limiterKey);
        return result;
    }
}
