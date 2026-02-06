package com.ncwu.repairservice.tools;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Collection;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/6
 */
public class Utils {
    public static boolean isUnValidDeviceId(Collection<String> deviceCode, StringRedisTemplate redisTemplate) {
        return !deviceCode.stream().allMatch(code ->
                Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("device:meter", code)) ||
                        Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("device:sensor", code))
        );
    }
}

