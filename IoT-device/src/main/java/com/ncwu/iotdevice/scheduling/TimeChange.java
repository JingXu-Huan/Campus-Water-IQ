package com.ncwu.iotdevice.scheduling;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeChange {
    private final StringRedisTemplate redisTemplate;

    //redis 时钟五分钟指针动一次
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void timeChange() {
        //读取旧值 time以秒为单位
        int time = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("Time")));
        time += 60 * 5;
        redisTemplate.opsForValue().set("Time", String.valueOf((time) % (24 * 60 * 60)));
        log.info("修改时间成功,现在是:{}", time);
    }
}
