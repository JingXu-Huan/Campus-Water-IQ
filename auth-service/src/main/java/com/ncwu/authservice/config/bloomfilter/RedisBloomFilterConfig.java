package com.ncwu.authservice.config.bloomfilter;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RedisBloomFilterConfig {

    private final RedissonClient redissonClient;

    @Bean
    public List<RBloomFilter<String>> userBloomFilter() {
        RBloomFilter<String> emailBloomFilter = redissonClient.getBloomFilter("Email:bloomfilter");
        RBloomFilter<String> phoneBloomFilter = redissonClient.getBloomFilter("Phone:bloomfilter");
        RBloomFilter<String> uidBloomFilter = redissonClient.getBloomFilter("Uid:bloomfilter");
        
        // 初始化布隆过滤器（仅首次需要）
        if (!uidBloomFilter.isExists() || !phoneBloomFilter.isExists() || !emailBloomFilter.isExists()) {
            uidBloomFilter.tryInit(100000L, 0.01);
            phoneBloomFilter.tryInit(100000L, 0.01);
            emailBloomFilter.tryInit(100000L, 0.01);
        }
        
        return List.of(emailBloomFilter, phoneBloomFilter, uidBloomFilter);
    }
}