package com.ncwu.common.utils;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */
@Component
@RequiredArgsConstructor
public class Utils {
    private final StringRedisTemplate redisTemplate;
    public static double keep2(double num) {
        return BigDecimal.valueOf(num)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

}

